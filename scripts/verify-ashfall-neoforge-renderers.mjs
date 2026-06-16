import { promises as fs } from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { pathToFileURL } from 'node:url'

const SCHEMA = 'echo.modules.ashfall_neoforge_renderer_audit.v1'
const DEFAULT_MANIFEST = '../ECHO-Ashfall-NeoForge-Edition/release-manifest.template.json'

function usage() {
  return `Usage: node scripts/verify-ashfall-neoforge-renderers.mjs [options]

Checks the Ashfall NeoForge module closure for ECHO entity types without client
renderer registration. Because Ashfall includes echorendercore, modules with a
RenderCore integration must cover every declared entity there as well as in the
general renderer registration path.

Options:
  --repo-root <path>   ECHO-Modules root. Default: current directory.
  --manifest <path>    Ashfall NeoForge release manifest template.
                       Default: ${DEFAULT_MANIFEST}
  --out <path>         Optional JSON report output path.
  --json               Print the full JSON report.
  --help               Print this help text.
`
}

function parseArgs(argv) {
  const args = {
    repoRoot: process.cwd(),
    manifest: '',
    out: '',
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    const next = () => {
      const value = argv[++index]
      if (!value) throw new Error(`${arg} requires a value`)
      return value
    }
    if (arg === '--repo-root') args.repoRoot = path.resolve(next())
    else if (arg === '--manifest') args.manifest = path.resolve(next())
    else if (arg === '--out') args.out = path.resolve(next())
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  args.repoRoot = path.resolve(args.repoRoot)
  args.manifest = args.manifest || path.resolve(args.repoRoot, DEFAULT_MANIFEST)
  return args
}

async function readJson(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'))
}

async function exists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

async function walk(dir, predicate = () => true) {
  const found = []
  async function visit(current) {
    let entries
    try {
      entries = await fs.readdir(current, { withFileTypes: true })
    } catch {
      return
    }
    for (const entry of entries) {
      const absolutePath = path.join(current, entry.name)
      if (entry.isDirectory()) {
        await visit(absolutePath)
      } else if (predicate(absolutePath, entry.name)) {
        found.push(absolutePath)
      }
    }
  }
  await visit(dir)
  return found
}

function collectMatches(regex, text) {
  const found = new Set()
  for (const match of text.matchAll(regex)) found.add(match[1])
  return found
}

function sorted(values) {
  return [...values].sort((a, b) => a.localeCompare(b))
}

function moduleIdsFromManifest(manifest) {
  const requirements = Array.isArray(manifest.moduleRequirements) && manifest.moduleRequirements.length > 0
    ? manifest.moduleRequirements
    : Array.isArray(manifest.modules)
      ? manifest.modules
      : []
  return sorted(new Set(requirements.map((entry) => entry.moduleId || entry.id).filter(Boolean)))
}

async function collectEntityConstants(javaFiles) {
  const entities = new Set()
  for (const filePath of javaFiles.filter((file) => path.basename(file) === 'ModEntities.java')) {
    const text = await fs.readFile(filePath, 'utf8')
    for (const name of collectMatches(
      /(?:public\s+)?static\s+final\s+(?:EchoBackendRegistryEntry|NativeRegistryHolder)\s*<\s*EntityType\s*<[^>]+>\s*>\s+([A-Z0-9_]+)\s*=/gu,
      text,
    )) {
      entities.add(name)
    }
  }
  return entities
}

async function collectRendererReferences(javaFiles) {
  const all = new Set()
  const renderCore = new Set()
  const general = new Set()
  for (const filePath of javaFiles) {
    const text = await fs.readFile(filePath, 'utf8')
    const refs = new Set([
      ...collectMatches(/registerEntityRenderer\s*\([\s\S]*?ModEntities\.([A-Z0-9_]+)\.get\s*\(/gu, text),
      ...collectMatches(/EntityRenderers\.register\s*\([\s\S]*?ModEntities\.([A-Z0-9_]+)\.get\s*\(/gu, text),
    ])
    for (const ref of refs) {
      all.add(ref)
      if (/RenderCoreClientIntegration\.java$/u.test(filePath)) renderCore.add(ref)
      else general.add(ref)
    }
  }
  return { all, renderCore, general }
}

export async function generateAshfallNeoForgeRendererAudit(options = {}) {
  const repoRoot = path.resolve(options.repoRoot || process.cwd())
  const manifestPath = path.resolve(options.manifest || path.resolve(repoRoot, DEFAULT_MANIFEST))
  const manifest = await readJson(manifestPath)
  const moduleIds = moduleIdsFromManifest(manifest)
  const renderCoreRequired = moduleIds.includes('echorendercore')
  const modules = []
  const issues = []

  for (const moduleId of moduleIds) {
    const moduleRoot = path.join(repoRoot, 'addons', moduleId)
    const sourceRoot = path.join(moduleRoot, 'src', 'main', 'java')
    if (!(await exists(moduleRoot))) {
      const moduleReport = {
        moduleId,
        present: false,
        entityCount: 0,
        rendererRegistrationCount: 0,
        renderCoreRegistrationCount: 0,
        missingRendererRegistrations: [],
        missingRenderCoreRegistrations: [],
      }
      modules.push(moduleReport)
      issues.push({ moduleId, reason: 'module source directory missing' })
      continue
    }

    const javaFiles = await walk(sourceRoot, (filePath) => filePath.endsWith('.java'))
    const entities = await collectEntityConstants(javaFiles)
    if (entities.size === 0) continue

    const rendererRefs = await collectRendererReferences(javaFiles)
    const hasRenderCoreIntegration = javaFiles.some((filePath) => /RenderCoreClientIntegration\.java$/u.test(filePath))
    const missingRendererRegistrations = sorted([...entities].filter((entity) => !rendererRefs.all.has(entity)))
    const missingRenderCoreRegistrations = renderCoreRequired && hasRenderCoreIntegration
      ? sorted([...entities].filter((entity) => !rendererRefs.renderCore.has(entity)))
      : []
    const moduleReport = {
      moduleId,
      present: true,
      entityCount: entities.size,
      entities: sorted(entities),
      rendererRegistrationCount: rendererRefs.all.size,
      renderCoreRegistrationCount: rendererRefs.renderCore.size,
      hasRenderCoreIntegration,
      missingRendererRegistrations,
      missingRenderCoreRegistrations,
    }
    modules.push(moduleReport)
    if (missingRendererRegistrations.length > 0 || missingRenderCoreRegistrations.length > 0) {
      issues.push({
        moduleId,
        missingRendererRegistrations,
        missingRenderCoreRegistrations,
      })
    }
  }

  return {
    schema: SCHEMA,
    generatedAt: new Date().toISOString(),
    pack: manifest.pack || 'ashfall-neoforge-edition',
    manifestPath,
    moduleCount: moduleIds.length,
    entityModuleCount: modules.length,
    entityCount: modules.reduce((sum, moduleReport) => sum + moduleReport.entityCount, 0),
    renderCoreRequired,
    ok: issues.length === 0,
    issueCount: issues.length,
    issues,
    modules,
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    console.log(usage())
    return
  }

  const report = await generateAshfallNeoForgeRendererAudit(args)
  if (args.out) {
    await fs.mkdir(path.dirname(args.out), { recursive: true })
    await fs.writeFile(args.out, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  }
  if (args.json) {
    console.log(JSON.stringify(report, null, 2))
  } else if (report.ok) {
    console.log(`Ashfall NeoForge renderer audit passed: ${report.entityCount} entity type(s) across ${report.entityModuleCount} module(s).`)
  }
  if (!report.ok) {
    const summary = report.issues
      .map((issue) => `${issue.moduleId}: missing renderer=${issue.missingRendererRegistrations?.join(', ') || 'none'}; missing RenderCore=${issue.missingRenderCoreRegistrations?.join(', ') || 'none'}`)
      .join('\n')
    throw new Error(`Ashfall NeoForge renderer audit failed:\n${summary}`)
  }
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : error)
    process.exitCode = 1
  })
}

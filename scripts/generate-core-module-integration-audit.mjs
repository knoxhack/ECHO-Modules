import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const DESCRIPTOR_PATH = path.join('src', 'main', 'resources', 'META-INF', 'echo.mod.json')
const DEFAULT_OUT = path.join('reports', 'echo-native', 'core-module-integration-audit.json')
const CORE_SPINE_MODULES = [
  'echocore',
  'echoplatformcore',
  'echoadaptercore',
  'echopackcore',
  'echoschemacore',
  'echovalidationcore',
  'echometadatacore',
  'echomodulegraph',
  'echohealthcore',
  'echobridgecore',
  'echocontentcore',
  'echoassetcore',
  'echoreportcore',
  'echonetcore',
  'echodatacore',
  'echoruntimeguard',
  'echoinputcore',
  'echoscreencore',
  'echorendercore',
  'echothemecore',
  'echosoundcore',
  'echohudcore',
  'echonotificationcore',
  'echoguidecore',
  'echocodexcore',
  'echolorecore',
  'echowiki',
  'echoindex',
  'echolens',
  'echoholomap',
  'echoterminal',
  'echomissioncore',
  'echoworldcore',
  'echorecovery',
]

async function exists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

async function readJson(filePath) {
  const text = await fs.readFile(filePath, 'utf8')
  return JSON.parse(text.charCodeAt(0) === 0xfeff ? text.slice(1) : text)
}

async function discoverModules(repoRoot) {
  const addonsRoot = path.join(repoRoot, 'addons')
  const entries = await fs.readdir(addonsRoot, { withFileTypes: true })
  const modules = []
  for (const entry of entries) {
    if (!entry.isDirectory()) continue
    const moduleRoot = path.join(addonsRoot, entry.name)
    const descriptorPath = path.join(moduleRoot, DESCRIPTOR_PATH)
    if (!(await exists(descriptorPath))) continue
    const descriptor = await readJson(descriptorPath)
    modules.push(await moduleRecord(repoRoot, entry.name, moduleRoot, descriptorPath, descriptor))
  }
  return modules.sort((left, right) => left.moduleId.localeCompare(right.moduleId))
}

async function moduleRecord(repoRoot, directoryName, moduleRoot, descriptorPath, descriptor) {
  const access = descriptor.access && typeof descriptor.access === 'object' ? descriptor.access : {}
  const adapterCore = access.adapterCore && typeof access.adapterCore === 'object' ? access.adapterCore : {}
  const nativeEntrypoint = string(access.nativeEntrypoint)
  const moduleId = string(descriptor.id)
  const directory = `addons/${directoryName}`
  const hasBuildGradle = await exists(path.join(moduleRoot, 'build.gradle'))
  const hasJavaSource = await exists(path.join(moduleRoot, 'src', 'main', 'java'))
  const adapterCoreRuntimes = cleanList(adapterCore.runtimes)
  const bridgeable = nativeEntrypoint.length > 0

  return {
    moduleId,
    directory,
    descriptorPath: path.relative(repoRoot, descriptorPath).replace(/\\/g, '/'),
    name: string(descriptor.name),
    version: string(descriptor.version),
    kind: string(descriptor.kind),
    role: string(descriptor.role),
    channel: string(descriptor.channel),
    official: Boolean(descriptor.official),
    trustLevel: string(descriptor.trustLevel),
    side: string(descriptor.side),
    standalone: descriptor.standalone !== false,
    requires: cleanList(descriptor.requires),
    optional: cleanList(descriptor.optional),
    provides: cleanList(descriptor.provides),
    consumes: cleanList(descriptor.consumes),
    apiStability: descriptor.apiStability ?? null,
    nativeEntrypoint,
    adapterCoreRuntimes,
    hasBuildGradle,
    hasJavaSource,
    inCoreSpineAudit: CORE_SPINE_MODULES.includes(moduleId),
    nativeIntegrationStatus: bridgeable ? 'LEGACY_ADAPTER_BRIDGEABLE' : 'NO_NATIVE_ENTRYPOINT',
  }
}

function cleanList(value) {
  return Array.isArray(value)
    ? value.filter((item) => typeof item === 'string' && item.trim().length > 0)
    : []
}

function string(value) {
  return typeof value === 'string' ? value : ''
}

function validate(modules, { strictCounts = true } = {}) {
  const errors = []
  const ids = new Set()
  for (const module of modules) {
    if (!module.moduleId) {
      errors.push(`${module.directory}: descriptor is missing id`)
      continue
    }
    if (ids.has(module.moduleId)) errors.push(`${module.directory}: duplicate module id ${module.moduleId}`)
    ids.add(module.moduleId)
    if (module.nativeIntegrationStatus === 'LEGACY_ADAPTER_BRIDGEABLE' && !module.hasBuildGradle) {
      errors.push(`${module.moduleId}: bridgeable native module is missing build.gradle`)
    }
  }

  if (strictCounts) {
    for (const moduleId of CORE_SPINE_MODULES) {
      if (!ids.has(moduleId)) errors.push(`core spine module missing descriptor: ${moduleId}`)
    }
  }

  const bridgeableCount = modules.filter((module) => module.nativeIntegrationStatus === 'LEGACY_ADAPTER_BRIDGEABLE').length
  const coreSpineCount = modules.filter((module) => module.inCoreSpineAudit).length
  if (strictCounts && bridgeableCount < 90) {
    errors.push(`expected at least 90 bridgeable native modules, found ${bridgeableCount}`)
  }
  if (strictCounts && coreSpineCount < CORE_SPINE_MODULES.length) {
    errors.push(`expected ${CORE_SPINE_MODULES.length} core spine modules, found ${coreSpineCount}`)
  }

  return { errors, bridgeableCount, coreSpineCount }
}

export async function generateCoreModuleIntegrationAudit({
  repoRoot = process.cwd(),
  out = path.join(process.cwd(), DEFAULT_OUT),
  strictCounts = true,
} = {}) {
  const normalizedRoot = path.resolve(repoRoot)
  const outputPath = path.resolve(normalizedRoot, out)
  const modules = await discoverModules(normalizedRoot)
  const validation = validate(modules, { strictCounts })
  if (validation.errors.length > 0) {
    throw new Error(validation.errors.join('\n'))
  }

  const report = {
    schema: 'echo.native.core_module_integration_audit.v1',
    generatedAt: new Date().toISOString(),
    generatedFrom: 'addons/*/src/main/resources/META-INF/echo.mod.json',
    repoRoot: normalizedRoot.replace(/\\/g, '/'),
    moduleCount: modules.length,
    bridgeableModuleCount: validation.bridgeableCount,
    coreSpineModuleCount: validation.coreSpineCount,
    coreSpineModules: CORE_SPINE_MODULES,
    modules,
  }
  await fs.mkdir(path.dirname(outputPath), { recursive: true })
  await fs.writeFile(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  return { outputPath, report }
}

function parseArgs(argv) {
  const options = {
    repoRoot: process.cwd(),
    out: DEFAULT_OUT,
    strictCounts: true,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--repo-root') options.repoRoot = argv[++index]
    else if (arg === '--out') options.out = argv[++index]
    else if (arg === '--no-strict-counts') options.strictCounts = false
    else if (arg === '--help') options.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return options
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const options = parseArgs(process.argv.slice(2))
    if (options.help) {
      console.log('Usage: node scripts/generate-core-module-integration-audit.mjs [--repo-root <path>] [--out <path>] [--no-strict-counts]')
    } else {
      const result = await generateCoreModuleIntegrationAudit(options)
      console.log(`Wrote core module integration audit: ${result.outputPath}`)
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}

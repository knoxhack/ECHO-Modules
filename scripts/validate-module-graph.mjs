import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const DESCRIPTOR_PATH = path.join('src', 'main', 'resources', 'META-INF', 'echo.mod.json')

async function fileExists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

async function readJson(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'))
}

async function discoverDescriptors(repoRoot) {
  const addonsRoot = path.join(repoRoot, 'addons')
  const entries = await fs.readdir(addonsRoot, { withFileTypes: true })
  const descriptors = []
  for (const entry of entries) {
    if (!entry.isDirectory()) continue
    const descriptorPath = path.join(addonsRoot, entry.name, DESCRIPTOR_PATH)
    if (!(await fileExists(descriptorPath))) continue
    descriptors.push({
      moduleDir: entry.name,
      descriptorPath,
      descriptor: await readJson(descriptorPath),
    })
  }
  return descriptors.sort((left, right) => left.moduleDir.localeCompare(right.moduleDir))
}

function dependencyIds(descriptor) {
  const required = Array.isArray(descriptor.requires) ? descriptor.requires : []
  const optional = Array.isArray(descriptor.optional) ? descriptor.optional : []
  return [
    ...required.map((id) => ({ id, kind: 'requires' })),
    ...optional.map((id) => ({ id, kind: 'optional' })),
  ]
}

function moduleRecord(repoRoot, entry) {
  const descriptor = entry.descriptor
  return {
    id: descriptor.id,
    name: descriptor.name,
    version: descriptor.version,
    kind: descriptor.kind,
    role: descriptor.role,
    channel: descriptor.channel,
    official: Boolean(descriptor.official),
    trustLevel: descriptor.trustLevel,
    side: descriptor.side,
    standalone: descriptor.standalone !== false,
    descriptorPath: path.relative(repoRoot, entry.descriptorPath).replace(/\\/g, '/'),
    moduleDir: `addons/${entry.moduleDir}`,
    requires: descriptor.requires ?? [],
    optional: descriptor.optional ?? [],
    provides: descriptor.provides ?? [],
    consumes: descriptor.consumes ?? [],
    apiStability: descriptor.apiStability ?? null,
  }
}

async function writeIndex(repoRoot, descriptors) {
  const indexPath = path.join(repoRoot, 'metadata', 'modules', 'index.json')
  const modules = descriptors.map((entry) => moduleRecord(repoRoot, entry))
  const index = {
    schemaVersion: 1,
    generatedFrom: 'addons/*/src/main/resources/META-INF/echo.mod.json',
    generatedAt: new Date().toISOString(),
    moduleCount: modules.length,
    modules,
  }
  await fs.mkdir(path.dirname(indexPath), { recursive: true })
  await fs.writeFile(indexPath, `${JSON.stringify(index, null, 2)}\n`, 'utf8')
}

export async function validateModuleGraph({ repoRoot = process.cwd(), write = false } = {}) {
  const descriptors = await discoverDescriptors(repoRoot)
  const ids = new Set()
  const errors = []

  for (const entry of descriptors) {
    const id = entry.descriptor.id
    if (!id || typeof id !== 'string') {
      errors.push(`${entry.moduleDir}: descriptor is missing string id`)
      continue
    }
    if (ids.has(id)) errors.push(`${entry.moduleDir}: duplicate module id ${id}`)
    ids.add(id)
  }

  for (const entry of descriptors) {
    const sourceId = entry.descriptor.id ?? entry.moduleDir
    for (const dependency of dependencyIds(entry.descriptor)) {
      if (!ids.has(dependency.id)) {
        errors.push(`${sourceId}: ${dependency.kind} missing descriptor for ${dependency.id}`)
      }
    }
  }

  if (errors.length > 0) {
    throw new Error(errors.join('\n'))
  }

  if (write) await writeIndex(repoRoot, descriptors)
  return { moduleCount: descriptors.length }
}

function parseArgs(argv) {
  return {
    write: argv.includes('--write-index'),
    help: argv.includes('--help'),
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  const options = parseArgs(process.argv.slice(2))
  if (options.help) {
    console.log('Usage: node scripts/validate-module-graph.mjs [--write-index]')
  } else {
    validateModuleGraph({ write: options.write })
      .then((result) => {
        console.log(`Module graph valid for ${result.moduleCount} descriptor(s).`)
      })
      .catch((error) => {
        console.error(error.message)
        process.exitCode = 1
      })
  }
}

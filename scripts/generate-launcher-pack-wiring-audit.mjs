import { createHash } from 'node:crypto'
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'
import zlib from 'node:zlib'

const DEFAULT_LAUNCHER_INSTANCES_ROOT = path.join(os.homedir(), 'ECHOLauncher', 'Instances')
const DEFAULT_OUT_DIR = path.join('reports', 'runtime-parity')
const REPORT_JSON = 'launcher-pack-runtime-wiring-audit.json'
const REPORT_MD = 'launcher-pack-runtime-wiring-audit.md'
const SURFACE_MODULES = [
  'echohudcore',
  'echoscreencore',
  'echothemecore',
  'echoterminal',
  'echoindex',
  'echolens',
  'echoholomap',
]
const KNOWN_EXTERNAL_MODS = new Set(['java', 'minecraft', 'neoforge'])

function parseArgs(argv) {
  const args = {
    launcherInstancesRoot: DEFAULT_LAUNCHER_INSTANCES_ROOT,
    outDir: DEFAULT_OUT_DIR,
    strict: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--launcher-instances-root') args.launcherInstancesRoot = path.resolve(argv[++index])
    else if (arg === '--out-dir') args.outDir = path.resolve(argv[++index])
    else if (arg === '--strict') args.strict = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

async function fileExists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

async function sha256File(filePath) {
  return createHash('sha256').update(await fs.readFile(filePath)).digest('hex')
}

function readZipEntries(buffer) {
  let eocd = -1
  const minimum = Math.max(0, buffer.length - 65557)
  for (let offset = buffer.length - 22; offset >= minimum; offset -= 1) {
    if (buffer.readUInt32LE(offset) === 0x06054b50) {
      eocd = offset
      break
    }
  }
  if (eocd < 0) throw new Error('ZIP end-of-central-directory record not found.')
  const entryCount = buffer.readUInt16LE(eocd + 10)
  const centralDirOffset = buffer.readUInt32LE(eocd + 16)
  const entries = []
  let cursor = centralDirOffset
  for (let index = 0; index < entryCount; index += 1) {
    if (buffer.readUInt32LE(cursor) !== 0x02014b50) throw new Error('Invalid ZIP central directory entry.')
    const method = buffer.readUInt16LE(cursor + 10)
    const compressedSize = buffer.readUInt32LE(cursor + 20)
    const uncompressedSize = buffer.readUInt32LE(cursor + 24)
    const nameLength = buffer.readUInt16LE(cursor + 28)
    const extraLength = buffer.readUInt16LE(cursor + 30)
    const commentLength = buffer.readUInt16LE(cursor + 32)
    const localHeaderOffset = buffer.readUInt32LE(cursor + 42)
    const name = buffer.subarray(cursor + 46, cursor + 46 + nameLength).toString('utf8')
    entries.push({ name, method, compressedSize, uncompressedSize, localHeaderOffset })
    cursor += 46 + nameLength + extraLength + commentLength
  }
  return entries
}

function readZipEntry(buffer, entry) {
  const cursor = entry.localHeaderOffset
  if (buffer.readUInt32LE(cursor) !== 0x04034b50) throw new Error(`Invalid ZIP local header for ${entry.name}.`)
  const nameLength = buffer.readUInt16LE(cursor + 26)
  const extraLength = buffer.readUInt16LE(cursor + 28)
  const dataStart = cursor + 30 + nameLength + extraLength
  const compressed = buffer.subarray(dataStart, dataStart + entry.compressedSize)
  if (entry.method === 0) return compressed
  if (entry.method === 8) return zlib.inflateRawSync(compressed, { finishFlush: zlib.constants.Z_SYNC_FLUSH })
  throw new Error(`Unsupported ZIP compression method ${entry.method} for ${entry.name}.`)
}

async function inspectZip(filePath) {
  const buffer = await fs.readFile(filePath)
  const entries = readZipEntries(buffer)
  const entryMap = new Map(entries.map((entry) => [entry.name, entry]))
  return {
    entries: new Set(entries.map((entry) => entry.name)),
    text(name) {
      const entry = entryMap.get(name)
      return entry ? readZipEntry(buffer, entry).toString('utf8') : null
    },
    json(name) {
      const text = this.text(name)
      return text ? JSON.parse(text) : null
    },
  }
}

function detectLane(instanceName) {
  if (/\bNeoForge Edition$/u.test(instanceName)) {
    return { id: 'neoforge', artifactDir: 'mods', extension: '.jar', artifactFamily: 'neoforge' }
  }
  if (/\bNative Edition$/u.test(instanceName)) {
    return { id: 'echo_native', artifactDir: 'addons', extension: '.echo-addon', artifactFamily: 'echo-addon' }
  }
  if (/\bStandalone Edition$/u.test(instanceName)) {
    return { id: 'standalone', artifactDir: 'mods', extension: '.jar', artifactFamily: 'standalone' }
  }
  return null
}

async function discoverInstances(root) {
  if (!(await fileExists(root))) return []
  const entries = await fs.readdir(root, { withFileTypes: true })
  return entries
    .filter((entry) => entry.isDirectory())
    .map((entry) => ({ name: entry.name, root: path.join(root, entry.name), lane: detectLane(entry.name) }))
    .filter((entry) => entry.lane)
    .sort((left, right) => left.name.localeCompare(right.name))
}

function normalizeDependencyId(value) {
  if (typeof value === 'string') return value
  if (value && typeof value === 'object') return String(value.id ?? value.moduleId ?? '')
  return ''
}

function dependencyVersion(value) {
  if (value && typeof value === 'object') return String(value.version ?? value.versionRange ?? '*')
  return '*'
}

function echoDependencies(descriptor) {
  return (Array.isArray(descriptor?.requires) ? descriptor.requires : [])
    .map((dependency) => ({
      id: normalizeDependencyId(dependency),
      version: dependencyVersion(dependency),
    }))
    .filter((dependency) => dependency.id && !KNOWN_EXTERNAL_MODS.has(dependency.id))
}

function parseTomlScalar(block, key) {
  const match = new RegExp(`^\\s*${key}\\s*=\\s*\"([^\"]*)\"`, 'mu').exec(block)
  return match ? match[1] : ''
}

function parseNeoForgeToml(tomlText) {
  const text = String(tomlText ?? '')
  const moduleBlock = text.split(/\n(?=\[\[dependencies\.)/u)[0] ?? text
  const dependencies = []
  for (const block of text.split(/\r?\n(?=\s*\[\[dependencies\.)/u)) {
    const match = /^\s*\[\[dependencies\.([^\]]+)\]\]/u.exec(block)
    if (!match) continue
    dependencies.push({
      owner: match[1],
      modId: parseTomlScalar(block, 'modId'),
      type: parseTomlScalar(block, 'type') || 'required',
      versionRange: parseTomlScalar(block, 'versionRange') || '*',
      ordering: parseTomlScalar(block, 'ordering'),
      side: parseTomlScalar(block, 'side'),
    })
  }
  return {
    modId: parseTomlScalar(moduleBlock, 'modId'),
    version: parseTomlScalar(moduleBlock, 'version'),
    displayName: parseTomlScalar(moduleBlock, 'displayName'),
    dependencies,
  }
}

function compareVersion(actual, expected) {
  const actualParts = String(actual).split(/[.-]/u).map((part) => (/^\d+$/u.test(part) ? Number(part) : part))
  const expectedParts = String(expected).split(/[.-]/u).map((part) => (/^\d+$/u.test(part) ? Number(part) : part))
  const length = Math.max(actualParts.length, expectedParts.length)
  for (let index = 0; index < length; index += 1) {
    const left = actualParts[index] ?? 0
    const right = expectedParts[index] ?? 0
    if (typeof left === 'number' && typeof right === 'number') {
      if (left !== right) return left - right
    } else {
      const result = String(left).localeCompare(String(right))
      if (result !== 0) return result
    }
  }
  return 0
}

function satisfiesVersionRange(actual, range) {
  const cleanRange = String(range ?? '*').trim()
  if (!cleanRange || cleanRange === '*') return true
  const lower = cleanRange.match(/^\[([^,\]]+),/u)
  if (lower) return compareVersion(actual, lower[1]) >= 0
  return actual === cleanRange
}

async function inspectArtifact(filePath, lane) {
  const zip = await inspectZip(filePath)
  const descriptor = zip.json('META-INF/echo.mod.json')
  const packageManifest = lane.id === 'echo_native' ? zip.json('echo-addon-package.json') : null
  const neoForgeTomlText = lane.id === 'neoforge' ? zip.text('META-INF/neoforge.mods.toml') : null
  const neoForge = neoForgeTomlText ? parseNeoForgeToml(neoForgeTomlText) : null
  return {
    fileName: path.basename(filePath),
    path: filePath,
    sha256: await sha256File(filePath),
    moduleId: descriptor?.id ?? neoForge?.modId ?? null,
    version: descriptor?.version ?? neoForge?.version ?? null,
    descriptor,
    packageManifest,
    neoForge,
    hasDescriptor: Boolean(descriptor),
    hasPackageManifest: Boolean(packageManifest),
    hasNeoForgeToml: Boolean(neoForgeTomlText),
    hasNamedRegisterModListener: zip.entries.has('com/knoxhack/echo/adaptercore/EchoNativeBridge.class')
      || zip.entries.has('com/knoxhack/echo/adaptercore/EchoAdapterCoreSpinePublisher.class'),
  }
}

async function auditInstance(instance) {
  const artifactRoot = path.join(instance.root, instance.lane.artifactDir)
  const issues = []
  if (!(await fileExists(artifactRoot))) {
    return {
      instance: instance.name,
      lane: instance.lane.id,
      artifactRoot,
      status: 'fail',
      artifactCount: 0,
      moduleCount: 0,
      surfaceModules: {},
      missingSurfaceModules: SURFACE_MODULES,
      missingRequiredDependencies: [],
      unsupportedRequiredDependencies: [],
      issues: [`missing artifact directory ${artifactRoot}`],
      modules: [],
    }
  }

  const files = (await fs.readdir(artifactRoot, { withFileTypes: true }))
    .filter((entry) => entry.isFile() && entry.name.endsWith(instance.lane.extension))
    .map((entry) => path.join(artifactRoot, entry.name))
    .sort((left, right) => path.basename(left).localeCompare(path.basename(right)))

  const modules = []
  for (const file of files) {
    try {
      const inspected = await inspectArtifact(file, instance.lane)
      if (inspected.moduleId?.startsWith('echo') || inspected.moduleId?.startsWith('signalos')) modules.push(inspected)
    } catch (error) {
      issues.push(`${path.basename(file)} could not be inspected: ${error instanceof Error ? error.message : String(error)}`)
    }
  }

  const modulesById = new Map(modules.filter((module) => module.moduleId).map((module) => [module.moduleId, module]))
  const missingRequiredDependencies = []
  const unsupportedRequiredDependencies = []

  for (const module of modules) {
    if (!module.hasDescriptor) issues.push(`${module.fileName} is missing META-INF/echo.mod.json`)
    if (instance.lane.id === 'echo_native' && !module.hasPackageManifest) {
      issues.push(`${module.fileName} is missing echo-addon-package.json`)
    }
    if (instance.lane.id === 'neoforge' && !module.hasNeoForgeToml) {
      issues.push(`${module.fileName} is missing META-INF/neoforge.mods.toml`)
    }

    for (const dependency of echoDependencies(module.descriptor)) {
      if (!modulesById.has(dependency.id)) {
        missingRequiredDependencies.push({ moduleId: module.moduleId, dependencyId: dependency.id, version: dependency.version })
      }
    }

    if (instance.lane.id === 'echo_native' && module.packageManifest) {
      for (const dependency of Array.isArray(module.packageManifest.dependencies) ? module.packageManifest.dependencies : []) {
        const dependencyId = normalizeDependencyId(dependency)
        if (dependencyId && !KNOWN_EXTERNAL_MODS.has(dependencyId) && !modulesById.has(dependencyId)) {
          missingRequiredDependencies.push({ moduleId: module.moduleId, dependencyId, version: dependencyVersion(dependency) })
        }
      }
    }

    if (instance.lane.id === 'neoforge' && module.neoForge) {
      for (const dependency of module.neoForge.dependencies) {
        if (dependency.type && dependency.type !== 'required') continue
        if (!dependency.modId || KNOWN_EXTERNAL_MODS.has(dependency.modId)) continue
        const installed = modulesById.get(dependency.modId)
        if (!installed) {
          missingRequiredDependencies.push({
            moduleId: module.moduleId,
            dependencyId: dependency.modId,
            version: dependency.versionRange,
            source: 'neoforge.mods.toml',
          })
        } else if (!satisfiesVersionRange(installed.neoForge?.version ?? installed.version, dependency.versionRange)) {
          unsupportedRequiredDependencies.push({
            moduleId: module.moduleId,
            dependencyId: dependency.modId,
            requiredRange: dependency.versionRange,
            actualVersion: installed.neoForge?.version ?? installed.version,
          })
        }
      }
    }
  }

  const surfaceModules = Object.fromEntries(SURFACE_MODULES.map((moduleId) => [moduleId, modulesById.has(moduleId)]))
  const missingSurfaceModules = SURFACE_MODULES.filter((moduleId) => !modulesById.has(moduleId))
  if (missingSurfaceModules.length > 0) {
    issues.push(`missing core player-facing surface module(s): ${missingSurfaceModules.join(', ')}`)
  }
  if (!modulesById.has('echoadaptercore')) issues.push('missing echoadaptercore')
  const adapterCore = modulesById.get('echoadaptercore')
  if (adapterCore && adapterCore.version !== '1.0.0') issues.push(`echoadaptercore descriptor version is ${adapterCore.version}, expected 1.0.0`)
  if (adapterCore?.neoForge && adapterCore.neoForge.version !== '1.0.0') {
    issues.push(`echoadaptercore NeoForge TOML version is ${adapterCore.neoForge.version}, expected 1.0.0`)
  }

  const uniqueMissingDeps = uniqueObjects(missingRequiredDependencies, (item) => `${item.moduleId}->${item.dependencyId}@${item.version}:${item.source ?? ''}`)
  const uniqueUnsupportedDeps = uniqueObjects(unsupportedRequiredDependencies, (item) => `${item.moduleId}->${item.dependencyId}@${item.requiredRange}:${item.actualVersion}`)
  if (uniqueMissingDeps.length > 0) issues.push(`${uniqueMissingDeps.length} required ECHO dependency gap(s)`)
  if (uniqueUnsupportedDeps.length > 0) issues.push(`${uniqueUnsupportedDeps.length} unsupported ECHO dependency version(s)`)

  return {
    instance: instance.name,
    lane: instance.lane.id,
    artifactRoot,
    status: issues.length === 0 ? 'pass' : 'fail',
    artifactCount: files.length,
    moduleCount: modules.length,
    surfaceModules,
    missingSurfaceModules,
    missingRequiredDependencies: uniqueMissingDeps,
    unsupportedRequiredDependencies: uniqueUnsupportedDeps,
    adapterCoreVersion: adapterCore?.version ?? null,
    adapterCoreNeoForgeVersion: adapterCore?.neoForge?.version ?? null,
    adapterCoreHasNamedRegisterModListener: Boolean(adapterCore?.hasNamedRegisterModListener),
    issues,
    modules: modules.map((module) => ({
      moduleId: module.moduleId,
      version: module.version,
      fileName: module.fileName,
      hasDescriptor: module.hasDescriptor,
      hasPackageManifest: module.hasPackageManifest,
      hasNeoForgeToml: module.hasNeoForgeToml,
      neoForgeVersion: module.neoForge?.version ?? null,
      requires: echoDependencies(module.descriptor),
    })),
  }
}

function uniqueObjects(values, keyFn) {
  const seen = new Set()
  const result = []
  for (const value of values) {
    const key = keyFn(value)
    if (seen.has(key)) continue
    seen.add(key)
    result.push(value)
  }
  return result
}

function markdown(report) {
  const lines = [
    '# Launcher Pack Runtime Wiring Audit',
    '',
    `Generated: ${report.generatedAt}`,
    '',
    `Instances: ${report.summary.instanceCount}`,
    `Passing: ${report.summary.passCount}`,
    `Failing: ${report.summary.failCount}`,
    '',
    '| Instance | Lane | Status | Artifacts | Modules | AdapterCore | Missing surfaces | Dependency gaps | Issues |',
    '| --- | --- | --- | ---: | ---: | --- | --- | ---: | --- |',
  ]
  for (const row of report.rows) {
    lines.push([
      row.instance,
      row.lane,
      row.status,
      row.artifactCount,
      row.moduleCount,
      row.adapterCoreNeoForgeVersion ?? row.adapterCoreVersion ?? '',
      row.missingSurfaceModules.join(', ') || 'none',
      row.missingRequiredDependencies.length + row.unsupportedRequiredDependencies.length,
      row.issues.join('; ') || 'none',
    ].map(escapeCell).join(' | ').replace(/^/u, '| ').replace(/$/u, ' |'))
  }
  return `${lines.join('\n')}\n`
}

function escapeCell(value) {
  return String(value).replace(/\|/gu, '\\|').replace(/\r?\n/gu, '<br>')
}

export async function generateLauncherPackWiringAudit(options = {}) {
  const args = {
    launcherInstancesRoot: options.launcherInstancesRoot ?? DEFAULT_LAUNCHER_INSTANCES_ROOT,
    outDir: options.outDir ?? DEFAULT_OUT_DIR,
    strict: Boolean(options.strict),
  }
  const instances = await discoverInstances(args.launcherInstancesRoot)
  const rows = []
  for (const instance of instances) rows.push(await auditInstance(instance))

  const report = {
    schemaVersion: 'echo.launcher-pack-runtime-wiring-audit.v1',
    generatedAt: new Date().toISOString(),
    launcherInstancesRoot: args.launcherInstancesRoot,
    expectedSurfaceModules: SURFACE_MODULES,
    summary: {
      instanceCount: rows.length,
      passCount: rows.filter((row) => row.status === 'pass').length,
      failCount: rows.filter((row) => row.status === 'fail').length,
      byLane: Object.fromEntries(['neoforge', 'echo_native', 'standalone'].map((lane) => [
        lane,
        {
          count: rows.filter((row) => row.lane === lane).length,
          pass: rows.filter((row) => row.lane === lane && row.status === 'pass').length,
          fail: rows.filter((row) => row.lane === lane && row.status === 'fail').length,
        },
      ])),
    },
    rows,
  }

  await fs.mkdir(args.outDir, { recursive: true })
  const jsonPath = path.join(args.outDir, REPORT_JSON)
  const mdPath = path.join(args.outDir, REPORT_MD)
  await fs.writeFile(jsonPath, `${JSON.stringify(report, null, 2)}\n`)
  await fs.writeFile(mdPath, markdown(report))
  if (args.strict && report.summary.failCount > 0) {
    throw new Error(`Launcher pack wiring audit failed: ${report.summary.failCount} instance(s) have wiring issues.`)
  }
  return report
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  const report = await generateLauncherPackWiringAudit(args)
  console.log(`Launcher pack wiring audit wrote ${path.join(args.outDir, REPORT_JSON)}`)
  console.log(`PASS ${report.summary.passCount}/${report.summary.instanceCount}; FAIL ${report.summary.failCount}`)
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}

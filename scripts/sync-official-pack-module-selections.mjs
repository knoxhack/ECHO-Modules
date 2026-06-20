#!/usr/bin/env node
import crypto from 'node:crypto'
import fs from 'node:fs/promises'
import { existsSync, statSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { constants as zlibConstants, deflateRawSync, inflateRawSync } from 'node:zlib'

const __filename = fileURLToPath(import.meta.url)
const modulesRoot = path.resolve(path.dirname(__filename), '..')
const workspaceRoot = path.resolve(modulesRoot, '..')
const releaseIndexRoot = path.join(workspaceRoot, 'ECHO-Release-Index')
const selectionPath = path.join(modulesRoot, 'metadata', 'official-pack-module-selections.json')
const moduleReleasePath = path.join(modulesRoot, 'dist', 'echo-module-release', 'echo-release.json')

function parseArgs(argv) {
  const options = {
    write: false,
    check: false,
    moduleDownloadBaseUrl: null,
    packKeys: [],
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--write') options.write = true
    else if (arg === '--check') options.check = true
    else if (arg === '--module-download-base-url') options.moduleDownloadBaseUrl = argv[++index]
    else if (arg === '--module-release-tag') {
      const tag = argv[++index]
      options.moduleDownloadBaseUrl = `https://github.com/knoxhack/ECHO-Modules/releases/download/${tag}`
    } else if (arg === '--pack') {
      const packKey = argv[++index]
      if (!packKey) throw new Error(`${arg} requires a value`)
      options.packKeys.push(String(packKey).toLowerCase())
    } else {
      throw new Error(`Unknown argument: ${arg}`)
    }
  }
  if (!options.write) options.check = true
  return options
}

function normalizeDownloadBaseUrl(value) {
  if (!value) return null
  const normalized = String(value).trim().replace(/\/+$/u, '')
  if (!/^https?:\/\/[^/]+/u.test(normalized)) {
    throw new Error(`Invalid module download base URL: ${value}`)
  }
  return normalized
}

const options = parseArgs(process.argv.slice(2))
const writeMode = options.write
const checkMode = options.check
const moduleDownloadBaseUrl = normalizeDownloadBaseUrl(options.moduleDownloadBaseUrl)

const lanes = {
  native: {
    label: 'Native',
    suffix: 'native-edition',
    artifactFamily: 'echo-addon',
    installDir: 'addons',
    artifactPattern: '<module>-<version>.echo-addon',
    sourcePattern: '<module>-<version>-sources.jar',
    runtimeTarget: 'echo_native',
    loader: 'echo-native-loader'
  },
  neoforge: {
    label: 'NeoForge',
    suffix: 'neoforge-edition',
    artifactFamily: 'neoforge',
    installDir: 'mods',
    artifactPattern: '<module>-<version>-neoforge.jar',
    sourcePattern: '<module>-<version>-sources.jar',
    runtimeTarget: 'neoforge',
    loader: 'neoforge'
  },
  standalone: {
    label: 'Standalone',
    suffix: 'standalone-edition',
    artifactFamily: 'standalone',
    installDir: 'mods',
    artifactPattern: '<module>-<version>-standalone.jar',
    sourcePattern: '<module>-<version>-sources.jar',
    runtimeTarget: 'echo_runtime_standalone',
    loader: 'echo-standalone-runtime'
  }
}

const packRepoNames = {
  ashfall: 'ECHO-Ashfall',
  openlands: 'ECHO-Openlands',
  'arcana-division': 'ECHO-Arcana-Division',
  'sky-relay': 'ECHO-Sky-Relay',
  'galactic-survey': 'ECHO-Galactic-Survey'
}

const foundationModules = [
  'echoadaptercore',
  'echoblockworks',
  'echocommonloot',
  'echocontentcore',
  'echocore',
  'echocreatureroles',
  'echofoundationcore',
  'echoholomap',
  'echohudcore',
  'echoindex',
  'echolens',
  'echomaterialcore',
  'echomissioncore',
  'echonetcore',
  'echoplatformcore',
  'echoruntimeguard',
  'echoschemacore',
  'echoscreencore',
  'echosoundcore',
  'echostationcore',
  'echoterminal',
  'echothemecore',
  'echotoolcore',
  'echotutorialcore',
  'echovalidationcore',
  'echoworldcore',
  'echoworldstarter'
]

const packRootModules = {
  ashfall: 'echoashfallprotocol',
  openlands: 'echoopenlandsprotocol',
  'arcana-division': 'echoarcanadivisionprotocol',
  'sky-relay': 'echoskyrelayprotocol',
  'galactic-survey': 'echogalacticsurveyprotocol'
}

const virtualFiles = new Map()
const changedFiles = []
const errors = []
let moduleReleaseArtifacts = new Map()
let moduleReleaseMetadata = null

const crcTable = new Uint32Array(256)
for (let i = 0; i < 256; i += 1) {
  let value = i
  for (let bit = 0; bit < 8; bit += 1) {
    value = (value & 1) ? (0xedb88320 ^ (value >>> 1)) : (value >>> 1)
  }
  crcTable[i] = value >>> 0
}

function formatJson(value) {
  return `${JSON.stringify(value, null, 2)}\n`
}

async function readJson(file) {
  return parseJsonText(await fs.readFile(file, 'utf8'))
}

async function readJsonIfExists(file) {
  if (!existsSync(file) && !hasVirtualFile(file)) return null
  return parseJsonText(await readTextMaybeVirtual(file))
}

function parseJsonText(text) {
  return JSON.parse(String(text).replace(/^\uFEFF/u, ''))
}

function hasVirtualFile(file) {
  return virtualFiles.has(path.resolve(file))
}

function getVirtualFile(file) {
  return virtualFiles.get(path.resolve(file))
}

function setVirtualFile(file, content) {
  virtualFiles.set(path.resolve(file), content)
}

async function readTextMaybeVirtual(file) {
  const resolved = path.resolve(file)
  if (virtualFiles.has(resolved)) {
    const value = virtualFiles.get(resolved)
    return Buffer.isBuffer(value) ? value.toString('utf8') : value
  }
  return fs.readFile(resolved, 'utf8')
}

async function writeTextIfChanged(file, content) {
  const resolved = path.resolve(file)
  const before = existsSync(resolved) ? await readTextMaybeVirtual(resolved) : null
  setVirtualFile(resolved, content)
  if (before === content) return false
  changedFiles.push(resolved)
  if (writeMode) {
    await fs.mkdir(path.dirname(resolved), { recursive: true })
    await fs.writeFile(resolved, content)
  }
  return true
}

async function writeJsonIfChanged(file, value) {
  return writeTextIfChanged(file, formatJson(value))
}

async function writeBytesIfChanged(file, bytes) {
  const resolved = path.resolve(file)
  const buffer = Buffer.isBuffer(bytes) ? bytes : Buffer.from(bytes)
  const before = existsSync(resolved) ? await fs.readFile(resolved) : null
  setVirtualFile(resolved, buffer)
  if (before && before.equals(buffer)) return false
  changedFiles.push(resolved)
  if (writeMode) {
    await fs.mkdir(path.dirname(resolved), { recursive: true })
    await fs.writeFile(resolved, buffer)
  }
  return true
}

function hashBytes(bytes) {
  const buffer = Buffer.isBuffer(bytes) ? bytes : Buffer.from(bytes)
  return {
    size: buffer.length,
    sha256: crypto.createHash('sha256').update(buffer).digest('hex')
  }
}

async function fileDigest(file) {
  const resolved = path.resolve(file)
  if (hasVirtualFile(resolved)) {
    const value = getVirtualFile(resolved)
    return hashBytes(Buffer.isBuffer(value) ? value : Buffer.from(value, 'utf8'))
  }
  const bytes = await fs.readFile(resolved)
  return hashBytes(bytes)
}

function normalizeZipPath(value) {
  return String(value ?? '').replace(/\\/g, '/').replace(/^\/+/u, '')
}

function crc32(buffer) {
  let value = 0xffffffff
  for (const byte of buffer) {
    value = crcTable[(value ^ byte) & 0xff] ^ (value >>> 8)
  }
  return (value ^ 0xffffffff) >>> 0
}

function dosDateTime(date = new Date()) {
  const year = Math.max(date.getFullYear(), 1980)
  const time = (date.getHours() << 11) | (date.getMinutes() << 5) | Math.floor(date.getSeconds() / 2)
  const day = ((year - 1980) << 9) | ((date.getMonth() + 1) << 5) | date.getDate()
  return { time, day }
}

function u16(value) {
  const buffer = Buffer.alloc(2)
  buffer.writeUInt16LE(value)
  return buffer
}

function u32(value) {
  const buffer = Buffer.alloc(4)
  buffer.writeUInt32LE(value >>> 0)
  return buffer
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
    if (buffer.readUInt32LE(cursor) !== 0x02014b50) {
      throw new Error('Invalid ZIP central directory entry.')
    }
    const method = buffer.readUInt16LE(cursor + 10)
    const compressedSize = buffer.readUInt32LE(cursor + 20)
    const nameLength = buffer.readUInt16LE(cursor + 28)
    const extraLength = buffer.readUInt16LE(cursor + 30)
    const commentLength = buffer.readUInt16LE(cursor + 32)
    const localHeaderOffset = buffer.readUInt32LE(cursor + 42)
    const name = normalizeZipPath(buffer.subarray(cursor + 46, cursor + 46 + nameLength).toString('utf8'))
    entries.push({ name, method, compressedSize, localHeaderOffset })
    cursor += 46 + nameLength + extraLength + commentLength
  }
  return entries
}

function readZipEntry(buffer, entry) {
  const cursor = entry.localHeaderOffset
  if (buffer.readUInt32LE(cursor) !== 0x04034b50) {
    throw new Error(`Invalid ZIP local header for ${entry.name}.`)
  }
  const nameLength = buffer.readUInt16LE(cursor + 26)
  const extraLength = buffer.readUInt16LE(cursor + 28)
  const dataStart = cursor + 30 + nameLength + extraLength
  const compressed = buffer.subarray(dataStart, dataStart + entry.compressedSize)
  if (entry.method === 0) return compressed
  if (entry.method === 8) return inflateRawSync(compressed, { finishFlush: zlibConstants.Z_SYNC_FLUSH })
  throw new Error(`Unsupported ZIP compression method ${entry.method} for ${entry.name}.`)
}

function storedZipBuffer(entries) {
  const now = dosDateTime(new Date('2020-01-01T00:00:00Z'))
  const localParts = []
  const centralParts = []
  let offset = 0

  for (const entry of entries.sort((a, b) => a.name.localeCompare(b.name))) {
    const name = Buffer.from(normalizeZipPath(entry.name), 'utf8')
    const data = Buffer.isBuffer(entry.data) ? entry.data : Buffer.from(entry.data)
    const compressed = deflateRawSync(data, { level: 9 })
    const crc = crc32(data)
    const localHeader = Buffer.concat([
      u32(0x04034b50),
      u16(20),
      u16(0x0800),
      u16(8),
      u16(now.time),
      u16(now.day),
      u32(crc),
      u32(compressed.length),
      u32(data.length),
      u16(name.length),
      u16(0),
      name,
    ])
    localParts.push(localHeader, compressed)
    centralParts.push(Buffer.concat([
      u32(0x02014b50),
      u16(20),
      u16(20),
      u16(0x0800),
      u16(8),
      u16(now.time),
      u16(now.day),
      u32(crc),
      u32(compressed.length),
      u32(data.length),
      u16(name.length),
      u16(0),
      u16(0),
      u16(0),
      u16(0),
      u32(0),
      u32(offset),
      name,
    ]))
    offset += localHeader.length + compressed.length
  }

  const central = Buffer.concat(centralParts)
  const end = Buffer.concat([
    u32(0x06054b50),
    u16(0),
    u16(0),
    u16(entries.length),
    u16(entries.length),
    u32(central.length),
    u32(offset),
    u16(0),
  ])
  return Buffer.concat([...localParts, central, end])
}

function checksumRowsForZipEntries(entries) {
  return entries
    .map((entry) => ({
      name: normalizeZipPath(entry.name),
      data: Buffer.isBuffer(entry.data) ? entry.data : Buffer.from(entry.data),
    }))
    .filter((entry) => entry.name !== '.echo/checksums.sha256')
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((entry) => `${hashBytes(entry.data).sha256}  ${entry.name}`)
    .join('\n') + '\n'
}

function fileExistsOrVirtual(file) {
  return existsSync(file) || hasVirtualFile(file)
}

function artifactNameFor(moduleId, version, lane) {
  if (lane.artifactFamily === 'echo-addon') return `${moduleId}-${version}.echo-addon`
  if (lane.artifactFamily === 'neoforge') return `${moduleId}-${version}-neoforge.jar`
  if (lane.artifactFamily === 'standalone') return `${moduleId}-${version}-standalone.jar`
  throw new Error(`Unsupported artifact family ${lane.artifactFamily}`)
}

function artifactKey(moduleId, artifactFamily) {
  return `${String(moduleId).toLowerCase()}:${artifactFamily}`
}

async function loadModuleReleaseArtifacts() {
  const release = await readJsonIfExists(moduleReleasePath)
  moduleReleaseMetadata = release ?? null
  const artifacts = new Map()
  for (const moduleRecord of release?.modules ?? []) {
    const moduleId = String(moduleRecord.moduleId ?? '').toLowerCase()
    if (!moduleId) continue
    for (const artifact of moduleRecord.artifacts ?? []) {
      if (!artifact?.kind || !artifact?.filename) continue
      const downloadUrl = moduleDownloadBaseUrl
        ? `${moduleDownloadBaseUrl}/${artifact.filename}`
        : artifact.downloadUrl
      artifacts.set(artifactKey(moduleId, artifact.kind), {
        filename: artifact.filename,
        sha256: artifact.sha256,
        size: artifact.size,
        downloadUrl
      })
    }
  }
  return artifacts
}

function applyModuleReleaseMetadata(manifest) {
  const releaseId = String(moduleReleaseMetadata?.releaseId ?? '').trim()
  if (!releaseId) return
  manifest.moduleRelease = releaseId
  manifest.moduleReleaseId = releaseId
  const generatedAt = String(moduleReleaseMetadata?.generatedAt ?? '').trim()
  const date = generatedAt ? generatedAt.slice(0, 10) : ''
  const commitSha = String(moduleReleaseMetadata?.commitSha ?? '').trim()
  const commitSuffix = commitSha ? ` (${commitSha})` : ''
  manifest.moduleSourceRevision = `ECHO-Modules ${releaseId}${commitSuffix}${date ? `, ${date}` : ''}`
}

function moduleReleaseArtifact(moduleId, lane) {
  return moduleReleaseArtifacts.get(artifactKey(moduleId, lane.artifactFamily)) ?? null
}

function siblingArtifactUrl(previousById, artifactName) {
  for (const previous of previousById.values()) {
    if (typeof previous?.url === 'string' && previous.url.includes('/releases/download/')) {
      return previous.url.replace(/\/[^/]+$/u, `/${artifactName}`)
    }
  }
  return null
}

function packIdFor(packKey, laneKey) {
  return `${packKey}-${lanes[laneKey].suffix}`
}

function repoNameFor(packKey, laneKey) {
  return `${packRepoNames[packKey]}-${lanes[laneKey].label}-Edition`
}

function selectedPackKeys(selections) {
  if (options.packKeys.length === 0) return new Set(Object.keys(selections.packs))
  const selected = new Set()
  for (const requested of options.packKeys) {
    for (const packKey of Object.keys(selections.packs)) {
      const ids = new Set([
        packKey,
        ...Object.keys(lanes).map((laneKey) => packIdFor(packKey, laneKey)),
      ])
      if (ids.has(requested)) selected.add(packKey)
    }
  }
  for (const requested of options.packKeys) {
    if (![...selected].some((packKey) => packKey === requested || Object.keys(lanes).some((laneKey) => packIdFor(packKey, laneKey) === requested))) {
      throw new Error(`Unknown pack filter: ${requested}`)
    }
  }
  return selected
}

function versionFor(moduleId, descriptors) {
  const descriptor = descriptors.get(moduleId)
  if (!descriptor) throw new Error(`No descriptor for ${moduleId}`)
  return String(descriptor.version)
}

function richRequirement(moduleId, lane, descriptors, previousById = new Map(), releaseDir = null) {
  const version = versionFor(moduleId, descriptors)
  const artifactName = artifactNameFor(moduleId, version, lane)
  const entry = {
    id: moduleId,
    moduleId,
    version,
    artifactFamily: lane.artifactFamily,
    assetName: artifactName,
    artifactName,
    path: `${lane.installDir}/${artifactName}`,
    required: true,
    side: 'both'
  }

  const artifactPath = releaseDir ? path.join(releaseDir, artifactName) : null
  const previous = previousById.get(moduleId)
  const releaseArtifact = moduleReleaseArtifact(moduleId, lane)
  if (releaseArtifact && releaseArtifact.filename === artifactName) {
    if (releaseArtifact.sha256) entry.sha256 = releaseArtifact.sha256
    if (Number.isFinite(releaseArtifact.size)) entry.size = releaseArtifact.size
  } else if (artifactPath && existsSync(artifactPath)) {
    const stat = statSync(artifactPath)
    entry.size = stat.size
  } else if (
    previous &&
    (previous.artifactName === artifactName || previous.assetName === artifactName || previous.path === entry.path)
  ) {
    if (previous.sha256) entry.sha256 = previous.sha256
    if (Number.isFinite(previous.size)) entry.size = previous.size
  }
  const previousUrl = previous?.url
  const releaseUrl = releaseArtifact?.downloadUrl
  const derivedUrl = siblingArtifactUrl(previousById, artifactName)
  if (releaseArtifact && releaseArtifact.filename === artifactName) {
    if (releaseUrl) {
      entry.url = releaseUrl
    } else {
      const previousSha = String(previous?.sha256 ?? '').toLowerCase()
      const releaseSha = String(releaseArtifact.sha256 ?? '').toLowerCase()
      const unchanged = previousSha && releaseSha && previousSha === releaseSha
      if (unchanged && previousUrl) {
        entry.url = previousUrl
      } else if (unchanged && derivedUrl) {
        entry.url = derivedUrl
      } else if (previousUrl || derivedUrl) {
        errors.push(`${moduleId} ${lane.label} ${artifactName} has regenerated hash metadata but no release download URL. Pass --module-release-tag <tag> or --module-download-base-url <url> before syncing pack manifests.`)
      }
    }
  } else if (previousUrl) entry.url = previousUrl
  else if (releaseUrl) entry.url = releaseUrl
  else if (derivedUrl) entry.url = derivedUrl
  return entry
}

async function decorateLocalArtifactHashes(requirements, releaseDir) {
  for (const requirement of requirements) {
    const artifactPath = path.join(releaseDir, requirement.assetName)
    if (!existsSync(artifactPath)) continue
    const digest = await fileDigest(artifactPath)
    requirement.sha256 = digest.sha256
    requirement.size = digest.size
  }
}

function simpleRequirement(moduleId, descriptors) {
  return {
    id: moduleId,
    version: versionFor(moduleId, descriptors)
  }
}

function catalogRequirement(moduleId, descriptors, previousById, preferRanges) {
  const version = versionFor(moduleId, descriptors)
  const previousVersion = previousById.get(moduleId)?.version
  if (typeof previousVersion === 'string' && previousVersion.startsWith('>=')) {
    return { id: moduleId, version: `>=${version}` }
  }
  if (previousVersion) return { id: moduleId, version }
  if (preferRanges && !moduleId.endsWith('protocol')) return { id: moduleId, version: `>=${version}` }
  return { id: moduleId, version }
}

function entriesByModuleId(entries = []) {
  return new Map(
    entries
      .map((entry) => [String(entry?.id ?? entry?.moduleId ?? '').toLowerCase(), entry])
      .filter(([id]) => id)
  )
}

function moduleIdsFromRequirements(entries = []) {
  return entries.map((entry) => String(entry?.id ?? entry?.moduleId ?? '').toLowerCase()).filter(Boolean)
}

function sameModuleIds(actual, expected) {
  if (actual.length !== expected.length) return false
  return actual.every((id, index) => id === expected[index])
}

async function collectDescriptors() {
  const descriptors = new Map()
  const addonsRoot = path.join(modulesRoot, 'addons')
  const dirs = await fs.readdir(addonsRoot, { withFileTypes: true })
  for (const dir of dirs) {
    if (!dir.isDirectory()) continue
    const descriptorPath = path.join(addonsRoot, dir.name, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (!existsSync(descriptorPath)) continue
    const descriptor = await readJson(descriptorPath)
    if (!descriptor.id) {
      errors.push(`${descriptorPath} is missing id`)
      continue
    }
    descriptors.set(String(descriptor.id).toLowerCase(), {
      ...descriptor,
      descriptorPath,
      moduleDir: path.join(addonsRoot, dir.name)
    })
  }
  return descriptors
}

function validateSelections(selectionSource, descriptors) {
  for (const [packKey, pack] of Object.entries(selectionSource.packs)) {
    const modules = pack.modules.map((moduleId) => moduleId.toLowerCase())
    const selected = new Set(modules)
    if (modules.length !== pack.expectedCount) {
      errors.push(`${packKey} expectedCount is ${pack.expectedCount}, but list has ${modules.length}`)
    }
    if (new Set(modules).size !== modules.length) {
      errors.push(`${packKey} has duplicate module IDs`)
    }
    for (const moduleId of modules) {
      if (!descriptors.has(moduleId)) errors.push(`${packKey} selects missing module descriptor ${moduleId}`)
    }
    for (const moduleId of foundationModules) {
      if (!selected.has(moduleId)) errors.push(`${packKey} is missing foundation module ${moduleId}`)
    }
    for (const [otherPack, rootModule] of Object.entries(packRootModules)) {
      if (otherPack !== packKey && selected.has(rootModule)) {
        errors.push(`${packKey} includes cross-pack root module ${rootModule}`)
      }
    }
    const ownRoot = packRootModules[packKey]
    if (ownRoot && !selected.has(ownRoot)) {
      errors.push(`${packKey} is missing its root module ${ownRoot}`)
    }
    for (const moduleId of modules) {
      const descriptor = descriptors.get(moduleId)
      if (!descriptor) continue
      for (const requiredId of descriptor.requires ?? []) {
        const normalizedRequiredId = String(requiredId).toLowerCase()
        if (normalizedRequiredId === 'echoaddonapi') {
          errors.push(`${packKey} selects ${moduleId}, but echoaddonapi is SDK/API-only and cannot be a runtime dependency`)
          continue
        }
        if (!selected.has(normalizedRequiredId)) {
          errors.push(`${packKey} selects ${moduleId}, but is missing required dependency ${normalizedRequiredId}`)
        }
      }
    }
    if (selected.has('echoaddonapi')) {
      errors.push(`${packKey} includes SDK/API-only echoaddonapi as a player-facing runtime module`)
    }
  }
}

async function walkFiles(root, predicate, found = []) {
  if (!existsSync(root)) return found
  const entries = await fs.readdir(root, { withFileTypes: true })
  for (const entry of entries) {
    const fullPath = path.join(root, entry.name)
    if (entry.isDirectory()) {
      if (entry.name === '.git' || entry.name === 'node_modules') continue
      await walkFiles(fullPath, predicate, found)
    } else if (predicate(fullPath)) {
      found.push(fullPath)
    }
  }
  return found
}

function updatePackManifestObject(manifest, selection, lane, descriptors, releaseDir = null) {
  const previousById = entriesByModuleId(manifest.moduleRequirements)
  const requirements = selection.map((moduleId) => richRequirement(moduleId, lane, descriptors, previousById, releaseDir))
  manifest.moduleArtifactFamily = lane.artifactFamily
  manifest.moduleArtifactPattern = lane.artifactPattern
  manifest.moduleSourcePattern = manifest.moduleSourcePattern ?? lane.sourcePattern
  manifest.moduleRequirements = requirements
  if ('runtimeTarget' in manifest || lane.runtimeTarget) manifest.runtimeTarget = lane.runtimeTarget
  if ('loader' in manifest || lane.loader) manifest.loader = lane.loader
  applyModuleReleaseMetadata(manifest)
  return requirements
}

function updatePackSnapshotObject(manifest, selection, lane, descriptors, releaseDir) {
  const previousById = entriesByModuleId(manifest.moduleRequirements)
  const requirements = selection.map((moduleId) => richRequirement(moduleId, lane, descriptors, previousById, releaseDir))
  manifest.moduleArtifactFamily = lane.artifactFamily
  manifest.moduleRequirements = requirements
  if (Array.isArray(manifest.modules)) manifest.modules = [...selection]
  if (Array.isArray(manifest.requiredArtifacts)) {
    const nonModuleArtifacts = manifest.requiredArtifacts.filter((artifact) => artifact?.kind && artifact.kind !== 'module')
    manifest.requiredArtifacts = [
      ...selection.map((moduleId) => ({
        id: moduleId,
        kind: 'module',
        version: versionFor(moduleId, descriptors),
        artifactFamily: lane.artifactFamily
      })),
      ...nonModuleArtifacts
    ]
  }
  if (Array.isArray(manifest.files)) {
    const selected = new Set(selection)
    const previousFilesById = entriesByModuleId(manifest.files)
    const nonModuleFiles = manifest.files.filter((file) => {
      const moduleId = String(file?.moduleId ?? file?.id ?? '').toLowerCase()
      return !moduleId
    })
    manifest.files = [
      ...requirements.map((requirement) => {
        const moduleId = String(requirement.moduleId ?? requirement.id ?? '').toLowerCase()
        const file = previousFilesById.get(moduleId) ?? {}
        const next = {
          ...file,
          id: file.id ?? moduleId,
          moduleId,
          path: requirement.path,
          assetName: requirement.assetName,
          artifactName: requirement.artifactName,
          artifactFamily: lane.artifactFamily,
          required: true,
          side: file.side ?? requirement.side ?? 'both'
        }
        if (requirement.url) next.url = requirement.url
        if (requirement.sha256) next.sha256 = requirement.sha256
        else delete next.sha256
        if (Number.isFinite(requirement.size)) next.size = requirement.size
        else delete next.size
        return next
      }),
      ...nonModuleFiles
    ]
    for (const requirement of requirements) {
      const moduleId = String(requirement.moduleId ?? requirement.id ?? '').toLowerCase()
      const expectedPath = normalizeZipPath(requirement.path)
      const matches = manifest.files.filter((file) => {
        const fileModuleId = String(file?.moduleId ?? file?.id ?? '').toLowerCase()
        return fileModuleId === moduleId && normalizeZipPath(file?.path) === expectedPath
      })
      if (selected.has(moduleId) && matches.length !== 1) {
        errors.push(`${manifest.packId ?? manifest.id ?? 'pack manifest'} files[] must contain exactly one ${moduleId} entry at ${expectedPath}; found ${matches.length}.`)
      }
    }
  }
  if ('moduleRequirementCount' in manifest) manifest.moduleRequirementCount = selection.length
  applyModuleReleaseMetadata(manifest)
  return requirements
}

async function updateReleaseManifestTemplate(repoRoot, packKey, laneKey, selection, descriptors) {
  const lane = lanes[laneKey]
  const file = path.join(repoRoot, 'release-manifest.template.json')
  let manifest = await readJsonIfExists(file)
  if (!manifest) {
    const packId = packIdFor(packKey, laneKey)
    manifest = {
      packId,
      displayName: `${selection.displayName} ${lane.label} Edition`,
      sourceRepo: `knoxhack/${repoNameFor(packKey, laneKey)}`,
      launcherFeed: 'github-releases',
      runtimeTarget: lane.runtimeTarget,
      loader: lane.loader,
      moduleArtifactFamily: lane.artifactFamily,
      moduleArtifactPattern: lane.artifactPattern,
      moduleSourcePattern: lane.sourcePattern,
      moduleRequirements: [],
      requiredModuleDescriptors: laneKey === 'neoforge'
        ? ['META-INF/echo.mod.json', 'META-INF/neoforge.mods.toml']
        : ['META-INF/echo.mod.json'],
      artifacts: []
    }
  }
  updatePackManifestObject(manifest, selection.modules, lane, descriptors)
  updatePackSnapshotObject(manifest, selection.modules, lane, descriptors, null)
  await refreshArtifactMetadata(manifest, repoRoot)
  await writeJsonIfChanged(file, manifest)
}

async function refreshArtifactMetadata(manifest, repoRoot) {
  if (!Array.isArray(manifest.artifacts)) return
  for (const artifact of manifest.artifacts) {
    const candidates = []
    if (artifact.path) candidates.push(path.join(repoRoot, artifact.path))
    if (artifact.file) candidates.push(path.join(repoRoot, artifact.file))
    const existing = candidates.find((candidate) => fileExistsOrVirtual(candidate))
    if (!existing) continue
    const digest = await fileDigest(existing)
    artifact.size = digest.size
    artifact.sha256 = digest.sha256
  }
}

async function updatePackSnapshots(repoRoot, packKey, laneKey, selection, descriptors) {
  const snapshotRoots = [
    path.join(repoRoot, 'release-assets'),
    path.join(repoRoot, 'dist')
  ]
  const packFiles = []
  for (const snapshotRoot of snapshotRoots) {
    packFiles.push(...await walkFiles(snapshotRoot, (file) => file.endsWith('.pack.json')))
  }
  const changedDirs = new Set()
  for (const file of packFiles) {
    const manifest = await readJson(file)
    const requirements = updatePackSnapshotObject(manifest, selection.modules, lanes[laneKey], descriptors, path.dirname(file))
    await decorateLocalArtifactHashes(requirements, path.dirname(file))
    await materializePackArchive(manifest, requirements, lanes[laneKey], path.dirname(file))
    await writeJsonIfChanged(file, manifest)
    changedDirs.add(path.dirname(file))
  }
  for (const dir of changedDirs) {
    await refreshReleaseSidecars(dir, selection.modules.length)
  }
}

async function existingZipEntries(zipPath, lane) {
  if (!fileExistsOrVirtual(zipPath)) return []
  const bytes = hasVirtualFile(zipPath) ? getVirtualFile(zipPath) : await fs.readFile(zipPath)
  const buffer = Buffer.isBuffer(bytes) ? bytes : Buffer.from(bytes)
  const installPrefix = `${lane.installDir}/`
  const entries = []
  for (const entry of readZipEntries(buffer)) {
    if (!entry.name || entry.name.endsWith('/')) continue
    if (entry.name.startsWith(installPrefix)) continue
    if (entry.name === '.echo/pack-manifest.json' || entry.name === '.echo/checksums.sha256') continue
    entries.push({ name: entry.name, data: readZipEntry(buffer, entry) })
  }
  return entries
}

async function materializePackArchive(manifest, requirements, lane, releaseDir) {
  const zipName = manifest.artifactName ?? `${manifest.pack ?? manifest.id ?? path.basename(releaseDir)}-${manifest.version ?? '0.1.0'}.zip`
  const zipPath = path.join(releaseDir, zipName)
  const entries = await existingZipEntries(zipPath, lane)
  const used = new Set(entries.map((entry) => normalizeZipPath(entry.name)))

  for (const requirement of requirements) {
    const moduleId = String(requirement.id ?? requirement.moduleId ?? '').toLowerCase()
    const artifactName = requirement.assetName ?? requirement.artifactName
    if (!moduleId || !artifactName) {
      errors.push(`${path.relative(workspaceRoot, zipPath)} has a module requirement without id or artifactName.`)
      continue
    }
    const artifactPath = path.join(modulesRoot, 'dist', 'echo-module-release', moduleId, artifactName)
    if (!existsSync(artifactPath)) {
      errors.push(`${path.relative(workspaceRoot, zipPath)} cannot materialize missing module artifact ${path.relative(workspaceRoot, artifactPath)}.`)
      continue
    }
    const data = await fs.readFile(artifactPath)
    const digest = hashBytes(data)
    requirement.sha256 = digest.sha256
    requirement.size = digest.size
    const archivePath = normalizeZipPath(requirement.path)
    if (used.has(archivePath)) {
      errors.push(`${path.relative(workspaceRoot, zipPath)} has duplicate archive path ${archivePath}.`)
      continue
    }
    used.add(archivePath)
    entries.push({ name: archivePath, data })
  }

  const embeddedManifest = JSON.parse(formatJson(manifest))
  embeddedManifest.artifactSha256 = ''
  embeddedManifest.artifactSize = 0
  entries.push({ name: '.echo/pack-manifest.json', data: Buffer.from(formatJson(embeddedManifest), 'utf8') })
  entries.push({ name: '.echo/checksums.sha256', data: Buffer.from(checksumRowsForZipEntries(entries), 'utf8') })

  const zipBytes = storedZipBuffer(entries)
  const zipDigest = hashBytes(zipBytes)
  manifest.artifactMode = manifest.artifactMode ?? 'zip'
  manifest.artifactName = zipName
  manifest.artifactSha256 = zipDigest.sha256
  manifest.artifactSize = zipDigest.size
  await writeBytesIfChanged(zipPath, zipBytes)
  return zipDigest
}

async function refreshReleaseSidecars(releaseDir, moduleRequirementCount) {
  const packFiles = await walkFiles(releaseDir, (file) => file.endsWith('.pack.json'))
  const packDigests = new Map()
  const moduleArtifactDigests = new Map()
  const moduleArtifactRows = []
  const seenModuleArtifacts = new Set()
  for (const packFile of packFiles) {
    packDigests.set(path.basename(packFile), await fileDigest(packFile))
    const packManifest = parseJsonText(await readTextMaybeVirtual(packFile))
    for (const requirement of packManifest.moduleRequirements ?? []) {
      const name = String(requirement?.assetName ?? requirement?.artifactName ?? '').toLowerCase()
      if (!name || !requirement.sha256) continue
      const row = {
        name: requirement.assetName ?? requirement.artifactName,
        role: 'pack-file',
        sha256: requirement.sha256,
        size: Number(requirement.size ?? 0),
        path: requirement.path
      }
      moduleArtifactDigests.set(name, row)
      if (!seenModuleArtifacts.has(name)) {
        seenModuleArtifacts.add(name)
        moduleArtifactRows.push(row)
      }
    }
  }
  const zipFiles = await walkFiles(releaseDir, (file) => file.endsWith('.zip'))
  const zipDigests = new Map()
  for (const zipFile of zipFiles) {
    zipDigests.set(path.basename(zipFile), await fileDigest(zipFile))
  }

  const echoReleasePath = path.join(releaseDir, 'echo-release.json')
  if (existsSync(echoReleasePath) || hasVirtualFile(echoReleasePath)) {
    const echoRelease = parseJsonText(await readTextMaybeVirtual(echoReleasePath))
    const manifestName = echoRelease.manifestAsset ?? [...packDigests.keys()][0]
    const manifestDigest = packDigests.get(manifestName)
    const artifactName = echoRelease.artifactAsset ?? [...zipDigests.keys()][0]
    const artifactDigest = zipDigests.get(artifactName)
    if (manifestDigest) {
      echoRelease.manifestAsset = manifestName
      echoRelease.manifestSha256 = manifestDigest.sha256
      if ('manifestSize' in echoRelease) echoRelease.manifestSize = manifestDigest.size
    }
    if (artifactDigest) {
      echoRelease.artifactAsset = artifactName
      echoRelease.artifactSha256 = artifactDigest.sha256
      if ('artifactSize' in echoRelease) echoRelease.artifactSize = artifactDigest.size
    }
    if ('moduleRequirementCount' in echoRelease) echoRelease.moduleRequirementCount = moduleRequirementCount
    if (Array.isArray(echoRelease.assets)) {
      const nonModuleAssets = echoRelease.assets.filter((asset) => asset?.role !== 'pack-file')
      for (const asset of nonModuleAssets) {
        const moduleDigest = moduleArtifactDigests.get(String(asset.name ?? '').toLowerCase())
        const digest = moduleDigest ?? packDigests.get(asset.name) ?? zipDigests.get(asset.name)
        if (digest) {
          asset.sha256 = digest.sha256
          asset.size = digest.size
          if (moduleDigest?.path && asset.role === 'pack-file') asset.path = moduleDigest.path
        }
      }
      echoRelease.assets = [...nonModuleAssets, ...moduleArtifactRows.map((row) => ({ ...row }))]
    }
    if (echoRelease.artifacts?.manifest?.file) {
      const digest = packDigests.get(echoRelease.artifacts.manifest.file)
      if (digest) {
        echoRelease.artifacts.manifest.sha256 = digest.sha256
        echoRelease.artifacts.manifest.size = digest.size
      }
    }
    if (echoRelease.artifacts?.pack?.file) {
      const digest = zipDigests.get(echoRelease.artifacts.pack.file)
      if (digest) {
        echoRelease.artifacts.pack.sha256 = digest.sha256
        echoRelease.artifacts.pack.size = digest.size
      }
    }
    await writeJsonIfChanged(echoReleasePath, echoRelease)
  }

  const checksumsPath = path.join(releaseDir, 'checksums.txt')
  if (existsSync(checksumsPath) || hasVirtualFile(checksumsPath)) {
    const checksumTargets = new Map([...zipDigests, ...packDigests])
    if (fileExistsOrVirtual(echoReleasePath)) {
      checksumTargets.set('echo-release.json', await fileDigest(echoReleasePath))
    }
    const nextLines = [...checksumTargets.entries()]
      .sort(([left], [right]) => {
        const rank = (name) => name === 'echo-release.json' ? 0 : name.endsWith('.zip') ? 1 : name.endsWith('.pack.json') ? 2 : 3
        return rank(left) - rank(right) || left.localeCompare(right)
      })
      .map(([name, digest]) => `${digest.sha256}  ${name}`)
    await writeTextIfChanged(checksumsPath, `${nextLines.join('\n')}\n`)
  }

  const releaseAuditPath = path.join(releaseDir, 'release-audit.json')
  if (existsSync(releaseAuditPath) || hasVirtualFile(releaseAuditPath)) {
    const releaseAudit = parseJsonText(await readTextMaybeVirtual(releaseAuditPath))
    const assetDigests = new Map([...zipDigests, ...packDigests])
    if (fileExistsOrVirtual(echoReleasePath)) {
      assetDigests.set('echo-release.json', await fileDigest(echoReleasePath))
    }
    if (fileExistsOrVirtual(checksumsPath)) {
      assetDigests.set('checksums.txt', await fileDigest(checksumsPath))
    }
    if (Array.isArray(releaseAudit.assets)) {
      for (const asset of releaseAudit.assets) {
        const digest = moduleArtifactDigests.get(String(asset.name ?? '').toLowerCase()) ?? assetDigests.get(asset.name)
        if (!digest) continue
        asset.sha256 = digest.sha256
        asset.size = digest.size
      }
    }
    if (releaseAudit.zip?.file) {
      const digest = zipDigests.get(releaseAudit.zip.file)
      if (digest) releaseAudit.zip.sha256 = digest.sha256
    }
    if (Array.isArray(releaseAudit.checksumEntries)) {
      const nonModuleEntries = releaseAudit.checksumEntries.filter((entry) => entry?.coveredBy !== 'zip-entry')
      for (const entry of nonModuleEntries) {
        const digest = moduleArtifactDigests.get(String(entry.file ?? '').toLowerCase()) ?? assetDigests.get(entry.file)
        if (!digest) continue
        entry.expectedSha256 = digest.sha256
        entry.actualSha256 = digest.sha256
        entry.size = digest.size
        entry.ok = true
      }
      const moduleEntries = moduleArtifactRows.map((row) => ({
        file: row.name,
        expectedSha256: row.sha256,
        actualSha256: row.sha256,
        coveredBy: 'zip-entry',
        matchedPath: row.path,
        size: row.size,
        ok: true
      }))
      releaseAudit.checksumEntries = [...nonModuleEntries, ...moduleEntries]
      releaseAudit.missingChecksumEntries = []
      releaseAudit.mismatchedChecksumEntries = []
      if (releaseAudit.checksumCoverage) {
        releaseAudit.checksumCoverage.total = releaseAudit.checksumEntries.length
        releaseAudit.checksumCoverage.missing = releaseAudit.missingChecksumEntries?.length ?? 0
        releaseAudit.checksumCoverage.mismatched = releaseAudit.mismatchedChecksumEntries?.length ?? 0
      }
    }
    await writeJsonIfChanged(releaseAuditPath, releaseAudit)
  }
}

async function updateReleaseIndexPack(packKey, laneKey, selection, descriptors) {
  const file = path.join(releaseIndexRoot, 'packs', `${packIdFor(packKey, laneKey)}.json`)
  const manifest = await readJsonIfExists(file)
  if (!manifest) {
    errors.push(`Missing Release Index pack entry ${file}`)
    return
  }
  const previousById = entriesByModuleId(manifest.moduleRequirements)
  const preferRanges = (manifest.moduleRequirements ?? []).some((requirement) => String(requirement?.version ?? '').startsWith('>='))
  manifest.moduleArtifactFamily = lanes[laneKey].artifactFamily
  manifest.moduleArtifactPattern = lanes[laneKey].artifactPattern
  manifest.moduleRequirements = selection.modules.map((moduleId) =>
    catalogRequirement(moduleId, descriptors, previousById, preferRanges)
  )
  await writeJsonIfChanged(file, manifest)
}

async function assertLaneParity(packKey, expectedModules) {
  for (const laneKey of Object.keys(lanes)) {
    const repoRoot = path.join(workspaceRoot, repoNameFor(packKey, laneKey))
    const template = await readJsonIfExists(path.join(repoRoot, 'release-manifest.template.json'))
    if (template) {
      const actual = moduleIdsFromRequirements(template.moduleRequirements)
      if (!sameModuleIds(actual, expectedModules)) {
        errors.push(`${repoNameFor(packKey, laneKey)} release-manifest.template.json moduleRequirements differ from ${packKey}`)
      }
    }
    const packFiles = await walkFiles(path.join(repoRoot, 'release-assets'), (file) => file.endsWith('.pack.json'))
    for (const packFile of packFiles) {
      const manifest = parseJsonText(await readTextMaybeVirtual(packFile))
      const actual = moduleIdsFromRequirements(manifest.moduleRequirements)
      if (!sameModuleIds(actual, expectedModules)) {
        errors.push(`${packFile} moduleRequirements differ from ${packKey}`)
      }
    }
    const indexEntry = await readJsonIfExists(path.join(releaseIndexRoot, 'packs', `${packIdFor(packKey, laneKey)}.json`))
    if (indexEntry) {
      const actual = moduleIdsFromRequirements(indexEntry.moduleRequirements)
      if (!sameModuleIds(actual, expectedModules)) {
        errors.push(`Release Index ${packIdFor(packKey, laneKey)} moduleRequirements differ from ${packKey}`)
      }
    }
  }
}

async function main() {
  const selections = await readJson(selectionPath)
  const descriptors = await collectDescriptors()
  moduleReleaseArtifacts = await loadModuleReleaseArtifacts()
  validateSelections(selections, descriptors)
  const packFilters = selectedPackKeys(selections)
  if (errors.length > 0) {
    throw new Error(`Selection validation failed:\n${errors.map((error) => `- ${error}`).join('\n')}`)
  }

  for (const [packKey, selection] of Object.entries(selections.packs)) {
    if (!packFilters.has(packKey)) continue
    for (const laneKey of Object.keys(lanes)) {
      const repoRoot = path.join(workspaceRoot, repoNameFor(packKey, laneKey))
      if (!existsSync(repoRoot)) {
        errors.push(`Missing pack repo ${repoRoot}`)
        continue
      }
      await updatePackSnapshots(repoRoot, packKey, laneKey, selection, descriptors)
      await updateReleaseManifestTemplate(repoRoot, packKey, laneKey, selection, descriptors)
      await updateReleaseIndexPack(packKey, laneKey, selection, descriptors)
    }
    await assertLaneParity(packKey, selection.modules.map((moduleId) => moduleId.toLowerCase()))
  }

  if (errors.length > 0) {
    throw new Error(`Sync validation failed:\n${errors.map((error) => `- ${error}`).join('\n')}`)
  }

  const uniqueChangedFiles = [...new Set(changedFiles)].sort()
  if (checkMode && uniqueChangedFiles.length > 0) {
    console.error('Official pack module selections are out of sync:')
    for (const file of uniqueChangedFiles) console.error(`- ${path.relative(workspaceRoot, file)}`)
    process.exitCode = 1
    return
  }

  if (writeMode) {
    if (uniqueChangedFiles.length === 0) {
      console.log('Official pack module selections already synchronized.')
    } else {
      console.log(`Synchronized ${uniqueChangedFiles.length} files:`)
      for (const file of uniqueChangedFiles) console.log(`- ${path.relative(workspaceRoot, file)}`)
    }
    return
  }

  console.log('Official pack module selections are synchronized.')
}

main().catch((error) => {
  console.error(error.message)
  process.exitCode = 1
})

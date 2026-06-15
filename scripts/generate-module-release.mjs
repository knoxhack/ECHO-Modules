import { createHash } from 'node:crypto'
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { constants as zlibConstants, deflateRawSync, inflateRawSync } from 'node:zlib'
import { generateContentGraph } from './generate-content-graph.mjs'

const DEFAULT_OUT_DIR = 'dist/echo-module-release'
const MODULE_RELEASE_SCHEMA_VERSION = 'echo.module.release.v1'
const DESCRIPTOR_PATH = 'src/main/resources/META-INF/echo.mod.json'
const NEOFORGE_TOML_PATHS = [
  'src/main/resources/META-INF/neoforge.mods.toml',
  'src/main/templates/META-INF/neoforge.mods.toml',
]
const TEMPLATE_DEFAULTS = {
  minecraft_version: '26.1.2',
  minecraft_version_range: '[26.1.2,26.2)',
  minecraftVersion: '26.1.2',
  minecraftVersionRange: '[26.1.2,26.2)',
  neo_version: '26.1.2.29-beta',
  neo_version_range: '[26.1.2.29-beta,)',
  neoForgeVersion: '26.1.2.29-beta',
  neoForgeVersionRange: '[26.1.2.29-beta,)',
  loader_version_range: '[4,)',
  loaderVersionRange: '[4,)',
  mod_authors: 'KnoxHack',
  mod_description: 'ECHO first-party module.',
}

const crcTable = new Uint32Array(256)
for (let i = 0; i < 256; i += 1) {
  let value = i
  for (let bit = 0; bit < 8; bit += 1) {
    value = (value & 1) ? (0xedb88320 ^ (value >>> 1)) : (value >>> 1)
  }
  crcTable[i] = value >>> 0
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

function normalizeZipPath(value) {
  return value.replace(/\\/g, '/').replace(/^\/+/, '')
}

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

async function readProperties(filePath) {
  if (!(await fileExists(filePath))) return {}
  const properties = {}
  for (const line of (await fs.readFile(filePath, 'utf8')).split(/\r?\n/u)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#') || trimmed.startsWith('!')) continue
    const match = trimmed.match(/^([^:=\s]+)\s*[:=]\s*(.*)$/u)
    if (match) properties[match[1]] = match[2]
  }
  return properties
}

async function moduleTemplateProperties(moduleDir, descriptor) {
  const moduleProperties = await readProperties(path.join(moduleDir, 'gradle.properties'))
  const merged = {
    ...TEMPLATE_DEFAULTS,
    ...moduleProperties,
    mod_id: moduleProperties.mod_id ?? descriptor.id ?? path.basename(moduleDir),
    mod_name: moduleProperties.mod_name ?? descriptor.name ?? descriptor.id ?? path.basename(moduleDir),
    mod_license: moduleProperties.mod_license ?? 'All Rights Reserved',
    mod_version: moduleProperties.mod_version ?? descriptor.version,
    mod_authors: moduleProperties.mod_authors ?? descriptor.authors ?? descriptor.author ?? TEMPLATE_DEFAULTS.mod_authors,
    mod_description: moduleProperties.mod_description ?? descriptor.description ?? descriptor.name ?? descriptor.id ?? TEMPLATE_DEFAULTS.mod_description,
  }
  return {
    ...merged,
    minecraft_version: TEMPLATE_DEFAULTS.minecraft_version,
    minecraft_version_range: TEMPLATE_DEFAULTS.minecraft_version_range,
    minecraftVersion: TEMPLATE_DEFAULTS.minecraftVersion,
    minecraftVersionRange: TEMPLATE_DEFAULTS.minecraftVersionRange,
    loader_version_range: TEMPLATE_DEFAULTS.loader_version_range,
    loaderVersionRange: TEMPLATE_DEFAULTS.loaderVersionRange,
  }
}

function renderTemplateText(text, properties, sourceLabel) {
  const rendered = String(text).replace(/\$\{([^}]+)\}/gu, (match, key) => {
    if (Object.hasOwn(properties, key)) return String(properties[key])
    throw new Error(`${sourceLabel}: missing template property ${key}`)
  })
  if (rendered.includes('${')) {
    throw new Error(`${sourceLabel}: unresolved template placeholder remains after rendering`)
  }
  return rendered
}

async function sha256File(filePath) {
  return createHash('sha256').update(await fs.readFile(filePath)).digest('hex')
}

function sha256Buffer(value) {
  return createHash('sha256').update(Buffer.isBuffer(value) ? value : Buffer.from(value)).digest('hex')
}

function normalizeDownloadBaseUrl(value) {
  if (!value) return null
  const normalized = String(value).trim().replace(/\/+$/u, '')
  if (!/^https?:\/\/[^/]+/u.test(normalized)) {
    throw new Error(`Invalid download base URL: ${value}`)
  }
  return normalized
}

function applyArtifactDownloadUrls(modules, downloadBaseUrl) {
  if (!downloadBaseUrl) return modules
  for (const moduleRecord of modules) {
    for (const artifact of moduleRecord.artifacts ?? []) {
      artifact.downloadUrl = `${downloadBaseUrl}/${artifact.filename}`
    }
  }
  return modules
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
    const uncompressedSize = buffer.readUInt32LE(cursor + 24)
    const nameLength = buffer.readUInt16LE(cursor + 28)
    const extraLength = buffer.readUInt16LE(cursor + 30)
    const commentLength = buffer.readUInt16LE(cursor + 32)
    const localHeaderOffset = buffer.readUInt32LE(cursor + 42)
    const name = normalizeZipPath(buffer.subarray(cursor + 46, cursor + 46 + nameLength).toString('utf8'))
    entries.push({ name, method, compressedSize, uncompressedSize, localHeaderOffset })
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

async function copyJarWithOverlaysAndRecord(sourcePath, outputPath, kind, contains, overlays) {
  const sourceBuffer = await fs.readFile(sourcePath)
  const files = new Map()
  for (const entry of readZipEntries(sourceBuffer)) {
    if (!entry.name.endsWith('/')) {
      files.set(entry.name, readZipEntry(sourceBuffer, entry))
    }
  }
  for (const overlay of overlays) {
    files.set(normalizeZipPath(overlay.name), Buffer.isBuffer(overlay.data) ? overlay.data : Buffer.from(overlay.data))
  }

  await writeStoredZip(
    [...files.entries()].map(([name, data]) => ({ name, data })),
    outputPath,
  )
  const stat = await fs.stat(outputPath)
  return {
    kind,
    filename: path.basename(outputPath),
    sha256: await sha256File(outputPath),
    size: stat.size,
    downloadUrl: '',
    runtimeTarget: kind === 'echo-addon' ? 'echo-native' : kind,
    buildMode: 'compiled-runtime',
    contains,
  }
}

async function listFiles(root, base = root) {
  if (!(await fileExists(root))) return []
  const entries = await fs.readdir(root, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const absolute = path.join(root, entry.name)
    if (entry.isDirectory()) {
      files.push(...await listFiles(absolute, base))
    } else if (entry.isFile()) {
      files.push({
        absolute,
        archivePath: normalizeZipPath(path.relative(base, absolute)),
      })
    }
  }
  return files
}

async function collectContentGraphEntries(graphDir) {
  const entries = []
  for (const file of await listFiles(graphDir, graphDir)) {
    entries.push({
      name: `.echo/content-graph/${file.archivePath}`,
      data: await fs.readFile(file.absolute),
    })
  }
  return entries
}

async function refreshArchiveChecksums(archivePath) {
  const buffer = await fs.readFile(archivePath)
  const entries = readZipEntries(buffer)
  const retained = []
  for (const entry of entries) {
    if (entry.name === 'checksums.sha256' || entry.name === 'checksums.txt') continue
    retained.push({ name: entry.name, data: readZipEntry(buffer, entry) })
  }
  const rows = retained
    .map((entry) => ({ name: normalizeZipPath(entry.name), data: entry.data }))
    .filter((entry) => entry.name !== 'checksums.sha256' && entry.name !== 'checksums.txt')
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((entry) => `${sha256Buffer(entry.data)}  ${entry.name}`)
    .join('\n') + '\n'
  await copyJarWithOverlaysAndRecord(
    archivePath,
    archivePath,
    'echo-addon',
    [],
    [{ name: 'checksums.sha256', data: rows }],
  )
}

async function embedContentGraphIntoModuleArtifacts({ moduleOutDir, moduleId, version, artifacts }) {
  const graphDir = path.join(moduleOutDir, version, '.echo', 'content-graph')
  if (!(await fileExists(graphDir))) return
  const graphEntries = await collectContentGraphEntries(graphDir)
  if (graphEntries.length === 0) return
  const graphPaths = graphEntries.map((entry) => entry.name)

  const artifactKindsToEmbed = ['echo-addon', 'neoforge', 'standalone']
  for (const artifact of artifacts) {
    if (!artifactKindsToEmbed.includes(artifact.kind)) continue
    const artifactPath = path.join(moduleOutDir, artifact.filename)
    if (!(await fileExists(artifactPath))) continue
    await copyJarWithOverlaysAndRecord(
      artifactPath,
      artifactPath,
      artifact.kind,
      [...(artifact.contains || []), ...graphPaths],
      graphEntries,
    )
    if (artifact.kind === 'echo-addon') {
      await refreshArchiveChecksums(artifactPath)
    }
    const stat = await fs.stat(artifactPath)
    artifact.sha256 = await sha256File(artifactPath)
    artifact.size = stat.size
    artifact.contains = [...new Set([...(artifact.contains || []), ...graphPaths])]
  }
}

async function releaseChecksumRows(outputRoot) {
  const files = (await listFiles(outputRoot, outputRoot))
    .filter((file) => file.archivePath !== 'checksums.sha256' && file.archivePath !== 'checksums.txt')
    .sort((a, b) => a.archivePath.localeCompare(b.archivePath))
  const rows = []
  for (const file of files) {
    rows.push(`${await sha256File(file.absolute)}  ${file.archivePath}`)
  }
  return rows.join('\n') + '\n'
}

async function writeStoredZip(entries, outputPath) {
  const now = dosDateTime()
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

  await fs.mkdir(path.dirname(outputPath), { recursive: true })
  await fs.writeFile(outputPath, Buffer.concat([...localParts, central, end]))
}

function checksumRowsForZipEntries(entries) {
  return entries
    .map((entry) => ({
      name: normalizeZipPath(entry.name),
      data: Buffer.isBuffer(entry.data) ? entry.data : Buffer.from(entry.data),
    }))
    .filter((entry) => entry.name !== 'checksums.sha256' && entry.name !== 'checksums.txt')
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((entry) => `${sha256Buffer(entry.data)}  ${entry.name}`)
    .join('\n') + '\n'
}

async function findRuntimeJar(moduleDir, moduleId, version, family) {
  const libsDir = path.join(moduleDir, 'build', 'libs')
  if (!(await fileExists(libsDir))) return null
  const jars = (await fs.readdir(libsDir))
    .filter((name) => name.endsWith('.jar'))
    .filter((name) => !name.includes('-sources') && !name.includes('-javadoc'))
    .sort()

  const preferred = [
    `${moduleId}-${version}-${family}.jar`,
    `${moduleId}-${version}.jar`,
    `${moduleId}.jar`,
  ]
  const match = preferred.find((name) => jars.includes(name)) ?? jars[0]
  return match ? path.join(libsDir, match) : null
}

async function readNeoForgeToml(moduleDir, templateProperties) {
  for (const relative of NEOFORGE_TOML_PATHS) {
    const absolute = path.join(moduleDir, relative)
    if (await fileExists(absolute)) {
      const text = await fs.readFile(absolute, 'utf8')
      return {
        absolute,
        relative,
        data: Buffer.from(renderTemplateText(text, templateProperties, `${path.basename(moduleDir)}:${relative}`)),
      }
    }
  }
  return null
}

async function buildSourcesJar(moduleDir, outputPath) {
  const sourceRoots = [
    path.join(moduleDir, 'src', 'main', 'java'),
    path.join(moduleDir, 'src', 'main', 'kotlin'),
    path.join(moduleDir, 'src', 'main', 'resources'),
  ]
  const entries = []
  for (const sourceRoot of sourceRoots) {
    for (const file of await listFiles(sourceRoot, sourceRoot)) {
      entries.push({
        name: file.archivePath,
        data: await fs.readFile(file.absolute),
      })
    }
  }
  for (const extra of ['build.gradle', 'gradle.properties', 'README.md', 'LICENSE']) {
    const absolute = path.join(moduleDir, extra)
    if (await fileExists(absolute)) {
      entries.push({ name: extra, data: await fs.readFile(absolute) })
    }
  }
  await writeStoredZip(entries, outputPath)
}

async function sourceEntries(moduleDir, descriptorPath, neoForgeToml = null) {
  const entries = [
    { name: 'META-INF/echo.mod.json', data: await fs.readFile(descriptorPath) },
  ]
  if (neoForgeToml) {
    entries.push({ name: 'META-INF/neoforge.mods.toml', data: neoForgeToml.data })
  }
  const sourceRoots = [
    { root: path.join(moduleDir, 'src', 'main', 'java'), prefix: 'src/main/java' },
    { root: path.join(moduleDir, 'src', 'main', 'kotlin'), prefix: 'src/main/kotlin' },
    { root: path.join(moduleDir, 'src', 'main', 'resources'), prefix: 'src/main/resources' },
  ]
  for (const sourceRoot of sourceRoots) {
    for (const file of await listFiles(sourceRoot.root, sourceRoot.root)) {
      entries.push({
        name: normalizeZipPath(path.join(sourceRoot.prefix, file.archivePath)),
        data: await fs.readFile(file.absolute),
      })
    }
  }
  return entries
}

async function buildSourcePackagedRuntimeJar({ moduleDir, descriptorPath, neoForgeToml, outputPath, runtimeTarget }) {
  const entries = await sourceEntries(moduleDir, descriptorPath, runtimeTarget === 'neoforge' ? neoForgeToml : null)
  entries.push({
    name: 'META-INF/echo-artifact-build.json',
    data: `${JSON.stringify({
      schemaVersion: 1,
      runtimeTarget,
      buildMode: 'source-packaged',
      note: 'This artifact was packaged from checked-in module source/resources because no compiled runtime jar was present in build/libs.',
    }, null, 2)}\n`,
  })
  await writeStoredZip(entries, outputPath)
}

function packageDependencies(descriptor) {
  return (descriptor.requires ?? []).map((dependency) => {
    if (typeof dependency === 'string') return { id: dependency, version: '*' }
    return {
      ...dependency,
      id: dependency.id,
      version: dependency.version ?? '*',
    }
  }).filter((dependency) => dependency.id)
}

async function buildEchoAddonPackage({ moduleDir, moduleId, version, descriptor, descriptorPath, runtimeJarPath, outputPath, packageFromSource }) {
  const runtimeJarName = `${moduleId}-${version}-runtime.jar`
  const packageJson = {
    schemaVersion: 'echo.addon.package.v1',
    id: moduleId,
    version,
    publisher: {
      githubOwner: 'knoxhack',
      githubRepo: 'ECHO-Modules',
    },
    targets: ['native', 'neoforge', 'standalone'],
    dependencies: packageDependencies(descriptor),
    artifacts: {
      native: `${moduleId}-${version}.echo-addon`,
      neoforge: `${moduleId}-${version}-neoforge.jar`,
      standalone: `${moduleId}-${version}-standalone.jar`,
      sources: `${moduleId}-${version}-sources.jar`,
    },
    runtime: 'echo-native',
    descriptor: 'META-INF/echo.mod.json',
    runtimeJar: runtimeJarPath ? `lib/${runtimeJarName}` : null,
    buildMode: runtimeJarPath ? 'compiled-runtime' : 'source-packaged',
  }
  const entries = [
    { name: 'META-INF/echo.mod.json', data: await fs.readFile(descriptorPath) },
    { name: 'echo-addon-package.json', data: `${JSON.stringify(packageJson, null, 2)}\n` },
  ]
  if (runtimeJarPath) {
    entries.push({ name: `lib/${runtimeJarName}`, data: await fs.readFile(runtimeJarPath) })
  } else if (packageFromSource) {
    for (const entry of await sourceEntries(moduleDir, descriptorPath)) {
      if (entry.name !== 'META-INF/echo.mod.json') {
        entries.push(entry)
      }
    }
  }
  const readmePath = path.join(moduleDir, 'README.md')
  if (await fileExists(readmePath)) {
    entries.push({ name: 'README.md', data: await fs.readFile(readmePath) })
  }
  entries.push({ name: 'checksums.sha256', data: checksumRowsForZipEntries(entries) })
  await writeStoredZip(entries, outputPath)
  return packageJson
}

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

function releaseProvenance() {
  const repository = process.env.GITHUB_REPOSITORY || 'knoxhack/ECHO-Modules'
  return {
    sourceRepo: `https://github.com/${repository}`,
    commitSha: process.env.GITHUB_SHA || '',
    workflow: process.env.GITHUB_WORKFLOW || '',
    workflowRef: process.env.GITHUB_WORKFLOW_REF || '',
    runId: process.env.GITHUB_RUN_ID || '',
    runAttempt: process.env.GITHUB_RUN_ATTEMPT || '',
    refName: process.env.GITHUB_REF_NAME || '',
    eventName: process.env.GITHUB_EVENT_NAME || '',
    generatedBy: 'scripts/generate-module-release.mjs',
    attestation: {
      action: 'actions/attest@v4',
      subjectChecksums: 'checksums.sha256',
    },
  }
}

async function writeMetadataFiles({ moduleOutDir, descriptorPath, neoForgeToml, echoAddonPackage }) {
  await fs.mkdir(path.join(moduleOutDir, 'META-INF'), { recursive: true })
  await fs.copyFile(descriptorPath, path.join(moduleOutDir, 'META-INF', 'echo.mod.json'))
  if (neoForgeToml) {
    await fs.writeFile(path.join(moduleOutDir, 'META-INF', 'neoforge.mods.toml'), neoForgeToml.data)
  }
  if (echoAddonPackage) {
    await writeJson(path.join(moduleOutDir, 'echo-addon-package.json'), echoAddonPackage)
  }
}

async function generateModule({ moduleDir, outputRoot, allowMissingRuntime, packageFromSource }) {
  const descriptorPath = path.join(moduleDir, DESCRIPTOR_PATH)
  const descriptor = await readJson(descriptorPath)
  const moduleId = descriptor.id ?? path.basename(moduleDir)
  const version = descriptor.version
  if (!version) {
    throw new Error(`${moduleId}: descriptor is missing version`)
  }

  const moduleOutDir = path.join(outputRoot, moduleId)
  const artifacts = []
  const missing = []
  const templateProperties = await moduleTemplateProperties(moduleDir, descriptor)
  const neoForgeToml = await readNeoForgeToml(moduleDir, templateProperties)
  const runtimeJarPath = await findRuntimeJar(moduleDir, moduleId, version, 'runtime')

  if (neoForgeToml) {
    const sourceJar = await findRuntimeJar(moduleDir, moduleId, version, 'neoforge')
    if (!sourceJar && !allowMissingRuntime && !packageFromSource) {
      missing.push('neoforge runtime jar')
    } else if (sourceJar || packageFromSource) {
      const outputPath = path.join(moduleOutDir, `${moduleId}-${version}-neoforge.jar`)
      if (sourceJar) {
        artifacts.push(await copyJarWithOverlaysAndRecord(
          sourceJar,
          outputPath,
          'neoforge',
          ['META-INF/echo.mod.json', 'META-INF/neoforge.mods.toml'],
          [
            { name: 'META-INF/echo.mod.json', data: await fs.readFile(descriptorPath) },
            { name: 'META-INF/neoforge.mods.toml', data: neoForgeToml.data },
          ],
        ))
      } else {
        await buildSourcePackagedRuntimeJar({ moduleDir, descriptorPath, neoForgeToml, outputPath, runtimeTarget: 'neoforge' })
        const stat = await fs.stat(outputPath)
        artifacts.push({
          kind: 'neoforge',
          filename: path.basename(outputPath),
          sha256: await sha256File(outputPath),
          size: stat.size,
          downloadUrl: '',
          runtimeTarget: 'neoforge',
          buildMode: 'source-packaged',
          contains: ['META-INF/echo.mod.json', 'META-INF/neoforge.mods.toml'],
        })
      }
    }
  }

  if (descriptor.standalone !== false) {
    const sourceJar = await findRuntimeJar(moduleDir, moduleId, version, 'standalone')
    if (!sourceJar && !allowMissingRuntime && !packageFromSource) {
      missing.push('standalone runtime jar')
    } else if (sourceJar || packageFromSource) {
      const outputPath = path.join(moduleOutDir, `${moduleId}-${version}-standalone.jar`)
      if (sourceJar) {
        artifacts.push(await copyJarWithOverlaysAndRecord(
          sourceJar,
          outputPath,
          'standalone',
          ['META-INF/echo.mod.json'],
          [
            { name: 'META-INF/echo.mod.json', data: await fs.readFile(descriptorPath) },
          ],
        ))
      } else {
        await buildSourcePackagedRuntimeJar({ moduleDir, descriptorPath, neoForgeToml: null, outputPath, runtimeTarget: 'standalone' })
        const stat = await fs.stat(outputPath)
        artifacts.push({
          kind: 'standalone',
          filename: path.basename(outputPath),
          sha256: await sha256File(outputPath),
          size: stat.size,
          downloadUrl: '',
          runtimeTarget: 'standalone',
          buildMode: 'source-packaged',
          contains: ['META-INF/echo.mod.json'],
        })
      }
    }
  }

  let echoAddonPackage = null
  if (descriptor.access?.nativeEntrypoint || descriptor.standalone !== false) {
    if (!runtimeJarPath && !allowMissingRuntime && !packageFromSource) {
      missing.push('echo-addon runtime jar')
    } else {
      const echoAddonPath = path.join(moduleOutDir, `${moduleId}-${version}.echo-addon`)
      echoAddonPackage = await buildEchoAddonPackage({
        moduleDir,
        moduleId,
        version,
        descriptor,
        descriptorPath,
        runtimeJarPath,
        outputPath: echoAddonPath,
        packageFromSource,
      })
      const stat = await fs.stat(echoAddonPath)
      artifacts.push({
        kind: 'echo-addon',
        filename: path.basename(echoAddonPath),
        sha256: await sha256File(echoAddonPath),
        size: stat.size,
        downloadUrl: '',
        runtimeTarget: 'echo-native',
        buildMode: runtimeJarPath ? 'compiled-runtime' : 'source-packaged',
        contains: ['META-INF/echo.mod.json', 'echo-addon-package.json', 'checksums.sha256'],
      })
    }
  }

  if (missing.length > 0) {
    throw new Error(`${moduleId}: missing ${missing.join(', ')}. Build the module first or pass --allow-missing-runtime for metadata-only dry runs.`)
  }

  const sourcesPath = path.join(moduleOutDir, `${moduleId}-${version}-sources.jar`)
  await buildSourcesJar(moduleDir, sourcesPath)
  const sourcesStat = await fs.stat(sourcesPath)
  artifacts.push({
    kind: 'sources',
    filename: path.basename(sourcesPath),
    sha256: await sha256File(sourcesPath),
    size: sourcesStat.size,
    downloadUrl: '',
    runtimeTarget: 'sources',
    contains: ['META-INF/echo.mod.json'],
  })

  await writeMetadataFiles({ moduleOutDir, descriptorPath, neoForgeToml, echoAddonPackage })

  return {
    moduleId,
    version,
    descriptor: {
      path: 'META-INF/echo.mod.json',
      sha256: await sha256File(descriptorPath),
    },
    requires: descriptor.requires ?? [],
    optional: descriptor.optional ?? [],
    artifacts,
  }
}

async function discoverModules(repoRoot, selectedModules) {
  const addonsDir = path.join(repoRoot, 'addons')
  const names = selectedModules.length > 0 ? selectedModules : (await fs.readdir(addonsDir)).sort()
  const modules = []
  for (const name of names) {
    const moduleDir = path.join(addonsDir, name)
    const descriptorPath = path.join(moduleDir, DESCRIPTOR_PATH)
    if (await fileExists(descriptorPath)) {
      modules.push(moduleDir)
    }
  }
  return modules
}

function parseArgs(argv) {
  const options = {
    modules: [],
    outDir: DEFAULT_OUT_DIR,
    allowMissingRuntime: false,
    packageFromSource: false,
    repoRoot: process.cwd(),
    releaseId: null,
    downloadBaseUrl: null,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module') options.modules.push(argv[++index])
    else if (arg === '--out') options.outDir = argv[++index]
    else if (arg === '--repo-root') options.repoRoot = argv[++index]
    else if (arg === '--release-id') options.releaseId = argv[++index]
    else if (arg === '--download-base-url') options.downloadBaseUrl = argv[++index]
    else if (arg === '--allow-missing-runtime') options.allowMissingRuntime = true
    else if (arg === '--package-from-source') {
      options.packageFromSource = true
      options.allowMissingRuntime = true
    }
    else if (arg === '--help') options.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return options
}

export async function generateModuleRelease(options = {}) {
  const repoRoot = path.resolve(options.repoRoot ?? process.cwd())
  const outputRoot = path.resolve(repoRoot, options.outDir ?? DEFAULT_OUT_DIR)
  const moduleDirs = await discoverModules(repoRoot, options.modules ?? [])
  if (moduleDirs.length === 0) {
    throw new Error('No modules with META-INF/echo.mod.json were found.')
  }
  await fs.rm(outputRoot, { recursive: true, force: true })
  await fs.mkdir(outputRoot, { recursive: true })

  const modules = []
  for (const moduleDir of moduleDirs) {
    modules.push(await generateModule({
      moduleDir,
      outputRoot,
      allowMissingRuntime: Boolean(options.allowMissingRuntime),
      packageFromSource: Boolean(options.packageFromSource),
    }))
  }
  applyArtifactDownloadUrls(modules, normalizeDownloadBaseUrl(options.downloadBaseUrl))

  // Generate and embed .ECHO Content Graph artifacts into release archives.
  console.log(`Generating .ECHO Content Graph artifacts for ${modules.length} module(s)...`)
  await generateContentGraph({ repoRoot, write: true, outputRoot })
  for (const moduleRecord of modules) {
    const moduleOutDir = path.join(outputRoot, moduleRecord.moduleId)
    const graphPath = path.join(moduleOutDir, moduleRecord.version, '.echo', 'content-graph', 'content-graph.json')
    if (await fileExists(graphPath)) {
      const sidecarPath = path.join(moduleOutDir, `${moduleRecord.moduleId}-${moduleRecord.version}-content-graph.json`)
      await fs.copyFile(graphPath, sidecarPath)
      const stat = await fs.stat(sidecarPath)
      moduleRecord.artifacts.push({
        kind: 'content-graph',
        filename: path.basename(sidecarPath),
        sha256: await sha256File(sidecarPath),
        size: stat.size,
        downloadUrl: '',
        runtimeTarget: 'content-graph',
        buildMode: 'generated',
        contains: ['.echo/content-graph/content-graph.json'],
      })
    }
  }
  for (const moduleRecord of modules) {
    const moduleOutDir = path.join(outputRoot, moduleRecord.moduleId)
    await embedContentGraphIntoModuleArtifacts({
      moduleOutDir,
      moduleId: moduleRecord.moduleId,
      version: moduleRecord.version,
      artifacts: moduleRecord.artifacts,
    })
  }

  const provenance = releaseProvenance()
  const release = {
    schemaVersion: MODULE_RELEASE_SCHEMA_VERSION,
    releaseId: options.releaseId ?? `modules-${new Date().toISOString().replace(/[:.]/g, '-')}`,
    generatedAt: new Date().toISOString(),
    sourceRepo: 'https://github.com/knoxhack/ECHO-Modules',
    commitSha: provenance.commitSha,
    provenance,
    modules,
  }
  await writeJson(path.join(outputRoot, 'echo-release.json'), release)
  const checksums = await releaseChecksumRows(outputRoot)
  await fs.writeFile(path.join(outputRoot, 'checksums.sha256'), checksums, 'utf8')
  await fs.writeFile(path.join(outputRoot, 'checksums.txt'), checksums, 'utf8')
  return release
}

function printHelp() {
  console.log(`Usage: node scripts/generate-module-release.mjs [options]

Options:
  --module <id>              Generate one module. Repeat for multiple modules.
  --out <dir>                Output directory relative to repo root. Default: ${DEFAULT_OUT_DIR}
  --release-id <id>          Release id to write into echo-release.json.
  --download-base-url <url>  Public release asset base URL used for artifact downloadUrl fields.
  --allow-missing-runtime    Allow metadata/source outputs when built runtime jars are missing.
  --package-from-source      Emit runtime-named archives from module source/resources when compiled jars are missing.
  --repo-root <path>         Repository root, mostly for tests.
`)
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const options = parseArgs(process.argv.slice(2))
    if (options.help) {
      printHelp()
    } else {
      const release = await generateModuleRelease(options)
      console.log(`Generated ${release.modules.length} module release record(s).`)
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}

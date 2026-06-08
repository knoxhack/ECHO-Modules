import { createHash } from 'node:crypto'
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const DEFAULT_OUT_DIR = 'dist/echo-module-release'
const DESCRIPTOR_PATH = 'src/main/resources/META-INF/echo.mod.json'
const NEOFORGE_TOML_PATHS = [
  'src/main/resources/META-INF/neoforge.mods.toml',
  'src/main/templates/META-INF/neoforge.mods.toml',
]

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

async function sha256File(filePath) {
  return createHash('sha256').update(await fs.readFile(filePath)).digest('hex')
}

async function copyFileWithRecord(sourcePath, outputPath, kind, contains = []) {
  await fs.mkdir(path.dirname(outputPath), { recursive: true })
  await fs.copyFile(sourcePath, outputPath)
  const stat = await fs.stat(outputPath)
  return {
    kind,
    filename: path.basename(outputPath),
    sha256: await sha256File(outputPath),
    size: stat.size,
    downloadUrl: '',
    runtimeTarget: kind === 'echo-addon' ? 'echo-native' : kind,
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

async function writeStoredZip(entries, outputPath) {
  const now = dosDateTime()
  const localParts = []
  const centralParts = []
  let offset = 0

  for (const entry of entries.sort((a, b) => a.name.localeCompare(b.name))) {
    const name = Buffer.from(normalizeZipPath(entry.name), 'utf8')
    const data = Buffer.isBuffer(entry.data) ? entry.data : Buffer.from(entry.data)
    const crc = crc32(data)
    const localHeader = Buffer.concat([
      u32(0x04034b50),
      u16(20),
      u16(0x0800),
      u16(0),
      u16(now.time),
      u16(now.day),
      u32(crc),
      u32(data.length),
      u32(data.length),
      u16(name.length),
      u16(0),
      name,
    ])
    localParts.push(localHeader, data)

    centralParts.push(Buffer.concat([
      u32(0x02014b50),
      u16(20),
      u16(20),
      u16(0x0800),
      u16(0),
      u16(now.time),
      u16(now.day),
      u32(crc),
      u32(data.length),
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
    offset += localHeader.length + data.length
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

async function readNeoForgeToml(moduleDir) {
  for (const relative of NEOFORGE_TOML_PATHS) {
    const absolute = path.join(moduleDir, relative)
    if (await fileExists(absolute)) {
      return { absolute, relative }
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

async function buildEchoAddonPackage({ moduleDir, moduleId, version, descriptorPath, runtimeJarPath, outputPath }) {
  const runtimeJarName = `${moduleId}-${version}-runtime.jar`
  const packageJson = {
    schemaVersion: 1,
    moduleId,
    version,
    runtime: 'echo-native',
    descriptor: 'META-INF/echo.mod.json',
    runtimeJar: `lib/${runtimeJarName}`,
  }
  const entries = [
    { name: 'META-INF/echo.mod.json', data: await fs.readFile(descriptorPath) },
    { name: 'echo-addon-package.json', data: `${JSON.stringify(packageJson, null, 2)}\n` },
  ]
  if (runtimeJarPath) {
    entries.push({ name: `lib/${runtimeJarName}`, data: await fs.readFile(runtimeJarPath) })
  }
  const readmePath = path.join(moduleDir, 'README.md')
  if (await fileExists(readmePath)) {
    entries.push({ name: 'README.md', data: await fs.readFile(readmePath) })
  }
  await writeStoredZip(entries, outputPath)
  return packageJson
}

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function writeMetadataFiles({ moduleOutDir, descriptorPath, neoForgeToml, echoAddonPackage }) {
  await fs.mkdir(path.join(moduleOutDir, 'META-INF'), { recursive: true })
  await fs.copyFile(descriptorPath, path.join(moduleOutDir, 'META-INF', 'echo.mod.json'))
  if (neoForgeToml) {
    await fs.copyFile(neoForgeToml.absolute, path.join(moduleOutDir, 'META-INF', 'neoforge.mods.toml'))
  }
  if (echoAddonPackage) {
    await writeJson(path.join(moduleOutDir, 'echo-addon-package.json'), echoAddonPackage)
  }
}

async function generateModule({ moduleDir, outputRoot, allowMissingRuntime }) {
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
  const neoForgeToml = await readNeoForgeToml(moduleDir)
  const runtimeJarPath = await findRuntimeJar(moduleDir, moduleId, version, 'runtime')

  if (neoForgeToml) {
    const sourceJar = await findRuntimeJar(moduleDir, moduleId, version, 'neoforge')
    if (!sourceJar && !allowMissingRuntime) {
      missing.push('neoforge runtime jar')
    } else if (sourceJar) {
      artifacts.push(await copyFileWithRecord(
        sourceJar,
        path.join(moduleOutDir, `${moduleId}-${version}-neoforge.jar`),
        'neoforge',
        ['META-INF/echo.mod.json', 'META-INF/neoforge.mods.toml'],
      ))
    }
  }

  if (descriptor.standalone !== false) {
    const sourceJar = await findRuntimeJar(moduleDir, moduleId, version, 'standalone')
    if (!sourceJar && !allowMissingRuntime) {
      missing.push('standalone runtime jar')
    } else if (sourceJar) {
      artifacts.push(await copyFileWithRecord(
        sourceJar,
        path.join(moduleOutDir, `${moduleId}-${version}-standalone.jar`),
        'standalone',
        ['META-INF/echo.mod.json'],
      ))
    }
  }

  let echoAddonPackage = null
  if (descriptor.access?.nativeEntrypoint || descriptor.standalone !== false) {
    if (!runtimeJarPath && !allowMissingRuntime) {
      missing.push('echo-addon runtime jar')
    } else {
      const echoAddonPath = path.join(moduleOutDir, `${moduleId}-${version}.echo-addon`)
      echoAddonPackage = await buildEchoAddonPackage({
        moduleDir,
        moduleId,
        version,
        descriptorPath,
        runtimeJarPath,
        outputPath: echoAddonPath,
      })
      const stat = await fs.stat(echoAddonPath)
      artifacts.push({
        kind: 'echo-addon',
        filename: path.basename(echoAddonPath),
        sha256: await sha256File(echoAddonPath),
        size: stat.size,
        downloadUrl: '',
        runtimeTarget: 'echo-native',
        contains: ['META-INF/echo.mod.json', 'echo-addon-package.json'],
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
    repoRoot: process.cwd(),
    releaseId: null,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module') options.modules.push(argv[++index])
    else if (arg === '--out') options.outDir = argv[++index]
    else if (arg === '--repo-root') options.repoRoot = argv[++index]
    else if (arg === '--release-id') options.releaseId = argv[++index]
    else if (arg === '--allow-missing-runtime') options.allowMissingRuntime = true
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
    }))
  }

  const release = {
    schemaVersion: 1,
    releaseId: options.releaseId ?? `modules-${new Date().toISOString().replace(/[:.]/g, '-')}`,
    generatedAt: new Date().toISOString(),
    sourceRepo: 'https://github.com/knoxhack/ECHO-Modules',
    modules,
  }
  await writeJson(path.join(outputRoot, 'echo-release.json'), release)
  await fs.writeFile(
    path.join(outputRoot, 'checksums.txt'),
    modules.flatMap((moduleRecord) => moduleRecord.artifacts.map((artifact) => `${artifact.sha256}  ${moduleRecord.moduleId}/${artifact.filename}`)).join('\n') + '\n',
    'utf8',
  )
  return release
}

function printHelp() {
  console.log(`Usage: node scripts/generate-module-release.mjs [options]

Options:
  --module <id>              Generate one module. Repeat for multiple modules.
  --out <dir>                Output directory relative to repo root. Default: ${DEFAULT_OUT_DIR}
  --release-id <id>          Release id to write into echo-release.json.
  --allow-missing-runtime    Allow metadata/source outputs when built runtime jars are missing.
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

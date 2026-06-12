import { createHash } from 'node:crypto'
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { deflateRawSync } from 'node:zlib'

const VERSION = '1.0.0'
const CHANNEL = 'beta'
const MINECRAFT_VERSION = '26.1.2'
const NEOFORGE_VERSION = '26.1.2'
const MODULE_RELEASE_ID = 'modules-arcana-division-1.0.0-beta'
const DEFAULT_MODULE_RELEASE_DIR = 'dist/arcana-division-module-release'

const coreModules = ['echocore', 'echoadaptercore', 'echonetcore']
const foundationModules = [
  'echofoundationcore',
  'echomaterialcore',
  'echotoolcore',
  'echostationcore',
  'echoworldstarter',
  'echocommonloot',
  'echocreatureroles',
]
const arcanaModules = [
  'echoarcanacore',
  'echoaetherworks',
  'echocursecore',
  'echofamiliarcore',
  'echogrimoire',
  'echoriftworlds',
  'echoritualcore',
  'echospellcore',
]
const launcherSupportModules = [
  'echoholomap',
  'echoindex',
  'echolens',
  'echoterminal',
  'echothemecore',
  'echomissioncore',
]
const runtimeModuleIds = [...coreModules, ...foundationModules, ...arcanaModules, ...launcherSupportModules]
const protocolModuleId = 'echoarcanadivisionprotocol'

const editions = [
  {
    target: 'native',
    repoName: 'ECHO-Arcana-Division-Native-Edition',
    packId: 'arcana-division-native-edition',
    name: 'Arcana Division Native Edition',
    moduleArtifactFamily: 'echo-addon',
    loader: 'echo-native-loader',
    runtimeMode: 'native-loader-minecraft',
    releaseTag: 'arcana-division-native-1.0.0-beta',
    installPathPrefix: 'addons',
  },
  {
    target: 'neoforge',
    repoName: 'ECHO-Arcana-Division-NeoForge-Edition',
    packId: 'arcana-division-neoforge-edition',
    name: 'Arcana Division NeoForge Edition',
    moduleArtifactFamily: 'neoforge',
    loader: 'neoforge',
    runtimeMode: 'neoforge-minecraft',
    releaseTag: 'arcana-division-neoforge-1.0.0-beta',
    installPathPrefix: 'mods',
  },
  {
    target: 'standalone',
    repoName: 'ECHO-Arcana-Division-Standalone-Edition',
    packId: 'arcana-division-standalone-edition',
    name: 'Arcana Division Standalone Edition',
    moduleArtifactFamily: 'standalone',
    loader: 'echo-standalone-runtime',
    runtimeMode: 'native-runtime',
    releaseTag: 'arcana-division-standalone-1.0.0-beta',
    installPathPrefix: 'mods',
  },
]

const crcTable = new Uint32Array(256)
for (let index = 0; index < 256; index += 1) {
  let value = index
  for (let bit = 0; bit < 8; bit += 1) {
    value = (value & 1) ? (0xedb88320 ^ (value >>> 1)) : (value >>> 1)
  }
  crcTable[index] = value >>> 0
}

function crc32(buffer) {
  let value = 0xffffffff
  for (const byte of buffer) value = crcTable[(value ^ byte) & 0xff] ^ (value >>> 8)
  return (value ^ 0xffffffff) >>> 0
}

function dosDateTime(date = new Date()) {
  const year = Math.max(date.getFullYear(), 1980)
  return {
    time: (date.getHours() << 11) | (date.getMinutes() << 5) | Math.floor(date.getSeconds() / 2),
    day: ((year - 1980) << 9) | ((date.getMonth() + 1) << 5) | date.getDate(),
  }
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

async function writeZip(entries, outputPath) {
  const now = dosDateTime()
  const localParts = []
  const centralParts = []
  let offset = 0

  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
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

function parseArgs(argv) {
  const args = {
    repoRoot: path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..'),
    moduleReleaseDir: DEFAULT_MODULE_RELEASE_DIR,
    editionsRoot: null,
    edition: null,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-release') args.moduleReleaseDir = argv[++index]
    else if (arg === '--editions-root') args.editionsRoot = argv[++index]
    else if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

async function readJson(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'))
}

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function sha256File(filePath) {
  return createHash('sha256').update(await fs.readFile(filePath)).digest('hex')
}

async function statRecord(filePath, role, fileName = path.basename(filePath)) {
  const stat = await fs.stat(filePath)
  return {
    role,
    file: fileName,
    sha256: await sha256File(filePath),
    size: stat.size,
  }
}

function moduleById(release) {
  return new Map(release.modules.map((module) => [module.moduleId, module]))
}

function artifactFor(module, family) {
  const kind = family === 'echo-addon' ? 'echo-addon' : family
  const artifact = module.artifacts.find((candidate) => candidate.kind === kind)
  if (!artifact) throw new Error(`${module.moduleId} has no ${kind} artifact in ${MODULE_RELEASE_ID}`)
  return artifact
}

function releaseAssetUrl(repoName, tag, file) {
  return `https://github.com/knoxhack/${repoName}/releases/download/${tag}/${file}`
}

function nativeLoaderManifest() {
  const versionId = 'echo-native-loader-1.0.0'
  return {
    version: '1.0.0',
    minecraftLauncherVersionId: versionId,
    versionJson: {
      id: versionId,
      inheritsFrom: MINECRAFT_VERSION,
      mainClass: 'com.echo.NativeLoaderClient',
      arguments: {
        game: ['--echo-pack-id', 'arcana-division-native-edition'],
        jvm: [],
      },
      libraries: [
        {
          name: 'com.echo:native-loader:1.0.0',
        },
      ],
    },
    libraries: [
      {
        name: 'com.echo:native-loader:1.0.0',
      },
    ],
  }
}

function packManifestBase(edition, moduleRequirements, files, artifact) {
  const manifest = {
    schemaVersion: 'echo.pack.v1',
    pack: edition.packId,
    id: edition.packId,
    name: edition.name,
    version: VERSION,
    channel: CHANNEL,
    target: edition.target,
    minecraft: edition.target === 'standalone' ? 'standalone' : MINECRAFT_VERSION,
    minecraftVersion: edition.target === 'standalone' ? undefined : MINECRAFT_VERSION,
    artifactMode: 'zip',
    artifactName: artifact.file,
    artifactSha256: artifact.sha256,
    artifactSize: artifact.size,
    moduleArtifactFamily: edition.moduleArtifactFamily,
    moduleReleaseId: MODULE_RELEASE_ID,
    moduleRequirements,
    requiredArtifacts: [
      ...moduleRequirements.map((requirement) => ({
        id: requirement.id,
        kind: 'module',
        version: requirement.version,
        artifactFamily: requirement.artifactFamily,
      })),
      {
        id: protocolModuleId,
        kind: 'module',
        version: VERSION,
        role: 'pack_root',
        artifactFamily: edition.moduleArtifactFamily,
      },
    ],
    modules: files.map((file) => file.moduleId),
    files,
    changelog: [
      'Arcana Division beta 1.0.0 pack root promoted for launcher installs.',
      'Pins core, Foundation, Arcana, HoloMap, Index, Lens, Terminal, ThemeCore, and MissionCore runtime modules.',
      'Ships Native, NeoForge, and Standalone edition metadata from one checksum-backed module release.',
    ],
    worldgenWarning: true,
  }

  if (edition.target === 'native') {
    manifest.nativeLoader = nativeLoaderManifest()
    manifest.launch = {
      mainClass: 'com.echo.NativeLoaderClient',
      gameArgs: ['--echo-pack-id', edition.packId],
      jvmArgs: [],
    }
  } else if (edition.target === 'neoforge') {
    manifest.loader = {
      type: 'neoforge',
      version: NEOFORGE_VERSION,
      minecraftLauncherVersionId: `neoforge-${NEOFORGE_VERSION}`,
      installer: {
        assetName: `neoforge-${NEOFORGE_VERSION}-installer.jar`,
        sha256: 'f'.repeat(64),
        installMode: 'client',
      },
    }
    manifest.launch = {
      mainClass: 'net.neoforged.fml.startup.Client',
      gameArgs: ['--echo-pack-id', edition.packId],
      jvmArgs: [],
    }
  } else {
    manifest.runtime = {
      id: 'echo-standalone-runtime',
      version: '1.0.0',
      requiredJava: 'none',
    }
    manifest.launch = {
      mainClass: 'com.echo.runtime.ArcanaDivisionStandaloneMain',
      gameArgs: ['--echo-pack-id', edition.packId],
      jvmArgs: [],
    }
  }

  return Object.fromEntries(Object.entries(manifest).filter(([, value]) => value !== undefined))
}

function readme(edition) {
  return `# ${edition.name}

Launcher-installable beta edition for ECHO: Arcana Division.

- Pack ID: \`${edition.packId}\`
- Version: \`${VERSION}\`
- Channel: \`${CHANNEL}\`
- Runtime target: \`${edition.target}\`
- Module release: \`${MODULE_RELEASE_ID}\`
- Release tag: \`${edition.releaseTag}\`

The generated artifacts live under \`dist/\`:

- \`${edition.packId}-${VERSION}.zip\`
- \`${edition.packId}-${CHANNEL}-${VERSION}.pack.json\`
- \`echo-release.json\`
- \`checksums.txt\`

Run \`npm run build\` after regenerating the ECHO-Modules Arcana beta release,
then \`npm run validate\` before publishing the GitHub prerelease.
`
}

function packageJson(edition) {
  return {
    name: edition.packId,
    private: true,
    version: VERSION,
    type: 'module',
    scripts: {
      build: 'node scripts/build-arcana-division-edition.mjs',
      validate: 'node scripts/validate-arcana-division-edition.mjs',
    },
  }
}

function buildWrapper(edition) {
  return `import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const modulesRoot = path.resolve(repoRoot, '..', 'ECHO-Modules')
const script = path.join(modulesRoot, 'scripts', 'generate-arcana-division-editions.mjs')
const result = spawnSync(process.execPath, [script, '--edition', '${edition.packId}'], {
  cwd: modulesRoot,
  stdio: 'inherit',
  shell: false,
})
process.exitCode = result.status ?? 1
`
}

function validateWrapper(edition) {
  return `import { createHash } from 'node:crypto'
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const dist = path.join(repoRoot, 'dist')
const expected = {
  packId: '${edition.packId}',
  version: '${VERSION}',
  channel: '${CHANNEL}',
  target: '${edition.target}',
  moduleRequirements: 24,
}

async function sha256(filePath) {
  return createHash('sha256').update(await fs.readFile(filePath)).digest('hex')
}

async function readJson(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'))
}

const manifestPath = path.join(dist, '${edition.packId}-${CHANNEL}-${VERSION}.pack.json')
const releasePath = path.join(dist, 'echo-release.json')
const checksumsPath = path.join(dist, 'checksums.txt')
const manifest = await readJson(manifestPath)
const release = await readJson(releasePath)

const errors = []
if (manifest.pack !== expected.packId) errors.push('pack id mismatch')
if (manifest.version !== expected.version) errors.push('manifest version mismatch')
if (manifest.channel !== expected.channel) errors.push('manifest channel mismatch')
if (manifest.target !== expected.target) errors.push('manifest target mismatch')
if ((manifest.moduleRequirements ?? []).length !== expected.moduleRequirements) errors.push('moduleRequirements must contain 24 entries')
if (!manifest.files?.some((file) => file.moduleId === 'echoarcanadivisionprotocol')) errors.push('pack root protocol artifact is missing from files')
if (release.id !== expected.packId) errors.push('release id mismatch')
if (release.releaseTag !== '${edition.releaseTag}') errors.push('release tag mismatch')

const checksumRows = (await fs.readFile(checksumsPath, 'utf8')).trim().split(/\\r?\\n/).filter(Boolean)
for (const row of checksumRows) {
  const [hash, file] = row.split(/\\s+/)
  const actual = await sha256(path.join(dist, file))
  if (hash !== actual) errors.push(\`checksum mismatch for \${file}\`)
}

if (errors.length) {
  console.error('${edition.name} validation failed:')
  for (const error of errors) console.error(\`- \${error}\`)
  process.exit(1)
}

console.log('${edition.name} validation passed.')
`
}

async function buildEdition({ edition, repoRoot, moduleReleaseDir, release }) {
  const parentRoot = path.resolve(repoRoot, '..')
  const editionRoot = path.join(parentRoot, edition.repoName)
  const distRoot = path.join(editionRoot, 'dist')
  const stagingFiles = []
  const byModule = moduleById(release)

  await fs.mkdir(editionRoot, { recursive: true })
  await fs.rm(distRoot, { recursive: true, force: true })
  await fs.mkdir(path.join(editionRoot, 'scripts'), { recursive: true })

  const manifestFiles = []
  const moduleRequirements = []
  const allFileModuleIds = [...runtimeModuleIds, protocolModuleId]
  for (const moduleId of allFileModuleIds) {
    const module = byModule.get(moduleId)
    if (!module) throw new Error(`${MODULE_RELEASE_ID} is missing ${moduleId}`)
    const artifact = artifactFor(module, edition.moduleArtifactFamily)
    const source = path.join(moduleReleaseDir, moduleId, artifact.filename)
    const installPath = moduleId === protocolModuleId
      ? `pack-root/${artifact.filename}`
      : `${edition.installPathPrefix}/${artifact.filename}`
    stagingFiles.push({ name: installPath, data: await fs.readFile(source) })
    manifestFiles.push({
      path: installPath,
      assetName: artifact.filename,
      sha256: artifact.sha256,
      size: artifact.size,
      required: true,
      moduleId,
      side: 'both',
    })
    if (moduleId !== protocolModuleId) {
      moduleRequirements.push({
        id: moduleId,
        version: module.version,
        artifactFamily: edition.moduleArtifactFamily,
        artifactName: artifact.filename,
        assetName: artifact.filename,
        path: installPath,
        sha256: artifact.sha256,
        size: artifact.size,
        required: true,
        side: 'both',
      })
    }
  }

  stagingFiles.push({
    name: 'README.txt',
    data: `${edition.name}\nVersion: ${VERSION}\nChannel: ${CHANNEL}\nModule release: ${MODULE_RELEASE_ID}\n`,
  })

  await fs.writeFile(path.join(editionRoot, 'README.md'), readme(edition), 'utf8')
  await fs.writeFile(path.join(editionRoot, '.gitignore'), 'dist/\nnode_modules/\n', 'utf8')
  await writeJson(path.join(editionRoot, 'package.json'), packageJson(edition))
  await fs.writeFile(path.join(editionRoot, 'scripts', 'build-arcana-division-edition.mjs'), buildWrapper(edition), 'utf8')
  await fs.writeFile(path.join(editionRoot, 'scripts', 'validate-arcana-division-edition.mjs'), validateWrapper(edition), 'utf8')

  const zipName = `${edition.packId}-${VERSION}.zip`
  const zipPath = path.join(distRoot, zipName)
  await writeZip(stagingFiles, zipPath)
  const zipArtifact = await statRecord(zipPath, 'pack', zipName)

  const packManifestName = `${edition.packId}-${CHANNEL}-${VERSION}.pack.json`
  const packManifestPath = path.join(distRoot, packManifestName)
  const packManifest = packManifestBase(edition, moduleRequirements, manifestFiles, zipArtifact)
  await writeJson(packManifestPath, packManifest)
  const manifestArtifact = await statRecord(packManifestPath, 'manifest', packManifestName)

  const releaseManifestPath = path.join(distRoot, 'echo-release.json')
  const releaseManifest = {
    schemaVersion: 'echo.pack.release.v1',
    id: edition.packId,
    name: edition.name,
    version: VERSION,
    channel: CHANNEL,
    target: edition.target,
    releaseTag: edition.releaseTag,
    generatedAt: new Date().toISOString(),
    sourceRepo: `https://github.com/knoxhack/${edition.repoName}`,
    moduleReleaseId: release.releaseId,
    moduleReleaseSource: 'https://github.com/knoxhack/ECHO-Modules',
    moduleRequirementCount: moduleRequirements.length,
    packRoot: {
      id: protocolModuleId,
      version: VERSION,
    },
    artifacts: {
      pack: {
        ...zipArtifact,
        url: releaseAssetUrl(edition.repoName, edition.releaseTag, zipArtifact.file),
      },
      manifest: {
        ...manifestArtifact,
        url: releaseAssetUrl(edition.repoName, edition.releaseTag, manifestArtifact.file),
      },
    },
    validation: {
      state: 'warning',
      reason: 'Local beta artifacts generated; promote to approved after GitHub release asset URLs, sizes, and hashes are indexed.',
    },
  }
  await writeJson(releaseManifestPath, releaseManifest)
  const releaseArtifact = await statRecord(releaseManifestPath, 'releaseManifest', 'echo-release.json')

  const checksumRows = []
  for (const artifact of [zipArtifact, manifestArtifact, releaseArtifact]) {
    checksumRows.push(`${artifact.sha256}  ${artifact.file}`)
  }
  const checksumsPath = path.join(distRoot, 'checksums.txt')
  await fs.writeFile(checksumsPath, `${checksumRows.join('\n')}\n`, 'utf8')
  const checksumsArtifact = await statRecord(checksumsPath, 'checksums', 'checksums.txt')

  return {
    edition,
    editionRoot,
    artifacts: {
      pack: zipArtifact,
      manifest: manifestArtifact,
      releaseManifest: releaseArtifact,
      checksums: checksumsArtifact,
    },
  }
}

function printHelp() {
  console.log(`Usage: node scripts/generate-arcana-division-editions.mjs [options]

Options:
  --module-release <dir>   Module release directory. Default: ${DEFAULT_MODULE_RELEASE_DIR}
  --edition <pack-id>      Generate one edition. Default: all Arcana Division editions.
`)
}

try {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
  } else {
    const repoRoot = path.resolve(args.repoRoot)
    const moduleReleaseDir = path.resolve(repoRoot, args.moduleReleaseDir)
    const release = await readJson(path.join(moduleReleaseDir, 'echo-release.json'))
    if (release.releaseId !== MODULE_RELEASE_ID) {
      throw new Error(`Expected module release ${MODULE_RELEASE_ID}, got ${release.releaseId}`)
    }
    const selected = args.edition
      ? editions.filter((edition) => edition.packId === args.edition || edition.target === args.edition)
      : editions
    if (!selected.length) throw new Error(`Unknown Arcana Division edition: ${args.edition}`)
    const results = []
    for (const edition of selected) {
      results.push(await buildEdition({ edition, repoRoot, moduleReleaseDir, release }))
    }
    for (const result of results) {
      console.log(`Generated ${result.edition.name} at ${result.editionRoot}`)
    }
  }
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error))
  process.exitCode = 1
}

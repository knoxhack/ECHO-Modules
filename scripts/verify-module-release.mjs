import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'
import zlib from 'node:zlib'
import { generateModuleRelease } from './generate-module-release.mjs'

function parseArgs(argv) {
  const args = {
    module: 'echoarmory',
    repoRoot: process.cwd(),
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module') args.module = argv[++index]
    else if (arg === '--repo-root') args.repoRoot = path.resolve(argv[++index])
    else if (arg === '--release-dir') args.releaseDir = path.resolve(argv[++index])
    else if (arg === '--skip-real') args.skipReal = true
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

async function readJson(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'))
}

async function sha256File(filePath) {
  return createHash('sha256').update(await fs.readFile(filePath)).digest('hex')
}

function parseChecksums(text) {
  const checksums = new Map()
  for (const line of String(text ?? '').split(/\r?\n/)) {
    const match = line.trim().match(/^([a-f0-9]{64})\s+(.+)$/i)
    if (match) checksums.set(match[2].trim(), match[1].toLowerCase())
  }
  return checksums
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
    assert.equal(buffer.readUInt32LE(cursor), 0x02014b50, 'invalid ZIP central directory entry')
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
  assert.equal(buffer.readUInt32LE(cursor), 0x04034b50, `invalid ZIP local header for ${entry.name}`)
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
    entries: entryMap,
    json(name) {
      const entry = entryMap.get(name)
      if (!entry) return null
      return JSON.parse(readZipEntry(buffer, entry).toString('utf8'))
    },
    text(name) {
      const entry = entryMap.get(name)
      if (!entry) return null
      return readZipEntry(buffer, entry).toString('utf8')
    },
  }
}

async function makeFixtureRepo() {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-module-fixture-'))
  const moduleDir = path.join(root, 'addons', 'echofixture')
  await fs.mkdir(path.join(moduleDir, 'src/main/resources/META-INF'), { recursive: true })
  await fs.mkdir(path.join(moduleDir, 'src/main/java/dev/echo/fixture'), { recursive: true })
  await fs.writeFile(path.join(moduleDir, 'src/main/java/dev/echo/fixture/Fixture.java'), 'package dev.echo.fixture; public final class Fixture {}')
  await fs.writeFile(path.join(moduleDir, 'src/main/resources/META-INF/neoforge.mods.toml'), 'modLoader="javafml"\n[[mods]]\nmodId="echofixture"\n')
  await fs.writeFile(path.join(moduleDir, 'src/main/resources/META-INF/echo.mod.json'), `${JSON.stringify({
    schema: 'echo.mod.v1',
    id: 'echofixture',
    name: 'ECHO Fixture',
    version: '1.2.3',
    standalone: true,
    requires: ['echocore'],
    optional: [],
    access: { nativeEntrypoint: 'dev.echo.fixture.FixtureModule' },
  }, null, 2)}\n`)
  return root
}

function expectedArtifactNames(moduleId, version) {
  return [
    `${moduleId}-${version}-content-graph.json`,
    `${moduleId}-${version}-neoforge.jar`,
    `${moduleId}-${version}-sources.jar`,
    `${moduleId}-${version}-standalone.jar`,
    `${moduleId}-${version}.echo-addon`,
  ]
}

function dependencyBlocks(tomlText) {
  return String(tomlText)
    .split(/\n(?=\[\[dependencies\.)/u)
    .filter((block) => /^\[\[dependencies\./u.test(block.trimStart()))
}

function assertContentGraphDocument(document, moduleRecord, context) {
  assert.ok(document && typeof document === 'object' && !Array.isArray(document), `${context} must be a JSON object`)
  assert.equal(document.schemaVersion, 'echo.content_graph.v1', `${context} must use echo.content_graph.v1`)
  assert.equal(document.moduleId, moduleRecord.moduleId, `${context} moduleId must match release record`)
  assert.ok(Array.isArray(document.modules), `${context} modules must be an array`)
  assert.ok(document.modules.includes(moduleRecord.moduleId), `${context} modules must include ${moduleRecord.moduleId}`)
  assert.ok(Array.isArray(document.nodes), `${context} nodes must be an array`)
  assert.ok(Array.isArray(document.edges), `${context} edges must be an array`)
  assert.ok(typeof document.generatedAt === 'string' && document.generatedAt.length > 0, `${context} must include generatedAt`)
}

function assertRuntimeArchiveContentGraph(zip, moduleRecord, label) {
  const requiredEntries = [
    '.echo/content-graph/content-graph.json',
    '.echo/content-graph/features.json',
    '.echo/content-graph/export-plans/hytale.json',
  ]
  for (const entry of requiredEntries) {
    assert.ok(zip.entries.has(entry), `${label} must embed ${entry}`)
  }
  assertContentGraphDocument(
    zip.json('.echo/content-graph/content-graph.json'),
    moduleRecord,
    `${label} embedded content graph`,
  )
  const features = zip.json('.echo/content-graph/features.json')
  assert.ok(features && typeof features === 'object' && Array.isArray(features.features), `${label} features.json must include features array`)
  const hytale = zip.json('.echo/content-graph/export-plans/hytale.json')
  assert.ok(hytale && typeof hytale === 'object', `${label} Hytale export plan must be a JSON object`)
  assert.equal(hytale.schemaVersion, 'echo.content_graph.export_plan.v1', `${label} Hytale export plan schema mismatch`)
  assert.equal(hytale.target, 'hytale', `${label} Hytale export plan target mismatch`)
  assert.ok(Array.isArray(hytale.nodes), `${label} Hytale export plan nodes must be an array`)
  assert.ok(hytale.summary && typeof hytale.summary === 'object', `${label} Hytale export plan must include summary`)
}

function assertContentGraphEvidenceDocument(document, release) {
  assert.ok(document && typeof document === 'object' && !Array.isArray(document), 'content-graph-evidence.json must be a JSON object')
  assert.equal(document.schemaVersion, 'echo.content_graph.evidence.v1', 'content graph evidence schema mismatch')
  assert.equal(document.graphCount, release.modules.length, 'content graph evidence graphCount must match release modules')
  assert.equal(document.moduleCount, release.modules.length, 'content graph evidence moduleCount must match release modules')
  assert.ok(document.nodeCount > 0, 'content graph evidence must count nodes')
  assert.ok(document.edgeCount >= 0, 'content graph evidence must count edges')
  assert.ok(document.featureCount >= 0, 'content graph evidence must count features')
  assert.ok(document.exportPlanCount >= release.modules.length, 'content graph evidence must count export plans')
  assert.ok(Number.isInteger(document.hytaleBlockerCount), 'content graph evidence must count Hytale blockers')
  assert.ok(Array.isArray(document.modules), 'content graph evidence modules must be an array')
  assert.equal(document.modules.length, release.modules.length, 'content graph evidence modules length must match release modules')
  assert.ok(Array.isArray(document.diagnostics), 'content graph evidence diagnostics must be an array')
}

async function verifyReleaseDir(releaseDir) {
  const release = await readJson(path.join(releaseDir, 'echo-release.json'))
  assert.equal(release.schemaVersion, 'echo.module.release.v1')
  assert.ok(Array.isArray(release.modules) && release.modules.length > 0, 'release manifest must include modules')
  assert.equal(release.provenance?.generatedBy, 'scripts/generate-module-release.mjs', 'release manifest must record generator provenance')
  assert.equal(release.provenance?.attestation?.action, 'actions/attest@v4', 'release manifest must record attestation action')
  assert.equal(release.provenance?.attestation?.subjectChecksums, 'checksums.sha256', 'release manifest must record checksum attestation subject')
  assert.equal(release.contentGraphEvidence?.kind, 'content-graph-evidence', 'release manifest must record content graph evidence artifact')
  assert.equal(release.contentGraphEvidence?.filename, 'content-graph-evidence.json', 'release manifest content graph evidence filename mismatch')
  assert.equal(release.contentGraphEvidence?.schemaVersion, 'echo.content_graph.evidence.v1', 'release manifest content graph evidence schema mismatch')
  assert.ok(await fileExists(path.join(releaseDir, 'checksums.sha256')), 'checksums.sha256 must exist')
  assert.ok(await fileExists(path.join(releaseDir, 'checksums.txt')), 'checksums.txt compatibility copy must exist')
  assert.ok(await fileExists(path.join(releaseDir, 'content-graph-evidence.json')), 'content-graph-evidence.json must exist')

  const checksums = parseChecksums(await fs.readFile(path.join(releaseDir, 'checksums.sha256'), 'utf8'))
  assert.equal(checksums.get('echo-release.json'), await sha256File(path.join(releaseDir, 'echo-release.json')), 'echo-release.json checksum row missing')
  assert.equal(
    checksums.get('content-graph-evidence.json'),
    await sha256File(path.join(releaseDir, 'content-graph-evidence.json')),
    'content-graph-evidence.json checksum row missing',
  )
  assert.equal(
    release.contentGraphEvidence.sha256,
    await sha256File(path.join(releaseDir, 'content-graph-evidence.json')),
    'release manifest content graph evidence sha256 mismatch',
  )
  assertContentGraphEvidenceDocument(await readJson(path.join(releaseDir, 'content-graph-evidence.json')), release)
  for (const moduleRecord of release.modules) {
    const moduleDir = path.join(releaseDir, moduleRecord.moduleId)
    assert.deepEqual(moduleRecord.artifacts.map((artifact) => artifact.filename).sort(), expectedArtifactNames(moduleRecord.moduleId, moduleRecord.version))
    assert.ok(await fileExists(path.join(moduleDir, 'META-INF', 'echo.mod.json')), `${moduleRecord.moduleId} descriptor sidecar missing`)
    assert.ok(await fileExists(path.join(moduleDir, 'echo-addon-package.json')), `${moduleRecord.moduleId} package sidecar missing`)
    assert.equal(
      checksums.get(`${moduleRecord.moduleId}/META-INF/echo.mod.json`),
      await sha256File(path.join(moduleDir, 'META-INF', 'echo.mod.json')),
      `${moduleRecord.moduleId} descriptor sidecar checksum row missing`,
    )
    assert.equal(
      checksums.get(`${moduleRecord.moduleId}/echo-addon-package.json`),
      await sha256File(path.join(moduleDir, 'echo-addon-package.json')),
      `${moduleRecord.moduleId} package sidecar checksum row missing`,
    )
    if (await fileExists(path.join(moduleDir, 'META-INF', 'neoforge.mods.toml'))) {
      assert.equal(
        checksums.get(`${moduleRecord.moduleId}/META-INF/neoforge.mods.toml`),
        await sha256File(path.join(moduleDir, 'META-INF', 'neoforge.mods.toml')),
        `${moduleRecord.moduleId} NeoForge TOML sidecar checksum row missing`,
      )
    }

    for (const artifact of moduleRecord.artifacts) {
      const artifactPath = path.join(moduleDir, artifact.filename)
      assert.equal(await sha256File(artifactPath), artifact.sha256, `${artifact.filename} manifest sha256 mismatch`)
      assert.equal(checksums.get(`${moduleRecord.moduleId}/${artifact.filename}`), artifact.sha256, `${artifact.filename} checksum row missing`)
      if (artifact.kind !== 'sources' && artifact.kind !== 'content-graph') {
        assert.ok(
          artifact.buildMode === 'compiled-runtime' || artifact.buildMode === 'source-packaged',
          `${artifact.filename} must declare compiled-runtime or source-packaged buildMode`,
        )
      }
    }

    const contentGraphArtifact = moduleRecord.artifacts.find((artifact) => artifact.kind === 'content-graph')
    assert.ok(contentGraphArtifact, `${moduleRecord.moduleId} content graph artifact missing`)
    assert.equal(contentGraphArtifact.runtimeTarget, 'content-graph', `${moduleRecord.moduleId} content graph artifact target mismatch`)
    assert.equal(contentGraphArtifact.buildMode, 'generated', `${moduleRecord.moduleId} content graph artifact must be generated`)
    assert.ok(
      contentGraphArtifact.contains.includes('.echo/content-graph/content-graph.json'),
      `${moduleRecord.moduleId} content graph artifact must declare canonical graph content`,
    )
    assertContentGraphDocument(
      await readJson(path.join(moduleDir, contentGraphArtifact.filename)),
      moduleRecord,
      `${moduleRecord.moduleId} content graph sidecar`,
    )

    const echoAddon = await inspectZip(path.join(moduleDir, `${moduleRecord.moduleId}-${moduleRecord.version}.echo-addon`))
    assert.ok(echoAddon.entries.has('META-INF/echo.mod.json'), 'echo-addon must embed descriptor')
    assert.ok(echoAddon.entries.has('echo-addon-package.json'), 'echo-addon must embed package manifest')
    assertRuntimeArchiveContentGraph(echoAddon, moduleRecord, `${moduleRecord.moduleId} echo-addon`)
    const packageManifest = echoAddon.json('echo-addon-package.json')
    assert.equal(packageManifest.schemaVersion, 'echo.addon.package.v1')
    assert.ok(Array.isArray(packageManifest.dependencies), 'package manifest dependencies must be an array')
    for (const dependency of packageManifest.dependencies) {
      assert.ok(dependency.id, 'package manifest dependency must include id')
      assert.ok(dependency.version, 'package manifest dependency must include version')
    }
    if (moduleRecord.requires.length > 0) {
      assert.deepEqual(
        packageManifest.dependencies.map((dependency) => dependency.id).sort(),
        moduleRecord.requires.slice().sort(),
        'package manifest dependencies must mirror module requires',
      )
    }

    const neoforge = await inspectZip(path.join(moduleDir, `${moduleRecord.moduleId}-${moduleRecord.version}-neoforge.jar`))
    assert.ok(neoforge.entries.has('META-INF/echo.mod.json'), 'NeoForge jar must embed descriptor')
    assert.ok(neoforge.entries.has('META-INF/neoforge.mods.toml'), 'NeoForge jar must embed neoforge.mods.toml')
    assertRuntimeArchiveContentGraph(neoforge, moduleRecord, `${moduleRecord.moduleId} NeoForge jar`)
    const neoForgeToml = neoforge.text('META-INF/neoforge.mods.toml')
    assert.ok(!neoForgeToml.includes('${'), `${moduleRecord.moduleId} NeoForge TOML must not contain unresolved template placeholders`)
    assert.match(neoForgeToml, new RegExp(`modId\\s*=\\s*"${moduleRecord.moduleId}"`, 'u'), `${moduleRecord.moduleId} NeoForge TOML must declare the module id`)
    const minecraftDependency = dependencyBlocks(neoForgeToml).find((block) => /modId\s*=\s*"minecraft"/u.test(block))
    if (minecraftDependency) {
      assert.match(minecraftDependency, /versionRange\s*=\s*"\[26\.1\.2,26\.2\)"/u, `${moduleRecord.moduleId} NeoForge TOML must target the ECHO NeoForge Minecraft runtime range`)
    }

    const standalone = await inspectZip(path.join(moduleDir, `${moduleRecord.moduleId}-${moduleRecord.version}-standalone.jar`))
    assert.ok(standalone.entries.has('META-INF/echo.mod.json'), 'Standalone jar must embed descriptor')
    assertRuntimeArchiveContentGraph(standalone, moduleRecord, `${moduleRecord.moduleId} Standalone jar`)

    const sources = await inspectZip(path.join(moduleDir, `${moduleRecord.moduleId}-${moduleRecord.version}-sources.jar`))
    assert.ok(sources.entries.has('META-INF/echo.mod.json'), 'sources jar must embed descriptor')
  }
  return release.modules.length
}

async function generateAndVerifyFixture() {
  const repoRoot = await makeFixtureRepo()
  const outputDir = path.join(await fs.mkdtemp(path.join(os.tmpdir(), 'echo-module-fixture-out-')), 'release')
  await generateModuleRelease({
    repoRoot,
    outDir: outputDir,
    modules: ['echofixture'],
    releaseId: 'fixture-verification',
    packageFromSource: true,
  })
  return verifyReleaseDir(outputDir)
}

async function generateAndVerifyReal(args) {
  const outputDir = path.join(await fs.mkdtemp(path.join(os.tmpdir(), 'echo-module-real-out-')), 'release')
  await generateModuleRelease({
    repoRoot: args.repoRoot,
    outDir: outputDir,
    modules: [args.module],
    releaseId: 'real-verification',
    packageFromSource: true,
  })
  return verifyReleaseDir(outputDir)
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  let verified = 0
  if (args.releaseDir) {
    verified += await verifyReleaseDir(args.releaseDir)
  } else {
    verified += await generateAndVerifyFixture()
    if (!args.skipReal) verified += await generateAndVerifyReal(args)
  }
  console.log(`Verified ${verified} module release record(s).`)
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error))
  process.exitCode = 1
})

import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { generateModuleRelease } from './generate-module-release.mjs'

function runJar(args, options = {}) {
  const result = spawnSync('jar', args, { encoding: 'utf8', ...options })
  if (result.status !== 0) {
    throw new Error(`jar ${args.join(' ')} failed: ${result.stderr || result.stdout}`)
  }
  return result.stdout
}

async function writeFixtureRuntimeJar(jarPath) {
  const jarRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-module-jar-'))
  await fs.mkdir(path.join(jarRoot, 'dev', 'echo', 'sample'), { recursive: true })
  await fs.writeFile(path.join(jarRoot, 'dev', 'echo', 'sample', 'Compiled.class'), 'compiled fixture')
  await fs.mkdir(path.dirname(jarPath), { recursive: true })
  runJar(['cf', jarPath, '-C', jarRoot, '.'])
}

function jarEntries(jarPath) {
  return new Set(runJar(['tf', jarPath]).split(/\r?\n/u).filter(Boolean))
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

async function readJarEntry(jarPath, entryName) {
  const extractRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-module-jar-entry-'))
  runJar(['xf', jarPath, entryName], { cwd: extractRoot })
  return fs.readFile(path.join(extractRoot, entryName), 'utf8')
}

async function makeRepo() {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-modules-release-'))
  const moduleDir = path.join(root, 'addons', 'echosample')
  await fs.mkdir(path.join(moduleDir, 'src/main/resources/META-INF'), { recursive: true })
  await fs.mkdir(path.join(moduleDir, 'src/main/templates/META-INF'), { recursive: true })
  await fs.mkdir(path.join(moduleDir, 'src/main/java/dev/echo/sample'), { recursive: true })
  await fs.mkdir(path.join(moduleDir, 'build/libs'), { recursive: true })
  await fs.writeFile(path.join(moduleDir, 'src/main/java/dev/echo/sample/Sample.java'), 'package dev.echo.sample; public final class Sample {}')
  await fs.writeFile(path.join(moduleDir, 'src/main/templates/META-INF/neoforge.mods.toml'), 'modLoader="javafml"\n[[mods]]\nmodId="${mod_id}"\nversion="${mod_version}"\ndisplayName="${mod_name}"\n[[dependencies.${mod_id}]]\nmodId="minecraft"\nversionRange="${minecraft_version_range}"\n')
  await fs.writeFile(path.join(moduleDir, 'gradle.properties'), 'minecraft_version=26.1.2\nminecraft_version_range=[26.1.2,26.2)\nmod_id=echosample\nmod_name=ECHO Sample\nmod_license=All Rights Reserved\nmod_version=1.2.3\n')
  await fs.writeFile(path.join(moduleDir, 'src/main/resources/META-INF/echo.mod.json'), JSON.stringify({
    schema: 'echo.mod.v1',
    id: 'echosample',
    name: 'ECHO Sample',
    version: '1.2.3',
    standalone: true,
    requires: ['echoaddonapi'],
    optional: [],
    access: {
      nativeEntrypoint: 'dev.echo.sample.SampleModule',
    },
  }, null, 2))
  await writeFixtureRuntimeJar(path.join(moduleDir, 'build/libs/echosample-1.2.3.jar'))
  return root
}

test('generates per-module release artifacts and metadata', async () => {
  const repoRoot = await makeRepo()
  const release = await generateModuleRelease({
    repoRoot,
    releaseId: 'test-release',
    modules: ['echosample'],
    downloadBaseUrl: 'https://github.com/knoxhack/ECHO-Modules/releases/download/test-release',
  })

  assert.equal(release.releaseId, 'test-release')
  assert.equal(release.schemaVersion, 'echo.module.release.v1')
  assert.equal(release.provenance.generatedBy, 'scripts/generate-module-release.mjs')
  assert.equal(release.provenance.attestation.action, 'actions/attest@v4')
  assert.equal(release.provenance.attestation.subjectChecksums, 'echo-module-release.tar.gz.sha256')
  assert.equal(release.contentGraphEvidence.kind, 'content-graph-evidence')
  assert.equal(release.contentGraphEvidence.filename, 'content-graph-evidence.json')
  assert.equal(release.contentGraphEvidence.runtimeTarget, 'content-graph')
  assert.equal(release.contentGraphEvidence.buildMode, 'generated')
  assert.equal(release.contentGraphEvidence.schemaVersion, 'echo.content_graph.evidence.v1')
  assert.equal(
    release.contentGraphEvidence.downloadUrl,
    'https://github.com/knoxhack/ECHO-Modules/releases/download/test-release/content-graph-evidence.json',
  )
  assert.equal(release.modules.length, 1)
  const moduleRecord = release.modules[0]
  assert.equal(moduleRecord.moduleId, 'echosample')
  assert.deepEqual(moduleRecord.artifacts.map((artifact) => artifact.filename).sort(), expectedArtifactNames('echosample', '1.2.3'))
  assert.equal(moduleRecord.artifacts.find((artifact) => artifact.kind === 'neoforge').buildMode, 'compiled-runtime')
  assert.equal(moduleRecord.artifacts.find((artifact) => artifact.kind === 'standalone').buildMode, 'compiled-runtime')
  const contentGraphArtifact = moduleRecord.artifacts.find((artifact) => artifact.kind === 'content-graph')
  assert.equal(contentGraphArtifact.buildMode, 'generated')
  assert.equal(contentGraphArtifact.runtimeTarget, 'content-graph')
  assert.ok(contentGraphArtifact.contains.includes('.echo/content-graph/content-graph.json'))
  const echoAddonArtifact = moduleRecord.artifacts.find((artifact) => artifact.kind === 'echo-addon')
  assert.equal(echoAddonArtifact.buildMode, 'compiled-runtime')
  assert.equal(
    echoAddonArtifact.downloadUrl,
    'https://github.com/knoxhack/ECHO-Modules/releases/download/test-release/echosample-1.2.3.echo-addon',
  )
  assert.ok(echoAddonArtifact.contains.includes('checksums.sha256'))

  const outputDir = path.join(repoRoot, 'dist', 'echo-module-release', 'echosample')
  await fs.access(path.join(outputDir, 'META-INF', 'echo.mod.json'))
  await fs.access(path.join(outputDir, 'META-INF', 'neoforge.mods.toml'))
  await fs.access(path.join(outputDir, 'echo-addon-package.json'))
  await fs.access(path.join(outputDir, 'echosample-1.2.3-content-graph.json'))
  await fs.access(path.join(outputDir, '1.2.3', '.echo', 'content-graph', 'features.json'))
  await fs.access(path.join(outputDir, '1.2.3', '.echo', 'content-graph', 'export-plans', 'hytale.json'))
  await fs.access(path.join(repoRoot, 'dist', 'echo-module-release', 'echo-release.json'))
  await fs.access(path.join(repoRoot, 'dist', 'echo-module-release', 'content-graph-evidence.json'))
  await fs.access(path.join(repoRoot, 'dist', 'echo-module-release', 'checksums.txt'))

  const contentGraph = JSON.parse(await fs.readFile(path.join(outputDir, 'echosample-1.2.3-content-graph.json'), 'utf8'))
  assert.equal(contentGraph.schemaVersion, 'echo.content_graph.v1')
  assert.equal(contentGraph.moduleId, 'echosample')
  assert.ok(Array.isArray(contentGraph.nodes))
  assert.ok(Array.isArray(contentGraph.edges))
  const contentGraphEvidence = JSON.parse(await fs.readFile(path.join(repoRoot, 'dist', 'echo-module-release', 'content-graph-evidence.json'), 'utf8'))
  assert.equal(contentGraphEvidence.schemaVersion, 'echo.content_graph.evidence.v1')
  assert.equal(contentGraphEvidence.graphCount, 1)
  assert.equal(contentGraphEvidence.moduleCount, 1)
  assert.ok(contentGraphEvidence.nodeCount > 0)
  assert.ok(contentGraphEvidence.featureCount >= 0)
  assert.ok(contentGraphEvidence.exportPlanCount >= 1)
  assert.ok(Number.isInteger(contentGraphEvidence.hytaleBlockerCount))
  assert.equal(contentGraphEvidence.modules[0].moduleId, 'echosample')

  const neoforgeEntries = jarEntries(path.join(outputDir, 'echosample-1.2.3-neoforge.jar'))
  assert.ok(neoforgeEntries.has('META-INF/echo.mod.json'))
  assert.ok(neoforgeEntries.has('META-INF/neoforge.mods.toml'))
  assert.ok(neoforgeEntries.has('dev/echo/sample/Compiled.class'))
  assert.ok(neoforgeEntries.has('.echo/content-graph/content-graph.json'))
  assert.ok(neoforgeEntries.has('.echo/content-graph/features.json'))
  assert.ok(neoforgeEntries.has('.echo/content-graph/export-plans/hytale.json'))
  const neoforgeToml = await readJarEntry(path.join(outputDir, 'echosample-1.2.3-neoforge.jar'), 'META-INF/neoforge.mods.toml')
  assert.match(neoforgeToml, /modId="echosample"/u)
  assert.match(neoforgeToml, /versionRange="\[26\.1\.2,26\.2\)"/u)
  assert.doesNotMatch(neoforgeToml, /\$\{/u)

  const standaloneEntries = jarEntries(path.join(outputDir, 'echosample-1.2.3-standalone.jar'))
  assert.ok(standaloneEntries.has('META-INF/echo.mod.json'))
  assert.ok(standaloneEntries.has('dev/echo/sample/Compiled.class'))
  assert.ok(standaloneEntries.has('.echo/content-graph/content-graph.json'))
  assert.ok(standaloneEntries.has('.echo/content-graph/features.json'))
  assert.ok(standaloneEntries.has('.echo/content-graph/export-plans/hytale.json'))

  const addonPath = path.join(outputDir, 'echosample-1.2.3.echo-addon')
  const addonEntries = jarEntries(addonPath)
  assert.ok(addonEntries.has('META-INF/echo.mod.json'))
  assert.ok(addonEntries.has('echo-addon-package.json'))
  assert.ok(addonEntries.has('checksums.sha256'))
  assert.ok(addonEntries.has('lib/echosample-1.2.3-runtime.jar'))
  assert.ok(addonEntries.has('.echo/content-graph/content-graph.json'))
  assert.ok(addonEntries.has('.echo/content-graph/features.json'))
  assert.ok(addonEntries.has('.echo/content-graph/export-plans/hytale.json'))
  const addonChecksums = await readJarEntry(addonPath, 'checksums.sha256')
  assert.match(addonChecksums, /META-INF\/echo\.mod\.json/u)
  assert.match(addonChecksums, /echo-addon-package\.json/u)
  assert.match(addonChecksums, /lib\/echosample-1\.2\.3-runtime\.jar/u)
  assert.match(addonChecksums, /\.echo\/content-graph\/content-graph\.json/u)
})

test('fails by default when runtime jars are missing', async () => {
  const repoRoot = await makeRepo()
  await fs.rm(path.join(repoRoot, 'addons', 'echosample', 'build'), { recursive: true, force: true })

  await assert.rejects(
    () => generateModuleRelease({ repoRoot, modules: ['echosample'] }),
    /missing neoforge runtime jar, standalone runtime jar, echo-addon runtime jar/u,
  )
})

test('can emit runtime-named archives from source when build outputs are absent', async () => {
  const repoRoot = await makeRepo()
  await fs.rm(path.join(repoRoot, 'addons', 'echosample', 'build'), { recursive: true, force: true })

  const release = await generateModuleRelease({
    repoRoot,
    releaseId: 'source-packaged-test',
    modules: ['echosample'],
    packageFromSource: true,
  })

  const artifacts = release.modules[0].artifacts
  assert.deepEqual(artifacts.map((artifact) => artifact.filename).sort(), expectedArtifactNames('echosample', '1.2.3'))
  assert.equal(artifacts.find((artifact) => artifact.kind === 'neoforge').buildMode, 'source-packaged')
  assert.equal(artifacts.find((artifact) => artifact.kind === 'standalone').buildMode, 'source-packaged')
  assert.equal(artifacts.find((artifact) => artifact.kind === 'content-graph').buildMode, 'generated')
  const echoAddonArtifact = artifacts.find((artifact) => artifact.kind === 'echo-addon')
  assert.equal(echoAddonArtifact.buildMode, 'source-packaged')
  assert.ok(echoAddonArtifact.contains.includes('checksums.sha256'))
  assert.ok(echoAddonArtifact.contains.includes('.echo/content-graph/content-graph.json'))
})

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
  await fs.mkdir(path.join(moduleDir, 'src/main/resources/data/echosample/echosample/gear'), { recursive: true })
  await fs.mkdir(path.join(moduleDir, 'src/main/resources/data/echosample/echosample/station_recipes'), { recursive: true })
  await fs.mkdir(path.join(moduleDir, 'src/main/resources/data/echosample/echo_native'), { recursive: true })
  await fs.mkdir(path.join(moduleDir, 'src/main/resources/assets/echosample/eui'), { recursive: true })
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
  await fs.writeFile(path.join(moduleDir, 'src/main/resources/data/echosample/echosample/gear/sample_blade.json'), JSON.stringify({
    id: 'sample_blade',
    displayName: 'Sample Blade',
    tags: ['fixture_gear'],
  }, null, 2))
  await fs.writeFile(path.join(moduleDir, 'src/main/resources/data/echosample/echosample/station_recipes/bench_tune.json'), JSON.stringify({
    title: 'Bench Tune',
    station: 'sample_bench',
    inputs: [{ item: 'echosample:sample_blade', count: 1 }],
    outputs: [{ item: 'echosample:tuned_blade', count: 1 }],
  }, null, 2))
  await fs.writeFile(path.join(moduleDir, 'src/main/resources/assets/echosample/eui/eui_manifest.json'), JSON.stringify({
    pages: [
      {
        id: 'echosample:index_items',
        title: 'Sample Items',
        intent: 'index_page',
        documentedNodes: ['echosample:sample_blade'],
        actions: [{ id: 'inspect', label: 'Inspect' }],
      },
    ],
  }, null, 2))
  await fs.writeFile(path.join(moduleDir, 'src/main/resources/data/echosample/echo_native/player_surfaces.json'), JSON.stringify({
    schemaVersion: 'echo.native.player_surface_manifest.v1',
    ownerModule: 'echosample',
    requiredHostServices: ['echo.native.screens', 'echo.adaptercore.gameplay'],
    hostTargets: ['neoforge', 'echo_native', 'echo_runtime_standalone', 'standalone_engine'],
    surfaces: [
      {
        id: 'echosample:surface/sample_inventory',
        title: 'Sample Inventory',
        intent: 'inventory_surface',
        surface: 'inventory',
        contract: 'echo.inventory.surface.v1',
        requiredHostServices: ['echo.inventory.surface', 'echo.adaptercore.gameplay'],
        gameplayActions: ['echosample:action/sample_use'],
        actions: [{ id: 'use', label: 'Use', action: 'echosample:action/sample_use' }],
        controlledNodes: ['echosample:sample_blade'],
      },
    ],
  }, null, 2))
  await writeFixtureRuntimeJar(path.join(moduleDir, 'build/libs/echosample-1.2.3.jar'))
  await writeNeoForgeConformanceSourceReports(root)
  return root
}

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function writeNeoForgeConformanceSourceReports(root) {
  await writeJson(path.join(root, 'reports/runtime-parity/neoforge-runtime-evidence.json'), {
    schema: 'echo.neoforge.runtime_evidence.v1',
    generatedAt: '1970-01-01T00:00:00Z',
    status: 'PASS',
    runtime: 'neoforge',
    evidenceKind: 'compiled-source-resource-gametest-contract',
    moduleIds: ['echosample'],
    loadedModuleIds: ['echosample'],
    visibleRoutes: ['echosample:index_items'],
    blockers: [],
  })
  await writeJson(path.join(root, 'reports/runtime-parity/neoforge-client-ui-results.json'), {
    schema: 'echo.neoforge.client_ui_results.v1',
    status: 'PARTIAL',
    evidenceKind: 'ui-source-contract-not-live-client-route',
    moduleCount: 1,
    moduleIds: ['echosample'],
    blockers: ['missing executed NeoForge strict-play input: reports/neoforge-strict-play/neoforge-client-ui-results.json'],
  })
  await writeJson(path.join(root, 'reports/runtime-parity/neoforge-registry-content-results.json'), {
    schema: 'echo.neoforge.registry_content_results.v1',
    status: 'PARTIAL',
    evidenceKind: 'registry-source-contract-not-runtime-registry-dump',
    moduleCount: 1,
    moduleIds: ['echosample'],
    blockers: ['Registry/content source signals exist, but no runtime NeoForge registry/datapack/worldgen dump was ingested.'],
  })
  await writeJson(path.join(root, 'reports/runtime-parity/neoforge-module-gametest-results.json'), {
    schema: 'echo.neoforge.gametest_results.v1',
    status: 'PASS',
    evidenceKind: 'executed-neoforge-gametest-results',
    moduleCount: 1,
    moduleIds: ['echosample'],
    trustedMutations: ['NeoForge GameTest used a sample item.'],
    saveEvidence: ['NeoForge GameTest included save/reload-named tests.'],
    networkEvidence: ['NeoForge GameTest included network/sync-named tests.'],
    blockers: [],
  })
  await writeJson(path.join(root, 'reports/runtime-parity/neoforge-play-evidence.json'), {
    schema: 'echo.neoforge.strict_play_evidence.v1',
    status: 'PARTIAL',
    evidenceKind: 'source-contract-not-live-play',
    moduleCount: 1,
    moduleIds: ['echosample'],
    blockers: ['NeoForge source/compiled contract evidence exists, but no live NeoForge client/server session proof was ingested.'],
  })
  await writeJson(path.join(root, 'reports/runtime-parity/module-feature-contracts.json'), {
    schema: 'echo.module.feature_contracts.v1',
    generatedAt: '1970-01-01T00:00:00Z',
    modules: [{ moduleId: 'echosample', expectedFeatures: ['gui', 'screen', 'items'] }],
  })
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
  assert.equal(release.runtimeConformanceEvidence.length, 1)
  const neoForgeConformance = release.runtimeConformanceEvidence[0]
  assert.equal(neoForgeConformance.kind, 'runtime-conformance')
  assert.equal(neoForgeConformance.filename, 'neoforge-runtime-conformance.json')
  assert.equal(neoForgeConformance.runtimeTarget, 'neoforge')
  assert.equal(neoForgeConformance.hostId, 'neoforge')
  assert.equal(neoForgeConformance.schemaVersion, 'echo.runtime.conformance.v1')
  assert.equal(neoForgeConformance.summary.status, 'warning')
  assert.equal(
    neoForgeConformance.downloadUrl,
    'https://github.com/knoxhack/ECHO-Modules/releases/download/test-release/neoforge-runtime-conformance.json',
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
  await fs.access(path.join(outputDir, '1.2.3', '.echo', 'content-graph', 'export-plans', 'standalone_engine.json'))
  await fs.access(path.join(outputDir, '1.2.3', '.echo', 'content-graph', 'export-plans', 'hytale.json'))
  await fs.access(path.join(repoRoot, 'dist', 'echo-module-release', 'echo-release.json'))
  await fs.access(path.join(repoRoot, 'dist', 'echo-module-release', 'content-graph-evidence.json'))
  await fs.access(path.join(repoRoot, 'dist', 'echo-module-release', 'neoforge-runtime-conformance.json'))
  await fs.access(path.join(repoRoot, 'dist', 'echo-module-release', 'checksums.txt'))

  const contentGraph = JSON.parse(await fs.readFile(path.join(outputDir, 'echosample-1.2.3-content-graph.json'), 'utf8'))
  assert.equal(contentGraph.schemaVersion, 'echo.content_graph.v1')
  assert.equal(contentGraph.moduleId, 'echosample')
  assert.ok(Array.isArray(contentGraph.nodes))
  assert.ok(Array.isArray(contentGraph.edges))
  const contentGraphNodeIds = new Set(contentGraph.nodes.map((node) => node.id))
  assert.ok(contentGraphNodeIds.has('echosample:sample_blade'))
  assert.ok(contentGraphNodeIds.has('echosample:bench_tune'))
  assert.ok(contentGraphNodeIds.has('echosample:index_items'))
  assert.ok(contentGraphNodeIds.has('echosample:surface/sample_inventory'))
  assert.ok(contentGraph.edges.some((edge) => edge.kind === 'runtime_host_adapts_surface' && edge.to === 'echosample:index_items'))
  assert.ok(contentGraph.edges.some((edge) => edge.kind === 'index_page_documents_node' && edge.from === 'echosample:index_items' && edge.to === 'echosample:sample_blade'))
  assert.ok(contentGraph.edges.some((edge) => edge.kind === 'inventory_action_invokes_gameplay_action' && edge.from === 'echosample:surface/sample_inventory' && edge.to === 'echosample:action/sample_use'))
  const features = JSON.parse(await fs.readFile(path.join(outputDir, '1.2.3', '.echo', 'content-graph', 'features.json'), 'utf8'))
  assert.ok(features.features.some((feature) => feature.id === 'echosample:feature/surface/sample_inventory'))
  const contentGraphEvidence = JSON.parse(await fs.readFile(path.join(repoRoot, 'dist', 'echo-module-release', 'content-graph-evidence.json'), 'utf8'))
  assert.equal(contentGraphEvidence.schemaVersion, 'echo.content_graph.evidence.v1')
  assert.equal(contentGraphEvidence.graphCount, 1)
  assert.equal(contentGraphEvidence.moduleCount, 1)
  assert.ok(contentGraphEvidence.nodeCount > 0)
  assert.ok(contentGraphEvidence.featureCount >= 0)
  assert.ok(contentGraphEvidence.exportPlanCount >= 1)
  assert.ok(Number.isInteger(contentGraphEvidence.hytaleBlockerCount))
  assert.equal(contentGraphEvidence.modules[0].moduleId, 'echosample')
  const runtimeConformance = JSON.parse(await fs.readFile(path.join(repoRoot, 'dist', 'echo-module-release', 'neoforge-runtime-conformance.json'), 'utf8'))
  assert.equal(runtimeConformance.schemaVersion, 'echo.runtime.conformance.v1')
  assert.equal(runtimeConformance.hostId, 'neoforge')
  assert.equal(runtimeConformance.summary.status, 'warning')
  assert.ok(runtimeConformance.surfaceResults.some((row) => row.id === 'echoindex:surface/inventory'))

  const neoforgeEntries = jarEntries(path.join(outputDir, 'echosample-1.2.3-neoforge.jar'))
  assert.ok(neoforgeEntries.has('META-INF/echo.mod.json'))
  assert.ok(neoforgeEntries.has('META-INF/neoforge.mods.toml'))
  assert.ok(neoforgeEntries.has('dev/echo/sample/Compiled.class'))
  assert.ok(neoforgeEntries.has('.echo/content-graph/content-graph.json'))
  assert.ok(neoforgeEntries.has('.echo/content-graph/features.json'))
  assert.ok(neoforgeEntries.has('.echo/content-graph/export-plans/standalone_engine.json'))
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
  assert.ok(standaloneEntries.has('.echo/content-graph/export-plans/standalone_engine.json'))
  assert.ok(standaloneEntries.has('.echo/content-graph/export-plans/hytale.json'))

  const addonPath = path.join(outputDir, 'echosample-1.2.3.echo-addon')
  const addonEntries = jarEntries(addonPath)
  assert.ok(addonEntries.has('META-INF/echo.mod.json'))
  assert.ok(addonEntries.has('echo-addon-package.json'))
  assert.ok(addonEntries.has('checksums.sha256'))
  assert.ok(addonEntries.has('lib/echosample-1.2.3-runtime.jar'))
  assert.ok(addonEntries.has('.echo/content-graph/content-graph.json'))
  assert.ok(addonEntries.has('.echo/content-graph/features.json'))
  assert.ok(addonEntries.has('.echo/content-graph/export-plans/standalone_engine.json'))
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

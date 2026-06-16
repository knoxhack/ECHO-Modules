import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import assert from 'node:assert/strict'
import { generateRuntimeParityAudit } from './generate-runtime-parity-audit.mjs'

async function writeModule(repoRoot, directory, descriptor, sources = {}, resources = {}) {
  const moduleRoot = path.join(repoRoot, 'addons', directory)
  const descriptorPath = path.join(moduleRoot, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
  await fs.mkdir(path.dirname(descriptorPath), { recursive: true })
  await fs.writeFile(descriptorPath, `${JSON.stringify(descriptor, null, 2)}\n`, 'utf8')
  for (const [className, source] of Object.entries(sources)) {
    const sourcePath = path.join(moduleRoot, 'src', 'main', 'java', `${className.replace(/\./g, path.sep)}.java`)
    await fs.mkdir(path.dirname(sourcePath), { recursive: true })
    await fs.writeFile(sourcePath, source, 'utf8')
  }
  for (const [relativePath, content] of Object.entries(resources)) {
    const resourcePath = path.join(moduleRoot, 'src', 'main', 'resources', relativePath)
    await fs.mkdir(path.dirname(resourcePath), { recursive: true })
    await fs.writeFile(resourcePath, content, 'utf8')
  }
}

async function writePackManifest(echoRoot, repoName, moduleIds) {
  const repoRoot = path.join(echoRoot, repoName)
  await fs.mkdir(repoRoot, { recursive: true })
  await fs.writeFile(
    path.join(repoRoot, 'release-manifest.template.json'),
    `${JSON.stringify({
      schema: 'echo.pack.release_manifest.v1',
      moduleArtifactFamily: 'echo-addon',
      moduleRequirements: moduleIds.map((id) => ({ id })),
    }, null, 2)}\n`,
    'utf8',
  )
}

async function writeSdkContentGraphEvidenceSchema(echoRoot) {
  const schemaPath = path.join(echoRoot, 'ECHO-SDK', 'schemas', 'content-graph-evidence.schema.json')
  await fs.mkdir(path.dirname(schemaPath), { recursive: true })
  await fs.writeFile(
    schemaPath,
    `${JSON.stringify({
      $schema: 'https://json-schema.org/draft/2020-12/schema',
      title: 'Fixture Content Graph Evidence',
      type: 'object',
      additionalProperties: true,
      required: ['schemaVersion', 'modules'],
      properties: {
        schemaVersion: { const: 'echo.content_graph.evidence.v1' },
        modules: { type: 'array' },
      },
    }, null, 2)}\n`,
    'utf8',
  )
}

const echoRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-runtime-parity-audit-'))
const repoRoot = path.join(echoRoot, 'ECHO-Modules')

try {
  await fs.mkdir(path.join(repoRoot, 'docs'), { recursive: true })
  await fs.writeFile(
    path.join(repoRoot, 'docs', 'module-docs-index.md'),
    '# Module Docs\n\n- [echocore](../addons/echocore/README.md)\n',
    'utf8',
  )

  await writeModule(repoRoot, 'echocore', {
    schema: 'echo.mod.v1',
    id: 'echocore',
    name: 'ECHO Core',
    version: '1.0.0',
    kind: 'library',
    role: 'foundation',
    official: true,
    standalone: true,
    entrypoint: 'com.example.EchoCore',
    access: {
      adapterCore: { runtimes: ['neoforge', 'echo_native', 'echo_runtime_standalone'] },
      nativeEntrypoint: 'com.example.EchoCoreNativeModule',
    },
  }, {
    'com.example.EchoCore': 'package com.example; public final class EchoCore {}\n',
    'com.example.EchoCoreNativeModule': 'package com.example; public final class EchoCoreNativeModule {}\n',
  })

  await writeModule(repoRoot, 'echoindex', {
    schema: 'echo.mod.v1',
    id: 'echoindex',
    name: 'ECHO Index',
    version: '1.0.0',
    kind: 'ui_pack',
    role: 'inventory_overlay',
    official: true,
    standalone: true,
    entrypoint: 'com.example.EchoIndex',
    provides: ['index.inventory', 'ui.screens', 'recipes'],
    access: {
      adapterCore: {
        runtimes: ['neoforge', 'echo_native', 'echo_runtime_standalone'],
        domains: ['ui', 'items', 'recipes'],
      },
      nativeEntrypoint: 'com.example.EchoIndexNativeModule',
    },
  }, {
    'com.example.EchoIndex': 'package com.example; public final class EchoIndex {}\n',
    'com.example.EchoIndexNativeModule': [
      'package com.example;',
      'public final class EchoIndexNativeModule {',
      '  void describeNativeSurfaces() {}',
      '  String nativeSurfaceImplementationClass = "com.example.IndexSurface";',
      '  void ensureNativeClientRoutesRegisteredForNativeLoader() {}',
      '}',
      '',
    ].join('\n'),
  }, {
    'assets/echoindex/models/item/index_tablet.json': '{"parent":"item/generated"}\n',
  })

  await writePackManifest(echoRoot, 'ECHO-Ashfall-Native-Edition', ['echocore'])
  await writeSdkContentGraphEvidenceSchema(echoRoot)

  const { report, paths } = await generateRuntimeParityAudit({ repoRoot, echoRoot })

  assert.equal(report.schema, 'echo.module.runtime_parity_audit.v1')
  assert.equal(report.summary.moduleCount, 2)
  assert.equal(report.summary.runtimeRowCount, 6)
  assert.equal(report.summary.preferredPackManifestCount, 1)
  assert.equal(report.strictWouldFail, true)
  assert.equal(report.docsIndex.found, true)
  assert.deepEqual(report.docsIndex.missingModuleIds, ['echoindex'])
  assert.deepEqual(report.docsIndex.missingDirectories, ['echoindex'])

  const indexModule = report.modules.find((module) => module.moduleId === 'echoindex')
  assert.ok(indexModule.expectedFeatures.includes('inventory_overlay'))
  assert.ok(indexModule.expectedFeatures.includes('index'))
  assert.ok(indexModule.expectedFeatures.includes('creative_tab'))
  assert.deepEqual(indexModule.expectedCreativeTabs[0].expectedEntries, ['echoindex:index_tablet'])

  const nativeIndexRow = report.rows.find((row) => row.moduleId === 'echoindex' && row.runtime === 'echo_native')
  assert.equal(nativeIndexRow.uiSurfaceStatus, 'registered-headless')
  assert.equal(nativeIndexRow.actionRouteStatus, 'registered')
  assert.equal(nativeIndexRow.creativeTabStatus, 'declared-only')
  assert.deepEqual(nativeIndexRow.missingCreativeTabEntries, ['echoindex:index_tablet'])
  assert.deepEqual(nativeIndexRow.missingCreativeSearchEntries, ['echoindex:index_tablet'])
  assert.ok(nativeIndexRow.blockers.some((blocker) => blocker.includes('visible/actionable proof')))
  assert.ok(nativeIndexRow.blockers.some((blocker) => blocker.includes('creative tab content')))

  const pack = report.packAudit.preferredManifests[0]
  assert.equal(pack.repo, 'ECHO-Ashfall-Native-Edition')
  assert.ok(pack.missingVisibleCoreSurfaceModules.includes('echoindex'))
  assert.ok(pack.missingContentBaselineModules.includes('echoblockworks'))

  assert.ok(report.backlog.some((item) => item.priority === 'P0' && item.ownerRepo === 'ECHO-Native-Platform'))
  assert.ok(report.backlog.some((item) =>
    item.priority === 'P0'
    && item.ownerRepo === 'ECHO-Native-Platform'
    && item.subsystem === 'creative inventory parity'
    && item.runtimes.includes('echo_native')
    && item.modules.includes('echoindex')))
  assert.ok(report.backlogGroups.some((group) =>
    group.priority === 'P0'
    && group.ownerRepos.some((owner) =>
      owner.ownerRepo === 'ECHO-Native-Platform'
      && owner.runtimes.includes('echo_native')
      && owner.modules.includes('echoindex'))))
  assert.ok(report.backlog.some((item) => item.priority === 'P1' && item.subsystem === 'docs index'))

  assert.equal(JSON.parse(await fs.readFile(paths.json, 'utf8')).summary.runtimeRowCount, 6)
  assert.match(await fs.readFile(paths.markdown, 'utf8'), /ECHO Module Runtime Parity Audit/)
  assert.match(await fs.readFile(paths.backlog, 'utf8'), /ECHO Runtime Parity Fix Backlog/)

  const strictPlayRun = await generateRuntimeParityAudit({ repoRoot, echoRoot, strictPlay: true })
  assert.ok(strictPlayRun.play.playAudit.strictPlayWouldFail)
  assert.equal(strictPlayRun.play.playAudit.summary.runtimeRowCount, 6)
  assert.equal(strictPlayRun.play.playAudit.summary.resultCounts.pass, 0)
  assert.equal(
    strictPlayRun.play.playAudit.summary.resultCounts.partial + strictPlayRun.play.playAudit.summary.resultCounts.fail,
    6,
  )
  assert.ok(strictPlayRun.play.evidenceManifest.summary.expectedRuntimeEvidenceCount >= 15)
  assert.ok(strictPlayRun.play.evidenceManifest.summary.foundRuntimeEvidenceCount >= 4)
  assert.equal(strictPlayRun.play.evidenceManifest.summary.passingRuntimeEvidenceCount, 0)
  const playEvidenceEntry = strictPlayRun.play.evidenceManifest.runtimeEvidence.find((entry) => entry.key === 'neoforgePlayEvidence')
  assert.ok(playEvidenceEntry)
  assert.deepEqual(playEvidenceEntry.expectedModuleIds, ['echocore', 'echoindex'])
  assert.equal(playEvidenceEntry.expectedModuleCount, 2)
  assert.equal(
    playEvidenceEntry.missingModuleCount,
    playEvidenceEntry.expectedModuleCount - playEvidenceEntry.coveredModuleCount,
  )
  assert.equal(strictPlayRun.play.manualAcceptanceMatrix.summary.packLaneCount, 1)
  assert.equal(strictPlayRun.play.manualAcceptanceMatrix.summary.resultCounts.fail, 1)
  assert.equal(strictPlayRun.play.modulePlayCompletion.summary.moduleCount, 2)
  assert.equal(strictPlayRun.play.modulePlayCompletion.summary.incomplete, 2)
  assert.equal(JSON.parse(await fs.readFile(strictPlayRun.paths.playAuditJson, 'utf8')).schema, 'echo.module.runtime_play_audit.v1')
  assert.equal(JSON.parse(await fs.readFile(strictPlayRun.paths.evidenceManifest, 'utf8')).schema, 'echo.module.runtime_play_evidence_manifest.v1')
  assert.equal(JSON.parse(await fs.readFile(strictPlayRun.paths.manualAcceptanceMatrix, 'utf8')).schema, 'echo.module.manual_acceptance_matrix.v1')
  assert.equal(JSON.parse(await fs.readFile(strictPlayRun.paths.modulePlayCompletion, 'utf8')).schema, 'echo.module.play_completion.v1')
  const playBacklog = JSON.parse(await fs.readFile(strictPlayRun.paths.playFixBacklogJson, 'utf8'))
  assert.equal(playBacklog.schema, 'echo.module.runtime_play_fix_backlog.v1')
  assert.ok(playBacklog.summary.itemCount > 0)
  assert.ok(playBacklog.items.some((item) => item.category === 'pack_acceptance'))
  assert.ok(playBacklog.items.some((item) => item.category === 'runtime_evidence' && Array.isArray(item.missingModuleIds)))
  assert.match(await fs.readFile(strictPlayRun.paths.playAuditMarkdown, 'utf8'), /ECHO Module Runtime Play Audit/)
  assert.match(await fs.readFile(strictPlayRun.paths.playFixBacklogMarkdown, 'utf8'), /ECHO Runtime Play Fix Backlog/)

  await writeModule(repoRoot, 'echobadentity', {
    schema: 'echo.mod.v1',
    id: 'echobadentity',
    name: 'ECHO Bad Entity',
    version: '1.0.0',
    kind: 'content',
    role: 'entity_fixture',
    official: true,
    standalone: true,
    entrypoint: 'com.example.EchoBadEntity',
    access: {
      adapterCore: { runtimes: ['neoforge', 'echo_native', 'echo_runtime_standalone'] },
      nativeEntrypoint: 'com.example.EchoBadEntityNativeModule',
    },
  }, {
    'com.example.EchoBadEntity': 'package com.example; public final class EchoBadEntity {}\n',
    'com.example.EchoBadEntityNativeModule': 'package com.example; public final class EchoBadEntityNativeModule {}\n',
  }, {
    'data/echobadentity/creatures/bad_entity.json': `${JSON.stringify({
      schema: 'echo.creature.v1',
      creatures: [
        {
          id: 'bad_entity',
          displayName: 'Bad Entity',
          neoforgeClass: 'net.minecraft.world.entity.Mob',
        },
      ],
    }, null, 2)}\n`,
  })

  const strictFullRun = await generateRuntimeParityAudit({ repoRoot, echoRoot, strictFull: true })
  assert.equal(strictFullRun.report.contentGraphStrictAudit.passed, false)
  assert.ok(strictFullRun.report.contentGraphStrictAudit.errors.some((error) =>
    error.includes('echobadentity: node echobadentity:bad_entity contains runtime classes in portable fields')))
  const badEntityRows = strictFullRun.report.rows.filter((row) => row.moduleId === 'echobadentity')
  assert.equal(badEntityRows.length, 3)
  for (const row of badEntityRows) {
    assert.ok(row.contentGraphStrictBlockers.some((blocker) => blocker.includes('runtime classes in portable fields')))
    assert.ok(row.strictFullBlockers.some((blocker) => blocker.includes('Content Graph strict evidence')))
  }
  assert.match(await fs.readFile(strictFullRun.paths.markdown, 'utf8'), /Content Graph Strict Evidence/)
} finally {
  await fs.rm(echoRoot, { recursive: true, force: true })
}

console.log('generate-runtime-parity-audit tests passed')

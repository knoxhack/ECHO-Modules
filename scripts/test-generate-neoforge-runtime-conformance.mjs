#!/usr/bin/env node
import assert from 'node:assert/strict'
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { generateNeoForgeRuntimeConformance } from './generate-neoforge-runtime-conformance.mjs'

async function writeJson(root, relativePath, value) {
  const target = path.join(root, relativePath)
  await fs.mkdir(path.dirname(target), { recursive: true })
  await fs.writeFile(target, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function readJson(root, relativePath) {
  return JSON.parse(await fs.readFile(path.join(root, relativePath), 'utf8'))
}

const repoRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-neoforge-runtime-conformance-'))

try {
  await writeJson(repoRoot, 'reports/runtime-parity/neoforge-runtime-evidence.json', {
    schema: 'echo.neoforge.runtime_evidence.v1',
    generatedAt: '1970-01-01T00:00:00Z',
    status: 'PASS',
    runtime: 'neoforge',
    evidenceKind: 'compiled-source-resource-gametest-contract',
    moduleIds: ['echoscreencore', 'echohudcore', 'echoindex', 'echoterminal', 'echoadaptercore'],
    loadedModuleIds: ['echoscreencore', 'echohudcore', 'echoindex', 'echoterminal', 'echoadaptercore'],
    visibleRoutes: ['echoindex:index', 'echoterminal:terminal'],
    blockers: [],
  })
  await writeJson(repoRoot, 'reports/runtime-parity/neoforge-client-ui-results.json', {
    schema: 'echo.neoforge.client_ui_results.v1',
    status: 'PARTIAL',
    evidenceKind: 'ui-source-contract-not-live-client-route',
    moduleCount: 4,
    moduleIds: ['echoscreencore', 'echohudcore', 'echoindex', 'echoterminal'],
    blockers: ['missing executed NeoForge strict-play input: reports/neoforge-strict-play/neoforge-client-ui-results.json'],
  })
  await writeJson(repoRoot, 'reports/runtime-parity/neoforge-registry-content-results.json', {
    schema: 'echo.neoforge.registry_content_results.v1',
    status: 'PARTIAL',
    evidenceKind: 'registry-source-contract-not-runtime-registry-dump',
    moduleCount: 3,
    moduleIds: ['echoindex', 'echoadaptercore', 'echoterminal'],
    blockers: ['Registry/content source signals exist, but no runtime NeoForge registry/datapack/worldgen dump was ingested.'],
  })
  await writeJson(repoRoot, 'reports/runtime-parity/neoforge-module-gametest-results.json', {
    schema: 'echo.neoforge.gametest_results.v1',
    status: 'PASS',
    evidenceKind: 'executed-neoforge-gametest-results',
    moduleCount: 2,
    moduleIds: ['echoindex', 'echoadaptercore'],
    trustedMutations: ['NeoForge GameTest placed and used an Index-owned item.'],
    saveEvidence: ['NeoForge GameTest included save/reload-named tests.'],
    networkEvidence: ['NeoForge GameTest included network/sync-named tests.'],
    blockers: [],
  })
  await writeJson(repoRoot, 'reports/runtime-parity/neoforge-play-evidence.json', {
    schema: 'echo.neoforge.strict_play_evidence.v1',
    status: 'PARTIAL',
    evidenceKind: 'source-contract-not-live-play',
    moduleCount: 5,
    moduleIds: ['echoscreencore', 'echohudcore', 'echoindex', 'echoterminal', 'echoadaptercore'],
    blockers: ['NeoForge source/compiled contract evidence exists, but no live NeoForge client/server session proof was ingested.'],
  })
  await writeJson(repoRoot, 'reports/runtime-parity/module-feature-contracts.json', {
    schema: 'echo.module.feature_contracts.v1',
    generatedAt: '1970-01-01T00:00:00Z',
    modules: [],
  })
  await writeJson(repoRoot, 'dist/echo-module-release/content-graph-evidence.json', {
    schemaVersion: 'echo.content_graph.evidence.v1',
    generatedAt: '1970-01-01T00:00:00Z',
    graphCount: 5,
    moduleCount: 5,
    nodeCount: 50,
    edgeCount: 60,
    featureCount: 10,
    exportPlanCount: 20,
    unresolvedReferenceCount: 0,
    validationState: 'valid',
    modules: [],
    diagnostics: [],
  })

  const { report } = await generateNeoForgeRuntimeConformance({ repoRoot })
  assert.equal(report.schemaVersion, 'echo.runtime.conformance.v1')
  assert.equal(report.hostId, 'neoforge')
  assert.equal(report.summary.status, 'warning')
  assert.equal(report.summary.blocked, 0)
  assert.ok(report.summary.adapted > 0)
  assert.ok(report.summary.fallback > 0)

  const title = report.surfaceResults.find((row) => row.id === 'echoscreencore:surface/title_menu')
  assert.equal(title.status, 'fallback')
  assert.match(title.reason, /missing executed NeoForge strict-play input/u)

  const diagnostics = report.surfaceResults.find((row) => row.id === 'echoscreencore:surface/module_diagnostics')
  assert.equal(diagnostics.status, 'adapted')

  const mutation = report.actionResults.find((row) => row.id === 'echoadaptercore:action/world_place_block')
  assert.equal(mutation.status, 'adapted')

  const registry = report.actionResults.find((row) => row.id === 'echoadaptercore:action/registry_content_load')
  assert.equal(registry.status, 'fallback')

  const written = await readJson(repoRoot, 'reports/runtime-parity/neoforge-runtime-conformance.json')
  assert.equal(written.moduleGraphFingerprint, report.moduleGraphFingerprint)
} finally {
  await fs.rm(repoRoot, { recursive: true, force: true })
}

console.log('generate-neoforge-runtime-conformance tests passed')

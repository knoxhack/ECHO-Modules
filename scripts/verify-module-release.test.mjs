import assert from 'node:assert/strict'
import { test } from 'node:test'
import { assertContentGraphEvidenceDocument } from './verify-module-release.mjs'

function release(...moduleIds) {
  return {
    modules: moduleIds.map((moduleId) => ({ moduleId })),
  }
}

function evidence(...moduleIds) {
  return {
    schemaVersion: 'echo.content_graph.evidence.v1',
    graphCount: moduleIds.length,
    moduleCount: moduleIds.length,
    nodeCount: Math.max(1, moduleIds.length),
    edgeCount: 0,
    featureCount: 0,
    exportPlanCount: moduleIds.length,
    hytaleBlockerCount: 0,
    modules: moduleIds.map((moduleId) => ({ moduleId })),
    diagnostics: [],
  }
}

test('accepts repository-wide content graph evidence as a superset of targeted release modules', () => {
  assert.doesNotThrow(() => assertContentGraphEvidenceDocument(
    evidence('echoadaptercore', 'echoashfallprotocol', 'echocore'),
    release('echoadaptercore', 'echoashfallprotocol'),
  ))
})

test('rejects superset evidence that omits a targeted release module', () => {
  assert.throws(
    () => assertContentGraphEvidenceDocument(
      evidence('echoadaptercore', 'echocore'),
      release('echoadaptercore', 'echoashfallprotocol'),
    ),
    /content graph evidence missing release module\(s\): echoashfallprotocol/u,
  )
})

test('rejects evidence with inconsistent graph and module counts', () => {
  const document = evidence('echoadaptercore', 'echoashfallprotocol')
  document.graphCount = 133

  assert.throws(
    () => assertContentGraphEvidenceDocument(document, release('echoadaptercore', 'echoashfallprotocol')),
    /content graph evidence graphCount must match evidence modules/u,
  )
})

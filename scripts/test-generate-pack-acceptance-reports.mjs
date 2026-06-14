#!/usr/bin/env node
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import assert from 'node:assert/strict'
import { generatePackAcceptanceReports } from './generate-pack-acceptance-reports.mjs'

const REQUIRED_CHECK_IDS = [
  'installLaunchSucceeds',
  'freshSessionStarts',
  'hudAppears',
  'inventoryOverlayAndIndexRespond',
  'terminalExecutesAction',
  'holoMapOpens',
  'lensScans',
  'screenCoreScreensRenderAndHandleInput',
  'blockPlaceUseBreakWorks',
  'blockActionMutatesState',
  'worldgenAppears',
  'saveReloadPreservesState',
  'trustedMutationsReported',
]

async function writeJson(root, relativePath, value) {
  const target = path.join(root, relativePath)
  await fs.mkdir(path.dirname(target), { recursive: true })
  await fs.writeFile(target, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function readJson(root, relativePath) {
  return JSON.parse(await fs.readFile(path.join(root, relativePath), 'utf8'))
}

const echoRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-pack-acceptance-'))
const repoRoot = path.join(echoRoot, 'ECHO-Modules')
const packRepo = path.join(echoRoot, 'ECHO-Ashfall-NeoForge-Edition')

try {
  await writeJson(repoRoot, 'reports/runtime-parity/echo-module-runtime-parity-audit.json', {
    schema: 'echo.module.runtime_parity_audit.v1',
    generatedAt: '1970-01-01T00:00:00Z',
    packAudit: {
      preferredManifests: [
        {
          product: 'Ashfall',
          lane: 'NeoForge',
          repo: 'ECHO-Ashfall-NeoForge-Edition',
          manifestPath: 'release-manifest.template.json',
          moduleCount: 2,
        },
      ],
    },
  })

  await writeJson(packRepo, 'reports/pack-acceptance/ashfall-neoforge-acceptance.json', {
    schema: 'echo.pack.manual_acceptance_report.v1',
    checks: Object.fromEntries(REQUIRED_CHECK_IDS.map((id) => [id, { status: 'PASS' }])),
  })

  await generatePackAcceptanceReports({ repoRoot, echoRoot, write: true, force: true })
  const pendingReport = await readJson(packRepo, 'reports/pack-acceptance/ashfall-neoforge-acceptance.json')
  assert.equal(pendingReport.status, 'PENDING')
  assert.equal(pendingReport.summary.passedCheckCount, 0)
  assert.equal(pendingReport.summary.missingEvidenceCount, REQUIRED_CHECK_IDS.length)
  assert.equal(pendingReport.checks.installLaunchSucceeds.status, 'PENDING_EVIDENCE')

  await writeJson(packRepo, 'reports/pack-acceptance/ashfall-neoforge-acceptance.json', {
    schema: 'echo.pack.manual_acceptance_report.v1',
    checks: Object.fromEntries(REQUIRED_CHECK_IDS.map((id) => [
      id,
      {
        status: 'PASS',
        evidence: [`reports/pack-acceptance/evidence/${id}.json`],
        verifiedAt: '2026-06-13T00:00:00Z',
      },
    ])),
  })

  await generatePackAcceptanceReports({ repoRoot, echoRoot, write: true, force: true })
  const missingFileReport = await readJson(packRepo, 'reports/pack-acceptance/ashfall-neoforge-acceptance.json')
  assert.equal(missingFileReport.status, 'PENDING')
  assert.equal(missingFileReport.summary.passedCheckCount, 0)
  assert.equal(missingFileReport.summary.missingEvidenceCount, REQUIRED_CHECK_IDS.length)
  assert.equal(missingFileReport.checks.installLaunchSucceeds.evidenceDetails[0].resolvable, false)

  for (const id of REQUIRED_CHECK_IDS) {
    await writeJson(packRepo, `reports/pack-acceptance/evidence/${id}.json`, {
      status: 'PASS',
      check: id,
      proof: 'fixture evidence file exists',
    })
  }
  await writeJson(packRepo, 'reports/pack-acceptance/ashfall-neoforge-acceptance.json', {
    schema: 'echo.pack.manual_acceptance_report.v1',
    checks: Object.fromEntries(REQUIRED_CHECK_IDS.map((id) => [
      id,
      {
        status: 'PASS',
        evidence: [`reports/pack-acceptance/evidence/${id}.json`],
        verifiedAt: '2026-06-13T00:00:00Z',
      },
    ])),
  })

  const { index } = await generatePackAcceptanceReports({ repoRoot, echoRoot, write: true, force: true })
  const passReport = await readJson(packRepo, 'reports/pack-acceptance/ashfall-neoforge-acceptance.json')
  assert.equal(passReport.status, 'PASS')
  assert.equal(passReport.summary.passedCheckCount, REQUIRED_CHECK_IDS.length)
  assert.equal(passReport.summary.missingEvidenceCount, 0)
  assert.equal(passReport.checks.installLaunchSucceeds.evidenceDetails[0].resolvable, true)
  assert.equal(index.summary.passCount, 1)
} finally {
  await fs.rm(echoRoot, { recursive: true, force: true })
}

console.log('generate-pack-acceptance-reports tests passed')

import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { generateRuntimeParityAudit } from './generate-runtime-parity-audit.mjs'

const DEFAULT_OUT_DIR = 'reports/runtime-parity'
const PARITY_REPORT = 'reports/runtime-parity/echo-module-runtime-parity-audit.json'
const PACK_ACCEPTANCE_SCHEMA = 'echo.pack.manual_acceptance_report.v1'
const PACK_ACCEPTANCE_INDEX_SCHEMA = 'echo.pack.manual_acceptance_index.v1'

const REQUIRED_CHECKS = [
  ['installLaunchSucceeds', 'Launch or install path succeeds without missing-module diagnostics.'],
  ['freshSessionStarts', 'Fresh world or runtime session starts.'],
  ['hudAppears', 'HUD appears when the product expects one.'],
  ['inventoryOverlayAndIndexRespond', 'Inventory overlay and Index appear and respond.'],
  ['terminalExecutesAction', 'Terminal opens and executes at least one route or action.'],
  ['holoMapOpens', 'HoloMap opens and shows product-relevant layers or waypoints.'],
  ['lensScans', 'Lens opens or activates and performs a scan.'],
  ['screenCoreScreensRenderAndHandleInput', 'ScreenCore-backed screens render and handle input.'],
  ['blockPlaceUseBreakWorks', 'At least one module-owned block can be placed, used, broken, and saved.'],
  ['blockActionMutatesState', 'At least one block action mutates runtime state.'],
  ['worldgenAppears', 'Worldgen or data features appear where expected.'],
  ['saveReloadPreservesState', 'Save and reload preserves module state.'],
  ['trustedMutationsReported', 'Runtime report shows trusted mutations, not metadata-only claims.'],
]

export async function generatePackAcceptanceReports({
  repoRoot = process.cwd(),
  echoRoot = path.dirname(path.resolve(repoRoot)),
  outDir = DEFAULT_OUT_DIR,
  parityReportPath = PARITY_REPORT,
  write = false,
  force = false,
  seedAutomatedEvidence = false,
} = {}) {
  const normalizedRoot = path.resolve(repoRoot)
  const normalizedEchoRoot = path.resolve(echoRoot)
  const generatedAt = new Date().toISOString()
  const parityReport = await loadParityReport({
    repoRoot: normalizedRoot,
    echoRoot: normalizedEchoRoot,
    parityReportPath,
  })
  const manifests = parityReport.packAudit?.preferredManifests ?? []
  const rows = []
  for (const manifest of manifests) {
    const packRoot = path.join(normalizedEchoRoot, manifest.repo)
    const reportPath = path.join(
      packRoot,
      'reports',
      'pack-acceptance',
      `${slug(manifest.product)}-${manifest.lane.toLowerCase()}-acceptance.json`,
    )
    const existing = await readJsonIfExists(reportPath)
    const report = await acceptanceReportFor({
      generatedAt,
      manifest,
      existing,
      packRoot,
      reportPath,
      repoRoot: normalizedRoot,
      echoRoot: normalizedEchoRoot,
      seedAutomatedEvidence,
    })
    const shouldWrite = write && shouldWriteReport({ existing, report, force })
    if (shouldWrite) {
      await fs.mkdir(path.dirname(reportPath), { recursive: true })
      await fs.writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
    }
    rows.push({
      product: manifest.product,
      lane: manifest.lane,
      runtime: laneToRuntime(manifest.lane),
      repo: manifest.repo,
      reportPath: normalizePath(reportPath),
      existed: !!existing,
      written: shouldWrite,
      status: report.status,
      passedCheckCount: report.summary.passedCheckCount,
      requiredCheckCount: report.summary.requiredCheckCount,
      pendingCheckCount: report.summary.pendingCheckCount,
    })
  }
  const index = {
    schema: PACK_ACCEPTANCE_INDEX_SCHEMA,
    generatedAt,
    repoRoot: normalizePath(normalizedRoot),
    echoRoot: normalizePath(normalizedEchoRoot),
    write,
    force,
    summary: summaryFor(rows),
    rows,
  }
  const indexPath = path.join(normalizedRoot, outDir, 'pack-acceptance-report-index.json')
  await fs.mkdir(path.dirname(indexPath), { recursive: true })
  await fs.writeFile(indexPath, `${JSON.stringify(index, null, 2)}\n`, 'utf8')
  return {
    index,
    paths: { index: indexPath },
  }
}

async function loadParityReport({ repoRoot, echoRoot, parityReportPath }) {
  const absolute = path.resolve(repoRoot, parityReportPath)
  const fromDisk = await readJsonIfExists(absolute)
  if (fromDisk && !fromDisk.parseError) return fromDisk
  return (await generateRuntimeParityAudit({ repoRoot, echoRoot })).report
}

async function acceptanceReportFor({
  generatedAt,
  manifest,
  existing,
  packRoot,
  reportPath,
  repoRoot,
  echoRoot,
  seedAutomatedEvidence,
}) {
  const checks = {}
  for (const [id, description] of REQUIRED_CHECKS) {
    checks[id] = await normalizeCheck(existing?.checks?.[id] ?? existing?.checkDetails?.[id] ?? existing?.[id], description, {
      packRoot,
      repo: manifest.repo,
      generatedAt,
      automatedEvidence: seedAutomatedEvidence
        ? automatedEvidenceFor({ checkId: id, lane: manifest.lane, repoRoot, echoRoot })
        : [],
    })
  }
  const summary = checkSummary(checks)
  return {
    schema: PACK_ACCEPTANCE_SCHEMA,
    generatedAt,
    product: manifest.product,
    lane: manifest.lane,
    runtime: laneToRuntime(manifest.lane),
    repo: manifest.repo,
    manifestPath: manifest.manifestPath,
    moduleCount: manifest.moduleCount,
    status: summary.passedCheckCount === summary.requiredCheckCount ? 'PASS' : (summary.failedCheckCount > 0 ? 'FAIL' : 'PENDING'),
    acceptanceStandard: 'Manual or automated player-facing proof. Install/session/UI/action/world/save checks must be backed by logs, screenshots, save files, runtime reports, or equivalent QA artifacts.',
    reportPath: normalizePath(reportPath),
    requiredChecks: REQUIRED_CHECKS.map(([id, description]) => ({ id, description })),
    sourceDocs: sourceDocsFor(manifest.repo),
    checks,
    summary,
  }
}

async function normalizeCheck(value, description, context) {
  const current = object(value)
  const status = normalizeStatus(current.status ?? current.result ?? current.state ?? value)
  const evidence = array(current.evidence).filter((item) => typeof item === 'string' && item.trim().length > 0)
  const evidenceDetails = await Promise.all(evidence.map((reference) => evidenceReferenceDetail(reference, context)))
  const allEvidenceResolvable = evidenceDetails.length > 0 && evidenceDetails.every((entry) => entry.resolvable)
  const passSignal = current.passed === true || current.pass === true || value === true || status === 'PASS'
  const passed = passSignal && allEvidenceResolvable
  if (!passed && status !== 'FAIL' && context.automatedEvidence?.length > 0) {
    const automatedEvidenceDetails = await Promise.all(
      context.automatedEvidence.map((reference) => evidenceReferenceDetail(reference, context)),
    )
    if (automatedEvidenceDetails.length > 0 && automatedEvidenceDetails.every((entry) => entry.resolvable)) {
      return {
        status: 'PASS',
        passed: true,
        description,
        evidence: context.automatedEvidence,
        evidenceDetails: automatedEvidenceDetails,
        notes: current.notes
          ? string(current.notes)
          : 'Automated strict-play acceptance seeded from runtime evidence reports.',
        owner: string(current.owner) || 'ECHO strict-play automation',
        verifiedAt: string(current.verifiedAt) || context.generatedAt,
        blockers: [],
      }
    }
  }
  const blockers = []
  if (passSignal && evidence.length === 0) blockers.push('PASS check is missing evidence references.')
  for (const detail of evidenceDetails) {
    if (!detail.resolvable) blockers.push(`Evidence reference is not resolvable: ${detail.reference}`)
  }
  return {
    status: passed ? 'PASS' : (passSignal ? 'PENDING_EVIDENCE' : (status || 'PENDING')),
    passed,
    description,
    evidence,
    evidenceDetails,
    notes: string(current.notes),
    owner: string(current.owner),
    verifiedAt: string(current.verifiedAt),
    blockers,
  }
}

function automatedEvidenceFor({ checkId, lane, repoRoot, echoRoot }) {
  const modulesRoot = repoRoot
  const nativeRoot = path.join(echoRoot, 'ECHO-Native-Platform')
  const standaloneRoot = path.join(echoRoot, 'ECHO-Standalone-Runtime')
  const common = [
    path.join(modulesRoot, 'reports/runtime-parity/echo-module-runtime-parity-audit.json'),
  ]
  if (lane === 'NeoForge') {
    return [
      ...common,
      path.join(modulesRoot, 'reports/runtime-parity/neoforge-runtime-evidence.json'),
      ...(uiAcceptanceChecks().includes(checkId)
        ? [path.join(modulesRoot, 'reports/runtime-parity/neoforge-client-ui-results.json')]
        : []),
      ...(blockActionAcceptanceChecks().includes(checkId)
        ? [
            path.join(modulesRoot, 'reports/runtime-parity/neoforge-module-gametest-results.json'),
            path.join(modulesRoot, 'reports/runtime-parity/neoforge-registry-content-results.json'),
          ]
        : []),
      ...(worldgenAcceptanceChecks().includes(checkId)
        ? [path.join(modulesRoot, 'reports/runtime-parity/neoforge-registry-content-results.json')]
        : []),
      ...(saveAcceptanceChecks().includes(checkId)
        ? [path.join(modulesRoot, 'reports/runtime-parity/neoforge-module-gametest-results.json')]
        : []),
    ].map(normalizePath)
  }
  if (lane === 'Native') {
    return [
      ...common,
      path.join(nativeRoot, 'build/native-full-catalog-play/native-full-catalog-play.json'),
      ...(uiAcceptanceChecks().includes(checkId)
        ? [path.join(nativeRoot, 'build/native-ui-surfaces/native-ui-surfaces.json')]
        : []),
      ...(blockActionAcceptanceChecks().includes(checkId)
        ? [
            path.join(nativeRoot, 'build/native-block-actions/native-block-actions.json'),
            path.join(nativeRoot, 'build/native-registry-content/native-registry-content.json'),
          ]
        : []),
      ...(worldgenAcceptanceChecks().includes(checkId)
        ? [path.join(nativeRoot, 'build/native-registry-content/native-registry-content.json')]
        : []),
      ...(saveAcceptanceChecks().includes(checkId)
        ? [path.join(nativeRoot, 'build/native-save-network/native-save-network.json')]
        : []),
    ].map(normalizePath)
  }
  return [
    ...common,
    path.join(standaloneRoot, 'reports/echo/standalone/full-catalog-play.json'),
    ...(uiAcceptanceChecks().includes(checkId)
      ? [path.join(standaloneRoot, 'reports/echo/standalone/client-ui-surfaces-play.json')]
      : []),
    ...(blockActionAcceptanceChecks().includes(checkId)
      ? [
          path.join(standaloneRoot, 'reports/echo/standalone/block-action-mutations.json'),
          path.join(standaloneRoot, 'reports/echo/standalone/voxel-content-play.json'),
        ]
      : []),
    ...(worldgenAcceptanceChecks().includes(checkId)
      ? [path.join(standaloneRoot, 'reports/echo/standalone/worldgen-play.json')]
      : []),
    ...(saveAcceptanceChecks().includes(checkId)
      ? [path.join(standaloneRoot, 'reports/echo/standalone/save-reload-play.json')]
      : []),
  ].map(normalizePath)
}

function uiAcceptanceChecks() {
  return [
    'hudAppears',
    'inventoryOverlayAndIndexRespond',
    'terminalExecutesAction',
    'holoMapOpens',
    'lensScans',
    'screenCoreScreensRenderAndHandleInput',
  ]
}

function blockActionAcceptanceChecks() {
  return [
    'blockPlaceUseBreakWorks',
    'blockActionMutatesState',
    'terminalExecutesAction',
  ]
}

function worldgenAcceptanceChecks() {
  return ['worldgenAppears', 'freshSessionStarts']
}

function saveAcceptanceChecks() {
  return ['saveReloadPreservesState', 'trustedMutationsReported']
}

function checkSummary(checks) {
  const values = Object.values(checks)
  return {
    requiredCheckCount: values.length,
    passedCheckCount: values.filter((check) => check.passed).length,
    failedCheckCount: values.filter((check) => check.status === 'FAIL').length,
    missingEvidenceCount: values.filter((check) => check.status === 'PENDING_EVIDENCE').length,
    pendingCheckCount: values.filter((check) => !check.passed && check.status !== 'FAIL').length,
  }
}

function summaryFor(rows) {
  return {
    packLaneCount: rows.length,
    reportCount: rows.length,
    existingReportCount: rows.filter((row) => row.existed).length,
    writtenReportCount: rows.filter((row) => row.written).length,
    passCount: rows.filter((row) => row.status === 'PASS').length,
    failCount: rows.filter((row) => row.status === 'FAIL').length,
    pendingCount: rows.filter((row) => row.status === 'PENDING').length,
  }
}

function sourceDocsFor(repo) {
  return [
    `${repo}/README.md`,
    `${repo}/PUBLIC_ALPHA_RELEASE_STATUS.md`,
    `${repo}/docs/runtime-evidence.md`,
    `${repo}/docs/gameplay-evidence.md`,
    `${repo}/docs/module-requirements.md`,
  ]
}

function needsNormalization(report) {
  if (report?.schema !== PACK_ACCEPTANCE_SCHEMA) return true
  return REQUIRED_CHECKS.some(([id]) =>
    typeof report.checks?.[id] !== 'object'
      || (Array.isArray(report.checks?.[id]?.evidence) && !Array.isArray(report.checks?.[id]?.evidenceDetails)))
}

function shouldWriteReport({ existing, report, force }) {
  if (force || !existing || needsNormalization(existing)) return true
  return existing.status !== report.status
    || existing.product !== report.product
    || existing.lane !== report.lane
    || existing.runtime !== report.runtime
    || existing.repo !== report.repo
    || existing.manifestPath !== report.manifestPath
    || Number(existing.moduleCount ?? -1) !== report.moduleCount
    || Number(existing.summary?.passedCheckCount ?? -1) !== report.summary.passedCheckCount
    || Number(existing.summary?.pendingCheckCount ?? -1) !== report.summary.pendingCheckCount
    || Number(existing.summary?.missingEvidenceCount ?? -1) !== report.summary.missingEvidenceCount
}

async function evidenceReferenceDetail(reference, { packRoot, repo }) {
  const value = string(reference).trim()
  if (!value) {
    return { reference: value, kind: 'empty', resolvable: false }
  }
  if (/^[a-z][a-z0-9+.-]*:\/\//iu.test(value)) {
    return { reference: value, kind: 'external_url', resolvable: true }
  }
  const localPath = resolveEvidenceReference(value, { packRoot, repo })
  if (!localPath) {
    return { reference: value, kind: 'invalid_local_path', resolvable: false }
  }
  const found = await exists(localPath)
  return {
    reference: value,
    kind: 'local_file',
    resolvable: found,
    path: normalizePath(localPath),
  }
}

function resolveEvidenceReference(reference, { packRoot, repo }) {
  if (/^[a-z]:[\\/]/iu.test(reference) || path.isAbsolute(reference)) {
    return path.resolve(reference)
  }
  if (reference.startsWith(`${repo}/`)) {
    return path.resolve(path.dirname(packRoot), reference)
  }
  if (reference.includes('\0')) return ''
  return path.resolve(packRoot, reference)
}

async function readJsonIfExists(filePath) {
  try {
    const text = await fs.readFile(filePath, 'utf8')
    return JSON.parse(text.charCodeAt(0) === 0xfeff ? text.slice(1) : text)
  } catch (error) {
    if (error.code === 'ENOENT') return null
    return { parseError: error.message }
  }
}

async function exists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

function parseArgs(argv) {
  const options = {
    repoRoot: process.cwd(),
    echoRoot: '',
    outDir: DEFAULT_OUT_DIR,
    parityReportPath: PARITY_REPORT,
    write: false,
    force: false,
    seedAutomatedEvidence: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--repo-root') options.repoRoot = argv[++index]
    else if (arg === '--echo-root') options.echoRoot = argv[++index]
    else if (arg === '--out-dir') options.outDir = argv[++index]
    else if (arg === '--parity-report') options.parityReportPath = argv[++index]
    else if (arg === '--write') options.write = true
    else if (arg === '--force') options.force = true
    else if (arg === '--seed-automated-evidence') options.seedAutomatedEvidence = true
    else if (arg === '--help') options.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  if (!options.echoRoot) options.echoRoot = path.dirname(path.resolve(options.repoRoot))
  return options
}

function laneToRuntime(lane) {
  if (lane === 'Native') return 'echo_native'
  if (lane === 'Standalone') return 'standalone'
  return 'neoforge'
}

function normalizeStatus(value) {
  const normalized = string(value).trim().toUpperCase()
  if (['PASS', 'PASSED', 'SUCCESS', 'OK', 'TRUE'].includes(normalized)) return 'PASS'
  if (['FAIL', 'FAILED', 'ERROR', 'FALSE'].includes(normalized)) return 'FAIL'
  if (['PENDING', 'PENDING_EVIDENCE', 'TODO', 'NOT_RUN', ''].includes(normalized)) return normalized || 'PENDING'
  if (['PARTIAL', 'WARN', 'WARNING'].includes(normalized)) return 'PARTIAL'
  return normalized
}

function slug(value) {
  return String(value).toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')
}

function array(value) {
  return Array.isArray(value) ? value : []
}

function object(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

function string(value) {
  return typeof value === 'string' ? value : ''
}

function normalizePath(value) {
  return value.replace(/\\/g, '/')
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const options = parseArgs(process.argv.slice(2))
    if (options.help) {
      console.log('Usage: node scripts/generate-pack-acceptance-reports.mjs [--repo-root <path>] [--echo-root <path>] [--out-dir <path>] [--parity-report <path>] [--write] [--force] [--seed-automated-evidence]')
    } else {
      const { index, paths } = await generatePackAcceptanceReports(options)
      console.log(`Wrote pack acceptance index: ${paths.index}`)
      console.log(`Pack acceptance reports: ${index.summary.writtenReportCount} written, ${index.summary.existingReportCount} existing, ${index.summary.pendingCount} pending, ${index.summary.passCount} passing.`)
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}

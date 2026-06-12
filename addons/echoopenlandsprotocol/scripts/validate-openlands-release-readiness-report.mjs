import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { pathToFileURL } from 'node:url'
import { isPublicHttpsUrl } from './openlands-public-download-url.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const RELEASE_PUBLICATION_CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json'
const LAUNCHER_FLOW_CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json'
const DISTRIBUTION_CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json'
const DISTRIBUTION_APPROVAL_CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json'
const LAUNCH_ROADMAP_PATH = 'data/echoopenlandsprotocol/openlands/progression/launch_roadmap.json'
const CROSS_PLATFORM_PARITY_PATH = 'data/echoopenlandsprotocol/openlands/systems/cross_platform_parity.json'
const CONFORMANCE_PATH = 'data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json'
const RUNTIME_ADAPTER_LOAD_PLAN_PATH = 'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json'
const LEGAL_CONTENT_AUDIT_PATH = 'data/echoopenlandsprotocol/openlands/systems/legal_content_audit.json'
const CONTENT_POLICY_PATH = 'data/echoopenlandsprotocol/openlands/config/content_policy.json'
const ASSET_MANIFEST_PATH = 'assets/echoopenlandsprotocol/asset_manifest.json'
const EDITIONS = [
  {
    id: 'native',
    repo: 'ECHO-Openlands-Native-Edition',
    runtimeTarget: 'echo_native',
    runtimeExecutionReport: 'evidence/native-runtime-execution-report.json',
    localRuntimeRehearsalReport: 'evidence/native-local-runtime-rehearsal-report.json',
    distributionReport: 'evidence/native-distribution-roadmap-report.json',
    launcherFlowReport: 'evidence/native-launcher-flow-report.json',
    launcherExecutionReport: 'evidence/native-launcher-execution-report.json',
    localLauncherRehearsalReport: 'evidence/native-local-launcher-rehearsal-report.json',
    legalReport: 'evidence/native-legal-content-audit.json',
    finalReviewReport: 'evidence/native-final-release-review-report.json',
    distributionApprovalReport: 'evidence/native-distribution-approval-report.json',
  },
  {
    id: 'neoforge',
    repo: 'ECHO-Openlands-NeoForge-Edition',
    runtimeTarget: 'neoforge',
    runtimeExecutionReport: 'evidence/neoforge-runtime-execution-report.json',
    localRuntimeRehearsalReport: 'evidence/neoforge-local-runtime-rehearsal-report.json',
    distributionReport: 'evidence/neoforge-distribution-roadmap-report.json',
    launcherFlowReport: 'evidence/neoforge-launcher-flow-report.json',
    launcherExecutionReport: 'evidence/neoforge-launcher-execution-report.json',
    localLauncherRehearsalReport: 'evidence/neoforge-local-launcher-rehearsal-report.json',
    legalReport: 'evidence/neoforge-legal-content-audit.json',
    finalReviewReport: 'evidence/neoforge-final-release-review-report.json',
    distributionApprovalReport: 'evidence/neoforge-distribution-approval-report.json',
  },
  {
    id: 'standalone',
    repo: 'ECHO-Openlands-Standalone-Edition',
    runtimeTarget: 'echo_runtime_standalone',
    runtimeExecutionReport: 'evidence/standalone-runtime-execution-report.json',
    localRuntimeRehearsalReport: 'evidence/standalone-local-runtime-rehearsal-report.json',
    distributionReport: 'evidence/standalone-distribution-roadmap-report.json',
    launcherFlowReport: 'evidence/standalone-launcher-flow-report.json',
    launcherExecutionReport: 'evidence/standalone-launcher-execution-report.json',
    localLauncherRehearsalReport: 'evidence/standalone-local-launcher-rehearsal-report.json',
    legalReport: 'evidence/standalone-legal-content-audit.json',
    finalReviewReport: 'evidence/standalone-final-release-review-report.json',
    distributionApprovalReport: 'evidence/standalone-distribution-approval-report.json',
  },
]
const EXPECTED_EDITIONS = EDITIONS.map((edition) => edition.id)
const EDITION_REPORT_SCHEMAS = {
  runtimeExecution: 'echo.openlands.edition.runtime_execution_report.v1',
  localRuntimeRehearsal: 'echo.openlands.edition.local_runtime_rehearsal_report.v1',
  distribution: 'echo.openlands.edition.distribution_roadmap_report.v1',
  launcherFlow: 'echo.openlands.edition.launcher_flow_report.v1',
  launcherExecution: 'echo.openlands.edition.launcher_execution_report.v1',
  localLauncherRehearsal: 'echo.openlands.edition.local_launcher_rehearsal_report.v1',
  legal: 'echo.openlands.edition.legal_content_audit_report.v1',
  finalReview: 'echo.openlands.edition.final_release_review_report.v1',
  distributionApproval: 'echo.openlands.edition.distribution_approval_report.v1',
}

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    releaseRoot: null,
    report: null,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--workspace-root') args.workspaceRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--report') args.report = argv[++index]
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function fileExists(filePath) {
  return fs.existsSync(filePath)
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function assert(errors, condition, message) {
  if (!condition) errors.push(message)
}

function sorted(values) {
  return [...(values ?? [])].sort()
}

function sortedUnique(values) {
  return [...new Set((values ?? []).filter(Boolean))].sort()
}

function sameSet(actual, expected) {
  return JSON.stringify(sorted(actual)) === JSON.stringify(sorted(expected))
}

function sameJson(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function hasOwn(value, key) {
  return value !== null && typeof value === 'object' && Object.prototype.hasOwnProperty.call(value, key)
}

function normalizeRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
}

function listFiles(root) {
  if (!fileExists(root)) return []
  const files = []
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const absolute = path.join(root, entry.name)
    if (entry.isDirectory()) files.push(...listFiles(absolute))
    else if (entry.isFile()) files.push(absolute)
  }
  return files
}

function findModuleRoot(explicitRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = process.cwd()
  for (;;) {
    const direct = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(direct)) return cursor
    const nested = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(nested)) return path.join(cursor, 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function resolveReportedPath(value, releaseRoot) {
  if (typeof value !== 'string' || value.length === 0) return null
  return path.isAbsolute(value) ? path.resolve(value) : path.resolve(releaseRoot, value)
}

function sameResolvedPath(actual, expected) {
  return typeof actual === 'string' && path.resolve(actual) === path.resolve(expected)
}

function defaultPhaseReadinessMarkdownPath(reportPath) {
  const parsed = path.parse(reportPath)
  return path.join(parsed.dir, `${parsed.name}-by-phase.md`)
}

function renderPhaseReadinessMarkdown(report) {
  const phaseReadiness = report.phaseReadiness ?? {}
  const lines = [
    '# Openlands Release Readiness By Phase',
    '',
    `Status: ${report.status}`,
    `Public Alpha ready: ${report.publicAlphaReady}`,
    `Blockers: ${(report.blockers ?? []).length === 0 ? 'none' : report.blockers.join(', ')}`,
    '',
  ]
  for (const phase of phaseReadiness.phases ?? []) {
    lines.push(`## ${phase.order}. ${phase.displayName}`)
    lines.push('')
    lines.push(`Status: ${phase.status}`)
    lines.push(`Ready for Public Alpha: ${phase.readyForPublicAlpha}`)
    lines.push(`Active blockers: ${phase.activeBlockers.length === 0 ? 'none' : phase.activeBlockers.join(', ')}`)
    lines.push('')
    lines.push('Blocking checks:')
    for (const check of phase.blockingChecks ?? []) {
      lines.push(`- ${check.id}: ${check.passed ? 'passed' : 'blocked'}`)
    }
    lines.push('')
    lines.push('Next evidence:')
    for (const evidence of phase.nextEvidence ?? []) {
      lines.push(`- ${evidence}`)
    }
    if ((phase.handoffArtifacts ?? []).length > 0) {
      lines.push('')
      lines.push('Handoff files:')
      for (const artifact of phase.handoffArtifacts) {
        const state = artifact.present ? 'present' : 'expected'
        lines.push(`- ${artifact.label}: ${artifact.path} (${state}) - ${artifact.purpose}`)
      }
    }
    lines.push('')
  }
  return `${lines.join('\n').trimEnd()}\n`
}

function editionReport(workspaceRoot, edition, relativePath) {
  const filePath = path.join(workspaceRoot, edition.repo, relativePath)
  const present = fileExists(filePath)
  return {
    filePath,
    report: present ? readJson(filePath) : null,
  }
}

function assertReportIdentity(errors, report, schema, edition, label) {
  if (report === null) return
  assert(errors, report.schema === schema, `${label} schema mismatch`)
  if (hasOwn(report, 'edition')) assert(errors, report.edition === edition.id, `${label} edition mismatch`)
  if (hasOwn(report, 'runtimeTarget')) assert(errors, report.runtimeTarget === edition.runtimeTarget, `${label} runtime target mismatch`)
  if (hasOwn(report, 'moduleId')) assert(errors, report.moduleId === MODULE_ID, `${label} module id mismatch`)
  if (hasOwn(report, 'moduleVersion')) assert(errors, report.moduleVersion === VERSION, `${label} module version mismatch`)
}

function assertObjectFieldsMatch(errors, actual, expected, label) {
  assert(errors, actual !== null && typeof actual === 'object', `${label} must be present`)
  assert(errors, sameSet(Object.keys(actual ?? {}), Object.keys(expected ?? {})), `${label} fields do not match evidence`)
  for (const field of Object.keys(expected ?? {})) {
    assert(errors, actual?.[field] === expected[field], `${label} ${field} does not match evidence`)
  }
}

function stableReleaseReadinessReport(report) {
  if (!report || typeof report !== 'object') return report
  const { generatedAt, dryRun, ...stableReport } = report
  return stableReport
}

function stableGeneratorReport(value) {
  if (Array.isArray(value)) return value.map(stableGeneratorReport)
  if (!value || typeof value !== 'object') return value
  const stable = {}
  for (const [key, entry] of Object.entries(value)) {
    if (['generatedAt', 'dryRun'].includes(key)) continue
    stable[key] = stableGeneratorReport(entry)
  }
  return stable
}

function openlandsDataPath(moduleRoot, relativePath) {
  return path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands', relativePath)
}

function normalizeHarnessDriverSurfaces(value) {
  if (!Array.isArray(value)) return []
  return value
    .map((entry) => {
      if (typeof entry === 'string') {
        return {
          id: entry,
          harnessFamily: null,
          methodsImplemented: [],
          capturesImplemented: [],
        }
      }
      if (entry && typeof entry === 'object') {
        return {
          ...entry,
          id: entry.id ?? entry.driverSurfaceId ?? entry.name,
          harnessFamily: entry.harnessFamily ?? entry.family ?? null,
          methodsImplemented: Array.isArray(entry.methodsImplemented) ? entry.methodsImplemented : [],
          capturesImplemented: Array.isArray(entry.capturesImplemented) ? entry.capturesImplemented : [],
        }
      }
      return null
    })
    .filter((entry) => typeof entry?.id === 'string' && entry.id.length > 0)
}

function includesAll(actual, expected) {
  const actualSet = new Set(actual ?? [])
  return (expected ?? []).every((entry) => actualSet.has(entry))
}

function expectedHarnessDriverCompleteness(planDriver, manifestDriver) {
  const requiredMethods = planDriver.requiredMethods ?? []
  const requiredCaptures = planDriver.mustCapture ?? []
  const methodsImplemented = manifestDriver?.methodsImplemented ?? []
  const capturesImplemented = manifestDriver?.capturesImplemented ?? []
  const missingMethods = requiredMethods.filter((method) => !methodsImplemented.includes(method))
  const missingCaptures = requiredCaptures.filter((capture) => !capturesImplemented.includes(capture))
  return {
    id: planDriver.id,
    declared: Boolean(manifestDriver),
    complete: Boolean(manifestDriver)
      && includesAll(methodsImplemented, requiredMethods)
      && includesAll(capturesImplemented, requiredCaptures),
    requiredMethodCount: requiredMethods.length,
    implementedMethodCount: methodsImplemented.length,
    missingMethods,
    requiredCaptureCount: requiredCaptures.length,
    implementedCaptureCount: capturesImplemented.length,
    missingCaptures,
  }
}

function buildExpectedHarnessDriverSummary({ plan, manifest, harnessType }) {
  const requiredDrivers = plan.driverSurfaces ?? []
  const requiredDriverSurfaceIds = sortedUnique(requiredDrivers.map((driver) => driver.id))
  const allDeclaredDriverSurfaces = normalizeHarnessDriverSurfaces([
    ...(Array.isArray(manifest.availableDriverSurfaces) ? manifest.availableDriverSurfaces : []),
    ...(Array.isArray(manifest.driverSurfaces) ? manifest.driverSurfaces : []),
    ...(Array.isArray(manifest.drivers) ? manifest.drivers : []),
  ])
  const declaredDriverSurfaces = allDeclaredDriverSurfaces
    .filter((driver) => driver.harnessFamily === harnessType || driver.harnessFamily === null)
  const ignoredDriverSurfaces = allDeclaredDriverSurfaces
    .filter((driver) => driver.harnessFamily !== harnessType && driver.harnessFamily !== null)
  const declaredDriverSurfaceIds = sortedUnique(declaredDriverSurfaces.map((driver) => driver.id))
  const ignoredDriverSurfaceIds = sortedUnique(ignoredDriverSurfaces.map((driver) => `${driver.harnessFamily}:${driver.id}`))
  const manifestDriversById = new Map(declaredDriverSurfaces.map((driver) => [driver.id, driver]))
  const driverCompletenessDetails = requiredDrivers
    .map((planDriver) => expectedHarnessDriverCompleteness(planDriver, manifestDriversById.get(planDriver.id)))
    .sort((left, right) => left.id.localeCompare(right.id))
  const completeDriverSurfaceIds = driverCompletenessDetails
    .filter((driver) => driver.complete)
    .map((driver) => driver.id)
    .sort()
  const incompleteDriverSurfaceIds = driverCompletenessDetails
    .filter((driver) => driver.declared && !driver.complete)
    .map((driver) => driver.id)
    .sort()
  const missingDriverSurfaceIds = requiredDriverSurfaceIds.filter((id) => !completeDriverSurfaceIds.includes(id))

  return {
    requiredDriverSurfaceCount: requiredDriverSurfaceIds.length,
    declaredDriverSurfaceCount: declaredDriverSurfaceIds.length,
    ignoredDriverSurfaceCount: ignoredDriverSurfaceIds.length,
    completeDriverSurfaceCount: completeDriverSurfaceIds.length,
    incompleteDriverSurfaceCount: incompleteDriverSurfaceIds.length,
    availableDriverSurfaceCount: completeDriverSurfaceIds.length,
    missingDriverSurfaceCount: missingDriverSurfaceIds.length,
    requiredDriverSurfaceIds,
    declaredDriverSurfaceIds,
    ignoredDriverSurfaceIds,
    completeDriverSurfaceIds,
    incompleteDriverSurfaceIds,
    availableDriverSurfaceIds: completeDriverSurfaceIds,
    missingDriverSurfaceIds,
    driverCompletenessDetails,
  }
}

function validateHarnessDriverSummary(errors, { label, actual, expected }) {
  assert(errors, actual !== null && typeof actual === 'object', `${label} driver summary missing`)
  for (const field of [
    'requiredDriverSurfaceCount',
    'declaredDriverSurfaceCount',
    'ignoredDriverSurfaceCount',
    'completeDriverSurfaceCount',
    'incompleteDriverSurfaceCount',
    'availableDriverSurfaceCount',
    'missingDriverSurfaceCount',
  ]) {
    assert(errors, actual?.[field] === expected[field], `${label} ${field} mismatch`)
  }
  for (const field of [
    'requiredDriverSurfaceIds',
    'declaredDriverSurfaceIds',
    'ignoredDriverSurfaceIds',
    'completeDriverSurfaceIds',
    'incompleteDriverSurfaceIds',
    'availableDriverSurfaceIds',
    'missingDriverSurfaceIds',
  ]) {
    assert(errors, sameSet(actual?.[field], expected[field]), `${label} ${field} mismatch`)
  }
  assert(errors, sameSet((actual?.driverCompletenessDetails ?? []).map((driver) => driver.id), expected.driverCompletenessDetails.map((driver) => driver.id)), `${label} driver completeness ids mismatch`)
  const actualDetails = new Map((actual?.driverCompletenessDetails ?? []).map((driver) => [driver.id, driver]))
  for (const expectedDetail of expected.driverCompletenessDetails) {
    const actualDetail = actualDetails.get(expectedDetail.id)
    assert(errors, actualDetail !== undefined, `${label} missing completeness detail for ${expectedDetail.id}`)
    if (!actualDetail) continue
    for (const field of [
      'declared',
      'complete',
      'requiredMethodCount',
      'implementedMethodCount',
      'requiredCaptureCount',
      'implementedCaptureCount',
    ]) {
      assert(errors, actualDetail[field] === expectedDetail[field], `${label} ${expectedDetail.id} ${field} mismatch`)
    }
    assert(errors, sameSet(actualDetail.missingMethods, expectedDetail.missingMethods), `${label} ${expectedDetail.id} missing methods mismatch`)
    assert(errors, sameSet(actualDetail.missingCaptures, expectedDetail.missingCaptures), `${label} ${expectedDetail.id} missing captures mismatch`)
  }
}

function parseValidatorJson(output) {
  const text = String(output ?? '').trim()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

function runSubValidator(errors, { label, moduleRoot, scriptName, args }) {
  const scriptPath = path.join(moduleRoot, 'scripts', scriptName)
  const result = spawnSync(process.execPath, [scriptPath, ...args, '--json'], {
    cwd: path.resolve(moduleRoot, '..', '..'),
    encoding: 'utf8',
    windowsHide: true,
  })
  if (result.error) {
    errors.push(`${label} validator could not run: ${result.error.message}`)
    return null
  }
  const parsed = parseValidatorJson(result.stdout)
  if (!parsed) {
    const detail = (result.stderr || result.stdout || '').trim().split(/\r?\n/).slice(0, 3).join(' ')
    errors.push(`${label} validator did not return JSON${detail ? `: ${detail}` : ''}`)
    return null
  }
  if (result.status !== 0 || parsed.status !== 'passed') {
    errors.push(`${label} validator failed`)
    for (const error of parsed.errors ?? []) errors.push(`${label}: ${error}`)
  }
  return parsed
}

function runGeneratorDryRunJson(errors, { label, moduleRoot, scriptName, args }) {
  const scriptPath = path.join(moduleRoot, 'scripts', scriptName)
  assert(errors, fileExists(scriptPath), `${label} generator missing: ${scriptPath}`)
  if (!fileExists(scriptPath)) return null
  const result = spawnSync(process.execPath, [scriptPath, ...args, '--dry-run', '--json'], {
    cwd: path.resolve(moduleRoot, '..', '..'),
    encoding: 'utf8',
    windowsHide: true,
  })
  if (result.error) {
    errors.push(`${label} generator could not run: ${result.error.message}`)
    return null
  }
  if (result.status !== 0) {
    errors.push(`${label} generator dry-run failed: ${(result.stderr || result.stdout || '').trim()}`)
    return null
  }
  const parsed = parseValidatorJson(result.stdout)
  if (!parsed) {
    const detail = (result.stderr || result.stdout || '').trim().split(/\r?\n/).slice(0, 3).join(' ')
    errors.push(`${label} generator dry-run did not return JSON${detail ? `: ${detail}` : ''}`)
    return null
  }
  return parsed
}

function validateReferencedEditionReports(errors, { moduleRoot, workspaceRoot, releaseRoot, editionEvidence }) {
  const validators = [
    {
      key: 'runtimeExecution',
      label: 'runtime execution',
      scriptName: 'validate-openlands-runtime-execution-report.mjs',
      reportPathKey: 'runtimeExecutionReport',
      rootArg: '--edition-root',
      extraArgs: [],
    },
    {
      key: 'localRuntimeRehearsal',
      label: 'local runtime rehearsal',
      scriptName: 'validate-openlands-local-runtime-rehearsal-report.mjs',
      reportPathKey: 'localRuntimeRehearsalReport',
      rootArg: '--edition-root',
      extraArgs: ['--release-root', releaseRoot],
    },
    {
      key: 'launcherExecution',
      label: 'launcher execution',
      scriptName: 'validate-openlands-launcher-execution-report.mjs',
      reportPathKey: 'launcherExecutionReport',
      rootArg: '--edition-root',
      extraArgs: [],
    },
    {
      key: 'localLauncherRehearsal',
      label: 'local launcher rehearsal',
      scriptName: 'validate-openlands-local-launcher-rehearsal-report.mjs',
      reportPathKey: 'localLauncherRehearsalReport',
      rootArg: '--edition-root',
      extraArgs: ['--release-root', releaseRoot],
    },
    {
      key: 'finalReview',
      label: 'final release review',
      scriptName: 'validate-openlands-final-release-review-report.mjs',
      reportPathKey: 'finalReviewReport',
      rootArg: '--edition-root',
      extraArgs: [],
    },
    {
      key: 'distributionApproval',
      label: 'distribution approval',
      scriptName: 'validate-openlands-distribution-approval-report.mjs',
      reportPathKey: 'distributionApprovalReport',
      rootArg: '--edition-root',
      extraArgs: ['--release-root', releaseRoot],
    },
  ]
  for (const edition of EDITIONS) {
    const editionRoot = path.join(workspaceRoot, edition.repo)
    const evidence = editionEvidence.get(edition.id)
    for (const validator of validators) {
      const reportRef = evidence?.reports?.[validator.key]
      if (!reportRef?.report) continue
      runSubValidator(errors, {
        label: `${edition.id} ${validator.label}`,
        moduleRoot,
        scriptName: validator.scriptName,
        args: [
          '--module-root',
          moduleRoot,
          '--edition',
          edition.id,
          validator.rootArg,
          editionRoot,
          '--report',
          path.join(workspaceRoot, edition.repo, edition[validator.reportPathKey]),
          ...validator.extraArgs,
        ],
      })
    }
  }
}

function validateReferencedModuleReports(errors, { moduleRoot, workspaceRoot, releaseRoot, publicationManifestPath, publicationRehearsalPath, editionManifestIndexPreviewPath }) {
  runSubValidator(errors, {
    label: 'release publication manifest',
    moduleRoot,
    scriptName: 'validate-openlands-release-publication-manifest.mjs',
    args: [
      '--module-root',
      moduleRoot,
      '--workspace-root',
      workspaceRoot,
      '--release-root',
      releaseRoot,
      '--manifest',
      publicationManifestPath ?? path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.template.json'),
    ],
  })
  runSubValidator(errors, {
    label: 'release publication rehearsal',
    moduleRoot,
    scriptName: 'validate-openlands-release-publication-rehearsal-report.mjs',
    args: [
      '--module-root',
      moduleRoot,
      '--release-root',
      releaseRoot,
      '--report',
      publicationRehearsalPath ?? path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-rehearsal-report.json'),
    ],
  })
  runSubValidator(errors, {
    label: 'edition manifest index preview',
    moduleRoot,
    scriptName: 'validate-openlands-edition-manifest-index-preview.mjs',
    args: [
      '--module-root',
      moduleRoot,
      '--workspace-root',
      workspaceRoot,
      '--release-root',
      releaseRoot,
      '--report',
      editionManifestIndexPreviewPath ?? path.join(releaseRoot, MODULE_ID, 'openlands-edition-manifest-index-preview.json'),
    ],
  })
}

function validateEditionAggregate(errors, { moduleRoot, workspaceRoot, reportPath }) {
  runSubValidator(errors, {
    label: 'edition aggregate',
    moduleRoot,
    scriptName: 'validate-openlands-editions.mjs',
    args: [
      '--module-root',
      moduleRoot,
      '--workspace-root',
      workspaceRoot,
      '--readiness-report',
      reportPath,
    ],
  })
}

function validateHarnessDriverManifests(errors, { moduleRoot, workspaceRoot }) {
  for (const edition of EDITIONS) {
    const editionRoot = path.join(workspaceRoot, edition.repo)
    runSubValidator(errors, {
      label: `${edition.id} harness driver manifest`,
      moduleRoot,
      scriptName: 'validate-openlands-harness-driver-manifest.mjs',
      args: [
        '--module-root',
        moduleRoot,
        '--edition',
        edition.id,
        '--edition-root',
        editionRoot,
        '--manifest',
        path.join(editionRoot, 'evidence', `${edition.id}-harness-driver-manifest.template.json`),
      ],
    })
  }
}

function runHarnessDryRun(errors, { label, moduleRoot, scriptName, args, expected }) {
  const scriptPath = path.join(moduleRoot, 'scripts', scriptName)
  const result = spawnSync(process.execPath, [scriptPath, ...args, '--dry-run', '--json'], {
    cwd: path.resolve(moduleRoot, '..', '..'),
    encoding: 'utf8',
    windowsHide: true,
    maxBuffer: 10 * 1024 * 1024,
  })
  if (result.error) {
    errors.push(`${label} dry-run could not run: ${result.error.message}`)
    return null
  }
  const parsed = parseValidatorJson(result.stdout)
  if (!parsed) {
    const detail = (result.stderr || result.stdout || '').trim().split(/\r?\n/).slice(0, 3).join(' ')
    errors.push(`${label} dry-run did not return JSON${detail ? `: ${detail}` : ''}`)
    return null
  }
  if (result.status !== 0) {
    const detail = (result.stderr || '').trim().split(/\r?\n/).slice(0, 3).join(' ')
    errors.push(`${label} dry-run failed${detail ? `: ${detail}` : ''}`)
  }
  assert(errors, parsed.schema === expected.reportSchema, `${label} report schema mismatch`)
  assert(errors, parsed.status === 'blocked', `${label} report must remain blocked until real harness execution`)
  assert(errors, parsed.moduleId === MODULE_ID, `${label} module id mismatch`)
  assert(errors, parsed.moduleVersion === VERSION, `${label} module version mismatch`)
  assert(errors, parsed.edition === expected.editionId, `${label} edition mismatch`)
  assert(errors, parsed.runtimeTarget === expected.runtimeTarget, `${label} runtime target mismatch`)
  assert(errors, parsed[expected.readyField] === false, `${label} must not mark ready in dry-run`)

  const harnessRun = parsed.harnessRun
  const driverSummary = harnessRun?.driverSummary ?? {}
  const expectedDriverSummary = buildExpectedHarnessDriverSummary({
    plan: expected.plan,
    manifest: expected.driverManifest,
    harnessType: expected.harnessType,
  })
  const expectedBlockedBy = expectedDriverSummary.missingDriverSurfaceCount > 0
    ? [expected.driverMissingBlocker, 'real_harness_execution_not_run']
    : ['real_harness_execution_not_run']
  assert(errors, harnessRun?.schema === 'echo.openlands.edition.harness_run.v1', `${label} harnessRun schema mismatch`)
  assert(errors, harnessRun?.status === 'blocked', `${label} harnessRun must remain blocked`)
  assert(errors, harnessRun?.harnessType === expected.harnessType, `${label} harness type mismatch`)
  assert(errors, harnessRun?.harnessDisplayName === expected.displayName, `${label} harness display name mismatch`)
  assert(errors, harnessRun?.moduleId === MODULE_ID, `${label} harnessRun module id mismatch`)
  assert(errors, harnessRun?.moduleVersion === VERSION, `${label} harnessRun version mismatch`)
  assert(errors, harnessRun?.edition === expected.editionId, `${label} harnessRun edition mismatch`)
  assert(errors, harnessRun?.runtimeTarget === expected.runtimeTarget, `${label} harnessRun runtime target mismatch`)
  assert(errors, harnessRun?.harnessPlan === `data/${MODULE_ID}/openlands/${expected.planRelativePath}`, `${label} harness plan id mismatch`)
  assert(errors, harnessRun?.harnessPlanSchema === expected.plan.schema, `${label} harness plan schema mismatch`)
  assert(errors, sameResolvedPath(harnessRun?.harnessPlanPath, expected.planPath), `${label} harness plan path mismatch`)
  assert(errors, harnessRun?.entryPoint === expected.editionHarness?.entryPoint, `${label} harness entry point mismatch`)
  assert(errors, harnessRun?.driverKind === expected.editionHarness?.driverKind, `${label} driver kind mismatch`)
  assert(errors, harnessRun?.requiredReport === expected.editionHarness?.requiredReport, `${label} required report mismatch`)
  assert(errors, harnessRun?.artifactPattern === expected.editionHarness?.artifactPattern, `${label} artifact pattern mismatch`)
  assert(errors, harnessRun?.driverManifest?.schema === 'echo.openlands.edition.harness_driver_manifest.v1', `${label} driver manifest schema mismatch`)
  assert(errors, sameResolvedPath(harnessRun?.driverManifest?.path, expected.driverManifestPath), `${label} driver manifest path mismatch`)
  validateHarnessDriverSummary(errors, { label, actual: driverSummary, expected: expectedDriverSummary })
  assert(errors, harnessRun?.bindingSummary?.bindingKey === expected.bindingKey, `${label} binding key mismatch`)
  assert(errors, harnessRun?.bindingSummary?.bindingLabel === expected.bindingLabel, `${label} binding label mismatch`)
  assert(errors, harnessRun?.bindingSummary?.bindingCount === (expected.plan[expected.bindingKey] ?? []).length, `${label} binding count mismatch`)
  assert(errors, (harnessRun?.bindingSummary?.bindingCount ?? 0) > 0, `${label} binding count must be positive`)
  assert(errors, sameSet(harnessRun?.blockedBy, expectedBlockedBy), `${label} blockedBy mismatch`)
  return parsed
}

function validateHarnessDryRuns(errors, { moduleRoot, workspaceRoot, releaseRoot }) {
  const harnesses = [
    {
      harnessType: 'runtime',
      label: 'runtime execution harness',
      scriptName: 'run-openlands-runtime-execution-harness.mjs',
      planRelativePath: 'systems/runtime_execution_harness_plan.json',
      bindingKey: 'scenarioBindings',
      bindingLabel: 'scenarios',
      displayName: 'runtime execution',
      reportSchema: 'echo.openlands.edition.runtime_execution_report.v1',
      readyField: 'publicAlphaReady',
      driverMissingBlocker: 'real_runtime_harness_drivers_missing',
      extraArgs: [],
    },
    {
      harnessType: 'launcher',
      label: 'launcher execution harness',
      scriptName: 'run-openlands-launcher-execution-harness.mjs',
      planRelativePath: 'systems/launcher_execution_harness_plan.json',
      bindingKey: 'flowBindings',
      bindingLabel: 'flows',
      displayName: 'launcher execution',
      reportSchema: 'echo.openlands.edition.launcher_execution_report.v1',
      readyField: 'publicAlphaReady',
      driverMissingBlocker: 'real_launcher_harness_drivers_missing',
      extraArgs: [],
    },
    {
      harnessType: 'finalReview',
      label: 'final release review harness',
      scriptName: 'run-openlands-final-release-review-harness.mjs',
      planRelativePath: 'systems/final_release_review_harness_plan.json',
      bindingKey: 'reviewAreaBindings',
      bindingLabel: 'reviewAreas',
      displayName: 'final release review',
      reportSchema: 'echo.openlands.edition.final_release_review_report.v1',
      readyField: 'publicReleaseReady',
      driverMissingBlocker: 'final_review_harness_drivers_missing',
      extraArgs: [],
    },
    {
      harnessType: 'distributionApproval',
      label: 'distribution approval harness',
      scriptName: 'run-openlands-distribution-approval-harness.mjs',
      planRelativePath: 'systems/distribution_approval_harness_plan.json',
      bindingKey: 'approvalAreaBindings',
      bindingLabel: 'approvalAreas',
      displayName: 'distribution approval',
      reportSchema: 'echo.openlands.edition.distribution_approval_report.v1',
      readyField: 'publicAlphaReady',
      driverMissingBlocker: 'distribution_approval_harness_drivers_missing',
      extraArgs: ['--release-root', releaseRoot],
    },
  ]
  for (const edition of EDITIONS) {
    const editionRoot = path.join(workspaceRoot, edition.repo)
    const driverManifestPath = path.join(editionRoot, 'evidence', `${edition.id}-harness-driver-manifest.template.json`)
    for (const harness of harnesses) {
      const planPath = openlandsDataPath(moduleRoot, harness.planRelativePath)
      const plan = readJson(planPath)
      const driverManifest = readJson(driverManifestPath)
      const editionHarness = (plan.editionHarnesses ?? []).find((entry) => entry.edition === edition.id)
      runHarnessDryRun(errors, {
        label: `${edition.id} ${harness.label}`,
        moduleRoot,
        scriptName: harness.scriptName,
        args: [
          '--module-root',
          moduleRoot,
          '--edition',
          edition.id,
          '--edition-root',
          editionRoot,
          '--driver-manifest',
          driverManifestPath,
          '--require-driver-manifest',
          ...harness.extraArgs,
        ],
        expected: {
          ...harness,
          editionId: edition.id,
          runtimeTarget: edition.runtimeTarget,
          driverManifestPath,
          driverManifest,
          plan,
          planPath,
          editionHarness,
        },
      })
    }
  }
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function buildArtifactEvidenceResults({ releaseRoot, releaseModule, releasePublicationContract }) {
  return (releasePublicationContract.artifactTargets ?? []).map((target) => {
    const artifactPath = path.join(releaseRoot, MODULE_ID, target.file)
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    const exists = fileExists(artifactPath)
    const actualSha256 = exists ? sha256File(artifactPath) : null
    return {
      id: target.id,
      file: target.file,
      requiredForPublicAlpha: target.requiredForPublicAlpha === true,
      exists,
      releaseIndexEntryPresent: releaseArtifact !== undefined,
      sha256Present: typeof releaseArtifact?.sha256 === 'string' && releaseArtifact.sha256.length === 64,
      sha256MatchesFile: exists && releaseArtifact?.sha256 === actualSha256,
      sizeMatchesFile: exists && releaseArtifact?.size === fs.statSync(artifactPath).size,
      downloadUrlPresent: typeof releaseArtifact?.downloadUrl === 'string' && releaseArtifact.downloadUrl.length > 0,
      buildMode: releaseArtifact?.buildMode ?? null,
    }
  })
}

function buildExpectedArtifactSummaries({ releaseModule, releasePublicationContract }) {
  return (releasePublicationContract.artifactTargets ?? []).map((target) => {
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    return {
      id: target.id,
      file: target.file,
      kind: target.kind,
      size: releaseArtifact?.size ?? null,
      sha256: releaseArtifact?.sha256 ?? null,
      buildMode: releaseArtifact?.buildMode ?? null,
      downloadUrlPresent: typeof releaseArtifact?.downloadUrl === 'string' && releaseArtifact.downloadUrl.length > 0,
      requiredForPublicAlpha: target.requiredForPublicAlpha === true,
    }
  })
}

function assertArtifactSummaryMatches(errors, actual, expected, label) {
  assert(errors, actual !== null && typeof actual === 'object', `${label} must be present`)
  for (const [field, value] of Object.entries(expected ?? {})) {
    assert(errors, actual?.[field] === value, `${label} ${field} mismatch`)
  }
}

function assertReportArtifactMatches(errors, reportArtifact, expected, releaseRoot, label, { requireHash = false } = {}) {
  assert(errors, reportArtifact !== null && typeof reportArtifact === 'object', `${label} artifact must be present`)
  assert(errors, reportArtifact?.file === expected?.file, `${label} artifact file mismatch`)
  assert(errors, reportArtifact?.kind === expected?.kind, `${label} artifact kind mismatch`)
  assert(errors, sameResolvedPath(reportArtifact?.path, path.join(releaseRoot, MODULE_ID, expected?.file ?? '')), `${label} artifact path mismatch`)
  assert(errors, typeof reportArtifact?.path === 'string' && fileExists(reportArtifact.path), `${label} artifact path must exist`)
  if (requireHash) {
    assert(errors, reportArtifact?.sha256 === expected?.sha256, `${label} artifact sha mismatch`)
    assert(errors, reportArtifact?.size === expected?.size, `${label} artifact size mismatch`)
    assert(errors, reportArtifact?.buildMode === expected?.buildMode, `${label} artifact build mode mismatch`)
    assert(errors, reportArtifact?.downloadUrlPresent === expected?.downloadUrlPresent, `${label} artifact download URL flag mismatch`)
  }
}

function validateDistributionRoadmapPreflightReport(errors, { report, edition, expectedSummaries, workspaceRoot, releaseIndex, distributionContract, distributionApprovalContract, launchRoadmap, launcherFlowContract, crossPlatformParity, conformance, expectedFreshReport = null }) {
  const launcherEntry = (launcherFlowContract.editionMatrix ?? []).find((entry) => entry.id === edition.id)
  const parityTarget = (crossPlatformParity.runtimeTargets ?? []).find((target) => target.id === edition.runtimeTarget)
  const artifactTarget = (distributionContract.artifactTargets ?? []).find((target) => target.id === edition.id)
  const editionManifestPath = launcherEntry ? path.join(workspaceRoot, launcherEntry.editionRepo, launcherEntry.releaseManifest) : null
  const editionManifest = editionManifestPath && fileExists(editionManifestPath) ? readJson(editionManifestPath) : null
  const launchRoadmapPhases = new Map((launchRoadmap.phases ?? []).map((phase) => [phase.id, phase]))
  const requiredBlockers = [
    'release_index_download_urls_missing',
    'real_launcher_install_update_repair_rollback_execution_missing',
    'native_standalone_neoforge_runtime_parity_execution_missing',
    'public_alpha_coop_session_test_missing',
    'public_alpha_release_index_approval_missing',
  ]
  const requiredProofs = [
    'distribution_contract_loaded',
    'distribution_approval_contract_loaded',
    'launch_roadmap_loaded',
    'launcher_flow_contract_loaded',
    'cross_platform_parity_contract_loaded',
    'edition_manifest_matches_distribution_matrix',
    'public_alpha_minimums_match_mvp_floor',
    'compiled_release_artifacts_have_sha256_and_size',
    'release_index_stays_warning_until_uploads',
    'roadmap_relaxed_default_and_invariants_declared',
    'public_alpha_blocked_until_real_distribution_execution',
  ]

  assert(errors, launcherEntry !== undefined, `${edition.id} distribution roadmap launcher matrix entry missing`)
  assert(errors, parityTarget !== undefined, `${edition.id} distribution roadmap parity target missing`)
  assert(errors, artifactTarget !== undefined, `${edition.id} distribution roadmap artifact target missing`)
  assert(errors, editionManifest !== null, `${edition.id} distribution roadmap release manifest missing`)
  assert(errors, report.schema === 'echo.openlands.edition.distribution_roadmap_report.v1', `${edition.id} distribution roadmap schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.id} distribution roadmap status must be preflight_passed`)
  assert(errors, report.publicAlphaReady === false, `${edition.id} distribution roadmap must not mark public alpha ready`)
  assert(errors, report.realDistributionExecutionRequiredBeforePublicAlpha === true, `${edition.id} distribution roadmap must require real distribution execution`)
  assert(errors, report.packId === launcherEntry?.packId, `${edition.id} distribution roadmap pack id mismatch`)
  assert(errors, report.displayName === editionManifest?.displayName, `${edition.id} distribution roadmap display name mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.id} distribution roadmap runtime target mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.id} distribution roadmap module id mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.id} distribution roadmap module version mismatch`)
  assert(errors, report.contracts?.distribution === DISTRIBUTION_CONTRACT_PATH, `${edition.id} distribution roadmap distribution contract mismatch`)
  assert(errors, report.contracts?.distributionApproval === DISTRIBUTION_APPROVAL_CONTRACT_PATH, `${edition.id} distribution roadmap distribution approval contract mismatch`)
  assert(errors, report.contracts?.launchRoadmap === LAUNCH_ROADMAP_PATH, `${edition.id} distribution roadmap launch roadmap contract mismatch`)
  assert(errors, report.contracts?.launcherFlow === LAUNCHER_FLOW_CONTRACT_PATH, `${edition.id} distribution roadmap launcher flow contract mismatch`)
  assert(errors, report.contracts?.crossPlatformParity === CROSS_PLATFORM_PARITY_PATH, `${edition.id} distribution roadmap parity contract mismatch`)
  assert(errors, report.contracts?.conformance === CONFORMANCE_PATH, `${edition.id} distribution roadmap conformance contract mismatch`)

  assert(errors, report.releaseManifest?.packId === editionManifest?.packId, `${edition.id} distribution roadmap manifest pack id mismatch`)
  assert(errors, report.releaseManifest?.runtimeTarget === editionManifest?.runtimeTarget, `${edition.id} distribution roadmap manifest runtime target mismatch`)
  assert(errors, report.releaseManifest?.loader === editionManifest?.loader, `${edition.id} distribution roadmap manifest loader mismatch`)
  assert(errors, report.releaseManifest?.moduleArtifactFamily === editionManifest?.moduleArtifactFamily, `${edition.id} distribution roadmap manifest artifact family mismatch`)
  assert(errors, report.releaseManifest?.moduleArtifactPattern === editionManifest?.moduleArtifactPattern, `${edition.id} distribution roadmap manifest artifact pattern mismatch`)
  assert(errors, sameSet(report.releaseManifest?.requiredPublicAlphaEvidence, editionManifest?.requiredPublicAlphaEvidence ?? []), `${edition.id} distribution roadmap public alpha evidence mismatch`)

  assert(errors, report.releaseIndex?.releaseId === releaseIndex?.releaseId, `${edition.id} distribution roadmap release id mismatch`)
  assert(errors, report.releaseIndex?.currentAllowedState === distributionContract.releaseIndexStates?.currentAllowedState, `${edition.id} distribution roadmap release index state mismatch`)
  assert(errors, report.releaseIndex?.launcherCurrentIndexStateAllowed === launcherFlowContract.artifactVerification?.currentIndexStateAllowed, `${edition.id} distribution roadmap launcher index state mismatch`)
  assert(errors, sameSet(report.releaseIndex?.approvedRequires, distributionContract.releaseIndexStates?.approvedRequires ?? []), `${edition.id} distribution roadmap approved requirements mismatch`)
  assert(errors, report.releaseIndex?.uploadedArtifactUrlsPresent === false, `${edition.id} distribution roadmap must keep uploaded artifact URLs missing before publication`)
  assert(errors, sameSet((report.releaseIndex?.artifactSummaries ?? []).map((artifact) => artifact.id), expectedSummaries.map((artifact) => artifact.id)), `${edition.id} distribution roadmap artifact summary id mismatch`)
  for (const expected of expectedSummaries) {
    const actual = (report.releaseIndex?.artifactSummaries ?? []).find((artifact) => artifact.file === expected.file)
    assertArtifactSummaryMatches(errors, actual, expected, `${edition.id} distribution roadmap artifact ${expected.file}`)
  }

  assert(errors, report.editionMatrix?.id === edition.id, `${edition.id} distribution roadmap edition matrix id mismatch`)
  assert(errors, sameJson(report.editionMatrix?.launcherEntry, launcherEntry), `${edition.id} distribution roadmap launcher matrix mismatch`)
  assert(errors, sameJson(report.editionMatrix?.parityTarget, parityTarget), `${edition.id} distribution roadmap parity target mismatch`)
  assert(errors, sameJson(report.editionMatrix?.artifactTarget, artifactTarget), `${edition.id} distribution roadmap artifact target mismatch`)

  assert(errors, report.publicAlphaMinimum?.biomes === distributionContract.publicAlphaMinimum?.biomes, `${edition.id} distribution roadmap Public Alpha biome minimum mismatch`)
  assert(errors, sameJson(report.publicAlphaMinimum?.blocks, distributionContract.publicAlphaMinimum?.blocks), `${edition.id} distribution roadmap Public Alpha block minimum mismatch`)
  assert(errors, sameJson(report.publicAlphaMinimum?.items, distributionContract.publicAlphaMinimum?.items), `${edition.id} distribution roadmap Public Alpha item minimum mismatch`)
  assert(errors, report.publicAlphaMinimum?.creatures === distributionContract.publicAlphaMinimum?.creatures, `${edition.id} distribution roadmap Public Alpha creature minimum mismatch`)
  assert(errors, sameSet(report.publicAlphaMinimum?.requiredLoops, distributionContract.publicAlphaMinimum?.requiredLoops ?? []), `${edition.id} distribution roadmap Public Alpha loops mismatch`)
  assert(errors, sameJson(report.publicAlphaMinimum?.coOp, distributionContract.publicAlphaMinimum?.coOp), `${edition.id} distribution roadmap Public Alpha co-op mismatch`)
  const publicAlphaConformanceCounts = {
    biomes: conformance.biomeRegistry?.length ?? 0,
    blocks: (conformance.blockRegistry?.length ?? 0) + (conformance.foundationRegistries?.blocksMovedToFoundation?.length ?? 0),
    items: (conformance.itemRegistry?.length ?? 0) + (conformance.foundationRegistries?.itemsMovedToFoundation?.length ?? 0),
    creatures: conformance.creatureRegistry?.length ?? 0,
  }
  assert(errors, report.publicAlphaMinimum?.conformanceCounts?.biomes === publicAlphaConformanceCounts.biomes, `${edition.id} distribution roadmap conformance biome count mismatch`)
  assert(errors, report.publicAlphaMinimum?.conformanceCounts?.blocks === publicAlphaConformanceCounts.blocks, `${edition.id} distribution roadmap conformance block count mismatch`)
  assert(errors, report.publicAlphaMinimum?.conformanceCounts?.items === publicAlphaConformanceCounts.items, `${edition.id} distribution roadmap conformance item count mismatch`)
  assert(errors, report.publicAlphaMinimum?.conformanceCounts?.creatures === publicAlphaConformanceCounts.creatures, `${edition.id} distribution roadmap conformance creature count mismatch`)
  assert(errors, report.publicAlphaMinimum?.mvpMinimumsMet?.biomes === true, `${edition.id} distribution roadmap biome minimum must pass`)
  assert(errors, report.publicAlphaMinimum?.mvpMinimumsMet?.blocks === true, `${edition.id} distribution roadmap block minimum must pass`)
  assert(errors, report.publicAlphaMinimum?.mvpMinimumsMet?.items === true, `${edition.id} distribution roadmap item minimum must pass`)
  assert(errors, report.publicAlphaMinimum?.mvpMinimumsMet?.creatures === true, `${edition.id} distribution roadmap creature minimum must pass`)

  assert(errors, report.roadmap?.defaultRule === launchRoadmap.defaultRule, `${edition.id} distribution roadmap default rule mismatch`)
  assert(errors, sameJson(report.roadmap?.phaseIds, (launchRoadmap.phases ?? []).map((phase) => phase.id)), `${edition.id} distribution roadmap phase order mismatch`)
  assert(errors, sameJson(report.roadmap?.mvpScope, launchRoadmapPhases.get('mvp')?.scope), `${edition.id} distribution roadmap MVP scope mismatch`)
  assert(errors, sameJson(report.roadmap?.publicAlphaScope, launchRoadmapPhases.get('public_alpha')?.scope), `${edition.id} distribution roadmap Public Alpha scope mismatch`)
  assert(errors, sameJson(report.roadmap?.oneDotZeroLoops, launchRoadmapPhases.get('one_dot_zero')?.scope?.coreLoops), `${edition.id} distribution roadmap 1.0 loops mismatch`)
  assert(errors, sameSet(report.roadmap?.nonNegotiableInvariants, launchRoadmap.nonNegotiableInvariants ?? []), `${edition.id} distribution roadmap invariant mismatch`)

  assert(errors, sameSet((report.launcherFlows ?? []).map((flow) => flow.id), (launcherFlowContract.requiredLauncherFlows ?? []).map((flow) => flow.id)), `${edition.id} distribution roadmap launcher flow ids mismatch`)
  for (const expectedFlow of launcherFlowContract.requiredLauncherFlows ?? []) {
    const flow = (report.launcherFlows ?? []).find((entry) => entry.id === expectedFlow.id)
    const distributionFlow = (distributionContract.launcherGates ?? []).find((entry) => entry.id === expectedFlow.id)
    assert(errors, flow !== undefined, `${edition.id} distribution roadmap launcher flow ${expectedFlow.id} missing`)
    if (!flow) continue
    assert(errors, sameSet(flow.appliesTo, expectedFlow.appliesTo), `${edition.id} distribution roadmap launcher flow ${expectedFlow.id} appliesTo mismatch`)
    assert(errors, flow.appliesTo?.includes(edition.runtimeTarget), `${edition.id} distribution roadmap launcher flow ${expectedFlow.id} must apply to edition`)
    assert(errors, sameSet(flow.mustVerify, expectedFlow.mustVerify), `${edition.id} distribution roadmap launcher flow ${expectedFlow.id} checks mismatch`)
    assert(errors, sameSet(flow.mustVerify, distributionFlow?.mustVerify ?? []), `${edition.id} distribution roadmap launcher flow ${expectedFlow.id} distribution checks mismatch`)
    assert(errors, flow.evidenceAttachment === expectedFlow.evidenceAttachment, `${edition.id} distribution roadmap launcher flow ${expectedFlow.id} evidence attachment mismatch`)
  }

  for (const blocker of requiredBlockers) {
    assert(errors, report.blockedBy?.includes(blocker), `${edition.id} distribution roadmap missing blocker ${blocker}`)
  }
  for (const proof of requiredProofs) {
    assert(errors, report.proofs?.includes(proof), `${edition.id} distribution roadmap missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(
      errors,
      sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)),
      `${edition.id} distribution roadmap report stale against generator dry-run`,
    )
  }
}

function validateLauncherFlowPreflightReport(errors, { report, edition, editionSummary, workspaceRoot, releaseRoot, releaseIndex, releaseModule, launcherFlowContract, runtimePlan, distributionContract, expectedFreshReport = null }) {
  const matrix = (launcherFlowContract.editionMatrix ?? []).find((entry) => entry.id === edition.id)
  const publicAlphaGate = (runtimePlan.acceptanceGates ?? []).find((gate) => gate.id === 'public_alpha')
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const artifactFile = matrix?.artifactPattern ?? editionSummary?.file
  const artifactPath = path.join(releaseRoot, MODULE_ID, artifactFile ?? '')
  const releaseArtifact = artifactByFile(releaseModule, artifactFile)
  const editionRoot = matrix ? path.join(workspaceRoot, matrix.editionRepo) : null
  const editionManifestPath = editionRoot && matrix ? path.join(editionRoot, matrix.releaseManifest) : null
  const evidenceTemplatePath = editionRoot ? path.join(editionRoot, 'evidence', 'runtime-evidence.template.json') : null
  const editionManifest = editionManifestPath && fileExists(editionManifestPath) ? readJson(editionManifestPath) : null
  const evidenceTemplate = evidenceTemplatePath && fileExists(evidenceTemplatePath) ? readJson(evidenceTemplatePath) : null

  assert(errors, matrix !== undefined, `${edition.id} launcher flow matrix entry missing`)
  assert(errors, publicAlphaGate !== undefined, `${edition.id} launcher flow public alpha gate missing`)
  assert(errors, releaseArtifact !== undefined, `${edition.id} launcher flow release artifact missing`)
  assert(errors, editionManifest !== null, `${edition.id} launcher flow edition manifest missing`)
  assert(errors, evidenceTemplate !== null, `${edition.id} launcher flow runtime evidence template missing`)
  assert(errors, report.status === 'preflight_passed', `${edition.id} launcher flow status must be preflight_passed`)
  assert(errors, report.publicAlphaReady === false, `${edition.id} launcher flow must not mark public alpha ready`)
  assert(errors, report.packId === matrix?.packId, `${edition.id} launcher flow pack id mismatch`)
  assert(errors, report.displayName === editionManifest?.displayName, `${edition.id} launcher flow display name mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.id} launcher flow runtime target mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.id} launcher flow module id mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.id} launcher flow module version mismatch`)
  assert(errors, report.releaseId === releaseIndex?.releaseId, `${edition.id} launcher flow release id mismatch`)
  assert(errors, sameResolvedPath(report.releaseManifest, releaseIndexPath), `${edition.id} launcher flow release index path mismatch`)
  assert(errors, report.launcherFlowContract === LAUNCHER_FLOW_CONTRACT_PATH, `${edition.id} launcher flow contract path mismatch`)
  assert(errors, report.distributionContract === DISTRIBUTION_CONTRACT_PATH, `${edition.id} launcher flow distribution contract path mismatch`)
  assert(errors, report.releaseIndexStateAllowed === (launcherFlowContract.artifactVerification?.currentIndexStateAllowed ?? distributionContract.releaseIndexStates?.currentAllowedState), `${edition.id} launcher flow release index warning state mismatch`)
  assert(errors, sameSet(report.requiredPublicAlphaEvidence, publicAlphaGate?.requiresEvidence ?? []), `${edition.id} launcher flow public alpha evidence mismatch`)
  assert(errors, sameSet(report.requiredPublicAlphaEvidence, editionManifest?.requiredPublicAlphaEvidence ?? []), `${edition.id} launcher flow manifest public alpha evidence mismatch`)
  assert(errors, sameSet(report.requiredPublicAlphaEvidence, evidenceTemplate?.requiredPublicAlphaEvidence ?? []), `${edition.id} launcher flow evidence template public alpha evidence mismatch`)

  assertReportArtifactMatches(errors, report.artifact, editionSummary, releaseRoot, `${edition.id} launcher flow`, { requireHash: true })
  assert(errors, report.artifact?.kind === matrix?.artifactFamily, `${edition.id} launcher flow artifact family mismatch`)
  assert(errors, report.artifact?.buildMode === 'compiled-runtime', `${edition.id} launcher flow artifact must be compiled-runtime`)
  assert(errors, report.artifact?.downloadUrlPresent === Boolean(releaseArtifact?.downloadUrl), `${edition.id} launcher flow artifact download URL flag mismatch`)
  assert(errors, releaseArtifact?.kind === matrix?.artifactFamily, `${edition.id} launcher flow release artifact kind mismatch`)
  assert(errors, normalizeRuntimeTarget(releaseArtifact?.runtimeTarget) === edition.runtimeTarget, `${edition.id} launcher flow release artifact runtime target mismatch`)
  assert(errors, releaseArtifact?.sha256 === report.artifact?.sha256, `${edition.id} launcher flow release artifact sha mismatch`)
  assert(errors, releaseArtifact?.size === report.artifact?.size, `${edition.id} launcher flow release artifact size mismatch`)
  if (fileExists(artifactPath)) {
    assert(errors, sha256File(artifactPath) === report.artifact?.sha256, `${edition.id} launcher flow local artifact sha mismatch`)
    assert(errors, fs.statSync(artifactPath).size === report.artifact?.size, `${edition.id} launcher flow local artifact size mismatch`)
  }
  for (const descriptorEntry of matrix?.requiredDescriptors ?? []) {
    assert(errors, report.artifact?.packageEntriesChecked?.includes(descriptorEntry), `${edition.id} launcher flow missing package descriptor ${descriptorEntry}`)
  }
  if (matrix?.artifactFamily === 'echo-addon') {
    assert(errors, report.artifact?.packageEntriesChecked?.includes('echo-addon-package.json'), `${edition.id} launcher flow missing echo-addon package entry`)
    assert(errors, typeof report.artifact?.nestedRuntimeEntry === 'string' && report.artifact.nestedRuntimeEntry.length > 0, `${edition.id} launcher flow missing nested runtime entry`)
    assert(errors, report.artifact?.packageEntriesChecked?.includes(report.artifact?.nestedRuntimeEntry), `${edition.id} launcher flow package entries must include nested runtime`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.id} launcher flow non-addon nested runtime entry mismatch`)
  }
  for (const runtimeEntry of [
    LAUNCHER_FLOW_CONTRACT_PATH,
    RUNTIME_ADAPTER_LOAD_PLAN_PATH,
    'data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json',
    'data/echoopenlandsprotocol/openlands/systems/launcher_execution_acceptance.json',
    'com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.class',
  ]) {
    assert(errors, report.artifact?.runtimeEntriesChecked?.includes(runtimeEntry), `${edition.id} launcher flow missing runtime entry ${runtimeEntry}`)
  }

  const expectedFlows = (launcherFlowContract.requiredLauncherFlows ?? []).filter((flow) => (flow.appliesTo ?? []).includes(edition.runtimeTarget))
  assert(errors, sameSet((report.flowResults ?? []).map((flow) => flow.id), expectedFlows.map((flow) => flow.id)), `${edition.id} launcher flow ids mismatch`)
  for (const expectedFlow of expectedFlows) {
    const flow = (report.flowResults ?? []).find((entry) => entry.id === expectedFlow.id)
    const distributionFlow = (distributionContract.launcherGates ?? []).find((entry) => entry.id === expectedFlow.id)
    assert(errors, flow !== undefined, `${edition.id} launcher flow ${expectedFlow.id} missing`)
    if (!flow) continue
    assert(errors, flow.displayName === expectedFlow.displayName, `${edition.id} launcher flow ${expectedFlow.id} display name mismatch`)
    assert(errors, flow.status === 'preflight_mapped', `${edition.id} launcher flow ${expectedFlow.id} must be preflight_mapped`)
    assert(errors, sameSet(flow.preconditions, expectedFlow.preconditions), `${edition.id} launcher flow ${expectedFlow.id} preconditions mismatch`)
    assert(errors, sameSet(flow.mustVerify, expectedFlow.mustVerify), `${edition.id} launcher flow ${expectedFlow.id} mustVerify mismatch`)
    assert(errors, sameSet(flow.mustVerify, distributionFlow?.mustVerify ?? []), `${edition.id} launcher flow ${expectedFlow.id} distribution checks mismatch`)
    assert(errors, sameSet(flow.additionalAssertions, expectedFlow.additionalAssertions), `${edition.id} launcher flow ${expectedFlow.id} assertions mismatch`)
    assert(errors, sameJson(flow.worldStatePolicy, expectedFlow.worldStatePolicy), `${edition.id} launcher flow ${expectedFlow.id} world state policy mismatch`)
    assert(errors, flow.evidenceAttachment === expectedFlow.evidenceAttachment, `${edition.id} launcher flow ${expectedFlow.id} evidence attachment mismatch`)
    assert(errors, flow.realLauncherExecutionRequiredBeforePublicAlpha === true, `${edition.id} launcher flow ${expectedFlow.id} must require real launcher execution`)
  }

  assert(errors, sameJson(report.statePreservation, launcherFlowContract.statePreservation), `${edition.id} launcher flow state preservation mismatch`)
  assert(errors, report.blockedBy?.includes('real_launcher_install_update_repair_rollback_execution_missing'), `${edition.id} launcher flow missing real launcher blocker`)
  if (!releaseArtifact?.downloadUrl) {
    assert(errors, report.blockedBy?.includes('release_artifact_download_url_missing'), `${edition.id} launcher flow missing release artifact URL blocker`)
  }
  for (const proof of [
    'launcher_flow_contract_loaded',
    'edition_matrix_matches_manifest',
    'release_manifest_template_loaded',
    'compiled_runtime_artifact_present',
    'artifact_sha256_matches_release_manifest',
    'artifact_size_matches_release_manifest',
    'required_descriptors_present',
    'install_update_repair_rollback_flows_mapped',
    'state_preservation_fields_mapped',
    'public_alpha_blocked_until_real_launcher_execution',
  ]) {
    assert(errors, report.proofs?.includes(proof), `${edition.id} launcher flow missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(
      errors,
      sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)),
      `${edition.id} launcher flow report stale against generator dry-run`,
    )
  }
}

function validateLegalPreflightReport(errors, { report, edition, editionSummary, moduleRoot, releaseRoot, legalAuditContract, contentPolicy, assetManifest, expectedFreshReport = null }) {
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const assetsRoot = path.join(resourcesRoot, 'assets', MODULE_ID)
  const blocks = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json')).blocks ?? []
  const items = readJson(path.join(dataRoot, 'items', 'mvp_items.json')).items ?? []
  const recipes = readJson(path.join(dataRoot, 'recipes', 'mvp_recipes.json')).recipes ?? []
  const assetPathCount = listFiles(assetsRoot).length
  const requiredRuntimeEntries = [
    CONTENT_POLICY_PATH,
    LEGAL_CONTENT_AUDIT_PATH,
    'data/echoopenlandsprotocol/openlands/systems/final_release_review_acceptance.json',
    ASSET_MANIFEST_PATH,
    'assets/echoopenlandsprotocol/lang/en_us.json',
    'com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.class',
  ]
  const requiredBlockers = [
    'final_asset_human_review_missing',
    'placeholder_assets_block_public_release',
    'generated_output_human_review_missing',
  ]
  const requiredProofs = [
    'legal_content_audit_contract_loaded',
    'content_policy_loaded',
    'no_forbidden_public_terms_in_public_identity',
    'canonical_echo_ids_retained',
    'asset_manifest_placeholder_policy_applied',
    'mvp_asset_paths_resolve',
    'recipe_identity_uses_openlands_stations',
    'generated_artifact_paths_audited',
    'runtime_descriptor_adapter_metadata_exceptions_recorded',
    'public_release_blocked_until_final_asset_human_review',
  ]

  assert(errors, report.schema === 'echo.openlands.edition.legal_content_audit_report.v1', `${edition.id} legal audit schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.id} legal audit status must be preflight_passed`)
  assert(errors, report.publicReleaseAllowed === false, `${edition.id} legal audit must not allow public release before final review`)
  assert(errors, report.packId === (edition.id === 'native' ? 'openlands-native-edition' : edition.id === 'neoforge' ? 'openlands-neoforge-edition' : 'openlands-standalone-edition'), `${edition.id} legal audit pack id mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.id} legal audit runtime target mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.id} legal audit module id mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.id} legal audit module version mismatch`)
  assert(errors, report.legalAuditContract === LEGAL_CONTENT_AUDIT_PATH, `${edition.id} legal audit contract path mismatch`)
  assert(errors, report.contentPolicy === CONTENT_POLICY_PATH, `${edition.id} legal audit content policy path mismatch`)
  assert(errors, report.assetManifest === ASSET_MANIFEST_PATH, `${edition.id} legal audit asset manifest path mismatch`)
  assert(errors, contentPolicy.namespace === MODULE_ID, `${edition.id} legal audit content policy namespace mismatch`)
  assert(errors, legalAuditContract.policySource === 'config/content_policy.json', `${edition.id} legal audit policy source mismatch`)
  assert(errors, legalAuditContract.assetManifest === ASSET_MANIFEST_PATH, `${edition.id} legal audit asset manifest contract mismatch`)
  assert(errors, assetManifest.status === legalAuditContract.assetRules?.currentStatus, `${edition.id} legal audit asset manifest status mismatch`)
  assert(errors, assetManifest.publicReleaseAllowedWithPlaceholders === false, `${edition.id} legal audit placeholder release policy mismatch`)
  assert(errors, legalAuditContract.publicAlphaGate?.requiresHumanReview === true, `${edition.id} legal audit human review gate mismatch`)
  assert(errors, legalAuditContract.publicAlphaGate?.requiresNoForbiddenPublicTerms === true, `${edition.id} legal audit forbidden term gate mismatch`)
  assert(errors, legalAuditContract.publicAlphaGate?.requiresNoBorrowedAssets === true, `${edition.id} legal audit borrowed asset gate mismatch`)
  assert(errors, legalAuditContract.publicAlphaGate?.requiresGeneratedOutputAudit?.includes(edition.runtimeTarget), `${edition.id} legal audit generated output target missing`)

  assertReportArtifactMatches(errors, report.artifact, editionSummary, releaseRoot, `${edition.id} legal audit`)
  for (const entry of requiredRuntimeEntries) {
    assert(errors, report.artifact?.runtimeEntriesChecked?.includes(entry), `${edition.id} legal audit missing runtime entry ${entry}`)
  }
  if (edition.id === 'native') {
    assert(errors, typeof report.artifact?.nestedRuntimeEntry === 'string' && report.artifact.nestedRuntimeEntry.length > 0, `${edition.id} legal audit nested runtime entry missing`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.id} legal audit nested runtime entry mismatch`)
  }

  assert(errors, Number.isInteger(report.scanSummary?.publicIdentityValues) && report.scanSummary.publicIdentityValues > 0, `${edition.id} legal audit must scan public identity values`)
  assert(errors, report.scanSummary?.assetPaths === assetPathCount, `${edition.id} legal audit asset path count mismatch`)
  assert(errors, report.scanSummary?.forbiddenPublicTerms === (legalAuditContract.forbiddenPublicTerms ?? []).length, `${edition.id} legal audit forbidden term count mismatch`)
  assert(errors, report.scanSummary?.blockAssetsChecked === blocks.length, `${edition.id} legal audit block asset count mismatch`)
  assert(errors, report.scanSummary?.itemAssetsChecked === items.length, `${edition.id} legal audit item asset count mismatch`)
  assert(errors, report.scanSummary?.recipesChecked === recipes.length, `${edition.id} legal audit recipe count mismatch`)
  assert(errors, report.scanSummary?.descriptorPublicFieldsChecked === (edition.id === 'neoforge' ? 4 : 0), `${edition.id} legal audit descriptor public field count mismatch`)

  assert(errors, report.policyResults?.noForbiddenPublicTerms === true, `${edition.id} legal audit forbidden public term scan must pass`)
  assert(errors, report.policyResults?.canonicalEchoIdsRetained === true, `${edition.id} legal audit canonical Echo id policy mismatch`)
  assert(errors, report.policyResults?.borrowedAssetPathsDetected === false, `${edition.id} legal audit borrowed asset path policy mismatch`)
  assert(errors, report.policyResults?.placeholderCoverageComplete === true, `${edition.id} legal audit placeholder coverage mismatch`)
  assert(errors, report.policyResults?.publicReleaseAllowedWithPlaceholders === false, `${edition.id} legal audit placeholder public release policy mismatch`)
  assert(errors, report.policyResults?.requiresHumanArtLegalReview === true, `${edition.id} legal audit must require human art/legal review`)
  if (edition.id === 'neoforge') {
    assert(errors, report.adapterMetadataExceptions?.some((entry) => String(entry).includes('modId="minecraft"') && String(entry).includes('runtime loader metadata')), `${edition.id} legal audit missing adapter metadata exception`)
  } else {
    assert(errors, Array.isArray(report.adapterMetadataExceptions) && report.adapterMetadataExceptions.length === 0, `${edition.id} legal audit adapter metadata exceptions mismatch`)
  }
  for (const blocker of requiredBlockers) {
    assert(errors, report.blockedBy?.includes(blocker), `${edition.id} legal audit missing blocker ${blocker}`)
  }
  for (const proof of requiredProofs) {
    assert(errors, report.proofs?.includes(proof), `${edition.id} legal audit missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(
      errors,
      sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)),
      `${edition.id} legal audit report stale against generator dry-run`,
    )
  }
}

function validatePreflightReportFreshness(errors, { editionEvidence, moduleRoot, workspaceRoot, releaseIndex, releaseRoot, releasePublicationContract, releaseModule, launcherFlowContract, runtimePlan, distributionContract, distributionApprovalContract, launchRoadmap, crossPlatformParity, conformance, legalAuditContract, contentPolicy, assetManifest }) {
  const expectedSummaries = buildExpectedArtifactSummaries({ releaseModule, releasePublicationContract })
  for (const edition of EDITIONS) {
    const reports = editionEvidence.get(edition.id)?.reports
    if (!reports) continue
    const editionSummary = expectedSummaries.find((artifact) => artifact.id === edition.id)
    const editionRoot = path.join(workspaceRoot, edition.repo)
    const distributionReport = reports.distribution.report
    if (distributionReport) {
      const distributionDryRun = runGeneratorDryRunJson(errors, {
        label: `${edition.id} distribution roadmap`,
        moduleRoot,
        scriptName: 'generate-openlands-distribution-roadmap-report.mjs',
        args: [
          '--module-root',
          moduleRoot,
          '--release-root',
          releaseRoot,
          '--edition',
          edition.id,
          '--edition-root',
          editionRoot,
          '--out',
          reports.distribution.filePath,
        ],
      })
      validateDistributionRoadmapPreflightReport(errors, {
        report: distributionReport,
        edition,
        expectedSummaries,
        workspaceRoot,
        releaseIndex,
        distributionContract,
        distributionApprovalContract,
        launchRoadmap,
        launcherFlowContract,
        crossPlatformParity,
        conformance,
        expectedFreshReport: distributionDryRun,
      })
    }

    const launcherFlow = reports.launcherFlow.report
    if (launcherFlow) {
      const launcherFlowDryRun = runGeneratorDryRunJson(errors, {
        label: `${edition.id} launcher flow`,
        moduleRoot,
        scriptName: 'generate-openlands-launcher-flow-report.mjs',
        args: [
          '--module-root',
          moduleRoot,
          '--release-root',
          releaseRoot,
          '--edition',
          edition.id,
          '--edition-root',
          editionRoot,
          '--out',
          reports.launcherFlow.filePath,
        ],
      })
      validateLauncherFlowPreflightReport(errors, {
        report: launcherFlow,
        edition,
        editionSummary,
        workspaceRoot,
        releaseRoot,
        releaseIndex,
        releaseModule,
        launcherFlowContract,
        runtimePlan,
        distributionContract,
        expectedFreshReport: launcherFlowDryRun,
      })
    }

    const legal = reports.legal.report
    if (legal) {
      const legalDryRun = runGeneratorDryRunJson(errors, {
        label: `${edition.id} legal audit`,
        moduleRoot,
        scriptName: 'generate-openlands-legal-audit-report.mjs',
        args: [
          '--module-root',
          moduleRoot,
          '--release-root',
          releaseRoot,
          '--edition',
          edition.id,
          '--edition-root',
          editionRoot,
          '--out',
          reports.legal.filePath,
        ],
      })
      validateLegalPreflightReport(errors, {
        report: legal,
        edition,
        editionSummary,
        moduleRoot,
        releaseRoot,
        legalAuditContract,
        contentPolicy,
        assetManifest,
        expectedFreshReport: legalDryRun,
      })
    }
  }
}

function publicationDownloadsVerified(publicationManifest, expectedFileCount) {
  const publications = publicationManifest?.artifactPublications ?? []
  return publications.length === expectedFileCount && publications.every((publication) => {
    const verification = publication.downloadVerification ?? {}
    return publication.downloadUrl
      && verification.downloadAttempted === true
      && verification.sha256Matches === true
      && verification.sizeMatches === true
      && verification.downloadedSha256 === publication.sha256
      && verification.downloadedSize === publication.size
  })
}

function buildExpectedReadiness({ artifactResults, editionResults, runtimeExecution, launcherExecution, releasePublicationContract, releasePublicationManifest, releasePublicationRehearsal, editionManifestIndexPreview, runtimeGateIds, launcherGateIds, finalReviewGateIds, distributionGateIds }) {
  const launcherExecutionFlowIds = sortedUnique((launcherExecution.executionFlows ?? []).map((flow) => flow.id))
  const expectedPublicationFiles = sortedUnique((releasePublicationContract.artifactTargets ?? []).map((target) => target.file))
  const actualPublicationFiles = sortedUnique((releasePublicationManifest?.artifactPublications ?? []).map((publication) => publication.file))
  const releasePublicationManifestPresent = releasePublicationManifest?.schema === releasePublicationContract.reportContract?.schema
  const releasePublicationArtifactCoverageComplete = releasePublicationManifestPresent && JSON.stringify(actualPublicationFiles) === JSON.stringify(expectedPublicationFiles)
  const releasePublicationDownloadsVerified = releasePublicationArtifactCoverageComplete && publicationDownloadsVerified(releasePublicationManifest, expectedPublicationFiles.length)
  const releasePublicationApproved = releasePublicationDownloadsVerified
    && releasePublicationManifest?.status === 'approved'
    && (releasePublicationManifest?.artifactPublications ?? []).every((publication) => publication.urlStatus === 'approved' && publication.releaseIndexPatch?.patchApplied === true)
  const releasePublicationRehearsalLocalDownloadVerifiedCount = releasePublicationRehearsal?.summary?.localDownloadVerifiedCount
    ?? (releasePublicationRehearsal?.artifactResults ?? []).filter((artifact) => artifact.localDownloadBack?.sha256Matches === true && artifact.localDownloadBack?.sizeMatches === true).length
  const releasePublicationRehearsalPatchPreviewCount = releasePublicationRehearsal?.summary?.patchPreviewCount
    ?? (releasePublicationRehearsal?.artifactResults ?? []).filter((artifact) => artifact.releaseIndexPatchPreview?.patchApplied === false).length
  const releasePublicationRehearsalPresent = releasePublicationRehearsal?.schema === 'echo.openlands.release_publication_rehearsal_report.v1'
  const releasePublicationRehearsalPassed = releasePublicationRehearsalPresent
    && releasePublicationRehearsal?.status === 'preflight_passed'
    && releasePublicationRehearsal?.publicAlphaReady === false
    && releasePublicationRehearsal?.rehearsalOnly === true
    && releasePublicationRehearsal?.clearsDistributionGates === false
    && releasePublicationRehearsal?.clearsReleasePublicationGates === false
    && releasePublicationRehearsal?.summary?.artifactCount === expectedPublicationFiles.length
    && releasePublicationRehearsalLocalDownloadVerifiedCount === expectedPublicationFiles.length
    && releasePublicationRehearsalPatchPreviewCount === expectedPublicationFiles.length
  const editionManifestIndexPreviewPresent = editionManifestIndexPreview?.schema === 'echo.openlands.edition_manifest_index_preview.v1'
  const editionManifestIndexPreviewPassed = editionManifestIndexPreviewPresent
    && editionManifestIndexPreview?.status === 'preflight_passed'
    && editionManifestIndexPreview?.publicAlphaReady === false
    && editionManifestIndexPreview?.previewOnly === true
    && editionManifestIndexPreview?.clearsLauncherGates === false
    && editionManifestIndexPreview?.clearsDistributionGates === false
    && editionManifestIndexPreview?.summary?.editionCount === EDITIONS.length
    && editionManifestIndexPreview?.summary?.localArtifactCount === EDITIONS.length
    && editionManifestIndexPreview?.summary?.artifactSha256MatchCount === EDITIONS.length
    && editionManifestIndexPreview?.summary?.artifactSizeMatchCount === EDITIONS.length
    && editionManifestIndexPreview?.summary?.requiredDescriptorMatchCount === EDITIONS.length
    && editionManifestIndexPreview?.moduleRequirementResolution?.passed === true
    && editionManifestIndexPreview?.launcherChannelListing?.editionCount === EDITIONS.length
  const allArtifactsExist = artifactResults.every((artifact) => artifact.exists && artifact.releaseIndexEntryPresent && artifact.sha256Present && artifact.sha256MatchesFile && artifact.sizeMatchesFile)
  const allArtifactUrlsPresent = artifactResults.every((artifact) => artifact.downloadUrlPresent)
  const allRuntimeReportsPresent = editionResults.every((edition) => edition.reportsPresent.runtimeExecution)
  const allRuntimeGatesCleared = editionResults.every((edition) => edition.runtimeExecution.clearedRuntimeGates === runtimeGateIds.length && edition.runtimeExecution.remainingRuntimeGates === 0)
  const allLocalRuntimeRehearsalsPresent = editionResults.every((edition) => edition.reportsPresent.localRuntimeRehearsal)
  const allLocalRuntimeRehearsalsPassed = allLocalRuntimeRehearsalsPresent && editionResults.every((edition) =>
    edition.localRuntimeRehearsal.status === 'preflight_passed'
    && edition.localRuntimeRehearsal.scenarioCount === (runtimeExecution.scenarios ?? []).length
    && edition.localRuntimeRehearsal.rehearsalOnly === true
    && edition.localRuntimeRehearsal.clearsRuntimeGates === false
    && edition.localRuntimeRehearsal.publicAlphaReady === false)
  const allLauncherReportsPresent = editionResults.every((edition) => edition.reportsPresent.launcherExecution)
  const allLauncherGatesCleared = editionResults.every((edition) => edition.launcherExecution.clearedLauncherGates === launcherGateIds.length && edition.launcherExecution.remainingLauncherGates === 0)
  const allLauncherReady = allLauncherReportsPresent && allLauncherGatesCleared && editionResults.every((edition) => edition.launcherExecution.publicAlphaReady)
  const allLocalLauncherRehearsalsPresent = editionResults.every((edition) => edition.reportsPresent.localLauncherRehearsal)
  const allLocalLauncherRehearsalsPassed = allLocalLauncherRehearsalsPresent && editionResults.every((edition) =>
    edition.localLauncherRehearsal.status === 'preflight_passed'
    && edition.localLauncherRehearsal.flowCount === launcherExecutionFlowIds.length
    && edition.localLauncherRehearsal.rehearsalOnly === true
    && edition.localLauncherRehearsal.clearsLauncherGates === false
    && edition.localLauncherRehearsal.publicAlphaReady === false)
  const allLegalPreflightReportsPresent = editionResults.every((edition) => edition.reportsPresent.legal)
  const allFinalReviewReportsPresent = editionResults.every((edition) => edition.reportsPresent.finalReview)
  const allFinalReviewGatesCleared = editionResults.every((edition) => edition.finalReview.clearedFinalReviewGates === finalReviewGateIds.length && edition.finalReview.remainingFinalReviewGates === 0)
  const allLegalReady = allLegalPreflightReportsPresent && allFinalReviewReportsPresent && allFinalReviewGatesCleared && editionResults.every((edition) => edition.finalReview.publicReleaseReady)
  const allDistributionApprovalReportsPresent = editionResults.every((edition) => edition.reportsPresent.distributionApproval)
  const allDistributionGatesCleared = editionResults.every((edition) => edition.distributionApproval.clearedDistributionGates === distributionGateIds.length && edition.distributionApproval.remainingDistributionGates === 0)
  const allDistributionReady = allDistributionApprovalReportsPresent && allDistributionGatesCleared && editionResults.every((edition) => edition.distributionApproval.publicAlphaReady)
  const readinessChecks = {
    allArtifactsExist,
    allArtifactUrlsPresent,
    releasePublicationManifestPresent,
    releasePublicationArtifactCoverageComplete,
    releasePublicationRehearsalPresent,
    releasePublicationRehearsalPassed,
    editionManifestIndexPreviewPresent,
    editionManifestIndexPreviewPassed,
    releasePublicationDownloadsVerified,
    releasePublicationApproved,
    allRuntimeReportsPresent,
    allLocalRuntimeRehearsalsPresent,
    allLocalRuntimeRehearsalsPassed,
    allRuntimeGatesCleared,
    allLauncherReportsPresent,
    allLauncherGatesCleared,
    allLauncherReady,
    allLocalLauncherRehearsalsPresent,
    allLocalLauncherRehearsalsPassed,
    allLegalPreflightReportsPresent,
    allFinalReviewReportsPresent,
    allFinalReviewGatesCleared,
    allLegalReady,
    allDistributionApprovalReportsPresent,
    allDistributionGatesCleared,
    allDistributionReady,
  }
  const blockers = []
  if (!allArtifactsExist) blockers.push('local_artifact_or_release_index_metadata_missing')
  if (!allArtifactUrlsPresent) blockers.push('release_index_download_urls_missing')
  if (!releasePublicationManifestPresent) blockers.push('release_publication_manifest_missing')
  if (!releasePublicationArtifactCoverageComplete) blockers.push('release_publication_artifact_coverage_mismatch')
  if (!releasePublicationRehearsalPresent) blockers.push('release_publication_rehearsal_report_missing')
  if (!releasePublicationRehearsalPassed) blockers.push('release_publication_rehearsal_failed')
  if (!editionManifestIndexPreviewPresent) blockers.push('edition_manifest_index_preview_missing')
  if (!editionManifestIndexPreviewPassed) blockers.push('edition_manifest_index_preview_failed')
  if (!releasePublicationDownloadsVerified) blockers.push('download_verification_missing')
  if (!releasePublicationApproved) blockers.push('release_index_patch_not_approved')
  for (const blocker of releasePublicationManifest?.blockedBy ?? []) blockers.push(blocker)
  if (!allRuntimeReportsPresent) blockers.push('runtime_execution_reports_missing')
  if (!allLocalRuntimeRehearsalsPresent) blockers.push('local_runtime_rehearsal_reports_missing')
  if (!allLocalRuntimeRehearsalsPassed) blockers.push('local_runtime_rehearsal_failed')
  if (!allRuntimeGatesCleared) blockers.push('runtime_execution_gates_not_cleared')
  if (!allLauncherReportsPresent) blockers.push('launcher_execution_reports_missing')
  if (!allLauncherGatesCleared) blockers.push('real_launcher_install_update_repair_rollback_missing')
  if (!allLocalLauncherRehearsalsPresent) blockers.push('local_launcher_rehearsal_reports_missing')
  if (!allLocalLauncherRehearsalsPassed) blockers.push('local_launcher_rehearsal_failed')
  if (!allFinalReviewReportsPresent) blockers.push('final_release_review_reports_missing')
  if (!allLegalReady) blockers.push('final_asset_legal_review_missing')
  if (!allDistributionApprovalReportsPresent) blockers.push('distribution_approval_reports_missing')
  if (!allDistributionReady) blockers.push('distribution_approval_missing')
  return {
    readinessChecks,
    publicAlphaReady: Object.values(readinessChecks).every((value) => value === true),
    blockers: sortedUnique(blockers),
  }
}

function validatePhaseReadiness(errors, { report, reportPath, production, readinessChecks }) {
  const phaseReadiness = report.phaseReadiness
  assert(errors, phaseReadiness !== null && typeof phaseReadiness === 'object', 'release readiness phaseReadiness must be present')
  if (!phaseReadiness || typeof phaseReadiness !== 'object') return

  const phases = Array.isArray(phaseReadiness.phases) ? phaseReadiness.phases : []
  const productionPhases = production.phases ?? []
  const productionPhaseById = new Map(productionPhases.map((phase) => [phase.id, phase]))
  const reportBlockers = report.blockers ?? []
  const mappedBlockers = sortedUnique(phases.flatMap((phase) => phase.activeBlockers ?? []))
  const expectedMarkdownPath = defaultPhaseReadinessMarkdownPath(reportPath)

  assert(errors, phaseReadiness.schema === 'echo.openlands.release_phase_readiness.v1', 'release readiness phaseReadiness schema mismatch')
  assert(errors, phaseReadiness.phaseCount === productionPhases.length, 'release readiness phaseReadiness phase count mismatch')
  assert(errors, phases.length === productionPhases.length, 'release readiness phaseReadiness phases length mismatch')
  assert(errors, phaseReadiness.blockedPhaseCount === phases.filter((phase) => phase.status === 'blocked').length, 'release readiness phaseReadiness blocked phase count mismatch')
  assert(errors, phaseReadiness.blockerCount === reportBlockers.length, 'release readiness phaseReadiness blocker count mismatch')
  assert(errors, sameSet(phaseReadiness.blockers, reportBlockers), 'release readiness phaseReadiness blockers mismatch')
  assert(errors, sameSet(phaseReadiness.mappedBlockers, mappedBlockers), 'release readiness phaseReadiness mapped blockers mismatch')
  assert(errors, sameSet(phaseReadiness.unmappedBlockers, []), 'release readiness phaseReadiness must map every current blocker to at least one phase')
  assert(errors, sameResolvedPath(phaseReadiness.markdownPath, expectedMarkdownPath), 'release readiness phaseReadiness markdown path mismatch')
  assert(errors, sameSet(phases.map((phase) => phase.id), productionPhases.map((phase) => phase.id)), 'release readiness phaseReadiness phase ids mismatch')

  for (const phase of phases) {
    const productionPhase = productionPhaseById.get(phase.id)
    assert(errors, productionPhase !== undefined, `release readiness phaseReadiness unknown phase ${phase.id}`)
    if (productionPhase) {
      assert(errors, phase.order === productionPhase.order, `release readiness phaseReadiness ${phase.id} order mismatch`)
      assert(errors, phase.displayName === productionPhase.displayName, `release readiness phaseReadiness ${phase.id} display name mismatch`)
    }
    assert(errors, phase.status === 'ready' || phase.status === 'blocked', `release readiness phaseReadiness ${phase.id} status must be ready or blocked`)
    assert(errors, Array.isArray(phase.activeBlockers), `release readiness phaseReadiness ${phase.id} activeBlockers must be an array`)
    assert(errors, Array.isArray(phase.blockingChecks), `release readiness phaseReadiness ${phase.id} blockingChecks must be an array`)
    assert(errors, Array.isArray(phase.nextEvidence) && phase.nextEvidence.length > 0, `release readiness phaseReadiness ${phase.id} nextEvidence must be non-empty`)
    assert(errors, Array.isArray(phase.handoffArtifacts), `release readiness phaseReadiness ${phase.id} handoffArtifacts must be an array`)
    for (const blocker of phase.activeBlockers ?? []) {
      assert(errors, reportBlockers.includes(blocker), `release readiness phaseReadiness ${phase.id} maps inactive blocker ${blocker}`)
    }
    for (const artifact of phase.handoffArtifacts ?? []) {
      assert(errors, typeof artifact.id === 'string' && artifact.id.length > 0, `release readiness phaseReadiness ${phase.id} handoff artifact id missing`)
      assert(errors, typeof artifact.label === 'string' && artifact.label.length > 0, `release readiness phaseReadiness ${phase.id} handoff artifact label missing`)
      assert(errors, typeof artifact.path === 'string' && artifact.path.length > 0, `release readiness phaseReadiness ${phase.id} handoff artifact path missing`)
      assert(errors, typeof artifact.purpose === 'string' && artifact.purpose.length > 0, `release readiness phaseReadiness ${phase.id} handoff artifact purpose missing`)
      if (typeof artifact.path === 'string' && artifact.path.length > 0) {
        assert(errors, artifact.present === fileExists(artifact.path), `release readiness phaseReadiness ${phase.id} handoff artifact ${artifact.id} present flag mismatch`)
      }
    }
    for (const check of phase.blockingChecks ?? []) {
      assert(errors, Object.prototype.hasOwnProperty.call(readinessChecks, check.id), `release readiness phaseReadiness ${phase.id} unknown readiness check ${check.id}`)
      if (Object.prototype.hasOwnProperty.call(readinessChecks, check.id)) {
        assert(errors, check.passed === (readinessChecks[check.id] === true), `release readiness phaseReadiness ${phase.id} readiness check ${check.id} mismatch`)
      }
    }
    const expectedReady = (phase.activeBlockers ?? []).length === 0
      && (phase.blockingChecks ?? []).every((check) => check.passed === true)
    assert(errors, phase.readyForPublicAlpha === expectedReady, `release readiness phaseReadiness ${phase.id} ready flag mismatch`)
    assert(errors, phase.status === (expectedReady ? 'ready' : 'blocked'), `release readiness phaseReadiness ${phase.id} status does not match blockers/checks`)
  }

  assert(errors, fileExists(expectedMarkdownPath), `release readiness phase markdown missing: ${expectedMarkdownPath}`)
  if (fileExists(expectedMarkdownPath)) {
    assert(errors, readText(expectedMarkdownPath) === renderPhaseReadinessMarkdown(report), 'release readiness phase markdown stale against JSON phaseReadiness')
  }
}

function buildEditionEvidenceResult({ workspaceRoot, edition, runtimeGateIds, launcherGateIds, finalReviewGateIds, distributionGateIds }) {
  const reports = {
    runtimeExecution: editionReport(workspaceRoot, edition, edition.runtimeExecutionReport),
    localRuntimeRehearsal: editionReport(workspaceRoot, edition, edition.localRuntimeRehearsalReport),
    distribution: editionReport(workspaceRoot, edition, edition.distributionReport),
    launcherFlow: editionReport(workspaceRoot, edition, edition.launcherFlowReport),
    launcherExecution: editionReport(workspaceRoot, edition, edition.launcherExecutionReport),
    localLauncherRehearsal: editionReport(workspaceRoot, edition, edition.localLauncherRehearsalReport),
    legal: editionReport(workspaceRoot, edition, edition.legalReport),
    finalReview: editionReport(workspaceRoot, edition, edition.finalReviewReport),
    distributionApproval: editionReport(workspaceRoot, edition, edition.distributionApprovalReport),
  }
  const runtimeReport = reports.runtimeExecution.report
  const localRuntimeRehearsalReport = reports.localRuntimeRehearsal.report
  const distributionReport = reports.distribution.report
  const launcherFlowReport = reports.launcherFlow.report
  const launcherExecutionReport = reports.launcherExecution.report
  const localLauncherRehearsalReport = reports.localLauncherRehearsal.report
  const legalReport = reports.legal.report
  const finalReviewReport = reports.finalReview.report
  const distributionApprovalReport = reports.distributionApproval.report
  const clearedRuntimeGates = runtimeReport?.clearedRuntimeGates ?? []
  const remainingRuntimeGates = runtimeReport?.remainingRuntimeGates ?? runtimeGateIds
  const clearedLauncherGates = launcherExecutionReport?.clearedLauncherGates ?? []
  const remainingLauncherGates = launcherExecutionReport?.remainingLauncherGates ?? launcherGateIds
  const clearedFinalReviewGates = finalReviewReport?.clearedFinalReviewGates ?? []
  const remainingFinalReviewGates = finalReviewReport?.remainingFinalReviewGates ?? finalReviewGateIds
  const clearedDistributionGates = distributionApprovalReport?.clearedDistributionGates ?? []
  const remainingDistributionGates = distributionApprovalReport?.remainingDistributionGates ?? distributionGateIds

  return {
    reports,
    result: {
      reportsPresent: {
        runtimeExecution: runtimeReport !== null,
        localRuntimeRehearsal: localRuntimeRehearsalReport !== null,
        distribution: distributionReport !== null,
        launcherFlow: launcherFlowReport !== null,
        launcherExecution: launcherExecutionReport !== null,
        localLauncherRehearsal: localLauncherRehearsalReport !== null,
        legal: legalReport !== null,
        finalReview: finalReviewReport !== null,
        distributionApproval: distributionApprovalReport !== null,
      },
      runtimeExecution: {
        status: runtimeReport?.status ?? 'missing',
        scenarioCount: runtimeReport?.scenarioResults?.length ?? 0,
        clearedRuntimeGates: clearedRuntimeGates.length,
        remainingRuntimeGates: remainingRuntimeGates.length,
        publicAlphaReady: runtimeReport?.publicAlphaReady === true,
      },
      localRuntimeRehearsal: {
        status: localRuntimeRehearsalReport?.status ?? 'missing',
        scenarioCount: localRuntimeRehearsalReport?.scenarioResults?.length ?? 0,
        rehearsalOnly: localRuntimeRehearsalReport?.rehearsalOnly === true,
        clearsRuntimeGates: localRuntimeRehearsalReport?.clearsRuntimeGates === true,
        publicAlphaReady: localRuntimeRehearsalReport?.publicAlphaReady === true,
      },
      distribution: {
        status: distributionReport?.status ?? 'missing',
        publicAlphaReady: distributionReport?.publicAlphaReady === true,
        uploadedArtifactUrlsPresent: distributionReport?.releaseIndex?.uploadedArtifactUrlsPresent === true,
      },
      distributionApproval: {
        status: distributionApprovalReport?.status ?? 'missing',
        approvalAreaCount: distributionApprovalReport?.approvalResults?.length ?? 0,
        clearedDistributionGates: clearedDistributionGates.length,
        remainingDistributionGates: remainingDistributionGates.length,
        publicAlphaReady: distributionApprovalReport?.publicAlphaReady === true,
      },
      launcherFlow: {
        status: launcherFlowReport?.status ?? 'missing',
        publicAlphaReady: launcherFlowReport?.publicAlphaReady === true,
      },
      launcherExecution: {
        status: launcherExecutionReport?.status ?? 'missing',
        flowCount: launcherExecutionReport?.flowResults?.length ?? 0,
        clearedLauncherGates: clearedLauncherGates.length,
        remainingLauncherGates: remainingLauncherGates.length,
        publicAlphaReady: launcherExecutionReport?.publicAlphaReady === true,
      },
      localLauncherRehearsal: {
        status: localLauncherRehearsalReport?.status ?? 'missing',
        flowCount: localLauncherRehearsalReport?.flowResults?.length ?? 0,
        rehearsalOnly: localLauncherRehearsalReport?.rehearsalOnly === true,
        clearsLauncherGates: localLauncherRehearsalReport?.clearsLauncherGates === true,
        publicAlphaReady: localLauncherRehearsalReport?.publicAlphaReady === true,
      },
      legal: {
        status: legalReport?.status ?? 'missing',
        publicReleaseAllowed: legalReport?.publicReleaseAllowed === true,
      },
      finalReview: {
        status: finalReviewReport?.status ?? 'missing',
        reviewAreaCount: finalReviewReport?.reviewResults?.length ?? 0,
        clearedFinalReviewGates: clearedFinalReviewGates.length,
        remainingFinalReviewGates: remainingFinalReviewGates.length,
        publicReleaseReady: finalReviewReport?.publicReleaseReady === true,
      },
    },
  }
}

function validate({ moduleRoot, workspaceRoot, releaseRoot, reportPath }) {
  const errors = []
  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const production = readJson(path.join(dataRoot, 'progression', 'production_phase_matrix.json'))
  const runtimeExecution = readJson(path.join(dataRoot, 'systems', 'runtime_execution_acceptance.json'))
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const launcherFlow = readJson(path.join(dataRoot, 'systems', 'launcher_flow_acceptance.json'))
  const launcherExecution = readJson(path.join(dataRoot, 'systems', 'launcher_execution_acceptance.json'))
  const finalReview = readJson(path.join(dataRoot, 'systems', 'final_release_review_acceptance.json'))
  const distributionApproval = readJson(path.join(dataRoot, 'systems', 'distribution_approval_acceptance.json'))
  const distribution = readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json'))
  const launchRoadmap = readJson(path.join(dataRoot, 'progression', 'launch_roadmap.json'))
  const crossPlatformParity = readJson(path.join(dataRoot, 'systems', 'cross_platform_parity.json'))
  const legalAudit = readJson(path.join(dataRoot, 'systems', 'legal_content_audit.json'))
  const contentPolicy = readJson(path.join(dataRoot, 'config', 'content_policy.json'))
  const assetManifest = readJson(path.join(moduleRoot, 'src', 'main', 'resources', ASSET_MANIFEST_PATH))
  const releasePublicationContract = readJson(path.join(moduleRoot, 'src', 'main', 'resources', RELEASE_PUBLICATION_CONTRACT_PATH))
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const report = readJson(reportPath)
  const generatorScript = path.join(moduleRoot, 'scripts', 'generate-openlands-release-readiness-report.mjs')
  let generatedReadinessReport = null
  assert(errors, fileExists(generatorScript), `missing Openlands release readiness generator ${generatorScript}`)
  if (fileExists(generatorScript)) {
    const generated = spawnSync(process.execPath, [
      generatorScript,
      '--module-root',
      moduleRoot,
      '--workspace-root',
      workspaceRoot,
      '--release-root',
      releaseRoot,
      '--out',
      reportPath,
      '--dry-run',
      '--json',
    ], {
      cwd: path.resolve(moduleRoot, '..', '..'),
      encoding: 'utf8',
      windowsHide: true,
    })
    assert(errors, generated.status === 0, `Openlands release readiness generator dry-run failed: ${generated.stderr || generated.stdout}`)
    if (generated.status === 0) {
      try {
        generatedReadinessReport = JSON.parse(generated.stdout)
      } catch (error) {
        errors.push(`Openlands release readiness generator dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseIndex = fileExists(releaseIndexPath) ? readJson(releaseIndexPath) : null
  const releaseModule = (releaseIndex?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const publicationPaths = {
    template: path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.template.json'),
    verified: path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.verified.json'),
    approved: path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.json'),
  }
  const reportedPublicationManifestPath = resolveReportedPath(report.releasePublication?.manifestPath, releaseRoot)
  const reportedPublicationManifest = reportedPublicationManifestPath && fileExists(reportedPublicationManifestPath)
    ? readJson(reportedPublicationManifestPath)
    : null
  const reportedPublicationRehearsalPath = resolveReportedPath(report.releasePublicationRehearsal?.reportPath, releaseRoot)
  const reportedPublicationRehearsal = reportedPublicationRehearsalPath && fileExists(reportedPublicationRehearsalPath)
    ? readJson(reportedPublicationRehearsalPath)
    : null
  const reportedEditionManifestIndexPreviewPath = resolveReportedPath(report.editionManifestIndexPreview?.reportPath, releaseRoot)
  const reportedEditionManifestIndexPreview = reportedEditionManifestIndexPreviewPath && fileExists(reportedEditionManifestIndexPreviewPath)
    ? readJson(reportedEditionManifestIndexPreviewPath)
    : null
  const runtimeGateIds = sortedUnique((runtimeExecution.runtimeGates ?? []).map((gate) => gate.id))
  const launcherGateIds = sortedUnique((launcherExecution.launcherGates ?? []).map((gate) => gate.id))
  const finalReviewGateIds = sortedUnique((finalReview.finalReviewGates ?? []).map((gate) => gate.id))
  const distributionGateIds = sortedUnique((distributionApproval.distributionGates ?? []).map((gate) => gate.id))
  const editionEvidence = new Map(EDITIONS.map((edition) => [edition.id, buildEditionEvidenceResult({
    workspaceRoot,
    edition,
    runtimeGateIds,
    launcherGateIds,
    finalReviewGateIds,
    distributionGateIds,
  })]))
  validateReferencedModuleReports(errors, {
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    publicationManifestPath: reportedPublicationManifestPath,
    publicationRehearsalPath: reportedPublicationRehearsalPath,
    editionManifestIndexPreviewPath: reportedEditionManifestIndexPreviewPath,
  })
  validateEditionAggregate(errors, {
    moduleRoot,
    workspaceRoot,
    reportPath,
  })
  validateReferencedEditionReports(errors, {
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    editionEvidence,
  })
  validateHarnessDriverManifests(errors, {
    moduleRoot,
    workspaceRoot,
  })
  validateHarnessDryRuns(errors, {
    moduleRoot,
    workspaceRoot,
    releaseRoot,
  })
  const expectedArtifactResults = buildArtifactEvidenceResults({ releaseRoot, releaseModule, releasePublicationContract })
  validatePreflightReportFreshness(errors, {
    editionEvidence,
    moduleRoot,
    workspaceRoot,
    releaseIndex,
    releaseRoot,
    releasePublicationContract,
    releaseModule,
    launcherFlowContract: launcherFlow,
    runtimePlan,
    distributionContract: distribution,
    distributionApprovalContract: distributionApproval,
    launchRoadmap,
    crossPlatformParity,
    conformance,
    legalAuditContract: legalAudit,
    contentPolicy,
    assetManifest,
  })
  const expectedEditionResults = EDITIONS.map((edition) => editionEvidence.get(edition.id)?.result)
  const expectedReadiness = buildExpectedReadiness({
    artifactResults: expectedArtifactResults,
    editionResults: expectedEditionResults,
    runtimeExecution,
    launcherExecution,
    releasePublicationContract,
    releasePublicationManifest: reportedPublicationManifest,
    releasePublicationRehearsal: reportedPublicationRehearsal,
    editionManifestIndexPreview: reportedEditionManifestIndexPreview,
    runtimeGateIds,
    launcherGateIds,
    finalReviewGateIds,
    distributionGateIds,
  })

  assert(errors, report.schema === 'echo.openlands.release_readiness_report.v1', 'release readiness report schema mismatch')
  assert(errors, report.moduleId === MODULE_ID, 'release readiness module id mismatch')
  assert(errors, report.moduleVersion === VERSION, 'release readiness version mismatch')
  if (generatedReadinessReport) {
    assert(errors, sameJson(
      stableReleaseReadinessReport(report),
      stableReleaseReadinessReport(generatedReadinessReport),
    ), 'release readiness report stale against generator dry-run')
  }
  assert(errors, report.status === 'blocked' || report.status === 'ready', 'release readiness status must be blocked or ready')
  assert(errors, report.productionMatrix?.phases === production.counts?.phases, 'release readiness phase count mismatch')
  assert(errors, report.productionMatrix?.checkpoints === production.counts?.checkpoints, 'release readiness checkpoint count mismatch')
  assert(errors, report.productionMatrix?.runtimeGates === production.counts?.runtimeGates, 'release readiness runtime gate count mismatch')
  assert(errors, report.productionMatrix?.missingEvidence === production.counts?.missingEvidence, 'release readiness missing evidence count mismatch')
  assert(errors, report.runtimeExecution?.gateCount === (runtimeExecution.runtimeGates ?? []).length, 'release readiness runtime gate contract count mismatch')
  assert(errors, report.runtimeExecution?.scenarioCount === (runtimeExecution.scenarios ?? []).length, 'release readiness runtime scenario count mismatch')
  assert(errors, report.runtimeExecution?.suiteCount === (runtimeExecution.executionSuites ?? []).length, 'release readiness runtime suite count mismatch')
  assert(errors, report.localRuntimeRehearsal?.reportContract === 'echo.openlands.edition.local_runtime_rehearsal_report.v1', 'release readiness local runtime rehearsal contract mismatch')
  assert(errors, report.localRuntimeRehearsal?.requiredScenarioCount === (runtimeExecution.scenarios ?? []).length, 'release readiness local runtime rehearsal scenario count mismatch')
  assert(errors, report.localRuntimeRehearsal?.requiredStatus === 'preflight_passed', 'release readiness local runtime rehearsal required status mismatch')
  assert(errors, report.localRuntimeRehearsal?.rehearsalOnlyDoesNotClearGates === true, 'release readiness local runtime rehearsal must not clear runtime gates')
  assert(errors, report.localRuntimeRehearsal?.reportsPresent === EXPECTED_EDITIONS.length, 'release readiness local runtime rehearsal reports present mismatch')
  assert(errors, report.localRuntimeRehearsal?.reportsPassed === EXPECTED_EDITIONS.length, 'release readiness local runtime rehearsal reports passed mismatch')
  assert(errors, report.localRuntimeRehearsal?.clearsRealRuntimeGates === false, 'release readiness local runtime rehearsal must leave real runtime gates uncleared')
  assert(errors, report.launcherExecution?.gateCount === (launcherExecution.launcherGates ?? []).length, 'release readiness launcher gate contract count mismatch')
  assert(errors, report.launcherExecution?.flowCount === (launcherExecution.executionFlows ?? []).length, 'release readiness launcher flow count mismatch')
  assert(errors, report.localLauncherRehearsal?.reportContract === 'echo.openlands.edition.local_launcher_rehearsal_report.v1', 'release readiness local launcher rehearsal contract mismatch')
  assert(errors, report.localLauncherRehearsal?.requiredFlowCount === (launcherExecution.executionFlows ?? []).length, 'release readiness local launcher rehearsal flow count mismatch')
  assert(errors, report.localLauncherRehearsal?.requiredStatus === 'preflight_passed', 'release readiness local launcher rehearsal required status mismatch')
  assert(errors, report.localLauncherRehearsal?.rehearsalOnlyDoesNotClearGates === true, 'release readiness local launcher rehearsal must not clear launcher gates')
  assert(errors, report.localLauncherRehearsal?.reportsPresent === EXPECTED_EDITIONS.length, 'release readiness local launcher rehearsal reports present mismatch')
  assert(errors, report.localLauncherRehearsal?.reportsPassed === EXPECTED_EDITIONS.length, 'release readiness local launcher rehearsal reports passed mismatch')
  assert(errors, report.localLauncherRehearsal?.clearsRealLauncherGates === false, 'release readiness local launcher rehearsal must leave real launcher gates uncleared')
  assert(errors, report.finalReleaseReview?.gateCount === (finalReview.finalReviewGates ?? []).length, 'release readiness final review gate count mismatch')
  assert(errors, report.finalReleaseReview?.reviewAreaCount === (finalReview.reviewAreas ?? []).length, 'release readiness final review area count mismatch')
  assert(errors, report.distributionApproval?.gateCount === (distributionApproval.distributionGates ?? []).length, 'release readiness distribution approval gate count mismatch')
  assert(errors, report.distributionApproval?.approvalAreaCount === (distributionApproval.approvalAreas ?? []).length, 'release readiness distribution approval area count mismatch')
  assert(errors, report.releasePublication?.contract === RELEASE_PUBLICATION_CONTRACT_PATH, 'release readiness publication contract path mismatch')
  assert(errors, ['template', 'verified', 'approved'].includes(report.releasePublication?.manifestSource), 'release readiness publication manifest source mismatch')
  assert(errors, releasePublicationContract.reportContract?.allowedStatus?.includes(report.releasePublication?.manifestStatus), 'release readiness publication status is not allowed')
  assert(errors, sameResolvedPath(report.releasePublication?.templatePath, publicationPaths.template), 'release readiness publication template path mismatch')
  assert(errors, sameResolvedPath(report.releasePublication?.verifiedPath, publicationPaths.verified), 'release readiness publication verified path mismatch')
  assert(errors, sameResolvedPath(report.releasePublication?.approvedPath, publicationPaths.approved), 'release readiness publication approved path mismatch')
  if (report.releasePublication?.manifestSource === 'template') assert(errors, sameResolvedPath(report.releasePublication?.manifestPath, publicationPaths.template), 'release readiness template manifest path mismatch')
  if (report.releasePublication?.manifestSource === 'verified') assert(errors, sameResolvedPath(report.releasePublication?.manifestPath, publicationPaths.verified), 'release readiness verified manifest path mismatch')
  if (report.releasePublication?.manifestSource === 'approved') assert(errors, sameResolvedPath(report.releasePublication?.manifestPath, publicationPaths.approved), 'release readiness approved manifest path mismatch')
  assert(errors, reportedPublicationManifest !== null, 'release readiness publication manifest path must exist')
  assert(errors, report.releasePublication?.manifestPresent === true, 'release readiness publication manifest must be present')
  assert(errors, report.releasePublication?.artifactCount === (releasePublicationContract.artifactTargets ?? []).length, 'release readiness publication artifact count mismatch')
  assert(errors, sameSet(report.releasePublication?.expectedArtifactFiles, (releasePublicationContract.artifactTargets ?? []).map((target) => target.file)), 'release readiness publication expected artifact files mismatch')
  assert(errors, sameSet(report.releasePublication?.actualArtifactFiles, (releasePublicationContract.artifactTargets ?? []).map((target) => target.file)), 'release readiness publication actual artifact files mismatch')
  if (reportedPublicationManifest) {
    const publications = reportedPublicationManifest.artifactPublications ?? []
    const expectedFiles = (releasePublicationContract.artifactTargets ?? []).map((target) => target.file)
    const actualFiles = publications.map((publication) => publication.file)
    const missingDownloadUrlCount = publications.filter((publication) => typeof publication.downloadUrl !== 'string' || publication.downloadUrl.length === 0).length
    const downloadVerifiedCount = publications.filter((publication) => {
      const verification = publication.downloadVerification ?? {}
      return publication.downloadUrl
        && verification.downloadAttempted === true
        && verification.sha256Matches === true
        && verification.sizeMatches === true
        && verification.downloadedSha256 === publication.sha256
        && verification.downloadedSize === publication.size
    }).length
    const releaseIndexPatchAllowedCount = publications.filter((publication) => publication.releaseIndexPatch?.patchAllowed === true).length
    const approved = reportedPublicationManifest.status === 'approved'
      && publications.length === expectedFiles.length
      && downloadVerifiedCount === publications.length
      && publications.every((publication) => publication.urlStatus === 'approved' && publication.releaseIndexPatch?.patchApplied === true)

    assert(errors, reportedPublicationManifest.schema === releasePublicationContract.reportContract?.schema, 'release readiness referenced publication manifest schema mismatch')
    assert(errors, reportedPublicationManifest.moduleId === MODULE_ID, 'release readiness referenced publication manifest module id mismatch')
    assert(errors, reportedPublicationManifest.moduleVersion === VERSION, 'release readiness referenced publication manifest version mismatch')
    assert(errors, reportedPublicationManifest.status === report.releasePublication?.manifestStatus, 'release readiness publication manifest status does not match referenced manifest')
    assert(errors, publications.length === report.releasePublication?.artifactCount, 'release readiness publication artifact count does not match referenced manifest')
    assert(errors, sameSet(report.releasePublication?.actualArtifactFiles, actualFiles), 'release readiness publication actual files do not match referenced manifest')
    assert(errors, sameSet(expectedFiles, actualFiles), 'release readiness referenced publication manifest artifact coverage mismatch')
    assert(errors, report.releasePublication?.missingDownloadUrlCount === missingDownloadUrlCount, 'release readiness publication missing download URL count does not match referenced manifest')
    assert(errors, report.releasePublication?.downloadVerifiedCount === downloadVerifiedCount, 'release readiness publication verified download count does not match referenced manifest')
    assert(errors, report.releasePublication?.releaseIndexPatchAllowedCount === releaseIndexPatchAllowedCount, 'release readiness publication patch allowed count does not match referenced manifest')
    assert(errors, report.releasePublication?.approved === approved, 'release readiness publication approved flag does not match referenced manifest')
    assert(errors, sameSet(report.releasePublication?.blockedBy, reportedPublicationManifest.blockedBy), 'release readiness publication blockers do not match referenced manifest')
    for (const publication of publications) {
      if (typeof publication.downloadUrl === 'string' && publication.downloadUrl.length > 0) {
        assert(errors, isPublicHttpsUrl(publication.downloadUrl), `release readiness publication URL must be public HTTPS for ${publication.file}`)
      }
    }
  }
  assert(errors, report.releasePublicationRehearsal?.reportContract === 'echo.openlands.release_publication_rehearsal_report.v1', 'release readiness publication rehearsal contract mismatch')
  assert(errors, reportedPublicationRehearsal !== null, 'release readiness publication rehearsal report path must exist')
  assert(errors, report.releasePublicationRehearsal?.reportPresent === true, 'release readiness publication rehearsal report must be present')
  assert(errors, report.releasePublicationRehearsal?.status === 'preflight_passed', 'release readiness publication rehearsal must pass')
  assert(errors, report.releasePublicationRehearsal?.artifactCount === (releasePublicationContract.artifactTargets ?? []).length, 'release readiness publication rehearsal artifact count mismatch')
  assert(errors, report.releasePublicationRehearsal?.localDownloadVerifiedCount === (releasePublicationContract.artifactTargets ?? []).length, 'release readiness publication rehearsal local download count mismatch')
  assert(errors, report.releasePublicationRehearsal?.patchPreviewCount === (releasePublicationContract.artifactTargets ?? []).length, 'release readiness publication rehearsal patch preview count mismatch')
  assert(errors, report.releasePublicationRehearsal?.rehearsalOnlyDoesNotClearGates === true, 'release readiness publication rehearsal must be rehearsal-only')
  assert(errors, report.releasePublicationRehearsal?.clearsDistributionGates === false, 'release readiness publication rehearsal must not clear distribution gates')
  assert(errors, report.releasePublicationRehearsal?.clearsReleasePublicationGates === false, 'release readiness publication rehearsal must not clear release publication gates')
  assert(errors, report.releasePublicationRehearsal?.publicAlphaReady === false, 'release readiness publication rehearsal must not mark public alpha ready')
  assert(errors, report.releasePublicationRehearsal?.passed === true, 'release readiness publication rehearsal passed flag mismatch')
  if (reportedPublicationRehearsal) {
    const artifactResults = reportedPublicationRehearsal.artifactResults ?? []
    const localDownloadVerifiedCount = artifactResults.filter((artifact) => artifact.localDownloadBack?.sha256Matches === true && artifact.localDownloadBack?.sizeMatches === true).length
    const patchPreviewCount = artifactResults.filter((artifact) => artifact.releaseIndexPatchPreview?.patchApplied === false).length
    const passed = reportedPublicationRehearsal.status === 'preflight_passed'
      && reportedPublicationRehearsal.publicAlphaReady === false
      && reportedPublicationRehearsal.rehearsalOnly === true
      && reportedPublicationRehearsal.clearsDistributionGates === false
      && reportedPublicationRehearsal.clearsReleasePublicationGates === false
      && (reportedPublicationRehearsal.summary?.artifactCount ?? 0) === (releasePublicationContract.artifactTargets ?? []).length
      && localDownloadVerifiedCount === (releasePublicationContract.artifactTargets ?? []).length
      && patchPreviewCount === (releasePublicationContract.artifactTargets ?? []).length
    assert(errors, reportedPublicationRehearsal.schema === 'echo.openlands.release_publication_rehearsal_report.v1', 'release readiness referenced publication rehearsal schema mismatch')
    assert(errors, reportedPublicationRehearsal.status === report.releasePublicationRehearsal?.status, 'release readiness publication rehearsal status does not match referenced report')
    assert(errors, (reportedPublicationRehearsal.summary?.artifactCount ?? 0) === report.releasePublicationRehearsal?.artifactCount, 'release readiness publication rehearsal artifact count does not match referenced report')
    assert(errors, localDownloadVerifiedCount === report.releasePublicationRehearsal?.localDownloadVerifiedCount, 'release readiness publication rehearsal local download count does not match referenced report')
    assert(errors, patchPreviewCount === report.releasePublicationRehearsal?.patchPreviewCount, 'release readiness publication rehearsal patch preview count does not match referenced report')
    assert(errors, passed === report.releasePublicationRehearsal?.passed, 'release readiness publication rehearsal passed flag does not match referenced report')
    assert(errors, sameSet(report.releasePublicationRehearsal?.blockedBy, reportedPublicationRehearsal.blockedBy), 'release readiness publication rehearsal blockers do not match referenced report')
  }
  assert(errors, report.editionManifestIndexPreview?.reportContract === 'echo.openlands.edition_manifest_index_preview.v1', 'release readiness edition manifest index preview contract mismatch')
  assert(errors, reportedEditionManifestIndexPreview !== null, 'release readiness edition manifest index preview path must exist')
  assert(errors, report.editionManifestIndexPreview?.reportPresent === true, 'release readiness edition manifest index preview report must be present')
  assert(errors, ['preflight_passed', 'preflight_blocked'].includes(report.editionManifestIndexPreview?.status), 'release readiness edition manifest index preview status mismatch')
  assert(errors, report.editionManifestIndexPreview?.editionCount === EXPECTED_EDITIONS.length, 'release readiness edition manifest index preview edition count mismatch')
  assert(errors, report.editionManifestIndexPreview?.savedArtifactCount >= 3, 'release readiness edition manifest index preview saved artifact count mismatch')
  assert(errors, typeof report.editionManifestIndexPreview?.moduleRequirementResolutionPassed === 'boolean', 'release readiness edition manifest index preview module requirement resolution flag mismatch')
  assert(errors, report.editionManifestIndexPreview?.launcherChannelListingEditionCount === EXPECTED_EDITIONS.length, 'release readiness edition manifest index preview launcher channel count mismatch')
  assert(errors, report.editionManifestIndexPreview?.previewOnlyDoesNotClearGates === true, 'release readiness edition manifest index preview must be preview-only')
  assert(errors, report.editionManifestIndexPreview?.clearsLauncherGates === false, 'release readiness edition manifest index preview must not clear launcher gates')
  assert(errors, report.editionManifestIndexPreview?.clearsDistributionGates === false, 'release readiness edition manifest index preview must not clear distribution gates')
  assert(errors, report.editionManifestIndexPreview?.publicAlphaReady === false, 'release readiness edition manifest index preview must not mark public alpha ready')
  assert(errors, typeof report.editionManifestIndexPreview?.passed === 'boolean', 'release readiness edition manifest index preview passed flag mismatch')
  if (reportedEditionManifestIndexPreview) {
    const previewSummary = reportedEditionManifestIndexPreview.summary ?? {}
    const passed = reportedEditionManifestIndexPreview.status === 'preflight_passed'
      && reportedEditionManifestIndexPreview.publicAlphaReady === false
      && reportedEditionManifestIndexPreview.previewOnly === true
      && reportedEditionManifestIndexPreview.clearsLauncherGates === false
      && reportedEditionManifestIndexPreview.clearsDistributionGates === false
      && previewSummary.editionCount === EXPECTED_EDITIONS.length
      && previewSummary.localArtifactCount === EXPECTED_EDITIONS.length
      && previewSummary.artifactSha256MatchCount === EXPECTED_EDITIONS.length
      && previewSummary.artifactSizeMatchCount === EXPECTED_EDITIONS.length
      && previewSummary.requiredDescriptorMatchCount === EXPECTED_EDITIONS.length
      && reportedEditionManifestIndexPreview.moduleRequirementResolution?.passed === true
      && reportedEditionManifestIndexPreview.launcherChannelListing?.editionCount === EXPECTED_EDITIONS.length
    assert(errors, reportedEditionManifestIndexPreview.schema === 'echo.openlands.edition_manifest_index_preview.v1', 'release readiness referenced edition manifest index preview schema mismatch')
    assert(errors, reportedEditionManifestIndexPreview.status === report.editionManifestIndexPreview?.status, 'release readiness edition manifest index preview status does not match referenced report')
    assert(errors, previewSummary.editionCount === report.editionManifestIndexPreview?.editionCount, 'release readiness edition manifest index preview edition count does not match referenced report')
    assert(errors, previewSummary.savedArtifactCount === report.editionManifestIndexPreview?.savedArtifactCount, 'release readiness edition manifest index preview saved artifact count does not match referenced report')
    assert(errors, reportedEditionManifestIndexPreview.moduleRequirementResolution?.passed === report.editionManifestIndexPreview?.moduleRequirementResolutionPassed, 'release readiness edition manifest index preview module resolution does not match referenced report')
    assert(errors, reportedEditionManifestIndexPreview.launcherChannelListing?.editionCount === report.editionManifestIndexPreview?.launcherChannelListingEditionCount, 'release readiness edition manifest index preview launcher listing does not match referenced report')
    assert(errors, passed === report.editionManifestIndexPreview?.passed, 'release readiness edition manifest index preview passed flag does not match referenced report')
    assert(errors, sameSet(report.editionManifestIndexPreview?.blockedBy, reportedEditionManifestIndexPreview.blockedBy), 'release readiness edition manifest index preview blockers do not match referenced report')
  }
  assert(errors, report.currentRegistryCounts?.blocks === conformance.blockRegistry?.length, 'release readiness block count mismatch')
  assert(errors, report.currentRegistryCounts?.items === conformance.itemRegistry?.length, 'release readiness item count mismatch')
  assert(errors, report.currentRegistryCounts?.recipes === conformance.recipeRegistry?.length, 'release readiness recipe count mismatch')
  assert(errors, report.currentRegistryCounts?.biomes === conformance.biomeRegistry?.length, 'release readiness biome count mismatch')
  assert(errors, report.currentRegistryCounts?.creatures === conformance.creatureRegistry?.length, 'release readiness creature count mismatch')
  assert(errors, sameSet((report.editionResults ?? []).map((edition) => edition.id), EXPECTED_EDITIONS), 'release readiness edition list mismatch')
  assert(errors, (report.editionResults ?? []).length === EXPECTED_EDITIONS.length, 'release readiness edition result count mismatch')
  assert(errors, (report.artifactResults ?? []).length === expectedArtifactResults.length, 'release readiness artifact target count mismatch')
  assert(errors, sameSet((report.artifactResults ?? []).map((artifact) => artifact.file), expectedArtifactResults.map((artifact) => artifact.file)), 'release readiness artifact target files mismatch')
  for (const expectedArtifact of expectedArtifactResults) {
    const artifact = (report.artifactResults ?? []).find((entry) => entry.file === expectedArtifact.file)
    assertObjectFieldsMatch(errors, artifact, expectedArtifact, `release readiness artifact ${expectedArtifact.file}`)
  }

  const readinessChecks = report.readinessChecks ?? {}
  assertObjectFieldsMatch(errors, readinessChecks, expectedReadiness.readinessChecks, 'release readiness readinessChecks')
  const expectedPublicAlphaReady = expectedReadiness.publicAlphaReady
  assert(errors, report.publicAlphaReady === expectedPublicAlphaReady, 'release readiness publicAlphaReady must match readiness checks')
  assert(errors, sameSet(report.blockers, expectedReadiness.blockers), 'release readiness blockers do not match recomputed evidence')
  validatePhaseReadiness(errors, {
    report,
    reportPath,
    production,
    readinessChecks,
  })
  assert(errors, readinessChecks.releasePublicationManifestPresent === report.releasePublication?.manifestPresent, 'release readiness publication manifest check mismatch')
  assert(errors, readinessChecks.releasePublicationArtifactCoverageComplete === true, 'release readiness publication artifact coverage must be complete')
  assert(errors, readinessChecks.releasePublicationRehearsalPresent === report.releasePublicationRehearsal?.reportPresent, 'release readiness publication rehearsal present check mismatch')
  assert(errors, readinessChecks.releasePublicationRehearsalPassed === report.releasePublicationRehearsal?.passed, 'release readiness publication rehearsal passed check mismatch')
  assert(errors, readinessChecks.editionManifestIndexPreviewPresent === report.editionManifestIndexPreview?.reportPresent, 'release readiness edition manifest index preview present check mismatch')
  assert(errors, readinessChecks.editionManifestIndexPreviewPassed === report.editionManifestIndexPreview?.passed, 'release readiness edition manifest index preview passed check mismatch')
  assert(errors, readinessChecks.releasePublicationDownloadsVerified === (report.releasePublication?.downloadVerifiedCount === report.releasePublication?.artifactCount), 'release readiness publication download verification check mismatch')
  assert(errors, readinessChecks.releasePublicationApproved === report.releasePublication?.approved, 'release readiness publication approved check mismatch')
  assert(errors, readinessChecks.allLocalRuntimeRehearsalsPresent === true, 'release readiness local runtime rehearsal reports must be present')
  assert(errors, readinessChecks.allLocalRuntimeRehearsalsPassed === true, 'release readiness local runtime rehearsals must pass')
  assert(errors, readinessChecks.allLocalLauncherRehearsalsPresent === true, 'release readiness local launcher rehearsal reports must be present')
  assert(errors, readinessChecks.allLocalLauncherRehearsalsPassed === true, 'release readiness local launcher rehearsals must pass')
  if (!expectedPublicAlphaReady) {
    assert(errors, report.status === 'blocked', 'non-ready release report must be blocked')
    assert(errors, Array.isArray(report.blockers) && report.blockers.length > 0, 'blocked release report must list blockers')
  }
  if (expectedPublicAlphaReady) {
    assert(errors, report.status === 'ready', 'ready release report must have ready status')
    assert(errors, Array.isArray(report.blockers) && report.blockers.length === 0, 'ready release report must have no blockers')
  }
  for (const expectedEdition of EDITIONS) {
    const edition = (report.editionResults ?? []).find((entry) => entry.id === expectedEdition.id)
    const evidence = editionEvidence.get(expectedEdition.id)
    assert(errors, edition !== undefined, `${expectedEdition.id} edition result missing`)
    if (!edition || !evidence) continue

    assert(errors, edition.repo === expectedEdition.repo, `${expectedEdition.id} edition repo mismatch`)
    assert(errors, edition.runtimeTarget === expectedEdition.runtimeTarget, `${expectedEdition.id} edition runtime target mismatch`)
    for (const [key, schema] of Object.entries(EDITION_REPORT_SCHEMAS)) {
      assertReportIdentity(errors, evidence.reports[key]?.report ?? null, schema, expectedEdition, `${expectedEdition.id} ${key} report`)
    }

    assertObjectFieldsMatch(errors, edition.reportsPresent, evidence.result.reportsPresent, `${expectedEdition.id} reportsPresent`)
    assertObjectFieldsMatch(errors, edition.runtimeExecution, evidence.result.runtimeExecution, `${expectedEdition.id} runtime execution`)
    assertObjectFieldsMatch(errors, edition.localRuntimeRehearsal, evidence.result.localRuntimeRehearsal, `${expectedEdition.id} local runtime rehearsal`)
    assertObjectFieldsMatch(errors, edition.distribution, evidence.result.distribution, `${expectedEdition.id} distribution`)
    assertObjectFieldsMatch(errors, edition.distributionApproval, evidence.result.distributionApproval, `${expectedEdition.id} distribution approval`)
    assertObjectFieldsMatch(errors, edition.launcherFlow, evidence.result.launcherFlow, `${expectedEdition.id} launcher flow`)
    assertObjectFieldsMatch(errors, edition.launcherExecution, evidence.result.launcherExecution, `${expectedEdition.id} launcher execution`)
    assertObjectFieldsMatch(errors, edition.localLauncherRehearsal, evidence.result.localLauncherRehearsal, `${expectedEdition.id} local launcher rehearsal`)
    assertObjectFieldsMatch(errors, edition.legal, evidence.result.legal, `${expectedEdition.id} legal`)
    assertObjectFieldsMatch(errors, edition.finalReview, evidence.result.finalReview, `${expectedEdition.id} final review`)
    assert(errors, edition.runtimeExecution?.scenarioCount === (runtimeExecution.scenarios ?? []).length, `${edition.id} runtime scenario count mismatch`)
    assert(errors, edition.runtimeExecution?.clearedRuntimeGates + edition.runtimeExecution?.remainingRuntimeGates === runtimeGateIds.length, `${edition.id} runtime gate coverage mismatch`)
    assert(errors, edition.reportsPresent?.localRuntimeRehearsal === true, `${edition.id} local runtime rehearsal report must be present`)
    assert(errors, edition.localRuntimeRehearsal?.status === 'preflight_passed', `${edition.id} local runtime rehearsal must pass`)
    assert(errors, edition.localRuntimeRehearsal?.scenarioCount === (runtimeExecution.scenarios ?? []).length, `${edition.id} local runtime rehearsal scenario count mismatch`)
    assert(errors, edition.localRuntimeRehearsal?.rehearsalOnly === true, `${edition.id} local runtime rehearsal must be rehearsal-only`)
    assert(errors, edition.localRuntimeRehearsal?.clearsRuntimeGates === false, `${edition.id} local runtime rehearsal must not clear runtime gates`)
    assert(errors, edition.localRuntimeRehearsal?.publicAlphaReady === false, `${edition.id} local runtime rehearsal must not mark public alpha ready`)
    assert(errors, edition.launcherExecution?.flowCount === (launcherExecution.executionFlows ?? []).length, `${edition.id} launcher execution flow count mismatch`)
    assert(errors, edition.launcherExecution?.clearedLauncherGates + edition.launcherExecution?.remainingLauncherGates === launcherGateIds.length, `${edition.id} launcher gate coverage mismatch`)
    assert(errors, edition.reportsPresent?.localLauncherRehearsal === true, `${edition.id} local launcher rehearsal report must be present`)
    assert(errors, edition.localLauncherRehearsal?.status === 'preflight_passed', `${edition.id} local launcher rehearsal must pass`)
    assert(errors, edition.localLauncherRehearsal?.flowCount === (launcherExecution.executionFlows ?? []).length, `${edition.id} local launcher rehearsal flow count mismatch`)
    assert(errors, edition.localLauncherRehearsal?.rehearsalOnly === true, `${edition.id} local launcher rehearsal must be rehearsal-only`)
    assert(errors, edition.localLauncherRehearsal?.clearsLauncherGates === false, `${edition.id} local launcher rehearsal must not clear launcher gates`)
    assert(errors, edition.localLauncherRehearsal?.publicAlphaReady === false, `${edition.id} local launcher rehearsal must not mark public alpha ready`)
    assert(errors, edition.finalReview?.reviewAreaCount === (finalReview.reviewAreas ?? []).length, `${edition.id} final review area count mismatch`)
    assert(errors, edition.finalReview?.clearedFinalReviewGates + edition.finalReview?.remainingFinalReviewGates === finalReviewGateIds.length, `${edition.id} final review gate coverage mismatch`)
    assert(errors, edition.distributionApproval?.approvalAreaCount === (distributionApproval.approvalAreas ?? []).length, `${edition.id} distribution approval area count mismatch`)
    assert(errors, edition.distributionApproval?.clearedDistributionGates + edition.distributionApproval?.remainingDistributionGates === distributionGateIds.length, `${edition.id} distribution approval gate coverage mismatch`)
    if (edition.runtimeExecution?.status === 'blocked') {
      assert(errors, edition.runtimeExecution?.publicAlphaReady === false, `${edition.id} blocked runtime execution must not be public alpha ready`)
    }
    if (edition.launcherExecution?.status === 'blocked') {
      assert(errors, edition.launcherExecution?.publicAlphaReady === false, `${edition.id} blocked launcher execution must not be public alpha ready`)
    }
    if (edition.finalReview?.status === 'blocked') {
      assert(errors, edition.finalReview?.publicReleaseReady === false, `${edition.id} blocked final review must not be public release ready`)
    }
    if (edition.distributionApproval?.status === 'blocked') {
      assert(errors, edition.distributionApproval?.publicAlphaReady === false, `${edition.id} blocked distribution approval must not be public alpha ready`)
    }
  }
  for (const artifact of report.artifactResults ?? []) {
    assert(errors, artifact.exists === true, `release readiness artifact missing ${artifact.file}`)
    assert(errors, artifact.releaseIndexEntryPresent === true, `release readiness release index missing artifact ${artifact.file}`)
    assert(errors, artifact.sha256Present === true, `release readiness artifact missing sha256 ${artifact.file}`)
    assert(errors, artifact.sha256MatchesFile === true, `release readiness artifact sha mismatch ${artifact.file}`)
    assert(errors, artifact.sizeMatchesFile === true, `release readiness artifact size mismatch ${artifact.file}`)
  }
  if (!readinessChecks.allArtifactUrlsPresent) {
    assert(errors, report.blockers?.includes('release_index_download_urls_missing'), 'release readiness missing download URL blocker')
  }
  if (!readinessChecks.releasePublicationManifestPresent) {
    assert(errors, report.blockers?.includes('release_publication_manifest_missing'), 'release readiness missing publication manifest blocker')
  }
  if (!readinessChecks.releasePublicationArtifactCoverageComplete) {
    assert(errors, report.blockers?.includes('release_publication_artifact_coverage_mismatch'), 'release readiness missing publication coverage blocker')
  }
  if (!readinessChecks.releasePublicationRehearsalPresent) {
    assert(errors, report.blockers?.includes('release_publication_rehearsal_report_missing'), 'release readiness missing publication rehearsal report blocker')
  }
  if (!readinessChecks.releasePublicationRehearsalPassed) {
    assert(errors, report.blockers?.includes('release_publication_rehearsal_failed'), 'release readiness missing publication rehearsal failed blocker')
  }
  if (!readinessChecks.editionManifestIndexPreviewPresent) {
    assert(errors, report.blockers?.includes('edition_manifest_index_preview_missing'), 'release readiness missing edition manifest index preview report blocker')
  }
  if (!readinessChecks.editionManifestIndexPreviewPassed) {
    assert(errors, report.blockers?.includes('edition_manifest_index_preview_failed'), 'release readiness missing edition manifest index preview failed blocker')
  }
  if (!readinessChecks.releasePublicationDownloadsVerified) {
    assert(errors, report.blockers?.includes('download_verification_missing'), 'release readiness missing publication download verification blocker')
  }
  if (!readinessChecks.releasePublicationApproved) {
    assert(errors, report.blockers?.includes('release_index_patch_not_approved'), 'release readiness missing publication patch approval blocker')
  }
  if (!readinessChecks.allRuntimeGatesCleared) {
    assert(errors, report.blockers?.includes('runtime_execution_gates_not_cleared'), 'release readiness missing runtime gate blocker')
  }
  if (!readinessChecks.allLocalRuntimeRehearsalsPresent) {
    assert(errors, report.blockers?.includes('local_runtime_rehearsal_reports_missing'), 'release readiness missing local runtime rehearsal report blocker')
  }
  if (!readinessChecks.allLocalRuntimeRehearsalsPassed) {
    assert(errors, report.blockers?.includes('local_runtime_rehearsal_failed'), 'release readiness missing local runtime rehearsal failed blocker')
  }
  if (!readinessChecks.allLauncherReportsPresent) {
    assert(errors, report.blockers?.includes('launcher_execution_reports_missing'), 'release readiness missing launcher execution report blocker')
  }
  if (!readinessChecks.allLauncherGatesCleared) {
    assert(errors, report.blockers?.includes('real_launcher_install_update_repair_rollback_missing'), 'release readiness missing launcher execution blocker')
  }
  if (!readinessChecks.allLocalLauncherRehearsalsPresent) {
    assert(errors, report.blockers?.includes('local_launcher_rehearsal_reports_missing'), 'release readiness missing local launcher rehearsal report blocker')
  }
  if (!readinessChecks.allLocalLauncherRehearsalsPassed) {
    assert(errors, report.blockers?.includes('local_launcher_rehearsal_failed'), 'release readiness missing local launcher rehearsal failed blocker')
  }
  if (!readinessChecks.allFinalReviewReportsPresent) {
    assert(errors, report.blockers?.includes('final_release_review_reports_missing'), 'release readiness missing final review report blocker')
  }
  if (!readinessChecks.allFinalReviewGatesCleared) {
    assert(errors, report.blockers?.includes('final_asset_legal_review_missing'), 'release readiness missing final review blocker')
  }
  if (!readinessChecks.allDistributionApprovalReportsPresent) {
    assert(errors, report.blockers?.includes('distribution_approval_reports_missing'), 'release readiness missing distribution approval report blocker')
  }
  if (!readinessChecks.allDistributionGatesCleared) {
    assert(errors, report.blockers?.includes('distribution_approval_missing'), 'release readiness missing distribution approval blocker')
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    reportPath,
    publicAlphaReady: report.publicAlphaReady,
    blockerCount: report.blockers?.length ?? 0,
    editionCount: report.editionResults?.length ?? 0,
    artifactCount: report.artifactResults?.length ?? 0,
    errors,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const moduleRoot = findModuleRoot(args.moduleRoot)
  const workspaceRoot = args.workspaceRoot ? path.resolve(args.workspaceRoot) : path.resolve(moduleRoot, '..', '..', '..')
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
  const reportPath = args.report ? path.resolve(args.report) : path.join(releaseRoot, MODULE_ID, 'openlands-release-readiness-report.json')
  const result = validate({ moduleRoot, workspaceRoot, releaseRoot, reportPath })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands release readiness report validated: publicAlphaReady=${result.publicAlphaReady}, blockers=${result.blockerCount}.`)
  } else {
    console.error(`Openlands release readiness report failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-release-readiness-report.mjs [options]

Options:
  --module-root <path>    Openlands module root. Auto-detected by default.
  --workspace-root <p>    Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --report <path>         Report path. Defaults to dist/echo-module-release/echoopenlandsprotocol/openlands-release-readiness-report.json.
  --json                  Print JSON output.
  --help                  Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}

import { spawnSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import { isPublicHttpsUrl } from './openlands-public-download-url.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    runtimeTarget: 'echo_native',
    reportName: 'native-runtime-execution-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    runtimeTarget: 'neoforge',
    reportName: 'neoforge-runtime-execution-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    runtimeTarget: 'echo_runtime_standalone',
    reportName: 'standalone-runtime-execution-report.json',
  },
}

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    edition: null,
    editionRoot: null,
    report: null,
    allowMissing: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--edition-root') args.editionRoot = argv[++index]
    else if (arg === '--report') args.report = argv[++index]
    else if (arg === '--allow-missing') args.allowMissing = true
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

function readJsonIfPossible(filePath) {
  try {
    return readJson(filePath)
  } catch {
    return null
  }
}

function assert(errors, condition, message) {
  if (!condition) errors.push(message)
}

function sortedUnique(values) {
  return [...new Set((values ?? []).filter(Boolean))].sort()
}

function sameSet(actualValues, expectedValues) {
  return JSON.stringify(sortedUnique(actualValues)) === JSON.stringify(sortedUnique(expectedValues))
}

function sameJson(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function requireFields(errors, object, fields, label) {
  for (const field of fields) {
    assert(errors, object?.[field] !== undefined && object?.[field] !== null && object?.[field] !== '', `${label} missing ${field}`)
  }
}

function findModuleRoot(explicitRoot, editionRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = editionRoot ? path.resolve(editionRoot) : process.cwd()
  for (;;) {
    const direct = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(direct)) return cursor
    const sibling = path.join(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(sibling)) return path.resolve(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID)
    const nested = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(nested)) return path.join(cursor, 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function sha256(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function stableRuntimeExecutionReport(value) {
  if (Array.isArray(value)) return value.map(stableRuntimeExecutionReport)
  if (!value || typeof value !== 'object') return value
  const stable = {}
  for (const [key, entry] of Object.entries(value)) {
    if (['generatedAt', 'startedAt', 'finishedAt'].includes(key)) continue
    stable[key] = stableRuntimeExecutionReport(entry)
  }
  return stable
}

function runGeneratorJson({ moduleRoot, editionRoot, editionKey, reportPath }) {
  const generatorPath = path.join(moduleRoot, 'scripts', 'generate-openlands-runtime-execution-report.mjs')
  const result = spawnSync(process.execPath, [
    generatorPath,
    '--module-root',
    moduleRoot,
    '--edition',
    editionKey,
    '--edition-root',
    editionRoot,
    '--out',
    reportPath,
    '--dry-run',
    '--json',
  ], {
    cwd: moduleRoot,
    encoding: 'utf8',
    shell: false,
  })
  if (result.status !== 0) {
    return {
      error: `runtime execution report generator dry-run failed: ${(result.stderr || result.stdout || '').trim()}`,
    }
  }
  try {
    return {
      json: JSON.parse(result.stdout),
    }
  } catch (error) {
    return {
      error: `runtime execution report generator dry-run did not emit valid JSON: ${error instanceof Error ? error.message : String(error)}`,
    }
  }
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function resolveMaybeFile(value, editionRoot, moduleRoot) {
  if (typeof value !== 'string' || value.length === 0) return null
  const candidates = [
    path.resolve(editionRoot, value),
    path.resolve(moduleRoot, value),
    path.resolve(moduleRoot, '..', '..', value),
    path.resolve(value),
  ]
  return candidates.find((candidate) => fileExists(candidate)) ?? null
}

function validateModuleArtifactBinding(errors, warnings, { report, artifactPath, expectedArtifactPattern, requireLocal }) {
  const artifactLabel = String(report.moduleArtifact ?? '')
  assert(errors, path.basename(artifactLabel) === expectedArtifactPattern, `runtime execution report moduleArtifact must be ${expectedArtifactPattern}`)
  if (!artifactPath) {
    const message = `runtime execution report moduleArtifact must resolve to local release artifact ${expectedArtifactPattern}`
    if (requireLocal) {
      assert(errors, false, message)
    } else {
      warnings.push('moduleArtifact was not a local file; sha256 format was checked but file bytes were not rehashed')
    }
    return
  }
  const stats = fs.statSync(artifactPath)
  assert(errors, stats.isFile(), 'runtime execution report moduleArtifact must resolve to a file')
  assert(errors, path.basename(artifactPath) === expectedArtifactPattern, `runtime execution report local moduleArtifact must be ${expectedArtifactPattern}`)
  const actualHash = sha256(artifactPath)
  assert(errors, actualHash.toLowerCase() === String(report.moduleArtifactSha256).toLowerCase(), 'runtime execution report moduleArtifactSha256 does not match local moduleArtifact')
}

function resolveSavedArtifactPath({ artifactName, scenarioId, editionRoot, reportPath, runtimeArtifactRoot, moduleRoot }) {
  if (typeof artifactName !== 'string' || artifactName.length === 0) return null
  const candidates = path.isAbsolute(artifactName)
    ? [artifactName]
    : [
        path.join(runtimeArtifactRoot, scenarioId, artifactName),
        path.join(runtimeArtifactRoot, artifactName),
        path.join(path.dirname(reportPath), scenarioId, artifactName),
        path.join(path.dirname(reportPath), artifactName),
        path.join(editionRoot, artifactName),
        path.join(moduleRoot, '..', '..', 'dist', 'echo-module-release', MODULE_ID, artifactName),
      ]
  return candidates.map((candidate) => path.resolve(candidate)).find((candidate) => fileExists(candidate)) ?? null
}

function realRuntimeSavedArtifactPath({ artifactName, scenarioId, runtimeArtifactRoot }) {
  if (typeof artifactName !== 'string' || artifactName.length === 0 || path.isAbsolute(artifactName)) return null
  return path.resolve(runtimeArtifactRoot, scenarioId, artifactName)
}

function looksLikePreflightEvidence(filePath, payload) {
  const normalizedPath = filePath.replace(/\\/g, '/').toLowerCase()
  if ([
    'local-runtime-rehearsal',
    'local-launcher-rehearsal',
    'release-publication-rehearsal',
    'edition-manifest-index-preview',
  ].some((marker) => normalizedPath.includes(marker))) {
    return true
  }
  const schema = String(payload?.schema ?? payload?.schemaVersion ?? '').toLowerCase()
  if ([
    'local_runtime_rehearsal',
    'local_launcher_rehearsal',
    'release_publication_rehearsal',
    'edition_manifest_index_preview',
  ].some((marker) => schema.includes(marker))) {
    return true
  }
  return payload?.rehearsalOnly === true
    || payload?.preflightOnly === true
    || payload?.previewOnly === true
    || payload?.clearsRuntimeGates === false
    || payload?.clearsLauncherGates === false
    || payload?.clearsDistributionGates === false
}

function validateRealSavedArtifact(errors, { artifactPath, expectedArtifactPath, artifactName, scenarioId }) {
  assert(errors, artifactPath === expectedArtifactPath, `runtime execution passed scenario ${scenarioId} saved artifact ${artifactName} must be saved under the real runtime artifact root`)
  const extension = path.extname(artifactPath).toLowerCase()
  if (extension === '.json') {
    const payload = readJsonIfPossible(artifactPath)
    assert(errors, payload !== null, `runtime execution passed scenario ${scenarioId} saved artifact ${artifactName} must be valid JSON`)
    if (payload) {
      assert(errors, !looksLikePreflightEvidence(artifactPath, payload), `runtime execution passed scenario ${scenarioId} saved artifact ${artifactName} must be real runtime evidence, not preflight or rehearsal evidence`)
    }
  } else {
    assert(errors, !looksLikePreflightEvidence(artifactPath, null), `runtime execution passed scenario ${scenarioId} saved artifact ${artifactName} must be real runtime evidence, not preflight or rehearsal evidence`)
  }
}

function validateRuntimeDownloadVerificationReport(errors, { payload, contract, releaseIndex, releaseModule }) {
  assert(errors, releaseIndex !== null, 'runtime execution artifact upload/download verification requires current Release Index')
  assert(errors, releaseModule !== undefined, `runtime execution artifact upload/download verification requires ${MODULE_ID} ${VERSION} in Release Index`)
  assert(errors, payload.schema === 'echo.openlands.release_publication_download_verification_summary.v1', 'runtime execution download-verification-report schema mismatch')
  assert(errors, payload.moduleId === MODULE_ID, 'runtime execution download-verification-report module id mismatch')
  assert(errors, payload.moduleVersion === VERSION, 'runtime execution download-verification-report module version mismatch')
  assert(errors, payload.releaseId === releaseIndex?.releaseId, 'runtime execution download-verification-report release id mismatch')
  const results = Array.isArray(payload.verificationResults) ? payload.verificationResults : []
  const targets = contract.releasePublication?.artifactTargets ?? []
  assert(errors, payload.artifactCount === targets.length, 'runtime execution download-verification-report artifact count mismatch')
  assert(errors, sameSet(results.map((entry) => entry.id), targets.map((target) => target.id)), 'runtime execution download-verification-report artifact ids mismatch')
  for (const target of targets) {
    const result = results.find((entry) => entry.id === target.id)
    assert(errors, result !== undefined, `runtime execution download-verification-report missing ${target.id}`)
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    assert(errors, releaseArtifact !== undefined, `runtime execution download-verification-report Release Index missing ${target.file}`)
    if (!result || !releaseArtifact) continue
    assert(errors, result.file === target.file, `runtime execution download-verification-report ${target.id} file mismatch`)
    assert(errors, isPublicHttpsUrl(releaseArtifact.downloadUrl), `runtime execution download-verification-report ${target.id} Release Index downloadUrl must use a public https URL`)
    assert(errors, isPublicHttpsUrl(result.downloadUrl), `runtime execution download-verification-report ${target.id} downloadUrl must use a public https URL`)
    assert(errors, result.downloadUrl === releaseArtifact.downloadUrl, `runtime execution download-verification-report ${target.id} downloadUrl must match Release Index`)
    assert(errors, isPublicHttpsUrl(result.finalUrl), `runtime execution download-verification-report ${target.id} finalUrl must use a public https URL`)
    assert(errors, result.statusCode === 200, `runtime execution download-verification-report ${target.id} statusCode must be 200`)
    assert(errors, result.expectedSha256 === releaseArtifact.sha256, `runtime execution download-verification-report ${target.id} expected sha mismatch`)
    assert(errors, result.downloadedSha256 === releaseArtifact.sha256, `runtime execution download-verification-report ${target.id} downloaded sha mismatch`)
    assert(errors, result.expectedSize === releaseArtifact.size, `runtime execution download-verification-report ${target.id} expected size mismatch`)
    assert(errors, result.downloadedSize === releaseArtifact.size, `runtime execution download-verification-report ${target.id} downloaded size mismatch`)
    assert(errors, result.sha256Matches === true, `runtime execution download-verification-report ${target.id} must confirm sha match`)
    assert(errors, result.sizeMatches === true, `runtime execution download-verification-report ${target.id} must confirm size match`)
    if (result.contentLength !== null && result.contentLength !== undefined) {
      assert(errors, Number.parseInt(result.contentLength, 10) === releaseArtifact.size, `runtime execution download-verification-report ${target.id} content length mismatch`)
    }
  }
}

function buildContract(moduleRoot) {
  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const acceptance = readJson(path.join(dataRoot, 'systems', 'runtime_execution_acceptance.json'))
  const harness = readJson(path.join(dataRoot, 'systems', 'runtime_execution_harness_plan.json'))
  const production = readJson(path.join(dataRoot, 'progression', 'production_phase_matrix.json'))
  const releasePublication = readJson(path.join(dataRoot, 'systems', 'release_publication_manifest_contract.json'))
  const releaseRoot = path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseIndex = fileExists(releaseIndexPath) ? readJson(releaseIndexPath) : null
  const releaseModule = (releaseIndex?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const scenarioById = new Map((acceptance.scenarios ?? []).map((scenario) => [scenario.id, scenario]))
  const suiteById = new Map((acceptance.executionSuites ?? []).map((suite) => [suite.id, suite]))
  const gateById = new Map((acceptance.runtimeGates ?? []).map((gate) => [gate.id, gate]))
  const productionGateIds = sortedUnique((production.phases ?? [])
    .flatMap((phase) => phase.checkpoints ?? [])
    .flatMap((checkpoint) => checkpoint.evidence ?? [])
    .filter((evidence) => evidence.kind === 'runtime_gate')
    .map((evidence) => evidence.id))
  return {
    acceptance,
    harness,
    releasePublication,
    releaseIndex,
    releaseModule,
    productionGateIds,
    expectedScenarioIds: sortedUnique((acceptance.scenarios ?? []).map((scenario) => scenario.id)),
    scenarioById,
    harnessScenarioById: new Map((harness.scenarioBindings ?? []).map((scenario) => [scenario.id, scenario])),
    suiteById,
    gateById,
  }
}

function validateReport({ moduleRoot, editionRoot, editionKey, reportPath, allowMissing }) {
  const errors = []
  const warnings = []
  const edition = EDITIONS[editionKey]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)

  const contract = buildContract(moduleRoot)
  const expectedEditionReport = (contract.acceptance.editionReports ?? []).find((entry) => entry.edition === editionKey)
  const expectedEditionHarness = (contract.harness.editionHarnesses ?? []).find((entry) => entry.edition === editionKey)
  assert(errors, expectedEditionReport !== undefined, `runtime execution acceptance missing edition ${editionKey}`)
  assert(errors, expectedEditionReport?.runtimeTarget === edition.runtimeTarget, `runtime execution acceptance runtime target mismatch for ${editionKey}`)
  assert(errors, contract.harness.schema === 'echo.openlands.systems.runtime_execution_harness_plan.v1', 'runtime execution harness schema mismatch')
  assert(errors, contract.harness.sourceContracts?.runtimeExecutionAcceptance === 'systems/runtime_execution_acceptance.json', 'runtime execution harness source contract mismatch')
  assert(errors, sameSet((contract.harness.scenarioBindings ?? []).map((scenario) => scenario.id), contract.expectedScenarioIds), 'runtime execution harness must cover every execution scenario')
  assert(errors, expectedEditionHarness !== undefined, `runtime execution harness missing edition ${editionKey}`)
  assert(errors, expectedEditionHarness?.runtimeTarget === edition.runtimeTarget, `runtime execution harness runtime target mismatch for ${editionKey}`)
  assert(errors, expectedEditionHarness?.requiredReport === expectedEditionReport?.requiredReport, `runtime execution harness report path mismatch for ${editionKey}`)
  assert(errors, expectedEditionHarness?.artifactPattern === expectedEditionReport?.artifactPattern, `runtime execution harness artifact pattern mismatch for ${editionKey}`)
  assert(errors, typeof expectedEditionHarness?.runtimeArtifactRoot === 'string' && expectedEditionHarness.runtimeArtifactRoot.startsWith(`evidence/runtime-execution/${editionKey}`), `runtime execution harness artifact root mismatch for ${editionKey}`)
  for (const expectedScenario of contract.acceptance.scenarios ?? []) {
    const binding = contract.harnessScenarioById.get(expectedScenario.id)
    assert(errors, binding !== undefined, `runtime execution harness missing binding ${expectedScenario.id}`)
    if (!binding) continue
    assert(errors, binding.suiteId === expectedScenario.suiteId, `runtime execution harness binding ${expectedScenario.id} suiteId mismatch`)
    assert(errors, sameSet(binding.gateIds, expectedScenario.gateIds), `runtime execution harness binding ${expectedScenario.id} gateIds mismatch`)
    assert(errors, sameSet(binding.inputFixtureRefs, expectedScenario.inputFixtureRefs), `runtime execution harness binding ${expectedScenario.id} inputFixtureRefs mismatch`)
    assert(errors, sameSet(binding.actions, expectedScenario.actions), `runtime execution harness binding ${expectedScenario.id} actions mismatch`)
    assert(errors, sameSet(binding.assertions, expectedScenario.assertions), `runtime execution harness binding ${expectedScenario.id} assertions mismatch`)
    assert(errors, Array.isArray(binding.requiredSavedArtifacts) && binding.requiredSavedArtifacts.length > 0, `runtime execution harness binding ${expectedScenario.id} requiredSavedArtifacts missing`)
  }

  const resolvedReportPath = reportPath
    ? path.resolve(reportPath)
    : path.join(editionRoot, 'evidence', edition.reportName)
  const runtimeArtifactRoot = path.resolve(editionRoot, expectedEditionHarness?.runtimeArtifactRoot ?? path.join('evidence', 'runtime-execution', editionKey))

  if (!fileExists(resolvedReportPath)) {
    const result = {
      status: allowMissing ? 'missing' : 'failed',
      edition: editionKey,
      runtimeTarget: edition.runtimeTarget,
      reportPath: resolvedReportPath,
      expectedScenarioCount: contract.expectedScenarioIds.length,
      expectedRuntimeGateCount: contract.productionGateIds.length,
      missingReason: 'runtime execution report has not been produced by the real adapter yet',
      errors: allowMissing ? [] : [`runtime execution report missing: ${resolvedReportPath}`],
      warnings,
    }
    return result
  }

  const report = readJson(resolvedReportPath)
  const reportContract = contract.acceptance.reportContract ?? {}
  const scenarioResults = report.scenarioResults ?? []
  const scenarioResultById = new Map(scenarioResults.map((scenario) => [scenario.id, scenario]))
  const allowedReportStatus = reportContract.allowedReportStatus ?? ['passed', 'failed', 'blocked']
  const allowedScenarioStatus = reportContract.allowedScenarioStatus ?? ['passed', 'failed', 'blocked', 'skipped']
  const allowedAssertionStatus = reportContract.allowedAssertionStatus ?? ['passed', 'failed', 'blocked', 'skipped']

  for (const field of reportContract.requiredReportFields ?? []) {
    requireFields(errors, report, [field], 'runtime execution report')
  }
  assert(errors, report.schema === reportContract.schema, 'runtime execution report schema mismatch')
  assert(errors, allowedReportStatus.includes(report.status), `runtime execution report status must be one of ${allowedReportStatus.join(', ')}`)
  assert(errors, report.edition === editionKey, `runtime execution report edition must be ${editionKey}`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `runtime execution report runtimeTarget must be ${edition.runtimeTarget}`)
  assert(errors, report.moduleId === MODULE_ID, `runtime execution report moduleId must be ${MODULE_ID}`)
  assert(errors, report.moduleVersion === VERSION, `runtime execution report moduleVersion must be ${VERSION}`)
  assert(errors, /^[a-f0-9]{64}$/i.test(String(report.moduleArtifactSha256 ?? '')), 'runtime execution report moduleArtifactSha256 must be a 64-character hex string')

  const artifactPath = resolveMaybeFile(report.moduleArtifact, editionRoot, moduleRoot)
  validateModuleArtifactBinding(errors, warnings, {
    report,
    artifactPath,
    expectedArtifactPattern: expectedEditionReport?.artifactPattern,
    requireLocal: report.status === 'passed'
      || report.publicAlphaReady === true
      || scenarioResults.some((scenario) => scenario.status === 'passed'),
  })

  assert(errors, sameSet(scenarioResults.map((scenario) => scenario.id), contract.expectedScenarioIds), 'runtime execution report scenarioResults must contain exactly the acceptance scenarios')
  for (const scenario of scenarioResults) {
    const expected = contract.scenarioById.get(scenario.id)
    const harnessBinding = contract.harnessScenarioById.get(scenario.id)
    assert(errors, expected !== undefined, `runtime execution report contains unknown scenario ${scenario.id}`)
    if (!expected) continue
    for (const field of reportContract.requiredScenarioFields ?? []) {
      requireFields(errors, scenario, [field], `runtime execution scenario ${scenario.id}`)
    }
    assert(errors, scenario.suiteId === expected.suiteId, `runtime execution scenario ${scenario.id} suiteId mismatch`)
    assert(errors, scenario.runtimeTarget === edition.runtimeTarget, `runtime execution scenario ${scenario.id} runtimeTarget mismatch`)
    assert(errors, scenario.artifactSha256 === report.moduleArtifactSha256, `runtime execution scenario ${scenario.id} artifactSha256 must match report moduleArtifactSha256`)
    assert(errors, allowedScenarioStatus.includes(scenario.status), `runtime execution scenario ${scenario.id} status must be allowed`)
    assert(errors, Number.isInteger(scenario.durationMs) && scenario.durationMs >= 0, `runtime execution scenario ${scenario.id} durationMs must be a non-negative integer`)
    assert(errors, sameSet(scenario.inputFixtureRefs, expected.inputFixtureRefs), `runtime execution scenario ${scenario.id} inputFixtureRefs mismatch`)
    assert(errors, sameSet(scenario.plannedActions, expected.actions), `runtime execution scenario ${scenario.id} plannedActions mismatch`)
    assert(errors, Array.isArray(scenario.actionsRun), `runtime execution scenario ${scenario.id} actionsRun must be an array`)
    if (scenario.status === 'passed') {
      assert(errors, sameSet(scenario.actionsRun, expected.actions), `runtime execution passed scenario ${scenario.id} actionsRun mismatch`)
    } else {
      const unknownActions = (scenario.actionsRun ?? []).filter((action) => !(expected.actions ?? []).includes(action))
      assert(errors, unknownActions.length === 0, `runtime execution scenario ${scenario.id} actionsRun contains unknown actions`)
    }
    const assertions = scenario.assertions ?? []
    const assertionIds = assertions.map((assertion) => assertion.id)
    assert(errors, sameSet(assertionIds, expected.assertions), `runtime execution scenario ${scenario.id} assertions mismatch`)
    for (const assertion of assertions) {
      for (const field of reportContract.requiredAssertionFields ?? []) {
        requireFields(errors, assertion, [field], `runtime execution assertion ${scenario.id}/${assertion.id}`)
      }
      assert(errors, allowedAssertionStatus.includes(assertion.status), `runtime execution assertion ${scenario.id}/${assertion.id} status must be allowed`)
      if (scenario.status === 'passed') {
        assert(errors, assertion.status === 'passed', `runtime execution passed scenario ${scenario.id} has non-passed assertion ${assertion.id}`)
      }
    }
    if (scenario.status === 'passed') {
      assert(errors, Array.isArray(scenario.savedArtifacts), `runtime execution passed scenario ${scenario.id} must record savedArtifacts`)
      assert(errors, sameSet(scenario.savedArtifacts, harnessBinding?.requiredSavedArtifacts), `runtime execution passed scenario ${scenario.id} savedArtifacts mismatch`)
      for (const artifactName of harnessBinding?.requiredSavedArtifacts ?? []) {
        const artifactPath = resolveSavedArtifactPath({
          artifactName,
          scenarioId: scenario.id,
          editionRoot,
          reportPath: resolvedReportPath,
          runtimeArtifactRoot,
          moduleRoot,
        })
        assert(errors, artifactPath !== null, `runtime execution passed scenario ${scenario.id} missing saved artifact file ${artifactName}`)
        if (artifactPath) {
          const stats = fs.statSync(artifactPath)
          assert(errors, stats.isFile(), `runtime execution passed scenario ${scenario.id} saved artifact ${artifactName} must be a file`)
          assert(errors, stats.size > 0, `runtime execution passed scenario ${scenario.id} saved artifact ${artifactName} must not be empty`)
          validateRealSavedArtifact(errors, {
            artifactPath,
            expectedArtifactPath: realRuntimeSavedArtifactPath({
              artifactName,
              scenarioId: scenario.id,
              runtimeArtifactRoot,
            }),
            artifactName,
            scenarioId: scenario.id,
          })
          if (scenario.id === 'artifact_upload_download_hash' && artifactName === 'download-verification-report.json') {
            const payload = readJsonIfPossible(artifactPath)
            if (payload) validateRuntimeDownloadVerificationReport(errors, {
              payload,
              contract,
              releaseIndex: contract.releaseIndex,
              releaseModule: contract.releaseModule,
            })
          }
        }
      }
    }
  }

  const scenarioIdsByGate = new Map()
  for (const expectedScenario of contract.acceptance.scenarios ?? []) {
    for (const gateId of expectedScenario.gateIds ?? []) {
      const ids = scenarioIdsByGate.get(gateId) ?? []
      ids.push(expectedScenario.id)
      scenarioIdsByGate.set(gateId, ids)
    }
  }
  const gatesClearedByPassingScenarios = contract.productionGateIds.filter((gateId) =>
    (scenarioIdsByGate.get(gateId) ?? []).every((scenarioId) => scenarioResultById.get(scenarioId)?.status === 'passed'))
  const clearedRuntimeGates = sortedUnique(report.clearedRuntimeGates ?? [])
  const remainingRuntimeGates = sortedUnique(report.remainingRuntimeGates ?? [])

  assert(errors, sameSet([...clearedRuntimeGates, ...remainingRuntimeGates], contract.productionGateIds), 'runtime execution report cleared and remaining gates must cover every runtime gate')
  for (const gateId of clearedRuntimeGates) {
    assert(errors, gatesClearedByPassingScenarios.includes(gateId), `runtime execution report clears ${gateId} without all required scenarios passing`)
  }
  for (const gateId of remainingRuntimeGates) {
    assert(errors, contract.productionGateIds.includes(gateId), `runtime execution report remainingRuntimeGates contains unknown gate ${gateId}`)
  }
  for (const gateId of clearedRuntimeGates) {
    assert(errors, !remainingRuntimeGates.includes(gateId), `runtime execution gate ${gateId} cannot be both cleared and remaining`)
  }

  if (report.status === 'passed') {
    assert(errors, scenarioResults.every((scenario) => scenario.status === 'passed'), 'passed runtime execution report requires every scenario to pass')
    assert(errors, sameSet(clearedRuntimeGates, contract.productionGateIds), 'passed runtime execution report must clear every runtime gate')
    assert(errors, remainingRuntimeGates.length === 0, 'passed runtime execution report must have no remaining runtime gates')
  } else {
    assert(errors, report.publicAlphaReady === false, 'non-passed runtime execution report must not mark publicAlphaReady true')
  }
  if (report.publicAlphaReady === true) {
    assert(errors, report.status === 'passed', 'publicAlphaReady true requires report status passed')
    assert(errors, sameSet(clearedRuntimeGates, contract.productionGateIds), 'publicAlphaReady true requires every runtime gate cleared')
  }
  if (report.status === 'blocked' && report.generatedBy === 'generate-openlands-runtime-execution-report.mjs') {
    const generated = runGeneratorJson({
      moduleRoot,
      editionRoot,
      editionKey,
      reportPath: resolvedReportPath,
    })
    if (generated.error) {
      errors.push(generated.error)
    } else if (generated.json?.status === 'blocked') {
      assert(
        errors,
        sameJson(stableRuntimeExecutionReport(report), stableRuntimeExecutionReport(generated.json)),
        'runtime execution report stale against generator dry-run',
      )
    }
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    edition: editionKey,
    runtimeTarget: edition.runtimeTarget,
    reportPath: resolvedReportPath,
    scenarioCount: scenarioResults.length,
    reportStatus: report.status,
    runtimeGateCount: contract.productionGateIds.length,
    clearedRuntimeGates: clearedRuntimeGates.length,
    remainingRuntimeGates: remainingRuntimeGates.length,
    publicAlphaReady: report.publicAlphaReady === true,
    errors,
    warnings,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : process.cwd()
  const moduleRoot = findModuleRoot(args.moduleRoot, editionRoot)
  const result = validateReport({
    moduleRoot,
    editionRoot,
    editionKey: args.edition,
    reportPath: args.report,
    allowMissing: args.allowMissing,
  })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands ${result.edition} runtime execution report validated (${result.reportStatus}): ${result.scenarioCount} scenarios, ${result.clearedRuntimeGates}/${result.runtimeGateCount} gates cleared.`)
    for (const warning of result.warnings) console.warn(`warning: ${warning}`)
  } else if (result.status === 'missing') {
    console.log(`Openlands ${result.edition} runtime execution report missing: ${result.reportPath}`)
  } else {
    console.error(`Openlands ${result.edition} runtime execution report failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-runtime-execution-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Path to addons/echoopenlandsprotocol. Auto-detected by default.
  --report <path>         Runtime execution report path. Defaults to evidence/<edition>-runtime-execution-report.json.
  --allow-missing         Return status "missing" instead of failing when the report does not exist.
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

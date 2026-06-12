import { spawnSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    runtimeTarget: 'echo_native',
    reportName: 'native-launcher-execution-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    runtimeTarget: 'neoforge',
    reportName: 'neoforge-launcher-execution-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    runtimeTarget: 'echo_runtime_standalone',
    reportName: 'standalone-launcher-execution-report.json',
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

function stableLauncherExecutionReport(value) {
  if (Array.isArray(value)) return value.map(stableLauncherExecutionReport)
  if (!value || typeof value !== 'object') return value
  const stable = {}
  for (const [key, entry] of Object.entries(value)) {
    if (['generatedAt', 'startedAt', 'finishedAt'].includes(key)) continue
    stable[key] = stableLauncherExecutionReport(entry)
  }
  return stable
}

function runGeneratorJson({ moduleRoot, editionRoot, editionKey, reportPath }) {
  const generatorPath = path.join(moduleRoot, 'scripts', 'generate-openlands-launcher-execution-report.mjs')
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
      error: `launcher execution report generator dry-run failed: ${(result.stderr || result.stdout || '').trim()}`,
    }
  }
  try {
    return {
      json: JSON.parse(result.stdout),
    }
  } catch (error) {
    return {
      error: `launcher execution report generator dry-run did not emit valid JSON: ${error instanceof Error ? error.message : String(error)}`,
    }
  }
}

function normalizeHashText(value) {
  return String(value ?? '').trim().toLowerCase()
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
  assert(errors, path.basename(artifactLabel) === expectedArtifactPattern, `launcher execution report moduleArtifact must be ${expectedArtifactPattern}`)
  if (!artifactPath) {
    const message = `launcher execution report moduleArtifact must resolve to local release artifact ${expectedArtifactPattern}`
    if (requireLocal) {
      assert(errors, false, message)
    } else {
      warnings.push('moduleArtifact was not a local file; sha256 format was checked but file bytes were not rehashed')
    }
    return
  }
  const stats = fs.statSync(artifactPath)
  assert(errors, stats.isFile(), 'launcher execution report moduleArtifact must resolve to a file')
  assert(errors, path.basename(artifactPath) === expectedArtifactPattern, `launcher execution report local moduleArtifact must be ${expectedArtifactPattern}`)
  const actualHash = sha256(artifactPath)
  assert(errors, actualHash.toLowerCase() === String(report.moduleArtifactSha256).toLowerCase(), 'launcher execution report moduleArtifactSha256 does not match local moduleArtifact')
}

function resolveSavedArtifactPath({ artifactName, flowId, editionRoot, reportPath, launcherArtifactRoot, moduleRoot }) {
  if (typeof artifactName !== 'string' || artifactName.length === 0) return null
  const candidates = path.isAbsolute(artifactName)
    ? [artifactName]
    : [
        path.join(launcherArtifactRoot, flowId, artifactName),
        path.join(launcherArtifactRoot, artifactName),
        path.join(path.dirname(reportPath), flowId, artifactName),
        path.join(path.dirname(reportPath), artifactName),
        path.join(editionRoot, artifactName),
        path.join(moduleRoot, '..', '..', 'dist', 'echo-module-release', MODULE_ID, artifactName),
      ]
  return candidates.map((candidate) => path.resolve(candidate)).find((candidate) => fileExists(candidate)) ?? null
}

function realLauncherSavedArtifactPath({ artifactName, flowId, launcherArtifactRoot }) {
  if (typeof artifactName !== 'string' || artifactName.length === 0 || path.isAbsolute(artifactName)) return null
  return path.resolve(launcherArtifactRoot, flowId, artifactName)
}

function looksLikePreflightEvidence(filePath, payload) {
  const normalizedPath = filePath.replace(/\\/g, '/').toLowerCase()
  if ([
    'local-launcher-rehearsal',
    'local-runtime-rehearsal',
    'release-publication-rehearsal',
    'edition-manifest-index-preview',
  ].some((marker) => normalizedPath.includes(marker))) {
    return true
  }
  const schema = String(payload?.schema ?? payload?.schemaVersion ?? '').toLowerCase()
  if ([
    'local_launcher_rehearsal',
    'local_runtime_rehearsal',
    'release_publication_rehearsal',
    'edition_manifest_index_preview',
  ].some((marker) => schema.includes(marker))) {
    return true
  }
  return payload?.rehearsalOnly === true
    || payload?.preflightOnly === true
    || payload?.previewOnly === true
    || payload?.clearsLauncherGates === false
    || payload?.clearsRuntimeGates === false
    || payload?.clearsDistributionGates === false
}

function fileTextLooksLikePreflight(filePath) {
  const extension = path.extname(filePath).toLowerCase()
  if (!['.txt', '.log', '.md'].includes(extension)) return false
  const text = fs.readFileSync(filePath, 'utf8').slice(0, 65536).toLowerCase()
  return text.includes('local launcher rehearsal')
    || text.includes('local_runtime_rehearsal')
    || text.includes('local_launcher_rehearsal')
    || text.includes('rehearsalonly')
    || text.includes('preflightonly')
}

function validateRealSavedArtifact(errors, { artifactPath, expectedArtifactPath, artifactName, flowId }) {
  assert(errors, artifactPath === expectedArtifactPath, `launcher execution passed flow ${flowId} saved artifact ${artifactName} must be saved under the real launcher artifact root`)
  const extension = path.extname(artifactPath).toLowerCase()
  if (extension === '.json') {
    const payload = readJsonIfPossible(artifactPath)
    assert(errors, payload !== null, `launcher execution passed flow ${flowId} saved artifact ${artifactName} must be valid JSON`)
    if (payload) {
      assert(errors, !looksLikePreflightEvidence(artifactPath, payload), `launcher execution passed flow ${flowId} saved artifact ${artifactName} must be real launcher evidence, not preflight or rehearsal evidence`)
    }
  } else {
    assert(errors, !looksLikePreflightEvidence(artifactPath, null), `launcher execution passed flow ${flowId} saved artifact ${artifactName} must be real launcher evidence, not preflight or rehearsal evidence`)
    assert(errors, !fileTextLooksLikePreflight(artifactPath), `launcher execution passed flow ${flowId} saved artifact ${artifactName} must not be a local rehearsal text artifact`)
  }
}

function jsonTextIncludes(value, needle) {
  return JSON.stringify(value).toLowerCase().includes(String(needle ?? '').toLowerCase())
}

function walkJson(value, visit) {
  if (Array.isArray(value)) {
    for (const entry of value) walkJson(entry, visit)
    return
  }
  if (!value || typeof value !== 'object') return
  for (const [key, entry] of Object.entries(value)) {
    visit(key, entry)
    walkJson(entry, visit)
  }
}

function deletionEvidenceIsUnsafe(payload) {
  let unsafe = false
  walkJson(payload, (key, value) => {
    const normalizedKey = key.toLowerCase()
    if (typeof value === 'number' && normalizedKey.includes('delet') && value > 0) unsafe = true
    if (typeof value === 'boolean'
      && normalizedKey.includes('delet')
      && !normalizedKey.includes('no')
      && !normalizedKey.includes('without')
      && value === true) {
      unsafe = true
    }
  })
  return unsafe
}

function validateLauncherManifestArtifact(errors, { payload, artifactName, flowId, report }) {
  assert(errors, jsonTextIncludes(payload, MODULE_ID), `launcher execution passed flow ${flowId} ${artifactName} must mention ${MODULE_ID}`)
  if (!['previous-manifest.json', 'rollback-manifest.json'].includes(artifactName)) {
    assert(errors, jsonTextIncludes(payload, VERSION), `launcher execution passed flow ${flowId} ${artifactName} must mention ${VERSION}`)
    assert(errors, jsonTextIncludes(payload, report.moduleArtifactSha256), `launcher execution passed flow ${flowId} ${artifactName} must mention moduleArtifactSha256`)
  }
}

function validatePreservationArtifact(errors, { payload, artifactName, flowId }) {
  assert(errors, jsonTextIncludes(payload, 'preserv') || jsonTextIncludes(payload, 'hash'), `launcher execution passed flow ${flowId} ${artifactName} must record preservation or hash evidence`)
  assert(errors, !deletionEvidenceIsUnsafe(payload), `launcher execution passed flow ${flowId} ${artifactName} must not report deleted world or config data`)
}

function validateLauncherSavedArtifactSemantics(errors, { artifactPath, artifactName, flow, report }) {
  if (path.extname(artifactPath).toLowerCase() !== '.json') return
  const payload = readJsonIfPossible(artifactPath)
  if (!payload) return
  if (artifactName.endsWith('manifest.json')) {
    validateLauncherManifestArtifact(errors, {
      payload,
      artifactName,
      flowId: flow.id,
      report,
    })
  }
  if (artifactName.includes('preservation') || artifactName.includes('preservation-diff')) {
    validatePreservationArtifact(errors, {
      payload,
      artifactName,
      flowId: flow.id,
    })
  }
  if (artifactName === 'fresh-world-summary.json') {
    assert(errors, jsonTextIncludes(payload, 'world'), 'launcher execution install fresh-world-summary.json must record world evidence')
    assert(errors, jsonTextIncludes(payload, 'standard'), 'launcher execution install fresh-world-summary.json must record Openlands Standard world evidence')
    assert(errors, !jsonTextIncludes(payload, 'hardcoreMetersOn'), 'launcher execution install fresh-world-summary.json must not report hardcore meters on')
  }
  if (artifactName === 'corruption-record.json') {
    assert(errors, jsonTextIncludes(payload, 'corrupt'), 'launcher execution repair corruption-record.json must record corruption evidence')
  }
  if (artifactName === 'repair-hash-report.json') {
    assert(errors, jsonTextIncludes(payload, 'restored'), 'launcher execution repair repair-hash-report.json must record restored artifact evidence')
    assert(errors, jsonTextIncludes(payload, report.moduleArtifactSha256), 'launcher execution repair repair-hash-report.json must mention moduleArtifactSha256')
  }
  if (artifactName === 'schema-compatibility-decision.json') {
    assert(errors, jsonTextIncludes(payload, 'schema'), 'launcher execution rollback schema-compatibility-decision.json must record schema evidence')
    assert(errors, jsonTextIncludes(payload, 'decision'), 'launcher execution rollback schema-compatibility-decision.json must record a decision')
  }
  if (artifactName === 'return-to-current-manifest-report.json') {
    assert(errors, jsonTextIncludes(payload, 'return'), 'launcher execution rollback return-to-current-manifest-report.json must record return evidence')
    assert(errors, jsonTextIncludes(payload, 'current'), 'launcher execution rollback return-to-current-manifest-report.json must record current manifest evidence')
  }
}

function buildContract(moduleRoot) {
  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const acceptance = readJson(path.join(dataRoot, 'systems', 'launcher_execution_acceptance.json'))
  const launcherFlow = readJson(path.join(dataRoot, 'systems', 'launcher_flow_acceptance.json'))
  const harness = readJson(path.join(dataRoot, 'systems', 'launcher_execution_harness_plan.json'))
  const flowById = new Map((acceptance.executionFlows ?? []).map((flow) => [flow.id, flow]))
  const launcherFlowById = new Map((launcherFlow.requiredLauncherFlows ?? []).map((flow) => [flow.id, flow]))
  const gateById = new Map((acceptance.launcherGates ?? []).map((gate) => [gate.id, gate]))
  return {
    acceptance,
    launcherFlow,
    harness,
    flowById,
    launcherFlowById,
    gateById,
    harnessFlowById: new Map((harness.flowBindings ?? []).map((flow) => [flow.id, flow])),
    expectedFlowIds: sortedUnique((acceptance.executionFlows ?? []).map((flow) => flow.id)),
    launcherFlowIds: sortedUnique((launcherFlow.requiredLauncherFlows ?? []).map((flow) => flow.id)),
    expectedGateIds: sortedUnique((acceptance.launcherGates ?? []).map((gate) => gate.id)),
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
  assert(errors, expectedEditionReport !== undefined, `launcher execution acceptance missing edition ${editionKey}`)
  assert(errors, expectedEditionReport?.runtimeTarget === edition.runtimeTarget, `launcher execution acceptance runtime target mismatch for ${editionKey}`)
  assert(errors, sameSet(contract.expectedFlowIds, contract.launcherFlowIds), 'launcher execution flows must match launcher flow acceptance ids')
  assert(errors, contract.harness.schema === 'echo.openlands.systems.launcher_execution_harness_plan.v1', 'launcher execution harness schema mismatch')
  assert(errors, contract.harness.sourceContracts?.launcherExecutionAcceptance === 'systems/launcher_execution_acceptance.json', 'launcher execution harness source contract mismatch')
  assert(errors, sameSet((contract.harness.flowBindings ?? []).map((flow) => flow.id), contract.expectedFlowIds), 'launcher execution harness must cover every execution flow')
  assert(errors, expectedEditionHarness !== undefined, `launcher execution harness missing edition ${editionKey}`)
  assert(errors, expectedEditionHarness?.runtimeTarget === edition.runtimeTarget, `launcher execution harness runtime target mismatch for ${editionKey}`)
  assert(errors, expectedEditionHarness?.requiredReport === expectedEditionReport?.requiredReport, `launcher execution harness report path mismatch for ${editionKey}`)
  assert(errors, expectedEditionHarness?.artifactPattern === expectedEditionReport?.artifactPattern, `launcher execution harness artifact pattern mismatch for ${editionKey}`)
  assert(errors, typeof expectedEditionHarness?.launcherArtifactRoot === 'string' && expectedEditionHarness.launcherArtifactRoot.startsWith(`evidence/launcher-execution/${editionKey}`), `launcher execution harness artifact root mismatch for ${editionKey}`)
  for (const expectedFlow of contract.acceptance.executionFlows ?? []) {
    const binding = contract.harnessFlowById.get(expectedFlow.id)
    assert(errors, binding !== undefined, `launcher execution harness missing binding ${expectedFlow.id}`)
    if (!binding) continue
    assert(errors, sameSet(binding.gateIds, expectedFlow.gateIds), `launcher execution harness binding ${expectedFlow.id} gateIds mismatch`)
    assert(errors, sameSet(binding.inputFixtureRefs, expectedFlow.inputFixtureRefs), `launcher execution harness binding ${expectedFlow.id} inputFixtureRefs mismatch`)
    assert(errors, sameSet(binding.preconditions, expectedFlow.preconditions), `launcher execution harness binding ${expectedFlow.id} preconditions mismatch`)
    assert(errors, sameSet(binding.plannedActions, expectedFlow.plannedActions), `launcher execution harness binding ${expectedFlow.id} plannedActions mismatch`)
    assert(errors, sameSet(binding.assertions, expectedFlow.assertions), `launcher execution harness binding ${expectedFlow.id} assertions mismatch`)
    assert(errors, sameSet(binding.requiredSavedArtifacts, expectedFlow.requiredSavedArtifacts), `launcher execution harness binding ${expectedFlow.id} requiredSavedArtifacts mismatch`)
    assert(errors, JSON.stringify(binding.worldStatePolicy ?? {}) === JSON.stringify(expectedFlow.worldStatePolicy ?? {}), `launcher execution harness binding ${expectedFlow.id} worldStatePolicy mismatch`)
  }

  const resolvedReportPath = reportPath
    ? path.resolve(reportPath)
    : path.join(editionRoot, 'evidence', edition.reportName)
  const launcherArtifactRoot = path.resolve(editionRoot, expectedEditionHarness?.launcherArtifactRoot ?? path.join('evidence', 'launcher-execution', editionKey))

  if (!fileExists(resolvedReportPath)) {
    return {
      status: allowMissing ? 'missing' : 'failed',
      edition: editionKey,
      runtimeTarget: edition.runtimeTarget,
      reportPath: resolvedReportPath,
      expectedFlowCount: contract.expectedFlowIds.length,
      expectedLauncherGateCount: contract.expectedGateIds.length,
      missingReason: 'launcher execution report has not been produced by the real launcher harness yet',
      errors: allowMissing ? [] : [`launcher execution report missing: ${resolvedReportPath}`],
      warnings,
    }
  }

  const report = readJson(resolvedReportPath)
  const reportContract = contract.acceptance.reportContract ?? {}
  const flowResults = report.flowResults ?? []
  const flowResultById = new Map(flowResults.map((flow) => [flow.id, flow]))
  const allowedReportStatus = reportContract.allowedReportStatus ?? ['passed', 'failed', 'blocked']
  const allowedFlowStatus = reportContract.allowedFlowStatus ?? ['passed', 'failed', 'blocked', 'skipped']
  const allowedAssertionStatus = reportContract.allowedAssertionStatus ?? ['passed', 'failed', 'blocked', 'skipped']

  for (const field of reportContract.requiredReportFields ?? []) {
    requireFields(errors, report, [field], 'launcher execution report')
  }
  assert(errors, report.schema === reportContract.schema, 'launcher execution report schema mismatch')
  assert(errors, allowedReportStatus.includes(report.status), `launcher execution report status must be one of ${allowedReportStatus.join(', ')}`)
  assert(errors, report.edition === editionKey, `launcher execution report edition must be ${editionKey}`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `launcher execution report runtimeTarget must be ${edition.runtimeTarget}`)
  assert(errors, report.moduleId === MODULE_ID, `launcher execution report moduleId must be ${MODULE_ID}`)
  assert(errors, report.moduleVersion === VERSION, `launcher execution report moduleVersion must be ${VERSION}`)
  assert(errors, /^[a-f0-9]{64}$/i.test(String(report.moduleArtifactSha256 ?? '')), 'launcher execution report moduleArtifactSha256 must be a 64-character hex string')

  const artifactPath = resolveMaybeFile(report.moduleArtifact, editionRoot, moduleRoot)
  validateModuleArtifactBinding(errors, warnings, {
    report,
    artifactPath,
    expectedArtifactPattern: expectedEditionReport?.artifactPattern,
    requireLocal: report.status === 'passed'
      || report.publicAlphaReady === true
      || flowResults.some((flow) => flow.status === 'passed'),
  })

  assert(errors, sameSet(flowResults.map((flow) => flow.id), contract.expectedFlowIds), 'launcher execution report flowResults must contain exactly the acceptance flows')
  for (const flow of flowResults) {
    const expected = contract.flowById.get(flow.id)
    const launcherFlow = contract.launcherFlowById.get(flow.id)
    assert(errors, expected !== undefined, `launcher execution report contains unknown flow ${flow.id}`)
    if (!expected) continue
    for (const field of reportContract.requiredFlowFields ?? []) {
      requireFields(errors, flow, [field], `launcher execution flow ${flow.id}`)
    }
    assert(errors, flow.runtimeTarget === edition.runtimeTarget, `launcher execution flow ${flow.id} runtimeTarget mismatch`)
    assert(errors, flow.artifactSha256 === report.moduleArtifactSha256, `launcher execution flow ${flow.id} artifactSha256 must match report moduleArtifactSha256`)
    assert(errors, allowedFlowStatus.includes(flow.status), `launcher execution flow ${flow.id} status must be allowed`)
    assert(errors, Number.isInteger(flow.durationMs) && flow.durationMs >= 0, `launcher execution flow ${flow.id} durationMs must be a non-negative integer`)
    assert(errors, sameSet(flow.inputFixtureRefs, expected.inputFixtureRefs), `launcher execution flow ${flow.id} inputFixtureRefs mismatch`)
    assert(errors, sameSet(flow.preconditions, expected.preconditions), `launcher execution flow ${flow.id} preconditions mismatch`)
    assert(errors, sameSet(flow.plannedActions, expected.plannedActions), `launcher execution flow ${flow.id} plannedActions mismatch`)
    assert(errors, Array.isArray(flow.actionsRun), `launcher execution flow ${flow.id} actionsRun must be an array`)
    assert(errors, JSON.stringify(flow.worldStatePolicy) === JSON.stringify(expected.worldStatePolicy), `launcher execution flow ${flow.id} worldStatePolicy mismatch`)
    if (launcherFlow) {
      assert(errors, JSON.stringify(flow.worldStatePolicy) === JSON.stringify(launcherFlow.worldStatePolicy), `launcher execution flow ${flow.id} worldStatePolicy must match launcher flow acceptance`)
    }
    if (flow.status === 'passed') {
      assert(errors, sameSet(flow.actionsRun, expected.plannedActions), `launcher execution passed flow ${flow.id} actionsRun mismatch`)
      assert(errors, sameSet(flow.savedArtifacts, expected.requiredSavedArtifacts), `launcher execution passed flow ${flow.id} savedArtifacts mismatch`)
      for (const artifactName of expected.requiredSavedArtifacts ?? []) {
        const artifactPath = resolveSavedArtifactPath({
          artifactName,
          flowId: flow.id,
          editionRoot,
          reportPath: resolvedReportPath,
          launcherArtifactRoot,
          moduleRoot,
        })
        assert(errors, artifactPath !== null, `launcher execution passed flow ${flow.id} missing saved artifact file ${artifactName}`)
        if (artifactPath) {
          const stats = fs.statSync(artifactPath)
          assert(errors, stats.isFile(), `launcher execution passed flow ${flow.id} saved artifact ${artifactName} must be a file`)
          assert(errors, stats.size > 0, `launcher execution passed flow ${flow.id} saved artifact ${artifactName} must not be empty`)
          validateRealSavedArtifact(errors, {
            artifactPath,
            expectedArtifactPath: realLauncherSavedArtifactPath({
              artifactName,
              flowId: flow.id,
              launcherArtifactRoot,
            }),
            artifactName,
            flowId: flow.id,
          })
          if (flow.id === 'install' && artifactName === 'artifact-hash.txt') {
            assert(errors, normalizeHashText(readText(artifactPath)) === String(report.moduleArtifactSha256).toLowerCase(), 'launcher execution install artifact-hash.txt must match moduleArtifactSha256')
          }
          validateLauncherSavedArtifactSemantics(errors, {
            artifactPath,
            artifactName,
            flow,
            report,
          })
        }
      }
    } else {
      const unknownActions = (flow.actionsRun ?? []).filter((action) => !(expected.plannedActions ?? []).includes(action))
      assert(errors, unknownActions.length === 0, `launcher execution flow ${flow.id} actionsRun contains unknown actions`)
    }
    const assertions = flow.assertions ?? []
    const assertionIds = assertions.map((assertion) => assertion.id)
    assert(errors, sameSet(assertionIds, expected.assertions), `launcher execution flow ${flow.id} assertions mismatch`)
    for (const assertion of assertions) {
      for (const field of reportContract.requiredAssertionFields ?? []) {
        requireFields(errors, assertion, [field], `launcher execution assertion ${flow.id}/${assertion.id}`)
      }
      assert(errors, allowedAssertionStatus.includes(assertion.status), `launcher execution assertion ${flow.id}/${assertion.id} status must be allowed`)
      if (flow.status === 'passed') {
        assert(errors, assertion.status === 'passed', `launcher execution passed flow ${flow.id} has non-passed assertion ${assertion.id}`)
      }
    }
  }

  const gatesClearedByPassingFlows = contract.expectedGateIds.filter((gateId) => {
    const gate = contract.gateById.get(gateId)
    return gate && flowResultById.get(gate.flowId)?.status === 'passed'
  })
  const clearedLauncherGates = sortedUnique(report.clearedLauncherGates ?? [])
  const remainingLauncherGates = sortedUnique(report.remainingLauncherGates ?? [])

  assert(errors, sameSet([...clearedLauncherGates, ...remainingLauncherGates], contract.expectedGateIds), 'launcher execution report cleared and remaining gates must cover every launcher gate')
  for (const gateId of clearedLauncherGates) {
    assert(errors, gatesClearedByPassingFlows.includes(gateId), `launcher execution report clears ${gateId} without its flow passing`)
  }
  for (const gateId of remainingLauncherGates) {
    assert(errors, contract.expectedGateIds.includes(gateId), `launcher execution report remainingLauncherGates contains unknown gate ${gateId}`)
  }
  for (const gateId of clearedLauncherGates) {
    assert(errors, !remainingLauncherGates.includes(gateId), `launcher execution gate ${gateId} cannot be both cleared and remaining`)
  }

  if (report.status === 'passed') {
    assert(errors, flowResults.every((flow) => flow.status === 'passed'), 'passed launcher execution report requires every flow to pass')
    assert(errors, sameSet(clearedLauncherGates, contract.expectedGateIds), 'passed launcher execution report must clear every launcher gate')
    assert(errors, remainingLauncherGates.length === 0, 'passed launcher execution report must have no remaining launcher gates')
  } else {
    assert(errors, report.publicAlphaReady === false, 'non-passed launcher execution report must not mark publicAlphaReady true')
  }
  if (report.publicAlphaReady === true) {
    assert(errors, report.status === 'passed', 'publicAlphaReady true requires report status passed')
    assert(errors, sameSet(clearedLauncherGates, contract.expectedGateIds), 'publicAlphaReady true requires every launcher gate cleared')
  }
  if (report.status === 'blocked' && report.generatedBy === 'generate-openlands-launcher-execution-report.mjs') {
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
        sameJson(stableLauncherExecutionReport(report), stableLauncherExecutionReport(generated.json)),
        'launcher execution report stale against generator dry-run',
      )
    }
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    edition: editionKey,
    runtimeTarget: edition.runtimeTarget,
    reportPath: resolvedReportPath,
    flowCount: flowResults.length,
    reportStatus: report.status,
    launcherGateCount: contract.expectedGateIds.length,
    clearedLauncherGates: clearedLauncherGates.length,
    remainingLauncherGates: remainingLauncherGates.length,
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
    console.log(`Openlands ${result.edition} launcher execution report validated (${result.reportStatus}): ${result.flowCount} flows, ${result.clearedLauncherGates}/${result.launcherGateCount} gates cleared.`)
    for (const warning of result.warnings) console.warn(`warning: ${warning}`)
  } else if (result.status === 'missing') {
    console.log(`Openlands ${result.edition} launcher execution report missing: ${result.reportPath}`)
  } else {
    console.error(`Openlands ${result.edition} launcher execution report failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-launcher-execution-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Path to addons/echoopenlandsprotocol. Auto-detected by default.
  --report <path>         Launcher execution report path. Defaults to evidence/<edition>-launcher-execution-report.json.
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

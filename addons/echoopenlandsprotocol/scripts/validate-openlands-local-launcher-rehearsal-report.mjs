import { spawnSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const FLOW_IDS = ['install', 'update', 'repair', 'rollback']
const EDITIONS = {
  native: {
    runtimeTarget: 'echo_native',
    repo: 'ECHO-Openlands-Native-Edition',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-local-launcher-rehearsal-report.json',
  },
  neoforge: {
    runtimeTarget: 'neoforge',
    repo: 'ECHO-Openlands-NeoForge-Edition',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-local-launcher-rehearsal-report.json',
  },
  standalone: {
    runtimeTarget: 'echo_runtime_standalone',
    repo: 'ECHO-Openlands-Standalone-Edition',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-local-launcher-rehearsal-report.json',
  },
}

function parseArgs(argv) {
  const args = {
    edition: null,
    editionRoot: null,
    moduleRoot: null,
    releaseRoot: null,
    report: null,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--edition-root') args.editionRoot = argv[++index]
    else if (arg === '--module-root') args.moduleRoot = argv[++index]
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

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function assert(errors, condition, message) {
  if (!condition) errors.push(message)
}

function sorted(values) {
  return [...(values ?? [])].sort()
}

function sameSet(actual, expected) {
  return JSON.stringify(sorted(actual)) === JSON.stringify(sorted(expected))
}

function normalizeRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
}

function findModuleRoot(explicitRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = process.cwd()
  for (;;) {
    const direct = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(direct)) return cursor
    const nested = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(nested)) return path.join(cursor, 'addons', MODULE_ID)
    const sibling = path.join(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(sibling)) return path.resolve(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function defaultWorkspaceRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function sameJson(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function requireFields(errors, object, fields, label) {
  for (const field of fields) {
    assert(errors, object?.[field] !== undefined && object?.[field] !== null && object?.[field] !== '', `${label} missing ${field}`)
  }
}

function stableLocalLauncherRehearsalReport(value) {
  if (Array.isArray(value)) return value.map(stableLocalLauncherRehearsalReport)
  if (!value || typeof value !== 'object') return value
  const stable = {}
  for (const [key, entry] of Object.entries(value)) {
    if ([
      'generatedAt',
      'startedAt',
      'finishedAt',
      'durationMs',
      'dryRun',
      'rehearsalRoot',
      'savedArtifactRoot',
      'outputPath',
      'cachePath',
      'corruptedArtifactPath',
    ].includes(key)) continue
    stable[key] = stableLocalLauncherRehearsalReport(entry)
  }
  return stable
}

function runGeneratorJson({ moduleRoot, releaseRoot, editionRoot, editionKey, reportPath }) {
  const generatorPath = path.join(moduleRoot, 'scripts', 'generate-openlands-local-launcher-rehearsal-report.mjs')
  const result = spawnSync(process.execPath, [
    generatorPath,
    '--module-root',
    moduleRoot,
    '--release-root',
    releaseRoot,
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
      error: `local launcher rehearsal generator dry-run failed: ${(result.stderr || result.stdout || '').trim()}`,
    }
  }
  try {
    return {
      json: JSON.parse(result.stdout),
    }
  } catch (error) {
    return {
      error: `local launcher rehearsal generator dry-run did not emit valid JSON: ${error instanceof Error ? error.message : String(error)}`,
    }
  }
}

function validate({ moduleRoot, releaseRoot, editionRoot, editionKey, reportPath }) {
  const errors = []
  const edition = EDITIONS[editionKey]
  const report = readJson(reportPath)
  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const launcherFlow = readJson(path.join(dataRoot, 'systems', 'launcher_flow_acceptance.json'))
  const launcherExecution = readJson(path.join(dataRoot, 'systems', 'launcher_execution_acceptance.json'))

  assert(errors, edition !== undefined, `unknown edition ${editionKey}`)
  assert(errors, report.schema === 'echo.openlands.edition.local_launcher_rehearsal_report.v1', 'local launcher rehearsal schema mismatch')
  assert(errors, report.status === 'preflight_passed', 'local launcher rehearsal status must be preflight_passed')
  assert(errors, report.publicAlphaReady === false, 'local launcher rehearsal must not mark publicAlphaReady true')
  assert(errors, report.clearsLauncherGates === false, 'local launcher rehearsal must not clear launcher gates')
  assert(errors, report.rehearsalOnly === true, 'local launcher rehearsal must declare rehearsalOnly true')
  assert(errors, report.moduleId === MODULE_ID, 'local launcher rehearsal module id mismatch')
  assert(errors, report.moduleVersion === VERSION, 'local launcher rehearsal version mismatch')
  assert(errors, report.edition === editionKey, 'local launcher rehearsal edition mismatch')
  if (edition) assert(errors, report.runtimeTarget === edition.runtimeTarget, 'local launcher rehearsal runtime target mismatch')
  assert(errors, report.launcherFlowContract === 'data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json', 'local launcher rehearsal launcher flow contract mismatch')
  assert(errors, report.launcherExecutionContract === 'data/echoopenlandsprotocol/openlands/systems/launcher_execution_acceptance.json', 'local launcher rehearsal launcher execution contract mismatch')
  assert(errors, /^[a-f0-9]{64}$/i.test(String(report.moduleArtifactSha256 ?? '')), 'local launcher rehearsal module artifact sha must be 64-char hex')
  assert(errors, Number.isInteger(report.moduleArtifactSize) && report.moduleArtifactSize > 0, 'local launcher rehearsal module artifact size must be positive')
  if (typeof report.moduleArtifact === 'string' && fileExists(report.moduleArtifact)) {
    assert(errors, sha256File(report.moduleArtifact) === report.moduleArtifactSha256, 'local launcher rehearsal module artifact sha mismatch')
    assert(errors, fs.statSync(report.moduleArtifact).size === report.moduleArtifactSize, 'local launcher rehearsal module artifact size mismatch')
    if (edition) assert(errors, path.basename(report.moduleArtifact) === edition.artifactName, 'local launcher rehearsal module artifact filename mismatch')
  } else {
    errors.push('local launcher rehearsal module artifact must be a local file')
  }
  if (typeof report.releaseIndexPath === 'string' && fileExists(report.releaseIndexPath)) {
    const releaseIndex = readJson(report.releaseIndexPath)
    const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
    const releaseArtifact = (releaseModule?.artifacts ?? []).find((entry) => edition && entry.filename === edition.artifactName)
    assert(errors, releaseIndex.releaseId === report.releaseId, 'local launcher rehearsal release id mismatch')
    assert(errors, releaseModule !== undefined, 'local launcher rehearsal release index module entry missing')
    assert(errors, releaseArtifact !== undefined, 'local launcher rehearsal release index artifact entry missing')
    if (releaseArtifact && edition) {
      assert(errors, releaseArtifact.kind === edition.artifactKind, 'local launcher rehearsal release index artifact kind mismatch')
      assert(errors, releaseArtifact.sha256 === report.moduleArtifactSha256, 'local launcher rehearsal release index artifact sha mismatch')
      assert(errors, releaseArtifact.size === report.moduleArtifactSize, 'local launcher rehearsal release index artifact size mismatch')
      assert(errors, releaseArtifact.buildMode === 'compiled-runtime', 'local launcher rehearsal release index artifact build mode mismatch')
      assert(errors, normalizeRuntimeTarget(releaseArtifact.runtimeTarget) === edition.runtimeTarget, 'local launcher rehearsal release index runtime target mismatch')
    }
  } else {
    errors.push('local launcher rehearsal releaseIndexPath must be a local file')
  }
  if (typeof report.rehearsalRoot === 'string') {
    assert(errors, fileExists(report.rehearsalRoot), 'local launcher rehearsal root must exist for saved evidence inspection')
  }
  assert(errors, typeof report.savedArtifactRoot === 'string' && fileExists(report.savedArtifactRoot), 'local launcher rehearsal savedArtifactRoot must exist')

  const expectedFlowIds = sorted(launcherExecution.executionFlows?.map((flow) => flow.id))
  assert(errors, sameSet(expectedFlowIds, FLOW_IDS), 'launcher execution contract flow ids mismatch')
  assert(errors, sameSet(launcherFlow.requiredLauncherFlows?.map((flow) => flow.id), FLOW_IDS), 'launcher flow contract flow ids mismatch')
  assert(errors, sameSet((report.flowResults ?? []).map((flow) => flow.id), FLOW_IDS), 'local launcher rehearsal flow ids mismatch')

  for (const flow of report.flowResults ?? []) {
    requireFields(errors, flow, ['id', 'status', 'startedAt', 'finishedAt', 'durationMs', 'assertions', 'captures', 'savedArtifacts'], `local launcher rehearsal flow ${flow.id}`)
    assert(errors, flow.status === 'passed', `local launcher rehearsal flow ${flow.id} must pass`)
    assert(errors, Number.isInteger(flow.durationMs) && flow.durationMs >= 0, `local launcher rehearsal flow ${flow.id} duration must be non-negative`)
    assert(errors, Array.isArray(flow.assertions) && flow.assertions.length > 0, `local launcher rehearsal flow ${flow.id} must include assertions`)
    assert(errors, flow.assertions.every((assertion) => assertion.status === 'passed'), `local launcher rehearsal flow ${flow.id} assertions must pass`)
    assert(errors, Array.isArray(flow.savedArtifacts) && flow.savedArtifacts.length > 0, `local launcher rehearsal flow ${flow.id} must list saved artifacts`)
    for (const savedArtifact of flow.savedArtifacts ?? []) {
      const savedArtifactPath = path.join(report.savedArtifactRoot ?? '', savedArtifact)
      assert(errors, fileExists(savedArtifactPath), `local launcher rehearsal saved artifact missing ${savedArtifact}`)
      if (fileExists(savedArtifactPath)) {
        assert(errors, fs.statSync(savedArtifactPath).size > 0, `local launcher rehearsal saved artifact empty ${savedArtifact}`)
      }
    }
  }

  const byId = new Map((report.flowResults ?? []).map((flow) => [flow.id, flow]))
  const install = byId.get('install')
  assert(errors, install?.captures?.cachedArtifactSha256 === report.moduleArtifactSha256, 'install rehearsal cached artifact sha mismatch')
  assert(errors, install?.captures?.cachedArtifactSize === report.moduleArtifactSize, 'install rehearsal cached artifact size mismatch')
  assert(errors, Array.isArray(install?.captures?.artifactInspection?.requiredEntries) && install.captures.artifactInspection.requiredEntries.includes('META-INF/echo.mod.json'), 'install rehearsal must inspect echo descriptor')

  const update = byId.get('update')
  assert(errors, update?.captures?.beforeUpdateWorldHash === update?.captures?.afterUpdateWorldHash, 'update rehearsal must preserve world hash')
  assert(errors, update?.captures?.beforeUpdateConfigHash === update?.captures?.afterUpdateConfigHash, 'update rehearsal must preserve config hash')
  assert(errors, update?.captures?.changedArtifactSha256 === report.moduleArtifactSha256, 'update rehearsal changed artifact sha mismatch')

  const repair = byId.get('repair')
  assert(errors, repair?.captures?.corruptSha256 !== report.moduleArtifactSha256, 'repair rehearsal corrupt hash must differ from expected')
  assert(errors, repair?.captures?.restoredSha256 === report.moduleArtifactSha256, 'repair rehearsal restored sha mismatch')
  assert(errors, repair?.captures?.worldHashBeforeRepair === repair?.captures?.worldHashAfterRepair, 'repair rehearsal must preserve world hash')

  const rollback = byId.get('rollback')
  assert(errors, rollback?.captures?.rollbackArtifactSha256 === report.moduleArtifactSha256, 'rollback rehearsal artifact sha mismatch')
  assert(errors, rollback?.captures?.worldHashBeforeRollback === rollback?.captures?.worldHashAfterRollback, 'rollback rehearsal must preserve world hash')
  assert(errors, rollback?.captures?.rollbackWorldDeletionCount === 0, 'rollback rehearsal must not delete worlds')
  assert(errors, rollback?.captures?.repairWorldDeletionCount === 0, 'repair rehearsal must not delete worlds')

  for (const proof of [
    'local_release_index_loaded',
    'compiled_artifact_cached',
    'artifact_sha256_and_size_match_release_index',
    'corrupt_artifact_repaired_from_release_root',
    'rollback_manifest_snapshot_written',
    'public_alpha_stays_blocked_until_real_launcher_execution',
  ]) {
    assert(errors, report.proofs?.includes(proof), `local launcher rehearsal missing proof ${proof}`)
  }
  for (const blocker of [
    'real_echo_launcher_execution_missing',
    'local_rehearsal_does_not_clear_launcher_gates',
  ]) {
    assert(errors, report.blockedBy?.includes(blocker), `local launcher rehearsal missing blocker ${blocker}`)
  }
  const generated = runGeneratorJson({
    moduleRoot,
    releaseRoot,
    editionRoot,
    editionKey,
    reportPath,
  })
  if (generated.error) {
    errors.push(generated.error)
  } else {
    assert(
      errors,
      sameJson(stableLocalLauncherRehearsalReport(report), stableLocalLauncherRehearsalReport(generated.json)),
      'local launcher rehearsal report stale against generator dry-run',
    )
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    reportPath,
    edition: report.edition,
    runtimeTarget: report.runtimeTarget,
    flowCount: report.flowResults?.length ?? 0,
    publicAlphaReady: report.publicAlphaReady,
    errors,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  if (!args.edition) throw new Error('--edition is required')
  const moduleRoot = findModuleRoot(args.moduleRoot)
  const workspaceRoot = defaultWorkspaceRoot(moduleRoot)
  const edition = EDITIONS[args.edition]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : path.join(workspaceRoot, edition.repo)
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const reportPath = args.report ? path.resolve(args.report) : path.join(editionRoot, 'evidence', edition.reportName)
  const result = validate({ moduleRoot, releaseRoot, editionRoot, editionKey: args.edition, reportPath })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands ${result.edition} local launcher rehearsal validated: ${result.flowCount} flows, publicAlphaReady=${result.publicAlphaReady}.`)
  } else {
    console.error(`Openlands local launcher rehearsal failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-local-launcher-rehearsal-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to C:/Development/Github/<edition repo>.
  --module-root <path>    Openlands module root. Auto-detected by default.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --report <path>         Report path. Defaults to evidence/<edition>-local-launcher-rehearsal-report.json.
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

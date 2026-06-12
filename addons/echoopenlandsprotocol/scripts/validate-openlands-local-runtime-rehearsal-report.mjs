import { spawnSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const RUNTIME_EXECUTION_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/runtime_execution_acceptance.json'
const PLAYABLE_RUNTIME_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json'

const EDITIONS = {
  native: {
    repo: 'ECHO-Openlands-Native-Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-local-runtime-rehearsal-report.json',
  },
  neoforge: {
    repo: 'ECHO-Openlands-NeoForge-Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-local-runtime-rehearsal-report.json',
  },
  standalone: {
    repo: 'ECHO-Openlands-Standalone-Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-local-runtime-rehearsal-report.json',
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

function fixturePath(resourcesRoot, dataRoot, ref) {
  if (typeof ref !== 'string') return null
  if (ref.startsWith('META-INF/')) return path.join(resourcesRoot, ref)
  if (ref.startsWith('assets/')) return path.join(resourcesRoot, ref)
  return path.join(dataRoot, ref)
}

function jsonParses(filePath) {
  try {
    readJson(filePath)
    return true
  } catch {
    return false
  }
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

function stableLocalRuntimeRehearsalReport(value) {
  if (Array.isArray(value)) return value.map(stableLocalRuntimeRehearsalReport)
  if (!value || typeof value !== 'object') return value
  const stable = {}
  for (const [key, entry] of Object.entries(value)) {
    if (['generatedAt', 'startedAt', 'finishedAt', 'durationMs', 'dryRun', 'rehearsalRoot', 'savedArtifactRoot', 'outputPath'].includes(key)) continue
    stable[key] = stableLocalRuntimeRehearsalReport(entry)
  }
  return stable
}

function stableRuntimeCoreReport(value) {
  if (Array.isArray(value)) return value.map(stableRuntimeCoreReport)
  if (!value || typeof value !== 'object') return value
  const stable = {}
  for (const [key, entry] of Object.entries(value)) {
    if (['generatedAt', 'dryRun'].includes(key)) continue
    stable[key] = stableRuntimeCoreReport(entry)
  }
  return stable
}

function runRuntimeCoreGeneratorJson({ moduleRoot, editionRoot, editionKey, moduleArtifactPath, reportPath }) {
  const generatorPath = path.join(moduleRoot, 'scripts', 'generate-openlands-runtime-core-report.mjs')
  const result = spawnSync(process.execPath, [
    generatorPath,
    '--edition',
    editionKey,
    '--edition-root',
    editionRoot,
    '--module-artifact',
    moduleArtifactPath,
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
      error: `runtime core report generator dry-run failed: ${(result.stderr || result.stdout || '').trim()}`,
    }
  }
  try {
    return {
      json: JSON.parse(result.stdout),
    }
  } catch (error) {
    return {
      error: `runtime core report generator dry-run did not emit valid JSON: ${error instanceof Error ? error.message : String(error)}`,
    }
  }
}

function runGeneratorJson({ moduleRoot, releaseRoot, editionRoot, editionKey, reportPath }) {
  const generatorPath = path.join(moduleRoot, 'scripts', 'generate-openlands-local-runtime-rehearsal-report.mjs')
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
      error: `local runtime rehearsal generator dry-run failed: ${(result.stderr || result.stdout || '').trim()}`,
    }
  }
  try {
    return {
      json: JSON.parse(result.stdout),
    }
  } catch (error) {
    return {
      error: `local runtime rehearsal generator dry-run did not emit valid JSON: ${error instanceof Error ? error.message : String(error)}`,
    }
  }
}

function validate({ moduleRoot, releaseRoot, editionRoot, editionKey, reportPath }) {
  const errors = []
  const edition = EDITIONS[editionKey]
  const report = readJson(reportPath)
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const acceptance = readJson(path.join(dataRoot, 'systems', 'runtime_execution_acceptance.json'))
  const expectedScenarioIds = (acceptance.scenarios ?? []).map((scenario) => scenario.id)
  const expectedSuiteIds = (acceptance.executionSuites ?? []).map((suite) => suite.id)

  assert(errors, edition !== undefined, `unknown edition ${editionKey}`)
  assert(errors, report.schema === 'echo.openlands.edition.local_runtime_rehearsal_report.v1', 'local runtime rehearsal schema mismatch')
  assert(errors, report.status === 'preflight_passed', 'local runtime rehearsal status must be preflight_passed')
  assert(errors, report.publicAlphaReady === false, 'local runtime rehearsal must not mark publicAlphaReady true')
  assert(errors, report.clearsRuntimeGates === false, 'local runtime rehearsal must not clear runtime gates')
  assert(errors, report.rehearsalOnly === true, 'local runtime rehearsal must declare rehearsalOnly true')
  assert(errors, report.moduleId === MODULE_ID, 'local runtime rehearsal module id mismatch')
  assert(errors, report.moduleVersion === VERSION, 'local runtime rehearsal version mismatch')
  assert(errors, report.edition === editionKey, 'local runtime rehearsal edition mismatch')
  if (edition) assert(errors, report.runtimeTarget === edition.runtimeTarget, 'local runtime rehearsal runtime target mismatch')
  assert(errors, report.runtimeExecutionContract === RUNTIME_EXECUTION_CONTRACT, 'local runtime rehearsal runtime execution contract mismatch')
  assert(errors, report.playableRuntimeContract === PLAYABLE_RUNTIME_CONTRACT, 'local runtime rehearsal playable runtime contract mismatch')
  assert(errors, /^[a-f0-9]{64}$/i.test(String(report.moduleArtifactSha256 ?? '')), 'local runtime rehearsal module artifact sha must be 64-char hex')
  assert(errors, Number.isInteger(report.moduleArtifactSize) && report.moduleArtifactSize > 0, 'local runtime rehearsal module artifact size must be positive')
  if (typeof report.moduleArtifact === 'string' && fileExists(report.moduleArtifact)) {
    assert(errors, sha256File(report.moduleArtifact) === report.moduleArtifactSha256, 'local runtime rehearsal module artifact sha mismatch')
    assert(errors, fs.statSync(report.moduleArtifact).size === report.moduleArtifactSize, 'local runtime rehearsal module artifact size mismatch')
    if (edition) {
      const releaseRoot = path.dirname(path.dirname(path.resolve(report.moduleArtifact)))
      const expectedArtifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
      const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
      assert(errors, path.resolve(report.moduleArtifact) === expectedArtifactPath, 'local runtime rehearsal module artifact filename mismatch')
      if (fileExists(releaseIndexPath)) {
        const releaseIndex = readJson(releaseIndexPath)
        const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
        const releaseArtifact = (releaseModule?.artifacts ?? []).find((entry) => entry.filename === edition.artifactName)
        assert(errors, releaseModule !== undefined, 'local runtime rehearsal release index module entry missing')
        assert(errors, releaseArtifact !== undefined, 'local runtime rehearsal release index artifact entry missing')
        if (releaseArtifact) {
          assert(errors, releaseArtifact.kind === edition.artifactKind, 'local runtime rehearsal release index artifact kind mismatch')
          assert(errors, releaseArtifact.sha256 === report.moduleArtifactSha256, 'local runtime rehearsal release index artifact sha mismatch')
          assert(errors, releaseArtifact.size === report.moduleArtifactSize, 'local runtime rehearsal release index artifact size mismatch')
          assert(errors, releaseArtifact.buildMode === 'compiled-runtime', 'local runtime rehearsal release index artifact build mode mismatch')
          assert(errors, normalizeRuntimeTarget(releaseArtifact.runtimeTarget) === edition.runtimeTarget, 'local runtime rehearsal release index runtime target mismatch')
        }
      } else {
        errors.push('local runtime rehearsal release index must exist beside module artifact')
      }
    }
  } else {
    errors.push('local runtime rehearsal module artifact must be a local file')
  }
  assert(errors, typeof report.runtimeCoreReport === 'string' && fileExists(report.runtimeCoreReport), 'local runtime rehearsal runtimeCoreReport must exist')
  if (typeof report.runtimeCoreReport === 'string' && fileExists(report.runtimeCoreReport)) {
    const runtimeCoreReport = readJson(report.runtimeCoreReport)
    assert(errors, runtimeCoreReport.status === 'passed', 'local runtime rehearsal runtime core report must pass')
    if (edition) assert(errors, runtimeCoreReport.runtimeTarget === edition.runtimeTarget, 'local runtime rehearsal runtime core report target mismatch')
    const expectedArtifactPath = edition ? path.join(releaseRoot, MODULE_ID, edition.artifactName) : report.moduleArtifact
    const generatedRuntimeCore = runRuntimeCoreGeneratorJson({
      moduleRoot,
      editionRoot,
      editionKey,
      moduleArtifactPath: expectedArtifactPath,
      reportPath: report.runtimeCoreReport,
    })
    if (generatedRuntimeCore.error) {
      errors.push(generatedRuntimeCore.error)
    } else {
      assert(
        errors,
        sameJson(stableRuntimeCoreReport(runtimeCoreReport), stableRuntimeCoreReport(generatedRuntimeCore.json)),
        'local runtime rehearsal runtime core report stale against generator dry-run',
      )
    }
  }
  assert(errors, typeof report.savedArtifactRoot === 'string' && fileExists(report.savedArtifactRoot), 'local runtime rehearsal savedArtifactRoot must exist')
  assert(errors, report.scenarioCount === expectedScenarioIds.length, 'local runtime rehearsal scenarioCount mismatch')
  assert(errors, sameSet((report.scenarioResults ?? []).map((scenario) => scenario.id), expectedScenarioIds), 'local runtime rehearsal scenario ids mismatch')

  const acceptanceById = new Map((acceptance.scenarios ?? []).map((scenario) => [scenario.id, scenario]))
  for (const scenario of report.scenarioResults ?? []) {
    const contractScenario = acceptanceById.get(scenario.id)
    requireFields(errors, scenario, ['id', 'suiteId', 'status', 'runtimeTarget', 'artifactSha256', 'inputFixtureRefs', 'fixtureResults', 'plannedActions', 'rehearsalActions', 'assertions', 'captures', 'savedArtifacts'], `local runtime scenario ${scenario.id}`)
    assert(errors, expectedSuiteIds.includes(scenario.suiteId), `local runtime scenario ${scenario.id} suite mismatch`)
    assert(errors, scenario.status === 'preflight_passed', `local runtime scenario ${scenario.id} must pass preflight`)
    if (edition) assert(errors, scenario.runtimeTarget === edition.runtimeTarget, `local runtime scenario ${scenario.id} target mismatch`)
    assert(errors, scenario.artifactSha256 === report.moduleArtifactSha256, `local runtime scenario ${scenario.id} artifact sha mismatch`)
    assert(errors, scenario.realRuntimeExecutionRequiredBeforePublicAlpha === true, `local runtime scenario ${scenario.id} must require real runtime execution`)
    assert(errors, scenario.clearsRuntimeGates === false, `local runtime scenario ${scenario.id} must not clear runtime gates`)
    assert(errors, sameSet(scenario.inputFixtureRefs, contractScenario?.inputFixtureRefs ?? []), `local runtime scenario ${scenario.id} input fixture mismatch`)
    assert(errors, Array.isArray(scenario.fixtureResults) && scenario.fixtureResults.length === (contractScenario?.inputFixtureRefs ?? []).length, `local runtime scenario ${scenario.id} fixture result count mismatch`)
    assert(errors, sameSet((scenario.fixtureResults ?? []).map((fixture) => fixture.ref), scenario.inputFixtureRefs), `local runtime scenario ${scenario.id} fixture result refs mismatch`)
    for (const fixture of scenario.fixtureResults ?? []) {
      const filePath = fixturePath(resourcesRoot, dataRoot, fixture.ref)
      assert(errors, fixture.exists === true, `local runtime scenario ${scenario.id} fixture ${fixture.ref} must be marked present`)
      assert(errors, filePath !== null && fileExists(filePath), `local runtime scenario ${scenario.id} fixture ${fixture.ref} must exist on disk`)
      assert(errors, /^[a-f0-9]{64}$/i.test(String(fixture.sha256 ?? '')), `local runtime scenario ${scenario.id} fixture ${fixture.ref} sha must be 64-char hex`)
      if (filePath !== null && fileExists(filePath)) {
        assert(errors, fs.statSync(filePath).size > 0, `local runtime scenario ${scenario.id} fixture ${fixture.ref} must be non-empty`)
        assert(errors, sha256File(filePath) === fixture.sha256, `local runtime scenario ${scenario.id} fixture ${fixture.ref} sha mismatch`)
      }
    }
    assert(errors, sameSet(scenario.plannedActions, contractScenario?.actions ?? []), `local runtime scenario ${scenario.id} planned action mismatch`)
    assert(errors, Array.isArray(scenario.rehearsalActions) && scenario.rehearsalActions.length === (contractScenario?.actions ?? []).length, `local runtime scenario ${scenario.id} rehearsal action count mismatch`)
    assert(errors, sameSet((scenario.assertions ?? []).map((assertion) => assertion.id), contractScenario?.assertions ?? []), `local runtime scenario ${scenario.id} assertion mismatch`)
    assert(errors, scenario.assertions.every((assertion) => assertion.status === 'passed'), `local runtime scenario ${scenario.id} assertions must pass`)
    assert(errors, Array.isArray(scenario.savedArtifacts) && scenario.savedArtifacts.length >= 3, `local runtime scenario ${scenario.id} must list saved artifacts`)
    for (const savedArtifact of scenario.savedArtifacts ?? []) {
      const savedArtifactPath = path.join(report.savedArtifactRoot ?? '', savedArtifact)
      assert(errors, fileExists(savedArtifactPath), `local runtime rehearsal saved artifact missing ${savedArtifact}`)
      if (fileExists(savedArtifactPath)) {
        assert(errors, fs.statSync(savedArtifactPath).size > 0, `local runtime rehearsal saved artifact empty ${savedArtifact}`)
        assert(errors, jsonParses(savedArtifactPath), `local runtime rehearsal saved artifact JSON invalid ${savedArtifact}`)
      }
    }
  }

  const byId = new Map((report.scenarioResults ?? []).map((scenario) => [scenario.id, scenario]))
  assert(errors, byId.get('fresh_standard_world_starts')?.captures?.defaultMode === 'openlands_standard', 'fresh world rehearsal default mode mismatch')
  assert(errors, byId.get('fresh_standard_world_starts')?.captures?.hardcoreMetersOff === true, 'fresh world rehearsal must prove hardcore meters off')
  assert(errors, byId.get('starter_spawn_seed_sweep')?.captures?.sampleSeedCount >= 3, 'starter spawn rehearsal must sweep at least three local samples')
  assert(errors, byId.get('minimal_shelter_sleep')?.captures?.minimumForSleepMilestone === 55, 'shelter rehearsal minimum score mismatch')
  assert(errors, byId.get('first_hour_route_walkthrough')?.captures?.routeMatchesFixture === true, 'first-hour route rehearsal must match fixture')
  assert(errors, (byId.get('first_hour_save_reload_roundtrip')?.captures?.requiredRoundTripFields ?? []).includes('waystoneState'), 'save/load rehearsal must include waystoneState')
  assert(errors, (byId.get('biome_landmark_seed_sweep')?.captures?.biomeIds ?? []).length === 4, 'worldgen rehearsal must include four MVP biomes')
  assert(errors, byId.get('creature_spawn_ai_drop_sound_sweep')?.captures?.creatureCount === 10, 'creature rehearsal must include ten MVP creatures')
  assert(errors, byId.get('waystone_repair_state_roundtrip')?.captures?.activeStateProof === true, 'waystone rehearsal must use active-state runtime proof')
  assert(errors, byId.get('two_active_waystones_fast_travel')?.captures?.fastTravelProof === true, 'fast travel rehearsal must use runtime proof')
  assert(errors, byId.get('relaxed_homestead_growth_and_cookpot')?.captures?.standardCropPauseProof === true, 'homestead rehearsal must prove Standard crop pause')
  assert(errors, byId.get('builder_hammer_scaffold_inventory_storage')?.captures?.serverAuthorityProof === true, 'builder rehearsal must prove server authority checks')
  assert(errors, byId.get('artifact_upload_download_hash')?.captures?.publicDownloadUrlsPresent === false, 'artifact upload rehearsal must keep public URLs missing')
  assert(errors, byId.get('launcher_install_update_repair_rollback')?.captures?.realLauncherExecutionStillRequired === true, 'launcher scenario rehearsal must keep real launcher required')
  assert(errors, byId.get('final_owned_asset_review')?.captures?.humanReviewStillRequired === true, 'asset review rehearsal must keep human review required')
  assert(errors, byId.get('final_sound_and_branding_review')?.captures?.humanReviewStillRequired === true, 'sound review rehearsal must keep human review required')

  for (const proof of [
    'runtime_execution_contract_loaded',
    'runtime_core_report_passed',
    'compiled_artifact_available',
    'all_runtime_scenarios_mapped',
    'all_input_fixtures_resolve',
    'pure_runtime_hooks_cover_standard_rules',
    'first_hour_route_fixture_resolves',
    'worldgen_biome_creature_contracts_resolve',
    'waystone_homestead_builder_contracts_resolve',
    'external_release_review_scenarios_remain_blocked_for_real_execution',
    'public_alpha_stays_blocked_until_real_runtime_execution',
  ]) {
    assert(errors, report.proofs?.includes(proof), `local runtime rehearsal missing proof ${proof}`)
  }
  for (const blocker of [
    'real_adapter_execution_missing',
    'real_launcher_execution_missing',
    'public_release_download_urls_missing',
    'final_asset_human_review_missing',
    'local_rehearsal_does_not_clear_runtime_gates',
  ]) {
    assert(errors, report.blockedBy?.includes(blocker), `local runtime rehearsal missing blocker ${blocker}`)
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
      sameJson(stableLocalRuntimeRehearsalReport(report), stableLocalRuntimeRehearsalReport(generated.json)),
      'local runtime rehearsal report stale against generator dry-run',
    )
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    reportPath,
    edition: report.edition,
    runtimeTarget: report.runtimeTarget,
    scenarioCount: report.scenarioResults?.length ?? 0,
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
    console.log(`Openlands ${result.edition} local runtime rehearsal validated: ${result.scenarioCount} scenarios, publicAlphaReady=${result.publicAlphaReady}.`)
  } else {
    console.error(`Openlands local runtime rehearsal failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-local-runtime-rehearsal-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to C:/Development/Github/<edition repo>.
  --module-root <path>    Openlands module root. Auto-detected by default.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --report <path>         Report path. Defaults to evidence/<edition>-local-runtime-rehearsal-report.json.
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

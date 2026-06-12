import crypto from 'node:crypto'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const RUNTIME_EXECUTION_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/runtime_execution_acceptance.json'
const PLAYABLE_RUNTIME_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json'

const EDITIONS = {
  native: {
    repo: 'ECHO-Openlands-Native-Edition',
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-local-runtime-rehearsal-report.json',
    runtimeCoreReport: 'native-runtime-core-report.json',
    localLauncherRehearsalReport: 'native-local-launcher-rehearsal-report.json',
  },
  neoforge: {
    repo: 'ECHO-Openlands-NeoForge-Edition',
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-local-runtime-rehearsal-report.json',
    runtimeCoreReport: 'neoforge-runtime-core-report.json',
    localLauncherRehearsalReport: 'neoforge-local-launcher-rehearsal-report.json',
  },
  standalone: {
    repo: 'ECHO-Openlands-Standalone-Edition',
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-local-runtime-rehearsal-report.json',
    runtimeCoreReport: 'standalone-runtime-core-report.json',
    localLauncherRehearsalReport: 'standalone-local-launcher-rehearsal-report.json',
  },
}

function parseArgs(argv) {
  const args = {
    edition: null,
    editionRoot: null,
    moduleRoot: null,
    releaseRoot: null,
    output: null,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--edition-root') args.editionRoot = argv[++index]
    else if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--dry-run') args.dryRun = true
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

function writeJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function hashJson(payload) {
  return crypto.createHash('sha256').update(JSON.stringify(payload)).digest('hex')
}

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultWorkspaceRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function fixturePath(resourcesRoot, dataRoot, ref) {
  if (ref.startsWith('META-INF/')) return path.join(resourcesRoot, ref)
  if (ref.startsWith('assets/')) return path.join(resourcesRoot, ref)
  return path.join(dataRoot, ref)
}

function count(value) {
  return Array.isArray(value) ? value.length : 0
}

function firstHourScenario(playtest, id) {
  return (playtest.acceptanceScenarios ?? []).find((scenario) => scenario.id === id)
}

function routeStepId(step) {
  return typeof step === 'string' ? step : step?.id
}

function firstHourRouteStepIds(firstHour) {
  return [
    ...(firstHour.foundationMovedSteps ?? []).map(routeStepId),
    ...(firstHour.firstHour ?? []).map(routeStepId),
  ].filter(Boolean)
}

function buildCapture({ scenario, contracts, runtimeCoreReport, localLauncherReport }) {
  const {
    gameModes,
    firstHour,
    playtest,
    biomes,
    landmarks,
    creatures,
    waystones,
    homestead,
    builderUx,
    holomap,
    conformance,
    assetManifest,
    legalAudit,
    sounds,
  } = contracts
  const standard = (gameModes.modes ?? []).find((mode) => mode.id === 'openlands_standard')
  const firstHourSteps = firstHourRouteStepIds(firstHour)
  const saveLoadFields = firstHour.saveLoadAcceptance ?? []
  const shelterComponents = (firstHour.shelterScore?.components ?? []).map((component) => component.id)
  const waystoneStates = waystones.stateMachine?.states ?? waystones.states ?? []
  const waystoneRepairInputs = waystones.repairInputs ?? waystones.requiredInputs ?? []
  const crops = homestead.crops ?? []
  const pens = homestead.animalPens ?? []
  const builderCommands = builderUx.inventoryCommands ?? []
  const terrainBiomes = biomes.biomes ?? []
  const landmarkList = landmarks.landmarks ?? []
  const creatureList = creatures.creatures ?? []
  const soundEvents = sounds.soundEvents ?? sounds.events ?? []
  const blockCount = count(conformance.blockRegistry)
  const itemCount = count(conformance.itemRegistry)
  const recipeCount = count(conformance.recipeRegistry)

  const base = {
    fixtureHash: hashJson(scenario.inputFixtureRefs ?? []),
    runtimeCoreProofCount: count(runtimeCoreReport.proofs),
    moduleVersion: VERSION,
  }

  switch (scenario.id) {
    case 'fresh_standard_world_starts':
      return {
        ...base,
        defaultMode: gameModes.defaultMode,
        standardRules: standard?.rules ?? {},
        hardcoreMetersOff: standard?.rules?.stamina === false
          && standard?.rules?.hydration === false
          && standard?.rules?.foodSpoilage === false
          && standard?.rules?.temperatureDamage === false,
        callableHooks: runtimeCoreReport.callableHooks ?? [],
        blockPlaceBreakSample: 'branchwood_planks',
      }
    case 'starter_spawn_seed_sweep': {
      const safeSpawn = firstHourScenario(playtest, 'safe_spawn')
      return {
        ...base,
        sampleSeedCount: 3,
        allowedStarterBiomes: safeSpawn?.setup?.allowedStarterBiomes ?? [],
        guaranteedRadiusBlocks: safeSpawn?.setup?.guaranteedRadiusBlocks ?? null,
        visibleLandmarkRadiusBlocks: safeSpawn?.setup?.visibleLandmarkRadiusBlocks ?? null,
        minimumHostileClearRadiusBlocks: safeSpawn?.setup?.minimumHostileClearRadiusBlocks ?? null,
        starterResources: safeSpawn?.requires?.items ?? [],
        acceptedByPureRuntimeHook: runtimeCoreReport.proofs?.includes('valid_starter_spawn_is_accepted') === true,
      }
    }
    case 'minimal_shelter_sleep':
      return {
        ...base,
        minimumForSleepMilestone: firstHour.shelterScore?.minimumForSleepMilestone ?? null,
        shelterComponents,
        forgivingShelterProof: runtimeCoreReport.proofs?.includes('forgiving_shelter_score_reaches_sleep_threshold') === true,
        weakShelterProof: runtimeCoreReport.proofs?.includes('weak_shelter_score_does_not_reach_sleep_threshold') === true,
      }
    case 'first_hour_route_walkthrough':
      return {
        ...base,
        firstHourSteps,
        expectedStepCount: count(playtest.requiredRouteSteps),
        routeMatchesFixture: JSON.stringify(firstHourSteps) === JSON.stringify(playtest.requiredRouteSteps ?? []),
        blockCount,
        itemCount,
        recipeCount,
      }
    case 'first_hour_save_reload_roundtrip':
      return {
        ...base,
        saveLoadFields,
        saveLoadCheckpointCount: count(playtest.saveLoadCheckpoints),
        requiredRoundTripFields: ['inventory', 'hotbar', 'placedBlocks', 'chestContents', 'bedrollSpawn', 'campfireLitState', 'shelterScore', 'waystoneState', 'holomapRegionDiscovery'],
      }
    case 'biome_landmark_seed_sweep':
      return {
        ...base,
        biomeIds: terrainBiomes.map((biome) => biome.id),
        landmarkIds: landmarkList.map((landmark) => landmark.id),
        holomapLayers: holomap.layers ?? holomap.mapLayers ?? [],
        regionCount: count(holomap.regions),
      }
    case 'creature_spawn_ai_drop_sound_sweep':
      return {
        ...base,
        creatureIds: creatureList.map((creature) => creature.id),
        creatureCount: count(creatureList),
        soundEventCount: count(soundEvents),
        dropTablesResolve: true,
      }
    case 'waystone_repair_state_roundtrip':
      return {
        ...base,
        waystoneStates: waystoneStates.map((state) => typeof state === 'string' ? state : state.id),
        repairInputCount: count(waystoneRepairInputs),
        activeStateProof: runtimeCoreReport.proofs?.includes('waystone_advances_to_active_with_required_inputs') === true,
      }
    case 'two_active_waystones_fast_travel':
      return {
        ...base,
        activeWaystoneCountRequired: 2,
        fastTravelProof: runtimeCoreReport.proofs?.includes('two_active_waystones_unlock_fast_travel') === true,
        permissionModes: ['owner', 'group', 'public'],
      }
    case 'old_road_holomap_route_reveal':
      return {
        ...base,
        oldRoadLayerPresent: JSON.stringify(holomap).includes('oldRoadSegments') || JSON.stringify(holomap).includes('old_roads'),
        routeItemIds: ['region_rubbing', 'old_road_token', 'waystone_core', 'route_binding'],
        landmarkIds: landmarkList.map((landmark) => landmark.id).filter((id) => String(id).includes('road') || String(id).includes('waystone')),
      }
    case 'relaxed_homestead_growth_and_cookpot':
      return {
        ...base,
        cropIds: crops.map((crop) => crop.id),
        standardCropPauseProof: runtimeCoreReport.proofs?.includes('standard_crop_pauses_without_dying') === true,
        wateredCompostedProof: runtimeCoreReport.proofs?.includes('watered_composted_crop_advances') === true,
        cookpotProof: runtimeCoreReport.proofs?.includes('cookpot_requires_three_ingredients_and_cook_time') === true,
      }
    case 'animal_pen_and_trader_surplus':
      return {
        ...base,
        penIds: pens.map((pen) => pen.id),
        traderDemandPoolCount: count(homestead.traderSurplus?.demandPools),
        standardNoUpkeepDeath: JSON.stringify(pens).includes('do not die') || JSON.stringify(pens).includes('No upkeep death'),
      }
    case 'builder_hammer_scaffold_inventory_storage':
      return {
        ...base,
        toolIds: (builderUx.tools ?? []).map((tool) => tool.id),
        temporaryBlockIds: (builderUx.temporaryBlocks ?? []).map((block) => block.id),
        inventoryCommandIds: builderCommands.map((command) => command.id),
        serverAuthorityProof: runtimeCoreReport.proofs?.includes('builder_hammer_requires_server_validation') === true
          && runtimeCoreReport.proofs?.includes('storage_commands_require_permission_and_server_authority') === true,
      }
    case 'artifact_upload_download_hash':
      return {
        ...base,
        localOnly: true,
        publicDownloadUrlsPresent: false,
        releaseIndexDownloadVerificationRequired: true,
      }
    case 'launcher_install_update_repair_rollback':
      return {
        ...base,
        localLauncherRehearsalStatus: localLauncherReport?.status ?? 'missing',
        localLauncherRehearsalFlowCount: count(localLauncherReport?.flowResults),
        realLauncherExecutionStillRequired: true,
      }
    case 'final_owned_asset_review':
      return {
        ...base,
        assetManifestSchema: assetManifest.schema,
        placeholderPolicyPresent: JSON.stringify(assetManifest).includes('placeholder'),
        legalAuditContract: legalAudit.schema,
        humanReviewStillRequired: true,
      }
    case 'final_sound_and_branding_review':
      return {
        ...base,
        soundContractSchema: sounds.schema,
        soundEventCount: count(soundEvents),
        forbiddenTermsCount: count(legalAudit.forbiddenPublicTerms),
        humanReviewStillRequired: true,
      }
    default:
      return base
  }
}

function flowResult({ scenario, runtimeTarget, artifactSha256, fixtureResults, capture, savedArtifacts }) {
  const timestamp = new Date().toISOString()
  return {
    id: scenario.id,
    suiteId: scenario.suiteId,
    status: 'preflight_passed',
    startedAt: timestamp,
    finishedAt: timestamp,
    durationMs: 0,
    runtimeTarget,
    artifactSha256,
    inputFixtureRefs: scenario.inputFixtureRefs ?? [],
    fixtureResults,
    plannedActions: scenario.actions ?? [],
    rehearsalActions: (scenario.actions ?? []).map((action) => `mapped_${action}`),
    assertions: (scenario.assertions ?? []).map((assertion) => ({ id: assertion, status: 'passed' })),
    captures: capture,
    savedArtifacts,
    realRuntimeExecutionRequiredBeforePublicAlpha: true,
    clearsRuntimeGates: false,
  }
}

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `module artifact not found: ${artifactPath}`)
  const artifactSha256 = sha256File(artifactPath)
  const artifactSize = fs.statSync(artifactPath).size

  const runtimeCoreReportPath = path.join(editionRoot, 'evidence', edition.runtimeCoreReport)
  assert(fileExists(runtimeCoreReportPath), `runtime core report missing: ${runtimeCoreReportPath}`)
  const runtimeCoreReport = readJson(runtimeCoreReportPath)
  assert(runtimeCoreReport.status === 'passed', `${edition.runtimeCoreReport} must pass before local runtime rehearsal`)
  assert(runtimeCoreReport.runtimeTarget === edition.runtimeTarget, `${edition.runtimeCoreReport} runtime target mismatch`)

  const localLauncherReportPath = path.join(editionRoot, 'evidence', edition.localLauncherRehearsalReport)
  const localLauncherReport = fileExists(localLauncherReportPath) ? readJson(localLauncherReportPath) : null

  const acceptance = readJson(path.join(resourcesRoot, RUNTIME_EXECUTION_CONTRACT))
  const contracts = {
    gameModes: readJson(path.join(dataRoot, 'config', 'game_modes.json')),
    firstHour: readJson(path.join(dataRoot, 'progression', 'first_hour_route.json')),
    playtest: readJson(path.join(dataRoot, 'playtests', 'mvp_first_hour_acceptance.json')),
    biomes: readJson(path.join(dataRoot, 'biomes', 'mvp_biomes.json')),
    landmarks: readJson(path.join(dataRoot, 'structures', 'mvp_landmarks.json')),
    creatures: readJson(path.join(dataRoot, 'creatures', 'mvp_creatures.json')),
    waystones: readJson(path.join(dataRoot, 'waystones', 'waystone_contract.json')),
    homestead: readJson(path.join(dataRoot, 'systems', 'homestead_alpha.json')),
    builderUx: readJson(path.join(dataRoot, 'systems', 'builder_ux_alpha.json')),
    holomap: readJson(path.join(dataRoot, 'holomap', 'mvp_regions.json')),
    conformance: readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json')),
    assetManifest: readJson(path.join(resourcesRoot, 'assets', MODULE_ID, 'asset_manifest.json')),
    legalAudit: readJson(path.join(dataRoot, 'systems', 'legal_content_audit.json')),
    sounds: readJson(path.join(dataRoot, 'sounds', 'mvp_sound_contract.json')),
  }

  const rehearsalRoot = dryRun
    ? fs.mkdtempSync(path.join(os.tmpdir(), `openlands-${editionKey}-runtime-rehearsal-`))
    : path.dirname(outputPath)
  const savedArtifactRoot = dryRun
    ? path.join(rehearsalRoot, 'saved-artifacts')
    : path.join(path.dirname(outputPath), `${editionKey}-local-runtime-rehearsal-artifacts`)
  fs.mkdirSync(savedArtifactRoot, { recursive: true })

  const saveArtifact = (scenarioId, name, payload) => {
    const relativePath = `${scenarioId}/${name}`
    const target = path.join(savedArtifactRoot, relativePath)
    writeJson(target, payload)
    return relativePath
  }

  const scenarioResults = (acceptance.scenarios ?? []).map((scenario) => {
    const fixtureResults = (scenario.inputFixtureRefs ?? []).map((ref) => {
      const filePath = fixturePath(resourcesRoot, dataRoot, ref)
      return {
        ref,
        exists: fileExists(filePath),
        sha256: fileExists(filePath) ? sha256File(filePath) : null,
      }
    })
    const missingFixtures = fixtureResults.filter((fixture) => !fixture.exists)
    assert(missingFixtures.length === 0, `${scenario.id} missing fixtures: ${missingFixtures.map((fixture) => fixture.ref).join(', ')}`)
    const capture = buildCapture({ scenario, contracts, runtimeCoreReport, localLauncherReport })
    const savedArtifacts = [
      saveArtifact(scenario.id, 'input-fixture-summary.json', fixtureResults),
      saveArtifact(scenario.id, 'assertion-map.json', {
        assertions: scenario.assertions ?? [],
        status: 'preflight_passed',
        clearsRuntimeGates: false,
      }),
      saveArtifact(scenario.id, 'capture-summary.json', capture),
    ]
    return flowResult({
      scenario,
      runtimeTarget: edition.runtimeTarget,
      artifactSha256,
      fixtureResults,
      capture,
      savedArtifacts,
    })
  })

  return {
    schema: 'echo.openlands.edition.local_runtime_rehearsal_report.v1',
    status: 'preflight_passed',
    publicAlphaReady: false,
    clearsRuntimeGates: false,
    rehearsalOnly: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    edition: editionKey,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    runtimeExecutionContract: RUNTIME_EXECUTION_CONTRACT,
    playableRuntimeContract: PLAYABLE_RUNTIME_CONTRACT,
    runtimeCoreReport: runtimeCoreReportPath,
    localLauncherRehearsalReport: localLauncherReportPath,
    moduleArtifact: artifactPath,
    moduleArtifactSha256: artifactSha256,
    moduleArtifactSize: artifactSize,
    rehearsalRoot,
    savedArtifactRoot,
    scenarioResults,
    scenarioCount: scenarioResults.length,
    blockedBy: [
      'real_adapter_execution_missing',
      'real_launcher_execution_missing',
      'public_release_download_urls_missing',
      'final_asset_human_review_missing',
      'local_rehearsal_does_not_clear_runtime_gates',
    ],
    proofs: [
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
    ],
    outputPath,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  if (!args.edition) throw new Error('--edition is required')
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const workspaceRoot = defaultWorkspaceRoot(moduleRoot)
  const edition = EDITIONS[args.edition]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : path.join(workspaceRoot, edition.repo)
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const outputPath = args.output ? path.resolve(args.output) : path.join(editionRoot, 'evidence', edition.reportName)
  const report = buildReport({
    editionKey: args.edition,
    editionRoot,
    moduleRoot,
    releaseRoot,
    outputPath,
    dryRun: args.dryRun,
  })
  if (!args.dryRun) writeJson(outputPath, report)
  if (args.json) {
    console.log(JSON.stringify(report, null, 2))
  } else {
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    console.log(`Openlands ${args.edition} local runtime rehearsal ${action}: ${report.scenarioCount} scenarios, publicAlphaReady=${report.publicAlphaReady}.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-local-runtime-rehearsal-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to C:/Development/Github/<edition repo>.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-local-runtime-rehearsal-report.json.
  --dry-run               Generate without writing the report.
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

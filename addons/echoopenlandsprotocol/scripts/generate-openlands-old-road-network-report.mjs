import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EXPECTED_RUNTIMES = ['echo_native', 'echo_runtime_standalone', 'neoforge']
const REQUIRED_OLD_ROAD_BLOCKS = ['old_road_block', 'old_road_marker', 'broken_waystone', 'restored_waystone', 'waystone_plinth']
const REQUIRED_ROUTE_ITEMS = ['region_rubbing', 'old_road_token', 'waystone_core', 'route_binding']
const REQUIRED_ROUTE_RECIPES = ['region_rubbing', 'old_road_token', 'waystone_core', 'route_binding']
const REQUIRED_ROAD_LANDMARKS = ['road_marker', 'broken_bridge', 'broken_waystone_site']
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-old-road-network-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-old-road-network-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-old-road-network-report.json',
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

function run(command, args, cwd) {
  const result = spawnSync(command, args, {
    cwd,
    encoding: 'utf8',
    shell: false,
  })
  return {
    status: result.status,
    stdout: result.stdout?.trim() ?? '',
    stderr: result.stderr?.trim() ?? '',
  }
}

function jarEntries(artifactPath) {
  const result = run('jar', ['tf', artifactPath], path.dirname(artifactPath))
  if (result.status !== 0) {
    throw new Error(`jar tf failed for ${artifactPath}: ${result.stderr || result.stdout}`)
  }
  return result.stdout.split(/\r?\n/).filter(Boolean)
}

function extractJar(artifactPath, entryNames) {
  const extractRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-old-road-network-'))
  const extract = run('jar', ['xf', artifactPath, ...entryNames], extractRoot)
  if (extract.status !== 0) {
    throw new Error(`jar xf failed for ${artifactPath}: ${extract.stderr || extract.stdout}`)
  }
  return extractRoot
}

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function normalizeId(value) {
  if (typeof value !== 'string') return value
  return value.includes(':') ? value.split(':').pop() : value
}

function sameSet(actual, expected) {
  const sort = (values) => [...(values ?? [])].sort()
  return JSON.stringify(sort(actual)) === JSON.stringify(sort(expected))
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function assertRuntimeParity(payload, label) {
  assert(sameSet(payload.runtimeParity ?? payload.runtimeTargets ?? [], EXPECTED_RUNTIMES), `${label} runtime parity mismatch`)
}

function artifactRuntimeEntries(artifactPath, edition) {
  const packageEntries = jarEntries(artifactPath)
  if (edition.artifactKind !== 'echo-addon') {
    return { packageEntries, runtimeEntries: packageEntries, nestedRuntimeEntry: null }
  }
  const nestedRuntimeEntry = packageEntries.find((entry) => /^lib\/.*-runtime\.jar$/.test(entry))
  assert(nestedRuntimeEntry, `${edition.artifactName} missing nested runtime jar`)
  const extractRoot = extractJar(artifactPath, [nestedRuntimeEntry])
  const runtimeEntries = jarEntries(path.join(extractRoot, nestedRuntimeEntry))
  return { packageEntries, runtimeEntries, nestedRuntimeEntry }
}

function idSet(records) {
  return new Set((records ?? []).map((record) => normalizeId(record.id)))
}

function byId(records) {
  return new Map((records ?? []).map((record) => [normalizeId(record.id), record]))
}

function requireIds(ids, knownIds, label) {
  for (const id of ids) {
    assert(knownIds.has(normalizeId(id)), `${label} missing ${id}`)
  }
}

function inputContexts(recipe) {
  return (recipe.inputs ?? []).filter((input) => input.context).map((input) => input.context)
}

function itemInputCount(recipe, itemId) {
  return (recipe.inputs ?? [])
    .filter((input) => normalizeId(input.item) === itemId)
    .reduce((total, input) => total + (input.count ?? 0), 0)
}

function outputsItem(recipe, itemId) {
  return (recipe.outputs ?? []).some((output) => normalizeId(output.item) === itemId)
}

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)
  assert(fileExists(moduleRoot), `module root not found: ${moduleRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const blocksPayload = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json'))
  const itemsPayload = readJson(path.join(dataRoot, 'items', 'mvp_items.json'))
  const recipesPayload = readJson(path.join(dataRoot, 'recipes', 'mvp_recipes.json'))
  const structuresPayload = readJson(path.join(dataRoot, 'structures', 'mvp_landmarks.json'))
  const waystonesPayload = readJson(path.join(dataRoot, 'waystones', 'waystone_contract.json'))
  const holomapPayload = readJson(path.join(dataRoot, 'holomap', 'mvp_regions.json'))
  const playtestPayload = readJson(path.join(dataRoot, 'playtests', 'mvp_first_hour_acceptance.json'))
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))

  assertRuntimeParity(blocksPayload, 'blocks')
  assertRuntimeParity(itemsPayload, 'items')
  assertRuntimeParity(recipesPayload, 'recipes')
  assertRuntimeParity(structuresPayload, 'structures')
  assertRuntimeParity(waystonesPayload, 'waystones')
  assertRuntimeParity(holomapPayload, 'holomap')
  assertRuntimeParity(playtestPayload, 'playtest')
  assert(evidenceTemplate.packId === edition.packId, `${edition.packId} evidence template packId mismatch`)
  assert(evidenceTemplate.runtimeTarget === edition.runtimeTarget, `${edition.packId} evidence template runtime target mismatch`)

  const blockMap = byId(blocksPayload.blocks)
  const itemMap = byId(itemsPayload.items)
  const recipeMap = byId(recipesPayload.recipes)
  const landmarkMap = byId(structuresPayload.landmarks)
  requireIds(REQUIRED_OLD_ROAD_BLOCKS, idSet(blocksPayload.blocks), 'old-road block registry')
  requireIds(REQUIRED_ROUTE_ITEMS, idSet(itemsPayload.items), 'route item registry')
  requireIds(REQUIRED_ROUTE_RECIPES, idSet(recipesPayload.recipes), 'route recipe registry')
  requireIds(REQUIRED_ROAD_LANDMARKS, idSet(structuresPayload.landmarks), 'old-road landmark registry')
  assert(sameSet(waystonesPayload.blocks ?? [], REQUIRED_OLD_ROAD_BLOCKS), 'waystone contract blocks must exactly declare old-road and waystone blocks')

  const oldRoadBlock = blockMap.get('old_road_block')
  const oldRoadMarker = blockMap.get('old_road_marker')
  const brokenWaystone = blockMap.get('broken_waystone')
  const restoredWaystone = blockMap.get('restored_waystone')
  const waystonePlinth = blockMap.get('waystone_plinth')
  assert(oldRoadBlock.tags?.includes('openlands:old_roads'), 'old_road_block must carry openlands:old_roads')
  assert(oldRoadBlock.structurePlacement?.includes('old_road_segments'), 'old_road_block must place in old_road_segments')
  assert(oldRoadMarker.tags?.includes('openlands:map_hint'), 'old_road_marker must carry openlands:map_hint')
  assert(oldRoadMarker.structurePlacement?.includes('roadside_markers'), 'old_road_marker must place in roadside_markers')
  assert(brokenWaystone.tags?.includes('openlands:repairable'), 'broken_waystone must be repairable')
  assert(restoredWaystone.tags?.includes('openlands:travel_node'), 'restored_waystone must be a travel node')
  assert(restoredWaystone.effects?.includes('route_link'), 'restored_waystone must expose route_link effect')
  assert(waystonePlinth.tags?.includes('openlands:route_building'), 'waystone_plinth must support route building')

  const oldRoadToken = itemMap.get('old_road_token')
  const routeBinding = itemMap.get('route_binding')
  const regionRubbing = itemMap.get('region_rubbing')
  const waystoneCore = itemMap.get('waystone_core')
  assert(oldRoadToken.useType === 'route_record', 'old_road_token must be a route_record')
  assert(oldRoadToken.tags?.includes('openlands:old_road'), 'old_road_token must carry openlands:old_road')
  assert(oldRoadToken.tags?.includes('openlands:holomap'), 'old_road_token must carry openlands:holomap')
  assert(routeBinding.tags?.includes('openlands:waystone_binding'), 'route_binding must carry openlands:waystone_binding')
  assert(regionRubbing.tags?.includes('openlands:holomap'), 'region_rubbing must carry openlands:holomap')
  assert(waystoneCore.tags?.includes('openlands:waystone_repair'), 'waystone_core must carry openlands:waystone_repair')

  const regionRubbingRecipe = recipeMap.get('region_rubbing')
  const oldRoadTokenRecipe = recipeMap.get('old_road_token')
  const waystoneCoreRecipe = recipeMap.get('waystone_core')
  const routeBindingRecipe = recipeMap.get('route_binding')
  assert(regionRubbingRecipe.station === 'map_table', 'region_rubbing must be a map_table recipe')
  assert(inputContexts(regionRubbingRecipe).includes('discovered_region_marker'), 'region_rubbing must require discovered_region_marker context')
  assert(outputsItem(regionRubbingRecipe, 'region_rubbing'), 'region_rubbing recipe must output region_rubbing')
  assert(oldRoadTokenRecipe.station === 'map_table', 'old_road_token must be a map_table recipe')
  assert(inputContexts(oldRoadTokenRecipe).includes('walked_old_road_segment'), 'old_road_token must require walked_old_road_segment context')
  assert(itemInputCount(oldRoadTokenRecipe, 'region_rubbing') === 1, 'old_road_token must consume one region_rubbing')
  assert(outputsItem(oldRoadTokenRecipe, 'old_road_token'), 'old_road_token recipe must output old_road_token')
  assert(waystoneCoreRecipe.station === 'map_table', 'waystone_core must be a map_table recipe')
  assert(itemInputCount(waystoneCoreRecipe, 'glow_crystal') === 1, 'waystone_core must consume one glow_crystal')
  assert(itemInputCount(waystoneCoreRecipe, 'cupral_fitting') === 4, 'waystone_core must consume four cupral_fitting')
  assert(itemInputCount(waystoneCoreRecipe, 'region_rubbing') === 1, 'waystone_core must consume one region_rubbing')
  assert(outputsItem(waystoneCoreRecipe, 'waystone_core'), 'waystone_core recipe must output waystone_core')
  assert(routeBindingRecipe.station === 'map_table', 'route_binding must be a map_table recipe')
  assert(inputContexts(routeBindingRecipe).includes('two_discovered_waystones'), 'route_binding must require two_discovered_waystones context')
  assert(itemInputCount(routeBindingRecipe, 'old_road_token') === 2, 'route_binding must consume two old_road_token')
  assert(itemInputCount(routeBindingRecipe, 'region_rubbing') === 1, 'route_binding must consume one region_rubbing')
  assert(outputsItem(routeBindingRecipe, 'route_binding'), 'route_binding recipe must output route_binding')

  const oldRoadLayer = (holomapPayload.layers ?? []).find((layer) => layer.id === 'old_roads')
  const waystoneLayer = (holomapPayload.layers ?? []).find((layer) => layer.id === 'waystones')
  const roadSegmentHint = (holomapPayload.hintTypes ?? []).find((hint) => hint.id === 'road_segment')
  assert(holomapPayload.regionDataContract?.storedFields?.includes('oldRoadSegments'), 'HoloMap must persist oldRoadSegments')
  assert(oldRoadLayer?.visibleByDefault === true, 'old_roads HoloMap layer must be visible by default')
  assert(oldRoadLayer?.source === 'old_road_block_and_marker_discovery', 'old_roads layer source mismatch')
  assert(waystoneLayer?.source === 'waystone_state_machine', 'waystone layer source mismatch')
  assert(roadSegmentHint?.revealSources?.includes('walked_old_road'), 'road_segment hint must reveal from walked_old_road')
  assert(roadSegmentHint?.revealSources?.includes('old_road_token'), 'road_segment hint must reveal from old_road_token')

  const roadLandmarks = (structuresPayload.landmarks ?? []).filter((landmark) => {
    const blocks = new Set((landmark.blocks ?? []).map((block) => normalizeId(block)))
    return REQUIRED_OLD_ROAD_BLOCKS.some((block) => blocks.has(block))
  })
  for (const id of REQUIRED_ROAD_LANDMARKS) {
    assert(roadLandmarks.some((landmark) => normalizeId(landmark.id) === id), `old-road landmark coverage missing ${id}`)
  }
  assert(landmarkMap.get('road_marker')?.tutorialHook === 'first_old_road_marker', 'road_marker must trigger first_old_road_marker')
  assert(landmarkMap.get('broken_bridge')?.holoMapHint === 'interrupted_road', 'broken_bridge must provide interrupted_road hint')
  assert(landmarkMap.get('broken_waystone_site')?.holoMapHint === 'broken_waystone', 'broken_waystone_site hint mismatch')

  const boundState = (waystonesPayload.stateMachine ?? []).find((state) => state.state === 'bound')
  const activeState = (waystonesPayload.stateMachine ?? []).find((state) => state.state === 'active')
  assert(boundState?.inputs?.some((input) => input.item === 'route_binding' && input.count === 1), 'bound waystone state must consume one route_binding')
  assert(boundState?.outputs?.includes('route_id'), 'bound waystone state must output route_id')
  assert(activeState?.outputs?.includes('fast_travel_if_two_active'), 'active waystone state must output fast_travel_if_two_active')
  assert(waystonesPayload.effects?.fastTravel?.enabledByDefault === true, 'fast travel must be enabled by default in relaxed modes')
  assert(waystonesPayload.effects?.fastTravel?.requiresActiveStones === 2, 'fast travel must require two active stones')
  for (const field of ['linkedRouteIds', 'isPublicTravel', 'canPublicRename', 'repairContributorIds']) {
    assert(waystonesPayload.multiplayerState?.storedFields?.includes(field), `waystone multiplayer state missing ${field}`)
  }
  assert(waystonesPayload.multiplayerState?.defaultPermissions?.travel === 'public_after_active', 'travel permission default mismatch')

  const explorationScenario = (playtestPayload.acceptanceScenarios ?? []).find((scenario) => scenario.id === 'first_exploration_hook')
  const firstWaystoneScenario = (playtestPayload.acceptanceScenarios ?? []).find((scenario) => scenario.id === 'first_waystone')
  const firstWaystoneCheckpoint = (playtestPayload.saveLoadCheckpoints ?? []).find((checkpoint) => checkpoint.id === 'after_first_waystone_repair')
  assert(explorationScenario?.requires?.blocks?.includes('old_road_block'), 'first_exploration_hook must require old_road_block')
  assert(explorationScenario?.requires?.blocks?.includes('old_road_marker'), 'first_exploration_hook must require old_road_marker')
  assert(explorationScenario?.requires?.holomapLayers?.includes('old_roads'), 'first_exploration_hook must require old_roads layer')
  assert(explorationScenario?.requires?.hintTypes?.includes('road_segment'), 'first_exploration_hook must require road_segment hint')
  assert((explorationScenario?.runtimeActions ?? []).some((action) => action.assertions?.includes('old_road_marker_writes_old_road_segment')), 'first_exploration_hook must assert old-road segment writing')
  assert(firstWaystoneScenario?.requires?.blocks?.includes('old_road_marker'), 'first_waystone must require old_road_marker')
  assert(firstWaystoneScenario?.requires?.blocks?.includes('old_road_block'), 'first_waystone must require old_road_block')
  assert(firstWaystoneCheckpoint?.requiredAssertions?.includes('old_road_segment_preserved'), 'waystone save/load checkpoint must preserve old road segment')
  for (const item of ['old_road_token', 'region_rubbing', 'route_binding']) {
    assert(playtestPayload.waystonePublicAlphaScenario?.requiresItems?.includes(item), `waystone public alpha scenario missing item ${item}`)
  }
  for (const recipe of ['old_road_token', 'region_rubbing', 'route_binding']) {
    assert(playtestPayload.waystonePublicAlphaScenario?.requiresRecipes?.includes(recipe), `waystone public alpha scenario missing recipe ${recipe}`)
  }

  const loadStep = (runtimePlan.loadSteps ?? []).find((step) => step.id === 'bind_waystones_holomap_and_multiplayer_state')
  assert(loadStep?.successSignal === 'openlands_waystone_network_bound', 'old road network load step success signal mismatch')
  assert(loadStep?.resourceIds?.includes('waystones/waystone_contract'), 'old road network load step must include waystone contract')
  assert(loadStep?.resourceIds?.includes('holomap/mvp_regions'), 'old road network load step must include HoloMap contract')
  assert(loadStep?.resourceIds?.includes('systems/coop_and_smp'), 'old road network load step must include co-op contract')
  for (const evidence of ['waystone_state_persistence_ready', 'holomap_region_persistence_ready', 'multiplayer_permissions_bound']) {
    assert(loadStep?.requiredEvidence?.includes(evidence), `old road network load step missing evidence ${evidence}`)
  }

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifact = artifactRuntimeEntries(artifactPath, edition)
  const runtimeEntriesChecked = [
    'data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json',
    'data/echoopenlandsprotocol/openlands/items/mvp_items.json',
    'data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json',
    'data/echoopenlandsprotocol/openlands/structures/mvp_landmarks.json',
    'data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json',
    'data/echoopenlandsprotocol/openlands/holomap/mvp_regions.json',
    'data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json',
    'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json',
  ]
  for (const entry of runtimeEntriesChecked) {
    assert(artifact.runtimeEntries.includes(entry), `${edition.artifactName} missing runtime entry ${entry}`)
  }

  const report = {
    schema: 'echo.openlands.edition.old_road_network_report.v1',
    status: 'preflight_passed',
    realRuntimeOldRoadNetworkRequiredBeforePublicAlpha: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    contracts: {
      blocks: 'data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json',
      items: 'data/echoopenlandsprotocol/openlands/items/mvp_items.json',
      recipes: 'data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json',
      landmarks: 'data/echoopenlandsprotocol/openlands/structures/mvp_landmarks.json',
      waystones: 'data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json',
      holomap: 'data/echoopenlandsprotocol/openlands/holomap/mvp_regions.json',
      playtest: 'data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json',
      runtimePlan: 'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json',
    },
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      nestedRuntimeEntry: artifact.nestedRuntimeEntry,
      runtimeEntriesChecked,
    },
    counts: {
      oldRoadBlocks: REQUIRED_OLD_ROAD_BLOCKS.length,
      routeItems: REQUIRED_ROUTE_ITEMS.length,
      routeRecipes: REQUIRED_ROUTE_RECIPES.length,
      roadLandmarks: roadLandmarks.length,
      holomapOldRoadLayers: [oldRoadLayer, waystoneLayer].filter(Boolean).length,
    },
    oldRoadBlockIds: REQUIRED_OLD_ROAD_BLOCKS,
    routeItemIds: REQUIRED_ROUTE_ITEMS,
    routeRecipeIds: REQUIRED_ROUTE_RECIPES,
    roadLandmarkIds: roadLandmarks.map((landmark) => normalizeId(landmark.id)),
    routeRecipeSummaries: REQUIRED_ROUTE_RECIPES.map((id) => {
      const recipe = recipeMap.get(id)
      return {
        id,
        station: recipe.station,
        contextInputs: inputContexts(recipe),
        itemInputs: (recipe.inputs ?? []).filter((input) => input.item).map((input) => ({ item: input.item, count: input.count })),
        outputs: recipe.outputs ?? [],
        unlockedBy: recipe.unlockedBy ?? [],
      }
    }),
    holomapOldRoadContract: {
      storedField: 'oldRoadSegments',
      oldRoadLayer: oldRoadLayer.id,
      oldRoadLayerSource: oldRoadLayer.source,
      roadSegmentHint: roadSegmentHint.id,
      roadSegmentRevealSources: roadSegmentHint.revealSources,
    },
    waystoneRouteContract: {
      boundStateConsumes: 'route_binding',
      boundStateOutputs: boundState.outputs,
      activeStateOutputs: activeState.outputs,
      fastTravelRequiresActiveStones: waystonesPayload.effects.fastTravel.requiresActiveStones,
      travelPermissionDefault: waystonesPayload.multiplayerState.defaultPermissions.travel,
      multiplayerStoredFields: waystonesPayload.multiplayerState.storedFields,
    },
    playtestCoverage: {
      explorationScenario: explorationScenario.id,
      explorationAssertions: (explorationScenario.runtimeActions ?? []).flatMap((action) => action.assertions ?? []),
      firstWaystoneScenario: firstWaystoneScenario.id,
      firstWaystoneCheckpoint: firstWaystoneCheckpoint.id,
      publicAlphaScenario: playtestPayload.waystonePublicAlphaScenario.id,
    },
    oldRoadNetworkLoadStep: {
      id: loadStep.id,
      successSignal: loadStep.successSignal,
      requiredEvidence: loadStep.requiredEvidence,
    },
    blockedBy: [
      'real_runtime_old_road_generation_missing',
      'old_road_segment_walk_recording_missing',
      'map_table_route_binding_runtime_missing',
      'holomap_old_road_reveal_runtime_missing',
      'waystone_link_travel_runtime_missing',
    ],
    outputPath,
    proofs: [
      'old_road_blocks_declared',
      'old_road_items_declared',
      'road_landmarks_resolve',
      'map_table_route_recipes_resolve',
      'holomap_old_road_layer_resolves',
      'waystone_route_state_fields_resolve',
      'old_road_playtest_acceptance_resolves',
      'runtime_load_step_binds_old_road_network',
      'compiled_artifact_contains_old_road_contracts',
      'public_alpha_blocked_until_real_old_road_runtime_execution',
    ],
  }

  if (!dryRun) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true })
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  }
  return report
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const edition = EDITIONS[args.edition]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : process.cwd()
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(editionRoot, 'evidence', edition.reportName)
  const report = buildReport({
    editionKey: args.edition,
    editionRoot,
    moduleRoot,
    releaseRoot,
    outputPath,
    dryRun: args.dryRun,
  })
  if (args.json) {
    console.log(JSON.stringify(report, null, 2))
  } else {
    const action = args.dryRun ? 'validated' : `wrote ${outputPath}`
    console.log(`Openlands ${args.edition} old road network report ${action}: ${report.counts.oldRoadBlocks} road/waystone blocks, ${report.counts.routeRecipes} route recipes, ${report.counts.roadLandmarks} road landmarks.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-old-road-network-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-old-road-network-report.json.
  --dry-run               Validate without writing the report.
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

import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const MODULE_ID = 'echoopenlandsprotocol'
const EXPECTED_RUNTIMES = ['echo_native', 'echo_runtime_standalone', 'neoforge']
const HARDCORE_FLAGS = ['stamina', 'hydration', 'foodSpoilage', 'temperatureDamage']
const EXPECTED_LOAD_PHASES = [
  'discover',
  'load_data',
  'register_content',
  'bind_worldgen',
  'bind_gameplay_state',
  'ready',
  'release_gate',
]
const EXPECTED_RUNTIME_EVIDENCE = [
  'descriptor_resolved',
  'module_identity_verified',
  'runtime_target_accepted',
  'source_root_mounted',
  'standard_mode_relaxed',
  'hardlands_optional',
  'legal_policy_accepted',
  'registry_json_parsed',
  'runtime_parity_declared',
  'minimum_content_counts_met',
  'registry_ids_resolved',
  'block_ids_registered',
  'item_ids_registered',
  'recipe_ids_registered',
  'tag_ids_registered',
  'canonical_echo_ids_retained',
  'loot_tables_bound',
  'station_surfaces_bound',
  'biome_palettes_bound',
  'spawn_tables_bound',
  'landmark_pools_bound',
  'starter_spawn_guarantees_bound',
  'tutorial_triggers_bound',
  'shelter_score_bound',
  'first_hour_save_load_ready',
  'waystone_state_persistence_ready',
  'holomap_region_persistence_ready',
  'multiplayer_permissions_bound',
  'homestead_state_bound',
  'builder_ux_bound',
  'sound_events_bound',
  'missing_asset_policy_applied',
  'adapter_ready_signal',
  'runtime_smoke_test_ready',
  'hardcore_meters_default_off',
  'openlands_contract_validator_pass',
  'native_standalone_neoforge_artifacts_uploaded_with_sha256',
  'launcher_install_update_repair_rollback_pass',
  'first_hour_runtime_playtest_pass',
  'waystone_state_save_load_pass',
  'legal_content_audit_pass',
]
const EXPECTED_PRODUCTION_PHASES = [
  'phase_01_product_contract',
  'phase_02_repo_and_artifact_setup',
  'phase_03_data_and_schema_layout',
  'phase_04_mvp_block_registry',
  'phase_05_mvp_item_registry',
  'phase_06_crafting_and_stations',
  'phase_07_first_hour_gameplay',
  'phase_08_worldgen_and_exploration',
  'phase_09_waystones_and_old_roads',
  'phase_10_alpha_systems_and_distribution',
  'final_launch_phase_openlands_1_0_roadmap',
]
const EXPECTED_PRODUCTION_CHECKPOINTS = {
  phase_01_product_contract: [
    'official_pack_identity',
    'standard_relaxed_default',
    'mode_overlays',
    'legal_content_bible',
    'mvp_player_promise',
  ],
  phase_02_repo_and_artifact_setup: [
    'addon_module_scaffold',
    'echo_descriptor',
    'neoforge_template',
    'edition_repos',
    'edition_docs',
  ],
  phase_03_data_and_schema_layout: [
    'content_roots',
    'asset_roots',
    'block_schema_fields',
    'item_schema_fields',
    'conformance_fixture',
  ],
  phase_04_mvp_block_registry: [
    'terrain_blocks',
    'stone_blocks',
    'ore_blocks',
    'wood_blocks',
    'utility_blocks',
  ],
  phase_05_mvp_item_registry: [
    'raw_material_items',
    'metal_items',
    'food_items',
    'tool_items',
    'utility_waystone_items',
  ],
  phase_06_crafting_and_stations: [
    'handcrafting_recipes',
    'workbench_recipes',
    'kiln_recipes',
    'forge_recipes',
    'map_table_recipes',
  ],
  phase_07_first_hour_gameplay: [
    'starter_spawn_generator',
    'discovery_tutorials',
    'shelter_score',
    'sleep_milestone',
    'first_hour_save_load',
  ],
  phase_08_worldgen_and_exploration: [
    'mvp_biomes',
    'biome_resource_identity',
    'landmarks',
    'creature_spawns_and_ambience',
    'holomap_region_data',
  ],
  phase_09_waystones_and_old_roads: [
    'old_road_blocks',
    'waystone_state_machine',
    'repair_inputs',
    'active_waystone_effects',
    'multiplayer_permissions',
  ],
  phase_10_alpha_systems_and_distribution: [
    'homestead_systems',
    'creature_roster',
    'builder_ux',
    'artifact_outputs',
    'release_validation',
  ],
  final_launch_phase_openlands_1_0_roadmap: [
    'freeze_relaxed_default',
    'ship_mvp_scope',
    'public_alpha_scope',
    'one_dot_zero_scope',
    'parity_source_of_truth',
  ],
}
const ALLOWED_PRODUCTION_EVIDENCE_KINDS = [
  'module_file',
  'edition_file',
  'artifact_file',
  'runtime_gate',
]

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    json: false,
    requireArtifacts: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--json') args.json = true
    else if (arg === '--require-artifacts') args.requireArtifacts = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function fileExists(filePath) {
  return fs.existsSync(filePath)
}

function isPngFile(filePath) {
  if (!fileExists(filePath)) return false
  const signature = fs.readFileSync(filePath).subarray(0, 8)
  return signature.equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function sameJson(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function runGeneratorJson(errors, { moduleRoot, scriptName, args, label }) {
  const scriptPath = path.join(moduleRoot, 'scripts', scriptName)
  assert(errors, fileExists(scriptPath), `missing ${label} generator ${scriptPath}`)
  if (!fileExists(scriptPath)) return null
  const result = spawnSync(process.execPath, [scriptPath, ...args, '--dry-run', '--json'], {
    cwd: path.resolve(moduleRoot, '..', '..'),
    encoding: 'utf8',
    windowsHide: true,
  })
  assert(errors, result.status === 0, `${label} generator dry-run failed: ${result.stderr || result.stdout}`)
  if (result.status !== 0) return null
  try {
    return JSON.parse(result.stdout)
  } catch (error) {
    errors.push(`${label} generator dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
    return null
  }
}

function normalizeId(value) {
  if (typeof value !== 'string') return value
  return value.includes(':') ? value.split(':').pop() : value
}

function normalizeRecipeRef(value) {
  return normalizeId(value).replace(/^recipe\//, '')
}

function soundKey(value) {
  if (typeof value !== 'string') return value
  return value.replace(/^openlands:/, 'openlands.')
}

function assert(errors, condition, message) {
  if (!condition) errors.push(message)
}

function sortedUnique(values) {
  return [...new Set(values)].sort()
}

function assertSameStringSet(errors, actualValues, expectedValues, label) {
  const actual = sortedUnique((actualValues ?? []).filter((value) => typeof value === 'string' && value.length > 0))
  const expected = sortedUnique((expectedValues ?? []).filter((value) => typeof value === 'string' && value.length > 0))
  assert(errors, JSON.stringify(actual) === JSON.stringify(expected), `${label} must match expected ids`)
  for (const value of expected) {
    assert(errors, actual.includes(value), `${label} missing ${value}`)
  }
  for (const value of actual) {
    assert(errors, expected.includes(value), `${label} contains unknown ${value}`)
  }
}

function assertNoForbiddenTerms(errors, values, forbiddenTerms, label) {
  for (const value of values) {
    if (value === undefined || value === null) continue
    const normalized = String(value).toLowerCase()
    for (const term of forbiddenTerms ?? []) {
      const normalizedTerm = String(term).toLowerCase()
      if (normalized.includes(normalizedTerm)) {
        errors.push(`${label} contains forbidden public term "${term}" in "${value}"`)
      }
    }
  }
}

function productionEvidenceExists(evidence, moduleRoot) {
  const modulesRoot = path.resolve(moduleRoot, '..', '..')
  const workspaceRoot = path.resolve(modulesRoot, '..')
  if (evidence.kind === 'module_file') return fileExists(path.join(moduleRoot, evidence.path))
  if (evidence.kind === 'edition_file') return fileExists(path.join(workspaceRoot, evidence.path))
  if (evidence.kind === 'artifact_file') return fileExists(path.join(modulesRoot, evidence.path))
  return evidence.kind === 'runtime_gate'
}

function findModuleRoot(explicitRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = process.cwd()
  for (;;) {
    const descriptor = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(descriptor)) return cursor
    const candidate = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(candidate)) return path.join(cursor, 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function requireFields(errors, object, fields, label) {
  for (const field of fields) {
    assert(errors, object[field] !== undefined && object[field] !== null && object[field] !== '', `${label} missing ${field}`)
  }
}

function requireRuntimeParity(errors, payload, label) {
  const raw = payload.runtimeParity ?? payload.runtimeTargets ?? []
  const runtimes = sortedUnique(raw.map((entry) => (typeof entry === 'string' ? entry : entry?.id)).filter(Boolean))
  assert(errors, JSON.stringify(runtimes) === JSON.stringify(EXPECTED_RUNTIMES), `${label} runtime parity must be ${EXPECTED_RUNTIMES.join(', ')}`)
}

function collectRecipeItemRefs(recipe) {
  const refs = []
  for (const side of ['inputs', 'outputs']) {
    for (const entry of recipe[side] ?? []) {
      if (entry.item) refs.push({ id: normalizeId(entry.item), side })
      if (entry.block) refs.push({ id: normalizeId(entry.block), side, kind: 'block' })
    }
  }
  return refs
}

function blockTextureKey(block) {
  return String(block.texture ?? '').replace(/^block\//, '')
}

function itemTextureKey(item) {
  return String(item.texture ?? '').replace(/^item\//, '')
}

function validate({ moduleRoot, requireArtifacts }) {
  const errors = []
  const warnings = []
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const assetsRoot = path.join(resourcesRoot, 'assets', MODULE_ID)

  const descriptor = readJson(path.join(resourcesRoot, 'META-INF', 'echo.mod.json'))
  const contractSourceRoot = path.join(moduleRoot, 'src', 'main', 'java', 'com', 'knoxhack', 'echoopenlandsprotocol', 'contract')
  const runtimeContractSource = fs.readdirSync(contractSourceRoot)
    .filter((fileName) => fileName.endsWith('.java'))
    .map((fileName) => fs.readFileSync(path.join(contractSourceRoot, fileName), 'utf8'))
    .join('\n')
  const runtimeSourceRoot = path.join(moduleRoot, 'src', 'main', 'java', 'com', 'knoxhack', 'echoopenlandsprotocol', 'runtime')
  const runtimeSourceFiles = fileExists(runtimeSourceRoot)
    ? fs.readdirSync(runtimeSourceRoot).filter((fileName) => fileName.endsWith('.java'))
    : []
  const runtimeSource = runtimeSourceFiles
    .map((fileName) => fs.readFileSync(path.join(runtimeSourceRoot, fileName), 'utf8'))
    .join('\n')
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const blocksPayload = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json'))
  const itemsPayload = readJson(path.join(dataRoot, 'items', 'mvp_items.json'))
  const recipesPayload = readJson(path.join(dataRoot, 'recipes', 'mvp_recipes.json'))
  const tagsPayload = readJson(path.join(dataRoot, 'tags', 'mvp_tags.json'))
  const biomesPayload = readJson(path.join(dataRoot, 'biomes', 'mvp_biomes.json'))
  const structuresPayload = readJson(path.join(dataRoot, 'structures', 'mvp_landmarks.json'))
  const creaturesPayload = readJson(path.join(dataRoot, 'creatures', 'mvp_creatures.json'))
  const lootPayload = readJson(path.join(dataRoot, 'loot', 'mvp_loot.json'))
  const modesPayload = readJson(path.join(dataRoot, 'config', 'game_modes.json'))
  const policyPayload = readJson(path.join(dataRoot, 'config', 'content_policy.json'))
  const waystonesPayload = readJson(path.join(dataRoot, 'waystones', 'waystone_contract.json'))
  const progressionPayload = readJson(path.join(dataRoot, 'progression', 'first_hour_route.json'))
  const launchRoadmapPayload = readJson(path.join(dataRoot, 'progression', 'launch_roadmap.json'))
  const productionPhaseMatrixPayload = readJson(path.join(dataRoot, 'progression', 'production_phase_matrix.json'))
  const playtestPayload = readJson(path.join(dataRoot, 'playtests', 'mvp_first_hour_acceptance.json'))
  const tutorialsPayload = readJson(path.join(dataRoot, 'tutorials', 'first_hour_prompts.json'))
  const overviewPayload = readJson(path.join(dataRoot, 'index', 'openlands_overview.json'))
  const gameplayCatalogPayload = readJson(path.join(dataRoot, 'index', 'mvp_gameplay_catalog.json'))
  const workspaceRoot = path.resolve(moduleRoot, '..', '..', '..')
  const generatedProductionPhaseMatrix = runGeneratorJson(errors, {
    moduleRoot,
    scriptName: 'generate-openlands-production-phase-matrix.mjs',
    args: ['--module-root', moduleRoot, '--workspace-root', workspaceRoot],
    label: 'production phase matrix',
  })
  const generatedGameplayCatalog = runGeneratorJson(errors, {
    moduleRoot,
    scriptName: 'generate-openlands-gameplay-catalog.mjs',
    args: ['--module-root', moduleRoot],
    label: 'gameplay catalog',
  })
  if (generatedProductionPhaseMatrix) {
    assert(errors, sameJson(productionPhaseMatrixPayload, generatedProductionPhaseMatrix), 'production phase matrix stale against generator dry-run')
  }
  if (generatedGameplayCatalog) {
    assert(errors, sameJson(gameplayCatalogPayload, generatedGameplayCatalog), 'gameplay catalog stale against generator dry-run')
  }
  const holomapPayload = readJson(path.join(dataRoot, 'holomap', 'mvp_regions.json'))
  const soundContractPayload = readJson(path.join(dataRoot, 'sounds', 'mvp_sound_contract.json'))
  const soundsPayload = readJson(path.join(assetsRoot, 'sounds.json'))
  const assetManifestPayload = readJson(path.join(assetsRoot, 'asset_manifest.json'))
  const langPayload = readJson(path.join(assetsRoot, 'lang', 'en_us.json'))
  const systemPayloads = {
    homestead_alpha: readJson(path.join(dataRoot, 'systems', 'homestead_alpha.json')),
    builder_ux_alpha: readJson(path.join(dataRoot, 'systems', 'builder_ux_alpha.json')),
    cross_platform_parity: readJson(path.join(dataRoot, 'systems', 'cross_platform_parity.json')),
    playable_runtime_contract: readJson(path.join(dataRoot, 'systems', 'playable_runtime_contract.json')),
    runtime_adapter_load_plan: readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json')),
    runtime_execution_acceptance: readJson(path.join(dataRoot, 'systems', 'runtime_execution_acceptance.json')),
    runtime_execution_harness_plan: readJson(path.join(dataRoot, 'systems', 'runtime_execution_harness_plan.json')),
    harness_driver_manifest_contract: readJson(path.join(dataRoot, 'systems', 'harness_driver_manifest_contract.json')),
    legal_content_audit: readJson(path.join(dataRoot, 'systems', 'legal_content_audit.json')),
    launcher_flow_acceptance: readJson(path.join(dataRoot, 'systems', 'launcher_flow_acceptance.json')),
    launcher_execution_acceptance: readJson(path.join(dataRoot, 'systems', 'launcher_execution_acceptance.json')),
    launcher_execution_harness_plan: readJson(path.join(dataRoot, 'systems', 'launcher_execution_harness_plan.json')),
    final_release_review_acceptance: readJson(path.join(dataRoot, 'systems', 'final_release_review_acceptance.json')),
    final_release_review_harness_plan: readJson(path.join(dataRoot, 'systems', 'final_release_review_harness_plan.json')),
    distribution_approval_acceptance: readJson(path.join(dataRoot, 'systems', 'distribution_approval_acceptance.json')),
    distribution_approval_harness_plan: readJson(path.join(dataRoot, 'systems', 'distribution_approval_harness_plan.json')),
    release_publication_manifest_contract: readJson(path.join(dataRoot, 'systems', 'release_publication_manifest_contract.json')),
    distribution_alpha_gates: readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json')),
    coop_and_smp: readJson(path.join(dataRoot, 'systems', 'coop_and_smp.json')),
  }

  assert(errors, descriptor.id === MODULE_ID, 'descriptor id must be echoopenlandsprotocol')
  assert(errors, descriptor.version === conformance.version, 'descriptor version must match conformance version')
  assert(errors, descriptor.role === 'official_pack', 'descriptor role must be official_pack')
  assert(errors, descriptor.kind === 'pack_root', 'descriptor kind must be pack_root')
  assert(errors, descriptor.official === true, 'descriptor official must be true')
  assert(errors, descriptor.standalone === true, 'descriptor standalone must be true')
  assert(errors, runtimeContractSource.includes('public static final String STANDARD_MODE = "openlands_standard"'), 'OpenlandsRuntimeContracts must expose openlands_standard')
  assert(errors, runtimeContractSource.includes('public static final List<OpenlandsContractResource> CONTRACT_RESOURCES'), 'OpenlandsRuntimeContracts must expose CONTRACT_RESOURCES')
  assert(errors, runtimeContractSource.includes('public static final List<OpenlandsAdapterLoadStep> ADAPTER_LOAD_STEPS'), 'OpenlandsRuntimeContracts must expose ADAPTER_LOAD_STEPS')
  assert(errors, runtimeContractSource.includes('public static final List<OpenlandsRuntimeEvidence> RUNTIME_EVIDENCE_REQUIREMENTS'), 'OpenlandsRuntimeContracts must expose RUNTIME_EVIDENCE_REQUIREMENTS')
  assert(errors, runtimeContractSource.includes('systems/playable_runtime_contract'), 'OpenlandsRuntimeContracts must expose systems/playable_runtime_contract')
  assert(errors, runtimeContractSource.includes('systems/runtime_execution_acceptance'), 'OpenlandsRuntimeContracts must expose systems/runtime_execution_acceptance')
  assert(errors, runtimeContractSource.includes('systems/runtime_execution_harness_plan'), 'OpenlandsRuntimeContracts must expose systems/runtime_execution_harness_plan')
  assert(errors, runtimeContractSource.includes('systems/harness_driver_manifest_contract'), 'OpenlandsRuntimeContracts must expose systems/harness_driver_manifest_contract')
  assert(errors, runtimeContractSource.includes('systems/launcher_execution_acceptance'), 'OpenlandsRuntimeContracts must expose systems/launcher_execution_acceptance')
  assert(errors, runtimeContractSource.includes('systems/launcher_execution_harness_plan'), 'OpenlandsRuntimeContracts must expose systems/launcher_execution_harness_plan')
  assert(errors, runtimeContractSource.includes('systems/final_release_review_acceptance'), 'OpenlandsRuntimeContracts must expose systems/final_release_review_acceptance')
  assert(errors, runtimeContractSource.includes('systems/final_release_review_harness_plan'), 'OpenlandsRuntimeContracts must expose systems/final_release_review_harness_plan')
  assert(errors, runtimeContractSource.includes('systems/distribution_approval_acceptance'), 'OpenlandsRuntimeContracts must expose systems/distribution_approval_acceptance')
  assert(errors, runtimeContractSource.includes('systems/distribution_approval_harness_plan'), 'OpenlandsRuntimeContracts must expose systems/distribution_approval_harness_plan')
  assert(errors, runtimeContractSource.includes('systems/release_publication_manifest_contract'), 'OpenlandsRuntimeContracts must expose systems/release_publication_manifest_contract')
  assert(errors, runtimeContractSource.includes('index/mvp_gameplay_catalog'), 'OpenlandsRuntimeContracts must expose index/mvp_gameplay_catalog')
  assert(errors, runtimeContractSource.includes('progression/production_phase_matrix'), 'OpenlandsRuntimeContracts must expose progression/production_phase_matrix')
  assert(errors, runtimeContractSource.includes('OpenlandsFirstHourRuntime.adapterBindingManifest()'), 'OpenlandsRuntimeContracts must expose OpenlandsFirstHourRuntime adapter binding manifest')
  for (const runtime of EXPECTED_RUNTIMES) {
    assert(errors, runtimeContractSource.includes(`"${runtime}"`), `OpenlandsRuntimeContracts missing runtime ${runtime}`)
  }
  for (const phase of EXPECTED_LOAD_PHASES) {
    assert(errors, runtimeContractSource.includes(`"${phase}"`), `OpenlandsRuntimeContracts missing adapter load phase ${phase}`)
  }
  for (const evidence of EXPECTED_RUNTIME_EVIDENCE) {
    assert(errors, runtimeContractSource.includes(`"${evidence}"`), `OpenlandsRuntimeContracts missing runtime evidence ${evidence}`)
  }
  for (const runtime of EXPECTED_RUNTIMES) {
    assert(errors, descriptor.access?.adapterCore?.runtimes?.includes(runtime), `descriptor adapter runtimes missing ${runtime}`)
  }
  for (const provided of ['openlands.blocks', 'openlands.index', 'openlands.items', 'openlands.playtests', 'openlands.progression', 'openlands.recipes', 'openlands.systems', 'openlands.waystones', 'openlands.world']) {
    assert(errors, descriptor.provides?.includes(provided), `descriptor provides missing ${provided}`)
  }

  for (const root of conformance.requiredContentRoots) {
    assert(errors, fileExists(path.join(dataRoot, root)), `required content root missing ${root}`)
  }
  for (const root of conformance.requiredAssetRoots) {
    assert(errors, fileExists(path.join(assetsRoot, root)), `required asset root missing ${root}`)
  }

  for (const [label, payload] of Object.entries({
    blocks: blocksPayload,
    items: itemsPayload,
    recipes: recipesPayload,
    biomes: biomesPayload,
    structures: structuresPayload,
    creatures: creaturesPayload,
    waystones: waystonesPayload,
    playtests: playtestPayload,
    gameplay_catalog: gameplayCatalogPayload,
    holomap: holomapPayload,
    sounds: soundContractPayload,
    launch_roadmap: launchRoadmapPayload,
    production_phase_matrix: productionPhaseMatrixPayload,
    ...systemPayloads,
  })) {
    requireRuntimeParity(errors, payload, label)
  }

  const runtimeAdapter = systemPayloads.runtime_adapter_load_plan
  const runtimeAdapterPhases = runtimeAdapter.phases ?? []
  const phaseIds = runtimeAdapterPhases.map((phase) => phase.id)
  assert(errors, JSON.stringify(phaseIds) === JSON.stringify(EXPECTED_LOAD_PHASES), `runtime adapter phases must be ${EXPECTED_LOAD_PHASES.join(' -> ')}`)
  for (const phase of runtimeAdapterPhases) {
    requireFields(errors, phase, ['id', 'order', 'gate', 'description'], `runtime adapter phase ${phase.id}`)
    assert(errors, Number.isInteger(phase.order), `runtime adapter phase ${phase.id} order must be an integer`)
  }
  const phaseIdSet = new Set(phaseIds)
  const evidenceIds = new Set((runtimeAdapter.runtimeEvidenceRequirements ?? []).map((entry) => entry.id))
  for (const expected of EXPECTED_RUNTIME_EVIDENCE) {
    assert(errors, evidenceIds.has(expected), `runtime adapter evidence missing ${expected}`)
  }
  for (const evidence of runtimeAdapter.runtimeEvidenceRequirements ?? []) {
    requireFields(errors, evidence, ['id', 'category', 'requiredForPublicAlpha', 'successCriteria', 'failureAction'], `runtime adapter evidence ${evidence.id}`)
    assert(errors, evidence.requiredForPublicAlpha === true, `runtime adapter evidence ${evidence.id} must be required for Public Alpha`)
  }
  const loadSteps = runtimeAdapter.loadSteps ?? []
  assert(errors, loadSteps.length >= EXPECTED_LOAD_PHASES.length, `runtime adapter load plan should have at least ${EXPECTED_LOAD_PHASES.length} steps`)
  for (const step of loadSteps) {
    requireFields(errors, step, ['id', 'phase', 'summary', 'resourceIds', 'runtimeTargets', 'requiredEvidence', 'successSignal', 'failurePolicy'], `runtime adapter load step ${step.id}`)
    assert(errors, phaseIdSet.has(step.phase), `runtime adapter load step ${step.id} references unknown phase ${step.phase}`)
    requireRuntimeParity(errors, step, `runtime adapter load step ${step.id}`)
    assert(errors, Array.isArray(step.resourceIds) && step.resourceIds.length > 0, `runtime adapter load step ${step.id} must list resourceIds`)
    assert(errors, Array.isArray(step.requiredEvidence) && step.requiredEvidence.length > 0, `runtime adapter load step ${step.id} must list requiredEvidence`)
    for (const evidence of step.requiredEvidence ?? []) {
      assert(errors, evidenceIds.has(evidence), `runtime adapter load step ${step.id} references unknown evidence ${evidence}`)
    }
  }
  for (const phaseId of EXPECTED_LOAD_PHASES) {
    assert(errors, loadSteps.some((step) => step.phase === phaseId), `runtime adapter load plan missing step for phase ${phaseId}`)
  }
  const worldAndSystemLoadStep = loadSteps.find((step) => step.id === 'load_world_and_system_payloads')
  for (const resourceId of ['index/openlands_overview', 'index/mvp_gameplay_catalog', 'progression/production_phase_matrix', 'systems/runtime_execution_acceptance', 'systems/runtime_execution_harness_plan', 'systems/harness_driver_manifest_contract', 'systems/launcher_execution_acceptance', 'systems/launcher_execution_harness_plan', 'systems/final_release_review_acceptance', 'systems/final_release_review_harness_plan', 'systems/distribution_approval_acceptance', 'systems/distribution_approval_harness_plan', 'systems/release_publication_manifest_contract']) {
    assert(errors, worldAndSystemLoadStep?.resourceIds?.includes(resourceId), `runtime adapter world/system load step missing ${resourceId}`)
  }
  for (const gate of runtimeAdapter.acceptanceGates ?? []) {
    requireFields(errors, gate, ['id', 'requiresPhases', 'requiresEvidence'], `runtime adapter acceptance gate ${gate.id}`)
    for (const phase of gate.requiresPhases ?? []) {
      assert(errors, phaseIdSet.has(phase), `runtime adapter acceptance gate ${gate.id} references unknown phase ${phase}`)
    }
    for (const evidence of gate.requiresEvidence ?? []) {
      assert(errors, evidenceIds.has(evidence), `runtime adapter acceptance gate ${gate.id} references unknown evidence ${evidence}`)
    }
  }

  const blocks = blocksPayload.blocks ?? []
  const items = itemsPayload.items ?? []
  const recipes = recipesPayload.recipes ?? []
  const biomes = biomesPayload.biomes ?? []
  const structures = structuresPayload.landmarks ?? []
  const creatures = creaturesPayload.creatures ?? []
  const blockById = new Map(blocks.map((block) => [normalizeId(block.id), block]))
  const itemById = new Map(items.map((item) => [normalizeId(item.id), item]))
  const blockIds = new Set(blocks.map((block) => normalizeId(block.id)))
  const itemIds = new Set(items.map((item) => normalizeId(item.id)))
  const recipeIds = new Set(recipes.map((recipe) => normalizeId(recipe.id)))
  const biomeIds = new Set(biomes.map((biome) => normalizeId(biome.id)))
  const creatureIds = new Set(creatures.map((creature) => normalizeId(creature.id)))
  const stationIds = new Set((recipesPayload.stations ?? []).map((station) => station.id))
  const landmarkIds = new Set(structures.map((structure) => normalizeId(structure.id)))
  const tutorialPromptIds = new Set((tutorialsPayload.prompts ?? []).map((prompt) => prompt.id))
  const holomapLayerIds = new Set((holomapPayload.layers ?? []).map((layer) => layer.id))
  const holomapHintTypeIds = new Set((holomapPayload.hintTypes ?? []).map((hintType) => hintType.id))

  assert(errors, blocks.length >= 50, `expected at least 50 blocks, found ${blocks.length}`)
  assert(errors, items.length >= 45, `expected at least 45 items, found ${items.length}`)
  assert(errors, recipes.length >= 35, `expected at least 35 recipes, found ${recipes.length}`)
  assert(errors, biomes.length >= 4, `expected at least 4 biomes, found ${biomes.length}`)
  assert(errors, creatures.length >= 10, `expected at least 10 creatures, found ${creatures.length}`)

  assert(errors, overviewPayload.schema === 'echo.openlands.index.v1', 'Openlands overview schema mismatch')
  assert(errors, overviewPayload.publicName === 'Openlands', 'Openlands overview public name must be Openlands')
  assert(errors, overviewPayload.positioning?.defaultExperience === 'Openlands Standard', 'Openlands overview must name Openlands Standard as default')
  assert(errors, overviewPayload.positioning?.notACloneRule?.includes('Do not use Minecraft branding'), 'Openlands overview must preserve no-clone rule')
  assert(errors, (overviewPayload.indexCategories ?? []).some((category) => category.id === 'old_roads'), 'Openlands overview must include old_roads index category')
  assert(errors, (overviewPayload.mvpAcceptance ?? []).includes('Player can spawn in a safe start area.'), 'Openlands overview MVP acceptance must include safe spawn')

  const productionPhases = productionPhaseMatrixPayload.phases ?? []
  const productionCheckpoints = productionPhases.flatMap((phase) => phase.checkpoints ?? [])
  const productionEvidence = productionCheckpoints.flatMap((checkpoint) => checkpoint.evidence ?? [])
  assert(errors, productionPhaseMatrixPayload.schema === 'echo.openlands.progression.production_phase_matrix.v1', 'production phase matrix schema mismatch')
  assert(errors, productionPhaseMatrixPayload.namespace === MODULE_ID, 'production phase matrix namespace mismatch')
  assert(errors, productionPhaseMatrixPayload.version === descriptor.version, 'production phase matrix version must match descriptor')
  assert(errors, productionPhaseMatrixPayload.launchRoadmapDefaultRule === launchRoadmapPayload.defaultRule, 'production phase matrix default rule must match launch roadmap')
  assert(errors, productionPhaseMatrixPayload.counts?.phases === 11, 'production phase matrix must have 11 phases')
  assert(errors, productionPhaseMatrixPayload.counts?.checkpoints === 55, 'production phase matrix must have 55 checkpoints')
  assert(errors, productionPhases.length === 11, 'production phase matrix phases array must have 11 entries')
  assert(errors, productionCheckpoints.length === 55, 'production phase matrix checkpoints array must have 55 entries')
  assert(errors, productionPhaseMatrixPayload.currentRegistryCounts?.blocks === blocks.length, 'production phase matrix block count must match registry')
  assert(errors, productionPhaseMatrixPayload.currentRegistryCounts?.items === items.length, 'production phase matrix item count must match registry')
  assert(errors, productionPhaseMatrixPayload.currentRegistryCounts?.recipes === recipes.length, 'production phase matrix recipe count must match registry')
  assert(errors, productionPhaseMatrixPayload.currentRegistryCounts?.biomes === biomes.length, 'production phase matrix biome count must match registry')
  assert(errors, productionPhaseMatrixPayload.currentRegistryCounts?.structures === structures.length, 'production phase matrix structure count must match registry')
  assert(errors, productionPhaseMatrixPayload.currentRegistryCounts?.creatures === creatures.length, 'production phase matrix creature count must match registry')
  for (const source of ['README.md', 'META-INF/echo.mod.json', 'config/game_modes.json', 'blocks/mvp_blocks.json', 'items/mvp_items.json', 'progression/launch_roadmap.json', 'index/mvp_gameplay_catalog.json', 'systems/runtime_execution_acceptance.json', 'systems/runtime_execution_harness_plan.json', 'systems/harness_driver_manifest_contract.json', 'systems/launcher_execution_acceptance.json', 'systems/launcher_execution_harness_plan.json', 'systems/final_release_review_acceptance.json', 'systems/final_release_review_harness_plan.json', 'systems/distribution_approval_acceptance.json', 'systems/distribution_approval_harness_plan.json', 'systems/release_publication_manifest_contract.json']) {
    assert(errors, productionPhaseMatrixPayload.generatedFrom?.includes(source), `production phase matrix generatedFrom missing ${source}`)
  }
  for (const rule of [
    'The production matrix must keep all ten production phases plus the final launch phase visible as 55 checkable subphases.',
    'Openlands Standard remains relaxed; blocked runtime work must not be reclassified as complete.',
    'Every checkpoint must list concrete module, edition, artifact, or runtime-gate evidence.',
    'Echo-owned data IDs remain the source of truth for Native, Standalone, and NeoForge adapters.',
  ]) {
    assert(errors, productionPhaseMatrixPayload.designRules?.includes(rule), `production phase matrix design rule missing ${rule}`)
  }
  assert(errors, JSON.stringify(productionPhases.map((phase) => phase.id)) === JSON.stringify(EXPECTED_PRODUCTION_PHASES), 'production phase matrix phase order must match production plan')
  for (const phase of productionPhases) {
    requireFields(errors, phase, ['id', 'order', 'displayName', 'objective', 'checkpointCount', 'checkpoints'], `production phase ${phase.id}`)
    assert(errors, phase.checkpointCount === 5, `production phase ${phase.id} must declare 5 checkpoints`)
    assert(errors, (phase.checkpoints ?? []).length === 5, `production phase ${phase.id} must contain 5 checkpoints`)
    assert(errors, phase.order === EXPECTED_PRODUCTION_PHASES.indexOf(phase.id) + 1, `production phase ${phase.id} order mismatch`)
    assert(errors, JSON.stringify((phase.checkpoints ?? []).map((checkpoint) => checkpoint.id)) === JSON.stringify(EXPECTED_PRODUCTION_CHECKPOINTS[phase.id]), `production phase ${phase.id} checkpoint order mismatch`)
    for (const checkpoint of phase.checkpoints ?? []) {
      requireFields(errors, checkpoint, ['id', 'order', 'title', 'requirement', 'currentState', 'runtimeParity', 'evidence', 'acceptance'], `production checkpoint ${checkpoint.id}`)
      assert(errors, checkpoint.order === (phase.checkpoints ?? []).indexOf(checkpoint) + 1, `production checkpoint ${checkpoint.id} order mismatch`)
      requireRuntimeParity(errors, checkpoint, `production checkpoint ${checkpoint.id}`)
      assert(errors, Array.isArray(checkpoint.evidence) && checkpoint.evidence.length > 0, `production checkpoint ${checkpoint.id} must list evidence`)
      assert(errors, Array.isArray(checkpoint.acceptance) && checkpoint.acceptance.length > 0, `production checkpoint ${checkpoint.id} must list acceptance rules`)
      const hasRuntimeGate = (checkpoint.evidence ?? []).some((evidence) => evidence.kind === 'runtime_gate')
      assert(errors, !hasRuntimeGate || checkpoint.currentState !== 'contract_ready', `production checkpoint ${checkpoint.id} has a runtime gate and must not be marked contract_ready`)
    }
  }
  let presentProductionEvidence = 0
  let missingProductionEvidence = 0
  for (const evidence of productionEvidence) {
    requireFields(errors, evidence, ['kind', 'proves'], `production evidence ${evidence.path ?? evidence.id}`)
    assert(errors, ALLOWED_PRODUCTION_EVIDENCE_KINDS.includes(evidence.kind), `production evidence ${evidence.path ?? evidence.id} has unknown kind ${evidence.kind}`)
    if (evidence.kind === 'runtime_gate') {
      requireFields(errors, evidence, ['id'], `production runtime gate ${evidence.id}`)
    } else {
      requireFields(errors, evidence, ['path'], `production evidence ${evidence.kind}`)
    }
    const actualPresent = productionEvidenceExists(evidence, moduleRoot)
    if (actualPresent) presentProductionEvidence += 1
    else missingProductionEvidence += 1
    assert(errors, evidence.present === actualPresent, `production evidence ${evidence.path ?? evidence.id} present flag is stale`)
    assert(errors, actualPresent, `production evidence missing ${evidence.path ?? evidence.id}`)
  }
  assert(errors, productionPhaseMatrixPayload.counts?.moduleEvidence === productionEvidence.filter((entry) => entry.kind === 'module_file').length, 'production phase matrix module evidence count mismatch')
  assert(errors, productionPhaseMatrixPayload.counts?.editionEvidence === productionEvidence.filter((entry) => entry.kind === 'edition_file').length, 'production phase matrix edition evidence count mismatch')
  assert(errors, productionPhaseMatrixPayload.counts?.artifactEvidence === productionEvidence.filter((entry) => entry.kind === 'artifact_file').length, 'production phase matrix artifact evidence count mismatch')
  assert(errors, productionPhaseMatrixPayload.counts?.runtimeGates === productionEvidence.filter((entry) => entry.kind === 'runtime_gate').length, 'production phase matrix runtime gate count mismatch')
  assert(errors, productionPhaseMatrixPayload.counts?.presentEvidence === presentProductionEvidence, 'production phase matrix present evidence count mismatch')
  assert(errors, productionPhaseMatrixPayload.counts?.missingEvidence === missingProductionEvidence, 'production phase matrix missing evidence count mismatch')

  const runtimeExecution = systemPayloads.runtime_execution_acceptance
  const productionRuntimeGateIds = sortedUnique(productionEvidence
    .filter((entry) => entry.kind === 'runtime_gate')
    .map((entry) => entry.id))
  const runtimeExecutionGateIds = sortedUnique((runtimeExecution.runtimeGates ?? []).map((gate) => gate.id))
  const executionSuites = runtimeExecution.executionSuites ?? []
  const executionScenarios = runtimeExecution.scenarios ?? []
  const executionSuiteIds = new Set(executionSuites.map((suite) => suite.id))
  const executionScenarioIds = new Set(executionScenarios.map((scenario) => scenario.id))
  const executionGateIdsFromScenarios = sortedUnique(executionScenarios.flatMap((scenario) => scenario.gateIds ?? []))
  assert(errors, runtimeExecution.schema === 'echo.openlands.systems.runtime_execution_acceptance.v1', 'runtime execution acceptance schema mismatch')
  assert(errors, runtimeExecution.namespace === MODULE_ID, 'runtime execution acceptance namespace mismatch')
  assert(errors, runtimeExecution.version === descriptor.version, 'runtime execution acceptance version must match descriptor')
  assertSameStringSet(errors, runtimeExecutionGateIds, productionRuntimeGateIds, 'runtime execution acceptance gates')
  assertSameStringSet(errors, executionGateIdsFromScenarios, productionRuntimeGateIds, 'runtime execution acceptance scenario gate coverage')
  assert(errors, runtimeExecution.sourceContracts?.productionPhaseMatrix === 'progression/production_phase_matrix.json', 'runtime execution sourceContracts production phase matrix mismatch')
  assert(errors, runtimeExecution.sourceContracts?.runtimeAdapterLoadPlan === 'systems/runtime_adapter_load_plan.json', 'runtime execution sourceContracts runtime adapter load plan mismatch')
  assert(errors, runtimeExecution.sourceContracts?.playableRuntimeContract === 'systems/playable_runtime_contract.json', 'runtime execution sourceContracts playable runtime mismatch')
  assert(errors, runtimeExecution.sourceContracts?.firstHourPlaytest === 'playtests/mvp_first_hour_acceptance.json', 'runtime execution sourceContracts first-hour playtest mismatch')
  assert(errors, runtimeExecution.reportContract?.schema === 'echo.openlands.edition.runtime_execution_report.v1', 'runtime execution report schema mismatch')
  for (const status of ['passed', 'failed', 'blocked']) {
    assert(errors, runtimeExecution.reportContract?.allowedReportStatus?.includes(status), `runtime execution allowed report status missing ${status}`)
  }
  for (const field of ['runtimeTarget', 'moduleArtifactSha256', 'scenarioResults', 'clearedRuntimeGates', 'remainingRuntimeGates', 'publicAlphaReady']) {
    assert(errors, runtimeExecution.reportContract?.requiredReportFields?.includes(field), `runtime execution report required fields missing ${field}`)
  }
  for (const status of ['passed', 'failed', 'blocked', 'skipped']) {
    assert(errors, runtimeExecution.reportContract?.allowedScenarioStatus?.includes(status), `runtime execution allowed scenario status missing ${status}`)
    assert(errors, runtimeExecution.reportContract?.allowedAssertionStatus?.includes(status), `runtime execution allowed assertion status missing ${status}`)
  }
  for (const field of ['id', 'status']) {
    assert(errors, runtimeExecution.reportContract?.requiredAssertionFields?.includes(field), `runtime execution required assertion field missing ${field}`)
  }
  for (const report of runtimeExecution.editionReports ?? []) {
    requireFields(errors, report, ['edition', 'runtimeTarget', 'repo', 'requiredReport', 'artifactPattern'], `runtime execution edition report ${report.edition}`)
    assert(errors, EXPECTED_RUNTIMES.includes(report.runtimeTarget), `runtime execution edition report ${report.edition} runtime target mismatch`)
    assert(errors, report.requiredReport?.startsWith(`evidence/${report.edition}-runtime-execution-report.json`), `runtime execution edition report ${report.edition} required report path mismatch`)
  }
  assert(errors, (runtimeExecution.editionReports ?? []).length === EXPECTED_RUNTIMES.length, 'runtime execution acceptance must declare one report per runtime')
  for (const gate of runtimeExecution.runtimeGates ?? []) {
    requireFields(errors, gate, ['id', 'suiteId', 'clearsEvidence', 'mustCapture'], `runtime execution gate ${gate.id}`)
    assert(errors, executionSuiteIds.has(gate.suiteId), `runtime execution gate ${gate.id} references unknown suite ${gate.suiteId}`)
    assert(errors, Array.isArray(gate.mustCapture) && gate.mustCapture.length > 0, `runtime execution gate ${gate.id} must list captured fields`)
    for (const evidence of gate.clearsEvidence ?? []) {
      assert(errors, evidenceIds.has(evidence), `runtime execution gate ${gate.id} clears unknown evidence ${evidence}`)
    }
  }
  for (const suite of executionSuites) {
    requireFields(errors, suite, ['id', 'displayName', 'requiredFor', 'minimumRunsPerRuntime', 'scenarioIds'], `runtime execution suite ${suite.id}`)
    assert(errors, Number.isInteger(suite.minimumRunsPerRuntime) && suite.minimumRunsPerRuntime > 0, `runtime execution suite ${suite.id} minimumRunsPerRuntime must be positive`)
    assert(errors, Array.isArray(suite.scenarioIds) && suite.scenarioIds.length > 0, `runtime execution suite ${suite.id} must list scenarios`)
    for (const scenarioId of suite.scenarioIds ?? []) {
      assert(errors, executionScenarioIds.has(scenarioId), `runtime execution suite ${suite.id} references unknown scenario ${scenarioId}`)
    }
  }
  for (const scenario of executionScenarios) {
    requireFields(errors, scenario, ['id', 'suiteId', 'gateIds', 'inputFixtureRefs', 'actions', 'assertions'], `runtime execution scenario ${scenario.id}`)
    assert(errors, executionSuiteIds.has(scenario.suiteId), `runtime execution scenario ${scenario.id} references unknown suite ${scenario.suiteId}`)
    assert(errors, Array.isArray(scenario.gateIds) && scenario.gateIds.length > 0, `runtime execution scenario ${scenario.id} must list gateIds`)
    assert(errors, Array.isArray(scenario.actions) && scenario.actions.length > 0, `runtime execution scenario ${scenario.id} must list actions`)
    assert(errors, Array.isArray(scenario.assertions) && scenario.assertions.length > 0, `runtime execution scenario ${scenario.id} must list assertions`)
    for (const gateId of scenario.gateIds ?? []) {
      assert(errors, productionRuntimeGateIds.includes(gateId), `runtime execution scenario ${scenario.id} references unknown runtime gate ${gateId}`)
    }
  }
  assert(errors, runtimeExecution.publicAlphaClearance?.requiresEveryEditionReport === true, 'runtime execution acceptance must require every edition report')
  assert(errors, runtimeExecution.publicAlphaClearance?.requiresEveryRuntimeGateCleared === true, 'runtime execution acceptance must require every runtime gate cleared')
  assert(errors, runtimeExecution.publicAlphaClearance?.preflightReportsDoNotClearRuntimeGates === true, 'runtime execution acceptance must preserve preflight/runtime distinction')

  const runtimeHarness = systemPayloads.runtime_execution_harness_plan
  const harnessScenarioBindings = runtimeHarness.scenarioBindings ?? []
  const harnessScenarioById = new Map(harnessScenarioBindings.map((scenario) => [scenario.id, scenario]))
  const harnessDriverIds = new Set((runtimeHarness.driverSurfaces ?? []).map((driver) => driver.id))
  const runtimeExecutionScenarioById = new Map(executionScenarios.map((scenario) => [scenario.id, scenario]))
  assert(errors, runtimeHarness.schema === 'echo.openlands.systems.runtime_execution_harness_plan.v1', 'runtime execution harness plan schema mismatch')
  assert(errors, runtimeHarness.namespace === MODULE_ID, 'runtime execution harness plan namespace mismatch')
  assert(errors, runtimeHarness.version === descriptor.version, 'runtime execution harness plan version must match descriptor')
  assert(errors, runtimeHarness.sourceContracts?.runtimeExecutionAcceptance === 'systems/runtime_execution_acceptance.json', 'runtime execution harness sourceContracts runtime execution mismatch')
  assert(errors, runtimeHarness.sourceContracts?.runtimeAdapterLoadPlan === 'systems/runtime_adapter_load_plan.json', 'runtime execution harness sourceContracts runtime adapter load plan mismatch')
  assert(errors, runtimeHarness.sourceContracts?.playableRuntimeContract === 'systems/playable_runtime_contract.json', 'runtime execution harness sourceContracts playable runtime mismatch')
  assert(errors, runtimeHarness.sourceContracts?.launcherExecutionAcceptance === 'systems/launcher_execution_acceptance.json', 'runtime execution harness sourceContracts launcher execution mismatch')
  assert(errors, runtimeHarness.sourceContracts?.finalReleaseReviewAcceptance === 'systems/final_release_review_acceptance.json', 'runtime execution harness sourceContracts final release review mismatch')
  assert(errors, runtimeHarness.sourceContracts?.distributionApprovalAcceptance === 'systems/distribution_approval_acceptance.json', 'runtime execution harness sourceContracts distribution approval mismatch')
  assertSameStringSet(errors, (runtimeHarness.editionHarnesses ?? []).map((entry) => entry.runtimeTarget), EXPECTED_RUNTIMES, 'runtime execution harness edition runtime targets')
  assertSameStringSet(errors, harnessScenarioBindings.map((scenario) => scenario.id), executionScenarios.map((scenario) => scenario.id), 'runtime execution harness scenario coverage')
  assert(errors, (runtimeHarness.driverSurfaces ?? []).length >= 12, 'runtime execution harness must define at least twelve driver surfaces')
  for (const editionHarness of runtimeHarness.editionHarnesses ?? []) {
    requireFields(errors, editionHarness, ['edition', 'runtimeTarget', 'repo', 'driverKind', 'entryPoint', 'requiredReport', 'artifactPattern', 'runtimeArtifactRoot', 'mustBootWith'], `runtime execution harness edition ${editionHarness.edition}`)
    assert(errors, EXPECTED_RUNTIMES.includes(editionHarness.runtimeTarget), `runtime execution harness edition ${editionHarness.edition} runtime target mismatch`)
    assert(errors, editionHarness.entryPoint === 'scripts/run-runtime-execution-harness.mjs', `runtime execution harness edition ${editionHarness.edition} entry point mismatch`)
    const expectedReport = (runtimeExecution.editionReports ?? []).find((report) => report.edition === editionHarness.edition)
    assert(errors, expectedReport !== undefined, `runtime execution harness edition ${editionHarness.edition} missing matching runtime execution report`)
    if (expectedReport) {
      assert(errors, editionHarness.runtimeTarget === expectedReport.runtimeTarget, `runtime execution harness edition ${editionHarness.edition} runtime target must match acceptance`)
      assert(errors, editionHarness.repo === expectedReport.repo, `runtime execution harness edition ${editionHarness.edition} repo must match acceptance`)
      assert(errors, editionHarness.requiredReport === expectedReport.requiredReport, `runtime execution harness edition ${editionHarness.edition} report path must match acceptance`)
      assert(errors, editionHarness.artifactPattern === expectedReport.artifactPattern, `runtime execution harness edition ${editionHarness.edition} artifact pattern must match acceptance`)
    }
    assert(errors, typeof editionHarness.runtimeArtifactRoot === 'string' && editionHarness.runtimeArtifactRoot.startsWith(`evidence/runtime-execution/${editionHarness.edition}`), `runtime execution harness edition ${editionHarness.edition} runtime artifact root mismatch`)
    assert(errors, Array.isArray(editionHarness.mustBootWith) && editionHarness.mustBootWith.length >= 3, `runtime execution harness edition ${editionHarness.edition} must list boot requirements`)
  }
  for (const driver of runtimeHarness.driverSurfaces ?? []) {
    requireFields(errors, driver, ['id', 'requiredMethods', 'mustCapture'], `runtime execution harness driver ${driver.id}`)
    assert(errors, Array.isArray(driver.requiredMethods) && driver.requiredMethods.length > 0, `runtime execution harness driver ${driver.id} must list methods`)
    assert(errors, Array.isArray(driver.mustCapture) && driver.mustCapture.length > 0, `runtime execution harness driver ${driver.id} must list captured fields`)
  }
  for (const scenario of harnessScenarioBindings) {
    const expectedScenario = runtimeExecutionScenarioById.get(scenario.id)
    assert(errors, expectedScenario !== undefined, `runtime execution harness scenario ${scenario.id} missing acceptance scenario`)
    if (!expectedScenario) continue
    requireFields(errors, scenario, ['id', 'suiteId', 'gateIds', 'driverSurfaceIds', 'inputFixtureRefs', 'actions', 'assertions', 'requiredSavedArtifacts'], `runtime execution harness scenario ${scenario.id}`)
    assert(errors, scenario.suiteId === expectedScenario.suiteId, `runtime execution harness scenario ${scenario.id} suite mismatch`)
    assert(errors, JSON.stringify(scenario.gateIds ?? []) === JSON.stringify(expectedScenario.gateIds ?? []), `runtime execution harness scenario ${scenario.id} gate ids must match acceptance`)
    assert(errors, JSON.stringify(scenario.inputFixtureRefs ?? []) === JSON.stringify(expectedScenario.inputFixtureRefs ?? []), `runtime execution harness scenario ${scenario.id} fixtures must match acceptance`)
    assert(errors, JSON.stringify(scenario.actions ?? []) === JSON.stringify(expectedScenario.actions ?? []), `runtime execution harness scenario ${scenario.id} actions must match acceptance`)
    assert(errors, JSON.stringify(scenario.assertions ?? []) === JSON.stringify(expectedScenario.assertions ?? []), `runtime execution harness scenario ${scenario.id} assertions must match acceptance`)
    assert(errors, Array.isArray(scenario.driverSurfaceIds) && scenario.driverSurfaceIds.length > 0, `runtime execution harness scenario ${scenario.id} must list driver surfaces`)
    assert(errors, Array.isArray(scenario.requiredSavedArtifacts) && scenario.requiredSavedArtifacts.length > 0, `runtime execution harness scenario ${scenario.id} must list saved artifacts`)
    for (const driverId of scenario.driverSurfaceIds ?? []) {
      assert(errors, harnessDriverIds.has(driverId), `runtime execution harness scenario ${scenario.id} references unknown driver ${driverId}`)
    }
  }
  assert(errors, harnessScenarioById.has('first_hour_route_walkthrough'), 'runtime execution harness must include first-hour route walkthrough')
  assert(errors, harnessScenarioById.has('launcher_install_update_repair_rollback'), 'runtime execution harness must include launcher execution bridge')
  assert(errors, harnessScenarioById.has('final_owned_asset_review'), 'runtime execution harness must include final asset review bridge')
  assert(errors, runtimeHarness.reportAssemblyRules?.allActionsMustRun === true, 'runtime execution harness must require all actions to run')
  assert(errors, runtimeHarness.reportAssemblyRules?.allAssertionsMustPassForScenarioPass === true, 'runtime execution harness must require all assertions to pass')
  assert(errors, runtimeHarness.reportAssemblyRules?.gateClearsOnlyWhenEveryMappedScenarioPasses === true, 'runtime execution harness must require mapped scenarios to pass before clearing gates')
  assert(errors, runtimeHarness.reportAssemblyRules?.blockedHarnessDoesNotClearRuntimeGates === true, 'runtime execution harness must preserve blocked gate honesty')

  const gameplayBlockEntries = gameplayCatalogPayload.blockEntries ?? []
  const gameplayItemEntries = gameplayCatalogPayload.itemEntries ?? []
  const gameplayBlockById = new Map(gameplayBlockEntries.map((entry) => [entry.id, entry]))
  const gameplayItemById = new Map(gameplayItemEntries.map((entry) => [entry.id, entry]))
  assert(errors, gameplayCatalogPayload.schema === 'echo.openlands.gameplay_catalog.v1', 'gameplay catalog schema mismatch')
  assert(errors, gameplayCatalogPayload.namespace === MODULE_ID, 'gameplay catalog namespace mismatch')
  assert(errors, gameplayCatalogPayload.counts?.blocks === blocks.length, 'gameplay catalog block count must match registry')
  assert(errors, gameplayCatalogPayload.counts?.items === items.length, 'gameplay catalog item count must match registry')
  assert(errors, gameplayCatalogPayload.counts?.recipes === recipes.length, 'gameplay catalog recipe count must match registry')
  assert(errors, gameplayCatalogPayload.firstHourPromise === progressionPayload.playerPromise, 'gameplay catalog first-hour promise must match progression')
  assert(errors, gameplayCatalogPayload.roadmapDefaultRule === launchRoadmapPayload.defaultRule, 'gameplay catalog roadmap default rule must match launch roadmap')
  for (const source of ['blocks/mvp_blocks.json', 'items/mvp_items.json', 'recipes/mvp_recipes.json', 'loot/mvp_loot.json', 'progression/first_hour_route.json', 'progression/launch_roadmap.json']) {
    assert(errors, gameplayCatalogPayload.generatedFrom?.includes(source), `gameplay catalog generatedFrom missing ${source}`)
  }
  for (const rule of [
    'Every MVP block and item must have an acquisition path, gameplay role, player use, progression stage, and runtime parity note.',
    'Openlands Standard stays relaxed: food, shelter, and farming entries must avoid hardcore upkeep pressure.',
    'Echo IDs remain the source of truth across Native, Standalone, and NeoForge adapters.',
  ]) {
    assert(errors, gameplayCatalogPayload.designRules?.includes(rule), `gameplay catalog design rule missing ${rule}`)
  }
  assert(errors, JSON.stringify(gameplayBlockEntries.map((entry) => entry.id)) === JSON.stringify(blocks.map((block) => normalizeId(block.id))), 'gameplay catalog block order must match block registry')
  assert(errors, JSON.stringify(gameplayItemEntries.map((entry) => entry.id)) === JSON.stringify(items.map((item) => normalizeId(item.id))), 'gameplay catalog item order must match item registry')
  assertSameStringSet(errors, gameplayBlockEntries.map((entry) => entry.id), blocks.map((block) => normalizeId(block.id)), 'gameplay catalog block entries')
  assertSameStringSet(errors, gameplayItemEntries.map((entry) => entry.id), items.map((item) => normalizeId(item.id)), 'gameplay catalog item entries')
  for (const role of ['world_resource', 'building_palette', 'crafting_station', 'shelter_score', 'old_road_discovery', 'waystone_progression']) {
    assert(errors, gameplayCatalogPayload.roleCoverage?.blocks?.includes(role), `gameplay catalog block role coverage missing ${role}`)
  }
  for (const role of ['raw_material', 'food_and_comfort', 'tool_progression', 'waystone_and_map_progression', 'builder_quality_of_life']) {
    assert(errors, gameplayCatalogPayload.roleCoverage?.items?.includes(role), `gameplay catalog item role coverage missing ${role}`)
  }
  for (const stage of ['first_hour', 'first_tools', 'first_metal', 'homestead', 'old_roads', 'waystone_network']) {
    assert(errors, gameplayCatalogPayload.roleCoverage?.progressionStages?.includes(stage), `gameplay catalog progression coverage missing ${stage}`)
  }
  for (const entry of gameplayBlockEntries) {
    const block = blockById.get(entry.id)
    assert(errors, block !== undefined, `gameplay catalog block ${entry.id} is not in block registry`)
    if (!block) continue
    requireFields(errors, entry, ['id', 'namespacedId', 'displayName', 'category', 'progressionStage', 'gameplayRoles', 'acquisition', 'playerUse', 'tool', 'hardness', 'drops', 'tags', 'worldPlacement', 'crafting', 'systems', 'runtimeParity', 'runtimeNote', 'designNote'], `gameplay catalog block ${entry.id}`)
    requireRuntimeParity(errors, entry, `gameplay catalog block ${entry.id}`)
    assert(errors, entry.namespacedId === block.id, `gameplay catalog block ${entry.id} namespaced id mismatch`)
    assert(errors, entry.displayName === block.displayName, `gameplay catalog block ${entry.id} displayName mismatch`)
    assert(errors, entry.category === block.category, `gameplay catalog block ${entry.id} category mismatch`)
    assert(errors, entry.tool === block.tool, `gameplay catalog block ${entry.id} tool mismatch`)
    assert(errors, entry.hardness === block.hardness, `gameplay catalog block ${entry.id} hardness mismatch`)
    assert(errors, JSON.stringify(entry.tags ?? []) === JSON.stringify(block.tags ?? []), `gameplay catalog block ${entry.id} tags mismatch`)
    assert(errors, JSON.stringify(entry.drops ?? []) === JSON.stringify(block.drops ?? []), `gameplay catalog block ${entry.id} drops mismatch`)
    assert(errors, Array.isArray(entry.gameplayRoles) && entry.gameplayRoles.length > 0, `gameplay catalog block ${entry.id} must have gameplay roles`)
    assert(errors, Array.isArray(entry.acquisition) && entry.acquisition.length > 0, `gameplay catalog block ${entry.id} must have acquisition paths`)
    assert(errors, Array.isArray(entry.playerUse) && entry.playerUse.length > 0, `gameplay catalog block ${entry.id} must have player uses`)
    assert(errors, typeof entry.progressionStage === 'string' && entry.progressionStage.length > 0, `gameplay catalog block ${entry.id} must have progression stage`)
    assert(errors, entry.runtimeNote?.includes('Echo block id'), `gameplay catalog block ${entry.id} must name Echo block id runtime note`)
    assert(errors, entry.designNote === block.notes, `gameplay catalog block ${entry.id} design note mismatch`)
    assert(errors, JSON.stringify(entry.worldPlacement?.biomes ?? []) === JSON.stringify((block.biomePlacement ?? []).map(normalizeId)), `gameplay catalog block ${entry.id} biome placement mismatch`)
    assert(errors, JSON.stringify(entry.worldPlacement?.structures ?? []) === JSON.stringify(block.structurePlacement ?? []), `gameplay catalog block ${entry.id} structure placement mismatch`)
    assert(errors, (entry.crafting?.recipeSource ?? null) === (block.recipeSource ? normalizeRecipeRef(block.recipeSource) : null), `gameplay catalog block ${entry.id} recipe source mismatch`)
    for (const recipe of entry.crafting?.consumedByRecipes ?? []) {
      assert(errors, recipeIds.has(recipe), `gameplay catalog block ${entry.id} consumedByRecipes references unknown recipe ${recipe}`)
    }
    assert(errors, (entry.systems?.shelterScore ?? 0) === (block.shelterScore ?? 0), `gameplay catalog block ${entry.id} shelter score mismatch`)
    assert(errors, (entry.systems?.light ?? 0) === (block.light ?? 0), `gameplay catalog block ${entry.id} light mismatch`)
  }
  for (const entry of gameplayItemEntries) {
    const item = itemById.get(entry.id)
    assert(errors, item !== undefined, `gameplay catalog item ${entry.id} is not in item registry`)
    if (!item) continue
    requireFields(errors, entry, ['id', 'displayName', 'useType', 'progressionStage', 'gameplayRoles', 'acquisition', 'playerUse', 'stackSize', 'tags', 'recipeRefs', 'consumedByRecipes', 'runtimeParity', 'runtimeNote', 'designNote'], `gameplay catalog item ${entry.id}`)
    requireRuntimeParity(errors, entry, `gameplay catalog item ${entry.id}`)
    assert(errors, entry.displayName === item.displayName, `gameplay catalog item ${entry.id} displayName mismatch`)
    assert(errors, entry.useType === item.useType, `gameplay catalog item ${entry.id} useType mismatch`)
    assert(errors, entry.stackSize === item.stackSize, `gameplay catalog item ${entry.id} stackSize mismatch`)
    assert(errors, JSON.stringify(entry.tags ?? []) === JSON.stringify(item.tags ?? []), `gameplay catalog item ${entry.id} tags mismatch`)
    assert(errors, JSON.stringify(entry.recipeRefs ?? []) === JSON.stringify((item.recipeRefs ?? []).map(normalizeRecipeRef)), `gameplay catalog item ${entry.id} recipe refs mismatch`)
    assert(errors, Array.isArray(entry.gameplayRoles) && entry.gameplayRoles.length > 0, `gameplay catalog item ${entry.id} must have gameplay roles`)
    assert(errors, Array.isArray(entry.acquisition) && entry.acquisition.length > 0, `gameplay catalog item ${entry.id} must have acquisition paths`)
    assert(errors, Array.isArray(entry.playerUse) && entry.playerUse.length > 0, `gameplay catalog item ${entry.id} must have player uses`)
    assert(errors, typeof entry.progressionStage === 'string' && entry.progressionStage.length > 0, `gameplay catalog item ${entry.id} must have progression stage`)
    assert(errors, entry.runtimeNote?.includes('Echo item id'), `gameplay catalog item ${entry.id} must name Echo item id runtime note`)
    assert(errors, entry.designNote === item.notes, `gameplay catalog item ${entry.id} design note mismatch`)
    for (const recipe of entry.consumedByRecipes ?? []) {
      assert(errors, recipeIds.has(recipe), `gameplay catalog item ${entry.id} consumedByRecipes references unknown recipe ${recipe}`)
    }
    if (item.nutrition) {
      assert(errors, entry.nutrition?.hunger === item.nutrition.hunger, `gameplay catalog item ${entry.id} nutrition hunger mismatch`)
      assert(errors, entry.nutrition?.comfort === item.nutrition.comfort, `gameplay catalog item ${entry.id} nutrition comfort mismatch`)
    } else {
      assert(errors, entry.nutrition === null, `gameplay catalog item ${entry.id} nutrition should be null`)
    }
    if (item.toolStats) {
      assert(errors, entry.toolStats?.toolClass === item.toolStats.toolClass, `gameplay catalog item ${entry.id} tool class mismatch`)
      assert(errors, entry.toolStats?.tier === item.toolStats.tier, `gameplay catalog item ${entry.id} tool tier mismatch`)
    } else {
      assert(errors, entry.toolStats === null, `gameplay catalog item ${entry.id} toolStats should be null`)
    }
    if (item.placesBlock) {
      assert(errors, entry.placesBlock === item.placesBlock, `gameplay catalog item ${entry.id} placesBlock mismatch`)
      assert(errors, blockIds.has(item.placesBlock), `gameplay catalog item ${entry.id} places unknown block ${item.placesBlock}`)
    } else {
      assert(errors, entry.placesBlock === null, `gameplay catalog item ${entry.id} placesBlock should be null`)
    }
  }

  assert(errors, assetManifestPayload.status === 'owned_placeholder_coverage', 'asset manifest status must be owned_placeholder_coverage during MVP asset scaffolding')
  assert(errors, assetManifestPayload.publicReleaseAllowedWithPlaceholders === false, 'asset manifest must block public release with placeholders')
  assert(errors, assetManifestPayload.placeholderPolicy?.mustBeOriginal === true, 'asset manifest placeholder policy must require original Echo assets')
  assert(errors, assetManifestPayload.placeholderPolicy?.replacementGate === 'public_alpha_art_review', 'asset manifest placeholder policy must point to public_alpha_art_review')
  assert(errors, assetManifestPayload.pathTemplates?.blockstate === 'blockstates/{id}.json', 'asset manifest blockstate template mismatch')
  assert(errors, assetManifestPayload.pathTemplates?.blockModel === 'models/block/{id}.json', 'asset manifest block model template mismatch')
  assert(errors, assetManifestPayload.pathTemplates?.blockTexture === 'textures/block/{texture}.png', 'asset manifest block texture template mismatch')
  assert(errors, assetManifestPayload.pathTemplates?.itemModel === 'models/item/{id}.json', 'asset manifest item model template mismatch')
  assert(errors, assetManifestPayload.pathTemplates?.itemTexture === 'textures/item/{texture}.png', 'asset manifest item texture template mismatch')
  assertSameStringSet(errors, assetManifestPayload.mvpCoverage?.blockIds, blocks.map((block) => normalizeId(block.id)), 'asset manifest MVP block coverage')
  assertSameStringSet(errors, assetManifestPayload.mvpCoverage?.itemIds, items.map((item) => normalizeId(item.id)), 'asset manifest MVP item coverage')
  for (const block of blocks) {
    const id = normalizeId(block.id)
    assert(errors, typeof block.texture === 'string' && block.texture.startsWith('block/'), `block ${id} texture must be a block texture key`)
  }
  for (const item of items) {
    const id = normalizeId(item.id)
    assert(errors, typeof item.texture === 'string' && !item.texture.includes('/'), `item ${id} texture must be an item-local texture key`)
  }

  for (const expected of conformance.blockRegistry ?? []) {
    assert(errors, blockIds.has(expected), `conformance blockRegistry missing actual block ${expected}`)
  }
  for (const expected of conformance.itemRegistry ?? []) {
    assert(errors, itemIds.has(expected), `conformance itemRegistry missing actual item ${expected}`)
  }
  for (const expected of conformance.recipeRegistry ?? []) {
    assert(errors, recipeIds.has(expected), `conformance recipeRegistry missing actual recipe ${expected}`)
  }
  for (const expected of conformance.biomeRegistry ?? []) {
    assert(errors, biomeIds.has(expected), `conformance biomeRegistry missing actual biome ${expected}`)
  }
  for (const expected of conformance.creatureRegistry ?? []) {
    assert(errors, creatureIds.has(expected), `conformance creatureRegistry missing actual creature ${expected}`)
  }
  for (const expected of conformance.systemContracts ?? []) {
    assert(errors, systemPayloads[expected] !== undefined, `conformance systemContracts missing file ${expected}`)
  }
  assert(errors, (conformance.playtestFixtures ?? []).includes('mvp_first_hour_acceptance'), 'conformance playtestFixtures missing mvp_first_hour_acceptance')

  for (const block of blocks) {
    const id = normalizeId(block.id)
    requireFields(errors, block, ['id', 'displayName', 'hardness', 'tool', 'drops', 'tags', 'model', 'texture'], `block ${id}`)
    assert(errors, langPayload[`block.${MODULE_ID}.${id}`] === block.displayName, `lang key mismatch for block ${id}`)
    const blockstatePath = path.join(assetsRoot, 'blockstates', `${id}.json`)
    const blockModelPath = path.join(assetsRoot, 'models', 'block', `${id}.json`)
    const blockTexturePath = path.join(assetsRoot, 'textures', 'block', `${blockTextureKey(block)}.png`)
    assert(errors, fileExists(blockstatePath), `block ${id} missing blockstate asset ${path.relative(assetsRoot, blockstatePath)}`)
    assert(errors, fileExists(blockModelPath), `block ${id} missing block model asset ${path.relative(assetsRoot, blockModelPath)}`)
    assert(errors, isPngFile(blockTexturePath), `block ${id} missing valid PNG texture asset ${path.relative(assetsRoot, blockTexturePath)}`)
    if (fileExists(blockstatePath)) {
      const blockstate = readJson(blockstatePath)
      assert(errors, blockstate.variants?.['']?.model === `${MODULE_ID}:block/${id}`, `block ${id} blockstate should point to its Openlands block model`)
    }
    if (fileExists(blockModelPath)) {
      const model = readJson(blockModelPath)
      assert(errors, model.textures?.all === `${MODULE_ID}:block/${blockTextureKey(block)}`, `block ${id} model should point to declared Openlands block texture`)
      assert(errors, Array.isArray(model.elements) && model.elements.length > 0, `block ${id} model should define placeholder geometry`)
    }
    for (const drop of block.drops ?? []) {
      const dropId = normalizeId(drop.item ?? drop.block)
      if (!dropId) continue
      assert(errors, itemIds.has(dropId) || blockIds.has(dropId), `block ${id} drop references unknown id ${dropId}`)
    }
  }

  for (const item of items) {
    const id = normalizeId(item.id)
    requireFields(errors, item, ['id', 'displayName', 'stackSize', 'useType', 'tags', 'model', 'texture', 'recipeRefs'], `item ${id}`)
    assert(errors, Number.isInteger(item.stackSize) && item.stackSize >= 1, `item ${id} stackSize must be a positive integer`)
    assert(errors, langPayload[`item.${MODULE_ID}.${id}`] === item.displayName, `lang key mismatch for item ${id}`)
    const itemModelPath = path.join(assetsRoot, 'models', 'item', `${id}.json`)
    const itemTexturePath = path.join(assetsRoot, 'textures', 'item', `${itemTextureKey(item)}.png`)
    assert(errors, fileExists(itemModelPath), `item ${id} missing item model asset ${path.relative(assetsRoot, itemModelPath)}`)
    assert(errors, isPngFile(itemTexturePath), `item ${id} missing valid PNG texture asset ${path.relative(assetsRoot, itemTexturePath)}`)
    if (fileExists(itemModelPath)) {
      const model = readJson(itemModelPath)
      assert(errors, model.textures?.layer0 === `${MODULE_ID}:item/${itemTextureKey(item)}`, `item ${id} model should point to declared Openlands item texture`)
      assert(errors, Array.isArray(model.elements) && model.elements.length > 0, `item ${id} model should define placeholder geometry`)
    }
  }

  for (const recipe of recipes) {
    const id = normalizeId(recipe.id)
    requireFields(errors, recipe, ['id', 'station', 'inputs', 'outputs', 'timeTicks', 'unlockedBy', 'parityNotes'], `recipe ${id}`)
    assert(errors, stationIds.has(recipe.station), `recipe ${id} references unknown station ${recipe.station}`)
    for (const ref of collectRecipeItemRefs(recipe)) {
      if (ref.kind === 'block') assert(errors, blockIds.has(ref.id), `recipe ${id} ${ref.side} references unknown block ${ref.id}`)
      else assert(errors, itemIds.has(ref.id), `recipe ${id} ${ref.side} references unknown item ${ref.id}`)
    }
  }

  for (const [tag, ids] of Object.entries(tagsPayload.blockTags ?? {})) {
    for (const id of ids) assert(errors, blockIds.has(normalizeId(id)), `block tag ${tag} references unknown block ${id}`)
  }
  for (const [tag, ids] of Object.entries(tagsPayload.itemTags ?? {})) {
    for (const id of ids) assert(errors, itemIds.has(normalizeId(id)), `item tag ${tag} references unknown item ${id}`)
  }

  for (const biome of biomes) {
    requireFields(errors, biome, ['id', 'displayName', 'blockPalette', 'resourceSet', 'spawnTable', 'ambience', 'landmarkFrequency'], `biome ${biome.id}`)
    for (const spawn of biome.spawnTable ?? []) {
      assert(errors, creatureIds.has(spawn.creature), `biome ${biome.id} spawn table references unknown creature ${spawn.creature}`)
    }
  }

  for (const structure of structures) {
    requireFields(errors, structure, ['id', 'displayName', 'footprint', 'preferredBiomes', 'blocks', 'holoMapHint', 'tutorialHook'], `landmark ${structure.id}`)
    for (const biome of structure.preferredBiomes ?? []) assert(errors, biomeIds.has(biome), `landmark ${structure.id} references unknown biome ${biome}`)
    for (const block of structure.blocks ?? []) assert(errors, blockIds.has(normalizeId(block)), `landmark ${structure.id} references unknown block ${block}`)
  }

  const lootCreatureIds = new Set((lootPayload.creatureDrops ?? []).map((entry) => entry.creature))
  for (const creature of creatures) {
    requireFields(errors, creature, ['id', 'displayName', 'category', 'biomes', 'spawnRules', 'ai', 'health', 'drops', 'sounds'], `creature ${creature.id}`)
    assert(errors, lootCreatureIds.has(creature.id), `creature ${creature.id} missing loot table`)
    for (const biome of creature.biomes ?? []) assert(errors, biomeIds.has(biome), `creature ${creature.id} references unknown biome ${biome}`)
    for (const [eventName, sound] of Object.entries(creature.sounds ?? {})) {
      assert(errors, soundsPayload[soundKey(sound)] !== undefined, `creature ${creature.id} ${eventName} sound missing ${sound}`)
    }
  }

  const modes = new Map((modesPayload.modes ?? []).map((mode) => [mode.id, mode]))
  const standard = modes.get('openlands_standard')
  const hardlands = modes.get('openlands_hardlands')
  assert(errors, modesPayload.defaultMode === 'openlands_standard', 'defaultMode must be openlands_standard')
  assert(errors, standard !== undefined, 'openlands_standard mode missing')
  assert(errors, hardlands !== undefined, 'openlands_hardlands mode missing')
  if (standard) {
    assert(errors, standard.rules?.hunger === 'gentle', 'openlands_standard hunger must be gentle')
    for (const flag of HARDCORE_FLAGS) assert(errors, standard.rules?.[flag] === false, `openlands_standard ${flag} must be false`)
    assert(errors, standard.rules?.deathPack === 'recoverable', 'openlands_standard deathPack must be recoverable')
  }
  if (hardlands) {
    assert(errors, hardlands.rules?.stamina === true, 'openlands_hardlands should be the opt-in stamina mode')
    assert(errors, hardlands.rules?.foodSpoilage === true, 'openlands_hardlands should be the opt-in food spoilage mode')
  }
  for (const mode of modes.values()) {
    if (mode.id === 'openlands_hardlands') continue
    for (const flag of HARDCORE_FLAGS) {
      assert(errors, mode.rules?.[flag] !== true, `${mode.id} must not enable ${flag}; only openlands_hardlands may do that`)
    }
  }

  assert(errors, policyPayload.namespace === MODULE_ID, 'content policy namespace mismatch')
  for (const required of ['Original item and block names', 'Echo data IDs as canonical IDs', 'Gentle default gameplay with Hardlands kept optional']) {
    assert(errors, policyPayload.required?.includes(required), `content policy missing required rule: ${required}`)
  }

  const legalAudit = systemPayloads.legal_content_audit
  assert(errors, legalAudit.policySource === 'config/content_policy.json', 'legal audit policySource mismatch')
  assert(errors, legalAudit.assetManifest === 'assets/echoopenlandsprotocol/asset_manifest.json', 'legal audit assetManifest mismatch')
  for (const requiredScope of ['public_pack_text', 'block_display_names', 'item_display_names', 'canonical_echo_ids', 'texture_model_sound_paths', 'recipe_identity', 'generated_neoforge_output']) {
    assert(errors, legalAudit.auditScope?.mustCheck?.includes(requiredScope), `legal audit auditScope missing ${requiredScope}`)
  }
  for (const evidence of ['legal_policy_accepted', 'missing_asset_policy_applied', 'legal_content_audit_pass']) {
    assert(errors, legalAudit.releaseEvidence?.includes(evidence), `legal audit releaseEvidence missing ${evidence}`)
    assert(errors, evidenceIds.has(evidence), `legal audit releaseEvidence references unknown runtime evidence ${evidence}`)
  }
  assert(errors, legalAudit.canonicalIdRules?.namespace === MODULE_ID, 'legal audit canonical namespace mismatch')
  assert(errors, legalAudit.canonicalIdRules?.idPrefixesAllowed?.includes('echoopenlandsprotocol:'), 'legal audit must allow echoopenlandsprotocol ids')
  assert(errors, legalAudit.canonicalIdRules?.idPrefixesAllowed?.includes('openlands:'), 'legal audit must allow openlands tag/recipe ids')
  assert(errors, legalAudit.assetRules?.currentStatus === assetManifestPayload.status, 'legal audit asset status must match asset manifest status')
  assert(errors, legalAudit.assetRules?.publicReleaseAllowedWithPlaceholders === false, 'legal audit must block public release with placeholder assets')
  for (const root of assetManifestPayload.requiredRoots ?? []) {
    assert(errors, legalAudit.assetRules?.requiredRoots?.includes(root), `legal audit assetRules missing required root ${root}`)
  }
  for (const source of ['Minecraft textures', 'Minecraft sounds', 'Minecraft model silhouettes', 'copied vanilla resource pack files']) {
    assert(errors, legalAudit.assetRules?.forbiddenSources?.includes(source), `legal audit assetRules missing forbidden source ${source}`)
  }
  for (const mustUse of ['explicit ingredient lists', 'Openlands stations', 'Openlands unlocks', 'Openlands parity notes']) {
    assert(errors, legalAudit.recipeIdentityRules?.mustUse?.includes(mustUse), `legal audit recipeIdentityRules missing ${mustUse}`)
  }
  assert(errors, legalAudit.publicAlphaGate?.requiresHumanReview === true, 'legal audit publicAlphaGate must require human review')
  assert(errors, legalAudit.publicAlphaGate?.requiresNoForbiddenPublicTerms === true, 'legal audit publicAlphaGate must require no forbidden public terms')
  assert(errors, legalAudit.publicAlphaGate?.requiresNoBorrowedAssets === true, 'legal audit publicAlphaGate must require no borrowed assets')
  requireRuntimeParity(errors, { runtimeTargets: legalAudit.publicAlphaGate?.requiresGeneratedOutputAudit ?? [] }, 'legal audit generated output audit')

  const launcherFlow = systemPayloads.launcher_flow_acceptance
  assert(errors, launcherFlow.sourceContracts?.distribution === 'systems/distribution_alpha_gates.json', 'launcher flow sourceContracts distribution mismatch')
  assert(errors, launcherFlow.sourceContracts?.runtimeAdapterLoadPlan === 'systems/runtime_adapter_load_plan.json', 'launcher flow sourceContracts runtimeAdapterLoadPlan mismatch')
  assert(errors, launcherFlow.sourceContracts?.playtestFixture === 'playtests/mvp_first_hour_acceptance.json', 'launcher flow sourceContracts playtestFixture mismatch')
  assert(errors, launcherFlow.artifactVerification?.currentIndexStateAllowed === systemPayloads.distribution_alpha_gates.releaseIndexStates?.currentAllowedState, 'launcher flow current index state must match distribution gate')
  assert(errors, launcherFlow.artifactVerification?.approvedStateRequiresArtifacts === true, 'launcher flow must require artifacts before approved state')
  const distributionArtifactFiles = sortedUnique((systemPayloads.distribution_alpha_gates.artifactTargets ?? []).map((target) => target.file))
  const launcherArtifactFiles = sortedUnique((launcherFlow.artifactVerification?.requiredBeforePublicAlpha ?? []).map((target) => target.file))
  assert(errors, JSON.stringify(launcherArtifactFiles) === JSON.stringify(distributionArtifactFiles), 'launcher flow artifact list must match distribution artifact targets')
  for (const target of launcherFlow.artifactVerification?.requiredBeforePublicAlpha ?? []) {
    for (const field of ['url', 'sha256', 'size', 'moduleId', 'version']) {
      assert(errors, target.mustHaveIndexFields?.includes(field), `launcher artifact ${target.id} must require index field ${field}`)
    }
  }
  const parityTargets = new Map((systemPayloads.cross_platform_parity.runtimeTargets ?? []).map((runtime) => [runtime.id, runtime]))
  for (const edition of launcherFlow.editionMatrix ?? []) {
    requireFields(errors, edition, ['id', 'runtimeTarget', 'packId', 'editionRepo', 'releaseManifest', 'releaseIndexEntry', 'artifactFamily', 'artifactPattern', 'launcherProfileKind', 'requiredDescriptors'], `launcher edition ${edition.id}`)
    assert(errors, parityTargets.has(edition.runtimeTarget), `launcher edition ${edition.id} references unknown runtime target ${edition.runtimeTarget}`)
    const parityTarget = parityTargets.get(edition.runtimeTarget)
    assert(errors, parityTarget?.editionRepo === edition.editionRepo, `launcher edition ${edition.id} editionRepo must match cross-platform parity`)
    assert(errors, parityTarget?.artifactFamily === edition.artifactFamily, `launcher edition ${edition.id} artifactFamily must match cross-platform parity`)
    assert(errors, parityTarget?.artifactPattern === edition.artifactPattern, `launcher edition ${edition.id} artifactPattern must match cross-platform parity`)
    assert(errors, edition.releaseManifest === 'release-manifest.template.json', `launcher edition ${edition.id} releaseManifest must be release-manifest.template.json`)
  }
  requireRuntimeParity(errors, { runtimeTargets: (launcherFlow.editionMatrix ?? []).map((edition) => edition.runtimeTarget) }, 'launcher edition matrix')
  const launcherFlowIds = (launcherFlow.requiredLauncherFlows ?? []).map((flow) => flow.id)
  const distributionFlowIds = (systemPayloads.distribution_alpha_gates.launcherGates ?? []).map((flow) => flow.id)
  assert(errors, JSON.stringify(launcherFlowIds) === JSON.stringify(distributionFlowIds), 'launcher flow ids must match distribution launcher gates')
  const distributionFlowById = new Map((systemPayloads.distribution_alpha_gates.launcherGates ?? []).map((flow) => [flow.id, flow]))
  for (const flow of launcherFlow.requiredLauncherFlows ?? []) {
    requireFields(errors, flow, ['id', 'displayName', 'appliesTo', 'preconditions', 'mustVerify', 'additionalAssertions', 'worldStatePolicy', 'evidenceAttachment'], `launcher flow ${flow.id}`)
    requireRuntimeParity(errors, { runtimeTargets: flow.appliesTo ?? [] }, `launcher flow ${flow.id}`)
    const distributionFlow = distributionFlowById.get(flow.id)
    assert(errors, distributionFlow !== undefined, `launcher flow ${flow.id} must exist in distribution gates`)
    for (const check of distributionFlow?.mustVerify ?? []) {
      assert(errors, flow.mustVerify?.includes(check), `launcher flow ${flow.id} missing distribution check ${check}`)
    }
    assert(errors, (flow.preconditions ?? []).length > 0, `launcher flow ${flow.id} must define preconditions`)
    assert(errors, (flow.additionalAssertions ?? []).length > 0, `launcher flow ${flow.id} must define additionalAssertions`)
    assert(errors, typeof flow.worldStatePolicy?.mustPreserveWorlds === 'boolean', `launcher flow ${flow.id} must define world preservation policy`)
    assert(errors, flow.evidenceAttachment?.startsWith('launcher-') && flow.evidenceAttachment?.endsWith('-report.json'), `launcher flow ${flow.id} evidenceAttachment must be launcher report json`)
  }
  for (const requiredSave of progressionPayload.saveLoadAcceptance ?? []) {
    assert(errors, launcherFlow.statePreservation?.firstHourSaveFields?.includes(requiredSave), `launcher statePreservation missing first-hour save field ${requiredSave}`)
  }
  for (const field of waystonesPayload.multiplayerState?.storedFields ?? []) {
    assert(errors, launcherFlow.statePreservation?.waystoneFields?.includes(field), `launcher statePreservation missing waystone field ${field}`)
  }
  for (const field of holomapPayload.regionDataContract?.storedFields ?? []) {
    assert(errors, launcherFlow.statePreservation?.holomapFields?.includes(field), `launcher statePreservation missing HoloMap field ${field}`)
  }
  for (const evidence of ['native_standalone_neoforge_artifacts_uploaded_with_sha256', 'launcher_install_update_repair_rollback_pass']) {
    assert(errors, launcherFlow.releaseEvidence?.includes(evidence), `launcher flow releaseEvidence missing ${evidence}`)
    assert(errors, evidenceIds.has(evidence), `launcher flow releaseEvidence references unknown runtime evidence ${evidence}`)
  }
  assert(errors, (launcherFlow.failurePolicies ?? []).some((policy) => policy.condition === 'missing_sha256' && policy.action === 'keep_release_index_warning'), 'launcher flow must keep release index warning when sha256 is missing')
  assert(errors, (launcherFlow.failurePolicies ?? []).some((policy) => policy.condition === 'launcher_flow_missing' && policy.action === 'block_public_alpha'), 'launcher flow must block public alpha when launcher evidence is missing')

  const launcherExecution = systemPayloads.launcher_execution_acceptance
  assert(errors, launcherExecution.schema === 'echo.openlands.systems.launcher_execution_acceptance.v1', 'launcher execution acceptance schema mismatch')
  assert(errors, launcherExecution.namespace === MODULE_ID, 'launcher execution acceptance namespace mismatch')
  assert(errors, launcherExecution.version === descriptor.version, 'launcher execution acceptance version must match descriptor')
  assert(errors, launcherExecution.sourceContracts?.launcherFlow === 'systems/launcher_flow_acceptance.json', 'launcher execution sourceContracts launcherFlow mismatch')
  assert(errors, launcherExecution.sourceContracts?.distribution === 'systems/distribution_alpha_gates.json', 'launcher execution sourceContracts distribution mismatch')
  assert(errors, launcherExecution.sourceContracts?.runtimeExecutionAcceptance === 'systems/runtime_execution_acceptance.json', 'launcher execution sourceContracts runtimeExecutionAcceptance mismatch')
  assert(errors, launcherExecution.reportContract?.schema === 'echo.openlands.edition.launcher_execution_report.v1', 'launcher execution report schema mismatch')
  for (const status of ['passed', 'failed', 'blocked']) {
    assert(errors, launcherExecution.reportContract?.allowedReportStatus?.includes(status), `launcher execution allowed report status missing ${status}`)
  }
  for (const field of ['runtimeTarget', 'moduleArtifactSha256', 'flowResults', 'clearedLauncherGates', 'remainingLauncherGates', 'publicAlphaReady']) {
    assert(errors, launcherExecution.reportContract?.requiredReportFields?.includes(field), `launcher execution report required fields missing ${field}`)
  }
  for (const status of ['passed', 'failed', 'blocked', 'skipped']) {
    assert(errors, launcherExecution.reportContract?.allowedFlowStatus?.includes(status), `launcher execution allowed flow status missing ${status}`)
    assert(errors, launcherExecution.reportContract?.allowedAssertionStatus?.includes(status), `launcher execution allowed assertion status missing ${status}`)
  }
  for (const field of ['id', 'status']) {
    assert(errors, launcherExecution.reportContract?.requiredAssertionFields?.includes(field), `launcher execution required assertion field missing ${field}`)
  }
  const launcherExecutionFlowIds = (launcherExecution.executionFlows ?? []).map((flow) => flow.id)
  assert(errors, JSON.stringify(sortedUnique(launcherExecutionFlowIds)) === JSON.stringify(sortedUnique(launcherFlowIds)), 'launcher execution flows must match launcher flow acceptance ids')
  const launcherExecutionGateIds = new Set((launcherExecution.launcherGates ?? []).map((gate) => gate.id))
  const launcherExecutionFlowById = new Map((launcherExecution.executionFlows ?? []).map((flow) => [flow.id, flow]))
  for (const report of launcherExecution.editionReports ?? []) {
    requireFields(errors, report, ['edition', 'runtimeTarget', 'repo', 'requiredReport', 'artifactPattern'], `launcher execution edition report ${report.edition}`)
    assert(errors, EXPECTED_RUNTIMES.includes(report.runtimeTarget), `launcher execution edition report ${report.edition} runtime target mismatch`)
    assert(errors, report.requiredReport?.startsWith(`evidence/${report.edition}-launcher-execution-report.json`), `launcher execution edition report ${report.edition} required report path mismatch`)
  }
  assert(errors, (launcherExecution.editionReports ?? []).length === EXPECTED_RUNTIMES.length, 'launcher execution acceptance must declare one report per runtime')
  for (const gate of launcherExecution.launcherGates ?? []) {
    requireFields(errors, gate, ['id', 'flowId', 'clearsEvidence', 'mustCapture'], `launcher execution gate ${gate.id}`)
    assert(errors, launcherExecutionFlowById.has(gate.flowId), `launcher execution gate ${gate.id} references unknown flow ${gate.flowId}`)
    assert(errors, Array.isArray(gate.mustCapture) && gate.mustCapture.length > 0, `launcher execution gate ${gate.id} must list captured fields`)
    for (const evidence of gate.clearsEvidence ?? []) {
      assert(errors, evidenceIds.has(evidence), `launcher execution gate ${gate.id} clears unknown evidence ${evidence}`)
    }
  }
  assert(errors, launcherExecution.executionSuite?.id === 'launcher_distribution_execution', 'launcher execution suite id mismatch')
  assert(errors, launcherExecution.executionSuite?.minimumRunsPerRuntime === 1, 'launcher execution suite minimum runs must be 1')
  assert(errors, JSON.stringify(sortedUnique(launcherExecution.executionSuite?.flowIds ?? [])) === JSON.stringify(sortedUnique(launcherFlowIds)), 'launcher execution suite flow ids mismatch')
  const launcherFlowById = new Map((launcherFlow.requiredLauncherFlows ?? []).map((flow) => [flow.id, flow]))
  for (const flow of launcherExecution.executionFlows ?? []) {
    requireFields(errors, flow, ['id', 'displayName', 'gateIds', 'inputFixtureRefs', 'preconditions', 'plannedActions', 'assertions', 'requiredSavedArtifacts', 'worldStatePolicy'], `launcher execution flow ${flow.id}`)
    assert(errors, launcherFlowById.has(flow.id), `launcher execution flow ${flow.id} missing from launcher flow acceptance`)
    assert(errors, Array.isArray(flow.gateIds) && flow.gateIds.length > 0, `launcher execution flow ${flow.id} must list gateIds`)
    assert(errors, Array.isArray(flow.plannedActions) && flow.plannedActions.length >= 8, `launcher execution flow ${flow.id} must list detailed actions`)
    assert(errors, Array.isArray(flow.assertions) && flow.assertions.length >= 6, `launcher execution flow ${flow.id} must list detailed assertions`)
    assert(errors, Array.isArray(flow.requiredSavedArtifacts) && flow.requiredSavedArtifacts.length >= 4, `launcher execution flow ${flow.id} must list saved artifacts`)
    const launcherFlowEntry = launcherFlowById.get(flow.id)
    assert(errors, JSON.stringify(flow.worldStatePolicy) === JSON.stringify(launcherFlowEntry?.worldStatePolicy), `launcher execution flow ${flow.id} worldStatePolicy must match launcher flow`)
    for (const precondition of launcherFlowEntry?.preconditions ?? []) {
      assert(errors, flow.preconditions?.includes(precondition), `launcher execution flow ${flow.id} missing launcher precondition ${precondition}`)
    }
    for (const gateId of flow.gateIds ?? []) {
      assert(errors, launcherExecutionGateIds.has(gateId), `launcher execution flow ${flow.id} references unknown gate ${gateId}`)
    }
  }
  assert(errors, launcherExecution.publicAlphaClearance?.requiresEveryEditionReport === true, 'launcher execution must require every edition report')
  assert(errors, launcherExecution.publicAlphaClearance?.requiresEveryLauncherGateCleared === true, 'launcher execution must require every launcher gate cleared')
  assert(errors, launcherExecution.publicAlphaClearance?.requiresLauncherFlowPreflightReport === true, 'launcher execution must require launcher flow preflight report')
  assert(errors, launcherExecution.publicAlphaClearance?.preflightReportsDoNotClearLauncherGates === true, 'launcher execution must preserve preflight/execution distinction')

  const launcherHarness = systemPayloads.launcher_execution_harness_plan
  const launcherHarnessFlowBindings = launcherHarness.flowBindings ?? []
  const launcherHarnessFlowById = new Map(launcherHarnessFlowBindings.map((flow) => [flow.id, flow]))
  const launcherHarnessDriverIds = new Set((launcherHarness.driverSurfaces ?? []).map((driver) => driver.id))
  assert(errors, launcherHarness.schema === 'echo.openlands.systems.launcher_execution_harness_plan.v1', 'launcher execution harness plan schema mismatch')
  assert(errors, launcherHarness.namespace === MODULE_ID, 'launcher execution harness namespace mismatch')
  assert(errors, launcherHarness.version === descriptor.version, 'launcher execution harness version must match descriptor')
  assert(errors, launcherHarness.sourceContracts?.launcherExecutionAcceptance === 'systems/launcher_execution_acceptance.json', 'launcher execution harness sourceContracts launcher execution mismatch')
  assert(errors, launcherHarness.sourceContracts?.launcherFlowAcceptance === 'systems/launcher_flow_acceptance.json', 'launcher execution harness sourceContracts launcher flow mismatch')
  assert(errors, launcherHarness.sourceContracts?.distribution === 'systems/distribution_alpha_gates.json', 'launcher execution harness sourceContracts distribution mismatch')
  assert(errors, launcherHarness.sourceContracts?.runtimeExecutionAcceptance === 'systems/runtime_execution_acceptance.json', 'launcher execution harness sourceContracts runtime execution mismatch')
  assert(errors, launcherHarness.sourceContracts?.runtimeExecutionHarnessPlan === 'systems/runtime_execution_harness_plan.json', 'launcher execution harness sourceContracts runtime harness mismatch')
  assertSameStringSet(errors, (launcherHarness.editionHarnesses ?? []).map((entry) => entry.runtimeTarget), EXPECTED_RUNTIMES, 'launcher execution harness edition runtime targets')
  assertSameStringSet(errors, launcherHarnessFlowBindings.map((flow) => flow.id), launcherExecution.executionFlows?.map((flow) => flow.id), 'launcher execution harness flow coverage')
  assert(errors, (launcherHarness.driverSurfaces ?? []).length >= 9, 'launcher execution harness must define at least nine driver surfaces')
  for (const editionHarness of launcherHarness.editionHarnesses ?? []) {
    requireFields(errors, editionHarness, ['edition', 'runtimeTarget', 'repo', 'driverKind', 'entryPoint', 'requiredReport', 'artifactPattern', 'launcherProfileKind', 'launcherArtifactRoot'], `launcher execution harness edition ${editionHarness.edition}`)
    assert(errors, EXPECTED_RUNTIMES.includes(editionHarness.runtimeTarget), `launcher execution harness edition ${editionHarness.edition} runtime target mismatch`)
    assert(errors, editionHarness.entryPoint === 'scripts/run-launcher-execution-harness.mjs', `launcher execution harness edition ${editionHarness.edition} entry point mismatch`)
    const expectedReport = (launcherExecution.editionReports ?? []).find((report) => report.edition === editionHarness.edition)
    assert(errors, expectedReport !== undefined, `launcher execution harness edition ${editionHarness.edition} missing matching launcher execution report`)
    if (expectedReport) {
      assert(errors, editionHarness.runtimeTarget === expectedReport.runtimeTarget, `launcher execution harness edition ${editionHarness.edition} runtime target must match acceptance`)
      assert(errors, editionHarness.repo === expectedReport.repo, `launcher execution harness edition ${editionHarness.edition} repo must match acceptance`)
      assert(errors, editionHarness.requiredReport === expectedReport.requiredReport, `launcher execution harness edition ${editionHarness.edition} report path must match acceptance`)
      assert(errors, editionHarness.artifactPattern === expectedReport.artifactPattern, `launcher execution harness edition ${editionHarness.edition} artifact pattern must match acceptance`)
    }
    assert(errors, typeof editionHarness.launcherArtifactRoot === 'string' && editionHarness.launcherArtifactRoot.startsWith(`evidence/launcher-execution/${editionHarness.edition}`), `launcher execution harness edition ${editionHarness.edition} launcher artifact root mismatch`)
  }
  for (const driver of launcherHarness.driverSurfaces ?? []) {
    requireFields(errors, driver, ['id', 'requiredMethods', 'mustCapture'], `launcher execution harness driver ${driver.id}`)
    assert(errors, Array.isArray(driver.requiredMethods) && driver.requiredMethods.length > 0, `launcher execution harness driver ${driver.id} must list methods`)
    assert(errors, Array.isArray(driver.mustCapture) && driver.mustCapture.length > 0, `launcher execution harness driver ${driver.id} must list captured fields`)
  }
  for (const flow of launcherHarnessFlowBindings) {
    const expectedFlow = launcherExecutionFlowById.get(flow.id)
    assert(errors, expectedFlow !== undefined, `launcher execution harness flow ${flow.id} missing acceptance flow`)
    if (!expectedFlow) continue
    requireFields(errors, flow, ['id', 'gateIds', 'driverSurfaceIds', 'inputFixtureRefs', 'preconditions', 'plannedActions', 'assertions', 'requiredSavedArtifacts', 'worldStatePolicy'], `launcher execution harness flow ${flow.id}`)
    assert(errors, JSON.stringify(flow.gateIds ?? []) === JSON.stringify(expectedFlow.gateIds ?? []), `launcher execution harness flow ${flow.id} gate ids must match acceptance`)
    assert(errors, JSON.stringify(flow.inputFixtureRefs ?? []) === JSON.stringify(expectedFlow.inputFixtureRefs ?? []), `launcher execution harness flow ${flow.id} fixtures must match acceptance`)
    assert(errors, JSON.stringify(flow.preconditions ?? []) === JSON.stringify(expectedFlow.preconditions ?? []), `launcher execution harness flow ${flow.id} preconditions must match acceptance`)
    assert(errors, JSON.stringify(flow.plannedActions ?? []) === JSON.stringify(expectedFlow.plannedActions ?? []), `launcher execution harness flow ${flow.id} planned actions must match acceptance`)
    assert(errors, JSON.stringify(flow.assertions ?? []) === JSON.stringify(expectedFlow.assertions ?? []), `launcher execution harness flow ${flow.id} assertions must match acceptance`)
    assert(errors, JSON.stringify(flow.requiredSavedArtifacts ?? []) === JSON.stringify(expectedFlow.requiredSavedArtifacts ?? []), `launcher execution harness flow ${flow.id} saved artifacts must match acceptance`)
    assert(errors, JSON.stringify(flow.worldStatePolicy ?? {}) === JSON.stringify(expectedFlow.worldStatePolicy ?? {}), `launcher execution harness flow ${flow.id} world state policy must match acceptance`)
    assert(errors, Array.isArray(flow.driverSurfaceIds) && flow.driverSurfaceIds.length > 0, `launcher execution harness flow ${flow.id} must list driver surfaces`)
    for (const driverId of flow.driverSurfaceIds ?? []) {
      assert(errors, launcherHarnessDriverIds.has(driverId), `launcher execution harness flow ${flow.id} references unknown driver ${driverId}`)
    }
  }
  assert(errors, launcherHarnessFlowById.has('install'), 'launcher execution harness must include install flow')
  assert(errors, launcherHarnessFlowById.has('update'), 'launcher execution harness must include update flow')
  assert(errors, launcherHarnessFlowById.has('repair'), 'launcher execution harness must include repair flow')
  assert(errors, launcherHarnessFlowById.has('rollback'), 'launcher execution harness must include rollback flow')
  assert(errors, launcherHarness.reportAssemblyRules?.allPreconditionsMustBeSatisfied === true, 'launcher execution harness must require preconditions')
  assert(errors, launcherHarness.reportAssemblyRules?.allPlannedActionsMustRun === true, 'launcher execution harness must require planned actions to run')
  assert(errors, launcherHarness.reportAssemblyRules?.allAssertionsMustPassForFlowPass === true, 'launcher execution harness must require assertions to pass')
  assert(errors, launcherHarness.reportAssemblyRules?.worldStatePolicyMustMatchAcceptance === true, 'launcher execution harness must preserve world-state policy')
  assert(errors, launcherHarness.reportAssemblyRules?.blockedHarnessDoesNotClearLauncherGates === true, 'launcher execution harness must preserve blocked gate honesty')

  const finalReview = systemPayloads.final_release_review_acceptance
  assert(errors, finalReview.schema === 'echo.openlands.systems.final_release_review_acceptance.v1', 'final release review acceptance schema mismatch')
  assert(errors, finalReview.namespace === MODULE_ID, 'final release review acceptance namespace mismatch')
  assert(errors, finalReview.version === descriptor.version, 'final release review acceptance version must match descriptor')
  assert(errors, finalReview.sourceContracts?.legalContentAudit === 'systems/legal_content_audit.json', 'final review sourceContracts legal audit mismatch')
  assert(errors, finalReview.sourceContracts?.assetManifest === 'assets/echoopenlandsprotocol/asset_manifest.json', 'final review sourceContracts asset manifest mismatch')
  assert(errors, finalReview.sourceContracts?.runtimeExecutionAcceptance === 'systems/runtime_execution_acceptance.json', 'final review sourceContracts runtime execution mismatch')
  assert(errors, finalReview.reportContract?.schema === 'echo.openlands.edition.final_release_review_report.v1', 'final release review report schema mismatch')
  for (const status of ['passed', 'failed', 'blocked']) {
    assert(errors, finalReview.reportContract?.allowedReportStatus?.includes(status), `final review allowed report status missing ${status}`)
  }
  for (const field of ['runtimeTarget', 'moduleArtifactSha256', 'assetManifestHash', 'legalAuditHash', 'reviewer', 'reviewDate', 'reviewResults', 'clearedFinalReviewGates', 'remainingFinalReviewGates', 'publicReleaseReady']) {
    assert(errors, finalReview.reportContract?.requiredReportFields?.includes(field), `final review report required fields missing ${field}`)
  }
  for (const status of ['passed', 'failed', 'blocked', 'skipped']) {
    assert(errors, finalReview.reportContract?.allowedReviewStatus?.includes(status), `final review allowed review status missing ${status}`)
    assert(errors, finalReview.reportContract?.allowedChecklistStatus?.includes(status), `final review allowed checklist status missing ${status}`)
  }
  const runtimeExecutionGateSet = new Set(runtimeExecutionGateIds)
  const finalReviewGateIds = new Set((finalReview.finalReviewGates ?? []).map((gate) => gate.id))
  for (const report of finalReview.editionReports ?? []) {
    requireFields(errors, report, ['edition', 'runtimeTarget', 'repo', 'requiredReport', 'artifactPattern'], `final review edition report ${report.edition}`)
    assert(errors, EXPECTED_RUNTIMES.includes(report.runtimeTarget), `final review edition report ${report.edition} runtime target mismatch`)
    assert(errors, report.requiredReport?.startsWith(`evidence/${report.edition}-final-release-review-report.json`), `final review edition report ${report.edition} required report path mismatch`)
  }
  assert(errors, (finalReview.editionReports ?? []).length === EXPECTED_RUNTIMES.length, 'final review acceptance must declare one report per runtime')
  for (const gate of finalReview.finalReviewGates ?? []) {
    requireFields(errors, gate, ['id', 'clearsRuntimeGate', 'clearsEvidence', 'mustCapture'], `final review gate ${gate.id}`)
    assert(errors, runtimeExecutionGateSet.has(gate.clearsRuntimeGate), `final review gate ${gate.id} references unknown runtime execution gate ${gate.clearsRuntimeGate}`)
    assert(errors, Array.isArray(gate.mustCapture) && gate.mustCapture.length > 0, `final review gate ${gate.id} must list captured fields`)
    for (const evidence of gate.clearsEvidence ?? []) {
      assert(errors, evidenceIds.has(evidence), `final review gate ${gate.id} clears unknown evidence ${evidence}`)
    }
  }
  for (const gateId of ['final_asset_legal_review', 'final_art_audio_pass']) {
    assert(errors, finalReviewGateIds.has(gateId), `final review gates missing ${gateId}`)
  }
  for (const area of finalReview.reviewAreas ?? []) {
    requireFields(errors, area, ['id', 'displayName', 'gateIds', 'inputFixtureRefs', 'checklist', 'requiredSavedArtifacts'], `final review area ${area.id}`)
    assert(errors, Array.isArray(area.gateIds) && area.gateIds.length > 0, `final review area ${area.id} must list gateIds`)
    assert(errors, Array.isArray(area.checklist) && area.checklist.length >= 5, `final review area ${area.id} must list detailed checklist items`)
    assert(errors, Array.isArray(area.requiredSavedArtifacts) && area.requiredSavedArtifacts.length >= 3, `final review area ${area.id} must list saved artifacts`)
    for (const gateId of area.gateIds ?? []) {
      assert(errors, finalReviewGateIds.has(gateId), `final review area ${area.id} references unknown gate ${gateId}`)
    }
  }
  assert(errors, (finalReview.reviewAreas ?? []).length >= 5, 'final review acceptance must define at least five review areas')
  assert(errors, finalReview.publicReleaseClearance?.requiresEveryEditionReport === true, 'final review must require every edition report')
  assert(errors, finalReview.publicReleaseClearance?.requiresEveryFinalReviewGateCleared === true, 'final review must require every final review gate cleared')
  assert(errors, finalReview.publicReleaseClearance?.requiresLegalPreflightReport === true, 'final review must require legal preflight report')
  assert(errors, finalReview.publicReleaseClearance?.requiresNoPlaceholdersForPublicRelease === true, 'final review must require no placeholders for public release')
  assert(errors, finalReview.publicReleaseClearance?.preflightReportsDoNotClearFinalReviewGates === true, 'final review must preserve preflight/final-review distinction')

  const finalReviewHarness = systemPayloads.final_release_review_harness_plan
  const finalReviewHarnessBindings = finalReviewHarness.reviewAreaBindings ?? []
  const finalReviewHarnessBindingById = new Map(finalReviewHarnessBindings.map((area) => [area.id, area]))
  const finalReviewHarnessDriverIds = new Set((finalReviewHarness.driverSurfaces ?? []).map((driver) => driver.id))
  const finalReviewAreaById = new Map((finalReview.reviewAreas ?? []).map((area) => [area.id, area]))
  assert(errors, finalReviewHarness.schema === 'echo.openlands.systems.final_release_review_harness_plan.v1', 'final release review harness plan schema mismatch')
  assert(errors, finalReviewHarness.namespace === MODULE_ID, 'final release review harness namespace mismatch')
  assert(errors, finalReviewHarness.version === descriptor.version, 'final release review harness version must match descriptor')
  assert(errors, finalReviewHarness.sourceContracts?.finalReleaseReviewAcceptance === 'systems/final_release_review_acceptance.json', 'final release review harness sourceContracts final review mismatch')
  assert(errors, finalReviewHarness.sourceContracts?.legalContentAudit === 'systems/legal_content_audit.json', 'final release review harness sourceContracts legal audit mismatch')
  assert(errors, finalReviewHarness.sourceContracts?.contentPolicy === 'config/content_policy.json', 'final release review harness sourceContracts content policy mismatch')
  assert(errors, finalReviewHarness.sourceContracts?.assetManifest === 'assets/echoopenlandsprotocol/asset_manifest.json', 'final release review harness sourceContracts asset manifest mismatch')
  assert(errors, finalReviewHarness.sourceContracts?.soundContract === 'sounds/mvp_sound_contract.json', 'final release review harness sourceContracts sound contract mismatch')
  assert(errors, finalReviewHarness.sourceContracts?.runtimeExecutionAcceptance === 'systems/runtime_execution_acceptance.json', 'final release review harness sourceContracts runtime execution mismatch')
  assert(errors, finalReviewHarness.sourceContracts?.runtimeExecutionHarnessPlan === 'systems/runtime_execution_harness_plan.json', 'final release review harness sourceContracts runtime harness mismatch')
  assert(errors, finalReviewHarness.sourceContracts?.launcherExecutionHarnessPlan === 'systems/launcher_execution_harness_plan.json', 'final release review harness sourceContracts launcher harness mismatch')
  assert(errors, finalReviewHarness.sourceContracts?.distributionApprovalAcceptance === 'systems/distribution_approval_acceptance.json', 'final release review harness sourceContracts distribution approval mismatch')
  assertSameStringSet(errors, (finalReviewHarness.editionHarnesses ?? []).map((entry) => entry.runtimeTarget), EXPECTED_RUNTIMES, 'final release review harness edition runtime targets')
  assertSameStringSet(errors, finalReviewHarnessBindings.map((area) => area.id), (finalReview.reviewAreas ?? []).map((area) => area.id), 'final release review harness area coverage')
  assert(errors, (finalReviewHarness.driverSurfaces ?? []).length >= 8, 'final release review harness must define at least eight driver surfaces')
  for (const editionHarness of finalReviewHarness.editionHarnesses ?? []) {
    requireFields(errors, editionHarness, ['edition', 'runtimeTarget', 'repo', 'driverKind', 'entryPoint', 'requiredReport', 'artifactPattern', 'reviewArtifactRoot'], `final release review harness edition ${editionHarness.edition}`)
    assert(errors, EXPECTED_RUNTIMES.includes(editionHarness.runtimeTarget), `final release review harness edition ${editionHarness.edition} runtime target mismatch`)
    assert(errors, editionHarness.entryPoint === 'scripts/run-final-release-review-harness.mjs', `final release review harness edition ${editionHarness.edition} entry point mismatch`)
    const expectedReport = (finalReview.editionReports ?? []).find((report) => report.edition === editionHarness.edition)
    assert(errors, expectedReport !== undefined, `final release review harness edition ${editionHarness.edition} missing matching final review report`)
    if (expectedReport) {
      assert(errors, editionHarness.runtimeTarget === expectedReport.runtimeTarget, `final release review harness edition ${editionHarness.edition} runtime target must match acceptance`)
      assert(errors, editionHarness.repo === expectedReport.repo, `final release review harness edition ${editionHarness.edition} repo must match acceptance`)
      assert(errors, editionHarness.requiredReport === expectedReport.requiredReport, `final release review harness edition ${editionHarness.edition} report path must match acceptance`)
      assert(errors, editionHarness.artifactPattern === expectedReport.artifactPattern, `final release review harness edition ${editionHarness.edition} artifact pattern must match acceptance`)
    }
    assert(errors, typeof editionHarness.reviewArtifactRoot === 'string' && editionHarness.reviewArtifactRoot.startsWith(`evidence/final-review/${editionHarness.edition}`), `final release review harness edition ${editionHarness.edition} review artifact root mismatch`)
  }
  for (const driver of finalReviewHarness.driverSurfaces ?? []) {
    requireFields(errors, driver, ['id', 'requiredMethods', 'mustCapture'], `final release review harness driver ${driver.id}`)
    assert(errors, Array.isArray(driver.requiredMethods) && driver.requiredMethods.length > 0, `final release review harness driver ${driver.id} must list methods`)
    assert(errors, Array.isArray(driver.mustCapture) && driver.mustCapture.length > 0, `final release review harness driver ${driver.id} must list captured fields`)
  }
  for (const area of finalReviewHarnessBindings) {
    const expectedArea = finalReviewAreaById.get(area.id)
    assert(errors, expectedArea !== undefined, `final release review harness area ${area.id} missing acceptance review area`)
    if (!expectedArea) continue
    requireFields(errors, area, ['id', 'gateIds', 'driverSurfaceIds', 'inputFixtureRefs', 'checklist', 'requiredSavedArtifacts'], `final release review harness area ${area.id}`)
    assert(errors, JSON.stringify(area.gateIds ?? []) === JSON.stringify(expectedArea.gateIds ?? []), `final release review harness area ${area.id} gate ids must match acceptance`)
    assert(errors, JSON.stringify(area.inputFixtureRefs ?? []) === JSON.stringify(expectedArea.inputFixtureRefs ?? []), `final release review harness area ${area.id} fixtures must match acceptance`)
    assert(errors, JSON.stringify(area.checklist ?? []) === JSON.stringify(expectedArea.checklist ?? []), `final release review harness area ${area.id} checklist must match acceptance`)
    assert(errors, JSON.stringify(area.requiredSavedArtifacts ?? []) === JSON.stringify(expectedArea.requiredSavedArtifacts ?? []), `final release review harness area ${area.id} saved artifacts must match acceptance`)
    assert(errors, Array.isArray(area.driverSurfaceIds) && area.driverSurfaceIds.length > 0, `final release review harness area ${area.id} must list driver surfaces`)
    for (const gateId of area.gateIds ?? []) {
      assert(errors, finalReviewGateIds.has(gateId), `final release review harness area ${area.id} references unknown gate ${gateId}`)
    }
    for (const driverId of area.driverSurfaceIds ?? []) {
      assert(errors, finalReviewHarnessDriverIds.has(driverId), `final release review harness area ${area.id} references unknown driver ${driverId}`)
    }
  }
  for (const reviewAreaId of ['public_identity_and_branding', 'block_textures_models_and_blockstates', 'item_icons_models_and_tools', 'audio_sources_and_sound_events', 'generated_runtime_outputs']) {
    assert(errors, finalReviewHarnessBindingById.has(reviewAreaId), `final release review harness missing area ${reviewAreaId}`)
  }
  assert(errors, finalReviewHarness.reportAssemblyRules?.allChecklistItemsMustPassForAreaPass === true, 'final release review harness must require checklist items to pass')
  assert(errors, finalReviewHarness.reportAssemblyRules?.allRequiredSavedArtifactsMustExistForAreaPass === true, 'final release review harness must require saved artifacts')
  assert(errors, finalReviewHarness.reportAssemblyRules?.allRequiredDriverCapturesMustExist === true, 'final release review harness must require driver captures')
  assert(errors, finalReviewHarness.reportAssemblyRules?.allInputFixturesMustBeHashed === true, 'final release review harness must require fixture hashes')
  assert(errors, finalReviewHarness.reportAssemblyRules?.gateClearsOnlyWhenEveryMappedReviewAreaPasses === true, 'final release review harness must require mapped areas for gate clearance')
  assert(errors, finalReviewHarness.reportAssemblyRules?.blockedHarnessDoesNotClearFinalReviewGates === true, 'final release review harness must preserve blocked gate honesty')
  assert(errors, finalReviewHarness.reportAssemblyRules?.preflightReportsDoNotClearFinalReviewGates === true, 'final release review harness must preserve preflight/final-review distinction')
  assert(errors, finalReviewHarness.reportAssemblyRules?.publicReleaseReadyRequiresReviewerDateAndApprovedAssets === true, 'final release review harness must require reviewer, date, and approved assets')

  const distributionApproval = systemPayloads.distribution_approval_acceptance
  assert(errors, distributionApproval.schema === 'echo.openlands.systems.distribution_approval_acceptance.v1', 'distribution approval acceptance schema mismatch')
  assert(errors, distributionApproval.namespace === MODULE_ID, 'distribution approval acceptance namespace mismatch')
  assert(errors, distributionApproval.version === descriptor.version, 'distribution approval acceptance version must match descriptor')
  assert(errors, distributionApproval.sourceContracts?.distribution === 'systems/distribution_alpha_gates.json', 'distribution approval sourceContracts distribution mismatch')
  assert(errors, distributionApproval.sourceContracts?.launcherExecution === 'systems/launcher_execution_acceptance.json', 'distribution approval sourceContracts launcher execution mismatch')
  assert(errors, distributionApproval.sourceContracts?.runtimeExecution === 'systems/runtime_execution_acceptance.json', 'distribution approval sourceContracts runtime execution mismatch')
  assert(errors, distributionApproval.sourceContracts?.finalReleaseReview === 'systems/final_release_review_acceptance.json', 'distribution approval sourceContracts final review mismatch')
  assert(errors, distributionApproval.sourceContracts?.releasePublication === 'systems/release_publication_manifest_contract.json', 'distribution approval sourceContracts release publication mismatch')
  assert(errors, distributionApproval.reportContract?.schema === 'echo.openlands.edition.distribution_approval_report.v1', 'distribution approval report schema mismatch')
  for (const status of ['passed', 'failed', 'blocked']) {
    assert(errors, distributionApproval.reportContract?.allowedReportStatus?.includes(status), `distribution approval allowed report status missing ${status}`)
  }
  for (const field of ['runtimeTarget', 'releaseId', 'approvalRun', 'approvalResults', 'clearedDistributionGates', 'remainingDistributionGates', 'publicAlphaReady']) {
    assert(errors, distributionApproval.reportContract?.requiredReportFields?.includes(field), `distribution approval report required fields missing ${field}`)
  }
  for (const status of ['passed', 'failed', 'blocked', 'skipped']) {
    assert(errors, distributionApproval.reportContract?.allowedApprovalStatus?.includes(status), `distribution approval allowed approval status missing ${status}`)
    assert(errors, distributionApproval.reportContract?.allowedChecklistStatus?.includes(status), `distribution approval allowed checklist status missing ${status}`)
  }
  const distributionApprovalGateIds = new Set((distributionApproval.distributionGates ?? []).map((gate) => gate.id))
  for (const report of distributionApproval.editionReports ?? []) {
    requireFields(errors, report, ['edition', 'runtimeTarget', 'repo', 'requiredReport', 'artifactPattern'], `distribution approval edition report ${report.edition}`)
    assert(errors, EXPECTED_RUNTIMES.includes(report.runtimeTarget), `distribution approval edition report ${report.edition} runtime target mismatch`)
    assert(errors, report.requiredReport?.startsWith(`evidence/${report.edition}-distribution-approval-report.json`), `distribution approval edition report ${report.edition} required report path mismatch`)
  }
  assert(errors, (distributionApproval.editionReports ?? []).length === EXPECTED_RUNTIMES.length, 'distribution approval acceptance must declare one report per runtime')
  for (const gate of distributionApproval.distributionGates ?? []) {
    requireFields(errors, gate, ['id', 'clearsEvidence', 'mustCapture'], `distribution approval gate ${gate.id}`)
    assert(errors, Array.isArray(gate.mustCapture) && gate.mustCapture.length > 0, `distribution approval gate ${gate.id} must list captured fields`)
    for (const evidence of gate.clearsEvidence ?? []) {
      assert(errors, evidenceIds.has(evidence), `distribution approval gate ${gate.id} clears unknown evidence ${evidence}`)
    }
  }
  for (const gateId of ['release_index_artifact_urls_published', 'release_index_download_hash_verified', 'edition_pack_manifests_indexed', 'public_alpha_coop_session_tested', 'public_alpha_distribution_approved']) {
    assert(errors, distributionApprovalGateIds.has(gateId), `distribution approval gates missing ${gateId}`)
  }
  for (const area of distributionApproval.approvalAreas ?? []) {
    requireFields(errors, area, ['id', 'displayName', 'gateIds', 'inputFixtureRefs', 'checklist', 'requiredSavedArtifacts'], `distribution approval area ${area.id}`)
    assert(errors, Array.isArray(area.gateIds) && area.gateIds.length > 0, `distribution approval area ${area.id} must list gateIds`)
    assert(errors, Array.isArray(area.checklist) && area.checklist.length >= 5, `distribution approval area ${area.id} must list detailed checklist items`)
    assert(errors, Array.isArray(area.requiredSavedArtifacts) && area.requiredSavedArtifacts.length >= 3, `distribution approval area ${area.id} must list saved artifacts`)
    for (const gateId of area.gateIds ?? []) {
      assert(errors, distributionApprovalGateIds.has(gateId), `distribution approval area ${area.id} references unknown gate ${gateId}`)
    }
  }
  assert(errors, (distributionApproval.approvalAreas ?? []).length >= 5, 'distribution approval acceptance must define at least five approval areas')
  assert(errors, distributionApproval.publicAlphaClearance?.requiresEveryEditionReport === true, 'distribution approval must require every edition report')
  assert(errors, distributionApproval.publicAlphaClearance?.requiresEveryDistributionGateCleared === true, 'distribution approval must require every distribution gate cleared')
  assert(errors, distributionApproval.publicAlphaClearance?.requiresUploadedArtifactUrls === true, 'distribution approval must require uploaded artifact URLs')
  assert(errors, distributionApproval.publicAlphaClearance?.requiresReleaseIndexApproval === true, 'distribution approval must require Release Index approval')
  assert(errors, distributionApproval.publicAlphaClearance?.requiresCoopSessionEvidence === true, 'distribution approval must require co-op session evidence')
  assert(errors, distributionApproval.publicAlphaClearance?.requiresRuntimeLauncherAndFinalReviewPass === true, 'distribution approval must require runtime, launcher, and final review pass')
  assert(errors, distributionApproval.publicAlphaClearance?.preflightReportsDoNotClearDistributionGates === true, 'distribution approval must preserve preflight/distribution distinction')

  const distributionHarness = systemPayloads.distribution_approval_harness_plan
  const distributionHarnessBindings = distributionHarness.approvalAreaBindings ?? []
  const distributionHarnessBindingById = new Map(distributionHarnessBindings.map((area) => [area.id, area]))
  const distributionHarnessDriverIds = new Set((distributionHarness.driverSurfaces ?? []).map((driver) => driver.id))
  const distributionApprovalAreaById = new Map((distributionApproval.approvalAreas ?? []).map((area) => [area.id, area]))
  assert(errors, distributionHarness.schema === 'echo.openlands.systems.distribution_approval_harness_plan.v1', 'distribution approval harness plan schema mismatch')
  assert(errors, distributionHarness.namespace === MODULE_ID, 'distribution approval harness namespace mismatch')
  assert(errors, distributionHarness.version === descriptor.version, 'distribution approval harness version must match descriptor')
  assert(errors, distributionHarness.sourceContracts?.distributionApprovalAcceptance === 'systems/distribution_approval_acceptance.json', 'distribution approval harness sourceContracts distribution approval mismatch')
  assert(errors, distributionHarness.sourceContracts?.distribution === 'systems/distribution_alpha_gates.json', 'distribution approval harness sourceContracts distribution mismatch')
  assert(errors, distributionHarness.sourceContracts?.releasePublication === 'systems/release_publication_manifest_contract.json', 'distribution approval harness sourceContracts release publication mismatch')
  assert(errors, distributionHarness.sourceContracts?.launcherFlowAcceptance === 'systems/launcher_flow_acceptance.json', 'distribution approval harness sourceContracts launcher flow mismatch')
  assert(errors, distributionHarness.sourceContracts?.launcherExecutionAcceptance === 'systems/launcher_execution_acceptance.json', 'distribution approval harness sourceContracts launcher execution mismatch')
  assert(errors, distributionHarness.sourceContracts?.launcherExecutionHarnessPlan === 'systems/launcher_execution_harness_plan.json', 'distribution approval harness sourceContracts launcher harness mismatch')
  assert(errors, distributionHarness.sourceContracts?.runtimeExecutionAcceptance === 'systems/runtime_execution_acceptance.json', 'distribution approval harness sourceContracts runtime execution mismatch')
  assert(errors, distributionHarness.sourceContracts?.runtimeExecutionHarnessPlan === 'systems/runtime_execution_harness_plan.json', 'distribution approval harness sourceContracts runtime harness mismatch')
  assert(errors, distributionHarness.sourceContracts?.finalReleaseReviewAcceptance === 'systems/final_release_review_acceptance.json', 'distribution approval harness sourceContracts final review mismatch')
  assert(errors, distributionHarness.sourceContracts?.finalReleaseReviewHarnessPlan === 'systems/final_release_review_harness_plan.json', 'distribution approval harness sourceContracts final review harness mismatch')
  assert(errors, distributionHarness.sourceContracts?.coopSmp === 'systems/coop_and_smp.json', 'distribution approval harness sourceContracts coop mismatch')
  assert(errors, distributionHarness.sourceContracts?.launchRoadmap === 'progression/launch_roadmap.json', 'distribution approval harness sourceContracts launch roadmap mismatch')
  assert(errors, distributionHarness.sourceContracts?.productionPhaseMatrix === 'progression/production_phase_matrix.json', 'distribution approval harness sourceContracts production matrix mismatch')
  assertSameStringSet(errors, (distributionHarness.editionHarnesses ?? []).map((entry) => entry.runtimeTarget), EXPECTED_RUNTIMES, 'distribution approval harness edition runtime targets')
  assertSameStringSet(errors, distributionHarnessBindings.map((area) => area.id), (distributionApproval.approvalAreas ?? []).map((area) => area.id), 'distribution approval harness area coverage')
  assert(errors, (distributionHarness.driverSurfaces ?? []).length >= 9, 'distribution approval harness must define at least nine driver surfaces')
  for (const editionHarness of distributionHarness.editionHarnesses ?? []) {
    requireFields(errors, editionHarness, ['edition', 'runtimeTarget', 'repo', 'driverKind', 'entryPoint', 'requiredReport', 'artifactPattern', 'approvalArtifactRoot'], `distribution approval harness edition ${editionHarness.edition}`)
    assert(errors, EXPECTED_RUNTIMES.includes(editionHarness.runtimeTarget), `distribution approval harness edition ${editionHarness.edition} runtime target mismatch`)
    assert(errors, editionHarness.entryPoint === 'scripts/run-distribution-approval-harness.mjs', `distribution approval harness edition ${editionHarness.edition} entry point mismatch`)
    const expectedReport = (distributionApproval.editionReports ?? []).find((report) => report.edition === editionHarness.edition)
    assert(errors, expectedReport !== undefined, `distribution approval harness edition ${editionHarness.edition} missing matching distribution approval report`)
    if (expectedReport) {
      assert(errors, editionHarness.runtimeTarget === expectedReport.runtimeTarget, `distribution approval harness edition ${editionHarness.edition} runtime target must match acceptance`)
      assert(errors, editionHarness.repo === expectedReport.repo, `distribution approval harness edition ${editionHarness.edition} repo must match acceptance`)
      assert(errors, editionHarness.requiredReport === expectedReport.requiredReport, `distribution approval harness edition ${editionHarness.edition} report path must match acceptance`)
      assert(errors, editionHarness.artifactPattern === expectedReport.artifactPattern, `distribution approval harness edition ${editionHarness.edition} artifact pattern must match acceptance`)
    }
    assert(errors, typeof editionHarness.approvalArtifactRoot === 'string' && editionHarness.approvalArtifactRoot.startsWith(`evidence/distribution-approval/${editionHarness.edition}`), `distribution approval harness edition ${editionHarness.edition} approval artifact root mismatch`)
  }
  for (const driver of distributionHarness.driverSurfaces ?? []) {
    requireFields(errors, driver, ['id', 'requiredMethods', 'mustCapture'], `distribution approval harness driver ${driver.id}`)
    assert(errors, Array.isArray(driver.requiredMethods) && driver.requiredMethods.length > 0, `distribution approval harness driver ${driver.id} must list methods`)
    assert(errors, Array.isArray(driver.mustCapture) && driver.mustCapture.length > 0, `distribution approval harness driver ${driver.id} must list captured fields`)
  }
  for (const area of distributionHarnessBindings) {
    const expectedArea = distributionApprovalAreaById.get(area.id)
    assert(errors, expectedArea !== undefined, `distribution approval harness area ${area.id} missing acceptance approval area`)
    if (!expectedArea) continue
    requireFields(errors, area, ['id', 'gateIds', 'driverSurfaceIds', 'inputFixtureRefs', 'checklist', 'requiredSavedArtifacts'], `distribution approval harness area ${area.id}`)
    assert(errors, JSON.stringify(area.gateIds ?? []) === JSON.stringify(expectedArea.gateIds ?? []), `distribution approval harness area ${area.id} gate ids must match acceptance`)
    assert(errors, JSON.stringify(area.inputFixtureRefs ?? []) === JSON.stringify(expectedArea.inputFixtureRefs ?? []), `distribution approval harness area ${area.id} fixtures must match acceptance`)
    assert(errors, JSON.stringify(area.checklist ?? []) === JSON.stringify(expectedArea.checklist ?? []), `distribution approval harness area ${area.id} checklist must match acceptance`)
    assert(errors, JSON.stringify(area.requiredSavedArtifacts ?? []) === JSON.stringify(expectedArea.requiredSavedArtifacts ?? []), `distribution approval harness area ${area.id} saved artifacts must match acceptance`)
    assert(errors, Array.isArray(area.driverSurfaceIds) && area.driverSurfaceIds.length > 0, `distribution approval harness area ${area.id} must list driver surfaces`)
    for (const gateId of area.gateIds ?? []) {
      assert(errors, distributionApprovalGateIds.has(gateId), `distribution approval harness area ${area.id} references unknown gate ${gateId}`)
    }
    for (const driverId of area.driverSurfaceIds ?? []) {
      assert(errors, distributionHarnessDriverIds.has(driverId), `distribution approval harness area ${area.id} references unknown driver ${driverId}`)
    }
  }
  for (const approvalAreaId of ['artifact_publication', 'edition_manifest_indexing', 'runtime_launcher_final_review_dependency', 'coop_public_alpha_session', 'public_alpha_approval']) {
    assert(errors, distributionHarnessBindingById.has(approvalAreaId), `distribution approval harness missing area ${approvalAreaId}`)
  }
  assert(errors, distributionHarness.reportAssemblyRules?.allChecklistItemsMustPassForAreaPass === true, 'distribution approval harness must require checklist items to pass')
  assert(errors, distributionHarness.reportAssemblyRules?.allRequiredSavedArtifactsMustExistForAreaPass === true, 'distribution approval harness must require saved artifacts')
  assert(errors, distributionHarness.reportAssemblyRules?.allRequiredDriverCapturesMustExist === true, 'distribution approval harness must require driver captures')
  assert(errors, distributionHarness.reportAssemblyRules?.allInputFixturesMustBeHashed === true, 'distribution approval harness must require fixture hashes')
  assert(errors, distributionHarness.reportAssemblyRules?.allDependencyReportsMustPass === true, 'distribution approval harness must require dependency reports to pass')
  assert(errors, distributionHarness.reportAssemblyRules?.gateClearsOnlyWhenEveryMappedApprovalAreaPasses === true, 'distribution approval harness must require mapped areas for gate clearance')
  assert(errors, distributionHarness.reportAssemblyRules?.blockedHarnessDoesNotClearDistributionGates === true, 'distribution approval harness must preserve blocked gate honesty')
  assert(errors, distributionHarness.reportAssemblyRules?.preflightReportsDoNotClearDistributionGates === true, 'distribution approval harness must preserve preflight/distribution distinction')
  assert(errors, distributionHarness.reportAssemblyRules?.publicAlphaReadyRequiresApprovalSignatureAndReadyReleaseIndex === true, 'distribution approval harness must require signature and ready release index')
  assert(errors, distributionHarness.reportAssemblyRules?.publicAlphaReadyRequiresRuntimeLauncherFinalReviewAndDistributionApproval === true, 'distribution approval harness must require runtime, launcher, final review, and approval readiness')

  const harnessDriverManifest = systemPayloads.harness_driver_manifest_contract
  const harnessFamilyPlans = new Map([
    ['runtime', { plan: runtimeHarness, path: 'systems/runtime_execution_harness_plan.json', bindingKey: 'scenarioBindings', blocker: 'real_runtime_harness_drivers_missing' }],
    ['launcher', { plan: launcherHarness, path: 'systems/launcher_execution_harness_plan.json', bindingKey: 'flowBindings', blocker: 'real_launcher_harness_drivers_missing' }],
    ['finalReview', { plan: finalReviewHarness, path: 'systems/final_release_review_harness_plan.json', bindingKey: 'reviewAreaBindings', blocker: 'final_review_harness_drivers_missing' }],
    ['distributionApproval', { plan: distributionHarness, path: 'systems/distribution_approval_harness_plan.json', bindingKey: 'approvalAreaBindings', blocker: 'distribution_approval_harness_drivers_missing' }],
  ])
  assert(errors, harnessDriverManifest.schema === 'echo.openlands.systems.harness_driver_manifest_contract.v1', 'harness driver manifest contract schema mismatch')
  assert(errors, harnessDriverManifest.namespace === MODULE_ID, 'harness driver manifest contract namespace mismatch')
  assert(errors, harnessDriverManifest.version === descriptor.version, 'harness driver manifest contract version must match descriptor')
  assert(errors, harnessDriverManifest.sourceContracts?.runtimeExecutionHarnessPlan === 'systems/runtime_execution_harness_plan.json', 'harness driver manifest source runtime harness mismatch')
  assert(errors, harnessDriverManifest.sourceContracts?.launcherExecutionHarnessPlan === 'systems/launcher_execution_harness_plan.json', 'harness driver manifest source launcher harness mismatch')
  assert(errors, harnessDriverManifest.sourceContracts?.finalReleaseReviewHarnessPlan === 'systems/final_release_review_harness_plan.json', 'harness driver manifest source final review harness mismatch')
  assert(errors, harnessDriverManifest.sourceContracts?.distributionApprovalHarnessPlan === 'systems/distribution_approval_harness_plan.json', 'harness driver manifest source distribution approval harness mismatch')
  assert(errors, harnessDriverManifest.reportContract?.schema === 'echo.openlands.edition.harness_driver_manifest.v1', 'harness driver manifest edition schema mismatch')
  for (const field of ['schema', 'edition', 'runtimeTarget', 'moduleId', 'moduleVersion', 'generatedAt', 'status', 'sourceContracts', 'harnessFamilies', 'availableDriverSurfaces', 'missingDriverSurfaces', 'blockedBy', 'nextSteps']) {
    assert(errors, harnessDriverManifest.reportContract?.requiredTopLevelFields?.includes(field), `harness driver manifest required field missing ${field}`)
  }
  for (const status of ['template_blocked', 'implementation_partial', 'implementation_ready', 'execution_passed', 'execution_failed']) {
    assert(errors, harnessDriverManifest.reportContract?.allowedStatus?.includes(status), `harness driver manifest allowed status missing ${status}`)
  }
  const expectedManifestTemplates = [
    { edition: 'native', runtimeTarget: 'echo_native', repo: 'ECHO-Openlands-Native-Edition', path: 'evidence/native-harness-driver-manifest.template.json', artifactPattern: 'echoopenlandsprotocol-0.1.0.echo-addon' },
    { edition: 'neoforge', runtimeTarget: 'neoforge', repo: 'ECHO-Openlands-NeoForge-Edition', path: 'evidence/neoforge-harness-driver-manifest.template.json', artifactPattern: 'echoopenlandsprotocol-0.1.0-neoforge.jar' },
    { edition: 'standalone', runtimeTarget: 'echo_runtime_standalone', repo: 'ECHO-Openlands-Standalone-Edition', path: 'evidence/standalone-harness-driver-manifest.template.json', artifactPattern: 'echoopenlandsprotocol-0.1.0-standalone.jar' },
  ]
  assertSameStringSet(errors, (harnessDriverManifest.editionManifestTemplates ?? []).map((entry) => entry.runtimeTarget), EXPECTED_RUNTIMES, 'harness driver manifest edition runtime targets')
  for (const expectedTemplate of expectedManifestTemplates) {
    const template = (harnessDriverManifest.editionManifestTemplates ?? []).find((entry) => entry.edition === expectedTemplate.edition)
    assert(errors, template !== undefined, `harness driver manifest template missing ${expectedTemplate.edition}`)
    if (!template) continue
    for (const [key, value] of Object.entries(expectedTemplate)) {
      assert(errors, template[key] === value, `harness driver manifest ${expectedTemplate.edition} ${key} mismatch`)
    }
  }
  assertSameStringSet(errors, (harnessDriverManifest.harnessFamilies ?? []).map((family) => family.id), [...harnessFamilyPlans.keys()], 'harness driver manifest families')
  for (const family of harnessDriverManifest.harnessFamilies ?? []) {
    requireFields(errors, family, ['id', 'plan', 'bindingKey', 'bindingLabel', 'requiredReportPattern', 'readinessBlocker', 'driverMissingBlocker', 'requiredDriverSurfaceIds', 'requiredBindingIds'], `harness driver manifest family ${family.id}`)
    const expectedFamily = harnessFamilyPlans.get(family.id)
    assert(errors, expectedFamily !== undefined, `harness driver manifest unknown family ${family.id}`)
    if (!expectedFamily) continue
    assert(errors, family.plan === expectedFamily.path, `harness driver manifest family ${family.id} plan mismatch`)
    assert(errors, family.bindingKey === expectedFamily.bindingKey, `harness driver manifest family ${family.id} binding key mismatch`)
    assert(errors, family.driverMissingBlocker === expectedFamily.blocker, `harness driver manifest family ${family.id} driver blocker mismatch`)
    assert(errors, family.requiredReportPattern?.includes('{edition}'), `harness driver manifest family ${family.id} report pattern must include edition placeholder`)
    assertSameStringSet(errors, family.requiredDriverSurfaceIds ?? [], (expectedFamily.plan.driverSurfaces ?? []).map((driver) => driver.id), `harness driver manifest ${family.id} driver surfaces`)
    assertSameStringSet(errors, family.requiredBindingIds ?? [], (expectedFamily.plan[expectedFamily.bindingKey] ?? []).map((binding) => binding.id), `harness driver manifest ${family.id} binding ids`)
  }
  assert(errors, harnessDriverManifest.blockedTemplateRules?.availableDriverSurfacesMustBeEmpty === true, 'harness driver manifest blocked templates must start with no available drivers')
  assert(errors, harnessDriverManifest.blockedTemplateRules?.missingDriverSurfacesMustMatchAllRequiredSurfaces === true, 'harness driver manifest blocked templates must list every missing driver')
  assert(errors, harnessDriverManifest.blockedTemplateRules?.familyStatus === 'template_blocked', 'harness driver manifest blocked family status mismatch')
  for (const blocker of ['real_runtime_harness_drivers_missing', 'real_launcher_harness_drivers_missing', 'final_review_harness_drivers_missing', 'distribution_approval_harness_drivers_missing', 'real_harness_execution_not_run']) {
    assert(errors, harnessDriverManifest.blockedTemplateRules?.requiredBlockedBy?.includes(blocker), `harness driver manifest blocked template missing blocker ${blocker}`)
  }
  for (const rule of harnessDriverManifest.clearingRules ?? []) {
    assert(errors, typeof rule === 'string' && rule.length > 20, 'harness driver manifest clearing rules must be detailed strings')
  }

  const releasePublication = systemPayloads.release_publication_manifest_contract
  const expectedPublicationTargets = [
    { id: 'native', file: 'echoopenlandsprotocol-0.1.0.echo-addon', kind: 'echo-addon', runtimeTarget: 'echo_native', releaseIndexArtifactKind: 'echo-addon' },
    { id: 'standalone', file: 'echoopenlandsprotocol-0.1.0-standalone.jar', kind: 'standalone', runtimeTarget: 'echo_runtime_standalone', releaseIndexArtifactKind: 'standalone' },
    { id: 'neoforge', file: 'echoopenlandsprotocol-0.1.0-neoforge.jar', kind: 'neoforge', runtimeTarget: 'neoforge', releaseIndexArtifactKind: 'neoforge' },
    { id: 'sources', file: 'echoopenlandsprotocol-0.1.0-sources.jar', kind: 'sources', runtimeTarget: 'sources', releaseIndexArtifactKind: 'sources' },
  ]
  const distributionArtifactById = new Map((systemPayloads.distribution_alpha_gates.artifactTargets ?? []).map((target) => [target.id, target]))
  assert(errors, releasePublication.schema === 'echo.openlands.systems.release_publication_manifest_contract.v1', 'release publication manifest contract schema mismatch')
  assert(errors, releasePublication.namespace === MODULE_ID, 'release publication manifest contract namespace mismatch')
  assert(errors, releasePublication.version === descriptor.version, 'release publication manifest contract version must match descriptor')
  assert(errors, releasePublication.sourceContracts?.distribution === 'systems/distribution_alpha_gates.json', 'release publication source distribution mismatch')
  assert(errors, releasePublication.sourceContracts?.distributionApproval === 'systems/distribution_approval_acceptance.json', 'release publication source distribution approval mismatch')
  assert(errors, releasePublication.sourceContracts?.distributionApprovalHarnessPlan === 'systems/distribution_approval_harness_plan.json', 'release publication source distribution approval harness mismatch')
  assert(errors, releasePublication.sourceContracts?.launcherFlow === 'systems/launcher_flow_acceptance.json', 'release publication source launcher flow mismatch')
  assert(errors, releasePublication.sourceContracts?.releaseIndex === 'dist/echo-module-release/echo-release.json', 'release publication source release index mismatch')
  assert(errors, releasePublication.sourceContracts?.readinessReport === 'dist/echo-module-release/echoopenlandsprotocol/openlands-release-readiness-report.json', 'release publication source readiness report mismatch')
  assert(errors, releasePublication.manifestPaths?.blockedTemplate === 'dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.template.json', 'release publication blocked template path mismatch')
  assert(errors, releasePublication.manifestPaths?.approvedManifest === 'dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.json', 'release publication approved manifest path mismatch')
  assert(errors, releasePublication.approvalContract?.schema === 'echo.openlands.release_publication_approval.v1', 'release publication approval schema mismatch')
  for (const field of ['schema', 'moduleId', 'moduleVersion', 'releaseId', 'approver', 'approvedAt', 'releaseIndexPatch', 'distributionApproval', 'checklist']) {
    assert(errors, releasePublication.approvalContract?.requiredTopLevelFields?.includes(field), `release publication approval required field missing ${field}`)
  }
  for (const field of ['signoffId', 'approver', 'approvedAt', 'reports']) {
    assert(errors, releasePublication.approvalContract?.requiredDistributionApprovalFields?.includes(field), `release publication distribution approval field missing ${field}`)
  }
  for (const field of ['edition', 'path', 'sha256']) {
    assert(errors, releasePublication.approvalContract?.requiredDistributionApprovalReportFields?.includes(field), `release publication distribution approval report field missing ${field}`)
  }
  assertSameStringSet(errors, releasePublication.approvalContract?.requiredDistributionApprovalReportEditions ?? [], ['native', 'neoforge', 'standalone'], 'release publication distribution approval report editions')
  assert(errors, releasePublication.reportContract?.schema === 'echo.openlands.release_publication_manifest.v1', 'release publication report schema mismatch')
  for (const field of ['schema', 'status', 'generatedAt', 'moduleId', 'moduleVersion', 'releaseId', 'releaseIndexPath', 'sourceContracts', 'artifactPublications', 'releaseIndexPatchRules', 'blockedBy', 'nextSteps']) {
    assert(errors, releasePublication.reportContract?.requiredTopLevelFields?.includes(field), `release publication required top-level field missing ${field}`)
  }
  for (const status of ['template_blocked', 'urls_pending', 'uploaded_unverified', 'verified', 'approved', 'failed']) {
    assert(errors, releasePublication.reportContract?.allowedStatus?.includes(status), `release publication allowed status missing ${status}`)
  }
  for (const field of ['id', 'file', 'kind', 'runtimeTarget', 'requiredForPublicAlpha', 'sha256', 'size', 'downloadUrl', 'urlStatus', 'uploadProvider', 'storageKey', 'publishedAt', 'downloadVerification', 'releaseIndexPatch']) {
    assert(errors, releasePublication.reportContract?.requiredArtifactPublicationFields?.includes(field), `release publication artifact field missing ${field}`)
  }
  for (const status of ['missing_url', 'url_recorded', 'download_verified', 'approved']) {
    assert(errors, releasePublication.reportContract?.allowedUrlStatus?.includes(status), `release publication URL status missing ${status}`)
  }
  for (const field of ['downloadAttempted', 'downloadedSha256', 'downloadedSize', 'sha256Matches', 'sizeMatches', 'verifiedAt', 'verificationArtifact']) {
    assert(errors, releasePublication.reportContract?.requiredDownloadVerificationFields?.includes(field), `release publication download verification field missing ${field}`)
  }
  assertSameStringSet(errors, (releasePublication.artifactTargets ?? []).map((target) => target.id), expectedPublicationTargets.map((target) => target.id), 'release publication artifact targets')
  for (const expectedTarget of expectedPublicationTargets) {
    const actualTarget = (releasePublication.artifactTargets ?? []).find((target) => target.id === expectedTarget.id)
    const distributionTarget = distributionArtifactById.get(expectedTarget.id)
    assert(errors, actualTarget !== undefined, `release publication artifact target missing ${expectedTarget.id}`)
    assert(errors, distributionTarget !== undefined, `release publication distribution artifact missing ${expectedTarget.id}`)
    if (!actualTarget) continue
    for (const [key, value] of Object.entries(expectedTarget)) {
      assert(errors, actualTarget[key] === value, `release publication target ${expectedTarget.id} ${key} mismatch`)
    }
    assert(errors, actualTarget.file === distributionTarget?.file, `release publication target ${expectedTarget.id} must use distribution artifact file`)
    assert(errors, actualTarget.requiredForPublicAlpha === distributionTarget?.requiredForPublicAlpha, `release publication target ${expectedTarget.id} public alpha flag mismatch`)
  }
  for (const [field, expectedValue] of Object.entries({
    allArtifactUrlsRequired: true,
    downloadVerificationRequired: true,
    releaseIndexCommitRequired: true,
    distributionApprovalRequired: true,
    blockedTemplateDoesNotPatchReleaseIndex: true,
    warningStateRequiredUntilApproved: true,
  })) {
    assert(errors, releasePublication.releaseIndexPatchRules?.[field] === expectedValue, `release publication patch rule ${field} mismatch`)
  }
  assert(errors, releasePublication.releaseIndexPatchRules?.patchTarget === 'modules[].artifacts[].downloadUrl', 'release publication patch target mismatch')
  assert(errors, releasePublication.releaseIndexPatchRules?.publicDownloadUrlProtocol === 'https:', 'release publication public download URL protocol mismatch')
  assertSameStringSet(errors, releasePublication.releaseIndexPatchRules?.matchBy ?? [], ['moduleId', 'version', 'filename', 'sha256', 'size'], 'release publication patch match fields')
  assert(errors, releasePublication.blockedTemplateRules?.status === 'template_blocked', 'release publication blocked template status mismatch')
  assert(errors, releasePublication.blockedTemplateRules?.downloadUrlMustBeEmpty === true, 'release publication blocked template must require empty URLs')
  assert(errors, releasePublication.blockedTemplateRules?.urlStatus === 'missing_url', 'release publication blocked template URL status mismatch')
  for (const blocker of ['release_index_download_urls_missing', 'download_verification_missing', 'release_index_patch_not_approved']) {
    assert(errors, releasePublication.blockedTemplateRules?.requiredBlockedBy?.includes(blocker), `release publication blocked template missing blocker ${blocker}`)
  }
  assert(errors, (releasePublication.blockedTemplateRules?.requiredNextSteps ?? []).length >= expectedPublicationTargets.length, 'release publication blocked template must describe concrete next steps')
  for (const rule of releasePublication.clearingRules ?? []) {
    assert(errors, typeof rule === 'string' && rule.length > 20, 'release publication clearing rules must be detailed strings')
  }

  const forbiddenPublicTerms = legalAudit.forbiddenPublicTerms ?? []
  const publicIdentityValues = [
    ...Object.keys(langPayload),
    ...Object.values(langPayload),
    ...blocks.flatMap((block) => [block.id, normalizeId(block.id), block.displayName, block.model, block.texture, block.category]),
    ...items.flatMap((item) => [item.id, normalizeId(item.id), item.displayName, item.model, item.texture, item.useType]),
    ...recipes.flatMap((recipe) => [recipe.id, normalizeId(recipe.id), recipe.station, ...(recipe.unlockedBy ?? [])]),
    ...biomes.flatMap((biome) => [biome.id, normalizeId(biome.id), biome.displayName]),
    ...structures.flatMap((structure) => [structure.id, normalizeId(structure.id), structure.displayName, structure.holoMapHint, structure.tutorialHook]),
    ...creatures.flatMap((creature) => [creature.id, normalizeId(creature.id), creature.displayName, creature.category]),
    ...Object.keys(soundsPayload),
  ]
  assertNoForbiddenTerms(errors, publicIdentityValues, forbiddenPublicTerms, 'public Openlands identity')

  const waystoneStates = (waystonesPayload.stateMachine ?? []).map((state) => state.state)
  const expectedWaystoneStates = ['undiscovered', 'discovered', 'debris_cleared', 'stone_repaired', 'fitted', 'charged', 'bound', 'active']
  assert(errors, JSON.stringify(waystoneStates) === JSON.stringify(expectedWaystoneStates), `waystone states must be ${expectedWaystoneStates.join(' -> ')}`)
  assert(errors, waystonesPayload.effects?.fastTravel?.requiresActiveStones === 2, 'fast travel must require two active waystones')

  for (const requiredSave of ['inventory', 'hotbar', 'placedBlocks', 'chestContents', 'bedrollSpawn', 'campfireLitState', 'shelterScore', 'waystoneState']) {
    assert(errors, progressionPayload.saveLoadAcceptance?.includes(requiredSave), `first-hour save/load missing ${requiredSave}`)
  }
  assert(errors, progressionPayload.shelterScore?.minimumForSleepMilestone <= 60, 'shelter milestone should stay forgiving')
  for (const phase of ['mvp', 'public_alpha', 'one_dot_zero', 'post_launch']) {
    assert(errors, (launchRoadmapPayload.phases ?? []).some((entry) => entry.id === phase), `launch roadmap missing phase ${phase}`)
  }
  assert(errors, (launchRoadmapPayload.nonNegotiableInvariants ?? []).some((entry) => entry.includes('Hardlands remains optional')), 'launch roadmap must preserve optional Hardlands invariant')
  assert(errors, holomapPayload.regionDataContract?.storedFields?.includes('oldRoadSegments'), 'HoloMap region data must store oldRoadSegments')

  const routeStepIds = (progressionPayload.firstHour ?? []).map((step) => step.id)
  assert(errors, JSON.stringify(playtestPayload.requiredRouteSteps ?? []) === JSON.stringify(routeStepIds), 'playtest requiredRouteSteps must match first_hour_route step ids')
  assert(errors, playtestPayload.defaultMode === 'openlands_standard', 'playtest defaultMode must be openlands_standard')
  const scenarioIds = new Set((playtestPayload.acceptanceScenarios ?? []).map((scenario) => scenario.id))
  for (const routeStep of routeStepIds) {
    assert(errors, scenarioIds.has(routeStep), `playtest missing acceptance scenario for route step ${routeStep}`)
  }
  const assertKnownRefs = (values, knownIds, label) => {
    for (const value of values ?? []) assert(errors, knownIds.has(normalizeId(value)), `${label} references unknown id ${value}`)
  }
  for (const scenario of playtestPayload.acceptanceScenarios ?? []) {
    requireFields(errors, scenario, ['id', 'routeStep', 'targetTimeMinutes', 'requires', 'runtimeActions', 'successEvidence'], `playtest scenario ${scenario.id}`)
    assert(errors, routeStepIds.includes(scenario.routeStep), `playtest scenario ${scenario.id} references unknown routeStep ${scenario.routeStep}`)
    assertKnownRefs(scenario.requires?.blocks, blockIds, `playtest scenario ${scenario.id} blocks`)
    assertKnownRefs(scenario.requires?.items, itemIds, `playtest scenario ${scenario.id} items`)
    assertKnownRefs(scenario.requires?.recipes, recipeIds, `playtest scenario ${scenario.id} recipes`)
    assertKnownRefs(scenario.requires?.landmarks, landmarkIds, `playtest scenario ${scenario.id} landmarks`)
    assertKnownRefs(scenario.requires?.biomes, biomeIds, `playtest scenario ${scenario.id} biomes`)
    assertKnownRefs(scenario.requires?.creaturesAllowed, creatureIds, `playtest scenario ${scenario.id} creaturesAllowed`)
    assertKnownRefs(scenario.requires?.waystoneStates, new Set(expectedWaystoneStates), `playtest scenario ${scenario.id} waystoneStates`)
    assertKnownRefs(scenario.requires?.tutorialPrompts, tutorialPromptIds, `playtest scenario ${scenario.id} tutorialPrompts`)
    assertKnownRefs(scenario.requires?.holomapLayers, holomapLayerIds, `playtest scenario ${scenario.id} holomapLayers`)
    assertKnownRefs(scenario.requires?.hintTypes, holomapHintTypeIds, `playtest scenario ${scenario.id} hintTypes`)
    for (const evidence of scenario.successEvidence ?? []) {
      assert(errors, evidenceIds.has(evidence), `playtest scenario ${scenario.id} references unknown evidence ${evidence}`)
    }
    assert(errors, (scenario.runtimeActions ?? []).length > 0, `playtest scenario ${scenario.id} must define runtimeActions`)
    for (const action of scenario.runtimeActions ?? []) {
      requireFields(errors, action, ['id', 'assertions'], `playtest scenario ${scenario.id} action ${action.id}`)
      assert(errors, (action.assertions ?? []).length > 0, `playtest scenario ${scenario.id} action ${action.id} must define assertions`)
    }
  }
  const shelterScenario = (playtestPayload.acceptanceScenarios ?? []).find((scenario) => scenario.id === 'first_shelter')
  assert(errors, shelterScenario?.shelterScoreMinimum === progressionPayload.shelterScore?.minimumForSleepMilestone, 'playtest shelterScoreMinimum must match first-hour shelter score minimum')
  for (const component of shelterScenario?.shelterComponents ?? []) {
    assert(errors, (progressionPayload.shelterScore?.components ?? []).some((entry) => entry.id === component), `playtest first_shelter references unknown shelter component ${component}`)
  }
  const firstShelterCheckpoint = (playtestPayload.saveLoadCheckpoints ?? []).find((checkpoint) => checkpoint.id === 'after_first_shelter_sleep')
  assert(errors, firstShelterCheckpoint !== undefined, 'playtest missing after_first_shelter_sleep checkpoint')
  for (const requiredSave of progressionPayload.saveLoadAcceptance ?? []) {
    assert(errors, firstShelterCheckpoint?.mustPersist?.includes(requiredSave), `playtest after_first_shelter_sleep checkpoint missing save field ${requiredSave}`)
  }
  for (const checkpoint of playtestPayload.saveLoadCheckpoints ?? []) {
    requireFields(errors, checkpoint, ['id', 'afterScenario', 'mustPersist', 'sampleInventoryItems', 'samplePlacedBlocks', 'requiredAssertions'], `playtest saveLoadCheckpoint ${checkpoint.id}`)
    assert(errors, scenarioIds.has(checkpoint.afterScenario), `playtest checkpoint ${checkpoint.id} references unknown scenario ${checkpoint.afterScenario}`)
    assertKnownRefs(checkpoint.sampleInventoryItems, itemIds, `playtest checkpoint ${checkpoint.id} sampleInventoryItems`)
    assertKnownRefs(checkpoint.samplePlacedBlocks, blockIds, `playtest checkpoint ${checkpoint.id} samplePlacedBlocks`)
    assert(errors, (checkpoint.requiredAssertions ?? []).length > 0, `playtest checkpoint ${checkpoint.id} must define requiredAssertions`)
  }
  const waystonePublicAlpha = playtestPayload.waystonePublicAlphaScenario
  requireFields(errors, waystonePublicAlpha ?? {}, ['id', 'requiresStates', 'requiresItems', 'requiresRecipes', 'requiresBlocks', 'expectedEffects', 'mustPersist', 'successEvidence'], 'playtest waystonePublicAlphaScenario')
  assert(errors, JSON.stringify(waystonePublicAlpha?.requiresStates ?? []) === JSON.stringify(expectedWaystoneStates), 'playtest waystone public alpha scenario must cover every waystone state')
  assertKnownRefs(waystonePublicAlpha?.requiresItems, itemIds, 'playtest waystone public alpha requiresItems')
  assertKnownRefs(waystonePublicAlpha?.requiresRecipes, recipeIds, 'playtest waystone public alpha requiresRecipes')
  assertKnownRefs(waystonePublicAlpha?.requiresBlocks, blockIds, 'playtest waystone public alpha requiresBlocks')
  for (const field of waystonePublicAlpha?.mustPersist ?? []) {
    assert(errors, waystonesPayload.multiplayerState?.storedFields?.includes(field), `playtest waystone public alpha mustPersist unknown field ${field}`)
  }
  for (const evidence of waystonePublicAlpha?.successEvidence ?? []) {
    assert(errors, evidenceIds.has(evidence), `playtest waystone public alpha references unknown evidence ${evidence}`)
  }
  const holomapAcceptance = playtestPayload.holomapAcceptance
  requireFields(errors, holomapAcceptance ?? {}, ['mustPersistFields', 'requiredLayers', 'requiredHintTypes', 'fallbackRequired'], 'playtest holomapAcceptance')
  for (const field of holomapAcceptance?.mustPersistFields ?? []) {
    assert(errors, holomapPayload.regionDataContract?.storedFields?.includes(field), `playtest holomapAcceptance unknown persisted field ${field}`)
  }
  assertKnownRefs(holomapAcceptance?.requiredLayers, holomapLayerIds, 'playtest holomapAcceptance requiredLayers')
  assertKnownRefs(holomapAcceptance?.requiredHintTypes, holomapHintTypeIds, 'playtest holomapAcceptance requiredHintTypes')
  assert(errors, holomapAcceptance?.fallbackRequired === true, 'playtest holomapAcceptance fallbackRequired must be true')
  for (const evidence of playtestPayload.releaseEvidence ?? []) {
    assert(errors, evidenceIds.has(evidence), `playtest releaseEvidence references unknown evidence ${evidence}`)
  }
  for (const evidence of ['first_hour_runtime_playtest_pass', 'waystone_state_save_load_pass']) {
    assert(errors, playtestPayload.releaseEvidence?.includes(evidence), `playtest releaseEvidence missing ${evidence}`)
  }
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'mvp_first_hour_acceptance'), 'cross-platform parity testFixtures missing mvp_first_hour_acceptance')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'playable_runtime_contract'), 'cross-platform parity testFixtures missing playable_runtime_contract')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'runtime_execution_acceptance'), 'cross-platform parity testFixtures missing runtime_execution_acceptance')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'runtime_execution_harness_plan'), 'cross-platform parity testFixtures missing runtime_execution_harness_plan')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'launcher_execution_acceptance'), 'cross-platform parity testFixtures missing launcher_execution_acceptance')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'launcher_execution_harness_plan'), 'cross-platform parity testFixtures missing launcher_execution_harness_plan')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'final_release_review_acceptance'), 'cross-platform parity testFixtures missing final_release_review_acceptance')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'final_release_review_harness_plan'), 'cross-platform parity testFixtures missing final_release_review_harness_plan')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'distribution_approval_acceptance'), 'cross-platform parity testFixtures missing distribution_approval_acceptance')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'distribution_approval_harness_plan'), 'cross-platform parity testFixtures missing distribution_approval_harness_plan')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'release_publication_manifest_contract'), 'cross-platform parity testFixtures missing release_publication_manifest_contract')
  assert(errors, (systemPayloads.cross_platform_parity.testFixtures ?? []).some((fixture) => fixture.id === 'harness_driver_manifest_contract'), 'cross-platform parity testFixtures missing harness_driver_manifest_contract')

  const playableRuntime = systemPayloads.playable_runtime_contract
  assert(errors, playableRuntime.sourceContracts?.gameModes === 'config/game_modes.json', 'playable runtime sourceContracts gameModes mismatch')
  assert(errors, playableRuntime.sourceContracts?.firstHourRoute === 'progression/first_hour_route.json', 'playable runtime sourceContracts firstHourRoute mismatch')
  assert(errors, playableRuntime.sourceContracts?.playtestFixture === 'playtests/mvp_first_hour_acceptance.json', 'playable runtime sourceContracts playtestFixture mismatch')
  assert(errors, playableRuntime.sourceContracts?.waystones === 'waystones/waystone_contract.json', 'playable runtime sourceContracts waystones mismatch')
  assert(errors, playableRuntime.sourceContracts?.homesteadAlpha === 'systems/homestead_alpha.json', 'playable runtime sourceContracts homesteadAlpha mismatch')
  assert(errors, playableRuntime.sourceContracts?.builderUxAlpha === 'systems/builder_ux_alpha.json', 'playable runtime sourceContracts builderUxAlpha mismatch')
  assert(errors, playableRuntime.sourceContracts?.coopSmp === 'systems/coop_and_smp.json', 'playable runtime sourceContracts coopSmp mismatch')
  assert(errors, playableRuntime.sourceContracts?.runtimeAdapterLoadPlan === 'systems/runtime_adapter_load_plan.json', 'playable runtime sourceContracts runtime adapter mismatch')
  assert(errors, playableRuntime.runtimeCore?.package === 'com.knoxhack.echoopenlandsprotocol.runtime', 'playable runtime package mismatch')
  assert(errors, playableRuntime.runtimeCore?.entrypointClass === 'OpenlandsFirstHourRuntime', 'playable runtime entrypoint class mismatch')
  assert(errors, playableRuntime.runtimeCore?.adapterManifestMethod === 'adapterBindingManifest', 'playable runtime adapter manifest method mismatch')
  assert(errors, playableRuntime.runtimeCore?.compileScope === 'pure_java_no_platform_classes', 'playable runtime compileScope must remain pure Java')
  for (const className of playableRuntime.runtimeCore?.requiredClasses ?? []) {
    assert(errors, runtimeSourceFiles.includes(`${className}.java`), `playable runtime missing Java class file ${className}.java`)
    assert(errors,
      runtimeSource.includes(`class ${className}`) || runtimeSource.includes(`record ${className}`) || runtimeSource.includes(`enum ${className}`),
      `playable runtime source missing declaration for ${className}`)
  }
  for (const requiredSourceToken of [
    'OpenlandsFirstHourRuntime.standardRules',
    'OpenlandsFirstHourRuntime.validateStarterSpawn',
    'OpenlandsFirstHourRuntime.scoreShelter',
    'OpenlandsFirstHourRuntime.advanceWaystone',
    'OpenlandsFirstHourRuntime.advanceCrop',
    'OpenlandsFirstHourRuntime.cookpotMealReady',
    'OpenlandsFirstHourRuntime.validateBuilderAction',
    'OpenlandsFirstHourRuntime.firstHourStepIds',
    'OpenlandsShelterScoring.MINIMUM_FOR_SLEEP_MILESTONE',
    'OpenlandsWaystoneRuntime.ACTIVE_STONES_REQUIRED_FOR_FAST_TRAVEL',
  ]) {
    assert(errors, runtimeSource.includes(requiredSourceToken) || JSON.stringify(playableRuntime).includes(requiredSourceToken), `playable runtime missing hook token ${requiredSourceToken}`)
  }
  const runtimeHookIds = new Set((playableRuntime.firstHourRuntimeHooks ?? []).map((hook) => hook.id))
  const expectedRuntimeHookIds = ['standard_mode_rules', 'starter_spawn_guarantees', 'shelter_score', 'waystone_state_machine', 'homestead_crop_growth', 'cookpot_meal_ready', 'builder_ux_action', 'first_hour_route', 'adapter_ready_signal']
  for (const hookId of expectedRuntimeHookIds) assert(errors, runtimeHookIds.has(hookId), `playable runtime missing hook ${hookId}`)
  for (const hook of playableRuntime.firstHourRuntimeHooks ?? []) {
    requireFields(errors, hook, ['id', 'method', 'inputs', 'outputs', 'requiredEvidence'], `playable runtime hook ${hook.id}`)
    assert(errors, String(hook.method).startsWith('OpenlandsFirstHourRuntime.'), `playable runtime hook ${hook.id} must use OpenlandsFirstHourRuntime`)
    assert(errors, (hook.outputs ?? []).length > 0, `playable runtime hook ${hook.id} must define outputs`)
    for (const evidence of hook.requiredEvidence ?? []) {
      assert(errors, evidenceIds.has(evidence), `playable runtime hook ${hook.id} references unknown evidence ${evidence}`)
    }
  }
  const routeHook = (playableRuntime.firstHourRuntimeHooks ?? []).find((hook) => hook.id === 'first_hour_route')
  assert(errors, JSON.stringify(routeHook?.outputs ?? []) === JSON.stringify(routeStepIds), 'playable runtime first_hour_route outputs must match first hour route step ids')
  const standardHook = (playableRuntime.firstHourRuntimeHooks ?? []).find((hook) => hook.id === 'standard_mode_rules')
  for (const output of ['modeId', 'hunger', 'stamina', 'hydration', 'foodSpoilage', 'temperatureDamage', 'deathPack', 'hardcoreMetersOff']) {
    assert(errors, standardHook?.outputs?.includes(output), `playable runtime standard_mode_rules missing output ${output}`)
  }
  assert(errors, JSON.stringify(sortedUnique(playableRuntime.starterSpawnRules?.allowedStarterBiomes ?? [])) === JSON.stringify(['meadows', 'woodlands']), 'playable runtime starter biomes must be meadows and woodlands')
  assert(errors, playableRuntime.starterSpawnRules?.guaranteedResourceRadiusBlocks === 64, 'playable runtime starter resource radius must be 64')
  assert(errors, playableRuntime.starterSpawnRules?.visibleLandmarkRadiusBlocks === 128, 'playable runtime visible landmark radius must be 128')
  assert(errors, playableRuntime.starterSpawnRules?.minimumHostileClearRadiusBlocks === 16, 'playable runtime hostile clear radius must be 16')
  for (const signal of ['wood_source_found', 'loose_stone_found', 'fiber_source_found', 'starter_food_found', 'water_or_well_hint_found', 'cave_road_or_ruin_hook_found']) {
    assert(errors, playableRuntime.starterSpawnRules?.requiredResourceSignals?.includes(signal), `playable runtime starter spawn missing signal ${signal}`)
  }
  assert(errors, playableRuntime.shelterScoring?.minimumForSleepMilestone === progressionPayload.shelterScore?.minimumForSleepMilestone, 'playable runtime shelter minimum must match progression')
  assert(errors, playableRuntime.shelterScoring?.idealScore === progressionPayload.shelterScore?.idealScore, 'playable runtime shelter ideal score must match progression')
  const progressionShelterComponents = new Map((progressionPayload.shelterScore?.components ?? []).map((component) => [component.id, component]))
  for (const component of playableRuntime.shelterScoring?.components ?? []) {
    requireFields(errors, component, ['id', 'maxPoints', 'runtimeInput'], `playable runtime shelter component ${component.id}`)
    assert(errors, progressionShelterComponents.has(component.id), `playable runtime shelter component ${component.id} not found in progression`)
    assert(errors, progressionShelterComponents.get(component.id)?.maxPoints === component.maxPoints, `playable runtime shelter component ${component.id} maxPoints mismatch`)
  }
  assert(errors, JSON.stringify(playableRuntime.waystoneTransitions?.stateOrder ?? []) === JSON.stringify(expectedWaystoneStates), 'playable runtime waystone state order mismatch')
  for (const [state, inputs] of Object.entries(playableRuntime.waystoneTransitions?.requiredInputsByState ?? {})) {
    assert(errors, expectedWaystoneStates.includes(state), `playable runtime waystone input state unknown ${state}`)
    for (const [id, count] of Object.entries(inputs ?? {})) {
      assert(errors, itemIds.has(normalizeId(id)) || blockIds.has(normalizeId(id)), `playable runtime waystone state ${state} references unknown input ${id}`)
      assert(errors, Number.isInteger(count) && count >= 1, `playable runtime waystone state ${state} input ${id} count must be positive`)
    }
  }
  for (const alternate of playableRuntime.waystoneTransitions?.alternateInputs ?? []) {
    assert(errors, expectedWaystoneStates.includes(alternate.state), `playable runtime alternate input state unknown ${alternate.state}`)
    for (const id of Object.keys(alternate.accept ?? {})) {
      assert(errors, itemIds.has(normalizeId(id)) || blockIds.has(normalizeId(id)), `playable runtime alternate input references unknown id ${id}`)
    }
  }
  assert(errors, playableRuntime.waystoneTransitions?.activeStonesRequiredForFastTravel === waystonesPayload.effects?.fastTravel?.requiresActiveStones, 'playable runtime fast travel count must match waystone contract')
  for (const requiredSave of progressionPayload.saveLoadAcceptance ?? []) {
    assert(errors, playableRuntime.saveLoadSnapshot?.requiredFields?.includes(requiredSave), `playable runtime saveLoadSnapshot missing ${requiredSave}`)
  }
  const adapterBindingsByRuntime = new Map((playableRuntime.adapterBindings ?? []).map((binding) => [binding.runtimeTarget, binding]))
  for (const runtime of EXPECTED_RUNTIMES) {
    const binding = adapterBindingsByRuntime.get(runtime)
    assert(errors, binding !== undefined, `playable runtime adapter binding missing ${runtime}`)
    if (binding) {
      for (const hookId of expectedRuntimeHookIds) {
        assert(errors, binding.mustCallHooks?.includes(hookId), `playable runtime adapter binding ${runtime} missing hook ${hookId}`)
      }
      assert(errors, binding.evidenceAttachment?.startsWith('openlands-') && binding.evidenceAttachment?.endsWith('-runtime-core-report.json'), `playable runtime adapter binding ${runtime} evidence attachment must be runtime-core report json`)
    }
  }
  requireRuntimeParity(errors, { runtimeTargets: (playableRuntime.adapterBindings ?? []).map((binding) => binding.runtimeTarget) }, 'playable runtime adapter bindings')
  assert(errors, JSON.stringify(playableRuntime.smokeHarness?.requiredScenarioIds ?? []) === JSON.stringify(routeStepIds), 'playable runtime smokeHarness scenarios must match first hour route')
  for (const proof of ['standard_rules_keep_hardcore_meters_off', 'valid_starter_spawn_is_accepted', 'invalid_starter_spawn_reports_missing_signals', 'forgiving_shelter_score_reaches_sleep_threshold', 'weak_shelter_score_does_not_reach_sleep_threshold', 'waystone_advances_to_active_with_required_inputs', 'two_active_waystones_unlock_fast_travel', 'standard_crop_pauses_without_dying', 'watered_composted_crop_advances', 'cookpot_requires_three_ingredients_and_cook_time', 'builder_hammer_requires_server_validation', 'storage_commands_require_permission_and_server_authority']) {
    assert(errors, playableRuntime.smokeHarness?.mustProve?.includes(proof), `playable runtime smokeHarness missing proof ${proof}`)
  }
  for (const evidence of playableRuntime.smokeHarness?.releaseEvidence ?? []) {
    assert(errors, evidenceIds.has(evidence), `playable runtime smokeHarness references unknown release evidence ${evidence}`)
  }
  const adapterLoadStepById = new Map((runtimeAdapter.loadSteps ?? []).map((step) => [step.id, step]))
  for (const stepId of ['load_world_and_system_payloads', 'bind_first_hour_shelter_and_save_load', 'bind_waystones_holomap_and_multiplayer_state', 'bind_homestead_builder_and_audio', 'report_runtime_ready']) {
    assert(errors, adapterLoadStepById.get(stepId)?.resourceIds?.includes('systems/playable_runtime_contract'), `runtime adapter step ${stepId} must include systems/playable_runtime_contract`)
  }

  const soundKeys = new Set(Object.keys(soundsPayload))
  for (const family of soundContractPayload.soundFamilies ?? []) {
    assert(errors, soundKeys.has(soundKey(family.assetKey)), `sound contract asset missing ${family.assetKey}`)
  }

  const homestead = systemPayloads.homestead_alpha
  assert(errors, (homestead.crops ?? []).length >= 3, 'homestead must define at least three crops')
  assert(errors, homestead.defaultModeRule?.includes('do not die in Openlands Standard'), 'homestead Standard rule must keep crops relaxed')
  for (const crop of homestead.crops ?? []) {
    assert(errors, itemIds.has(crop.seedItem), `homestead crop ${crop.id} seedItem missing ${crop.seedItem}`)
    assert(errors, itemIds.has(crop.harvestItem), `homestead crop ${crop.id} harvestItem missing ${crop.harvestItem}`)
  }
  const builder = systemPayloads.builder_ux_alpha
  for (const command of ['quick_stack', 'quick_deposit', 'sort_inventory', 'craft_from_nearby_storage', 'named_chests']) {
    assert(errors, (builder.inventoryCommands ?? []).some((entry) => entry.id === command), `builder UX missing ${command}`)
  }
  const parity = systemPayloads.cross_platform_parity
  assert(errors, sortedUnique((parity.runtimeTargets ?? []).map((runtime) => runtime.id)).join(',') === EXPECTED_RUNTIMES.join(','), 'cross-platform parity runtimeTargets mismatch')
  const distribution = systemPayloads.distribution_alpha_gates
  for (const artifact of ['echoopenlandsprotocol-0.1.0.echo-addon', 'echoopenlandsprotocol-0.1.0-standalone.jar', 'echoopenlandsprotocol-0.1.0-neoforge.jar', 'echoopenlandsprotocol-0.1.0-sources.jar']) {
    assert(errors, (distribution.artifactTargets ?? []).some((entry) => entry.file === artifact), `distribution gates missing artifact ${artifact}`)
  }
  const coop = systemPayloads.coop_and_smp
  for (const shared of ['waystones', 'containers', 'homestead_claims', 'holomap_markers']) {
    assert(errors, (coop.sharedState ?? []).some((entry) => entry.id === shared), `co-op contract missing shared state ${shared}`)
  }

  if (requireArtifacts) {
    const distDir = path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release', MODULE_ID)
    for (const target of distribution.artifactTargets ?? []) {
      assert(errors, fileExists(path.join(distDir, target.file)), `required local artifact missing ${target.file}`)
    }
    const releaseManifestPath = path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release', 'echo-release.json')
    assert(errors, fileExists(releaseManifestPath), `required release manifest missing ${releaseManifestPath}`)
    if (fileExists(releaseManifestPath)) {
      const releaseManifest = readJson(releaseManifestPath)
      const moduleRecord = (releaseManifest.modules ?? []).find((record) => record.moduleId === MODULE_ID)
      assert(errors, moduleRecord !== undefined, `release manifest missing ${MODULE_ID} module record`)
      if (moduleRecord) {
        const runtimeArtifacts = (moduleRecord.artifacts ?? []).filter((artifact) => artifact.kind !== 'sources')
        assert(errors, runtimeArtifacts.length >= 3, 'Openlands release manifest should expose runtime artifacts for neoforge, standalone, and echo-addon')
        for (const artifact of runtimeArtifacts) {
          assert(errors, artifact.buildMode === 'compiled-runtime', `Openlands artifact ${artifact.filename} must be compiled-runtime, found ${artifact.buildMode}`)
          assert(errors, fileExists(path.join(distDir, artifact.filename)), `Openlands release manifest artifact missing on disk ${artifact.filename}`)
        }
      }
    }
  } else {
    warnings.push('artifact presence not enforced; pass --require-artifacts to require local dist outputs')
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    moduleRoot,
    counts: {
      blocks: blocks.length,
      items: items.length,
      recipes: recipes.length,
      biomes: biomes.length,
      structures: structures.length,
      creatures: creatures.length,
      gameplayCatalogBlocks: gameplayBlockEntries.length,
      gameplayCatalogItems: gameplayItemEntries.length,
      productionPhaseCheckpoints: productionCheckpoints.length,
      playtestScenarios: (playtestPayload.acceptanceScenarios ?? []).length,
      systems: Object.keys(systemPayloads).length,
    },
    errors,
    warnings,
  }
}

function printHelp() {
  console.log(`Usage: node scripts/validate-openlands-contract.mjs [options]

Options:
  --module-root <path>   Path to addons/echoopenlandsprotocol. Auto-detected by default.
  --require-artifacts    Require generated dist/echo-module-release artifacts.
  --json                 Print JSON output.
  --help                 Show this help.
`)
}

try {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
  } else {
    const moduleRoot = findModuleRoot(args.moduleRoot)
    const result = validate({ moduleRoot, requireArtifacts: args.requireArtifacts })
    if (args.json) {
      console.log(JSON.stringify(result, null, 2))
    } else if (result.status === 'passed') {
      console.log(`Openlands contract validation passed: ${result.counts.blocks} blocks, ${result.counts.items} items, ${result.counts.recipes} recipes, ${result.counts.biomes} biomes, ${result.counts.creatures} creatures, ${result.counts.gameplayCatalogBlocks + result.counts.gameplayCatalogItems} gameplay catalog entries, ${result.counts.productionPhaseCheckpoints} production checkpoints, ${result.counts.playtestScenarios} playtest scenarios, ${result.counts.systems} systems.`)
      for (const warning of result.warnings) console.warn(`warning: ${warning}`)
    } else {
      console.error(`Openlands contract validation failed with ${result.errors.length} error(s):`)
      for (const error of result.errors) console.error(`- ${error}`)
      process.exitCode = 1
    }
  }
} catch (error) {
  console.error(error.message)
  process.exitCode = 1
}

import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import { spawnSync } from 'node:child_process'
import {
  addFoundationKnownBlocks,
  addFoundationKnownItems,
  addFoundationKnownRecipes,
} from './openlands-foundation-id-resolver.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const RUNTIME_EVIDENCE_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json'
const PLAYTEST_FIXTURE = 'data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json'
const LEGAL_AUDIT_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/legal_content_audit.json'
const CONTENT_POLICY_CONTRACT = 'data/echoopenlandsprotocol/openlands/config/content_policy.json'
const ASSET_MANIFEST_CONTRACT = 'assets/echoopenlandsprotocol/asset_manifest.json'
const LAUNCHER_FLOW_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json'
const WAYSTONE_CONTRACT = 'data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json'
const HOLOMAP_CONTRACT = 'data/echoopenlandsprotocol/openlands/holomap/mvp_regions.json'
const CONFORMANCE_REGISTRY = 'data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json'
const CROSS_PLATFORM_PARITY_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/cross_platform_parity.json'
const FIRST_HOUR_ROUTE_CONTRACT = 'data/echoopenlandsprotocol/openlands/progression/first_hour_route.json'
const GAME_MODES_CONTRACT = 'data/echoopenlandsprotocol/openlands/config/game_modes.json'
const BLOCKS_CONTRACT = 'data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json'
const ITEMS_CONTRACT = 'data/echoopenlandsprotocol/openlands/items/mvp_items.json'
const RECIPES_CONTRACT = 'data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json'
const BIOMES_CONTRACT = 'data/echoopenlandsprotocol/openlands/biomes/mvp_biomes.json'
const STRUCTURES_CONTRACT = 'data/echoopenlandsprotocol/openlands/structures/mvp_landmarks.json'
const CREATURES_CONTRACT = 'data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json'
const LOOT_CONTRACT = 'data/echoopenlandsprotocol/openlands/loot/mvp_loot.json'
const TAGS_CONTRACT = 'data/echoopenlandsprotocol/openlands/tags/mvp_tags.json'
const TUTORIALS_CONTRACT = 'data/echoopenlandsprotocol/openlands/tutorials/first_hour_prompts.json'
const SOUNDS_ASSET_CONTRACT = 'assets/echoopenlandsprotocol/sounds.json'
const PLAYABLE_RUNTIME_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json'
const RUNTIME_EXECUTION_ACCEPTANCE = 'data/echoopenlandsprotocol/openlands/systems/runtime_execution_acceptance.json'
const RUNTIME_EXECUTION_HARNESS_PLAN = 'data/echoopenlandsprotocol/openlands/systems/runtime_execution_harness_plan.json'
const HARNESS_DRIVER_MANIFEST_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/harness_driver_manifest_contract.json'
const LAUNCHER_EXECUTION_ACCEPTANCE = 'data/echoopenlandsprotocol/openlands/systems/launcher_execution_acceptance.json'
const LAUNCHER_EXECUTION_HARNESS_PLAN = 'data/echoopenlandsprotocol/openlands/systems/launcher_execution_harness_plan.json'
const FINAL_RELEASE_REVIEW_ACCEPTANCE = 'data/echoopenlandsprotocol/openlands/systems/final_release_review_acceptance.json'
const FINAL_RELEASE_REVIEW_HARNESS_PLAN = 'data/echoopenlandsprotocol/openlands/systems/final_release_review_harness_plan.json'
const DISTRIBUTION_APPROVAL_ACCEPTANCE = 'data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json'
const DISTRIBUTION_APPROVAL_HARNESS_PLAN = 'data/echoopenlandsprotocol/openlands/systems/distribution_approval_harness_plan.json'
const RELEASE_PUBLICATION_MANIFEST_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json'
const HOMESTEAD_ALPHA_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/homestead_alpha.json'
const BUILDER_UX_ALPHA_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/builder_ux_alpha.json'
const COOP_SMP_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/coop_and_smp.json'
const DISTRIBUTION_ALPHA_GATES_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json'
const LAUNCH_ROADMAP_CONTRACT = 'data/echoopenlandsprotocol/openlands/progression/launch_roadmap.json'
const REQUIRED_RUNTIME_CORE_PROOFS = [
  'artifact_contains_playable_runtime_contract',
  'adapter_manifest_exposes_playable_runtime_core',
  'standard_rules_keep_hardcore_meters_off',
  'valid_starter_spawn_is_accepted',
  'invalid_starter_spawn_reports_missing_signals',
  'forgiving_shelter_score_reaches_sleep_threshold',
  'weak_shelter_score_does_not_reach_sleep_threshold',
  'waystone_advances_to_active_with_required_inputs',
  'two_active_waystones_unlock_fast_travel',
  'standard_crop_pauses_without_dying',
  'watered_composted_crop_advances',
  'cookpot_requires_three_ingredients_and_cook_time',
  'builder_hammer_requires_server_validation',
  'storage_commands_require_permission_and_server_authority',
]
const RUNTIME_CORE_ARTIFACT_PROOFS = [
  'artifact_contains_runtime_core_sources',
  'artifact_contains_compiled_runtime_core_classes',
]
const REQUIRED_ADAPTER_BOOT_PROOFS = [
  'adapter_load_plan_loaded',
  'descriptor_identity_verified',
  'runtime_target_accepted',
  'adapter_phases_match_evidence_template',
  'load_step_resources_exist_in_source',
  'compiled_artifact_contains_load_step_resources',
  'load_step_evidence_ids_resolve',
  'adapter_ready_signal_declared',
  'compiled_artifact_contains_runtime_contracts',
  'public_alpha_blocked_until_real_adapter_boot',
]
const REQUIRED_REGISTRY_PARITY_PROOFS = [
  'conformance_registry_loaded',
  'block_item_recipe_biome_creature_ids_match_conformance',
  'registry_references_resolve',
  'cross_platform_parity_target_matches_edition',
  'parity_surfaces_declared',
  'standard_mode_rules_match_parity_surface',
  'first_hour_save_load_fields_match_parity_surface',
  'waystone_state_machine_matches_parity_surface',
  'compiled_artifact_contains_registry_contracts',
  'public_alpha_blocked_until_real_registry_parity_execution',
]
const REQUIRED_CRAFTING_STATION_PROOFS = [
  'recipe_contract_loaded',
  'station_surfaces_declared',
  'recipe_refs_resolve_to_mvp_registry',
  'recipe_station_counts_match_mvp',
  'heated_station_processes_have_timings',
  'map_table_route_recipes_declared',
  'freeform_crafting_identity_declared',
  'deferred_station_blocks_recorded',
  'compiled_artifact_contains_recipe_contracts',
  'public_alpha_blocked_until_real_station_runtime_execution',
]
const REQUIRED_WORLDGEN_EXPLORATION_PROOFS = [
  'biome_contract_loaded',
  'landmark_contract_loaded',
  'creature_contract_loaded',
  'holomap_contract_loaded',
  'starter_spawn_safety_declared',
  'biome_palettes_and_resources_resolve',
  'landmark_blocks_loot_and_biomes_resolve',
  'creature_spawn_tables_and_sounds_resolve',
  'holomap_layers_hints_and_region_fields_resolve',
  'worldgen_semantic_markers_recorded',
  'compiled_artifact_contains_worldgen_exploration_contracts',
  'public_alpha_blocked_until_real_worldgen_execution',
]
const EXPECTED_WORLDGEN_BIOMES = ['meadows', 'woodlands', 'stonehills', 'marshlands']
const EXPECTED_WORLDGEN_LANDMARKS = [
  'ruined_well',
  'road_marker',
  'tiny_camp',
  'watchtower',
  'old_mine',
  'broken_bridge',
  'cellar_entrance',
  'broken_waystone_site',
]
const EXPECTED_WORLDGEN_CREATURES = ['hare', 'deer', 'boar', 'goat', 'marsh_hen', 'fish', 'greyling', 'bristleback', 'hollow_stalker', 'mire_leech']
const EXPECTED_HOLOMAP_LAYERS = ['region_names', 'old_roads', 'waystones', 'nearby_hints', 'player_markers']
const EXPECTED_HOLOMAP_HINT_TYPES = ['cave_mouth', 'old_mine', 'ruin', 'resource_patch', 'road_segment']
const REQUIRED_CREATURE_ROSTER_PROOFS = [
  'creature_contract_loaded',
  'creature_ids_match_conformance',
  'creature_spawn_rules_resolve',
  'creature_drop_tables_resolve',
  'creature_sound_keys_resolve',
  'creature_ai_hints_declared',
  'starter_creature_safety_declared',
  'worldgen_load_step_binds_creatures',
  'compiled_artifact_contains_creature_contracts',
  'public_alpha_blocked_until_real_creature_runtime_execution',
]
const EXPECTED_CREATURE_CATEGORY_COUNTS = {
  passive_small: 2,
  passive_large: 2,
  neutral: 1,
  aquatic_passive: 1,
  hostile_small: 2,
  hostile_large: 1,
  hostile_rare: 1,
}
const REQUIRED_OLD_ROAD_NETWORK_PROOFS = [
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
]
const EXPECTED_OLD_ROAD_BLOCKS = ['old_road_block', 'old_road_marker', 'broken_waystone', 'restored_waystone', 'waystone_plinth']
const EXPECTED_ROUTE_ITEMS = ['region_rubbing', 'old_road_token', 'waystone_core', 'route_binding']
const EXPECTED_ROUTE_RECIPES = ['region_rubbing', 'old_road_token', 'waystone_core', 'route_binding']
const EXPECTED_REQUIRED_ROAD_LANDMARKS = ['road_marker', 'broken_bridge', 'broken_waystone_site']
const REQUIRED_ALPHA_SYSTEMS_PROOFS = [
  'homestead_contract_loaded',
  'builder_ux_contract_loaded',
  'coop_smp_contract_loaded',
  'homestead_refs_resolve_to_mvp_registry',
  'builder_ux_refs_resolve_to_mvp_registry',
  'coop_state_permissions_declared',
  'standard_homestead_rules_are_relaxed',
  'cookpot_trader_and_pen_surfaces_declared',
  'builder_inventory_commands_declared',
  'compiled_artifact_contains_alpha_system_contracts',
  'public_alpha_blocked_until_real_alpha_systems_execution',
]
const REQUIRED_LAUNCHER_FLOW_PROOFS = [
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
]
const REQUIRED_LAUNCHER_FLOW_RUNTIME_ENTRIES = [
  LAUNCHER_FLOW_CONTRACT,
  RUNTIME_EVIDENCE_CONTRACT,
  PLAYABLE_RUNTIME_CONTRACT,
  LAUNCHER_EXECUTION_ACCEPTANCE,
  'com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.class',
]
const REQUIRED_LEGAL_AUDIT_PROOFS = [
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
const REQUIRED_LEGAL_AUDIT_RUNTIME_ENTRIES = [
  CONTENT_POLICY_CONTRACT,
  LEGAL_AUDIT_CONTRACT,
  FINAL_RELEASE_REVIEW_ACCEPTANCE,
  ASSET_MANIFEST_CONTRACT,
  'assets/echoopenlandsprotocol/lang/en_us.json',
  'com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.class',
]
const REQUIRED_FIRST_HOUR_RUNTIME_ENTRIES = [
  PLAYTEST_FIXTURE,
  FIRST_HOUR_ROUTE_CONTRACT,
  TUTORIALS_CONTRACT,
  HOLOMAP_CONTRACT,
  'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsFirstHourRuntime.class',
]
const REQUIRED_FIRST_HOUR_PROOFS = [
  'first_hour_fixture_loaded',
  'first_hour_route_order_matches_fixture',
  'scenario_references_resolve_to_mvp_registries',
  'scenario_runtime_actions_have_assertions',
  'scenario_success_evidence_resolves',
  'save_load_checkpoints_cover_first_hour_fields',
  'holomap_layers_and_hint_types_resolve',
  'runtime_core_report_passed',
  'compiled_artifact_contains_first_hour_contracts',
  'public_alpha_blocked_until_real_first_hour_playtest',
]
const REQUIRED_WAYSTONE_SAVE_LOAD_RUNTIME_ENTRIES = [
  WAYSTONE_CONTRACT,
  PLAYTEST_FIXTURE,
  HOLOMAP_CONTRACT,
  'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsWaystoneRuntime.class',
  'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsWaystoneState.class',
]
const REQUIRED_WAYSTONE_SAVE_LOAD_PROOFS = [
  'waystone_contract_loaded',
  'waystone_state_machine_order_matches_runtime',
  'waystone_repair_inputs_resolve',
  'first_waystone_save_load_checkpoint_covers_required_fields',
  'public_alpha_waystone_scenario_resolves',
  'two_active_waystones_required_for_fast_travel',
  'holomap_persistence_fields_resolve',
  'multiplayer_permission_fields_persist',
  'runtime_core_report_passed',
  'compiled_artifact_contains_waystone_contracts',
  'public_alpha_blocked_until_real_waystone_save_load',
]
const REQUIRED_DISTRIBUTION_ROADMAP_PROOFS = [
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
const EXPECTED_DISTRIBUTION_ARTIFACT_TARGETS = ['native', 'standalone', 'neoforge', 'sources']
const EXPECTED_ROADMAP_PHASES = ['mvp', 'public_alpha', 'one_dot_zero', 'post_launch']
const REQUIRED_DOCS = [
  'README.md',
  'release-manifest.template.json',
  'docs/install.md',
  'docs/update-flow.md',
  'docs/rollback.md',
  'docs/troubleshooting.md',
  'docs/module-requirements.md',
  'docs/runtime-evidence.md',
  'evidence/runtime-evidence.template.json',
]
const REQUIRED_MODULES = [
  'echocore',
  'echonetcore',
  'echoadaptercore',
  'echocontentcore',
  'echoworldcore',
  'echorecipecore',
  'echobiomecore',
  'echostructurecore',
  'echocreaturecore',
  'echofoundationcore',
  'echomaterialcore',
  'echotoolcore',
  'echostationcore',
  'echoworldstarter',
  'echocommonloot',
  'echocreatureroles',
  'echoprogressioncore',
  'echoassetcore',
  MODULE_ID,
]

const EDITIONS = [
  {
    key: 'native',
    directory: 'ECHO-Openlands-Native-Edition',
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    sourceRepo: 'knoxhack/ECHO-Openlands-Native-Edition',
    runtimeTarget: 'echo_native',
    loader: 'echo-native-loader',
    moduleArtifactFamily: 'echo-addon',
    moduleArtifactPattern: '<module>-<version>.echo-addon',
    requiredModuleDescriptors: ['META-INF/echo.mod.json'],
    docsMustContain: ['echo_native', 'EchoOpenlandsNativeModule', 'adapterBootstrapStepIds'],
    harnessRequirementKey: 'nativeHarnessRequirements',
    attachmentPrefix: 'native',
    harnessDriverManifestTemplate: 'evidence/native-harness-driver-manifest.template.json',
  },
  {
    key: 'neoforge',
    directory: 'ECHO-Openlands-NeoForge-Edition',
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    sourceRepo: 'knoxhack/ECHO-Openlands-NeoForge-Edition',
    runtimeTarget: 'neoforge',
    loader: 'neoforge',
    moduleArtifactFamily: 'neoforge',
    moduleArtifactPattern: '<module>-<version>-neoforge.jar',
    requiredModuleDescriptors: ['META-INF/echo.mod.json', 'META-INF/neoforge.mods.toml'],
    docsMustContain: ['neoforge', 'ResourceLocation', 'generated'],
    harnessRequirementKey: 'neoForgeHarnessRequirements',
    attachmentPrefix: 'neoforge',
    harnessDriverManifestTemplate: 'evidence/neoforge-harness-driver-manifest.template.json',
  },
  {
    key: 'standalone',
    directory: 'ECHO-Openlands-Standalone-Edition',
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    sourceRepo: 'knoxhack/ECHO-Openlands-Standalone-Edition',
    runtimeTarget: 'echo_runtime_standalone',
    loader: 'echo-standalone-runtime',
    moduleArtifactFamily: 'standalone',
    moduleArtifactPattern: '<module>-<version>-standalone.jar',
    requiredModuleDescriptors: ['META-INF/echo.mod.json'],
    docsMustContain: ['echo_runtime_standalone', 'without Minecraft classes', 'first-frame'],
    harnessRequirementKey: 'standaloneHarnessRequirements',
    attachmentPrefix: 'standalone',
    harnessDriverManifestTemplate: 'evidence/standalone-harness-driver-manifest.template.json',
  },
]

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    readinessReport: null,
    json: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--workspace-root') args.workspaceRoot = argv[++index]
    else if (arg === '--readiness-report') args.readinessReport = argv[++index]
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function fileExists(filePath) {
  return fs.existsSync(filePath)
}

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function readJson(filePath) {
  return JSON.parse(readText(filePath))
}

function listFiles(root) {
  const files = []
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const absolute = path.join(root, entry.name)
    if (entry.isDirectory()) files.push(...listFiles(absolute))
    else if (entry.isFile()) files.push(absolute)
  }
  return files
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function assert(errors, condition, message) {
  if (!condition) errors.push(message)
}

function sorted(values) {
  return [...values].sort()
}

function sameStringList(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function sameStringSet(actual, expected) {
  return sameStringList(sorted(actual ?? []), sorted(expected ?? []))
}

function sameJson(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function everyReadinessCheckPassed(checks) {
  const values = Object.values(checks ?? {})
  return values.length > 0 && values.every((value) => value === true)
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

function stableRehearsalGeneratorReport(value) {
  if (Array.isArray(value)) return value.map(stableRehearsalGeneratorReport)
  if (!value || typeof value !== 'object') return value
  const stable = {}
  for (const [key, entry] of Object.entries(value)) {
    if ([
      'generatedAt',
      'dryRun',
      'startedAt',
      'finishedAt',
      'durationMs',
      'rehearsalRoot',
      'savedArtifactRoot',
      'cachePath',
      'corruptedArtifactPath',
    ].includes(key)) continue
    stable[key] = stableRehearsalGeneratorReport(entry)
  }
  return stable
}

function stableBlockedGeneratorReport(value) {
  if (Array.isArray(value)) return value.map(stableBlockedGeneratorReport)
  if (!value || typeof value !== 'object') return value
  const stable = {}
  for (const [key, entry] of Object.entries(value)) {
    if (['generatedAt', 'startedAt', 'finishedAt'].includes(key)) continue
    stable[key] = stableBlockedGeneratorReport(entry)
  }
  return stable
}

function sameResolvedPath(actual, expected) {
  return typeof actual === 'string' && path.resolve(actual) === path.resolve(expected)
}

function normalizeId(value) {
  if (typeof value !== 'string') return value
  return value.includes(':') ? value.split(':').pop() : value
}

function byId(values) {
  return new Map((values ?? []).map((entry) => [entry.id, entry]))
}

function byNormalizedField(values, key = 'id') {
  return new Map((values ?? []).map((entry) => [normalizeId(entry[key]), entry]))
}

function idList(records) {
  return (records ?? []).map((record) => normalizeId(record.id))
}

function groupRangeMax(value) {
  if (typeof value === 'number') return value
  if (typeof value !== 'string') return 0
  if (value.includes('-')) return Number.parseInt(value.split('-').pop(), 10)
  return Number.parseInt(value, 10)
}

function minimumSpawnDistance(spawnRules) {
  return spawnRules?.minimumDistanceFromWorldSpawn ?? spawnRules?.minimumSpawnDistanceFromWorldSpawn ?? 0
}

function countBy(records, keyFn) {
  const counts = {}
  for (const record of records ?? []) {
    const key = keyFn(record)
    counts[key] = (counts[key] ?? 0) + 1
  }
  return counts
}

function idSet(records) {
  return new Set((records ?? []).map((record) => normalizeId(record.id)))
}

function inputContexts(recipe) {
  return (recipe?.inputs ?? []).filter((input) => input.context).map((input) => input.context)
}

function itemInputCount(recipe, itemId) {
  return (recipe?.inputs ?? [])
    .filter((input) => normalizeId(input.item) === itemId)
    .reduce((total, input) => total + (input.count ?? 0), 0)
}

function outputsItem(recipe, itemId) {
  return (recipe?.outputs ?? []).some((output) => normalizeId(output.item) === itemId)
}

function normalizeRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function standardRuleAvoidsUpkeepDeath(rule) {
  const text = rule ?? ''
  if (/no .*death|do not die|does not die|animals do not die|no upkeep death/i.test(text)) return true
  return !/die|death|dead/i.test(text)
}

function entryRefs(entries) {
  const refs = []
  for (const entry of entries ?? []) {
    if (entry.item) refs.push(normalizeId(entry.item))
    if (entry.block) refs.push(normalizeId(entry.block))
  }
  return refs
}

function collectPaletteRefs(value, refs = []) {
  if (typeof value === 'string') refs.push(value)
  else if (Array.isArray(value)) value.forEach((entry) => collectPaletteRefs(entry, refs))
  else if (value && typeof value === 'object') Object.values(value).forEach((entry) => collectPaletteRefs(entry, refs))
  return refs
}

function soundKey(value) {
  if (typeof value !== 'string') return value
  return value.replace(/^openlands:/, 'openlands.')
}

function landmarkIdFromFrequency(value) {
  return value === 'broken_waystone' ? 'broken_waystone_site' : value
}

function scenarioAssertionCount(scenario) {
  return (scenario.runtimeActions ?? [])
    .reduce((total, action) => total + (Array.isArray(action.assertions) ? action.assertions.length : 0), 0)
}

function expectedFirstHourScenarioSummary(scenario) {
  return {
    id: scenario.id,
    routeStep: scenario.routeStep,
    targetTimeMinutes: scenario.targetTimeMinutes,
    runtimeActions: (scenario.runtimeActions ?? []).length,
    assertions: scenarioAssertionCount(scenario),
    successEvidence: scenario.successEvidence ?? [],
  }
}

function expectedSaveLoadCheckpointSummary(checkpoint) {
  return {
    id: checkpoint.id,
    afterScenario: checkpoint.afterScenario,
    persistedFields: (checkpoint.mustPersist ?? []).length,
    requiredAssertions: (checkpoint.requiredAssertions ?? []).length,
  }
}

function adapterResourcePathForId(resourceId) {
  if (resourceId === 'META-INF/echo.mod.json') return 'META-INF/echo.mod.json'
  if (resourceId === 'assets/sounds.json') return `assets/${MODULE_ID}/sounds.json`
  return `data/${MODULE_ID}/openlands/${resourceId}.json`
}

function expectedAdapterArtifactEntries(runtimePlan) {
  return sorted([
    'META-INF/echo.mod.json',
    RUNTIME_EVIDENCE_CONTRACT,
    'com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.class',
    ...(runtimePlan.loadSteps ?? []).flatMap((step) => step.resourceIds ?? []).map(adapterResourcePathForId),
  ].filter((entry, index, values) => values.indexOf(entry) === index))
}

function buildExpectedRegistryParity({ conformance, aliasBridge, crossPlatformParity, firstHourRoute, gameModes, blocks, items, recipes, biomes, structures, waystoneContract, edition }) {
  const blockIds = (blocks.blocks ?? []).map((block) => normalizeId(block.id))
  const itemIds = (items.items ?? []).map((item) => normalizeId(item.id))
  const recipeIds = (recipes.recipes ?? []).map((recipe) => normalizeId(recipe.id))
  const biomeIds = (biomes.biomes ?? []).map((biome) => normalizeId(biome.id))
  const blockIdSet = addFoundationKnownBlocks(new Set(blockIds), conformance, aliasBridge)
  const itemIdSet = addFoundationKnownItems(new Set(itemIds), conformance, aliasBridge)
  const recipeIdSet = addFoundationKnownRecipes(new Set(recipeIds), conformance, aliasBridge)
  const nonShippingRecipeRefs = []
  const nonRegistryBiomeResources = []

  for (const item of items.items ?? []) {
    for (const recipeRef of item.recipeRefs ?? []) {
      const normalized = normalizeId(recipeRef)
      if (!recipeIdSet.has(normalized)) {
        nonShippingRecipeRefs.push({
          item: normalizeId(item.id),
          recipeRef,
        })
      }
    }
  }

  for (const biome of biomes.biomes ?? []) {
    for (const resource of biome.resourceSet ?? []) {
      const normalized = normalizeId(resource)
      if (!itemIdSet.has(normalized) && !blockIdSet.has(normalized)) {
        nonRegistryBiomeResources.push({
          biome: normalizeId(biome.id),
          resource,
        })
      }
    }
  }

  const standardMode = (gameModes.modes ?? []).find((mode) => mode.id === 'openlands_standard')
  const parityTarget = (crossPlatformParity.runtimeTargets ?? []).find((target) => target.id === edition.runtimeTarget)

  return {
    runtimeEntriesChecked: [
      CONFORMANCE_REGISTRY,
      CROSS_PLATFORM_PARITY_CONTRACT,
      BLOCKS_CONTRACT,
      ITEMS_CONTRACT,
      RECIPES_CONTRACT,
      BIOMES_CONTRACT,
      STRUCTURES_CONTRACT,
      'data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json',
      WAYSTONE_CONTRACT,
    ],
    registryCounts: {
      blocks: blockIds.length,
      items: itemIds.length,
      recipes: recipeIds.length,
      biomes: biomeIds.length,
      structures: (structures.landmarks ?? []).length,
      creatures: (conformance.creatureRegistry ?? []).length,
      waystoneStates: (waystoneContract.stateMachine ?? []).length,
      systems: conformance.systemContracts?.length ?? 0,
    },
    nonShippingRecipeRefs,
    nonRegistryBiomeResources,
    paritySurfaces: (crossPlatformParity.paritySurfaces ?? []).map((surface) => ({
      id: surface.id,
      mustMatch: surface.mustMatch,
      allowedRuntimeDifference: surface.allowedRuntimeDifference,
    })),
    runtimeResponsibilities: parityTarget?.adapterResponsibilities ?? [],
    saveLoadFields: firstHourRoute.saveLoadAcceptance ?? [],
    standardModeRules: standardMode?.rules ?? {},
    blockIds,
    itemIds,
    recipeIds,
    biomeIds,
  }
}

function buildExpectedCraftingStation({ conformance, aliasBridge, runtimePlan, blocks, items, recipes }) {
  const blockIds = addFoundationKnownBlocks(new Set((blocks.blocks ?? []).map((block) => normalizeId(block.id))), conformance, aliasBridge)
  const itemIds = addFoundationKnownItems(new Set((items.items ?? []).map((item) => normalizeId(item.id))), conformance, aliasBridge)
  const recipeIds = (recipes.recipes ?? []).map((recipe) => normalizeId(recipe.id))
  const recipeIdSet = new Set(recipeIds)
  const stationIds = [
    ...(recipes.foundationStations ?? []),
    ...(recipes.stations ?? []).map((station) => station.id),
  ].map((station) => normalizeId(station))
  const stationCounts = {}
  const nonInventoryUnlockRefs = []

  for (const stationId of stationIds) stationCounts[stationId] = 0
  for (const recipe of recipes.recipes ?? []) {
    const stationId = normalizeId(recipe.station)
    stationCounts[stationId] = (stationCounts[stationId] ?? 0) + 1
    for (const unlock of recipe.unlockedBy ?? []) {
      const normalized = normalizeId(unlock)
      if (!recipeIdSet.has(normalized) && !itemIds.has(normalized) && !blockIds.has(normalized)) {
        nonInventoryUnlockRefs.push({
          recipe: normalizeId(recipe.id),
          unlock,
        })
      }
    }
  }

  const stationSurfaceStep = (runtimePlan.loadSteps ?? []).find((step) => step.id === 'register_recipes_and_station_surfaces')

  return {
    runtimeEntriesChecked: [
      RECIPES_CONTRACT,
      BLOCKS_CONTRACT,
      ITEMS_CONTRACT,
      TAGS_CONTRACT,
      CONFORMANCE_REGISTRY,
      RUNTIME_EVIDENCE_CONTRACT,
    ],
    recipeCount: recipeIds.length,
    stationSummaries: stationIds.map((stationId) => {
      const station = (recipes.stations ?? []).find((entry) => normalizeId(entry.id) === stationId)
      return {
        id: stationId,
        owner: station ? MODULE_ID : 'foundation',
        requiresBlock: station?.requiresBlock ?? null,
        recipeCount: stationCounts[stationId] ?? 0,
        process: station?.process ?? station?.grid ?? 'foundation_surface',
      }
    }),
    expectedStationCounts: stationCounts,
    processStations: Object.keys(stationCounts).filter((station) => stationCounts[station] > 0),
    requiredMapTableRecipes: (recipes.recipes ?? [])
      .filter((recipe) => normalizeId(recipe.station) === 'map_table')
      .map((recipe) => normalizeId(recipe.id)),
    deferredStationBlocks: ['loom', 'mason_table'].filter((block) => blockIds.has(block) && !stationIds.includes(block)),
    nonInventoryUnlockRefs,
    stationSurfaceLoadStep: {
      id: stationSurfaceStep?.id,
      successSignal: stationSurfaceStep?.successSignal,
      requiredEvidence: stationSurfaceStep?.requiredEvidence ?? [],
    },
    recipeIds,
    conformanceRecipeIds: conformance.recipeRegistry ?? [],
    blockIds,
    itemIds,
    stationIds,
  }
}

function buildExpectedWorldgenExploration({ runtimePlan, conformance, aliasBridge, blocks, items, biomes, structures, creatures, holomap, tags, loot, tutorials, sounds }) {
  const blockIds = addFoundationKnownBlocks(new Set((blocks.blocks ?? []).map((block) => normalizeId(block.id))), conformance, aliasBridge)
  const itemIds = addFoundationKnownItems(new Set((items.items ?? []).map((item) => normalizeId(item.id))), conformance, aliasBridge)
  const biomeIds = (biomes.biomes ?? []).map((biome) => normalizeId(biome.id))
  const biomeIdSet = new Set(biomeIds)
  const creatureIds = (creatures.creatures ?? []).map((creature) => normalizeId(creature.id))
  const creatureIdSet = new Set(creatureIds)
  const landmarkIds = (structures.landmarks ?? []).map((landmark) => normalizeId(landmark.id))
  const landmarkIdSet = new Set(landmarkIds)
  const blockTagIds = new Set(Object.keys(tags.blockTags ?? {}))
  const itemTagIds = new Set(Object.keys(tags.itemTags ?? {}))
  const lootTableIds = new Set((loot.chestTables ?? []).map((table) => normalizeId(table.id)))
  const tutorialPromptIds = new Set((tutorials.prompts ?? []).map((prompt) => normalizeId(prompt.id)))
  const soundKeys = new Set(Object.keys(sounds ?? {}))
  const worldgenResourceMarkers = []
  const biomeSummaries = []

  for (const biome of biomes.biomes ?? []) {
    const biomeId = normalizeId(biome.id)
    for (const ref of collectPaletteRefs(biome.blockPalette)) {
      const normalized = normalizeId(ref)
      if (!blockIds.has(normalized)) {
        worldgenResourceMarkers.push({ source: `biome:${biomeId}:palette`, resource: ref })
      }
    }
    for (const resource of biome.resourceSet ?? []) {
      const normalized = normalizeId(resource)
      if (!blockIds.has(normalized) && !itemIds.has(normalized)) {
        worldgenResourceMarkers.push({ source: `biome:${biomeId}:resourceSet`, resource })
      }
    }
    biomeSummaries.push({
      id: biomeId,
      resourceCount: (biome.resourceSet ?? []).length,
      spawnCount: (biome.spawnTable ?? []).length,
      landmarkCount: Object.keys(biome.landmarkFrequency ?? {}).length,
      ambienceKeys: Object.keys(biome.ambience ?? {}),
    })
  }

  const landmarkHoloMapHintMarkers = []
  const nonPromptTutorialHooks = []
  for (const landmark of structures.landmarks ?? []) {
    const landmarkId = normalizeId(landmark.id)
    if (landmark.holoMapHint) {
      landmarkHoloMapHintMarkers.push({ landmark: landmarkId, hint: landmark.holoMapHint })
    }
    if (landmark.tutorialHook && !tutorialPromptIds.has(normalizeId(landmark.tutorialHook))) {
      nonPromptTutorialHooks.push({ landmark: landmarkId, tutorialHook: landmark.tutorialHook })
    }
  }

  const creatureSoundEvents = []
  for (const creature of creatures.creatures ?? []) {
    for (const soundRef of Object.values(creature.sounds ?? {})) {
      creatureSoundEvents.push(soundKey(soundRef))
    }
  }

  const worldgenStep = (runtimePlan.loadSteps ?? []).find((step) => step.id === 'bind_biomes_structures_creatures_and_spawn')

  return {
    runtimeEntriesChecked: [
      BIOMES_CONTRACT,
      STRUCTURES_CONTRACT,
      CREATURES_CONTRACT,
      HOLOMAP_CONTRACT,
      LOOT_CONTRACT,
      TUTORIALS_CONTRACT,
      RUNTIME_EVIDENCE_CONTRACT,
      SOUNDS_ASSET_CONTRACT,
    ],
    counts: {
      biomes: biomeIds.length,
      landmarks: landmarkIds.length,
      creatures: creatureIds.length,
      holomapLayers: (holomap.layers ?? []).length,
      holomapHintTypes: (holomap.hintTypes ?? []).length,
      creatureSoundEvents: new Set(creatureSoundEvents).size,
    },
    spawnSafety: biomes.spawnSafetyContract,
    biomeSummaries,
    landmarkIds,
    creatureIds,
    holomapLayers: (holomap.layers ?? []).map((layer) => layer.id),
    holomapHintTypes: (holomap.hintTypes ?? []).map((hint) => hint.id),
    worldgenResourceMarkers,
    landmarkHoloMapHintMarkers,
    nonPromptTutorialHooks,
    worldgenLoadStep: {
      id: worldgenStep?.id,
      successSignal: worldgenStep?.successSignal,
      requiredEvidence: worldgenStep?.requiredEvidence ?? [],
    },
    blockIds,
    itemIds,
    biomeIds,
    biomeIdSet,
    creatureIdSet,
    landmarkIdSet,
    blockTagIds,
    itemTagIds,
    lootTableIds,
    soundKeys,
    worldgenStep,
  }
}

function buildExpectedCreatureRoster({ runtimePlan, conformance, aliasBridge, creatures, loot, biomes, items, tags, playtestFixture, sounds }) {
  const creatureIds = idList(creatures.creatures)
  const creatureIdSet = new Set(creatureIds)
  const biomeIds = new Set(idList(biomes.biomes))
  const itemIds = addFoundationKnownItems(new Set(idList(items.items)), conformance, aliasBridge)
  const blockTagIds = new Set(Object.keys(tags.blockTags ?? {}))
  const soundKeys = new Set(Object.keys(sounds ?? {}))
  const dropTables = byNormalizedField(loot.creatureDrops, 'creature')
  const biomeSpawnMap = new Map()
  for (const biome of biomes.biomes ?? []) {
    for (const spawn of biome.spawnTable ?? []) {
      const creatureId = normalizeId(spawn.creature)
      const list = biomeSpawnMap.get(creatureId) ?? []
      list.push({ biome: normalizeId(biome.id), weight: spawn.weight })
      biomeSpawnMap.set(creatureId, list)
    }
  }

  const safeSpawnScenario = (playtestFixture.acceptanceScenarios ?? []).find((scenario) => scenario.id === 'safe_spawn')
  const starterFriendlyCreatures = new Set(safeSpawnScenario?.requires?.creaturesAllowed ?? [])
  const soundEvents = []
  const creatureSummaries = []
  for (const creature of creatures.creatures ?? []) {
    const creatureId = normalizeId(creature.id)
    const dropTable = dropTables.get(creatureId)
    for (const soundRef of Object.values(creature.sounds ?? {})) {
      soundEvents.push(soundKey(soundRef))
    }
    const categoryId = creature.legacyCategory ?? normalizeId(creature.category)
    creatureSummaries.push({
      id: creatureId,
      category: categoryId,
      biomes: creature.biomes,
      spawnTime: creature.spawnRules?.time,
      group: creature.spawnRules?.group,
      maxGroupSize: groupRangeMax(creature.spawnRules?.group),
      minimumDistanceFromWorldSpawn: minimumSpawnDistance(creature.spawnRules),
      aiCount: (creature.ai ?? []).length,
      soundCount: Object.keys(creature.sounds ?? {}).length,
      dropCount: (dropTable?.drops ?? []).length,
      biomeSpawnEntries: biomeSpawnMap.get(creatureId),
      starterFriendly: starterFriendlyCreatures.has(creatureId),
    })
  }
  const worldgenStep = (runtimePlan.loadSteps ?? []).find((step) => step.id === 'bind_biomes_structures_creatures_and_spawn')

  return {
    runtimeEntriesChecked: [
      CREATURES_CONTRACT,
      LOOT_CONTRACT,
      BIOMES_CONTRACT,
      ITEMS_CONTRACT,
      TAGS_CONTRACT,
      CONFORMANCE_REGISTRY,
      RUNTIME_EVIDENCE_CONTRACT,
      PLAYTEST_FIXTURE,
      SOUNDS_ASSET_CONTRACT,
    ],
    globalRules: creatures.globalRules,
    counts: {
      creatures: creatureIds.length,
      categories: new Set((creatures.creatures ?? []).map((creature) => creature.legacyCategory ?? normalizeId(creature.category))).size,
      creatureDropTables: (loot.creatureDrops ?? []).length,
      creatureSoundEvents: new Set(soundEvents).size,
      passiveOrNeutralCreatures: (creatures.creatures ?? []).filter((creature) => !(creature.legacyCategory ?? normalizeId(creature.category))?.startsWith('hostile')).length,
      hostileCreatures: (creatures.creatures ?? []).filter((creature) => (creature.legacyCategory ?? normalizeId(creature.category))?.startsWith('hostile')).length,
    },
    creatureIds,
    categoryCounts: countBy(creatures.creatures, (creature) => creature.legacyCategory ?? normalizeId(creature.category)),
    creatureSummaries,
    starterSafety: {
      safeSpawnAllowedCreatures: [...starterFriendlyCreatures],
      hostilesMinimumDistanceBlocks: Math.min(...creatureSummaries
        .filter((creature) => creature.category?.startsWith('hostile'))
        .map((creature) => creature.minimumDistanceFromWorldSpawn)),
      boarMinimumDistanceBlocks: creatureSummaries.find((creature) => creature.id === 'boar')?.minimumDistanceFromWorldSpawn,
      avoidHardcorePressure: creatures.globalRules?.avoidHardcorePressure,
    },
    worldgenLoadStep: {
      id: worldgenStep?.id,
      successSignal: worldgenStep?.successSignal,
      requiredEvidence: worldgenStep?.requiredEvidence ?? [],
    },
    biomeIds,
    itemIds,
    blockTagIds,
    soundKeys,
    dropTables,
    biomeSpawnMap,
    creatureIdSet,
    conformanceCreatureIds: conformance.creatureRegistry ?? [],
    worldgenStep,
  }
}

function buildExpectedOldRoadNetwork({ runtimePlan, blocks, items, recipes, structures, waystoneContract, holomapContract, playtestFixture }) {
  const blockMap = byNormalizedField(blocks.blocks)
  const itemMap = byNormalizedField(items.items)
  const recipeMap = byNormalizedField(recipes.recipes)
  const landmarkMap = byNormalizedField(structures.landmarks)
  const oldRoadBlockIds = (waystoneContract.blocks ?? []).map((block) => normalizeId(block))
  const oldRoadBlockIdSet = new Set(oldRoadBlockIds)
  const publicAlphaRouteItems = new Set((playtestFixture.waystonePublicAlphaScenario?.requiresItems ?? []).map((item) => normalizeId(item)))
  const routeRecipeIds = (recipes.recipes ?? [])
    .filter((recipe) => normalizeId(recipe.station) === 'map_table')
    .filter((recipe) => (recipe.outputs ?? []).some((output) => output.item && publicAlphaRouteItems.has(normalizeId(output.item))))
    .map((recipe) => normalizeId(recipe.id))
  const routeItemIds = routeRecipeIds
    .flatMap((recipeId) => recipeMap.get(recipeId)?.outputs ?? [])
    .filter((output) => output.item)
    .map((output) => normalizeId(output.item))
  const routeItemIdSet = new Set(routeItemIds)
  const routeRecipeIdSet = new Set(routeRecipeIds)
  const roadLandmarks = (structures.landmarks ?? []).filter((landmark) => {
    const landmarkBlocks = new Set((landmark.blocks ?? []).map((block) => normalizeId(block)))
    return oldRoadBlockIds.some((block) => landmarkBlocks.has(block))
  })
  const oldRoadLayer = (holomapContract.layers ?? []).find((layer) => layer.id === 'old_roads')
  const waystoneLayer = (holomapContract.layers ?? []).find((layer) => layer.id === 'waystones')
  const roadSegmentHint = (holomapContract.hintTypes ?? []).find((hint) => hint.id === 'road_segment')
  const boundState = (waystoneContract.stateMachine ?? []).find((state) => state.state === 'bound')
  const activeState = (waystoneContract.stateMachine ?? []).find((state) => state.state === 'active')
  const explorationScenario = (playtestFixture.acceptanceScenarios ?? []).find((scenario) => scenario.id === 'first_exploration_hook')
  const firstWaystoneScenario = (playtestFixture.acceptanceScenarios ?? []).find((scenario) => scenario.id === 'first_waystone')
  const firstWaystoneCheckpoint = (playtestFixture.saveLoadCheckpoints ?? []).find((checkpoint) => checkpoint.id === 'after_first_waystone_repair')
  const loadStep = (runtimePlan.loadSteps ?? []).find((step) => step.id === 'bind_waystones_holomap_and_multiplayer_state')

  return {
    runtimeEntriesChecked: [
      BLOCKS_CONTRACT,
      ITEMS_CONTRACT,
      RECIPES_CONTRACT,
      STRUCTURES_CONTRACT,
      WAYSTONE_CONTRACT,
      HOLOMAP_CONTRACT,
      PLAYTEST_FIXTURE,
      RUNTIME_EVIDENCE_CONTRACT,
    ],
    counts: {
      oldRoadBlocks: oldRoadBlockIds.length,
      routeItems: routeItemIds.length,
      routeRecipes: routeRecipeIds.length,
      roadLandmarks: roadLandmarks.length,
      holomapOldRoadLayers: [oldRoadLayer, waystoneLayer].filter(Boolean).length,
    },
    oldRoadBlockIds,
    routeItemIds,
    routeRecipeIds,
    roadLandmarkIds: roadLandmarks.map((landmark) => normalizeId(landmark.id)),
    routeRecipeSummaries: routeRecipeIds.map((id) => {
      const recipe = recipeMap.get(id)
      return {
        id,
        station: recipe?.station,
        contextInputs: inputContexts(recipe),
        itemInputs: (recipe?.inputs ?? [])
          .filter((input) => input.item)
          .map((input) => ({ item: input.item, count: input.count })),
        outputs: recipe?.outputs ?? [],
        unlockedBy: recipe?.unlockedBy ?? [],
      }
    }),
    holomapOldRoadContract: {
      storedField: 'oldRoadSegments',
      oldRoadLayer: oldRoadLayer?.id,
      oldRoadLayerSource: oldRoadLayer?.source,
      roadSegmentHint: roadSegmentHint?.id,
      roadSegmentRevealSources: roadSegmentHint?.revealSources ?? [],
    },
    waystoneRouteContract: {
      boundStateConsumes: 'route_binding',
      boundStateOutputs: boundState?.outputs ?? [],
      activeStateOutputs: activeState?.outputs ?? [],
      fastTravelRequiresActiveStones: waystoneContract.effects?.fastTravel?.requiresActiveStones,
      travelPermissionDefault: waystoneContract.multiplayerState?.defaultPermissions?.travel,
      multiplayerStoredFields: waystoneContract.multiplayerState?.storedFields ?? [],
    },
    playtestCoverage: {
      explorationScenario: explorationScenario?.id,
      explorationAssertions: (explorationScenario?.runtimeActions ?? []).flatMap((action) => action.assertions ?? []),
      firstWaystoneScenario: firstWaystoneScenario?.id,
      firstWaystoneCheckpoint: firstWaystoneCheckpoint?.id,
      publicAlphaScenario: playtestFixture.waystonePublicAlphaScenario?.id,
    },
    oldRoadNetworkLoadStep: {
      id: loadStep?.id,
      successSignal: loadStep?.successSignal,
      requiredEvidence: loadStep?.requiredEvidence ?? [],
    },
    blockMap,
    itemMap,
    recipeMap,
    landmarkMap,
    blockIds: idSet(blocks.blocks),
    itemIds: idSet(items.items),
    recipeIds: idSet(recipes.recipes),
    landmarkIds: idSet(structures.landmarks),
    oldRoadBlockIdSet,
    routeItemIdSet,
    routeRecipeIdSet,
    publicAlphaRouteItems,
    oldRoadLayer,
    waystoneLayer,
    roadSegmentHint,
    boundState,
    activeState,
    explorationScenario,
    firstWaystoneScenario,
    firstWaystoneCheckpoint,
    loadStep,
  }
}

function buildExpectedAlphaSystems({ homestead, builderUx, coopSmp, distribution, conformance, aliasBridge, blocks, items, tags, creatures, biomes }) {
  const blockIds = addFoundationKnownBlocks(idSet(blocks.blocks), conformance, aliasBridge)
  const itemIds = addFoundationKnownItems(idSet(items.items), conformance, aliasBridge)
  const creatureIds = idSet(creatures.creatures)
  const itemTagIds = new Set(Object.keys(tags.itemTags ?? {}))
  const blockMap = byNormalizedField(blocks.blocks)
  const itemMap = byNormalizedField(items.items)
  const creatureIdSet = creatureIds
  const homesteadUseMarkers = []
  const traderRewardMarkers = []

  for (const crop of homestead.crops ?? []) {
    for (const use of crop.uses ?? []) {
      const normalized = normalizeId(use)
      if (!itemIds.has(normalized)) homesteadUseMarkers.push({ crop: crop.id, use })
    }
  }
  for (const pool of homestead.traderSurplus?.demandPools ?? []) {
    for (const reward of pool.rewardTypes ?? []) {
      const normalized = normalizeId(reward)
      if (!itemIds.has(normalized)) traderRewardMarkers.push({ pool: pool.id, reward })
    }
  }

  const scaffold = (builderUx.temporaryBlocks ?? []).find((block) => block.id === 'scaffold_bundle')

  return {
    runtimeEntriesChecked: [
      HOMESTEAD_ALPHA_CONTRACT,
      BUILDER_UX_ALPHA_CONTRACT,
      COOP_SMP_CONTRACT,
      DISTRIBUTION_ALPHA_GATES_CONTRACT,
      TAGS_CONTRACT,
      ITEMS_CONTRACT,
      BLOCKS_CONTRACT,
      CREATURES_CONTRACT,
    ],
    relaxedStandardGuarantees: {
      cropsDoNotDieInStandard: true,
      wateringOptionalInStandard: homestead.soilCare?.watering?.standardRequired === false,
      compostOptionalInStandard: homestead.soilCare?.compost?.standardRequired === false,
      cookpotSpoilageDisabled: (homestead.cookpotMeals ?? []).every((meal) => meal.standardResult?.spoilage === false),
      scaffoldNoFallingPhysics: scaffold?.fallRule === 'no_falling_physics_in_standard',
    },
    homesteadSummary: {
      crops: (homestead.crops ?? []).map((crop) => ({
        id: crop.id,
        growthStages: crop.growthStages,
        baseGrowthMinutes: crop.baseGrowthMinutes,
        standardFailure: crop.standardFailure,
      })),
      cookpotMeals: (homestead.cookpotMeals ?? []).map((meal) => meal.id),
      animalPens: (homestead.animalPens ?? []).map((pen) => pen.id),
      traderDemandPools: (homestead.traderSurplus?.demandPools ?? []).map((pool) => pool.id),
      traderStoredFields: homestead.traderSurplus?.storedFields ?? [],
      nonRegistryUseMarkers: homesteadUseMarkers,
      nonRegistryTraderRewards: traderRewardMarkers,
    },
    builderUxSummary: {
      tools: (builderUx.tools ?? []).map((tool) => tool.id),
      temporaryBlocks: (builderUx.temporaryBlocks ?? []).map((block) => block.id),
      inventoryCommands: (builderUx.inventoryCommands ?? []).map((command) => command.id),
      acceptanceChecks: builderUx.acceptance ?? [],
    },
    coopSummary: {
      targetPlayers: coopSmp.targetPlayers,
      sharedStates: (coopSmp.sharedState ?? []).map((state) => ({
        id: state.id,
        storedFields: state.storedFields,
        conflictResolution: state.conflictResolution,
      })),
      permissionDefaults: coopSmp.permissions?.defaults,
      networkEvents: (coopSmp.networkEvents ?? []).map((event) => event.id),
    },
    publicAlphaMinimum: distribution.publicAlphaMinimum ?? {},
    blockIds,
    itemIds,
    creatureIds: creatureIdSet,
    biomeCount: (biomes.biomes ?? []).length,
    itemTagIds,
    blockMap,
    itemMap,
    scaffold,
  }
}

function buildExpectedDistributionRoadmap({ distribution, distributionApproval, launchRoadmap, launcherFlow, crossPlatformParity, conformance, releaseManifest, releaseIndex, releaseRoot, edition, publicAlphaEvidence }) {
  const expectedArtifactFile = edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION)
  const artifactTargets = byNormalizedField(distribution.artifactTargets)
  const launcherMatrix = byNormalizedField(launcherFlow.editionMatrix)
  const launcherEntry = launcherMatrix.get(edition.key)
  const parityTargets = byNormalizedField(crossPlatformParity.runtimeTargets)
  const parityTarget = parityTargets.get(edition.runtimeTarget)
  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const artifactSummaries = []
  for (const target of distribution.artifactTargets ?? []) {
    const artifact = artifactByFile(releaseModule, target.file)
    const artifactPath = path.join(releaseRoot, MODULE_ID, target.file)
    artifactSummaries.push({
      id: target.id,
      file: target.file,
      kind: artifact?.kind,
      size: fileExists(artifactPath) ? fs.statSync(artifactPath).size : artifact?.size,
      sha256: fileExists(artifactPath) ? sha256File(artifactPath) : artifact?.sha256,
      buildMode: artifact?.buildMode ?? null,
      downloadUrlPresent: typeof artifact?.downloadUrl === 'string' && artifact.downloadUrl.length > 0,
      requiredForPublicAlpha: target.requiredForPublicAlpha,
    })
  }
  const roadmapPhases = byNormalizedField(launchRoadmap.phases)
  const publicAlphaConformanceCounts = {
    biomes: (conformance.biomeRegistry ?? []).length,
    blocks: (conformance.blockRegistry ?? []).length + (conformance.foundationRegistries?.blocksMovedToFoundation?.length ?? 0),
    items: (conformance.itemRegistry ?? []).length + (conformance.foundationRegistries?.itemsMovedToFoundation?.length ?? 0),
    creatures: (conformance.creatureRegistry ?? []).length,
  }
  const mvpMinimumsMet = {
    biomes: publicAlphaConformanceCounts.biomes >= distribution.publicAlphaMinimum?.biomes,
    blocks: publicAlphaConformanceCounts.blocks >= distribution.publicAlphaMinimum?.blocks?.min,
    items: publicAlphaConformanceCounts.items >= distribution.publicAlphaMinimum?.items?.min,
    creatures: publicAlphaConformanceCounts.creatures >= distribution.publicAlphaMinimum?.creatures,
  }

  return {
    expectedArtifactFile,
    contracts: {
      distribution: DISTRIBUTION_ALPHA_GATES_CONTRACT,
      distributionApproval: DISTRIBUTION_APPROVAL_ACCEPTANCE,
      launchRoadmap: LAUNCH_ROADMAP_CONTRACT,
      launcherFlow: LAUNCHER_FLOW_CONTRACT,
      crossPlatformParity: CROSS_PLATFORM_PARITY_CONTRACT,
      conformance: CONFORMANCE_REGISTRY,
    },
    releaseManifest: {
      packId: releaseManifest.packId,
      runtimeTarget: releaseManifest.runtimeTarget,
      loader: releaseManifest.loader,
      moduleArtifactFamily: releaseManifest.moduleArtifactFamily,
      moduleArtifactPattern: releaseManifest.moduleArtifactPattern,
      requiredPublicAlphaEvidence: releaseManifest.requiredPublicAlphaEvidence,
    },
    releaseIndex: {
      releaseId: releaseIndex.releaseId,
      currentAllowedState: distribution.releaseIndexStates?.currentAllowedState,
      launcherCurrentIndexStateAllowed: launcherFlow.artifactVerification?.currentIndexStateAllowed,
      approvedRequires: distribution.releaseIndexStates?.approvedRequires ?? [],
      artifactSummaries,
      uploadedArtifactUrlsPresent: artifactSummaries.every((artifact) => artifact.downloadUrlPresent),
    },
    editionMatrix: {
      id: edition.key,
      launcherEntry,
      parityTarget,
      artifactTarget: artifactTargets.get(edition.key),
    },
    publicAlphaMinimum: {
      ...distribution.publicAlphaMinimum,
      conformanceCounts: publicAlphaConformanceCounts,
      mvpMinimumsMet,
    },
    roadmap: {
      defaultRule: launchRoadmap.defaultRule,
      phaseIds: (launchRoadmap.phases ?? []).map((phase) => phase.id),
      mvpScope: roadmapPhases.get('mvp')?.scope,
      publicAlphaScope: roadmapPhases.get('public_alpha')?.scope,
      oneDotZeroLoops: roadmapPhases.get('one_dot_zero')?.scope?.coreLoops,
      nonNegotiableInvariants: launchRoadmap.nonNegotiableInvariants,
    },
    launcherFlows: (launcherFlow.requiredLauncherFlows ?? []).map((flow) => ({
      id: flow.id,
      appliesTo: flow.appliesTo,
      mustVerify: flow.mustVerify,
      evidenceAttachment: flow.evidenceAttachment,
    })),
    artifactTargets,
    releaseModule,
    launcherEntry,
    parityTarget,
    publicAlphaEvidence,
    distributionApproval,
    launchRoadmap,
    launcherFlow,
    crossPlatformParity,
    conformance,
  }
}

function buildExpectedLauncherFlow({ launcherFlow, distribution, runtimePlan, releaseIndex, releaseRoot, editionManifest, evidenceTemplate, edition }) {
  const expectedArtifactFile = edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION)
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const matrix = (launcherFlow.editionMatrix ?? []).find((entry) => entry.id === edition.key)
  const artifactVerification = (launcherFlow.artifactVerification?.requiredBeforePublicAlpha ?? [])
    .find((entry) => entry.file === expectedArtifactFile)
  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const releaseArtifact = artifactByFile(releaseModule, expectedArtifactFile)
  const artifactPath = path.join(releaseRoot, MODULE_ID, expectedArtifactFile)
  const artifactExists = fileExists(artifactPath)
  const publicAlphaGate = (runtimePlan.acceptanceGates ?? []).find((gate) => gate.id === 'public_alpha')
  const distributionGateMap = byNormalizedField(distribution.launcherGates)
  const flowResults = (launcherFlow.requiredLauncherFlows ?? [])
    .filter((flow) => (flow.appliesTo ?? []).includes(edition.runtimeTarget))
    .map((flow) => ({
      id: flow.id,
      displayName: flow.displayName,
      status: 'preflight_mapped',
      preconditions: flow.preconditions,
      mustVerify: flow.mustVerify,
      additionalAssertions: flow.additionalAssertions,
      worldStatePolicy: flow.worldStatePolicy,
      evidenceAttachment: flow.evidenceAttachment,
      realLauncherExecutionRequiredBeforePublicAlpha: true,
    }))

  return {
    expectedArtifactFile,
    releaseIndexPath,
    matrix,
    artifactVerification,
    releaseModule,
    releaseArtifact,
    artifactPath,
    artifactExists,
    artifact: {
      file: expectedArtifactFile,
      kind: edition.moduleArtifactFamily,
      path: artifactPath,
      size: artifactExists ? fs.statSync(artifactPath).size : releaseArtifact?.size,
      sha256: artifactExists ? sha256File(artifactPath) : releaseArtifact?.sha256,
      buildMode: releaseArtifact?.buildMode,
      downloadUrlPresent: Boolean(releaseArtifact?.downloadUrl),
    },
    publicAlphaGate,
    requiredPublicAlphaEvidence: editionManifest?.requiredPublicAlphaEvidence ?? [],
    releaseIndexStateAllowed: launcherFlow.artifactVerification?.currentIndexStateAllowed ?? distribution.releaseIndexStates?.currentAllowedState,
    flowResults,
    statePreservation: launcherFlow.statePreservation,
    distributionGateMap,
    launcherFlow,
    distribution,
    runtimePlan,
    releaseIndex,
    editionManifest,
    evidenceTemplate,
  }
}

function buildExpectedLegalAudit({ legalAudit, contentPolicy, assetManifest, blocks, items, recipes, moduleRoot, edition }) {
  const assetsRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'assets', MODULE_ID)
  return {
    artifactFile: edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION),
    contractPaths: {
      legalAuditContract: LEGAL_AUDIT_CONTRACT,
      contentPolicy: CONTENT_POLICY_CONTRACT,
      assetManifest: ASSET_MANIFEST_CONTRACT,
    },
    scanSummary: {
      assetPaths: listFiles(assetsRoot).length,
      forbiddenPublicTerms: (legalAudit.forbiddenPublicTerms ?? []).length,
      blockAssetsChecked: (blocks.blocks ?? []).length,
      itemAssetsChecked: (items.items ?? []).length,
      recipesChecked: (recipes.recipes ?? []).length,
      descriptorPublicFieldsChecked: edition.key === 'neoforge' ? 4 : 0,
    },
    policyResults: {
      noForbiddenPublicTerms: true,
      canonicalEchoIdsRetained: true,
      borrowedAssetPathsDetected: false,
      placeholderCoverageComplete: true,
      publicReleaseAllowedWithPlaceholders: false,
      requiresHumanArtLegalReview: true,
    },
    contentPolicy,
    legalAudit,
    assetManifest,
  }
}

function buildExpectedLocalLauncherRehearsal({ launcherFlow, launcherExecution, releaseIndex, releaseRoot, edition }) {
  const artifactFile = edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION)
  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const releaseArtifact = artifactByFile(releaseModule, artifactFile)
  const artifactPath = path.join(releaseRoot, MODULE_ID, artifactFile)
  return {
    artifactFile,
    releaseModule,
    releaseArtifact,
    artifactPath,
    releaseIndexPath: path.join(releaseRoot, 'echo-release.json'),
    launcherFlowIds: (launcherFlow.requiredLauncherFlows ?? []).map((flow) => flow.id),
    launcherExecutionFlowIds: (launcherExecution.executionFlows ?? []).map((flow) => flow.id),
  }
}

function stableLocalLauncherCaptures(flowId, captures = {}) {
  const copy = { ...captures }
  delete copy.cachePath
  delete copy.corruptedArtifactPath
  return copy
}

function stableLocalLauncherFlow(flow) {
  return {
    id: flow.id,
    status: flow.status,
    assertions: (flow.assertions ?? []).map((assertion) => ({ id: assertion.id, status: assertion.status })),
    captures: stableLocalLauncherCaptures(flow.id, flow.captures),
    savedArtifacts: flow.savedArtifacts ?? [],
  }
}

function stableLocalLauncherRehearsal(report) {
  return {
    releaseId: report.releaseId,
    releaseIndexPath: report.releaseIndexPath,
    moduleArtifact: report.moduleArtifact,
    moduleArtifactSha256: report.moduleArtifactSha256,
    moduleArtifactSize: report.moduleArtifactSize,
    flowResults: (report.flowResults ?? []).map(stableLocalLauncherFlow),
    preservedState: report.preservedState,
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableLauncherFlowReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    publicAlphaReady: report.publicAlphaReady,
    packId: report.packId,
    displayName: report.displayName,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    releaseManifest: report.releaseManifest,
    releaseId: report.releaseId,
    artifact: report.artifact,
    launcherFlowContract: report.launcherFlowContract,
    distributionContract: report.distributionContract,
    releaseIndexStateAllowed: report.releaseIndexStateAllowed,
    requiredPublicAlphaEvidence: report.requiredPublicAlphaEvidence ?? [],
    flowResults: report.flowResults ?? [],
    statePreservation: report.statePreservation,
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableDistributionRoadmapReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    publicAlphaReady: report.publicAlphaReady,
    realDistributionExecutionRequiredBeforePublicAlpha: report.realDistributionExecutionRequiredBeforePublicAlpha,
    packId: report.packId,
    displayName: report.displayName,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    contracts: report.contracts,
    releaseManifest: report.releaseManifest,
    releaseIndex: report.releaseIndex,
    editionMatrix: report.editionMatrix,
    publicAlphaMinimum: report.publicAlphaMinimum,
    roadmap: report.roadmap,
    launcherFlows: report.launcherFlows ?? [],
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableAdapterBootReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    realAdapterBootRequiredBeforePublicAlpha: report.realAdapterBootRequiredBeforePublicAlpha,
    packId: report.packId,
    displayName: report.displayName,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    runtimeEvidenceContract: report.runtimeEvidenceContract,
    artifact: report.artifact,
    descriptor: report.descriptor,
    phases: report.phases ?? [],
    loadStepSummaries: report.loadStepSummaries ?? [],
    runtimeEvidenceIds: report.runtimeEvidenceIds ?? [],
    requiredPublicAlphaEvidence: report.requiredPublicAlphaEvidence ?? [],
    adapterReadySignal: report.adapterReadySignal,
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableRegistryParityReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    realRegistryParityExecutionRequiredBeforePublicAlpha: report.realRegistryParityExecutionRequiredBeforePublicAlpha,
    packId: report.packId,
    displayName: report.displayName,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    conformanceFixture: report.conformanceFixture,
    parityContract: report.parityContract,
    artifact: report.artifact,
    registryCounts: report.registryCounts,
    nonShippingRecipeRefs: report.nonShippingRecipeRefs ?? [],
    nonRegistryBiomeResources: report.nonRegistryBiomeResources ?? [],
    paritySurfaces: report.paritySurfaces ?? [],
    runtimeResponsibilities: report.runtimeResponsibilities ?? [],
    saveLoadFields: report.saveLoadFields ?? [],
    standardModeRules: report.standardModeRules,
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableCraftingStationReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    realRuntimeStationExecutionRequiredBeforePublicAlpha: report.realRuntimeStationExecutionRequiredBeforePublicAlpha,
    packId: report.packId,
    displayName: report.displayName,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    recipeContract: report.recipeContract,
    artifact: report.artifact,
    recipeCount: report.recipeCount,
    stationSummaries: report.stationSummaries ?? [],
    expectedStationCounts: report.expectedStationCounts,
    processStations: report.processStations ?? [],
    requiredMapTableRecipes: report.requiredMapTableRecipes ?? [],
    deferredStationBlocks: report.deferredStationBlocks ?? [],
    nonInventoryUnlockRefs: report.nonInventoryUnlockRefs ?? [],
    stationSurfaceLoadStep: report.stationSurfaceLoadStep,
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableWorldgenExplorationReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    realRuntimeWorldgenRequiredBeforePublicAlpha: report.realRuntimeWorldgenRequiredBeforePublicAlpha,
    packId: report.packId,
    displayName: report.displayName,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    contracts: report.contracts,
    artifact: report.artifact,
    counts: report.counts,
    spawnSafety: report.spawnSafety,
    biomeSummaries: report.biomeSummaries ?? [],
    landmarkIds: report.landmarkIds ?? [],
    creatureIds: report.creatureIds ?? [],
    holomapLayers: report.holomapLayers ?? [],
    holomapHintTypes: report.holomapHintTypes ?? [],
    worldgenResourceMarkers: report.worldgenResourceMarkers ?? [],
    landmarkHoloMapHintMarkers: report.landmarkHoloMapHintMarkers ?? [],
    nonPromptTutorialHooks: report.nonPromptTutorialHooks ?? [],
    worldgenLoadStep: report.worldgenLoadStep,
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableCreatureRosterReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    realRuntimeCreatureExecutionRequiredBeforePublicAlpha: report.realRuntimeCreatureExecutionRequiredBeforePublicAlpha,
    packId: report.packId,
    displayName: report.displayName,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    contracts: report.contracts,
    artifact: report.artifact,
    globalRules: report.globalRules,
    counts: report.counts,
    creatureIds: report.creatureIds ?? [],
    categoryCounts: report.categoryCounts,
    creatureSummaries: report.creatureSummaries ?? [],
    starterSafety: report.starterSafety,
    worldgenLoadStep: report.worldgenLoadStep,
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableOldRoadNetworkReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    realRuntimeOldRoadNetworkRequiredBeforePublicAlpha: report.realRuntimeOldRoadNetworkRequiredBeforePublicAlpha,
    packId: report.packId,
    displayName: report.displayName,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    contracts: report.contracts,
    artifact: report.artifact,
    counts: report.counts,
    oldRoadBlockIds: report.oldRoadBlockIds ?? [],
    routeItemIds: report.routeItemIds ?? [],
    routeRecipeIds: report.routeRecipeIds ?? [],
    roadLandmarkIds: report.roadLandmarkIds ?? [],
    routeRecipeSummaries: report.routeRecipeSummaries ?? [],
    holomapOldRoadContract: report.holomapOldRoadContract,
    waystoneRouteContract: report.waystoneRouteContract,
    playtestCoverage: report.playtestCoverage,
    oldRoadNetworkLoadStep: report.oldRoadNetworkLoadStep,
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableAlphaSystemsReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    realRuntimeAlphaSystemsRequiredBeforePublicAlpha: report.realRuntimeAlphaSystemsRequiredBeforePublicAlpha,
    packId: report.packId,
    displayName: report.displayName,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    contracts: report.contracts,
    artifact: report.artifact,
    relaxedStandardGuarantees: report.relaxedStandardGuarantees,
    homesteadSummary: report.homesteadSummary,
    builderUxSummary: report.builderUxSummary,
    coopSummary: report.coopSummary,
    publicAlphaMinimum: report.publicAlphaMinimum,
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableHarnessDriverManifestTemplate(manifest) {
  if (!manifest || typeof manifest !== 'object') return manifest
  const { generatedAt, ...stableManifest } = manifest
  return stableManifest
}

function localRuntimeFixturePath(moduleRoot, ref) {
  if (typeof ref !== 'string') return null
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  if (ref.startsWith('META-INF/')) return path.join(resourcesRoot, ref)
  if (ref.startsWith('assets/')) return path.join(resourcesRoot, ref)
  return path.join(dataRoot, ref)
}

function stableLocalRuntimeScenario(scenario) {
  return {
    id: scenario.id,
    suiteId: scenario.suiteId,
    status: scenario.status,
    runtimeTarget: scenario.runtimeTarget,
    artifactSha256: scenario.artifactSha256,
    inputFixtureRefs: scenario.inputFixtureRefs ?? [],
    fixtureResults: scenario.fixtureResults ?? [],
    plannedActions: scenario.plannedActions ?? [],
    rehearsalActions: scenario.rehearsalActions ?? [],
    assertions: (scenario.assertions ?? []).map((assertion) => ({ id: assertion.id, status: assertion.status })),
    captures: scenario.captures,
    savedArtifacts: scenario.savedArtifacts ?? [],
    realRuntimeExecutionRequiredBeforePublicAlpha: scenario.realRuntimeExecutionRequiredBeforePublicAlpha,
    clearsRuntimeGates: scenario.clearsRuntimeGates,
  }
}

function stableLocalRuntimeRehearsal(report) {
  return {
    runtimeExecutionContract: report.runtimeExecutionContract,
    playableRuntimeContract: report.playableRuntimeContract,
    runtimeCoreReport: report.runtimeCoreReport,
    localLauncherRehearsalReport: report.localLauncherRehearsalReport,
    moduleArtifact: report.moduleArtifact,
    moduleArtifactSha256: report.moduleArtifactSha256,
    moduleArtifactSize: report.moduleArtifactSize,
    scenarioResults: (report.scenarioResults ?? []).map(stableLocalRuntimeScenario),
    scenarioCount: report.scenarioCount,
    blockedBy: report.blockedBy ?? [],
    proofs: report.proofs ?? [],
  }
}

function stableRuntimeExecutionScenario(scenario) {
  return {
    id: scenario.id,
    suiteId: scenario.suiteId,
    status: scenario.status,
    durationMs: scenario.durationMs,
    runtimeTarget: scenario.runtimeTarget,
    artifactSha256: scenario.artifactSha256,
    inputFixtureRefs: scenario.inputFixtureRefs ?? [],
    plannedActions: scenario.plannedActions ?? [],
    actionsRun: scenario.actionsRun ?? [],
    assertions: (scenario.assertions ?? []).map((assertion) => ({
      id: assertion.id,
      status: assertion.status,
      reason: assertion.reason,
    })),
    savedArtifacts: scenario.savedArtifacts ?? [],
    blockedBy: scenario.blockedBy ?? [],
  }
}

function stableRuntimeExecutionReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    generatedBy: report.generatedBy,
    edition: report.edition,
    packId: report.packId,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    moduleArtifact: report.moduleArtifact,
    moduleArtifactSha256: report.moduleArtifactSha256,
    runtimeBuild: report.runtimeBuild,
    executionEnvironment: report.executionEnvironment,
    scenarioResults: (report.scenarioResults ?? []).map(stableRuntimeExecutionScenario),
    clearedRuntimeGates: report.clearedRuntimeGates ?? [],
    remainingRuntimeGates: report.remainingRuntimeGates ?? [],
    publicAlphaReady: report.publicAlphaReady,
    blockedBy: report.blockedBy ?? [],
  }
}

function stableLauncherExecutionFlow(flow) {
  return {
    id: flow.id,
    status: flow.status,
    durationMs: flow.durationMs,
    runtimeTarget: flow.runtimeTarget,
    artifactSha256: flow.artifactSha256,
    inputFixtureRefs: flow.inputFixtureRefs ?? [],
    preconditions: flow.preconditions ?? [],
    plannedActions: flow.plannedActions ?? [],
    actionsRun: flow.actionsRun ?? [],
    assertions: (flow.assertions ?? []).map((assertion) => ({
      id: assertion.id,
      status: assertion.status,
      reason: assertion.reason,
    })),
    savedArtifacts: flow.savedArtifacts ?? [],
    requiredSavedArtifacts: flow.requiredSavedArtifacts ?? [],
    worldStatePolicy: flow.worldStatePolicy,
    blockedBy: flow.blockedBy ?? [],
  }
}

function stableLauncherExecutionReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    generatedBy: report.generatedBy,
    edition: report.edition,
    packId: report.packId,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    moduleArtifact: report.moduleArtifact,
    moduleArtifactSha256: report.moduleArtifactSha256,
    launcherBuild: report.launcherBuild,
    executionEnvironment: report.executionEnvironment,
    flowResults: (report.flowResults ?? []).map(stableLauncherExecutionFlow),
    clearedLauncherGates: report.clearedLauncherGates ?? [],
    remainingLauncherGates: report.remainingLauncherGates ?? [],
    publicAlphaReady: report.publicAlphaReady,
    blockedBy: report.blockedBy ?? [],
  }
}

function stableFinalReviewArea(area) {
  return {
    id: area.id,
    displayName: area.displayName,
    gateIds: area.gateIds ?? [],
    status: area.status,
    checklist: (area.checklist ?? []).map((item) => ({
      id: item.id,
      status: item.status,
      reason: item.reason,
    })),
    findings: area.findings ?? [],
    savedArtifacts: area.savedArtifacts ?? [],
    requiredSavedArtifacts: area.requiredSavedArtifacts ?? [],
    inputFixtureRefs: area.inputFixtureRefs ?? [],
  }
}

function stableFinalReleaseReviewReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    generatedBy: report.generatedBy,
    edition: report.edition,
    packId: report.packId,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    moduleArtifact: report.moduleArtifact,
    moduleArtifactSha256: report.moduleArtifactSha256,
    assetManifestHash: report.assetManifestHash,
    legalAuditHash: report.legalAuditHash,
    reviewer: report.reviewer,
    reviewDate: report.reviewDate,
    reviewResults: (report.reviewResults ?? []).map(stableFinalReviewArea),
    clearedFinalReviewGates: report.clearedFinalReviewGates ?? [],
    remainingFinalReviewGates: report.remainingFinalReviewGates ?? [],
    publicReleaseReady: report.publicReleaseReady,
    blockedBy: report.blockedBy ?? [],
  }
}

function stableDistributionApprovalArea(area) {
  return {
    id: area.id,
    displayName: area.displayName,
    gateIds: area.gateIds ?? [],
    status: area.status,
    checklist: (area.checklist ?? []).map((item) => ({
      id: item.id,
      status: item.status,
      reason: item.reason,
    })),
    evidenceRefs: area.evidenceRefs ?? [],
    savedArtifacts: area.savedArtifacts ?? [],
    requiredSavedArtifacts: area.requiredSavedArtifacts ?? [],
    blockedBy: area.blockedBy ?? [],
  }
}

function stableDistributionApprovalReport(report) {
  return {
    schema: report.schema,
    status: report.status,
    generatedBy: report.generatedBy,
    edition: report.edition,
    packId: report.packId,
    runtimeTarget: report.runtimeTarget,
    moduleId: report.moduleId,
    moduleVersion: report.moduleVersion,
    releaseId: report.releaseId,
    releaseIndex: report.releaseIndex,
    approvalRun: report.approvalRun,
    approvalResults: (report.approvalResults ?? []).map(stableDistributionApprovalArea),
    clearedDistributionGates: report.clearedDistributionGates ?? [],
    remainingDistributionGates: report.remainingDistributionGates ?? [],
    publicAlphaReady: report.publicAlphaReady,
    blockedBy: report.blockedBy ?? [],
  }
}

function readJsonIfPresent(filePath) {
  return fileExists(filePath) ? readJson(filePath) : null
}

function runJson(command, args, cwd) {
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

function resolveReleaseRootPath(value, releaseRoot) {
  if (typeof value !== 'string' || value.length === 0) return null
  return path.isAbsolute(value) ? path.resolve(value) : path.resolve(releaseRoot, value)
}

function validateRuntimeCoreReport(errors, { report, edition, expectedDryRun, outputSuffixLabel, moduleArtifactPath, expectedFreshReport }) {
  assert(errors, report.schema === 'echo.openlands.edition.runtime_core_report.v1', `${edition.directory} runtime core report schema mismatch`)
  assert(errors, report.status === 'passed', `${edition.directory} runtime core report status must be passed`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} runtime core report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} runtime core report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} runtime core report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} runtime core report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} runtime core report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} runtime core report moduleVersion mismatch`)
  assert(errors, report.runtimeCorePackage === 'com.knoxhack.echoopenlandsprotocol.runtime', `${edition.directory} runtime core package mismatch`)
  assert(errors, typeof report.artifactPath === 'string' && path.resolve(report.artifactPath) === path.resolve(moduleArtifactPath), `${edition.directory} runtime core artifact path mismatch`)
  assert(errors, typeof report.inspectedArtifactPath === 'string' && path.resolve(report.inspectedArtifactPath) === path.resolve(moduleArtifactPath), `${edition.directory} runtime core inspected artifact path mismatch`)
  assert(errors, ['compiled-runtime', 'source-packaged'].includes(report.artifactMode), `${edition.directory} runtime core artifact mode mismatch`)
  assert(errors, Number.isInteger(report.compiledSources) && report.compiledSources > 0, `${edition.directory} runtime core compiled source count mismatch`)
  assert(errors, Array.isArray(report.artifactEntriesChecked) && report.artifactEntriesChecked.length > 0, `${edition.directory} runtime core must list artifact entries checked`)
  assert(errors, report.artifactEntriesChecked?.includes('META-INF/echo.mod.json'), `${edition.directory} runtime core must inspect echo.mod descriptor`)
  assert(errors, report.artifactEntriesChecked?.includes('data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json'), `${edition.directory} runtime core must inspect playable runtime contract`)
  for (const proof of REQUIRED_RUNTIME_CORE_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} runtime core report missing proof ${proof}`)
  }
  assert(errors,
    RUNTIME_CORE_ARTIFACT_PROOFS.some((proof) => report.proofs?.includes(proof)),
    `${edition.directory} runtime core report missing runtime core artifact proof`)
  for (const hook of ['standardRules', 'validateStarterSpawn', 'scoreShelter', 'advanceWaystone', 'advanceCrop', 'cookpotMealReady', 'validateBuilderAction', 'firstHourStepIds', 'adapterBindingManifest']) {
    assert(errors, report.callableHooks?.includes(hook), `${edition.directory} runtime core report missing hook ${hook}`)
  }
  if (expectedFreshReport) {
    assert(errors, report.artifactMode === expectedFreshReport.artifactMode, `${edition.directory} runtime core artifact mode stale against dry-run`)
    assert(errors, report.inspectedArtifactEntry === expectedFreshReport.inspectedArtifactEntry, `${edition.directory} runtime core inspected artifact entry stale against dry-run`)
    assert(errors, report.compiledSources === expectedFreshReport.compiledSources, `${edition.directory} runtime core compiled source count stale against dry-run`)
    assert(errors, sameStringList(report.artifactEntriesChecked ?? [], expectedFreshReport.artifactEntriesChecked ?? []), `${edition.directory} runtime core artifact entries stale against dry-run`)
    assert(errors, sameStringList(report.callableHooks ?? [], expectedFreshReport.callableHooks ?? []), `${edition.directory} runtime core callable hooks stale against dry-run`)
    assert(errors, sameStringList(report.proofs ?? [], expectedFreshReport.proofs ?? []), `${edition.directory} runtime core proofs stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} runtime core report stale against dry-run`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} runtime core report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateRuntimeExecutionReport(errors, { report, edition, moduleArtifactPath, runtimeScenarioIds, outputSuffixLabel, expectedFreshReport = null }) {
  assert(errors, report.schema === 'echo.openlands.edition.runtime_execution_report.v1', `${edition.directory} runtime execution report schema mismatch`)
  assert(errors, ['blocked', 'passed'].includes(report.status), `${edition.directory} runtime execution report status must be blocked or passed`)
  assert(errors, report.edition === edition.key, `${edition.directory} runtime execution report edition mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} runtime execution report packId mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} runtime execution report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} runtime execution report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} runtime execution report moduleVersion mismatch`)
  assert(errors, typeof report.moduleArtifact === 'string' && fileExists(report.moduleArtifact), `${edition.directory} runtime execution module artifact path must exist`)
  assert(errors, sameResolvedPath(report.moduleArtifact, moduleArtifactPath), `${edition.directory} runtime execution module artifact path mismatch`)
  assert(errors, report.moduleArtifactSha256 === sha256File(moduleArtifactPath), `${edition.directory} runtime execution module artifact sha mismatch`)
  assert(errors, sameStringList((report.scenarioResults ?? []).map((scenario) => scenario.id), runtimeScenarioIds), `${edition.directory} runtime execution scenario order mismatch`)
  assert(errors, report.publicAlphaReady === false || report.status === 'passed', `${edition.directory} runtime execution publicAlphaReady requires passed status`)
  if (report.status === 'blocked') {
    assert(errors, report.generatedBy === 'generate-openlands-runtime-execution-report.mjs', `${edition.directory} blocked runtime execution report generatedBy mismatch`)
    assert(errors, report.runtimeBuild?.status === 'not_executed', `${edition.directory} blocked runtime execution build status mismatch`)
    assert(errors, report.executionEnvironment?.status === 'not_executed', `${edition.directory} blocked runtime execution environment status mismatch`)
    assert(errors, (report.clearedRuntimeGates ?? []).length === 0, `${edition.directory} blocked runtime execution report must clear zero gates`)
    assert(errors, (report.remainingRuntimeGates ?? []).length >= 14, `${edition.directory} blocked runtime execution report must keep gates remaining`)
    assert(errors, report.blockedBy?.includes('real_runtime_execution_report_missing'), `${edition.directory} blocked runtime execution report missing runtime blocker`)
    for (const scenario of report.scenarioResults ?? []) {
      assert(errors, scenario.status === 'blocked', `${edition.directory} blocked runtime execution scenario ${scenario.id} status mismatch`)
      assert(errors, scenario.runtimeTarget === edition.runtimeTarget, `${edition.directory} blocked runtime execution scenario ${scenario.id} runtime target mismatch`)
      assert(errors, scenario.artifactSha256 === report.moduleArtifactSha256, `${edition.directory} blocked runtime execution scenario ${scenario.id} artifact sha mismatch`)
      assert(errors, Array.isArray(scenario.actionsRun) && scenario.actionsRun.length === 0, `${edition.directory} blocked runtime execution scenario ${scenario.id} must not record actions run`)
      assert(errors, Array.isArray(scenario.savedArtifacts) && scenario.savedArtifacts.length === 0, `${edition.directory} blocked runtime execution scenario ${scenario.id} must not record saved artifacts`)
      assert(errors, scenario.blockedBy?.includes('real_adapter_execution_missing'), `${edition.directory} blocked runtime execution scenario ${scenario.id} missing adapter blocker`)
      for (const assertion of scenario.assertions ?? []) {
        assert(errors, assertion.status === 'blocked', `${edition.directory} blocked runtime execution assertion ${scenario.id}/${assertion.id} status mismatch`)
      }
    }
  }
  if (expectedFreshReport && report.status === 'blocked' && expectedFreshReport.status === 'blocked') {
    assert(errors, sameJson(stableRuntimeExecutionReport(report), stableRuntimeExecutionReport(expectedFreshReport)), `${edition.directory} runtime execution blocked report stale against dry-run`)
    assert(errors, sameJson(stableBlockedGeneratorReport(report), stableBlockedGeneratorReport(expectedFreshReport)), `${edition.directory} runtime execution blocked report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} runtime execution report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateLauncherExecutionReport(errors, { report, edition, moduleArtifactPath, launcherFlowIds, outputSuffixLabel, expectedFreshReport = null }) {
  assert(errors, report.schema === 'echo.openlands.edition.launcher_execution_report.v1', `${edition.directory} launcher execution report schema mismatch`)
  assert(errors, ['blocked', 'passed'].includes(report.status), `${edition.directory} launcher execution report status must be blocked or passed`)
  assert(errors, report.edition === edition.key, `${edition.directory} launcher execution report edition mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} launcher execution report packId mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} launcher execution report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} launcher execution report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} launcher execution report moduleVersion mismatch`)
  assert(errors, typeof report.moduleArtifact === 'string' && fileExists(report.moduleArtifact), `${edition.directory} launcher execution module artifact path must exist`)
  assert(errors, sameResolvedPath(report.moduleArtifact, moduleArtifactPath), `${edition.directory} launcher execution module artifact path mismatch`)
  assert(errors, report.moduleArtifactSha256 === sha256File(moduleArtifactPath), `${edition.directory} launcher execution module artifact sha mismatch`)
  assert(errors, sameStringList((report.flowResults ?? []).map((flow) => flow.id), launcherFlowIds), `${edition.directory} launcher execution flow order mismatch`)
  assert(errors, report.publicAlphaReady === false || report.status === 'passed', `${edition.directory} launcher execution publicAlphaReady requires passed status`)
  if (report.status === 'blocked') {
    assert(errors, report.generatedBy === 'generate-openlands-launcher-execution-report.mjs', `${edition.directory} blocked launcher execution report generatedBy mismatch`)
    assert(errors, report.launcherBuild?.status === 'not_executed', `${edition.directory} blocked launcher execution build status mismatch`)
    assert(errors, report.executionEnvironment?.status === 'not_executed', `${edition.directory} blocked launcher execution environment status mismatch`)
    assert(errors, (report.clearedLauncherGates ?? []).length === 0, `${edition.directory} blocked launcher execution report must clear zero gates`)
    assert(errors, (report.remainingLauncherGates ?? []).length >= 5, `${edition.directory} blocked launcher execution report must keep gates remaining`)
    assert(errors, report.blockedBy?.includes('real_launcher_install_update_repair_rollback_execution_missing'), `${edition.directory} blocked launcher execution report missing launcher execution blocker`)
    for (const flow of report.flowResults ?? []) {
      assert(errors, flow.status === 'blocked', `${edition.directory} blocked launcher execution flow ${flow.id} status mismatch`)
      assert(errors, flow.runtimeTarget === edition.runtimeTarget, `${edition.directory} blocked launcher execution flow ${flow.id} runtime target mismatch`)
      assert(errors, flow.artifactSha256 === report.moduleArtifactSha256, `${edition.directory} blocked launcher execution flow ${flow.id} artifact sha mismatch`)
      assert(errors, Array.isArray(flow.actionsRun) && flow.actionsRun.length === 0, `${edition.directory} blocked launcher execution flow ${flow.id} must not record actions run`)
      assert(errors, Array.isArray(flow.savedArtifacts) && flow.savedArtifacts.length === 0, `${edition.directory} blocked launcher execution flow ${flow.id} must not record saved artifacts`)
      assert(errors, Array.isArray(flow.requiredSavedArtifacts) && flow.requiredSavedArtifacts.length > 0, `${edition.directory} blocked launcher execution flow ${flow.id} must keep required saved artifacts`)
      assert(errors, flow.blockedBy?.includes('real_launcher_execution_missing'), `${edition.directory} blocked launcher execution flow ${flow.id} missing launcher blocker`)
      for (const assertion of flow.assertions ?? []) {
        assert(errors, assertion.status === 'blocked', `${edition.directory} blocked launcher execution assertion ${flow.id}/${assertion.id} status mismatch`)
      }
    }
  }
  if (expectedFreshReport && report.status === 'blocked' && expectedFreshReport.status === 'blocked') {
    assert(errors, sameJson(stableLauncherExecutionReport(report), stableLauncherExecutionReport(expectedFreshReport)), `${edition.directory} launcher execution blocked report stale against dry-run`)
    assert(errors, sameJson(stableBlockedGeneratorReport(report), stableBlockedGeneratorReport(expectedFreshReport)), `${edition.directory} launcher execution blocked report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} launcher execution report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateFinalReleaseReviewReport(errors, { report, edition, finalReviewContracts, outputSuffixLabel, expectedFreshReport = null }) {
  const { moduleArtifactPath, assetManifestPath, legalAuditPath, reviewAreaIds, finalReviewGateIds } = finalReviewContracts
  assert(errors, report.schema === 'echo.openlands.edition.final_release_review_report.v1', `${edition.directory} final release review report schema mismatch`)
  assert(errors, ['blocked', 'passed'].includes(report.status), `${edition.directory} final release review report status must be blocked or passed`)
  assert(errors, report.edition === edition.key, `${edition.directory} final release review report edition mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} final release review report packId mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} final release review report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} final release review report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} final release review report moduleVersion mismatch`)
  assert(errors, typeof report.moduleArtifact === 'string' && fileExists(report.moduleArtifact), `${edition.directory} final release review module artifact path must exist`)
  assert(errors, sameResolvedPath(report.moduleArtifact, moduleArtifactPath), `${edition.directory} final release review module artifact path mismatch`)
  assert(errors, report.moduleArtifactSha256 === sha256File(moduleArtifactPath), `${edition.directory} final release review module artifact sha mismatch`)
  assert(errors, report.assetManifestHash === sha256File(assetManifestPath), `${edition.directory} final release review asset manifest hash mismatch`)
  assert(errors, report.legalAuditHash === sha256File(legalAuditPath), `${edition.directory} final release review legal audit hash mismatch`)
  assert(errors, sameStringList((report.reviewResults ?? []).map((area) => area.id), reviewAreaIds), `${edition.directory} final release review area order mismatch`)
  assert(errors, report.publicReleaseReady === false || report.status === 'passed', `${edition.directory} final release review publicReleaseReady requires passed status`)
  if (report.status === 'blocked') {
    assert(errors, report.generatedBy === 'generate-openlands-final-release-review-report.mjs', `${edition.directory} blocked final release review report generatedBy mismatch`)
    assert(errors, report.reviewer === null, `${edition.directory} blocked final release review reviewer must be null`)
    assert(errors, report.reviewDate === null, `${edition.directory} blocked final release review reviewDate must be null`)
    assert(errors, (report.clearedFinalReviewGates ?? []).length === 0, `${edition.directory} blocked final release review report must clear zero gates`)
    assert(errors, sameStringList(report.remainingFinalReviewGates ?? [], finalReviewGateIds), `${edition.directory} blocked final release review remaining gates mismatch`)
    assert(errors, report.blockedBy?.includes('final_asset_human_review_missing'), `${edition.directory} blocked final release review report missing asset human review blocker`)
    assert(errors, report.blockedBy?.includes('final_art_audio_review_missing'), `${edition.directory} blocked final release review report missing art/audio review blocker`)
    for (const area of report.reviewResults ?? []) {
      assert(errors, area.status === 'blocked', `${edition.directory} blocked final release review area ${area.id} status mismatch`)
      assert(errors, Array.isArray(area.gateIds) && area.gateIds.length > 0, `${edition.directory} blocked final release review area ${area.id} must keep gate ids`)
      assert(errors, area.findings?.includes('final_human_review_missing'), `${edition.directory} blocked final release review area ${area.id} missing human review finding`)
      assert(errors, area.findings?.includes('placeholder_assets_block_public_release'), `${edition.directory} blocked final release review area ${area.id} missing placeholder finding`)
      assert(errors, Array.isArray(area.savedArtifacts) && area.savedArtifacts.length === 0, `${edition.directory} blocked final release review area ${area.id} must not record saved artifacts`)
      assert(errors, Array.isArray(area.requiredSavedArtifacts) && area.requiredSavedArtifacts.length > 0, `${edition.directory} blocked final release review area ${area.id} must keep required saved artifacts`)
      assert(errors, Array.isArray(area.inputFixtureRefs) && area.inputFixtureRefs.length > 0, `${edition.directory} blocked final release review area ${area.id} must keep input fixture refs`)
      for (const item of area.checklist ?? []) {
        assert(errors, item.status === 'blocked', `${edition.directory} blocked final release review checklist ${area.id}/${item.id} status mismatch`)
      }
    }
  }
  if (expectedFreshReport && report.status === 'blocked' && expectedFreshReport.status === 'blocked') {
    assert(errors, sameJson(stableFinalReleaseReviewReport(report), stableFinalReleaseReviewReport(expectedFreshReport)), `${edition.directory} final release review blocked report stale against dry-run`)
    assert(errors, sameJson(stableBlockedGeneratorReport(report), stableBlockedGeneratorReport(expectedFreshReport)), `${edition.directory} final release review blocked report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} final release review report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateDistributionApprovalReportAttachment(errors, { report, edition, distributionApprovalContracts, outputSuffixLabel, expectedFreshReport = null }) {
  const { distributionApproval, releaseIndex, releaseIndexPath, approvalAreaIds, distributionGateIds } = distributionApprovalContracts
  assert(errors, report.schema === 'echo.openlands.edition.distribution_approval_report.v1', `${edition.directory} distribution approval report schema mismatch`)
  assert(errors, ['blocked', 'passed'].includes(report.status), `${edition.directory} distribution approval report status must be blocked or passed`)
  assert(errors, report.edition === edition.key, `${edition.directory} distribution approval report edition mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} distribution approval report packId mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} distribution approval report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} distribution approval report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} distribution approval report moduleVersion mismatch`)
  assert(errors, report.releaseId === releaseIndex?.releaseId, `${edition.directory} distribution approval release id mismatch`)
  assert(errors, sameResolvedPath(report.releaseIndex?.path, releaseIndexPath), `${edition.directory} distribution approval release index path mismatch`)
  assert(errors, report.releaseIndex?.hash === sha256File(releaseIndexPath), `${edition.directory} distribution approval release index hash mismatch`)
  assert(errors, sameStringList((report.approvalResults ?? []).map((area) => area.id), approvalAreaIds), `${edition.directory} distribution approval area order mismatch`)
  assert(errors, report.publicAlphaReady === false || report.status === 'passed', `${edition.directory} distribution approval publicAlphaReady requires passed status`)
  if (report.status === 'blocked') {
    assert(errors, report.generatedBy === 'generate-openlands-distribution-approval-report.mjs', `${edition.directory} blocked distribution approval report generatedBy mismatch`)
    assert(errors, report.releaseIndex?.artifactDownloadUrlsPresent === false, `${edition.directory} blocked distribution approval must record missing artifact download URLs`)
    assert(errors, report.releaseIndex?.approvedState === false, `${edition.directory} blocked distribution approval must record unapproved release index state`)
    assert(errors, report.approvalRun?.status === 'not_executed', `${edition.directory} blocked distribution approval run status mismatch`)
    assert(errors, report.approvalRun?.approver === null, `${edition.directory} blocked distribution approval approver must be null`)
    assert(errors, report.approvalRun?.approvalDate === null, `${edition.directory} blocked distribution approval date must be null`)
    assert(errors, (report.clearedDistributionGates ?? []).length === 0, `${edition.directory} blocked distribution approval report must clear zero gates`)
    assert(errors, sameStringList(report.remainingDistributionGates ?? [], distributionGateIds), `${edition.directory} blocked distribution approval remaining gates mismatch`)
    assert(errors, report.blockedBy?.includes('release_index_download_urls_missing'), `${edition.directory} blocked distribution approval missing release URL blocker`)
    assert(errors, report.blockedBy?.includes('public_alpha_release_index_approval_missing'), `${edition.directory} blocked distribution approval missing Release Index approval blocker`)
    assert(errors, report.blockedBy?.includes('public_alpha_coop_session_test_missing'), `${edition.directory} blocked distribution approval missing co-op session blocker`)
    assert(errors, report.blockedBy?.includes('runtime_launcher_or_final_review_gate_missing'), `${edition.directory} blocked distribution approval missing dependency gate blocker`)
    for (const area of report.approvalResults ?? []) {
      const expected = (distributionApproval.approvalAreas ?? []).find((entry) => entry.id === area.id)
      assert(errors, expected !== undefined, `${edition.directory} blocked distribution approval area ${area.id} is not in acceptance`)
      if (!expected) continue
      assert(errors, area.status === 'blocked', `${edition.directory} blocked distribution approval area ${area.id} status mismatch`)
      assert(errors, sameStringList(area.gateIds ?? [], expected.gateIds ?? []), `${edition.directory} blocked distribution approval area ${area.id} gate ids mismatch`)
      assert(errors, sameStringList(area.evidenceRefs ?? [], expected.inputFixtureRefs ?? []), `${edition.directory} blocked distribution approval area ${area.id} evidence refs mismatch`)
      assert(errors, Array.isArray(area.savedArtifacts) && area.savedArtifacts.length === 0, `${edition.directory} blocked distribution approval area ${area.id} must not record saved artifacts`)
      assert(errors, sameStringList(area.requiredSavedArtifacts ?? [], expected.requiredSavedArtifacts ?? []), `${edition.directory} blocked distribution approval area ${area.id} required saved artifacts mismatch`)
      assert(errors, area.blockedBy?.includes('distribution_approval_missing'), `${edition.directory} blocked distribution approval area ${area.id} missing approval blocker`)
      assert(errors, sameStringList((area.checklist ?? []).map((item) => item.id), expected.checklist ?? []), `${edition.directory} blocked distribution approval area ${area.id} checklist mismatch`)
      for (const item of area.checklist ?? []) {
        assert(errors, item.status === 'blocked', `${edition.directory} blocked distribution approval checklist ${area.id}/${item.id} status mismatch`)
      }
    }
  }
  if (expectedFreshReport && report.status === 'blocked' && expectedFreshReport.status === 'blocked') {
    assert(errors, sameJson(stableDistributionApprovalReport(report), stableDistributionApprovalReport(expectedFreshReport)), `${edition.directory} distribution approval blocked report stale against dry-run`)
    assert(errors, sameJson(stableBlockedGeneratorReport(report), stableBlockedGeneratorReport(expectedFreshReport)), `${edition.directory} distribution approval blocked report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} distribution approval report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateHarnessRunnerDryRun(errors, { scriptPath, moduleRoot, repoRoot, edition, schema, harnessType, readyField, clearedGateField, remainingGateField, minimumRemainingGates }) {
  assert(errors, fileExists(scriptPath), `${edition.directory} missing scripts/${path.basename(scriptPath)}`)
  if (!fileExists(scriptPath)) return
  const dryRun = runJson('node', [
    scriptPath,
    '--module-root',
    moduleRoot,
    '--dry-run',
    '--json',
  ], repoRoot)
  assert(errors, dryRun.status === 0, `${edition.directory} ${path.basename(scriptPath)} dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
  if (dryRun.status !== 0) return
  try {
    const report = JSON.parse(dryRun.stdout)
    assert(errors, report.schema === schema, `${edition.directory} ${path.basename(scriptPath)} schema mismatch`)
    assert(errors, report.status === 'blocked', `${edition.directory} ${path.basename(scriptPath)} must produce a blocked report until real drivers exist`)
    assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} ${path.basename(scriptPath)} runtime target mismatch`)
    assert(errors, report[readyField] === false, `${edition.directory} ${path.basename(scriptPath)} must not mark readiness true`)
    assert(errors, Array.isArray(report[clearedGateField]) && report[clearedGateField].length === 0, `${edition.directory} ${path.basename(scriptPath)} must clear zero gates`)
    assert(errors, Array.isArray(report[remainingGateField]) && report[remainingGateField].length >= minimumRemainingGates, `${edition.directory} ${path.basename(scriptPath)} remaining gate count too low`)
    assert(errors, report.harnessRun?.schema === 'echo.openlands.edition.harness_run.v1', `${edition.directory} ${path.basename(scriptPath)} missing harnessRun`)
    assert(errors, report.harnessRun?.harnessType === harnessType, `${edition.directory} ${path.basename(scriptPath)} harness type mismatch`)
    assert(errors, report.harnessRun?.status === 'blocked', `${edition.directory} ${path.basename(scriptPath)} harnessRun must be blocked`)
    assert(errors, report.harnessRun?.driverManifest?.schema === 'echo.openlands.edition.harness_driver_manifest.v1', `${edition.directory} ${path.basename(scriptPath)} must attach the harness driver manifest template`)
    assert(errors, report.harnessRun?.driverManifest?.path?.endsWith(edition.harnessDriverManifestTemplate.replace(/\//g, path.sep)), `${edition.directory} ${path.basename(scriptPath)} driver manifest path mismatch`)
    const driverSummary = report.harnessRun?.driverSummary ?? {}
    assert(errors, (driverSummary.requiredDriverSurfaceCount ?? 0) > 0, `${edition.directory} ${path.basename(scriptPath)} must declare required driver surfaces`)
    assert(errors, driverSummary.declaredDriverSurfaceCount === 0, `${edition.directory} ${path.basename(scriptPath)} template manifest must declare zero driver surfaces`)
    assert(errors, driverSummary.ignoredDriverSurfaceCount === 0, `${edition.directory} ${path.basename(scriptPath)} template manifest must ignore zero driver surfaces`)
    assert(errors, driverSummary.completeDriverSurfaceCount === 0, `${edition.directory} ${path.basename(scriptPath)} template manifest must complete zero driver surfaces`)
    assert(errors, driverSummary.incompleteDriverSurfaceCount === 0, `${edition.directory} ${path.basename(scriptPath)} template manifest must have zero incomplete declared driver surfaces`)
    assert(errors, driverSummary.availableDriverSurfaceCount === 0, `${edition.directory} ${path.basename(scriptPath)} template manifest must expose zero available driver surfaces`)
    assert(errors, driverSummary.missingDriverSurfaceCount === driverSummary.requiredDriverSurfaceCount, `${edition.directory} ${path.basename(scriptPath)} template manifest must report every required driver as missing`)
    assert(errors, sameStringSet(driverSummary.missingDriverSurfaceIds, driverSummary.requiredDriverSurfaceIds), `${edition.directory} ${path.basename(scriptPath)} missing driver ids must match required driver ids before real drivers exist`)
    assert(errors, Array.isArray(driverSummary.driverCompletenessDetails) && driverSummary.driverCompletenessDetails.length === driverSummary.requiredDriverSurfaceCount, `${edition.directory} ${path.basename(scriptPath)} must report driver completeness details for every required driver`)
    for (const driver of driverSummary.driverCompletenessDetails ?? []) {
      assert(errors, driver.declared === false, `${edition.directory} ${path.basename(scriptPath)} template driver ${driver.id} must not be declared`)
      assert(errors, driver.complete === false, `${edition.directory} ${path.basename(scriptPath)} template driver ${driver.id} must not be complete`)
    }
    assert(errors, !report.harnessRun?.blockedBy?.includes('harness_driver_manifest_missing'), `${edition.directory} ${path.basename(scriptPath)} must load the template manifest instead of reporting it missing`)
    assert(errors, report.harnessRun?.blockedBy?.includes('real_harness_execution_not_run'), `${edition.directory} ${path.basename(scriptPath)} must preserve real execution blocker`)
  } catch (error) {
    errors.push(`${edition.directory} ${path.basename(scriptPath)} did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
  }
}

function validateHarnessDriverManifestTemplate(errors, manifest, edition, contract) {
  assert(errors, manifest.schema === contract.reportContract?.schema, `${edition.directory} harness driver manifest schema mismatch`)
  assert(errors, manifest.edition === edition.key, `${edition.directory} harness driver manifest edition mismatch`)
  assert(errors, manifest.runtimeTarget === edition.runtimeTarget, `${edition.directory} harness driver manifest runtime target mismatch`)
  assert(errors, manifest.moduleId === MODULE_ID, `${edition.directory} harness driver manifest module id mismatch`)
  assert(errors, manifest.moduleVersion === VERSION, `${edition.directory} harness driver manifest module version mismatch`)
  assert(errors, manifest.status === 'template_blocked', `${edition.directory} harness driver manifest must remain template_blocked until real drivers exist`)
  assert(errors, manifest.harnessDriverManifestContract === HARNESS_DRIVER_MANIFEST_CONTRACT, `${edition.directory} harness driver manifest contract path mismatch`)
  assert(errors, manifest.artifactPattern === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} harness driver manifest artifact pattern mismatch`)
  assert(errors, Array.isArray(manifest.availableDriverSurfaces) && manifest.availableDriverSurfaces.length === 0, `${edition.directory} harness driver manifest template must have no available drivers`)
  assert(errors, Array.isArray(manifest.missingDriverSurfaces) && manifest.missingDriverSurfaces.length > 0, `${edition.directory} harness driver manifest template must list missing drivers`)
  for (const blocker of contract.blockedTemplateRules?.requiredBlockedBy ?? []) {
    assert(errors, manifest.blockedBy?.includes(blocker), `${edition.directory} harness driver manifest missing blocker ${blocker}`)
  }
  for (const nextStep of contract.blockedTemplateRules?.requiredNextSteps ?? []) {
    assert(errors, manifest.nextSteps?.includes(nextStep), `${edition.directory} harness driver manifest missing next step ${nextStep}`)
  }

  for (const [key, value] of Object.entries(contract.sourceContracts ?? {})) {
    assert(errors, manifest.sourceContracts?.[key] === value, `${edition.directory} harness driver manifest source contract ${key} mismatch`)
  }

  const expectedTemplate = (contract.editionManifestTemplates ?? []).find((entry) => entry.edition === edition.key)
  assert(errors, expectedTemplate !== undefined, `${edition.directory} harness driver manifest contract missing edition template`)
  if (expectedTemplate) {
    assert(errors, expectedTemplate.path === edition.harnessDriverManifestTemplate, `${edition.directory} harness driver manifest template path mismatch`)
    assert(errors, expectedTemplate.runtimeTarget === edition.runtimeTarget, `${edition.directory} harness driver manifest contract runtime target mismatch`)
    assert(errors, expectedTemplate.artifactPattern === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} harness driver manifest contract artifact mismatch`)
  }

  const families = manifest.harnessFamilies ?? []
  assert(errors, sameStringSet(families.map((family) => family.id), (contract.harnessFamilies ?? []).map((family) => family.id)), `${edition.directory} harness driver manifest family ids mismatch`)
  const flatMissing = []
  for (const contractFamily of contract.harnessFamilies ?? []) {
    const family = families.find((entry) => entry.id === contractFamily.id)
    assert(errors, family !== undefined, `${edition.directory} harness driver manifest missing family ${contractFamily.id}`)
    if (!family) continue
    assert(errors, family.plan === contractFamily.plan, `${edition.directory} harness driver manifest family ${contractFamily.id} plan mismatch`)
    assert(errors, family.bindingKey === contractFamily.bindingKey, `${edition.directory} harness driver manifest family ${contractFamily.id} binding key mismatch`)
    assert(errors, family.requiredReport === contractFamily.requiredReportPattern.replace('{edition}', edition.key), `${edition.directory} harness driver manifest family ${contractFamily.id} report path mismatch`)
    assert(errors, family.status === 'template_blocked', `${edition.directory} harness driver manifest family ${contractFamily.id} must be template_blocked`)
    assert(errors, Array.isArray(family.availableDriverSurfaceIds) && family.availableDriverSurfaceIds.length === 0, `${edition.directory} harness driver manifest family ${contractFamily.id} must have no available drivers`)
    assert(errors, sameStringSet(family.requiredDriverSurfaceIds, contractFamily.requiredDriverSurfaceIds), `${edition.directory} harness driver manifest family ${contractFamily.id} required driver ids mismatch`)
    assert(errors, sameStringSet(family.missingDriverSurfaceIds, contractFamily.requiredDriverSurfaceIds), `${edition.directory} harness driver manifest family ${contractFamily.id} missing driver ids mismatch`)
    assert(errors, sameStringSet(family.requiredBindingIds, contractFamily.requiredBindingIds), `${edition.directory} harness driver manifest family ${contractFamily.id} binding ids mismatch`)
    assert(errors, family.blockedBy?.includes(contractFamily.driverMissingBlocker), `${edition.directory} harness driver manifest family ${contractFamily.id} missing driver blocker`)
    assert(errors, family.blockedBy?.includes('real_harness_execution_not_run'), `${edition.directory} harness driver manifest family ${contractFamily.id} missing real execution blocker`)
    for (const driverId of contractFamily.requiredDriverSurfaceIds ?? []) {
      flatMissing.push(`${contractFamily.id}:${driverId}`)
    }
  }
  const actualFlatMissing = (manifest.missingDriverSurfaces ?? []).map((entry) => `${entry.harnessFamily}:${entry.id}`)
  assert(errors, sameStringSet(actualFlatMissing, flatMissing), `${edition.directory} harness driver manifest flat missing driver list mismatch`)
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

function moduleIds(manifest) {
  return new Set((manifest.moduleRequirements ?? []).map((entry) => entry.id))
}

function validateAdapterBootReport(errors, report, edition, runtimePlan, requiredPhases, runtimeEvidenceIds, publicAlphaEvidence, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expectedPhases = (runtimePlan.phases ?? []).map((phase) => ({
    id: phase.id,
    order: phase.order,
    gate: phase.gate,
  }))
  const expectedLoadSteps = (runtimePlan.loadSteps ?? []).map((step) => ({
    id: step.id,
    phase: step.phase,
    successSignal: step.successSignal,
    resources: (step.resourceIds ?? []).length,
    requiredEvidence: (step.requiredEvidence ?? []).length,
  }))
  const runtimeEvidenceSet = new Set(runtimeEvidenceIds)
  assert(errors, report.schema === 'echo.openlands.edition.adapter_boot_report.v1', `${edition.directory} adapter boot report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} adapter boot report status must be preflight_passed`)
  assert(errors, report.realAdapterBootRequiredBeforePublicAlpha === true, `${edition.directory} adapter boot report must require real adapter boot before public alpha`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} adapter boot report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} adapter boot report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} adapter boot report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} adapter boot report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} adapter boot report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} adapter boot report moduleVersion mismatch`)
  assert(errors, report.runtimeEvidenceContract === RUNTIME_EVIDENCE_CONTRACT, `${edition.directory} adapter boot runtime evidence contract mismatch`)
  assert(errors, report.artifact?.file === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} adapter boot artifact filename mismatch`)
  assert(errors, report.artifact?.kind === edition.moduleArtifactFamily, `${edition.directory} adapter boot artifact kind mismatch`)
  assert(errors, Array.isArray(report.artifact?.entriesChecked) && report.artifact.entriesChecked.includes(RUNTIME_EVIDENCE_CONTRACT), `${edition.directory} adapter boot report must inspect runtime evidence contract in artifact`)
  assert(errors, report.artifact?.entriesChecked?.includes('META-INF/echo.mod.json'), `${edition.directory} adapter boot report must inspect echo.mod descriptor`)
  assert(errors, report.artifact?.entriesChecked?.includes('com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.class'), `${edition.directory} adapter boot report must inspect compiled runtime contracts`)
  assert(errors, sameStringList(report.artifact?.entriesChecked ?? [], expectedAdapterArtifactEntries(runtimePlan)), `${edition.directory} adapter boot artifact entries checked mismatch`)
  if (edition.moduleArtifactFamily === 'echo-addon') {
    assert(errors, report.artifact?.nestedRuntimeEntry === `${MODULE_ID}-${VERSION}-runtime.jar` || report.artifact?.nestedRuntimeEntry === `lib/${MODULE_ID}-${VERSION}-runtime.jar`, `${edition.directory} adapter boot report must record native nested runtime jar`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.directory} adapter boot report should not record nested runtime jar for ${edition.moduleArtifactFamily}`)
  }
  assert(errors, report.descriptor?.id === MODULE_ID, `${edition.directory} adapter boot descriptor id mismatch`)
  assert(errors, report.descriptor?.version === VERSION, `${edition.directory} adapter boot descriptor version mismatch`)
  assert(errors, report.descriptor?.kind === 'pack_root', `${edition.directory} adapter boot descriptor kind mismatch`)
  assert(errors, report.descriptor?.role === 'official_pack', `${edition.directory} adapter boot descriptor role mismatch`)
  assert(errors, report.descriptor?.official === true, `${edition.directory} adapter boot descriptor must be official`)
  assert(errors, report.descriptor?.runtimes?.includes(edition.runtimeTarget), `${edition.directory} adapter boot descriptor runtimes missing ${edition.runtimeTarget}`)
  assert(errors, sameStringList((report.phases ?? []).map((phase) => phase.id), expectedPhases.map((phase) => phase.id)), `${edition.directory} adapter boot phase ids mismatch`)
  assert(errors, sameStringList((report.phases ?? []).map((phase) => phase.id), requiredPhases), `${edition.directory} adapter boot phase ids must match public alpha gate`)
  for (const expected of expectedPhases) {
    const phase = (report.phases ?? []).find((entry) => entry.id === expected.id)
    assert(errors, phase !== undefined, `${edition.directory} adapter boot missing phase ${expected.id}`)
    if (!phase) continue
    assert(errors, phase.order === expected.order, `${edition.directory} adapter boot phase ${expected.id} order mismatch`)
    assert(errors, phase.gate === expected.gate, `${edition.directory} adapter boot phase ${expected.id} gate mismatch`)
  }
  assert(errors, sameStringList((report.loadStepSummaries ?? []).map((step) => step.id), expectedLoadSteps.map((step) => step.id)), `${edition.directory} adapter boot load step ids mismatch`)
  for (const expected of expectedLoadSteps) {
    const step = (report.loadStepSummaries ?? []).find((entry) => entry.id === expected.id)
    assert(errors, step !== undefined, `${edition.directory} adapter boot missing load step ${expected.id}`)
    if (!step) continue
    assert(errors, step.phase === expected.phase, `${edition.directory} adapter boot load step ${expected.id} phase mismatch`)
    assert(errors, step.successSignal === expected.successSignal, `${edition.directory} adapter boot load step ${expected.id} success signal mismatch`)
    assert(errors, step.resources === expected.resources && step.resources > 0, `${edition.directory} adapter boot load step ${expected.id} resource count mismatch`)
    assert(errors, step.requiredEvidence === expected.requiredEvidence && step.requiredEvidence > 0, `${edition.directory} adapter boot load step ${expected.id} evidence count mismatch`)
  }
  for (const step of runtimePlan.loadSteps ?? []) {
    assert(errors, requiredPhases.includes(step.phase), `${edition.directory} adapter boot load step ${step.id} references unknown phase`)
    assert(errors, step.runtimeTargets?.includes(edition.runtimeTarget), `${edition.directory} adapter boot load step ${step.id} missing runtime target ${edition.runtimeTarget}`)
    for (const evidence of step.requiredEvidence ?? []) {
      assert(errors, runtimeEvidenceSet.has(evidence), `${edition.directory} adapter boot load step ${step.id} references unknown evidence ${evidence}`)
    }
  }
  assert(errors, sameStringList(report.runtimeEvidenceIds ?? [], runtimeEvidenceIds), `${edition.directory} adapter boot runtime evidence ids mismatch`)
  assert(errors, (report.runtimeEvidenceIds ?? []).length >= 40, `${edition.directory} adapter boot report must cover at least 40 runtime evidence ids`)
  assert(errors, sameStringList(report.requiredPublicAlphaEvidence ?? [], publicAlphaEvidence), `${edition.directory} adapter boot public alpha evidence mismatch`)
  assert(errors, report.adapterReadySignal === 'openlands_runtime_ready', `${edition.directory} adapter boot ready signal mismatch`)
  assert(errors, report.blockedBy?.includes('real_runtime_adapter_boot_missing'), `${edition.directory} adapter boot report must name missing real adapter boot blocker`)
  for (const proof of REQUIRED_ADAPTER_BOOT_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} adapter boot report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(stableAdapterBootReport(report), stableAdapterBootReport(expectedFreshReport)), `${edition.directory} adapter boot report stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} adapter boot report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} adapter boot report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateRegistryParityReport(errors, report, edition, registryParityContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expected = buildExpectedRegistryParity({ ...registryParityContracts, edition })
  assert(errors, report.schema === 'echo.openlands.edition.registry_parity_report.v1', `${edition.directory} registry parity report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} registry parity report status must be preflight_passed`)
  assert(errors, report.realRegistryParityExecutionRequiredBeforePublicAlpha === true, `${edition.directory} registry parity report must require real registry parity execution before public alpha`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} registry parity report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} registry parity report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} registry parity report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} registry parity report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} registry parity report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} registry parity report moduleVersion mismatch`)
  assert(errors, report.conformanceFixture === CONFORMANCE_REGISTRY, `${edition.directory} registry parity conformance fixture mismatch`)
  assert(errors, report.parityContract === CROSS_PLATFORM_PARITY_CONTRACT, `${edition.directory} registry parity contract mismatch`)
  assert(errors, report.artifact?.file === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} registry parity artifact filename mismatch`)
  assert(errors, report.artifact?.kind === edition.moduleArtifactFamily, `${edition.directory} registry parity artifact kind mismatch`)
  assert(errors, sameStringList(report.artifact?.runtimeEntriesChecked ?? [], expected.runtimeEntriesChecked), `${edition.directory} registry parity runtime entries checked mismatch`)
  assert(errors, sameJson(report.registryCounts, expected.registryCounts), `${edition.directory} registry parity counts mismatch`)
  assert(errors, sameJson(report.nonShippingRecipeRefs ?? [], expected.nonShippingRecipeRefs), `${edition.directory} registry parity non-shipping recipe refs mismatch`)
  assert(errors, sameJson(report.nonRegistryBiomeResources ?? [], expected.nonRegistryBiomeResources), `${edition.directory} registry parity non-registry biome resources mismatch`)
  assert(errors, sameJson(report.paritySurfaces ?? [], expected.paritySurfaces), `${edition.directory} registry parity surfaces mismatch`)
  assert(errors, sameStringList(report.runtimeResponsibilities ?? [], expected.runtimeResponsibilities), `${edition.directory} registry parity runtime responsibilities mismatch`)
  assert(errors, sameStringList(report.saveLoadFields ?? [], expected.saveLoadFields), `${edition.directory} registry parity save/load fields mismatch`)
  assert(errors, sameJson(report.standardModeRules, expected.standardModeRules), `${edition.directory} registry parity standard mode rules mismatch`)
  assert(errors, sameStringSet(expected.blockIds, registryParityContracts.conformance.blockRegistry ?? []), `${edition.directory} registry parity block ids must match conformance`)
  assert(errors, sameStringSet(expected.itemIds, registryParityContracts.conformance.itemRegistry ?? []), `${edition.directory} registry parity item ids must match conformance`)
  assert(errors, sameStringSet(expected.recipeIds, registryParityContracts.conformance.recipeRegistry ?? []), `${edition.directory} registry parity recipe ids must match conformance`)
  assert(errors, sameStringSet(expected.biomeIds, registryParityContracts.conformance.biomeRegistry ?? []), `${edition.directory} registry parity biome ids must match conformance`)
  assert(errors, report.standardModeRules?.hunger === 'gentle', `${edition.directory} registry parity report must keep gentle hunger`)
  for (const flag of ['stamina', 'hydration', 'foodSpoilage', 'temperatureDamage']) {
    assert(errors, report.standardModeRules?.[flag] === false, `${edition.directory} registry parity report must keep ${flag} off`)
  }
  assert(errors, report.blockedBy?.includes('real_runtime_registry_parity_test_missing'), `${edition.directory} registry parity report must name missing real runtime parity blocker`)
  for (const proof of REQUIRED_REGISTRY_PARITY_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} registry parity report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(stableRegistryParityReport(report), stableRegistryParityReport(expectedFreshReport)), `${edition.directory} registry parity report stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} registry parity report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} registry parity report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateCraftingStationReport(errors, report, edition, craftingContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expected = buildExpectedCraftingStation(craftingContracts)
  assert(errors, report.schema === 'echo.openlands.edition.crafting_station_report.v1', `${edition.directory} crafting station report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} crafting station report status must be preflight_passed`)
  assert(errors, report.realRuntimeStationExecutionRequiredBeforePublicAlpha === true, `${edition.directory} crafting station report must require real station execution before public alpha`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} crafting station report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} crafting station report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} crafting station report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} crafting station report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} crafting station report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} crafting station report moduleVersion mismatch`)
  assert(errors, report.recipeContract === RECIPES_CONTRACT, `${edition.directory} crafting station recipe contract mismatch`)
  assert(errors, report.artifact?.file === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} crafting station artifact filename mismatch`)
  assert(errors, report.artifact?.kind === edition.moduleArtifactFamily, `${edition.directory} crafting station artifact kind mismatch`)
  assert(errors, sameStringList(report.artifact?.runtimeEntriesChecked ?? [], expected.runtimeEntriesChecked), `${edition.directory} crafting station runtime entries checked mismatch`)
  assert(errors, report.recipeCount === expected.recipeCount, `${edition.directory} crafting station recipe count mismatch`)
  assert(errors, sameJson(report.stationSummaries ?? [], expected.stationSummaries), `${edition.directory} crafting station summaries mismatch`)
  assert(errors, sameJson(report.expectedStationCounts ?? {}, expected.expectedStationCounts), `${edition.directory} crafting station expected station counts mismatch`)
  assert(errors, sameStringList(report.processStations ?? [], expected.processStations), `${edition.directory} crafting station process stations mismatch`)
  assert(errors, sameStringList(report.requiredMapTableRecipes ?? [], expected.requiredMapTableRecipes), `${edition.directory} crafting station map-table recipes mismatch`)
  assert(errors, sameStringList(report.deferredStationBlocks ?? [], expected.deferredStationBlocks), `${edition.directory} crafting station deferred station blocks mismatch`)
  assert(errors, sameJson(report.nonInventoryUnlockRefs ?? [], expected.nonInventoryUnlockRefs), `${edition.directory} crafting station non-inventory unlock refs mismatch`)
  assert(errors, sameJson(report.stationSurfaceLoadStep, expected.stationSurfaceLoadStep), `${edition.directory} crafting station load step mismatch`)
  assert(errors, sameStringSet(expected.recipeIds, expected.conformanceRecipeIds), `${edition.directory} crafting station recipes must match conformance`)
  for (const station of craftingContracts.recipes.stations ?? []) {
    const stationId = normalizeId(station.id)
    assert(errors, expected.stationIds.includes(stationId), `${edition.directory} crafting station source missing station ${stationId}`)
    if (station.requiresBlock !== null) {
      assert(errors, expected.blockIds.has(normalizeId(station.requiresBlock)), `${edition.directory} crafting station ${stationId} requires unknown block ${station.requiresBlock}`)
    }
    if (station.grid) {
      assert(errors, String(station.grid).startsWith('freeform_'), `${edition.directory} crafting station ${stationId} must keep freeform grid identity`)
    }
  }
  for (const recipe of craftingContracts.recipes.recipes ?? []) {
    const recipeId = normalizeId(recipe.id)
    assert(errors, expected.stationIds.includes(normalizeId(recipe.station)), `${edition.directory} crafting recipe ${recipeId} references unknown station ${recipe.station}`)
    assert(errors, Number.isInteger(recipe.timeTicks) && recipe.timeTicks > 0, `${edition.directory} crafting recipe ${recipeId} must define positive timeTicks`)
    for (const ref of entryRefs(recipe.inputs)) {
      assert(errors, expected.itemIds.has(ref) || expected.blockIds.has(ref), `${edition.directory} crafting recipe ${recipeId} input references unknown id ${ref}`)
    }
    for (const ref of entryRefs(recipe.outputs)) {
      assert(errors, expected.itemIds.has(ref) || expected.blockIds.has(ref), `${edition.directory} crafting recipe ${recipeId} output references unknown id ${ref}`)
    }
  }
  assert(errors, report.blockedBy?.includes('real_runtime_station_execution_missing'), `${edition.directory} crafting station report must name missing real station execution blocker`)
  for (const proof of REQUIRED_CRAFTING_STATION_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} crafting station report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(stableCraftingStationReport(report), stableCraftingStationReport(expectedFreshReport)), `${edition.directory} crafting station report stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} crafting station report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} crafting station report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateWorldgenExplorationReport(errors, report, edition, worldgenContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expected = buildExpectedWorldgenExploration(worldgenContracts)
  assert(errors, report.schema === 'echo.openlands.edition.worldgen_exploration_report.v1', `${edition.directory} worldgen exploration report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} worldgen exploration report status must be preflight_passed`)
  assert(errors, report.realRuntimeWorldgenRequiredBeforePublicAlpha === true, `${edition.directory} worldgen exploration report must require real worldgen execution before public alpha`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} worldgen exploration report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} worldgen exploration report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} worldgen exploration report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} worldgen exploration report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} worldgen exploration report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} worldgen exploration report moduleVersion mismatch`)
  assert(errors, report.contracts?.biomes === BIOMES_CONTRACT, `${edition.directory} worldgen exploration biome contract mismatch`)
  assert(errors, report.contracts?.landmarks === STRUCTURES_CONTRACT, `${edition.directory} worldgen exploration landmark contract mismatch`)
  assert(errors, report.contracts?.creatures === CREATURES_CONTRACT, `${edition.directory} worldgen exploration creature contract mismatch`)
  assert(errors, report.contracts?.holomap === HOLOMAP_CONTRACT, `${edition.directory} worldgen exploration HoloMap contract mismatch`)
  assert(errors, report.artifact?.file === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} worldgen exploration artifact filename mismatch`)
  assert(errors, report.artifact?.kind === edition.moduleArtifactFamily, `${edition.directory} worldgen exploration artifact kind mismatch`)
  if (edition.moduleArtifactFamily === 'echo-addon') {
    assert(errors, report.artifact?.nestedRuntimeEntry === `${MODULE_ID}-${VERSION}-runtime.jar` || report.artifact?.nestedRuntimeEntry === `lib/${MODULE_ID}-${VERSION}-runtime.jar`, `${edition.directory} worldgen exploration report must record native nested runtime jar`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.directory} worldgen exploration report should not record nested runtime jar for ${edition.moduleArtifactFamily}`)
  }
  assert(errors, sameStringList(report.artifact?.runtimeEntriesChecked ?? [], expected.runtimeEntriesChecked), `${edition.directory} worldgen exploration runtime entries checked mismatch`)
  assert(errors, sameJson(report.counts, expected.counts), `${edition.directory} worldgen exploration counts mismatch`)
  assert(errors, sameJson(report.spawnSafety, expected.spawnSafety), `${edition.directory} worldgen exploration spawn safety mismatch`)
  assert(errors, sameJson(report.biomeSummaries ?? [], expected.biomeSummaries), `${edition.directory} worldgen exploration biome summaries mismatch`)
  assert(errors, sameStringList(report.landmarkIds ?? [], expected.landmarkIds), `${edition.directory} worldgen exploration landmark ids mismatch`)
  assert(errors, sameStringList(report.creatureIds ?? [], expected.creatureIds), `${edition.directory} worldgen exploration creature ids mismatch`)
  assert(errors, sameStringList(report.holomapLayers ?? [], expected.holomapLayers), `${edition.directory} worldgen exploration HoloMap layers mismatch`)
  assert(errors, sameStringList(report.holomapHintTypes ?? [], expected.holomapHintTypes), `${edition.directory} worldgen exploration HoloMap hint types mismatch`)
  assert(errors, sameJson(report.worldgenResourceMarkers ?? [], expected.worldgenResourceMarkers), `${edition.directory} worldgen exploration semantic resource markers mismatch`)
  assert(errors, sameJson(report.landmarkHoloMapHintMarkers ?? [], expected.landmarkHoloMapHintMarkers), `${edition.directory} worldgen exploration HoloMap hint markers mismatch`)
  assert(errors, sameJson(report.nonPromptTutorialHooks ?? [], expected.nonPromptTutorialHooks), `${edition.directory} worldgen exploration non-prompt tutorial hooks mismatch`)
  assert(errors, sameJson(report.worldgenLoadStep, expected.worldgenLoadStep), `${edition.directory} worldgen exploration load step mismatch`)
  assert(errors, sameStringSet(expected.biomeIds, worldgenContracts.conformance.biomeRegistry ?? []), `${edition.directory} worldgen exploration biome ids must match conformance`)
  assert(errors, sameStringSet(expected.creatureIds, worldgenContracts.conformance.creatureRegistry ?? []), `${edition.directory} worldgen exploration creature ids must match conformance`)
  for (const biome of worldgenContracts.biomes.biomes ?? []) {
    const biomeId = normalizeId(biome.id)
    for (const spawn of biome.spawnTable ?? []) {
      assert(errors, expected.creatureIdSet.has(normalizeId(spawn.creature)), `${edition.directory} worldgen biome ${biomeId} spawn references unknown creature ${spawn.creature}`)
      assert(errors, Number.isInteger(spawn.weight) && spawn.weight > 0, `${edition.directory} worldgen biome ${biomeId} spawn ${spawn.creature} must have positive weight`)
    }
    for (const [soundName, soundRef] of Object.entries(biome.ambience ?? {})) {
      if (soundName !== 'musicHint') {
        assert(errors, expected.soundKeys.has(soundKey(soundRef)), `${edition.directory} worldgen biome ${biomeId} ambience ${soundName} missing sound key ${soundRef}`)
      }
    }
    for (const landmark of Object.keys(biome.landmarkFrequency ?? {})) {
      assert(errors, expected.landmarkIdSet.has(landmarkIdFromFrequency(landmark)), `${edition.directory} worldgen biome ${biomeId} landmarkFrequency references unknown landmark ${landmark}`)
    }
  }
  for (const landmark of worldgenContracts.structures.landmarks ?? []) {
    const landmarkId = normalizeId(landmark.id)
    for (const block of landmark.blocks ?? []) {
      assert(errors, expected.blockIds.has(normalizeId(block)), `${edition.directory} worldgen landmark ${landmarkId} references unknown block ${block}`)
    }
    for (const biome of landmark.preferredBiomes ?? []) {
      assert(errors, expected.biomeIdSet.has(normalizeId(biome)), `${edition.directory} worldgen landmark ${landmarkId} references unknown preferred biome ${biome}`)
    }
    if (landmark.lootTable !== null) {
      assert(errors, expected.lootTableIds.has(normalizeId(landmark.lootTable)), `${edition.directory} worldgen landmark ${landmarkId} references unknown loot table ${landmark.lootTable}`)
    }
    for (const field of ['width', 'depth', 'height']) {
      assert(errors, Number.isInteger(landmark.footprint?.[field]) && landmark.footprint[field] > 0, `${edition.directory} worldgen landmark ${landmarkId} footprint ${field} must be positive`)
    }
  }
  for (const field of ['regionId', 'displayName', 'seedSalt', 'biomeType', 'discoveredAt', 'nearbyHints', 'restoredWaystones', 'playerMarkers', 'oldRoadSegments']) {
    assert(errors, worldgenContracts.holomap.regionDataContract?.storedFields?.includes(field), `${edition.directory} worldgen HoloMap region data missing field ${field}`)
  }
  assert(errors, worldgenContracts.holomap.regionDataContract?.fallbackIfHoloMapMissing?.includes('Echo Index'), `${edition.directory} worldgen HoloMap fallback must mention Echo Index`)
  for (const biomeId of expected.biomeIds) {
    assert(errors, (worldgenContracts.holomap.starterRegionNamePools?.[biomeId] ?? []).length >= 4, `${edition.directory} worldgen HoloMap starter name pool missing entries for ${biomeId}`)
  }
  for (const creature of worldgenContracts.creatures.creatures ?? []) {
    const creatureId = normalizeId(creature.id)
    for (const biome of creature.biomes ?? []) {
      assert(errors, expected.biomeIdSet.has(normalizeId(biome)), `${edition.directory} worldgen creature ${creatureId} references unknown biome ${biome}`)
    }
    for (const tag of creature.spawnRules?.surfaceTags ?? []) {
      assert(errors, expected.blockTagIds.has(tag), `${edition.directory} worldgen creature ${creatureId} references unknown surface tag ${tag}`)
    }
    assert(errors, Number.isInteger(creature.health) && creature.health > 0, `${edition.directory} worldgen creature ${creatureId} must have positive health`)
    assert(errors, Number.isInteger(creature.damage) && creature.damage >= 0, `${edition.directory} worldgen creature ${creatureId} must have non-negative damage`)
    assert(errors, (creature.ai ?? []).length >= 3, `${edition.directory} worldgen creature ${creatureId} must declare at least three AI hints`)
    for (const soundRef of Object.values(creature.sounds ?? {})) {
      assert(errors, expected.soundKeys.has(soundKey(soundRef)), `${edition.directory} worldgen creature ${creatureId} missing sound key ${soundRef}`)
    }
  }
  assert(errors, expected.itemTagIds.has('openlands:food'), `${edition.directory} worldgen report expected item tag openlands:food`)
  assert(errors, expected.worldgenStep?.resourceIds?.includes('biomes/mvp_biomes'), `${edition.directory} worldgen load step must include biomes`)
  assert(errors, expected.worldgenStep?.resourceIds?.includes('structures/mvp_landmarks'), `${edition.directory} worldgen load step must include structures`)
  assert(errors, expected.worldgenStep?.resourceIds?.includes('creatures/mvp_creatures'), `${edition.directory} worldgen load step must include creatures`)
  for (const evidence of ['biome_palettes_bound', 'spawn_tables_bound', 'landmark_pools_bound', 'starter_spawn_guarantees_bound']) {
    assert(errors, expected.worldgenStep?.requiredEvidence?.includes(evidence), `${edition.directory} worldgen load step missing evidence ${evidence}`)
  }
  const semanticMarkers = expected.worldgenResourceMarkers.map((marker) => marker.resource)
  for (const marker of ['cave_mouth', 'old_mine', 'reed_patch']) {
    assert(errors, semanticMarkers.includes(marker), `${edition.directory} worldgen exploration missing semantic marker ${marker}`)
  }
  assert(errors, report.blockedBy?.includes('real_runtime_worldgen_execution_missing'), `${edition.directory} worldgen exploration report must name missing real worldgen execution blocker`)
  for (const proof of REQUIRED_WORLDGEN_EXPLORATION_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} worldgen exploration report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(stableWorldgenExplorationReport(report), stableWorldgenExplorationReport(expectedFreshReport)), `${edition.directory} worldgen exploration report stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} worldgen exploration report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} worldgen exploration report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateCreatureRosterReport(errors, report, edition, creatureRosterContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expected = buildExpectedCreatureRoster(creatureRosterContracts)
  assert(errors, report.schema === 'echo.openlands.edition.creature_roster_report.v1', `${edition.directory} creature roster report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} creature roster report status must be preflight_passed`)
  assert(errors, report.realRuntimeCreatureExecutionRequiredBeforePublicAlpha === true, `${edition.directory} creature roster report must require real creature runtime execution before public alpha`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} creature roster report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} creature roster report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} creature roster report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} creature roster report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} creature roster report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} creature roster report moduleVersion mismatch`)
  assert(errors, report.contracts?.creatures === CREATURES_CONTRACT, `${edition.directory} creature roster creature contract mismatch`)
  assert(errors, report.contracts?.loot === LOOT_CONTRACT, `${edition.directory} creature roster loot contract mismatch`)
  assert(errors, report.contracts?.biomes === BIOMES_CONTRACT, `${edition.directory} creature roster biome contract mismatch`)
  assert(errors, report.contracts?.items === ITEMS_CONTRACT, `${edition.directory} creature roster item contract mismatch`)
  assert(errors, report.contracts?.tags === TAGS_CONTRACT, `${edition.directory} creature roster tag contract mismatch`)
  assert(errors, report.contracts?.conformance === CONFORMANCE_REGISTRY, `${edition.directory} creature roster conformance contract mismatch`)
  assert(errors, report.contracts?.runtimePlan === RUNTIME_EVIDENCE_CONTRACT, `${edition.directory} creature roster runtime plan contract mismatch`)
  assert(errors, report.contracts?.playtest === PLAYTEST_FIXTURE, `${edition.directory} creature roster playtest contract mismatch`)
  assert(errors, report.contracts?.sounds === SOUNDS_ASSET_CONTRACT, `${edition.directory} creature roster sounds contract mismatch`)
  assert(errors, report.artifact?.file === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} creature roster artifact filename mismatch`)
  assert(errors, report.artifact?.kind === edition.moduleArtifactFamily, `${edition.directory} creature roster artifact kind mismatch`)
  if (edition.moduleArtifactFamily === 'echo-addon') {
    assert(errors, report.artifact?.nestedRuntimeEntry === `${MODULE_ID}-${VERSION}-runtime.jar` || report.artifact?.nestedRuntimeEntry === `lib/${MODULE_ID}-${VERSION}-runtime.jar`, `${edition.directory} creature roster report must record native nested runtime jar`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.directory} creature roster report should not record nested runtime jar for ${edition.moduleArtifactFamily}`)
  }
  assert(errors, sameStringList(report.artifact?.runtimeEntriesChecked ?? [], expected.runtimeEntriesChecked), `${edition.directory} creature roster runtime entries checked mismatch`)
  assert(errors, sameJson(report.globalRules, expected.globalRules), `${edition.directory} creature roster global rules mismatch`)
  assert(errors, sameJson(report.counts, expected.counts), `${edition.directory} creature roster counts mismatch`)
  assert(errors, sameStringList(report.creatureIds ?? [], expected.creatureIds), `${edition.directory} creature roster ids mismatch`)
  assert(errors, sameJson(report.categoryCounts ?? {}, expected.categoryCounts), `${edition.directory} creature roster category counts mismatch`)
  assert(errors, sameJson(report.creatureSummaries ?? [], expected.creatureSummaries), `${edition.directory} creature roster summaries mismatch`)
  assert(errors, sameJson(report.starterSafety, expected.starterSafety), `${edition.directory} creature roster starter safety mismatch`)
  assert(errors, sameJson(report.worldgenLoadStep, expected.worldgenLoadStep), `${edition.directory} creature roster load step mismatch`)
  assert(errors, sameStringSet(expected.creatureIds, expected.conformanceCreatureIds), `${edition.directory} creature roster ids must match conformance`)
  assert(errors, expected.globalRules?.defaultHostility === 'moderate', `${edition.directory} creature roster must keep moderate default hostility`)
  assert(errors, expected.globalRules?.avoidHardcorePressure === true, `${edition.directory} creature roster must avoid hardcore pressure`)
  assert(errors, expected.globalRules?.noCopiedSilhouettes === true, `${edition.directory} creature roster must forbid copied silhouettes`)
  assert(errors, expected.globalRules?.soundNamespace === MODULE_ID, `${edition.directory} creature roster sound namespace mismatch`)
  for (const biome of creatureRosterContracts.biomes.biomes ?? []) {
    for (const spawn of biome.spawnTable ?? []) {
      assert(errors, expected.creatureIdSet.has(normalizeId(spawn.creature)), `${edition.directory} creature roster biome ${biome.id} spawn references unknown creature ${spawn.creature}`)
      assert(errors, Number.isInteger(spawn.weight) && spawn.weight > 0, `${edition.directory} creature roster biome ${biome.id} spawn ${spawn.creature} must have positive weight`)
    }
  }
  for (const creature of creatureRosterContracts.creatures.creatures ?? []) {
    const creatureId = normalizeId(creature.id)
    const categoryId = creature.legacyCategory ?? normalizeId(creature.category)
    assert(errors, typeof creature.category === 'string' && creature.category.length > 0, `${edition.directory} creature roster ${creatureId} must declare category`)
    assert(errors, Array.isArray(creature.biomes) && creature.biomes.length > 0, `${edition.directory} creature roster ${creatureId} must declare at least one biome`)
    for (const biome of creature.biomes ?? []) {
      assert(errors, expected.biomeIds.has(normalizeId(biome)), `${edition.directory} creature roster ${creatureId} references unknown biome ${biome}`)
    }
    assert(errors, expected.biomeSpawnMap.has(creatureId), `${edition.directory} creature roster ${creatureId} must appear in at least one biome spawn table`)
    assert(errors, typeof creature.spawnRules?.time === 'string' && creature.spawnRules.time.length > 0, `${edition.directory} creature roster ${creatureId} must declare spawn time`)
    assert(errors, typeof creature.spawnRules?.group === 'string' && creature.spawnRules.group.length > 0, `${edition.directory} creature roster ${creatureId} must declare spawn group`)
    assert(errors, groupRangeMax(creature.spawnRules?.group) > 0, `${edition.directory} creature roster ${creatureId} spawn group must be parseable`)
    assert(errors, Number.isInteger(creature.health) && creature.health > 0, `${edition.directory} creature roster ${creatureId} must have positive health`)
    assert(errors, Number.isInteger(creature.damage) && creature.damage >= 0, `${edition.directory} creature roster ${creatureId} must have non-negative damage`)
    assert(errors, (creature.ai ?? []).length >= 3, `${edition.directory} creature roster ${creatureId} must declare at least three AI hints`)
    if (categoryId.startsWith('hostile')) {
      assert(errors, creature.damage > 0, `${edition.directory} hostile creature ${creatureId} must have damage`)
      assert(errors, minimumSpawnDistance(creature.spawnRules) >= 96, `${edition.directory} hostile creature ${creatureId} must not spawn near world spawn`)
    }
    if (categoryId.startsWith('passive') || categoryId === 'aquatic_passive') {
      assert(errors, creature.damage === 0, `${edition.directory} passive creature ${creatureId} must not deal damage`)
    }
    if (creatureId === 'boar') {
      assert(errors, categoryId === 'neutral', `${edition.directory} boar must remain neutral`)
      assert(errors, minimumSpawnDistance(creature.spawnRules) >= 48, `${edition.directory} boar must not spawn directly on the world spawn`)
    }
    for (const tag of creature.spawnRules?.surfaceTags ?? []) {
      assert(errors, expected.blockTagIds.has(tag), `${edition.directory} creature roster ${creatureId} references unknown surface tag ${tag}`)
    }
    if (creature.spawnRules?.fluid) {
      assert(errors, creature.spawnRules.fluid === 'water', `${edition.directory} creature roster ${creatureId} fluid spawn must be water`)
    }
    for (const [event, soundRef] of Object.entries(creature.sounds ?? {})) {
      assert(errors, expected.soundKeys.has(soundKey(soundRef)), `${edition.directory} creature roster ${creatureId} ${event} sound key missing ${soundRef}`)
    }
    const dropTable = expected.dropTables.get(creatureId)
    assert(errors, dropTable !== undefined, `${edition.directory} creature roster ${creatureId} missing creatureDrops table`)
    assert(errors, (dropTable?.drops ?? []).length > 0, `${edition.directory} creature roster ${creatureId} drops table must not be empty`)
    for (const drop of dropTable?.drops ?? []) {
      assert(errors, expected.itemIds.has(normalizeId(drop.item)), `${edition.directory} creature roster ${creatureId} drop references unknown item ${drop.item}`)
      assert(errors, drop.count !== undefined, `${edition.directory} creature roster ${creatureId} drop ${drop.item} missing count`)
      if (drop.chance !== undefined) {
        assert(errors, typeof drop.chance === 'number' && drop.chance > 0 && drop.chance <= 1, `${edition.directory} creature roster ${creatureId} drop ${drop.item} chance must be 0-1`)
      }
    }
  }
  assert(errors, sameStringSet([...expected.dropTables.keys()], expected.creatureIds), `${edition.directory} creature roster drop tables must match creature ids`)
  for (const creatureId of expected.starterSafety.safeSpawnAllowedCreatures ?? []) {
    const creature = expected.creatureSummaries.find((summary) => summary.id === creatureId)
    assert(errors, creature !== undefined, `${edition.directory} creature roster safe spawn creature ${creatureId} must exist`)
    assert(errors, !creature?.category?.startsWith('hostile'), `${edition.directory} creature roster safe spawn creature ${creatureId} must not be hostile`)
  }
  assert(errors, expected.starterSafety.hostilesMinimumDistanceBlocks >= 96, `${edition.directory} creature roster hostile starter distance too low`)
  assert(errors, expected.starterSafety.boarMinimumDistanceBlocks >= 48, `${edition.directory} creature roster boar starter distance too low`)
  assert(errors, expected.starterSafety.avoidHardcorePressure === true, `${edition.directory} creature roster starter safety must avoid hardcore pressure`)
  assert(errors, expected.worldgenStep?.resourceIds?.includes('creatures/mvp_creatures'), `${edition.directory} creature roster load step must include creatures`)
  assert(errors, expected.worldgenStep?.requiredEvidence?.includes('spawn_tables_bound'), `${edition.directory} creature roster load step must require spawn_tables_bound`)
  assert(errors, expected.worldgenStep?.successSignal === 'openlands_worldgen_bound', `${edition.directory} creature roster load step success signal mismatch`)
  assert(errors, report.blockedBy?.includes('real_runtime_creature_spawn_execution_missing'), `${edition.directory} creature roster report must name missing real creature spawn execution blocker`)
  for (const proof of REQUIRED_CREATURE_ROSTER_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} creature roster report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(stableCreatureRosterReport(report), stableCreatureRosterReport(expectedFreshReport)), `${edition.directory} creature roster report stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} creature roster report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} creature roster report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateOldRoadNetworkReport(errors, report, edition, oldRoadNetworkContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expected = buildExpectedOldRoadNetwork(oldRoadNetworkContracts)
  assert(errors, report.schema === 'echo.openlands.edition.old_road_network_report.v1', `${edition.directory} old road network report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} old road network report status must be preflight_passed`)
  assert(errors, report.realRuntimeOldRoadNetworkRequiredBeforePublicAlpha === true, `${edition.directory} old road network report must require real old-road runtime execution before public alpha`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} old road network report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} old road network report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} old road network report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} old road network report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} old road network report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} old road network report moduleVersion mismatch`)
  assert(errors, report.contracts?.blocks === BLOCKS_CONTRACT, `${edition.directory} old road network block contract mismatch`)
  assert(errors, report.contracts?.items === ITEMS_CONTRACT, `${edition.directory} old road network item contract mismatch`)
  assert(errors, report.contracts?.recipes === RECIPES_CONTRACT, `${edition.directory} old road network recipe contract mismatch`)
  assert(errors, report.contracts?.landmarks === STRUCTURES_CONTRACT, `${edition.directory} old road network landmark contract mismatch`)
  assert(errors, report.contracts?.waystones === WAYSTONE_CONTRACT, `${edition.directory} old road network waystone contract mismatch`)
  assert(errors, report.contracts?.holomap === HOLOMAP_CONTRACT, `${edition.directory} old road network HoloMap contract mismatch`)
  assert(errors, report.contracts?.playtest === PLAYTEST_FIXTURE, `${edition.directory} old road network playtest contract mismatch`)
  assert(errors, report.contracts?.runtimePlan === RUNTIME_EVIDENCE_CONTRACT, `${edition.directory} old road network runtime plan contract mismatch`)
  assert(errors, report.artifact?.file === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} old road network artifact filename mismatch`)
  assert(errors, report.artifact?.kind === edition.moduleArtifactFamily, `${edition.directory} old road network artifact kind mismatch`)
  if (edition.moduleArtifactFamily === 'echo-addon') {
    assert(errors, report.artifact?.nestedRuntimeEntry === `${MODULE_ID}-${VERSION}-runtime.jar` || report.artifact?.nestedRuntimeEntry === `lib/${MODULE_ID}-${VERSION}-runtime.jar`, `${edition.directory} old road network report must record native nested runtime jar`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.directory} old road network report should not record nested runtime jar for ${edition.moduleArtifactFamily}`)
  }
  assert(errors, sameStringList(report.artifact?.runtimeEntriesChecked ?? [], expected.runtimeEntriesChecked), `${edition.directory} old road network runtime entries checked mismatch`)
  assert(errors, sameJson(report.counts, expected.counts), `${edition.directory} old road network counts mismatch`)
  assert(errors, sameStringList(report.oldRoadBlockIds ?? [], expected.oldRoadBlockIds), `${edition.directory} old road network block ids mismatch`)
  assert(errors, sameStringList(report.routeItemIds ?? [], expected.routeItemIds), `${edition.directory} old road network item ids mismatch`)
  assert(errors, sameStringList(report.routeRecipeIds ?? [], expected.routeRecipeIds), `${edition.directory} old road network recipe ids mismatch`)
  assert(errors, sameStringList(report.roadLandmarkIds ?? [], expected.roadLandmarkIds), `${edition.directory} old road network road landmark ids mismatch`)
  assert(errors, sameJson(report.routeRecipeSummaries ?? [], expected.routeRecipeSummaries), `${edition.directory} old road network route recipe summaries mismatch`)
  assert(errors, sameJson(report.holomapOldRoadContract, expected.holomapOldRoadContract), `${edition.directory} old road network HoloMap old-road contract mismatch`)
  assert(errors, sameJson(report.waystoneRouteContract, expected.waystoneRouteContract), `${edition.directory} old road network waystone route contract mismatch`)
  assert(errors, sameJson(report.playtestCoverage, expected.playtestCoverage), `${edition.directory} old road network playtest coverage mismatch`)
  assert(errors, sameJson(report.oldRoadNetworkLoadStep, expected.oldRoadNetworkLoadStep), `${edition.directory} old road network load step mismatch`)
  assert(errors, sameStringSet(expected.oldRoadBlockIds, oldRoadNetworkContracts.waystoneContract.blocks ?? []), `${edition.directory} old road network block ids must match waystone contract`)
  for (const id of expected.oldRoadBlockIds) {
    assert(errors, expected.blockIds.has(id), `${edition.directory} old road network source missing block ${id}`)
  }
  for (const id of expected.routeItemIds) {
    assert(errors, expected.itemIds.has(id), `${edition.directory} old road network source missing route item ${id}`)
  }
  for (const id of expected.routeRecipeIds) {
    assert(errors, expected.recipeIds.has(id), `${edition.directory} old road network source missing route recipe ${id}`)
  }
  for (const landmark of EXPECTED_REQUIRED_ROAD_LANDMARKS) {
    assert(errors, expected.roadLandmarkIds.includes(landmark), `${edition.directory} old road network missing road landmark ${landmark}`)
  }
  const oldRoadBlock = expected.blockMap.get('old_road_block')
  const oldRoadMarker = expected.blockMap.get('old_road_marker')
  const brokenWaystone = expected.blockMap.get('broken_waystone')
  const restoredWaystone = expected.blockMap.get('restored_waystone')
  const waystonePlinth = expected.blockMap.get('waystone_plinth')
  assert(errors, oldRoadBlock?.tags?.includes('openlands:old_roads'), `${edition.directory} old_road_block must carry openlands:old_roads`)
  assert(errors, oldRoadBlock?.structurePlacement?.includes('old_road_segments'), `${edition.directory} old_road_block must place in old_road_segments`)
  assert(errors, oldRoadMarker?.tags?.includes('openlands:map_hint'), `${edition.directory} old_road_marker must carry openlands:map_hint`)
  assert(errors, oldRoadMarker?.structurePlacement?.includes('roadside_markers'), `${edition.directory} old_road_marker must place in roadside_markers`)
  assert(errors, brokenWaystone?.tags?.includes('openlands:repairable'), `${edition.directory} broken_waystone must be repairable`)
  assert(errors, restoredWaystone?.tags?.includes('openlands:travel_node'), `${edition.directory} restored_waystone must be a travel node`)
  assert(errors, restoredWaystone?.effects?.includes('route_link'), `${edition.directory} restored_waystone must expose route_link effect`)
  assert(errors, waystonePlinth?.tags?.includes('openlands:route_building'), `${edition.directory} waystone_plinth must support route building`)
  const oldRoadToken = expected.itemMap.get('old_road_token')
  const routeBinding = expected.itemMap.get('route_binding')
  const regionRubbing = expected.itemMap.get('region_rubbing')
  const waystoneCore = expected.itemMap.get('waystone_core')
  assert(errors, oldRoadToken?.useType === 'route_record', `${edition.directory} old_road_token must be a route_record`)
  assert(errors, oldRoadToken?.tags?.includes('openlands:old_road'), `${edition.directory} old_road_token must carry openlands:old_road`)
  assert(errors, oldRoadToken?.tags?.includes('openlands:holomap'), `${edition.directory} old_road_token must carry openlands:holomap`)
  assert(errors, routeBinding?.tags?.includes('openlands:waystone_binding'), `${edition.directory} route_binding must carry openlands:waystone_binding`)
  assert(errors, regionRubbing?.tags?.includes('openlands:holomap'), `${edition.directory} region_rubbing must carry openlands:holomap`)
  assert(errors, waystoneCore?.tags?.includes('openlands:waystone_repair'), `${edition.directory} waystone_core must carry openlands:waystone_repair`)
  const regionRubbingRecipe = expected.recipeMap.get('region_rubbing')
  const oldRoadTokenRecipe = expected.recipeMap.get('old_road_token')
  const waystoneCoreRecipe = expected.recipeMap.get('waystone_core')
  const routeBindingRecipe = expected.recipeMap.get('route_binding')
  assert(errors, regionRubbingRecipe?.station === 'map_table', `${edition.directory} region_rubbing must be a map_table recipe`)
  assert(errors, inputContexts(regionRubbingRecipe).includes('discovered_region_marker'), `${edition.directory} region_rubbing must require discovered_region_marker context`)
  assert(errors, outputsItem(regionRubbingRecipe, 'region_rubbing'), `${edition.directory} region_rubbing recipe must output region_rubbing`)
  assert(errors, oldRoadTokenRecipe?.station === 'map_table', `${edition.directory} old_road_token must be a map_table recipe`)
  assert(errors, inputContexts(oldRoadTokenRecipe).includes('walked_old_road_segment'), `${edition.directory} old_road_token must require walked_old_road_segment context`)
  assert(errors, itemInputCount(oldRoadTokenRecipe, 'region_rubbing') === 1, `${edition.directory} old_road_token must consume one region_rubbing`)
  assert(errors, outputsItem(oldRoadTokenRecipe, 'old_road_token'), `${edition.directory} old_road_token recipe must output old_road_token`)
  assert(errors, waystoneCoreRecipe?.station === 'map_table', `${edition.directory} waystone_core must be a map_table recipe`)
  assert(errors, itemInputCount(waystoneCoreRecipe, 'glow_crystal') === 1, `${edition.directory} waystone_core must consume one glow_crystal`)
  assert(errors, itemInputCount(waystoneCoreRecipe, 'cupral_fitting') === 4, `${edition.directory} waystone_core must consume four cupral_fitting`)
  assert(errors, itemInputCount(waystoneCoreRecipe, 'region_rubbing') === 1, `${edition.directory} waystone_core must consume one region_rubbing`)
  assert(errors, outputsItem(waystoneCoreRecipe, 'waystone_core'), `${edition.directory} waystone_core recipe must output waystone_core`)
  assert(errors, routeBindingRecipe?.station === 'map_table', `${edition.directory} route_binding must be a map_table recipe`)
  assert(errors, inputContexts(routeBindingRecipe).includes('two_discovered_waystones'), `${edition.directory} route_binding must require two_discovered_waystones context`)
  assert(errors, itemInputCount(routeBindingRecipe, 'old_road_token') === 2, `${edition.directory} route_binding must consume two old road tokens`)
  assert(errors, itemInputCount(routeBindingRecipe, 'region_rubbing') === 1, `${edition.directory} route_binding must consume one region_rubbing`)
  assert(errors, outputsItem(routeBindingRecipe, 'route_binding'), `${edition.directory} route_binding recipe must output route_binding`)
  assert(errors, oldRoadNetworkContracts.holomapContract.regionDataContract?.storedFields?.includes('oldRoadSegments'), `${edition.directory} old road network HoloMap must persist oldRoadSegments`)
  assert(errors, expected.oldRoadLayer?.visibleByDefault === true, `${edition.directory} old_roads HoloMap layer must be visible by default`)
  assert(errors, expected.oldRoadLayer?.source === 'old_road_block_and_marker_discovery', `${edition.directory} old_roads layer source mismatch`)
  assert(errors, expected.waystoneLayer?.source === 'waystone_state_machine', `${edition.directory} waystone layer source mismatch`)
  assert(errors, expected.roadSegmentHint?.revealSources?.includes('walked_old_road'), `${edition.directory} road_segment hint must reveal from walked_old_road`)
  assert(errors, expected.roadSegmentHint?.revealSources?.includes('old_road_token'), `${edition.directory} road_segment hint must reveal from old_road_token`)
  assert(errors, expected.boundState?.inputs?.some((input) => input.item === 'route_binding' && input.count === 1), `${edition.directory} bound waystone state must consume one route_binding`)
  assert(errors, expected.boundState?.outputs?.includes('route_id'), `${edition.directory} bound waystone state must output route_id`)
  assert(errors, expected.activeState?.outputs?.includes('fast_travel_if_two_active'), `${edition.directory} active waystone state must output fast_travel_if_two_active`)
  assert(errors, oldRoadNetworkContracts.waystoneContract.effects?.fastTravel?.requiresActiveStones === 2, `${edition.directory} old road network fast-travel active stone count mismatch`)
  assert(errors, oldRoadNetworkContracts.waystoneContract.multiplayerState?.defaultPermissions?.travel === 'public_after_active', `${edition.directory} old road network travel permission mismatch`)
  for (const field of ['linkedRouteIds', 'isPublicTravel', 'canPublicRename', 'repairContributorIds']) {
    assert(errors, oldRoadNetworkContracts.waystoneContract.multiplayerState?.storedFields?.includes(field), `${edition.directory} old road network multiplayer field missing ${field}`)
  }
  assert(errors, expected.explorationScenario?.requires?.blocks?.includes('old_road_block'), `${edition.directory} first_exploration_hook must require old_road_block`)
  assert(errors, expected.explorationScenario?.requires?.blocks?.includes('old_road_marker'), `${edition.directory} first_exploration_hook must require old_road_marker`)
  assert(errors, expected.explorationScenario?.requires?.holomapLayers?.includes('old_roads'), `${edition.directory} first_exploration_hook must require old_roads layer`)
  assert(errors, expected.explorationScenario?.requires?.hintTypes?.includes('road_segment'), `${edition.directory} first_exploration_hook must require road_segment hint`)
  assert(errors, expected.playtestCoverage.explorationAssertions?.includes('old_road_marker_writes_old_road_segment'), `${edition.directory} first_exploration_hook must assert old-road segment writing`)
  assert(errors, expected.firstWaystoneScenario?.requires?.blocks?.includes('old_road_marker'), `${edition.directory} first_waystone must require old_road_marker`)
  assert(errors, expected.firstWaystoneScenario?.requires?.blocks?.includes('old_road_block'), `${edition.directory} first_waystone must require old_road_block`)
  assert(errors, expected.firstWaystoneCheckpoint?.requiredAssertions?.includes('old_road_segment_preserved'), `${edition.directory} waystone save/load checkpoint must preserve old road segment`)
  for (const item of expected.routeItemIds) {
    assert(errors, expected.publicAlphaRouteItems.has(item), `${edition.directory} waystone public alpha scenario missing route item ${item}`)
  }
  for (const recipe of expected.routeRecipeIds) {
    assert(errors, oldRoadNetworkContracts.playtestFixture.waystonePublicAlphaScenario?.requiresRecipes?.includes(recipe), `${edition.directory} waystone public alpha scenario missing route recipe ${recipe}`)
  }
  assert(errors, expected.loadStep?.resourceIds?.includes('waystones/waystone_contract'), `${edition.directory} old road network load step must include waystone contract`)
  assert(errors, expected.loadStep?.resourceIds?.includes('holomap/mvp_regions'), `${edition.directory} old road network load step must include HoloMap contract`)
  assert(errors, expected.loadStep?.resourceIds?.includes('systems/coop_and_smp'), `${edition.directory} old road network load step must include co-op contract`)
  for (const evidence of ['waystone_state_persistence_ready', 'holomap_region_persistence_ready', 'multiplayer_permissions_bound']) {
    assert(errors, expected.loadStep?.requiredEvidence?.includes(evidence), `${edition.directory} old road network load step missing evidence ${evidence}`)
  }
  assert(errors, report.blockedBy?.includes('real_runtime_old_road_generation_missing'), `${edition.directory} old road network report must name missing real old-road generation blocker`)
  for (const proof of REQUIRED_OLD_ROAD_NETWORK_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} old road network report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(stableOldRoadNetworkReport(report), stableOldRoadNetworkReport(expectedFreshReport)), `${edition.directory} old road network report stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} old road network report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} old road network report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateAlphaSystemsReport(errors, report, edition, alphaSystemsContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expected = buildExpectedAlphaSystems(alphaSystemsContracts)
  assert(errors, report.schema === 'echo.openlands.edition.alpha_systems_report.v1', `${edition.directory} alpha systems report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} alpha systems report status must be preflight_passed`)
  assert(errors, report.realRuntimeAlphaSystemsRequiredBeforePublicAlpha === true, `${edition.directory} alpha systems report must require real alpha systems execution before public alpha`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} alpha systems report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} alpha systems report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} alpha systems report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} alpha systems report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} alpha systems report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} alpha systems report moduleVersion mismatch`)
  assert(errors, report.contracts?.homestead === HOMESTEAD_ALPHA_CONTRACT, `${edition.directory} alpha systems homestead contract mismatch`)
  assert(errors, report.contracts?.builderUx === BUILDER_UX_ALPHA_CONTRACT, `${edition.directory} alpha systems builder UX contract mismatch`)
  assert(errors, report.contracts?.coopSmp === COOP_SMP_CONTRACT, `${edition.directory} alpha systems co-op contract mismatch`)
  assert(errors, report.contracts?.distribution === DISTRIBUTION_ALPHA_GATES_CONTRACT, `${edition.directory} alpha systems distribution contract mismatch`)
  assert(errors, report.artifact?.file === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} alpha systems artifact filename mismatch`)
  assert(errors, report.artifact?.kind === edition.moduleArtifactFamily, `${edition.directory} alpha systems artifact kind mismatch`)
  if (edition.moduleArtifactFamily === 'echo-addon') {
    assert(errors, report.artifact?.nestedRuntimeEntry === `${MODULE_ID}-${VERSION}-runtime.jar` || report.artifact?.nestedRuntimeEntry === `lib/${MODULE_ID}-${VERSION}-runtime.jar`, `${edition.directory} alpha systems report must record native nested runtime jar`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.directory} alpha systems report should not record nested runtime jar for ${edition.moduleArtifactFamily}`)
  }
  assert(errors, sameStringList(report.artifact?.runtimeEntriesChecked ?? [], expected.runtimeEntriesChecked), `${edition.directory} alpha systems runtime entries checked mismatch`)
  assert(errors, sameJson(report.relaxedStandardGuarantees, expected.relaxedStandardGuarantees), `${edition.directory} alpha systems relaxed Standard guarantees mismatch`)
  assert(errors, sameJson(report.homesteadSummary, expected.homesteadSummary), `${edition.directory} alpha systems homestead summary mismatch`)
  assert(errors, sameJson(report.builderUxSummary, expected.builderUxSummary), `${edition.directory} alpha systems builder UX summary mismatch`)
  assert(errors, sameJson(report.coopSummary, expected.coopSummary), `${edition.directory} alpha systems co-op summary mismatch`)
  assert(errors, sameJson(report.publicAlphaMinimum, expected.publicAlphaMinimum), `${edition.directory} alpha systems Public Alpha minimum mismatch`)
  assert(errors, expected.relaxedStandardGuarantees.cropsDoNotDieInStandard === true, `${edition.directory} alpha systems must keep crops from dying in Standard`)
  assert(errors, expected.relaxedStandardGuarantees.wateringOptionalInStandard === true, `${edition.directory} alpha systems must keep watering optional in Standard`)
  assert(errors, expected.relaxedStandardGuarantees.compostOptionalInStandard === true, `${edition.directory} alpha systems must keep compost optional in Standard`)
  assert(errors, expected.relaxedStandardGuarantees.cookpotSpoilageDisabled === true, `${edition.directory} alpha systems must disable cookpot spoilage in Standard`)
  assert(errors, expected.relaxedStandardGuarantees.scaffoldNoFallingPhysics === true, `${edition.directory} alpha systems must keep scaffold non-falling in Standard`)
  for (const crop of alphaSystemsContracts.homestead.crops ?? []) {
    assert(errors, expected.itemIds.has(normalizeId(crop.seedItem)), `${edition.directory} alpha systems crop ${crop.id} seed item missing ${crop.seedItem}`)
    assert(errors, expected.itemIds.has(normalizeId(crop.harvestItem)), `${edition.directory} alpha systems crop ${crop.id} harvest item missing ${crop.harvestItem}`)
    for (const soil of crop.preferredSoils ?? []) {
      assert(errors, expected.blockIds.has(normalizeId(soil)), `${edition.directory} alpha systems crop ${crop.id} preferred soil missing ${soil}`)
    }
    assert(errors, Number.isInteger(crop.growthStages) && crop.growthStages > 0, `${edition.directory} alpha systems crop ${crop.id} must define positive growthStages`)
    assert(errors, Number.isInteger(crop.baseGrowthMinutes) && crop.baseGrowthMinutes >= 10, `${edition.directory} alpha systems crop ${crop.id} must define baseGrowthMinutes`)
    assert(errors, crop.standardFailure !== undefined && !/die|dead|wither/i.test(crop.standardFailure), `${edition.directory} alpha systems crop ${crop.id} Standard failure must not kill crops`)
  }
  assert(errors, alphaSystemsContracts.homestead.soilCare?.watering?.standardRequired === false, `${edition.directory} alpha systems Standard watering must stay optional`)
  assert(errors, alphaSystemsContracts.homestead.soilCare?.compost?.standardRequired === false, `${edition.directory} alpha systems Standard compost must stay optional`)
  assert(errors, expected.itemIds.has(normalizeId(alphaSystemsContracts.homestead.soilCare?.compost?.item)), `${edition.directory} alpha systems compost item missing from registry`)
  for (const input of alphaSystemsContracts.homestead.soilCare?.compost?.inputs ?? []) {
    assert(errors, expected.itemIds.has(normalizeId(input)), `${edition.directory} alpha systems compost input missing ${input}`)
  }
  for (const meal of alphaSystemsContracts.homestead.cookpotMeals ?? []) {
    assert(errors, expected.blockIds.has(normalizeId(meal.station)), `${edition.directory} alpha systems cookpot meal ${meal.id} station missing ${meal.station}`)
    assert(errors, expected.itemIds.has(normalizeId(meal.baseContainer)), `${edition.directory} alpha systems cookpot meal ${meal.id} base container missing ${meal.baseContainer}`)
    assert(errors, expected.itemTagIds.has(meal.validIngredientsTag), `${edition.directory} alpha systems cookpot meal ${meal.id} ingredient tag missing ${meal.validIngredientsTag}`)
    assert(errors, meal.standardResult?.spoilage === false, `${edition.directory} alpha systems cookpot meal ${meal.id} must disable Standard spoilage`)
    assert(errors, meal.saveFields?.includes('remainingCookTicks'), `${edition.directory} alpha systems cookpot meal ${meal.id} must persist remainingCookTicks`)
  }
  for (const pen of alphaSystemsContracts.homestead.animalPens ?? []) {
    assert(errors, expected.creatureIds.has(normalizeId(pen.creature)), `${edition.directory} alpha systems animal pen ${pen.id} creature missing ${pen.creature}`)
    for (const block of pen.comfortBlocks ?? []) {
      assert(errors, expected.blockIds.has(normalizeId(block)), `${edition.directory} alpha systems animal pen ${pen.id} comfort block missing ${block}`)
    }
    for (const item of pen.feedItems ?? []) {
      assert(errors, expected.itemIds.has(normalizeId(item)), `${edition.directory} alpha systems animal pen ${pen.id} feed item missing ${item}`)
    }
    assert(errors, Number.isInteger(pen.minimumFenceArea) && pen.minimumFenceArea >= 16, `${edition.directory} alpha systems animal pen ${pen.id} minimumFenceArea too small`)
    assert(errors, standardRuleAvoidsUpkeepDeath(pen.standardRule), `${edition.directory} alpha systems animal pen ${pen.id} Standard rule must avoid upkeep death`)
  }
  for (const pool of alphaSystemsContracts.homestead.traderSurplus?.demandPools ?? []) {
    for (const item of pool.acceptedItems ?? []) {
      assert(errors, expected.itemIds.has(normalizeId(item)), `${edition.directory} alpha systems trader demand pool ${pool.id} accepted item missing ${item}`)
    }
  }
  for (const field of ['traderId', 'regionId', 'demandPoolId', 'expiresAt', 'completedOfferIds']) {
    assert(errors, alphaSystemsContracts.homestead.traderSurplus?.storedFields?.includes(field), `${edition.directory} alpha systems trader stored fields missing ${field}`)
  }
  const hammer = (alphaSystemsContracts.builderUx.tools ?? []).find((tool) => normalizeId(tool.id) === 'field_hammer')
  assert(errors, hammer !== undefined, `${edition.directory} alpha systems missing field_hammer tool`)
  assert(errors, expected.itemIds.has(normalizeId(hammer?.item)), `${edition.directory} alpha systems field_hammer item missing from registry`)
  for (const block of hammer?.supportedBlocks ?? []) {
    assert(errors, expected.blockIds.has(normalizeId(block)), `${edition.directory} alpha systems field_hammer supported block missing ${block}`)
  }
  for (const validation of ['player_can_edit_block', 'target_block_in_supported_set', 'variant_exists_for_runtime', 'inventory_or_creative_has_required_item_if_converting']) {
    assert(errors, hammer?.serverValidation?.includes(validation), `${edition.directory} alpha systems field_hammer missing server validation ${validation}`)
  }
  assert(errors, expected.scaffold !== undefined, `${edition.directory} alpha systems missing scaffold_bundle temporary block`)
  assert(errors, expected.itemIds.has(normalizeId(expected.scaffold?.item)), `${edition.directory} alpha systems scaffold_bundle item missing from registry`)
  assert(errors, expected.scaffold?.fallRule === 'no_falling_physics_in_standard', `${edition.directory} alpha systems scaffold must avoid falling physics in Standard`)
  assert(errors, Number.isInteger(expected.scaffold?.maxChainPlacement) && expected.scaffold.maxChainPlacement >= 16, `${edition.directory} alpha systems scaffold maxChainPlacement must support roof/bridge work`)
  for (const command of alphaSystemsContracts.builderUx.inventoryCommands ?? []) {
    for (const block of command.eligibleContainers ?? []) {
      assert(errors, expected.blockIds.has(normalizeId(block)), `${edition.directory} alpha systems inventory command ${command.id} eligible container missing ${block}`)
    }
    for (const block of command.eligibleStations ?? []) {
      assert(errors, expected.blockIds.has(normalizeId(block)), `${edition.directory} alpha systems inventory command ${command.id} eligible station missing ${block}`)
    }
    if (command.id === 'named_chests') {
      assert(errors, command.limits?.maxCharacters === 32, `${edition.directory} alpha systems named_chests maxCharacters must be 32`)
      assert(errors, command.limits?.allowColorCodes === false, `${edition.directory} alpha systems named_chests must disable color codes`)
      assert(errors, command.limits?.filterControlCharacters === true, `${edition.directory} alpha systems named_chests must filter control characters`)
    }
    if (command.id === 'craft_from_nearby_storage') {
      for (const field of ['reservationId', 'recipeId', 'containerIds', 'reservedStacks', 'expiresAt']) {
        assert(errors, command.storedFields?.includes(field), `${edition.directory} alpha systems craft_from_nearby_storage missing stored field ${field}`)
      }
    }
    if (command.id === 'quick_stack' || command.id === 'quick_deposit') {
      assert(errors, command.multiplayerValidation?.includes('container_permission'), `${edition.directory} alpha systems ${command.id} must validate container permissions`)
      assert(errors, command.multiplayerValidation?.includes('server_authoritative_transfer'), `${edition.directory} alpha systems ${command.id} must be server authoritative`)
    }
  }
  for (const acceptance of ['Hammer actions never delete a block without server validation.', 'Craft-from-storage must not duplicate items when save/load happens mid-craft.', 'Sorting order must be deterministic across all runtime targets.']) {
    assert(errors, alphaSystemsContracts.builderUx.acceptance?.includes(acceptance), `${edition.directory} alpha systems builder UX missing acceptance ${acceptance}`)
  }
  const containersState = (alphaSystemsContracts.coopSmp.sharedState ?? []).find((state) => state.id === 'containers')
  const waystonesState = (alphaSystemsContracts.coopSmp.sharedState ?? []).find((state) => state.id === 'waystones')
  assert(errors, containersState?.storedFields?.includes('inventoryStacks'), `${edition.directory} alpha systems containers shared state must persist inventoryStacks`)
  assert(errors, waystonesState?.storedFields?.includes('repairContributorIds'), `${edition.directory} alpha systems waystones shared state must persist repairContributorIds`)
  assert(errors, alphaSystemsContracts.coopSmp.permissions?.defaults?.containerQuickStack === 'owner_or_group', `${edition.directory} alpha systems quick-stack permission mismatch`)
  assert(errors, alphaSystemsContracts.coopSmp.permissions?.defaults?.waystoneTravel === 'public_after_active', `${edition.directory} alpha systems waystone travel permission mismatch`)
  for (const event of alphaSystemsContracts.coopSmp.networkEvents ?? []) {
    assert(errors, Array.isArray(event.payloadFields) && event.payloadFields.length > 0, `${edition.directory} alpha systems network event ${event.id} must declare payload fields`)
  }
  assert(errors, expected.publicAlphaMinimum.biomes === expected.biomeCount, `${edition.directory} alpha systems public alpha biome minimum mismatch`)
  assert(errors, expected.blockIds.size >= expected.publicAlphaMinimum.blocks?.min, `${edition.directory} alpha systems public alpha block minimum too low`)
  assert(errors, expected.itemIds.size >= expected.publicAlphaMinimum.items?.min, `${edition.directory} alpha systems public alpha item minimum too low`)
  assert(errors, expected.creatureIds.size === expected.publicAlphaMinimum.creatures, `${edition.directory} alpha systems public alpha creature minimum mismatch`)
  assert(errors, expected.publicAlphaMinimum.coOp?.targetPlayers === alphaSystemsContracts.coopSmp.targetPlayers?.publicAlpha, `${edition.directory} alpha systems public alpha co-op target mismatch`)
  assert(errors, report.blockedBy?.includes('real_runtime_alpha_systems_execution_missing'), `${edition.directory} alpha systems report must name missing real runtime alpha systems execution blocker`)
  for (const proof of REQUIRED_ALPHA_SYSTEMS_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} alpha systems report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(stableAlphaSystemsReport(report), stableAlphaSystemsReport(expectedFreshReport)), `${edition.directory} alpha systems report stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} alpha systems report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} alpha systems report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateDistributionRoadmapReport(errors, report, edition, distributionRoadmapContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expected = buildExpectedDistributionRoadmap({ ...distributionRoadmapContracts, edition })
  assert(errors, report.schema === 'echo.openlands.edition.distribution_roadmap_report.v1', `${edition.directory} distribution roadmap report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} distribution roadmap report status must be preflight_passed`)
  assert(errors, report.publicAlphaReady === false, `${edition.directory} distribution roadmap report must not mark Public Alpha ready before real execution`)
  assert(errors, report.realDistributionExecutionRequiredBeforePublicAlpha === true, `${edition.directory} distribution roadmap report must require real distribution execution before Public Alpha`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} distribution roadmap report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} distribution roadmap report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} distribution roadmap report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} distribution roadmap report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} distribution roadmap report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} distribution roadmap report moduleVersion mismatch`)
  assert(errors, sameJson(report.contracts, expected.contracts), `${edition.directory} distribution roadmap contracts mismatch`)
  assert(errors, sameJson(report.releaseManifest, expected.releaseManifest), `${edition.directory} distribution roadmap release manifest summary mismatch`)
  assert(errors, distributionRoadmapContracts.releaseManifest?.packId === edition.packId, `${edition.directory} distribution roadmap source manifest packId mismatch`)
  assert(errors, distributionRoadmapContracts.releaseManifest?.runtimeTarget === edition.runtimeTarget, `${edition.directory} distribution roadmap source manifest runtimeTarget mismatch`)
  assert(errors, distributionRoadmapContracts.releaseManifest?.loader === edition.loader, `${edition.directory} distribution roadmap source manifest loader mismatch`)
  assert(errors, distributionRoadmapContracts.releaseManifest?.moduleArtifactFamily === edition.moduleArtifactFamily, `${edition.directory} distribution roadmap source manifest artifact family mismatch`)
  assert(errors, distributionRoadmapContracts.releaseManifest?.moduleArtifactPattern === edition.moduleArtifactPattern, `${edition.directory} distribution roadmap source manifest artifact pattern mismatch`)
  assert(errors, sameStringList(distributionRoadmapContracts.releaseManifest?.requiredPublicAlphaEvidence ?? [], expected.publicAlphaEvidence ?? []), `${edition.directory} distribution roadmap source public alpha evidence mismatch`)

  assert(errors, distributionRoadmapContracts.releaseIndex?.schemaVersion === 'echo.module.release.v1', `${edition.directory} distribution roadmap release index schema mismatch`)
  assert(errors, expected.releaseModule !== undefined, `${edition.directory} distribution roadmap release index missing ${MODULE_ID} ${VERSION}`)
  assert(errors, sameJson(report.releaseIndex, expected.releaseIndex), `${edition.directory} distribution roadmap release index summary mismatch`)
  assert(errors, expected.releaseIndex.currentAllowedState === 'warning', `${edition.directory} distribution roadmap release index must remain warning`)
  assert(errors, expected.releaseIndex.launcherCurrentIndexStateAllowed === 'warning', `${edition.directory} distribution roadmap launcher index state must remain warning`)
  assert(errors, expected.releaseIndex.uploadedArtifactUrlsPresent === false, `${edition.directory} distribution roadmap must record missing uploaded artifact URLs`)
  const sourceArtifactTargets = distributionRoadmapContracts.distribution?.artifactTargets ?? []
  const artifactSummaries = report.releaseIndex?.artifactSummaries ?? []
  assert(errors, sameStringList(artifactSummaries.map((artifact) => artifact.id), sourceArtifactTargets.map((artifact) => artifact.id)), `${edition.directory} distribution roadmap artifact target order mismatch`)
  assert(errors, sourceArtifactTargets.every((target) => target.requiredForPublicAlpha === true), `${edition.directory} distribution roadmap source artifact targets must all be required for Public Alpha`)
  for (const target of sourceArtifactTargets) {
    const summary = artifactSummaries.find((artifact) => artifact.id === target.id)
    const releaseArtifact = artifactByFile(expected.releaseModule, target.file)
    const artifactPath = path.join(distributionRoadmapContracts.releaseRoot, MODULE_ID, target.file)
    const artifactExists = fileExists(artifactPath)
    assert(errors, summary !== undefined, `${edition.directory} distribution roadmap missing artifact summary ${target.id}`)
    assert(errors, releaseArtifact !== undefined, `${edition.directory} distribution roadmap release index missing artifact ${target.file}`)
    assert(errors, artifactExists, `${edition.directory} distribution roadmap artifact file missing ${artifactPath}`)
    assert(errors, summary?.file === target.file, `${edition.directory} distribution roadmap artifact ${target.id} filename mismatch`)
    assert(errors, summary?.kind === releaseArtifact?.kind, `${edition.directory} distribution roadmap artifact ${target.id} kind mismatch`)
    assert(errors, summary?.downloadUrlPresent === false, `${edition.directory} distribution roadmap artifact ${target.id} must record missing download URL before upload`)
    assert(errors, summary?.requiredForPublicAlpha === target.requiredForPublicAlpha, `${edition.directory} distribution roadmap artifact ${target.id} public alpha requirement mismatch`)
    if (artifactExists && releaseArtifact) {
      const actualSize = fs.statSync(artifactPath).size
      const actualSha256 = sha256File(artifactPath)
      assert(errors, releaseArtifact.sha256 === actualSha256, `${edition.directory} distribution roadmap release index sha mismatch for ${target.file}`)
      assert(errors, releaseArtifact.size === actualSize, `${edition.directory} distribution roadmap release index size mismatch for ${target.file}`)
      assert(errors, summary?.sha256 === actualSha256, `${edition.directory} distribution roadmap artifact ${target.id} sha256 mismatch`)
      assert(errors, summary?.size === actualSize, `${edition.directory} distribution roadmap artifact ${target.id} size mismatch`)
      assert(errors, typeof releaseArtifact.sha256 === 'string' && releaseArtifact.sha256.length === 64, `${edition.directory} distribution roadmap artifact ${target.id} release sha256 must be recorded`)
      assert(errors, Number.isInteger(releaseArtifact.size) && releaseArtifact.size > 0, `${edition.directory} distribution roadmap artifact ${target.id} release size must be recorded`)
    }
    if (target.id !== 'sources') {
      const expectedRuntimeTarget = target.id === 'native' ? 'echo_native' : target.id === 'standalone' ? 'echo_runtime_standalone' : 'neoforge'
      assert(errors, summary?.buildMode === 'compiled-runtime', `${edition.directory} distribution roadmap artifact ${target.id} must be compiled-runtime`)
      assert(errors, normalizeRuntimeTarget(releaseArtifact?.runtimeTarget) === expectedRuntimeTarget, `${edition.directory} distribution roadmap artifact ${target.id} runtime target mismatch`)
    } else {
      assert(errors, summary?.buildMode === null, `${edition.directory} distribution roadmap sources artifact should not be a runtime artifact`)
    }
  }

  assert(errors, sameJson(report.editionMatrix, expected.editionMatrix), `${edition.directory} distribution roadmap edition matrix mismatch`)
  assert(errors, expected.launcherEntry !== undefined, `${edition.directory} distribution roadmap launcher matrix missing edition ${edition.key}`)
  assert(errors, expected.parityTarget !== undefined, `${edition.directory} distribution roadmap parity target missing runtime ${edition.runtimeTarget}`)
  assert(errors, expected.editionMatrix.artifactTarget !== undefined, `${edition.directory} distribution roadmap artifact target missing edition ${edition.key}`)
  assert(errors, expected.launcherEntry?.runtimeTarget === edition.runtimeTarget, `${edition.directory} distribution roadmap launcher runtime target mismatch`)
  assert(errors, expected.launcherEntry?.packId === edition.packId, `${edition.directory} distribution roadmap launcher pack id mismatch`)
  assert(errors, expected.launcherEntry?.editionRepo === edition.directory, `${edition.directory} distribution roadmap launcher edition repo mismatch`)
  assert(errors, expected.launcherEntry?.artifactFamily === edition.moduleArtifactFamily, `${edition.directory} distribution roadmap launcher artifact family mismatch`)
  assert(errors, expected.launcherEntry?.artifactPattern === expected.expectedArtifactFile, `${edition.directory} distribution roadmap launcher artifact pattern mismatch`)
  assert(errors, expected.launcherEntry?.launcherProfileKind === edition.loader, `${edition.directory} distribution roadmap launcher profile mismatch`)
  assert(errors, sameStringList(expected.launcherEntry?.requiredDescriptors ?? [], edition.requiredModuleDescriptors), `${edition.directory} distribution roadmap required descriptors mismatch`)
  assert(errors, expected.parityTarget?.editionRepo === edition.directory, `${edition.directory} distribution roadmap parity edition repo mismatch`)
  assert(errors, expected.parityTarget?.artifactFamily === edition.moduleArtifactFamily, `${edition.directory} distribution roadmap parity artifact family mismatch`)
  assert(errors, expected.parityTarget?.artifactPattern === expected.expectedArtifactFile, `${edition.directory} distribution roadmap parity artifact pattern mismatch`)
  assert(errors, expected.editionMatrix.artifactTarget?.file === expected.expectedArtifactFile, `${edition.directory} distribution roadmap artifact target file mismatch`)
  assert(errors, expected.editionMatrix.artifactTarget?.editionRepo === edition.directory, `${edition.directory} distribution roadmap artifact target edition repo mismatch`)
  assert(errors, expected.editionMatrix.artifactTarget?.requiredForPublicAlpha === true, `${edition.directory} distribution roadmap artifact target must be required for Public Alpha`)

  assert(errors, sameJson(report.publicAlphaMinimum, expected.publicAlphaMinimum), `${edition.directory} distribution roadmap Public Alpha minimum mismatch`)
  assert(errors, Object.values(expected.publicAlphaMinimum.mvpMinimumsMet ?? {}).every(Boolean), `${edition.directory} distribution roadmap MVP minimums must pass`)
  assert(errors, (expected.publicAlphaMinimum.conformanceCounts?.biomes ?? 0) >= (distributionRoadmapContracts.distribution?.publicAlphaMinimum?.biomes ?? 0), `${edition.directory} distribution roadmap conformance biome count below source minimum`)
  assert(errors, (expected.publicAlphaMinimum.conformanceCounts?.blocks ?? 0) >= (distributionRoadmapContracts.distribution?.publicAlphaMinimum?.blocks?.min ?? 0), `${edition.directory} distribution roadmap conformance block count below source minimum`)
  assert(errors, (expected.publicAlphaMinimum.conformanceCounts?.items ?? 0) >= (distributionRoadmapContracts.distribution?.publicAlphaMinimum?.items?.min ?? 0), `${edition.directory} distribution roadmap conformance item count below source minimum`)
  assert(errors, (expected.publicAlphaMinimum.conformanceCounts?.creatures ?? 0) >= (distributionRoadmapContracts.distribution?.publicAlphaMinimum?.creatures ?? 0), `${edition.directory} distribution roadmap conformance creature count below source minimum`)

  assert(errors, sameJson(report.roadmap, expected.roadmap), `${edition.directory} distribution roadmap launch roadmap mismatch`)
  assert(errors, typeof expected.roadmap.defaultRule === 'string' && expected.roadmap.defaultRule.includes('relaxed default'), `${edition.directory} distribution roadmap must preserve relaxed default`)
  assert(errors, sameStringList(expected.roadmap.phaseIds ?? [], (distributionRoadmapContracts.launchRoadmap?.phases ?? []).map((phase) => phase.id)), `${edition.directory} distribution roadmap source phase order mismatch`)
  assert(errors, Array.isArray(expected.roadmap.oneDotZeroLoops) && expected.roadmap.oneDotZeroLoops.length > 0, `${edition.directory} distribution roadmap 1.0 loops must be declared`)
  assert(errors, Array.isArray(expected.roadmap.nonNegotiableInvariants) && expected.roadmap.nonNegotiableInvariants.length > 0, `${edition.directory} distribution roadmap invariants must be declared`)

  assert(errors, sameJson(report.launcherFlows ?? [], expected.launcherFlows), `${edition.directory} distribution roadmap launcher flows mismatch`)
  for (const flow of report.launcherFlows ?? []) {
    assert(errors, Array.isArray(flow.appliesTo) && flow.appliesTo.includes(edition.runtimeTarget), `${edition.directory} distribution roadmap launcher flow ${flow.id} must apply to ${edition.runtimeTarget}`)
    assert(errors, Array.isArray(flow.mustVerify) && flow.mustVerify.length > 0, `${edition.directory} distribution roadmap launcher flow ${flow.id} must list verification checks`)
    assert(errors, typeof flow.evidenceAttachment === 'string' && flow.evidenceAttachment.endsWith('-report.json'), `${edition.directory} distribution roadmap launcher flow ${flow.id} evidence attachment mismatch`)
  }
  assert(errors, sameStringList((distributionRoadmapContracts.distribution?.launcherGates ?? []).map((gate) => gate.id), expected.launcherFlows.map((flow) => flow.id)), `${edition.directory} distribution roadmap launcher gate ids must match launcher flows`)
  for (const surface of ['registry_ids', 'first_hour_save_load', 'standard_mode_rules', 'waystone_state_machine']) {
    assert(errors, (distributionRoadmapContracts.crossPlatformParity?.paritySurfaces ?? []).some((entry) => entry.id === surface), `${edition.directory} distribution roadmap parity surface missing ${surface}`)
  }
  assert(errors, sameStringSet(distributionRoadmapContracts.distributionApproval?.runtimeParity ?? [], [edition.runtimeTarget, ...EDITIONS.filter((entry) => entry.runtimeTarget !== edition.runtimeTarget).map((entry) => entry.runtimeTarget)]), `${edition.directory} distribution roadmap distribution approval runtime parity mismatch`)
  for (const blocker of [
    'release_index_download_urls_missing',
    'real_launcher_install_update_repair_rollback_execution_missing',
    'native_standalone_neoforge_runtime_parity_execution_missing',
    'public_alpha_coop_session_test_missing',
    'public_alpha_release_index_approval_missing',
  ]) {
    assert(errors, report.blockedBy?.includes(blocker), `${edition.directory} distribution roadmap missing blocker ${blocker}`)
  }
  for (const proof of REQUIRED_DISTRIBUTION_ROADMAP_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} distribution roadmap report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(stableDistributionRoadmapReport(report), stableDistributionRoadmapReport(expectedFreshReport)), `${edition.directory} distribution roadmap report stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} distribution roadmap report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} distribution roadmap report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateLauncherFlowReport(errors, report, edition, launcherFlowContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expected = buildExpectedLauncherFlow({ ...launcherFlowContracts, edition })
  assert(errors, report.schema === 'echo.openlands.edition.launcher_flow_report.v1', `${edition.directory} launcher flow report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} launcher flow report status must be preflight_passed`)
  assert(errors, report.publicAlphaReady === false, `${edition.directory} launcher flow report must not mark public alpha ready before real launcher execution`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} launcher flow report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} launcher flow report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} launcher flow report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} launcher flow report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} launcher flow report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} launcher flow report moduleVersion mismatch`)
  assert(errors, expected.matrix !== undefined, `${edition.directory} launcher flow source matrix entry missing`)
  assert(errors, expected.artifactVerification !== undefined, `${edition.directory} launcher flow artifact verification entry missing`)
  assert(errors, expected.releaseModule !== undefined, `${edition.directory} launcher flow release module missing`)
  assert(errors, expected.releaseArtifact !== undefined, `${edition.directory} launcher flow release artifact missing`)
  assert(errors, expected.publicAlphaGate !== undefined, `${edition.directory} launcher flow public alpha gate missing`)
  assert(errors, expected.editionManifest !== null, `${edition.directory} launcher flow edition manifest missing`)
  assert(errors, expected.evidenceTemplate !== null, `${edition.directory} launcher flow evidence template missing`)
  assert(errors, report.releaseId === expected.releaseIndex?.releaseId, `${edition.directory} launcher flow release id mismatch`)
  assert(errors, sameResolvedPath(report.releaseManifest, expected.releaseIndexPath), `${edition.directory} launcher flow release index path mismatch`)
  assert(errors, report.launcherFlowContract === LAUNCHER_FLOW_CONTRACT, `${edition.directory} launcher flow contract path mismatch`)
  assert(errors, report.distributionContract === DISTRIBUTION_ALPHA_GATES_CONTRACT, `${edition.directory} launcher flow distribution contract path mismatch`)
  assert(errors, report.releaseIndexStateAllowed === expected.releaseIndexStateAllowed, `${edition.directory} launcher flow release index warning state mismatch`)
  assert(errors, sameStringList(report.requiredPublicAlphaEvidence ?? [], expected.publicAlphaGate?.requiresEvidence ?? []), `${edition.directory} launcher flow public alpha evidence mismatch`)
  assert(errors, sameStringList(report.requiredPublicAlphaEvidence ?? [], expected.editionManifest?.requiredPublicAlphaEvidence ?? []), `${edition.directory} launcher flow manifest public alpha evidence mismatch`)
  assert(errors, sameStringList(report.requiredPublicAlphaEvidence ?? [], expected.evidenceTemplate?.requiredPublicAlphaEvidence ?? []), `${edition.directory} launcher flow evidence template public alpha evidence mismatch`)
  assert(errors, expected.matrix?.packId === edition.packId, `${edition.directory} launcher flow source matrix packId mismatch`)
  assert(errors, expected.matrix?.runtimeTarget === edition.runtimeTarget, `${edition.directory} launcher flow source matrix runtimeTarget mismatch`)
  assert(errors, expected.matrix?.editionRepo === edition.directory, `${edition.directory} launcher flow source matrix edition repo mismatch`)
  assert(errors, expected.matrix?.artifactFamily === edition.moduleArtifactFamily, `${edition.directory} launcher flow source matrix artifact family mismatch`)
  assert(errors, expected.matrix?.artifactPattern === expected.expectedArtifactFile, `${edition.directory} launcher flow source matrix artifact pattern mismatch`)
  assert(errors, expected.matrix?.launcherProfileKind === edition.loader, `${edition.directory} launcher flow source matrix loader mismatch`)
  assert(errors, sameStringList(expected.matrix?.requiredDescriptors ?? [], edition.requiredModuleDescriptors), `${edition.directory} launcher flow source matrix descriptors mismatch`)
  assert(errors, expected.editionManifest?.packId === edition.packId, `${edition.directory} launcher flow source manifest packId mismatch`)
  assert(errors, expected.editionManifest?.runtimeTarget === edition.runtimeTarget, `${edition.directory} launcher flow source manifest runtimeTarget mismatch`)
  assert(errors, expected.editionManifest?.moduleArtifactFamily === edition.moduleArtifactFamily, `${edition.directory} launcher flow source manifest artifact family mismatch`)
  assert(errors, report.artifact?.file === expected.expectedArtifactFile, `${edition.directory} launcher flow artifact filename mismatch`)
  assert(errors, report.artifact?.kind === expected.artifact.kind, `${edition.directory} launcher flow artifact kind mismatch`)
  assert(errors, sameResolvedPath(report.artifact?.path, expected.artifactPath), `${edition.directory} launcher flow artifact path mismatch`)
  assert(errors, typeof report.artifact?.path === 'string' && fileExists(report.artifact.path), `${edition.directory} launcher flow artifact path must exist`)
  assert(errors, report.artifact?.buildMode === 'compiled-runtime', `${edition.directory} launcher flow artifact must be compiled-runtime`)
  assert(errors, report.artifact?.buildMode === expected.artifact.buildMode, `${edition.directory} launcher flow artifact build mode mismatch`)
  assert(errors, report.artifact?.downloadUrlPresent === expected.artifact.downloadUrlPresent, `${edition.directory} launcher flow artifact download URL flag mismatch`)
  assert(errors, report.artifact?.sha256 === expected.artifact.sha256, `${edition.directory} launcher flow artifact sha256 mismatch`)
  assert(errors, report.artifact?.size === expected.artifact.size, `${edition.directory} launcher flow artifact size mismatch`)
  assert(errors, expected.releaseArtifact?.kind === edition.moduleArtifactFamily, `${edition.directory} launcher flow release artifact kind mismatch`)
  assert(errors, normalizeRuntimeTarget(expected.releaseArtifact?.runtimeTarget) === edition.runtimeTarget, `${edition.directory} launcher flow release artifact runtime target mismatch`)
  assert(errors, expected.releaseArtifact?.sha256 === expected.artifact.sha256, `${edition.directory} launcher flow release artifact sha mismatch`)
  assert(errors, expected.releaseArtifact?.size === expected.artifact.size, `${edition.directory} launcher flow release artifact size mismatch`)
  assert(errors, typeof expected.artifact.sha256 === 'string' && expected.artifact.sha256.length === 64, `${edition.directory} launcher flow artifact sha256 must be recorded`)
  assert(errors, Number.isInteger(expected.artifact.size) && expected.artifact.size > 0, `${edition.directory} launcher flow artifact size must be recorded`)
  for (const descriptorEntry of expected.matrix?.requiredDescriptors ?? []) {
    assert(errors, report.artifact?.packageEntriesChecked?.includes(descriptorEntry), `${edition.directory} launcher flow missing package descriptor ${descriptorEntry}`)
  }
  if (edition.moduleArtifactFamily === 'echo-addon') {
    assert(errors, report.artifact?.packageEntriesChecked?.includes('echo-addon-package.json'), `${edition.directory} launcher flow missing echo-addon package entry`)
    assert(errors, typeof report.artifact?.nestedRuntimeEntry === 'string' && report.artifact.nestedRuntimeEntry.length > 0, `${edition.directory} launcher flow missing nested runtime entry`)
    assert(errors, report.artifact?.packageEntriesChecked?.includes(report.artifact?.nestedRuntimeEntry), `${edition.directory} launcher flow package entries must include nested runtime`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.directory} launcher flow non-addon nested runtime entry mismatch`)
  }
  assert(errors, sameStringList(report.artifact?.runtimeEntriesChecked ?? [], REQUIRED_LAUNCHER_FLOW_RUNTIME_ENTRIES), `${edition.directory} launcher flow runtime entries mismatch`)
  for (const requiredField of expected.artifactVerification?.mustHaveIndexFields ?? []) {
    if (requiredField === 'url') continue
    if (requiredField === 'moduleId') assert(errors, expected.releaseModule?.moduleId === MODULE_ID, `${edition.directory} launcher flow release index module id mismatch`)
    else if (requiredField === 'version') assert(errors, expected.releaseModule?.version === VERSION, `${edition.directory} launcher flow release index version mismatch`)
    else if (requiredField === 'runtimeTarget') assert(errors, normalizeRuntimeTarget(expected.releaseArtifact?.runtimeTarget) === edition.runtimeTarget, `${edition.directory} launcher flow release index runtime target mismatch`)
    else assert(errors, expected.releaseArtifact?.[requiredField] !== undefined && expected.releaseArtifact?.[requiredField] !== null && expected.releaseArtifact?.[requiredField] !== '', `${edition.directory} launcher flow release index missing field ${requiredField}`)
  }
  assert(errors, sameJson(report.flowResults ?? [], expected.flowResults), `${edition.directory} launcher flow results mismatch`)
  for (const flow of expected.flowResults) {
    const distributionFlow = expected.distributionGateMap.get(flow.id)
    assert(errors, distributionFlow !== undefined, `${edition.directory} launcher flow ${flow.id} missing distribution launcher gate`)
    assert(errors, sameStringList(flow.mustVerify ?? [], distributionFlow?.mustVerify ?? []), `${edition.directory} launcher flow ${flow.id} distribution checks mismatch`)
  }
  assert(errors, sameStringList(expected.evidenceTemplate?.launcherFlows ?? [], expected.flowResults.map((flow) => flow.id)), `${edition.directory} launcher flow evidence template flow ids mismatch`)
  assert(errors, sameJson(report.statePreservation, expected.statePreservation), `${edition.directory} launcher flow state preservation mismatch`)
  for (const proof of REQUIRED_LAUNCHER_FLOW_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} launcher flow report missing proof ${proof}`)
  }
  assert(errors, report.blockedBy?.includes('real_launcher_install_update_repair_rollback_execution_missing'), `${edition.directory} launcher flow report must name missing real launcher execution blocker`)
  if (!expected.releaseArtifact?.downloadUrl) {
    assert(errors, report.blockedBy?.includes('release_artifact_download_url_missing'), `${edition.directory} launcher flow missing release artifact URL blocker`)
  }
  assert(errors, Array.isArray(expected.statePreservation?.firstHourSaveFields) && expected.statePreservation.firstHourSaveFields.includes('waystoneState'), `${edition.directory} launcher flow source must preserve first-hour waystone state`)
  if (expectedFreshReport) {
    assert(errors, sameJson(stableLauncherFlowReport(report), stableLauncherFlowReport(expectedFreshReport)), `${edition.directory} launcher flow report stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} launcher flow report stale against generator body`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} launcher flow report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateLocalLauncherRehearsalReport(errors, report, edition, localLauncherRehearsalContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expected = buildExpectedLocalLauncherRehearsal({ ...localLauncherRehearsalContracts, edition })
  assert(errors, report.schema === 'echo.openlands.edition.local_launcher_rehearsal_report.v1', `${edition.directory} local launcher rehearsal schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} local launcher rehearsal status must be preflight_passed`)
  assert(errors, report.publicAlphaReady === false, `${edition.directory} local launcher rehearsal must not mark public alpha ready`)
  assert(errors, report.clearsLauncherGates === false, `${edition.directory} local launcher rehearsal must not clear real launcher gates`)
  assert(errors, report.rehearsalOnly === true, `${edition.directory} local launcher rehearsal must declare rehearsalOnly true`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} local launcher rehearsal dryRun mismatch`)
  assert(errors, report.edition === edition.key, `${edition.directory} local launcher rehearsal edition mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} local launcher rehearsal packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} local launcher rehearsal displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} local launcher rehearsal runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} local launcher rehearsal moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} local launcher rehearsal moduleVersion mismatch`)
  assert(errors, report.releaseId === localLauncherRehearsalContracts.releaseIndex?.releaseId, `${edition.directory} local launcher rehearsal release id mismatch`)
  assert(errors, sameResolvedPath(report.releaseIndexPath, expected.releaseIndexPath), `${edition.directory} local launcher rehearsal release index path mismatch`)
  assert(errors, report.launcherFlowContract === LAUNCHER_FLOW_CONTRACT, `${edition.directory} local launcher rehearsal launcher flow contract mismatch`)
  assert(errors, report.launcherExecutionContract === LAUNCHER_EXECUTION_ACCEPTANCE, `${edition.directory} local launcher rehearsal launcher execution contract mismatch`)
  assert(errors, expected.releaseModule !== undefined, `${edition.directory} local launcher rehearsal release module missing`)
  assert(errors, expected.releaseArtifact !== undefined, `${edition.directory} local launcher rehearsal release artifact missing`)
  assert(errors, typeof report.moduleArtifact === 'string' && path.basename(report.moduleArtifact) === expected.artifactFile, `${edition.directory} local launcher rehearsal artifact filename mismatch`)
  assert(errors, sameResolvedPath(report.moduleArtifact, expected.artifactPath), `${edition.directory} local launcher rehearsal artifact path mismatch`)
  assert(errors, fileExists(expected.artifactPath), `${edition.directory} local launcher rehearsal artifact file missing`)
  if (fileExists(expected.artifactPath)) {
    assert(errors, report.moduleArtifactSha256 === sha256File(expected.artifactPath), `${edition.directory} local launcher rehearsal artifact sha mismatch`)
    assert(errors, report.moduleArtifactSize === fs.statSync(expected.artifactPath).size, `${edition.directory} local launcher rehearsal artifact size mismatch`)
  }
  assert(errors, expected.releaseArtifact?.kind === edition.moduleArtifactFamily, `${edition.directory} local launcher rehearsal release artifact kind mismatch`)
  assert(errors, expected.releaseArtifact?.buildMode === 'compiled-runtime', `${edition.directory} local launcher rehearsal release artifact build mode mismatch`)
  assert(errors, normalizeRuntimeTarget(expected.releaseArtifact?.runtimeTarget) === edition.runtimeTarget, `${edition.directory} local launcher rehearsal release artifact runtime target mismatch`)
  assert(errors, expected.releaseArtifact?.sha256 === report.moduleArtifactSha256, `${edition.directory} local launcher rehearsal release artifact sha mismatch`)
  assert(errors, expected.releaseArtifact?.size === report.moduleArtifactSize, `${edition.directory} local launcher rehearsal release artifact size mismatch`)
  assert(errors, typeof report.moduleArtifactSha256 === 'string' && report.moduleArtifactSha256.length === 64, `${edition.directory} local launcher rehearsal artifact sha must be recorded`)
  assert(errors, Number.isInteger(report.moduleArtifactSize) && report.moduleArtifactSize > 0, `${edition.directory} local launcher rehearsal artifact size must be recorded`)
  assert(errors, sameStringList(expected.launcherFlowIds, expected.launcherExecutionFlowIds), `${edition.directory} local launcher rehearsal launcher contract flow ids mismatch`)
  assert(errors, sameStringList((report.flowResults ?? []).map((flow) => flow.id), expected.launcherExecutionFlowIds), `${edition.directory} local launcher rehearsal flow ids mismatch`)
  for (const flow of report.flowResults ?? []) {
    assert(errors, flow.status === 'passed', `${edition.directory} local launcher rehearsal flow ${flow.id} must pass`)
    assert(errors, Number.isInteger(flow.durationMs) && flow.durationMs >= 0, `${edition.directory} local launcher rehearsal flow ${flow.id} duration must be non-negative`)
    assert(errors, Array.isArray(flow.assertions) && flow.assertions.length > 0, `${edition.directory} local launcher rehearsal flow ${flow.id} must include assertions`)
    assert(errors, flow.assertions.every((assertion) => assertion.status === 'passed'), `${edition.directory} local launcher rehearsal flow ${flow.id} assertions must pass`)
    assert(errors, Array.isArray(flow.savedArtifacts) && flow.savedArtifacts.length > 0, `${edition.directory} local launcher rehearsal flow ${flow.id} must list saved artifacts`)
  }
  const byId = new Map((report.flowResults ?? []).map((flow) => [flow.id, flow]))
  assert(errors, byId.get('install')?.captures?.cachedArtifactSha256 === report.moduleArtifactSha256, `${edition.directory} local launcher install cache sha mismatch`)
  assert(errors, byId.get('install')?.captures?.cachedArtifactSize === report.moduleArtifactSize, `${edition.directory} local launcher install cache size mismatch`)
  assert(errors, byId.get('update')?.captures?.beforeUpdateWorldHash === byId.get('update')?.captures?.afterUpdateWorldHash, `${edition.directory} local launcher update must preserve world hash`)
  assert(errors, byId.get('update')?.captures?.beforeUpdateConfigHash === byId.get('update')?.captures?.afterUpdateConfigHash, `${edition.directory} local launcher update must preserve config hash`)
  assert(errors, byId.get('repair')?.captures?.corruptSha256 !== report.moduleArtifactSha256, `${edition.directory} local launcher repair corrupt sha must differ`)
  assert(errors, byId.get('repair')?.captures?.restoredSha256 === report.moduleArtifactSha256, `${edition.directory} local launcher repair restored sha mismatch`)
  assert(errors, byId.get('repair')?.captures?.worldHashBeforeRepair === byId.get('repair')?.captures?.worldHashAfterRepair, `${edition.directory} local launcher repair must preserve world hash`)
  assert(errors, byId.get('rollback')?.captures?.rollbackArtifactSha256 === report.moduleArtifactSha256, `${edition.directory} local launcher rollback artifact sha mismatch`)
  assert(errors, byId.get('rollback')?.captures?.schemaCompatibilityDecision === 'compatible_rehearsal_no_schema_change', `${edition.directory} local launcher rollback schema decision mismatch`)
  assert(errors, byId.get('rollback')?.captures?.worldHashBeforeRollback === byId.get('rollback')?.captures?.worldHashAfterRollback, `${edition.directory} local launcher rollback must preserve world hash`)
  assert(errors, byId.get('rollback')?.captures?.rollbackWorldDeletionCount === 0, `${edition.directory} local launcher rollback must not delete worlds`)
  assert(errors, byId.get('rollback')?.captures?.repairWorldDeletionCount === 0, `${edition.directory} local launcher repair must not delete worlds`)
  assert(errors, report.preservedState?.installWorldHash === byId.get('install')?.captures?.worldHash, `${edition.directory} local launcher preserved install world hash mismatch`)
  assert(errors, report.preservedState?.installConfigHash === byId.get('install')?.captures?.configHash, `${edition.directory} local launcher preserved install config hash mismatch`)
  assert(errors, report.preservedState?.beforeUpdateWorldHash === byId.get('update')?.captures?.beforeUpdateWorldHash, `${edition.directory} local launcher preserved update world hash mismatch`)
  assert(errors, report.preservedState?.afterUpdateWorldHash === byId.get('update')?.captures?.afterUpdateWorldHash, `${edition.directory} local launcher preserved update world hash mismatch`)
  assert(errors, report.preservedState?.worldHashBeforeRepair === byId.get('repair')?.captures?.worldHashBeforeRepair, `${edition.directory} local launcher preserved repair world hash mismatch`)
  assert(errors, report.preservedState?.worldHashAfterRepair === byId.get('repair')?.captures?.worldHashAfterRepair, `${edition.directory} local launcher preserved repair world hash mismatch`)
  assert(errors, report.preservedState?.worldHashBeforeRollback === byId.get('rollback')?.captures?.worldHashBeforeRollback, `${edition.directory} local launcher preserved rollback world hash mismatch`)
  assert(errors, report.preservedState?.worldHashAfterRollback === byId.get('rollback')?.captures?.worldHashAfterRollback, `${edition.directory} local launcher preserved rollback world hash mismatch`)
  for (const proof of [
    'local_release_index_loaded',
    'compiled_artifact_cached',
    'artifact_sha256_and_size_match_release_index',
    'artifact_descriptor_entries_present',
    'openlands_standard_config_preserved',
    'world_state_preserved_across_update_repair_rollback',
    'corrupt_artifact_repaired_from_release_root',
    'rollback_manifest_snapshot_written',
    'public_alpha_stays_blocked_until_real_launcher_execution',
  ]) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} local launcher rehearsal missing proof ${proof}`)
  }
  for (const blocker of [
    'real_echo_launcher_execution_missing',
    'public_release_download_urls_missing',
    'local_rehearsal_does_not_clear_launcher_gates',
  ]) {
    assert(errors, report.blockedBy?.includes(blocker), `${edition.directory} local launcher rehearsal missing blocker ${blocker}`)
  }
  if (!expectedDryRun) {
    assert(errors, typeof report.savedArtifactRoot === 'string' && fileExists(report.savedArtifactRoot), `${edition.directory} local launcher saved artifact root missing`)
    for (const flow of report.flowResults ?? []) {
      for (const savedArtifact of flow.savedArtifacts ?? []) {
        const savedArtifactPath = path.join(report.savedArtifactRoot ?? '', savedArtifact)
        assert(errors, fileExists(savedArtifactPath), `${edition.directory} local launcher saved artifact missing ${savedArtifact}`)
        if (fileExists(savedArtifactPath)) {
          assert(errors, fs.statSync(savedArtifactPath).size > 0, `${edition.directory} local launcher saved artifact empty ${savedArtifact}`)
        }
      }
    }
    const artifactHashPath = path.join(report.savedArtifactRoot ?? '', 'install', 'artifact-hash.txt')
    if (fileExists(artifactHashPath)) {
      assert(errors, readText(artifactHashPath).trim() === report.moduleArtifactSha256, `${edition.directory} local launcher saved artifact hash mismatch`)
    }
    const installManifest = readJsonIfPresent(path.join(report.savedArtifactRoot ?? '', 'install', 'install-manifest.json'))
    const updatedManifest = readJsonIfPresent(path.join(report.savedArtifactRoot ?? '', 'update', 'updated-manifest.json'))
    const previousManifest = readJsonIfPresent(path.join(report.savedArtifactRoot ?? '', 'update', 'previous-manifest.json'))
    const repairHashReport = readJsonIfPresent(path.join(report.savedArtifactRoot ?? '', 'repair', 'repair-hash-report.json'))
    const repairPreservationReport = readJsonIfPresent(path.join(report.savedArtifactRoot ?? '', 'repair', 'repair-preservation-report.json'))
    const rollbackManifest = readJsonIfPresent(path.join(report.savedArtifactRoot ?? '', 'rollback', 'rollback-manifest.json'))
    const rollbackWorldDiff = readJsonIfPresent(path.join(report.savedArtifactRoot ?? '', 'rollback', 'world-preservation-diff.json'))
    const rollbackConfigDiff = readJsonIfPresent(path.join(report.savedArtifactRoot ?? '', 'rollback', 'config-preservation-diff.json'))
    assert(errors, installManifest?.releaseId === report.releaseId, `${edition.directory} local launcher install manifest release id mismatch`)
    assert(errors, updatedManifest?.releaseId === report.releaseId, `${edition.directory} local launcher updated manifest release id mismatch`)
    assert(errors, previousManifest?.releaseId === `${report.releaseId}-previous-local-rehearsal`, `${edition.directory} local launcher previous manifest release id mismatch`)
    assert(errors, repairHashReport?.expectedSha256 === report.moduleArtifactSha256, `${edition.directory} local launcher repair expected sha mismatch`)
    assert(errors, repairHashReport?.restoredSha256 === report.moduleArtifactSha256, `${edition.directory} local launcher repair saved restored sha mismatch`)
    assert(errors, repairHashReport?.restored === true, `${edition.directory} local launcher repair saved restored flag mismatch`)
    assert(errors, repairPreservationReport?.preserved === true, `${edition.directory} local launcher repair preservation saved flag mismatch`)
    assert(errors, rollbackManifest?.releaseId === `${report.releaseId}-previous-local-rehearsal`, `${edition.directory} local launcher rollback manifest release id mismatch`)
    assert(errors, rollbackWorldDiff?.preserved === true, `${edition.directory} local launcher rollback world preservation saved flag mismatch`)
    assert(errors, rollbackWorldDiff?.rollbackWorldDeletionCount === 0, `${edition.directory} local launcher rollback saved deletion count mismatch`)
    assert(errors, rollbackConfigDiff?.preserved === true, `${edition.directory} local launcher rollback config preservation saved flag mismatch`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(stableLocalLauncherRehearsal(report), stableLocalLauncherRehearsal(expectedFreshReport)), `${edition.directory} local launcher rehearsal stale against dry-run`)
    assert(errors, sameJson(stableRehearsalGeneratorReport(report), stableRehearsalGeneratorReport(expectedFreshReport)), `${edition.directory} local launcher rehearsal report stale against dry-run`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} local launcher rehearsal outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateLocalRuntimeRehearsalReport(errors, report, edition, localRuntimeRehearsalContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const { runtimeExecution, releaseIndex, releaseRoot, moduleRoot } = localRuntimeRehearsalContracts
  const expectedArtifactFile = edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION)
  const expectedArtifactPath = path.join(releaseRoot, MODULE_ID, expectedArtifactFile)
  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const releaseArtifact = artifactByFile(releaseModule, expectedArtifactFile)
  const runtimeScenarioIds = (runtimeExecution.scenarios ?? []).map((scenario) => scenario.id)
  const runtimeScenarioById = byId(runtimeExecution.scenarios)
  const runtimeSuiteIds = (runtimeExecution.executionSuites ?? []).map((suite) => suite.id)
  assert(errors, report.schema === 'echo.openlands.edition.local_runtime_rehearsal_report.v1', `${edition.directory} local runtime rehearsal schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} local runtime rehearsal status must be preflight_passed`)
  assert(errors, report.publicAlphaReady === false, `${edition.directory} local runtime rehearsal must not mark public alpha ready`)
  assert(errors, report.clearsRuntimeGates === false, `${edition.directory} local runtime rehearsal must not clear real runtime gates`)
  assert(errors, report.rehearsalOnly === true, `${edition.directory} local runtime rehearsal must declare rehearsalOnly true`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} local runtime rehearsal dryRun mismatch`)
  assert(errors, report.edition === edition.key, `${edition.directory} local runtime rehearsal edition mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} local runtime rehearsal packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} local runtime rehearsal displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} local runtime rehearsal runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} local runtime rehearsal moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} local runtime rehearsal moduleVersion mismatch`)
  assert(errors, report.runtimeExecutionContract === RUNTIME_EXECUTION_ACCEPTANCE, `${edition.directory} local runtime rehearsal runtime execution contract mismatch`)
  assert(errors, report.playableRuntimeContract === PLAYABLE_RUNTIME_CONTRACT, `${edition.directory} local runtime rehearsal playable runtime contract mismatch`)
  assert(errors, releaseModule !== undefined, `${edition.directory} local runtime rehearsal release module missing`)
  assert(errors, releaseArtifact !== undefined, `${edition.directory} local runtime rehearsal release artifact missing`)
  assert(errors, typeof report.runtimeCoreReport === 'string' && fileExists(report.runtimeCoreReport), `${edition.directory} local runtime rehearsal runtime core report missing`)
  if (typeof report.runtimeCoreReport === 'string' && fileExists(report.runtimeCoreReport)) {
    const runtimeCoreReport = readJson(report.runtimeCoreReport)
    assert(errors, runtimeCoreReport.status === 'passed', `${edition.directory} local runtime rehearsal runtime core report must pass`)
    assert(errors, runtimeCoreReport.runtimeTarget === edition.runtimeTarget, `${edition.directory} local runtime rehearsal runtime core target mismatch`)
  }
  assert(errors, typeof report.localLauncherRehearsalReport === 'string' && fileExists(report.localLauncherRehearsalReport), `${edition.directory} local runtime rehearsal local launcher report missing`)
  if (typeof report.localLauncherRehearsalReport === 'string' && fileExists(report.localLauncherRehearsalReport)) {
    const localLauncherReport = readJson(report.localLauncherRehearsalReport)
    assert(errors, localLauncherReport.status === 'preflight_passed', `${edition.directory} local runtime rehearsal local launcher report must pass`)
    assert(errors, localLauncherReport.runtimeTarget === edition.runtimeTarget, `${edition.directory} local runtime rehearsal local launcher target mismatch`)
  }
  assert(errors, sameResolvedPath(report.moduleArtifact, expectedArtifactPath), `${edition.directory} local runtime rehearsal artifact path mismatch`)
  assert(errors, fileExists(expectedArtifactPath), `${edition.directory} local runtime rehearsal artifact file missing`)
  if (fileExists(expectedArtifactPath)) {
    assert(errors, report.moduleArtifactSha256 === sha256File(expectedArtifactPath), `${edition.directory} local runtime rehearsal artifact sha mismatch`)
    assert(errors, report.moduleArtifactSize === fs.statSync(expectedArtifactPath).size, `${edition.directory} local runtime rehearsal artifact size mismatch`)
  }
  assert(errors, releaseArtifact?.kind === edition.moduleArtifactFamily, `${edition.directory} local runtime rehearsal release artifact kind mismatch`)
  assert(errors, releaseArtifact?.buildMode === 'compiled-runtime', `${edition.directory} local runtime rehearsal release artifact build mode mismatch`)
  assert(errors, normalizeRuntimeTarget(releaseArtifact?.runtimeTarget) === edition.runtimeTarget, `${edition.directory} local runtime rehearsal release artifact runtime target mismatch`)
  assert(errors, releaseArtifact?.sha256 === report.moduleArtifactSha256, `${edition.directory} local runtime rehearsal release artifact sha mismatch`)
  assert(errors, releaseArtifact?.size === report.moduleArtifactSize, `${edition.directory} local runtime rehearsal release artifact size mismatch`)
  assert(errors, typeof report.moduleArtifactSha256 === 'string' && report.moduleArtifactSha256.length === 64, `${edition.directory} local runtime rehearsal artifact sha must be recorded`)
  assert(errors, Number.isInteger(report.moduleArtifactSize) && report.moduleArtifactSize > 0, `${edition.directory} local runtime rehearsal artifact size must be recorded`)
  assert(errors, report.scenarioCount === runtimeScenarioIds.length, `${edition.directory} local runtime rehearsal scenario count mismatch`)
  assert(errors, sameStringList((report.scenarioResults ?? []).map((scenario) => scenario.id), runtimeScenarioIds), `${edition.directory} local runtime rehearsal scenario ids mismatch`)
  for (const scenario of report.scenarioResults ?? []) {
    const contractScenario = runtimeScenarioById.get(scenario.id)
    assert(errors, contractScenario !== undefined, `${edition.directory} local runtime rehearsal scenario ${scenario.id} missing from contract`)
    assert(errors, runtimeSuiteIds.includes(scenario.suiteId), `${edition.directory} local runtime rehearsal scenario ${scenario.id} suite mismatch`)
    assert(errors, scenario.status === 'preflight_passed', `${edition.directory} local runtime rehearsal scenario ${scenario.id} must pass preflight`)
    assert(errors, scenario.runtimeTarget === edition.runtimeTarget, `${edition.directory} local runtime rehearsal scenario ${scenario.id} target mismatch`)
    assert(errors, scenario.artifactSha256 === report.moduleArtifactSha256, `${edition.directory} local runtime rehearsal scenario ${scenario.id} artifact sha mismatch`)
    assert(errors, scenario.realRuntimeExecutionRequiredBeforePublicAlpha === true, `${edition.directory} local runtime rehearsal scenario ${scenario.id} must require real runtime execution`)
    assert(errors, scenario.clearsRuntimeGates === false, `${edition.directory} local runtime rehearsal scenario ${scenario.id} must not clear gates`)
    assert(errors, sameStringList(scenario.inputFixtureRefs ?? [], contractScenario?.inputFixtureRefs ?? []), `${edition.directory} local runtime rehearsal scenario ${scenario.id} input fixture mismatch`)
    assert(errors, Array.isArray(scenario.fixtureResults) && scenario.fixtureResults.length === (contractScenario?.inputFixtureRefs ?? []).length, `${edition.directory} local runtime rehearsal scenario ${scenario.id} fixture count mismatch`)
    assert(errors, sameStringList((scenario.fixtureResults ?? []).map((fixture) => fixture.ref), scenario.inputFixtureRefs ?? []), `${edition.directory} local runtime rehearsal scenario ${scenario.id} fixture refs mismatch`)
    for (const fixture of scenario.fixtureResults ?? []) {
      const filePath = localRuntimeFixturePath(moduleRoot, fixture.ref)
      assert(errors, fixture.exists === true, `${edition.directory} local runtime rehearsal scenario ${scenario.id} fixture ${fixture.ref} must be present`)
      assert(errors, filePath !== null && fileExists(filePath), `${edition.directory} local runtime rehearsal scenario ${scenario.id} fixture ${fixture.ref} missing on disk`)
      assert(errors, typeof fixture.sha256 === 'string' && fixture.sha256.length === 64, `${edition.directory} local runtime rehearsal scenario ${scenario.id} fixture ${fixture.ref} sha must be recorded`)
      if (filePath !== null && fileExists(filePath)) {
        assert(errors, fs.statSync(filePath).size > 0, `${edition.directory} local runtime rehearsal scenario ${scenario.id} fixture ${fixture.ref} must be non-empty`)
        assert(errors, fixture.sha256 === sha256File(filePath), `${edition.directory} local runtime rehearsal scenario ${scenario.id} fixture ${fixture.ref} sha mismatch`)
      }
    }
    assert(errors, sameStringList(scenario.plannedActions ?? [], contractScenario?.actions ?? []), `${edition.directory} local runtime rehearsal scenario ${scenario.id} planned actions mismatch`)
    assert(errors, sameStringList(scenario.rehearsalActions ?? [], (contractScenario?.actions ?? []).map((action) => `mapped_${action}`)), `${edition.directory} local runtime rehearsal scenario ${scenario.id} rehearsal actions mismatch`)
    assert(errors, Array.isArray(scenario.assertions) && scenario.assertions.length > 0, `${edition.directory} local runtime rehearsal scenario ${scenario.id} must include assertions`)
    assert(errors, sameStringList((scenario.assertions ?? []).map((assertion) => assertion.id), contractScenario?.assertions ?? []), `${edition.directory} local runtime rehearsal scenario ${scenario.id} assertion ids mismatch`)
    assert(errors, scenario.assertions.every((assertion) => assertion.status === 'passed'), `${edition.directory} local runtime rehearsal scenario ${scenario.id} assertions must pass`)
    assert(errors, Array.isArray(scenario.savedArtifacts) && scenario.savedArtifacts.length >= 3, `${edition.directory} local runtime rehearsal scenario ${scenario.id} must list saved artifacts`)
  }
  const scenarioResultById = new Map((report.scenarioResults ?? []).map((scenario) => [scenario.id, scenario]))
  assert(errors, scenarioResultById.get('fresh_standard_world_starts')?.captures?.defaultMode === 'openlands_standard', `${edition.directory} local runtime rehearsal default mode mismatch`)
  assert(errors, scenarioResultById.get('fresh_standard_world_starts')?.captures?.hardcoreMetersOff === true, `${edition.directory} local runtime rehearsal must prove hardcore meters off`)
  assert(errors, scenarioResultById.get('starter_spawn_seed_sweep')?.captures?.sampleSeedCount >= 3, `${edition.directory} local runtime rehearsal must sweep starter samples`)
  assert(errors, scenarioResultById.get('minimal_shelter_sleep')?.captures?.minimumForSleepMilestone === 55, `${edition.directory} local runtime rehearsal shelter minimum mismatch`)
  assert(errors, scenarioResultById.get('first_hour_route_walkthrough')?.captures?.routeMatchesFixture === true, `${edition.directory} local runtime rehearsal route fixture mismatch`)
  assert(errors, (scenarioResultById.get('biome_landmark_seed_sweep')?.captures?.biomeIds ?? []).length === 4, `${edition.directory} local runtime rehearsal must cover four MVP biomes`)
  assert(errors, scenarioResultById.get('creature_spawn_ai_drop_sound_sweep')?.captures?.creatureCount === 10, `${edition.directory} local runtime rehearsal must cover ten creatures`)
  assert(errors, scenarioResultById.get('waystone_repair_state_roundtrip')?.captures?.activeStateProof === true, `${edition.directory} local runtime rehearsal must include active waystone proof`)
  assert(errors, scenarioResultById.get('two_active_waystones_fast_travel')?.captures?.fastTravelProof === true, `${edition.directory} local runtime rehearsal must include fast travel proof`)
  assert(errors, scenarioResultById.get('relaxed_homestead_growth_and_cookpot')?.captures?.standardCropPauseProof === true, `${edition.directory} local runtime rehearsal must include relaxed crop proof`)
  assert(errors, scenarioResultById.get('builder_hammer_scaffold_inventory_storage')?.captures?.serverAuthorityProof === true, `${edition.directory} local runtime rehearsal must include server authority proof`)
  assert(errors, scenarioResultById.get('artifact_upload_download_hash')?.captures?.publicDownloadUrlsPresent === false, `${edition.directory} local runtime rehearsal must keep public download URLs missing`)
  assert(errors, scenarioResultById.get('launcher_install_update_repair_rollback')?.captures?.realLauncherExecutionStillRequired === true, `${edition.directory} local runtime rehearsal must keep real launcher execution required`)
  assert(errors, scenarioResultById.get('final_owned_asset_review')?.captures?.humanReviewStillRequired === true, `${edition.directory} local runtime rehearsal must keep asset human review required`)
  assert(errors, scenarioResultById.get('final_sound_and_branding_review')?.captures?.humanReviewStillRequired === true, `${edition.directory} local runtime rehearsal must keep sound human review required`)
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
    assert(errors, report.proofs?.includes(proof), `${edition.directory} local runtime rehearsal missing proof ${proof}`)
  }
  for (const blocker of [
    'real_adapter_execution_missing',
    'real_launcher_execution_missing',
    'public_release_download_urls_missing',
    'final_asset_human_review_missing',
    'local_rehearsal_does_not_clear_runtime_gates',
  ]) {
    assert(errors, report.blockedBy?.includes(blocker), `${edition.directory} local runtime rehearsal missing blocker ${blocker}`)
  }
  if (!expectedDryRun) {
    assert(errors, typeof report.savedArtifactRoot === 'string' && fileExists(report.savedArtifactRoot), `${edition.directory} local runtime saved artifact root missing`)
    for (const scenario of report.scenarioResults ?? []) {
      for (const savedArtifact of scenario.savedArtifacts ?? []) {
        const savedArtifactPath = path.join(report.savedArtifactRoot ?? '', savedArtifact)
        assert(errors, fileExists(savedArtifactPath), `${edition.directory} local runtime saved artifact missing ${savedArtifact}`)
        if (fileExists(savedArtifactPath)) {
          assert(errors, fs.statSync(savedArtifactPath).size > 0, `${edition.directory} local runtime saved artifact empty ${savedArtifact}`)
          const savedJson = readJsonIfPresent(savedArtifactPath)
          assert(errors, savedJson !== null, `${edition.directory} local runtime saved artifact JSON invalid ${savedArtifact}`)
          if (savedArtifact.endsWith('/input-fixture-summary.json')) {
            assert(errors, sameJson(savedJson, scenario.fixtureResults), `${edition.directory} local runtime saved fixture summary mismatch ${scenario.id}`)
          } else if (savedArtifact.endsWith('/assertion-map.json')) {
            assert(errors, sameJson(savedJson, {
              assertions: runtimeScenarioById.get(scenario.id)?.assertions ?? [],
              status: 'preflight_passed',
              clearsRuntimeGates: false,
            }), `${edition.directory} local runtime saved assertion map mismatch ${scenario.id}`)
          } else if (savedArtifact.endsWith('/capture-summary.json')) {
            assert(errors, sameJson(savedJson, scenario.captures), `${edition.directory} local runtime saved capture summary mismatch ${scenario.id}`)
          }
        }
      }
    }
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(stableLocalRuntimeRehearsal(report), stableLocalRuntimeRehearsal(expectedFreshReport)), `${edition.directory} local runtime rehearsal stale against dry-run`)
    assert(errors, sameJson(stableRehearsalGeneratorReport(report), stableRehearsalGeneratorReport(expectedFreshReport)), `${edition.directory} local runtime rehearsal report stale against dry-run`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} local runtime rehearsal outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateLegalAuditReport(errors, report, edition, legalAuditContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const expected = buildExpectedLegalAudit({ ...legalAuditContracts, edition })
  assert(errors, report.schema === 'echo.openlands.edition.legal_content_audit_report.v1', `${edition.directory} legal audit report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} legal audit report status must be preflight_passed`)
  assert(errors, report.publicReleaseAllowed === false, `${edition.directory} legal audit report must block public release until final review`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} legal audit report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} legal audit report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} legal audit report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} legal audit report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} legal audit report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} legal audit report moduleVersion mismatch`)
  assert(errors, report.legalAuditContract === expected.contractPaths.legalAuditContract, `${edition.directory} legal audit contract path mismatch`)
  assert(errors, report.contentPolicy === expected.contractPaths.contentPolicy, `${edition.directory} legal audit content policy path mismatch`)
  assert(errors, report.assetManifest === expected.contractPaths.assetManifest, `${edition.directory} legal audit asset manifest path mismatch`)
  assert(errors, expected.contentPolicy.namespace === MODULE_ID, `${edition.directory} legal audit content policy namespace mismatch`)
  assert(errors, expected.legalAudit.policySource === 'config/content_policy.json', `${edition.directory} legal audit policy source mismatch`)
  assert(errors, expected.legalAudit.assetManifest === ASSET_MANIFEST_CONTRACT, `${edition.directory} legal audit asset manifest contract mismatch`)
  assert(errors, expected.assetManifest.status === expected.legalAudit.assetRules?.currentStatus, `${edition.directory} legal audit asset manifest status mismatch`)
  assert(errors, expected.assetManifest.publicReleaseAllowedWithPlaceholders === false, `${edition.directory} legal audit placeholder release policy mismatch`)
  assert(errors, expected.legalAudit.publicAlphaGate?.requiresHumanReview === true, `${edition.directory} legal audit human review gate mismatch`)
  assert(errors, expected.legalAudit.publicAlphaGate?.requiresNoForbiddenPublicTerms === true, `${edition.directory} legal audit forbidden term gate mismatch`)
  assert(errors, expected.legalAudit.publicAlphaGate?.requiresNoBorrowedAssets === true, `${edition.directory} legal audit borrowed asset gate mismatch`)
  assert(errors, expected.legalAudit.publicAlphaGate?.requiresGeneratedOutputAudit?.includes(edition.runtimeTarget), `${edition.directory} legal audit generated output target missing`)
  assert(errors, report.artifact?.file === expected.artifactFile, `${edition.directory} legal audit artifact filename mismatch`)
  assert(errors, report.artifact?.kind === edition.moduleArtifactFamily, `${edition.directory} legal audit artifact kind mismatch`)
  assert(errors, typeof report.artifact?.path === 'string' && fileExists(report.artifact.path), `${edition.directory} legal audit artifact path must exist`)
  assert(errors, sameResolvedPath(report.artifact?.path, path.join(legalAuditContracts.releaseRoot, MODULE_ID, expected.artifactFile)), `${edition.directory} legal audit artifact path mismatch`)
  assert(errors, sameStringList(report.artifact?.runtimeEntriesChecked ?? [], REQUIRED_LEGAL_AUDIT_RUNTIME_ENTRIES), `${edition.directory} legal audit runtime entries mismatch`)
  if (edition.moduleArtifactFamily === 'echo-addon') {
    assert(errors, typeof report.artifact?.nestedRuntimeEntry === 'string' && report.artifact.nestedRuntimeEntry.length > 0, `${edition.directory} legal audit nested runtime entry missing`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.directory} legal audit nested runtime entry mismatch`)
  }
  assert(errors, Number.isInteger(report.scanSummary?.publicIdentityValues) && report.scanSummary.publicIdentityValues > 0, `${edition.directory} legal audit report must scan public identity values`)
  assert(errors, report.scanSummary?.assetPaths === expected.scanSummary.assetPaths, `${edition.directory} legal audit asset path count mismatch`)
  assert(errors, report.scanSummary?.forbiddenPublicTerms === expected.scanSummary.forbiddenPublicTerms, `${edition.directory} legal audit forbidden public term count mismatch`)
  assert(errors, report.scanSummary?.blockAssetsChecked === expected.scanSummary.blockAssetsChecked, `${edition.directory} legal audit block asset count mismatch`)
  assert(errors, report.scanSummary?.itemAssetsChecked === expected.scanSummary.itemAssetsChecked, `${edition.directory} legal audit item asset count mismatch`)
  assert(errors, report.scanSummary?.recipesChecked === expected.scanSummary.recipesChecked, `${edition.directory} legal audit recipe count mismatch`)
  assert(errors, report.scanSummary?.descriptorPublicFieldsChecked === expected.scanSummary.descriptorPublicFieldsChecked, `${edition.directory} legal audit descriptor public field count mismatch`)
  assert(errors, sameJson(report.policyResults, expected.policyResults), `${edition.directory} legal audit policy results mismatch`)
  for (const blocker of [
    'final_asset_human_review_missing',
    'placeholder_assets_block_public_release',
    'generated_output_human_review_missing',
  ]) {
    assert(errors, report.blockedBy?.includes(blocker), `${edition.directory} legal audit report missing blocker ${blocker}`)
  }
  if (edition.key === 'neoforge') {
    assert(errors, (report.adapterMetadataExceptions ?? []).some((entry) => String(entry).includes('modId="minecraft"') && String(entry).includes('runtime loader metadata')), `${edition.directory} legal audit report must record NeoForge runtime metadata exception`)
  } else {
    assert(errors, Array.isArray(report.adapterMetadataExceptions) && report.adapterMetadataExceptions.length === 0, `${edition.directory} legal audit adapter metadata exceptions mismatch`)
  }
  for (const proof of REQUIRED_LEGAL_AUDIT_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} legal audit report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(report.artifact, expectedFreshReport.artifact), `${edition.directory} legal audit artifact stale against dry-run`)
    assert(errors, sameJson(report.scanSummary, expectedFreshReport.scanSummary), `${edition.directory} legal audit scan summary stale against dry-run`)
    assert(errors, sameJson(report.policyResults, expectedFreshReport.policyResults), `${edition.directory} legal audit policy results stale against dry-run`)
    assert(errors, sameJson(report.adapterMetadataExceptions ?? [], expectedFreshReport.adapterMetadataExceptions ?? []), `${edition.directory} legal audit adapter metadata exceptions stale against dry-run`)
    assert(errors, sameStringList(report.blockedBy ?? [], expectedFreshReport.blockedBy ?? []), `${edition.directory} legal audit blockers stale against dry-run`)
    assert(errors, sameStringList(report.proofs ?? [], expectedFreshReport.proofs ?? []), `${edition.directory} legal audit proofs stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} legal audit report stale against dry-run`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} legal audit report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateFirstHourPlaytestReport(errors, report, edition, firstHourContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const { playtestFixture, moduleArtifactPath, runtimeCoreReportPath, repoRoot } = firstHourContracts
  const expectedScenarioSummaries = (playtestFixture.acceptanceScenarios ?? []).map(expectedFirstHourScenarioSummary)
  const expectedCheckpointSummaries = (playtestFixture.saveLoadCheckpoints ?? []).map(expectedSaveLoadCheckpointSummary)
  const expectedScenarioById = byId(expectedScenarioSummaries)
  const expectedCheckpointById = byId(expectedCheckpointSummaries)
  assert(errors, report.schema === 'echo.openlands.edition.first_hour_playtest_report.v1', `${edition.directory} first-hour playtest report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} first-hour playtest report status must be preflight_passed`)
  assert(errors, report.realRuntimePlaytestRequiredBeforePublicAlpha === true, `${edition.directory} first-hour report must require real runtime playtest before public alpha`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} first-hour playtest report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} first-hour playtest report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} first-hour playtest report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} first-hour playtest report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} first-hour playtest report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} first-hour playtest report moduleVersion mismatch`)
  assert(errors, report.defaultMode === 'openlands_standard', `${edition.directory} first-hour playtest report default mode mismatch`)
  assert(errors, report.playtestFixture === PLAYTEST_FIXTURE, `${edition.directory} first-hour playtest fixture mismatch`)
  assert(errors, report.routeContract === FIRST_HOUR_ROUTE_CONTRACT, `${edition.directory} first-hour route contract mismatch`)
  assert(errors, report.artifact?.file === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} first-hour artifact filename mismatch`)
  assert(errors, report.artifact?.kind === edition.moduleArtifactFamily, `${edition.directory} first-hour artifact kind mismatch`)
  assert(errors, typeof report.artifact?.path === 'string' && fileExists(report.artifact.path), `${edition.directory} first-hour artifact path must exist`)
  if (moduleArtifactPath) {
    assert(errors, sameResolvedPath(report.artifact?.path, moduleArtifactPath), `${edition.directory} first-hour artifact path mismatch`)
  }
  if (edition.moduleArtifactFamily === 'echo-addon') {
    assert(errors, typeof report.artifact?.nestedRuntimeEntry === 'string' && report.artifact.nestedRuntimeEntry.length > 0, `${edition.directory} first-hour nested runtime entry missing`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.directory} first-hour nested runtime entry mismatch`)
  }
  assert(errors, sameStringList(report.artifact?.runtimeEntriesChecked ?? [], REQUIRED_FIRST_HOUR_RUNTIME_ENTRIES), `${edition.directory} first-hour runtime entries mismatch`)
  assert(errors, report.defaultMode === playtestFixture.defaultMode, `${edition.directory} first-hour default mode must match fixture`)
  if (runtimeCoreReportPath) {
    assert(errors, report.runtimeCoreReport === runtimeCoreReportPath, `${edition.directory} first-hour runtime core report path mismatch`)
  }
  if (repoRoot && typeof report.runtimeCoreReport === 'string') {
    assert(errors, fileExists(path.join(repoRoot, report.runtimeCoreReport)), `${edition.directory} first-hour runtime core report file missing`)
  }
  assert(errors, sameStringList((report.scenarioSummaries ?? []).map((scenario) => scenario.id), expectedScenarioSummaries.map((scenario) => scenario.id)), `${edition.directory} first-hour scenario order mismatch`)
  assert(errors, sameStringList((report.scenarioSummaries ?? []).map((scenario) => scenario.routeStep), playtestFixture.requiredRouteSteps ?? []), `${edition.directory} first-hour route step order mismatch`)
  assert(errors, sameStringList((report.saveLoadCheckpoints ?? []).map((checkpoint) => checkpoint.id), expectedCheckpointSummaries.map((checkpoint) => checkpoint.id)), `${edition.directory} first-hour save/load checkpoint ids mismatch`)
  for (const scenario of report.scenarioSummaries ?? []) {
    const expected = expectedScenarioById.get(scenario.id)
    assert(errors, expected !== undefined, `${edition.directory} first-hour scenario ${scenario.id} is not in fixture`)
    if (!expected) continue
    assert(errors, scenario.routeStep === expected.routeStep, `${edition.directory} first-hour scenario ${scenario.id} route step mismatch`)
    assert(errors, scenario.targetTimeMinutes === expected.targetTimeMinutes, `${edition.directory} first-hour scenario ${scenario.id} target time mismatch`)
    assert(errors, scenario.runtimeActions === expected.runtimeActions && scenario.runtimeActions > 0, `${edition.directory} first-hour scenario ${scenario.id} runtime action count mismatch`)
    assert(errors, scenario.assertions === expected.assertions && scenario.assertions > 0, `${edition.directory} first-hour scenario ${scenario.id} assertion count mismatch`)
    assert(errors, sameStringList(scenario.successEvidence ?? [], expected.successEvidence), `${edition.directory} first-hour scenario ${scenario.id} success evidence mismatch`)
  }
  for (const checkpoint of report.saveLoadCheckpoints ?? []) {
    const expected = expectedCheckpointById.get(checkpoint.id)
    assert(errors, expected !== undefined, `${edition.directory} first-hour checkpoint ${checkpoint.id} is not in fixture`)
    if (!expected) continue
    assert(errors, checkpoint.afterScenario === expected.afterScenario, `${edition.directory} first-hour checkpoint ${checkpoint.id} afterScenario mismatch`)
    assert(errors, checkpoint.persistedFields === expected.persistedFields && checkpoint.persistedFields > 0, `${edition.directory} first-hour checkpoint ${checkpoint.id} persisted field count mismatch`)
    assert(errors, checkpoint.requiredAssertions === expected.requiredAssertions && checkpoint.requiredAssertions > 0, `${edition.directory} first-hour checkpoint ${checkpoint.id} assertion count mismatch`)
  }
  assert(errors, report.blockedBy?.includes('real_runtime_first_hour_playtest_missing'), `${edition.directory} first-hour report must name missing real runtime playtest blocker`)
  for (const proof of REQUIRED_FIRST_HOUR_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} first-hour playtest report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(report.artifact, expectedFreshReport.artifact), `${edition.directory} first-hour artifact stale against dry-run`)
    assert(errors, sameJson(report.scenarioSummaries, expectedFreshReport.scenarioSummaries), `${edition.directory} first-hour scenario summaries stale against dry-run`)
    assert(errors, sameJson(report.saveLoadCheckpoints, expectedFreshReport.saveLoadCheckpoints), `${edition.directory} first-hour save/load checkpoints stale against dry-run`)
    assert(errors, report.runtimeCoreReport === expectedFreshReport.runtimeCoreReport, `${edition.directory} first-hour runtime core report stale against dry-run`)
    assert(errors, sameStringList(report.blockedBy ?? [], expectedFreshReport.blockedBy ?? []), `${edition.directory} first-hour blockers stale against dry-run`)
    assert(errors, sameStringList(report.proofs ?? [], expectedFreshReport.proofs ?? []), `${edition.directory} first-hour proofs stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} first-hour playtest report stale against dry-run`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} first-hour playtest report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateWaystoneSaveLoadReport(errors, report, edition, waystoneSaveLoadContracts, expectedDryRun, outputSuffixLabel, expectedFreshReport = null) {
  const { playtestFixture, waystoneContract, holomapContract, moduleArtifactPath, runtimeCoreReportPath, repoRoot } = waystoneSaveLoadContracts
  const expectedStateIds = (waystoneContract.stateMachine ?? []).map((state) => state.state)
  const expectedCheckpoint = (playtestFixture.saveLoadCheckpoints ?? []).find((checkpoint) => checkpoint.id === 'after_first_waystone_repair')
  const expectedPublicAlphaScenario = playtestFixture.waystonePublicAlphaScenario ?? {}
  const expectedHolomapAcceptance = playtestFixture.holomapAcceptance ?? {}
  const expectedHolomapLayers = (holomapContract.layers ?? []).map((layer) => layer.id)
  const expectedHolomapHintTypes = (holomapContract.hintTypes ?? []).map((hint) => hint.id)
  assert(errors, report.schema === 'echo.openlands.edition.waystone_save_load_report.v1', `${edition.directory} waystone save/load report schema mismatch`)
  assert(errors, report.status === 'preflight_passed', `${edition.directory} waystone save/load report status must be preflight_passed`)
  assert(errors, report.realRuntimeSaveLoadRequiredBeforePublicAlpha === true, `${edition.directory} waystone report must require real runtime save/load before public alpha`)
  assert(errors, report.dryRun === expectedDryRun, `${edition.directory} waystone save/load report dryRun mismatch`)
  assert(errors, report.packId === edition.packId, `${edition.directory} waystone save/load report packId mismatch`)
  assert(errors, report.displayName === edition.displayName, `${edition.directory} waystone save/load report displayName mismatch`)
  assert(errors, report.runtimeTarget === edition.runtimeTarget, `${edition.directory} waystone save/load report runtimeTarget mismatch`)
  assert(errors, report.moduleId === MODULE_ID, `${edition.directory} waystone save/load report moduleId mismatch`)
  assert(errors, report.moduleVersion === VERSION, `${edition.directory} waystone save/load report moduleVersion mismatch`)
  assert(errors, report.waystoneContract === WAYSTONE_CONTRACT, `${edition.directory} waystone contract path mismatch`)
  assert(errors, report.playtestFixture === PLAYTEST_FIXTURE, `${edition.directory} waystone playtest fixture mismatch`)
  assert(errors, report.holomapContract === HOLOMAP_CONTRACT, `${edition.directory} waystone HoloMap contract path mismatch`)
  assert(errors, report.artifact?.file === edition.moduleArtifactPattern.replace('<module>', MODULE_ID).replace('<version>', VERSION), `${edition.directory} waystone artifact filename mismatch`)
  assert(errors, report.artifact?.kind === edition.moduleArtifactFamily, `${edition.directory} waystone artifact kind mismatch`)
  assert(errors, typeof report.artifact?.path === 'string' && fileExists(report.artifact.path), `${edition.directory} waystone artifact path must exist`)
  if (moduleArtifactPath) {
    assert(errors, sameResolvedPath(report.artifact?.path, moduleArtifactPath), `${edition.directory} waystone artifact path mismatch`)
  }
  if (edition.moduleArtifactFamily === 'echo-addon') {
    assert(errors, typeof report.artifact?.nestedRuntimeEntry === 'string' && report.artifact.nestedRuntimeEntry.length > 0, `${edition.directory} waystone nested runtime entry missing`)
  } else {
    assert(errors, report.artifact?.nestedRuntimeEntry === null, `${edition.directory} waystone nested runtime entry mismatch`)
  }
  assert(errors, sameStringList(report.artifact?.runtimeEntriesChecked ?? [], REQUIRED_WAYSTONE_SAVE_LOAD_RUNTIME_ENTRIES), `${edition.directory} waystone runtime entries mismatch`)
  if (runtimeCoreReportPath) {
    assert(errors, report.runtimeCoreReport === runtimeCoreReportPath, `${edition.directory} waystone runtime core report path mismatch`)
  }
  if (repoRoot && typeof report.runtimeCoreReport === 'string') {
    assert(errors, fileExists(path.join(repoRoot, report.runtimeCoreReport)), `${edition.directory} waystone runtime core report file missing`)
  }
  assert(errors, sameStringList(report.stateMachine?.states ?? [], expectedStateIds), `${edition.directory} waystone state order mismatch`)
  assert(errors, sameStringList(expectedPublicAlphaScenario.requiresStates ?? [], expectedStateIds), `${edition.directory} waystone public alpha scenario must cover contract state order`)
  assert(errors, report.stateMachine?.activeStonesRequiredForFastTravel === waystoneContract.effects?.fastTravel?.requiresActiveStones, `${edition.directory} waystone fast travel requirement mismatch`)
  assert(errors, report.stateMachine?.activeStonesRequiredForFastTravel === 2, `${edition.directory} waystone fast travel must require two active stones`)
  assert(errors, report.stateMachine?.nearbyHintRange?.min === waystoneContract.effects?.nearbyHints?.min, `${edition.directory} waystone nearby hint min mismatch`)
  assert(errors, report.stateMachine?.nearbyHintRange?.max === waystoneContract.effects?.nearbyHints?.max, `${edition.directory} waystone nearby hint max mismatch`)
  assert(errors, sameStringList(report.stateMachine?.nearbyHintRange?.categories ?? [], waystoneContract.effects?.nearbyHints?.categories ?? []), `${edition.directory} waystone nearby hint categories mismatch`)
  assert(errors, report.saveLoadCheckpoint?.id === expectedCheckpoint?.id, `${edition.directory} waystone save/load checkpoint id mismatch`)
  assert(errors, report.saveLoadCheckpoint?.afterScenario === expectedCheckpoint?.afterScenario, `${edition.directory} waystone save/load checkpoint afterScenario mismatch`)
  assert(errors, sameStringList(report.saveLoadCheckpoint?.persistedFields ?? [], expectedCheckpoint?.mustPersist ?? []), `${edition.directory} waystone save/load persisted fields mismatch`)
  assert(errors, sameStringList(report.saveLoadCheckpoint?.requiredAssertions ?? [], expectedCheckpoint?.requiredAssertions ?? []), `${edition.directory} waystone save/load assertions mismatch`)
  assert(errors, report.saveLoadCheckpoint?.persistedFields?.includes('waystoneState'), `${edition.directory} waystone save/load checkpoint must persist waystoneState`)
  assert(errors, report.saveLoadCheckpoint?.persistedFields?.includes('holomapRegionDiscovery'), `${edition.directory} waystone save/load checkpoint must persist holomapRegionDiscovery`)
  assert(errors, report.publicAlphaScenario?.id === expectedPublicAlphaScenario.id, `${edition.directory} waystone public alpha scenario id mismatch`)
  assert(errors, sameStringList(report.publicAlphaScenario?.states ?? [], expectedPublicAlphaScenario.requiresStates ?? []), `${edition.directory} waystone public alpha states mismatch`)
  assert(errors, sameStringList(report.publicAlphaScenario?.effects ?? [], expectedPublicAlphaScenario.expectedEffects ?? []), `${edition.directory} waystone public alpha effects mismatch`)
  assert(errors, sameStringList(report.publicAlphaScenario?.persistedFields ?? [], expectedPublicAlphaScenario.mustPersist ?? []), `${edition.directory} waystone public alpha persisted fields mismatch`)
  assert(errors, sameStringList(report.publicAlphaScenario?.successEvidence ?? [], expectedPublicAlphaScenario.successEvidence ?? []), `${edition.directory} waystone public alpha success evidence mismatch`)
  assert(errors, report.publicAlphaScenario?.persistedFields?.includes('repairContributorIds'), `${edition.directory} waystone public alpha scenario must persist repairContributorIds`)
  assert(errors, report.publicAlphaScenario?.effects?.includes('two_active_stones_unlock_fast_travel'), `${edition.directory} waystone public alpha scenario must assert fast travel unlock`)
  assert(errors, sameStringList(report.holomapAcceptance?.mustPersistFields ?? [], expectedHolomapAcceptance.mustPersistFields ?? []), `${edition.directory} waystone HoloMap persisted fields mismatch`)
  assert(errors, sameStringList(report.holomapAcceptance?.mustPersistFields ?? [], holomapContract.regionDataContract?.storedFields ?? []), `${edition.directory} waystone HoloMap persisted fields must match HoloMap contract`)
  assert(errors, sameStringList(report.holomapAcceptance?.requiredLayers ?? [], expectedHolomapAcceptance.requiredLayers ?? []), `${edition.directory} waystone HoloMap layer fixture mismatch`)
  assert(errors, sameStringList(report.holomapAcceptance?.requiredLayers ?? [], expectedHolomapLayers), `${edition.directory} waystone HoloMap layer contract mismatch`)
  assert(errors, sameStringList(report.holomapAcceptance?.requiredHintTypes ?? [], expectedHolomapAcceptance.requiredHintTypes ?? []), `${edition.directory} waystone HoloMap hint fixture mismatch`)
  assert(errors, sameStringSet(report.holomapAcceptance?.requiredHintTypes ?? [], expectedHolomapHintTypes), `${edition.directory} waystone HoloMap hint contract mismatch`)
  assert(errors, report.holomapAcceptance?.fallbackRequired === expectedHolomapAcceptance.fallbackRequired, `${edition.directory} waystone HoloMap fallback mismatch`)
  assert(errors, report.holomapAcceptance?.fallbackRequired === Boolean(holomapContract.regionDataContract?.fallbackIfHoloMapMissing), `${edition.directory} waystone HoloMap fallback must match HoloMap contract`)
  assert(errors, report.holomapAcceptance?.mustPersistFields?.includes('oldRoadSegments'), `${edition.directory} waystone report must preserve oldRoadSegments`)
  assert(errors, report.blockedBy?.includes('real_runtime_waystone_save_load_test_missing'), `${edition.directory} waystone report must name missing real runtime save/load blocker`)
  for (const proof of REQUIRED_WAYSTONE_SAVE_LOAD_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `${edition.directory} waystone save/load report missing proof ${proof}`)
  }
  if (expectedFreshReport) {
    assert(errors, sameJson(report.artifact, expectedFreshReport.artifact), `${edition.directory} waystone artifact stale against dry-run`)
    assert(errors, sameJson(report.stateMachine, expectedFreshReport.stateMachine), `${edition.directory} waystone state machine stale against dry-run`)
    assert(errors, sameJson(report.saveLoadCheckpoint, expectedFreshReport.saveLoadCheckpoint), `${edition.directory} waystone save/load checkpoint stale against dry-run`)
    assert(errors, sameJson(report.publicAlphaScenario, expectedFreshReport.publicAlphaScenario), `${edition.directory} waystone public alpha scenario stale against dry-run`)
    assert(errors, sameJson(report.holomapAcceptance, expectedFreshReport.holomapAcceptance), `${edition.directory} waystone HoloMap acceptance stale against dry-run`)
    assert(errors, report.runtimeCoreReport === expectedFreshReport.runtimeCoreReport, `${edition.directory} waystone runtime core report stale against dry-run`)
    assert(errors, sameStringList(report.blockedBy ?? [], expectedFreshReport.blockedBy ?? []), `${edition.directory} waystone blockers stale against dry-run`)
    assert(errors, sameStringList(report.proofs ?? [], expectedFreshReport.proofs ?? []), `${edition.directory} waystone proofs stale against dry-run`)
    assert(errors, sameJson(stableGeneratorReport(report), stableGeneratorReport(expectedFreshReport)), `${edition.directory} waystone save/load report stale against dry-run`)
  }
  if (outputSuffixLabel) {
    assert(errors, report.outputPath?.endsWith(outputSuffixLabel.replace(/\//g, path.sep)), `${edition.directory} waystone save/load report outputPath must end with ${outputSuffixLabel}`)
  }
}

function validateEdition({ edition, repoRoot, moduleRoot, runtimePlan, requiredPhases, publicAlphaEvidence, runtimeEvidenceIds, runtimeScenarioIds, registryParityContracts, craftingContracts, worldgenContracts, creatureRosterContracts, oldRoadNetworkContracts, alphaSystemsContracts, distributionRoadmapContracts, launcherFlowContracts, localLauncherRehearsalContracts, localRuntimeRehearsalContracts, legalAuditContracts, finalReleaseReview, playtestFixture, playtestScenarioIds, saveLoadCheckpointIds, waystonePublicAlphaScenario, waystoneContract, holomapContract, launcherFlowIds, finalReviewAreaIds, finalReviewGateIds, harnessDriverManifestContract }) {
  const errors = []
  const warnings = []

  assert(errors, fileExists(repoRoot), `${edition.directory} directory is missing`)
  if (!fileExists(repoRoot)) {
    return { key: edition.key, repoRoot, errors, warnings }
  }

  for (const relativePath of REQUIRED_DOCS) {
    assert(errors, fileExists(path.join(repoRoot, relativePath)), `${edition.directory} missing ${relativePath}`)
  }

  const manifestPath = path.join(repoRoot, 'release-manifest.template.json')
  if (!fileExists(manifestPath)) {
    return { key: edition.key, repoRoot, errors, warnings }
  }

  const manifest = readJson(manifestPath)
  const evidenceTemplatePath = path.join(repoRoot, 'evidence', 'runtime-evidence.template.json')
  const evidenceTemplate = fileExists(evidenceTemplatePath) ? readJson(evidenceTemplatePath) : null
  const distributionRoadmapContractsForEdition = {
    ...distributionRoadmapContracts,
    releaseManifest: manifest,
  }
  const launcherFlowContractsForEdition = {
    ...launcherFlowContracts,
    editionManifest: manifest,
    evidenceTemplate,
  }
  assert(errors, manifest.packId === edition.packId, `${edition.directory} packId must be ${edition.packId}`)
  assert(errors, manifest.displayName === edition.displayName, `${edition.directory} displayName must be ${edition.displayName}`)
  assert(errors, manifest.sourceRepo === edition.sourceRepo, `${edition.directory} sourceRepo must be ${edition.sourceRepo}`)
  assert(errors, manifest.runtimeTarget === edition.runtimeTarget, `${edition.directory} runtimeTarget must be ${edition.runtimeTarget}`)
  assert(errors, manifest.loader === edition.loader, `${edition.directory} loader must be ${edition.loader}`)
  assert(errors, manifest.moduleArtifactFamily === edition.moduleArtifactFamily, `${edition.directory} artifact family must be ${edition.moduleArtifactFamily}`)
  assert(errors, manifest.moduleArtifactPattern === edition.moduleArtifactPattern, `${edition.directory} artifact pattern must be ${edition.moduleArtifactPattern}`)
  assert(errors, manifest.moduleSourcePattern === '<module>-<version>-sources.jar', `${edition.directory} moduleSourcePattern must remain sources jar pattern`)
  assert(errors, sameStringList(manifest.requiredModuleDescriptors ?? [], edition.requiredModuleDescriptors), `${edition.directory} requiredModuleDescriptors mismatch`)
  assert(errors, (manifest.optionalPackageDescriptors ?? []).includes('echo-addon-package.json'), `${edition.directory} optional descriptors must include echo-addon-package.json`)
  assert(errors, manifest.requiredRuntimeEvidenceContract === RUNTIME_EVIDENCE_CONTRACT, `${edition.directory} requiredRuntimeEvidenceContract mismatch`)
  assert(errors, sameStringList(manifest.requiredAdapterLoadPhases ?? [], requiredPhases), `${edition.directory} requiredAdapterLoadPhases must match public_alpha gate`)
  assert(errors, sameStringList(manifest.requiredPublicAlphaEvidence ?? [], publicAlphaEvidence), `${edition.directory} requiredPublicAlphaEvidence must match public_alpha gate`)
  assert(errors, Array.isArray(manifest.artifacts), `${edition.directory} artifacts must be an array`)
  if (Array.isArray(manifest.artifacts) && manifest.artifacts.length > 0) {
    warnings.push(`${edition.directory} template has artifact entries; verify these are placeholders, not release artifacts`)
  }

  const requiredModuleIds = moduleIds(manifest)
  for (const moduleId of REQUIRED_MODULES) {
    assert(errors, requiredModuleIds.has(moduleId), `${edition.directory} moduleRequirements missing ${moduleId}`)
  }
  const openlandsRequirement = (manifest.moduleRequirements ?? []).find((entry) => entry.id === MODULE_ID)
  assert(errors, openlandsRequirement?.version === VERSION, `${edition.directory} ${MODULE_ID} version must be ${VERSION}`)

  const runtimeEvidenceDoc = fileExists(path.join(repoRoot, 'docs', 'runtime-evidence.md'))
    ? readText(path.join(repoRoot, 'docs', 'runtime-evidence.md'))
    : ''
  const moduleRequirementsDoc = fileExists(path.join(repoRoot, 'docs', 'module-requirements.md'))
    ? readText(path.join(repoRoot, 'docs', 'module-requirements.md'))
    : ''
  const readme = fileExists(path.join(repoRoot, 'README.md')) ? readText(path.join(repoRoot, 'README.md')) : ''

  for (const phase of requiredPhases) {
    assert(errors, runtimeEvidenceDoc.includes(`\`${phase}\``), `${edition.directory} runtime evidence doc missing phase ${phase}`)
  }
  for (const evidence of publicAlphaEvidence) {
    assert(errors, runtimeEvidenceDoc.includes(`\`${evidence}\``), `${edition.directory} runtime evidence doc missing public alpha evidence ${evidence}`)
  }
  for (const text of edition.docsMustContain) {
    assert(errors, runtimeEvidenceDoc.includes(text) || moduleRequirementsDoc.includes(text), `${edition.directory} docs missing required text: ${text}`)
  }
  for (const moduleId of REQUIRED_MODULES) {
    assert(errors, moduleRequirementsDoc.includes(`\`${moduleId}\``), `${edition.directory} module requirements doc missing ${moduleId}`)
  }
  assert(errors, moduleRequirementsDoc.includes(RUNTIME_EVIDENCE_CONTRACT), `${edition.directory} module requirements doc missing runtime evidence contract path`)
  assert(errors, moduleRequirementsDoc.includes(HARNESS_DRIVER_MANIFEST_CONTRACT) || runtimeEvidenceDoc.includes(HARNESS_DRIVER_MANIFEST_CONTRACT), `${edition.directory} docs missing harness driver manifest contract path`)
  assert(errors, readme.includes('Implementation foundation only'), `${edition.directory} README must clearly mark foundation status`)

  const harnessDriverManifestTemplatePath = path.join(repoRoot, edition.harnessDriverManifestTemplate)
  let harnessDriverManifestTemplate = null
  assert(errors, fileExists(harnessDriverManifestTemplatePath), `${edition.directory} missing ${edition.harnessDriverManifestTemplate}`)
  if (fileExists(harnessDriverManifestTemplatePath)) {
    harnessDriverManifestTemplate = readJson(harnessDriverManifestTemplatePath)
    validateHarnessDriverManifestTemplate(errors, harnessDriverManifestTemplate, edition, harnessDriverManifestContract)
  }
  const harnessManifestGeneratorScript = path.join(moduleRoot, 'scripts', 'generate-openlands-harness-driver-manifest.mjs')
  assert(errors, fileExists(harnessManifestGeneratorScript), `missing Openlands harness driver manifest generator ${harnessManifestGeneratorScript}`)
  let generatedHarnessDriverManifest = null
  if (fileExists(harnessManifestGeneratorScript)) {
    const dryRun = runJson('node', [
      harnessManifestGeneratorScript,
      '--module-root',
      moduleRoot,
      '--edition',
      edition.key,
      '--edition-root',
      repoRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} harness driver manifest generator dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        generatedHarnessDriverManifest = JSON.parse(dryRun.stdout)
        validateHarnessDriverManifestTemplate(errors, generatedHarnessDriverManifest, edition, harnessDriverManifestContract)
        assert(errors, generatedHarnessDriverManifest.status === 'template_blocked', `${edition.directory} generated harness driver manifest must remain template_blocked before real drivers exist`)
        assert(errors, (generatedHarnessDriverManifest.availableDriverSurfaces ?? []).length === 0, `${edition.directory} generated harness driver manifest must have zero available drivers`)
        assert(errors, (generatedHarnessDriverManifest.missingDriverSurfaces ?? []).length > 0, `${edition.directory} generated harness driver manifest must list missing drivers`)
      } catch (error) {
        errors.push(`${edition.directory} harness driver manifest generator dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }
  if (harnessDriverManifestTemplate && generatedHarnessDriverManifest) {
    assert(errors, sameJson(
      stableHarnessDriverManifestTemplate(harnessDriverManifestTemplate),
      stableHarnessDriverManifestTemplate(generatedHarnessDriverManifest),
    ), `${edition.directory} harness driver manifest template stale against dry-run`)
  }
  const harnessManifestValidatorScript = path.join(moduleRoot, 'scripts', 'validate-openlands-harness-driver-manifest.mjs')
  assert(errors, fileExists(harnessManifestValidatorScript), `missing Openlands harness driver manifest validator ${harnessManifestValidatorScript}`)
  if (fileExists(harnessManifestValidatorScript)) {
    const result = runJson('node', [
      harnessManifestValidatorScript,
      '--module-root',
      moduleRoot,
      '--edition',
      edition.key,
      '--edition-root',
      repoRoot,
      '--json',
    ], repoRoot)
    assert(errors, result.status === 0, `${edition.directory} harness driver manifest validator failed: ${result.stderr || result.stdout}`)
    if (result.status === 0) {
      try {
        const validation = JSON.parse(result.stdout)
        assert(errors, validation.status === 'passed', `${edition.directory} harness driver manifest validator status must pass`)
        assert(errors, validation.missingDriverSurfaceCount > 0, `${edition.directory} harness driver manifest validator must report missing drivers before real drivers exist`)
      } catch (error) {
        errors.push(`${edition.directory} harness driver manifest validator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const moduleArtifactPath = path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release', MODULE_ID, edition.moduleArtifactPattern
    .replace('<module>', MODULE_ID)
    .replace('<version>', VERSION))
  let adapterBootDryRunReport = null
  let registryParityDryRunReport = null
  let craftingStationDryRunReport = null
  let worldgenExplorationDryRunReport = null
  let creatureRosterDryRunReport = null
  let oldRoadNetworkDryRunReport = null
  let alphaSystemsDryRunReport = null
  let distributionRoadmapDryRunReport = null
  let localRuntimeRehearsalDryRunReport = null
  let localLauncherRehearsalDryRunReport = null
  let launcherFlowDryRunReport = null
  let legalAuditDryRunReport = null
  let firstHourPlaytestDryRunReport = null
  let waystoneSaveLoadDryRunReport = null
  let runtimeExecutionDryRunReport = null
  let launcherExecutionDryRunReport = null
  let finalReleaseReviewDryRunReport = null
  let distributionApprovalDryRunReport = null
  const firstHourContracts = {
    playtestFixture,
    moduleArtifactPath,
    runtimeCoreReportPath: evidenceTemplate?.evidenceAttachments?.runtimeCoreReport,
    repoRoot,
  }
  const waystoneSaveLoadContracts = {
    playtestFixture,
    waystoneContract,
    holomapContract,
    moduleArtifactPath,
    runtimeCoreReportPath: evidenceTemplate?.evidenceAttachments?.runtimeCoreReport,
    repoRoot,
  }
  const finalReviewContracts = {
    finalReleaseReview,
    moduleArtifactPath,
    assetManifestPath: path.join(moduleRoot, 'src', 'main', 'resources', ASSET_MANIFEST_CONTRACT),
    legalAuditPath: path.join(moduleRoot, 'src', 'main', 'resources', LEGAL_AUDIT_CONTRACT),
    reviewAreaIds: finalReviewAreaIds,
    finalReviewGateIds,
  }
  const distributionApprovalAttachmentContracts = {
    distributionApproval: distributionRoadmapContracts.distributionApproval,
    releaseIndex: distributionRoadmapContracts.releaseIndex,
    releaseIndexPath: path.join(distributionRoadmapContracts.releaseRoot, 'echo-release.json'),
    approvalAreaIds: (distributionRoadmapContracts.distributionApproval.approvalAreas ?? []).map((area) => area.id),
    distributionGateIds: (distributionRoadmapContracts.distributionApproval.distributionGates ?? []).map((gate) => gate.id).sort(),
  }

  const adapterBootScript = path.join(repoRoot, 'scripts', 'generate-adapter-boot-report.mjs')
  assert(errors, fileExists(adapterBootScript), `${edition.directory} missing scripts/generate-adapter-boot-report.mjs`)
  if (fileExists(adapterBootScript)) {
    const dryRun = runJson('node', [
      adapterBootScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} adapter boot harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        adapterBootDryRunReport = JSON.parse(dryRun.stdout)
        validateAdapterBootReport(errors, adapterBootDryRunReport, edition, runtimePlan, requiredPhases, runtimeEvidenceIds, publicAlphaEvidence, true, null)
      } catch (error) {
        errors.push(`${edition.directory} adapter boot harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const registryParityScript = path.join(repoRoot, 'scripts', 'generate-registry-parity-report.mjs')
  assert(errors, fileExists(registryParityScript), `${edition.directory} missing scripts/generate-registry-parity-report.mjs`)
  if (fileExists(registryParityScript)) {
    const dryRun = runJson('node', [
      registryParityScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} registry parity harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        registryParityDryRunReport = JSON.parse(dryRun.stdout)
        validateRegistryParityReport(errors, registryParityDryRunReport, edition, registryParityContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} registry parity harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const craftingStationScript = path.join(repoRoot, 'scripts', 'generate-crafting-station-report.mjs')
  assert(errors, fileExists(craftingStationScript), `${edition.directory} missing scripts/generate-crafting-station-report.mjs`)
  if (fileExists(craftingStationScript)) {
    const dryRun = runJson('node', [
      craftingStationScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} crafting station harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        craftingStationDryRunReport = JSON.parse(dryRun.stdout)
        validateCraftingStationReport(errors, craftingStationDryRunReport, edition, craftingContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} crafting station harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const worldgenExplorationScript = path.join(repoRoot, 'scripts', 'generate-worldgen-exploration-report.mjs')
  assert(errors, fileExists(worldgenExplorationScript), `${edition.directory} missing scripts/generate-worldgen-exploration-report.mjs`)
  if (fileExists(worldgenExplorationScript)) {
    const dryRun = runJson('node', [
      worldgenExplorationScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} worldgen exploration harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        worldgenExplorationDryRunReport = JSON.parse(dryRun.stdout)
        validateWorldgenExplorationReport(errors, worldgenExplorationDryRunReport, edition, worldgenContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} worldgen exploration harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const creatureRosterScript = path.join(repoRoot, 'scripts', 'generate-creature-roster-report.mjs')
  assert(errors, fileExists(creatureRosterScript), `${edition.directory} missing scripts/generate-creature-roster-report.mjs`)
  if (fileExists(creatureRosterScript)) {
    const dryRun = runJson('node', [
      creatureRosterScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} creature roster harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        creatureRosterDryRunReport = JSON.parse(dryRun.stdout)
        validateCreatureRosterReport(errors, creatureRosterDryRunReport, edition, creatureRosterContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} creature roster harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const oldRoadNetworkScript = path.join(repoRoot, 'scripts', 'generate-old-road-network-report.mjs')
  assert(errors, fileExists(oldRoadNetworkScript), `${edition.directory} missing scripts/generate-old-road-network-report.mjs`)
  if (fileExists(oldRoadNetworkScript)) {
    const dryRun = runJson('node', [
      oldRoadNetworkScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} old road network harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        oldRoadNetworkDryRunReport = JSON.parse(dryRun.stdout)
        validateOldRoadNetworkReport(errors, oldRoadNetworkDryRunReport, edition, oldRoadNetworkContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} old road network harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const alphaSystemsScript = path.join(repoRoot, 'scripts', 'generate-alpha-systems-report.mjs')
  assert(errors, fileExists(alphaSystemsScript), `${edition.directory} missing scripts/generate-alpha-systems-report.mjs`)
  if (fileExists(alphaSystemsScript)) {
    const dryRun = runJson('node', [
      alphaSystemsScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} alpha systems harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        alphaSystemsDryRunReport = JSON.parse(dryRun.stdout)
        validateAlphaSystemsReport(errors, alphaSystemsDryRunReport, edition, alphaSystemsContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} alpha systems harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const distributionRoadmapScript = path.join(repoRoot, 'scripts', 'generate-distribution-roadmap-report.mjs')
  assert(errors, fileExists(distributionRoadmapScript), `${edition.directory} missing scripts/generate-distribution-roadmap-report.mjs`)
  if (fileExists(distributionRoadmapScript)) {
    const dryRun = runJson('node', [
      distributionRoadmapScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} distribution roadmap harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        distributionRoadmapDryRunReport = JSON.parse(dryRun.stdout)
        validateDistributionRoadmapReport(errors, distributionRoadmapDryRunReport, edition, distributionRoadmapContractsForEdition, true, null)
      } catch (error) {
        errors.push(`${edition.directory} distribution roadmap harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const runtimeCoreScript = path.join(repoRoot, 'scripts', 'generate-runtime-core-report.mjs')
  let runtimeCoreDryRunReport = null
  assert(errors, fileExists(runtimeCoreScript), `${edition.directory} missing scripts/generate-runtime-core-report.mjs`)
  if (fileExists(runtimeCoreScript)) {
    const dryRun = runJson('node', [
      runtimeCoreScript,
      '--module-artifact',
      moduleArtifactPath,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} runtime core harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        runtimeCoreDryRunReport = JSON.parse(dryRun.stdout)
        validateRuntimeCoreReport(errors, {
          report: runtimeCoreDryRunReport,
          edition,
          expectedDryRun: true,
          outputSuffixLabel: null,
          moduleArtifactPath,
        })
      } catch (error) {
        errors.push(`${edition.directory} runtime core harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const localRuntimeRehearsalGeneratorScript = path.join(moduleRoot, 'scripts', 'generate-openlands-local-runtime-rehearsal-report.mjs')
  assert(errors, fileExists(localRuntimeRehearsalGeneratorScript), `${edition.directory} missing module local runtime rehearsal generator`)
  if (fileExists(localRuntimeRehearsalGeneratorScript)) {
    const dryRun = runJson('node', [
      localRuntimeRehearsalGeneratorScript,
      '--module-root',
      moduleRoot,
      '--edition',
      edition.key,
      '--edition-root',
      repoRoot,
      '--dry-run',
      '--json',
    ], moduleRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} local runtime rehearsal generator dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        localRuntimeRehearsalDryRunReport = JSON.parse(dryRun.stdout)
        validateLocalRuntimeRehearsalReport(errors, localRuntimeRehearsalDryRunReport, edition, localRuntimeRehearsalContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} local runtime rehearsal dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }
  const localRuntimeRehearsalValidatorScript = path.join(moduleRoot, 'scripts', 'validate-openlands-local-runtime-rehearsal-report.mjs')
  assert(errors, fileExists(localRuntimeRehearsalValidatorScript), `${edition.directory} missing module local runtime rehearsal validator`)
  if (fileExists(localRuntimeRehearsalValidatorScript)) {
    const validation = runJson('node', [
      localRuntimeRehearsalValidatorScript,
      '--module-root',
      moduleRoot,
      '--edition',
      edition.key,
      '--edition-root',
      repoRoot,
      '--json',
    ], moduleRoot)
    assert(errors, validation.status === 0, `${edition.directory} local runtime rehearsal validator failed: ${validation.stderr || validation.stdout}`)
    if (validation.status === 0) {
      try {
        const result = JSON.parse(validation.stdout)
        assert(errors, result.status === 'passed', `${edition.directory} local runtime rehearsal validator must pass`)
        assert(errors, result.runtimeTarget === edition.runtimeTarget, `${edition.directory} local runtime rehearsal validator target mismatch`)
        assert(errors, result.scenarioCount === runtimeScenarioIds.length, `${edition.directory} local runtime rehearsal validator scenario count mismatch`)
        assert(errors, result.publicAlphaReady === false, `${edition.directory} local runtime rehearsal validator must keep public alpha blocked`)
      } catch (error) {
        errors.push(`${edition.directory} local runtime rehearsal validator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const launcherFlowScript = path.join(repoRoot, 'scripts', 'generate-launcher-flow-report.mjs')
  assert(errors, fileExists(launcherFlowScript), `${edition.directory} missing scripts/generate-launcher-flow-report.mjs`)
  if (fileExists(launcherFlowScript)) {
    const dryRun = runJson('node', [
      launcherFlowScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} launcher flow harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        launcherFlowDryRunReport = JSON.parse(dryRun.stdout)
        validateLauncherFlowReport(errors, launcherFlowDryRunReport, edition, launcherFlowContractsForEdition, true, null)
      } catch (error) {
        errors.push(`${edition.directory} launcher flow harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const localLauncherRehearsalGeneratorScript = path.join(moduleRoot, 'scripts', 'generate-openlands-local-launcher-rehearsal-report.mjs')
  assert(errors, fileExists(localLauncherRehearsalGeneratorScript), `${edition.directory} missing module local launcher rehearsal generator`)
  if (fileExists(localLauncherRehearsalGeneratorScript)) {
    const dryRun = runJson('node', [
      localLauncherRehearsalGeneratorScript,
      '--module-root',
      moduleRoot,
      '--edition',
      edition.key,
      '--edition-root',
      repoRoot,
      '--dry-run',
      '--json',
    ], moduleRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} local launcher rehearsal generator dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        localLauncherRehearsalDryRunReport = JSON.parse(dryRun.stdout)
        validateLocalLauncherRehearsalReport(errors, localLauncherRehearsalDryRunReport, edition, localLauncherRehearsalContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} local launcher rehearsal dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }
  const localLauncherRehearsalValidatorScript = path.join(moduleRoot, 'scripts', 'validate-openlands-local-launcher-rehearsal-report.mjs')
  assert(errors, fileExists(localLauncherRehearsalValidatorScript), `${edition.directory} missing module local launcher rehearsal validator`)
  if (fileExists(localLauncherRehearsalValidatorScript)) {
    const validation = runJson('node', [
      localLauncherRehearsalValidatorScript,
      '--module-root',
      moduleRoot,
      '--edition',
      edition.key,
      '--edition-root',
      repoRoot,
      '--json',
    ], moduleRoot)
    assert(errors, validation.status === 0, `${edition.directory} local launcher rehearsal validator failed: ${validation.stderr || validation.stdout}`)
    if (validation.status === 0) {
      try {
        const result = JSON.parse(validation.stdout)
        assert(errors, result.status === 'passed', `${edition.directory} local launcher rehearsal validator must pass`)
        assert(errors, result.runtimeTarget === edition.runtimeTarget, `${edition.directory} local launcher rehearsal validator target mismatch`)
        assert(errors, result.flowCount === launcherFlowIds.length, `${edition.directory} local launcher rehearsal validator flow count mismatch`)
        assert(errors, result.publicAlphaReady === false, `${edition.directory} local launcher rehearsal validator must keep public alpha blocked`)
      } catch (error) {
        errors.push(`${edition.directory} local launcher rehearsal validator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const legalAuditScript = path.join(repoRoot, 'scripts', 'generate-legal-audit-report.mjs')
  assert(errors, fileExists(legalAuditScript), `${edition.directory} missing scripts/generate-legal-audit-report.mjs`)
  if (fileExists(legalAuditScript)) {
    const dryRun = runJson('node', [
      legalAuditScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} legal audit harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        legalAuditDryRunReport = JSON.parse(dryRun.stdout)
        validateLegalAuditReport(errors, legalAuditDryRunReport, edition, legalAuditContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} legal audit harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const firstHourScript = path.join(repoRoot, 'scripts', 'generate-first-hour-playtest-report.mjs')
  assert(errors, fileExists(firstHourScript), `${edition.directory} missing scripts/generate-first-hour-playtest-report.mjs`)
  if (fileExists(firstHourScript)) {
    const dryRun = runJson('node', [
      firstHourScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} first-hour playtest harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        firstHourPlaytestDryRunReport = JSON.parse(dryRun.stdout)
        validateFirstHourPlaytestReport(errors, firstHourPlaytestDryRunReport, edition, firstHourContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} first-hour playtest harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const waystoneSaveLoadScript = path.join(repoRoot, 'scripts', 'generate-waystone-save-load-report.mjs')
  assert(errors, fileExists(waystoneSaveLoadScript), `${edition.directory} missing scripts/generate-waystone-save-load-report.mjs`)
  if (fileExists(waystoneSaveLoadScript)) {
    const dryRun = runJson('node', [
      waystoneSaveLoadScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} waystone save/load harness dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        waystoneSaveLoadDryRunReport = JSON.parse(dryRun.stdout)
        validateWaystoneSaveLoadReport(errors, waystoneSaveLoadDryRunReport, edition, waystoneSaveLoadContracts, true, null)
      } catch (error) {
        errors.push(`${edition.directory} waystone save/load harness dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const runtimeExecutionValidatorScript = path.join(repoRoot, 'scripts', 'validate-runtime-execution-report.mjs')
  assert(errors, fileExists(runtimeExecutionValidatorScript), `${edition.directory} missing scripts/validate-runtime-execution-report.mjs`)
  const runtimeExecutionGeneratorScript = path.join(repoRoot, 'scripts', 'generate-runtime-execution-report.mjs')
  assert(errors, fileExists(runtimeExecutionGeneratorScript), `${edition.directory} missing scripts/generate-runtime-execution-report.mjs`)
  if (fileExists(runtimeExecutionGeneratorScript)) {
    const dryRun = runJson('node', [
      runtimeExecutionGeneratorScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} runtime execution blocked-report generator dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        runtimeExecutionDryRunReport = JSON.parse(dryRun.stdout)
        validateRuntimeExecutionReport(errors, {
          report: runtimeExecutionDryRunReport,
          edition,
          moduleArtifactPath,
          runtimeScenarioIds,
          outputSuffixLabel: null,
        })
      } catch (error) {
        errors.push(`${edition.directory} runtime execution generator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }
  validateHarnessRunnerDryRun(errors, {
    scriptPath: path.join(repoRoot, 'scripts', 'run-runtime-execution-harness.mjs'),
    moduleRoot,
    repoRoot,
    edition,
    schema: 'echo.openlands.edition.runtime_execution_report.v1',
    harnessType: 'runtime',
    readyField: 'publicAlphaReady',
    clearedGateField: 'clearedRuntimeGates',
    remainingGateField: 'remainingRuntimeGates',
    minimumRemainingGates: 14,
  })
  if (fileExists(runtimeExecutionValidatorScript)) {
    const dryRun = runJson('node', [
      runtimeExecutionValidatorScript,
      '--module-root',
      moduleRoot,
      '--allow-missing',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} runtime execution validator allow-missing check failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        const result = JSON.parse(dryRun.stdout)
        assert(errors, ['missing', 'passed'].includes(result.status), `${edition.directory} runtime execution validator status must be missing or passed`)
        assert(errors, result.runtimeTarget === edition.runtimeTarget, `${edition.directory} runtime execution validator target mismatch`)
        if (result.status === 'missing') {
          assert(errors, result.expectedScenarioCount >= 17, `${edition.directory} runtime execution validator scenario count too low`)
          assert(errors, result.expectedRuntimeGateCount >= 14, `${edition.directory} runtime execution validator gate count too low`)
        } else {
          assert(errors, ['blocked', 'passed'].includes(result.reportStatus), `${edition.directory} runtime execution report status must be blocked or passed`)
          if (result.reportStatus === 'blocked') {
            assert(errors, result.publicAlphaReady === false, `${edition.directory} blocked runtime execution report must not mark public alpha ready`)
            assert(errors, result.clearedRuntimeGates === 0, `${edition.directory} blocked runtime execution report must clear zero gates`)
            assert(errors, result.remainingRuntimeGates >= 14, `${edition.directory} blocked runtime execution report must keep runtime gates remaining`)
          }
        }
      } catch (error) {
        errors.push(`${edition.directory} runtime execution validator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const launcherExecutionValidatorScript = path.join(repoRoot, 'scripts', 'validate-launcher-execution-report.mjs')
  assert(errors, fileExists(launcherExecutionValidatorScript), `${edition.directory} missing scripts/validate-launcher-execution-report.mjs`)
  const launcherExecutionGeneratorScript = path.join(repoRoot, 'scripts', 'generate-launcher-execution-report.mjs')
  assert(errors, fileExists(launcherExecutionGeneratorScript), `${edition.directory} missing scripts/generate-launcher-execution-report.mjs`)
  if (fileExists(launcherExecutionGeneratorScript)) {
    const dryRun = runJson('node', [
      launcherExecutionGeneratorScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} launcher execution blocked-report generator dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        launcherExecutionDryRunReport = JSON.parse(dryRun.stdout)
        validateLauncherExecutionReport(errors, {
          report: launcherExecutionDryRunReport,
          edition,
          moduleArtifactPath,
          launcherFlowIds,
          outputSuffixLabel: null,
        })
      } catch (error) {
        errors.push(`${edition.directory} launcher execution generator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }
  validateHarnessRunnerDryRun(errors, {
    scriptPath: path.join(repoRoot, 'scripts', 'run-launcher-execution-harness.mjs'),
    moduleRoot,
    repoRoot,
    edition,
    schema: 'echo.openlands.edition.launcher_execution_report.v1',
    harnessType: 'launcher',
    readyField: 'publicAlphaReady',
    clearedGateField: 'clearedLauncherGates',
    remainingGateField: 'remainingLauncherGates',
    minimumRemainingGates: 5,
  })
  if (fileExists(launcherExecutionValidatorScript)) {
    const dryRun = runJson('node', [
      launcherExecutionValidatorScript,
      '--module-root',
      moduleRoot,
      '--allow-missing',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} launcher execution validator allow-missing check failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        const result = JSON.parse(dryRun.stdout)
        assert(errors, ['missing', 'passed'].includes(result.status), `${edition.directory} launcher execution validator status must be missing or passed`)
        assert(errors, result.runtimeTarget === edition.runtimeTarget, `${edition.directory} launcher execution validator target mismatch`)
        if (result.status === 'missing') {
          assert(errors, result.expectedFlowCount === launcherFlowIds.length, `${edition.directory} launcher execution validator flow count mismatch`)
          assert(errors, result.expectedLauncherGateCount >= 5, `${edition.directory} launcher execution validator gate count too low`)
        } else {
          assert(errors, ['blocked', 'passed'].includes(result.reportStatus), `${edition.directory} launcher execution report status must be blocked or passed`)
          if (result.reportStatus === 'blocked') {
            assert(errors, result.publicAlphaReady === false, `${edition.directory} blocked launcher execution report must not mark public alpha ready`)
            assert(errors, result.clearedLauncherGates === 0, `${edition.directory} blocked launcher execution report must clear zero gates`)
            assert(errors, result.remainingLauncherGates >= 5, `${edition.directory} blocked launcher execution report must keep launcher gates remaining`)
          }
        }
      } catch (error) {
        errors.push(`${edition.directory} launcher execution validator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const finalReviewValidatorScript = path.join(repoRoot, 'scripts', 'validate-final-release-review-report.mjs')
  assert(errors, fileExists(finalReviewValidatorScript), `${edition.directory} missing scripts/validate-final-release-review-report.mjs`)
  const finalReviewGeneratorScript = path.join(repoRoot, 'scripts', 'generate-final-release-review-report.mjs')
  assert(errors, fileExists(finalReviewGeneratorScript), `${edition.directory} missing scripts/generate-final-release-review-report.mjs`)
  if (fileExists(finalReviewGeneratorScript)) {
    const dryRun = runJson('node', [
      finalReviewGeneratorScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} final release review blocked-report generator dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        finalReleaseReviewDryRunReport = JSON.parse(dryRun.stdout)
        validateFinalReleaseReviewReport(errors, {
          report: finalReleaseReviewDryRunReport,
          edition,
          finalReviewContracts,
          outputSuffixLabel: null,
        })
      } catch (error) {
        errors.push(`${edition.directory} final review generator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }
  validateHarnessRunnerDryRun(errors, {
    scriptPath: path.join(repoRoot, 'scripts', 'run-final-release-review-harness.mjs'),
    moduleRoot,
    repoRoot,
    edition,
    schema: 'echo.openlands.edition.final_release_review_report.v1',
    harnessType: 'finalReview',
    readyField: 'publicReleaseReady',
    clearedGateField: 'clearedFinalReviewGates',
    remainingGateField: 'remainingFinalReviewGates',
    minimumRemainingGates: 2,
  })
  if (fileExists(finalReviewValidatorScript)) {
    const dryRun = runJson('node', [
      finalReviewValidatorScript,
      '--module-root',
      moduleRoot,
      '--allow-missing',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} final review validator allow-missing check failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        const result = JSON.parse(dryRun.stdout)
        assert(errors, ['missing', 'passed'].includes(result.status), `${edition.directory} final review validator status must be missing or passed`)
        assert(errors, result.runtimeTarget === edition.runtimeTarget, `${edition.directory} final review validator target mismatch`)
        if (result.status === 'missing') {
          assert(errors, result.expectedReviewAreaCount >= 5, `${edition.directory} final review validator review area count too low`)
          assert(errors, result.expectedFinalReviewGateCount >= 2, `${edition.directory} final review validator gate count too low`)
        } else {
          assert(errors, ['blocked', 'passed'].includes(result.reportStatus), `${edition.directory} final review report status must be blocked or passed`)
          if (result.reportStatus === 'blocked') {
            assert(errors, result.publicReleaseReady === false, `${edition.directory} blocked final review report must not mark public release ready`)
            assert(errors, result.clearedFinalReviewGates === 0, `${edition.directory} blocked final review report must clear zero gates`)
            assert(errors, result.remainingFinalReviewGates >= 2, `${edition.directory} blocked final review report must keep final review gates remaining`)
          }
        }
      } catch (error) {
        errors.push(`${edition.directory} final review validator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  const distributionApprovalValidatorScript = path.join(repoRoot, 'scripts', 'validate-distribution-approval-report.mjs')
  assert(errors, fileExists(distributionApprovalValidatorScript), `${edition.directory} missing scripts/validate-distribution-approval-report.mjs`)
  const distributionApprovalGeneratorScript = path.join(repoRoot, 'scripts', 'generate-distribution-approval-report.mjs')
  assert(errors, fileExists(distributionApprovalGeneratorScript), `${edition.directory} missing scripts/generate-distribution-approval-report.mjs`)
  if (fileExists(distributionApprovalGeneratorScript)) {
    const dryRun = runJson('node', [
      distributionApprovalGeneratorScript,
      '--module-root',
      moduleRoot,
      '--dry-run',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} distribution approval blocked-report generator dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        distributionApprovalDryRunReport = JSON.parse(dryRun.stdout)
        validateDistributionApprovalReportAttachment(errors, {
          report: distributionApprovalDryRunReport,
          edition,
          distributionApprovalContracts: distributionApprovalAttachmentContracts,
          outputSuffixLabel: null,
        })
      } catch (error) {
        errors.push(`${edition.directory} distribution approval generator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }
  validateHarnessRunnerDryRun(errors, {
    scriptPath: path.join(repoRoot, 'scripts', 'run-distribution-approval-harness.mjs'),
    moduleRoot,
    repoRoot,
    edition,
    schema: 'echo.openlands.edition.distribution_approval_report.v1',
    harnessType: 'distributionApproval',
    readyField: 'publicAlphaReady',
    clearedGateField: 'clearedDistributionGates',
    remainingGateField: 'remainingDistributionGates',
    minimumRemainingGates: 5,
  })
  if (fileExists(distributionApprovalValidatorScript)) {
    const dryRun = runJson('node', [
      distributionApprovalValidatorScript,
      '--module-root',
      moduleRoot,
      '--allow-missing',
      '--json',
    ], repoRoot)
    assert(errors, dryRun.status === 0, `${edition.directory} distribution approval validator allow-missing check failed: ${dryRun.stderr || dryRun.stdout}`)
    if (dryRun.status === 0) {
      try {
        const result = JSON.parse(dryRun.stdout)
        assert(errors, ['missing', 'passed'].includes(result.status), `${edition.directory} distribution approval validator status must be missing or passed`)
        assert(errors, result.runtimeTarget === edition.runtimeTarget, `${edition.directory} distribution approval validator target mismatch`)
        if (result.status === 'missing') {
          assert(errors, result.expectedApprovalAreaCount >= 5, `${edition.directory} distribution approval validator approval area count too low`)
          assert(errors, result.expectedDistributionGateCount >= 5, `${edition.directory} distribution approval validator gate count too low`)
        } else {
          assert(errors, ['blocked', 'passed'].includes(result.reportStatus), `${edition.directory} distribution approval report status must be blocked or passed`)
          if (result.reportStatus === 'blocked') {
            assert(errors, result.publicAlphaReady === false, `${edition.directory} blocked distribution approval report must not mark public alpha ready`)
            assert(errors, result.clearedDistributionGates === 0, `${edition.directory} blocked distribution approval report must clear zero gates`)
            assert(errors, result.remainingDistributionGates >= 5, `${edition.directory} blocked distribution approval report must keep distribution gates remaining`)
          }
        }
      } catch (error) {
        errors.push(`${edition.directory} distribution approval validator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }

  if (evidenceTemplate !== null) {
    assert(errors, evidenceTemplate.schema === 'echo.openlands.edition.runtime_evidence_template.v1', `${edition.directory} evidence template schema mismatch`)
    assert(errors, evidenceTemplate.packId === edition.packId, `${edition.directory} evidence template packId must be ${edition.packId}`)
    assert(errors, evidenceTemplate.displayName === edition.displayName, `${edition.directory} evidence template displayName must be ${edition.displayName}`)
    assert(errors, evidenceTemplate.runtimeTarget === edition.runtimeTarget, `${edition.directory} evidence template runtimeTarget must be ${edition.runtimeTarget}`)
    assert(errors, evidenceTemplate.moduleId === MODULE_ID, `${edition.directory} evidence template moduleId must be ${MODULE_ID}`)
    assert(errors, evidenceTemplate.moduleVersion === VERSION, `${edition.directory} evidence template moduleVersion must be ${VERSION}`)
    assert(errors, evidenceTemplate.status === 'template', `${edition.directory} evidence template status must be template`)
    assert(errors, evidenceTemplate.requiredRuntimeEvidenceContract === RUNTIME_EVIDENCE_CONTRACT, `${edition.directory} evidence template runtime contract mismatch`)
    assert(errors, evidenceTemplate.playtestFixture === PLAYTEST_FIXTURE, `${edition.directory} evidence template playtest fixture mismatch`)
    assert(errors, evidenceTemplate.legalAuditContract === LEGAL_AUDIT_CONTRACT, `${edition.directory} evidence template legal audit contract mismatch`)
    assert(errors, evidenceTemplate.launcherFlowContract === LAUNCHER_FLOW_CONTRACT, `${edition.directory} evidence template launcher flow contract mismatch`)
    assert(errors, evidenceTemplate.playableRuntimeContract === PLAYABLE_RUNTIME_CONTRACT, `${edition.directory} evidence template playable runtime contract mismatch`)
    assert(errors, evidenceTemplate.runtimeExecutionAcceptance === RUNTIME_EXECUTION_ACCEPTANCE, `${edition.directory} evidence template runtime execution acceptance mismatch`)
    assert(errors, evidenceTemplate.runtimeExecutionHarnessPlan === RUNTIME_EXECUTION_HARNESS_PLAN, `${edition.directory} evidence template runtime execution harness plan mismatch`)
    assert(errors, evidenceTemplate.harnessDriverManifestContract === HARNESS_DRIVER_MANIFEST_CONTRACT, `${edition.directory} evidence template harness driver manifest contract mismatch`)
    assert(errors, evidenceTemplate.harnessDriverManifestTemplate === edition.harnessDriverManifestTemplate, `${edition.directory} evidence template harness driver manifest template mismatch`)
    assert(errors, evidenceTemplate.launcherExecutionAcceptance === LAUNCHER_EXECUTION_ACCEPTANCE, `${edition.directory} evidence template launcher execution acceptance mismatch`)
    assert(errors, evidenceTemplate.launcherExecutionHarnessPlan === LAUNCHER_EXECUTION_HARNESS_PLAN, `${edition.directory} evidence template launcher execution harness plan mismatch`)
    assert(errors, evidenceTemplate.finalReleaseReviewAcceptance === FINAL_RELEASE_REVIEW_ACCEPTANCE, `${edition.directory} evidence template final release review acceptance mismatch`)
    assert(errors, evidenceTemplate.finalReleaseReviewHarnessPlan === FINAL_RELEASE_REVIEW_HARNESS_PLAN, `${edition.directory} evidence template final release review harness plan mismatch`)
    assert(errors, evidenceTemplate.distributionApprovalAcceptance === DISTRIBUTION_APPROVAL_ACCEPTANCE, `${edition.directory} evidence template distribution approval acceptance mismatch`)
    assert(errors, evidenceTemplate.distributionApprovalHarnessPlan === DISTRIBUTION_APPROVAL_HARNESS_PLAN, `${edition.directory} evidence template distribution approval harness plan mismatch`)
    assert(errors, evidenceTemplate.adapterReadySignal === 'openlands_runtime_ready', `${edition.directory} evidence template adapterReadySignal mismatch`)
    assert(errors, sameStringList(evidenceTemplate.requiredAdapterLoadPhases ?? [], requiredPhases), `${edition.directory} evidence template requiredAdapterLoadPhases mismatch`)
    assert(errors, sameStringList(evidenceTemplate.requiredPublicAlphaEvidence ?? [], publicAlphaEvidence), `${edition.directory} evidence template requiredPublicAlphaEvidence mismatch`)
    assert(errors, sameStringList(evidenceTemplate.requiredRuntimeEvidenceIds ?? [], runtimeEvidenceIds), `${edition.directory} evidence template requiredRuntimeEvidenceIds mismatch`)
    assert(errors, sameStringList(evidenceTemplate.playtestScenarios ?? [], playtestScenarioIds), `${edition.directory} evidence template playtestScenarios mismatch`)
    assert(errors, sameStringList(evidenceTemplate.saveLoadCheckpoints ?? [], saveLoadCheckpointIds), `${edition.directory} evidence template saveLoadCheckpoints mismatch`)
    assert(errors, evidenceTemplate.waystonePublicAlphaScenario === waystonePublicAlphaScenario, `${edition.directory} evidence template waystonePublicAlphaScenario mismatch`)
    assert(errors, sameStringList(evidenceTemplate.launcherFlows ?? [], launcherFlowIds), `${edition.directory} evidence template launcherFlows mismatch`)
    assert(errors, Array.isArray(evidenceTemplate[edition.harnessRequirementKey]) && evidenceTemplate[edition.harnessRequirementKey].length >= 4, `${edition.directory} evidence template missing ${edition.harnessRequirementKey}`)
    const attachments = evidenceTemplate.evidenceAttachments ?? {}
    for (const key of ['adapterBootReport', 'registryParityReport', 'craftingStationReport', 'worldgenExplorationReport', 'creatureRosterReport', 'oldRoadNetworkReport', 'alphaSystemsReport', 'distributionRoadmapReport', 'distributionApprovalReport', 'runtimeCoreReport', 'runtimeExecutionReport', 'localRuntimeRehearsalReport', 'firstHourPlaytestReport', 'waystoneSaveLoadReport', 'launcherFlowReport', 'launcherExecutionReport', 'localLauncherRehearsalReport', 'legalAuditReport', 'finalReleaseReviewReport']) {
      assert(errors, typeof attachments[key] === 'string', `${edition.directory} evidence template missing attachment ${key}`)
      assert(errors, attachments[key]?.startsWith(`evidence/${edition.attachmentPrefix}-`), `${edition.directory} evidence attachment ${key} must use ${edition.attachmentPrefix} prefix`)
      assert(errors, attachments[key]?.endsWith('.json'), `${edition.directory} evidence attachment ${key} must be a json path`)
    }
    if (typeof attachments.adapterBootReport === 'string') {
      const adapterBootReportPath = path.join(repoRoot, attachments.adapterBootReport)
      assert(errors, fileExists(adapterBootReportPath), `${edition.directory} missing adapter boot report ${attachments.adapterBootReport}`)
      if (fileExists(adapterBootReportPath)) {
        validateAdapterBootReport(errors, readJson(adapterBootReportPath), edition, runtimePlan, requiredPhases, runtimeEvidenceIds, publicAlphaEvidence, false, attachments.adapterBootReport, adapterBootDryRunReport)
      }
    }
    if (typeof attachments.registryParityReport === 'string') {
      const registryParityReportPath = path.join(repoRoot, attachments.registryParityReport)
      assert(errors, fileExists(registryParityReportPath), `${edition.directory} missing registry parity report ${attachments.registryParityReport}`)
      if (fileExists(registryParityReportPath)) {
        validateRegistryParityReport(errors, readJson(registryParityReportPath), edition, registryParityContracts, false, attachments.registryParityReport, registryParityDryRunReport)
      }
    }
    if (typeof attachments.craftingStationReport === 'string') {
      const craftingStationReportPath = path.join(repoRoot, attachments.craftingStationReport)
      assert(errors, fileExists(craftingStationReportPath), `${edition.directory} missing crafting station report ${attachments.craftingStationReport}`)
      if (fileExists(craftingStationReportPath)) {
        validateCraftingStationReport(errors, readJson(craftingStationReportPath), edition, craftingContracts, false, attachments.craftingStationReport, craftingStationDryRunReport)
      }
    }
    if (typeof attachments.worldgenExplorationReport === 'string') {
      const worldgenExplorationReportPath = path.join(repoRoot, attachments.worldgenExplorationReport)
      assert(errors, fileExists(worldgenExplorationReportPath), `${edition.directory} missing worldgen exploration report ${attachments.worldgenExplorationReport}`)
      if (fileExists(worldgenExplorationReportPath)) {
        validateWorldgenExplorationReport(errors, readJson(worldgenExplorationReportPath), edition, worldgenContracts, false, attachments.worldgenExplorationReport, worldgenExplorationDryRunReport)
      }
    }
    if (typeof attachments.creatureRosterReport === 'string') {
      const creatureRosterReportPath = path.join(repoRoot, attachments.creatureRosterReport)
      assert(errors, fileExists(creatureRosterReportPath), `${edition.directory} missing creature roster report ${attachments.creatureRosterReport}`)
      if (fileExists(creatureRosterReportPath)) {
        validateCreatureRosterReport(errors, readJson(creatureRosterReportPath), edition, creatureRosterContracts, false, attachments.creatureRosterReport, creatureRosterDryRunReport)
      }
    }
    if (typeof attachments.oldRoadNetworkReport === 'string') {
      const oldRoadNetworkReportPath = path.join(repoRoot, attachments.oldRoadNetworkReport)
      assert(errors, fileExists(oldRoadNetworkReportPath), `${edition.directory} missing old road network report ${attachments.oldRoadNetworkReport}`)
      if (fileExists(oldRoadNetworkReportPath)) {
        validateOldRoadNetworkReport(errors, readJson(oldRoadNetworkReportPath), edition, oldRoadNetworkContracts, false, attachments.oldRoadNetworkReport, oldRoadNetworkDryRunReport)
      }
    }
    if (typeof attachments.alphaSystemsReport === 'string') {
      const alphaSystemsReportPath = path.join(repoRoot, attachments.alphaSystemsReport)
      assert(errors, fileExists(alphaSystemsReportPath), `${edition.directory} missing alpha systems report ${attachments.alphaSystemsReport}`)
      if (fileExists(alphaSystemsReportPath)) {
        validateAlphaSystemsReport(errors, readJson(alphaSystemsReportPath), edition, alphaSystemsContracts, false, attachments.alphaSystemsReport, alphaSystemsDryRunReport)
      }
    }
    if (typeof attachments.distributionRoadmapReport === 'string') {
      const distributionRoadmapReportPath = path.join(repoRoot, attachments.distributionRoadmapReport)
      assert(errors, fileExists(distributionRoadmapReportPath), `${edition.directory} missing distribution roadmap report ${attachments.distributionRoadmapReport}`)
      if (fileExists(distributionRoadmapReportPath)) {
        validateDistributionRoadmapReport(errors, readJson(distributionRoadmapReportPath), edition, distributionRoadmapContractsForEdition, false, attachments.distributionRoadmapReport, distributionRoadmapDryRunReport)
      }
    }
    if (typeof attachments.distributionApprovalReport === 'string') {
      const distributionApprovalReportPath = path.join(repoRoot, attachments.distributionApprovalReport)
      assert(errors, fileExists(distributionApprovalReportPath), `${edition.directory} missing distribution approval report ${attachments.distributionApprovalReport}`)
      if (fileExists(distributionApprovalReportPath)) {
        validateDistributionApprovalReportAttachment(errors, {
          report: readJson(distributionApprovalReportPath),
          edition,
          distributionApprovalContracts: distributionApprovalAttachmentContracts,
          outputSuffixLabel: attachments.distributionApprovalReport,
          expectedFreshReport: distributionApprovalDryRunReport,
        })
      }
    }
    if (typeof attachments.runtimeCoreReport === 'string') {
      const runtimeCoreReportPath = path.join(repoRoot, attachments.runtimeCoreReport)
      assert(errors, fileExists(runtimeCoreReportPath), `${edition.directory} missing runtime core report ${attachments.runtimeCoreReport}`)
      if (fileExists(runtimeCoreReportPath)) {
        validateRuntimeCoreReport(errors, {
          report: readJson(runtimeCoreReportPath),
          edition,
          expectedDryRun: false,
          outputSuffixLabel: attachments.runtimeCoreReport,
          moduleArtifactPath,
          expectedFreshReport: runtimeCoreDryRunReport,
        })
      }
    }
    if (typeof attachments.runtimeExecutionReport === 'string') {
      const runtimeExecutionReportPath = path.join(repoRoot, attachments.runtimeExecutionReport)
      assert(errors, fileExists(runtimeExecutionReportPath), `${edition.directory} missing runtime execution report ${attachments.runtimeExecutionReport}`)
      if (fileExists(runtimeExecutionReportPath)) {
        validateRuntimeExecutionReport(errors, {
          report: readJson(runtimeExecutionReportPath),
          edition,
          moduleArtifactPath,
          runtimeScenarioIds,
          outputSuffixLabel: attachments.runtimeExecutionReport,
          expectedFreshReport: runtimeExecutionDryRunReport,
        })
      }
    }
    if (typeof attachments.localRuntimeRehearsalReport === 'string') {
      const localRuntimeRehearsalReportPath = path.join(repoRoot, attachments.localRuntimeRehearsalReport)
      assert(errors, fileExists(localRuntimeRehearsalReportPath), `${edition.directory} missing local runtime rehearsal report ${attachments.localRuntimeRehearsalReport}`)
      if (fileExists(localRuntimeRehearsalReportPath)) {
        validateLocalRuntimeRehearsalReport(errors, readJson(localRuntimeRehearsalReportPath), edition, localRuntimeRehearsalContracts, false, attachments.localRuntimeRehearsalReport, localRuntimeRehearsalDryRunReport)
      }
    }
    if (typeof attachments.launcherFlowReport === 'string') {
      const launcherFlowReportPath = path.join(repoRoot, attachments.launcherFlowReport)
      assert(errors, fileExists(launcherFlowReportPath), `${edition.directory} missing launcher flow report ${attachments.launcherFlowReport}`)
      if (fileExists(launcherFlowReportPath)) {
        validateLauncherFlowReport(errors, readJson(launcherFlowReportPath), edition, launcherFlowContractsForEdition, false, attachments.launcherFlowReport, launcherFlowDryRunReport)
      }
    }
    if (typeof attachments.launcherExecutionReport === 'string') {
      const launcherExecutionReportPath = path.join(repoRoot, attachments.launcherExecutionReport)
      assert(errors, fileExists(launcherExecutionReportPath), `${edition.directory} missing launcher execution report ${attachments.launcherExecutionReport}`)
      if (fileExists(launcherExecutionReportPath)) {
        validateLauncherExecutionReport(errors, {
          report: readJson(launcherExecutionReportPath),
          edition,
          moduleArtifactPath,
          launcherFlowIds,
          outputSuffixLabel: attachments.launcherExecutionReport,
          expectedFreshReport: launcherExecutionDryRunReport,
        })
      }
    }
    if (typeof attachments.localLauncherRehearsalReport === 'string') {
      const localLauncherRehearsalReportPath = path.join(repoRoot, attachments.localLauncherRehearsalReport)
      assert(errors, fileExists(localLauncherRehearsalReportPath), `${edition.directory} missing local launcher rehearsal report ${attachments.localLauncherRehearsalReport}`)
      if (fileExists(localLauncherRehearsalReportPath)) {
        validateLocalLauncherRehearsalReport(errors, readJson(localLauncherRehearsalReportPath), edition, localLauncherRehearsalContracts, false, attachments.localLauncherRehearsalReport, localLauncherRehearsalDryRunReport)
      }
    }
    if (typeof attachments.legalAuditReport === 'string') {
      const legalAuditReportPath = path.join(repoRoot, attachments.legalAuditReport)
      assert(errors, fileExists(legalAuditReportPath), `${edition.directory} missing legal audit report ${attachments.legalAuditReport}`)
      if (fileExists(legalAuditReportPath)) {
        validateLegalAuditReport(errors, readJson(legalAuditReportPath), edition, legalAuditContracts, false, attachments.legalAuditReport, legalAuditDryRunReport)
      }
    }
    if (typeof attachments.finalReleaseReviewReport === 'string') {
      const finalReviewReportPath = path.join(repoRoot, attachments.finalReleaseReviewReport)
      assert(errors, fileExists(finalReviewReportPath), `${edition.directory} missing final release review report ${attachments.finalReleaseReviewReport}`)
      if (fileExists(finalReviewReportPath)) {
        validateFinalReleaseReviewReport(errors, {
          report: readJson(finalReviewReportPath),
          edition,
          finalReviewContracts,
          outputSuffixLabel: attachments.finalReleaseReviewReport,
          expectedFreshReport: finalReleaseReviewDryRunReport,
        })
      }
    }
    if (typeof attachments.firstHourPlaytestReport === 'string') {
      const firstHourReportPath = path.join(repoRoot, attachments.firstHourPlaytestReport)
      assert(errors, fileExists(firstHourReportPath), `${edition.directory} missing first-hour playtest report ${attachments.firstHourPlaytestReport}`)
      if (fileExists(firstHourReportPath)) {
        validateFirstHourPlaytestReport(errors, readJson(firstHourReportPath), edition, firstHourContracts, false, attachments.firstHourPlaytestReport, firstHourPlaytestDryRunReport)
      }
    }
    if (typeof attachments.waystoneSaveLoadReport === 'string') {
      const waystoneReportPath = path.join(repoRoot, attachments.waystoneSaveLoadReport)
      assert(errors, fileExists(waystoneReportPath), `${edition.directory} missing waystone save/load report ${attachments.waystoneSaveLoadReport}`)
      if (fileExists(waystoneReportPath)) {
        validateWaystoneSaveLoadReport(errors, readJson(waystoneReportPath), edition, waystoneSaveLoadContracts, false, attachments.waystoneSaveLoadReport, waystoneSaveLoadDryRunReport)
      }
    }
  }

  return { key: edition.key, repoRoot, errors, warnings }
}

function validate({ moduleRoot, workspaceRoot, readinessReportPath = null }) {
  const errors = []
  const warnings = []
  const runtimePlanPath = path.join(moduleRoot, 'src', 'main', 'resources', RUNTIME_EVIDENCE_CONTRACT)
  const runtimeExecutionPath = path.join(moduleRoot, 'src', 'main', 'resources', RUNTIME_EXECUTION_ACCEPTANCE)
  const playtestFixturePath = path.join(moduleRoot, 'src', 'main', 'resources', PLAYTEST_FIXTURE)
  const legalAuditPath = path.join(moduleRoot, 'src', 'main', 'resources', LEGAL_AUDIT_CONTRACT)
  const contentPolicyPath = path.join(moduleRoot, 'src', 'main', 'resources', CONTENT_POLICY_CONTRACT)
  const assetManifestPath = path.join(moduleRoot, 'src', 'main', 'resources', ASSET_MANIFEST_CONTRACT)
  const launcherFlowPath = path.join(moduleRoot, 'src', 'main', 'resources', LAUNCHER_FLOW_CONTRACT)
  const launcherExecutionPath = path.join(moduleRoot, 'src', 'main', 'resources', LAUNCHER_EXECUTION_ACCEPTANCE)
  const finalReleaseReviewPath = path.join(moduleRoot, 'src', 'main', 'resources', FINAL_RELEASE_REVIEW_ACCEPTANCE)
  const waystoneContractPath = path.join(moduleRoot, 'src', 'main', 'resources', WAYSTONE_CONTRACT)
  const holomapContractPath = path.join(moduleRoot, 'src', 'main', 'resources', HOLOMAP_CONTRACT)
  const conformancePath = path.join(moduleRoot, 'src', 'main', 'resources', CONFORMANCE_REGISTRY)
  const crossPlatformParityPath = path.join(moduleRoot, 'src', 'main', 'resources', CROSS_PLATFORM_PARITY_CONTRACT)
  const firstHourRoutePath = path.join(moduleRoot, 'src', 'main', 'resources', FIRST_HOUR_ROUTE_CONTRACT)
  const gameModesPath = path.join(moduleRoot, 'src', 'main', 'resources', GAME_MODES_CONTRACT)
  const blocksPath = path.join(moduleRoot, 'src', 'main', 'resources', BLOCKS_CONTRACT)
  const itemsPath = path.join(moduleRoot, 'src', 'main', 'resources', ITEMS_CONTRACT)
  const recipesPath = path.join(moduleRoot, 'src', 'main', 'resources', RECIPES_CONTRACT)
  const biomesPath = path.join(moduleRoot, 'src', 'main', 'resources', BIOMES_CONTRACT)
  const structuresPath = path.join(moduleRoot, 'src', 'main', 'resources', STRUCTURES_CONTRACT)
  const creaturesPath = path.join(moduleRoot, 'src', 'main', 'resources', CREATURES_CONTRACT)
  const tagsPath = path.join(moduleRoot, 'src', 'main', 'resources', TAGS_CONTRACT)
  const lootPath = path.join(moduleRoot, 'src', 'main', 'resources', LOOT_CONTRACT)
  const aliasBridgePath = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands', 'foundation', 'foundation_alias_bridge.json')
  const tutorialsPath = path.join(moduleRoot, 'src', 'main', 'resources', TUTORIALS_CONTRACT)
  const soundsPath = path.join(moduleRoot, 'src', 'main', 'resources', SOUNDS_ASSET_CONTRACT)
  const homesteadAlphaPath = path.join(moduleRoot, 'src', 'main', 'resources', HOMESTEAD_ALPHA_CONTRACT)
  const builderUxAlphaPath = path.join(moduleRoot, 'src', 'main', 'resources', BUILDER_UX_ALPHA_CONTRACT)
  const coopSmpPath = path.join(moduleRoot, 'src', 'main', 'resources', COOP_SMP_CONTRACT)
  const distributionAlphaGatesPath = path.join(moduleRoot, 'src', 'main', 'resources', DISTRIBUTION_ALPHA_GATES_CONTRACT)
  const distributionApprovalPath = path.join(moduleRoot, 'src', 'main', 'resources', DISTRIBUTION_APPROVAL_ACCEPTANCE)
  const releasePublicationManifestPath = path.join(moduleRoot, 'src', 'main', 'resources', RELEASE_PUBLICATION_MANIFEST_CONTRACT)
  const launchRoadmapPath = path.join(moduleRoot, 'src', 'main', 'resources', LAUNCH_ROADMAP_CONTRACT)
  const harnessDriverManifestPath = path.join(moduleRoot, 'src', 'main', 'resources', HARNESS_DRIVER_MANIFEST_CONTRACT)
  const releaseRoot = path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseReadinessGeneratorPath = path.join(moduleRoot, 'scripts', 'generate-openlands-release-readiness-report.mjs')
  const releasePublicationManifestValidatorPath = path.join(moduleRoot, 'scripts', 'validate-openlands-release-publication-manifest.mjs')
  const releasePublicationUrlMapValidatorPath = path.join(moduleRoot, 'scripts', 'validate-openlands-publication-url-map.mjs')
  const releasePublicationUrlMapTemplatePath = path.join(releaseRoot, MODULE_ID, 'openlands-publication-url-map.template.json')
  const runtimePlan = readJson(runtimePlanPath)
  const runtimeExecution = readJson(runtimeExecutionPath)
  const playtestFixture = readJson(playtestFixturePath)
  const launcherFlow = readJson(launcherFlowPath)
  const launcherExecution = readJson(launcherExecutionPath)
  const finalReleaseReview = readJson(finalReleaseReviewPath)
  const waystoneContract = readJson(waystoneContractPath)
  const holomapContract = readJson(holomapContractPath)
  const registryParityContracts = {
    conformance: readJson(conformancePath),
    aliasBridge: readJson(aliasBridgePath),
    crossPlatformParity: readJson(crossPlatformParityPath),
    firstHourRoute: readJson(firstHourRoutePath),
    gameModes: readJson(gameModesPath),
    blocks: readJson(blocksPath),
    items: readJson(itemsPath),
    recipes: readJson(recipesPath),
    biomes: readJson(biomesPath),
    structures: readJson(structuresPath),
    waystoneContract,
  }
  const craftingContracts = {
    conformance: registryParityContracts.conformance,
    aliasBridge: registryParityContracts.aliasBridge,
    runtimePlan,
    blocks: registryParityContracts.blocks,
    items: registryParityContracts.items,
    recipes: registryParityContracts.recipes,
  }
  const worldgenContracts = {
    conformance: registryParityContracts.conformance,
    aliasBridge: registryParityContracts.aliasBridge,
    runtimePlan,
    blocks: registryParityContracts.blocks,
    items: registryParityContracts.items,
    biomes: registryParityContracts.biomes,
    structures: registryParityContracts.structures,
    creatures: readJson(creaturesPath),
    tags: readJson(tagsPath),
    loot: readJson(lootPath),
    tutorials: readJson(tutorialsPath),
    holomap: holomapContract,
    sounds: readJson(soundsPath),
  }
  const creatureRosterContracts = {
    conformance: registryParityContracts.conformance,
    aliasBridge: registryParityContracts.aliasBridge,
    runtimePlan,
    creatures: worldgenContracts.creatures,
    loot: worldgenContracts.loot,
    biomes: registryParityContracts.biomes,
    items: registryParityContracts.items,
    tags: worldgenContracts.tags,
    playtestFixture,
    sounds: worldgenContracts.sounds,
  }
  const oldRoadNetworkContracts = {
    runtimePlan,
    blocks: registryParityContracts.blocks,
    items: registryParityContracts.items,
    recipes: registryParityContracts.recipes,
    structures: registryParityContracts.structures,
    waystoneContract,
    holomapContract,
    playtestFixture,
  }
  const alphaSystemsContracts = {
    homestead: readJson(homesteadAlphaPath),
    builderUx: readJson(builderUxAlphaPath),
    coopSmp: readJson(coopSmpPath),
    distribution: readJson(distributionAlphaGatesPath),
    conformance: registryParityContracts.conformance,
    aliasBridge: registryParityContracts.aliasBridge,
    blocks: registryParityContracts.blocks,
    items: registryParityContracts.items,
    tags: worldgenContracts.tags,
    creatures: worldgenContracts.creatures,
    biomes: registryParityContracts.biomes,
  }
  const harnessDriverManifestContract = readJson(harnessDriverManifestPath)
  const publicAlphaGate = (runtimePlan.acceptanceGates ?? []).find((gate) => gate.id === 'public_alpha')
  assert(errors, publicAlphaGate !== undefined, 'runtime adapter load plan must define public_alpha acceptance gate')
  const requiredPhases = publicAlphaGate?.requiresPhases ?? []
  const publicAlphaEvidence = publicAlphaGate?.requiresEvidence ?? []
  const runtimeEvidenceIds = (runtimePlan.runtimeEvidenceRequirements ?? []).map((evidence) => evidence.id)
  const runtimeScenarioIds = (runtimeExecution.scenarios ?? []).map((scenario) => scenario.id)
  const playtestScenarioIds = (playtestFixture.acceptanceScenarios ?? []).map((scenario) => scenario.id)
  const saveLoadCheckpointIds = (playtestFixture.saveLoadCheckpoints ?? []).map((checkpoint) => checkpoint.id)
  const waystonePublicAlphaScenario = playtestFixture.waystonePublicAlphaScenario?.id
  const launcherFlowIds = (launcherFlow.requiredLauncherFlows ?? []).map((flow) => flow.id)
  const finalReviewAreaIds = (finalReleaseReview.reviewAreas ?? []).map((area) => area.id)
  const finalReviewGateIds = (finalReleaseReview.finalReviewGates ?? []).map((gate) => gate.id).sort()
  const releaseIndex = readJson(releaseIndexPath)
  const releasePublicationManifestContract = readJson(releasePublicationManifestPath)
  const releasePublicationArtifactFiles = (releasePublicationManifestContract.artifactTargets ?? []).map((target) => target.file)
  const releasePublicationArtifactCount = releasePublicationArtifactFiles.length
  const distributionRoadmapContracts = {
    distribution: alphaSystemsContracts.distribution,
    distributionApproval: readJson(distributionApprovalPath),
    launchRoadmap: readJson(launchRoadmapPath),
    launcherFlow,
    crossPlatformParity: registryParityContracts.crossPlatformParity,
    conformance: registryParityContracts.conformance,
    releaseIndex,
    releaseRoot,
    publicAlphaEvidence,
  }
  const launcherFlowContracts = {
    launcherFlow,
    distribution: alphaSystemsContracts.distribution,
    runtimePlan,
    releaseIndex,
    releaseRoot,
  }
  const localLauncherRehearsalContracts = {
    launcherFlow,
    launcherExecution,
    releaseIndex,
    releaseRoot,
  }
  const localRuntimeRehearsalContracts = {
    runtimeExecution,
    releaseIndex,
    releaseRoot,
    moduleRoot,
  }
  const legalAuditContracts = {
    legalAudit: readJson(legalAuditPath),
    contentPolicy: readJson(contentPolicyPath),
    assetManifest: readJson(assetManifestPath),
    blocks: registryParityContracts.blocks,
    items: registryParityContracts.items,
    recipes: registryParityContracts.recipes,
    moduleRoot,
    releaseRoot,
  }

  for (const edition of EDITIONS) {
    const repoRoot = path.join(workspaceRoot, edition.directory)
    const result = validateEdition({
      edition,
      repoRoot,
      moduleRoot,
      runtimePlan,
      requiredPhases,
      publicAlphaEvidence,
      runtimeEvidenceIds,
      runtimeScenarioIds,
      registryParityContracts,
      craftingContracts,
      worldgenContracts,
      creatureRosterContracts,
      oldRoadNetworkContracts,
      alphaSystemsContracts,
      distributionRoadmapContracts,
      launcherFlowContracts,
      localLauncherRehearsalContracts,
      localRuntimeRehearsalContracts,
      legalAuditContracts,
      finalReleaseReview,
      playtestFixture,
      playtestScenarioIds,
      saveLoadCheckpointIds,
      waystonePublicAlphaScenario,
      waystoneContract,
      holomapContract,
      launcherFlowIds,
      finalReviewAreaIds,
      finalReviewGateIds,
      harnessDriverManifestContract,
    })
    for (const error of result.errors) errors.push(error)
    for (const warning of result.warnings) warnings.push(warning)
  }

  const resolvedReadinessReportPath = readinessReportPath ?? path.join(workspaceRoot, 'ECHO-Modules', 'dist', 'echo-module-release', MODULE_ID, 'openlands-release-readiness-report.json')
  assert(errors, fileExists(resolvedReadinessReportPath), `missing Openlands release readiness report ${resolvedReadinessReportPath}`)
  if (fileExists(resolvedReadinessReportPath)) {
    const readinessReport = readJson(resolvedReadinessReportPath)
    assert(errors, fileExists(releaseReadinessGeneratorPath), `missing Openlands release readiness generator ${releaseReadinessGeneratorPath}`)
    let expectedFreshReadinessReport = null
    if (fileExists(releaseReadinessGeneratorPath)) {
      const dryRun = runJson('node', [
        releaseReadinessGeneratorPath,
        '--module-root',
        moduleRoot,
        '--workspace-root',
        workspaceRoot,
        '--release-root',
        releaseRoot,
        '--out',
        resolvedReadinessReportPath,
        '--dry-run',
        '--json',
      ], path.resolve(moduleRoot, '..', '..'))
      assert(errors, dryRun.status === 0, `Openlands release readiness generator dry-run failed: ${dryRun.stderr || dryRun.stdout}`)
      if (dryRun.status === 0) {
        try {
          expectedFreshReadinessReport = JSON.parse(dryRun.stdout)
        } catch (error) {
          errors.push(`Openlands release readiness generator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
        }
      }
    }
    const readinessChecks = readinessReport.readinessChecks ?? {}
    const reportedReleasePublicationManifestPath = resolveReleaseRootPath(readinessReport.releasePublication?.manifestPath, releaseRoot)
    const readinessArtifactResults = readinessReport.artifactResults ?? []
    const readinessEditionResults = readinessReport.editionResults ?? []
    const expectedPublicAlphaReady = everyReadinessCheckPassed(readinessChecks)
    const allArtifactsExist = readinessArtifactResults.length === releasePublicationArtifactCount && readinessArtifactResults.every((artifact) =>
      artifact.exists === true
      && artifact.releaseIndexEntryPresent === true
      && artifact.sha256Present === true
      && artifact.sha256MatchesFile === true
      && artifact.sizeMatchesFile === true)
    const allArtifactUrlsPresent = readinessArtifactResults.length === releasePublicationArtifactCount && readinessArtifactResults.every((artifact) =>
      artifact.downloadUrlPresent === true)
    const allRuntimeReportsPresent = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.reportsPresent?.runtimeExecution === true)
    const allLocalRuntimeRehearsalsPresent = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.reportsPresent?.localRuntimeRehearsal === true)
    const allLocalRuntimeRehearsalsPassed = allLocalRuntimeRehearsalsPresent && readinessEditionResults.every((edition) =>
      edition.localRuntimeRehearsal?.status === readinessReport.localRuntimeRehearsal?.requiredStatus
      && edition.localRuntimeRehearsal?.scenarioCount === readinessReport.localRuntimeRehearsal?.requiredScenarioCount
      && edition.localRuntimeRehearsal?.rehearsalOnly === true
      && edition.localRuntimeRehearsal?.clearsRuntimeGates === false
      && edition.localRuntimeRehearsal?.publicAlphaReady === false)
    const allRuntimeGatesCleared = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.runtimeExecution?.clearedRuntimeGates === readinessReport.runtimeExecution?.gateCount
      && edition.runtimeExecution?.remainingRuntimeGates === 0)
    const allLauncherReportsPresent = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.reportsPresent?.launcherExecution === true)
    const allLocalLauncherRehearsalsPresent = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.reportsPresent?.localLauncherRehearsal === true)
    const allLocalLauncherRehearsalsPassed = allLocalLauncherRehearsalsPresent && readinessEditionResults.every((edition) =>
      edition.localLauncherRehearsal?.status === readinessReport.localLauncherRehearsal?.requiredStatus
      && edition.localLauncherRehearsal?.flowCount === readinessReport.localLauncherRehearsal?.requiredFlowCount
      && edition.localLauncherRehearsal?.rehearsalOnly === true
      && edition.localLauncherRehearsal?.clearsLauncherGates === false
      && edition.localLauncherRehearsal?.publicAlphaReady === false)
    const allLauncherGatesCleared = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.launcherExecution?.clearedLauncherGates === readinessReport.launcherExecution?.gateCount
      && edition.launcherExecution?.remainingLauncherGates === 0)
    const allLauncherReady = allLauncherReportsPresent && allLauncherGatesCleared && readinessEditionResults.every((edition) =>
      edition.launcherExecution?.publicAlphaReady === true)
    const allLegalPreflightReportsPresent = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.reportsPresent?.legal === true)
    const allFinalReviewReportsPresent = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.reportsPresent?.finalReview === true)
    const allFinalReviewGatesCleared = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.finalReview?.clearedFinalReviewGates === readinessReport.finalReleaseReview?.gateCount
      && edition.finalReview?.remainingFinalReviewGates === 0)
    const allLegalReady = allLegalPreflightReportsPresent && allFinalReviewReportsPresent && allFinalReviewGatesCleared && readinessEditionResults.every((edition) =>
      edition.finalReview?.publicReleaseReady === true)
    const allDistributionApprovalReportsPresent = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.reportsPresent?.distributionApproval === true)
    const allDistributionGatesCleared = readinessEditionResults.length === EDITIONS.length && readinessEditionResults.every((edition) =>
      edition.distributionApproval?.clearedDistributionGates === readinessReport.distributionApproval?.gateCount
      && edition.distributionApproval?.remainingDistributionGates === 0)
    const allDistributionReady = allDistributionApprovalReportsPresent && allDistributionGatesCleared && readinessEditionResults.every((edition) =>
      edition.distributionApproval?.publicAlphaReady === true)
    assert(errors, readinessReport.schema === 'echo.openlands.release_readiness_report.v1', 'release readiness report schema mismatch')
    assert(errors, readinessReport.moduleId === MODULE_ID, 'release readiness report moduleId mismatch')
    assert(errors, readinessReport.moduleVersion === VERSION, 'release readiness report version mismatch')
    if (expectedFreshReadinessReport) {
      assert(errors, sameJson(stableGeneratorReport(readinessReport), stableGeneratorReport(expectedFreshReadinessReport)), 'release readiness report stale against generator dry-run')
    }
    assert(errors, readinessReport.publicAlphaReady === expectedPublicAlphaReady, 'release readiness publicAlphaReady must match readiness checks')
    if (expectedPublicAlphaReady) {
      assert(errors, readinessReport.status === 'ready', 'ready release readiness report must have ready status')
      assert(errors, Array.isArray(readinessReport.blockers) && readinessReport.blockers.length === 0, 'ready release readiness report must have no blockers')
    } else {
      assert(errors, readinessReport.status === 'blocked', 'non-ready release readiness report must stay blocked')
      assert(errors, Array.isArray(readinessReport.blockers) && readinessReport.blockers.length > 0, 'blocked release readiness report must list blockers')
    }
    assert(errors, readinessReport.releasePublication?.contract === RELEASE_PUBLICATION_MANIFEST_CONTRACT, 'release readiness publication contract mismatch')
    assert(errors, ['template', 'verified', 'approved'].includes(readinessReport.releasePublication?.manifestSource), 'release readiness publication manifest source mismatch')
    assert(errors, releasePublicationManifestContract.reportContract?.allowedStatus?.includes(readinessReport.releasePublication?.manifestStatus), 'release readiness publication status is not allowed')
    assert(errors, readinessReport.releasePublication?.manifestPresent === true, 'release readiness must include selected publication manifest')
    assert(errors, readinessReport.releasePublication?.artifactCount === releasePublicationArtifactCount, 'release readiness publication manifest artifact count mismatch')
    assert(errors, sameStringSet(readinessReport.releasePublication?.expectedArtifactFiles, releasePublicationArtifactFiles), 'release readiness publication expected artifact files mismatch')
    assert(errors, sameStringSet(readinessReport.releasePublication?.actualArtifactFiles, releasePublicationArtifactFiles), 'release readiness publication actual artifact files mismatch')
    assert(errors, reportedReleasePublicationManifestPath !== null && fileExists(reportedReleasePublicationManifestPath), `release readiness selected publication manifest not found: ${reportedReleasePublicationManifestPath ?? readinessReport.releasePublication?.manifestPath}`)
    assert(errors, fileExists(releasePublicationManifestValidatorPath), `missing Openlands release publication manifest validator ${releasePublicationManifestValidatorPath}`)
    if (reportedReleasePublicationManifestPath && fileExists(reportedReleasePublicationManifestPath) && fileExists(releasePublicationManifestValidatorPath)) {
      const validation = runJson('node', [
        releasePublicationManifestValidatorPath,
        '--module-root',
        moduleRoot,
        '--workspace-root',
        workspaceRoot,
        '--release-root',
        releaseRoot,
        '--manifest',
        reportedReleasePublicationManifestPath,
        '--json',
      ], path.resolve(moduleRoot, '..', '..'))
      let publicationManifestValidation = null
      if (validation.stdout) {
        try {
          publicationManifestValidation = JSON.parse(validation.stdout)
        } catch (error) {
          errors.push(`release publication manifest validator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
        }
      }
      if (!publicationManifestValidation) {
        if (validation.status !== 0) {
          errors.push(`release publication manifest validator failed: ${validation.stderr || validation.stdout}`)
        } else {
          errors.push('release publication manifest validator did not output JSON')
        }
      } else if (validation.status !== 0 || publicationManifestValidation.status !== 'passed') {
        errors.push('release publication manifest validator failed')
        for (const error of publicationManifestValidation.errors ?? []) errors.push(`release publication manifest: ${error}`)
      }
    }
    assert(errors, fileExists(releasePublicationUrlMapValidatorPath), `missing Openlands publication URL map validator ${releasePublicationUrlMapValidatorPath}`)
    assert(errors, fileExists(releasePublicationUrlMapTemplatePath), `missing Openlands publication URL map template ${releasePublicationUrlMapTemplatePath}`)
    if (reportedReleasePublicationManifestPath && fileExists(reportedReleasePublicationManifestPath) && fileExists(releasePublicationUrlMapValidatorPath) && fileExists(releasePublicationUrlMapTemplatePath)) {
      const validation = runJson('node', [
        releasePublicationUrlMapValidatorPath,
        '--module-root',
        moduleRoot,
        '--release-root',
        releaseRoot,
        '--manifest',
        reportedReleasePublicationManifestPath,
        '--url-map',
        releasePublicationUrlMapTemplatePath,
        '--json',
      ], path.resolve(moduleRoot, '..', '..'))
      let publicationUrlMapValidation = null
      if (validation.stdout) {
        try {
          publicationUrlMapValidation = JSON.parse(validation.stdout)
        } catch (error) {
          errors.push(`publication URL map validator did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
        }
      }
      if (!publicationUrlMapValidation) {
        if (validation.status !== 0) {
          errors.push(`publication URL map validator failed: ${validation.stderr || validation.stdout}`)
        } else {
          errors.push('publication URL map validator did not output JSON')
        }
      } else if (validation.status !== 0 || publicationUrlMapValidation.status !== 'passed') {
        errors.push('publication URL map validator failed')
        for (const error of publicationUrlMapValidation.errors ?? []) errors.push(`publication URL map: ${error}`)
      } else {
        assert(errors, publicationUrlMapValidation.artifactCount === releasePublicationArtifactCount, 'publication URL map artifact count mismatch')
        assert(errors, publicationUrlMapValidation.missingUrlCount === releasePublicationArtifactCount, 'publication URL map template must keep every downloadUrl empty')
        assert(errors, publicationUrlMapValidation.publicHttpsUrlCount === 0, 'publication URL map template must not contain public URLs')
      }
    }
    assert(errors, readinessChecks.releasePublicationManifestPresent === readinessReport.releasePublication?.manifestPresent, 'release readiness publication manifest check mismatch')
    assert(errors, readinessChecks.releasePublicationArtifactCoverageComplete === true, 'release readiness publication artifact coverage must be complete')
    const releasePublicationDownloadsVerified = readinessReport.releasePublication?.downloadVerifiedCount === releasePublicationArtifactCount
      && readinessReport.releasePublication?.missingDownloadUrlCount === 0
    assert(errors, readinessChecks.releasePublicationDownloadsVerified === releasePublicationDownloadsVerified, 'release readiness publication download verification check mismatch')
    assert(errors, readinessChecks.releasePublicationApproved === readinessReport.releasePublication?.approved, 'release readiness publication approval check mismatch')
    assert(errors, readinessChecks.allArtifactsExist === allArtifactsExist, 'release readiness artifact existence check mismatch')
    assert(errors, readinessChecks.allArtifactUrlsPresent === allArtifactUrlsPresent, 'release readiness artifact URL check mismatch')
    assert(errors, readinessChecks.allRuntimeReportsPresent === allRuntimeReportsPresent, 'release readiness runtime report presence check mismatch')
    assert(errors, readinessChecks.allLocalRuntimeRehearsalsPresent === allLocalRuntimeRehearsalsPresent, 'release readiness local runtime rehearsal presence check mismatch')
    assert(errors, readinessChecks.allLocalRuntimeRehearsalsPassed === allLocalRuntimeRehearsalsPassed, 'release readiness local runtime rehearsal pass check mismatch')
    assert(errors, readinessChecks.allRuntimeGatesCleared === allRuntimeGatesCleared, 'release readiness runtime gate check mismatch')
    assert(errors, readinessChecks.allLauncherReportsPresent === allLauncherReportsPresent, 'release readiness launcher report presence check mismatch')
    assert(errors, readinessChecks.allLocalLauncherRehearsalsPresent === allLocalLauncherRehearsalsPresent, 'release readiness local launcher rehearsal presence check mismatch')
    assert(errors, readinessChecks.allLocalLauncherRehearsalsPassed === allLocalLauncherRehearsalsPassed, 'release readiness local launcher rehearsal pass check mismatch')
    assert(errors, readinessChecks.allLauncherGatesCleared === allLauncherGatesCleared, 'release readiness launcher gate check mismatch')
    assert(errors, readinessChecks.allLauncherReady === allLauncherReady, 'release readiness launcher ready check mismatch')
    assert(errors, readinessChecks.allLegalPreflightReportsPresent === allLegalPreflightReportsPresent, 'release readiness legal preflight presence check mismatch')
    assert(errors, readinessChecks.allFinalReviewReportsPresent === allFinalReviewReportsPresent, 'release readiness final review report presence check mismatch')
    assert(errors, readinessChecks.allFinalReviewGatesCleared === allFinalReviewGatesCleared, 'release readiness final review gate check mismatch')
    assert(errors, readinessChecks.allLegalReady === allLegalReady, 'release readiness legal ready check mismatch')
    assert(errors, readinessChecks.allDistributionApprovalReportsPresent === allDistributionApprovalReportsPresent, 'release readiness distribution approval presence check mismatch')
    assert(errors, readinessChecks.allDistributionGatesCleared === allDistributionGatesCleared, 'release readiness distribution gate check mismatch')
    assert(errors, readinessChecks.allDistributionReady === allDistributionReady, 'release readiness distribution ready check mismatch')
    assert(errors, readinessReport.releasePublicationRehearsal?.reportContract === 'echo.openlands.release_publication_rehearsal_report.v1', 'release readiness publication rehearsal contract mismatch')
    assert(errors, readinessReport.releasePublicationRehearsal?.reportPresent === true, 'release readiness must include publication rehearsal report')
    assert(errors, readinessReport.releasePublicationRehearsal?.status === 'preflight_passed', 'release readiness publication rehearsal must pass')
    assert(errors, readinessReport.releasePublicationRehearsal?.artifactCount === releasePublicationArtifactCount, 'release readiness publication rehearsal artifact count mismatch')
    assert(errors, readinessReport.releasePublicationRehearsal?.localDownloadVerifiedCount === releasePublicationArtifactCount, 'release readiness publication rehearsal local download count mismatch')
    assert(errors, readinessReport.releasePublicationRehearsal?.patchPreviewCount === releasePublicationArtifactCount, 'release readiness publication rehearsal patch preview count mismatch')
    assert(errors, readinessReport.releasePublicationRehearsal?.rehearsalOnlyDoesNotClearGates === true, 'release readiness publication rehearsal must be rehearsal-only')
    assert(errors, readinessReport.releasePublicationRehearsal?.clearsDistributionGates === false, 'release readiness publication rehearsal must not clear distribution gates')
    assert(errors, readinessReport.releasePublicationRehearsal?.clearsReleasePublicationGates === false, 'release readiness publication rehearsal must not clear publication gates')
    assert(errors, readinessReport.releasePublicationRehearsal?.publicAlphaReady === false, 'release readiness publication rehearsal must not mark public alpha ready')
    assert(errors, readinessReport.releasePublicationRehearsal?.passed === true, 'release readiness publication rehearsal passed flag mismatch')
    assert(errors, readinessChecks.releasePublicationRehearsalPresent === true, 'release readiness must see publication rehearsal report')
    assert(errors, readinessChecks.releasePublicationRehearsalPassed === true, 'release readiness publication rehearsal must pass')
    assert(errors, readinessReport.editionManifestIndexPreview?.reportContract === 'echo.openlands.edition_manifest_index_preview.v1', 'release readiness edition manifest index preview contract mismatch')
    assert(errors, readinessReport.editionManifestIndexPreview?.reportPresent === true, 'release readiness must include edition manifest index preview report')
    assert(errors, ['preflight_passed', 'preflight_blocked'].includes(readinessReport.editionManifestIndexPreview?.status), 'release readiness edition manifest index preview status mismatch')
    assert(errors, readinessReport.editionManifestIndexPreview?.editionCount === EDITIONS.length, 'release readiness edition manifest index preview edition count mismatch')
    assert(errors, typeof readinessReport.editionManifestIndexPreview?.moduleRequirementResolutionPassed === 'boolean', 'release readiness edition manifest index preview module requirement flag mismatch')
    assert(errors, readinessReport.editionManifestIndexPreview?.launcherChannelListingEditionCount === EDITIONS.length, 'release readiness edition manifest index preview launcher channel count mismatch')
    assert(errors, readinessReport.editionManifestIndexPreview?.previewOnlyDoesNotClearGates === true, 'release readiness edition manifest index preview must be preview-only')
    assert(errors, readinessReport.editionManifestIndexPreview?.clearsLauncherGates === false, 'release readiness edition manifest index preview must not clear launcher gates')
    assert(errors, readinessReport.editionManifestIndexPreview?.clearsDistributionGates === false, 'release readiness edition manifest index preview must not clear distribution gates')
    assert(errors, readinessReport.editionManifestIndexPreview?.publicAlphaReady === false, 'release readiness edition manifest index preview must not mark public alpha ready')
    assert(errors, readinessReport.editionManifestIndexPreview?.passed === readinessChecks.editionManifestIndexPreviewPassed, 'release readiness edition manifest index preview passed flag mismatch')
    assert(errors, readinessChecks.editionManifestIndexPreviewPresent === true, 'release readiness must see edition manifest index preview')
    if (readinessChecks.editionManifestIndexPreviewPassed !== true) {
      assert(errors, readinessReport.blockers?.includes('edition_manifest_index_preview_failed'), 'release readiness missing edition manifest index preview failed blocker')
    }
    assert(errors, readinessReport.localRuntimeRehearsal?.reportsPresent === EDITIONS.length, 'release readiness local runtime rehearsal present count mismatch')
    assert(errors, readinessReport.localRuntimeRehearsal?.reportsPassed === EDITIONS.length, 'release readiness local runtime rehearsal passed count mismatch')
    assert(errors, readinessReport.localRuntimeRehearsal?.clearsRealRuntimeGates === false, 'release readiness local runtime rehearsal must not clear real runtime gates')
    assert(errors, readinessReport.localLauncherRehearsal?.reportsPresent === EDITIONS.length, 'release readiness local launcher rehearsal present count mismatch')
    assert(errors, readinessReport.localLauncherRehearsal?.reportsPassed === EDITIONS.length, 'release readiness local launcher rehearsal passed count mismatch')
    assert(errors, readinessReport.localLauncherRehearsal?.clearsRealLauncherGates === false, 'release readiness local launcher rehearsal must not clear real launcher gates')
    if (readinessChecks.allArtifactsExist !== true) {
      assert(errors, readinessReport.blockers?.includes('local_artifact_or_release_index_metadata_missing'), 'release readiness report missing local artifact blocker')
    }
    if (readinessChecks.allRuntimeGatesCleared !== true) {
      assert(errors, readinessReport.blockers?.includes('runtime_execution_gates_not_cleared'), 'release readiness report missing runtime gate blocker')
    }
    if (readinessChecks.allRuntimeReportsPresent !== true) {
      assert(errors, readinessReport.blockers?.includes('runtime_execution_reports_missing'), 'release readiness report missing runtime report blocker')
    }
    if (readinessChecks.allLocalRuntimeRehearsalsPresent !== true) {
      assert(errors, readinessReport.blockers?.includes('local_runtime_rehearsal_reports_missing'), 'release readiness report missing local runtime rehearsal report blocker')
    }
    if (readinessChecks.allLocalRuntimeRehearsalsPassed !== true) {
      assert(errors, readinessReport.blockers?.includes('local_runtime_rehearsal_failed'), 'release readiness report missing local runtime rehearsal failed blocker')
    }
    if (readinessChecks.allLauncherGatesCleared !== true) {
      assert(errors, readinessReport.blockers?.includes('real_launcher_install_update_repair_rollback_missing'), 'release readiness report missing launcher execution blocker')
    }
    if (readinessChecks.allLauncherReportsPresent !== true) {
      assert(errors, readinessReport.blockers?.includes('launcher_execution_reports_missing'), 'release readiness report missing launcher report blocker')
    }
    if (readinessChecks.allLocalLauncherRehearsalsPresent !== true) {
      assert(errors, readinessReport.blockers?.includes('local_launcher_rehearsal_reports_missing'), 'release readiness report missing local launcher rehearsal report blocker')
    }
    if (readinessChecks.allLocalLauncherRehearsalsPassed !== true) {
      assert(errors, readinessReport.blockers?.includes('local_launcher_rehearsal_failed'), 'release readiness report missing local launcher rehearsal failed blocker')
    }
    if (readinessChecks.allFinalReviewGatesCleared !== true) {
      assert(errors, readinessReport.blockers?.includes('final_asset_legal_review_missing'), 'release readiness report missing final review blocker')
    }
    if (readinessChecks.allFinalReviewReportsPresent !== true) {
      assert(errors, readinessReport.blockers?.includes('final_release_review_reports_missing'), 'release readiness report missing final review report blocker')
    }
    if (readinessChecks.allDistributionGatesCleared !== true) {
      assert(errors, readinessReport.blockers?.includes('distribution_approval_missing'), 'release readiness report missing distribution approval blocker')
    }
    if (readinessChecks.allDistributionApprovalReportsPresent !== true) {
      assert(errors, readinessReport.blockers?.includes('distribution_approval_reports_missing'), 'release readiness report missing distribution approval report blocker')
    }
    if (readinessChecks.allArtifactUrlsPresent !== true) {
      assert(errors, readinessReport.blockers?.includes('release_index_download_urls_missing'), 'release readiness report missing download URL blocker')
    }
    if (readinessChecks.releasePublicationDownloadsVerified !== true) {
      assert(errors, readinessReport.blockers?.includes('download_verification_missing'), 'release readiness report missing download verification blocker')
    }
    if (readinessChecks.releasePublicationApproved !== true) {
      assert(errors, readinessReport.blockers?.includes('release_index_patch_not_approved'), 'release readiness report missing Release Index patch approval blocker')
    }
    assert(errors, readinessEditionResults.length === EDITIONS.length, 'release readiness report edition count mismatch')
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    moduleRoot,
    workspaceRoot,
    editions: EDITIONS.map((edition) => edition.directory),
    requiredPhases,
    publicAlphaEvidence,
    runtimeEvidenceCount: runtimeEvidenceIds.length,
    runtimeScenarioCount: runtimeScenarioIds.length,
    playtestScenarios: playtestScenarioIds,
    saveLoadCheckpoints: saveLoadCheckpointIds,
    waystonePublicAlphaScenario,
    launcherFlows: launcherFlowIds,
    errors,
    warnings,
  }
}

function printHelp() {
  console.log(`Usage: node scripts/validate-openlands-editions.mjs [options]

Options:
  --module-root <path>     Path to addons/echoopenlandsprotocol. Auto-detected by default.
  --workspace-root <path>  Parent directory containing ECHO-Openlands-* edition repos.
                           Defaults to the module workspace parent.
  --readiness-report <path>
                           Release readiness report to validate. Defaults to dist/echo-module-release.
  --json                   Print JSON output.
  --help                   Show this help.
`)
}

try {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
  } else {
    const moduleRoot = findModuleRoot(args.moduleRoot)
    const workspaceRoot = args.workspaceRoot
      ? path.resolve(args.workspaceRoot)
      : path.resolve(moduleRoot, '..', '..', '..')
    const readinessReportPath = args.readinessReport ? path.resolve(args.readinessReport) : null
    const result = validate({ moduleRoot, workspaceRoot, readinessReportPath })
    if (args.json) {
      console.log(JSON.stringify(result, null, 2))
    } else if (result.status === 'passed') {
      console.log(`Openlands edition validation passed for ${result.editions.length} edition repo(s).`)
      for (const warning of result.warnings) console.warn(`warning: ${warning}`)
    } else {
      console.error(`Openlands edition validation failed with ${result.errors.length} error(s):`)
      for (const error of result.errors) console.error(`- ${error}`)
      process.exitCode = 1
    }
  }
} catch (error) {
  console.error(error.message)
  process.exitCode = 1
}

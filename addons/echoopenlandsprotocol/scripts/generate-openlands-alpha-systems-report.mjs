import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import {
  addFoundationKnownBlocks,
  addFoundationKnownItems,
} from './openlands-foundation-id-resolver.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EXPECTED_RUNTIMES = ['echo_native', 'echo_runtime_standalone', 'neoforge']
const REQUIRED_INVENTORY_COMMANDS = [
  'quick_stack',
  'quick_deposit',
  'sort_inventory',
  'craft_from_nearby_storage',
  'named_chests',
]
const REQUIRED_CROP_IDS = ['grain', 'root_crop', 'berries']
const REQUIRED_PEN_IDS = ['hare_pen', 'goat_pen', 'marsh_hen_pen']
const REQUIRED_SHARED_STATE_IDS = ['waystones', 'containers', 'homestead_claims', 'holomap_markers']
const REQUIRED_NETWORK_EVENT_IDS = [
  'openlands_waystone_state_changed',
  'openlands_shelter_score_updated',
  'openlands_storage_transaction',
  'openlands_holomap_marker_changed',
]
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-alpha-systems-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-alpha-systems-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-alpha-systems-report.json',
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
  const extractRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-alpha-systems-'))
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

function sorted(values) {
  return [...values].sort()
}

function sameSet(actual, expected) {
  return JSON.stringify(sorted(actual ?? [])) === JSON.stringify(sorted(expected ?? []))
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function assertRuntimeParity(payload, label) {
  assert(sameSet(payload.runtimeParity ?? [], EXPECTED_RUNTIMES), `${label} runtime parity mismatch`)
}

function validateRefs(values, knownIds, label) {
  for (const value of values ?? []) {
    assert(knownIds.has(normalizeId(value)), `${label} references unknown id ${value}`)
  }
}

function standardRuleAvoidsUpkeepDeath(rule) {
  const text = rule ?? ''
  if (/no .*death|do not die|does not die|animals do not die|no upkeep death/i.test(text)) return true
  return !/die|death|dead/i.test(text)
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

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)
  assert(fileExists(moduleRoot), `module root not found: ${moduleRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const blocksPayload = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json'))
  const itemsPayload = readJson(path.join(dataRoot, 'items', 'mvp_items.json'))
  const tagsPayload = readJson(path.join(dataRoot, 'tags', 'mvp_tags.json'))
  const creaturesPayload = readJson(path.join(dataRoot, 'creatures', 'mvp_creatures.json'))
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const aliasBridge = readJson(path.join(dataRoot, 'foundation', 'foundation_alias_bridge.json'))
  const homestead = readJson(path.join(dataRoot, 'systems', 'homestead_alpha.json'))
  const builderUx = readJson(path.join(dataRoot, 'systems', 'builder_ux_alpha.json'))
  const coopSmp = readJson(path.join(dataRoot, 'systems', 'coop_and_smp.json'))
  const distribution = readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))

  assertRuntimeParity(blocksPayload, 'blocks')
  assertRuntimeParity(itemsPayload, 'items')
  assertRuntimeParity(tagsPayload, 'tags')
  assertRuntimeParity(creaturesPayload, 'creatures')
  assertRuntimeParity(homestead, 'homestead alpha')
  assertRuntimeParity(builderUx, 'builder UX alpha')
  assertRuntimeParity(coopSmp, 'co-op/SMP')
  assertRuntimeParity(distribution, 'distribution alpha gates')
  assert(evidenceTemplate.packId === edition.packId, `${edition.packId} evidence template packId mismatch`)
  assert(evidenceTemplate.runtimeTarget === edition.runtimeTarget, `${edition.packId} evidence template runtime target mismatch`)

  const blockIds = addFoundationKnownBlocks(new Set((blocksPayload.blocks ?? []).map((block) => normalizeId(block.id))), conformance, aliasBridge)
  const itemIds = addFoundationKnownItems(new Set((itemsPayload.items ?? []).map((item) => normalizeId(item.id))), conformance, aliasBridge)
  const creatureIds = new Set((creaturesPayload.creatures ?? []).map((creature) => normalizeId(creature.id)))
  const itemTagIds = new Set(Object.keys(tagsPayload.itemTags ?? {}))
  const blockTagIds = new Set(Object.keys(tagsPayload.blockTags ?? {}))

  assert(sameSet((homestead.crops ?? []).map((crop) => crop.id), REQUIRED_CROP_IDS), 'homestead crop ids mismatch')
  assert(sameSet((homestead.animalPens ?? []).map((pen) => pen.id), REQUIRED_PEN_IDS), 'homestead animal pen ids mismatch')
  const homesteadUseMarkers = []
  for (const crop of homestead.crops ?? []) {
    assert(itemIds.has(normalizeId(crop.seedItem)), `crop ${crop.id} seed item missing ${crop.seedItem}`)
    assert(itemIds.has(normalizeId(crop.harvestItem)), `crop ${crop.id} harvest item missing ${crop.harvestItem}`)
    validateRefs(crop.preferredSoils, blockIds, `crop ${crop.id} preferredSoils`)
    assert(Number.isInteger(crop.growthStages) && crop.growthStages > 0, `crop ${crop.id} must define positive growthStages`)
    assert(Number.isInteger(crop.baseGrowthMinutes) && crop.baseGrowthMinutes >= 10, `crop ${crop.id} must define baseGrowthMinutes`)
    assert(crop.standardFailure !== undefined && !/die|dead|wither/i.test(crop.standardFailure), `crop ${crop.id} Standard failure must not kill crops`)
    for (const use of crop.uses ?? []) {
      const normalized = normalizeId(use)
      if (!itemIds.has(normalized)) homesteadUseMarkers.push({ crop: crop.id, use })
    }
  }
  assert(homestead.soilCare?.watering?.standardRequired === false, 'Standard watering must stay optional')
  assert(homestead.soilCare?.compost?.standardRequired === false, 'Standard compost must stay optional')
  validateRefs(homestead.soilCare?.compost?.inputs, itemIds, 'compost inputs')
  assert(itemIds.has(normalizeId(homestead.soilCare?.compost?.item)), 'compost item missing from registry')
  for (const meal of homestead.cookpotMeals ?? []) {
    assert(blockIds.has(normalizeId(meal.station)), `cookpot meal ${meal.id} station missing ${meal.station}`)
    assert(itemIds.has(normalizeId(meal.baseContainer)), `cookpot meal ${meal.id} base container missing ${meal.baseContainer}`)
    assert(itemTagIds.has(meal.validIngredientsTag), `cookpot meal ${meal.id} ingredient tag missing ${meal.validIngredientsTag}`)
    assert(meal.standardResult?.spoilage === false, `cookpot meal ${meal.id} must disable Standard spoilage`)
    assert(Array.isArray(meal.saveFields) && meal.saveFields.includes('remainingCookTicks'), `cookpot meal ${meal.id} must persist remainingCookTicks`)
  }
  for (const pen of homestead.animalPens ?? []) {
    assert(creatureIds.has(normalizeId(pen.creature)), `animal pen ${pen.id} creature missing ${pen.creature}`)
    validateRefs(pen.comfortBlocks, blockIds, `animal pen ${pen.id} comfortBlocks`)
    validateRefs(pen.feedItems, itemIds, `animal pen ${pen.id} feedItems`)
    assert(Number.isInteger(pen.minimumFenceArea) && pen.minimumFenceArea >= 16, `animal pen ${pen.id} minimumFenceArea too small`)
    assert(standardRuleAvoidsUpkeepDeath(pen.standardRule), `animal pen ${pen.id} Standard rule must avoid upkeep death`)
  }
  const traderRewardMarkers = []
  for (const pool of homestead.traderSurplus?.demandPools ?? []) {
    validateRefs(pool.acceptedItems, itemIds, `trader demand pool ${pool.id} acceptedItems`)
    for (const reward of pool.rewardTypes ?? []) {
      const normalized = normalizeId(reward)
      if (!itemIds.has(normalized)) traderRewardMarkers.push({ pool: pool.id, reward })
    }
  }
  for (const field of ['traderId', 'regionId', 'demandPoolId', 'expiresAt', 'completedOfferIds']) {
    assert(homestead.traderSurplus?.storedFields?.includes(field), `trader surplus missing stored field ${field}`)
  }

  assert(sameSet((builderUx.inventoryCommands ?? []).map((command) => command.id), REQUIRED_INVENTORY_COMMANDS), 'builder UX inventory command ids mismatch')
  const hammer = (builderUx.tools ?? []).find((tool) => normalizeId(tool.id) === 'field_hammer')
  assert(hammer, 'builder UX missing field_hammer tool')
  assert(itemIds.has(normalizeId(hammer.item)), 'field_hammer item missing from registry')
  validateRefs(hammer.supportedBlocks, blockIds, 'field_hammer supportedBlocks')
  for (const validation of ['player_can_edit_block', 'target_block_in_supported_set', 'variant_exists_for_runtime', 'inventory_or_creative_has_required_item_if_converting']) {
    assert(hammer.serverValidation?.includes(validation), `field_hammer missing server validation ${validation}`)
  }
  const scaffold = (builderUx.temporaryBlocks ?? []).find((block) => block.id === 'scaffold_bundle')
  assert(scaffold, 'builder UX missing scaffold_bundle temporary block')
  assert(itemIds.has(normalizeId(scaffold.item)), 'scaffold_bundle item missing from registry')
  assert(scaffold.fallRule === 'no_falling_physics_in_standard', 'scaffold must avoid falling physics in Standard')
  assert(Number.isInteger(scaffold.maxChainPlacement) && scaffold.maxChainPlacement >= 16, 'scaffold maxChainPlacement must support roof/bridge work')
  for (const command of builderUx.inventoryCommands ?? []) {
    validateRefs(command.eligibleContainers, blockIds, `inventory command ${command.id} eligibleContainers`)
    validateRefs(command.eligibleStations, blockIds, `inventory command ${command.id} eligibleStations`)
    if (command.id === 'named_chests') {
      assert(command.limits?.maxCharacters === 32, 'named_chests maxCharacters must be 32')
      assert(command.limits?.allowColorCodes === false, 'named_chests must disable color codes')
      assert(command.limits?.filterControlCharacters === true, 'named_chests must filter control characters')
    }
    if (command.id === 'craft_from_nearby_storage') {
      for (const field of ['reservationId', 'recipeId', 'containerIds', 'reservedStacks', 'expiresAt']) {
        assert(command.storedFields?.includes(field), `craft_from_nearby_storage missing stored field ${field}`)
      }
    }
    if (command.id === 'quick_stack' || command.id === 'quick_deposit') {
      assert(command.multiplayerValidation?.includes('container_permission'), `${command.id} must validate container permissions`)
      assert(command.multiplayerValidation?.includes('server_authoritative_transfer'), `${command.id} must be server authoritative`)
    }
  }
  for (const acceptance of ['Hammer actions never delete a block without server validation.', 'Craft-from-storage must not duplicate items when save/load happens mid-craft.', 'Sorting order must be deterministic across all runtime targets.']) {
    assert(builderUx.acceptance?.includes(acceptance), `builder UX missing acceptance: ${acceptance}`)
  }

  assert(sameSet((coopSmp.sharedState ?? []).map((state) => state.id), REQUIRED_SHARED_STATE_IDS), 'co-op shared state ids mismatch')
  assert(sameSet((coopSmp.networkEvents ?? []).map((event) => event.id), REQUIRED_NETWORK_EVENT_IDS), 'co-op network event ids mismatch')
  const containersState = (coopSmp.sharedState ?? []).find((state) => state.id === 'containers')
  assert(containersState?.storedFields?.includes('inventoryStacks'), 'containers shared state must persist inventoryStacks')
  const waystonesState = (coopSmp.sharedState ?? []).find((state) => state.id === 'waystones')
  assert(waystonesState?.storedFields?.includes('repairContributorIds'), 'waystones shared state must persist repairContributorIds')
  assert(coopSmp.permissions?.defaults?.containerQuickStack === 'owner_or_group', 'quick stack permission default mismatch')
  assert(coopSmp.permissions?.defaults?.waystoneTravel === 'public_after_active', 'waystone travel permission default mismatch')

  const publicAlphaMinimum = distribution.publicAlphaMinimum ?? {}
  assert(publicAlphaMinimum.biomes === 4, 'Public Alpha minimum biomes mismatch')
  assert(publicAlphaMinimum.blocks?.min >= 50, 'Public Alpha minimum block count too low')
  assert(publicAlphaMinimum.items?.min >= 45, 'Public Alpha minimum item count too low')
  assert(publicAlphaMinimum.creatures === 10, 'Public Alpha minimum creature count mismatch')
  assert(publicAlphaMinimum.coOp?.targetPlayers === '1-8', 'Public Alpha co-op target mismatch')

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifact = artifactRuntimeEntries(artifactPath, edition)
  const runtimeEntriesChecked = [
    'data/echoopenlandsprotocol/openlands/systems/homestead_alpha.json',
    'data/echoopenlandsprotocol/openlands/systems/builder_ux_alpha.json',
    'data/echoopenlandsprotocol/openlands/systems/coop_and_smp.json',
    'data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json',
    'data/echoopenlandsprotocol/openlands/tags/mvp_tags.json',
    'data/echoopenlandsprotocol/openlands/items/mvp_items.json',
    'data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json',
    'data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json',
  ]
  for (const entry of runtimeEntriesChecked) {
    assert(artifact.runtimeEntries.includes(entry), `${edition.artifactName} missing runtime entry ${entry}`)
  }

  const report = {
    schema: 'echo.openlands.edition.alpha_systems_report.v1',
    status: 'preflight_passed',
    realRuntimeAlphaSystemsRequiredBeforePublicAlpha: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    contracts: {
      homestead: 'data/echoopenlandsprotocol/openlands/systems/homestead_alpha.json',
      builderUx: 'data/echoopenlandsprotocol/openlands/systems/builder_ux_alpha.json',
      coopSmp: 'data/echoopenlandsprotocol/openlands/systems/coop_and_smp.json',
      distribution: 'data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json',
    },
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      nestedRuntimeEntry: artifact.nestedRuntimeEntry,
      runtimeEntriesChecked,
    },
    relaxedStandardGuarantees: {
      cropsDoNotDieInStandard: true,
      wateringOptionalInStandard: homestead.soilCare.watering.standardRequired === false,
      compostOptionalInStandard: homestead.soilCare.compost.standardRequired === false,
      cookpotSpoilageDisabled: homestead.cookpotMeals.every((meal) => meal.standardResult?.spoilage === false),
      scaffoldNoFallingPhysics: scaffold.fallRule === 'no_falling_physics_in_standard',
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
    publicAlphaMinimum,
    blockedBy: [
      'real_runtime_alpha_systems_execution_missing',
      'builder_ux_runtime_parity_test_missing',
      'homestead_runtime_save_load_test_missing',
      'coop_storage_transaction_test_missing',
    ],
    outputPath,
    proofs: [
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
    console.log(`Openlands ${args.edition} alpha systems report ${action}: ${report.homesteadSummary.crops.length} crops, ${report.builderUxSummary.inventoryCommands.length} builder commands.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-alpha-systems-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-alpha-systems-report.json.
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

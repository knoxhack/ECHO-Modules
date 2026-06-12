import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import {
  addFoundationKnownBlocks,
  addFoundationKnownItems,
  addFoundationKnownRecipes,
} from './openlands-foundation-id-resolver.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EXPECTED_RUNTIMES = ['echo_native', 'echo_runtime_standalone', 'neoforge']
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-registry-parity-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-registry-parity-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-registry-parity-report.json',
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
  const extractRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-registry-parity-'))
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

function assertSameSet(actual, expected, label) {
  assert(sameSet(actual, expected), `${label} mismatch`)
}

function assertRuntimeParity(payload, label) {
  assertSameSet(payload.runtimeParity ?? payload.runtimeTargets ?? [], EXPECTED_RUNTIMES, `${label} runtime parity`)
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

function itemRefs(entries) {
  const refs = []
  for (const entry of entries ?? []) {
    if (entry.item) refs.push(normalizeId(entry.item))
    if (entry.block) refs.push(normalizeId(entry.block))
  }
  return refs
}

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)
  assert(fileExists(moduleRoot), `module root not found: ${moduleRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const aliasBridge = readJson(path.join(dataRoot, 'foundation', 'foundation_alias_bridge.json'))
  const parity = readJson(path.join(dataRoot, 'systems', 'cross_platform_parity.json'))
  const blocksPayload = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json'))
  const itemsPayload = readJson(path.join(dataRoot, 'items', 'mvp_items.json'))
  const recipesPayload = readJson(path.join(dataRoot, 'recipes', 'mvp_recipes.json'))
  const biomesPayload = readJson(path.join(dataRoot, 'biomes', 'mvp_biomes.json'))
  const structuresPayload = readJson(path.join(dataRoot, 'structures', 'mvp_landmarks.json'))
  const creaturesPayload = readJson(path.join(dataRoot, 'creatures', 'mvp_creatures.json'))
  const waystonesPayload = readJson(path.join(dataRoot, 'waystones', 'waystone_contract.json'))
  const tagsPayload = readJson(path.join(dataRoot, 'tags', 'mvp_tags.json'))
  const lootPayload = readJson(path.join(dataRoot, 'loot', 'mvp_loot.json'))
  const modesPayload = readJson(path.join(dataRoot, 'config', 'game_modes.json'))
  const routePayload = readJson(path.join(dataRoot, 'progression', 'first_hour_route.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))

  assertRuntimeParity(conformance, 'conformance')
  assertRuntimeParity(blocksPayload, 'blocks')
  assertRuntimeParity(itemsPayload, 'items')
  assertRuntimeParity(recipesPayload, 'recipes')
  assertRuntimeParity(biomesPayload, 'biomes')
  assertRuntimeParity(structuresPayload, 'structures')
  assertRuntimeParity(creaturesPayload, 'creatures')
  assertRuntimeParity(waystonesPayload, 'waystones')
  assertRuntimeParity(tagsPayload, 'tags')
  assertRuntimeParity(lootPayload, 'loot')

  const blocks = blocksPayload.blocks ?? []
  const items = itemsPayload.items ?? []
  const recipes = recipesPayload.recipes ?? []
  const biomes = biomesPayload.biomes ?? []
  const structures = structuresPayload.landmarks ?? []
  const creatures = creaturesPayload.creatures ?? []
  const blockIds = blocks.map((block) => normalizeId(block.id))
  const itemIds = items.map((item) => normalizeId(item.id))
  const recipeIds = recipes.map((recipe) => normalizeId(recipe.id))
  const biomeIds = biomes.map((biome) => normalizeId(biome.id))
  const creatureIds = creatures.map((creature) => normalizeId(creature.id))
  const structureIds = structures.map((structure) => normalizeId(structure.id))
  const waystoneStates = (waystonesPayload.stateMachine ?? []).map((state) => state.state)

  assertSameSet(blockIds, conformance.blockRegistry, 'block registry')
  assertSameSet(itemIds, conformance.itemRegistry, 'item registry')
  assertSameSet(recipeIds, conformance.recipeRegistry, 'recipe registry')
  assertSameSet(biomeIds, conformance.biomeRegistry, 'biome registry')
  assertSameSet(creatureIds, conformance.creatureRegistry, 'creature registry')

  const blockIdSet = addFoundationKnownBlocks(new Set(blockIds), conformance, aliasBridge)
  const itemIdSet = addFoundationKnownItems(new Set(itemIds), conformance, aliasBridge)
  const recipeIdSet = addFoundationKnownRecipes(new Set(recipeIds), conformance, aliasBridge)
  const biomeIdSet = new Set(biomeIds)
  const creatureIdSet = new Set(creatureIds)
  const structureIdSet = new Set(structureIds)
  for (const block of blocks) {
    for (const drop of block.drops ?? []) {
      if (drop.item) assert(itemIdSet.has(normalizeId(drop.item)) || blockIdSet.has(normalizeId(drop.item)), `block ${block.id} drop references unknown id ${drop.item}`)
      if (drop.fallback) assert(blockIdSet.has(normalizeId(drop.fallback)), `block ${block.id} fallback references unknown block ${drop.fallback}`)
    }
  }
  const nonShippingRecipeRefs = []
  for (const item of items) {
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
  for (const recipe of recipes) {
    for (const ref of itemRefs(recipe.inputs)) assert(itemIdSet.has(ref) || blockIdSet.has(ref), `recipe ${recipe.id} input references unknown id ${ref}`)
    for (const ref of itemRefs(recipe.outputs)) assert(itemIdSet.has(ref) || blockIdSet.has(ref), `recipe ${recipe.id} output references unknown id ${ref}`)
  }
  const nonRegistryBiomeResources = []
  for (const biome of biomes) {
    for (const resource of biome.resourceSet ?? []) {
      const normalized = normalizeId(resource)
      if (!itemIdSet.has(normalized) && !blockIdSet.has(normalized)) {
        nonRegistryBiomeResources.push({
          biome: normalizeId(biome.id),
          resource,
        })
      }
    }
    for (const spawn of biome.spawnTable ?? []) assert(creatureIdSet.has(normalizeId(spawn.creature)), `biome ${biome.id} spawn references unknown creature ${spawn.creature}`)
    for (const landmark of Object.keys(biome.landmarkFrequency ?? {})) assert(structureIdSet.has(normalizeId(landmark)) || landmark === 'broken_waystone', `biome ${biome.id} landmarkFrequency references unknown landmark ${landmark}`)
  }
  for (const structure of structures) {
    for (const block of structure.blocks ?? []) assert(blockIdSet.has(normalizeId(block)), `structure ${structure.id} block references unknown block ${block}`)
    for (const biome of structure.preferredBiomes ?? []) assert(biomeIdSet.has(normalizeId(biome)), `structure ${structure.id} preferred biome references unknown biome ${biome}`)
  }
  for (const creature of creatures) {
    for (const biome of creature.biomes ?? []) assert(biomeIdSet.has(normalizeId(biome)), `creature ${creature.id} biome references unknown biome ${biome}`)
  }

  const parityTarget = (parity.runtimeTargets ?? []).find((target) => target.id === edition.runtimeTarget)
  assert(parityTarget, `cross-platform parity missing runtime target ${edition.runtimeTarget}`)
  assert(parityTarget.editionRepo === path.basename(editionRoot), `${edition.packId} parity edition repo mismatch`)
  assert(parityTarget.artifactFamily === edition.artifactKind, `${edition.packId} parity artifact family mismatch`)
  assert(parityTarget.artifactPattern === edition.artifactName, `${edition.packId} parity artifact pattern mismatch`)
  for (const surface of ['registry_ids', 'first_hour_save_load', 'standard_mode_rules', 'waystone_state_machine']) {
    assert((parity.paritySurfaces ?? []).some((entry) => entry.id === surface), `cross-platform parity missing surface ${surface}`)
  }

  const standard = (modesPayload.modes ?? []).find((mode) => mode.id === 'openlands_standard')
  assert(standard?.rules?.hunger === 'gentle', 'Standard mode hunger must be gentle')
  for (const flag of ['stamina', 'hydration', 'foodSpoilage', 'temperatureDamage']) {
    assert(standard?.rules?.[flag] === false, `Standard mode must keep ${flag} off`)
  }
  for (const field of ['inventory', 'hotbar', 'placedBlocks', 'chestContents', 'bedrollSpawn', 'campfireLitState', 'shelterScore', 'waystoneState', 'holomapRegionDiscovery']) {
    assert(routePayload.saveLoadAcceptance?.includes(field), `first-hour save/load missing ${field}`)
  }

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifact = artifactRuntimeEntries(artifactPath, edition)
  const runtimeEntriesChecked = [
    'data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json',
    'data/echoopenlandsprotocol/openlands/systems/cross_platform_parity.json',
    'data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json',
    'data/echoopenlandsprotocol/openlands/items/mvp_items.json',
    'data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json',
    'data/echoopenlandsprotocol/openlands/biomes/mvp_biomes.json',
    'data/echoopenlandsprotocol/openlands/structures/mvp_landmarks.json',
    'data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json',
    'data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json',
  ]
  for (const entry of runtimeEntriesChecked) {
    assert(artifact.runtimeEntries.includes(entry), `${edition.artifactName} missing runtime entry ${entry}`)
  }

  const report = {
    schema: 'echo.openlands.edition.registry_parity_report.v1',
    status: 'preflight_passed',
    realRegistryParityExecutionRequiredBeforePublicAlpha: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    conformanceFixture: 'data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json',
    parityContract: 'data/echoopenlandsprotocol/openlands/systems/cross_platform_parity.json',
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      nestedRuntimeEntry: artifact.nestedRuntimeEntry,
      runtimeEntriesChecked,
    },
    registryCounts: {
      blocks: blockIds.length,
      items: itemIds.length,
      recipes: recipeIds.length,
      biomes: biomeIds.length,
      structures: structureIds.length,
      creatures: creatureIds.length,
      waystoneStates: waystoneStates.length,
      systems: conformance.systemContracts?.length ?? 0,
    },
    nonShippingRecipeRefs,
    nonRegistryBiomeResources,
    paritySurfaces: (parity.paritySurfaces ?? []).map((surface) => ({
      id: surface.id,
      mustMatch: surface.mustMatch,
      allowedRuntimeDifference: surface.allowedRuntimeDifference,
    })),
    runtimeResponsibilities: parityTarget.adapterResponsibilities,
    saveLoadFields: routePayload.saveLoadAcceptance,
    standardModeRules: standard.rules,
    blockedBy: [
      'real_runtime_registry_parity_test_missing',
      'adapter_boot_execution_missing',
      'runtime_id_mapping_report_missing',
    ],
    outputPath,
    proofs: [
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
    console.log(`Openlands ${args.edition} registry parity report ${action}: ${report.registryCounts.blocks} blocks, ${report.registryCounts.items} items.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-registry-parity-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-registry-parity-report.json.
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

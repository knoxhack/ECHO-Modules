import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const EXPECTED_RUNTIMES = ['echo_native', 'echo_runtime_standalone', 'neoforge']

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    output: null,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--dry-run') args.dryRun = true
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function normalizeId(value) {
  if (typeof value !== 'string') return value
  return value.includes(':') ? value.split(':').pop() : value
}

function normalizeRecipeRef(value) {
  return normalizeId(value).replace(/^recipe\//, '')
}

function unique(values) {
  return [...new Set((values ?? []).filter((value) => typeof value === 'string' && value.length > 0))]
}

function hasTag(record, fragment) {
  return (record.tags ?? []).some((tag) => tag.includes(fragment))
}

function buildRecipeIndexes(recipes) {
  const byOutput = new Map()
  const byInput = new Map()
  for (const recipe of recipes) {
    const recipeId = normalizeRecipeRef(recipe.id)
    for (const output of recipe.outputs ?? []) {
      for (const key of ['item', 'block']) {
        if (!output[key]) continue
        const id = normalizeId(output[key])
        const bucket = byOutput.get(id) ?? []
        bucket.push(recipeId)
        byOutput.set(id, bucket)
      }
    }
    for (const input of recipe.inputs ?? []) {
      for (const key of ['item', 'block']) {
        if (!input[key]) continue
        const id = normalizeId(input[key])
        const bucket = byInput.get(id) ?? []
        bucket.push(recipeId)
        byInput.set(id, bucket)
      }
    }
  }
  return { byOutput, byInput }
}

function buildDropIndex(blocks, loot) {
  const itemSources = new Map()
  const blockSources = new Map()
  function add(map, id, source) {
    const bucket = map.get(id) ?? []
    bucket.push(source)
    map.set(id, bucket)
  }
  for (const block of blocks) {
    const blockId = normalizeId(block.id)
    for (const drop of [...(block.drops ?? []), ...(block.bonusDrops ?? [])]) {
      if (drop.item) add(itemSources, normalizeId(drop.item), `block:${blockId}`)
      if (drop.block) add(blockSources, normalizeId(drop.block), `block:${blockId}`)
    }
  }
  for (const entry of loot.blockDrops ?? []) {
    const blockId = normalizeId(entry.block)
    for (const drop of [...(entry.drops ?? []), ...(entry.bonusChance ?? [])]) {
      if (drop.item) add(itemSources, normalizeId(drop.item), `loot:block:${blockId}`)
      if (drop.block) add(blockSources, normalizeId(drop.block), `loot:block:${blockId}`)
    }
  }
  for (const entry of loot.creatureDrops ?? []) {
    for (const drop of entry.drops ?? []) {
      if (drop.item) add(itemSources, normalizeId(drop.item), `creature:${normalizeId(entry.creature)}`)
    }
  }
  for (const table of loot.chestTables ?? []) {
    for (const drop of table.entries ?? []) {
      if (drop.item) add(itemSources, normalizeId(drop.item), `cache:${table.id}`)
      if (drop.block) add(blockSources, normalizeId(drop.block), `cache:${table.id}`)
    }
  }
  return { itemSources, blockSources }
}

function progressionStageForBlock(block) {
  const id = normalizeId(block.id)
  if (id.includes('waystone') || block.stateMachine) return 'waystone_network'
  if (hasTag(block, 'old_roads') || block.category === 'road') return 'old_roads'
  if (block.category === 'ore' && (id.includes('iron') || id.includes('glow'))) return 'midgame_caves'
  if (block.category === 'ore') return 'first_metal'
  if (block.category === 'station' && id === 'forge') return 'metal_age'
  if (block.category === 'station' && id === 'kiln') return 'clay_age'
  if (block.category === 'station') return 'homestead_and_building'
  if (block.category === 'terrain' || block.category === 'wood' || block.category === 'stone') return 'first_hour'
  if (hasTag(block, 'shelter') || block.shelterScore) return 'first_shelter'
  return 'mvp_building'
}

function progressionStageForItem(item) {
  const id = normalizeId(item.id)
  if (hasTag(item, 'waystone') || id.includes('waystone') || id.includes('road') || id.includes('route') || id.includes('region')) return 'waystone_network'
  if (hasTag(item, 'metal/iron') || id.includes('iron')) return 'midgame_caves'
  if (hasTag(item, 'metal/bronze') || id.includes('bronze')) return 'bronze_age'
  if (hasTag(item, 'metal/copper') || id.includes('copper') || hasTag(item, 'metal/tin') || id.includes('tin')) return 'first_metal'
  if (hasTag(item, 'tool')) return 'first_tools'
  if (hasTag(item, 'food') || hasTag(item, 'farming')) return 'homestead'
  if (hasTag(item, 'builder_ux') || hasTag(item, 'inventory')) return 'builder_quality_of_life'
  return 'first_hour_materials'
}

function blockRoles(block) {
  const roles = []
  if (block.biomePlacement || block.spawnLayer) roles.push('world_resource')
  if (block.category === 'terrain' || hasTag(block, 'starter_surface')) roles.push('terrain_foundation')
  if (block.category === 'stone' || hasTag(block, 'stone')) roles.push('stone_and_masonry')
  if (block.category === 'ore') roles.push('mining_progression')
  if (hasTag(block, 'wood_family')) roles.push('wood_building_family')
  if (hasTag(block, 'building') || block.category?.includes('building') || block.plannedVariants) roles.push('building_palette')
  if (block.category === 'station') roles.push('crafting_station')
  if (block.category === 'storage' || hasTag(block, 'container')) roles.push('storage')
  if (hasTag(block, 'shelter') || block.shelterScore) roles.push('shelter_score')
  if (block.light) roles.push('lighting')
  if (hasTag(block, 'old_roads') || block.category === 'road') roles.push('old_road_discovery')
  if (block.category === 'waystone' || block.stateMachine) roles.push('waystone_progression')
  if (hasTag(block, 'farmable') || hasTag(block, 'farm_protection')) roles.push('homestead')
  return unique(roles.length ? roles : ['mvp_content'])
}

function itemRoles(item) {
  const roles = []
  if (hasTag(item, 'raw_material')) roles.push('raw_material')
  if (hasTag(item, 'food') || item.nutrition) roles.push('food_and_comfort')
  if (hasTag(item, 'tool') || item.toolStats) roles.push('tool_progression')
  if (hasTag(item, 'ingot') || hasTag(item, 'ore_chunk')) roles.push('metal_progression')
  if (hasTag(item, 'waystone') || hasTag(item, 'holomap') || hasTag(item, 'old_road')) roles.push('waystone_and_map_progression')
  if (hasTag(item, 'farming')) roles.push('homestead')
  if (hasTag(item, 'builder_ux')) roles.push('builder_quality_of_life')
  if (item.placesBlock) roles.push('placeable_utility')
  if (item.equipment) roles.push('equipment')
  return unique(roles.length ? roles : ['mvp_content'])
}

function playerUseForBlock(block) {
  const parts = []
  if (block.tool) parts.push(`break with ${block.tool}`)
  if (block.recipeSource) parts.push(`craft via ${normalizeRecipeRef(block.recipeSource)}`)
  if (block.processes) parts.push(`station processes: ${block.processes.join(', ')}`)
  if (block.shelterScore) parts.push(`adds ${block.shelterScore} shelter score`)
  if (block.light) parts.push(`emits light ${block.light}`)
  if (block.effects) parts.push(`effects: ${block.effects.join(', ')}`)
  if (block.slots) parts.push(`stores ${block.slots} slots`)
  if (block.movementModifier) parts.push(`movement modifier ${block.movementModifier}`)
  if (!parts.length) parts.push(block.notes ?? 'MVP block interaction')
  return parts
}

function playerUseForItem(item) {
  const parts = []
  if (item.nutrition) parts.push(`food: hunger ${item.nutrition.hunger}, comfort ${item.nutrition.comfort}, saturation ${item.nutrition.saturation}`)
  if (item.toolStats) {
    const stats = [`${item.toolStats.toolClass} tier ${item.toolStats.tier}`]
    if (item.toolStats.speed) stats.push(`speed ${item.toolStats.speed}`)
    if (item.toolStats.durability) stats.push(`durability ${item.toolStats.durability}`)
    parts.push(`tool: ${stats.join(', ')}`)
  }
  if (item.placesBlock) parts.push(`places ${item.placesBlock}`)
  if (item.equipment) parts.push(`equipment ${item.equipment.slot} with ${item.equipment.storageSlots} storage slots`)
  if (item.recipeRefs?.length) parts.push(`recipe refs: ${item.recipeRefs.map(normalizeRecipeRef).join(', ')}`)
  if (!parts.length) parts.push(item.notes ?? 'MVP item interaction')
  return parts
}

function acquisitionForBlock(block, recipeIndexes, dropIndex) {
  const id = normalizeId(block.id)
  const sources = []
  for (const biome of block.biomePlacement ?? []) sources.push(`biome:${normalizeId(biome)}`)
  for (const structure of block.structurePlacement ?? []) sources.push(`structure:${structure}`)
  if (block.spawnLayer) sources.push(`cave_layer:${block.spawnLayer}`)
  if (block.recipeSource) sources.push(`recipe:${normalizeRecipeRef(block.recipeSource)}`)
  for (const recipe of recipeIndexes.byOutput.get(id) ?? []) sources.push(`recipe:${recipe}`)
  for (const drop of dropIndex.blockSources.get(id) ?? []) sources.push(drop)
  if (block.stateMachine) sources.push('waystone_state_machine')
  if (!sources.length) sources.push(`registry:${block.category}`)
  return unique(sources)
}

function acquisitionForItem(item, recipeIndexes, dropIndex) {
  const id = normalizeId(item.id)
  const sources = []
  for (const recipe of item.recipeRefs ?? []) sources.push(`recipe_or_discovery:${normalizeRecipeRef(recipe)}`)
  for (const recipe of recipeIndexes.byOutput.get(id) ?? []) sources.push(`recipe:${recipe}`)
  for (const drop of dropIndex.itemSources.get(id) ?? []) sources.push(drop)
  if (!sources.length) sources.push(`registry:${item.useType}`)
  return unique(sources)
}

function buildCatalog({ moduleRoot }) {
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const blocksPayload = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json'))
  const itemsPayload = readJson(path.join(dataRoot, 'items', 'mvp_items.json'))
  const recipesPayload = readJson(path.join(dataRoot, 'recipes', 'mvp_recipes.json'))
  const lootPayload = readJson(path.join(dataRoot, 'loot', 'mvp_loot.json'))
  const firstHourPayload = readJson(path.join(dataRoot, 'progression', 'first_hour_route.json'))
  const roadmapPayload = readJson(path.join(dataRoot, 'progression', 'launch_roadmap.json'))
  const blocks = blocksPayload.blocks ?? []
  const items = itemsPayload.items ?? []
  const recipes = recipesPayload.recipes ?? []
  const recipeIndexes = buildRecipeIndexes(recipes)
  const dropIndex = buildDropIndex(blocks, lootPayload)

  const blockEntries = blocks.map((block) => ({
    id: normalizeId(block.id),
    namespacedId: block.id,
    displayName: block.displayName,
    category: block.category,
    progressionStage: progressionStageForBlock(block),
    gameplayRoles: blockRoles(block),
    acquisition: acquisitionForBlock(block, recipeIndexes, dropIndex),
    playerUse: playerUseForBlock(block),
    tool: block.tool,
    hardness: block.hardness,
    drops: block.drops ?? [],
    bonusDrops: block.bonusDrops ?? [],
    tags: block.tags ?? [],
    worldPlacement: {
      biomes: (block.biomePlacement ?? []).map(normalizeId),
      structures: block.structurePlacement ?? [],
      spawnLayer: block.spawnLayer ?? null,
    },
    crafting: {
      recipeSource: block.recipeSource ? normalizeRecipeRef(block.recipeSource) : null,
      consumedByRecipes: unique(recipeIndexes.byInput.get(normalizeId(block.id)) ?? []),
      stationProcesses: block.processes ?? [],
    },
    systems: {
      shelterScore: block.shelterScore ?? 0,
      light: block.light ?? 0,
      storageSlots: block.slots ?? 0,
      stateMachine: block.stateMachine ?? null,
      effects: block.effects ?? [],
      plannedVariants: block.plannedVariants ?? [],
    },
    runtimeParity: EXPECTED_RUNTIMES,
    runtimeNote: 'Adapters must preserve this Echo block id and map runtime-specific handles back to it.',
    designNote: block.notes,
  }))

  const itemEntries = items.map((item) => ({
    id: normalizeId(item.id),
    displayName: item.displayName,
    useType: item.useType,
    progressionStage: progressionStageForItem(item),
    gameplayRoles: itemRoles(item),
    acquisition: acquisitionForItem(item, recipeIndexes, dropIndex),
    playerUse: playerUseForItem(item),
    stackSize: item.stackSize,
    tags: item.tags ?? [],
    recipeRefs: (item.recipeRefs ?? []).map(normalizeRecipeRef),
    consumedByRecipes: unique(recipeIndexes.byInput.get(normalizeId(item.id)) ?? []),
    nutrition: item.nutrition ?? null,
    toolStats: item.toolStats ?? null,
    equipment: item.equipment ?? null,
    placesBlock: item.placesBlock ?? null,
    runtimeParity: EXPECTED_RUNTIMES,
    runtimeNote: 'Adapters must preserve this Echo item id and map runtime-specific handles back to it.',
    designNote: item.notes,
  }))

  const roleCoverage = {
    blocks: unique(blockEntries.flatMap((entry) => entry.gameplayRoles)).sort(),
    items: unique(itemEntries.flatMap((entry) => entry.gameplayRoles)).sort(),
    progressionStages: unique([...blockEntries, ...itemEntries].map((entry) => entry.progressionStage)).sort(),
  }

  return {
    schema: 'echo.openlands.gameplay_catalog.v1',
    namespace: MODULE_ID,
    runtimeParity: EXPECTED_RUNTIMES,
    generatedFrom: [
      'blocks/mvp_blocks.json',
      'items/mvp_items.json',
      'recipes/mvp_recipes.json',
      'loot/mvp_loot.json',
      'progression/first_hour_route.json',
      'progression/launch_roadmap.json',
    ],
    designRules: [
      'Every MVP block and item must have an acquisition path, gameplay role, player use, progression stage, and runtime parity note.',
      'Openlands Standard stays relaxed: food, shelter, and farming entries must avoid hardcore upkeep pressure.',
      'Echo IDs remain the source of truth across Native, Standalone, and NeoForge adapters.',
    ],
    firstHourPromise: firstHourPayload.playerPromise,
    roadmapDefaultRule: roadmapPayload.defaultRule,
    counts: {
      blocks: blockEntries.length,
      items: itemEntries.length,
      recipes: recipes.length,
    },
    roleCoverage,
    blockEntries,
    itemEntries,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands', 'index', 'mvp_gameplay_catalog.json')
  const catalog = buildCatalog({ moduleRoot })
  if (!args.dryRun) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true })
    fs.writeFileSync(outputPath, `${JSON.stringify(catalog, null, 2)}\n`, 'utf8')
  }
  if (args.json) {
    console.log(JSON.stringify(catalog, null, 2))
  } else {
    const action = args.dryRun ? 'validated' : `wrote ${outputPath}`
    console.log(`Openlands gameplay catalog ${action}: ${catalog.counts.blocks} blocks, ${catalog.counts.items} items, ${catalog.roleCoverage.progressionStages.length} progression stages.`)
  }
  return catalog
}

function printHelp() {
  console.log(`Usage: node scripts/generate-openlands-gameplay-catalog.mjs [options]

Options:
  --module-root <path>   Openlands module root. Defaults to this script's module.
  --out <path>           Output path. Defaults to data/.../index/mvp_gameplay_catalog.json.
  --dry-run              Build and validate without writing.
  --json                 Print JSON output.
  --help                 Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}

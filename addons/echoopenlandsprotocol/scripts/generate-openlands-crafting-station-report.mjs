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
const EXPECTED_STATION_COUNTS = {
  handcrafting: 0,
  field_bench: 3,
  kiln: 0,
  forge_hearth: 1,
  cookpot: 2,
  map_table: 4,
}
const REQUIRED_MAP_TABLE_RECIPES = ['region_rubbing', 'old_road_token', 'waystone_core', 'route_binding']
const DEFERRED_STATION_BLOCKS = ['loom', 'mason_table']
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-crafting-station-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-crafting-station-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-crafting-station-report.json',
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
  const extractRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-crafting-stations-'))
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

function entryRefs(entries) {
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
  const recipesPayload = readJson(path.join(dataRoot, 'recipes', 'mvp_recipes.json'))
  const blocksPayload = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json'))
  const itemsPayload = readJson(path.join(dataRoot, 'items', 'mvp_items.json'))
  const tagsPayload = readJson(path.join(dataRoot, 'tags', 'mvp_tags.json'))
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const aliasBridge = readJson(path.join(dataRoot, 'foundation', 'foundation_alias_bridge.json'))
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))

  assertRuntimeParity(recipesPayload, 'recipes')
  assertRuntimeParity(blocksPayload, 'blocks')
  assertRuntimeParity(itemsPayload, 'items')
  assertRuntimeParity(tagsPayload, 'tags')
  assertRuntimeParity(conformance, 'conformance')
  assert(evidenceTemplate.packId === edition.packId, `${edition.packId} evidence template packId mismatch`)
  assert(evidenceTemplate.runtimeTarget === edition.runtimeTarget, `${edition.packId} evidence template runtime target mismatch`)

  const blockIds = addFoundationKnownBlocks(new Set((blocksPayload.blocks ?? []).map((block) => normalizeId(block.id))), conformance, aliasBridge)
  const itemIds = addFoundationKnownItems(new Set((itemsPayload.items ?? []).map((item) => normalizeId(item.id))), conformance, aliasBridge)
  const recipeIds = (recipesPayload.recipes ?? []).map((recipe) => normalizeId(recipe.id))
  const recipeIdSet = new Set(recipeIds)
  const stationIds = [
    ...(recipesPayload.foundationStations ?? []),
    ...(recipesPayload.stations ?? []).map((station) => station.id),
  ].map((station) => normalizeId(station))
  const stationIdSet = new Set(stationIds)
  assert(sameSet(recipeIds, conformance.recipeRegistry ?? []), 'recipe registry does not match conformance')
  assert(sameSet(stationIds, Object.keys(EXPECTED_STATION_COUNTS)), 'MVP station ids mismatch')

  const stationSummaries = []
  const stationDefinitions = new Map((recipesPayload.stations ?? []).map((station) => [normalizeId(station.id), station]))
  for (const stationId of stationIds) {
    const station = stationDefinitions.get(stationId)
    if (station?.requiresBlock !== null && station?.requiresBlock !== undefined) {
      assert(blockIds.has(normalizeId(station.requiresBlock)), `station ${stationId} requires unknown block ${station.requiresBlock}`)
    }
    if (station?.grid) {
      assert(String(station.grid).startsWith('freeform_'), `station ${stationId} must use Openlands freeform grid identity`)
    }
    assert(!/crafting table/i.test(station?.displayName ?? ''), `station ${stationId} must not use copied crafting table identity`)
    const assignedRecipes = (recipesPayload.recipes ?? []).filter((recipe) => normalizeId(recipe.station) === stationId)
    assert(assignedRecipes.length === EXPECTED_STATION_COUNTS[stationId], `station ${stationId} recipe count mismatch`)
    stationSummaries.push({
      id: stationId,
      owner: station ? MODULE_ID : 'foundation',
      requiresBlock: station?.requiresBlock ?? null,
      recipeCount: assignedRecipes.length,
      process: station?.process ?? station?.grid ?? 'foundation_surface',
    })
  }

  const nonInventoryUnlockRefs = []
  for (const recipe of recipesPayload.recipes ?? []) {
    const recipeId = normalizeId(recipe.id)
    assert(stationIdSet.has(normalizeId(recipe.station)), `recipe ${recipeId} references unknown station ${recipe.station}`)
    assert(Number.isInteger(recipe.timeTicks) && recipe.timeTicks > 0, `recipe ${recipeId} must define positive timeTicks`)
    assert(Array.isArray(recipe.unlockedBy) && recipe.unlockedBy.length > 0, `recipe ${recipeId} must define unlock refs`)
    for (const ref of entryRefs(recipe.inputs)) {
      assert(itemIds.has(ref) || blockIds.has(ref), `recipe ${recipeId} input references unknown id ${ref}`)
    }
    for (const ref of entryRefs(recipe.outputs)) {
      assert(itemIds.has(ref) || blockIds.has(ref), `recipe ${recipeId} output references unknown id ${ref}`)
    }
    for (const unlock of recipe.unlockedBy ?? []) {
      const normalized = normalizeId(unlock)
      if (!recipeIdSet.has(normalized) && !itemIds.has(normalized) && !blockIds.has(normalized)) {
        nonInventoryUnlockRefs.push({ recipe: recipeId, unlock })
      }
    }
  }

  for (const [station, expectedCount] of Object.entries(EXPECTED_STATION_COUNTS)) {
    assert((recipesPayload.recipes ?? []).filter((recipe) => normalizeId(recipe.station) === station).length === expectedCount, `${station} recipe count mismatch`)
  }
  for (const recipeId of REQUIRED_MAP_TABLE_RECIPES) {
    const recipe = (recipesPayload.recipes ?? []).find((entry) => normalizeId(entry.id) === recipeId)
    assert(recipe, `map table recipe missing ${recipeId}`)
    assert(normalizeId(recipe.station) === 'map_table', `recipe ${recipeId} must be assigned to map_table`)
  }
  for (const recipe of recipesPayload.recipes ?? []) {
    const station = normalizeId(recipe.station)
    if (['kiln', 'forge_hearth', 'cookpot'].includes(station)) {
      assert(recipe.timeTicks >= 80, `process recipe ${recipe.id} should take at least 80 ticks`)
    }
    if (station === 'map_table') {
      assert(recipe.timeTicks >= 40, `map table recipe ${recipe.id} should take at least 40 ticks`)
    }
  }
  for (const block of DEFERRED_STATION_BLOCKS) {
    assert(blockIds.has(block), `deferred station block missing ${block}`)
    assert(!stationIdSet.has(block), `deferred station block ${block} should not be an MVP recipe station yet`)
  }
  const stationSurfaceStep = (runtimePlan.loadSteps ?? []).find((step) => step.id === 'register_recipes_and_station_surfaces')
  assert(stationSurfaceStep?.resourceIds?.includes('recipes/mvp_recipes'), 'runtime load step must include recipes/mvp_recipes')
  assert(stationSurfaceStep?.requiredEvidence?.includes('recipe_ids_registered'), 'runtime load step must require recipe_ids_registered')
  assert(stationSurfaceStep?.requiredEvidence?.includes('station_surfaces_bound'), 'runtime load step must require station_surfaces_bound')

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifact = artifactRuntimeEntries(artifactPath, edition)
  const runtimeEntriesChecked = [
    'data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json',
    'data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json',
    'data/echoopenlandsprotocol/openlands/items/mvp_items.json',
    'data/echoopenlandsprotocol/openlands/tags/mvp_tags.json',
    'data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json',
    'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json',
  ]
  for (const entry of runtimeEntriesChecked) {
    assert(artifact.runtimeEntries.includes(entry), `${edition.artifactName} missing runtime entry ${entry}`)
  }

  const report = {
    schema: 'echo.openlands.edition.crafting_station_report.v1',
    status: 'preflight_passed',
    realRuntimeStationExecutionRequiredBeforePublicAlpha: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    recipeContract: 'data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json',
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      nestedRuntimeEntry: artifact.nestedRuntimeEntry,
      runtimeEntriesChecked,
    },
    recipeCount: recipeIds.length,
    stationSummaries,
    expectedStationCounts: EXPECTED_STATION_COUNTS,
    processStations: Object.keys(EXPECTED_STATION_COUNTS).filter((station) => EXPECTED_STATION_COUNTS[station] > 0),
    requiredMapTableRecipes: REQUIRED_MAP_TABLE_RECIPES,
    deferredStationBlocks: DEFERRED_STATION_BLOCKS,
    nonInventoryUnlockRefs,
    stationSurfaceLoadStep: {
      id: stationSurfaceStep.id,
      successSignal: stationSurfaceStep.successSignal,
      requiredEvidence: stationSurfaceStep.requiredEvidence,
    },
    blockedBy: [
      'real_runtime_station_execution_missing',
      'station_ui_smoke_test_missing',
      'recipe_process_save_load_test_missing',
      'neoforge_recipe_output_generation_missing',
    ],
    outputPath,
    proofs: [
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
    console.log(`Openlands ${args.edition} crafting station report ${action}: ${report.recipeCount} recipes, ${report.stationSummaries.length} stations.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-crafting-station-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-crafting-station-report.json.
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

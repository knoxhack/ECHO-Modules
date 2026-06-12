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
const EXPECTED_BIOMES = ['meadows', 'woodlands', 'stonehills', 'marshlands']
const EXPECTED_LANDMARKS = [
  'ruined_well',
  'road_marker',
  'tiny_camp',
  'watchtower',
  'old_mine',
  'broken_bridge',
  'cellar_entrance',
  'broken_waystone_site',
]
const EXPECTED_HOLOMAP_LAYERS = ['region_names', 'old_roads', 'waystones', 'nearby_hints', 'player_markers']
const EXPECTED_HINT_TYPES = ['cave_mouth', 'old_mine', 'ruin', 'resource_patch', 'road_segment']
const EXPECTED_CREATURES = ['hare', 'deer', 'boar', 'goat', 'marsh_hen', 'fish', 'greyling', 'bristleback', 'hollow_stalker', 'mire_leech']
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-worldgen-exploration-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-worldgen-exploration-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-worldgen-exploration-report.json',
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
  const extractRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-worldgen-exploration-'))
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

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)
  assert(fileExists(moduleRoot), `module root not found: ${moduleRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const biomesPayload = readJson(path.join(dataRoot, 'biomes', 'mvp_biomes.json'))
  const structuresPayload = readJson(path.join(dataRoot, 'structures', 'mvp_landmarks.json'))
  const creaturesPayload = readJson(path.join(dataRoot, 'creatures', 'mvp_creatures.json'))
  const holomapPayload = readJson(path.join(dataRoot, 'holomap', 'mvp_regions.json'))
  const blocksPayload = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json'))
  const itemsPayload = readJson(path.join(dataRoot, 'items', 'mvp_items.json'))
  const tagsPayload = readJson(path.join(dataRoot, 'tags', 'mvp_tags.json'))
  const lootPayload = readJson(path.join(dataRoot, 'loot', 'mvp_loot.json'))
  const tutorialsPayload = readJson(path.join(dataRoot, 'tutorials', 'first_hour_prompts.json'))
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const aliasBridge = readJson(path.join(dataRoot, 'foundation', 'foundation_alias_bridge.json'))
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const soundsPayload = readJson(path.join(resourcesRoot, 'assets', MODULE_ID, 'sounds.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))

  assertRuntimeParity(biomesPayload, 'biomes')
  assertRuntimeParity(structuresPayload, 'structures')
  assertRuntimeParity(creaturesPayload, 'creatures')
  assertRuntimeParity(holomapPayload, 'holomap')
  assertRuntimeParity(blocksPayload, 'blocks')
  assertRuntimeParity(itemsPayload, 'items')
  assertRuntimeParity(tagsPayload, 'tags')
  assertRuntimeParity(lootPayload, 'loot')
  assertRuntimeParity(tutorialsPayload, 'tutorials')
  assertRuntimeParity(conformance, 'conformance')
  assert(evidenceTemplate.packId === edition.packId, `${edition.packId} evidence template packId mismatch`)
  assert(evidenceTemplate.runtimeTarget === edition.runtimeTarget, `${edition.packId} evidence template runtime target mismatch`)

  const blockIds = addFoundationKnownBlocks(new Set((blocksPayload.blocks ?? []).map((block) => normalizeId(block.id))), conformance, aliasBridge)
  const itemIds = addFoundationKnownItems(new Set((itemsPayload.items ?? []).map((item) => normalizeId(item.id))), conformance, aliasBridge)
  const biomeIds = (biomesPayload.biomes ?? []).map((biome) => normalizeId(biome.id))
  const biomeIdSet = new Set(biomeIds)
  const creatureIds = (creaturesPayload.creatures ?? []).map((creature) => normalizeId(creature.id))
  const creatureIdSet = new Set(creatureIds)
  const landmarkIds = (structuresPayload.landmarks ?? []).map((landmark) => normalizeId(landmark.id))
  const landmarkIdSet = new Set(landmarkIds)
  const itemTagIds = new Set(Object.keys(tagsPayload.itemTags ?? {}))
  const blockTagIds = new Set(Object.keys(tagsPayload.blockTags ?? {}))
  const lootTableIds = new Set((lootPayload.chestTables ?? []).map((table) => normalizeId(table.id)))
  const tutorialPromptIds = new Set((tutorialsPayload.prompts ?? []).map((prompt) => normalizeId(prompt.id)))
  const soundKeys = new Set(Object.keys(soundsPayload ?? {}))

  assert(sameSet(biomeIds, EXPECTED_BIOMES), 'biome ids mismatch')
  assert(sameSet(biomeIds, conformance.biomeRegistry ?? []), 'biome registry does not match conformance')
  assert(sameSet(creatureIds, EXPECTED_CREATURES), 'creature ids mismatch')
  assert(sameSet(creatureIds, conformance.creatureRegistry ?? []), 'creature registry does not match conformance')
  assert(sameSet(landmarkIds, EXPECTED_LANDMARKS), 'landmark ids mismatch')
  assert(biomesPayload.spawnSafetyContract?.maximumUnsafeSpawnAttempts === 64, 'spawn safety maximum attempts mismatch')
  assert(biomesPayload.spawnSafetyContract?.starterRadiusBlocks === 96, 'spawn safety starter radius mismatch')
  assert((biomesPayload.spawnSafetyContract?.guarantees ?? []).length >= 6, 'spawn safety must list six starter guarantees')

  const worldgenResourceMarkers = []
  const biomeSummaries = []
  for (const biome of biomesPayload.biomes ?? []) {
    const biomeId = normalizeId(biome.id)
    const paletteRefs = collectPaletteRefs(biome.blockPalette)
    for (const ref of paletteRefs) {
      const normalized = normalizeId(ref)
      if (!blockIds.has(normalized)) worldgenResourceMarkers.push({ source: `biome:${biomeId}:palette`, resource: ref })
    }
    for (const resource of biome.resourceSet ?? []) {
      const normalized = normalizeId(resource)
      if (!blockIds.has(normalized) && !itemIds.has(normalized)) {
        worldgenResourceMarkers.push({ source: `biome:${biomeId}:resourceSet`, resource })
      }
    }
    for (const spawn of biome.spawnTable ?? []) {
      assert(creatureIdSet.has(normalizeId(spawn.creature)), `biome ${biomeId} spawn references unknown creature ${spawn.creature}`)
      assert(Number.isInteger(spawn.weight) && spawn.weight > 0, `biome ${biomeId} spawn ${spawn.creature} must have positive weight`)
    }
    for (const [soundName, soundRef] of Object.entries(biome.ambience ?? {})) {
      if (soundName === 'musicHint') continue
      assert(soundKeys.has(soundKey(soundRef)), `biome ${biomeId} ambience ${soundName} missing sound key ${soundRef}`)
    }
    for (const landmark of Object.keys(biome.landmarkFrequency ?? {})) {
      assert(landmarkIdSet.has(landmarkIdFromFrequency(landmark)), `biome ${biomeId} landmarkFrequency references unknown landmark ${landmark}`)
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
  for (const landmark of structuresPayload.landmarks ?? []) {
    const landmarkId = normalizeId(landmark.id)
    for (const block of landmark.blocks ?? []) {
      assert(blockIds.has(normalizeId(block)), `landmark ${landmarkId} references unknown block ${block}`)
    }
    for (const biome of landmark.preferredBiomes ?? []) {
      assert(biomeIdSet.has(normalizeId(biome)), `landmark ${landmarkId} references unknown preferred biome ${biome}`)
    }
    if (landmark.lootTable !== null) {
      assert(lootTableIds.has(normalizeId(landmark.lootTable)), `landmark ${landmarkId} references unknown loot table ${landmark.lootTable}`)
    }
    if (landmark.holoMapHint) landmarkHoloMapHintMarkers.push({ landmark: landmarkId, hint: landmark.holoMapHint })
    if (landmark.tutorialHook && !tutorialPromptIds.has(normalizeId(landmark.tutorialHook))) {
      nonPromptTutorialHooks.push({ landmark: landmarkId, tutorialHook: landmark.tutorialHook })
    }
    for (const field of ['width', 'depth', 'height']) {
      assert(Number.isInteger(landmark.footprint?.[field]) && landmark.footprint[field] > 0, `landmark ${landmarkId} footprint ${field} must be positive`)
    }
  }

  for (const field of ['regionId', 'displayName', 'seedSalt', 'biomeType', 'discoveredAt', 'nearbyHints', 'restoredWaystones', 'playerMarkers', 'oldRoadSegments']) {
    assert(holomapPayload.regionDataContract?.storedFields?.includes(field), `HoloMap region data missing field ${field}`)
  }
  assert(holomapPayload.regionDataContract?.fallbackIfHoloMapMissing?.includes('Echo Index'), 'HoloMap fallback must mention Echo Index')
  assert(sameSet((holomapPayload.layers ?? []).map((layer) => layer.id), EXPECTED_HOLOMAP_LAYERS), 'HoloMap layer ids mismatch')
  assert(sameSet((holomapPayload.hintTypes ?? []).map((hint) => hint.id), EXPECTED_HINT_TYPES), 'HoloMap hint type ids mismatch')
  for (const biomeId of EXPECTED_BIOMES) {
    assert((holomapPayload.starterRegionNamePools?.[biomeId] ?? []).length >= 4, `HoloMap starter name pool missing entries for ${biomeId}`)
  }

  const creatureSoundEvents = []
  for (const creature of creaturesPayload.creatures ?? []) {
    const creatureId = normalizeId(creature.id)
    for (const biome of creature.biomes ?? []) {
      assert(biomeIdSet.has(normalizeId(biome)), `creature ${creatureId} references unknown biome ${biome}`)
    }
    for (const tag of creature.spawnRules?.surfaceTags ?? []) {
      assert(blockTagIds.has(tag), `creature ${creatureId} references unknown surface tag ${tag}`)
    }
    assert(Number.isInteger(creature.health) && creature.health > 0, `creature ${creatureId} must have positive health`)
    assert(Number.isInteger(creature.damage) && creature.damage >= 0, `creature ${creatureId} must have non-negative damage`)
    assert((creature.ai ?? []).length >= 3, `creature ${creatureId} must declare at least three AI hints`)
    for (const soundRef of Object.values(creature.sounds ?? {})) {
      assert(soundKeys.has(soundKey(soundRef)), `creature ${creatureId} missing sound key ${soundRef}`)
      creatureSoundEvents.push(soundKey(soundRef))
    }
  }

  for (const requiredFoodTag of ['openlands:food']) {
    assert(itemTagIds.has(requiredFoodTag), `worldgen report expected item tag ${requiredFoodTag}`)
  }
  const worldgenStep = (runtimePlan.loadSteps ?? []).find((step) => step.id === 'bind_biomes_structures_creatures_and_spawn')
  assert(worldgenStep?.resourceIds?.includes('biomes/mvp_biomes'), 'worldgen load step must include biomes')
  assert(worldgenStep?.resourceIds?.includes('structures/mvp_landmarks'), 'worldgen load step must include structures')
  assert(worldgenStep?.resourceIds?.includes('creatures/mvp_creatures'), 'worldgen load step must include creatures')
  for (const evidence of ['biome_palettes_bound', 'spawn_tables_bound', 'landmark_pools_bound', 'starter_spawn_guarantees_bound']) {
    assert(worldgenStep?.requiredEvidence?.includes(evidence), `worldgen load step missing evidence ${evidence}`)
  }

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifact = artifactRuntimeEntries(artifactPath, edition)
  const runtimeEntriesChecked = [
    'data/echoopenlandsprotocol/openlands/biomes/mvp_biomes.json',
    'data/echoopenlandsprotocol/openlands/structures/mvp_landmarks.json',
    'data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json',
    'data/echoopenlandsprotocol/openlands/holomap/mvp_regions.json',
    'data/echoopenlandsprotocol/openlands/loot/mvp_loot.json',
    'data/echoopenlandsprotocol/openlands/tutorials/first_hour_prompts.json',
    'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json',
    'assets/echoopenlandsprotocol/sounds.json',
  ]
  for (const entry of runtimeEntriesChecked) {
    assert(artifact.runtimeEntries.includes(entry), `${edition.artifactName} missing runtime entry ${entry}`)
  }

  const report = {
    schema: 'echo.openlands.edition.worldgen_exploration_report.v1',
    status: 'preflight_passed',
    realRuntimeWorldgenRequiredBeforePublicAlpha: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    contracts: {
      biomes: 'data/echoopenlandsprotocol/openlands/biomes/mvp_biomes.json',
      landmarks: 'data/echoopenlandsprotocol/openlands/structures/mvp_landmarks.json',
      creatures: 'data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json',
      holomap: 'data/echoopenlandsprotocol/openlands/holomap/mvp_regions.json',
    },
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      nestedRuntimeEntry: artifact.nestedRuntimeEntry,
      runtimeEntriesChecked,
    },
    counts: {
      biomes: biomeIds.length,
      landmarks: landmarkIds.length,
      creatures: creatureIds.length,
      holomapLayers: (holomapPayload.layers ?? []).length,
      holomapHintTypes: (holomapPayload.hintTypes ?? []).length,
      creatureSoundEvents: new Set(creatureSoundEvents).size,
    },
    spawnSafety: biomesPayload.spawnSafetyContract,
    biomeSummaries,
    landmarkIds,
    creatureIds,
    holomapLayers: (holomapPayload.layers ?? []).map((layer) => layer.id),
    holomapHintTypes: (holomapPayload.hintTypes ?? []).map((hint) => hint.id),
    worldgenResourceMarkers,
    landmarkHoloMapHintMarkers,
    nonPromptTutorialHooks,
    worldgenLoadStep: {
      id: worldgenStep.id,
      successSignal: worldgenStep.successSignal,
      requiredEvidence: worldgenStep.requiredEvidence,
    },
    blockedBy: [
      'real_runtime_worldgen_execution_missing',
      'starter_spawn_seed_sweep_missing',
      'landmark_generation_smoke_test_missing',
      'creature_spawn_parity_test_missing',
      'holomap_reveal_runtime_test_missing',
    ],
    outputPath,
    proofs: [
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
    console.log(`Openlands ${args.edition} worldgen exploration report ${action}: ${report.counts.biomes} biomes, ${report.counts.landmarks} landmarks, ${report.counts.creatures} creatures.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-worldgen-exploration-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-worldgen-exploration-report.json.
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

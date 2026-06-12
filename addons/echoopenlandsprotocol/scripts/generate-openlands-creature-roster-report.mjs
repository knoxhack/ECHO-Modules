import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { addFoundationKnownItems } from './openlands-foundation-id-resolver.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EXPECTED_RUNTIMES = ['echo_native', 'echo_runtime_standalone', 'neoforge']
const EXPECTED_CREATURES = ['hare', 'deer', 'boar', 'goat', 'marsh_hen', 'fish', 'greyling', 'bristleback', 'hollow_stalker', 'mire_leech']
const EXPECTED_CATEGORIES = [
  'passive_small',
  'passive_large',
  'neutral',
  'aquatic_passive',
  'hostile_small',
  'hostile_large',
  'hostile_rare',
]
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-creature-roster-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-creature-roster-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-creature-roster-report.json',
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
  const extractRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-creature-roster-'))
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

function soundKey(value) {
  if (typeof value !== 'string') return value
  return value.replace(/^openlands:/, 'openlands.')
}

function sorted(values) {
  return [...(values ?? [])].sort()
}

function sameSet(actual, expected) {
  return JSON.stringify(sorted(actual)) === JSON.stringify(sorted(expected))
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

function byId(records, key = 'id') {
  return new Map((records ?? []).map((record) => [normalizeId(record[key]), record]))
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
  return spawnRules.minimumDistanceFromWorldSpawn ?? spawnRules.minimumSpawnDistanceFromWorldSpawn ?? 0
}

function countBy(records, keyFn) {
  const counts = {}
  for (const record of records ?? []) {
    const key = keyFn(record)
    counts[key] = (counts[key] ?? 0) + 1
  }
  return counts
}

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)
  assert(fileExists(moduleRoot), `module root not found: ${moduleRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const creaturesPayload = readJson(path.join(dataRoot, 'creatures', 'mvp_creatures.json'))
  const lootPayload = readJson(path.join(dataRoot, 'loot', 'mvp_loot.json'))
  const biomesPayload = readJson(path.join(dataRoot, 'biomes', 'mvp_biomes.json'))
  const itemsPayload = readJson(path.join(dataRoot, 'items', 'mvp_items.json'))
  const tagsPayload = readJson(path.join(dataRoot, 'tags', 'mvp_tags.json'))
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const aliasBridge = readJson(path.join(dataRoot, 'foundation', 'foundation_alias_bridge.json'))
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const playtestPayload = readJson(path.join(dataRoot, 'playtests', 'mvp_first_hour_acceptance.json'))
  const soundsPayload = readJson(path.join(resourcesRoot, 'assets', MODULE_ID, 'sounds.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))

  assertRuntimeParity(creaturesPayload, 'creatures')
  assertRuntimeParity(lootPayload, 'loot')
  assertRuntimeParity(biomesPayload, 'biomes')
  assertRuntimeParity(itemsPayload, 'items')
  assertRuntimeParity(tagsPayload, 'tags')
  assertRuntimeParity(conformance, 'conformance')
  assertRuntimeParity(playtestPayload, 'playtest')
  assert(evidenceTemplate.packId === edition.packId, `${edition.packId} evidence template packId mismatch`)
  assert(evidenceTemplate.runtimeTarget === edition.runtimeTarget, `${edition.packId} evidence template runtime target mismatch`)

  assert(creaturesPayload.globalRules?.defaultHostility === 'moderate', 'creature default hostility must stay moderate')
  assert(creaturesPayload.globalRules?.avoidHardcorePressure === true, 'creature rules must avoid hardcore pressure')
  assert(creaturesPayload.globalRules?.noCopiedSilhouettes === true, 'creature rules must forbid copied silhouettes')
  assert(creaturesPayload.globalRules?.soundNamespace === MODULE_ID, 'creature sound namespace mismatch')

  const creatureIds = idList(creaturesPayload.creatures)
  assert(sameSet(creatureIds, EXPECTED_CREATURES), 'creature ids mismatch')
  assert(sameSet(creatureIds, conformance.creatureRegistry), 'creature ids must match conformance registry')

  const biomeIds = new Set(idList(biomesPayload.biomes))
  const itemIds = addFoundationKnownItems(new Set(idList(itemsPayload.items)), conformance, aliasBridge)
  const blockTagIds = new Set(Object.keys(tagsPayload.blockTags ?? {}))
  const soundKeys = new Set(Object.keys(soundsPayload ?? {}))
  const dropTables = byId(lootPayload.creatureDrops, 'creature')
  const biomeSpawnMap = new Map()
  for (const biome of biomesPayload.biomes ?? []) {
    for (const spawn of biome.spawnTable ?? []) {
      const creatureId = normalizeId(spawn.creature)
      assert(creatureIds.includes(creatureId), `biome ${biome.id} spawn references unknown creature ${spawn.creature}`)
      assert(Number.isInteger(spawn.weight) && spawn.weight > 0, `biome ${biome.id} spawn ${spawn.creature} must have positive weight`)
      const list = biomeSpawnMap.get(creatureId) ?? []
      list.push({ biome: normalizeId(biome.id), weight: spawn.weight })
      biomeSpawnMap.set(creatureId, list)
    }
  }

  const creatureSummaries = []
  const soundEvents = []
  const starterFriendlyCreatures = new Set(playtestPayload.acceptanceScenarios
    ?.find((scenario) => scenario.id === 'safe_spawn')
    ?.requires?.creaturesAllowed ?? [])
  for (const creature of creaturesPayload.creatures ?? []) {
    const creatureId = normalizeId(creature.id)
    const categoryId = creature.legacyCategory ?? normalizeId(creature.category)
    assert(EXPECTED_CATEGORIES.includes(categoryId), `creature ${creatureId} has unknown category ${creature.category}`)
    assert((creature.biomes ?? []).length > 0, `creature ${creatureId} must declare at least one biome`)
    for (const biome of creature.biomes ?? []) {
      assert(biomeIds.has(normalizeId(biome)), `creature ${creatureId} references unknown biome ${biome}`)
    }
    assert(biomeSpawnMap.has(creatureId), `creature ${creatureId} must appear in at least one biome spawn table`)
    assert(typeof creature.spawnRules?.time === 'string', `creature ${creatureId} must declare spawn time`)
    assert(typeof creature.spawnRules?.group === 'string', `creature ${creatureId} must declare spawn group`)
    assert(groupRangeMax(creature.spawnRules.group) > 0, `creature ${creatureId} spawn group must be parseable`)
    assert(Number.isInteger(creature.health) && creature.health > 0, `creature ${creatureId} must have positive health`)
    assert(Number.isInteger(creature.damage) && creature.damage >= 0, `creature ${creatureId} must have non-negative damage`)
    assert((creature.ai ?? []).length >= 3, `creature ${creatureId} must declare at least three AI hints`)
    if (categoryId.startsWith('hostile')) {
      assert(creature.damage > 0, `hostile creature ${creatureId} must have damage`)
      assert(minimumSpawnDistance(creature.spawnRules) >= 96, `hostile creature ${creatureId} must not spawn near world spawn`)
    }
    if (categoryId.startsWith('passive') || categoryId === 'aquatic_passive') {
      assert(creature.damage === 0, `passive creature ${creatureId} must not deal damage`)
    }
    if (creatureId === 'boar') {
      assert(categoryId === 'neutral', 'boar must remain neutral')
      assert(minimumSpawnDistance(creature.spawnRules) >= 48, 'boar must not spawn directly on the world spawn')
    }
    if (creature.spawnRules.surfaceTags) {
      for (const tag of creature.spawnRules.surfaceTags) {
        assert(blockTagIds.has(tag), `creature ${creatureId} references unknown surface tag ${tag}`)
      }
    }
    if (creature.spawnRules.fluid) {
      assert(creature.spawnRules.fluid === 'water', `creature ${creatureId} fluid spawn must be water`)
    }
    for (const [event, soundRef] of Object.entries(creature.sounds ?? {})) {
      assert(soundKeys.has(soundKey(soundRef)), `creature ${creatureId} ${event} sound key missing ${soundRef}`)
      soundEvents.push(soundKey(soundRef))
    }
    const dropTable = dropTables.get(creatureId)
    assert(dropTable, `creature ${creatureId} missing creatureDrops table`)
    assert((dropTable.drops ?? []).length > 0, `creature ${creatureId} drops table must not be empty`)
    for (const drop of dropTable.drops ?? []) {
      assert(itemIds.has(normalizeId(drop.item)), `creature ${creatureId} drop references unknown item ${drop.item}`)
      assert(drop.count !== undefined, `creature ${creatureId} drop ${drop.item} missing count`)
      if (drop.chance !== undefined) {
        assert(typeof drop.chance === 'number' && drop.chance > 0 && drop.chance <= 1, `creature ${creatureId} drop ${drop.item} chance must be 0-1`)
      }
    }
    creatureSummaries.push({
      id: creatureId,
      category: categoryId,
      biomes: creature.biomes,
      spawnTime: creature.spawnRules.time,
      group: creature.spawnRules.group,
      maxGroupSize: groupRangeMax(creature.spawnRules.group),
      minimumDistanceFromWorldSpawn: minimumSpawnDistance(creature.spawnRules),
      aiCount: creature.ai.length,
      soundCount: Object.keys(creature.sounds ?? {}).length,
      dropCount: (dropTable.drops ?? []).length,
      biomeSpawnEntries: biomeSpawnMap.get(creatureId),
      starterFriendly: starterFriendlyCreatures.has(creatureId),
    })
  }
  assert(sameSet([...dropTables.keys()], EXPECTED_CREATURES), 'creature drop tables must match creature ids')
  assert(new Set(soundEvents).size === 34, 'creature sound event count mismatch')
  assert(sameSet([...starterFriendlyCreatures], ['hare', 'deer']), 'safe spawn allowed creatures must remain hare and deer')

  const worldgenStep = (runtimePlan.loadSteps ?? []).find((step) => step.id === 'bind_biomes_structures_creatures_and_spawn')
  assert(worldgenStep?.resourceIds?.includes('creatures/mvp_creatures'), 'worldgen load step must include creatures')
  assert(worldgenStep?.requiredEvidence?.includes('spawn_tables_bound'), 'worldgen load step must require spawn_tables_bound')
  assert(worldgenStep?.successSignal === 'openlands_worldgen_bound', 'worldgen load step success signal mismatch')

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifact = artifactRuntimeEntries(artifactPath, edition)
  const runtimeEntriesChecked = [
    'data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json',
    'data/echoopenlandsprotocol/openlands/loot/mvp_loot.json',
    'data/echoopenlandsprotocol/openlands/biomes/mvp_biomes.json',
    'data/echoopenlandsprotocol/openlands/items/mvp_items.json',
    'data/echoopenlandsprotocol/openlands/tags/mvp_tags.json',
    'data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json',
    'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json',
    'data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json',
    'assets/echoopenlandsprotocol/sounds.json',
  ]
  for (const entry of runtimeEntriesChecked) {
    assert(artifact.runtimeEntries.includes(entry), `${edition.artifactName} missing runtime entry ${entry}`)
  }

  const report = {
    schema: 'echo.openlands.edition.creature_roster_report.v1',
    status: 'preflight_passed',
    realRuntimeCreatureExecutionRequiredBeforePublicAlpha: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    contracts: {
      creatures: 'data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json',
      loot: 'data/echoopenlandsprotocol/openlands/loot/mvp_loot.json',
      biomes: 'data/echoopenlandsprotocol/openlands/biomes/mvp_biomes.json',
      items: 'data/echoopenlandsprotocol/openlands/items/mvp_items.json',
      tags: 'data/echoopenlandsprotocol/openlands/tags/mvp_tags.json',
      conformance: 'data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json',
      runtimePlan: 'data/echoopenlandsprotocol/openlands/systems/runtime_adapter_load_plan.json',
      playtest: 'data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json',
      sounds: 'assets/echoopenlandsprotocol/sounds.json',
    },
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      nestedRuntimeEntry: artifact.nestedRuntimeEntry,
      runtimeEntriesChecked,
    },
    globalRules: creaturesPayload.globalRules,
    counts: {
      creatures: creatureIds.length,
      categories: new Set((creaturesPayload.creatures ?? []).map((creature) => creature.legacyCategory ?? normalizeId(creature.category))).size,
      creatureDropTables: (lootPayload.creatureDrops ?? []).length,
      creatureSoundEvents: new Set(soundEvents).size,
      passiveOrNeutralCreatures: creatureSummaries.filter((creature) => !creature.category.startsWith('hostile')).length,
      hostileCreatures: creatureSummaries.filter((creature) => creature.category.startsWith('hostile')).length,
    },
    creatureIds,
    categoryCounts: countBy(creaturesPayload.creatures, (creature) => creature.legacyCategory ?? normalizeId(creature.category)),
    creatureSummaries,
    starterSafety: {
      safeSpawnAllowedCreatures: [...starterFriendlyCreatures],
      hostilesMinimumDistanceBlocks: Math.min(...creatureSummaries
        .filter((creature) => creature.category.startsWith('hostile'))
        .map((creature) => creature.minimumDistanceFromWorldSpawn)),
      boarMinimumDistanceBlocks: creatureSummaries.find((creature) => creature.id === 'boar')?.minimumDistanceFromWorldSpawn,
      avoidHardcorePressure: creaturesPayload.globalRules.avoidHardcorePressure,
    },
    worldgenLoadStep: {
      id: worldgenStep.id,
      successSignal: worldgenStep.successSignal,
      requiredEvidence: worldgenStep.requiredEvidence,
    },
    blockedBy: [
      'real_runtime_creature_spawn_execution_missing',
      'creature_ai_behavior_smoke_test_missing',
      'creature_drop_runtime_test_missing',
      'creature_sound_runtime_test_missing',
      'creature_spawn_parity_test_missing',
    ],
    outputPath,
    proofs: [
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
    console.log(`Openlands ${args.edition} creature roster report ${action}: ${report.counts.creatures} creatures, ${report.counts.creatureDropTables} drop tables, ${report.counts.creatureSoundEvents} sound events.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-creature-roster-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-creature-roster-report.json.
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

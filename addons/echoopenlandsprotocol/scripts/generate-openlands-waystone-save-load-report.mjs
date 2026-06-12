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
const EXPECTED_WAYSTONE_STATES = [
  'undiscovered',
  'discovered',
  'debris_cleared',
  'stone_repaired',
  'fitted',
  'charged',
  'bound',
  'active',
]
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-waystone-save-load-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-waystone-save-load-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-waystone-save-load-report.json',
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
  const extractRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-waystone-'))
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

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function sameList(actual, expected) {
  return JSON.stringify(actual ?? []) === JSON.stringify(expected ?? [])
}

function validateRefs(values, knownIds, label) {
  for (const value of values ?? []) {
    assert(knownIds.has(normalizeId(value)), `${label} references unknown id ${value}`)
  }
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
  const waystones = readJson(path.join(dataRoot, 'waystones', 'waystone_contract.json'))
  const playtest = readJson(path.join(dataRoot, 'playtests', 'mvp_first_hour_acceptance.json'))
  const holomap = readJson(path.join(dataRoot, 'holomap', 'mvp_regions.json'))
  const blocks = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json')).blocks ?? []
  const items = readJson(path.join(dataRoot, 'items', 'mvp_items.json')).items ?? []
  const recipes = readJson(path.join(dataRoot, 'recipes', 'mvp_recipes.json')).recipes ?? []
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const aliasBridge = readJson(path.join(dataRoot, 'foundation', 'foundation_alias_bridge.json'))
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))
  const runtimeCoreReport = readJson(path.join(editionRoot, evidenceTemplate.evidenceAttachments.runtimeCoreReport))

  assert(runtimeCoreReport.status === 'passed', `${edition.packId} runtime core report must pass before waystone preflight`)
  assert(runtimeCoreReport.runtimeTarget === edition.runtimeTarget, `${edition.packId} runtime core report target mismatch`)

  const stateIds = (waystones.stateMachine ?? []).map((state) => state.state)
  assert(sameList(stateIds, EXPECTED_WAYSTONE_STATES), 'waystone state order mismatch')
  for (let index = 0; index < EXPECTED_WAYSTONE_STATES.length; index += 1) {
    const state = waystones.stateMachine[index]
    const expectedNext = EXPECTED_WAYSTONE_STATES[index + 1] ?? null
    assert(state.next === expectedNext, `waystone state ${state.state} next must be ${expectedNext}`)
  }
  assert(waystones.effects?.fastTravel?.requiresActiveStones === 2, 'fast travel must require two active waystones')

  const blockIds = addFoundationKnownBlocks(new Set(blocks.map((block) => normalizeId(block.id))), conformance, aliasBridge)
  const itemIds = addFoundationKnownItems(new Set(items.map((item) => normalizeId(item.id))), conformance, aliasBridge)
  const recipeIds = addFoundationKnownRecipes(new Set(recipes.map((recipe) => normalizeId(recipe.id))), conformance, aliasBridge)
  const evidenceIds = new Set((runtimePlan.runtimeEvidenceRequirements ?? []).map((evidence) => evidence.id))
  validateRefs(waystones.blocks, blockIds, 'waystone contract blocks')
  for (const state of waystones.stateMachine ?? []) {
    for (const input of state.inputs ?? []) {
      if (input.item) validateRefs([input.item], itemIds, `waystone state ${state.state} inputs`)
      for (const alternate of input.alternates ?? []) {
        if (String(alternate).startsWith('block:')) validateRefs([String(alternate).replace(/^block:/, '')], blockIds, `waystone state ${state.state} alternates`)
      }
    }
  }

  const firstWaystoneCheckpoint = (playtest.saveLoadCheckpoints ?? []).find((checkpoint) => checkpoint.id === 'after_first_waystone_repair')
  assert(firstWaystoneCheckpoint, 'playtest fixture missing after_first_waystone_repair checkpoint')
  for (const field of ['inventory', 'hotbar', 'placedBlocks', 'waystoneState', 'holomapRegionDiscovery']) {
    assert(firstWaystoneCheckpoint.mustPersist?.includes(field), `after_first_waystone_repair checkpoint missing ${field}`)
  }
  validateRefs(firstWaystoneCheckpoint.sampleInventoryItems, itemIds, 'after_first_waystone_repair sampleInventoryItems')
  validateRefs(firstWaystoneCheckpoint.samplePlacedBlocks, blockIds, 'after_first_waystone_repair samplePlacedBlocks')

  const publicAlphaScenario = playtest.waystonePublicAlphaScenario
  assert(publicAlphaScenario?.id === evidenceTemplate.waystonePublicAlphaScenario, 'waystone public alpha scenario id mismatch')
  assert(sameList(publicAlphaScenario.requiresStates, EXPECTED_WAYSTONE_STATES), 'waystone public alpha scenario state order mismatch')
  validateRefs(publicAlphaScenario.requiresItems, itemIds, 'waystone public alpha requiresItems')
  validateRefs(publicAlphaScenario.requiresRecipes, recipeIds, 'waystone public alpha requiresRecipes')
  validateRefs(publicAlphaScenario.requiresBlocks, blockIds, 'waystone public alpha requiresBlocks')
  validateRefs(publicAlphaScenario.successEvidence, evidenceIds, 'waystone public alpha successEvidence')
  for (const field of waystones.multiplayerState?.storedFields ?? []) {
    assert(publicAlphaScenario.mustPersist?.includes(field), `waystone public alpha mustPersist missing ${field}`)
  }
  for (const field of holomap.regionDataContract?.storedFields ?? []) {
    assert(playtest.holomapAcceptance?.mustPersistFields?.includes(field), `HoloMap acceptance missing persisted field ${field}`)
  }
  const holomapLayerIds = new Set((holomap.layers ?? []).map((layer) => layer.id))
  const hintTypeIds = new Set((holomap.hintTypes ?? []).map((hint) => hint.id))
  validateRefs(playtest.holomapAcceptance?.requiredLayers, holomapLayerIds, 'HoloMap requiredLayers')
  validateRefs(playtest.holomapAcceptance?.requiredHintTypes, hintTypeIds, 'HoloMap requiredHintTypes')

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifact = artifactRuntimeEntries(artifactPath, edition)
  const runtimeEntriesChecked = [
    'data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json',
    'data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json',
    'data/echoopenlandsprotocol/openlands/holomap/mvp_regions.json',
    'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsWaystoneRuntime.class',
    'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsWaystoneState.class',
  ]
  for (const entry of runtimeEntriesChecked) {
    assert(artifact.runtimeEntries.includes(entry), `${edition.artifactName} missing runtime entry ${entry}`)
  }

  const report = {
    schema: 'echo.openlands.edition.waystone_save_load_report.v1',
    status: 'preflight_passed',
    realRuntimeSaveLoadRequiredBeforePublicAlpha: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    waystoneContract: 'data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json',
    playtestFixture: 'data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json',
    holomapContract: 'data/echoopenlandsprotocol/openlands/holomap/mvp_regions.json',
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      nestedRuntimeEntry: artifact.nestedRuntimeEntry,
      runtimeEntriesChecked,
    },
    stateMachine: {
      states: stateIds,
      activeStonesRequiredForFastTravel: waystones.effects.fastTravel.requiresActiveStones,
      nearbyHintRange: waystones.effects.nearbyHints,
    },
    saveLoadCheckpoint: {
      id: firstWaystoneCheckpoint.id,
      afterScenario: firstWaystoneCheckpoint.afterScenario,
      persistedFields: firstWaystoneCheckpoint.mustPersist,
      requiredAssertions: firstWaystoneCheckpoint.requiredAssertions,
    },
    publicAlphaScenario: {
      id: publicAlphaScenario.id,
      states: publicAlphaScenario.requiresStates,
      effects: publicAlphaScenario.expectedEffects,
      persistedFields: publicAlphaScenario.mustPersist,
      successEvidence: publicAlphaScenario.successEvidence,
    },
    holomapAcceptance: playtest.holomapAcceptance,
    runtimeCoreReport: evidenceTemplate.evidenceAttachments.runtimeCoreReport,
    blockedBy: [
      'real_runtime_waystone_save_load_test_missing',
      'adapter_boot_report_missing',
      'registry_parity_report_missing',
    ],
    outputPath,
    proofs: [
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
    console.log(`Openlands ${args.edition} waystone save/load report ${action}: ${report.stateMachine.states.length} states.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-waystone-save-load-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-waystone-save-load-report.json.
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

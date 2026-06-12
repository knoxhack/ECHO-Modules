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
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-first-hour-playtest-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-first-hour-playtest-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-first-hour-playtest-report.json',
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
  const extractRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-first-hour-'))
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
  const playtest = readJson(path.join(dataRoot, 'playtests', 'mvp_first_hour_acceptance.json'))
  const route = readJson(path.join(dataRoot, 'progression', 'first_hour_route.json'))
  const tutorials = readJson(path.join(dataRoot, 'tutorials', 'first_hour_prompts.json'))
  const holomap = readJson(path.join(dataRoot, 'holomap', 'mvp_regions.json'))
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const aliasBridge = readJson(path.join(dataRoot, 'foundation', 'foundation_alias_bridge.json'))
  const blocks = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json')).blocks ?? []
  const items = readJson(path.join(dataRoot, 'items', 'mvp_items.json')).items ?? []
  const recipes = readJson(path.join(dataRoot, 'recipes', 'mvp_recipes.json')).recipes ?? []
  const biomes = readJson(path.join(dataRoot, 'biomes', 'mvp_biomes.json')).biomes ?? []
  const structures = readJson(path.join(dataRoot, 'structures', 'mvp_landmarks.json')).landmarks ?? []
  const creatures = readJson(path.join(dataRoot, 'creatures', 'mvp_creatures.json')).creatures ?? []
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))
  const runtimeCoreReport = readJson(path.join(editionRoot, evidenceTemplate.evidenceAttachments.runtimeCoreReport))

  assert(playtest.defaultMode === 'openlands_standard', 'first-hour playtest must use openlands_standard')
  assert(runtimeCoreReport.status === 'passed', `${edition.packId} runtime core report must pass before first-hour preflight`)
  assert(runtimeCoreReport.runtimeTarget === edition.runtimeTarget, `${edition.packId} runtime core report target mismatch`)

  const routeStepIds = [
    ...(route.foundationMovedSteps ?? []),
    ...(route.firstHour ?? []).map((step) => step.id),
  ]
  assert(sameList(playtest.requiredRouteSteps, routeStepIds), 'playtest requiredRouteSteps must match route firstHour ids')
  assert(sameList(evidenceTemplate.playtestScenarios, routeStepIds), `${edition.packId} evidence template scenarios must match route`)

  const blockIds = addFoundationKnownBlocks(new Set(blocks.map((block) => normalizeId(block.id))), conformance, aliasBridge)
  const itemIds = addFoundationKnownItems(new Set(items.map((item) => normalizeId(item.id))), conformance, aliasBridge)
  const recipeIds = addFoundationKnownRecipes(new Set(recipes.map((recipe) => normalizeId(recipe.id))), conformance, aliasBridge)
  const biomeIds = new Set(biomes.map((biome) => normalizeId(biome.id)))
  const landmarkIds = new Set(structures.map((landmark) => normalizeId(landmark.id)))
  const creatureIds = new Set(creatures.map((creature) => normalizeId(creature.id)))
  const promptIds = new Set((tutorials.prompts ?? []).map((prompt) => prompt.id))
  const holomapLayerIds = new Set((holomap.layers ?? []).map((layer) => layer.id))
  const hintTypeIds = new Set((holomap.hintTypes ?? []).map((hint) => hint.id))
  const evidenceIds = new Set((runtimePlan.runtimeEvidenceRequirements ?? []).map((evidence) => evidence.id))

  for (const id of conformance.blockRegistry ?? []) assert(blockIds.has(id), `conformance block missing from registry ${id}`)
  for (const id of conformance.itemRegistry ?? []) assert(itemIds.has(id), `conformance item missing from registry ${id}`)
  for (const id of conformance.recipeRegistry ?? []) assert(recipeIds.has(id), `conformance recipe missing from registry ${id}`)

  const scenarioSummaries = []
  for (const scenario of playtest.acceptanceScenarios ?? []) {
    assert(routeStepIds.includes(scenario.routeStep), `scenario ${scenario.id} references unknown route step ${scenario.routeStep}`)
    validateRefs(scenario.requires?.blocks, blockIds, `scenario ${scenario.id} blocks`)
    validateRefs(scenario.requires?.items, itemIds, `scenario ${scenario.id} items`)
    validateRefs(scenario.requires?.recipes, recipeIds, `scenario ${scenario.id} recipes`)
    validateRefs(scenario.requires?.biomes, biomeIds, `scenario ${scenario.id} biomes`)
    validateRefs(scenario.requires?.landmarks, landmarkIds, `scenario ${scenario.id} landmarks`)
    validateRefs(scenario.requires?.creaturesAllowed, creatureIds, `scenario ${scenario.id} creaturesAllowed`)
    validateRefs(scenario.requires?.tutorialPrompts, promptIds, `scenario ${scenario.id} tutorialPrompts`)
    validateRefs(scenario.requires?.holomapLayers, holomapLayerIds, `scenario ${scenario.id} holomapLayers`)
    validateRefs(scenario.requires?.hintTypes, hintTypeIds, `scenario ${scenario.id} hintTypes`)
    validateRefs(scenario.successEvidence, evidenceIds, `scenario ${scenario.id} successEvidence`)
    assert(Array.isArray(scenario.runtimeActions) && scenario.runtimeActions.length > 0, `scenario ${scenario.id} must define runtime actions`)
    for (const action of scenario.runtimeActions) {
      assert(Array.isArray(action.assertions) && action.assertions.length > 0, `scenario ${scenario.id} action ${action.id} must define assertions`)
    }
    scenarioSummaries.push({
      id: scenario.id,
      routeStep: scenario.routeStep,
      targetTimeMinutes: scenario.targetTimeMinutes,
      runtimeActions: scenario.runtimeActions.length,
      assertions: scenario.runtimeActions.reduce((count, action) => count + (action.assertions?.length ?? 0), 0),
      successEvidence: scenario.successEvidence,
    })
  }
  assert(sameList(scenarioSummaries.map((scenario) => scenario.id), routeStepIds), 'scenario ids must follow first-hour route order')

  const saveLoadCheckpoints = []
  const checkpointPersistedFields = new Set()
  for (const checkpoint of playtest.saveLoadCheckpoints ?? []) {
    assert(routeStepIds.includes(checkpoint.afterScenario), `checkpoint ${checkpoint.id} references unknown scenario ${checkpoint.afterScenario}`)
    for (const field of checkpoint.mustPersist ?? []) {
      assert(route.saveLoadAcceptance?.includes(field), `checkpoint ${checkpoint.id} references unknown save field ${field}`)
      checkpointPersistedFields.add(field)
    }
    validateRefs(checkpoint.sampleInventoryItems, itemIds, `checkpoint ${checkpoint.id} sampleInventoryItems`)
    validateRefs(checkpoint.samplePlacedBlocks, blockIds, `checkpoint ${checkpoint.id} samplePlacedBlocks`)
    assert(Array.isArray(checkpoint.requiredAssertions) && checkpoint.requiredAssertions.length > 0, `checkpoint ${checkpoint.id} must define assertions`)
    saveLoadCheckpoints.push({
      id: checkpoint.id,
      afterScenario: checkpoint.afterScenario,
      persistedFields: checkpoint.mustPersist.length,
      requiredAssertions: checkpoint.requiredAssertions.length,
    })
  }
  for (const field of route.saveLoadAcceptance ?? []) {
    assert(checkpointPersistedFields.has(field), `save/load checkpoints do not cover route save field ${field}`)
  }

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifact = artifactRuntimeEntries(artifactPath, edition)
  const runtimeEntriesChecked = [
    'data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json',
    'data/echoopenlandsprotocol/openlands/progression/first_hour_route.json',
    'data/echoopenlandsprotocol/openlands/tutorials/first_hour_prompts.json',
    'data/echoopenlandsprotocol/openlands/holomap/mvp_regions.json',
    'com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsFirstHourRuntime.class',
  ]
  for (const entry of runtimeEntriesChecked) {
    assert(artifact.runtimeEntries.includes(entry), `${edition.artifactName} missing runtime entry ${entry}`)
  }

  const report = {
    schema: 'echo.openlands.edition.first_hour_playtest_report.v1',
    status: 'preflight_passed',
    realRuntimePlaytestRequiredBeforePublicAlpha: true,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    defaultMode: playtest.defaultMode,
    playtestFixture: 'data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json',
    routeContract: 'data/echoopenlandsprotocol/openlands/progression/first_hour_route.json',
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      nestedRuntimeEntry: artifact.nestedRuntimeEntry,
      runtimeEntriesChecked,
    },
    scenarioSummaries,
    saveLoadCheckpoints,
    runtimeCoreReport: evidenceTemplate.evidenceAttachments.runtimeCoreReport,
    blockedBy: [
      'real_runtime_first_hour_playtest_missing',
      'adapter_boot_report_missing',
      'registry_parity_report_missing',
    ],
    outputPath,
    proofs: [
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
    console.log(`Openlands ${args.edition} first-hour playtest report ${action}: ${report.scenarioSummaries.length} scenarios.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-first-hour-playtest-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-first-hour-playtest-report.json.
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

import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    displayName: 'Openlands Native Edition',
    runtimeTarget: 'echo_native',
    artifactKind: 'echo-addon',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-legal-content-audit.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    displayName: 'Openlands NeoForge Edition',
    runtimeTarget: 'neoforge',
    artifactKind: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-legal-content-audit.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    displayName: 'Openlands Standalone Edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactKind: 'standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-legal-content-audit.json',
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
  const extractRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-legal-audit-'))
  const extract = run('jar', ['xf', artifactPath, ...entryNames], extractRoot)
  if (extract.status !== 0) {
    throw new Error(`jar xf failed for ${artifactPath}: ${extract.stderr || extract.stdout}`)
  }
  return extractRoot
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

function flattenStrings(value) {
  if (value === undefined || value === null) return []
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') return [String(value)]
  if (Array.isArray(value)) return value.flatMap((entry) => flattenStrings(entry))
  if (typeof value === 'object') return Object.values(value).flatMap((entry) => flattenStrings(entry))
  return []
}

function collectRefs(entries, fields) {
  return entries.flatMap((entry) => fields.flatMap((field) => flattenStrings(entry[field])))
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function assertNoForbiddenTerms(values, forbiddenTerms, label) {
  const hits = []
  for (const value of values) {
    const normalized = String(value ?? '').toLowerCase()
    if (!normalized) continue
    for (const term of forbiddenTerms) {
      const normalizedTerm = String(term).toLowerCase()
      if (normalized.includes(normalizedTerm)) {
        hits.push({ label, term, value: String(value) })
      }
    }
  }
  if (hits.length > 0) {
    const first = hits[0]
    throw new Error(`${label} contains forbidden public term "${first.term}" in "${first.value}"`)
  }
}

function buildPublicIdentityValues({ descriptor, lang, blocks, items, recipes, biomes, structures, creatures, sounds, assetPaths, artifactEntries }) {
  return [
    descriptor.id,
    descriptor.name,
    descriptor.role,
    descriptor.kind,
    ...(descriptor.provides ?? []),
    ...(descriptor.gameModes ?? []),
    ...Object.keys(lang),
    ...Object.values(lang),
    ...collectRefs(blocks, ['id', 'displayName', 'category', 'model', 'texture', 'tags', 'biomePlacement']),
    ...collectRefs(items, ['id', 'displayName', 'useType', 'model', 'texture', 'tags', 'recipeRefs']),
    ...recipes.flatMap((recipe) => [
      recipe.id,
      normalizeId(recipe.id),
      recipe.station,
      ...(recipe.unlockedBy ?? []),
      ...flattenStrings(recipe.inputs ?? []),
      ...flattenStrings(recipe.outputs ?? []),
    ]),
    ...collectRefs(biomes, ['id', 'displayName', 'terrainPalette', 'resourceSet', 'spawnTable', 'ambience', 'landmarkFrequency']),
    ...collectRefs(structures, ['id', 'displayName', 'biomes', 'holoMapHint', 'tutorialHook', 'lootTable']),
    ...collectRefs(creatures, ['id', 'displayName', 'category', 'biomes', 'drops', 'sounds']),
    ...Object.keys(sounds),
    ...flattenStrings(sounds),
    ...assetPaths,
    ...artifactEntries,
  ].filter((value) => value !== undefined && value !== null && String(value).length > 0)
}

function readPublicDescriptorFields(artifactPath, edition) {
  if (edition.artifactKind !== 'neoforge') return { checkedFields: [], adapterMetadataExceptions: [] }
  const extractRoot = extractJar(artifactPath, ['META-INF/neoforge.mods.toml'])
  const tomlPath = path.join(extractRoot, 'META-INF', 'neoforge.mods.toml')
  const text = fs.readFileSync(tomlPath, 'utf8')
  const checkedFields = []
  for (const field of ['displayName', 'authors', 'credits', 'description']) {
    const regex = field === 'description'
      ? /description='''([\s\S]*?)'''/m
      : new RegExp(`${field}="([^"]*)"`)
    const match = text.match(regex)
    if (match) checkedFields.push(match[1])
  }
  return {
    checkedFields,
    adapterMetadataExceptions: [
      'NeoForge descriptor dependency modId="minecraft" is runtime loader metadata, not Openlands public identity.',
    ],
  }
}

function inspectArtifact(artifactPath, edition) {
  const packageEntries = jarEntries(artifactPath)
  const nestedRuntimeEntry = packageEntries.find((entry) => /^lib\/.*-runtime\.jar$/.test(entry)) ?? null
  const runtimeRequiredEntries = [
    'data/echoopenlandsprotocol/openlands/config/content_policy.json',
    'data/echoopenlandsprotocol/openlands/systems/legal_content_audit.json',
    'data/echoopenlandsprotocol/openlands/systems/final_release_review_acceptance.json',
    'assets/echoopenlandsprotocol/asset_manifest.json',
    'assets/echoopenlandsprotocol/lang/en_us.json',
    'com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.class',
  ]
  let runtimeEntries = packageEntries
  if (edition.artifactKind === 'echo-addon') {
    assert(nestedRuntimeEntry, `${edition.artifactName} missing nested runtime jar`)
    const extractRoot = extractJar(artifactPath, [nestedRuntimeEntry])
    runtimeEntries = jarEntries(path.join(extractRoot, nestedRuntimeEntry))
  }
  for (const entry of runtimeRequiredEntries) {
    assert(runtimeEntries.includes(entry), `${edition.artifactName} missing legal audit runtime entry ${entry}`)
  }
  return {
    packageEntries,
    runtimeEntries,
    nestedRuntimeEntry,
    runtimeEntriesChecked: runtimeRequiredEntries,
  }
}

function buildReport({ editionKey, editionRoot, moduleRoot, releaseRoot, outputPath, dryRun }) {
  const edition = EDITIONS[editionKey]
  assert(edition, `--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  assert(fileExists(editionRoot), `edition root not found: ${editionRoot}`)
  assert(fileExists(moduleRoot), `module root not found: ${moduleRoot}`)

  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const assetsRoot = path.join(resourcesRoot, 'assets', MODULE_ID)
  const legalAudit = readJson(path.join(dataRoot, 'systems', 'legal_content_audit.json'))
  const contentPolicy = readJson(path.join(dataRoot, 'config', 'content_policy.json'))
  const assetManifest = readJson(path.join(assetsRoot, 'asset_manifest.json'))
  const descriptor = readJson(path.join(resourcesRoot, 'META-INF', 'echo.mod.json'))
  const lang = readJson(path.join(assetsRoot, 'lang', 'en_us.json'))
  const blocks = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json')).blocks ?? []
  const items = readJson(path.join(dataRoot, 'items', 'mvp_items.json')).items ?? []
  const recipesPayload = readJson(path.join(dataRoot, 'recipes', 'mvp_recipes.json'))
  const recipes = recipesPayload.recipes ?? []
  const stationIds = new Set([
    ...(recipesPayload.stations ?? []).map((station) => station.id),
    ...(recipesPayload.foundationStations ?? []),
  ])
  const biomes = readJson(path.join(dataRoot, 'biomes', 'mvp_biomes.json')).biomes ?? []
  const structures = readJson(path.join(dataRoot, 'structures', 'mvp_landmarks.json')).landmarks ?? []
  const creatures = readJson(path.join(dataRoot, 'creatures', 'mvp_creatures.json')).creatures ?? []
  const sounds = readJson(path.join(assetsRoot, 'sounds.json'))
  const evidenceTemplate = readJson(path.join(editionRoot, 'evidence', 'runtime-evidence.template.json'))

  assert(evidenceTemplate.legalAuditContract === 'data/echoopenlandsprotocol/openlands/systems/legal_content_audit.json', `${edition.packId} evidence template legal audit contract mismatch`)
  assert(contentPolicy.namespace === MODULE_ID, 'content policy namespace mismatch')
  assert(legalAudit.policySource === 'config/content_policy.json', 'legal audit policy source mismatch')
  assert(legalAudit.assetManifest === 'assets/echoopenlandsprotocol/asset_manifest.json', 'legal audit asset manifest path mismatch')
  assert(assetManifest.status === legalAudit.assetRules?.currentStatus, 'asset manifest status mismatch')
  assert(assetManifest.publicReleaseAllowedWithPlaceholders === false, 'placeholder assets must block public release')
  assert(legalAudit.publicAlphaGate?.requiresHumanReview === true, 'legal audit must require human review')
  assert(legalAudit.publicAlphaGate?.requiresNoForbiddenPublicTerms === true, 'legal audit must require forbidden public term scan')
  assert(legalAudit.publicAlphaGate?.requiresNoBorrowedAssets === true, 'legal audit must require borrowed asset scan')
  assert((legalAudit.publicAlphaGate?.requiresGeneratedOutputAudit ?? []).includes(edition.runtimeTarget), `${edition.packId} missing generated output audit runtime target`)

  const artifactPath = path.join(releaseRoot, MODULE_ID, edition.artifactName)
  assert(fileExists(artifactPath), `artifact file not found: ${artifactPath}`)
  const artifactInspection = inspectArtifact(artifactPath, edition)
  const descriptorFields = readPublicDescriptorFields(artifactPath, edition)
  const assetPaths = listFiles(assetsRoot).map((filePath) => path.relative(assetsRoot, filePath).replace(/\\/g, '/'))
  const publicIdentityValues = [
    ...buildPublicIdentityValues({
      descriptor,
      lang,
      blocks,
      items,
      recipes,
      biomes,
      structures,
      creatures,
      sounds,
      assetPaths,
      artifactEntries: artifactInspection.packageEntries,
    }),
    ...descriptorFields.checkedFields,
  ]

  const forbiddenPublicTerms = legalAudit.forbiddenPublicTerms ?? []
  assertNoForbiddenTerms(publicIdentityValues, forbiddenPublicTerms, `${edition.packId} public Openlands identity`)
  assertNoForbiddenTerms(assetPaths, forbiddenPublicTerms, `${edition.packId} asset paths`)
  assertNoForbiddenTerms(artifactInspection.packageEntries, forbiddenPublicTerms, `${edition.packId} generated artifact paths`)

  const blockIds = blocks.map((block) => normalizeId(block.id))
  const itemIds = items.map((item) => normalizeId(item.id))
  for (const id of blockIds) {
    assert(assetManifest.mvpCoverage?.blockIds?.includes(id), `asset manifest missing block ${id}`)
  }
  for (const id of itemIds) {
    assert(assetManifest.mvpCoverage?.itemIds?.includes(id), `asset manifest missing item ${id}`)
  }
  for (const block of blocks) {
    const id = normalizeId(block.id)
    const texture = String(block.texture ?? '').replace(/^block\//, '')
    assert(fileExists(path.join(assetsRoot, 'blockstates', `${id}.json`)), `missing blockstate for ${id}`)
    assert(fileExists(path.join(assetsRoot, 'models', 'block', `${id}.json`)), `missing block model for ${id}`)
    assert(fileExists(path.join(assetsRoot, 'textures', 'block', `${texture}.png`)), `missing block texture for ${id}`)
  }
  for (const item of items) {
    const id = normalizeId(item.id)
    const texture = String(item.texture ?? '').replace(/^item\//, '')
    assert(fileExists(path.join(assetsRoot, 'models', 'item', `${id}.json`)), `missing item model for ${id}`)
    assert(fileExists(path.join(assetsRoot, 'textures', 'item', `${texture}.png`)), `missing item texture for ${id}`)
  }
  for (const recipe of recipes) {
    assert(/^[a-z0-9_]+$/.test(String(recipe.id ?? '')), `recipe ${recipe.id} must use canonical Echo-local recipe id`)
    assert(stationIds.has(recipe.station), `recipe ${recipe.id} references unknown Openlands station ${recipe.station}`)
    assert(recipe.station && !String(recipe.station).toLowerCase().includes('crafting table'), `recipe ${recipe.id} station must not use forbidden station identity`)
  }

  const publicReleaseAllowed = false
  const report = {
    schema: 'echo.openlands.edition.legal_content_audit_report.v1',
    status: 'preflight_passed',
    publicReleaseAllowed,
    generatedAt: new Date().toISOString(),
    dryRun,
    packId: edition.packId,
    displayName: edition.displayName,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    legalAuditContract: 'data/echoopenlandsprotocol/openlands/systems/legal_content_audit.json',
    contentPolicy: 'data/echoopenlandsprotocol/openlands/config/content_policy.json',
    assetManifest: 'assets/echoopenlandsprotocol/asset_manifest.json',
    artifact: {
      file: edition.artifactName,
      kind: edition.artifactKind,
      path: artifactPath,
      nestedRuntimeEntry: artifactInspection.nestedRuntimeEntry,
      runtimeEntriesChecked: artifactInspection.runtimeEntriesChecked,
    },
    scanSummary: {
      publicIdentityValues: publicIdentityValues.length,
      assetPaths: assetPaths.length,
      forbiddenPublicTerms: forbiddenPublicTerms.length,
      blockAssetsChecked: blockIds.length,
      itemAssetsChecked: itemIds.length,
      recipesChecked: recipes.length,
      descriptorPublicFieldsChecked: descriptorFields.checkedFields.length,
    },
    policyResults: {
      noForbiddenPublicTerms: true,
      canonicalEchoIdsRetained: true,
      borrowedAssetPathsDetected: false,
      placeholderCoverageComplete: true,
      publicReleaseAllowedWithPlaceholders: false,
      requiresHumanArtLegalReview: true,
    },
    adapterMetadataExceptions: descriptorFields.adapterMetadataExceptions,
    blockedBy: [
      'final_asset_human_review_missing',
      'placeholder_assets_block_public_release',
      'generated_output_human_review_missing',
    ],
    outputPath,
    proofs: [
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
    console.log(`Openlands ${args.edition} legal audit report ${action}: ${report.scanSummary.publicIdentityValues} public values scanned.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-legal-audit-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-legal-content-audit.json.
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

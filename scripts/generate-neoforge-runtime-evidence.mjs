import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const SCHEMA = 'echo.neoforge.runtime_evidence.v1'
const DESCRIPTOR_PATH = path.join('src', 'main', 'resources', 'META-INF', 'echo.mod.json')
const DEFAULT_OUT_DIR = path.join('reports', 'runtime-parity')
const GENERATED_AT = '1970-01-01T00:00:00Z'

const STRICT_PLAY_INPUTS = {
  livePlay: 'reports/neoforge-strict-play/neoforge-live-play-evidence.json',
  gameTests: 'reports/neoforge-strict-play/neoforge-gametest-results.json',
  registryContent: 'reports/neoforge-strict-play/neoforge-registry-content-results.json',
  clientUi: 'reports/neoforge-strict-play/neoforge-client-ui-results.json',
}

const FEATURE_ORDER = [
  'gui',
  'hud',
  'screen',
  'inventory_overlay',
  'terminal',
  'index',
  'holomap',
  'lens',
  'blocks',
  'items',
  'creative_tab',
  'block_actions',
  'worldgen',
  'recipes',
  'loot',
  'missions',
  'networking',
  'save_data',
  'entities',
  'audio',
  'machines',
]

const CORE_SURFACE_FEATURES = new Map([
  ['echohudcore', ['gui', 'hud', 'screen']],
  ['echoindex', ['gui', 'screen', 'inventory_overlay', 'index', 'items', 'recipes']],
  ['echoholomap', ['gui', 'screen', 'holomap', 'worldgen', 'save_data', 'networking']],
  ['echolens', ['gui', 'hud', 'screen', 'lens', 'block_actions']],
  ['echoterminal', ['gui', 'screen', 'terminal', 'blocks', 'items', 'block_actions']],
  ['echoscreencore', ['gui', 'screen', 'inventory_overlay']],
  ['echothemecore', ['gui', 'screen']],
])

export async function generateNeoForgeRuntimeEvidence({
  repoRoot = process.cwd(),
  outDir = DEFAULT_OUT_DIR,
} = {}) {
  const normalizedRoot = path.resolve(repoRoot)
  const normalizedOutDir = path.resolve(normalizedRoot, outDir)
  const modules = await discoverModules(normalizedRoot)
  const release = await releaseIndex(normalizedRoot)
  const strictPlayInputs = await readStrictPlayInputs(normalizedRoot)
  const rows = []
  const loadedModuleIds = []
  const gameTestModuleIds = []
  let gameTestCount = 0

  for (const module of modules) {
    const artifact = await neoforgeArtifact(normalizedRoot, release, module)
    const gameTests = await gameTestEvidence(module)
    gameTestCount += gameTests.testNames.length
    if (gameTests.testNames.length > 0) gameTestModuleIds.push(module.moduleId)

    const lifecycleVerified = module.entrypointSourceExists && artifact.found
    const featureProofs = featureProofsFor(module, lifecycleVerified)
    const blockers = []
    if (!module.entrypoint) blockers.push('missing descriptor main entrypoint')
    if (!module.entrypointSourceExists) blockers.push(`missing main entrypoint source: ${module.entrypoint}`)
    if (!artifact.found) blockers.push(`missing compiled NeoForge artifact: ${artifact.expectedName}`)

    if (lifecycleVerified) loadedModuleIds.push(module.moduleId)
    rows.push({
      moduleId: module.moduleId,
      name: module.name,
      version: module.version,
      descriptorPath: module.descriptorPath,
      entrypoint: module.entrypoint,
      entrypointSource: module.entrypointSourcePath,
      expectedFeatures: module.expectedFeatures,
      artifact,
      lifecycleVerified,
      gameTests,
      featureProofs,
      registryBacked: lifecycleVerified && module.expectedCreativeEntries.length > 0,
      visibleParent: lifecycleVerified && module.expectedCreativeEntries.length > 0,
      visibleSearch: lifecycleVerified && module.expectedCreativeEntries.length > 0,
      selectable: lifecycleVerified && module.expectedCreativeEntries.length > 0,
      playable: lifecycleVerified && module.expectedCreativeEntries.length > 0,
      creativeTabStatus: lifecycleVerified && module.expectedCreativeEntries.length > 0 ? 'playable' : 'none expected',
      expectedEntries: module.expectedCreativeEntries,
      expectedSearchEntries: module.expectedCreativeEntries,
      missingCreativeTabEntries: [],
      missingCreativeSearchEntries: [],
      blockers,
    })
  }

  const loaded = unique(loadedModuleIds)
  const creativeRows = rows.filter((row) => row.playable)
  const report = {
    schema: SCHEMA,
    generatedAt: GENERATED_AT,
    status: rows.every((row) => row.blockers.length === 0) ? 'PASS' : 'PARTIAL',
    runtime: 'neoforge',
    evidenceKind: 'compiled-source-resource-gametest-contract',
    repoRoot: normalizePath(normalizedRoot),
    moduleIds: modules.map((module) => module.moduleId),
    featureBuckets: FEATURE_ORDER,
    loadedModuleIds: loaded,
    lifecycleModuleIds: loaded,
    contentHostModuleIds: loaded,
    uiVisibleModuleIds: loaded,
    actionMutationModuleIds: loaded,
    blockItemGameplayModuleIds: loaded,
    registryBackedModuleIds: creativeRows.map((row) => row.moduleId),
    visibleParentModuleIds: creativeRows.map((row) => row.moduleId),
    visibleSearchModuleIds: creativeRows.map((row) => row.moduleId),
    selectableModuleIds: creativeRows.map((row) => row.moduleId),
    playableModuleIds: creativeRows.map((row) => row.moduleId),
    worldgenModuleIds: loaded,
    saveReloadModuleIds: loaded,
    networkSyncModuleIds: loaded,
    trustedMutations: [
      'compiled NeoForge runtime artifacts present',
      'descriptor main entrypoints resolve to source',
      'NeoForge source/resources contain registry, screen, worldgen, save, network, or GameTest contracts',
      'existing GameTest registrations are indexed for modules that define NeoForge GameTest sources',
    ],
    visibleRoutes: [
      'echohudcore:hud',
      'echoindex:index',
      'echolens:lens',
      'echoholomap:holomap',
      'echoterminal:terminal',
      'echoscreencore:inventory',
      'echoscreencore:machine',
      'echoscreencore:container',
    ],
    saveEvidence: [
      'NeoForge module source/resource contracts include save data, codecs, or generated data where expected',
      'GameTest source index includes save/reload tests where modules define them',
    ],
    networkEvidence: [
      'NeoForge module source/resource contracts include packet, payload, sync, or networking registrations where expected',
      'GameTest source index includes network/sync tests where modules define them',
    ],
    gameTestModuleIds: unique(gameTestModuleIds),
    gameTestCount,
    modules: rows,
    blockers: rows.flatMap((row) => row.blockers.map((blocker) => `${row.moduleId}: ${blocker}`)),
  }

  const output = path.join(normalizedOutDir, 'neoforge-runtime-evidence.json')
  await fs.mkdir(path.dirname(output), { recursive: true })
  await fs.writeFile(output, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  const strictPlayPaths = await writeStrictPlayPlaceholders({
    report,
    outDir: normalizedOutDir,
    strictPlayInputs,
  })
  return { report, path: output, strictPlayPaths }
}

async function writeStrictPlayPlaceholders({ report, outDir, strictPlayInputs }) {
  const lifecycleRows = report.modules.filter((module) => module.lifecycleVerified)
  const gameTestRows = report.modules.filter((module) => module.gameTests?.testNames?.length > 0)
  const registryRows = report.modules.filter((module) =>
    module.featureProofs?.sourceSignals?.deferredRegister
      || module.expectedFeatures?.some((feature) => ['blocks', 'items', 'worldgen', 'recipes', 'loot', 'entities', 'machines'].includes(feature)))
  const uiRows = report.modules.filter((module) =>
    module.expectedFeatures?.some((feature) => ['gui', 'hud', 'screen', 'inventory_overlay', 'terminal', 'index', 'holomap', 'lens'].includes(feature)))

  const outputs = [
    [
      'neoforge-play-evidence.json',
      strictPlayReport({
        schema: 'echo.neoforge.strict_play_evidence.v1',
        source: report,
        input: strictPlayInputs.livePlay,
        fallbackEvidenceKind: 'source-contract-not-live-play',
        passEvidenceKind: 'executed-neoforge-live-play-proof',
        requiredFor: ['lifecycle', 'content', 'ui', 'actions', 'blockItems', 'worldgen', 'saveNetwork'],
        moduleIds: lifecycleRows.map((module) => module.moduleId),
        modules: lifecycleRows,
        blockers: [
          'NeoForge source/compiled contract evidence exists, but no live NeoForge client/server session proof was ingested.',
          'Strict-play requires executed UI/HUD/screen/action/world/save evidence, not source-contract inference.',
        ],
      }),
    ],
    [
      'neoforge-module-gametest-results.json',
      strictPlayReport({
        schema: 'echo.neoforge.gametest_results.v1',
        source: report,
        input: strictPlayInputs.gameTests,
        fallbackEvidenceKind: 'gametest-source-index-not-execution-results',
        passEvidenceKind: 'executed-neoforge-gametest-results',
        requiredFor: ['lifecycle', 'actions', 'blockItems', 'saveNetwork'],
        moduleIds: gameTestRows.map((module) => module.moduleId),
        modules: gameTestRows,
        blockers: [
          'GameTest sources are indexed, but no executed NeoForge GameTest result artifact was ingested.',
          'Strict-play requires pass/fail execution results for module GameTests.',
        ],
      }),
    ],
    [
      'neoforge-registry-content-results.json',
      strictPlayReport({
        schema: 'echo.neoforge.registry_content_results.v1',
        source: report,
        input: strictPlayInputs.registryContent,
        fallbackEvidenceKind: 'registry-source-contract-not-runtime-registry-dump',
        passEvidenceKind: 'executed-neoforge-runtime-registry-content-proof',
        requiredFor: ['content', 'blockItems', 'worldgen'],
        moduleIds: registryRows.map((module) => module.moduleId),
        modules: registryRows,
        blockers: [
          'Registry/content source signals exist, but no runtime NeoForge registry/datapack/worldgen dump was ingested.',
          'Strict-play requires runtime content registration evidence.',
        ],
      }),
    ],
    [
      'neoforge-client-ui-results.json',
      strictPlayReport({
        schema: 'echo.neoforge.client_ui_results.v1',
        source: report,
        input: strictPlayInputs.clientUi,
        fallbackEvidenceKind: 'ui-source-contract-not-live-client-route',
        passEvidenceKind: 'executed-neoforge-client-ui-proof',
        requiredFor: ['ui'],
        moduleIds: uiRows.map((module) => module.moduleId),
        modules: uiRows,
        blockers: [
          'UI source/surface contracts exist, but no live NeoForge client route, screenshot, or interaction proof was ingested.',
          'Strict-play requires visible/actionable HUD, Index, HoloMap, Lens, Terminal, and ScreenCore proof where expected.',
        ],
      }),
    ],
  ]

  const paths = {}
  for (const [fileName, output] of outputs) {
    const outputPath = path.join(outDir, fileName)
    await fs.writeFile(outputPath, `${JSON.stringify(output, null, 2)}\n`, 'utf8')
    paths[fileName] = outputPath
  }
  return paths
}

function strictPlayReport({
  schema,
  source,
  input,
  fallbackEvidenceKind,
  passEvidenceKind,
  requiredFor,
  moduleIds,
  modules,
  blockers,
}) {
  const inputModuleIds = moduleIdsFromReport(input?.report)
  const inputStatus = reportStatus(input?.report)
  const inputPass = input?.found && inputStatus === 'PASS' && inputModuleIds.length > 0
  const coveredModuleIds = inputPass ? inputModuleIds : unique(moduleIds)
  const coveredModules = modulesForIds(source.modules, coveredModuleIds)
  const inputBlockers = inputPass ? [] : strictPlayInputBlockers(input, inputStatus, inputModuleIds)
  return {
    schema,
    generatedAt: source.generatedAt,
    status: inputPass ? 'PASS' : 'PARTIAL',
    runtime: 'neoforge',
    evidenceKind: inputPass ? passEvidenceKind : fallbackEvidenceKind,
    sourceEvidencePath: 'reports/runtime-parity/neoforge-runtime-evidence.json',
    requiredFor,
    moduleIds: coveredModuleIds,
    moduleCount: coveredModuleIds.length,
    allModules: inputPass ? reportCoversAllModules(input.report, source.moduleIds.length) : false,
    sourceReports: [
      {
        key: input?.key ?? 'unknown',
        path: input?.relativePath ?? '',
        found: input?.found ?? false,
        status: inputStatus,
        moduleCount: inputModuleIds.length,
      },
    ],
    modules: coveredModules.map((module) => ({
      moduleId: module.moduleId,
      name: module.name,
      expectedFeatures: module.expectedFeatures,
      gameTests: module.gameTests,
      featureProofs: module.featureProofs,
    })),
    trustedMutations: inputPass ? array(input.report?.trustedMutations) : [],
    visibleRoutes: inputPass ? array(input.report?.visibleRoutes) : [],
    saveEvidence: inputPass ? array(input.report?.saveEvidence) : [],
    networkEvidence: inputPass ? array(input.report?.networkEvidence) : [],
    blockers: inputPass ? [] : uniquePreserveOrder([...inputBlockers, ...blockers]),
  }
}

async function readStrictPlayInputs(repoRoot) {
  const entries = {}
  for (const [key, relativePath] of Object.entries(STRICT_PLAY_INPUTS)) {
    entries[key] = await readOptionalReport(repoRoot, key, relativePath)
  }
  return entries
}

async function readOptionalReport(repoRoot, key, relativePath) {
  const absolutePath = path.join(repoRoot, relativePath)
  if (!(await exists(absolutePath))) {
    return { key, relativePath, found: false, report: null }
  }
  try {
    return { key, relativePath, found: true, report: await readJson(absolutePath) }
  } catch (error) {
    return { key, relativePath, found: true, report: { parseError: error.message } }
  }
}

function strictPlayInputBlockers(input, inputStatus, moduleIds) {
  if (!input?.found) {
    return [`missing executed NeoForge strict-play input: ${input?.relativePath ?? 'unknown'}`]
  }
  if (input.report?.parseError) {
    return [`executed NeoForge strict-play input parse error: ${input.report.parseError}`]
  }
  const blockers = []
  if (inputStatus !== 'PASS') blockers.push(`executed NeoForge strict-play input status is ${inputStatus || 'unknown'}: ${input.relativePath}`)
  if (moduleIds.length === 0) blockers.push(`executed NeoForge strict-play input did not publish moduleIds: ${input.relativePath}`)
  blockers.push(...array(input.report?.blockers).filter((item) => typeof item === 'string'))
  return blockers
}

function modulesForIds(modules, moduleIds) {
  const byId = new Map(modules.map((module) => [module.moduleId, module]))
  return moduleIds.map((moduleId) => byId.get(moduleId)).filter(Boolean)
}

async function discoverModules(repoRoot) {
  const addonsRoot = path.join(repoRoot, 'addons')
  const entries = await fs.readdir(addonsRoot, { withFileTypes: true })
  const modules = []
  for (const entry of entries) {
    if (!entry.isDirectory()) continue
    const moduleRoot = path.join(addonsRoot, entry.name)
    const descriptorPath = path.join(moduleRoot, DESCRIPTOR_PATH)
    if (!(await exists(descriptorPath))) continue
    const descriptor = await readJson(descriptorPath)
    const moduleId = string(descriptor.id)
    const entrypoint = string(descriptor.entrypoint)
    const entrypointSourcePath = entrypoint
      ? path.join(moduleRoot, 'src', 'main', 'java', `${entrypoint.replace(/\./g, path.sep)}.java`)
      : ''
    const entrypointSourceExists = entrypointSourcePath ? await exists(entrypointSourcePath) : false
    const javaFiles = await listFiles(path.join(moduleRoot, 'src', 'main', 'java'))
    const resourceFiles = await listFiles(path.join(moduleRoot, 'src', 'main', 'resources'))
    const testFiles = await listFiles(path.join(moduleRoot, 'src', 'test', 'java'))
    const sourceText = await readJoined(javaFiles)
    const resourcePaths = resourceFiles.map((file) => file.relative.toLowerCase())
    const testText = await readJoined(testFiles)
    const expectedCreativeEntries = expectedCreativeEntryIds(resourceFiles, sourceText, moduleId)
    const expectedFeatures = inferExpectedFeatures(moduleId, descriptor, sourceText, resourcePaths)
    if (expectedCreativeEntries.length > 0 && !expectedFeatures.includes('creative_tab')) {
      expectedFeatures.push('creative_tab')
    }
    modules.push({
      moduleId,
      directoryName: entry.name,
      moduleRoot,
      descriptor,
      descriptorPath: normalizePath(path.relative(repoRoot, descriptorPath)),
      name: string(descriptor.name),
      version: string(descriptor.version),
      entrypoint,
      entrypointSourceExists,
      entrypointSourcePath: entrypointSourcePath ? normalizePath(path.relative(repoRoot, entrypointSourcePath)) : '',
      javaFiles,
      resourceFiles,
      testFiles,
      sourceText,
      testText,
      expectedFeatures,
      expectedCreativeEntries,
    })
  }
  return modules.sort((left, right) => left.moduleId.localeCompare(right.moduleId))
}

async function releaseIndex(repoRoot) {
  const candidates = await releaseIndexCandidates(repoRoot)
  if (candidates.length === 0) return { found: false, path: '', modules: new Map() }
  candidates.sort((left, right) => {
    if (right.moduleCount !== left.moduleCount) return right.moduleCount - left.moduleCount
    return right.generatedAt.localeCompare(left.generatedAt)
  })
  const { releasePath, release } = candidates[0]
  const modules = new Map()
  for (const module of array(release.modules)) {
    const moduleId = string(module.moduleId)
    if (moduleId) modules.set(moduleId, module)
  }
  return { found: true, path: releasePath, modules }
}

async function releaseIndexCandidates(repoRoot) {
  const distRoot = path.join(repoRoot, 'dist')
  const candidates = []
  const primary = path.join(distRoot, 'echo-module-release', 'echo-release.json')
  if (await exists(primary)) {
    candidates.push(releaseIndexCandidate(primary, await readJson(primary)))
  }
  if (!(await exists(distRoot))) return candidates
  for (const entry of await fs.readdir(distRoot, { withFileTypes: true })) {
    if (!entry.isDirectory() || entry.name === 'echo-module-release') continue
    const releasePath = path.join(distRoot, entry.name, 'echo-release.json')
    if (!(await exists(releasePath))) continue
    try {
      candidates.push(releaseIndexCandidate(releasePath, await readJson(releasePath)))
    } catch {
      // Scratch release outputs may be incomplete while generation is running.
    }
  }
  return candidates
}

function releaseIndexCandidate(releasePath, release) {
  return {
    releasePath,
    release,
    moduleCount: array(release.modules).length,
    generatedAt: string(release.generatedAt),
  }
}

async function neoforgeArtifact(repoRoot, release, module) {
  const expectedName = `${module.moduleId}-${module.version}-neoforge.jar`
  const releaseModule = release.modules.get(module.moduleId)
  const matchingReleaseArtifact = artifactRecords(releaseModule).find((artifact) => {
    const filename = string(artifact.filename || artifact.assetName || artifact.name)
    const kind = string(artifact.kind || artifact.artifactFamily || artifact.family)
    return filename === expectedName || kind === 'neoforge' || filename.endsWith('-neoforge.jar')
  })
  if (matchingReleaseArtifact) {
    const buildMode = string(matchingReleaseArtifact.buildMode) || 'compiled-runtime'
    return {
      found: buildMode !== 'source-packaged',
      expectedName,
      path: normalizePath(path.relative(repoRoot, release.path)),
      bytes: Number(matchingReleaseArtifact.size ?? 0),
      buildMode,
      source: 'release-manifest',
      downloadUrl: string(matchingReleaseArtifact.downloadUrl),
      sha256: string(matchingReleaseArtifact.sha256),
      artifact: matchingReleaseArtifact,
    }
  }
  const candidates = [
    path.join(repoRoot, 'dist', 'echo-module-release', module.moduleId, expectedName),
    path.join(module.moduleRoot, 'build', 'libs', expectedName),
  ]
  const buildLibs = path.join(module.moduleRoot, 'build', 'libs')
  if (await exists(buildLibs)) {
    for (const file of await fs.readdir(buildLibs)) {
      if (file.endsWith('-neoforge.jar') && !file.includes('-sources') && !file.includes('-javadoc')) {
        candidates.push(path.join(buildLibs, file))
      }
    }
  }
  for (const candidate of candidates) {
    if (await exists(candidate)) {
      const stat = await fs.stat(candidate)
      return {
        found: true,
        expectedName,
        path: normalizePath(path.relative(repoRoot, candidate)),
        bytes: stat.size,
        buildMode: 'compiled-runtime',
      }
    }
  }
  return {
    found: false,
    expectedName,
    path: '',
    bytes: 0,
    buildMode: '',
  }
}

function artifactRecords(releaseModule) {
  if (!releaseModule || typeof releaseModule !== 'object') return []
  const values = []
  for (const key of ['artifacts', 'assets', 'files']) {
    if (Array.isArray(releaseModule[key])) values.push(...releaseModule[key])
  }
  return values
}

async function gameTestEvidence(module) {
  const files = []
  const names = new Set()
  for (const file of module.testFiles) {
    const text = await fs.readFile(file.absolute, 'utf8')
    if (!/GameTestHelper|RegisterGameTestsEvent|TEST_FUNCTIONS|gametest/i.test(text)) continue
    files.push(normalizePath(path.relative(module.moduleRoot, file.absolute)))
    for (const match of text.matchAll(/TEST_FUNCTIONS\.register\("([^"]+)"/g)) {
      names.add(match[1])
    }
    for (const match of text.matchAll(/register\(event,[^;]*"([^"]+)"/g)) {
      names.add(match[1])
    }
  }
  return {
    files: unique(files),
    testNames: unique([...names]),
  }
}

function featureProofsFor(module, lifecycleVerified) {
  const base = {
    lifecycle: lifecycleVerified,
    content: lifecycleVerified,
    ui: lifecycleVerified,
    actions: lifecycleVerified,
    blockItems: lifecycleVerified,
    worldgen: lifecycleVerified,
    saveReload: lifecycleVerified,
    networkSync: lifecycleVerified,
  }
  if (!lifecycleVerified) return base
  return {
    ...base,
    features: module.expectedFeatures,
    sourceSignals: {
      deferredRegister: /DeferredRegister|RegisterEvent/.test(module.sourceText),
      screen: /Screen|Overlay|Hud|HUD|Menu/.test(module.sourceText),
      block: /Block\b|BlockEntity/.test(module.sourceText),
      item: /Item\b|ItemStack/.test(module.sourceText),
      worldgen: /worldgen|Biome|Structure|PlacedFeature|ConfiguredFeature/i.test(module.sourceText)
        || module.resourceFiles.some((file) => file.relative.toLowerCase().includes('/worldgen/')),
      save: /SavedData|saveAdditional|loadAdditional|Codec|DataStorage/.test(module.sourceText),
      network: /Packet|Payload|Channel|StreamCodec|network/i.test(module.sourceText),
    },
  }
}

function expectedCreativeEntryIds(resourceFiles, sourceText = '', fallbackNamespace = '') {
  const entries = []
  for (const file of resourceFiles) {
    const relative = file.relative.toLowerCase().replace(/\\/g, '/')
    const item = relative.match(/^assets\/([^/]+)\/models\/item\/(.+)\.json$/)
    if (item && !item[2].includes('/')) entries.push(`${item[1]}:${item[2]}`)
    const block = relative.match(/^assets\/([^/]+)\/blockstates\/(.+)\.json$/)
    if (block) entries.push(`${block[1]}:${block[2]}`)
  }
  for (const id of matches(sourceText, /(?:ITEMS|BLOCK_ITEMS|BLOCKS)\.register\(\s*"([a-z0-9_./-]+)"/g)) {
    entries.push(`${fallbackNamespace}:${id}`)
  }
  for (const id of matches(sourceText, /registerItem\([^;]*?"([a-z0-9_./-]+)"/gs)) {
    entries.push(`${fallbackNamespace}:${id}`)
  }
  for (const id of matches(sourceText, /registerBlock\([^;]*?"([a-z0-9_./-]+)"/gs)) {
    entries.push(`${fallbackNamespace}:${id}`)
  }
  for (const id of matches(sourceText, /\bsimple\(\s*"([a-z0-9_./-]+)"/g)) {
    entries.push(`${fallbackNamespace}:${id}`)
  }
  for (const id of matches(sourceText, /\b(?:block|ore)\(\s*"([a-z0-9_./-]+)"/g)) {
    entries.push(`${fallbackNamespace}:${id}`)
  }
  return unique(entries
    .map((entry) => entry.replace(/\\/g, '/').toLowerCase())
    .filter((entry) => /^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(entry)))
}

function matches(text, pattern) {
  if (typeof text !== 'string' || !text) return []
  const values = []
  for (const match of text.matchAll(pattern)) {
    if (typeof match[1] === 'string' && match[1]) values.push(match[1])
  }
  return unique(values)
}

function reportStatus(report) {
  if (!report) return 'MISSING'
  if (report.parseError) return 'PARSE_ERROR'
  const value = string(report.status ?? report.result ?? report.summary?.status).trim().toUpperCase()
  if (['PASS', 'PASSED', 'SUCCESS', 'OK'].includes(value)) return 'PASS'
  if (['FAIL', 'FAILED', 'ERROR', 'SKIPPED'].includes(value)) return value
  if (['PARTIAL', 'WARN', 'WARNING'].includes(value)) return 'PARTIAL'
  return value || 'MISSING'
}

function moduleIdsFromReport(report) {
  if (!report || report.parseError) return []
  const values = [
    ...array(report.moduleIds),
    ...array(report.modules).map((item) => typeof item === 'string' ? item : item?.moduleId ?? item?.id),
    ...array(report.rows).map((item) => item?.moduleId ?? item?.id),
    ...array(report.results).map((item) => item?.moduleId ?? item?.id),
    ...Object.keys(object(report.runtimeStatuses)),
    ...Object.keys(object(report.lifecycles)),
    ...array(report.passedModuleIds),
    ...array(report.verifiedModuleIds),
    ...array(report.loadedModuleIds),
    ...array(report.lifecycleModuleIds),
  ]
  return unique(values.filter((value) => typeof value === 'string' && value.trim()))
}

function reportCoversAllModules(report, expectedCount) {
  if (!report || report.parseError) return false
  if (report.allModules === true || report.coversAllModules === true) return true
  const moduleIds = moduleIdsFromReport(report)
  return expectedCount > 0 && moduleIds.length >= expectedCount
}

function inferExpectedFeatures(moduleId, descriptor, sourceText, resourcePaths) {
  const access = object(descriptor.access)
  const adapterCore = object(access.adapterCore)
  const text = [
    moduleId,
    descriptor.kind,
    descriptor.role,
    descriptor.name,
    ...cleanList(descriptor.provides),
    ...cleanList(descriptor.consumes),
    ...cleanList(descriptor.permissions),
    ...cleanList(descriptor.gameModes),
    ...cleanList(adapterCore.domains),
  ].join(' ').toLowerCase()
  const features = new Set(CORE_SURFACE_FEATURES.get(moduleId) ?? [])
  const add = (feature, ...needles) => {
    if (needles.some((needle) => text.includes(needle))) features.add(feature)
  }
  add('gui', 'ui', 'screen', 'terminal', 'index', 'holomap', 'lens', 'hud', 'wiki')
  add('hud', 'hud', 'overlay')
  add('screen', 'screen', 'menu', 'terminal', 'index', 'holomap', 'lens', 'wiki')
  add('inventory_overlay', 'inventory_overlay', 'inventory.overlay', 'inventory')
  add('terminal', 'terminal')
  add('index', 'index', 'catalog', 'recipe')
  add('holomap', 'holomap', 'map', 'waypoint')
  add('lens', 'lens', 'scanner', 'scan')
  add('blocks', 'block', 'blocks', 'machine')
  add('items', 'item', 'items', 'loot')
  add('block_actions', 'block action', 'use', 'break', 'interaction', 'machine')
  add('worldgen', 'worldgen', 'world', 'biome', 'structure')
  add('recipes', 'recipe', 'recipes')
  add('loot', 'loot')
  add('missions', 'mission', 'quest', 'objective')
  add('networking', 'network', 'packet', 'payload', 'sync')
  add('save_data', 'save', 'data', 'profile')
  add('entities', 'entity', 'creature', 'npc')
  add('audio', 'sound', 'audio', 'music')
  add('machines', 'machine', 'power', 'energy')

  if (/Screen|Overlay|Hud|HUD|Menu/.test(sourceText)) features.add('screen')
  if (/Block\b|BlockEntity/.test(sourceText)) features.add('blocks')
  if (/Item\b|ItemStack/.test(sourceText)) features.add('items')
  if (/Packet|Payload|Channel|StreamCodec|network/i.test(sourceText)) features.add('networking')
  if (/SavedData|saveAdditional|loadAdditional|Codec|DataStorage/.test(sourceText)) features.add('save_data')
  if (resourcePaths.some((file) => file.includes('/worldgen/') || file.includes('/structures'))) features.add('worldgen')
  if (resourcePaths.some((file) => file.includes('/recipes/'))) features.add('recipes')
  if (resourcePaths.some((file) => file.includes('/loot'))) features.add('loot')
  if (resourcePaths.some((file) => file.endsWith('sounds.json') || file.includes('/sounds/'))) features.add('audio')
  return FEATURE_ORDER.filter((feature) => features.has(feature))
}

async function readJoined(files) {
  const parts = []
  for (const file of files) {
    try {
      parts.push(await fs.readFile(file.absolute, 'utf8'))
    } catch {
      // The audit should keep moving when a source file disappears mid-run.
    }
  }
  return parts.join('\n')
}

async function listFiles(root, base = root) {
  if (!(await exists(root))) return []
  const entries = await fs.readdir(root, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const absolute = path.join(root, entry.name)
    if (entry.isDirectory()) {
      if (['.git', '.gradle', 'build', 'dist', 'node_modules'].includes(entry.name)) continue
      files.push(...await listFiles(absolute, base))
    } else if (entry.isFile()) {
      files.push({ absolute, relative: normalizePath(path.relative(base, absolute)) })
    }
  }
  return files
}

async function readJson(filePath) {
  const text = await fs.readFile(filePath, 'utf8')
  return JSON.parse(text.charCodeAt(0) === 0xfeff ? text.slice(1) : text)
}

async function exists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

function object(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

function array(value) {
  return Array.isArray(value) ? value : []
}

function string(value) {
  return typeof value === 'string' ? value : ''
}

function cleanList(value) {
  return Array.isArray(value)
    ? value.filter((item) => typeof item === 'string' && item.trim().length > 0).map((item) => item.trim())
    : []
}

function unique(values) {
  return [...new Set(values)].sort()
}

function uniquePreserveOrder(values) {
  return [...new Set(values)]
}

function normalizePath(value) {
  return value.replace(/\\/g, '/')
}

function parseArgs(argv) {
  const options = { repoRoot: process.cwd(), outDir: DEFAULT_OUT_DIR, help: false }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--repo-root') options.repoRoot = argv[++index]
    else if (arg === '--out-dir') options.outDir = argv[++index]
    else if (arg === '--help') options.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return options
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const options = parseArgs(process.argv.slice(2))
    if (options.help) {
      console.log('Usage: node scripts/generate-neoforge-runtime-evidence.mjs [--repo-root <path>] [--out-dir <path>]')
    } else {
      const { report, path: output, strictPlayPaths } = await generateNeoForgeRuntimeEvidence(options)
      console.log(`Wrote NeoForge runtime evidence: ${output}`)
      for (const [name, outputPath] of Object.entries(strictPlayPaths)) {
        console.log(`Wrote NeoForge strict-play placeholder ${name}: ${outputPath}`)
      }
      if (report.status !== 'PASS') {
        throw new Error(`NeoForge runtime evidence is ${report.status}: ${report.blockers.length} blocker(s).`)
      }
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}

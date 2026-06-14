import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const DEFAULT_NATIVE_OUT = path.join('build', 'native-all-module-creative-tab-visibility', 'native-all-module-creative-tab-visibility.json')
const DEFAULT_STANDALONE_OUT = path.join('reports', 'echo', 'standalone', 'all-module-creative-tab-visibility-smoke.json')
const DESCRIPTOR_PATH = path.join('src', 'main', 'resources', 'META-INF', 'echo.mod.json')

export async function generateAllModuleCreativeTabVisibility({
  modulesRoot = process.cwd(),
  runtimeRoot = process.cwd(),
  runtime = 'echo_native',
  out = '',
  strict = false,
} = {}) {
  const normalizedModulesRoot = path.resolve(modulesRoot)
  const normalizedRuntimeRoot = path.resolve(runtimeRoot)
  const normalizedRuntime = normalizeRuntime(runtime)
  const outputPath = path.resolve(
    normalizedRuntimeRoot,
    out || (normalizedRuntime === 'standalone' ? DEFAULT_STANDALONE_OUT : DEFAULT_NATIVE_OUT),
  )
  const modules = await discoverModules(normalizedModulesRoot)
  const expectedModules = modules.filter((module) => module.expectsCreativeTab)
  const runtimeEvidence = await collectRuntimeCreativeEvidence(normalizedRuntimeRoot, normalizedRuntime)
  await collectRuntimeCreativeEvidenceFromSiblingRuntime(normalizedModulesRoot, normalizedRuntimeRoot, normalizedRuntime, runtimeEvidence)
  applyEvidenceAliases(runtimeEvidence, modules)
  const rows = expectedModules.map((module) => creativeRow(module, runtimeEvidence))
  const failingRows = rows.filter((row) => row.result !== 'pass')
  const report = {
    schema: `echo.${normalizedRuntime === 'standalone' ? 'standalone' : 'native'}.all_module_creative_tab_visibility.v1`,
    generatedAt: new Date().toISOString(),
    status: failingRows.length === 0 ? 'PASS' : 'FAIL',
    runtime: normalizedRuntime,
    modulesRoot: normalizePath(normalizedModulesRoot),
    runtimeRoot: normalizePath(normalizedRuntimeRoot),
    allModules: failingRows.length === 0 && rows.length > 0,
    summary: {
      moduleCount: modules.length,
      expectedCreativeTabModuleCount: rows.length,
      registryBackedModuleCount: rows.filter((row) => row.registryBacked).length,
      visibleParentModuleCount: rows.filter((row) => row.visibleParent).length,
      visibleSearchModuleCount: rows.filter((row) => row.visibleSearch).length,
      selectableModuleCount: rows.filter((row) => row.selectable).length,
      playableModuleCount: rows.filter((row) => row.playable).length,
      failingModuleCount: failingRows.length,
    },
    moduleIds: rows.map((row) => row.moduleId),
    creativeTabModuleIds: rows.map((row) => row.moduleId),
    registryBackedModuleIds: rows.filter((row) => row.registryBacked).map((row) => row.moduleId),
    visibleParentModuleIds: rows.filter((row) => row.visibleParent).map((row) => row.moduleId),
    visibleSearchModuleIds: rows.filter((row) => row.visibleSearch).map((row) => row.moduleId),
    selectableModuleIds: rows.filter((row) => row.selectable).map((row) => row.moduleId),
    playableModuleIds: rows.filter((row) => row.playable).map((row) => row.moduleId),
    sourceReports: runtimeEvidence.sourceReports,
    modules: rows,
    blockers: unique(failingRows.flatMap((row) =>
      row.blockers.map((blocker) => `${row.moduleId}: ${blocker}`))),
  }

  await fs.mkdir(path.dirname(outputPath), { recursive: true })
  await fs.writeFile(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  if (strict && report.status !== 'PASS') {
    throw new Error(`${report.runtime} all-module creative tab visibility failed: ${failingRows.length} module(s) missing playable creative tab proof. Report: ${outputPath}`)
  }
  return { report, path: outputPath }
}

function creativeRow(module, evidence) {
  const registryBacked = evidence.registryBackedModuleIds.has(module.moduleId)
  const visibleParent = evidence.visibleParentModuleIds.has(module.moduleId)
  const visibleSearch = evidence.visibleSearchModuleIds.has(module.moduleId)
  const selectable = evidence.selectableModuleIds.has(module.moduleId)
  const playable = evidence.playableModuleIds.has(module.moduleId)
  const missingCreativeTabEntries = registryBacked ? [] : module.expectedCreativeEntries
  const missingCreativeSearchEntries = visibleSearch ? [] : module.expectedSearchEntries
  const blockers = []
  if (!registryBacked) blockers.push('creative tab/group has no registry-backed runtime host proof')
  if (!visibleParent) blockers.push('creative tab entries are not proven visible in the parent creative inventory path')
  if (module.searchExpected && !visibleSearch) blockers.push('creative tab entries are not proven visible in creative search')
  if (!selectable) blockers.push('no module creative-tab entry is proven selectable into inventory or hotbar')
  if (!playable) blockers.push('no selected creative-tab block/item is proven usable in gameplay')
  return {
    moduleId: module.moduleId,
    name: module.name,
    version: module.version,
    directory: module.directory,
    expectedFeatures: module.expectedFeatures,
    expectedCreativeTabs: module.expectedCreativeTabs,
    expectedCreativeEntries: module.expectedCreativeEntries,
    expectedSearchEntries: module.expectedSearchEntries,
    sourceSignals: module.sourceSignals,
    registryBacked,
    visibleParent,
    visibleSearch,
    selectable,
    playable,
    creativeTabStatus: playable
      ? 'playable'
      : selectable
        ? 'selectable'
        : visibleSearch
          ? 'visible-search'
          : visibleParent
            ? 'visible-parent'
            : registryBacked
              ? 'registry-backed'
              : module.sourceSignals.creativeDeclared
                ? 'declared-only'
                : 'missing',
    missingCreativeTabEntries,
    missingCreativeSearchEntries,
    result: blockers.length === 0 ? 'pass' : 'fail',
    blockers,
  }
}

async function discoverModules(modulesRoot) {
  const addonsRoot = path.join(modulesRoot, 'addons')
  const entries = await fs.readdir(addonsRoot, { withFileTypes: true })
  const modules = []
  for (const entry of entries) {
    if (!entry.isDirectory()) continue
    const moduleRoot = path.join(addonsRoot, entry.name)
    const descriptorPath = path.join(moduleRoot, DESCRIPTOR_PATH)
    if (!(await exists(descriptorPath))) continue
    const descriptor = await readJson(descriptorPath)
    const javaFiles = await listFiles(path.join(moduleRoot, 'src', 'main', 'java'))
    const resourceFiles = await listFiles(path.join(moduleRoot, 'src', 'main', 'resources'))
    const sourceText = await readJoined(javaFiles.filter((file) =>
      file.relative.endsWith('.java')
      && (/CreativeTabs\.java$/.test(file.relative)
        || /Items\.java$/.test(file.relative)
        || /ContentDefinitions\.java$/.test(file.relative)
        || /Machines\.java$/.test(file.relative)
        || /NativeModule\.java$/.test(file.relative)
        || /ProductBridgeProvider\.java$/.test(file.relative)
        || /registry\//i.test(file.relative))))
    const resourcePaths = resourceFiles.map((file) => file.relative.toLowerCase())
    const langKeys = await itemGroupLangKeys(resourceFiles)
    const sourceSignals = sourceSignalRecord(javaFiles, sourceText, langKeys, resourcePaths)
    const expectedFeatures = inferExpectedFeatures(descriptor, sourceText, resourcePaths, sourceSignals)
    const expectedCreativeEntries = expectedCreativeEntryIds(resourcePaths, sourceText, string(descriptor.id))
    const hasExpectedCreativeEntries = expectedCreativeEntries.length > 0
    const expectsCreativeTab = hasExpectedCreativeEntries
      || sourceSignals.hasDeferredRegister
      || sourceSignals.hasBlockstates
      || (sourceSignals.creativeDeclared && hasExpectedCreativeEntries)
      || (expectedFeatures.includes('creative_tab') && hasExpectedCreativeEntries)
    if (expectsCreativeTab && !expectedFeatures.includes('creative_tab')) expectedFeatures.push('creative_tab')
    const expectedCreativeTabs = expectedCreativeTabRecords({
      moduleId: string(descriptor.id),
      langKeys,
      sourceSignals,
      expectsCreativeTab,
      expectedCreativeEntries,
    })
    modules.push({
      moduleId: string(descriptor.id),
      aliases: moduleAliasesFor(descriptor, string(descriptor.id)),
      name: string(descriptor.name),
      version: string(descriptor.version),
      directory: normalizePath(path.relative(modulesRoot, moduleRoot)),
      descriptorPath: normalizePath(path.relative(modulesRoot, descriptorPath)),
      expectedFeatures,
      expectsCreativeTab,
      expectedCreativeTabs,
      expectedCreativeEntries: unique(expectedCreativeTabs.flatMap((tab) => tab.expectedEntries)),
      expectedSearchEntries: expectedCreativeTabs.flatMap((tab) => tab.searchExpected ? tab.expectedEntries : []),
      searchExpected: expectedCreativeTabs.some((tab) => tab.searchExpected),
      sourceSignals,
    })
  }
  return modules.sort((left, right) => left.moduleId.localeCompare(right.moduleId))
}

function moduleAliasesFor(descriptor, moduleId) {
  const aliases = []
  for (const alias of cleanList(descriptor.aliases)) aliases.push(alias)
  for (const alias of cleanList(descriptor.legacyAliases)) aliases.push(alias)
  for (const replacement of Array.isArray(descriptor.replacements) ? descriptor.replacements : []) {
    if (!replacement || typeof replacement !== 'object') continue
    const legacyId = string(replacement.legacyId)
    const replacementId = string(replacement.replacementId)
    if (legacyId && (!replacementId || replacementId === moduleId)) aliases.push(legacyId)
  }
  return unique(aliases.filter((alias) => alias && alias !== moduleId))
}

function expectedCreativeTabRecords({ moduleId, langKeys, sourceSignals, expectsCreativeTab, expectedCreativeEntries }) {
  const records = []
  for (const key of langKeys) {
    records.push({
      id: `${moduleId}:${key.replace(/^itemGroup\./, '').replaceAll('.', '_').toLowerCase()}`,
      titleKey: key,
      source: 'lang.itemGroup',
      searchExpected: true,
      expectedEntries: expectedCreativeEntries,
    })
  }
  for (const id of sourceSignals.creativeTabIds) {
    if (!records.some((record) => record.id === id)) {
      records.push({
        id,
        titleKey: '',
        source: 'source.creative_tab_declaration',
        searchExpected: true,
        expectedEntries: expectedCreativeEntries,
      })
    }
  }
  if (records.length === 0 && expectsCreativeTab) {
    records.push({
      id: `${moduleId}:native_modules`,
      titleKey: `itemGroup.${moduleId}`,
      source: 'inferred.content_module',
      searchExpected: true,
      expectedEntries: expectedCreativeEntries,
    })
  }
  return records
}

function expectedCreativeEntryIds(resourcePaths, sourceText, fallbackNamespace) {
  const entries = []
  for (const file of resourcePaths) {
    const item = file.match(/^assets\/([^/]+)\/models\/item\/(.+)\.json$/)
    if (item && !item[2].includes('/')) entries.push(`${item[1]}:${item[2]}`)
    const block = file.match(/^assets\/([^/]+)\/blockstates\/(.+)\.json$/)
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
  return unique(entries.map((entry) => entry.replace(/\\/g, '/').toLowerCase()))
}

function inferExpectedFeatures(descriptor, sourceText, resourcePaths, sourceSignals) {
  const access = object(descriptor.access)
  const adapterCore = object(access.adapterCore)
  const text = [
    descriptor.id,
    descriptor.kind,
    descriptor.role,
    descriptor.name,
    ...cleanList(descriptor.provides),
    ...cleanList(descriptor.consumes),
    ...cleanList(descriptor.permissions),
    ...cleanList(descriptor.gameModes),
    ...cleanList(adapterCore.domains),
  ].join(' ').toLowerCase()
  const features = new Set()
  const add = (feature, ...needles) => {
    if (needles.some((needle) => text.includes(needle))) features.add(feature)
  }
  add('blocks', 'block', 'blocks', 'multiblock', 'machine')
  add('items', 'item', 'items', 'inventory', 'loot')
  add('machines', 'machine', 'power', 'energy', 'industrial', 'logistics')
  add('entities', 'entity', 'entities', 'creature', 'npc', 'spawn_egg')
  if (/DeferredRegister|RegisterEvent/.test(sourceText)) {
    if (/Block\b|BlockEntity|BLOCKS/.test(sourceText)) features.add('blocks')
    if (/Item\b|ItemStack|ITEMS|BLOCK_ITEMS/.test(sourceText)) features.add('items')
  }
  if (resourcePaths.some((file) => file.includes('/blockstates/'))) features.add('blocks')
  if (resourcePaths.some((file) => file.includes('/models/item/') || file.includes('/models/block/'))) features.add('items')
  if (sourceSignals.creativeDeclared) features.add('creative_tab')
  return [...features].sort()
}

function sourceSignalRecord(javaFiles, sourceText, langKeys, resourcePaths) {
  const creativeFiles = javaFiles
    .filter((file) => /CreativeTabs\.java$/.test(file.relative) || /CreativeTab/.test(file.relative))
    .map((file) => normalizePath(file.relative))
  return {
    hasDeferredRegister: /DeferredRegister|RegisterEvent/.test(sourceText),
    hasBlockstates: resourcePaths.some((file) => file.includes('/blockstates/')),
    hasModels: resourcePaths.some((file) => file.includes('/models/item/') || file.includes('/models/block/')),
    creativeDeclared: creativeFiles.length > 0
      || /CreativeModeTab|registerCreativeTab|creative_tab|creative_tabs|EchoCreativeContentGroup/.test(sourceText)
      || langKeys.length > 0,
    creativeTabFiles: creativeFiles,
    itemGroupLangKeys: langKeys,
    creativeTabIds: unique([
      ...matches(sourceText, /"id"\s*,\s*"([a-z0-9_.-]+:[a-z0-9_./-]+)"/g),
      ...matches(sourceText, /registerCreativeTab\([^;]*"([a-z0-9_.-]+:[a-z0-9_./-]+)"/g),
      ...matches(sourceText, /([a-z0-9_.-]+:[a-z0-9_./-]*creative[a-z0-9_./-]*)/g),
      ...matches(sourceText, /([a-z0-9_.-]+:[a-z0-9_./-]*tab[a-z0-9_./-]*)/g),
    ]),
  }
}

async function collectRuntimeCreativeEvidence(runtimeRoot, runtime) {
  const reportPaths = runtime === 'standalone'
    ? [
        path.join(runtimeRoot, 'reports', 'echo', 'standalone', 'all-module-creative-tab-visibility-smoke.json'),
        path.join(runtimeRoot, 'reports', 'echo', 'standalone', 'all-module-creative-tab-live-evidence.json'),
        path.join(runtimeRoot, 'reports', 'echo', 'standalone', 'client-creative-inventory-smoke.json'),
        path.join(runtimeRoot, 'reports', 'echo', 'standalone', 'creative-inventory-controller-smoke.json'),
        path.join(runtimeRoot, 'reports', 'echo', 'standalone', 'live-creative-inventory.json'),
        path.join(runtimeRoot, 'reports', 'echo', 'standalone', 'creative-tab-live-evidence.json'),
        path.join(runtimeRoot, 'reports', 'echo', 'standalone', 'voxel-content-play.json'),
        path.join(runtimeRoot, 'reports', 'echo', 'standalone', 'client-held-item-overlay-smoke.json'),
      ]
    : [
        path.join(runtimeRoot, 'build', 'native-all-module-creative-tab-visibility', 'native-all-module-creative-tab-visibility.json'),
        path.join(runtimeRoot, 'reports', 'echo-native', 'native-all-module-creative-tab-visibility.json'),
        path.join(runtimeRoot, 'reports', 'echo-native', 'all-module-creative-tab-live-evidence.json'),
        path.join(runtimeRoot, 'build', 'native-all-module-creative-tab-live-evidence', 'native-all-module-creative-tab-live-evidence.json'),
        path.join(runtimeRoot, 'reports', 'echo-native', 'live', 'module-activation.json'),
        path.join(runtimeRoot, 'build', 'native-live-client', 'module-activation.json'),
        path.join(runtimeRoot, 'run', 'echo-native', 'module-activation.json'),
      ]
  const sourceReports = []
  const evidence = {
    sourceReports,
    registryBackedModuleIds: new Set(),
    visibleParentModuleIds: new Set(),
    visibleSearchModuleIds: new Set(),
    selectableModuleIds: new Set(),
    playableModuleIds: new Set(),
  }
  for (const filePath of reportPaths) {
    const report = await readJsonIfExists(filePath)
    sourceReports.push({
      path: normalizePath(path.relative(runtimeRoot, filePath)),
      found: !!report,
      status: reportStatus(report),
      schema: string(report?.schema),
    })
    if (!report || report.parseError) continue
    collectLiveMarkerEvidence(evidence, report)
    addIds(evidence.registryBackedModuleIds, moduleIdsFromReport(report, [
      'registryBackedModuleIds',
      'creativeTabModuleIds',
    ]))
    addIds(evidence.visibleParentModuleIds, moduleIdsFromReport(report, [
      'visibleParentModuleIds',
      'visibleModuleIds',
    ]))
    addIds(evidence.visibleSearchModuleIds, moduleIdsFromReport(report, [
      'visibleSearchModuleIds',
      'searchVisibleModuleIds',
    ]))
    addIds(evidence.selectableModuleIds, moduleIdsFromReport(report, [
      'selectableModuleIds',
      'hotbarSelectableModuleIds',
    ]))
    addIds(evidence.playableModuleIds, moduleIdsFromReport(report, [
      'playableModuleIds',
      'usedModuleIds',
      'blockItemGameplayModuleIds',
    ]))
  }
  return evidence
}

async function collectRuntimeCreativeEvidenceFromSiblingRuntime(modulesRoot, runtimeRoot, runtime, evidence) {
  const sibling = path.join(
    path.dirname(modulesRoot),
    runtime === 'standalone' ? 'ECHO-Standalone-Runtime' : 'ECHO-Native-Platform',
  )
  if (path.resolve(sibling) === path.resolve(runtimeRoot) || !(await exists(sibling))) return
  const siblingEvidence = await collectRuntimeCreativeEvidence(sibling, runtime)
  for (const report of siblingEvidence.sourceReports) {
    evidence.sourceReports.push({
      ...report,
      path: normalizePath(path.join(path.basename(sibling), report.path)),
    })
  }
  mergeSet(evidence.registryBackedModuleIds, siblingEvidence.registryBackedModuleIds)
  mergeSet(evidence.visibleParentModuleIds, siblingEvidence.visibleParentModuleIds)
  mergeSet(evidence.visibleSearchModuleIds, siblingEvidence.visibleSearchModuleIds)
  mergeSet(evidence.selectableModuleIds, siblingEvidence.selectableModuleIds)
  mergeSet(evidence.playableModuleIds, siblingEvidence.playableModuleIds)
}

function applyEvidenceAliases(evidence, modules) {
  const sets = [
    evidence.registryBackedModuleIds,
    evidence.visibleParentModuleIds,
    evidence.visibleSearchModuleIds,
    evidence.selectableModuleIds,
    evidence.playableModuleIds,
  ]
  for (const module of modules) {
    const aliases = cleanList(module.aliases)
    if (module.moduleId === 'echosignalos' && !aliases.includes('signalos')) aliases.push('signalos')
    for (const alias of aliases) {
      for (const set of sets) {
        if (set.has(alias)) set.add(module.moduleId)
      }
    }
  }
}

function mergeSet(target, source) {
  for (const value of source) target.add(value)
}

function collectLiveMarkerEvidence(evidence, report) {
  const registryBridge = object(object(report.runtimeBridge).registryBridge ?? report.registryBridge)
  if (Object.keys(registryBridge).length === 0) return

  for (const tab of objectList(registryBridge.registeredCreativeTabs)) {
    collectNativeCreativeTab(evidence, tab)
  }

  if (registryBridge.nativeCreativeModuleTabRegistryBacked === true) {
    addIds(evidence.registryBackedModuleIds, moduleIdsFromItemIds(cleanList(registryBridge.visibleModuleItems)))
  }
  if (registryBridge.nativeCreativeModuleTabContentVisible === true
    && registryBridge.nativeCreativeModuleTabRegistryBacked === true) {
    const visibleModules = moduleIdsFromItemIds(cleanList(registryBridge.visibleModuleItems))
    addIds(evidence.visibleParentModuleIds, visibleModules)
    if (registryBridge.creativeVisibilityBridgeApplied === true) {
      addIds(evidence.visibleSearchModuleIds, visibleModules)
    }
  }

  addIds(evidence.selectableModuleIds, moduleIdsFromItemIds([
    ...cleanList(report.selectableItemIds),
    ...cleanList(registryBridge.creativeTabSelectableItemIds),
    ...cleanList(object(report.creativeInventorySelection).selectedItemIds),
    ...cleanList(object(object(report.runtimeBridge).creativeInventorySelection).selectedItemIds),
  ]))
  addIds(evidence.playableModuleIds, moduleIdsFromItemIds([
    ...cleanList(report.playableItemIds),
    ...cleanList(report.usedItemIds),
    ...cleanList(report.placedBlockIds),
    ...cleanList(registryBridge.creativeTabPlayableItemIds),
    ...cleanList(object(report.creativeInventoryUse).usedItemIds),
    ...cleanList(object(object(report.runtimeBridge).creativeInventoryUse).usedItemIds),
  ]))
}

function collectNativeCreativeTab(evidence, tab) {
  if (!firstClassNativeCreativeTab(tab)) return
  const registryItems = normalizedContentIds(cleanList(tab.creativeTabItemsFromNativeRegistry))
  const parentItems = normalizedContentIds(cleanList(tab.creativeTabOutputProofItemIds))
  const searchItems = normalizedContentIds(cleanList(tab.creativeTabSearchOutputProofItemIds))
  const registryModules = moduleIdsFromItemIds(registryItems)
  addIds(evidence.registryBackedModuleIds, registryModules)
  if (parentItems.length > 0 && containsAll(parentItems, registryItems)) {
    addIds(evidence.visibleParentModuleIds, moduleIdsFromItemIds(parentItems))
  }
  if (tab.searchVisible !== false && searchItems.length > 0 && containsAll(searchItems, registryItems)) {
    addIds(evidence.visibleSearchModuleIds, moduleIdsFromItemIds(searchItems))
  }
}

function firstClassNativeCreativeTab(tab) {
  const registryItems = normalizedContentIds(cleanList(tab.creativeTabItemsFromNativeRegistry))
  const parentItems = normalizedContentIds(cleanList(tab.creativeTabOutputProofItemIds))
  const searchItems = normalizedContentIds(cleanList(tab.creativeTabSearchOutputProofItemIds))
  const searchVisible = tab.searchVisible !== false
  return tab.firstClassNativeCreativeTabPresent === true
    && tab.registered === true
    && tab.nativeRegistryContentBacked === true
    && tab.releaseCreativeTabTrusted === true
    && tab.creativeTabOutputBacked === true
    && tab.creativeTabSearchOutputBacked === true
    && tab.declaredCreativeTabItemsBackedByNativeRegistry !== false
    && tab.declaredIconItemBackedByNativeRegistry !== false
    && tab.resolvedIconItemBackedByNativeRegistry !== false
    && tab.fallbackOnlyCreativeVisibility !== true
    && registryItems.length > 0
    && parentItems.length > 0
    && (!searchVisible || searchItems.length > 0)
    && containsAll(parentItems, registryItems)
    && (!searchVisible || containsAll(searchItems, registryItems))
}

function moduleIdsFromReport(report, keys) {
  const values = []
  for (const key of keys) values.push(...cleanList(report?.[key]))
  for (const module of Array.isArray(report?.modules) ? report.modules : []) {
    if (typeof module === 'string') values.push(module)
    else if (module && typeof module === 'object') {
      if (keys.includes('registryBackedModuleIds') && module.registryBacked === true) values.push(module.moduleId)
      if (keys.includes('visibleParentModuleIds') && module.visibleParent === true) values.push(module.moduleId)
      if (keys.includes('visibleSearchModuleIds') && module.visibleSearch === true) values.push(module.moduleId)
      if (keys.includes('selectableModuleIds') && module.selectable === true) values.push(module.moduleId)
      if (keys.includes('playableModuleIds') && module.playable === true) values.push(module.moduleId)
    }
  }
  return unique(values.filter((value) => typeof value === 'string' && (value.startsWith('echo') || value.startsWith('signalos'))))
}

function moduleIdsFromItemIds(itemIds) {
  return unique(normalizedContentIds(itemIds)
    .map((itemId) => itemId.split(':')[0])
    .filter((moduleId) => moduleId.startsWith('echo') || moduleId.startsWith('signalos')))
}

function normalizedContentIds(ids) {
  return unique(cleanList(ids)
    .map((id) => id.trim().toLowerCase())
    .filter((id) => /^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(id)))
}

function containsAll(values, expected) {
  return expected.every((item) => values.includes(item))
}

async function itemGroupLangKeys(resourceFiles) {
  const keys = []
  for (const file of resourceFiles) {
    if (!/assets\/[^/]+\/lang\/.+\.json$/i.test(file.relative)) continue
    const json = await readJsonIfExists(file.absolute)
    if (!json || json.parseError) continue
    for (const key of Object.keys(json)) {
      if (key.startsWith('itemGroup.')) keys.push(key)
    }
  }
  return unique(keys)
}

async function readJoined(files) {
  const parts = []
  for (const file of files) {
    try {
      parts.push(await fs.readFile(file.absolute, 'utf8'))
    } catch {
      // Keep the inventory resilient if a generated file disappears mid-run.
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
      if (['.git', '.gradle', 'build', 'dist', 'node_modules', 'run'].includes(entry.name)) continue
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

async function readJsonIfExists(filePath) {
  if (!(await exists(filePath))) return null
  try {
    return await readJson(filePath)
  } catch (error) {
    return { parseError: error.message }
  }
}

async function exists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

function addIds(target, ids) {
  for (const id of ids) target.add(id)
}

function matches(text, regex) {
  return [...text.matchAll(regex)].map((match) => match[1]).filter(Boolean)
}

function reportStatus(report) {
  if (!report) return 'MISSING'
  if (report.parseError) return 'PARSE_ERROR'
  const value = string(report.status ?? report.result).toUpperCase()
  if (['PASS', 'PASSED', 'SUCCESS', 'OK'].includes(value)) return 'PASS'
  if (['FAIL', 'FAILED', 'ERROR'].includes(value)) return 'FAIL'
  return value || 'MISSING'
}

function normalizeRuntime(runtime) {
  const value = string(runtime).toLowerCase()
  if (['standalone', 'echo_runtime_standalone'].includes(value)) return 'standalone'
  return 'echo_native'
}

function object(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

function objectList(value) {
  if (!Array.isArray(value)) return []
  return value.filter((item) => item && typeof item === 'object' && !Array.isArray(item))
}

function string(value) {
  return typeof value === 'string' ? value : ''
}

function cleanList(value) {
  return Array.isArray(value)
    ? value.filter((item) => typeof item === 'string' && item.trim()).map((item) => item.trim())
    : []
}

function unique(values) {
  return [...new Set(values)].sort()
}

function normalizePath(value) {
  return value.replace(/\\/g, '/')
}

function parseArgs(argv) {
  const options = {
    modulesRoot: process.cwd(),
    runtimeRoot: process.cwd(),
    runtime: 'echo_native',
    out: '',
    strict: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--modules-root') options.modulesRoot = argv[++index]
    else if (arg === '--runtime-root') options.runtimeRoot = argv[++index]
    else if (arg === '--runtime') options.runtime = argv[++index]
    else if (arg === '--out') options.out = argv[++index]
    else if (arg === '--strict') options.strict = true
    else if (arg === '--help') options.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return options
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const options = parseArgs(process.argv.slice(2))
    if (options.help) {
      console.log('Usage: node scripts/generate-all-module-creative-tab-visibility.mjs --runtime <echo_native|standalone> --modules-root <path> --runtime-root <path> [--out <path>] [--strict]')
    } else {
      const { report, path: outputPath } = await generateAllModuleCreativeTabVisibility(options)
      console.log(`Wrote ${report.runtime} all-module creative tab visibility report: ${outputPath}`)
      if (options.strict && report.status !== 'PASS') {
        throw new Error(`${report.runtime} creative tab visibility report is ${report.status}: ${report.summary.failingModuleCount} failing module(s).`)
      }
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}

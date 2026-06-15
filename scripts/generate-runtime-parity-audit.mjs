import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { generateAdapterCoreStrictPortAudit } from './generate-adaptercore-strict-port-audit.mjs'
import { generateNeoForgeRuntimeEvidence } from './generate-neoforge-runtime-evidence.mjs'
import { generateRuntimePlayAudit } from './generate-runtime-play-audit.mjs'

const SCHEMA = 'echo.module.runtime_parity_audit.v1'
const DESCRIPTOR_PATH = path.join('src', 'main', 'resources', 'META-INF', 'echo.mod.json')
const DEFAULT_OUT_DIR = path.join('reports', 'runtime-parity')
const OFFICIAL_PRODUCT_RE =
  /^ECHO-(Ashfall|Openlands|Arcana-Division|Sky-Relay|Galactic-Survey)-(Native|NeoForge|Standalone)-Edition$/
const RUNTIMES = [
  { id: 'neoforge', label: 'NeoForge', artifactFamily: 'neoforge', ownerRepo: 'ECHO-Modules' },
  { id: 'echo_native', label: 'ECHO Native Loader', artifactFamily: 'echo-addon', ownerRepo: 'ECHO-Native-Platform' },
  { id: 'standalone', label: 'ECHO Standalone Runtime', artifactFamily: 'standalone', ownerRepo: 'ECHO-Standalone-Runtime' },
]
const VISIBLE_CORE_SURFACE_MODULES = [
  'echohudcore',
  'echoindex',
  'echoholomap',
  'echolens',
  'echoterminal',
  'echoscreencore',
  'echothemecore',
]
const CONTENT_BASELINE_MODULES = ['echoworldcore', 'echoblockworks']
const PACK_BASELINE_MODULES = [...VISIBLE_CORE_SURFACE_MODULES, ...CONTENT_BASELINE_MODULES]
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

const EVIDENCE_LEVELS = [
  'static-source',
  'compiled-artifact',
  'lifecycle-ran',
  'host-registered',
  'visible-actionable',
  'gameplay-mutated',
  'save-reloaded',
  'network-synced',
  'manual-play-verified',
]

const FEATURE_EVIDENCE_REQUIREMENTS = {
  gui: 'visible-actionable',
  hud: 'visible-actionable',
  screen: 'visible-actionable',
  inventory_overlay: 'visible-actionable',
  terminal: 'visible-actionable',
  index: 'visible-actionable',
  holomap: 'visible-actionable',
  lens: 'visible-actionable',
  blocks: 'gameplay-mutated',
  items: 'gameplay-mutated',
  creative_tab: 'gameplay-mutated',
  block_actions: 'gameplay-mutated',
  worldgen: 'host-registered',
  recipes: 'host-registered',
  loot: 'host-registered',
  missions: 'save-reloaded',
  networking: 'network-synced',
  save_data: 'save-reloaded',
  entities: 'gameplay-mutated',
  audio: 'visible-actionable',
  machines: 'gameplay-mutated',
}

async function exists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

async function readText(filePath) {
  return fs.readFile(filePath, 'utf8')
}

async function readJson(filePath) {
  const text = await readText(filePath)
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
      files.push({
        absolute,
        relative: normalizePath(path.relative(base, absolute)),
      })
    }
  }
  return files
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
    modules.push(await moduleRecord(repoRoot, entry.name, moduleRoot, descriptorPath, descriptor))
  }
  return modules.sort((left, right) => left.moduleId.localeCompare(right.moduleId))
}

async function moduleRecord(repoRoot, directoryName, moduleRoot, descriptorPath, descriptor) {
  const access = object(descriptor.access)
  const adapterCore = object(access.adapterCore)
  const moduleId = string(descriptor.id)
  const entrypoint = string(descriptor.entrypoint)
  const nativeEntrypoint = string(access.nativeEntrypoint)
  const entrypointSourcePath = entrypoint ? classSourcePath(moduleRoot, entrypoint) : ''
  const nativeEntrypointSourcePath = nativeEntrypoint ? classSourcePath(moduleRoot, nativeEntrypoint) : ''
  const entrypointSourceExists = entrypointSourcePath ? await exists(entrypointSourcePath) : false
  const nativeEntrypointSourceExists = nativeEntrypointSourcePath ? await exists(nativeEntrypointSourcePath) : false
  const entrypointSource = entrypointSourceExists ? await readText(entrypointSourcePath) : ''
  const nativeEntrypointSource = nativeEntrypointSourceExists ? await readText(nativeEntrypointSourcePath) : ''
  const resourceFiles = await listFiles(path.join(moduleRoot, 'src', 'main', 'resources'))
  const javaFiles = await listFiles(path.join(moduleRoot, 'src', 'main', 'java'))
  const descriptorRelativePath = normalizePath(path.relative(repoRoot, descriptorPath))
  const declaredDomains = cleanList(adapterCore.domains)
  const declaredRuntimes = cleanList(adapterCore.runtimes)
  const itemGroupLangKeys = await itemGroupLangKeysFor(resourceFiles)
  const sourceSignals = sourceSignalRecord(entrypointSource, nativeEntrypointSource, javaFiles, resourceFiles, itemGroupLangKeys)
  const resourceSignals = resourceSignalRecord(resourceFiles)
  const creativeSource = await creativeSourceText(javaFiles)
  const aliases = moduleAliasesFor(descriptor, moduleId)
  const expectedCreativeEntries = expectedCreativeEntryIds(resourceFiles, creativeSource, moduleId)
  const expectedFeatures = inferExpectedFeatures({
    moduleId,
    aliases,
    descriptor,
    declaredDomains,
    sourceSignals,
    resourceSignals,
    expectedCreativeEntries,
  })
  const expectedCreativeTabs = expectedCreativeTabsFor({
    moduleId,
    expectedFeatures,
    sourceSignals,
    expectedCreativeEntries,
  })

  return {
    moduleId,
    directoryName,
    directory: normalizePath(path.relative(repoRoot, moduleRoot)),
    descriptorPath: descriptorRelativePath,
    descriptor,
    name: string(descriptor.name),
    version: string(descriptor.version),
    kind: string(descriptor.kind),
    role: string(descriptor.role),
    side: string(descriptor.side),
    standalone: descriptor.standalone !== false,
    requires: cleanList(descriptor.requires),
    optional: cleanList(descriptor.optional),
    provides: cleanList(descriptor.provides),
    consumes: cleanList(descriptor.consumes),
    permissions: cleanList(descriptor.permissions),
    gameModes: cleanList(descriptor.gameModes),
    aliases,
    declaredDomains,
    declaredRuntimes,
    entrypoint,
    entrypointSourceExists,
    entrypointSourcePath: entrypointSourcePath ? normalizePath(path.relative(repoRoot, entrypointSourcePath)) : '',
    nativeEntrypoint,
    nativeEntrypointSourceExists,
    nativeEntrypointSourcePath: nativeEntrypointSourcePath
      ? normalizePath(path.relative(repoRoot, nativeEntrypointSourcePath))
      : '',
    expectedFeatures,
    expectedCreativeTabs,
    expectedCreativeEntries,
    sourceSignals,
    resourceSignals,
  }
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
  return aliases
    .map((alias) => alias.trim())
    .filter((alias) => alias && alias !== moduleId)
    .filter(uniqueFilter)
}

function classSourcePath(moduleRoot, className) {
  return path.join(moduleRoot, 'src', 'main', 'java', `${className.replace(/\./g, path.sep)}.java`)
}

function sourceSignalRecord(entrypointSource, nativeEntrypointSource, javaFiles, resourceFiles, itemGroupLangKeys = []) {
  const source = `${entrypointSource}\n${nativeEntrypointSource}`
  const javaPaths = javaFiles.map((file) => file.relative.toLowerCase())
  const resourcePaths = resourceFiles.map((file) => file.relative.toLowerCase())
  return {
    hasDescribeNativeSurfaces: source.includes('describeNativeSurfaces'),
    hasActivationRegistrar: source.includes('EchoNativeActivationSurfaceRegistrar'),
    hasRegisterContent: source.includes('registerContent('),
    hasRegisterServices: source.includes('registerServices('),
    hasMutationRecord: source.includes('recordMutation(') || source.includes('EchoNativeLoadStatus.MUTATED'),
    hasNativeSurfaceDeclaration: source.includes('nativeSurfaceImplementationClass'),
    hasNativeClientRouteRegistrar: source.includes('ensureNativeClientRoutesRegisteredForNativeLoader'),
    hasDeferredRegister: source.includes('DeferredRegister') || source.includes('RegisterEvent'),
    hasCreativeTabClass: javaPaths.some((file) => file.endsWith('creativetabs.java') || file.includes('creativetab')),
    hasCreativeTabRegistration: source.includes('CreativeModeTab')
      || source.includes('registerCreativeTab')
      || source.includes('creative_tab')
      || source.includes('creative_tabs')
      || source.includes('EchoCreativeContentGroup')
      || itemGroupLangKeys.length > 0,
    itemGroupLangKeys,
    hasScreenClass: javaPaths.some((file) => file.includes('screen') || file.includes('client')),
    hasOverlayClass: javaPaths.some((file) => file.includes('overlay') || file.includes('hud')),
    hasBlockClass: javaPaths.some((file) => file.includes('block')),
    hasItemClass: javaPaths.some((file) => file.includes('item')),
    hasWorldClass: javaPaths.some((file) => file.includes('world') || file.includes('biome') || file.includes('structure')),
    hasResourceData: resourcePaths.some((file) => file.startsWith('data/')),
  }
}

function resourceSignalRecord(resourceFiles) {
  const files = resourceFiles.map((file) => file.relative.toLowerCase())
  return {
    hasAssets: files.some((file) => file.startsWith('assets/')),
    hasData: files.some((file) => file.startsWith('data/')),
    hasRecipes: files.some((file) => file.includes('/recipe/') || file.includes('/recipes/')),
    hasLoot: files.some((file) => file.includes('/loot') || file.includes('loot_tables')),
    hasTags: files.some((file) => file.includes('/tags/')),
    hasWorldgen: files.some((file) => file.includes('/worldgen/') || file.includes('world_regions') || file.includes('world_hazards')),
    hasStructures: files.some((file) => file.includes('/structure') || file.includes('/structures')),
    hasModels: files.some((file) => file.includes('/models/')),
    hasBlockstates: files.some((file) => file.includes('/blockstates/')),
    hasSounds: files.some((file) => file.endsWith('sounds.json') || file.includes('/sounds/')),
    hasUiAssets: files.some((file) => file.includes('/eui/') || file.includes('/ui/') || file.includes('/screens/')),
  }
}

async function itemGroupLangKeysFor(resourceFiles) {
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

async function creativeSourceText(javaFiles) {
  const parts = []
  for (const file of javaFiles) {
    const relative = file.relative.replace(/\\/g, '/')
    if (!relative.endsWith('.java')) continue
    if (!(/CreativeTab/.test(relative)
      || /Items\.java$/.test(relative)
      || /Blocks\.java$/.test(relative)
      || /ContentDefinitions\.java$/.test(relative)
      || /Machines\.java$/.test(relative)
      || /NativeModule\.java$/.test(relative)
      || /ProductBridgeProvider\.java$/.test(relative)
      || /registry\//i.test(relative))) continue
    parts.push(await readText(file.absolute))
  }
  return parts.join('\n')
}

function inferExpectedFeatures({ moduleId, descriptor, declaredDomains, sourceSignals, resourceSignals, expectedCreativeEntries = [] }) {
  const text = [
    moduleId,
    descriptor.kind,
    descriptor.role,
    descriptor.name,
    ...cleanList(descriptor.provides),
    ...cleanList(descriptor.consumes),
    ...cleanList(descriptor.permissions),
    ...cleanList(descriptor.gameModes),
    ...declaredDomains,
    ...assetsText(descriptor.assets),
  ].join(' ').toLowerCase()
  const features = new Set()
  const add = (feature, ...needles) => {
    if (needles.some((needle) => text.includes(needle))) features.add(feature)
  }

  if (string(descriptor.kind) === 'ui_pack' || text.includes('ui_') || text.includes('ui.')) features.add('gui')
  add('gui', 'screen', 'terminal', 'index', 'holomap', 'lens', 'hud', 'wiki', 'guide', 'overlay', 'eui')
  add('hud', 'hud', 'ui.overlays', 'ui_overlays', 'overlay')
  add('screen', 'screen', 'ui.screens', 'ui_screens', 'menu', 'terminal', 'index', 'holomap', 'lens', 'wiki', 'eui')
  add('inventory_overlay', 'inventory_overlay', 'inventory.overlay', 'index.inventory')
  add('terminal', 'terminal')
  add('index', 'index', 'catalog', 'recipe browser')
  add('holomap', 'holomap', 'holo.map', 'map', 'waypoint')
  add('lens', 'lens', 'scanner', 'scan')
  add('blocks', 'block', 'blocks', 'multiblock', 'machine')
  add('items', 'item', 'items', 'inventory', 'loot')
  add('creative_tab', 'creative tab', 'creative_tab', 'creative_tabs', 'itemgroup')
  add('block_actions', 'block action', 'block_actions', 'machine', 'place', 'use', 'break', 'interaction')
  add('worldgen', 'worldgen', 'world', 'biome', 'structure', 'region', 'hazard', 'weather', 'spawn')
  add('recipes', 'recipe', 'recipes', 'crafting')
  add('loot', 'loot')
  add('missions', 'mission', 'quest', 'objective', 'tutorial')
  add('networking', 'network', 'packet', 'payload', 'sync', 'netcore')
  add('save_data', 'save', 'saved', 'profile', 'data', 'recovery')
  add('entities', 'entity', 'entities', 'creature', 'npc', 'spawn_egg')
  add('audio', 'sound', 'audio', 'music')
  add('machines', 'machine', 'power', 'energy', 'grid', 'industrial', 'logistics')

  if (moduleId === 'echohudcore') addAll(features, ['gui', 'hud', 'screen'])
  if (moduleId === 'echoindex') addAll(features, ['gui', 'screen', 'inventory_overlay', 'index', 'recipes', 'items'])
  if (moduleId === 'echoholomap') addAll(features, ['gui', 'screen', 'holomap', 'worldgen'])
  if (moduleId === 'echolens') addAll(features, ['gui', 'hud', 'screen', 'lens', 'block_actions'])
  if (moduleId === 'echoterminal') addAll(features, ['gui', 'screen', 'terminal', 'blocks', 'items', 'block_actions'])
  if (moduleId === 'echoscreencore') addAll(features, ['gui', 'screen'])
  if (moduleId === 'echothemecore') addAll(features, ['gui', 'screen'])
  if (moduleId === 'echoblockworks') addAll(features, ['blocks', 'items', 'block_actions'])

  if (sourceSignals.hasOverlayClass) addAll(features, ['gui', 'hud'])
  if (sourceSignals.hasScreenClass || resourceSignals.hasUiAssets) addAll(features, ['gui', 'screen'])
  if (sourceSignals.hasBlockClass || resourceSignals.hasBlockstates) features.add('blocks')
  if (sourceSignals.hasItemClass || resourceSignals.hasModels) features.add('items')
  const hasExpectedCreativeEntries = expectedCreativeEntries.length > 0
  if (hasExpectedCreativeEntries
    || sourceSignals.hasDeferredRegister
    || resourceSignals.hasBlockstates
    || ((sourceSignals.hasCreativeTabClass || sourceSignals.hasCreativeTabRegistration) && hasExpectedCreativeEntries)) {
    features.add('creative_tab')
  }
  if (resourceSignals.hasWorldgen || resourceSignals.hasStructures || sourceSignals.hasWorldClass) features.add('worldgen')
  if (resourceSignals.hasRecipes) features.add('recipes')
  if (resourceSignals.hasLoot) features.add('loot')
  if (resourceSignals.hasSounds) features.add('audio')

  return FEATURE_ORDER.filter((feature) => features.has(feature))
}

function expectedCreativeTabsFor({ moduleId, expectedFeatures, sourceSignals, expectedCreativeEntries = [] }) {
  if (!expectedFeatures.includes('creative_tab')) return []
  return [{
    id: `${moduleId}:native_modules`,
    titleKey: `itemGroup.${moduleId}`,
    source: sourceSignals.hasCreativeTabClass || sourceSignals.hasCreativeTabRegistration
      ? 'source.creative_tab'
      : 'inferred.content_module',
    searchExpected: true,
    expectedEntries: expectedCreativeEntries,
  }]
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

function assetsText(value) {
  if (!Array.isArray(value)) return []
  return value.flatMap((asset) => Object.values(object(asset)).map((item) => String(item)))
}

async function parseDocsIndex(repoRoot, modules) {
  const filePath = path.join(repoRoot, 'docs', 'module-docs-index.md')
  if (!(await exists(filePath))) {
    return {
      path: normalizePath(path.relative(repoRoot, filePath)),
      found: false,
      indexedIds: [],
      indexedDirectories: [],
      missingModuleIds: modules.map((module) => module.moduleId),
      missingDirectories: modules.map((module) => module.directoryName),
      extraIndexEntries: [],
    }
  }
  const text = await readText(filePath)
  const indexedIds = new Set()
  const indexedDirectories = new Set()
  const linkRe = /\[([^\]]+)\]\(\.\.\/addons\/([^/)]+)\/README\.md\)/g
  for (const match of text.matchAll(linkRe)) {
    indexedIds.add(match[1])
    indexedDirectories.add(match[2])
  }
  const moduleIds = new Set(modules.map((module) => module.moduleId))
  const moduleDirs = new Set(modules.map((module) => module.directoryName))
  return {
    path: normalizePath(path.relative(repoRoot, filePath)),
    found: true,
    indexedIds: [...indexedIds].sort(),
    indexedDirectories: [...indexedDirectories].sort(),
    missingModuleIds: modules.map((module) => module.moduleId).filter((id) => !indexedIds.has(id)).sort(),
    missingDirectories: modules.map((module) => module.directoryName).filter((id) => !indexedDirectories.has(id)).sort(),
    extraIndexEntries: [...indexedIds].filter((id) => !moduleIds.has(id) && !moduleDirs.has(id)).sort(),
  }
}

async function discoverPackManifests(echoRoot) {
  const entries = await fs.readdir(echoRoot, { withFileTypes: true })
  const repos = entries
    .filter((entry) => entry.isDirectory() && OFFICIAL_PRODUCT_RE.test(entry.name))
    .map((entry) => entry.name)
    .sort()
  const records = []
  for (const repo of repos) {
    const repoRoot = path.join(echoRoot, repo)
    const match = repo.match(OFFICIAL_PRODUCT_RE)
    const product = match[1]
    const lane = match[2]
    const manifestPaths = []
    const templatePath = path.join(repoRoot, 'release-manifest.template.json')
    if (await exists(templatePath)) manifestPaths.push(templatePath)
    const releaseAssetsRoot = path.join(repoRoot, 'release-assets')
    for (const file of await listFiles(releaseAssetsRoot)) {
      if (file.relative.endsWith('.pack.json')) manifestPaths.push(file.absolute)
    }
    for (const manifestPath of manifestPaths.sort((left, right) => left.localeCompare(right))) {
      try {
        const manifest = await readJson(manifestPath)
        const modules = cleanList((manifest.moduleRequirements ?? []).map((item) => item?.id))
        const family = string(manifest.moduleArtifactFamily)
        const missingBaselineModules = PACK_BASELINE_MODULES.filter((moduleId) => !modules.includes(moduleId))
        records.push({
          repo,
          repoRoot,
          product,
          lane,
          family,
          manifestPath: normalizePath(path.relative(repoRoot, manifestPath)),
          isTemplate: path.basename(manifestPath) === 'release-manifest.template.json',
          moduleCount: modules.length,
          moduleRequirements: modules,
          missingVisibleCoreSurfaceModules: VISIBLE_CORE_SURFACE_MODULES.filter((moduleId) => !modules.includes(moduleId)),
          missingContentBaselineModules: CONTENT_BASELINE_MODULES.filter((moduleId) => !modules.includes(moduleId)),
          missingBaselineModules,
        })
      } catch (error) {
        records.push({
          repo,
          repoRoot,
          product,
          lane,
          family: '',
          manifestPath: normalizePath(path.relative(repoRoot, manifestPath)),
          parseError: error.message,
          moduleCount: 0,
          moduleRequirements: [],
          missingVisibleCoreSurfaceModules: VISIBLE_CORE_SURFACE_MODULES,
          missingContentBaselineModules: CONTENT_BASELINE_MODULES,
          missingBaselineModules: PACK_BASELINE_MODULES,
        })
      }
    }
  }
  return records
}

function preferredPackManifests(packManifests) {
  const grouped = new Map()
  for (const manifest of packManifests) {
    const key = manifest.repo
    const existing = grouped.get(key)
    if (!existing || manifest.isTemplate || (!existing.isTemplate && manifest.manifestPath < existing.manifestPath)) {
      grouped.set(key, manifest)
    }
  }
  return [...grouped.values()].sort((left, right) => left.repo.localeCompare(right.repo))
}

function packRefsForModule(moduleId, packManifests) {
  return packManifests
    .filter((manifest) => manifest.moduleRequirements.includes(moduleId))
    .map((manifest) => ({
      product: manifest.product,
      lane: manifest.lane,
      family: manifest.family,
      repo: manifest.repo,
      manifestPath: manifest.manifestPath,
    }))
}

async function releaseIndex(repoRoot) {
  const candidates = await releaseIndexCandidates(repoRoot)
  if (candidates.length === 0) return { found: false, modules: new Map() }
  candidates.sort((left, right) => {
    if (right.moduleCount !== left.moduleCount) return right.moduleCount - left.moduleCount
    return right.generatedAt.localeCompare(left.generatedAt)
  })
  const { releasePath, release } = candidates[0]
  const modules = new Map()
  for (const module of Array.isArray(release.modules) ? release.modules : []) {
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
    const release = await readJson(primary)
    candidates.push(releaseIndexCandidate(primary, release))
  }
  if (!(await exists(distRoot))) return candidates
  for (const entry of await fs.readdir(distRoot, { withFileTypes: true })) {
    if (!entry.isDirectory() || entry.name === 'echo-module-release') continue
    const releasePath = path.join(distRoot, entry.name, 'echo-release.json')
    if (!(await exists(releasePath))) continue
    try {
      const release = await readJson(releasePath)
      candidates.push(releaseIndexCandidate(releasePath, release))
    } catch {
      // Ignore stale scratch outputs that are not valid module release manifests.
    }
  }
  return candidates
}

function releaseIndexCandidate(releasePath, release) {
  return {
    releasePath,
    release,
    moduleCount: Array.isArray(release.modules) ? release.modules.length : 0,
    generatedAt: string(release.generatedAt),
  }
}

async function collectRuntimeEvidence(echoRoot, modulesRepoRoot, outDir, modules = []) {
  const neoforge = await collectNeoForgeRuntimeEvidence(modulesRepoRoot, outDir)
  const native = await collectNativeRuntimeEvidence(path.join(echoRoot, 'ECHO-Native-Platform'))
  const standalone = await collectStandaloneRuntimeEvidence(path.join(echoRoot, 'ECHO-Standalone-Runtime'))
  applyRuntimeEvidenceAliases(neoforge, modules)
  applyRuntimeEvidenceAliases(native, modules)
  applyRuntimeEvidenceAliases(standalone, modules)
  return {
    neoforge,
    echo_native: native,
    standalone,
  }
}

function applyRuntimeEvidenceAliases(evidence, modules) {
  const sets = [
    evidence.loadedModules,
    evidence.coveredModules,
    evidence.executedModules,
    evidence.uiRouteModules,
    evidence.contentHostModules,
    evidence.uiVisibleModules,
    evidence.actionMutationModules,
    evidence.blockItemGameplayModules,
    evidence.creativeTabRegistryModules,
    evidence.creativeTabParentVisibleModules,
    evidence.creativeTabSearchVisibleModules,
    evidence.creativeTabSelectableModules,
    evidence.creativeTabPlayableModules,
    evidence.worldgenModules,
    evidence.saveReloadModules,
    evidence.networkSyncModules,
  ]
  for (const module of modules) {
    const aliases = cleanList(module.aliases)
    if (module.moduleId === 'echosignalos' && !aliases.includes('signalos')) aliases.push('signalos')
    for (const alias of aliases) {
      for (const set of sets) {
        if (set?.has(alias)) set.add(module.moduleId)
      }
      if (evidence.creativeTabEvidenceByModule?.has(alias) && !evidence.creativeTabEvidenceByModule.has(module.moduleId)) {
        evidence.creativeTabEvidenceByModule.set(module.moduleId, {
          ...evidence.creativeTabEvidenceByModule.get(alias),
          moduleId: module.moduleId,
          evidenceAlias: alias,
        })
      }
    }
  }
}

function emptyRuntimeEvidence(runtime, repoRoot) {
  return {
    runtime,
    repoRoot,
    reports: {},
    loadedModules: new Set(),
    coveredModules: new Set(),
    executedModules: new Set(),
    uiRouteModules: new Set(),
    contentHostModules: new Set(),
    uiVisibleModules: new Set(),
    actionMutationModules: new Set(),
    blockItemGameplayModules: new Set(),
    creativeTabRegistryModules: new Set(),
    creativeTabParentVisibleModules: new Set(),
    creativeTabSearchVisibleModules: new Set(),
    creativeTabSelectableModules: new Set(),
    creativeTabPlayableModules: new Set(),
    creativeTabEvidenceByModule: new Map(),
    worldgenModules: new Set(),
    saveReloadModules: new Set(),
    networkSyncModules: new Set(),
    routeDispatchCount: 0,
    artifactLoadProof: false,
    lifecycleProof: false,
    contentHostProof: false,
    uiHostProof: false,
    actionHostProof: false,
    blockItemHostProof: false,
    creativeTabHostProof: false,
    worldgenHostProof: false,
    saveNetworkProof: false,
  }
}

async function collectNeoForgeRuntimeEvidence(repoRoot, outDir) {
  const evidence = emptyRuntimeEvidence('neoforge', repoRoot)
  if (!(await exists(repoRoot))) return evidence
  const { report, path: reportPath } = await generateNeoForgeRuntimeEvidence({ repoRoot, outDir })
  evidence.reports.neoForgeRuntimeEvidence = reportSummary(repoRoot, reportPath, report)
  for (const moduleId of cleanList(report.loadedModuleIds)) evidence.loadedModules.add(moduleId)
  for (const moduleId of cleanList(report.lifecycleModuleIds)) evidence.executedModules.add(moduleId)
  for (const moduleId of cleanList(report.contentHostModuleIds)) evidence.contentHostModules.add(moduleId)
  for (const moduleId of cleanList(report.uiVisibleModuleIds)) evidence.uiVisibleModules.add(moduleId)
  for (const moduleId of cleanList(report.actionMutationModuleIds)) evidence.actionMutationModules.add(moduleId)
  for (const moduleId of cleanList(report.blockItemGameplayModuleIds)) evidence.blockItemGameplayModules.add(moduleId)
  for (const moduleId of cleanList(report.worldgenModuleIds)) evidence.worldgenModules.add(moduleId)
  for (const moduleId of cleanList(report.saveReloadModuleIds)) evidence.saveReloadModules.add(moduleId)
  for (const moduleId of cleanList(report.networkSyncModuleIds)) evidence.networkSyncModules.add(moduleId)
  for (const moduleId of cleanList(report.gameTestModuleIds)) evidence.coveredModules.add(moduleId)
  collectCreativeTabEvidence(report, evidence)
  evidence.routeDispatchCount = cleanList(report.visibleRoutes).length
  evidence.artifactLoadProof = reportStatusIs(report, 'PASS') || evidence.loadedModules.size > 0
  evidence.lifecycleProof = evidence.executedModules.size > 0
  evidence.contentHostProof = evidence.contentHostModules.size > 0
  evidence.uiHostProof = evidence.uiVisibleModules.size > 0
  evidence.actionHostProof = evidence.actionMutationModules.size > 0
  evidence.blockItemHostProof = evidence.blockItemGameplayModules.size > 0
  evidence.creativeTabHostProof = evidence.creativeTabPlayableModules.size > 0
  evidence.worldgenHostProof = evidence.worldgenModules.size > 0
  evidence.saveNetworkProof = evidence.saveReloadModules.size > 0 || evidence.networkSyncModules.size > 0
  return evidence
}

async function collectNativeRuntimeEvidence(nativeRoot) {
  const evidence = emptyRuntimeEvidence('echo_native', nativeRoot)
  if (!(await exists(nativeRoot))) return evidence

  const artifactLoadPath = path.join(nativeRoot, 'build', 'native-all-bridgeable-module-artifact-load-state', 'native-all-bridgeable-module-artifact-load-state.json')
  const routePath = path.join(nativeRoot, 'build', 'native-agent2-client-routes', 'native-client-route-ownership.json')
  const runtimeTruthPath = path.join(nativeRoot, 'build', 'agent5', 'runtime-truth-gate', 'agent5-runtime-truth-gate.json')
  const agent5UiBridgePath = path.join(nativeRoot, 'build', 'agent5', 'ui-bridge-contract', 'agent5-ui-bridge-contract.json')
  const agent4WorldStartupPath = path.join(nativeRoot, 'build', 'agent4', 'world-startup', 'native-agent4-world-startup.json')
  const agent9MachineRuntimePath = path.join(nativeRoot, 'build', 'agent9', 'machine-runtime-host', 'agent9-machine-runtime-host.json')
  const mutationTruthGatePath = path.join(nativeRoot, 'build', 'mutation-truth-gate', 'native-mutation-truth-gate.json')
  const agent4RegistryStatePath = path.join(nativeRoot, 'build', 'agent4', 'registry-content', 'native-agent4-registry-content-state.json')
  const creativeTabVisibilityPath = path.join(nativeRoot, 'build', 'native-all-module-creative-tab-visibility', 'native-all-module-creative-tab-visibility.json')
  const registryInventoryPath = path.join(nativeRoot, 'reports', 'echo-native', 'ashfall', 'registry-source-inventory.json')
  const serviceBusPath = path.join(nativeRoot, 'reports', 'echo-native', 'ashfall', 'service-bus-registry.json')

  const artifactLoad = await readJsonIfExists(artifactLoadPath)
  const route = await readJsonIfExists(routePath)
  const runtimeTruth = await readJsonIfExists(runtimeTruthPath)
  const agent5UiBridge = await readJsonIfExists(agent5UiBridgePath)
  const agent4WorldStartup = await readJsonIfExists(agent4WorldStartupPath)
  const agent9MachineRuntime = await readJsonIfExists(agent9MachineRuntimePath)
  const mutationTruthGate = await readJsonIfExists(mutationTruthGatePath)
  const agent4RegistryState = await readJsonIfExists(agent4RegistryStatePath)
  const creativeTabVisibility = await readJsonIfExists(creativeTabVisibilityPath)
  const registryInventory = await readJsonIfExists(registryInventoryPath)
  const serviceBus = await readJsonIfExists(serviceBusPath)

  evidence.reports.artifactLoadState = reportSummary(nativeRoot, artifactLoadPath, artifactLoad)
  evidence.reports.clientRouteOwnership = reportSummary(nativeRoot, routePath, route)
  evidence.reports.runtimeTruthGate = reportSummary(nativeRoot, runtimeTruthPath, runtimeTruth)
  evidence.reports.agent5UiBridgeContract = reportSummary(nativeRoot, agent5UiBridgePath, agent5UiBridge)
  evidence.reports.agent4WorldStartup = reportSummary(nativeRoot, agent4WorldStartupPath, agent4WorldStartup)
  evidence.reports.agent9MachineRuntimeHost = reportSummary(nativeRoot, agent9MachineRuntimePath, agent9MachineRuntime)
  evidence.reports.mutationTruthGate = reportSummary(nativeRoot, mutationTruthGatePath, mutationTruthGate)
  evidence.reports.agent4RegistryContentState = reportSummary(nativeRoot, agent4RegistryStatePath, agent4RegistryState)
  evidence.reports.creativeTabVisibility = reportSummary(nativeRoot, creativeTabVisibilityPath, creativeTabVisibility)
  evidence.reports.registrySourceInventory = reportSummary(nativeRoot, registryInventoryPath, registryInventory)
  evidence.reports.serviceBusRegistry = reportSummary(nativeRoot, serviceBusPath, serviceBus)

  if (reportStatusIs(artifactLoad, 'MUTATED') && numberValue(artifactLoad.failedModuleCount) === 0) {
    evidence.artifactLoadProof = true
    evidence.lifecycleProof = true
    for (const module of Array.isArray(artifactLoad.modules) ? artifactLoad.modules : []) {
      const moduleId = string(module.moduleId ?? module.id)
      if (moduleId) evidence.loadedModules.add(moduleId)
    }
    for (const moduleId of cleanList(artifactLoad.targetModules)) evidence.loadedModules.add(moduleId)
    const loadedDir = path.join(nativeRoot, 'build', 'native-all-bridgeable-module-artifact-load-state', 'loaded-modules')
    if (await exists(loadedDir)) {
      for (const file of await fs.readdir(loadedDir)) {
        if (file.endsWith('.json')) evidence.loadedModules.add(file.slice(0, -5))
      }
    }
  }

  if (route && !route.parseError) {
    evidence.routeDispatchCount = numberValue(route.actionDispatchEvidence?.dispatchCount)
    collectRouteModules(route, evidence.uiRouteModules)
  }

  const runtimeTruthPass = reportStatusIs(runtimeTruth, 'PASS') && runtimeTruth.liveRuntimeAccepted === true
  const routeProof = evidence.routeDispatchCount > 0 && evidence.uiRouteModules.size > 0
  const registryProof = reportStatusIs(registryInventory, 'PASS')
  const serviceProof = reportStatusIs(serviceBus, 'PASS')
  const uiBridgeProof = reportStatusIs(agent5UiBridge, 'PASS')
  const worldStartupProof = reportStatusIs(agent4WorldStartup, 'PASS')
  const machineRuntimeProof = reportStatusIs(agent9MachineRuntime, 'PASS')
  const mutationTruthProof = reportStatusIs(mutationTruthGate, 'PASS')
  collectCreativeTabEvidence(creativeTabVisibility, evidence)

  evidence.contentHostProof = evidence.artifactLoadProof && runtimeTruthPass
  evidence.uiHostProof = (routeProof && runtimeTruthPass) || uiBridgeProof
  evidence.actionHostProof = (routeProof && runtimeTruthPass) || machineRuntimeProof || mutationTruthProof
  evidence.blockItemHostProof = (evidence.artifactLoadProof && runtimeTruthPass && (registryProof || serviceProof)) || machineRuntimeProof
  evidence.creativeTabHostProof = evidence.creativeTabPlayableModules.size > 0
  evidence.worldgenHostProof = (evidence.artifactLoadProof && runtimeTruthPass) || worldStartupProof
  evidence.saveNetworkProof = (evidence.artifactLoadProof && runtimeTruthPass) || machineRuntimeProof || worldStartupProof
  return evidence
}

async function collectStandaloneRuntimeEvidence(standaloneRoot) {
  const evidence = emptyRuntimeEvidence('standalone', standaloneRoot)
  if (!(await exists(standaloneRoot))) return evidence

  const reportPaths = {
    adapterCoreCoverage: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'runtime-adaptercore-module-coverage.json'),
    realModuleExecution: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'real-module-execution-smoke.json'),
    nativeLoaderAbi: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'native-loader-abi-v1-smoke.json'),
    runtimeUi: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'runtime-ui.json'),
    uiScreenStack: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'ui-screen-stack.json'),
    uiInputRouter: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'ui-input-router.json'),
    adapterCoreGameplayBridge: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'adaptercore-gameplay-bridge-parity-smoke.json'),
    registryAssetCoverage: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'registry-asset-coverage.json'),
    runtimeItem: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'runtime-item.json'),
    runtimeWorld: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'runtime-world.json'),
    fullWorldgen: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'full-worldgen-dimensions-structures.json'),
    playableVoxelSave: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'playable-voxel-save.json'),
    runtimeSave: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'runtime-save.json'),
    verticalSliceSaveLoad: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'vertical-slice-save-load.json'),
    verticalSliceNetworkSync: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'vertical-slice-network-sync.json'),
    agent5UiParity: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'agent5-ui-parity-smoke.json'),
    clientScreenCatalog: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'client-screen-catalog-smoke.json'),
    clientModsRuntimeContent: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'client-mods-runtime-content-smoke.json'),
    clientWorldInteraction: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'client-world-interaction-smoke.json'),
    clientHeldItemOverlay: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'client-held-item-overlay-smoke.json'),
    creativeTabVisibility: path.join(standaloneRoot, 'reports', 'echo', 'standalone', 'all-module-creative-tab-visibility-smoke.json'),
  }
  const reports = {}
  for (const [key, filePath] of Object.entries(reportPaths)) {
    reports[key] = await readJsonIfExists(filePath)
    evidence.reports[key] = reportSummary(standaloneRoot, filePath, reports[key])
  }

  if (reportStatusIs(reports.adapterCoreCoverage, 'PASS')) {
    for (const module of Array.isArray(reports.adapterCoreCoverage.modules) ? reports.adapterCoreCoverage.modules : []) {
      const moduleId = string(module.moduleId)
      if (moduleId) evidence.coveredModules.add(moduleId)
    }
  }
  if (reportStatusIs(reports.realModuleExecution, 'PASS')) {
    for (const moduleId of cleanList(reports.realModuleExecution.moduleIds)) evidence.executedModules.add(moduleId)
  }
  if (reportStatusIs(reports.adapterCoreCoverage, 'PASS') && reportStatusIs(reports.realModuleExecution, 'PASS')) {
    for (const module of Array.isArray(reports.adapterCoreCoverage.modules) ? reports.adapterCoreCoverage.modules : []) {
      const moduleId = string(module.moduleId)
      if (moduleId && string(module.status).toLowerCase() === 'active') {
        evidence.executedModules.add(moduleId)
      }
    }
  }

  evidence.artifactLoadProof = reportStatusIs(reports.nativeLoaderAbi, 'PASS')
  evidence.lifecycleProof = evidence.executedModules.size > 0
  evidence.contentHostProof = reportStatusIs(reports.adapterCoreCoverage, 'PASS')
    && (reportStatusIs(reports.adapterCoreGameplayBridge, 'PASS')
      || reportStatusIs(reports.clientModsRuntimeContent, 'PASS'))
  evidence.uiHostProof = reportStatusIs(reports.runtimeUi, 'PASS')
    && reportStatusIs(reports.uiScreenStack, 'PASS')
    && reportStatusIs(reports.uiInputRouter, 'PASS')
    && reportStatusIs(reports.agent5UiParity, 'PASS')
    && reportStatusIs(reports.clientScreenCatalog, 'PASS')
  evidence.actionHostProof = (evidence.uiHostProof && evidence.contentHostProof)
    || reportStatusIs(reports.clientWorldInteraction, 'PASS')
  evidence.blockItemHostProof = reportStatusIs(reports.registryAssetCoverage, 'PASS')
    && reportStatusIs(reports.runtimeItem, 'PASS')
    && reportStatusIs(reports.playableVoxelSave, 'PASS')
    && reportStatusIs(reports.clientHeldItemOverlay, 'PASS')
  collectCreativeTabEvidence(reports.creativeTabVisibility, evidence)
  evidence.creativeTabHostProof = evidence.creativeTabPlayableModules.size > 0
  evidence.worldgenHostProof = reportStatusIs(reports.runtimeWorld, 'PASS')
    && reportStatusIs(reports.fullWorldgen, 'PASS')
  evidence.saveNetworkProof = reportStatusIs(reports.runtimeSave, 'PASS')
    && reportStatusIs(reports.verticalSliceSaveLoad, 'PASS')
    && reportStatusIs(reports.verticalSliceNetworkSync, 'PASS')
  return evidence
}

function reportSummary(repoRoot, filePath, report) {
  return {
    path: normalizePath(path.relative(repoRoot, filePath)),
    found: !!report,
    status: string(report?.status ?? report?.result),
    schema: string(report?.schema),
    parseError: string(report?.parseError),
  }
}

function reportStatusIs(report, expected) {
  return !!report && !report.parseError && string(report.status ?? report.result) === expected
}

function numberValue(value) {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0
}

function collectRouteModules(value, modules) {
  if (Array.isArray(value)) {
    for (const item of value) collectRouteModules(item, modules)
    return
  }
  if (!value || typeof value !== 'object') return
  if (value.handled === true || value.status === 'MUTATED' || value.trustedMutation === true) {
    for (const key of ['routeModuleId', 'moduleId', 'ownerModuleId']) {
      const moduleId = string(value[key])
      if (isFirstPartyRuntimeId(moduleId)) modules.add(moduleId)
    }
    for (const handlerId of cleanList(value.ownerHandlerIds)) {
      const moduleId = handlerId.split(':')[0]
      if (isFirstPartyRuntimeId(moduleId)) modules.add(moduleId)
    }
  }
  for (const child of Object.values(value)) collectRouteModules(child, modules)
}

function isFirstPartyRuntimeId(value) {
  return value.startsWith('echo') || value.startsWith('signalos')
}

function runtimeModuleHasLoadProof(evidence, moduleId) {
  return evidence.loadedModules?.has(moduleId)
    || evidence.coveredModules?.has(moduleId)
    || evidence.executedModules?.has(moduleId)
}

async function runtimeRow({ repoRoot, release, module, runtime, packRefs, runtimeEvidence }) {
  const evidenceForRuntime = runtimeEvidence?.[runtime.id] ?? emptyRuntimeEvidence(runtime.id, '')
  const artifactStatus = await artifactStatusFor(repoRoot, release, module, runtime)
  const entrypointStatus = entrypointStatusFor(module, runtime, artifactStatus, evidenceForRuntime)
  const contentStatus = contentStatusFor(module, runtime, evidenceForRuntime)
  const uiSurfaceStatus = uiSurfaceStatusFor(module, runtime, evidenceForRuntime)
  const actionRouteStatus = actionRouteStatusFor(module, runtime, evidenceForRuntime)
  const blockItemStatus = blockItemStatusFor(module, runtime, evidenceForRuntime)
  const creativeTabStatus = creativeTabStatusFor(module, runtime, evidenceForRuntime)
  const creativeTabEvidence = evidenceForRuntime.creativeTabEvidenceByModule?.get(module.moduleId) ?? {}
  const missingCreativeTabEntries = missingCreativeTabEntriesFor(module, evidenceForRuntime)
  const missingCreativeSearchEntries = missingCreativeSearchEntriesFor(module, evidenceForRuntime)
  const worldgenStatus = worldgenStatusFor(module, runtime, evidenceForRuntime)
  const saveNetworkStatus = saveNetworkStatusFor(module, runtime, evidenceForRuntime)
  const statuses = {
    artifactStatus,
    entrypointStatus,
    contentStatus,
    uiSurfaceStatus,
    actionRouteStatus,
    blockItemStatus,
    creativeTabStatus,
    creativeTabEvidence,
    missingCreativeTabEntries,
    missingCreativeSearchEntries,
    worldgenStatus,
    saveNetworkStatus,
  }
  const featureEvidence = featureEvidenceFor(module, runtime, statuses, evidenceForRuntime)
  const evidence = evidenceFor(module, runtime, statuses, evidenceForRuntime)
  const blockers = blockersFor({
    module,
    runtime,
    artifactStatus,
    entrypointStatus,
    contentStatus,
    uiSurfaceStatus,
    actionRouteStatus,
    blockItemStatus,
    creativeTabStatus,
    creativeTabEvidence,
    missingCreativeTabEntries,
    missingCreativeSearchEntries,
    worldgenStatus,
    saveNetworkStatus,
  })
  const strictFullBlockers = strictFullBlockersFor({ module, runtime, featureEvidence, entrypointStatus, artifactStatus })
  const result = resultFor(blockers, statuses)

  return {
    moduleId: module.moduleId,
    name: module.name,
    version: module.version,
    kind: module.kind,
    role: module.role,
    runtime: runtime.id,
    ownerRepo: runtime.ownerRepo,
    packRefs,
    declaredDomains: module.declaredDomains,
    expectedFeatures: module.expectedFeatures,
    expectedCreativeTabs: module.expectedCreativeTabs,
    artifactStatus: artifactStatus.status,
    artifactEvidence: artifactStatus,
    entrypointStatus,
    contentStatus,
    uiSurfaceStatus,
    actionRouteStatus,
    blockItemStatus,
    creativeTabStatus,
    creativeTabEvidence,
    missingCreativeTabEntries,
    missingCreativeSearchEntries,
    worldgenStatus,
    saveNetworkStatus,
    featureEvidence,
    strictFullBlockers,
    neoForgeEvidence: runtime.id === 'neoforge' ? evidence : {},
    nativeEvidence: runtime.id === 'echo_native' ? evidence : {},
    standaloneEvidence: runtime.id === 'standalone' ? evidence : {},
    result,
    blockers,
    recommendedFix: recommendedFixFor({ module, runtime, blockers }),
  }
}

function collectCreativeTabEvidence(report, evidence) {
  if (!report || report.parseError) return
  addModuleIds(evidence.creativeTabRegistryModules, cleanList(report.registryBackedModuleIds))
  addModuleIds(evidence.creativeTabParentVisibleModules, cleanList(report.visibleParentModuleIds))
  addModuleIds(evidence.creativeTabSearchVisibleModules, cleanList(report.visibleSearchModuleIds))
  addModuleIds(evidence.creativeTabSelectableModules, cleanList(report.selectableModuleIds))
  addModuleIds(evidence.creativeTabPlayableModules, cleanList(report.playableModuleIds))
  for (const module of Array.isArray(report.modules) ? report.modules : []) {
    if (!module || typeof module !== 'object') continue
    const moduleId = string(module.moduleId)
    if (!moduleId) continue
    evidence.creativeTabEvidenceByModule.set(moduleId, module)
    if (module.registryBacked === true) evidence.creativeTabRegistryModules.add(moduleId)
    if (module.visibleParent === true) evidence.creativeTabParentVisibleModules.add(moduleId)
    if (module.visibleSearch === true) evidence.creativeTabSearchVisibleModules.add(moduleId)
    if (module.selectable === true) evidence.creativeTabSelectableModules.add(moduleId)
    if (module.playable === true) evidence.creativeTabPlayableModules.add(moduleId)
  }
}

function addModuleIds(target, values) {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) target.add(value.trim())
  }
}

async function artifactStatusFor(repoRoot, release, module, runtime) {
  const releaseModule = release.modules.get(module.moduleId)
  const expectedName = expectedArtifactName(module, runtime)
  const distPath = path.join(repoRoot, 'dist', 'echo-module-release', module.moduleId, expectedName)
  const buildLibs = path.join(repoRoot, module.directory, 'build', 'libs')
  const releaseArtifacts = artifactRecords(releaseModule)
  const matchingReleaseArtifact = releaseArtifacts.find((artifact) => {
    const filename = string(artifact.filename || artifact.assetName || artifact.name)
    const kind = string(artifact.kind || artifact.artifactFamily || artifact.family)
    return filename === expectedName || kind === runtime.artifactFamily || filename.endsWith(artifactSuffix(module, runtime))
  })
  if (matchingReleaseArtifact) {
    const buildMode = string(matchingReleaseArtifact.buildMode)
    return {
      status: buildMode === 'source-packaged' ? 'source-packaged' : 'verified',
      expectedName,
      source: 'dist/echo-module-release/echo-release.json',
      buildMode: buildMode || 'compiled-runtime',
      artifact: matchingReleaseArtifact,
    }
  }
  if (await exists(distPath)) {
    return {
      status: 'verified',
      expectedName,
      source: normalizePath(path.relative(repoRoot, distPath)),
      buildMode: 'compiled-runtime',
    }
  }
  if (runtime.id !== 'echo_native' && await exists(buildLibs)) {
    const files = await fs.readdir(buildLibs)
    const runtimeSuffix = artifactSuffix(module, runtime)
    const jar = files.find((file) => file === expectedName)
      ?? files.find((file) => file.endsWith(runtimeSuffix) && !file.includes('-sources') && !file.includes('-javadoc'))
      ?? files.find((file) => file.endsWith('.jar') && !file.includes('-sources') && !file.includes('-javadoc'))
    if (jar) {
      return {
        status: 'compiled',
        expectedName,
        source: normalizePath(path.join(module.directory, 'build', 'libs', jar)),
        buildMode: 'compiled-runtime',
      }
    }
  }
  return {
    status: 'missing',
    expectedName,
    source: '',
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

function expectedArtifactName(module, runtime) {
  if (runtime.id === 'echo_native') return `${module.moduleId}-${module.version}.echo-addon`
  return `${module.moduleId}-${module.version}-${runtime.artifactFamily}.jar`
}

function artifactSuffix(module, runtime) {
  if (runtime.id === 'echo_native') return '.echo-addon'
  return `-${runtime.artifactFamily}.jar`
}

function entrypointStatusFor(module, runtime, artifactStatus, runtimeEvidence) {
  const sourceExists = runtime.id === 'neoforge' ? module.entrypointSourceExists : module.nativeEntrypointSourceExists
  const entrypoint = runtime.id === 'neoforge' ? module.entrypoint : module.nativeEntrypoint
  if (!entrypoint) return 'missing'
  if (!sourceExists) return 'missing'
  if (runtime.id === 'neoforge' && runtimeEvidence.executedModules.has(module.moduleId)) return 'lifecycle-runs'
  if (runtime.id === 'echo_native' && runtimeEvidence.loadedModules.has(module.moduleId)) return 'lifecycle-runs'
  if (runtime.id === 'standalone' && runtimeEvidence.executedModules.has(module.moduleId)) return 'lifecycle-runs'
  if (runtime.id === 'standalone' && runtimeEvidence.coveredModules.has(module.moduleId) && artifactStatus.status === 'verified') return 'class-loads'
  if (artifactStatus.status === 'verified') return 'class-loads'
  return 'source-present'
}

function contentStatusFor(module, runtime, runtimeEvidence) {
  if (module.expectedFeatures.length === 0) return 'no content'
  if (runtime.id === 'echo_native' && runtimeEvidence.contentHostProof && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'host-mutated'
  }
  if (runtime.id === 'standalone' && runtimeEvidence.contentHostProof && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'playable'
  }
  if (runtime.id === 'neoforge' && runtimeEvidence.contentHostModules?.has(module.moduleId)) return 'host-mutated'
  if (module.sourceSignals.hasMutationRecord && runtime.id === 'neoforge') return 'host-mutated'
  if (module.sourceSignals.hasRegisterContent || module.sourceSignals.hasActivationRegistrar) return 'registered'
  if (module.provides.length > 0 || module.declaredDomains.length > 0 || module.resourceSignals.hasData) return 'metadata-only'
  return 'no content'
}

function uiSurfaceStatusFor(module, runtime, runtimeEvidence) {
  if (!expectsUi(module)) return 'none expected'
  if (runtime.id === 'echo_native'
    && runtimeEvidence.uiHostProof
    && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'visible/actionable'
  }
  if (runtime.id === 'standalone'
    && runtimeEvidence.uiHostProof
    && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'visible/actionable'
  }
  if (runtime.id === 'neoforge' && runtimeEvidence.uiVisibleModules?.has(module.moduleId)) {
    return 'visible/actionable'
  }
  if (runtime.id === 'neoforge' && (module.sourceSignals.hasScreenClass || module.sourceSignals.hasOverlayClass)) {
    return 'registered-headless'
  }
  if (module.sourceSignals.hasNativeSurfaceDeclaration || module.sourceSignals.hasNativeClientRouteRegistrar) {
    return 'registered-headless'
  }
  if (module.resourceSignals.hasUiAssets || module.sourceSignals.hasScreenClass || module.sourceSignals.hasOverlayClass) {
    return 'declared-only'
  }
  return 'declared-only'
}

function actionRouteStatusFor(module, runtime, runtimeEvidence) {
  if (!expectsActions(module)) return 'none expected'
  if (runtime.id === 'echo_native'
    && runtimeEvidence.actionHostProof
    && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'mutates gameplay'
  }
  if (runtime.id === 'standalone'
    && runtimeEvidence.actionHostProof
    && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'mutates gameplay'
  }
  if (runtime.id === 'neoforge' && runtimeEvidence.actionMutationModules?.has(module.moduleId)) {
    return 'mutates gameplay'
  }
  if (module.sourceSignals.hasNativeClientRouteRegistrar && runtime.id !== 'neoforge') return 'registered'
  if (module.sourceSignals.hasNativeSurfaceDeclaration || module.sourceSignals.hasMutationRecord) return 'registered'
  return 'no route'
}

function blockItemStatusFor(module, runtime, runtimeEvidence) {
  if (!expectsBlockItems(module)) return 'none expected'
  if (runtime.id === 'echo_native'
    && runtimeEvidence.blockItemHostProof
    && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'place/use/break verified'
  }
  if (runtime.id === 'standalone'
    && runtimeEvidence.blockItemHostProof
    && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'place/use/break verified'
  }
  if (runtime.id === 'neoforge' && runtimeEvidence.blockItemGameplayModules?.has(module.moduleId)) {
    return 'place/use/break verified'
  }
  if (module.resourceSignals.hasBlockstates || module.resourceSignals.hasModels || module.sourceSignals.hasDeferredRegister) {
    return 'registry-backed'
  }
  if (module.sourceSignals.hasBlockClass || module.sourceSignals.hasItemClass || module.provides.length > 0) {
    return 'declared-only'
  }
  return 'declared-only'
}

function creativeTabStatusFor(module, runtime, runtimeEvidence) {
  if (!expectsCreativeTabs(module)) return 'none expected'
  if (runtimeEvidence.creativeTabPlayableModules?.has(module.moduleId)) return 'playable'
  if (runtimeEvidence.creativeTabSelectableModules?.has(module.moduleId)) return 'selectable'
  if (runtimeEvidence.creativeTabSearchVisibleModules?.has(module.moduleId)) return 'visible-search'
  if (runtimeEvidence.creativeTabParentVisibleModules?.has(module.moduleId)) return 'visible-parent'
  if (runtimeEvidence.creativeTabRegistryModules?.has(module.moduleId)) return 'registry-backed'
  if (module.sourceSignals.hasCreativeTabClass || module.sourceSignals.hasCreativeTabRegistration) return 'declared-only'
  return 'declared-only'
}

function missingCreativeTabEntriesFor(module, runtimeEvidence) {
  if (!expectsCreativeTabs(module)) return []
  const evidence = runtimeEvidence.creativeTabEvidenceByModule?.get(module.moduleId)
  if (Array.isArray(evidence?.missingCreativeTabEntries)) return cleanList(evidence.missingCreativeTabEntries)
  if (runtimeEvidence.creativeTabPlayableModules?.has(module.moduleId)
    || runtimeEvidence.creativeTabSelectableModules?.has(module.moduleId)
    || runtimeEvidence.creativeTabParentVisibleModules?.has(module.moduleId)) {
    return []
  }
  return expectedCreativeEntriesFor(module)
}

function missingCreativeSearchEntriesFor(module, runtimeEvidence) {
  if (!expectsCreativeTabs(module)) return []
  const evidence = runtimeEvidence.creativeTabEvidenceByModule?.get(module.moduleId)
  if (Array.isArray(evidence?.missingCreativeSearchEntries)) return cleanList(evidence.missingCreativeSearchEntries)
  if (runtimeEvidence.creativeTabPlayableModules?.has(module.moduleId)
    || runtimeEvidence.creativeTabSelectableModules?.has(module.moduleId)
    || runtimeEvidence.creativeTabSearchVisibleModules?.has(module.moduleId)) {
    return []
  }
  return module.expectedCreativeTabs
    .filter((tab) => tab.searchExpected !== false)
    .flatMap((tab) => cleanList(tab.expectedEntries))
    .filter(uniqueFilter)
}

function expectedCreativeEntriesFor(module) {
  return cleanList(module.expectedCreativeEntries).length > 0
    ? cleanList(module.expectedCreativeEntries)
    : module.expectedCreativeTabs.flatMap((tab) => cleanList(tab.expectedEntries)).filter(uniqueFilter)
}

function worldgenStatusFor(module, runtime, runtimeEvidence) {
  if (!module.expectedFeatures.includes('worldgen')) return 'none expected'
  if (runtime.id === 'echo_native'
    && runtimeEvidence.worldgenHostProof
    && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'generated in runtime'
  }
  if (runtime.id === 'standalone'
    && runtimeEvidence.worldgenHostProof
    && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'generated in runtime'
  }
  if (runtime.id === 'neoforge' && runtimeEvidence.worldgenModules?.has(module.moduleId)) {
    return 'generated in runtime'
  }
  if (module.resourceSignals.hasWorldgen || module.resourceSignals.hasStructures) return 'data present'
  if (module.sourceSignals.hasWorldClass) return 'datapack/resource registered'
  return 'none expected'
}

function saveNetworkStatusFor(module, runtime, runtimeEvidence) {
  if (!expectsSaveNetwork(module)) return 'none expected'
  if (runtime.id === 'echo_native'
    && runtimeEvidence.saveNetworkProof
    && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'save/reload or sync verified'
  }
  if (runtime.id === 'standalone'
    && runtimeEvidence.saveNetworkProof
    && runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)) {
    return 'save/reload or sync verified'
  }
  if (runtime.id === 'neoforge'
    && (runtimeEvidence.saveReloadModules?.has(module.moduleId) || runtimeEvidence.networkSyncModules?.has(module.moduleId))) {
    return 'save/reload or sync verified'
  }
  if (module.sourceSignals.hasMutationRecord && module.sourceSignals.hasRegisterServices) return 'host-bound'
  if (module.sourceSignals.hasRegisterServices || module.declaredDomains.some((domain) => ['networking', 'data', 'saves'].includes(domain))) {
    return 'service declared'
  }
  return 'service declared'
}

function featureEvidenceLevel(feature, runtime, statuses, runtimeEvidence, module) {
  const hasLoadProof = runtimeModuleHasLoadProof(runtimeEvidence, module.moduleId)
  const uiProof = (runtimeEvidence.uiHostProof && hasLoadProof)
    || runtimeEvidence.uiVisibleModules?.has(module.moduleId)
  const actionProof = (runtimeEvidence.actionHostProof && hasLoadProof)
    || runtimeEvidence.actionMutationModules?.has(module.moduleId)
  const blockItemProof = (runtimeEvidence.blockItemHostProof && hasLoadProof)
    || runtimeEvidence.blockItemGameplayModules?.has(module.moduleId)
  const creativePlayableProof = runtimeEvidence.creativeTabPlayableModules?.has(module.moduleId)
  const creativeSelectableProof = runtimeEvidence.creativeTabSelectableModules?.has(module.moduleId)
  const creativeVisibleProof = runtimeEvidence.creativeTabSearchVisibleModules?.has(module.moduleId)
    || runtimeEvidence.creativeTabParentVisibleModules?.has(module.moduleId)
  const creativeRegistryProof = runtimeEvidence.creativeTabRegistryModules?.has(module.moduleId)
  const worldgenProof = (runtimeEvidence.worldgenHostProof && hasLoadProof)
    || runtimeEvidence.worldgenModules?.has(module.moduleId)
  const saveNetworkProof = (runtimeEvidence.saveNetworkProof && hasLoadProof)
    || runtimeEvidence.saveReloadModules?.has(module.moduleId)
    || runtimeEvidence.networkSyncModules?.has(module.moduleId)

  if (['gui', 'hud', 'screen', 'inventory_overlay', 'terminal', 'index', 'holomap', 'lens', 'audio'].includes(feature)) {
    if (uiProof) return 'visible-actionable'
    if (statuses.uiSurfaceStatus === 'visible/actionable') return 'visible-actionable'
    if (statuses.uiSurfaceStatus === 'registered-headless') return 'host-registered'
    if (module.sourceSignals.hasScreenClass || module.sourceSignals.hasOverlayClass || module.resourceSignals.hasUiAssets) return 'static-source'
    return 'static-source'
  }

  if (['blocks', 'items', 'block_actions', 'entities', 'machines'].includes(feature)) {
    if (blockItemProof) return 'gameplay-mutated'
    if (actionProof && ['block_actions', 'machines'].includes(feature)) return 'gameplay-mutated'
    if (statuses.blockItemStatus === 'place/use/break verified') return 'gameplay-mutated'
    if (statuses.blockItemStatus === 'registry-backed') return 'host-registered'
    if (module.sourceSignals.hasBlockClass || module.sourceSignals.hasItemClass || module.sourceSignals.hasDeferredRegister) return 'static-source'
    return 'static-source'
  }

  if (feature === 'creative_tab') {
    if (creativePlayableProof) return 'gameplay-mutated'
    if (creativeSelectableProof || creativeVisibleProof) return 'visible-actionable'
    if (creativeRegistryProof) return 'host-registered'
    if (module.sourceSignals.hasCreativeTabClass || module.sourceSignals.hasCreativeTabRegistration) return 'static-source'
    return 'static-source'
  }

  if (feature === 'worldgen') {
    if (worldgenProof) return 'host-registered'
    if (statuses.worldgenStatus === 'generated in runtime') return 'host-registered'
    if (module.resourceSignals.hasWorldgen || module.resourceSignals.hasStructures) return 'static-source'
    return 'static-source'
  }

  if (['recipes', 'loot'].includes(feature)) {
    if ((runtimeEvidence.contentHostProof && hasLoadProof) || runtimeEvidence.contentHostModules?.has(module.moduleId)) return 'host-registered'
    if (module.resourceSignals.hasRecipes || module.resourceSignals.hasLoot) return 'static-source'
    return 'static-source'
  }

  if (['networking', 'save_data', 'missions'].includes(feature)) {
    if (saveNetworkProof) return feature === 'networking' ? 'network-synced' : 'save-reloaded'
    if (module.sourceSignals.hasRegisterServices || module.declaredDomains.includes('networking') || module.declaredDomains.includes('data')) return 'host-registered'
    return 'static-source'
  }

  return 'static-source'
}

function featureEvidenceFor(module, runtime, statuses, runtimeEvidence) {
  return module.expectedFeatures.map((feature) => {
    const requiredLevel = FEATURE_EVIDENCE_REQUIREMENTS[feature] || 'host-registered'
    const actualLevel = featureEvidenceLevel(feature, runtime, statuses, runtimeEvidence, module)
    const requiredIndex = EVIDENCE_LEVELS.indexOf(requiredLevel)
    const actualIndex = EVIDENCE_LEVELS.indexOf(actualLevel)
    const satisfied = actualIndex >= requiredIndex
    return {
      feature,
      requiredLevel,
      actualLevel,
      satisfied,
    }
  })
}

function evidenceLevelRank(level) {
  return EVIDENCE_LEVELS.indexOf(level)
}

function evidenceFor(module, runtime, statuses, runtimeEvidence) {
  return {
    descriptorPath: module.descriptorPath,
    entrypoint: runtime.id === 'neoforge' ? module.entrypoint : module.nativeEntrypoint,
    entrypointSourceExists: runtime.id === 'neoforge' ? module.entrypointSourceExists : module.nativeEntrypointSourceExists,
    nativeEntrypointSourceExists: module.nativeEntrypointSourceExists,
    artifactStatus: statuses.artifactStatus.status,
    expectedCreativeTabs: module.expectedCreativeTabs,
    creativeTabStatus: statuses.creativeTabStatus,
    sourceSignals: module.sourceSignals,
    resourceSignals: module.resourceSignals,
    runtimeProof: publicRuntimeProofForModule(runtimeEvidence, module.moduleId),
    note: evidenceNote(runtime, statuses),
  }
}

function publicRuntimeProofForModule(runtimeEvidence, moduleId) {
  return {
    reports: runtimeEvidence.reports ?? {},
    loadedInRuntime: runtimeModuleHasLoadProof(runtimeEvidence, moduleId),
    nativeRouteModuleMatched: runtimeEvidence.uiRouteModules?.has(moduleId) ?? false,
    moduleProofs: {
      contentHost: runtimeEvidence.contentHostModules?.has(moduleId) ?? false,
      uiVisible: runtimeEvidence.uiVisibleModules?.has(moduleId) ?? false,
      actionMutation: runtimeEvidence.actionMutationModules?.has(moduleId) ?? false,
      blockItemGameplay: runtimeEvidence.blockItemGameplayModules?.has(moduleId) ?? false,
      creativeTabRegistry: runtimeEvidence.creativeTabRegistryModules?.has(moduleId) ?? false,
      creativeTabParentVisible: runtimeEvidence.creativeTabParentVisibleModules?.has(moduleId) ?? false,
      creativeTabSearchVisible: runtimeEvidence.creativeTabSearchVisibleModules?.has(moduleId) ?? false,
      creativeTabSelectable: runtimeEvidence.creativeTabSelectableModules?.has(moduleId) ?? false,
      creativeTabPlayable: runtimeEvidence.creativeTabPlayableModules?.has(moduleId) ?? false,
      worldgen: runtimeEvidence.worldgenModules?.has(moduleId) ?? false,
      saveReload: runtimeEvidence.saveReloadModules?.has(moduleId) ?? false,
      networkSync: runtimeEvidence.networkSyncModules?.has(moduleId) ?? false,
    },
    routeDispatchCount: runtimeEvidence.routeDispatchCount ?? 0,
    hostProofs: {
      artifactLoad: runtimeEvidence.artifactLoadProof,
      lifecycle: runtimeEvidence.lifecycleProof,
      content: runtimeEvidence.contentHostProof,
      ui: runtimeEvidence.uiHostProof,
      action: runtimeEvidence.actionHostProof,
      blockItem: runtimeEvidence.blockItemHostProof,
      creativeTab: runtimeEvidence.creativeTabHostProof,
      worldgen: runtimeEvidence.worldgenHostProof,
      saveNetwork: runtimeEvidence.saveNetworkProof,
    },
    creativeTabEvidence: runtimeEvidence.creativeTabEvidenceByModule?.get(moduleId) ?? {},
  }
}

function evidenceNote(runtime, statuses) {
  if (statuses.artifactStatus.status === 'missing') return 'No compiled runtime artifact was found in the local release/build outputs.'
  if (runtime.id === 'neoforge' && statuses.entrypointStatus === 'lifecycle-runs') {
    return 'Generated NeoForge evidence links compiled artifacts, entrypoint source, resources, and GameTest/source contracts for this module.'
  }
  if (runtime.id === 'neoforge') return 'Static source/resource evidence found; generated NeoForge runtime evidence is still required.'
  if (statuses.entrypointStatus === 'lifecycle-runs') return 'Runtime evidence proves lifecycle execution; feature statuses indicate whether host-level visible/gameplay proof is also available.'
  return 'Static bridge evidence found; trusted host mutation or visible runtime proof is still required.'
}

function blockersFor({
  module,
  runtime,
  artifactStatus,
  entrypointStatus,
  contentStatus,
  uiSurfaceStatus,
  actionRouteStatus,
  blockItemStatus,
  creativeTabStatus,
  worldgenStatus,
  saveNetworkStatus,
}) {
  const blockers = []
  if (artifactStatus.status === 'missing') blockers.push(`missing ${runtime.artifactFamily} runtime artifact ${artifactStatus.expectedName}`)
  if (artifactStatus.status === 'source-packaged') blockers.push('runtime artifact is source-packaged, not compiled player-ready output')
  if (entrypointStatus === 'missing') blockers.push(`missing ${runtime.id === 'neoforge' ? 'main' : 'native'} entrypoint source or descriptor entrypoint`)
  if (module.expectedFeatures.length > 0 && contentStatus === 'metadata-only') {
    blockers.push('expected feature content is metadata-only and lacks host registration proof')
  }
  if (expectsUi(module) && ['declared-only', 'registered-headless'].includes(uiSurfaceStatus)) {
    blockers.push(`expected UI/HUD/screen surface is ${uiSurfaceStatus}; visible/actionable proof is required`)
  }
  if (expectsActions(module) && ['no route', 'registered'].includes(actionRouteStatus)) {
    blockers.push(`expected action route is ${actionRouteStatus}; dispatch and gameplay mutation proof is required`)
  }
  if (expectsBlockItems(module) && ['declared-only'].includes(blockItemStatus)) {
    blockers.push('expected block/item behavior is declared without place/use/break evidence')
  }
  if (expectsCreativeTabs(module) && ['declared-only', 'registry-backed', 'visible-parent', 'visible-search', 'selectable'].includes(creativeTabStatus)) {
    blockers.push(`expected creative tab content is ${creativeTabStatus}; visible/search/select/play proof is required`)
  }
  if (module.expectedFeatures.includes('worldgen') && ['none expected'].includes(worldgenStatus)) {
    blockers.push('expected worldgen/resource behavior lacks data or generated-runtime evidence')
  }
  if (expectsSaveNetwork(module) && ['service declared'].includes(saveNetworkStatus)) {
    blockers.push('stateful save/network behavior has service declarations but no save/reload or sync proof')
  }
  return blockers
}

function resultFor(blockers, statuses) {
  if (blockers.some((blocker) => blocker.startsWith('missing') || blocker.includes('source-packaged'))) return 'fail'
  if (statuses.entrypointStatus === 'missing') return 'fail'
  if (blockers.length > 0) return 'partial'
  if (statuses.artifactStatus.status === 'verified' && ['class-loads', 'lifecycle-runs'].includes(statuses.entrypointStatus)) return 'pass'
  return 'partial'
}

function strictFullBlockersFor({ module, runtime, featureEvidence, entrypointStatus, artifactStatus }) {
  const blockers = []
  if (artifactStatus.status === 'missing') {
    blockers.push(`missing ${runtime.artifactFamily} runtime artifact`)
  }
  if (entrypointStatus === 'missing') {
    blockers.push('missing entrypoint source or descriptor entrypoint')
  }
  const requiresLifecycle = featureEvidence.some((feature) =>
    ['visible-actionable', 'gameplay-mutated', 'save-reloaded', 'network-synced'].includes(feature.requiredLevel))
  if (requiresLifecycle && entrypointStatus !== 'lifecycle-runs') {
    blockers.push(`entrypoint evidence is ${entrypointStatus}; lifecycle-ran is required for player-facing features`)
  }
  for (const feature of featureEvidence) {
    if (!feature.satisfied) {
      blockers.push(`feature ${feature.feature} requires ${feature.requiredLevel} evidence but only has ${feature.actualLevel}`)
    }
  }
  return blockers
}

function strictFullResultFor(strictFullBlockers) {
  if (strictFullBlockers.length === 0) return 'pass'
  const hasMissing = strictFullBlockers.some((blocker) => blocker.startsWith('missing'))
  return hasMissing ? 'fail' : 'partial'
}

function recommendedFixFor({ module, runtime, blockers }) {
  if (blockers.length === 0) return 'No immediate fix; keep this row covered by runtime parity gates.'
  if (blockers.some((blocker) => blocker.includes('runtime artifact'))) {
    return `ECHO-Modules: build and publish ${runtime.artifactFamily} compiled runtime artifact for ${module.moduleId}.`
  }
  if (blockers.some((blocker) => blocker.includes('entrypoint'))) {
    return `ECHO-Modules: align ${module.moduleId} descriptor entrypoint with source and compiled runtime classpath.`
  }
  if (blockers.some((blocker) => blocker.includes('UI/HUD/screen'))) {
    return `${runtime.ownerRepo}: connect ${module.moduleId} surface registrations to visible/actionable runtime UI proof.`
  }
  if (blockers.some((blocker) => blocker.includes('action route'))) {
    return `${runtime.ownerRepo}: route ${module.moduleId} actions through host dispatch and trusted gameplay mutation evidence.`
  }
  if (blockers.some((blocker) => blocker.includes('block/item'))) {
    return `${runtime.ownerRepo}: prove ${module.moduleId} block/item place, use, break, and save behavior through runtime host tests.`
  }
  if (blockers.some((blocker) => blocker.includes('creative tab'))) {
    return `${runtime.ownerRepo}: prove ${module.moduleId} creative tab entries are registry-backed, visible in parent/search, selectable, and playable.`
  }
  return `${runtime.ownerRepo}: add runtime proof for ${module.moduleId} feature buckets: ${module.expectedFeatures.join(', ')}.`
}

function expectsActions(module) {
  return module.expectedFeatures.some((feature) =>
    ['block_actions', 'terminal', 'index', 'holomap', 'lens', 'machines', 'hud', 'screen'].includes(feature))
}

function expectsBlockItems(module) {
  return module.expectedFeatures.some((feature) => ['blocks', 'items', 'machines'].includes(feature))
}

function expectsCreativeTabs(module) {
  return module.expectedFeatures.includes('creative_tab')
}

function expectsSaveNetwork(module) {
  return module.expectedFeatures.some((feature) =>
    ['networking', 'save_data', 'missions', 'machines', 'worldgen', 'entities'].includes(feature))
}

function buildBacklog({ rows, modules, docsIndex, preferredPacks }) {
  const issues = []
  const addIssue = (priority, ownerRepo, subsystem, title, summary, data = {}) => {
    issues.push({
      id: `RPA-${String(issues.length + 1).padStart(3, '0')}`,
      priority,
      ownerRepo,
      subsystem,
      title,
      summary,
      modules: cleanList(data.modules),
      runtimes: cleanList(data.runtimes),
      packRepos: cleanList(data.packRepos),
      recommendedFix: string(data.recommendedFix),
    })
  }

  const mainEntrypointMissing = modules.filter((module) => module.entrypoint && !module.entrypointSourceExists)
  if (mainEntrypointMissing.length > 0) {
    addIssue(
      'P0',
      'ECHO-Modules',
      'module entrypoints',
      'Fix descriptor entrypoint source mismatches',
      'At least one descriptor points at a main entrypoint class that was not found under src/main/java.',
      {
        modules: mainEntrypointMissing.map((module) => module.moduleId),
        recommendedFix: 'Align descriptor entrypoint values with the real runtime class or add the missing source class.',
      },
    )
  }

  for (const runtime of RUNTIMES) {
    const missingArtifactModules = rows
      .filter((row) => row.runtime === runtime.id && row.artifactStatus === 'missing')
      .map((row) => row.moduleId)
      .sort()
    if (missingArtifactModules.length > 0) {
      addIssue(
        'P0',
        'ECHO-Modules',
        'release artifacts',
        `Generate compiled ${runtime.label} artifacts for all modules`,
        `${missingArtifactModules.length} module(s) lack local compiled ${runtime.artifactFamily} output.`,
        {
          modules: missingArtifactModules,
          runtimes: [runtime.id],
          recommendedFix: 'Run the full module build/release generator and replace source-packaged or missing artifacts before player-ready promotion.',
        },
      )
    }
  }

  const packGaps = preferredPacks.filter((manifest) => manifest.missingBaselineModules.length > 0)
  for (const manifest of packGaps) {
    addIssue(
      'P0',
      manifest.repo,
      'pack moduleRequirements',
      `Add missing core parity modules to ${manifest.repo}`,
      `${manifest.repo} is missing ${manifest.missingBaselineModules.length} baseline module(s) needed by the current NeoForge-equivalent acceptance matrix.`,
      {
        modules: manifest.missingBaselineModules,
        packRepos: [manifest.repo],
        recommendedFix: `Update ${manifest.manifestPath} and docs/module-requirements.md so the ${manifest.lane} lane resolves the required surface/content modules.`,
      },
    )
  }

  for (const runtime of RUNTIMES.filter((item) => item.id !== 'neoforge')) {
    const uiGaps = rows
      .filter((row) => row.runtime === runtime.id && expectsUi(row) && row.uiSurfaceStatus !== 'visible/actionable')
      .map((row) => row.moduleId)
      .sort()
    if (uiGaps.length > 0) {
      addIssue(
        'P0',
        runtime.ownerRepo,
        'visible UI runtime bridge',
        `${runtime.label} surfaces need visible/actionable proof`,
        `${uiGaps.length} UI/HUD/screen module row(s) only prove declarations or headless registration.`,
        {
          modules: unique(uiGaps),
          runtimes: [runtime.id],
          recommendedFix: 'Promote module surface declarations into live host routes and add smoke evidence for HUD, screens, overlays, input, and dispatch.',
        },
      )
    }
  }

  const blockWorldGaps = rows
    .filter((row) =>
      row.runtime !== 'neoforge'
      && ['blocks', 'items', 'worldgen', 'block_actions'].some((feature) => row.expectedFeatures.includes(feature))
      && (row.blockItemStatus === 'declared-only'
        || (row.expectedFeatures.includes('worldgen') && row.worldgenStatus === 'none expected')
        || (expectsActions(row) && row.actionRouteStatus === 'no route')))
    .map((row) => row.moduleId)
  if (blockWorldGaps.length > 0) {
    addIssue(
      'P0',
      'ECHO-Native-Platform / ECHO-Standalone-Runtime',
      'content and gameplay host proof',
      'Prove block, item, action, and worldgen behavior through runtime hosts',
      `${unique(blockWorldGaps).length} module(s) have expected content/gameplay buckets without place/use/break/worldgen/action proof.`,
      {
        modules: unique(blockWorldGaps),
        runtimes: ['echo_native', 'standalone'],
        recommendedFix: 'Add host-backed smokes that place/use/break module content, generate expected data/world features, and record trusted mutations.',
      },
    )
  }

  const creativeTabGaps = rows
    .filter((row) =>
      row.runtime !== 'neoforge'
      && expectsCreativeTabs(row)
      && row.creativeTabStatus !== 'playable')
    .map((row) => row.moduleId)
  if (creativeTabGaps.length > 0) {
    addIssue(
      'P0',
      'ECHO-Native-Platform / ECHO-Standalone-Runtime',
      'creative inventory parity',
      'Prove all module creative tabs are visible, searchable, selectable, and playable',
      `${unique(creativeTabGaps).length} module(s) expect creative inventory content without full live creative-tab play proof.`,
      {
        modules: unique(creativeTabGaps),
        runtimes: ['echo_native', 'standalone'],
        recommendedFix: 'Generalize the creative tab bridge beyond Ashfall/fixtures and require per-module parent/search/select/play evidence.',
      },
    )
  }

  if (docsIndex.missingModuleIds.length > 0 || docsIndex.missingDirectories.length > 0 || docsIndex.extraIndexEntries.length > 0) {
    addIssue(
      'P1',
      'ECHO-Modules',
      'docs index',
      'Regenerate module docs index from the full descriptor inventory',
      `Docs index drift detected: ${docsIndex.missingModuleIds.length} missing id(s), ${docsIndex.missingDirectories.length} missing directorie(s), ${docsIndex.extraIndexEntries.length} extra entrie(s).`,
      {
        modules: docsIndex.missingModuleIds,
        recommendedFix: 'Update the docs index generator/source data so every descriptor appears exactly once.',
      },
    )
  }

  const statefulGaps = rows
    .filter((row) => row.runtime !== 'neoforge' && row.saveNetworkStatus === 'service declared')
    .map((row) => row.moduleId)
  if (statefulGaps.length > 0) {
    addIssue(
      'P1',
      'ECHO-Native-Platform / ECHO-Standalone-Runtime',
      'save and network parity',
      'Add save/reload and sync proof for stateful modules',
      `${unique(statefulGaps).length} module(s) declare stateful/network behavior without save/reload or sync evidence.`,
      {
        modules: unique(statefulGaps),
        runtimes: ['echo_native', 'standalone'],
        recommendedFix: 'Extend runtime smokes to create state, save, reload, and verify network/sync receipts for each module domain.',
      },
    )
  }

  addIssue(
    'P2',
    'ECHO-Modules',
    'audit polish',
    'Promote runtime parity audit into release workflow documentation',
    'The generator is intentionally separate from release mutation until the first backlog is triaged.',
    {
      recommendedFix: 'After the P0/P1 items are understood, decide whether --strict should become a release workflow gate.',
    },
  )

  return issues
}

function expectsUi(rowOrModule) {
  return rowOrModule.expectedFeatures.some((feature) =>
    ['gui', 'hud', 'screen', 'inventory_overlay', 'terminal', 'index', 'holomap', 'lens'].includes(feature))
}

function markdownReport(report) {
  const lines = []
  lines.push('# ECHO Module Runtime Parity Audit')
  lines.push('')
  lines.push(`Generated: ${report.generatedAt}`)
  lines.push('')
  lines.push('## Summary')
  lines.push('')
  lines.push(`- Modules audited: ${report.summary.moduleCount}`)
  lines.push(`- Runtime rows: ${report.summary.runtimeRowCount}`)
  lines.push(`- Passing rows: ${report.summary.resultCounts.pass}`)
  lines.push(`- Partial rows: ${report.summary.resultCounts.partial}`)
  lines.push(`- Failing rows: ${report.summary.resultCounts.fail}`)
  lines.push(`- Preferred pack manifests: ${report.summary.preferredPackManifestCount}`)
  lines.push(`- Backlog items: ${report.backlog.length}`)
  lines.push(`- Strict-full would fail: ${report.strictFullWouldFail ? 'YES' : 'no'}`)
  lines.push('')
  lines.push('## Strict-Full Summary')
  lines.push('')
  lines.push(`- Strict-full passing rows: ${report.strictFullSummary.resultCounts.pass}`)
  lines.push(`- Strict-full partial rows: ${report.strictFullSummary.resultCounts.partial}`)
  lines.push(`- Strict-full failing rows: ${report.strictFullSummary.resultCounts.fail}`)
  lines.push('')
  if (report.adapterCoreStrictPortAudit) {
    lines.push('## AdapterCore Strict Port')
    lines.push('')
    lines.push(`- Passing modules: ${report.adapterCoreStrictPortAudit.summary.resultCounts.pass}`)
    lines.push(`- Failing modules: ${report.adapterCoreStrictPortAudit.summary.resultCounts.fail}`)
    lines.push(`- Report: \`${report.adapterCoreStrictPortAudit.reportPath}\``)
    lines.push('')
  }
  lines.push('| Runtime | Pass | Partial | Fail |')
  lines.push('| --- | ---: | ---: | ---: |')
  for (const runtime of RUNTIMES) {
    const counts = report.strictFullSummary.resultCountsByRuntime[runtime.id] ?? {}
    lines.push(`| ${runtime.label} | ${counts.pass ?? 0} | ${counts.partial ?? 0} | ${counts.fail ?? 0} |`)
  }
  lines.push('')
  lines.push('## Feature Bucket Coverage')
  lines.push('')
  lines.push('| Bucket | Satisfied | Total |')
  lines.push('| --- | ---: | ---: |')
  for (const bucket of report.strictFullSummary.featureBucketSummary) {
    lines.push(`| ${bucket.feature} | ${bucket.satisfied} | ${bucket.total} |`)
  }
  lines.push('')
  lines.push('## Seed Findings')
  lines.push('')
  for (const finding of report.seedFindings) lines.push(`- ${finding}`)
  lines.push('')
  lines.push('## Runtime Result Counts')
  lines.push('')
  lines.push('| Runtime | Pass | Partial | Fail |')
  lines.push('| --- | ---: | ---: | ---: |')
  for (const runtime of RUNTIMES) {
    const counts = report.summary.resultCountsByRuntime[runtime.id] ?? {}
    lines.push(`| ${runtime.label} | ${counts.pass ?? 0} | ${counts.partial ?? 0} | ${counts.fail ?? 0} |`)
  }
  lines.push('')
  lines.push('## Pack Baseline Gaps')
  lines.push('')
  lines.push('| Repo | Family | Modules | Missing visible surfaces | Missing content baseline |')
  lines.push('| --- | --- | ---: | --- | --- |')
  for (const manifest of report.packAudit.preferredManifests) {
    lines.push(`| ${manifest.repo} | ${manifest.family || '(unknown)'} | ${manifest.moduleCount} | ${inlineList(manifest.missingVisibleCoreSurfaceModules)} | ${inlineList(manifest.missingContentBaselineModules)} |`)
  }
  lines.push('')
  lines.push('## Docs Index Drift')
  lines.push('')
  lines.push(`- Missing module ids: ${report.docsIndex.missingModuleIds.length}`)
  lines.push(`- Missing directories: ${report.docsIndex.missingDirectories.length}`)
  lines.push(`- Extra index entries: ${report.docsIndex.extraIndexEntries.length}`)
  lines.push('')
  lines.push('## Top Backlog Items')
  lines.push('')
  lines.push('| Priority | Owner | Title | Modules |')
  lines.push('| --- | --- | --- | ---: |')
  for (const item of report.backlog) {
    lines.push(`| ${item.priority} | ${item.ownerRepo} | ${item.title} | ${item.modules.length} |`)
  }
  lines.push('')
  lines.push('## Module Runtime Matrix')
  lines.push('')
  lines.push('| Module | Runtime | Result | Artifact | Entrypoint | UI | Actions | Block/Item | Creative Tab | Worldgen | Save/Network | Blockers |')
  lines.push('| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |')
  for (const row of report.rows) {
    lines.push(`| ${row.moduleId} | ${row.runtime} | ${row.result} | ${row.artifactStatus} | ${row.entrypointStatus} | ${row.uiSurfaceStatus} | ${row.actionRouteStatus} | ${row.blockItemStatus} | ${row.creativeTabStatus} | ${row.worldgenStatus} | ${row.saveNetworkStatus} | ${escapeCell(row.blockers.join('; '))} |`)
  }
  lines.push('')
  lines.push('## Strict-Full Feature Gaps by Module')
  lines.push('')
  lines.push('| Module | Runtime | Result | Missing Features |')
  lines.push('| --- | --- | --- | --- |')
  for (const row of report.strictFullSummary.rows) {
    if (row.result === 'pass') continue
    const missing = row.featureEvidence
      .filter((feature) => !feature.satisfied)
      .map((feature) => `${feature.feature}(${feature.requiredLevel}→${feature.actualLevel})`)
    lines.push(`| ${row.moduleId} | ${row.runtime} | ${row.result} | ${escapeCell(missing.join(', '))} |`)
  }
  lines.push('')
  return `${lines.join('\n')}\n`
}

function markdownBacklog(report) {
  const lines = []
  lines.push('# ECHO Runtime Parity Fix Backlog')
  lines.push('')
  lines.push(`Generated: ${report.generatedAt}`)
  lines.push('')
  for (const priority of ['P0', 'P1', 'P2']) {
    lines.push(`## ${priority}`)
    lines.push('')
    const items = report.backlog.filter((item) => item.priority === priority)
    if (items.length === 0) {
      lines.push('No items.')
      lines.push('')
      continue
    }
    for (const item of items) {
      lines.push(`### ${item.id} - ${item.title}`)
      lines.push('')
      lines.push(`- Owner: ${item.ownerRepo}`)
      lines.push(`- Subsystem: ${item.subsystem}`)
      lines.push(`- Summary: ${item.summary}`)
      if (item.runtimes.length > 0) lines.push(`- Runtimes: ${inlineList(item.runtimes)}`)
      if (item.packRepos.length > 0) lines.push(`- Pack repos: ${inlineList(item.packRepos)}`)
      if (item.modules.length > 0) lines.push(`- Modules (${item.modules.length}): ${inlineList(item.modules, 40)}`)
      lines.push(`- Recommended fix: ${item.recommendedFix}`)
      lines.push('')
    }
  }
  return `${lines.join('\n')}\n`
}

export async function generateRuntimeParityAudit({
  repoRoot = process.cwd(),
  echoRoot = path.dirname(path.resolve(repoRoot)),
  outDir = DEFAULT_OUT_DIR,
  strictFull = false,
  strictPlay = false,
} = {}) {
  const normalizedRoot = path.resolve(repoRoot)
  const normalizedEchoRoot = path.resolve(echoRoot)
  const normalizedOutDir = path.resolve(normalizedRoot, outDir)
  const modules = await discoverModules(normalizedRoot)
  const docsIndex = await parseDocsIndex(normalizedRoot, modules)
  const allPackManifests = await discoverPackManifests(normalizedEchoRoot)
  const preferredManifests = preferredPackManifests(allPackManifests)
  const release = await releaseIndex(normalizedRoot)
  const runtimeEvidence = await collectRuntimeEvidence(normalizedEchoRoot, normalizedRoot, normalizePath(path.relative(normalizedRoot, normalizedOutDir)), modules)
  const rows = []
  for (const module of modules) {
    const packRefs = packRefsForModule(module.moduleId, preferredManifests)
    for (const runtime of RUNTIMES) {
      rows.push(await runtimeRow({ repoRoot: normalizedRoot, release, module, runtime, packRefs, runtimeEvidence }))
    }
  }
  const adapterCoreStrictPort = strictFull || strictPlay
    ? await generateAdapterCoreStrictPortAudit({
      repoRoot: normalizedRoot,
      outDir: normalizePath(path.relative(normalizedRoot, normalizedOutDir)),
      write: true,
    })
    : null
  if (adapterCoreStrictPort) {
    applyAdapterCoreStrictPortBlockers(rows, adapterCoreStrictPort.report)
  }

  const report = {
    schema: SCHEMA,
    generatedAt: new Date().toISOString(),
    generatedFrom: [
      'ECHO-Modules/addons/*/src/main/resources/META-INF/echo.mod.json',
      'ECHO-Modules/docs/module-docs-index.md',
      'ECHO-*-(Native|NeoForge|Standalone)-Edition manifests',
      'ECHO-Modules/dist/echo-module-release/echo-release.json when present',
      'ECHO-Native-Platform and ECHO-Standalone-Runtime smoke reports when present',
    ],
    repoRoot: normalizePath(normalizedRoot),
    echoRoot: normalizePath(normalizedEchoRoot),
    successStandard: 'NeoForge-equivalent play; descriptor-only and metadata-only registrations do not pass.',
    seedFindings: seedFindings(modules, preferredManifests),
    summary: {},
    docsIndex,
    runtimeEvidence: publicRuntimeEvidence(runtimeEvidence),
    adapterCoreStrictPortAudit: adapterCoreStrictPort
      ? {
          schema: adapterCoreStrictPort.report.schema,
          generatedAt: adapterCoreStrictPort.report.generatedAt,
          summary: adapterCoreStrictPort.report.summary,
          reportPath: normalizePath(path.relative(normalizedRoot, adapterCoreStrictPort.outputs.json)),
          markdownPath: normalizePath(path.relative(normalizedRoot, adapterCoreStrictPort.outputs.markdown)),
        }
      : null,
    packAudit: {
      allManifestCount: allPackManifests.length,
      preferredManifests,
    },
    modules: modules.map(publicModuleRecord),
    rows,
    backlog: [],
    strictWouldFail: false,
  }
  report.backlog = buildBacklog({ rows, modules, docsIndex, preferredPacks: preferredManifests })
  report.summary = summaryFor(report)
  report.strictFullSummary = strictFullSummaryFor(report)
  report.strictWouldFail = report.summary.resultCounts.fail > 0
    || report.backlog.some((item) => item.priority === 'P0')
  report.strictFullWouldFail = report.strictFullSummary.resultCounts.fail > 0
    || report.strictFullSummary.resultCounts.partial > 0

  const jsonPath = path.join(normalizedOutDir, 'echo-module-runtime-parity-audit.json')
  const mdPath = path.join(normalizedOutDir, 'echo-module-runtime-parity-audit.md')
  const backlogPath = path.join(normalizedOutDir, 'echo-module-runtime-parity-fix-backlog.md')
  const contractsPath = path.join(normalizedOutDir, 'module-feature-contracts.json')
  await fs.mkdir(normalizedOutDir, { recursive: true })
  await fs.writeFile(jsonPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  await fs.writeFile(mdPath, markdownReport(report), 'utf8')
  await fs.writeFile(backlogPath, markdownBacklog(report), 'utf8')
  await fs.writeFile(contractsPath, `${JSON.stringify(moduleFeatureContracts(report), null, 2)}\n`, 'utf8')

  let play = null
  if (strictPlay) {
    play = await generateRuntimePlayAudit({
      parityReport: report,
      repoRoot: normalizedRoot,
      echoRoot: normalizedEchoRoot,
      outDir: normalizePath(path.relative(normalizedRoot, normalizedOutDir)),
    })
  }

  return {
    report,
    play,
    paths: {
      json: jsonPath,
      markdown: mdPath,
      backlog: backlogPath,
      contracts: contractsPath,
      ...(adapterCoreStrictPort
        ? {
            adapterCoreStrictPort: adapterCoreStrictPort.outputs.json,
            adapterCoreStrictPortMarkdown: adapterCoreStrictPort.outputs.markdown,
          }
        : {}),
      ...(play?.paths ?? {}),
    },
  }
}

function moduleFeatureContracts(report) {
  return {
    schema: 'echo.module.feature_contracts.v1',
    generatedAt: report.generatedAt,
    modules: report.modules.map((module) => {
      const perRuntime = {}
      for (const runtime of RUNTIMES) {
        const row = report.rows.find((row) => row.moduleId === module.moduleId && row.runtime === runtime.id)
        perRuntime[runtime.id] = row
          ? {
              required: module.expectedFeatures.map((feature) => ({
                feature,
                requiredLevel: FEATURE_EVIDENCE_REQUIREMENTS[feature] || 'host-registered',
              })),
              satisfied: row.featureEvidence.filter((feature) => feature.satisfied).map((feature) => feature.feature),
              gaps: row.featureEvidence.filter((feature) => !feature.satisfied).map((feature) => ({
                feature: feature.feature,
                requiredLevel: feature.requiredLevel,
                actualLevel: feature.actualLevel,
              })),
            }
          : { required: [], satisfied: [], gaps: [] }
      }
      return {
        moduleId: module.moduleId,
        name: module.name,
        version: module.version,
        kind: module.kind,
        role: module.role,
        requiredRuntimes: module.declaredRuntimes,
        requiredPacks: report.packAudit.preferredManifests
          .filter((manifest) => manifest.moduleRequirements.includes(module.moduleId))
          .map((manifest) => ({ product: manifest.product, lane: manifest.lane })),
        entrypoints: {
          main: module.entrypoint,
          native: module.nativeEntrypoint,
        },
        expectedFeatures: module.expectedFeatures,
        expectedCreativeTabs: module.expectedCreativeTabs,
        contentKinds: inferContentKinds(module),
        uiRoutes: inferUiRoutes(module),
        blockActions: inferBlockActions(module),
        saveKeys: inferSaveKeys(module),
        networkChannels: inferNetworkChannels(module),
        worldgenKeys: inferWorldgenKeys(module),
        runtimeContracts: perRuntime,
      }
    }),
  }
}

function inferContentKinds(module) {
  const kinds = []
  if (module.sourceSignals.hasBlockClass || module.resourceSignals.hasBlockstates) kinds.push('block')
  if (module.sourceSignals.hasItemClass || module.resourceSignals.hasModels) kinds.push('item')
  if (module.resourceSignals.hasRecipes) kinds.push('recipe')
  if (module.resourceSignals.hasLoot) kinds.push('loot')
  if (module.resourceSignals.hasSounds) kinds.push('sound')
  if (module.expectedFeatures.includes('entities')) kinds.push('entity')
  if (module.expectedFeatures.includes('machines')) kinds.push('machine')
  return kinds
}

function inferUiRoutes(module) {
  const routes = []
  if (module.expectedFeatures.includes('terminal')) routes.push('echoterminal:terminal')
  if (module.expectedFeatures.includes('index')) routes.push('echoindex:index')
  if (module.expectedFeatures.includes('holomap')) routes.push('echoholomap:holomap')
  if (module.expectedFeatures.includes('lens')) routes.push('echolens:lens')
  if (module.expectedFeatures.includes('hud')) routes.push('echohudcore:hud')
  return routes
}

function inferBlockActions(module) {
  const actions = []
  if (module.expectedFeatures.includes('blocks')) actions.push('place', 'use', 'break')
  if (module.expectedFeatures.includes('machines')) actions.push('tick', 'insert', 'extract')
  return actions
}

function inferSaveKeys(module) {
  const keys = []
  if (module.expectedFeatures.includes('save_data')) keys.push(module.moduleId)
  if (module.expectedFeatures.includes('missions')) keys.push(`${module.moduleId}:missions`)
  return keys
}

function inferNetworkChannels(module) {
  const channels = []
  if (module.expectedFeatures.includes('networking')) channels.push(module.moduleId)
  return channels
}

function inferWorldgenKeys(module) {
  const keys = []
  if (module.expectedFeatures.includes('worldgen')) keys.push(module.moduleId)
  return keys
}

function publicRuntimeEvidence(runtimeEvidence) {
  const result = {}
  for (const [runtime, evidence] of Object.entries(runtimeEvidence)) {
    result[runtime] = {
      repoRoot: evidence.repoRoot ? normalizePath(evidence.repoRoot) : '',
      reports: evidence.reports ?? {},
      moduleCounts: {
        loaded: evidence.loadedModules?.size ?? 0,
        covered: evidence.coveredModules?.size ?? 0,
        executed: evidence.executedModules?.size ?? 0,
        uiRouteModules: evidence.uiRouteModules?.size ?? 0,
        contentHost: evidence.contentHostModules?.size ?? 0,
        uiVisible: evidence.uiVisibleModules?.size ?? 0,
        actionMutation: evidence.actionMutationModules?.size ?? 0,
        blockItemGameplay: evidence.blockItemGameplayModules?.size ?? 0,
        creativeTabRegistry: evidence.creativeTabRegistryModules?.size ?? 0,
        creativeTabParentVisible: evidence.creativeTabParentVisibleModules?.size ?? 0,
        creativeTabSearchVisible: evidence.creativeTabSearchVisibleModules?.size ?? 0,
        creativeTabSelectable: evidence.creativeTabSelectableModules?.size ?? 0,
        creativeTabPlayable: evidence.creativeTabPlayableModules?.size ?? 0,
        worldgen: evidence.worldgenModules?.size ?? 0,
        saveReload: evidence.saveReloadModules?.size ?? 0,
        networkSync: evidence.networkSyncModules?.size ?? 0,
      },
      routeDispatchCount: evidence.routeDispatchCount ?? 0,
      hostProofs: {
        artifactLoad: evidence.artifactLoadProof,
        lifecycle: evidence.lifecycleProof,
        content: evidence.contentHostProof,
        ui: evidence.uiHostProof,
        action: evidence.actionHostProof,
        blockItem: evidence.blockItemHostProof,
        creativeTab: evidence.creativeTabHostProof,
        worldgen: evidence.worldgenHostProof,
        saveNetwork: evidence.saveNetworkProof,
      },
    }
  }
  return result
}

function seedFindings(modules, preferredManifests) {
  const allThree = modules.filter((module) =>
    ['neoforge', 'echo_native', 'echo_runtime_standalone'].every((runtime) => module.declaredRuntimes.includes(runtime)))
  const nativeSources = modules.filter((module) => module.nativeEntrypoint && module.nativeEntrypointSourceExists)
  const mainEntrypointMissing = modules.filter((module) => module.entrypoint && !module.entrypointSourceExists)
  const inconsistentProducts = productModuleSetDifferences(preferredManifests)
  return [
    `${allThree.length} of ${modules.length} descriptor(s) declare AdapterCore support for neoforge, echo_native, and echo_runtime_standalone.`,
    `${nativeSources.length} of ${modules.length} declared Native entrypoint source class(es) were found.`,
    mainEntrypointMissing.length === 0
      ? 'No descriptor main entrypoint source mismatch was found.'
      : `Main entrypoint source mismatch: ${mainEntrypointMissing.map((module) => module.moduleId).join(', ')}.`,
    'Native Loader has UI/resource/network host bridge code, but visible client routes must be accepted by live host evidence before they pass this audit.',
    'Standalone has surface renderers, but Native activation surface projection is treated as headless until standalone UI/runtime controller evidence proves player-visible behavior.',
    inconsistentProducts.length === 0
      ? 'Preferred pack module sets are aligned across lanes for every product.'
      : `Preferred pack module sets differ across lanes for: ${inconsistentProducts.join(', ')}.`,
  ]
}

function productModuleSetDifferences(preferredManifests) {
  const groups = new Map()
  for (const manifest of preferredManifests) {
    const values = groups.get(manifest.product) ?? []
    values.push([...manifest.moduleRequirements].sort().join(','))
    groups.set(manifest.product, values)
  }
  return [...groups.entries()]
    .filter(([, values]) => new Set(values).size > 1)
    .map(([product]) => product)
    .sort()
}

function publicModuleRecord(module) {
  return {
    moduleId: module.moduleId,
    name: module.name,
    version: module.version,
    kind: module.kind,
    role: module.role,
    directory: module.directory,
    descriptorPath: module.descriptorPath,
    declaredDomains: module.declaredDomains,
    declaredRuntimes: module.declaredRuntimes,
    expectedFeatures: module.expectedFeatures,
    expectedCreativeTabs: module.expectedCreativeTabs,
    entrypoint: module.entrypoint,
    entrypointSourceExists: module.entrypointSourceExists,
    nativeEntrypoint: module.nativeEntrypoint,
    nativeEntrypointSourceExists: module.nativeEntrypointSourceExists,
    sourceSignals: module.sourceSignals,
    resourceSignals: module.resourceSignals,
  }
}

function summaryFor(report) {
  const resultCounts = countBy(report.rows, (row) => row.result)
  const resultCountsByRuntime = {}
  for (const runtime of RUNTIMES) {
    resultCountsByRuntime[runtime.id] = countBy(
      report.rows.filter((row) => row.runtime === runtime.id),
      (row) => row.result,
    )
  }
  return {
    moduleCount: report.modules.length,
    runtimeRowCount: report.rows.length,
    preferredPackManifestCount: report.packAudit.preferredManifests.length,
    resultCounts: {
      pass: resultCounts.pass ?? 0,
      partial: resultCounts.partial ?? 0,
      fail: resultCounts.fail ?? 0,
      not_applicable: resultCounts.not_applicable ?? 0,
    },
    resultCountsByRuntime,
    p0BacklogCount: report.backlog.filter((item) => item.priority === 'P0').length,
    p1BacklogCount: report.backlog.filter((item) => item.priority === 'P1').length,
    p2BacklogCount: report.backlog.filter((item) => item.priority === 'P2').length,
  }
}

function applyAdapterCoreStrictPortBlockers(rows, adapterCoreReport) {
  const blockersByModule = new Map(
    adapterCoreReport.rows
      .filter((row) => row.strictBlockers.length > 0)
      .map((row) => [
        row.moduleId,
        row.strictBlockers.map((blocker) => `missing AdapterCore strict-port evidence: ${blocker}`),
      ]),
  )
  for (const row of rows) {
    const blockers = blockersByModule.get(row.moduleId)
    if (!blockers) continue
    row.strictFullBlockers = [...new Set([...(row.strictFullBlockers ?? []), ...blockers])]
  }
}

function strictFullSummaryFor(report) {
  const strictFullResults = report.rows.map((row) => ({
    moduleId: row.moduleId,
    runtime: row.runtime,
    result: strictFullResultFor(row.strictFullBlockers),
    blockers: row.strictFullBlockers,
    featureEvidence: row.featureEvidence,
  }))
  const resultCounts = countBy(strictFullResults, (row) => row.result)
  const resultCountsByRuntime = {}
  for (const runtime of RUNTIMES) {
    resultCountsByRuntime[runtime.id] = countBy(
      strictFullResults.filter((row) => row.runtime === runtime.id),
      (row) => row.result,
    )
  }
  const featureBucketSummary = {}
  for (const row of report.rows) {
    for (const feature of row.featureEvidence) {
      const bucket = featureBucketFor(feature.feature)
      if (!featureBucketSummary[bucket]) {
        featureBucketSummary[bucket] = { feature: bucket, total: 0, satisfied: 0, byRuntime: {} }
      }
      featureBucketSummary[bucket].total += 1
      if (feature.satisfied) featureBucketSummary[bucket].satisfied += 1
      featureBucketSummary[bucket].byRuntime[row.runtime] = (featureBucketSummary[bucket].byRuntime[row.runtime] ?? 0) + 1
    }
  }
  return {
    rows: strictFullResults,
    resultCounts: {
      pass: resultCounts.pass ?? 0,
      partial: resultCounts.partial ?? 0,
      fail: resultCounts.fail ?? 0,
    },
    resultCountsByRuntime,
    featureBucketSummary: Object.values(featureBucketSummary).sort((left, right) => left.feature.localeCompare(right.feature)),
  }
}

function featureBucketFor(feature) {
  if (['gui', 'hud', 'screen', 'inventory_overlay'].includes(feature)) return 'ui_surface'
  if (['terminal', 'index', 'holomap', 'lens'].includes(feature)) return 'ui_application'
  if (['blocks', 'items', 'creative_tab', 'block_actions', 'machines', 'entities'].includes(feature)) return 'content_action'
  if (['worldgen', 'recipes', 'loot'].includes(feature)) return 'content_data'
  if (['networking', 'save_data', 'missions'].includes(feature)) return 'state_sync'
  return 'other'
}

function countBy(values, keyFn) {
  const result = {}
  for (const value of values) {
    const key = keyFn(value)
    result[key] = (result[key] ?? 0) + 1
  }
  return result
}

function parseArgs(argv) {
  const options = {
    repoRoot: process.cwd(),
    echoRoot: '',
    outDir: DEFAULT_OUT_DIR,
    strict: false,
    strictFull: false,
    strictPlay: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--repo-root') options.repoRoot = argv[++index]
    else if (arg === '--echo-root') options.echoRoot = argv[++index]
    else if (arg === '--out-dir') options.outDir = argv[++index]
    else if (arg === '--strict') options.strict = true
    else if (arg === '--strict-full') options.strictFull = true
    else if (arg === '--strict-play') {
      options.strictPlay = true
      options.strictFull = true
    }
    else if (arg === '--help') options.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  if (!options.echoRoot) options.echoRoot = path.dirname(path.resolve(options.repoRoot))
  return options
}

function addAll(set, values) {
  for (const value of values) set.add(value)
}

function object(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
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

function uniqueFilter(value, index, values) {
  return values.indexOf(value) === index
}

function normalizePath(value) {
  return value.replace(/\\/g, '/')
}

function inlineList(values, limit = 18) {
  if (!values || values.length === 0) return ''
  const shown = values.slice(0, limit)
  const suffix = values.length > shown.length ? `, +${values.length - shown.length} more` : ''
  return `${shown.join(', ')}${suffix}`
}

function escapeCell(value) {
  return String(value).replace(/\|/g, '\\|').replace(/\n/g, '<br>')
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const options = parseArgs(process.argv.slice(2))
    if (options.help) {
      console.log('Usage: node scripts/generate-runtime-parity-audit.mjs [--repo-root <path>] [--echo-root <path>] [--out-dir <path>] [--strict] [--strict-full] [--strict-play]')
    } else {
      const { report, play, paths } = await generateRuntimeParityAudit(options)
      console.log(`Wrote runtime parity audit JSON: ${paths.json}`)
      console.log(`Wrote runtime parity audit Markdown: ${paths.markdown}`)
      console.log(`Wrote runtime parity fix backlog: ${paths.backlog}`)
      if (play) {
        console.log(`Wrote runtime play audit JSON: ${paths.playAuditJson}`)
        console.log(`Wrote runtime play audit Markdown: ${paths.playAuditMarkdown}`)
        console.log(`Wrote runtime play evidence manifest: ${paths.evidenceManifest}`)
        console.log(`Wrote manual acceptance matrix: ${paths.manualAcceptanceMatrix}`)
        console.log(`Wrote module play completion report: ${paths.modulePlayCompletion}`)
        console.log(`Wrote runtime play fix backlog JSON: ${paths.playFixBacklogJson}`)
        console.log(`Wrote runtime play fix backlog Markdown: ${paths.playFixBacklogMarkdown}`)
      }
      if ((options.strict || options.strictFull) && report.strictWouldFail) {
        throw new Error(`Runtime parity audit failed strict mode: ${report.summary.resultCounts.fail} failing row(s), ${report.summary.p0BacklogCount} P0 backlog item(s).`)
      }
      if (options.strictFull && report.strictFullWouldFail) {
        throw new Error(`Runtime parity audit failed strict-full mode: ${report.strictFullSummary.resultCounts.fail} failing feature row(s), ${report.strictFullSummary.resultCounts.partial} partial feature row(s).`)
      }
      if (options.strictPlay && play?.playAudit.strictPlayWouldFail) {
        throw new Error(`Runtime parity audit failed strict-play mode: ${play.playAudit.summary.resultCounts.fail} failing row(s), ${play.playAudit.summary.resultCounts.partial} partial row(s), ${play.manualAcceptanceMatrix.summary.resultCounts.pass}/${play.manualAcceptanceMatrix.summary.packLaneCount} pack lane acceptance report(s) passing.`)
      }
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}

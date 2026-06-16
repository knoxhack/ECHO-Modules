#!/usr/bin/env node
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const DESCRIPTOR_PATH = path.join('src', 'main', 'resources', 'META-INF', 'echo.mod.json')
const DATA_PATH = path.join('src', 'main', 'resources', 'data')
const ASSETS_PATH = path.join('src', 'main', 'resources', 'assets')

const NODE_SCHEMA = 'echo.content_graph.node.v1'
const EDGE_SCHEMA = 'echo.content_graph.edge.v1'
const GRAPH_SCHEMA = 'echo.content_graph.v1'
const FEATURE_LIST_SCHEMA = 'echo.content_feature_list.v1'
const EXPORT_PLAN_SCHEMA = 'echo.content_graph.export_plan.v1'
const EVIDENCE_SCHEMA = 'echo.content_graph.evidence.v1'

const NODE_KINDS = {
  MODULE: 'echo:module',
  ADDON: 'echo:addon',
  DEPENDENCY: 'echo:dependency',
  ASSET: 'echo:asset',
  BLOCK: 'echo:block',
  ITEM: 'echo:item',
  CREATIVE_TAB: 'echo:creative_tab',
  RECIPE: 'echo:recipe',
  ENTITY: 'echo:entity',
  NPC: 'echo:npc',
  REGION: 'echo:region',
  TRIGGER: 'echo:trigger',
  EFFECT: 'echo:effect',
  MISSION: 'echo:mission',
  OBJECTIVE: 'echo:objective',
  UI_INTENT: 'echo:ui_intent',
  SETTING: 'echo:setting',
  SYSTEM: 'echo:system',
}

const EDGE_KINDS = {
  ADDON_CONTAINS_MODULE: 'addon_contains_module',
  MODULE_REQUIRES_MODULE: 'module_requires_module',
  MODULE_DECLARES_RUNTIME: 'module_declares_runtime',
  BLOCK_USES_ASSET: 'block_uses_asset',
  ITEM_USES_ASSET: 'item_uses_asset',
  CREATIVE_TAB_CONTAINS_ITEM: 'creative_tab_contains_item',
  RECIPE_CONSUMES_ITEM: 'recipe_consumes_item',
  RECIPE_OUTPUTS_ITEM: 'recipe_outputs_item',
  MISSION_HAS_OBJECTIVE: 'mission_has_objective',
  OBJECTIVE_TARGETS_NODE: 'objective_targets_node',
  UI_INTENT_CONTROLS_NODE: 'ui_intent_controls_node',
  TRIGGER_INVOKES_EFFECT: 'trigger_invokes_effect',
  REGION_CONTAINS_TRIGGER: 'region_contains_trigger',
  SETTING_AFFECTS_SYSTEM: 'setting_affects_system',
  SYSTEM_DECLARES_CAPABILITY: 'system_declares_capability',
}

const RUNTIME_TARGETS = ['neoforge', 'echo_native', 'echo_runtime_standalone', 'hytale']
const EXPORT_PLAN_STATUSES = ['direct', 'adapter_required', 'fallback', 'blocked', 'not_applicable']

const UI_INTENTS = [
  'selection_menu',
  'detail_panel',
  'notification',
  'terminal_page',
  'map_overlay',
  'scanner_result',
  'inventory_action',
  'confirmation_prompt',
  'settings_panel',
  'progress_tracker',
]

const OBJECTIVE_TYPES = [
  'discover_object',
  'interact',
  'consume_items',
  'enter_region',
  'activate_system',
  'collect_item',
  'observe_entity',
  'complete_trigger',
]

async function fileExists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

async function readJson(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'))
}

async function discoverModules(repoRoot) {
  const addonsRoot = path.join(repoRoot, 'addons')
  const entries = []
  for (const entry of await fs.readdir(addonsRoot, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue
    const descriptorPath = path.join(addonsRoot, entry.name, DESCRIPTOR_PATH)
    if (!(await fileExists(descriptorPath))) continue
    const descriptor = await readJson(descriptorPath)
    const dataDir = path.join(addonsRoot, entry.name, DATA_PATH)
    const assetsDir = path.join(addonsRoot, entry.name, ASSETS_PATH)
    entries.push({
      moduleDir: entry.name,
      descriptorPath,
      descriptor,
      dataDir: await fileExists(dataDir) ? dataDir : null,
      assetsDir: await fileExists(assetsDir) ? assetsDir : null,
    })
  }
  return entries.sort((left, right) => left.moduleDir.localeCompare(right.moduleDir))
}

function nowIso() {
  return new Date().toISOString()
}

function nodeId(namespace, localId) {
  return `${namespace}:${localId}`
}

function edgeId(namespace, kind, from, to) {
  const safe = `${kind}_${from.replace(/[:/]/g, '_')}_${to.replace(/[:/]/g, '_')}`
  return `${namespace}:${safe}`
}

function makeNode({ kind, id, moduleId, displayName, source, data = {}, provenance = {}, extra = {} }) {
  const node = {
    schemaVersion: NODE_SCHEMA,
    kind,
    id,
    moduleId,
    displayName: displayName ?? id,
    source: source ?? { repo: 'ECHO-Modules', path: '', format: 'generated' },
    aliases: [],
    capabilities: [],
    runtimeHints: Object.fromEntries(RUNTIME_TARGETS.map((target) => [target, {}])),
    data,
    provenance: {
      generatedBy: 'scripts/generate-content-graph.mjs',
      generatedAt: nowIso(),
      ...provenance,
    },
    ...extra,
  }
  if (node.source.path === '') {
    node.source.path = `addons/${moduleId}`
  }
  return node
}

function makeEdge({ kind, from, to, moduleId, data = {}, provenance = {} }) {
  return {
    schemaVersion: EDGE_SCHEMA,
    id: edgeId(moduleId, kind, from, to),
    kind,
    from,
    to,
    moduleId,
    data,
    provenance: {
      generatedBy: 'scripts/generate-content-graph.mjs',
      generatedAt: nowIso(),
      ...provenance,
    },
  }
}

function declaredRuntimes(descriptor) {
  const runtimes = descriptor.access?.adapterCore?.runtimes ?? []
  return Array.isArray(runtimes) ? runtimes : []
}

function runtimeHintsFromDescriptor(descriptor) {
  const runtimes = declaredRuntimes(descriptor)
  const hints = Object.fromEntries(RUNTIME_TARGETS.map((target) => [target, {}]))
  for (const target of runtimes) {
    if (target === 'native') hints.echo_native.declared = true
    else if (RUNTIME_TARGETS.includes(target)) hints[target].declared = true
    else if (target === 'neoforge') hints.neoforge.declared = true
  }
  return hints
}

function generateModuleNodes(entry) {
  const descriptor = entry.descriptor
  const moduleId = descriptor.id
  const nodes = []
  const edges = []

  const moduleNode = makeNode({
    kind: NODE_KINDS.MODULE,
    id: nodeId(moduleId, 'module'),
    moduleId,
    displayName: descriptor.name ?? moduleId,
    source: {
      repo: 'ECHO-Modules',
      path: path.relative(process.cwd(), entry.descriptorPath).replace(/\\/g, '/'),
      format: 'json',
    },
    data: {
      version: descriptor.version,
      kind: descriptor.kind,
      role: descriptor.role,
      channel: descriptor.channel,
      official: Boolean(descriptor.official),
      trustLevel: descriptor.trustLevel,
      side: descriptor.side,
      standalone: descriptor.standalone !== false,
      apiStability: descriptor.apiStability,
    },
  })
  moduleNode.runtimeHints = runtimeHintsFromDescriptor(descriptor)
  if (descriptor.replacements) {
    moduleNode.aliases = descriptor.replacements.map((r) => r.legacyId).filter(Boolean)
  }
  nodes.push(moduleNode)

  if (descriptor.kind === 'addon' || descriptor.kind === 'pack_root' || descriptor.kind === 'content_pack') {
    nodes.push(makeNode({
      kind: NODE_KINDS.ADDON,
      id: nodeId(moduleId, 'addon'),
      moduleId,
      displayName: `${descriptor.name ?? moduleId} Addon`,
      data: { kind: descriptor.kind },
    }))
    edges.push(makeEdge({
      kind: EDGE_KINDS.ADDON_CONTAINS_MODULE,
      from: nodeId(moduleId, 'addon'),
      to: nodeId(moduleId, 'module'),
      moduleId,
    }))
  }

  for (const depId of descriptor.requires ?? []) {
    edges.push(makeEdge({
      kind: EDGE_KINDS.MODULE_REQUIRES_MODULE,
      from: nodeId(moduleId, 'module'),
      to: nodeId(depId, 'module'),
      moduleId,
      data: { optional: false },
    }))
  }
  for (const depId of descriptor.optional ?? []) {
    edges.push(makeEdge({
      kind: EDGE_KINDS.MODULE_REQUIRES_MODULE,
      from: nodeId(moduleId, 'module'),
      to: nodeId(depId, 'module'),
      moduleId,
      data: { optional: true },
    }))
  }

  for (const target of declaredRuntimes(descriptor)) {
    const normalized = target === 'native' ? 'echo_native' : target
    if (!RUNTIME_TARGETS.includes(normalized)) continue
    const runtimeId = nodeId('echo', `runtime/${normalized}`)
    nodes.push(makeNode({
      kind: NODE_KINDS.SYSTEM,
      id: runtimeId,
      moduleId: 'echo',
      displayName: normalized,
      source: { repo: 'ECHO-SDK', path: 'schemas/content-graph.schema.json', format: 'json' },
      data: { systemType: 'runtime_target' },
    }))
    edges.push(makeEdge({
      kind: EDGE_KINDS.MODULE_DECLARES_RUNTIME,
      from: nodeId(moduleId, 'module'),
      to: runtimeId,
      moduleId,
      data: { runtime: normalized },
    }))
  }

  for (const capability of descriptor.provides ?? []) {
    const systemId = nodeId(moduleId, `system/${capability}`)
    nodes.push(makeNode({
      kind: NODE_KINDS.SYSTEM,
      id: systemId,
      moduleId,
      displayName: capability,
      data: { capability },
    }))
    edges.push(makeEdge({
      kind: EDGE_KINDS.SYSTEM_DECLARES_CAPABILITY,
      from: systemId,
      to: nodeId(moduleId, 'module'),
      moduleId,
    }))
  }

  for (const permission of descriptor.permissions ?? []) {
    nodes.push(makeNode({
      kind: NODE_KINDS.SETTING,
      id: nodeId(moduleId, `setting/${permission}`),
      moduleId,
      displayName: permission,
      data: { settingType: 'permission' },
    }))
  }
  for (const mode of descriptor.gameModes ?? []) {
    nodes.push(makeNode({
      kind: NODE_KINDS.SETTING,
      id: nodeId(moduleId, `setting/gamemode/${mode}`),
      moduleId,
      displayName: mode,
      data: { settingType: 'gameMode' },
    }))
  }

  return { nodes, edges }
}

async function* walkFiles(dir) {
  if (!dir) return
  try {
    const entries = await fs.readdir(dir, { withFileTypes: true, recursive: true })
    for (const entry of entries) {
      if (entry.isFile() && entry.name.endsWith('.json')) {
        yield path.join(entry.parentPath ?? dir, entry.name)
      }
    }
  } catch {
    // ignore missing directories
  }
}

function namespaceFromPath(filePath, baseDir) {
  const relative = path.relative(baseDir, filePath)
  const parts = relative.split(path.sep)
  return parts[0]
}

function normalizedPath(value) {
  return String(value).replace(/\\/g, '/')
}

function sourceFor(filePath) {
  return {
    repo: 'ECHO-Modules',
    path: normalizedPath(path.relative(process.cwd(), filePath)),
    format: 'json',
  }
}

function normalizeContentId(raw, defaultNs) {
  if (!raw || typeof raw !== 'string') return null
  const id = raw.startsWith('#') ? raw.slice(1) : raw
  if (id.includes(':')) return id
  return nodeId(defaultNs, id)
}

function safeLocalId(name) {
  let safe = String(name).toLowerCase().replace(/[^a-z0-9_/-]/g, '_')
  if (!/^[a-z]/.test(safe)) safe = `g_${safe}`
  return safe
}

function looksLikeWikiBlock(item) {
  return item && typeof item === 'object'
    && typeof item.type === 'string'
    && typeof item.body === 'string'
}

function looksLikePalette(item) {
  return typeof item === 'string' && item.includes(':')
}

function extractCatalogItems(array, kind, ns, moduleId, filePath, nodes, edges) {
  if (!Array.isArray(array)) return
  for (const item of array) {
    if (!item || typeof item !== 'object') continue
    if (typeof item.id !== 'string') continue
    if (looksLikeWikiBlock(item)) continue
    const id = item.id.includes(':') ? item.id : nodeId(ns, item.id)
    const displayName = item.displayName || item.title || item.name || id.split(':').pop()
    const capabilities = [...(item.tags || [])]
    nodes.push(makeNode({
      kind,
      id,
      moduleId,
      displayName,
      source: sourceFor(filePath),
      data: { ...item },
      extra: { capabilities },
    }))
    for (const tag of item.tags || []) {
      const tagId = tag.includes(':') ? tag : nodeId(ns, tag)
      nodes.push(makeNode({
        kind: NODE_KINDS.ASSET,
        id: tagId,
        moduleId,
        displayName: tag,
        source: sourceFor(filePath),
        data: { assetKind: 'tag' },
      }))
      edges.push(makeEdge({
        kind: kind === NODE_KINDS.BLOCK ? EDGE_KINDS.BLOCK_USES_ASSET : EDGE_KINDS.ITEM_USES_ASSET,
        from: id,
        to: tagId,
        moduleId,
        data: { assetKind: 'tag' },
      }))
    }
  }
}

function* normalizeIngredient(ingredient, ns) {
  if (!ingredient) return
  if (typeof ingredient === 'string') {
    if (ingredient.startsWith('#')) return
    const id = normalizeContentId(ingredient, ns)
    if (id) yield id
    return
  }
  if (Array.isArray(ingredient)) {
    for (const item of ingredient) yield* normalizeIngredient(item, ns)
    return
  }
  if (typeof ingredient === 'object') {
    if (ingredient.tag) return
    if (ingredient.item) {
      const id = normalizeContentId(ingredient.item, ns)
      if (id) yield id
    }
  }
}

function extractMinecraftRecipe(filePath, payload, ns, moduleId, nodes, edges) {
  const localName = path.basename(filePath, '.json')
  const recipeId = nodeId(ns, localName)
  nodes.push(makeNode({
    kind: NODE_KINDS.RECIPE,
    id: recipeId,
    moduleId,
    displayName: localName,
    source: sourceFor(filePath),
    data: { recipeType: payload.type },
  }))

  let outputId = null
  if (typeof payload.result === 'string') {
    outputId = normalizeContentId(payload.result, ns)
  } else if (payload.result && typeof payload.result === 'object') {
    outputId = normalizeContentId(payload.result.id || payload.result.item, ns)
  }
  if (outputId) {
    nodes.push(makeNode({
      kind: NODE_KINDS.ITEM,
      id: outputId,
      moduleId,
      displayName: outputId.split(':').pop(),
      source: sourceFor(filePath),
      data: {},
    }))
    edges.push(makeEdge({
      kind: EDGE_KINDS.RECIPE_OUTPUTS_ITEM,
      from: recipeId,
      to: outputId,
      moduleId,
    }))
  }

  const inputIds = new Set()
  const usedKeys = new Set()
  if (Array.isArray(payload.pattern)) {
    for (const row of payload.pattern) {
      if (typeof row === 'string') {
        for (const ch of row) {
          if (ch !== ' ') usedKeys.add(ch)
        }
      }
    }
  }
  if (payload.key && typeof payload.key === 'object') {
    for (const [key, value] of Object.entries(payload.key)) {
      if (Array.isArray(payload.pattern) && !usedKeys.has(key)) continue
      for (const id of normalizeIngredient(value, ns)) inputIds.add(id)
    }
  }
  if (payload.ingredients) {
    for (const ingredient of (Array.isArray(payload.ingredients) ? payload.ingredients : [payload.ingredients])) {
      for (const id of normalizeIngredient(ingredient, ns)) inputIds.add(id)
    }
  }
  if (payload.ingredient) {
    for (const id of normalizeIngredient(payload.ingredient, ns)) inputIds.add(id)
  }
  for (const inputId of inputIds) {
    nodes.push(makeNode({
      kind: NODE_KINDS.ITEM,
      id: inputId,
      moduleId,
      displayName: inputId.split(':').pop(),
      source: sourceFor(filePath),
      data: {},
    }))
    edges.push(makeEdge({
      kind: EDGE_KINDS.RECIPE_CONSUMES_ITEM,
      from: recipeId,
      to: inputId,
      moduleId,
    }))
  }
}

function extractMinecraftLootTable(filePath, payload, ns, moduleId, nodes) {
  const isBlock = payload.type === 'minecraft:block' || /[\\/]blocks[\\/]/.test(filePath)
  const localName = path.basename(filePath, '.json')
  const blockId = isBlock ? nodeId(ns, localName) : null
  if (blockId) {
    nodes.push(makeNode({
      kind: NODE_KINDS.BLOCK,
      id: blockId,
      moduleId,
      displayName: localName,
      source: sourceFor(filePath),
      data: { lootTable: true },
    }))
  }
  for (const pool of payload.pools || []) {
    for (const entry of pool.entries || []) {
      if (entry.type === 'minecraft:item' && entry.name) {
        const itemId = normalizeContentId(entry.name, ns)
        if (itemId) {
          nodes.push(makeNode({
            kind: NODE_KINDS.ITEM,
            id: itemId,
            moduleId,
            displayName: itemId.split(':').pop(),
            source: sourceFor(filePath),
            data: {},
          }))
        }
      }
    }
  }
}

function extractSingleObject(filePath, payload, ns, moduleId, nodes) {
  if (!payload || typeof payload !== 'object') return
  const normalizedFilePath = normalizedPath(filePath)
  const localName = path.basename(filePath, '.json')
  const displayName = payload.title || payload.displayName || payload.name || localName
  if (normalizedFilePath.includes('/gear/') || normalizedFilePath.includes('/modules/')) {
    const id = payload.id ? (payload.id.includes(':') ? payload.id : nodeId(ns, payload.id)) : nodeId(ns, localName)
    nodes.push(makeNode({
      kind: NODE_KINDS.ITEM,
      id,
      moduleId,
      displayName,
      source: sourceFor(filePath),
      data: { ...payload },
      extra: { capabilities: payload.tags || [] },
    }))
  } else if (normalizedFilePath.includes('/station_recipes/')) {
    const id = nodeId(ns, localName)
    nodes.push(makeNode({
      kind: NODE_KINDS.RECIPE,
      id,
      moduleId,
      displayName,
      source: sourceFor(filePath),
      data: { ...payload },
    }))
  }
}

async function parseContentJson(filePath) {
  try {
    return await readJson(filePath)
  } catch (error) {
    return null
  }
}

async function readText(filePath) {
  try {
    return await fs.readFile(filePath, 'utf8')
  } catch {
    return ''
  }
}

async function* walkJavaFiles(dir) {
  if (!dir) return
  try {
    const entries = await fs.readdir(dir, { withFileTypes: true, recursive: true })
    for (const entry of entries) {
      if (entry.isFile() && entry.name.endsWith('.java')) {
        yield path.join(entry.parentPath ?? dir, entry.name)
      }
    }
  } catch {
    // ignore missing source trees
  }
}

function methodBody(source, methodName) {
  const pattern = new RegExp(`\\b${methodName}\\s*\\([^)]*\\)\\s*\\{`, 'm')
  const match = pattern.exec(source)
  if (!match) return ''
  let depth = 1
  let index = match.index + match[0].length
  for (; index < source.length; index++) {
    const char = source[index]
    if (char === '{') depth++
    if (char === '}') depth--
    if (depth === 0) return source.slice(match.index + match[0].length, index)
  }
  return ''
}

function stringLiterals(text) {
  const values = []
  for (const match of text.matchAll(/"([^"\\]*(?:\\.[^"\\]*)*)"/g)) {
    const value = match[1].replace(/\\"/g, '"')
    if (!values.includes(value)) values.push(value)
  }
  return values
}

function extractListMethodValues(source, methodName, seen = new Set()) {
  if (!methodName || seen.has(methodName)) return []
  seen.add(methodName)
  const body = methodBody(source, methodName)
  if (!body) return []
  const directList = /return\s+List\.of\s*\(([\s\S]*?)\)\s*;/m.exec(body)
  if (directList) return stringLiterals(directList[1]).filter((value) => value.includes(':'))
  for (const fallback of body.matchAll(/return\s+([A-Za-z0-9_]+)\s*\(\s*\)\s*;/g)) {
    const values = extractListMethodValues(source, fallback[1], seen)
    if (values.length > 0) return values
  }
  for (const delegated of body.matchAll(/([A-Za-z0-9_]+)\s*\(\s*\)/g)) {
    const values = extractListMethodValues(source, delegated[1], seen)
    if (values.length > 0) return values
  }
  return []
}

function extractNativeCreativeTabs(source) {
  const tabs = []
  const registerPattern = /\.register\s*\(\s*"creative_tab"\s*,\s*"([^"]+)"\s*,\s*"([^"]*)"\s*,([\s\S]*?)\)\s*(?:\.register|;)/g
  for (const match of source.matchAll(registerPattern)) {
    const id = match[1]
    const description = match[2]
    const body = match[3]
    const properties = /([A-Za-z0-9_]*CreativeTabProperties|creativeTabProperties)\s*\(\s*"([^"]*)"\s*,\s*"([^"]*)"\s*,\s*([A-Za-z0-9_]+)\s*\(\s*\)\s*\)/m.exec(body)
    const titleKey = properties?.[2] ?? ''
    const iconItem = properties?.[3] ?? ''
    const itemMethod = properties?.[4] ?? ''
    tabs.push({
      id,
      description,
      titleKey,
      iconItem,
      itemIds: extractListMethodValues(source, itemMethod),
      sourceMethod: itemMethod,
    })
  }
  return tabs
}

function inferContentNamespace(descriptor, dataDir) {
  // Prefer descriptor namespace hints; fall back to module id.
  return descriptor.id
}

async function generateContentNodes(entry, allModuleIds) {
  const descriptor = entry.descriptor
  const moduleId = descriptor.id
  const nodes = []
  const edges = []
  const unresolved = []
  const dataDir = entry.dataDir
  const assetsDir = entry.assetsDir
  const javaDir = path.join(path.dirname(path.dirname(path.dirname(entry.descriptorPath))), 'java')

  if (!dataDir) return { nodes, edges, unresolved }

  const contentNs = inferContentNamespace(descriptor, dataDir)

  // Parse Foundation moved payloads first so canonical IDs take precedence.
  const foundationDir = path.join(dataDir, contentNs, 'foundation')
  if (await fileExists(foundationDir)) {
    for await (const filePath of walkFiles(foundationDir)) {
      const payload = await parseContentJson(filePath)
      if (!payload || typeof payload !== 'object') continue
      const schema = payload.schema || ''
      if (!schema.startsWith('echo.foundation.moved_openlands_') || !schema.endsWith('.v1')) continue
      const owner = payload.canonicalOwner || contentNs
      for (const block of payload.blocks || []) {
        if (!block.id) continue
        nodes.push(makeNode({
          kind: NODE_KINDS.BLOCK,
          id: block.id,
          moduleId,
          displayName: block.displayName || block.id.split(':')[1],
          source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'json' },
          data: { category: block.category, hardness: block.hardness, tool: block.tool, drops: block.drops, tags: block.tags },
          extra: { capabilities: [...(block.tags || []), 'foundation_moved'], aliases: block.legacyOpenlandsId ? [block.legacyOpenlandsId] : [] },
        }))
      }
      for (const item of payload.items || []) {
        if (!item.id) continue
        nodes.push(makeNode({
          kind: NODE_KINDS.ITEM,
          id: item.id,
          moduleId,
          displayName: item.displayName || item.id.split(':')[1],
          source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'json' },
          data: { category: item.category, maxStack: item.maxStack, tags: item.tags },
          extra: { capabilities: item.tags || [], aliases: item.legacyOpenlandsId ? [item.legacyOpenlandsId] : [] },
        }))
      }
    }
  }

  // Parse high-signal Openlands content shapes
  const openlandsBlocksPath = path.join(dataDir, contentNs, 'openlands', 'blocks', 'mvp_blocks.json')
  if (await fileExists(openlandsBlocksPath)) {
    const payload = await parseContentJson(openlandsBlocksPath)
    if (payload && Array.isArray(payload.blocks)) {
      for (const block of payload.blocks) {
        const localId = block.id || block.name
        if (!localId) continue
        const id = localId.includes(':') ? localId : nodeId(contentNs, localId)
        nodes.push(makeNode({
          kind: NODE_KINDS.BLOCK,
          id,
          moduleId,
          displayName: block.displayName || localId,
          source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), openlandsBlocksPath).replace(/\\/g, '/'), format: 'json' },
          data: {
            category: block.category,
            hardness: block.hardness,
            tool: block.tool,
            drops: block.drops,
            tags: block.tags,
          },
          extra: {
            capabilities: [...(block.tags || []), ...(block.stateMachine ? ['stateful'] : [])],
          },
        }))
        for (const tag of block.tags || []) {
          const tagId = tag.includes(':') ? tag : nodeId(contentNs, tag)
          nodes.push(makeNode({
            kind: NODE_KINDS.ASSET,
            id: tagId,
            moduleId,
            displayName: tag,
            source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), openlandsBlocksPath).replace(/\\/g, '/'), format: 'json' },
            data: { assetKind: 'tag' },
          }))
          edges.push(makeEdge({
            kind: EDGE_KINDS.BLOCK_USES_ASSET,
            from: id,
            to: tagId,
            moduleId,
            data: { assetKind: 'tag' },
          }))
        }
      }
    }
  }

  const openlandsItemsPath = path.join(dataDir, contentNs, 'openlands', 'items', 'mvp_items.json')
  if (await fileExists(openlandsItemsPath)) {
    const payload = await parseContentJson(openlandsItemsPath)
    if (payload && Array.isArray(payload.items)) {
      for (const item of payload.items) {
        const localId = item.id || item.name
        if (!localId) continue
        const id = localId.includes(':') ? localId : nodeId(contentNs, localId)
        nodes.push(makeNode({
          kind: NODE_KINDS.ITEM,
          id,
          moduleId,
          displayName: item.displayName || localId,
          source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), openlandsItemsPath).replace(/\\/g, '/'), format: 'json' },
          data: {
            category: item.category,
            maxStack: item.maxStack,
            tags: item.tags,
          },
          extra: { capabilities: item.tags || [] },
        }))
      }
    }
  }

  const openlandsRecipesPath = path.join(dataDir, contentNs, 'openlands', 'recipes', 'mvp_recipes.json')
  if (await fileExists(openlandsRecipesPath)) {
    const payload = await parseContentJson(openlandsRecipesPath)
    if (payload && Array.isArray(payload.recipes)) {
      for (const recipe of payload.recipes) {
        const localId = recipe.id || recipe.result
        if (!localId) continue
        const id = localId.includes(':') ? localId : nodeId(contentNs, localId)
        nodes.push(makeNode({
          kind: NODE_KINDS.RECIPE,
          id,
          moduleId,
          displayName: recipe.displayName || localId,
          source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), openlandsRecipesPath).replace(/\\/g, '/'), format: 'json' },
          data: {
            category: recipe.category,
            result: recipe.result,
          },
        }))
        for (const input of recipe.inputs || []) {
          const rawInput = typeof input === 'string' ? input : (input.block || input.item || input.id)
          if (!rawInput) continue
          const inputId = rawInput.includes(':') ? rawInput : nodeId(contentNs, rawInput)
          edges.push(makeEdge({
            kind: EDGE_KINDS.RECIPE_CONSUMES_ITEM,
            from: id,
            to: inputId,
            moduleId,
          }))
        }
        const outputs = recipe.outputs || (recipe.result ? [{ block: recipe.result, count: 1 }] : [])
        for (const output of outputs) {
          const rawOutput = typeof output === 'string' ? output : (output.block || output.item || output.id)
          if (!rawOutput) continue
          const outputId = rawOutput.includes(':') ? rawOutput : nodeId(contentNs, rawOutput)
          edges.push(makeEdge({
            kind: EDGE_KINDS.RECIPE_OUTPUTS_ITEM,
            from: id,
            to: outputId,
            moduleId,
          }))
        }
      }
    }
  }

  // Generic data/<namespace>/ scan for block/item/recipe/loot/catalog shapes
  for await (const filePath of walkFiles(dataDir)) {
    const payload = await parseContentJson(filePath)
    if (!payload || typeof payload !== 'object') continue
    const normalizedFilePath = normalizedPath(filePath)
    if (normalizedFilePath.includes('/tags/')) continue
    const ns = namespaceFromPath(filePath, dataDir)

    // Minecraft datapack recipes and loot tables
    if (typeof payload.type === 'string' && payload.type.startsWith('minecraft:')) {
      const recipeTypes = [
        'minecraft:crafting_shaped',
        'minecraft:crafting_shapeless',
        'minecraft:smelting',
        'minecraft:blasting',
        'minecraft:smoking',
        'minecraft:campfire_cooking',
        'minecraft:stonecutting',
        'minecraft:smithing_transform',
        'minecraft:smithing_trim',
      ]
      if (recipeTypes.includes(payload.type)) {
        extractMinecraftRecipe(filePath, payload, ns, moduleId, nodes, edges)
      } else {
        extractMinecraftLootTable(filePath, payload, ns, moduleId, nodes)
      }
      continue
    }

    if (normalizedFilePath.includes('/loot_table/')) {
      extractMinecraftLootTable(filePath, payload, ns, moduleId, nodes)
      continue
    }

    // Catalog arrays (blocks/items/materials)
    if (Array.isArray(payload.blocks) && payload.blocks.length > 0 && !payload.blocks.some((b) => typeof b === 'string' || looksLikeWikiBlock(b))) {
      extractCatalogItems(payload.blocks, NODE_KINDS.BLOCK, ns, moduleId, filePath, nodes, edges)
    }
    if (Array.isArray(payload.items) && payload.items.length > 0 && !payload.items.some((i) => typeof i === 'string' || looksLikeWikiBlock(i))) {
      extractCatalogItems(payload.items, NODE_KINDS.ITEM, ns, moduleId, filePath, nodes, edges)
    }
    if (Array.isArray(payload.materials) && payload.materials.length > 0) {
      extractCatalogItems(payload.materials, NODE_KINDS.ITEM, ns, moduleId, filePath, nodes, edges)
    }

    // Schema-hinted content (creatures, weather, regions, missions)
    const schema = payload.schema
    if (schema && typeof schema === 'string') {
      if (schema.includes('creature') || schema.includes('entity')) {
        const list = Array.isArray(payload.creatures) ? payload.creatures : Array.isArray(payload.entities) ? payload.entities : []
        for (const entity of list) {
          const localId = entity.id || entity.name
          if (!localId) continue
          const entityId = localId.includes(':') ? localId : nodeId(ns, localId)
          nodes.push(makeNode({
            kind: NODE_KINDS.ENTITY,
            id: entityId,
            moduleId,
            displayName: entity.displayName || localId,
            source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'json' },
            data: entity,
          }))
        }
      }

      if (schema.includes('weather') || schema.includes('effect')) {
        const localId = payload.id
        if (localId) {
          nodes.push(makeNode({
            kind: NODE_KINDS.EFFECT,
            id: nodeId(ns, localId),
            moduleId,
            displayName: payload.displayName || localId,
            source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'json' },
            data: payload,
          }))
        }
      }

      if (schema.includes('region') || schema.includes('holomap')) {
        const list = Array.isArray(payload.regions) ? payload.regions : []
        for (const region of list) {
          const localId = region.id || region.name
          if (!localId) continue
          const regionId = localId.includes(':') ? localId : nodeId(ns, localId)
          nodes.push(makeNode({
            kind: NODE_KINDS.REGION,
            id: regionId,
            moduleId,
            displayName: region.displayName || localId,
            source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'json' },
            data: region,
          }))
        }
      }

      if (schema.includes('mission') || schema.includes('journey')) {
        const list = Array.isArray(payload.missions) ? payload.missions : []
        for (const mission of list) {
          const localId = mission.id || mission.advancement || mission.title
          if (!localId) continue
          const missionId = nodeId(ns, `mission/${localId}`)
          nodes.push(makeNode({
            kind: NODE_KINDS.MISSION,
            id: missionId,
            moduleId,
            displayName: mission.title || localId,
            source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'json' },
            data: {
              phase: mission.phase,
              tier: mission.tier,
              role: mission.role,
            },
          }))
          // Objectives inferred from mission structure if explicit objectives absent
          if (!Array.isArray(mission.objectives)) {
            const objectiveId = nodeId(ns, `objective/${localId}/complete`)
            nodes.push(makeNode({
              kind: NODE_KINDS.OBJECTIVE,
              id: objectiveId,
              moduleId,
              displayName: `Complete ${mission.title || localId}`,
              data: { objectiveType: 'complete_trigger' },
            }))
            edges.push(makeEdge({
              kind: EDGE_KINDS.MISSION_HAS_OBJECTIVE,
              from: missionId,
              to: objectiveId,
              moduleId,
            }))
          } else {
            for (const objective of mission.objectives) {
              const objectiveId = nodeId(ns, `objective/${localId}/${objective.id || 'main'}`)
              nodes.push(makeNode({
                kind: NODE_KINDS.OBJECTIVE,
                id: objectiveId,
                moduleId,
                displayName: objective.title || objective.id,
                data: {
                  objectiveType: OBJECTIVE_TYPES.includes(objective.type) ? objective.type : 'complete_trigger',
                },
              }))
              edges.push(makeEdge({
                kind: EDGE_KINDS.MISSION_HAS_OBJECTIVE,
                from: missionId,
                to: objectiveId,
                moduleId,
              }))
              if (objective.target) {
                edges.push(makeEdge({
                  kind: EDGE_KINDS.OBJECTIVE_TARGETS_NODE,
                  from: objectiveId,
                  to: nodeId(ns, objective.target),
                  moduleId,
                }))
              }
            }
          }
        }
      }
    }

    // Single-object item definitions from authoring directories
    extractSingleObject(filePath, payload, ns, moduleId, nodes)
  }

  // Assets scan for UI intent hints
  for await (const filePath of walkFiles(assetsDir)) {
    const payload = await parseContentJson(filePath)
    if (!payload || typeof payload !== 'object') continue
    const schema = payload.schema || ''
    const ns = namespaceFromPath(filePath, assetsDir)

    if (schema.includes('screen') || schema.includes('eui') || filePath.includes('eui_manifest')) {
      const pages = payload.pages || payload.screens || []
      for (const page of pages) {
        const localId = page.id || page.name
        if (!localId) continue
        const uiId = localId.includes(':') ? localId : nodeId(ns, `ui/${localId}`)
        nodes.push(makeNode({
          kind: NODE_KINDS.UI_INTENT,
          id: uiId,
          moduleId,
          displayName: page.title || localId,
          source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'json' },
          data: {},
          extra: {
            intent: UI_INTENTS.includes(page.intent) ? page.intent : 'detail_panel',
            actions: (page.actions || []).map((a) => ({ id: a.id, label: a.label || a.id, requires: a.requires })),
            fallbacks: {
              neoforge: page.fallbacks?.neoforge || 'custom_screen',
              echo_native: page.fallbacks?.echo_native || 'native_panel',
              echo_runtime_standalone: page.fallbacks?.echo_runtime_standalone || 'native_panel',
              hytale: page.fallbacks?.hytale || 'notification_and_basic_menu',
            },
          },
        }))
      }
    }
  }

  for await (const filePath of walkJavaFiles(javaDir)) {
    const source = await readText(filePath)
    if (!source.includes('"creative_tab"')) continue
    for (const tab of extractNativeCreativeTabs(source)) {
      nodes.push(makeNode({
        kind: NODE_KINDS.CREATIVE_TAB,
        id: tab.id,
        moduleId,
        displayName: tab.titleKey || tab.id,
        description: tab.description,
        source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'java' },
        data: {
          titleKey: tab.titleKey,
          iconItem: tab.iconItem,
          itemIds: tab.itemIds,
          sourceMethod: tab.sourceMethod,
          populationMode: 'native_registry_declaration',
        },
        extra: {
          capabilities: ['creative_inventory_surface'],
          runtimeHints: {
            neoforge: { declared: true },
            echo_native: { declared: true, nativeRegistryCreativeTab: true },
            echo_runtime_standalone: { declared: true },
            hytale: { plannedAsInventoryCategory: true },
          },
        },
      }))
      for (const itemId of tab.itemIds) {
        nodes.push(makeNode({
          kind: NODE_KINDS.ITEM,
          id: itemId,
          moduleId,
          displayName: itemId.split(':')[1] || itemId,
          source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'java' },
          data: {
            declaredByCreativeTab: tab.id,
            sourceMethod: tab.sourceMethod,
          },
          extra: { capabilities: ['creative_inventory_entry'] },
        }))
        edges.push(makeEdge({
          kind: EDGE_KINDS.CREATIVE_TAB_CONTAINS_ITEM,
          from: tab.id,
          to: itemId,
          moduleId,
          data: { sourceMethod: tab.sourceMethod },
        }))
      }
    }
  }

  return { nodes, edges, unresolved }
}

function deduplicateNodes(nodes) {
  const seen = new Map()
  for (const node of nodes) {
    if (!seen.has(node.id)) seen.set(node.id, node)
  }
  return Array.from(seen.values())
}

function deduplicateEdges(edges) {
  const seen = new Set()
  return edges.filter((edge) => {
    const key = `${edge.kind}|${edge.from}|${edge.to}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function generateFeatures(moduleId, nodes, edges) {
  const features = []
  const nodeById = new Map(nodes.map((n) => [n.id, n]))

  // Group connected UI intents + blocks/items into features
  const uiIntents = nodes.filter((n) => n.kind === NODE_KINDS.UI_INTENT)
  for (const ui of uiIntents) {
    const connectedEdges = edges.filter((e) => e.kind === EDGE_KINDS.UI_INTENT_CONTROLS_NODE && e.from === ui.id)
    const featureNodes = [ui.id, ...connectedEdges.map((e) => e.to)]
    const title = ui.displayName || ui.id
    features.push({
      id: nodeId(moduleId, `feature/${ui.id.split(':')[1]}`),
      title,
      nodes: featureNodes,
      runtimes: Object.fromEntries(RUNTIME_TARGETS.map((target) => [target, 'supported'])),
    })
  }

  // Mission features
  const missions = nodes.filter((n) => n.kind === NODE_KINDS.MISSION)
  for (const mission of missions) {
    const objectiveEdges = edges.filter((e) => e.kind === EDGE_KINDS.MISSION_HAS_OBJECTIVE && e.from === mission.id)
    const featureNodes = [mission.id, ...objectiveEdges.map((e) => e.to)]
    features.push({
      id: nodeId(moduleId, `feature/${mission.id.split(':')[1]}`),
      title: mission.displayName || mission.id,
      nodes: featureNodes,
      runtimes: Object.fromEntries(RUNTIME_TARGETS.map((target) => [target, 'supported'])),
    })
  }

  // Fallback: every block that isn't part of a UI feature becomes its own feature
  const featuredNodeIds = new Set(features.flatMap((f) => f.nodes))
  for (const node of nodes) {
    if (node.kind !== NODE_KINDS.BLOCK && node.kind !== NODE_KINDS.ITEM) continue
    if (featuredNodeIds.has(node.id)) continue
    features.push({
      id: nodeId(moduleId, `feature/${node.id.split(':')[1]}`),
      title: node.displayName || node.id,
      nodes: [node.id],
      runtimes: Object.fromEntries(RUNTIME_TARGETS.map((target) => [target, 'supported'])),
    })
  }

  return {
    schemaVersion: FEATURE_LIST_SCHEMA,
    moduleId,
    generatedAt: nowIso(),
    features,
    provenance: { generatedBy: 'scripts/generate-content-feature-list.mjs', generatedAt: nowIso() },
  }
}

function generateExportPlan(target, graph) {
  const plan = {
    schemaVersion: EXPORT_PLAN_SCHEMA,
    target,
    sourceGraphId: graph.id,
    plannedAt: nowIso(),
    nodes: [],
    summary: { direct: 0, adapter_required: 0, fallback: 0, blocked: 0, not_applicable: 0 },
    provenance: { generatedBy: 'scripts/generate-runtime-export-plan.mjs', generatedAt: nowIso() },
  }

  const mapping = {
    [NODE_KINDS.BLOCK]: (node) => {
      if (node.capabilities?.includes('stateful')) return { status: 'direct', mappedTo: 'server_state_object' }
      if (node.capabilities?.includes('interactable')) return { status: 'direct', mappedTo: 'interaction_trigger' }
      return { status: 'adapter_required', mappedTo: 'block_state_adapter' }
    },
    [NODE_KINDS.ITEM]: () => ({ status: 'direct', mappedTo: 'item' }),
    [NODE_KINDS.CREATIVE_TAB]: () => ({ status: 'direct', mappedTo: 'creative_inventory_category' }),
    [NODE_KINDS.RECIPE]: () => ({ status: 'adapter_required', mappedTo: 'crafting_action' }),
    [NODE_KINDS.ENTITY]: () => ({ status: 'blocked', mappedTo: null, rationale: 'Hytale entity contract not defined.' }),
    [NODE_KINDS.NPC]: () => ({ status: 'blocked', mappedTo: null, rationale: 'Hytale entity contract not defined.' }),
    [NODE_KINDS.REGION]: () => ({ status: 'direct', mappedTo: 'area_trigger' }),
    [NODE_KINDS.TRIGGER]: () => ({ status: 'direct', mappedTo: 'trigger' }),
    [NODE_KINDS.EFFECT]: () => ({ status: 'adapter_required', mappedTo: 'effect_adapter' }),
    [NODE_KINDS.MISSION]: () => ({ status: 'adapter_required', mappedTo: 'quest_sidecar' }),
    [NODE_KINDS.OBJECTIVE]: () => ({ status: 'adapter_required', mappedTo: 'quest_task' }),
    [NODE_KINDS.UI_INTENT]: (node) => {
      if (node.intent === 'selection_menu') return { status: 'fallback', mappedTo: 'basic_menu' }
      if (node.intent === 'notification') return { status: 'fallback', mappedTo: 'notification' }
      return { status: 'fallback', mappedTo: 'notification_and_basic_menu' }
    },
    [NODE_KINDS.MODULE]: () => ({ status: 'not_applicable', mappedTo: null }),
    [NODE_KINDS.ADDON]: () => ({ status: 'not_applicable', mappedTo: null }),
    [NODE_KINDS.DEPENDENCY]: () => ({ status: 'not_applicable', mappedTo: null }),
    [NODE_KINDS.SETTING]: () => ({ status: 'not_applicable', mappedTo: null }),
    [NODE_KINDS.SYSTEM]: () => ({ status: 'not_applicable', mappedTo: null }),
    [NODE_KINDS.ASSET]: () => ({ status: 'adapter_required', mappedTo: 'asset_adapter' }),
  }

  for (const node of graph.nodes) {
    const mapFn = mapping[node.kind] || (() => ({ status: 'blocked', mappedTo: null, rationale: 'No Hytale mapping defined.' }))
    const mapped = mapFn(node)
    plan.nodes.push({
      nodeId: node.id,
      status: mapped.status,
      mappedTo: mapped.mappedTo,
      rationale: mapped.rationale || undefined,
    })
    plan.summary[mapped.status]++
  }

  if (target !== 'hytale') {
    // For native/neoforge/standalone, mark all content nodes as direct by default
    for (const entry of plan.nodes) {
      if (entry.status === 'blocked' || entry.status === 'not_applicable') continue
      entry.status = 'direct'
    }
    plan.summary = { direct: plan.nodes.length, adapter_required: 0, fallback: 0, blocked: 0, not_applicable: 0 }
  }

  return plan
}

function generateMarkdown(graph, moduleId) {
  const lines = [
    `# Content Graph: ${moduleId}`,
    '',
    `- Schema: ${graph.schemaVersion}`,
    `- Generated: ${graph.generatedAt}`,
    `- Nodes: ${graph.nodes.length}`,
    `- Edges: ${graph.edges.length}`,
    `- Unresolved references: ${graph.unresolvedReferences.length}`,
    '',
    '## Nodes',
    '',
    '| Kind | ID | Display Name |',
    '|------|----|--------------|',
  ]
  for (const node of graph.nodes) {
    lines.push(`| ${node.kind} | \`${node.id}\` | ${node.displayName} |`)
  }
  lines.push('', '## Edges', '')
  for (const edge of graph.edges.slice(0, 50)) {
    lines.push(`- \`${edge.kind}\`: ${edge.from} → ${edge.to}`)
  }
  if (graph.edges.length > 50) {
    lines.push(`- ... and ${graph.edges.length - 50} more`)
  }
  return lines.join('\n') + '\n'
}

function generateProvenance(graph, moduleDir) {
  return {
    schema: 'echo.content_graph.provenance.v1',
    graphId: graph.id,
    moduleId: graph.moduleId,
    generatedAt: graph.generatedAt,
    sourceDescriptor: path.relative(process.cwd(), path.join('addons', moduleDir, DESCRIPTOR_PATH)).replace(/\\/g, '/'),
    nodeCount: graph.nodes.length,
    edgeCount: graph.edges.length,
    unresolvedCount: graph.unresolvedReferences.length,
    generatedBy: 'scripts/generate-content-graph.mjs',
  }
}

function hytaleStatusSummary(plan) {
  const summary = Object.fromEntries(EXPORT_PLAN_STATUSES.map((status) => [status, 0]))
  for (const node of plan?.nodes ?? []) {
    if (typeof node?.status === 'string' && Object.hasOwn(summary, node.status)) {
      summary[node.status] += 1
    }
  }
  return summary
}

function hytaleBlockerSummaries(plan) {
  const blockedNodes = (plan?.nodes ?? [])
    .filter((node) => node?.status === 'blocked')
    .map((node) => `${node.nodeId}${node.rationale ? `: ${node.rationale}` : ''}`)
    .filter(Boolean)
  if (blockedNodes.length > 0) return blockedNodes

  if (Array.isArray(plan?.blockers)) {
    return plan.blockers
      .map((blocker) => {
        if (typeof blocker === 'string') return blocker
        if (blocker && typeof blocker === 'object') {
          return `${blocker.nodeId ?? blocker.id ?? 'unknown'}${blocker.rationale ? `: ${blocker.rationale}` : ''}`
        }
        return ''
      })
      .filter(Boolean)
  }

  const blocked = Number(plan?.summary?.blocked ?? 0)
  return blocked > 0 ? [`${blocked} Hytale node(s) blocked by summary count.`] : []
}

export function summarizeContentGraphEvidence(results, {
  generatedAt = nowIso(),
  source = 'ECHO-Modules/dist/echo-module-release',
} = {}) {
  const modules = []
  const diagnostics = []
  const hytaleSummary = Object.fromEntries(EXPORT_PLAN_STATUSES.map((status) => [status, 0]))
  let nodeCount = 0
  let edgeCount = 0
  let featureCount = 0
  let exportPlanCount = 0
  let unresolvedReferenceCount = 0
  let hytaleBlockerCount = 0

  for (const result of results) {
    const graph = result.graph ?? {}
    const features = Array.isArray(result.features?.features) ? result.features.features : []
    const plans = result.plans ?? {}
    const hytale = plans.hytale
    const blockers = hytaleBlockerSummaries(hytale)
    const statusSummary = hytaleStatusSummary(hytale)
    const unresolved = Array.isArray(graph.unresolvedReferences) ? graph.unresolvedReferences : []
    const requiredUnresolved = unresolved.filter((ref) => ref?.required)
    const validationIssues = requiredUnresolved.map((ref) => `Required unresolved reference ${ref.id}${ref.context ? ` (${ref.context})` : ''}`)

    for (const [status, count] of Object.entries(statusSummary)) {
      hytaleSummary[status] += count
    }

    const moduleNodeCount = Array.isArray(graph.nodes) ? graph.nodes.length : 0
    const moduleEdgeCount = Array.isArray(graph.edges) ? graph.edges.length : 0
    const moduleExportPlanCount = Object.keys(plans).length
    nodeCount += moduleNodeCount
    edgeCount += moduleEdgeCount
    featureCount += features.length
    exportPlanCount += moduleExportPlanCount
    unresolvedReferenceCount += unresolved.length
    hytaleBlockerCount += blockers.length

    modules.push({
      moduleId: result.moduleId,
      version: result.version,
      graphPath: `${result.moduleId}/${result.version}/.echo/content-graph/content-graph.json`,
      schemaVersion: String(graph.schemaVersion ?? ''),
      nodeCount: moduleNodeCount,
      edgeCount: moduleEdgeCount,
      featureCount: features.length,
      exportPlanCount: moduleExportPlanCount,
      unresolvedReferenceCount: unresolved.length,
      hytaleBlockerCount: blockers.length,
      validationState: validationIssues.length > 0 ? 'invalid' : blockers.length > 0 || unresolved.length > 0 ? 'warning' : 'valid',
      hytaleBlockers: blockers,
      validationIssues,
    })

    if (blockers.length > 0) {
      diagnostics.push({
        severity: 'warning',
        code: 'HYTALE_BLOCKED',
        path: `${result.moduleId}/${result.version}/.echo/content-graph/export-plans/hytale.json`,
        message: `${result.moduleId} has ${blockers.length} Hytale blocked node(s).`,
      })
    }
    for (const issue of validationIssues) {
      diagnostics.push({
        severity: 'error',
        code: 'CONTENT_GRAPH_REQUIRED_UNRESOLVED_REFERENCE',
        path: `${result.moduleId}/${result.version}/.echo/content-graph/content-graph.json`,
        message: issue,
      })
    }
  }

  return {
    schemaVersion: EVIDENCE_SCHEMA,
    generatedAt,
    source,
    graphCount: results.length,
    moduleCount: results.length,
    nodeCount,
    edgeCount,
    featureCount,
    exportPlanCount,
    unresolvedReferenceCount,
    hytaleBlockerCount,
    validationState: diagnostics.some((diagnostic) => diagnostic.severity === 'error' || diagnostic.severity === 'fatal')
      ? 'invalid'
      : diagnostics.length > 0 ? 'warning' : 'valid',
    hytaleSummary,
    modules: modules.sort((left, right) => left.moduleId.localeCompare(right.moduleId)),
    diagnostics,
  }
}

export async function generateContentGraph({
  repoRoot = process.cwd(),
  moduleIds = [],
  write = false,
  outputRoot = path.join(repoRoot, 'dist', 'echo-module-release'),
} = {}) {
  const modules = await discoverModules(repoRoot)
  const allModuleIds = new Set(modules.map((m) => m.descriptor.id))
  const targetModules = moduleIds.length > 0 ? modules.filter((m) => moduleIds.includes(m.descriptor.id)) : modules
  const results = []

  for (const entry of targetModules) {
    const moduleId = entry.descriptor.id
    const { nodes: moduleNodes, edges: moduleEdges } = generateModuleNodes(entry)
    const { nodes: contentNodes, edges: contentEdges, unresolved } = await generateContentNodes(entry, allModuleIds)

    const nodes = deduplicateNodes([...moduleNodes, ...contentNodes])
    const edges = deduplicateEdges([...moduleEdges, ...contentEdges])

    const graphId = nodeId(moduleId, 'graph')
    const graph = {
      schemaVersion: GRAPH_SCHEMA,
      id: graphId,
      moduleId,
      generatedAt: nowIso(),
      modules: [moduleId, ...(entry.descriptor.requires || [])],
      nodes,
      edges,
      unresolvedReferences: unresolved,
      provenance: {
        generatedBy: 'scripts/generate-content-graph.mjs',
        generatedAt: nowIso(),
        sourceRepo: 'knoxhack/ECHO-Modules',
      },
    }

    const features = generateFeatures(moduleId, nodes, edges)
    const plans = Object.fromEntries(
      RUNTIME_TARGETS.map((target) => [target, generateExportPlan(target, graph)])
    )
    const markdown = generateMarkdown(graph, moduleId)
    const provenance = generateProvenance(graph, entry.moduleDir)

    const outDir = path.join(outputRoot, moduleId, entry.descriptor.version || '0.1.0', '.echo', 'content-graph')
    if (write) {
      await fs.mkdir(path.join(outDir, 'export-plans'), { recursive: true })
      await fs.writeFile(path.join(outDir, 'content-graph.json'), `${JSON.stringify(graph, null, 2)}\n`, 'utf8')
      await fs.writeFile(path.join(outDir, 'content-graph.md'), markdown, 'utf8')
      await fs.writeFile(path.join(outDir, 'features.json'), `${JSON.stringify(features, null, 2)}\n`, 'utf8')
      await fs.writeFile(path.join(outDir, 'provenance.json'), `${JSON.stringify(provenance, null, 2)}\n`, 'utf8')
      await fs.writeFile(path.join(outDir, 'unresolved-references.json'), `${JSON.stringify({ schema: 'echo.content_graph.unresolved_references.v1', moduleId, generatedAt: nowIso(), unresolved }, null, 2)}\n`, 'utf8')
      for (const [target, plan] of Object.entries(plans)) {
        await fs.writeFile(path.join(outDir, 'export-plans', `${target}.json`), `${JSON.stringify(plan, null, 2)}\n`, 'utf8')
      }
    }

    results.push({
      moduleId,
      moduleDir: entry.moduleDir,
      version: entry.descriptor.version || '0.1.0',
      nodeCount: nodes.length,
      edgeCount: edges.length,
      unresolvedCount: unresolved.length,
      outDir: write ? outDir : null,
      graph,
      features,
      plans,
    })
  }

  return results
}

function parseArgs(argv) {
  const write = argv.includes('--write')
  const all = argv.includes('--all')
  const help = argv.includes('--help')
  const moduleIndex = argv.indexOf('--module')
  const moduleIds = moduleIndex >= 0 && argv[moduleIndex + 1] ? argv[moduleIndex + 1].split(',') : []
  return { write, all, help, moduleIds }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  const options = parseArgs(process.argv.slice(2))
  if (options.help) {
    console.log('Usage: node scripts/generate-content-graph.mjs [--all] [--write] [--module id1,id2]')
    process.exit(0)
  }
  generateContentGraph({ write: options.write, moduleIds: options.moduleIds })
    .then((results) => {
      const totalNodes = results.reduce((sum, r) => sum + r.nodeCount, 0)
      const totalEdges = results.reduce((sum, r) => sum + r.edgeCount, 0)
      console.log(`Generated content graphs for ${results.length} module(s), ${totalNodes} nodes, ${totalEdges} edges.`)
      for (const r of results.slice(0, 10)) {
        console.log(`  ${r.moduleId}: ${r.nodeCount} nodes, ${r.edgeCount} edges, ${r.unresolvedCount} unresolved`)
      }
      if (results.length > 10) {
        console.log(`  ... and ${results.length - 10} more`)
      }
    })
    .catch((error) => {
      console.error(error)
      process.exitCode = 1
    })
}

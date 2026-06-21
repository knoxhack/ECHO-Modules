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
const PLAYER_SURFACE_MANIFEST_SCHEMA = 'echo.native.player_surface_manifest.v1'

const NODE_KINDS = {
  MODULE: 'echo:module',
  ADDON: 'echo:addon',
  DEPENDENCY: 'echo:dependency',
  ASSET: 'echo:asset',
  BLOCK: 'echo:block',
  ITEM: 'echo:item',
  CREATIVE_TAB: 'echo:creative_tab',
  RECIPE: 'echo:recipe',
  LOOT_TABLE: 'echo:loot_table',
  ENTITY: 'echo:entity',
  NPC: 'echo:npc',
  BIOME: 'echo:biome',
  STRUCTURE: 'echo:structure',
  FEATURE: 'echo:feature',
  SPAWN_RULE: 'echo:spawn_rule',
  SOUND_EVENT: 'echo:sound_event',
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
  LOOT_TABLE_DROPS_ITEM: 'loot_table_drops_item',
  BIOME_HAS_STRUCTURE: 'biome_has_structure',
  BIOME_HAS_FEATURE: 'biome_has_feature',
  STRUCTURE_USES_TEMPLATE_POOL: 'structure_uses_template_pool',
  SPAWN_RULE_SPAWNS_ENTITY: 'spawn_rule_spawns_entity',
  MISSION_HAS_OBJECTIVE: 'mission_has_objective',
  OBJECTIVE_TARGETS_NODE: 'objective_targets_node',
  UI_INTENT_CONTROLS_NODE: 'ui_intent_controls_node',
  UI_SURFACE_USES_THEME: 'ui_surface_uses_theme',
  UI_SURFACE_REQUIRES_INPUT: 'ui_surface_requires_input',
  UI_SURFACE_DISPATCHES_ACTION: 'ui_surface_dispatches_action',
  HUD_WIDGET_READS_SESSION_STATE: 'hud_widget_reads_session_state',
  INVENTORY_ACTION_INVOKES_GAMEPLAY_ACTION: 'inventory_action_invokes_gameplay_action',
  TERMINAL_PAGE_CONTROLS_NODE: 'terminal_page_controls_node',
  INDEX_PAGE_DOCUMENTS_NODE: 'index_page_documents_node',
  RUNTIME_HOST_ADAPTS_SURFACE: 'runtime_host_adapts_surface',
  TRIGGER_INVOKES_EFFECT: 'trigger_invokes_effect',
  REGION_CONTAINS_TRIGGER: 'region_contains_trigger',
  SETTING_AFFECTS_SYSTEM: 'setting_affects_system',
  SYSTEM_DECLARES_CAPABILITY: 'system_declares_capability',
}

const RUNTIME_TARGETS = ['neoforge', 'echo_native', 'echo_runtime_standalone', 'standalone_engine', 'hytale']
const PLAYER_HOST_TARGETS = ['neoforge', 'echo_native', 'echo_runtime_standalone', 'standalone_engine']
const EXPORT_PLAN_STATUSES = ['direct', 'adapter_required', 'fallback', 'blocked', 'not_applicable']
const UI_FEATURE_EDGE_KINDS = [
  EDGE_KINDS.UI_INTENT_CONTROLS_NODE,
  EDGE_KINDS.TERMINAL_PAGE_CONTROLS_NODE,
  EDGE_KINDS.INDEX_PAGE_DOCUMENTS_NODE,
  EDGE_KINDS.UI_SURFACE_DISPATCHES_ACTION,
  EDGE_KINDS.INVENTORY_ACTION_INVOKES_GAMEPLAY_ACTION,
]

const UI_INTENTS = [
  'selection_menu',
  'detail_panel',
  'notification',
  'terminal_page',
  'index_page',
  'title_menu',
  'pause_menu',
  'world_create',
  'world_load',
  'module_diagnostics',
  'inventory_surface',
  'crafting_surface',
  'hotbar_surface',
  'hud_widget',
  'overlay',
  'keybind_action',
  'mission_tracker',
  'death_respawn',
  'save_warning',
  'runtime_blocker',
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

function portableDisplayName(value) {
  let name = String(value ?? '').trim()
  if (!name) return name
  name = name.replace(/BlockEntity/g, 'Block Entity')
  if (/Screen\s*$/.test(name)) name = `${name} View`
  if (/Menu\s*$/.test(name)) name = `${name} Surface`
  return name
}

function makeNode({ kind, id, moduleId, displayName, source, data = {}, provenance = {}, extra = {} }) {
  const node = {
    schemaVersion: NODE_SCHEMA,
    kind,
    id,
    moduleId,
    displayName: portableDisplayName(displayName ?? id),
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

function uiRuntimeHints(uiId, baseHints = {}) {
  const hints = Object.fromEntries(RUNTIME_TARGETS.map((target) => [target, { ...(baseHints[target] ?? {}) }]))
  for (const target of PLAYER_HOST_TARGETS) {
    hints[target] = { ...(hints[target] ?? {}), id: uiId }
  }
  return hints
}

function pushUiHostAdaptationEdges(edges, uiId, moduleId) {
  for (const target of PLAYER_HOST_TARGETS) {
    edges.push(makeEdge({
      kind: EDGE_KINDS.RUNTIME_HOST_ADAPTS_SURFACE,
      from: `echo:runtime/${target}`,
      to: uiId,
      moduleId,
      data: { hostId: target, contract: 'echo.ui.surface.v1' },
    }))
  }
}

function asArray(value) {
  if (value === undefined || value === null) return []
  return Array.isArray(value) ? value : [value]
}

function pushExplicitUiControlEdges(edges, uiId, moduleId, intent, source) {
  const controlledNodes = [
    ...asArray(source.controlledNode),
    ...asArray(source.controlledNodes),
    ...asArray(source.controlsNode),
    ...asArray(source.controlsNodes),
  ].filter(Boolean)
  const documentedNodes = [
    ...asArray(source.documentedNode),
    ...asArray(source.documentedNodes),
    ...asArray(source.documentsNode),
    ...asArray(source.documentsNodes),
  ].filter(Boolean)
  for (const targetId of controlledNodes) {
    edges.push(makeEdge({
      kind: intent === 'terminal_page' ? EDGE_KINDS.TERMINAL_PAGE_CONTROLS_NODE : EDGE_KINDS.UI_INTENT_CONTROLS_NODE,
      from: uiId,
      to: String(targetId),
      moduleId,
    }))
  }
  for (const targetId of documentedNodes) {
    edges.push(makeEdge({
      kind: intent === 'index_page' ? EDGE_KINDS.INDEX_PAGE_DOCUMENTS_NODE : EDGE_KINDS.UI_INTENT_CONTROLS_NODE,
      from: uiId,
      to: String(targetId),
      moduleId,
    }))
  }
}

function pushPlayerSurfaceManifest({ filePath, payload, moduleId, nodes, edges }) {
  if (payload?.schemaVersion !== PLAYER_SURFACE_MANIFEST_SCHEMA) return false
  const ownerModule = payload.ownerModule || moduleId
  const hostTargets = Array.isArray(payload.hostTargets) && payload.hostTargets.length > 0
    ? payload.hostTargets.filter((target) => PLAYER_HOST_TARGETS.includes(target))
    : PLAYER_HOST_TARGETS
  const source = sourceFor(filePath)

  for (const surface of payload.surfaces ?? []) {
    if (!surface?.id) continue
    const uiId = surface.id
    const intent = inferUiIntent(uiId, surface.intent)
    const capabilities = surface.capabilities || [surface.surface || inferSurfaceFromIntent(intent)]
    const requiredHostServices = [
      ...new Set([...(payload.requiredHostServices ?? []), ...(surface.requiredHostServices ?? [])]),
    ]

    nodes.push(makeNode({
      kind: NODE_KINDS.UI_INTENT,
      id: uiId,
      moduleId,
      displayName: surface.title || displayNameFromLocalId(uiId.split(':')[1] || uiId),
      source,
      data: {
        surface: surface.surface || inferSurfaceFromIntent(intent),
        route: uiId,
        capabilities,
        contract: surface.contract,
        ownerModule,
        requiredHostServices,
        dataProviders: surface.dataProviders ?? [],
      },
      extra: {
        intent,
        capabilities,
        actions: (surface.actions || []).map((a) => ({ id: a.id, label: a.label || a.id, requires: a.requires })),
        fallbacks: {
          neoforge: surface.fallbacks?.neoforge || 'custom_screen',
          echo_native: surface.fallbacks?.echo_native || 'native_panel',
          echo_runtime_standalone: surface.fallbacks?.echo_runtime_standalone || 'native_panel',
          standalone_engine: surface.fallbacks?.standalone_engine || 'engine_panel',
        },
        runtimeHints: uiRuntimeHints(uiId),
      },
    }))

    for (const target of hostTargets) {
      edges.push(makeEdge({
        kind: EDGE_KINDS.RUNTIME_HOST_ADAPTS_SURFACE,
        from: `echo:runtime/${target}`,
        to: uiId,
        moduleId,
        data: { hostId: target, contract: surface.contract || 'echo.ui.surface.v1', ownerModule },
      }))
    }

    for (const tokenId of surface.themeTokens ?? []) {
      nodes.push(makeNode({
        kind: NODE_KINDS.ASSET,
        id: tokenId,
        moduleId,
        displayName: displayNameFromLocalId(tokenId.split(':')[1] || tokenId),
        source,
        data: { assetKind: 'theme_token', contract: 'echo.theme.tokens.v1', ownerModule },
      }))
      edges.push(makeEdge({ kind: EDGE_KINDS.UI_SURFACE_USES_THEME, from: uiId, to: tokenId, moduleId }))
    }

    for (const bindingId of surface.inputBindings ?? []) {
      nodes.push(makeNode({
        kind: NODE_KINDS.SYSTEM,
        id: bindingId,
        moduleId,
        displayName: displayNameFromLocalId(bindingId.split(':')[1] || bindingId),
        source,
        data: { systemType: 'input_binding', contract: 'echo.input.binding.v1', ownerModule },
      }))
      edges.push(makeEdge({ kind: EDGE_KINDS.UI_SURFACE_REQUIRES_INPUT, from: uiId, to: bindingId, moduleId }))
    }

    const actionIds = [
      ...(surface.gameplayActions ?? []),
      ...(surface.actions ?? []).map((action) => action.action).filter(Boolean),
    ]
    for (const actionId of [...new Set(actionIds)]) {
      nodes.push(makeNode({
        kind: NODE_KINDS.SYSTEM,
        id: actionId,
        moduleId,
        displayName: displayNameFromLocalId(actionId.split(':')[1] || actionId),
        source,
        data: { systemType: 'player_action', contract: surface.contract || 'echo.gameplay.action.v1', ownerModule },
      }))
      edges.push(makeEdge({ kind: EDGE_KINDS.UI_SURFACE_DISPATCHES_ACTION, from: uiId, to: actionId, moduleId }))
      if (['inventory_surface', 'crafting_surface', 'hotbar_surface', 'inventory_action'].includes(intent)) {
        edges.push(makeEdge({ kind: EDGE_KINDS.INVENTORY_ACTION_INVOKES_GAMEPLAY_ACTION, from: uiId, to: actionId, moduleId }))
      }
    }

    for (const stateId of surface.sessionState ?? []) {
      nodes.push(makeNode({
        kind: NODE_KINDS.SYSTEM,
        id: stateId,
        moduleId,
        displayName: displayNameFromLocalId(stateId.split(':')[1] || stateId),
        source,
        data: { systemType: 'session_state', contract: 'echo.save.session.v1', ownerModule },
      }))
      edges.push(makeEdge({ kind: EDGE_KINDS.HUD_WIDGET_READS_SESSION_STATE, from: uiId, to: stateId, moduleId }))
    }

    pushExplicitUiControlEdges(edges, uiId, moduleId, intent, {
      controlledNodes: surface.controlledNodes,
      documentedNodes: surface.documentedNodes,
    })
  }
  return true
}

function descriptorUiRegistrations(descriptor) {
  const moduleId = descriptor.id
  const provides = new Set((descriptor.provides ?? []).map(String))
  const permissions = new Set((descriptor.permissions ?? []).map(String))
  const role = String(descriptor.role ?? '')
  const registrations = []

  if (provides.has('terminal.surface') || role.includes('terminal')) {
    registrations.push({
      localId: 'ui/terminal',
      displayName: `${descriptor.name ?? moduleId} Terminal`,
      intent: 'terminal_page',
      surface: 'terminal',
      capabilities: ['terminal', 'screen'],
    })
  }

  if ([...provides].some((item) => item.startsWith('lens.')) || role.includes('scanner')) {
    registrations.push({
      localId: 'ui/lens_overlay',
      displayName: `${descriptor.name ?? moduleId} Lens Overlay`,
      intent: 'scanner_result',
      surface: 'lens_overlay',
      capabilities: ['lens', 'hud_overlay', 'scanner'],
    })
  }

  if ([...provides].some((item) => item.startsWith('hud.')) || permissions.has('hud.widgets') || role.includes('hud')) {
    registrations.push({
      localId: 'ui/hud_overlay',
      displayName: `${descriptor.name ?? moduleId} HUD Overlay`,
      intent: 'progress_tracker',
      surface: 'hud_overlay',
      capabilities: ['hud', 'hud_overlay'],
    })
  }

  return registrations
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
    nodes.push(makeNode({
      kind: NODE_KINDS.MODULE,
      id: nodeId(depId, 'module'),
      moduleId: depId,
      displayName: depId,
      source: {
        repo: 'ECHO-Modules',
        path: path.relative(process.cwd(), entry.descriptorPath).replace(/\\/g, '/'),
        format: 'json',
      },
      data: { dependencyOf: moduleId, optional: false, externalReference: true },
    }))
    edges.push(makeEdge({
      kind: EDGE_KINDS.MODULE_REQUIRES_MODULE,
      from: nodeId(moduleId, 'module'),
      to: nodeId(depId, 'module'),
      moduleId,
      data: { optional: false },
    }))
  }
  for (const depId of descriptor.optional ?? []) {
    nodes.push(makeNode({
      kind: NODE_KINDS.MODULE,
      id: nodeId(depId, 'module'),
      moduleId: depId,
      displayName: depId,
      source: {
        repo: 'ECHO-Modules',
        path: path.relative(process.cwd(), entry.descriptorPath).replace(/\\/g, '/'),
        format: 'json',
      },
      data: { dependencyOf: moduleId, optional: true, externalReference: true },
    }))
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

  for (const ui of descriptorUiRegistrations(descriptor)) {
    const uiId = nodeId(moduleId, ui.localId)
    nodes.push(makeNode({
      kind: NODE_KINDS.UI_INTENT,
      id: uiId,
      moduleId,
      displayName: ui.displayName,
      source: {
        repo: 'ECHO-Modules',
        path: path.relative(process.cwd(), entry.descriptorPath).replace(/\\/g, '/'),
        format: 'json',
      },
      data: {
        surface: ui.surface,
        route: uiId,
        capabilities: ui.capabilities,
        descriptorRole: descriptor.role,
        providedCapabilities: descriptor.provides ?? [],
      },
      extra: {
        intent: ui.intent,
        capabilities: ui.capabilities,
        runtimeHints: uiRuntimeHints(uiId, runtimeHintsFromDescriptor(descriptor)),
      },
    }))
    pushUiHostAdaptationEdges(edges, uiId, moduleId)
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

async function* walkResourceFiles(dir) {
  if (!dir) return
  try {
    const entries = await fs.readdir(dir, { withFileTypes: true, recursive: true })
    for (const entry of entries) {
      if (entry.isFile()) {
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

function sourceForResource(filePath) {
  const ext = path.extname(filePath).toLowerCase()
  return {
    repo: 'ECHO-Modules',
    path: normalizedPath(path.relative(process.cwd(), filePath)),
    format: ext === '.json' ? 'json' : ext === '.java' ? 'java' : ext === '.xml' ? 'xml' : 'generated',
  }
}

async function collectTagMappings(dataDir) {
  const mappings = {
    biomeTags: new Map(),
    structureTags: new Map(),
    placedFeatureTags: new Map(),
  }
  if (!dataDir) return mappings
  for await (const filePath of walkFiles(dataDir)) {
    const normalizedFilePath = normalizedPath(filePath)
    if (!normalizedFilePath.includes('/tags/')) continue
    const ns = namespaceFromPath(filePath, dataDir)
    const relative = localPathAfterNamespace(filePath, dataDir)
    const withoutExt = stripJsonExtension(relative)
    const match = /^tags\/(.+)\/([^/]+)$/.exec(withoutExt)
    if (!match) continue
    const tagType = match[1]
    const tagPath = match[2]
    const tagId = nodeId(ns, tagPath)
    let payload = null
    try {
      payload = await parseContentJson(filePath)
    } catch {
      continue
    }
    const values = Array.isArray(payload?.values) ? payload.values : []
    for (const value of values) {
      let rawId = typeof value === 'string' ? value : value?.id
      if (!rawId) continue
      rawId = rawId.startsWith('#') ? rawId.slice(1) : rawId
      const contentId = rawId.includes(':') ? rawId : nodeId(ns, rawId)
      if (tagType === 'worldgen/biome') {
        if (!mappings.biomeTags.has(contentId)) mappings.biomeTags.set(contentId, [])
        if (!mappings.biomeTags.get(contentId).includes(tagId)) mappings.biomeTags.get(contentId).push(tagId)
      } else if (tagType === 'worldgen/structure') {
        if (!mappings.structureTags.has(contentId)) mappings.structureTags.set(contentId, [])
        if (!mappings.structureTags.get(contentId).includes(tagId)) mappings.structureTags.get(contentId).push(tagId)
      } else if (tagType === 'worldgen/placed_feature') {
        if (!mappings.placedFeatureTags.has(contentId)) mappings.placedFeatureTags.set(contentId, [])
        if (!mappings.placedFeatureTags.get(contentId).includes(tagId)) mappings.placedFeatureTags.get(contentId).push(tagId)
      }
    }
  }
  return mappings
}

async function findEntityAssetPaths(assetsDir, ns, localId) {
  const result = {}
  if (!assetsDir) return result
  const namespaceDir = path.join(assetsDir, ns)
  const textureCandidates = [
    path.join(namespaceDir, 'textures', 'entity', `${localId}.png`),
    path.join(namespaceDir, 'textures', 'entity', `${localId}.png.png`),
  ]
  const modelCandidates = [
    path.join(namespaceDir, 'models', 'entity', `${localId}.json`),
    path.join(namespaceDir, 'geo', 'entity', `${localId}.json`),
    path.join(namespaceDir, 'models', `${localId}.json`),
  ]
  const animationCandidates = [
    path.join(namespaceDir, 'animations', 'entity', `${localId}.json`),
    path.join(namespaceDir, 'animations', `${localId}.json`),
  ]

  const entityTexturesDir = path.join(namespaceDir, 'textures', 'entity', localId)
  try {
    const entries = await fs.readdir(entityTexturesDir, { withFileTypes: true })
    for (const entry of entries.sort((a, b) => a.name.localeCompare(b.name))) {
      if (entry.isFile() && entry.name.endsWith('.png')) {
        textureCandidates.push(path.join(entityTexturesDir, entry.name))
      }
    }
  } catch {
    // ignore missing directory
  }

  const entityBaseDir = path.join(namespaceDir, 'textures', 'entity')
  try {
    const entries = await fs.readdir(entityBaseDir, { withFileTypes: true })
    for (const entry of entries) {
      if (entry.isDirectory() && entry.name !== localId) {
        const candidate = path.join(entityBaseDir, entry.name, `${localId}.png`)
        textureCandidates.push(candidate)
      }
    }
  } catch {
    // ignore missing directory
  }

  for (const candidate of textureCandidates) {
    if (await fileExists(candidate)) {
      result.texturePath = normalizedPath(path.relative(process.cwd(), candidate))
      break
    }
  }
  for (const candidate of modelCandidates) {
    if (await fileExists(candidate)) {
      result.modelPath = normalizedPath(path.relative(process.cwd(), candidate))
      break
    }
  }
  for (const candidate of animationCandidates) {
    if (await fileExists(candidate)) {
      result.animationPath = normalizedPath(path.relative(process.cwd(), candidate))
      break
    }
  }
  return result
}

function inferThreatFromCategory(category, builderBody = '', entityData = {}) {
  const threat = {}
  const explicitFields = ['threat', 'threatClass', 'threatProfile', 'threatLevel', 'dangerLevel', 'hostility', 'hostilityLevel']
  for (const field of explicitFields) {
    if (entityData[field] !== undefined) threat[field] = entityData[field]
  }
  const threatCall = /\.(?:threat|threatClass|threatProfile|threatLevel|dangerLevel|hostility|hostilityLevel)\s*\(\s*"([^"]+)"\s*\)/.exec(builderBody)
  if (threatCall && !threat.hostility) {
    threat.hostility = threatCall[1]
  }
  if (Object.keys(threat).length > 0) return threat

  const cat = String(category).toLowerCase()
  if (cat === 'monster') threat.hostility = 'hostile'
  else if (cat === 'creature') threat.hostility = 'neutral'
  else if (cat === 'ambient') threat.hostility = 'passive'
  else if (cat === 'water_creature') threat.hostility = 'neutral'
  else threat.hostility = 'neutral'
  return threat
}

function categoryToRuleType(category) {
  const map = {
    monster: 'monster',
    creature: 'ground_mob',
    ambient: 'no_restrictions',
    misc: 'no_restrictions',
    water_creature: 'ground_mob',
    underground_water_creature: 'ground_mob',
    water_ambient: 'no_restrictions',
    axolotls: 'ground_mob',
  }
  return map[String(category).toLowerCase()] || 'no_restrictions'
}

function normalizeBiomesValue(value, defaultNs) {
  if (!value) return []
  if (typeof value === 'string') {
    const id = value.startsWith('#') ? value.slice(1) : value
    return [id.includes(':') ? id : nodeId(defaultNs, id)]
  }
  if (Array.isArray(value)) {
    return value.flatMap((item) => normalizeBiomesValue(item, defaultNs))
  }
  if (value.tag) {
    const tagId = value.tag.includes(':') ? value.tag : nodeId(defaultNs, value.tag)
    return [`#${tagId}`]
  }
  return []
}

async function collectSpawnMetadata(dataDir, ns) {
  const map = new Map()
  if (!dataDir) return map

  for await (const filePath of walkFiles(dataDir)) {
    const normalizedFilePath = normalizedPath(filePath)
    if (!normalizedFilePath.includes('/neoforge/biome_modifier/')) continue
    const payload = await parseContentJson(filePath)
    if (!payload || payload.type !== 'neoforge:add_spawns') continue
    const biomeValues = normalizeBiomesValue(payload.biomes, ns)
    const biomeTags = biomeValues.filter((b) => b.startsWith('#'))
    const biomeIds = biomeValues.filter((b) => !b.startsWith('#'))
    for (const spawner of payload.spawners || []) {
      const entityId = normalizeContentId(spawner.type, ns)
      if (!entityId) continue
      const entry = {
        ruleType: 'monster',
        weight: spawner.weight,
        minGroupSize: spawner.minCount,
        maxGroupSize: spawner.maxCount,
        spawnBiomeTags: biomeTags,
        biomeIds,
      }
      if (!map.has(entityId)) map.set(entityId, [])
      map.get(entityId).push(entry)
    }
  }

  for await (const filePath of walkFiles(dataDir)) {
    const normalizedFilePath = normalizedPath(filePath)
    if (!normalizedFilePath.includes('/worldgen/biome/')) continue
    const payload = await parseContentJson(filePath)
    if (!payload || !payload.spawners) continue
    const localId = stripJsonExtension(localPathAfterNamespace(filePath, dataDir).replace(/^worldgen\/biome\//, ''))
    const biomeId = nodeId(ns, localId)
    for (const [category, spawnerList] of Object.entries(payload.spawners)) {
      if (!Array.isArray(spawnerList)) continue
      for (const spawner of spawnerList) {
        const entityId = normalizeContentId(spawner.type, ns)
        if (!entityId) continue
        const entry = {
          ruleType: categoryToRuleType(category),
          weight: spawner.weight,
          minGroupSize: spawner.minCount,
          maxGroupSize: spawner.maxCount,
          spawnBiomeTags: [],
          biomeIds: [biomeId],
        }
        if (!map.has(entityId)) map.set(entityId, [])
        map.get(entityId).push(entry)
      }
    }
  }

  return map
}

async function collectFeatureBiomes(dataDir, ns) {
  const map = new Map()
  if (!dataDir) return map
  for await (const filePath of walkFiles(dataDir)) {
    const normalizedFilePath = normalizedPath(filePath)
    if (!normalizedFilePath.includes('/worldgen/biome/')) continue
    const payload = await parseContentJson(filePath)
    if (!payload || !Array.isArray(payload.features)) continue
    const localId = stripJsonExtension(localPathAfterNamespace(filePath, dataDir).replace(/^worldgen\/biome\//, ''))
    const biomeId = nodeId(ns, localId)
    for (const stage of payload.features) {
      if (!Array.isArray(stage)) continue
      for (const feature of stage) {
        const featureId = typeof feature === 'string' ? normalizeContentId(feature, ns) : null
        if (!featureId) continue
        const graphFeatureId = nodeId(ns, `feature/placed/${featureId.split(':')[1]}`)
        if (!map.has(graphFeatureId)) map.set(graphFeatureId, new Set())
        map.get(graphFeatureId).add(biomeId)
      }
    }
  }
  const result = new Map()
  for (const [featureId, biomeIds] of map) {
    result.set(featureId, [...biomeIds])
  }
  return result
}

async function collectTemplatePoolBiomes(dataDir, ns, tagMappings) {
  const map = new Map()
  if (!dataDir) return map
  for await (const filePath of walkFiles(dataDir)) {
    const normalizedFilePath = normalizedPath(filePath)
    if (!normalizedFilePath.includes('/worldgen/structure/')) continue
    const payload = await parseContentJson(filePath)
    if (!payload || !payload.start_pool) continue
    const localId = stripJsonExtension(localPathAfterNamespace(filePath, dataDir).replace(/^worldgen\/structure\//, ''))
    const structureContentId = nodeId(ns, localId)
    const poolId = normalizeContentId(payload.start_pool, ns)
    if (!poolId) continue
    const graphPoolId = nodeId(ns, `feature/template_pool/${poolId.split(':')[1]}`)
    const biomeValues = payload.biomes ? normalizeBiomesValue(payload.biomes, ns) : []
    const biomeTags = biomeValues.filter((b) => b.startsWith('#'))
    const structureTags = tagMappings?.structureTags?.get(structureContentId) ?? []
    const allTags = [...new Set([...biomeTags, ...structureTags])]
    if (allTags.length === 0) continue
    if (!map.has(graphPoolId)) map.set(graphPoolId, new Set())
    for (const tag of allTags) map.get(graphPoolId).add(tag)
  }
  const result = new Map()
  for (const [poolId, tags] of map) {
    result.set(poolId, [...tags])
  }
  return result
}

function extractPlacementBlockFromFeaturePayload(payload) {
  if (!payload || typeof payload !== 'object') return null
  const toPlace = payload.config?.to_place
  if (toPlace) {
    if (toPlace.state?.Name) return toPlace.state.Name
    if (Array.isArray(toPlace.entries)) {
      for (const entry of toPlace.entries) {
        if (entry?.data?.Name) return entry.data.Name
      }
    }
  }
  const stateProvider = payload.config?.state_provider
  if (stateProvider) {
    if (stateProvider.state?.Name) return stateProvider.state.Name
    if (Array.isArray(stateProvider.entries)) {
      for (const entry of stateProvider.entries) {
        if (entry?.data?.Name) return entry.data.Name
      }
    }
  }
  if (payload.config?.fluid?.state?.Name) return payload.config.fluid.state.Name
  if (payload.config?.barrier?.state?.Name) return payload.config.barrier.state.Name
  if (payload.config?.state?.Name) return payload.config.state.Name
  if (payload.config?.target_state?.Name) return payload.config.target_state.Name
  if (payload.config?.contents?.Name) return payload.config.contents.Name
  if (Array.isArray(payload.config?.targets)) {
    for (const target of payload.config.targets) {
      if (target?.state?.Name) return target.state.Name
    }
  }
  if (payload.config?.trunk_provider?.state?.Name) return payload.config.trunk_provider.state.Name
  if (payload.config?.foliage_provider?.state?.Name) return payload.config.foliage_provider.state.Name
  if (payload.config?.default_block?.Name) return payload.config.default_block.Name
  return null
}

async function resolveConfiguredFeatureBlock(dataDir, ns, configuredFeatureId) {
  if (!dataDir || !configuredFeatureId) return null
  const [featureNs, localId] = configuredFeatureId.split(':')
  if (!featureNs || !localId) return null
  const filePath = path.join(dataDir, featureNs, 'worldgen', 'configured_feature', `${localId}.json`)
  const payload = await parseContentJson(filePath)
  if (!payload) return null
  return extractPlacementBlockFromFeaturePayload(payload)
}

async function extractWorldgenHints(kind, payload, tagMappings, featureBiomes, templatePoolBiomes, dataDir, contentId, graphNodeId, ns) {
  const hints = {}
  if (kind === NODE_KINDS.BIOME) {
    if (payload.effects) hints.effects = payload.effects
  } else if (kind === NODE_KINDS.STRUCTURE) {
    if (payload.step) hints.step = payload.step
    if (payload.terrain_adaptation !== undefined) hints.terrainAdaptation = payload.terrain_adaptation
    if (payload.start_pool) hints.templatePoolId = payload.start_pool
    if (payload.biomes) {
      const biomeValues = normalizeBiomesValue(payload.biomes, ns)
      hints.spawnBiomeTags = biomeValues.filter((b) => b.startsWith('#'))
      hints.biomeTags = hints.spawnBiomeTags
    }
  } else if (kind === NODE_KINDS.FEATURE) {
    if (payload.type) hints.featureType = payload.type
    const directBlock = extractPlacementBlockFromFeaturePayload(payload)
    if (directBlock) hints.placementBlockId = directBlock
    if (payload.feature) {
      if (typeof payload.feature === 'string') {
        hints.configuredFeatureId = payload.feature
      } else if (payload.feature.feature) {
        hints.configuredFeatureId = payload.feature.feature
      } else if (payload.feature.config) {
        const inlineBlock = extractPlacementBlockFromFeaturePayload(payload.feature)
        if (inlineBlock && !hints.placementBlockId) hints.placementBlockId = inlineBlock
      }
    }
    if (!hints.placementBlockId && hints.configuredFeatureId) {
      const resolved = await resolveConfiguredFeatureBlock(dataDir, ns, hints.configuredFeatureId)
      if (resolved) hints.placementBlockId = resolved
    }
    if (hints.configuredFeatureId && !hints.placedFeatureId && graphNodeId.startsWith(`${ns}:feature/placed/`)) {
      hints.placedFeatureId = graphNodeId
    }
    const biomeIds = featureBiomes?.get(graphNodeId)
    if (biomeIds && biomeIds.length > 0) {
      hints.spawnBiomeTags = biomeIds
      hints.biomeTags = biomeIds
    }
    if (graphNodeId.startsWith(`${ns}:feature/template_pool/`)) {
      const templatePoolTags = templatePoolBiomes?.get(graphNodeId)
      if (templatePoolTags && templatePoolTags.length > 0) {
        hints.templatePoolId = graphNodeId
        hints.spawnBiomeTags = hints.spawnBiomeTags ? [...new Set([...hints.spawnBiomeTags, ...templatePoolTags])] : templatePoolTags
        hints.biomeTags = hints.biomeTags ? [...new Set([...hints.biomeTags, ...templatePoolTags])] : templatePoolTags
      }
    }
  }
  if (kind === NODE_KINDS.BIOME && tagMappings?.biomeTags) {
    const tags = tagMappings.biomeTags.get(contentId)
    if (tags && tags.length > 0) hints.biomeTags = tags
  } else if (kind === NODE_KINDS.STRUCTURE && tagMappings?.structureTags) {
    const tags = tagMappings.structureTags.get(contentId)
    if (tags && tags.length > 0) hints.biomeTags = hints.biomeTags ? [...new Set([...hints.biomeTags, ...tags])] : tags
  } else if (kind === NODE_KINDS.FEATURE && tagMappings?.placedFeatureTags) {
    const tags = tagMappings.placedFeatureTags.get(contentId)
    if (tags && tags.length > 0) hints.biomeTags = hints.biomeTags ? [...new Set([...hints.biomeTags, ...tags])] : tags
  }
  return hints
}

function normalizeContentId(raw, defaultNs) {
  if (!raw || typeof raw !== 'string') return null
  const id = raw.startsWith('#') ? raw.slice(1) : raw
  if (id.includes(':')) return id
  return nodeId(defaultNs, id)
}

function localPathAfterNamespace(filePath, baseDir) {
  const parts = path.relative(baseDir, filePath).split(path.sep)
  return normalizedPath(parts.slice(1).join('/'))
}

function stripJsonExtension(value) {
  return String(value).replace(/\.json$/i, '')
}

function stripResourceExtension(value) {
  return String(value).replace(/\.[^.]+$/i, '')
}

function displayNameFromLocalId(value) {
  return String(value)
    .split(/[/:_.-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
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

function* collectLootEntryItems(entries, ns) {
  if (!Array.isArray(entries)) return
  for (const entry of entries) {
    if (!entry || typeof entry !== 'object') continue
    if ((entry.type === 'minecraft:item' || entry.type === 'item') && entry.name) {
      const itemId = normalizeContentId(entry.name, ns)
      if (itemId) yield itemId
    }
    if (Array.isArray(entry.children)) yield* collectLootEntryItems(entry.children, ns)
    if (Array.isArray(entry.entries)) yield* collectLootEntryItems(entry.entries, ns)
  }
}

function extractMinecraftLootTable(filePath, payload, ns, moduleId, dataDir, nodes, edges) {
  const relativePath = localPathAfterNamespace(filePath, dataDir)
  const lootRelative = stripJsonExtension(relativePath.replace(/^loot_tables?\//, ''))
  const lootTableId = nodeId(ns, lootRelative)
  const lootNodeId = nodeId(ns, `loot/${lootRelative}`)
  const isBlock = payload.type === 'minecraft:block' || /[\\/]blocks[\\/]/.test(filePath)
  const isEntity = payload.type === 'minecraft:entity' || payload.type === 'entity' || /[\\/]entities[\\/]/.test(filePath)
  const localName = path.basename(filePath, '.json')
  const blockId = isBlock ? nodeId(ns, localName) : null
  const entityId = isEntity ? nodeId(ns, localName) : null
  nodes.push(makeNode({
    kind: NODE_KINDS.LOOT_TABLE,
    id: lootNodeId,
    moduleId,
    displayName: displayNameFromLocalId(lootRelative),
    source: sourceFor(filePath),
    data: {
      lootTableId,
      lootPath: lootRelative,
      lootType: payload.type ?? (isBlock ? 'minecraft:block' : isEntity ? 'minecraft:entity' : 'unknown'),
      targetBlockId: blockId,
      targetEntityId: entityId,
      poolCount: Array.isArray(payload.pools) ? payload.pools.length : 0,
    },
  }))
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
  if (entityId) {
    nodes.push(makeNode({
      kind: NODE_KINDS.ENTITY,
      id: entityId,
      moduleId,
      displayName: displayNameFromLocalId(localName),
      source: sourceFor(filePath),
      data: { lootTableId, entityId },
    }))
  }
  const droppedItemIds = new Set()
  for (const pool of payload.pools || []) {
    for (const itemId of collectLootEntryItems(pool.entries, ns)) {
      droppedItemIds.add(itemId)
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
  for (const itemId of droppedItemIds) {
    edges.push(makeEdge({
      kind: EDGE_KINDS.LOOT_TABLE_DROPS_ITEM,
      from: lootNodeId,
      to: itemId,
      moduleId,
      data: { lootTableId },
    }))
  }
}

async function extractWorldgenNode(filePath, payload, ns, moduleId, dataDir, tagMappings, featureBiomes, templatePoolBiomes, nodes) {
  const relativePath = localPathAfterNamespace(filePath, dataDir)
  const localPath = stripJsonExtension(relativePath)
  let kind = null
  let localId = null
  let data = {}
  let contentId = null

  if (localPath.startsWith('worldgen/biome/')) {
    kind = NODE_KINDS.BIOME
    localId = localPath.replace(/^worldgen\/biome\//, '')
    contentId = nodeId(ns, localId)
    data = { biomeId: contentId, worldgenType: 'biome', definition: payload }
  } else if (localPath.startsWith('worldgen/structure/')) {
    kind = NODE_KINDS.STRUCTURE
    localId = localPath.replace(/^worldgen\/structure\//, '')
    contentId = nodeId(ns, localId)
    data = { structureId: contentId, worldgenType: 'structure', definition: payload }
  } else if (localPath.startsWith('worldgen/configured_feature/')) {
    kind = NODE_KINDS.FEATURE
    localId = `configured/${localPath.replace(/^worldgen\/configured_feature\//, '')}`
    contentId = nodeId(ns, localId)
    data = { featureId: contentId, worldgenType: 'configured_feature', definition: payload }
  } else if (localPath.startsWith('worldgen/placed_feature/')) {
    kind = NODE_KINDS.FEATURE
    localId = `placed/${localPath.replace(/^worldgen\/placed_feature\//, '')}`
    contentId = nodeId(ns, localId)
    data = { featureId: contentId, worldgenType: 'placed_feature', definition: payload }
  } else if (localPath.startsWith('worldgen/template_pool/')) {
    kind = NODE_KINDS.FEATURE
    localId = `template_pool/${localPath.replace(/^worldgen\/template_pool\//, '')}`
    contentId = nodeId(ns, localId)
    data = { featureId: contentId, worldgenType: 'template_pool', definition: payload }
  } else if (localPath.startsWith('worldgen/world_preset/')) {
    kind = NODE_KINDS.REGION
    localId = `world_preset/${localPath.replace(/^worldgen\/world_preset\//, '')}`
    contentId = nodeId(ns, localId)
    data = { regionType: 'world_preset', presetId: contentId, definition: payload }
  }

  if (!kind || !localId) return
  const nodeLocalId = kind === NODE_KINDS.BIOME
    ? `biome/${localId}`
    : kind === NODE_KINDS.STRUCTURE
      ? `structure/${localId}`
      : kind === NODE_KINDS.FEATURE
        ? `feature/${localId}`
        : `region/${localId}`
  const graphNodeId = nodeId(ns, nodeLocalId)
  const hints = await extractWorldgenHints(kind, payload, tagMappings, featureBiomes, templatePoolBiomes, dataDir, contentId, graphNodeId, ns)
  Object.assign(data, hints)
  nodes.push(makeNode({
    kind,
    id: nodeId(ns, nodeLocalId),
    moduleId,
    displayName: payload.displayName || payload.name || displayNameFromLocalId(localId),
    source: sourceFor(filePath),
    data,
  }))
}

function extractStructureAsset(filePath, ns, moduleId, dataDir, nodes) {
  const relativePath = localPathAfterNamespace(filePath, dataDir)
  if (!/^structures?\//.test(relativePath)) return
  const localPath = stripResourceExtension(relativePath.replace(/^structures?\//, ''))
  nodes.push(makeNode({
    kind: NODE_KINDS.STRUCTURE,
    id: nodeId(ns, `structure_asset/${localPath}`),
    moduleId,
    displayName: displayNameFromLocalId(localPath),
    source: sourceForResource(filePath),
    data: {
      structureId: nodeId(ns, localPath),
      worldgenType: 'nbt_template',
      assetPath: normalizedPath(path.relative(process.cwd(), filePath)),
    },
  }))
}

function extractSoundEvents(filePath, payload, ns, moduleId, nodes) {
  for (const [soundName, sound] of Object.entries(payload ?? {})) {
    if (!sound || typeof sound !== 'object') continue
    const soundEventId = nodeId(ns, soundName)
    nodes.push(makeNode({
      kind: NODE_KINDS.SOUND_EVENT,
      id: nodeId(ns, `sound/${soundName}`),
      moduleId,
      displayName: sound.subtitle || displayNameFromLocalId(soundName),
      source: sourceFor(filePath),
      data: {
        soundEventId,
        subtitle: sound.subtitle,
        sounds: Array.isArray(sound.sounds) ? sound.sounds : [],
      },
    }))
  }
}

function extractMissionObject(filePath, mission, ns, moduleId, nodes, edges) {
  if (!mission || typeof mission !== 'object') return
  const rawId = mission.id || mission.advancement || mission.title || path.basename(filePath, '.json')
  if (!rawId) return
  const missionId = rawId.includes(':') ? rawId : nodeId(ns, `mission/${rawId}`)
  const prerequisiteObjectiveIds = []
  for (const prereq of mission.prerequisites ?? []) {
    const prereqId = typeof prereq === 'string' ? prereq : prereq?.id
    if (!prereqId) continue
    if (String(prereqId).includes('/')) {
      const normalized = prereqId.includes(':') ? prereqId : nodeId(ns, `objective/${prereqId}`)
      prerequisiteObjectiveIds.push(normalized)
    }
  }
  const rewardItemIds = (mission.rewards ?? [])
    .map((reward) => (typeof reward === 'string' ? reward : reward?.item))
    .filter(Boolean)
  nodes.push(makeNode({
    kind: NODE_KINDS.MISSION,
    id: missionId,
    moduleId,
    displayName: mission.title || displayNameFromLocalId(rawId),
    source: sourceFor(filePath),
    data: {
      chapterId: mission.chapterId,
      phase: mission.phase,
      phaseTitle: mission.phaseTitle,
      order: mission.order,
      category: mission.category,
      difficulty: mission.difficulty,
      icon: mission.icon,
      kind: mission.kind,
      prerequisites: mission.prerequisites ?? [],
      guidanceLinks: mission.guidanceLinks ?? {},
      nativeHooks: mission.nativeHooks ?? {},
      requirements: mission.requirements ?? [],
      rewards: mission.rewards ?? [],
      rewardItemIds,
      triggerId: mission.triggerId || mission.trigger,
    },
  }))
  const objectives = Array.isArray(mission.objectives) ? mission.objectives : []
  if (objectives.length === 0) {
    const objectiveId = nodeId(ns, `objective/${missionId.split(':')[1]}/complete`)
    nodes.push(makeNode({
      kind: NODE_KINDS.OBJECTIVE,
      id: objectiveId,
      moduleId,
      displayName: `Complete ${mission.title || rawId}`,
      source: sourceFor(filePath),
      data: {
        objectiveType: 'complete_trigger',
        prerequisiteObjectiveIds,
        triggerId: mission.triggerId || mission.trigger,
      },
    }))
    edges.push(makeEdge({
      kind: EDGE_KINDS.MISSION_HAS_OBJECTIVE,
      from: missionId,
      to: objectiveId,
      moduleId,
    }))
    return
  }
  for (const objective of objectives) {
    const rawObjectiveId = objective.id || `${missionId.split(':')[1]}/main`
    const objectiveId = rawObjectiveId.includes(':') ? rawObjectiveId : nodeId(ns, `objective/${rawObjectiveId}`)
    nodes.push(makeNode({
      kind: NODE_KINDS.OBJECTIVE,
      id: objectiveId,
      moduleId,
      displayName: objective.label || objective.title || displayNameFromLocalId(rawObjectiveId),
      source: sourceFor(filePath),
      data: {
        objectiveType: OBJECTIVE_TYPES.includes(objective.type) ? objective.type : (objective.type || 'complete_trigger'),
        detail: objective.detail,
        icon: objective.icon,
        target: objective.target,
        interactionTarget: objective.target,
        required: objective.required,
        criteria: objective.criteria ?? {},
        prerequisiteObjectiveIds,
        rewardItemIds,
        triggerId: objective.triggerId || objective.trigger || mission.triggerId || mission.trigger,
      },
    }))
    edges.push(makeEdge({
      kind: EDGE_KINDS.MISSION_HAS_OBJECTIVE,
      from: missionId,
      to: objectiveId,
      moduleId,
    }))
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

async function extractJavaEntityRegistrations(source, ns, moduleId, filePath, spawnEggItemIds, assetsDir, spawnMetadata, nodes, edges) {
  const constantToEntity = new Map()
  const registerPattern = /public\s+static\s+final\s+EchoBackendRegistryEntry\s*<\s*EntityType\s*<\s*([^>]+?)\s*>\s*>\s+([A-Z0-9_]+)\s*=\s*registerEntityType\s*\(\s*"([^"]+)"\s*,\s*([A-Za-z0-9_:.]+)::new\s*,\s*MobCategory\.([A-Z_]+)\s*,\s*builder\s*->\s*builder([\s\S]*?)\);/g
  for (const match of source.matchAll(registerPattern)) {
    const [, genericType, constantName, localId, factoryClass, category, builderBody] = match
    const entityId = nodeId(ns, localId)
    constantToEntity.set(constantName, { localId, entityId })
    const size = /\.sized\s*\(\s*([0-9.]+)F?\s*,\s*([0-9.]+)F?\s*\)/.exec(builderBody)
    const tracking = /\.clientTrackingRange\s*\(\s*([0-9]+)\s*\)/.exec(builderBody)
    const spawnEggItemId = spawnEggItemIds.has(nodeId(ns, `${localId}_spawn_egg`))
      ? nodeId(ns, `${localId}_spawn_egg`)
      : null
    const assetPaths = await findEntityAssetPaths(assetsDir, ns, localId)
    const threat = inferThreatFromCategory(category, builderBody)
    const meta = spawnMetadata?.get(entityId) ?? []
    const spawnBiomeTags = [...new Set(meta.flatMap((entry) => entry.spawnBiomeTags ?? []))]
    nodes.push(makeNode({
      kind: NODE_KINDS.ENTITY,
      id: entityId,
      moduleId,
      displayName: displayNameFromLocalId(localId),
      source: sourceForResource(filePath),
      data: {
        entityId,
        registryId: entityId,
        javaType: genericType.trim(),
        factoryClass,
        category: category.toLowerCase(),
        width: size ? Number(size[1]) : null,
        height: size ? Number(size[2]) : null,
        clientTrackingRange: tracking ? Number(tracking[1]) : null,
        fireImmune: builderBody.includes('.fireImmune()'),
        spawnEggItemId,
        spawnRules: [],
        spawnBiomeTags,
        visualProfile: spawnEggItemId ? {
          itemModel: `${ns}:item/${localId}_spawn_egg`,
          itemTexture: `${ns}:textures/item/${localId}_spawn_egg.png`,
        } : null,
        ...assetPaths,
        ...threat,
      },
      extra: {
        capabilities: ['entity_registry', category.toLowerCase()],
        runtimeHints: {
          neoforge: { declared: true, registryBridge: 'EchoBackendEntityBridge' },
          echo_native: { declared: true },
          echo_runtime_standalone: { declared: true },
          standalone_engine: { declared: true },
          hytale: { actorAdapter: true },
        },
      },
    }))
  }

  const spawnCalls = [
    ['registerMonsterSpawn', 'monster', 'ON_GROUND', 'MOTION_BLOCKING_NO_LEAVES', 'Monster.checkMonsterSpawnRules'],
    ['registerGroundMobSpawn', 'ground_mob', 'ON_GROUND', 'MOTION_BLOCKING_NO_LEAVES', 'Mob.checkMobSpawnRules'],
    ['registerNoRestrictionSpawn', 'no_restrictions', 'NO_RESTRICTIONS', 'MOTION_BLOCKING_NO_LEAVES', 'always_true'],
  ]
  for (const [method, ruleType, placement, heightmap, predicate] of spawnCalls) {
    const pattern = new RegExp(`${method}\\s*\\(\\s*event\\s*,\\s*([A-Z0-9_]+)\\s*\\)`, 'g')
    for (const match of source.matchAll(pattern)) {
      const entity = constantToEntity.get(match[1])
      if (!entity) continue
      const ruleId = nodeId(ns, `spawn_rule/${entity.localId}`)
      const meta = spawnMetadata?.get(entity.entityId) ?? []
      const matching = meta.find((entry) => entry.ruleType === ruleType) || meta[0]
      const allBiomeTags = [...new Set(meta.flatMap((entry) => entry.spawnBiomeTags ?? []))]
      const allBiomeIds = [...new Set(meta.flatMap((entry) => entry.biomeIds ?? []))]
      const entityNode = nodes.find((node) => node.kind === NODE_KINDS.ENTITY && node.id === entity.entityId)
      if (entityNode) {
        if (!Array.isArray(entityNode.data.spawnRules)) entityNode.data.spawnRules = []
        entityNode.data.spawnRules.push(ruleId)
        if (allBiomeTags.length > 0 || allBiomeIds.length > 0) {
          const existing = new Set(entityNode.data.spawnBiomeTags ?? [])
          for (const tag of allBiomeTags) existing.add(tag)
          for (const biomeId of allBiomeIds) existing.add(biomeId)
          entityNode.data.spawnBiomeTags = [...existing]
        }
      }
      nodes.push(makeNode({
        kind: NODE_KINDS.SPAWN_RULE,
        id: ruleId,
        moduleId,
        displayName: `${displayNameFromLocalId(entity.localId)} Spawn Rule`,
        source: sourceForResource(filePath),
        data: {
          entityId: entity.entityId,
          ruleType,
          placement,
          heightmap,
          predicate,
          spawnBiomeTags: allBiomeTags.length > 0 ? allBiomeTags : (allBiomeIds.length > 0 ? allBiomeIds : undefined),
          weight: matching?.weight,
          minGroupSize: matching?.minGroupSize,
          maxGroupSize: matching?.maxGroupSize,
          safeZoneDistance: 0,
          cooldownSeconds: undefined,
        },
      }))
      edges.push(makeEdge({
        kind: EDGE_KINDS.SPAWN_RULE_SPAWNS_ENTITY,
        from: ruleId,
        to: entity.entityId,
        moduleId,
      }))
    }
  }
}

function inferUiIntent(rawId, requestedIntent = '') {
  if (UI_INTENTS.includes(requestedIntent)) return requestedIntent
  const id = String(rawId ?? '').toLowerCase()
  if (id.includes('title')) return 'title_menu'
  if (id.includes('pause')) return 'pause_menu'
  if (id.includes('world_create') || id.includes('new_world') || id.includes('create_world')) return 'world_create'
  if (id.includes('world_load') || id.includes('load_world') || id.includes('save_list')) return 'world_load'
  if (id.includes('diagnostic') || id.includes('module_status')) return 'module_diagnostics'
  if (id.includes('runtime_blocker') || id.includes('blocker')) return 'runtime_blocker'
  if (id.includes('death') || id.includes('respawn')) return 'death_respawn'
  if (id.includes('save_warning') || id.includes('migration_warning')) return 'save_warning'
  if (id.includes('terminal')) return 'terminal_page'
  if (id.includes('index') || id.includes('recipe') || id.includes('reference')) return 'index_page'
  if (id.includes('craft')) return 'crafting_surface'
  if (id.includes('hotbar')) return 'hotbar_surface'
  if (id.includes('inventory')) return 'inventory_surface'
  if (id.includes('keybind') || id.includes('input_binding')) return 'keybind_action'
  if (id.includes('lens') || id.includes('scan')) return 'scanner_result'
  if (id.includes('mission') || id.includes('objective')) return 'mission_tracker'
  if (id.includes('hud') || id.includes('vital') || id.includes('meter')) return 'hud_widget'
  if (id.includes('overlay')) return 'overlay'
  if (id.includes('settings')) return 'settings_panel'
  if (id.includes('search')) return 'selection_menu'
  return 'detail_panel'
}

function inferSurfaceFromIntent(intent) {
  switch (intent) {
    case 'terminal_page': return 'terminal'
    case 'index_page': return 'index'
    case 'title_menu': return 'screen'
    case 'pause_menu': return 'screen'
    case 'world_create': return 'screen'
    case 'world_load': return 'screen'
    case 'module_diagnostics': return 'screen'
    case 'runtime_blocker': return 'screen'
    case 'death_respawn': return 'screen'
    case 'save_warning': return 'modal'
    case 'inventory_surface': return 'inventory'
    case 'crafting_surface': return 'crafting'
    case 'hotbar_surface': return 'hotbar'
    case 'hud_widget': return 'hud_overlay'
    case 'overlay': return 'hud_overlay'
    case 'keybind_action': return 'input'
    case 'mission_tracker': return 'hud_overlay'
    case 'scanner_result': return 'lens_overlay'
    case 'progress_tracker': return 'hud_overlay'
    case 'selection_menu': return 'index'
    case 'notification': return 'hud_overlay'
    case 'inventory_action': return 'screen'
    case 'confirmation_prompt': return 'screen'
    case 'settings_panel': return 'screen'
    default: return 'screen'
  }
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

  const contentNs = inferContentNamespace(descriptor, dataDir)
  const tagMappings = dataDir
    ? await collectTagMappings(dataDir)
    : { biomeTags: new Map(), structureTags: new Map(), placedFeatureTags: new Map() }
  const spawnMetadata = dataDir
    ? await collectSpawnMetadata(dataDir, contentNs)
    : new Map()
  const featureBiomes = dataDir
    ? await collectFeatureBiomes(dataDir, contentNs)
    : new Map()
  const templatePoolBiomes = dataDir
    ? await collectTemplatePoolBiomes(dataDir, contentNs, tagMappings)
    : new Map()

  if (dataDir) {

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

    if (pushPlayerSurfaceManifest({ filePath, payload, moduleId, nodes, edges })) {
      continue
    }

    if (normalizedFilePath.includes('/worldgen/')) {
      await extractWorldgenNode(filePath, payload, ns, moduleId, dataDir, tagMappings, featureBiomes, templatePoolBiomes, nodes)
      continue
    }

    if (normalizedFilePath.includes('/missioncore/missions/')) {
      extractMissionObject(filePath, payload, ns, moduleId, nodes, edges)
      continue
    }

    if (normalizedFilePath.includes('/loot_table/') || normalizedFilePath.includes('/loot_tables/')) {
      extractMinecraftLootTable(filePath, payload, ns, moduleId, dataDir, nodes, edges)
      continue
    }

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
        extractMinecraftLootTable(filePath, payload, ns, moduleId, dataDir, nodes, edges)
      }
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
          const assetPaths = await findEntityAssetPaths(assetsDir, ns, localId.includes(':') ? localId.split(':')[1] : localId)
          const threat = inferThreatFromCategory(entity.category || entity.mobCategory, '', entity)
          const meta = spawnMetadata?.get(entityId) ?? []
          const spawnBiomeTags = [...new Set(meta.flatMap((entry) => entry.spawnBiomeTags ?? []))]
          const spawnBiomeIds = [...new Set(meta.flatMap((entry) => entry.biomeIds ?? []))]
          nodes.push(makeNode({
            kind: NODE_KINDS.ENTITY,
            id: entityId,
            moduleId,
            displayName: entity.displayName || localId,
            source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'json' },
            data: {
              ...entity,
              ...assetPaths,
              ...threat,
              spawnRules: [],
              spawnBiomeTags: spawnBiomeTags.length > 0 ? spawnBiomeTags : (spawnBiomeIds.length > 0 ? spawnBiomeIds : undefined),
            },
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
              const targetId = normalizeContentId(objective.target, ns)
              if (targetId) {
                edges.push(makeEdge({
                  kind: EDGE_KINDS.OBJECTIVE_TARGETS_NODE,
                  from: objectiveId,
                  to: targetId,
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

  for await (const filePath of walkResourceFiles(dataDir)) {
    if (!filePath.toLowerCase().endsWith('.nbt')) continue
    const ns = namespaceFromPath(filePath, dataDir)
    extractStructureAsset(filePath, ns, moduleId, dataDir, nodes)
  }
  }

  const spawnEggItemIds = new Set()

  // Assets scan for UI intent hints and renderable client assets
  for await (const filePath of walkFiles(assetsDir)) {
    const payload = await parseContentJson(filePath)
    if (!payload || typeof payload !== 'object') continue
    const normalizedFilePath = normalizedPath(filePath)
    const schema = payload.schema || ''
    const ns = namespaceFromPath(filePath, assetsDir)

    if (path.basename(filePath) === 'sounds.json') {
      extractSoundEvents(filePath, payload, ns, moduleId, nodes)
      continue
    }

    if (normalizedFilePath.includes('/items/') && path.basename(filePath, '.json').endsWith('_spawn_egg')) {
      const localId = path.basename(filePath, '.json')
      const itemId = nodeId(ns, localId)
      spawnEggItemIds.add(itemId)
      nodes.push(makeNode({
        kind: NODE_KINDS.ITEM,
        id: itemId,
        moduleId,
        displayName: displayNameFromLocalId(localId),
        source: sourceFor(filePath),
        data: {
          itemKind: 'spawn_egg',
          model: payload.model?.model,
          targetEntityId: nodeId(ns, localId.replace(/_spawn_egg$/, '')),
        },
        extra: { capabilities: ['spawn_egg', 'entity_spawn'] },
      }))
      continue
    }

    if (normalizedFilePath.includes('/rendercore/visual_profiles/screen/')) {
      const localId = path.basename(filePath, '.json')
      const profileId = nodeId(ns, `visual_profile/screen/${localId}`)
      nodes.push(makeNode({
        kind: NODE_KINDS.ASSET,
        id: profileId,
        moduleId,
        displayName: payload.surface?.display_name || displayNameFromLocalId(localId),
        source: sourceFor(filePath),
        data: {
          assetKind: 'rendercore_visual_profile',
          surface: payload.surface ?? {},
          baseTexture: payload.base_texture,
          requiredEvidence: payload.qa?.evidence ?? [],
        },
      }))
      const surfaceType = String(payload.surface?.type ?? '')
      const intent = surfaceType.includes('hud_overlay')
        ? inferUiIntent(`${ns}:${localId}`)
        : inferUiIntent(`${ns}:${localId}`, payload.intent)
      const uiId = nodeId(ns, `ui/${localId}`)
      const capabilities = [surfaceType || 'screen', ...(payload.surface?.tags ?? [])].filter(Boolean)
      nodes.push(makeNode({
        kind: NODE_KINDS.UI_INTENT,
        id: uiId,
        moduleId,
        displayName: payload.surface?.display_name || displayNameFromLocalId(localId),
        source: sourceFor(filePath),
        data: {
          surface: surfaceType || 'screen',
          route: uiId,
          capabilities,
          visualProfile: profileId,
          ownerAddon: payload.surface?.owner_addon,
        },
        extra: {
          intent,
          capabilities,
          runtimeHints: uiRuntimeHints(uiId),
        },
      }))
      pushUiHostAdaptationEdges(edges, uiId, moduleId)
      continue
    }

    if (schema.includes('screen') || schema.includes('eui') || filePath.includes('eui_manifest')) {
      const pages = payload.pages || payload.screens || []
      for (const page of pages) {
        const localId = page.id || page.name
        if (!localId) continue
        const uiId = localId.includes(':') ? localId : nodeId(ns, `ui/${localId}`)
        const intent = inferUiIntent(localId, page.intent ?? payload.defaultIntent ?? payload.intent)
        const surface = page.surface || payload.defaultSurface || inferSurfaceFromIntent(intent)
        const capabilities = page.capabilities || payload.capabilities || [surface]
        nodes.push(makeNode({
          kind: NODE_KINDS.UI_INTENT,
          id: uiId,
          moduleId,
          displayName: page.title || localId,
          source: { repo: 'ECHO-Modules', path: path.relative(process.cwd(), filePath).replace(/\\/g, '/'), format: 'json' },
          data: {
            surface,
            route: uiId,
            capabilities,
          },
          extra: {
            intent,
            actions: (page.actions || []).map((a) => ({ id: a.id, label: a.label || a.id, requires: a.requires })),
            fallbacks: {
              neoforge: page.fallbacks?.neoforge || 'custom_screen',
              echo_native: page.fallbacks?.echo_native || 'native_panel',
              echo_runtime_standalone: page.fallbacks?.echo_runtime_standalone || 'native_panel',
              standalone_engine: page.fallbacks?.standalone_engine || 'engine_panel',
              hytale: page.fallbacks?.hytale || 'notification_and_basic_menu',
            },
            runtimeHints: uiRuntimeHints(uiId),
          },
        }))
        pushUiHostAdaptationEdges(edges, uiId, moduleId)
        pushExplicitUiControlEdges(edges, uiId, moduleId, intent, page)
      }
    }
  }

  for await (const filePath of walkResourceFiles(assetsDir)) {
    const normalizedFilePath = normalizedPath(filePath)
    const ext = path.extname(filePath).toLowerCase()
    if (!['.png', '.jpg', '.jpeg', '.webp'].includes(ext)) continue
    if (!normalizedFilePath.includes('/textures/gui/hud/')) continue
    const ns = namespaceFromPath(filePath, assetsDir)
    const localId = stripResourceExtension(localPathAfterNamespace(filePath, assetsDir).replace(/^textures\/gui\/hud\//, ''))
    const assetId = nodeId(ns, `asset/hud/${localId}`)
    nodes.push(makeNode({
      kind: NODE_KINDS.ASSET,
      id: assetId,
      moduleId,
      displayName: displayNameFromLocalId(localId),
      source: sourceForResource(filePath),
      data: {
        assetKind: 'hud_texture',
        assetPath: normalizedPath(path.relative(process.cwd(), filePath)),
      },
    }))
    const uiId = nodeId(ns, `ui/hud/${localId}`)
    const capabilities = ['hud', 'hud_overlay']
    nodes.push(makeNode({
      kind: NODE_KINDS.UI_INTENT,
      id: uiId,
      moduleId,
      displayName: `${displayNameFromLocalId(localId)} HUD`,
      source: sourceForResource(filePath),
      data: {
        surface: 'hud_overlay',
        route: uiId,
        capabilities,
        asset: assetId,
      },
      extra: {
        intent: 'hud_widget',
        capabilities,
        runtimeHints: uiRuntimeHints(uiId),
      },
    }))
    pushUiHostAdaptationEdges(edges, uiId, moduleId)
  }

  for await (const filePath of walkJavaFiles(javaDir)) {
    const source = await readText(filePath)
    if (source.includes('registerEntityType(')) {
      await extractJavaEntityRegistrations(source, contentNs, moduleId, filePath, spawnEggItemIds, assetsDir, spawnMetadata, nodes, edges)
    }
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
            standalone_engine: { declared: true },
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
    if (!seen.has(node.id)) {
      seen.set(node.id, node)
      continue
    }
    const existing = seen.get(node.id)
    if (node.data && typeof node.data === 'object') {
      existing.data = { ...existing.data, ...node.data }
    }
    const capabilities = new Set([...(existing.capabilities || []), ...(node.capabilities || [])])
    existing.capabilities = [...capabilities]
    const aliases = new Set([...(existing.aliases || []), ...(node.aliases || [])])
    existing.aliases = [...aliases]
    if (node.displayName && node.displayName !== node.id && (!existing.displayName || existing.displayName === existing.id)) {
      existing.displayName = node.displayName
    }
    if (node.source && node.source.format !== 'generated' && existing.source?.format === 'generated') {
      existing.source = node.source
    }
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
    const connectedEdges = edges.filter((e) => UI_FEATURE_EDGE_KINDS.includes(e.kind) && e.from === ui.id)
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

function hytaleActorPlan(node, {
  contract,
  adapter,
  mappedTo,
  actorLabel,
}) {
  const hints = node.runtimeHints?.hytale ?? {}
  const declaredContract = hints.entityContract || hints.npcContract || hints.actorContract
  if (declaredContract === true || typeof declaredContract === 'string') {
    return {
      status: 'direct',
      mappedTo,
      contract: declaredContract === true ? contract : String(declaredContract),
      rationale: `${actorLabel} declares a Hytale runtime contract.`,
    }
  }
  const declaredAdapter = hints.entityAdapter || hints.npcAdapter || hints.actorAdapter
  if (declaredAdapter === true || typeof declaredAdapter === 'string') {
    return {
      status: 'adapter_required',
      mappedTo,
      contract,
      requiredAdapter: declaredAdapter === true ? adapter : String(declaredAdapter),
      rationale: `${actorLabel} requires a Hytale adapter contract before runtime export.`,
    }
  }
  const fallback = hints.fallback || hints.uiFallback || hints.exportFallback
  if (typeof fallback === 'string' && fallback.trim()) {
    return {
      status: 'fallback',
      mappedTo: fallback.trim(),
      contract,
      rationale: `${actorLabel} uses an explicit Hytale fallback instead of a runtime actor export.`,
    }
  }
  return {
    status: 'blocked',
    mappedTo: null,
    contract,
    requiredAdapter: adapter,
    blockedReasonCode: 'HYTALE_ACTOR_CONTRACT_MISSING',
    rationale: 'Hytale entity contract not defined.',
    recommendedFix: `Define ${contract} hints or an explicit Hytale fallback for this ${actorLabel.toLowerCase()} node.`,
  }
}

function runtimeTargetPlan(target, node, fallbackPlan) {
  if (target === 'hytale') return fallbackPlan

  const targetHints = node.runtimeHints?.[target] ?? {}
  if (targetHints.unsupported === true || targetHints.blocked === true) {
    return {
      status: 'blocked',
      mappedTo: null,
      blockedReasonCode: `${target.toUpperCase()}_EXPORT_BLOCKED`,
      rationale: `Content Graph node is explicitly blocked for ${target}.`,
    }
  }

  if (target === 'neoforge') {
    return neoforgeRuntimePlan(node, fallbackPlan)
  }
  if (target === 'echo_runtime_standalone') {
    return standaloneRuntimePlan(node, fallbackPlan)
  }
  if (target === 'standalone_engine') {
    return standaloneEngineRuntimePlan(node, fallbackPlan)
  }
  if (target === 'echo_native') {
    return nativeRuntimePlan(node, fallbackPlan)
  }
  return fallbackPlan
}

function neoforgeRuntimePlan(node, fallbackPlan) {
  switch (node.kind) {
    case NODE_KINDS.MODULE:
    case NODE_KINDS.ADDON:
    case NODE_KINDS.DEPENDENCY:
    case NODE_KINDS.SETTING:
    case NODE_KINDS.SYSTEM:
      return { status: 'not_applicable', mappedTo: null }
    case NODE_KINDS.BLOCK:
    case NODE_KINDS.ITEM:
    case NODE_KINDS.CREATIVE_TAB:
    case NODE_KINDS.ENTITY:
    case NODE_KINDS.NPC:
    case NODE_KINDS.BIOME:
    case NODE_KINDS.STRUCTURE:
    case NODE_KINDS.FEATURE:
    case NODE_KINDS.SOUND_EVENT:
      return { status: 'direct', mappedTo: 'neoforge_registry' }
    default:
      return fallbackPlan.status === 'blocked'
        ? { status: 'adapter_required', mappedTo: 'neoforge_adapter_bridge' }
        : fallbackPlan
  }
}

function nativeRuntimePlan(node, fallbackPlan) {
  switch (node.kind) {
    case NODE_KINDS.MODULE:
    case NODE_KINDS.ADDON:
    case NODE_KINDS.DEPENDENCY:
    case NODE_KINDS.SETTING:
    case NODE_KINDS.SYSTEM:
      return { status: 'not_applicable', mappedTo: null }
    case NODE_KINDS.CREATIVE_TAB:
    case NODE_KINDS.SOUND_EVENT:
    case NODE_KINDS.REGION:
      return { status: 'direct', mappedTo: 'native_runtime_registry' }
    default:
      return {
        status: 'adapter_required',
        mappedTo: 'adaptercore_native_bridge',
        requiredAdapter: 'echo.adaptercore.native_runtime.v1',
        rationale: 'Native runtime consumes Content Graph identity through AdapterCore-backed mutations.',
      }
  }
}

function standaloneRuntimePlan(node, fallbackPlan) {
  switch (node.kind) {
    case NODE_KINDS.MODULE:
    case NODE_KINDS.ADDON:
    case NODE_KINDS.DEPENDENCY:
    case NODE_KINDS.SETTING:
    case NODE_KINDS.SYSTEM:
      return { status: 'not_applicable', mappedTo: null }
    case NODE_KINDS.CREATIVE_TAB:
      return { status: 'direct', mappedTo: 'creative_inventory_category' }
    case NODE_KINDS.SOUND_EVENT:
      return { status: 'direct', mappedTo: 'sound_event' }
    case NODE_KINDS.ASSET:
      return { status: 'direct', mappedTo: 'runtime_asset' }
    case NODE_KINDS.UI_INTENT:
      return {
        status: 'adapter_required',
        mappedTo: 'adaptercore_ui_event',
        requiredAdapter: 'echo.adaptercore.ui_runtime.v1',
        rationale: 'Standalone hosts ScreenCore/HUD/Lens/Terminal/Index surfaces through AdapterCore events.',
      }
    case NODE_KINDS.ENTITY:
    case NODE_KINDS.NPC:
      return {
        status: 'adapter_required',
        mappedTo: 'adaptercore_entity_runtime',
        requiredAdapter: 'echo.adaptercore.entity_runtime.v1',
        rationale: 'Standalone consumes graph-backed entity registry, visuals, spawn eggs, and mutations through AdapterCore.',
      }
    case NODE_KINDS.SPAWN_RULE:
      return {
        status: 'adapter_required',
        mappedTo: 'adaptercore_spawn_rule',
        requiredAdapter: 'echo.adaptercore.spawn_runtime.v1',
        rationale: 'Standalone spawn rules are graph-backed and executed through AdapterCore spawn/despawn mutations.',
      }
    case NODE_KINDS.BLOCK:
      return {
        status: 'adapter_required',
        mappedTo: 'adaptercore_block_runtime',
        requiredAdapter: 'echo.adaptercore.block_runtime.v1',
        rationale: 'Standalone block placement and breaking must emit AdapterCore mutation receipts.',
      }
    case NODE_KINDS.ITEM:
      return {
        status: 'adapter_required',
        mappedTo: 'adaptercore_item_runtime',
        requiredAdapter: 'echo.adaptercore.item_runtime.v1',
        rationale: 'Standalone item use and inventory changes must emit AdapterCore mutation receipts.',
      }
    case NODE_KINDS.RECIPE:
      return { status: 'adapter_required', mappedTo: 'adaptercore_recipe_runtime' }
    case NODE_KINDS.LOOT_TABLE:
      return { status: 'adapter_required', mappedTo: 'adaptercore_loot_runtime' }
    case NODE_KINDS.BIOME:
    case NODE_KINDS.STRUCTURE:
    case NODE_KINDS.FEATURE:
    case NODE_KINDS.REGION:
      return {
        status: 'adapter_required',
        mappedTo: 'adaptercore_world_runtime',
        requiredAdapter: 'echo.adaptercore.world_runtime.v1',
        rationale: 'Standalone world identity is graph-backed and hosted through the AdapterCore world bridge.',
      }
    case NODE_KINDS.EFFECT:
    case NODE_KINDS.MISSION:
    case NODE_KINDS.OBJECTIVE:
      return {
        status: 'adapter_required',
        mappedTo: 'adaptercore_gameplay_runtime',
        requiredAdapter: 'echo.adaptercore.gameplay_runtime.v1',
      }
    default:
      return fallbackPlan.status === 'blocked'
        ? {
            status: 'adapter_required',
            mappedTo: 'adaptercore_content_runtime',
            requiredAdapter: 'echo.adaptercore.content_runtime.v1',
          }
        : fallbackPlan
  }
}

function standaloneEngineRuntimePlan(node, fallbackPlan) {
  const plan = standaloneRuntimePlan(node, fallbackPlan)
  if (plan.status === 'not_applicable') return plan
  if (node.kind === NODE_KINDS.UI_INTENT) {
    return {
      status: 'adapter_required',
      mappedTo: 'standalone_engine_surface_resolver',
      requiredAdapter: 'echo.native.surface_host.v1',
      rationale: 'Standalone Engine must render this module-owned ECHO surface through the unified surface resolver.',
    }
  }
  if (plan.mappedTo?.startsWith('adaptercore_')) {
    return {
      ...plan,
      mappedTo: `standalone_engine_${plan.mappedTo}`,
      rationale: plan.rationale || 'Standalone Engine consumes this graph node through the unified ECHO Native AdapterCore contract.',
    }
  }
  return plan
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
    [NODE_KINDS.LOOT_TABLE]: () => ({ status: 'adapter_required', mappedTo: 'loot_table_adapter' }),
    [NODE_KINDS.ENTITY]: (node) => hytaleActorPlan(node, {
      contract: 'echo.hytale.entity_contract.v1',
      adapter: 'echo.hytale.entity_adapter.v1',
      mappedTo: 'entity_definition',
      actorLabel: 'Entity',
    }),
    [NODE_KINDS.NPC]: (node) => hytaleActorPlan(node, {
      contract: 'echo.hytale.npc_contract.v1',
      adapter: 'echo.hytale.npc_adapter.v1',
      mappedTo: 'npc_definition',
      actorLabel: 'NPC',
    }),
    [NODE_KINDS.BIOME]: () => ({ status: 'adapter_required', mappedTo: 'biome_definition_adapter' }),
    [NODE_KINDS.STRUCTURE]: () => ({ status: 'adapter_required', mappedTo: 'structure_template_adapter' }),
    [NODE_KINDS.FEATURE]: () => ({ status: 'adapter_required', mappedTo: 'world_feature_adapter' }),
    [NODE_KINDS.SPAWN_RULE]: () => ({ status: 'adapter_required', mappedTo: 'spawn_rule_adapter' }),
    [NODE_KINDS.SOUND_EVENT]: () => ({ status: 'direct', mappedTo: 'sound_event' }),
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
    const mapped = runtimeTargetPlan(target, node, mapFn(node))
    const planNode = {
      nodeId: node.id,
      kind: node.kind,
      status: mapped.status,
    }
    for (const key of ['mappedTo', 'rationale', 'contract', 'requiredAdapter', 'blockedReasonCode', 'recommendedFix']) {
      if (mapped[key] !== undefined && mapped[key] !== null && mapped[key] !== '') {
        planNode[key] = mapped[key]
      }
    }
    plan.nodes.push(planNode)
    plan.summary[mapped.status]++
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

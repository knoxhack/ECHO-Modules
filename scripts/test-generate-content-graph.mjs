#!/usr/bin/env node
import { generateContentGraph } from './generate-content-graph.mjs'
import { validateContentGraph } from './validate-content-graph.mjs'

async function main() {
  const sampleModules = ['echocore', 'echoopenlandsprotocol', 'echoterminal', 'echoashfallprotocol']
  const results = await generateContentGraph({ moduleIds: sampleModules })
  console.log(`Smoke test generated ${results.length} module graph(s).`)
  for (const r of results) {
    console.log(`  ${r.moduleId}: ${r.nodeCount} nodes, ${r.edgeCount} edges`)
  }
  const byModule = new Map(results.map((result) => [result.moduleId, result]))
  const openlands = byModule.get('echoopenlandsprotocol')
  const terminal = byModule.get('echoterminal')
  const ashfall = byModule.get('echoashfallprotocol')
  if (!openlands) throw new Error('Missing echoopenlandsprotocol content graph result.')
  if (!terminal) throw new Error('Missing echoterminal content graph result.')

  const expectPlanNode = (result, target, expected) => {
    const node = result.plans[target].nodes.find((entry) => entry.nodeId === expected.nodeId)
    if (!node) throw new Error(`${result.moduleId} ${target} plan missing ${expected.nodeId}.`)
    for (const [key, value] of Object.entries(expected)) {
      if (node[key] !== value) {
        throw new Error(`${expected.nodeId} expected ${key}=${value}, found ${node[key]}.`)
      }
    }
    return node
  }

  expectPlanNode(openlands, 'hytale', {
    nodeId: 'echoopenlandsprotocol:broken_waystone',
    kind: 'echo:block',
    status: 'direct',
    mappedTo: 'server_state_object',
  })
  expectPlanNode(openlands, 'hytale', {
    nodeId: 'echoopenlandsprotocol:meadow_grass_block',
    kind: 'echo:block',
    status: 'adapter_required',
    mappedTo: 'block_state_adapter',
  })
  expectPlanNode(terminal, 'hytale', {
    nodeId: 'echoterminal:terminal_overview',
    kind: 'echo:ui_intent',
    status: 'fallback',
    mappedTo: 'notification_and_basic_menu',
  })
  expectPlanNode(openlands, 'hytale', {
    nodeId: 'echoopenlandsprotocol:module',
    kind: 'echo:module',
    status: 'not_applicable',
  })

  const blockedActors = openlands.plans.hytale.nodes.filter((node) =>
    node.status === 'blocked'
    && ['echo:entity', 'echo:npc'].includes(node.kind))
  if (blockedActors.length !== 9) {
    throw new Error(`Expected 9 explicit Openlands Hytale actor blockers, found ${blockedActors.length}.`)
  }
  if (openlands.plans.hytale.summary.blocked !== blockedActors.length) {
    throw new Error(`Openlands Hytale blocked summary ${openlands.plans.hytale.summary.blocked} does not match blocked actor count ${blockedActors.length}.`)
  }
  const expectedActorContracts = {
    'echo:entity': ['echo.hytale.entity_contract.v1', 'echo.hytale.entity_adapter.v1'],
    'echo:npc': ['echo.hytale.npc_contract.v1', 'echo.hytale.npc_adapter.v1'],
  }
  for (const node of blockedActors) {
    const [contract, adapter] = expectedActorContracts[node.kind]
    if (node.blockedReasonCode !== 'HYTALE_ACTOR_CONTRACT_MISSING') {
      throw new Error(`${node.nodeId} missing HYTALE_ACTOR_CONTRACT_MISSING reason code.`)
    }
    if (node.contract !== contract) {
      throw new Error(`${node.nodeId} expected Hytale actor contract ${contract}, found ${node.contract}.`)
    }
    if (node.requiredAdapter !== adapter) {
      throw new Error(`${node.nodeId} expected Hytale actor adapter ${adapter}, found ${node.requiredAdapter}.`)
    }
    if (!node.recommendedFix?.includes(contract)) {
      throw new Error(`${node.nodeId} missing actionable Hytale actor recommendedFix.`)
    }
  }

  // Standalone runtime metadata assertions
  const terminalUi = terminal.graph.nodes.find((n) => n.kind === 'echo:ui_intent')
  if (!terminalUi?.data?.surface || !terminalUi?.data?.route) {
    throw new Error('Terminal UI_INTENT node is missing data.surface or data.route.')
  }
  if (!terminalUi.runtimeHints?.echo_runtime_standalone?.id) {
    throw new Error('Terminal UI_INTENT node is missing runtimeHints.echo_runtime_standalone.id.')
  }
  if (!terminalUi.runtimeHints?.standalone_engine?.id) {
    throw new Error('Terminal UI_INTENT node is missing runtimeHints.standalone_engine.id.')
  }
  const terminalEnginePlan = terminal.plans.standalone_engine?.nodes.find((node) => node.nodeId === terminalUi.id)
  if (!terminalEnginePlan || terminalEnginePlan.mappedTo !== 'standalone_engine_surface_resolver') {
    throw new Error('Terminal UI_INTENT node is missing Standalone Engine surface resolver export evidence.')
  }
  const terminalHostEdges = terminal.graph.edges.filter((edge) =>
    edge.kind === 'runtime_host_adapts_surface' && edge.to === terminalUi.id)
  if (terminalHostEdges.length < 4) {
    throw new Error(`Terminal UI_INTENT node expected four runtime_host_adapts_surface edges, found ${terminalHostEdges.length}.`)
  }

  const ashfallEntity = ashfall?.graph.nodes.find(
    (n) => n.kind === 'echo:entity' && (n.data?.texturePath || n.data?.texture)
  )
  if (!ashfallEntity) {
    throw new Error('No Ashfall entity has texturePath/texture metadata.')
  }
  if (!ashfallEntity.data?.threat && !ashfallEntity.data?.hostility) {
    throw new Error(`Ashfall entity ${ashfallEntity.id} is missing threat/hostility metadata.`)
  }

  const openlandsBiome = openlands.graph.nodes.find((n) => n.kind === 'echo:biome')
  if (openlandsBiome && !openlandsBiome.data?.biomeTags && !openlandsBiome.data?.surfaceBlockId) {
    throw new Error(`Openlands biome ${openlandsBiome.id} is missing biomeTags or surfaceBlockId.`)
  }

  const openlandsFeature = openlands.graph.nodes.find((n) => n.kind === 'echo:feature')
  if (openlandsFeature && !openlandsFeature.data?.featureType) {
    throw new Error(`Openlands feature ${openlandsFeature.id} is missing featureType.`)
  }

  const validation = await validateContentGraph({ moduleIds: sampleModules })
  if (!validation.passed) {
    console.error('Validation failed:')
    for (const error of validation.errors.slice(0, 20)) console.error(`  - ${error}`)
    process.exitCode = 1
    return
  }
  console.log('Content graph smoke test passed.')
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})

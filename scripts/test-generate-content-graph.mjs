#!/usr/bin/env node
import { generateContentGraph } from './generate-content-graph.mjs'
import { validateContentGraph } from './validate-content-graph.mjs'

async function main() {
  const sampleModules = ['echocore', 'echoopenlandsprotocol', 'echoterminal']
  const results = await generateContentGraph({ moduleIds: sampleModules })
  console.log(`Smoke test generated ${results.length} module graph(s).`)
  for (const r of results) {
    console.log(`  ${r.moduleId}: ${r.nodeCount} nodes, ${r.edgeCount} edges`)
  }
  const openlands = results.find((r) => r.moduleId === 'echoopenlandsprotocol')
  const blockedActors = openlands.plans.hytale.nodes.filter((node) =>
    node.status === 'blocked'
    && ['echo:entity', 'echo:npc'].includes(node.kind))
  if (blockedActors.length > 0) {
    if (blockedActors.length !== 9) {
      throw new Error(`Expected 9 explicit Openlands Hytale actor blockers, found ${blockedActors.length}.`)
    }
    for (const node of blockedActors) {
      if (node.blockedReasonCode !== 'HYTALE_ACTOR_CONTRACT_MISSING') {
        throw new Error(`${node.nodeId} missing HYTALE_ACTOR_CONTRACT_MISSING reason code.`)
      }
      if (!node.contract || !node.requiredAdapter || !node.recommendedFix) {
        throw new Error(`${node.nodeId} missing Hytale actor contract planning metadata.`)
      }
    }
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

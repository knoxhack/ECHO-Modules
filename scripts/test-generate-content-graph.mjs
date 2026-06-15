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

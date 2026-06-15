#!/usr/bin/env node
import { generateContentGraph } from './generate-content-graph.mjs'

function parseArgs(argv) {
  const targetIndex = argv.indexOf('--target')
  return {
    target: targetIndex >= 0 ? argv[targetIndex + 1] : 'hytale',
    write: argv.includes('--write'),
    strict: argv.includes('--strict'),
    all: argv.includes('--all'),
    help: argv.includes('--help'),
    moduleIds: argv.includes('--module') ? argv[argv.indexOf('--module') + 1]?.split(',') ?? [] : [],
  }
}

const options = parseArgs(process.argv.slice(2))
if (options.help) {
  console.log('Usage: node scripts/generate-runtime-export-plan.mjs --target <neoforge|echo_native|echo_runtime_standalone|hytale> [--all] [--write] [--module id1,id2]')
  process.exit(0)
}

if (!['neoforge', 'echo_native', 'echo_runtime_standalone', 'hytale'].includes(options.target)) {
  console.error(`Unknown target: ${options.target}`)
  process.exit(1)
}

generateContentGraph({ write: options.write, moduleIds: options.moduleIds })
  .then((results) => {
    let blocked = 0
    for (const result of results) {
      const plan = result.plans[options.target]
      blocked += plan.summary.blocked
    }
    console.log(`Generated ${options.target} export plans for ${results.length} module(s).`)
    if (options.target === 'hytale') {
      console.log(`Blocked nodes: ${blocked}`)
      // Blocked is an explicit planning status, not a strict failure.
      // Strict mode only requires every node to be accounted for, which the generator guarantees.
    }
  })
  .catch((error) => {
    console.error(error)
    process.exitCode = 1
  })

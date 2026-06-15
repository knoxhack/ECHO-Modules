#!/usr/bin/env node
import { generateContentGraph } from './generate-content-graph.mjs'

function parseArgs(argv) {
  return {
    write: argv.includes('--write'),
    all: argv.includes('--all'),
    help: argv.includes('--help'),
    moduleIds: argv.includes('--module') ? argv[argv.indexOf('--module') + 1]?.split(',') ?? [] : [],
  }
}

const options = parseArgs(process.argv.slice(2))
if (options.help) {
  console.log('Usage: node scripts/generate-content-feature-list.mjs [--all] [--write] [--module id1,id2]')
  process.exit(0)
}

generateContentGraph({ write: options.write, moduleIds: options.moduleIds })
  .then((results) => {
    const totalFeatures = results.reduce((sum, r) => sum + r.features.features.length, 0)
    console.log(`Generated feature lists for ${results.length} module(s), ${totalFeatures} features.`)
  })
  .catch((error) => {
    console.error(error)
    process.exitCode = 1
  })

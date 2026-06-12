import { pathToFileURL } from 'node:url'
import { runHarness } from './run-openlands-harness.mjs'

export async function main(argv = process.argv.slice(2)) {
  return runHarness('runtime', argv)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}

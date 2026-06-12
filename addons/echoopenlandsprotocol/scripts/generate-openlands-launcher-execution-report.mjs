import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const EDITIONS = {
  native: {
    packId: 'openlands-native-edition',
    runtimeTarget: 'echo_native',
    artifactName: `${MODULE_ID}-${VERSION}.echo-addon`,
    reportName: 'native-launcher-execution-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    runtimeTarget: 'neoforge',
    artifactName: `${MODULE_ID}-${VERSION}-neoforge.jar`,
    reportName: 'neoforge-launcher-execution-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    runtimeTarget: 'echo_runtime_standalone',
    artifactName: `${MODULE_ID}-${VERSION}-standalone.jar`,
    reportName: 'standalone-launcher-execution-report.json',
  },
}

function parseArgs(argv) {
  const args = {
    edition: null,
    editionRoot: null,
    moduleRoot: null,
    moduleArtifact: null,
    output: null,
    status: 'blocked',
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--edition') args.edition = argv[++index]
    else if (arg === '--edition-root') args.editionRoot = argv[++index]
    else if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--module-artifact') args.moduleArtifact = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--status') args.status = argv[++index]
    else if (arg === '--dry-run') args.dryRun = true
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function fileExists(filePath) {
  return fs.existsSync(filePath)
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function sha256(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function findModuleRoot(explicitRoot, editionRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = editionRoot ? path.resolve(editionRoot) : process.cwd()
  for (;;) {
    const direct = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(direct)) return cursor
    const sibling = path.join(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(sibling)) return path.resolve(cursor, '..', 'ECHO-Modules', 'addons', MODULE_ID)
    const nested = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(nested)) return path.join(cursor, 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function defaultArtifactPath(moduleRoot, edition) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release', MODULE_ID, edition.artifactName)
}

function buildBlockedReport({ moduleRoot, editionKey, artifactPath, outputPath, status }) {
  const edition = EDITIONS[editionKey]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  if (status !== 'blocked') throw new Error('Only --status blocked is supported until a real launcher harness writes passed/failed execution results.')
  if (!fileExists(artifactPath)) throw new Error(`Openlands module artifact not found: ${artifactPath}`)

  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const acceptance = readJson(path.join(dataRoot, 'systems', 'launcher_execution_acceptance.json'))
  const launcherGateIds = [...new Set((acceptance.launcherGates ?? []).map((gate) => gate.id))].sort()
  const artifactSha256 = sha256(artifactPath)
  const timestamp = new Date().toISOString()

  const flowResults = (acceptance.executionFlows ?? []).map((flow) => ({
    id: flow.id,
    status: 'blocked',
    startedAt: timestamp,
    finishedAt: timestamp,
    durationMs: 0,
    runtimeTarget: edition.runtimeTarget,
    artifactSha256,
    inputFixtureRefs: flow.inputFixtureRefs ?? [],
    preconditions: flow.preconditions ?? [],
    plannedActions: flow.plannedActions ?? [],
    actionsRun: [],
    assertions: (flow.assertions ?? []).map((assertion) => ({
      id: assertion,
      status: 'blocked',
      reason: 'real launcher execution has not run yet',
    })),
    savedArtifacts: [],
    requiredSavedArtifacts: flow.requiredSavedArtifacts ?? [],
    worldStatePolicy: flow.worldStatePolicy,
    blockedBy: [
      'real_launcher_execution_missing',
    ],
  }))

  return {
    schema: acceptance.reportContract?.schema ?? 'echo.openlands.edition.launcher_execution_report.v1',
    status: 'blocked',
    generatedAt: timestamp,
    generatedBy: 'generate-openlands-launcher-execution-report.mjs',
    edition: editionKey,
    packId: edition.packId,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    moduleArtifact: artifactPath,
    moduleArtifactSha256: artifactSha256,
    launcherBuild: {
      status: 'not_executed',
      source: 'blocked_report_generator',
      note: 'This report is schema-valid readiness evidence only; it does not clear launcher gates.',
    },
    executionEnvironment: {
      status: 'not_executed',
      reason: 'real ECHO Launcher install/update/repair/rollback execution has not provided results',
    },
    flowResults,
    clearedLauncherGates: [],
    remainingLauncherGates: launcherGateIds,
    publicAlphaReady: false,
    blockedBy: [
      'real_launcher_install_update_repair_rollback_execution_missing',
      'release_index_download_urls_missing',
    ],
    outputPath,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const edition = EDITIONS[args.edition]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  const editionRoot = args.editionRoot ? path.resolve(args.editionRoot) : process.cwd()
  const moduleRoot = findModuleRoot(args.moduleRoot, editionRoot)
  const artifactPath = args.moduleArtifact ? path.resolve(args.moduleArtifact) : defaultArtifactPath(moduleRoot, edition)
  const outputPath = args.output ? path.resolve(args.output) : path.join(editionRoot, 'evidence', edition.reportName)
  const report = buildBlockedReport({
    moduleRoot,
    editionKey: args.edition,
    artifactPath,
    outputPath,
    status: args.status,
  })
  if (!args.dryRun) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true })
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  }
  if (args.json) {
    console.log(JSON.stringify(report, null, 2))
  } else {
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    console.log(`Openlands ${args.edition} blocked launcher execution report ${action}: ${report.flowResults.length} flows, ${report.remainingLauncherGates.length} gates remaining.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-launcher-execution-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Path to addons/echoopenlandsprotocol. Auto-detected by default.
  --module-artifact <p>   Openlands artifact to hash. Defaults to generated local dist artifact.
  --out <path>            Report output path. Defaults to evidence/<edition>-launcher-execution-report.json.
  --status blocked        Generate an honest blocked report. This is currently the only supported status.
  --dry-run               Generate without writing the report.
  --json                  Print JSON output.
  --help                  Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}

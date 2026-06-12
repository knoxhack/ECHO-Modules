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
    reportName: 'native-distribution-approval-report.json',
  },
  neoforge: {
    packId: 'openlands-neoforge-edition',
    runtimeTarget: 'neoforge',
    reportName: 'neoforge-distribution-approval-report.json',
  },
  standalone: {
    packId: 'openlands-standalone-edition',
    runtimeTarget: 'echo_runtime_standalone',
    reportName: 'standalone-distribution-approval-report.json',
  },
}

function parseArgs(argv) {
  const args = {
    edition: null,
    editionRoot: null,
    moduleRoot: null,
    releaseRoot: null,
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
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
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

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function buildBlockedReport({ moduleRoot, releaseRoot, editionKey, outputPath, status }) {
  const edition = EDITIONS[editionKey]
  if (!edition) throw new Error(`--edition must be one of ${Object.keys(EDITIONS).join(', ')}`)
  if (status !== 'blocked') throw new Error('Only --status blocked is supported until real distribution approval writes passed/failed results.')

  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const acceptance = readJson(path.join(dataRoot, 'systems', 'distribution_approval_acceptance.json'))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseIndex = fileExists(releaseIndexPath) ? readJson(releaseIndexPath) : null
  const releaseId = releaseIndex?.releaseId ?? 'missing'
  const distributionGateIds = [...new Set((acceptance.distributionGates ?? []).map((gate) => gate.id))].sort()
  const timestamp = new Date().toISOString()

  const approvalResults = (acceptance.approvalAreas ?? []).map((area) => ({
    id: area.id,
    displayName: area.displayName,
    gateIds: area.gateIds ?? [],
    status: 'blocked',
    checklist: (area.checklist ?? []).map((item) => ({
      id: item,
      status: 'blocked',
      reason: 'real distribution approval has not run yet',
    })),
    evidenceRefs: area.inputFixtureRefs ?? [],
    savedArtifacts: [],
    requiredSavedArtifacts: area.requiredSavedArtifacts ?? [],
    blockedBy: [
      'distribution_approval_missing',
    ],
  }))

  return {
    schema: acceptance.reportContract?.schema ?? 'echo.openlands.edition.distribution_approval_report.v1',
    status: 'blocked',
    generatedAt: timestamp,
    generatedBy: 'generate-openlands-distribution-approval-report.mjs',
    edition: editionKey,
    packId: edition.packId,
    runtimeTarget: edition.runtimeTarget,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId,
    releaseIndex: {
      path: releaseIndexPath,
      hash: fileExists(releaseIndexPath) ? sha256(releaseIndexPath) : null,
      artifactDownloadUrlsPresent: false,
      approvedState: false,
    },
    approvalRun: {
      status: 'not_executed',
      approver: null,
      approvalDate: null,
      reason: 'Release Index publication, co-op session, dependency gates, and approval signature are missing.',
    },
    approvalResults,
    clearedDistributionGates: [],
    remainingDistributionGates: distributionGateIds,
    publicAlphaReady: false,
    blockedBy: [
      'release_index_download_urls_missing',
      'public_alpha_release_index_approval_missing',
      'public_alpha_coop_session_test_missing',
      'runtime_launcher_or_final_review_gate_missing',
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
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const outputPath = args.output ? path.resolve(args.output) : path.join(editionRoot, 'evidence', edition.reportName)
  const report = buildBlockedReport({
    moduleRoot,
    releaseRoot,
    editionKey: args.edition,
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
    console.log(`Openlands ${args.edition} blocked distribution approval report ${action}: ${report.approvalResults.length} areas, ${report.remainingDistributionGates.length} gates remaining.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-distribution-approval-report.mjs --edition <native|neoforge|standalone> [options]

Options:
  --edition <id>          Edition key: native, neoforge, or standalone.
  --edition-root <path>   Edition repository root. Defaults to cwd.
  --module-root <path>    Path to addons/echoopenlandsprotocol. Auto-detected by default.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to evidence/<edition>-distribution-approval-report.json.
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

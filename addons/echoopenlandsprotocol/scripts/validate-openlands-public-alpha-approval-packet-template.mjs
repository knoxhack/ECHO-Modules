import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { buildApprovalPacketTemplate } from './generate-openlands-public-alpha-approval-packet-template.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const SCHEMA = 'echo.openlands.public_alpha_approval_packet_template.v1'

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    releaseRoot: null,
    readinessReport: null,
    evidenceIntake: null,
    report: null,
    packetRoot: null,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--workspace-root') args.workspaceRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--readiness-report') args.readinessReport = argv[++index]
    else if (arg === '--evidence-intake') args.evidenceIntake = argv[++index]
    else if (arg === '--report') args.report = argv[++index]
    else if (arg === '--packet-root') args.packetRoot = argv[++index]
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
  return JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''))
}

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultWorkspaceRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function releaseFile(releaseRoot, filename) {
  return path.join(releaseRoot, MODULE_ID, filename)
}

function defaultReportPath(releaseRoot) {
  return releaseFile(releaseRoot, 'openlands-public-alpha-approval-packet.template.json')
}

function defaultPacketRoot(reportPath) {
  return path.join(path.dirname(reportPath), 'openlands-public-alpha-approval-packet-template')
}

function stable(value) {
  if (Array.isArray(value)) return value.map(stable)
  if (!value || typeof value !== 'object') return value
  const result = {}
  for (const [key, entry] of Object.entries(value)) {
    if (key === 'generatedAt' || key === 'dryRun') continue
    result[key] = stable(entry)
  }
  return result
}

function sameJson(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function sameSet(actual, expected) {
  return JSON.stringify([...(actual ?? [])].sort()) === JSON.stringify([...(expected ?? [])].sort())
}

function assert(errors, condition, message) {
  if (!condition) errors.push(message)
}

export function validate({ moduleRoot, workspaceRoot, releaseRoot, reportPath, packetRoot, readinessReportPath, evidenceIntakePath }) {
  const errors = []
  assert(errors, fileExists(reportPath), `approval packet template missing: ${reportPath}`)
  if (!fileExists(reportPath)) {
    return {
      status: 'failed',
      reportPath,
      errors,
    }
  }

  const report = readJson(reportPath)
  const expected = buildApprovalPacketTemplate({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    readinessReportPath,
    evidenceIntakePath,
    outputPath: reportPath,
    packetRoot,
    dryRun: true,
  })
  const readinessReport = readJson(readinessReportPath)
  const evidenceIntake = readJson(evidenceIntakePath)

  assert(errors, report.schema === SCHEMA, `approval packet template schema must be ${SCHEMA}`)
  assert(errors, report.templateOnly === true, 'approval packet template must be templateOnly')
  assert(errors, report.moduleId === MODULE_ID, 'approval packet template moduleId mismatch')
  assert(errors, report.publicAlphaReady === (readinessReport.publicAlphaReady === true), 'approval packet publicAlphaReady mismatch readiness report')
  assert(errors, sameSet(report.blockers, readinessReport.blockers ?? []), 'approval packet blockers mismatch readiness report')
  assert(errors, report.blockerCount === (readinessReport.blockers ?? []).length, 'approval packet blocker count mismatch')
  assert(errors, Array.isArray(report.externalEvidenceRequirements), 'approval packet must include external evidence requirements')
  assert(errors, sameSet(
    (report.externalEvidenceRequirements ?? []).map((requirement) => requirement.blockerId),
    readinessReport.blockers ?? [],
  ), 'approval packet external evidence requirements must cover every readiness blocker')
  for (const requirement of report.externalEvidenceRequirements ?? []) {
    assert(errors, typeof requirement.displayName === 'string' && requirement.displayName.length > 0, `external evidence requirement ${requirement.blockerId} displayName is required`)
    assert(errors, typeof requirement.ownerHint === 'string' && requirement.ownerHint.length > 0, `external evidence requirement ${requirement.blockerId} ownerHint is required`)
    assert(errors, Array.isArray(requirement.impactedPhases) && requirement.impactedPhases.length > 0, `external evidence requirement ${requirement.blockerId} impacted phases are required`)
    assert(errors, Array.isArray(requirement.proofRequired) && requirement.proofRequired.length > 0, `external evidence requirement ${requirement.blockerId} proofRequired is required`)
    assert(errors, Array.isArray(requirement.evidenceTargets) && requirement.evidenceTargets.length > 0, `external evidence requirement ${requirement.blockerId} evidenceTargets are required`)
    assert(errors, Array.isArray(requirement.validationCommands) && requirement.validationCommands.length > 0, `external evidence requirement ${requirement.blockerId} validationCommands are required`)
  }
  assert(errors, sameSet(
    (report.externalEvidenceRequirements ?? []).map((requirement) => requirement.blockerId),
    (evidenceIntake.intakeItems ?? []).filter((item) => item.active === true).map((item) => item.blockerId),
  ), 'approval packet external evidence requirements must match active evidence intake items')
  assert(errors, sameJson(stable(report), stable(expected.packet)), 'approval packet template stale against generator dry-run')

  assert(errors, sameSet(
    (report.generatedFiles ?? []).map((file) => path.resolve(file.path)),
    (expected.packet.generatedFiles ?? []).map((file) => path.resolve(file.path)),
  ), 'approval packet generated file set mismatch')
  for (const file of expected.files) {
    assert(errors, fileExists(file.path), `approval packet generated file missing: ${file.path}`)
    if (fileExists(file.path)) {
      assert(errors, readText(file.path) === file.content, `approval packet generated file stale: ${file.path}`)
    }
  }

  const approvalInputReportIndexPath = path.join(packetRoot, 'approval-input-report-index.template.json')
  if (fileExists(approvalInputReportIndexPath)) {
    const approvalInputReportIndex = readJson(approvalInputReportIndexPath)
    assert(errors, approvalInputReportIndex.evidenceIntakePath === evidenceIntakePath, 'approval input report index evidenceIntakePath mismatch')
    assert(errors, approvalInputReportIndex.blockerCount === (readinessReport.blockers ?? []).length, 'approval input report index blocker count mismatch')
    assert(errors, sameSet(approvalInputReportIndex.blockers, readinessReport.blockers ?? []), 'approval input report index blockers mismatch readiness report')
    assert(errors, sameSet(
      (approvalInputReportIndex.externalEvidenceRequirements ?? []).map((requirement) => requirement.blockerId),
      readinessReport.blockers ?? [],
    ), 'approval input report index external evidence requirements must cover every readiness blocker')
  }

  const dependencyGateSummaryPath = path.join(packetRoot, 'dependency-gate-summary.template.json')
  if (fileExists(dependencyGateSummaryPath)) {
    const dependencyGateSummary = readJson(dependencyGateSummaryPath)
    assert(errors, dependencyGateSummary.activeBlockerCount === (readinessReport.blockers ?? []).length, 'dependency gate summary active blocker count mismatch')
    assert(errors, dependencyGateSummary.externalEvidenceRequirementCount === (report.externalEvidenceRequirements ?? []).length, 'dependency gate summary external requirement count mismatch')
    assert(errors, sameSet(dependencyGateSummary.externalEvidenceRequirementIds, readinessReport.blockers ?? []), 'dependency gate summary external requirement ids mismatch readiness blockers')
  }

  const requiredDrafts = [
    'approval-input-report-index.template.json',
    'dependency-gate-summary.template.json',
    'release-readiness-hash.template.txt',
    'public-alpha-approval.template.md',
    'rollback-plan-snapshot.template.md',
    'approved-readiness-report.template.json',
    'approved-readiness-report-by-phase.template.md',
  ]
  const generatedFilenames = (report.generatedFiles ?? []).map((file) => file.filename)
  for (const draft of requiredDrafts) {
    assert(errors, generatedFilenames.includes(draft), `approval packet missing draft file ${draft}`)
  }

  return {
    status: errors.length === 0 ? 'passed' : 'failed',
    reportPath,
    packetRoot,
    publicAlphaReady: report.publicAlphaReady,
    blockerCount: report.blockerCount,
    generatedFileCount: report.generatedFiles?.length ?? 0,
    errors,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const workspaceRoot = args.workspaceRoot ? path.resolve(args.workspaceRoot) : defaultWorkspaceRoot(moduleRoot)
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const reportPath = args.report ? path.resolve(args.report) : defaultReportPath(releaseRoot)
  const packetRoot = args.packetRoot ? path.resolve(args.packetRoot) : defaultPacketRoot(reportPath)
  const readinessReportPath = args.readinessReport
    ? path.resolve(args.readinessReport)
    : releaseFile(releaseRoot, 'openlands-release-readiness-report.json')
  const evidenceIntakePath = args.evidenceIntake
    ? path.resolve(args.evidenceIntake)
    : releaseFile(releaseRoot, 'openlands-public-alpha-evidence-intake.json')
  const result = validate({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    reportPath,
    packetRoot,
    readinessReportPath,
    evidenceIntakePath,
  })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands Public Alpha approval packet template validated: generatedFiles=${result.generatedFileCount}, blockers=${result.blockerCount}.`)
  } else {
    console.error(`Openlands Public Alpha approval packet template failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-public-alpha-approval-packet-template.mjs [options]

Options:
  --module-root <path>       Openlands module root. Defaults to this script's module.
  --workspace-root <path>    Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>      Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --readiness-report <path>  Readiness report path. Defaults to openlands-release-readiness-report.json.
  --evidence-intake <path>   Evidence intake path. Defaults to openlands-public-alpha-evidence-intake.json.
  --report <path>            Packet JSON path. Defaults to openlands-public-alpha-approval-packet.template.json.
  --packet-root <path>       Directory for generated draft artifacts.
  --json                     Print JSON output.
  --help                     Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}

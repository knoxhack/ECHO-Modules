import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import {
  buildEvidenceIntake,
  renderEvidenceIntakeMarkdown,
} from './generate-openlands-public-alpha-evidence-intake.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const SCHEMA = 'echo.openlands.public_alpha_evidence_intake.v1'

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    releaseRoot: null,
    readinessReport: null,
    report: null,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--workspace-root') args.workspaceRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--readiness-report') args.readinessReport = argv[++index]
    else if (arg === '--report') args.report = argv[++index]
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

function defaultReportPath(releaseRoot) {
  return path.join(releaseRoot, MODULE_ID, 'openlands-public-alpha-evidence-intake.json')
}

function defaultMarkdownPath(reportPath) {
  const parsed = path.parse(reportPath)
  return path.join(parsed.dir, `${parsed.name}.md`)
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

export function validate({ moduleRoot, workspaceRoot, releaseRoot, reportPath, readinessReportPath }) {
  const errors = []
  assert(errors, fileExists(reportPath), `evidence intake report missing: ${reportPath}`)
  if (!fileExists(reportPath)) {
    return {
      status: 'failed',
      reportPath,
      errors,
    }
  }

  const report = readJson(reportPath)
  const markdownPath = report.markdownPath ?? defaultMarkdownPath(reportPath)
  const expected = buildEvidenceIntake({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    readinessReportPath,
    outputPath: reportPath,
    markdownPath,
    dryRun: true,
  })
  const readinessReport = readJson(readinessReportPath)
  const activeItems = (report.intakeItems ?? []).filter((item) => item.active)

  assert(errors, report.schema === SCHEMA, `evidence intake schema must be ${SCHEMA}`)
  assert(errors, report.moduleId === MODULE_ID, 'evidence intake moduleId mismatch')
  assert(errors, report.publicAlphaReady === (readinessReport.publicAlphaReady === true), 'evidence intake publicAlphaReady mismatch')
  assert(errors, sameSet(report.blockers, readinessReport.blockers ?? []), 'evidence intake blockers mismatch readiness report')
  assert(errors, report.blockerCount === (readinessReport.blockers ?? []).length, 'evidence intake blocker count mismatch')
  assert(errors, sameSet(activeItems.map((item) => item.blockerId), readinessReport.blockers ?? []), 'active evidence intake items must cover every readiness blocker')
  assert(errors, sameJson(stable(report), stable(expected)), 'evidence intake report stale against generator dry-run')

  for (const item of report.intakeItems ?? []) {
    assert(errors, typeof item.blockerId === 'string' && item.blockerId.length > 0, 'evidence intake item blockerId missing')
    assert(errors, Array.isArray(item.impactedPhases), `${item.blockerId} impactedPhases must be an array`)
    assert(errors, Array.isArray(item.proofRequired) && item.proofRequired.length > 0, `${item.blockerId} proofRequired must be non-empty`)
    assert(errors, Array.isArray(item.evidenceTargets) && item.evidenceTargets.length > 0, `${item.blockerId} evidenceTargets must be non-empty`)
    assert(errors, Array.isArray(item.validationCommands) && item.validationCommands.length > 0, `${item.blockerId} validationCommands must be non-empty`)
    for (const evidenceTarget of item.evidenceTargets ?? []) {
      assert(errors, typeof evidenceTarget.path === 'string' && evidenceTarget.path.length > 0, `${item.blockerId} evidence target path missing`)
      if (typeof evidenceTarget.path === 'string' && evidenceTarget.path.length > 0) {
        assert(errors, evidenceTarget.present === fileExists(evidenceTarget.path), `${item.blockerId} evidence target ${evidenceTarget.id} present flag mismatch`)
      }
    }
  }

  assert(errors, fileExists(markdownPath), `evidence intake markdown missing: ${markdownPath}`)
  if (fileExists(markdownPath)) {
    assert(errors, readText(markdownPath) === renderEvidenceIntakeMarkdown(report), 'evidence intake markdown stale against JSON')
  }

  return {
    status: errors.length === 0 ? 'passed' : 'failed',
    reportPath,
    markdownPath,
    publicAlphaReady: report.publicAlphaReady,
    blockerCount: report.blockerCount,
    activeIntakeItemCount: report.activeIntakeItemCount,
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
  const readinessReportPath = args.readinessReport
    ? path.resolve(args.readinessReport)
    : path.join(releaseRoot, MODULE_ID, 'openlands-release-readiness-report.json')
  const result = validate({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    reportPath,
    readinessReportPath,
  })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands public alpha evidence intake validated: activeItems=${result.activeIntakeItemCount}, blockers=${result.blockerCount}.`)
  } else {
    console.error(`Openlands public alpha evidence intake failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-public-alpha-evidence-intake.mjs [options]

Options:
  --module-root <path>        Openlands module root. Defaults to this script's module.
  --workspace-root <path>     Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>       Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --readiness-report <path>   Readiness report path. Defaults to openlands-release-readiness-report.json.
  --report <path>             Evidence intake report path. Defaults to openlands-public-alpha-evidence-intake.json.
  --json                      Print JSON output.
  --help                      Show this help.
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}

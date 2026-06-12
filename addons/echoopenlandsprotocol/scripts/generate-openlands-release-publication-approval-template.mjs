import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const TEMPLATE_SCHEMA = 'echo.openlands.release_publication_approval_template.v1'
const PUBLICATION_CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json'
const DISTRIBUTION_APPROVAL_CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json'

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    releaseRoot: null,
    verifiedManifest: null,
    output: null,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--workspace-root') args.workspaceRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--verified-manifest') args.verifiedManifest = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
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

function writeJson(filePath, payload) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
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

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function reportPath(workspaceRoot, report) {
  return path.join(workspaceRoot, report.repo, report.requiredReport)
}

function readReportSummary(reportFile) {
  if (!fileExists(reportFile)) {
    return {
      exists: false,
      sha256: '',
      status: 'missing',
      publicAlphaReady: false,
      remainingDistributionGates: null,
    }
  }
  const report = readJson(reportFile)
  return {
    exists: true,
    sha256: sha256File(reportFile),
    status: report.status ?? 'unknown',
    publicAlphaReady: report.publicAlphaReady === true,
    remainingDistributionGates: Array.isArray(report.remainingDistributionGates)
      ? report.remainingDistributionGates.length
      : null,
  }
}

function buildApprovalTemplate({ moduleRoot, workspaceRoot, releaseRoot, verifiedManifestPath, outputPath, dryRun }) {
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const publicationContract = readJson(path.join(resourcesRoot, PUBLICATION_CONTRACT_PATH))
  const distributionApprovalContract = readJson(path.join(resourcesRoot, DISTRIBUTION_APPROVAL_CONTRACT_PATH))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  assert(fileExists(releaseIndexPath), `release index not found: ${releaseIndexPath}`)
  const releaseIndex = readJson(releaseIndexPath)
  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  assert(releaseModule, `${MODULE_ID} ${VERSION} missing from release index`)

  const verifiedManifestExists = verifiedManifestPath ? fileExists(verifiedManifestPath) : false
  const verifiedManifest = verifiedManifestExists ? readJson(verifiedManifestPath) : null
  if (verifiedManifest) {
    assert(verifiedManifest.schema === publicationContract.reportContract?.schema, 'verified manifest schema mismatch')
    assert(verifiedManifest.moduleId === MODULE_ID, 'verified manifest module id mismatch')
    assert(verifiedManifest.moduleVersion === VERSION, 'verified manifest version mismatch')
    assert(verifiedManifest.releaseId === releaseIndex.releaseId, 'verified manifest release id mismatch')
  }

  const distributionReports = (distributionApprovalContract.editionReports ?? []).map((report) => {
    const absolutePath = reportPath(workspaceRoot, report)
    const summary = readReportSummary(absolutePath)
    return {
      edition: report.edition,
      runtimeTarget: report.runtimeTarget,
      repo: report.repo,
      path: report.requiredReport,
      absolutePath,
      sha256: summary.sha256,
      status: summary.status,
      publicAlphaReady: summary.publicAlphaReady,
      remainingDistributionGates: summary.remainingDistributionGates,
      readyForApproval: summary.status === 'passed' && summary.publicAlphaReady === true && summary.remainingDistributionGates === 0,
    }
  })

  const approvalSchema = publicationContract.approvalContract?.schema ?? 'echo.openlands.release_publication_approval.v1'
  const checklist = (publicationContract.approvalContract?.requiredChecklistIds ?? []).map((id) => ({
    id,
    status: 'blocked',
  }))
  const approvalDraft = {
    schema: approvalSchema,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    approver: '',
    approvedAt: '',
    releaseIndexPatch: {
      patchId: '',
      releaseIndexCommit: '',
    },
    distributionApproval: {
      signoffId: '',
      approver: '',
      approvedAt: '',
      reports: distributionReports.map((report) => ({
        edition: report.edition,
        path: report.path,
        sha256: report.sha256,
      })),
    },
    checklist,
  }

  return {
    schema: TEMPLATE_SCHEMA,
    generatedAt: new Date().toISOString(),
    dryRun,
    templateOnly: true,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    releaseIndexPath,
    releaseIndexSha256: sha256File(releaseIndexPath),
    releasePublicationManifestContract: PUBLICATION_CONTRACT_PATH,
    distributionApprovalContract: DISTRIBUTION_APPROVAL_CONTRACT_PATH,
    verifiedManifestPath,
    verifiedManifestPresent: verifiedManifestExists,
    verifiedManifestStatus: verifiedManifest?.status ?? 'missing',
    requiredChecklistIds: publicationContract.approvalContract?.requiredChecklistIds ?? [],
    distributionReports,
    approvalDraft,
    nextSteps: [
      'Do not pass this template file directly to approve-openlands-release-publication.mjs.',
      'After public downloads verify, copy approvalDraft into a real approval JSON file.',
      'Fill approver, approvedAt, releaseIndexPatch.patchId, releaseIndexPatch.releaseIndexCommit, and distributionApproval signoff fields.',
      'Regenerate the template after Native, NeoForge, and Standalone distribution approval reports are passed so report hashes are current.',
      'Change every approvalDraft.checklist status to passed only after the matching evidence has been reviewed.',
    ],
    approvalCommand: [
      'node',
      'addons/echoopenlandsprotocol/scripts/approve-openlands-release-publication.mjs',
      '--module-root',
      'addons/echoopenlandsprotocol',
      '--workspace-root',
      'C:/Development/Github',
      '--approval',
      'C:/path/to/openlands-publication-approval.json',
      '--release-index-out',
      path.join(releaseRoot, MODULE_ID, 'echo-release.approved.preview.json'),
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
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const workspaceRoot = args.workspaceRoot ? path.resolve(args.workspaceRoot) : defaultWorkspaceRoot(moduleRoot)
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const verifiedManifestPath = args.verifiedManifest
    ? path.resolve(args.verifiedManifest)
    : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.verified.json')
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-approval.template.json')
  const template = buildApprovalTemplate({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    verifiedManifestPath,
    outputPath,
    dryRun: args.dryRun,
  })
  if (!args.dryRun) writeJson(outputPath, template)
  if (args.json) {
    console.log(JSON.stringify(template, null, 2))
  } else {
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    const readyReports = template.distributionReports.filter((report) => report.readyForApproval).length
    console.log(`Openlands publication approval template ${action}: distributionReportsReady=${readyReports}/${template.distributionReports.length}, verifiedManifest=${template.verifiedManifestStatus}.`)
  }
  return template
}

function printHelp() {
  console.log(`Usage: node generate-openlands-release-publication-approval-template.mjs [options]

Options:
  --module-root <path>        Openlands module root. Defaults to this script's module.
  --workspace-root <path>     Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>       Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --verified-manifest <path>  Verified publication manifest path. Defaults to openlands-release-publication-manifest.verified.json.
  --out <path>                Template output path. Defaults to openlands-release-publication-approval.template.json.
  --dry-run                   Generate without writing.
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

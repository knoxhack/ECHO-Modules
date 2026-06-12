import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { validatePublicationManifest } from './validate-openlands-release-publication-manifest.mjs'
import { isPublicHttpsUrl } from './openlands-public-download-url.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json'
const DISTRIBUTION_APPROVAL_CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json'
const APPROVAL_SCHEMA = 'echo.openlands.release_publication_approval.v1'
const REQUIRED_CHECKLIST = [
  'all_public_downloads_verified',
  'release_index_patch_reviewed',
  'distribution_approval_signoff_recorded',
  'rollback_plan_attached',
]

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    releaseRoot: null,
    verifiedManifest: null,
    approval: null,
    output: null,
    releaseIndexOut: null,
    applyReleaseIndex: false,
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
    else if (arg === '--approval') args.approval = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--release-index-out') args.releaseIndexOut = argv[++index]
    else if (arg === '--apply-release-index') args.applyReleaseIndex = true
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

function sha256Text(text) {
  return crypto.createHash('sha256').update(text).digest('hex')
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
}

function defaultWorkspaceRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', '..')
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function sortedUnique(values) {
  return [...new Set((values ?? []).filter(Boolean))].sort()
}

function sameSet(actual, expected) {
  return JSON.stringify(sortedUnique(actual)) === JSON.stringify(sortedUnique(expected))
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function publicationById(manifest, id) {
  return (manifest?.artifactPublications ?? []).find((publication) => publication.id === id)
}

function checklistStatus(approval, id) {
  return (approval.checklist ?? []).find((entry) => entry.id === id)?.status
}

function resolveDistributionApprovalReportPath(workspaceRoot, expectedReport, declaredPath) {
  if (typeof declaredPath !== 'string' || declaredPath.length === 0) return null
  return path.isAbsolute(declaredPath)
    ? path.resolve(declaredPath)
    : path.resolve(workspaceRoot, expectedReport.repo, declaredPath)
}

function validateDistributionApprovalReports({ approval, distributionApprovalContract, workspaceRoot, releaseIndex, releaseIndexPath, releaseId }) {
  const expectedReports = distributionApprovalContract.editionReports ?? []
  const expectedGateIds = sortedUnique((distributionApprovalContract.distributionGates ?? []).map((gate) => gate.id))
  assert(Array.isArray(approval.distributionApproval?.reports), 'approval distributionApproval.reports is required')
  const approvalReports = approval.distributionApproval.reports
  assert(sameSet(approvalReports.map((entry) => entry.edition), expectedReports.map((entry) => entry.edition)), 'approval distributionApproval.reports must cover every edition')
  for (const expectedReport of expectedReports) {
    const approvalReport = approvalReports.find((entry) => entry.edition === expectedReport.edition)
    assert(approvalReport !== undefined, `approval distributionApproval missing ${expectedReport.edition} report`)
    assert(typeof approvalReport.path === 'string' && approvalReport.path.length > 0, `approval distributionApproval ${expectedReport.edition} report path is required`)
    assert(typeof approvalReport.sha256 === 'string' && /^[a-f0-9]{64}$/i.test(approvalReport.sha256), `approval distributionApproval ${expectedReport.edition} report sha256 is required`)
    const reportPath = resolveDistributionApprovalReportPath(workspaceRoot, expectedReport, approvalReport.path)
    const expectedPath = path.resolve(workspaceRoot, expectedReport.repo, expectedReport.requiredReport)
    assert(reportPath === expectedPath, `approval distributionApproval ${expectedReport.edition} report path must match ${expectedReport.requiredReport}`)
    assert(fileExists(reportPath), `approval distributionApproval ${expectedReport.edition} report not found: ${reportPath}`)
    const reportSha256 = sha256File(reportPath)
    assert(approvalReport.sha256 === reportSha256, `approval distributionApproval ${expectedReport.edition} report sha256 mismatch`)
    const report = readJson(reportPath)
    assert(report.schema === distributionApprovalContract.reportContract?.schema, `approval distributionApproval ${expectedReport.edition} report schema mismatch`)
    assert(report.status === 'passed', `approval distributionApproval ${expectedReport.edition} report must be passed`)
    assert(report.publicAlphaReady === true, `approval distributionApproval ${expectedReport.edition} report must be publicAlphaReady`)
    assert(report.edition === expectedReport.edition, `approval distributionApproval ${expectedReport.edition} report edition mismatch`)
    assert(report.runtimeTarget === expectedReport.runtimeTarget, `approval distributionApproval ${expectedReport.edition} runtime target mismatch`)
    assert(report.moduleId === MODULE_ID, `approval distributionApproval ${expectedReport.edition} module id mismatch`)
    assert(report.moduleVersion === VERSION, `approval distributionApproval ${expectedReport.edition} module version mismatch`)
    assert(report.releaseId === releaseId, `approval distributionApproval ${expectedReport.edition} release id mismatch`)
    assert(sameSet(report.clearedDistributionGates, expectedGateIds), `approval distributionApproval ${expectedReport.edition} must clear every distribution gate`)
    assert((report.remainingDistributionGates ?? []).length === 0, `approval distributionApproval ${expectedReport.edition} must have no remaining distribution gates`)
    assert((report.approvalResults ?? []).every((area) => area.status === 'passed'), `approval distributionApproval ${expectedReport.edition} requires every approval area passed`)
    assert((report.approvalResults ?? []).every((area) => (area.checklist ?? []).every((item) => item.status === 'passed')), `approval distributionApproval ${expectedReport.edition} requires every checklist item passed`)
    assert(typeof report.approvalRun?.approver === 'string' && report.approvalRun.approver.length > 0, `approval distributionApproval ${expectedReport.edition} report approver is required`)
    assert(typeof report.approvalRun?.approvalDate === 'string' && report.approvalRun.approvalDate.length > 0, `approval distributionApproval ${expectedReport.edition} report approvalDate is required`)
    assert(report.releaseIndex?.path === releaseIndexPath, `approval distributionApproval ${expectedReport.edition} release index path mismatch`)
    assert(report.releaseIndex?.hash === sha256File(releaseIndexPath), `approval distributionApproval ${expectedReport.edition} release index hash mismatch`)
    assert(report.releaseIndex?.artifactDownloadUrlsPresent === true, `approval distributionApproval ${expectedReport.edition} requires artifact download URLs`)
    assert(report.releaseIndex?.approvedState === true, `approval distributionApproval ${expectedReport.edition} requires approved release index state`)
  }
}

function validateApproval({ approval, manifest, releaseIndex, releaseIndexPath, distributionApprovalContract, workspaceRoot }) {
  assert(approval.schema === APPROVAL_SCHEMA, `approval schema must be ${APPROVAL_SCHEMA}`)
  assert(approval.moduleId === MODULE_ID, 'approval moduleId mismatch')
  assert(approval.moduleVersion === VERSION, 'approval moduleVersion mismatch')
  assert(approval.releaseId === manifest.releaseId, 'approval releaseId must match verified manifest')
  assert(approval.releaseId === releaseIndex.releaseId, 'approval releaseId must match Release Index')
  assert(typeof approval.approver === 'string' && approval.approver.length > 0, 'approval approver is required')
  assert(typeof approval.approvedAt === 'string' && approval.approvedAt.length > 0, 'approval approvedAt is required')
  assert(approval.releaseIndexPatch && typeof approval.releaseIndexPatch === 'object', 'approval releaseIndexPatch is required')
  assert(typeof approval.releaseIndexPatch.patchId === 'string' && approval.releaseIndexPatch.patchId.length > 0, 'approval releaseIndexPatch.patchId is required')
  assert(typeof approval.releaseIndexPatch.releaseIndexCommit === 'string' && approval.releaseIndexPatch.releaseIndexCommit.length > 0, 'approval releaseIndexPatch.releaseIndexCommit is required')
  assert(approval.distributionApproval && typeof approval.distributionApproval === 'object', 'approval distributionApproval is required')
  assert(typeof approval.distributionApproval.signoffId === 'string' && approval.distributionApproval.signoffId.length > 0, 'approval distributionApproval.signoffId is required')
  assert(typeof approval.distributionApproval.approver === 'string' && approval.distributionApproval.approver.length > 0, 'approval distributionApproval.approver is required')
  assert(typeof approval.distributionApproval.approvedAt === 'string' && approval.distributionApproval.approvedAt.length > 0, 'approval distributionApproval.approvedAt is required')
  validateDistributionApprovalReports({
    approval,
    distributionApprovalContract,
    workspaceRoot,
    releaseIndex,
    releaseIndexPath,
    releaseId: manifest.releaseId,
  })
  for (const id of REQUIRED_CHECKLIST) {
    assert(checklistStatus(approval, id) === 'passed', `approval checklist ${id} must be passed`)
  }
}

function buildApprovedPublication({ moduleRoot, workspaceRoot, releaseRoot, verifiedManifestPath, approvalPath, outputPath, releaseIndexOut, applyReleaseIndex, dryRun }) {
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const contract = readJson(path.join(resourcesRoot, CONTRACT_PATH))
  const distributionApprovalContract = readJson(path.join(resourcesRoot, DISTRIBUTION_APPROVAL_CONTRACT_PATH))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  assert(!(applyReleaseIndex && releaseIndexOut), '--apply-release-index and --release-index-out are mutually exclusive')
  assert(!releaseIndexOut || path.resolve(releaseIndexOut) !== path.resolve(releaseIndexPath), '--release-index-out must not point to live echo-release.json')
  assert(fileExists(releaseIndexPath), `release index not found: ${releaseIndexPath}`)
  assert(fileExists(verifiedManifestPath), `verified publication manifest not found: ${verifiedManifestPath}`)
  assert(fileExists(approvalPath), `approval file not found: ${approvalPath}`)
  assert(applyReleaseIndex || dryRun || releaseIndexOut, '--apply-release-index is required unless --dry-run or --release-index-out is provided')

  const verifiedManifestValidation = validatePublicationManifest({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    manifestPath: verifiedManifestPath,
  })
  assert(
    verifiedManifestValidation.status === 'passed',
    `verified publication manifest validation failed: ${verifiedManifestValidation.errors.join('; ')}`,
  )
  assert(verifiedManifestValidation.manifestStatus === 'verified', 'input manifest must have status verified')

  const releaseIndex = readJson(releaseIndexPath)
  const verifiedManifest = readJson(verifiedManifestPath)
  const approval = readJson(approvalPath)
  assert(verifiedManifest.schema === contract.reportContract?.schema, 'verified manifest schema mismatch')
  assert(verifiedManifest.status === 'verified', 'input manifest must have status verified')
  assert(verifiedManifest.moduleId === MODULE_ID, 'verified manifest module id mismatch')
  assert(verifiedManifest.moduleVersion === VERSION, 'verified manifest module version mismatch')
  assert(verifiedManifest.releaseId === releaseIndex.releaseId, 'verified manifest release id mismatch')
  validateApproval({
    approval,
    manifest: verifiedManifest,
    releaseIndex,
    releaseIndexPath,
    distributionApprovalContract,
    workspaceRoot,
  })

  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  assert(releaseModule, `${MODULE_ID} ${VERSION} missing from release index`)
  const patchedReleaseIndex = JSON.parse(JSON.stringify(releaseIndex))
  const patchedModule = (patchedReleaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  assert(patchedModule, `${MODULE_ID} ${VERSION} missing from patched release index`)

  const artifactPublications = (contract.artifactTargets ?? []).map((target) => {
    const publication = publicationById(verifiedManifest, target.id)
    assert(publication, `verified manifest missing artifact ${target.id}`)
    assert(publication.file === target.file, `verified manifest file mismatch for ${target.id}`)
    assert(publication.urlStatus === 'download_verified', `verified manifest artifact ${target.id} must be download_verified`)
    assert(typeof publication.downloadUrl === 'string' && publication.downloadUrl.length > 0, `verified manifest artifact ${target.id} missing downloadUrl`)
    assert(isPublicHttpsUrl(publication.downloadUrl), `verified manifest artifact ${target.id} downloadUrl must use a public https URL`)
    assert(publication.downloadVerification?.downloadAttempted === true, `verified manifest artifact ${target.id} missing download attempt`)
    assert(publication.downloadVerification?.sha256Matches === true, `verified manifest artifact ${target.id} sha mismatch`)
    assert(publication.downloadVerification?.sizeMatches === true, `verified manifest artifact ${target.id} size mismatch`)
    assert(publication.downloadVerification?.downloadedSha256 === publication.sha256, `verified manifest artifact ${target.id} downloaded sha mismatch`)
    assert(publication.downloadVerification?.downloadedSize === publication.size, `verified manifest artifact ${target.id} downloaded size mismatch`)

    const releaseArtifact = artifactByFile(releaseModule, target.file)
    const patchedArtifact = artifactByFile(patchedModule, target.file)
    assert(releaseArtifact, `release index missing artifact ${target.file}`)
    assert(patchedArtifact, `patched release index missing artifact ${target.file}`)
    assert(releaseArtifact.sha256 === publication.sha256, `release index sha mismatch for ${target.id}`)
    assert(releaseArtifact.size === publication.size, `release index size mismatch for ${target.id}`)
    patchedArtifact.downloadUrl = publication.downloadUrl

    return {
      ...publication,
      urlStatus: 'approved',
      releaseIndexPatch: {
        target: 'modules[].artifacts[].downloadUrl',
        match: {
          moduleId: MODULE_ID,
          version: VERSION,
          filename: target.file,
          sha256: publication.sha256,
          size: publication.size,
        },
        patchAllowed: true,
        patchApplied: true,
        releaseIndexCommit: approval.releaseIndexPatch.releaseIndexCommit,
        patchId: approval.releaseIndexPatch.patchId,
      },
    }
  })

  const approvedManifest = {
    ...verifiedManifest,
    status: 'approved',
    generatedAt: new Date().toISOString(),
    dryRun,
    artifactPublications,
    blockedBy: [],
    nextSteps: [
      'Attach this approved publication manifest to distribution approval evidence.',
      'Publish the patched Release Index through the approved release channel.',
    ],
    summary: {
      artifactCount: artifactPublications.length,
      missingDownloadUrlCount: artifactPublications.filter((artifact) => !artifact.downloadUrl).length,
      downloadVerifiedCount: artifactPublications.filter((artifact) => artifact.downloadVerification.sha256Matches && artifact.downloadVerification.sizeMatches).length,
      releaseIndexPatchAllowedCount: artifactPublications.filter((artifact) => artifact.releaseIndexPatch.patchAllowed).length,
    },
    approval: {
      schema: approval.schema,
      approver: approval.approver,
      approvedAt: approval.approvedAt,
      releaseIndexPatch: approval.releaseIndexPatch,
      distributionApproval: approval.distributionApproval,
      checklist: approval.checklist,
      approvalFile: approvalPath,
      approvalFileSha256: sha256File(approvalPath),
    },
    inputVerifiedManifestPath: verifiedManifestPath,
    outputPath,
  }

  const patchedReleaseIndexText = `${JSON.stringify(patchedReleaseIndex, null, 2)}\n`
  const releaseIndexPatchSummary = {
    schema: 'echo.openlands.release_publication_release_index_patch_summary.v1',
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    patchId: approval.releaseIndexPatch.patchId,
    releaseIndexCommit: approval.releaseIndexPatch.releaseIndexCommit,
    releaseIndexPath,
    releaseIndexOut,
    applyReleaseIndex,
    artifactPatches: artifactPublications.map((publication) => ({
      id: publication.id,
      file: publication.file,
      downloadUrl: publication.downloadUrl,
      sha256: publication.sha256,
      size: publication.size,
    })),
    patchedReleaseIndexSha256: sha256Text(patchedReleaseIndexText),
  }

  if (!dryRun) {
    writeJson(outputPath, approvedManifest)
    const releaseIndexTarget = applyReleaseIndex ? releaseIndexPath : releaseIndexOut
    writeJson(releaseIndexTarget, patchedReleaseIndex)
    writeJson(path.join(path.dirname(outputPath), 'openlands-release-index-patch-summary.json'), releaseIndexPatchSummary)
  }

  return {
    approvedManifest,
    patchedReleaseIndex,
    releaseIndexPatchSummary,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  if (!args.approval) throw new Error('--approval is required')
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const workspaceRoot = args.workspaceRoot ? path.resolve(args.workspaceRoot) : defaultWorkspaceRoot(moduleRoot)
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const verifiedManifestPath = args.verifiedManifest
    ? path.resolve(args.verifiedManifest)
    : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.verified.json')
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.json')
  const releaseIndexOut = args.releaseIndexOut ? path.resolve(args.releaseIndexOut) : null
  const result = buildApprovedPublication({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    verifiedManifestPath,
    approvalPath: path.resolve(args.approval),
    outputPath,
    releaseIndexOut,
    applyReleaseIndex: args.applyReleaseIndex,
    dryRun: args.dryRun,
  })
  if (args.json) {
    console.log(JSON.stringify({
      status: result.approvedManifest.status,
      moduleId: result.approvedManifest.moduleId,
      moduleVersion: result.approvedManifest.moduleVersion,
      releaseId: result.approvedManifest.releaseId,
      artifactCount: result.approvedManifest.summary.artifactCount,
      downloadVerifiedCount: result.approvedManifest.summary.downloadVerifiedCount,
      patchAllowedCount: result.approvedManifest.summary.releaseIndexPatchAllowedCount,
      applyReleaseIndex: result.releaseIndexPatchSummary.applyReleaseIndex,
      dryRun: args.dryRun,
    }, null, 2))
  } else {
    const action = args.dryRun ? 'approved dry-run' : `wrote ${outputPath}`
    console.log(`Openlands release publication ${action}: artifacts=${result.approvedManifest.summary.artifactCount}, patchAllowed=${result.approvedManifest.summary.releaseIndexPatchAllowedCount}, applyReleaseIndex=${result.releaseIndexPatchSummary.applyReleaseIndex}.`)
  }
  return result
}

function printHelp() {
  console.log(`Usage: node approve-openlands-release-publication.mjs --approval <approval.json> [options]

Options:
  --approval <path>           Required approval/signoff JSON using schema ${APPROVAL_SCHEMA}.
  --module-root <path>        Openlands module root. Defaults to this script's module.
  --workspace-root <path>     Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>       Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --verified-manifest <path>  Input verified manifest. Defaults to openlands-release-publication-manifest.verified.json.
  --out <path>                Approved manifest output. Defaults to openlands-release-publication-manifest.json.
  --release-index-out <path>  Write patched Release Index to a separate file instead of live echo-release.json.
  --apply-release-index       Required for live echo-release.json mutation.
                              Mutually exclusive with --release-index-out.
  --dry-run                   Build the approved manifest in memory without writing files.
  --json                      Print compact JSON output.
  --help                      Show this help.

Approval checklist ids that must be passed:
  ${REQUIRED_CHECKLIST.join(', ')}

distributionApproval.reports must include native, neoforge, and standalone entries with:
  edition, path, sha256
`)
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error))
    process.exitCode = 1
  })
}

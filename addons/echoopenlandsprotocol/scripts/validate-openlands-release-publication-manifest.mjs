import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { pathToFileURL } from 'node:url'
import { isPublicHttpsUrl } from './openlands-public-download-url.mjs'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json'
const DISTRIBUTION_APPROVAL_CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json'
const APPROVAL_SCHEMA = 'echo.openlands.release_publication_approval.v1'
const DOWNLOAD_VERIFICATION_SUMMARY_SCHEMA = 'echo.openlands.release_publication_download_verification_summary.v1'

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    releaseRoot: null,
    manifest: null,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--workspace-root') args.workspaceRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--manifest') args.manifest = argv[++index]
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

function readJsonIfPossible(filePath) {
  try {
    return readJson(filePath)
  } catch {
    return null
  }
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function assert(errors, condition, message) {
  if (!condition) errors.push(message)
}

function sorted(values) {
  return [...(values ?? [])].sort()
}

function sortedUnique(values) {
  return [...new Set((values ?? []).filter(Boolean))].sort()
}

function sameSet(actual, expected) {
  return JSON.stringify(sorted(actual)) === JSON.stringify(sorted(expected))
}

function sameUniqueSet(actual, expected) {
  return JSON.stringify(sortedUnique(actual)) === JSON.stringify(sortedUnique(expected))
}

function deepEqual(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function stableGeneratedPublicationManifest(manifest) {
  if (!manifest || typeof manifest !== 'object') return manifest
  const { generatedAt, dryRun, ...stableManifest } = manifest
  return stableManifest
}

function sameResolvedPath(actual, expected) {
  if (typeof actual !== 'string' || typeof expected !== 'string') return false
  return path.resolve(actual) === path.resolve(expected)
}

function checklistStatus(source, id) {
  return (source?.checklist ?? []).find((entry) => entry.id === id)?.status
}

function findModuleRoot(explicitRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = process.cwd()
  for (;;) {
    const direct = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(direct)) return cursor
    const nested = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fileExists(nested)) return path.join(cursor, 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function defaultWorkspaceRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', '..')
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function publicationById(manifest, id) {
  return (manifest?.artifactPublications ?? []).find((publication) => publication.id === id)
}

function normalizeRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
}

function urlMapEntry(urlMap, id) {
  if (!urlMap) return null
  if (Array.isArray(urlMap.artifactUrls)) return urlMap.artifactUrls.find((entry) => entry.id === id) ?? null
  if (urlMap.urls && typeof urlMap.urls === 'object') {
    const value = urlMap.urls[id]
    if (typeof value === 'string') return { id, downloadUrl: value }
    if (value && typeof value === 'object') return { id, ...value }
  }
  const value = urlMap[id]
  if (typeof value === 'string') return { id, downloadUrl: value }
  if (value && typeof value === 'object') return { id, ...value }
  return null
}

function resolvePathFromCandidates(value, basePaths) {
  if (typeof value !== 'string' || value.length === 0) return null
  if (path.isAbsolute(value)) return path.resolve(value)
  const candidates = basePaths.filter(Boolean).map((basePath) => path.resolve(basePath, value))
  return candidates.find((candidate) => fileExists(candidate)) ?? candidates[0] ?? null
}

function resolveVerificationRoot(manifest, manifestPath, releaseRoot, workspaceRoot) {
  const basePaths = [path.dirname(manifestPath), releaseRoot, workspaceRoot]
  if (typeof manifest.verificationRoot === 'string' && manifest.verificationRoot.length > 0) {
    return resolvePathFromCandidates(manifest.verificationRoot, basePaths)
  }
  return path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-verification-artifacts')
}

function resolveDistributionApprovalReportPath(workspaceRoot, expectedReport, declaredPath) {
  if (typeof declaredPath !== 'string' || declaredPath.length === 0) return null
  return path.isAbsolute(declaredPath)
    ? path.resolve(declaredPath)
    : path.resolve(workspaceRoot, expectedReport.repo, declaredPath)
}

function validateDistributionApprovalReports(errors, { approval, manifestApproval, distributionApprovalContract, workspaceRoot, releaseIndexPath, releaseId }) {
  const expectedReports = distributionApprovalContract.editionReports ?? []
  const expectedReportEditions = expectedReports.map((entry) => entry.edition)
  const expectedGateIds = sortedUnique((distributionApprovalContract.distributionGates ?? []).map((gate) => gate.id))
  const approvalReports = approval.distributionApproval?.reports
  const manifestReports = manifestApproval?.distributionApproval?.reports
  assert(errors, Array.isArray(approvalReports), 'approved publication approval distributionApproval.reports is required')
  assert(errors, Array.isArray(manifestReports), 'approved publication manifest embedded distributionApproval.reports is required')
  if (!Array.isArray(approvalReports)) return

  assert(errors, approvalReports.length === expectedReports.length, 'approved publication approval distributionApproval.reports count mismatch')
  assert(errors, sameUniqueSet(approvalReports.map((entry) => entry.edition), expectedReportEditions), 'approved publication approval distributionApproval.reports must cover every edition')
  if (Array.isArray(manifestReports)) {
    assert(errors, manifestReports.length === expectedReports.length, 'approved publication manifest embedded distributionApproval.reports count mismatch')
    assert(errors, sameUniqueSet(manifestReports.map((entry) => entry.edition), expectedReportEditions), 'approved publication manifest embedded distributionApproval.reports must cover every edition')
  }

  const releaseIndexHash = fileExists(releaseIndexPath) ? sha256File(releaseIndexPath) : null
  for (const expectedReport of expectedReports) {
    const approvalReport = approvalReports.find((entry) => entry.edition === expectedReport.edition)
    assert(errors, approvalReport !== undefined, `approved publication approval missing ${expectedReport.edition} distribution report`)
    if (!approvalReport) continue
    for (const field of ['edition', 'path', 'sha256']) {
      assert(errors, approvalReport[field] !== undefined && approvalReport[field] !== null, `approved publication approval ${expectedReport.edition} report missing ${field}`)
    }
    assert(errors, typeof approvalReport.path === 'string' && approvalReport.path.length > 0, `approved publication approval ${expectedReport.edition} report path is required`)
    assert(errors, typeof approvalReport.sha256 === 'string' && /^[a-f0-9]{64}$/i.test(approvalReport.sha256), `approved publication approval ${expectedReport.edition} report sha256 is required`)

    const embeddedReport = Array.isArray(manifestReports)
      ? manifestReports.find((entry) => entry.edition === expectedReport.edition)
      : null
    if (embeddedReport) {
      assert(errors, embeddedReport.path === approvalReport.path, `approved publication manifest embedded ${expectedReport.edition} report path mismatch`)
      assert(errors, embeddedReport.sha256 === approvalReport.sha256, `approved publication manifest embedded ${expectedReport.edition} report sha256 mismatch`)
    }

    const reportPath = resolveDistributionApprovalReportPath(workspaceRoot, expectedReport, approvalReport.path)
    const expectedPath = path.resolve(workspaceRoot, expectedReport.repo, expectedReport.requiredReport)
    assert(errors, reportPath === expectedPath, `approved publication approval ${expectedReport.edition} report path must match ${expectedReport.requiredReport}`)
    assert(errors, reportPath !== null && fileExists(reportPath), `approved publication approval ${expectedReport.edition} report not found: ${reportPath ?? approvalReport.path}`)
    if (!reportPath || !fileExists(reportPath)) continue

    const reportSha256 = sha256File(reportPath)
    assert(errors, approvalReport.sha256 === reportSha256, `approved publication approval ${expectedReport.edition} report sha256 mismatch`)
    const report = readJsonIfPossible(reportPath)
    assert(errors, report !== null, `approved publication approval ${expectedReport.edition} report is not valid JSON`)
    if (!report) continue

    assert(errors, report.schema === distributionApprovalContract.reportContract?.schema, `approved publication approval ${expectedReport.edition} report schema mismatch`)
    assert(errors, report.status === 'passed', `approved publication approval ${expectedReport.edition} report must be passed`)
    assert(errors, report.publicAlphaReady === true, `approved publication approval ${expectedReport.edition} report must be publicAlphaReady`)
    assert(errors, report.edition === expectedReport.edition, `approved publication approval ${expectedReport.edition} report edition mismatch`)
    assert(errors, report.runtimeTarget === expectedReport.runtimeTarget, `approved publication approval ${expectedReport.edition} runtime target mismatch`)
    assert(errors, report.moduleId === MODULE_ID, `approved publication approval ${expectedReport.edition} module id mismatch`)
    assert(errors, report.moduleVersion === VERSION, `approved publication approval ${expectedReport.edition} module version mismatch`)
    assert(errors, report.releaseId === releaseId, `approved publication approval ${expectedReport.edition} release id mismatch`)
    assert(errors, sameUniqueSet(report.clearedDistributionGates, expectedGateIds), `approved publication approval ${expectedReport.edition} must clear every distribution gate`)
    assert(errors, (report.remainingDistributionGates ?? []).length === 0, `approved publication approval ${expectedReport.edition} must have no remaining distribution gates`)
    assert(errors, (report.approvalResults ?? []).every((area) => area.status === 'passed'), `approved publication approval ${expectedReport.edition} requires every approval area passed`)
    assert(errors, (report.approvalResults ?? []).every((area) => (area.checklist ?? []).every((item) => item.status === 'passed')), `approved publication approval ${expectedReport.edition} requires every checklist item passed`)
    assert(errors, typeof report.approvalRun?.approver === 'string' && report.approvalRun.approver.length > 0, `approved publication approval ${expectedReport.edition} report approver is required`)
    assert(errors, typeof report.approvalRun?.approvalDate === 'string' && report.approvalRun.approvalDate.length > 0, `approved publication approval ${expectedReport.edition} report approvalDate is required`)
    assert(errors, report.releaseIndex?.path === releaseIndexPath, `approved publication approval ${expectedReport.edition} release index path mismatch`)
    assert(errors, releaseIndexHash !== null && report.releaseIndex?.hash === releaseIndexHash, `approved publication approval ${expectedReport.edition} release index hash mismatch`)
    assert(errors, report.releaseIndex?.artifactDownloadUrlsPresent === true, `approved publication approval ${expectedReport.edition} requires artifact download URLs`)
    assert(errors, report.releaseIndex?.approvedState === true, `approved publication approval ${expectedReport.edition} requires approved release index state`)
  }
}

function releaseIndexPatchSummaryPath(manifestPath) {
  return path.join(path.dirname(manifestPath), 'openlands-release-index-patch-summary.json')
}

function resolveReleaseIndexOutPath(summary, manifestPath, releaseIndexPath) {
  if (typeof summary?.releaseIndexOut !== 'string' || summary.releaseIndexOut.length === 0) return null
  return resolvePathFromCandidates(summary.releaseIndexOut, [
    path.dirname(manifestPath),
    path.dirname(releaseIndexPath),
  ])
}

function approvedReleaseModuleFromPatchSummary({ manifestPath, releaseIndexPath, releaseIndex }) {
  const summaryPath = releaseIndexPatchSummaryPath(manifestPath)
  const summary = fileExists(summaryPath) ? readJsonIfPossible(summaryPath) : null
  if (summary?.applyReleaseIndex !== false) {
    return (releaseIndex?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  }
  const releaseIndexOutPath = resolveReleaseIndexOutPath(summary, manifestPath, releaseIndexPath)
  const releaseIndexOut = releaseIndexOutPath && fileExists(releaseIndexOutPath)
    ? readJsonIfPossible(releaseIndexOutPath)
    : null
  return (releaseIndexOut?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
}

function validateReleaseIndexPatchSummary(errors, { manifestPath, approval, manifestApproval, releaseIndexPath, publications }) {
  const summaryPath = releaseIndexPatchSummaryPath(manifestPath)
  assert(errors, fileExists(summaryPath), `approved publication release index patch summary not found: ${summaryPath}`)
  const summary = fileExists(summaryPath) ? readJsonIfPossible(summaryPath) : null
  assert(errors, summary !== null, 'approved publication release index patch summary is not valid JSON')
  if (!summary) return

  assert(errors, summary.schema === 'echo.openlands.release_publication_release_index_patch_summary.v1', 'approved publication release index patch summary schema mismatch')
  assert(errors, summary.moduleId === MODULE_ID, 'approved publication release index patch summary module id mismatch')
  assert(errors, summary.moduleVersion === VERSION, 'approved publication release index patch summary version mismatch')
  assert(errors, summary.releaseId === approval.releaseId, 'approved publication release index patch summary release id mismatch')
  assert(errors, summary.patchId === approval.releaseIndexPatch?.patchId, 'approved publication release index patch summary patch id mismatch')
  assert(errors, summary.patchId === manifestApproval.releaseIndexPatch?.patchId, 'approved publication release index patch summary embedded patch id mismatch')
  assert(errors, summary.releaseIndexCommit === approval.releaseIndexPatch?.releaseIndexCommit, 'approved publication release index patch summary commit mismatch')
  assert(errors, summary.releaseIndexCommit === manifestApproval.releaseIndexPatch?.releaseIndexCommit, 'approved publication release index patch summary embedded commit mismatch')
  assert(errors, sameResolvedPath(summary.releaseIndexPath, releaseIndexPath), 'approved publication release index patch summary release index path mismatch')
  assert(errors, typeof summary.applyReleaseIndex === 'boolean', 'approved publication release index patch summary applyReleaseIndex must be boolean')
  assert(errors, typeof summary.patchedReleaseIndexSha256 === 'string' && /^[a-f0-9]{64}$/i.test(summary.patchedReleaseIndexSha256), 'approved publication release index patch summary patched sha256 is required')

  let patchedReleaseIndexPath = releaseIndexPath
  if (summary.applyReleaseIndex === false) {
    assert(errors, typeof summary.releaseIndexOut === 'string' && summary.releaseIndexOut.length > 0, 'approved publication release index patch summary requires releaseIndexOut when applyReleaseIndex is false')
    const releaseIndexOutPath = resolveReleaseIndexOutPath(summary, manifestPath, releaseIndexPath)
    assert(errors, releaseIndexOutPath !== null && fileExists(releaseIndexOutPath), `approved publication release index patch summary releaseIndexOut not found: ${releaseIndexOutPath ?? summary.releaseIndexOut}`)
    assert(errors, releaseIndexOutPath !== null && !sameResolvedPath(releaseIndexOutPath, releaseIndexPath), 'approved publication release index patch summary releaseIndexOut must not point to live echo-release.json')
    patchedReleaseIndexPath = releaseIndexOutPath
  } else {
    assert(errors, typeof summary.releaseIndexOut !== 'string' || summary.releaseIndexOut.length === 0, 'approved publication release index patch summary releaseIndexOut must be empty when applyReleaseIndex is true')
  }

  const patchedReleaseIndex = patchedReleaseIndexPath && fileExists(patchedReleaseIndexPath)
    ? readJsonIfPossible(patchedReleaseIndexPath)
    : null
  assert(errors, patchedReleaseIndex !== null, `approved publication patched Release Index is not valid JSON: ${patchedReleaseIndexPath}`)
  if (patchedReleaseIndexPath && fileExists(patchedReleaseIndexPath)) {
    assert(errors, summary.patchedReleaseIndexSha256 === sha256File(patchedReleaseIndexPath), 'approved publication release index patch summary patched sha256 mismatch')
  }
  const patchedReleaseModule = (patchedReleaseIndex?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  if (patchedReleaseIndex) {
    assert(errors, patchedReleaseIndex.releaseId === approval.releaseId, 'approved publication patched Release Index release id mismatch')
    assert(errors, patchedReleaseModule !== undefined, 'approved publication patched Release Index missing Openlands module')
  }

  const artifactPatches = summary.artifactPatches ?? []
  assert(errors, Array.isArray(artifactPatches), 'approved publication release index patch summary artifactPatches must be an array')
  assert(errors, artifactPatches.length === publications.length, 'approved publication release index patch summary artifact patch count mismatch')
  assert(errors, sameUniqueSet(artifactPatches.map((entry) => entry.id), publications.map((entry) => entry.id)), 'approved publication release index patch summary artifact ids mismatch')
  for (const publication of publications) {
    const patch = artifactPatches.find((entry) => entry.id === publication.id)
    assert(errors, patch !== undefined, `approved publication release index patch summary missing artifact ${publication.id}`)
    if (!patch) continue
    assert(errors, patch.file === publication.file, `approved publication release index patch summary artifact ${publication.id} file mismatch`)
    assert(errors, patch.downloadUrl === publication.downloadUrl, `approved publication release index patch summary artifact ${publication.id} downloadUrl mismatch`)
    assert(errors, isPublicHttpsUrl(patch.downloadUrl), `approved publication release index patch summary artifact ${publication.id} downloadUrl must use a public https URL`)
    assert(errors, patch.sha256 === publication.sha256, `approved publication release index patch summary artifact ${publication.id} sha mismatch`)
    assert(errors, patch.size === publication.size, `approved publication release index patch summary artifact ${publication.id} size mismatch`)
    const patchedArtifact = artifactByFile(patchedReleaseModule, publication.file)
    assert(errors, patchedArtifact !== undefined, `approved publication patched Release Index missing artifact ${publication.file}`)
    if (patchedArtifact) {
      assert(errors, patchedArtifact.downloadUrl === publication.downloadUrl, `approved publication patched Release Index artifact ${publication.id} downloadUrl mismatch`)
      assert(errors, patchedArtifact.sha256 === publication.sha256, `approved publication patched Release Index artifact ${publication.id} sha mismatch`)
      assert(errors, patchedArtifact.size === publication.size, `approved publication patched Release Index artifact ${publication.id} size mismatch`)
    }
  }
}

function validateApprovalAttachment(errors, { contract, distributionApprovalContract, workspaceRoot, releaseRoot, manifestPath, manifest, releaseIndex, releaseIndexPath, publications }) {
  const manifestApproval = manifest.approval
  const approvalContract = contract.approvalContract ?? {}
  assert(errors, manifestApproval && typeof manifestApproval === 'object', 'approved publication manifest requires approval object')
  if (!manifestApproval || typeof manifestApproval !== 'object') return

  assert(errors, typeof manifestApproval.approvalFile === 'string' && manifestApproval.approvalFile.length > 0, 'approved publication manifest requires approval file path')
  assert(errors, typeof manifestApproval.approvalFileSha256 === 'string' && /^[a-f0-9]{64}$/i.test(manifestApproval.approvalFileSha256), 'approved publication manifest requires approval file sha256')
  const approvalFilePath = resolvePathFromCandidates(manifestApproval.approvalFile, [
    path.dirname(manifestPath),
    releaseRoot,
    workspaceRoot,
  ])
  assert(errors, approvalFilePath !== null && fileExists(approvalFilePath), `approved publication manifest approval file not found: ${approvalFilePath ?? manifestApproval.approvalFile}`)
  if (!approvalFilePath || !fileExists(approvalFilePath)) return

  const approvalFileSha256 = sha256File(approvalFilePath)
  assert(errors, manifestApproval.approvalFileSha256 === approvalFileSha256, 'approved publication manifest approval file sha256 mismatch')
  const approval = readJsonIfPossible(approvalFilePath)
  assert(errors, approval !== null, 'approved publication manifest approval file is not valid JSON')
  if (!approval) return

  for (const field of approvalContract.requiredTopLevelFields ?? []) {
    assert(errors, approval[field] !== undefined && approval[field] !== null, `approved publication approval file missing ${field}`)
  }
  assert(errors, approval.schema === (approvalContract.schema ?? APPROVAL_SCHEMA), `approved publication approval schema must be ${approvalContract.schema ?? APPROVAL_SCHEMA}`)
  assert(errors, approval.schema === manifestApproval.schema, 'approved publication manifest embedded approval schema mismatch')
  assert(errors, approval.moduleId === MODULE_ID, 'approved publication approval module id mismatch')
  assert(errors, approval.moduleVersion === VERSION, 'approved publication approval module version mismatch')
  assert(errors, approval.releaseId === manifest.releaseId, 'approved publication approval release id mismatch')
  assert(errors, releaseIndex !== null && approval.releaseId === releaseIndex.releaseId, 'approved publication approval release id must match Release Index')
  assert(errors, typeof approval.approver === 'string' && approval.approver.length > 0, 'approved publication approval approver is required')
  assert(errors, typeof approval.approvedAt === 'string' && approval.approvedAt.length > 0, 'approved publication approval approvedAt is required')
  assert(errors, approval.approver === manifestApproval.approver, 'approved publication manifest embedded approver mismatch')
  assert(errors, approval.approvedAt === manifestApproval.approvedAt, 'approved publication manifest embedded approvedAt mismatch')
  assert(errors, approval.releaseIndexPatch && typeof approval.releaseIndexPatch === 'object', 'approved publication approval releaseIndexPatch is required')
  assert(errors, deepEqual(approval.releaseIndexPatch, manifestApproval.releaseIndexPatch), 'approved publication manifest embedded releaseIndexPatch mismatch')
  assert(errors, typeof approval.releaseIndexPatch?.patchId === 'string' && approval.releaseIndexPatch.patchId.length > 0, 'approved publication approval patch id is required')
  assert(errors, typeof approval.releaseIndexPatch?.releaseIndexCommit === 'string' && approval.releaseIndexPatch.releaseIndexCommit.length > 0, 'approved publication approval release index commit is required')
  assert(errors, publications.every((entry) => entry.releaseIndexPatch?.patchId === approval.releaseIndexPatch?.patchId), 'approved publication artifacts must use approval patch id')
  assert(errors, publications.every((entry) => entry.releaseIndexPatch?.releaseIndexCommit === approval.releaseIndexPatch?.releaseIndexCommit), 'approved publication artifacts must use approval release index commit')
  assert(errors, approval.distributionApproval && typeof approval.distributionApproval === 'object', 'approved publication approval distributionApproval is required')
  for (const field of approvalContract.requiredDistributionApprovalFields ?? []) {
    assert(errors, approval.distributionApproval?.[field] !== undefined && approval.distributionApproval?.[field] !== null, `approved publication approval distributionApproval missing ${field}`)
  }
  assert(errors, approval.distributionApproval?.signoffId === manifestApproval.distributionApproval?.signoffId, 'approved publication manifest embedded distribution approval signoff mismatch')
  assert(errors, approval.distributionApproval?.approver === manifestApproval.distributionApproval?.approver, 'approved publication manifest embedded distribution approval approver mismatch')
  assert(errors, approval.distributionApproval?.approvedAt === manifestApproval.distributionApproval?.approvedAt, 'approved publication manifest embedded distribution approval approvedAt mismatch')

  for (const id of approvalContract.requiredChecklistIds ?? []) {
    assert(errors, checklistStatus(approval, id) === 'passed', `approved publication approval checklist ${id} must be passed`)
    assert(errors, checklistStatus(manifestApproval, id) === 'passed', `approved publication manifest embedded approval checklist ${id} must be passed`)
  }

  validateDistributionApprovalReports(errors, {
    approval,
    manifestApproval,
    distributionApprovalContract,
    workspaceRoot,
    releaseIndexPath,
    releaseId: manifest.releaseId,
  })
  validateReleaseIndexPatchSummary(errors, {
    manifestPath,
    approval,
    manifestApproval,
    releaseIndexPath,
    publications,
  })
}

function validateDownloadVerificationEvidence(errors, { manifest, manifestPath, releaseRoot, workspaceRoot, publications, expectedTargets }) {
  const verificationRoot = resolveVerificationRoot(manifest, manifestPath, releaseRoot, workspaceRoot)
  assert(errors, typeof manifest.verificationRoot === 'string' && manifest.verificationRoot.length > 0, 'verified publication manifest requires verificationRoot')
  assert(errors, verificationRoot !== null && fileExists(verificationRoot), `verified publication verification root not found: ${verificationRoot ?? manifest.verificationRoot}`)
  if (!verificationRoot || !fileExists(verificationRoot)) return

  const summaryPath = path.join(verificationRoot, 'public-download-verification-summary.json')
  assert(errors, fileExists(summaryPath), `verified publication download verification summary not found: ${summaryPath}`)
  const summary = fileExists(summaryPath) ? readJsonIfPossible(summaryPath) : null
  assert(errors, summary !== null, 'verified publication download verification summary is not valid JSON')
  const summaryResults = Array.isArray(summary?.verificationResults) ? summary.verificationResults : []
  if (summary) {
    assert(errors, summary.schema === DOWNLOAD_VERIFICATION_SUMMARY_SCHEMA, `verified publication download verification summary schema must be ${DOWNLOAD_VERIFICATION_SUMMARY_SCHEMA}`)
    assert(errors, summary.moduleId === MODULE_ID, 'verified publication download verification summary module id mismatch')
    assert(errors, summary.moduleVersion === VERSION, 'verified publication download verification summary module version mismatch')
    assert(errors, summary.releaseId === manifest.releaseId, 'verified publication download verification summary release id mismatch')
    assert(errors, summary.artifactCount === publications.length, 'verified publication download verification summary artifact count mismatch')
    assert(errors, sameSet(summaryResults.map((entry) => entry.id), publications.map((entry) => entry.id)), 'verified publication download verification summary artifact ids mismatch')
  }

  const releaseIndexSnapshotPath = path.join(verificationRoot, 'release-index-snapshot.json')
  assert(errors, fileExists(releaseIndexSnapshotPath), `verified publication release index snapshot not found: ${releaseIndexSnapshotPath}`)
  const releaseIndexSnapshot = fileExists(releaseIndexSnapshotPath) ? readJsonIfPossible(releaseIndexSnapshotPath) : null
  assert(errors, releaseIndexSnapshot !== null, 'verified publication release index snapshot is not valid JSON')
  const releaseIndexSnapshotModule = (releaseIndexSnapshot?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  if (releaseIndexSnapshot) {
    assert(errors, releaseIndexSnapshot.releaseId === manifest.releaseId, 'verified publication release index snapshot release id mismatch')
    assert(errors, releaseIndexSnapshotModule !== undefined, 'verified publication release index snapshot missing Openlands module')
  }

  const inputManifestSnapshotPath = path.join(verificationRoot, 'input-publication-manifest-snapshot.json')
  assert(errors, fileExists(inputManifestSnapshotPath), `verified publication input manifest snapshot not found: ${inputManifestSnapshotPath}`)
  const inputManifestSnapshot = fileExists(inputManifestSnapshotPath) ? readJsonIfPossible(inputManifestSnapshotPath) : null
  assert(errors, inputManifestSnapshot !== null, 'verified publication input manifest snapshot is not valid JSON')
  if (inputManifestSnapshot) {
    assert(errors, inputManifestSnapshot.schema === manifest.schema, 'verified publication input manifest snapshot schema mismatch')
    assert(errors, inputManifestSnapshot.moduleId === MODULE_ID, 'verified publication input manifest snapshot module id mismatch')
    assert(errors, inputManifestSnapshot.moduleVersion === VERSION, 'verified publication input manifest snapshot version mismatch')
    assert(errors, inputManifestSnapshot.releaseId === manifest.releaseId, 'verified publication input manifest snapshot release id mismatch')
    assert(errors, sameSet((inputManifestSnapshot.artifactPublications ?? []).map((entry) => entry.id), publications.map((entry) => entry.id)), 'verified publication input manifest snapshot artifact ids mismatch')
  }

  let urlMapSnapshot = null
  if (typeof manifest.urlMapPath === 'string' && manifest.urlMapPath.length > 0) {
    const urlMapSnapshotPath = path.join(verificationRoot, 'publication-url-map-snapshot.json')
    assert(errors, fileExists(urlMapSnapshotPath), `verified publication URL map snapshot not found: ${urlMapSnapshotPath}`)
    urlMapSnapshot = fileExists(urlMapSnapshotPath) ? readJsonIfPossible(urlMapSnapshotPath) : null
    assert(errors, urlMapSnapshot !== null, 'verified publication URL map snapshot is not valid JSON')
  }

  for (const target of expectedTargets) {
    const publication = publications.find((entry) => entry.id === target.id)
    assert(errors, publication !== undefined, `verified publication evidence missing artifact ${target.id}`)
    if (!publication) continue
    if (releaseIndexSnapshotModule) {
      const snapshotArtifact = artifactByFile(releaseIndexSnapshotModule, target.file)
      assert(errors, snapshotArtifact !== undefined, `verified publication release index snapshot missing artifact ${target.file}`)
      if (snapshotArtifact) {
        assert(errors, snapshotArtifact.kind === publication.kind, `verified publication artifact ${target.id} release index snapshot kind mismatch`)
        assert(errors, normalizeRuntimeTarget(snapshotArtifact.runtimeTarget) === publication.runtimeTarget, `verified publication artifact ${target.id} release index snapshot runtime target mismatch`)
        assert(errors, snapshotArtifact.sha256 === publication.sha256, `verified publication artifact ${target.id} release index snapshot sha mismatch`)
        assert(errors, snapshotArtifact.size === publication.size, `verified publication artifact ${target.id} release index snapshot size mismatch`)
      }
    }
    if (inputManifestSnapshot) {
      const inputPublication = publicationById(inputManifestSnapshot, target.id)
      assert(errors, inputPublication !== undefined, `verified publication input manifest snapshot missing artifact ${target.id}`)
      if (inputPublication) {
        assert(errors, inputPublication.file === target.file, `verified publication artifact ${target.id} input manifest snapshot file mismatch`)
        assert(errors, inputPublication.kind === publication.kind, `verified publication artifact ${target.id} input manifest snapshot kind mismatch`)
        assert(errors, inputPublication.runtimeTarget === publication.runtimeTarget, `verified publication artifact ${target.id} input manifest snapshot runtime target mismatch`)
        assert(errors, inputPublication.sha256 === publication.sha256, `verified publication artifact ${target.id} input manifest snapshot sha mismatch`)
        assert(errors, inputPublication.size === publication.size, `verified publication artifact ${target.id} input manifest snapshot size mismatch`)
      }
    }
    if (urlMapSnapshot) {
      const mapped = urlMapEntry(urlMapSnapshot, target.id)
      assert(errors, mapped !== null, `verified publication URL map snapshot missing artifact ${target.id}`)
      if (mapped) {
        assert(errors, mapped.downloadUrl === publication.downloadUrl, `verified publication artifact ${target.id} URL map snapshot downloadUrl mismatch`)
        assert(errors, isPublicHttpsUrl(mapped.downloadUrl), `verified publication artifact ${target.id} URL map snapshot downloadUrl must use a public https URL`)
      }
    }
    const verification = publication.downloadVerification ?? {}
    const expectedVerificationArtifactPath = path.join(verificationRoot, 'verification', `${target.id}-public-download-verification.json`)
    const verificationArtifactPath = resolvePathFromCandidates(verification.verificationArtifact, [
      verificationRoot,
      path.dirname(manifestPath),
      releaseRoot,
      workspaceRoot,
    ])
    assert(errors, verificationArtifactPath !== null && fileExists(verificationArtifactPath), `verified publication artifact ${target.id} verification artifact not found: ${verificationArtifactPath ?? verification.verificationArtifact}`)
    assert(errors, verificationArtifactPath !== null && sameResolvedPath(verificationArtifactPath, expectedVerificationArtifactPath), `verified publication artifact ${target.id} verification artifact path must match verification root`)
    const evidence = verificationArtifactPath && fileExists(verificationArtifactPath) ? readJsonIfPossible(verificationArtifactPath) : null
    assert(errors, evidence !== null, `verified publication artifact ${target.id} verification artifact is not valid JSON`)
    if (!evidence) continue

    assert(errors, evidence.id === target.id, `verified publication artifact ${target.id} evidence id mismatch`)
    assert(errors, evidence.file === target.file, `verified publication artifact ${target.id} evidence file mismatch`)
    assert(errors, evidence.downloadUrl === publication.downloadUrl, `verified publication artifact ${target.id} evidence downloadUrl mismatch`)
    assert(errors, isPublicHttpsUrl(evidence.downloadUrl), `verified publication artifact ${target.id} evidence downloadUrl must use a public https URL`)
    assert(errors, isPublicHttpsUrl(evidence.finalUrl), `verified publication artifact ${target.id} evidence finalUrl must use a public https URL`)
    assert(errors, evidence.statusCode === 200, `verified publication artifact ${target.id} evidence statusCode must be 200`)
    assert(errors, evidence.expectedSha256 === publication.sha256, `verified publication artifact ${target.id} evidence expected sha mismatch`)
    assert(errors, evidence.downloadedSha256 === publication.sha256, `verified publication artifact ${target.id} evidence downloaded sha mismatch`)
    assert(errors, evidence.downloadedSha256 === verification.downloadedSha256, `verified publication artifact ${target.id} manifest evidence sha mismatch`)
    assert(errors, evidence.expectedSize === publication.size, `verified publication artifact ${target.id} evidence expected size mismatch`)
    assert(errors, evidence.downloadedSize === publication.size, `verified publication artifact ${target.id} evidence downloaded size mismatch`)
    assert(errors, evidence.downloadedSize === verification.downloadedSize, `verified publication artifact ${target.id} manifest evidence size mismatch`)
    assert(errors, evidence.sha256Matches === true, `verified publication artifact ${target.id} evidence requires sha match`)
    assert(errors, evidence.sizeMatches === true, `verified publication artifact ${target.id} evidence requires size match`)
    assert(errors, evidence.verifiedAt === verification.verifiedAt, `verified publication artifact ${target.id} evidence verifiedAt mismatch`)
    if (evidence.contentLength !== null && evidence.contentLength !== undefined) {
      assert(errors, Number.parseInt(evidence.contentLength, 10) === publication.size, `verified publication artifact ${target.id} evidence content length mismatch`)
    }

    const downloadedArtifactPath = resolvePathFromCandidates(evidence.downloadedArtifact, [
      verificationRoot,
      path.dirname(verificationArtifactPath),
    ])
    assert(errors, downloadedArtifactPath !== null && fileExists(downloadedArtifactPath), `verified publication artifact ${target.id} downloaded evidence artifact not found: ${downloadedArtifactPath ?? evidence.downloadedArtifact}`)
    if (downloadedArtifactPath && fileExists(downloadedArtifactPath)) {
      assert(errors, sha256File(downloadedArtifactPath) === publication.sha256, `verified publication artifact ${target.id} downloaded evidence sha mismatch`)
      assert(errors, fs.statSync(downloadedArtifactPath).size === publication.size, `verified publication artifact ${target.id} downloaded evidence size mismatch`)
    }

    const summaryResult = summaryResults.find((entry) => entry.id === target.id)
    assert(errors, summaryResult !== undefined, `verified publication download verification summary missing ${target.id}`)
    if (summaryResult) {
      for (const field of ['file', 'downloadUrl', 'finalUrl', 'statusCode', 'expectedSha256', 'downloadedSha256', 'expectedSize', 'downloadedSize', 'sha256Matches', 'sizeMatches', 'verifiedAt', 'downloadedArtifact']) {
        assert(errors, summaryResult[field] === evidence[field], `verified publication artifact ${target.id} summary ${field} mismatch`)
      }
    }
  }
}

export function validatePublicationManifest({ moduleRoot, workspaceRoot, releaseRoot, manifestPath }) {
  const errors = []
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const contract = readJson(path.join(resourcesRoot, CONTRACT_PATH))
  const distributionApprovalContract = readJson(path.join(resourcesRoot, DISTRIBUTION_APPROVAL_CONTRACT_PATH))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseIndex = fileExists(releaseIndexPath) ? readJson(releaseIndexPath) : null
  const releaseModule = (releaseIndex?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const manifest = readJson(manifestPath)
  const approvedReleaseModule = manifest.status === 'approved'
    ? approvedReleaseModuleFromPatchSummary({ manifestPath, releaseIndexPath, releaseIndex })
    : releaseModule
  const generatorOwnedStatuses = [contract.blockedTemplateRules?.status, 'urls_pending']
  let generatedPublicationManifest = null
  if (generatorOwnedStatuses.includes(manifest.status)) {
    const generatorScript = path.join(moduleRoot, 'scripts', 'generate-openlands-release-publication-manifest.mjs')
    assert(errors, fileExists(generatorScript), `missing Openlands release publication manifest generator ${generatorScript}`)
    if (fileExists(generatorScript)) {
      const generated = spawnSync(process.execPath, [
        generatorScript,
        '--module-root',
        moduleRoot,
        '--release-root',
        releaseRoot,
        '--out',
        manifestPath,
        '--dry-run',
        '--json',
      ], {
        cwd: path.resolve(moduleRoot, '..', '..'),
        encoding: 'utf8',
        windowsHide: true,
      })
      assert(errors, generated.status === 0, `Openlands release publication manifest generator dry-run failed: ${generated.stderr || generated.stdout}`)
      if (generated.status === 0) {
        try {
          generatedPublicationManifest = JSON.parse(generated.stdout)
        } catch (error) {
          errors.push(`Openlands release publication manifest generator dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
        }
      }
    }
  }

  assert(errors, manifest.schema === contract.reportContract?.schema, 'publication manifest schema mismatch')
  assert(errors, contract.reportContract?.allowedStatus?.includes(manifest.status), 'publication manifest status is not allowed by contract')
  assert(errors, manifest.moduleId === MODULE_ID, 'publication manifest module id mismatch')
  assert(errors, manifest.moduleVersion === VERSION, 'publication manifest version mismatch')
  assert(errors, manifest.releasePublicationManifestContract === CONTRACT_PATH, 'publication manifest contract path mismatch')
  assert(errors, manifest.releaseId === releaseIndex?.releaseId, 'publication manifest release id mismatch')
  assert(errors, manifest.releaseIndexPath === releaseIndexPath, 'publication manifest release index path mismatch')
  for (const [key, value] of Object.entries(contract.sourceContracts ?? {})) {
    assert(errors, manifest.sourceContracts?.[key] === value, `publication manifest source contract ${key} mismatch`)
  }
  for (const field of contract.reportContract?.requiredTopLevelFields ?? []) {
    assert(errors, manifest[field] !== undefined && manifest[field] !== null, `publication manifest missing ${field}`)
  }
  for (const rule of ['allArtifactUrlsRequired', 'downloadVerificationRequired', 'releaseIndexCommitRequired', 'distributionApprovalRequired', 'blockedTemplateDoesNotPatchReleaseIndex', 'warningStateRequiredUntilApproved']) {
    assert(errors, manifest.releaseIndexPatchRules?.[rule] === contract.releaseIndexPatchRules?.[rule], `publication manifest release index patch rule ${rule} mismatch`)
  }
  assert(errors, manifest.releaseIndexPatchRules?.publicDownloadUrlProtocol === contract.releaseIndexPatchRules?.publicDownloadUrlProtocol, 'publication manifest public download URL protocol rule mismatch')

  const expectedTargets = contract.artifactTargets ?? []
  const publications = manifest.artifactPublications ?? []
  assert(errors, sameSet(publications.map((entry) => entry.id), expectedTargets.map((entry) => entry.id)), 'publication manifest artifact ids mismatch')
  assert(errors, publications.length === expectedTargets.length, 'publication manifest artifact count mismatch')
  for (const target of expectedTargets) {
    const publication = publications.find((entry) => entry.id === target.id)
    assert(errors, publication !== undefined, `publication manifest missing artifact ${target.id}`)
    if (!publication) continue
    for (const field of contract.reportContract?.requiredArtifactPublicationFields ?? []) {
      assert(errors, publication[field] !== undefined, `publication artifact ${target.id} missing ${field}`)
    }
    assert(errors, publication.file === target.file, `publication artifact ${target.id} file mismatch`)
    assert(errors, publication.runtimeTarget === target.runtimeTarget, `publication artifact ${target.id} runtime target mismatch`)
    assert(errors, publication.requiredForPublicAlpha === target.requiredForPublicAlpha, `publication artifact ${target.id} public alpha flag mismatch`)
    assert(errors, contract.reportContract?.allowedUrlStatus?.includes(publication.urlStatus), `publication artifact ${target.id} url status is not allowed`)
    if (typeof publication.downloadUrl === 'string' && publication.downloadUrl.length > 0) {
      assert(errors, isPublicHttpsUrl(publication.downloadUrl), `publication artifact ${target.id} downloadUrl must use a public https URL`)
    }
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    assert(errors, releaseArtifact !== undefined, `release index missing artifact ${target.file}`)
    const artifactPath = path.join(releaseRoot, MODULE_ID, target.file)
    assert(errors, fileExists(artifactPath), `artifact file missing ${artifactPath}`)
    if (releaseArtifact && fileExists(artifactPath)) {
      assert(errors, publication.kind === releaseArtifact.kind, `publication artifact ${target.id} kind mismatch`)
      assert(errors, publication.sha256 === releaseArtifact.sha256, `publication artifact ${target.id} sha256 mismatch`)
      assert(errors, publication.size === releaseArtifact.size, `publication artifact ${target.id} size mismatch`)
      assert(errors, publication.sha256 === sha256File(artifactPath), `publication artifact ${target.id} file sha mismatch`)
      assert(errors, publication.size === fs.statSync(artifactPath).size, `publication artifact ${target.id} file size mismatch`)
    }
    const verification = publication.downloadVerification ?? {}
    for (const field of contract.reportContract?.requiredDownloadVerificationFields ?? []) {
      assert(errors, verification[field] !== undefined, `publication artifact ${target.id} verification missing ${field}`)
    }
    if (publication.urlStatus === 'missing_url') {
      assert(errors, publication.downloadUrl === '', `publication artifact ${target.id} missing_url must keep downloadUrl empty`)
      assert(errors, verification.downloadAttempted === false, `publication artifact ${target.id} missing_url must not claim download attempted`)
      assert(errors, publication.releaseIndexPatch?.patchAllowed === false, `publication artifact ${target.id} missing_url must not allow release index patch`)
      assert(errors, publication.releaseIndexPatch?.patchApplied === false, `publication artifact ${target.id} missing_url must not apply release index patch`)
    }
    if (['download_verified', 'approved'].includes(publication.urlStatus)) {
      assert(errors, typeof publication.downloadUrl === 'string' && publication.downloadUrl.length > 0, `publication artifact ${target.id} verified state requires downloadUrl`)
      assert(errors, isPublicHttpsUrl(publication.downloadUrl), `publication artifact ${target.id} downloadUrl must use a public https URL`)
      assert(errors, verification.downloadAttempted === true, `publication artifact ${target.id} verified state requires download attempt`)
      assert(errors, verification.downloadedSha256 === publication.sha256, `publication artifact ${target.id} verified sha mismatch`)
      assert(errors, verification.downloadedSize === publication.size, `publication artifact ${target.id} verified size mismatch`)
      assert(errors, verification.sha256Matches === true, `publication artifact ${target.id} verified state requires sha match`)
      assert(errors, verification.sizeMatches === true, `publication artifact ${target.id} verified state requires size match`)
      assert(errors, typeof verification.verificationArtifact === 'string' && verification.verificationArtifact.length > 0, `publication artifact ${target.id} verified state requires verification artifact`)
    }
    if (publication.urlStatus === 'approved') {
      const approvedReleaseArtifact = artifactByFile(approvedReleaseModule, target.file)
      assert(errors, typeof publication.downloadUrl === 'string' && publication.downloadUrl.length > 0, `publication artifact ${target.id} approved state requires downloadUrl`)
      assert(errors, isPublicHttpsUrl(publication.downloadUrl), `publication artifact ${target.id} approved downloadUrl must use a public https URL`)
      assert(errors, approvedReleaseArtifact !== undefined, `publication artifact ${target.id} approved URL must resolve in patched Release Index`)
      assert(errors, approvedReleaseArtifact?.downloadUrl === publication.downloadUrl, `publication artifact ${target.id} approved URL must match patched Release Index`)
      assert(errors, publication.releaseIndexPatch?.patchAllowed === true, `publication artifact ${target.id} approved state requires patchAllowed`)
      assert(errors, publication.releaseIndexPatch?.patchApplied === true, `publication artifact ${target.id} approved state requires patchApplied`)
      assert(errors, typeof publication.releaseIndexPatch?.releaseIndexCommit === 'string' && publication.releaseIndexPatch.releaseIndexCommit.length > 0, `publication artifact ${target.id} approved state requires releaseIndexCommit`)
    }
  }

  const missingUrls = publications.filter((entry) => entry.urlStatus === 'missing_url')
  if (manifest.status === contract.blockedTemplateRules?.status) {
    assert(errors, missingUrls.length === publications.length, 'template_blocked publication manifest must keep every artifact missing_url')
    for (const blocker of contract.blockedTemplateRules?.requiredBlockedBy ?? []) {
      assert(errors, manifest.blockedBy?.includes(blocker), `template publication manifest missing blocker ${blocker}`)
    }
    for (const nextStep of contract.blockedTemplateRules?.requiredNextSteps ?? []) {
      assert(errors, manifest.nextSteps?.includes(nextStep), `template publication manifest missing next step ${nextStep}`)
    }
  }
  if (manifest.status === 'verified') {
    assert(errors, typeof manifest.outputPath === 'string' && sameResolvedPath(manifest.outputPath, manifestPath), 'verified publication manifest outputPath must match manifest path')
    assert(errors, typeof manifest.inputManifestPath === 'string' && manifest.inputManifestPath.length > 0, 'verified publication manifest requires inputManifestPath')
    assert(errors, missingUrls.length === 0, 'verified publication manifest must include every public URL')
    assert(errors, publications.every((entry) => entry.urlStatus === 'download_verified'), 'verified publication manifest requires every artifact download_verified')
    assert(errors, publications.every((entry) => entry.downloadVerification?.downloadAttempted === true), 'verified publication manifest requires every download attempted')
    assert(errors, publications.every((entry) => entry.downloadVerification?.sha256Matches === true), 'verified publication manifest requires every sha match')
    assert(errors, publications.every((entry) => entry.downloadVerification?.sizeMatches === true), 'verified publication manifest requires every size match')
    assert(errors, publications.every((entry) => entry.downloadVerification?.downloadedSha256 === entry.sha256), 'verified publication manifest downloaded sha mismatch')
    assert(errors, publications.every((entry) => entry.downloadVerification?.downloadedSize === entry.size), 'verified publication manifest downloaded size mismatch')
    assert(errors, publications.every((entry) => typeof entry.downloadVerification?.verificationArtifact === 'string' && entry.downloadVerification.verificationArtifact.length > 0), 'verified publication manifest requires verification artifact paths')
    assert(errors, publications.every((entry) => entry.releaseIndexPatch?.patchAllowed === false), 'verified publication manifest must not allow patch before approval')
    assert(errors, publications.every((entry) => entry.releaseIndexPatch?.patchApplied === false), 'verified publication manifest must not apply patch before approval')
    assert(errors, manifest.blockedBy?.includes('release_index_patch_not_approved'), 'verified publication manifest must keep patch approval blocker')
    assert(errors, manifest.blockedBy?.includes('distribution_approval_missing'), 'verified publication manifest must keep distribution approval blocker')
  }
  if (manifest.status === 'approved') {
    assert(errors, typeof manifest.outputPath === 'string' && sameResolvedPath(manifest.outputPath, manifestPath), 'approved publication manifest outputPath must match manifest path')
    assert(errors, typeof manifest.inputManifestPath === 'string' && manifest.inputManifestPath.length > 0, 'approved publication manifest requires inputManifestPath')
    assert(errors, typeof manifest.inputVerifiedManifestPath === 'string' && manifest.inputVerifiedManifestPath.length > 0, 'approved publication manifest requires inputVerifiedManifestPath')
    assert(errors, publications.every((entry) => entry.urlStatus === 'approved'), 'approved publication manifest requires every artifact approved')
    assert(errors, publications.every((entry) => entry.releaseIndexPatch?.patchAllowed === true), 'approved publication manifest requires every release index patch allowed')
    assert(errors, publications.every((entry) => entry.releaseIndexPatch?.patchApplied === true), 'approved publication manifest requires every release index patch applied')
    assert(errors, manifest.approval?.schema === 'echo.openlands.release_publication_approval.v1', 'approved publication manifest requires approval schema')
    assert(errors, typeof manifest.approval?.approver === 'string' && manifest.approval.approver.length > 0, 'approved publication manifest requires approval approver')
    assert(errors, typeof manifest.approval?.approvedAt === 'string' && manifest.approval.approvedAt.length > 0, 'approved publication manifest requires approvedAt')
    assert(errors, typeof manifest.approval?.releaseIndexPatch?.patchId === 'string' && manifest.approval.releaseIndexPatch.patchId.length > 0, 'approved publication manifest requires patch id')
    assert(errors, typeof manifest.approval?.releaseIndexPatch?.releaseIndexCommit === 'string' && manifest.approval.releaseIndexPatch.releaseIndexCommit.length > 0, 'approved publication manifest requires release index commit')
    assert(errors, typeof manifest.approval?.distributionApproval?.signoffId === 'string' && manifest.approval.distributionApproval.signoffId.length > 0, 'approved publication manifest requires distribution approval signoff')
    assert(errors, typeof manifest.approval?.distributionApproval?.approver === 'string' && manifest.approval.distributionApproval.approver.length > 0, 'approved publication manifest requires distribution approval approver')
    assert(errors, typeof manifest.approval?.distributionApproval?.approvedAt === 'string' && manifest.approval.distributionApproval.approvedAt.length > 0, 'approved publication manifest requires distribution approval approvedAt')
    assert(errors, Array.isArray(manifest.blockedBy) && manifest.blockedBy.length === 0, 'approved publication manifest must have no blockers')
    validateApprovalAttachment(errors, {
      contract,
      distributionApprovalContract,
      workspaceRoot,
      releaseRoot,
      manifestPath,
      manifest,
      releaseIndex,
      releaseIndexPath,
      publications,
    })
  }
  if (['verified', 'approved'].includes(manifest.status)) {
    validateDownloadVerificationEvidence(errors, {
      manifest,
      manifestPath,
      releaseRoot,
      workspaceRoot,
      publications,
      expectedTargets,
    })
  }
  assert(errors, manifest.summary?.artifactCount === publications.length, 'publication manifest summary artifact count mismatch')
  assert(errors, manifest.summary?.missingDownloadUrlCount === publications.filter((entry) => !entry.downloadUrl).length, 'publication manifest missing URL count mismatch')
  assert(errors, manifest.summary?.downloadVerifiedCount === publications.filter((entry) => entry.downloadVerification?.sha256Matches === true && entry.downloadVerification?.sizeMatches === true).length, 'publication manifest download verified count mismatch')
  assert(errors, manifest.summary?.releaseIndexPatchAllowedCount === publications.filter((entry) => entry.releaseIndexPatch?.patchAllowed === true).length, 'publication manifest patch allowed count mismatch')
  if (generatedPublicationManifest) {
    assert(errors, deepEqual(
      stableGeneratedPublicationManifest(manifest),
      stableGeneratedPublicationManifest(generatedPublicationManifest),
    ), 'publication manifest stale against generator dry-run')
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    manifestPath,
    manifestStatus: manifest.status,
    artifactCount: publications.length,
    missingDownloadUrlCount: publications.filter((entry) => !entry.downloadUrl).length,
    errors,
  }
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const moduleRoot = findModuleRoot(args.moduleRoot)
  const workspaceRoot = args.workspaceRoot ? path.resolve(args.workspaceRoot) : defaultWorkspaceRoot(moduleRoot)
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
  const manifestPath = args.manifest ? path.resolve(args.manifest) : path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.template.json')
  const result = validatePublicationManifest({ moduleRoot, workspaceRoot, releaseRoot, manifestPath })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands release publication manifest validated: status=${result.manifestStatus}, missingUrls=${result.missingDownloadUrlCount}.`)
  } else {
    console.error(`Openlands release publication manifest failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-release-publication-manifest.mjs [options]

Options:
  --module-root <path>    Openlands module root. Auto-detected by default.
  --workspace-root <path> Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --manifest <path>       Manifest path. Defaults to dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.template.json.
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

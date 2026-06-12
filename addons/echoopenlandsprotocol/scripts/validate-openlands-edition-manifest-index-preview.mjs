import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const LAUNCHER_FLOW_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json'
const DISTRIBUTION_APPROVAL_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json'
const DISTRIBUTION_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json'
const EXPECTED_EDITIONS = ['native', 'neoforge', 'standalone']
const REQUIRED_BLOCKERS = [
  'real_launcher_channel_index_missing',
  'real_launcher_execution_missing',
  'distribution_approval_missing',
  'preview_does_not_clear_distribution_gates',
]
const MODULE_REQUIREMENT_BLOCKER = 'module_requirement_resolution_failed'
const REQUIRED_PROOFS = [
  'launcher_flow_contract_loaded',
  'distribution_approval_contract_loaded',
  'all_edition_manifests_loaded',
  'edition_matrix_matches_manifests',
  'module_requirement_graph_evaluated',
  'local_release_index_artifacts_resolve',
  'launcher_channel_listing_preview_generated',
  'distribution_gate_stays_blocked_until_real_indexing',
]

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    releaseRoot: null,
    report: null,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--workspace-root') args.workspaceRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
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
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
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

function sameSet(actual, expected) {
  return JSON.stringify(sorted(actual)) === JSON.stringify(sorted(expected))
}

function sameJson(actual, expected) {
  return JSON.stringify(actual) === JSON.stringify(expected)
}

function stableEditionManifestIndexPreview(report) {
  if (!report || typeof report !== 'object') return report
  const { generatedAt, dryRun, savedArtifactRoot, ...stableReport } = report
  return stableReport
}

function readSavedJson(errors, filePath, label) {
  assert(errors, typeof filePath === 'string' && filePath.length > 0, `${label} path missing`)
  assert(errors, fileExists(filePath), `${label} missing`)
  if (!fileExists(filePath)) return null
  assert(errors, fs.statSync(filePath).size > 0, `${label} must be non-empty`)
  try {
    return readJson(filePath)
  } catch {
    errors.push(`${label} must be valid JSON`)
    return null
  }
}

function expandPattern(pattern) {
  return String(pattern ?? '').replace('<module>', MODULE_ID).replace('<version>', VERSION)
}

function normalizeRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
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

function releaseArtifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function requirementById(requirements, id) {
  return (requirements ?? []).find((requirement) => requirement.id === id)
}

function validate({ moduleRoot, workspaceRoot, releaseRoot, reportPath }) {
  const errors = []
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const launcherFlow = readJson(path.join(dataRoot, 'systems', 'launcher_flow_acceptance.json'))
  const distributionApproval = readJson(path.join(dataRoot, 'systems', 'distribution_approval_acceptance.json'))
  const distribution = readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json'))
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const descriptor = readJson(path.join(resourcesRoot, 'META-INF', 'echo.mod.json'))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseIndex = readJson(releaseIndexPath)
  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const report = readJson(reportPath)
  const expectedSavedArtifactRoot = path.join(path.dirname(reportPath), 'openlands-edition-manifest-index-preview-artifacts')
  const generatorScript = path.join(moduleRoot, 'scripts', 'generate-openlands-edition-manifest-index-preview.mjs')
  let generatedPreviewReport = null
  assert(errors, fileExists(generatorScript), `missing Openlands edition manifest index preview generator ${generatorScript}`)
  if (fileExists(generatorScript)) {
    const generated = spawnSync(process.execPath, [
      generatorScript,
      '--module-root',
      moduleRoot,
      '--workspace-root',
      workspaceRoot,
      '--release-root',
      releaseRoot,
      '--out',
      reportPath,
      '--dry-run',
      '--json',
    ], {
      cwd: path.resolve(moduleRoot, '..', '..'),
      encoding: 'utf8',
      windowsHide: true,
    })
    assert(errors, generated.status === 0, `Openlands edition manifest index preview generator dry-run failed: ${generated.stderr || generated.stdout}`)
    if (generated.status === 0) {
      try {
        generatedPreviewReport = JSON.parse(generated.stdout)
      } catch (error) {
        errors.push(`Openlands edition manifest index preview generator dry-run did not output valid JSON: ${error instanceof Error ? error.message : String(error)}`)
      }
    }
  }
  const publicAlphaGate = (runtimePlan.acceptanceGates ?? []).find((gate) => gate.id === 'public_alpha')
  const indexingArea = (distributionApproval.approvalAreas ?? []).find((area) => area.id === 'edition_manifest_indexing')
  const distributionTargets = new Map((distribution.artifactTargets ?? []).map((target) => [target.id, target]))
  const moduleRequirementResolutionPassed = report.moduleRequirementResolution?.passed === true
  const expectedPreviewStatus = moduleRequirementResolutionPassed ? 'preflight_passed' : 'preflight_blocked'
  const expectedModuleRequirementStatus = moduleRequirementResolutionPassed ? 'passed' : 'failed'
  const releaseIndexRequiresMatchDescriptor = sameSet(releaseModule?.requires ?? [], descriptor.requires ?? [])

  assert(errors, report.schema === 'echo.openlands.edition_manifest_index_preview.v1', 'edition manifest index preview schema mismatch')
  assert(errors, report.status === expectedPreviewStatus, 'edition manifest index preview status mismatch')
  assert(errors, report.moduleId === MODULE_ID, 'edition manifest index preview module id mismatch')
  assert(errors, report.moduleVersion === VERSION, 'edition manifest index preview module version mismatch')
  assert(errors, report.releaseId === releaseIndex.releaseId, 'edition manifest index preview release id mismatch')
  assert(errors, report.releaseIndexPath === releaseIndexPath, 'edition manifest index preview release index path mismatch')
  assert(errors, report.launcherFlowContract === LAUNCHER_FLOW_CONTRACT, 'edition manifest index launcher flow contract mismatch')
  assert(errors, report.distributionApprovalContract === DISTRIBUTION_APPROVAL_CONTRACT, 'edition manifest index distribution approval contract mismatch')
  assert(errors, report.distributionContract === DISTRIBUTION_CONTRACT, 'edition manifest index distribution contract mismatch')
  assert(errors, report.publicAlphaReady === false, 'edition manifest index preview must not mark public alpha ready')
  assert(errors, report.previewOnly === true, 'edition manifest index preview must be preview-only')
  assert(errors, report.clearsLauncherGates === false, 'edition manifest index preview must not clear launcher gates')
  assert(errors, report.clearsDistributionGates === false, 'edition manifest index preview must not clear distribution gates')
  assert(errors, report.savedArtifactRoot === expectedSavedArtifactRoot, 'edition manifest index savedArtifactRoot path mismatch')
  assert(errors, fileExists(report.savedArtifactRoot ?? ''), 'edition manifest index savedArtifactRoot must exist')
  if (generatedPreviewReport) {
    assert(errors, sameJson(
      stableEditionManifestIndexPreview(report),
      stableEditionManifestIndexPreview(generatedPreviewReport),
    ), 'edition manifest index preview stale against generator dry-run')
  }
  assert(errors, releaseModule !== undefined, 'release index missing Openlands module')
  assert(errors, descriptor.id === MODULE_ID && descriptor.version === VERSION, 'module descriptor identity mismatch')
  assert(errors, publicAlphaGate !== undefined, 'runtime adapter plan missing public_alpha gate')
  assert(errors, indexingArea !== undefined, 'distribution approval contract missing edition_manifest_indexing area')
  assert(errors, sameSet((report.editionResults ?? []).map((edition) => edition.id), EXPECTED_EDITIONS), 'edition manifest index edition list mismatch')
  assert(errors, report.summary?.editionCount === EXPECTED_EDITIONS.length, 'edition manifest index summary edition count mismatch')
  assert(errors, report.summary?.requiredSavedArtifactCount === (indexingArea?.requiredSavedArtifacts ?? []).length, 'edition manifest index required saved artifact count mismatch')
  assert(errors, report.summary?.savedArtifactCount === (report.savedArtifacts ?? []).length, 'edition manifest index saved artifact count mismatch')
  assert(errors, report.summary?.releaseIndexEntryCount === EXPECTED_EDITIONS.length, 'edition manifest index release entry count mismatch')
  assert(errors, report.summary?.localArtifactCount === EXPECTED_EDITIONS.length, 'edition manifest index local artifact count mismatch')
  assert(errors, report.summary?.sourceArtifactCount === EXPECTED_EDITIONS.length, 'edition manifest index source artifact count mismatch')
  assert(errors, report.summary?.artifactSha256MatchCount === EXPECTED_EDITIONS.length, 'edition manifest index artifact sha count mismatch')
  assert(errors, report.summary?.artifactSizeMatchCount === EXPECTED_EDITIONS.length, 'edition manifest index artifact size count mismatch')
  assert(errors, report.summary?.sourceSha256MatchCount === EXPECTED_EDITIONS.length, 'edition manifest index source sha count mismatch')
  assert(errors, report.summary?.sourceSizeMatchCount === EXPECTED_EDITIONS.length, 'edition manifest index source size count mismatch')
  assert(errors, report.summary?.requiredDescriptorMatchCount === EXPECTED_EDITIONS.length, 'edition manifest index descriptor match count mismatch')
  assert(errors, report.summary?.moduleRequirementResolutionPassed === moduleRequirementResolutionPassed, 'edition manifest index module requirement summary flag mismatch')
  assert(errors, report.moduleRequirementResolution?.status === expectedModuleRequirementStatus, 'edition manifest index module requirement resolution status mismatch')
  assert(errors, report.moduleRequirementResolution?.editionCount === EXPECTED_EDITIONS.length, 'edition manifest index module requirement edition count mismatch')
  assert(errors, (report.moduleRequirementResolution?.passedCount === EXPECTED_EDITIONS.length) === moduleRequirementResolutionPassed, 'edition manifest index module requirement passed count mismatch')
  assert(errors, report.launcherChannelListing?.status === 'preview_only', 'edition manifest index launcher channel listing status mismatch')
  assert(errors, report.launcherChannelListing?.editionCount === EXPECTED_EDITIONS.length, 'edition manifest index launcher channel edition count mismatch')

  for (const blocker of REQUIRED_BLOCKERS) {
    assert(errors, report.blockedBy?.includes(blocker), `edition manifest index missing blocker ${blocker}`)
  }
  if (report.summary?.allDownloadUrlsPresent === false) {
    assert(errors, report.blockedBy?.includes('release_index_download_urls_missing'), 'edition manifest index missing release index download URL blocker')
  }
  if (!moduleRequirementResolutionPassed) {
    assert(errors, report.blockedBy?.includes(MODULE_REQUIREMENT_BLOCKER), 'edition manifest index missing module requirement resolution blocker')
  }
  for (const proof of REQUIRED_PROOFS) {
    assert(errors, report.proofs?.includes(proof), `edition manifest index missing proof ${proof}`)
  }
  if (moduleRequirementResolutionPassed) {
    assert(errors, report.proofs?.includes('module_requirement_graph_resolves_echoopenlandsprotocol'), 'edition manifest index missing module requirement resolution proof')
  }
  for (const savedArtifact of report.savedArtifacts ?? []) {
    const savedArtifactPath = path.join(report.savedArtifactRoot ?? '', savedArtifact)
    assert(errors, fileExists(savedArtifactPath), `edition manifest index saved artifact missing ${savedArtifact}`)
    if (fileExists(savedArtifactPath)) {
      assert(errors, fs.statSync(savedArtifactPath).size > 0, `edition manifest index saved artifact empty ${savedArtifact}`)
    }
  }
  for (const requiredArtifact of indexingArea?.requiredSavedArtifacts ?? []) {
    assert(errors, report.savedArtifacts?.includes(requiredArtifact), `edition manifest index missing required saved artifact ${requiredArtifact}`)
  }

  const savedIndexReport = readSavedJson(errors, path.join(report.savedArtifactRoot, 'edition-manifest-index-report.json'), 'saved edition manifest index report')
  const savedRequirementResolution = readSavedJson(errors, path.join(report.savedArtifactRoot, 'module-requirement-resolution.json'), 'saved module requirement resolution')
  const savedChannelListing = readSavedJson(errors, path.join(report.savedArtifactRoot, 'launcher-channel-listing.json'), 'saved launcher channel listing')
  if (savedIndexReport) {
    assert(errors, savedIndexReport.schema === 'echo.openlands.edition_manifest_index_report_preview.v1', 'saved edition manifest index report schema mismatch')
    assert(errors, savedIndexReport.status === expectedPreviewStatus, 'saved edition manifest index report status mismatch')
    assert(errors, savedIndexReport.previewOnly === true, 'saved edition manifest index report must be preview-only')
    assert(errors, savedIndexReport.publicAlphaReady === false, 'saved edition manifest index report must not mark public alpha ready')
    assert(errors, savedIndexReport.clearsLauncherGates === false, 'saved edition manifest index report must not clear launcher gates')
    assert(errors, savedIndexReport.clearsDistributionGates === false, 'saved edition manifest index report must not clear distribution gates')
    assert(errors, savedIndexReport.moduleId === MODULE_ID, 'saved edition manifest index report module mismatch')
    assert(errors, savedIndexReport.moduleVersion === VERSION, 'saved edition manifest index report version mismatch')
    assert(errors, savedIndexReport.releaseId === releaseIndex.releaseId, 'saved edition manifest index report release id mismatch')
    assert(errors, sameJson(savedIndexReport.editionResults, report.editionResults), 'saved edition manifest index report edition results mismatch')
    assert(errors, sameSet(savedIndexReport.requiredSavedArtifacts, indexingArea?.requiredSavedArtifacts ?? []), 'saved edition manifest index report required artifacts mismatch')
    assert(errors, sameSet(savedIndexReport.editionIndexArtifacts, (report.savedArtifacts ?? []).filter((artifact) => artifact.startsWith('edition-index-entries/'))), 'saved edition manifest index report edition entry artifact list mismatch')
    assert(errors, sameSet(savedIndexReport.blockedBy, report.blockedBy), 'saved edition manifest index report blockers mismatch')
  }
  if (savedRequirementResolution) {
    assert(errors, savedRequirementResolution.schema === 'echo.openlands.module_requirement_resolution_preview.v1', 'saved module requirement resolution schema mismatch')
    assert(errors, savedRequirementResolution.status === expectedModuleRequirementStatus, 'saved module requirement resolution status mismatch')
    assert(errors, savedRequirementResolution.moduleId === MODULE_ID, 'saved module requirement resolution module mismatch')
    assert(errors, savedRequirementResolution.moduleVersion === VERSION, 'saved module requirement resolution version mismatch')
    assert(errors, sameSet(savedRequirementResolution.requiredCoreModules, descriptor.requires), 'saved module requirement core modules mismatch')
    assert(errors, sameSet(savedRequirementResolution.requiredModuleIds, [...(descriptor.requires ?? []), MODULE_ID]), 'saved module requirement module id list mismatch')
    assert(errors, sameSet(savedRequirementResolution.releaseIndexRequires, releaseModule?.requires ?? []), 'saved module requirement release index requires mismatch')
    assert(errors, savedRequirementResolution.summary?.editionCount === report.moduleRequirementResolution?.editionCount, 'saved module requirement edition count mismatch')
    assert(errors, savedRequirementResolution.summary?.passedCount === report.moduleRequirementResolution?.passedCount, 'saved module requirement passed count mismatch')
    assert(errors, savedRequirementResolution.summary?.missingRequirementCount === report.moduleRequirementResolution?.missingRequirementCount, 'saved module requirement missing count mismatch')
    assert(errors, (savedRequirementResolution.summary?.missingRequirementCount === 0) === moduleRequirementResolutionPassed, 'saved module requirement missing count must match pass flag')
    assert(errors, savedRequirementResolution.summary?.openlandsExactVersionCount === EXPECTED_EDITIONS.length, 'saved module requirement Openlands exact version count mismatch')
    assert(errors, sameSet((savedRequirementResolution.editionResolutions ?? []).map((entry) => entry.id), EXPECTED_EDITIONS), 'saved module requirement edition list mismatch')
  }
  if (savedChannelListing) {
    assert(errors, savedChannelListing.schema === 'echo.openlands.launcher_channel_listing_preview.v1', 'saved launcher channel listing schema mismatch')
    assert(errors, savedChannelListing.status === 'preview_only', 'saved launcher channel listing status mismatch')
    assert(errors, savedChannelListing.previewOnly === true, 'saved launcher channel listing must be preview-only')
    assert(errors, savedChannelListing.publicAlphaReady === false, 'saved launcher channel listing must not mark public alpha ready')
    assert(errors, savedChannelListing.clearsLauncherGates === false, 'saved launcher channel listing must not clear launcher gates')
    assert(errors, savedChannelListing.clearsDistributionGates === false, 'saved launcher channel listing must not clear distribution gates')
    assert(errors, savedChannelListing.channelId === 'openlands', 'saved launcher channel listing channel id mismatch')
    assert(errors, savedChannelListing.moduleId === MODULE_ID, 'saved launcher channel listing module mismatch')
    assert(errors, savedChannelListing.moduleVersion === VERSION, 'saved launcher channel listing version mismatch')
    assert(errors, savedChannelListing.releaseId === releaseIndex.releaseId, 'saved launcher channel listing release id mismatch')
    assert(errors, savedChannelListing.editionCount === EXPECTED_EDITIONS.length, 'saved launcher channel listing edition count mismatch')
    assert(errors, sameSet((savedChannelListing.entries ?? []).map((entry) => entry.id), EXPECTED_EDITIONS), 'saved launcher channel listing edition list mismatch')
    for (const blocker of REQUIRED_BLOCKERS) {
      assert(errors, savedChannelListing.blockedBy?.includes(blocker), `saved launcher channel listing missing blocker ${blocker}`)
    }
    if (!moduleRequirementResolutionPassed) {
      assert(errors, savedChannelListing.blockedBy?.includes(MODULE_REQUIREMENT_BLOCKER), 'saved launcher channel listing missing module requirement blocker')
    }
  }

  for (const matrix of launcherFlow.editionMatrix ?? []) {
    if (!EXPECTED_EDITIONS.includes(matrix.id)) continue
    const result = (report.editionResults ?? []).find((edition) => edition.id === matrix.id)
    assert(errors, result !== undefined, `edition manifest index missing ${matrix.id}`)
    if (!result) continue
    const manifestPath = path.join(workspaceRoot, matrix.editionRepo, matrix.releaseManifest)
    const manifest = readJson(manifestPath)
    const artifactPath = path.join(releaseRoot, MODULE_ID, matrix.artifactPattern)
    const releaseArtifact = releaseArtifactByFile(releaseModule, matrix.artifactPattern)
    const sourceFile = expandPattern(manifest.moduleSourcePattern)
    const sourceArtifactPath = path.join(releaseRoot, MODULE_ID, sourceFile)
    const sourceArtifact = releaseArtifactByFile(releaseModule, sourceFile)
    const artifactSha = fileExists(artifactPath) ? sha256File(artifactPath) : null
    const sourceSha = fileExists(sourceArtifactPath) ? sha256File(sourceArtifactPath) : null
    const artifactSize = fileExists(artifactPath) ? fs.statSync(artifactPath).size : null
    const sourceSize = fileExists(sourceArtifactPath) ? fs.statSync(sourceArtifactPath).size : null
    const openlandsRequirement = requirementById(manifest.moduleRequirements, MODULE_ID)
    const requirementIds = (manifest.moduleRequirements ?? []).map((requirement) => requirement.id)
    const missingCoreModuleIds = (descriptor.requires ?? []).filter((moduleId) => !requirementIds.includes(moduleId))
    const moduleRequirementsResolved = missingCoreModuleIds.length === 0
      && openlandsRequirement?.version === VERSION
      && releaseIndexRequiresMatchDescriptor
    const moduleRequirementResolutionStatus = moduleRequirementsResolved ? 'passed' : 'failed'
    const distributionTarget = distributionTargets.get(matrix.id)

    assert(errors, result.repo === matrix.editionRepo, `${matrix.id} repo mismatch`)
    assert(errors, result.packId === matrix.packId, `${matrix.id} pack id mismatch`)
    assert(errors, result.runtimeTarget === matrix.runtimeTarget, `${matrix.id} runtime target mismatch`)
    assert(errors, result.loader === matrix.launcherProfileKind, `${matrix.id} loader mismatch`)
    assert(errors, result.launcherProfileKind === matrix.launcherProfileKind, `${matrix.id} launcher profile mismatch`)
    assert(errors, result.releaseManifestPath === manifestPath, `${matrix.id} release manifest path mismatch`)
    assert(errors, result.releaseIndexEntry === matrix.releaseIndexEntry, `${matrix.id} release index entry mismatch`)
    assert(errors, result.artifactFamily === matrix.artifactFamily, `${matrix.id} artifact family mismatch`)
    assert(errors, result.artifactPattern === matrix.artifactPattern, `${matrix.id} artifact pattern mismatch`)
    assert(errors, result.sourcePattern === sourceFile, `${matrix.id} source pattern mismatch`)
    assert(errors, sameSet(result.requiredDescriptors, matrix.requiredDescriptors), `${matrix.id} required descriptors mismatch`)
    assert(errors, result.requiredDescriptorsMatchManifest === true, `${matrix.id} required descriptors must match manifest`)
    assert(errors, result.artifactContainsRequiredDescriptors === true, `${matrix.id} artifact must contain required descriptors`)
    assert(errors, sameSet(result.requiredPublicAlphaEvidence, publicAlphaGate?.requiresEvidence ?? []), `${matrix.id} public alpha evidence mismatch`)
    assert(errors, result.requiredPublicAlphaEvidenceMatches === true, `${matrix.id} public alpha evidence match flag mismatch`)
    assert(errors, result.moduleRequirementsResolved === moduleRequirementsResolved, `${matrix.id} module requirement resolution flag mismatch`)
    assert(errors, sameSet(result.missingCoreModuleIds, missingCoreModuleIds), `${matrix.id} missing core module list mismatch`)
    assert(errors, result.openlandsRequirement?.version === VERSION, `${matrix.id} Openlands requirement version mismatch`)
    assert(errors, openlandsRequirement?.version === VERSION, `${matrix.id} manifest Openlands requirement version mismatch`)
    assert(errors, distributionTarget?.file === matrix.artifactPattern, `${matrix.id} distribution target file mismatch`)
    assert(errors, result.releaseIndexEntryResolved === true, `${matrix.id} release index entry must resolve`)
    assert(errors, result.artifact?.file === matrix.artifactPattern, `${matrix.id} artifact file mismatch`)
    assert(errors, result.artifact?.path === artifactPath, `${matrix.id} artifact path mismatch`)
    assert(errors, result.artifact?.kind === matrix.artifactFamily, `${matrix.id} artifact kind mismatch`)
    assert(errors, result.artifact?.runtimeTarget === matrix.runtimeTarget, `${matrix.id} artifact runtime target mismatch`)
    assert(errors, result.artifact?.buildMode === releaseArtifact?.buildMode, `${matrix.id} artifact build mode mismatch`)
    assert(errors, result.artifact?.localArtifactExists === true && fileExists(artifactPath), `${matrix.id} artifact must exist`)
    assert(errors, result.artifact?.sha256 === artifactSha, `${matrix.id} artifact sha mismatch`)
    assert(errors, result.artifact?.size === artifactSize, `${matrix.id} artifact size mismatch`)
    assert(errors, result.artifact?.releaseIndexSha256 === releaseArtifact?.sha256, `${matrix.id} release index sha mismatch`)
    assert(errors, result.artifact?.releaseIndexSize === releaseArtifact?.size, `${matrix.id} release index size mismatch`)
    assert(errors, result.artifact?.sha256MatchesReleaseIndex === true, `${matrix.id} artifact sha must match release index`)
    assert(errors, result.artifact?.sizeMatchesReleaseIndex === true, `${matrix.id} artifact size must match release index`)
    assert(errors, normalizeRuntimeTarget(releaseArtifact?.runtimeTarget) === matrix.runtimeTarget, `${matrix.id} release artifact runtime target mismatch`)
    for (const descriptorEntry of matrix.requiredDescriptors ?? []) {
      assert(errors, result.artifact?.contains?.includes(descriptorEntry), `${matrix.id} artifact missing descriptor ${descriptorEntry}`)
    }
    assert(errors, result.sourceArtifact?.file === sourceFile, `${matrix.id} source artifact file mismatch`)
    assert(errors, result.sourceArtifact?.path === sourceArtifactPath, `${matrix.id} source artifact path mismatch`)
    assert(errors, result.sourceArtifact?.localArtifactExists === true && fileExists(sourceArtifactPath), `${matrix.id} source artifact must exist`)
    assert(errors, result.sourceArtifact?.sha256 === sourceSha, `${matrix.id} source artifact sha mismatch`)
    assert(errors, result.sourceArtifact?.size === sourceSize, `${matrix.id} source artifact size mismatch`)
    assert(errors, result.sourceArtifact?.releaseIndexSha256 === sourceArtifact?.sha256, `${matrix.id} source release index sha mismatch`)
    assert(errors, result.sourceArtifact?.releaseIndexSize === sourceArtifact?.size, `${matrix.id} source release index size mismatch`)
    assert(errors, result.sourceArtifact?.sha256MatchesReleaseIndex === true, `${matrix.id} source sha must match release index`)
    assert(errors, result.sourceArtifact?.sizeMatchesReleaseIndex === true, `${matrix.id} source size must match release index`)
    assert(errors, result.publicAlphaReady === false, `${matrix.id} preview must not mark public alpha ready`)
    assert(errors, result.indexedState === 'preview_only', `${matrix.id} indexed state mismatch`)

    const channelEntry = (savedChannelListing?.entries ?? []).find((entry) => entry.id === matrix.id)
    assert(errors, channelEntry !== undefined, `${matrix.id} missing launcher channel listing entry`)
    assert(errors, channelEntry?.packId === matrix.packId, `${matrix.id} launcher channel pack id mismatch`)
    assert(errors, channelEntry?.displayName === result.displayName, `${matrix.id} launcher channel display name mismatch`)
    assert(errors, channelEntry?.runtimeTarget === matrix.runtimeTarget, `${matrix.id} launcher channel runtime mismatch`)
    assert(errors, channelEntry?.loader === result.loader, `${matrix.id} launcher channel loader mismatch`)
    assert(errors, channelEntry?.launcherProfileKind === result.launcherProfileKind, `${matrix.id} launcher channel profile mismatch`)
    assert(errors, channelEntry?.releaseIndexEntry === matrix.releaseIndexEntry, `${matrix.id} launcher channel release entry mismatch`)
    assert(errors, channelEntry?.releaseManifestPath === manifestPath, `${matrix.id} launcher channel manifest path mismatch`)
    assert(errors, channelEntry?.sourceRepo === result.sourceRepo, `${matrix.id} launcher channel source repo mismatch`)
    assert(errors, channelEntry?.moduleArtifact?.moduleId === MODULE_ID, `${matrix.id} launcher channel module artifact module mismatch`)
    assert(errors, channelEntry?.moduleArtifact?.version === VERSION, `${matrix.id} launcher channel module artifact version mismatch`)
    assert(errors, channelEntry?.moduleArtifact?.family === result.artifactFamily, `${matrix.id} launcher channel artifact family mismatch`)
    assert(errors, channelEntry?.moduleArtifact?.file === matrix.artifactPattern, `${matrix.id} launcher channel artifact mismatch`)
    assert(errors, channelEntry?.moduleArtifact?.sha256 === result.artifact?.sha256, `${matrix.id} launcher channel artifact sha mismatch`)
    assert(errors, channelEntry?.moduleArtifact?.size === result.artifact?.size, `${matrix.id} launcher channel artifact size mismatch`)
    assert(errors, channelEntry?.moduleArtifact?.downloadUrl === result.artifact?.releaseIndexDownloadUrl, `${matrix.id} launcher channel artifact URL mismatch`)
    assert(errors, channelEntry?.moduleArtifact?.downloadUrlPresent === result.artifact?.downloadUrlPresent, `${matrix.id} launcher channel artifact URL flag mismatch`)
    assert(errors, sameJson(channelEntry?.sourceArtifact, result.sourceArtifact), `${matrix.id} launcher channel source artifact mismatch`)
    assert(errors, channelEntry?.moduleRequirementResolutionStatus === moduleRequirementResolutionStatus, `${matrix.id} launcher channel requirement status mismatch`)
    assert(errors, channelEntry?.indexedState === 'preview_only', `${matrix.id} launcher channel indexed state mismatch`)
    assert(errors, channelEntry?.publicListingAllowed === false, `${matrix.id} launcher channel must not allow public listing`)

    const requirementEntry = (savedRequirementResolution?.editionResolutions ?? []).find((entry) => entry.id === matrix.id)
    assert(errors, requirementEntry !== undefined, `${matrix.id} missing module requirement resolution entry`)
    assert(errors, requirementEntry?.passed === moduleRequirementsResolved, `${matrix.id} module requirement resolution flag mismatch`)
    assert(errors, requirementEntry?.packId === matrix.packId, `${matrix.id} module requirement resolution pack id mismatch`)
    assert(errors, requirementEntry?.runtimeTarget === matrix.runtimeTarget, `${matrix.id} module requirement resolution runtime mismatch`)
    assert(errors, requirementEntry?.requirementCount === (manifest.moduleRequirements ?? []).length, `${matrix.id} module requirement resolution count mismatch`)
    assert(errors, sameSet(requirementEntry?.requiredCoreModules, descriptor.requires), `${matrix.id} module requirement resolution core module list mismatch`)
    assert(errors, sameSet(requirementEntry?.missingCoreModuleIds, missingCoreModuleIds), `${matrix.id} module requirement resolution missing cores mismatch`)
    assert(errors, (requirementEntry?.extraModuleIds ?? []).length === 0, `${matrix.id} module requirement resolution has extra modules`)
    assert(errors, requirementEntry?.openlandsRequirement?.version === VERSION, `${matrix.id} module requirement resolution Openlands version mismatch`)
    assert(errors, requirementEntry?.releaseIndexRequiresMatchDescriptor === releaseIndexRequiresMatchDescriptor, `${matrix.id} module requirement resolution release-index descriptor flag mismatch`)

    const previewArtifact = path.join(report.savedArtifactRoot, 'edition-index-entries', `${matrix.id}-modpack-index-entry.preview.json`)
    const previewEntry = readSavedJson(errors, previewArtifact, `${matrix.id} saved edition index entry preview`)
    if (previewEntry) {
      assert(errors, previewEntry.schema === 'echo.openlands.edition_modpack_index_entry_preview.v1', `${matrix.id} saved index entry schema mismatch`)
      assert(errors, previewEntry.status === 'preview_only', `${matrix.id} saved index entry status mismatch`)
      assert(errors, previewEntry.previewOnly === true, `${matrix.id} saved index entry must be preview-only`)
      assert(errors, previewEntry.publicAlphaReady === false, `${matrix.id} saved index entry must not mark public alpha ready`)
      assert(errors, previewEntry.clearsLauncherGates === false, `${matrix.id} saved index entry must not clear launcher gates`)
      assert(errors, previewEntry.clearsDistributionGates === false, `${matrix.id} saved index entry must not clear distribution gates`)
      assert(errors, previewEntry.releaseId === releaseIndex.releaseId, `${matrix.id} saved index entry release id mismatch`)
      assert(errors, previewEntry.moduleId === MODULE_ID, `${matrix.id} saved index entry module mismatch`)
      assert(errors, previewEntry.moduleVersion === VERSION, `${matrix.id} saved index entry version mismatch`)
      assert(errors, previewEntry.id === matrix.id, `${matrix.id} saved index entry id mismatch`)
      assert(errors, previewEntry.packId === matrix.packId, `${matrix.id} saved index entry pack id mismatch`)
      assert(errors, previewEntry.displayName === result.displayName, `${matrix.id} saved index entry display name mismatch`)
      assert(errors, previewEntry.runtimeTarget === matrix.runtimeTarget, `${matrix.id} saved index entry runtime mismatch`)
      assert(errors, previewEntry.loader === result.loader, `${matrix.id} saved index entry loader mismatch`)
      assert(errors, previewEntry.releaseIndexEntry === matrix.releaseIndexEntry, `${matrix.id} saved index entry release index mismatch`)
      assert(errors, previewEntry.releaseManifestPath === manifestPath, `${matrix.id} saved index entry manifest path mismatch`)
      assert(errors, sameSet(previewEntry.requiredDescriptors, result.requiredDescriptors), `${matrix.id} saved index entry required descriptors mismatch`)
      assert(errors, sameJson(previewEntry.moduleRequirements, result.moduleRequirements), `${matrix.id} saved index entry module requirements mismatch`)
      assert(errors, sameJson(previewEntry.moduleArtifact, result.artifact), `${matrix.id} saved index entry module artifact mismatch`)
      assert(errors, sameJson(previewEntry.sourceArtifact, result.sourceArtifact), `${matrix.id} saved index entry source artifact mismatch`)
      for (const blocker of REQUIRED_BLOCKERS) {
        assert(errors, previewEntry.blockedBy?.includes(blocker), `${matrix.id} saved index entry missing blocker ${blocker}`)
      }
      if (!moduleRequirementsResolved) {
        assert(errors, previewEntry.blockedBy?.includes(MODULE_REQUIREMENT_BLOCKER), `${matrix.id} saved index entry missing module requirement blocker`)
      }
    }
  }

  return {
    status: errors.length ? 'failed' : 'passed',
    reportPath,
    editionCount: report.editionResults?.length ?? 0,
    savedArtifactCount: report.savedArtifacts?.length ?? 0,
    moduleRequirementResolutionPassed: report.moduleRequirementResolution?.passed === true,
    launcherChannelListingEditionCount: report.launcherChannelListing?.editionCount ?? 0,
    publicAlphaReady: report.publicAlphaReady,
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
  const workspaceRoot = args.workspaceRoot ? path.resolve(args.workspaceRoot) : path.resolve(moduleRoot, '..', '..', '..')
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
  const reportPath = args.report ? path.resolve(args.report) : path.join(releaseRoot, MODULE_ID, 'openlands-edition-manifest-index-preview.json')
  const result = validate({ moduleRoot, workspaceRoot, releaseRoot, reportPath })
  if (args.json) {
    console.log(JSON.stringify(result, null, 2))
  } else if (result.status === 'passed') {
    console.log(`Openlands edition manifest index preview validated: ${result.editionCount} editions, savedArtifacts=${result.savedArtifactCount}, publicAlphaReady=${result.publicAlphaReady}.`)
  } else {
    console.error(`Openlands edition manifest index preview failed with ${result.errors.length} error(s):`)
    for (const error of result.errors) console.error(`- ${error}`)
    process.exitCode = 1
  }
  return result
}

function printHelp() {
  console.log(`Usage: node validate-openlands-edition-manifest-index-preview.mjs [options]

Options:
  --module-root <path>    Openlands module root. Auto-detected by default.
  --workspace-root <p>    Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --report <path>         Report path. Defaults to dist/echo-module-release/echoopenlandsprotocol/openlands-edition-manifest-index-preview.json.
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

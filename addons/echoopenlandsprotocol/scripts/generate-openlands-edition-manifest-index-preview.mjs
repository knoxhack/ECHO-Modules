import crypto from 'node:crypto'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const LAUNCHER_FLOW_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json'
const DISTRIBUTION_APPROVAL_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json'
const DISTRIBUTION_CONTRACT = 'data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json'
const EDITION_ORDER = ['native', 'neoforge', 'standalone']

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    releaseRoot: null,
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
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--dry-run') args.dryRun = true
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function defaultReleaseRoot(moduleRoot) {
  return path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
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

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function sorted(values) {
  return [...(values ?? [])].sort()
}

function sameList(actual, expected) {
  return JSON.stringify(sorted(actual)) === JSON.stringify(sorted(expected))
}

function expandPattern(pattern) {
  return String(pattern ?? '').replace('<module>', MODULE_ID).replace('<version>', VERSION)
}

function normalizeRuntimeTarget(value) {
  if (value === 'echo-native') return 'echo_native'
  if (value === 'standalone') return 'echo_runtime_standalone'
  return value
}

function releaseArtifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function requirementById(requirements, id) {
  return (requirements ?? []).find((requirement) => requirement.id === id)
}

function buildModuleRequirementResolution({ descriptor, releaseModule, editionResults }) {
  const requiredCoreModules = sorted(descriptor.requires ?? [])
  const requiredModuleIds = sorted([...requiredCoreModules, MODULE_ID])
  const editionResolutions = editionResults.map((edition) => {
    const requirementIds = sorted((edition.moduleRequirements ?? []).map((requirement) => requirement.id))
    const missingCoreModuleIds = requiredCoreModules.filter((moduleId) => !requirementIds.includes(moduleId))
    const extraModuleIds = requirementIds.filter((moduleId) => !requiredModuleIds.includes(moduleId))
    const openlandsRequirement = requirementById(edition.moduleRequirements, MODULE_ID)
    const passed = missingCoreModuleIds.length === 0
      && openlandsRequirement?.version === VERSION
      && releaseModule?.requires
      && sameList(releaseModule.requires, requiredCoreModules)
    return {
      id: edition.id,
      packId: edition.packId,
      runtimeTarget: edition.runtimeTarget,
      requirementCount: requirementIds.length,
      requiredCoreModules,
      missingCoreModuleIds,
      extraModuleIds,
      openlandsRequirement: openlandsRequirement ?? null,
      releaseIndexRequiresMatchDescriptor: sameList(releaseModule?.requires ?? [], requiredCoreModules),
      passed,
    }
  })
  return {
    schema: 'echo.openlands.module_requirement_resolution_preview.v1',
    status: editionResolutions.every((edition) => edition.passed) ? 'passed' : 'failed',
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    requiredCoreModules,
    requiredModuleIds,
    releaseIndexRequires: sorted(releaseModule?.requires ?? []),
    editionResolutions,
    summary: {
      editionCount: editionResolutions.length,
      passedCount: editionResolutions.filter((edition) => edition.passed).length,
      missingRequirementCount: editionResolutions.reduce((total, edition) => total + edition.missingCoreModuleIds.length, 0),
      openlandsExactVersionCount: editionResolutions.filter((edition) => edition.openlandsRequirement?.version === VERSION).length,
    },
  }
}

function buildLauncherChannelListing({ releaseIndex, editionResults }) {
  const moduleRequirementsResolved = editionResults.every((edition) => edition.moduleRequirementsResolved)
  return {
    schema: 'echo.openlands.launcher_channel_listing_preview.v1',
    status: 'preview_only',
    previewOnly: true,
    publicAlphaReady: false,
    clearsLauncherGates: false,
    clearsDistributionGates: false,
    channelId: 'openlands',
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    editionCount: editionResults.length,
    entries: editionResults.map((edition) => ({
      id: edition.id,
      packId: edition.packId,
      displayName: edition.displayName,
      runtimeTarget: edition.runtimeTarget,
      loader: edition.loader,
      launcherProfileKind: edition.launcherProfileKind,
      releaseIndexEntry: edition.releaseIndexEntry,
      releaseManifestPath: edition.releaseManifestPath,
      sourceRepo: edition.sourceRepo,
      moduleArtifact: {
        moduleId: MODULE_ID,
        version: VERSION,
        family: edition.artifactFamily,
        file: edition.artifact.file,
        sha256: edition.artifact.sha256,
        size: edition.artifact.size,
        downloadUrl: edition.artifact.releaseIndexDownloadUrl,
        downloadUrlPresent: edition.artifact.downloadUrlPresent,
      },
      sourceArtifact: edition.sourceArtifact,
      moduleRequirementResolutionStatus: edition.moduleRequirementsResolved ? 'passed' : 'failed',
      indexedState: 'preview_only',
      publicListingAllowed: false,
    })),
    blockedBy: [
      'real_launcher_channel_index_missing',
      'real_launcher_execution_missing',
      ...(moduleRequirementsResolved ? [] : ['module_requirement_resolution_failed']),
      'distribution_approval_missing',
      'preview_does_not_clear_distribution_gates',
    ],
  }
}

function buildReport({ moduleRoot, workspaceRoot, releaseRoot, outputPath, dryRun }) {
  const resourcesRoot = path.join(moduleRoot, 'src', 'main', 'resources')
  const dataRoot = path.join(resourcesRoot, 'data', MODULE_ID, 'openlands')
  const launcherFlow = readJson(path.join(dataRoot, 'systems', 'launcher_flow_acceptance.json'))
  const distributionApproval = readJson(path.join(dataRoot, 'systems', 'distribution_approval_acceptance.json'))
  const runtimePlan = readJson(path.join(dataRoot, 'systems', 'runtime_adapter_load_plan.json'))
  const distribution = readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json'))
  const descriptorPath = path.join(resourcesRoot, 'META-INF', 'echo.mod.json')
  const descriptor = readJson(descriptorPath)
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  assert(fileExists(releaseIndexPath), `release index not found: ${releaseIndexPath}`)
  const releaseIndex = readJson(releaseIndexPath)
  const releaseModule = (releaseIndex.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  assert(releaseModule, `${MODULE_ID} ${VERSION} missing from release index`)
  assert(descriptor.id === MODULE_ID, 'descriptor module id mismatch')
  assert(descriptor.version === VERSION, 'descriptor version mismatch')

  const publicAlphaGate = (runtimePlan.acceptanceGates ?? []).find((gate) => gate.id === 'public_alpha')
  assert(publicAlphaGate, 'runtime adapter load plan missing public_alpha gate')
  const editionManifestIndexingArea = (distributionApproval.approvalAreas ?? []).find((area) => area.id === 'edition_manifest_indexing')
  assert(editionManifestIndexingArea, 'distribution approval contract missing edition_manifest_indexing area')
  const distributionTargets = new Map((distribution.artifactTargets ?? []).map((target) => [target.id, target]))
  const matrixEntries = EDITION_ORDER.map((id) => {
    const entry = (launcherFlow.editionMatrix ?? []).find((item) => item.id === id)
    assert(entry, `launcher flow edition matrix missing ${id}`)
    return entry
  })

  const editionResults = matrixEntries.map((matrix) => {
    const editionRoot = path.join(workspaceRoot, matrix.editionRepo)
    const releaseManifestPath = path.join(editionRoot, matrix.releaseManifest)
    assert(fileExists(releaseManifestPath), `edition release manifest missing: ${releaseManifestPath}`)
    const manifest = readJson(releaseManifestPath)
    const artifactFile = matrix.artifactPattern
    const sourceFile = expandPattern(manifest.moduleSourcePattern)
    const releaseArtifact = releaseArtifactByFile(releaseModule, artifactFile)
    const sourceArtifactRecord = releaseArtifactByFile(releaseModule, sourceFile)
    assert(releaseArtifact, `release index missing artifact ${artifactFile}`)
    assert(sourceArtifactRecord, `release index missing source artifact ${sourceFile}`)
    const artifactPath = path.join(releaseRoot, MODULE_ID, artifactFile)
    const sourceArtifactPath = path.join(releaseRoot, MODULE_ID, sourceFile)
    assert(fileExists(artifactPath), `local artifact missing: ${artifactPath}`)
    assert(fileExists(sourceArtifactPath), `local source artifact missing: ${sourceArtifactPath}`)
    const artifactSha256 = sha256File(artifactPath)
    const artifactSize = fs.statSync(artifactPath).size
    const sourceSha256 = sha256File(sourceArtifactPath)
    const sourceSize = fs.statSync(sourceArtifactPath).size
    const manifestArtifactPattern = expandPattern(manifest.moduleArtifactPattern)
    const requiredDescriptors = matrix.requiredDescriptors ?? []
    const artifactContains = releaseArtifact.contains ?? []
    const moduleRequirementIds = sorted((manifest.moduleRequirements ?? []).map((requirement) => requirement.id))
    const missingCoreModuleIds = sorted((descriptor.requires ?? []).filter((moduleId) => !moduleRequirementIds.includes(moduleId)))
    const openlandsRequirement = requirementById(manifest.moduleRequirements, MODULE_ID)
    const moduleRequirementsResolved = missingCoreModuleIds.length === 0
      && openlandsRequirement?.version === VERSION
      && sameList(releaseModule.requires ?? [], descriptor.requires ?? [])
    const requiredPublicAlphaEvidenceMatches = sameList(manifest.requiredPublicAlphaEvidence ?? [], publicAlphaGate.requiresEvidence ?? [])
    const distributionTarget = distributionTargets.get(matrix.id)

    assert(manifest.packId === matrix.packId, `${matrix.id} packId mismatch`)
    assert(manifest.runtimeTarget === matrix.runtimeTarget, `${matrix.id} runtime target mismatch`)
    assert(manifest.loader === matrix.launcherProfileKind, `${matrix.id} loader/profile mismatch`)
    assert(manifest.moduleArtifactFamily === matrix.artifactFamily, `${matrix.id} artifact family mismatch`)
    assert(manifestArtifactPattern === matrix.artifactPattern, `${matrix.id} artifact pattern mismatch`)
    assert(sameList(manifest.requiredModuleDescriptors, requiredDescriptors), `${matrix.id} required descriptors mismatch`)
    assert(sameList(manifest.requiredAdapterLoadPhases, publicAlphaGate.requiresPhases), `${matrix.id} required adapter phases mismatch`)
    assert(requiredPublicAlphaEvidenceMatches, `${matrix.id} public alpha evidence mismatch`)
    assert(distributionTarget?.file === artifactFile, `${matrix.id} distribution target file mismatch`)
    assert(releaseArtifact.kind === matrix.artifactFamily, `${matrix.id} release artifact kind mismatch`)
    assert(normalizeRuntimeTarget(releaseArtifact.runtimeTarget) === matrix.runtimeTarget, `${matrix.id} release artifact runtime target mismatch`)
    assert(releaseArtifact.sha256 === artifactSha256, `${matrix.id} artifact sha mismatch`)
    assert(releaseArtifact.size === artifactSize, `${matrix.id} artifact size mismatch`)
    assert(sourceArtifactRecord.sha256 === sourceSha256, `${matrix.id} source artifact sha mismatch`)
    assert(sourceArtifactRecord.size === sourceSize, `${matrix.id} source artifact size mismatch`)
    for (const descriptorEntry of requiredDescriptors) {
      assert(artifactContains.includes(descriptorEntry), `${matrix.id} release artifact missing descriptor ${descriptorEntry}`)
    }

    return {
      id: matrix.id,
      repo: matrix.editionRepo,
      editionRoot,
      packId: matrix.packId,
      displayName: manifest.displayName,
      sourceRepo: manifest.sourceRepo,
      runtimeTarget: matrix.runtimeTarget,
      loader: manifest.loader,
      launcherProfileKind: matrix.launcherProfileKind,
      releaseManifestPath,
      releaseIndexEntry: matrix.releaseIndexEntry,
      artifactFamily: matrix.artifactFamily,
      artifactPattern: matrix.artifactPattern,
      sourcePattern: sourceFile,
      requiredDescriptors,
      requiredDescriptorsMatchManifest: sameList(manifest.requiredModuleDescriptors, requiredDescriptors),
      artifactContainsRequiredDescriptors: requiredDescriptors.every((descriptorEntry) => artifactContains.includes(descriptorEntry)),
      requiredPublicAlphaEvidence: manifest.requiredPublicAlphaEvidence ?? [],
      requiredPublicAlphaEvidenceMatches,
      moduleRequirements: manifest.moduleRequirements ?? [],
      moduleRequirementsResolved,
      missingCoreModuleIds,
      openlandsRequirement: openlandsRequirement ?? null,
      releaseIndexEntryResolved: true,
      artifact: {
        file: artifactFile,
        path: artifactPath,
        kind: releaseArtifact.kind,
        runtimeTarget: normalizeRuntimeTarget(releaseArtifact.runtimeTarget),
        buildMode: releaseArtifact.buildMode ?? null,
        sha256: artifactSha256,
        size: artifactSize,
        releaseIndexSha256: releaseArtifact.sha256,
        releaseIndexSize: releaseArtifact.size,
        releaseIndexDownloadUrl: releaseArtifact.downloadUrl ?? '',
        downloadUrlPresent: Boolean(releaseArtifact.downloadUrl),
        localArtifactExists: true,
        sha256MatchesReleaseIndex: releaseArtifact.sha256 === artifactSha256,
        sizeMatchesReleaseIndex: releaseArtifact.size === artifactSize,
        contains: artifactContains,
      },
      sourceArtifact: {
        file: sourceFile,
        path: sourceArtifactPath,
        kind: sourceArtifactRecord.kind,
        sha256: sourceSha256,
        size: sourceSize,
        releaseIndexSha256: sourceArtifactRecord.sha256,
        releaseIndexSize: sourceArtifactRecord.size,
        releaseIndexDownloadUrl: sourceArtifactRecord.downloadUrl ?? '',
        downloadUrlPresent: Boolean(sourceArtifactRecord.downloadUrl),
        localArtifactExists: true,
        sha256MatchesReleaseIndex: sourceArtifactRecord.sha256 === sourceSha256,
        sizeMatchesReleaseIndex: sourceArtifactRecord.size === sourceSize,
      },
      publicAlphaReady: false,
      indexedState: 'preview_only',
    }
  })

  const moduleRequirementResolution = buildModuleRequirementResolution({ descriptor, releaseModule, editionResults })
  const launcherChannelListing = buildLauncherChannelListing({ releaseIndex, editionResults })
  const moduleRequirementsResolved = moduleRequirementResolution.status === 'passed'
  const previewStatus = moduleRequirementsResolved ? 'preflight_passed' : 'preflight_blocked'
  const allDownloadUrlsPresent = editionResults.every((edition) => edition.artifact.downloadUrlPresent)
    && editionResults.every((edition) => edition.sourceArtifact.downloadUrlPresent)
  const blockedBy = [
    'real_launcher_channel_index_missing',
    'real_launcher_execution_missing',
    ...(moduleRequirementsResolved ? [] : ['module_requirement_resolution_failed']),
    ...(allDownloadUrlsPresent ? [] : ['release_index_download_urls_missing']),
    'distribution_approval_missing',
    'preview_does_not_clear_distribution_gates',
  ]
  const savedArtifactRoot = dryRun
    ? path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'openlands-edition-manifest-index-preview-')), 'saved-artifacts')
    : path.join(path.dirname(outputPath), 'openlands-edition-manifest-index-preview-artifacts')
  if (!dryRun) fs.rmSync(savedArtifactRoot, { recursive: true, force: true })
  fs.mkdirSync(savedArtifactRoot, { recursive: true })

  const editionIndexArtifacts = editionResults.map((edition) => {
    const relativePath = path.join('edition-index-entries', `${edition.id}-modpack-index-entry.preview.json`).replace(/\\/g, '/')
    const payload = {
      schema: 'echo.openlands.edition_modpack_index_entry_preview.v1',
      status: 'preview_only',
      previewOnly: true,
      publicAlphaReady: false,
      clearsLauncherGates: false,
      clearsDistributionGates: false,
      releaseId: releaseIndex.releaseId,
      moduleId: MODULE_ID,
      moduleVersion: VERSION,
      id: edition.id,
      packId: edition.packId,
      displayName: edition.displayName,
      runtimeTarget: edition.runtimeTarget,
      loader: edition.loader,
      releaseIndexEntry: edition.releaseIndexEntry,
      releaseManifestPath: edition.releaseManifestPath,
      requiredDescriptors: edition.requiredDescriptors,
      moduleRequirements: edition.moduleRequirements,
      moduleArtifact: edition.artifact,
      sourceArtifact: edition.sourceArtifact,
      blockedBy: [
        'real_launcher_channel_index_missing',
        'real_launcher_execution_missing',
        ...(edition.moduleRequirementsResolved ? [] : ['module_requirement_resolution_failed']),
        'distribution_approval_missing',
        'preview_does_not_clear_distribution_gates',
      ],
    }
    writeJson(path.join(savedArtifactRoot, relativePath), payload)
    return relativePath
  })

  const requiredSavedArtifacts = editionManifestIndexingArea.requiredSavedArtifacts ?? []
  const editionManifestIndexReport = {
    schema: 'echo.openlands.edition_manifest_index_report_preview.v1',
    status: previewStatus,
    previewOnly: true,
    publicAlphaReady: false,
    clearsLauncherGates: false,
    clearsDistributionGates: false,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    editionResults,
    requiredSavedArtifacts,
    editionIndexArtifacts,
    blockedBy,
  }
  writeJson(path.join(savedArtifactRoot, 'edition-manifest-index-report.json'), editionManifestIndexReport)
  writeJson(path.join(savedArtifactRoot, 'module-requirement-resolution.json'), moduleRequirementResolution)
  writeJson(path.join(savedArtifactRoot, 'launcher-channel-listing.json'), launcherChannelListing)

  const savedArtifacts = [
    'edition-manifest-index-report.json',
    'module-requirement-resolution.json',
    'launcher-channel-listing.json',
    ...editionIndexArtifacts,
  ]
  const report = {
    schema: 'echo.openlands.edition_manifest_index_preview.v1',
    status: previewStatus,
    publicAlphaReady: false,
    previewOnly: true,
    clearsLauncherGates: false,
    clearsDistributionGates: false,
    generatedAt: new Date().toISOString(),
    dryRun,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex.releaseId,
    releaseIndexPath,
    launcherFlowContract: LAUNCHER_FLOW_CONTRACT,
    distributionApprovalContract: DISTRIBUTION_APPROVAL_CONTRACT,
    distributionContract: DISTRIBUTION_CONTRACT,
    descriptorPath,
    savedArtifactRoot,
    savedArtifacts,
    editionResults,
    moduleRequirementResolution: {
      artifact: 'module-requirement-resolution.json',
      status: moduleRequirementResolution.status,
      passed: moduleRequirementResolution.status === 'passed',
      editionCount: moduleRequirementResolution.summary.editionCount,
      passedCount: moduleRequirementResolution.summary.passedCount,
      missingRequirementCount: moduleRequirementResolution.summary.missingRequirementCount,
      requiredCoreModules: moduleRequirementResolution.requiredCoreModules,
    },
    launcherChannelListing: {
      artifact: 'launcher-channel-listing.json',
      status: launcherChannelListing.status,
      editionCount: launcherChannelListing.editionCount,
      entries: launcherChannelListing.entries.map((entry) => ({
        id: entry.id,
        packId: entry.packId,
        runtimeTarget: entry.runtimeTarget,
        releaseIndexEntry: entry.releaseIndexEntry,
        artifactFile: entry.moduleArtifact.file,
      })),
    },
    summary: {
      editionCount: editionResults.length,
      requiredSavedArtifactCount: requiredSavedArtifacts.length,
      savedArtifactCount: savedArtifacts.length,
      releaseIndexEntryCount: editionResults.filter((edition) => edition.releaseIndexEntryResolved).length,
      localArtifactCount: editionResults.filter((edition) => edition.artifact.localArtifactExists).length,
      sourceArtifactCount: editionResults.filter((edition) => edition.sourceArtifact.localArtifactExists).length,
      artifactSha256MatchCount: editionResults.filter((edition) => edition.artifact.sha256MatchesReleaseIndex).length,
      artifactSizeMatchCount: editionResults.filter((edition) => edition.artifact.sizeMatchesReleaseIndex).length,
      sourceSha256MatchCount: editionResults.filter((edition) => edition.sourceArtifact.sha256MatchesReleaseIndex).length,
      sourceSizeMatchCount: editionResults.filter((edition) => edition.sourceArtifact.sizeMatchesReleaseIndex).length,
      requiredDescriptorMatchCount: editionResults.filter((edition) => edition.requiredDescriptorsMatchManifest && edition.artifactContainsRequiredDescriptors).length,
      moduleRequirementResolutionPassed: moduleRequirementResolution.status === 'passed',
      allDownloadUrlsPresent,
    },
    blockedBy,
    proofs: [
      'launcher_flow_contract_loaded',
      'distribution_approval_contract_loaded',
      'all_edition_manifests_loaded',
      'edition_matrix_matches_manifests',
      'module_requirement_graph_evaluated',
      ...(moduleRequirementsResolved ? ['module_requirement_graph_resolves_echoopenlandsprotocol'] : []),
      'local_release_index_artifacts_resolve',
      'launcher_channel_listing_preview_generated',
      'distribution_gate_stays_blocked_until_real_indexing',
    ],
    outputPath,
  }

  if (!dryRun) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true })
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  }
  return report
}

export async function main(argv = process.argv.slice(2)) {
  const args = parseArgs(argv)
  if (args.help) {
    printHelp()
    return null
  }
  const moduleRoot = args.moduleRoot ? path.resolve(args.moduleRoot) : defaultModuleRoot()
  const workspaceRoot = args.workspaceRoot ? path.resolve(args.workspaceRoot) : path.resolve(moduleRoot, '..', '..', '..')
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : defaultReleaseRoot(moduleRoot)
  const outputPath = args.output
    ? path.resolve(args.output)
    : path.join(releaseRoot, MODULE_ID, 'openlands-edition-manifest-index-preview.json')
  const report = buildReport({ moduleRoot, workspaceRoot, releaseRoot, outputPath, dryRun: args.dryRun })
  if (args.json) {
    console.log(JSON.stringify(report, null, 2))
  } else {
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    console.log(`Openlands edition manifest index preview ${action}: ${report.summary.editionCount} editions, savedArtifacts=${report.summary.savedArtifactCount}, publicAlphaReady=${report.publicAlphaReady}.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-edition-manifest-index-preview.mjs [options]

Options:
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --workspace-root <p>    Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to dist/echo-module-release/echoopenlandsprotocol/openlands-edition-manifest-index-preview.json.
  --dry-run               Generate saved artifacts in a temp directory without writing the report.
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

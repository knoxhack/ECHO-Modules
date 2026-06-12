import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const RELEASE_PUBLICATION_CONTRACT_PATH = 'data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json'
const EDITIONS = [
  {
    id: 'native',
    repo: 'ECHO-Openlands-Native-Edition',
    runtimeTarget: 'echo_native',
    runtimeExecutionReport: 'evidence/native-runtime-execution-report.json',
    localRuntimeRehearsalReport: 'evidence/native-local-runtime-rehearsal-report.json',
    distributionReport: 'evidence/native-distribution-roadmap-report.json',
    launcherFlowReport: 'evidence/native-launcher-flow-report.json',
    launcherExecutionReport: 'evidence/native-launcher-execution-report.json',
    localLauncherRehearsalReport: 'evidence/native-local-launcher-rehearsal-report.json',
    legalReport: 'evidence/native-legal-content-audit.json',
    finalReviewReport: 'evidence/native-final-release-review-report.json',
    distributionApprovalReport: 'evidence/native-distribution-approval-report.json',
  },
  {
    id: 'neoforge',
    repo: 'ECHO-Openlands-NeoForge-Edition',
    runtimeTarget: 'neoforge',
    runtimeExecutionReport: 'evidence/neoforge-runtime-execution-report.json',
    localRuntimeRehearsalReport: 'evidence/neoforge-local-runtime-rehearsal-report.json',
    distributionReport: 'evidence/neoforge-distribution-roadmap-report.json',
    launcherFlowReport: 'evidence/neoforge-launcher-flow-report.json',
    launcherExecutionReport: 'evidence/neoforge-launcher-execution-report.json',
    localLauncherRehearsalReport: 'evidence/neoforge-local-launcher-rehearsal-report.json',
    legalReport: 'evidence/neoforge-legal-content-audit.json',
    finalReviewReport: 'evidence/neoforge-final-release-review-report.json',
    distributionApprovalReport: 'evidence/neoforge-distribution-approval-report.json',
  },
  {
    id: 'standalone',
    repo: 'ECHO-Openlands-Standalone-Edition',
    runtimeTarget: 'echo_runtime_standalone',
    runtimeExecutionReport: 'evidence/standalone-runtime-execution-report.json',
    localRuntimeRehearsalReport: 'evidence/standalone-local-runtime-rehearsal-report.json',
    distributionReport: 'evidence/standalone-distribution-roadmap-report.json',
    launcherFlowReport: 'evidence/standalone-launcher-flow-report.json',
    launcherExecutionReport: 'evidence/standalone-launcher-execution-report.json',
    localLauncherRehearsalReport: 'evidence/standalone-local-launcher-rehearsal-report.json',
    legalReport: 'evidence/standalone-legal-content-audit.json',
    finalReviewReport: 'evidence/standalone-final-release-review-report.json',
    distributionApprovalReport: 'evidence/standalone-distribution-approval-report.json',
  },
]
const PHASE_READINESS_PLAN = [
  {
    id: 'phase_01_product_contract',
    readinessChecks: ['allLegalReady'],
    blockerIds: ['final_asset_legal_review_missing'],
    nextEvidence: [
      'Final human public identity, art, audio, and legal review signed for every edition.',
      'Final review reports clear final_asset_legal_review and final_art_audio_pass.',
    ],
  },
  {
    id: 'phase_02_repo_and_artifact_setup',
    readinessChecks: ['allArtifactsExist', 'allArtifactUrlsPresent', 'releasePublicationDownloadsVerified', 'releasePublicationApproved'],
    blockerIds: [
      'local_artifact_or_release_index_metadata_missing',
      'edition_manifest_index_preview_failed',
      'release_index_download_urls_missing',
      'download_verification_missing',
      'release_index_patch_not_approved',
    ],
    nextEvidence: [
      'Public HTTPS download URLs for Native, NeoForge, Standalone, and sources artifacts.',
      'Verified download SHA-256 and byte-size evidence for every artifact.',
      'Approved Release Index patch for the Openlands artifact entries.',
    ],
  },
  {
    id: 'phase_03_data_and_schema_layout',
    readinessChecks: ['allFinalReviewGatesCleared'],
    blockerIds: ['final_asset_legal_review_missing'],
    nextEvidence: [
      'Public-release-approved asset, audio, and generated-output review evidence.',
      'No placeholder-only public assets or unbound public sound events in final review evidence.',
    ],
  },
  {
    id: 'phase_04_mvp_block_registry',
    readinessChecks: ['allRuntimeGatesCleared', 'allFinalReviewGatesCleared'],
    blockerIds: ['runtime_execution_gates_not_cleared', 'final_asset_legal_review_missing'],
    nextEvidence: [
      'Real adapter runtime evidence for MVP block registration, drops, recipes, and rendering.',
      'Final block texture, model, blockstate, and ownership review evidence.',
    ],
  },
  {
    id: 'phase_05_mvp_item_registry',
    readinessChecks: ['allRuntimeGatesCleared', 'allFinalReviewGatesCleared'],
    blockerIds: ['runtime_execution_gates_not_cleared', 'final_asset_legal_review_missing'],
    nextEvidence: [
      'Real adapter runtime evidence for MVP item registration, tools, foods, and acquisition paths.',
      'Final item icon, model, tool silhouette, and ownership review evidence.',
    ],
  },
  {
    id: 'phase_06_crafting_and_stations',
    readinessChecks: ['allRuntimeGatesCleared'],
    blockerIds: ['runtime_execution_gates_not_cleared'],
    nextEvidence: [
      'Real adapter runtime evidence for handcrafting, workbench, kiln, forge, cookpot, and map-table flows.',
    ],
  },
  {
    id: 'phase_07_first_hour_gameplay',
    readinessChecks: ['allRuntimeGatesCleared'],
    blockerIds: ['runtime_execution_gates_not_cleared'],
    nextEvidence: [
      'Real first-hour route execution evidence for safe spawn, gathering, shelter, sleep, exploration, and first waystone.',
      'Save/load proof from the real edition runtimes, not only preflight rehearsal.',
    ],
  },
  {
    id: 'phase_08_worldgen_and_exploration',
    readinessChecks: ['allRuntimeGatesCleared', 'allFinalReviewGatesCleared'],
    blockerIds: ['runtime_execution_gates_not_cleared', 'final_asset_legal_review_missing'],
    nextEvidence: [
      'Real worldgen, creature, ambience, HoloMap, structure, and exploration execution evidence.',
      'Approved public audio sources and bound sound events for ambience and creature sounds.',
    ],
  },
  {
    id: 'phase_09_waystones_and_old_roads',
    readinessChecks: ['allRuntimeGatesCleared'],
    blockerIds: ['runtime_execution_gates_not_cleared'],
    nextEvidence: [
      'Real waystone repair, save/load, two-waystone travel, old-road route, and multiplayer permission evidence.',
    ],
  },
  {
    id: 'phase_10_alpha_systems_and_distribution',
    readinessChecks: ['allRuntimeGatesCleared', 'allLauncherReady', 'allDistributionReady', 'allArtifactUrlsPresent', 'releasePublicationDownloadsVerified', 'releasePublicationApproved'],
    blockerIds: [
      'runtime_execution_gates_not_cleared',
      'real_launcher_install_update_repair_rollback_missing',
      'distribution_approval_missing',
      'edition_manifest_index_preview_failed',
      'release_index_download_urls_missing',
      'download_verification_missing',
      'release_index_patch_not_approved',
    ],
    nextEvidence: [
      'Real homestead, builder/storage, co-op, and public-alpha system execution evidence.',
      'Real launcher install, update, repair, and rollback execution evidence.',
      'Real launcher channel index publication for the dependency-resolved edition manifests.',
      'Distribution approval evidence covering uploads, index listing, co-op session, dependency gates, and approval signature.',
    ],
  },
  {
    id: 'final_launch_phase_openlands_1_0_roadmap',
    readinessChecks: ['allRuntimeGatesCleared', 'allLauncherReady', 'allLegalReady', 'allDistributionReady', 'allArtifactUrlsPresent', 'releasePublicationDownloadsVerified', 'releasePublicationApproved'],
    blockerIds: [
      'runtime_execution_gates_not_cleared',
      'real_launcher_install_update_repair_rollback_missing',
      'final_asset_legal_review_missing',
      'distribution_approval_missing',
      'edition_manifest_index_preview_failed',
      'release_index_download_urls_missing',
      'download_verification_missing',
      'release_index_patch_not_approved',
    ],
    nextEvidence: [
      'Blocker-free readiness report with publicAlphaReady true.',
      'Public Alpha approval memo, rollback plan snapshot, Release Index approval, and distribution evidence IDs.',
    ],
  },
]

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

function fileExists(filePath) {
  return fs.existsSync(filePath)
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function defaultModuleRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

function sortedUnique(values) {
  return [...new Set((values ?? []).filter(Boolean))].sort()
}

function defaultPhaseReadinessMarkdownPath(outputPath) {
  const parsed = path.parse(outputPath)
  return path.join(parsed.dir, `${parsed.name}-by-phase.md`)
}

function addHandoffArtifact(artifacts, { id, label, filePath, purpose }) {
  if (artifacts.has(id)) return
  artifacts.set(id, {
    id,
    label,
    path: filePath,
    present: fileExists(filePath),
    purpose,
  })
}

function buildPhaseHandoffArtifacts({ activeBlockers, workspaceRoot, outputPath }) {
  const blockerSet = new Set(activeBlockers)
  const outputDir = path.dirname(outputPath)
  const artifacts = new Map()
  const add = (id, label, filePath, purpose) => addHandoffArtifact(artifacts, { id, label, filePath, purpose })

  if (blockerSet.has('release_index_download_urls_missing') || blockerSet.has('download_verification_missing')) {
    add(
      'publication_url_map_template',
      'Publication URL map template',
      path.join(outputDir, 'openlands-publication-url-map.template.json'),
      'Fill public HTTPS downloadUrl values for Native, Standalone, NeoForge, and sources before download verification.',
    )
    add(
      'verified_publication_manifest',
      'Verified publication manifest output',
      path.join(outputDir, 'openlands-release-publication-manifest.verified.json'),
      'Written by public download verification after downloaded hashes and sizes match the Release Index.',
    )
  }

  if (blockerSet.has('release_index_patch_not_approved') || blockerSet.has('distribution_approval_missing')) {
    add(
      'publication_approval_template',
      'Publication approval draft template',
      path.join(outputDir, 'openlands-release-publication-approval.template.json'),
      'Copy approvalDraft into the real approval JSON after verified downloads and passed distribution reports exist.',
    )
    add(
      'approved_publication_manifest',
      'Approved publication manifest output',
      path.join(outputDir, 'openlands-release-publication-manifest.json'),
      'Written by the approval tool after Release Index patch review and distribution signoff pass.',
    )
  }

  if (blockerSet.has('runtime_execution_gates_not_cleared')) {
    for (const edition of EDITIONS) {
      add(
        `${edition.id}_runtime_execution_report`,
        `${edition.id} runtime execution report`,
        path.join(workspaceRoot, edition.repo, edition.runtimeExecutionReport),
        'Replace the blocked report with real adapter runtime execution evidence.',
      )
    }
  }

  if (blockerSet.has('real_launcher_install_update_repair_rollback_missing')) {
    for (const edition of EDITIONS) {
      add(
        `${edition.id}_launcher_execution_report`,
        `${edition.id} launcher execution report`,
        path.join(workspaceRoot, edition.repo, edition.launcherExecutionReport),
        'Replace the blocked report with real launcher install, update, repair, and rollback evidence.',
      )
    }
  }

  if (blockerSet.has('final_asset_legal_review_missing')) {
    for (const edition of EDITIONS) {
      add(
        `${edition.id}_final_release_review_report`,
        `${edition.id} final release review report`,
        path.join(workspaceRoot, edition.repo, edition.finalReviewReport),
        'Replace the blocked report with human public identity, art, audio, and legal review signoff.',
      )
    }
  }

  if (blockerSet.has('distribution_approval_missing')) {
    for (const edition of EDITIONS) {
      add(
        `${edition.id}_distribution_approval_report`,
        `${edition.id} distribution approval report`,
        path.join(workspaceRoot, edition.repo, edition.distributionApprovalReport),
        'Replace the blocked report with public artifact, indexed manifest, co-op, dependency, rollback, and approval evidence.',
      )
    }
  }

  return [...artifacts.values()]
}

function buildPhaseReadiness({ productionMatrix, readinessChecks, blockers, outputPath, workspaceRoot }) {
  const blockerSet = new Set(blockers)
  const phaseById = new Map((productionMatrix.phases ?? []).map((phase) => [phase.id, phase]))
  const mappedBlockers = sortedUnique(PHASE_READINESS_PLAN.flatMap((phase) => phase.blockerIds).filter((blocker) => blockerSet.has(blocker)))
  const phases = PHASE_READINESS_PLAN.map((definition) => {
    const sourcePhase = phaseById.get(definition.id) ?? {}
    const blockingChecks = definition.readinessChecks.map((check) => ({
      id: check,
      passed: readinessChecks[check] === true,
    }))
    const activeBlockers = sortedUnique(definition.blockerIds.filter((blocker) => blockerSet.has(blocker)))
    const readyForPublicAlpha = activeBlockers.length === 0 && blockingChecks.every((check) => check.passed)
    return {
      id: definition.id,
      order: sourcePhase.order ?? null,
      displayName: sourcePhase.displayName ?? definition.id,
      status: readyForPublicAlpha ? 'ready' : 'blocked',
      readyForPublicAlpha,
      blockingChecks,
      activeBlockers,
      nextEvidence: definition.nextEvidence,
      handoffArtifacts: buildPhaseHandoffArtifacts({ activeBlockers, workspaceRoot, outputPath }),
    }
  })
  return {
    schema: 'echo.openlands.release_phase_readiness.v1',
    phaseCount: phases.length,
    blockedPhaseCount: phases.filter((phase) => phase.status === 'blocked').length,
    blockerCount: blockers.length,
    blockers,
    mappedBlockers,
    unmappedBlockers: sortedUnique(blockers.filter((blocker) => !mappedBlockers.includes(blocker))),
    markdownPath: defaultPhaseReadinessMarkdownPath(outputPath),
    phases,
  }
}

function renderPhaseReadinessMarkdown(report) {
  const phaseReadiness = report.phaseReadiness ?? {}
  const lines = [
    '# Openlands Release Readiness By Phase',
    '',
    `Status: ${report.status}`,
    `Public Alpha ready: ${report.publicAlphaReady}`,
    `Blockers: ${(report.blockers ?? []).length === 0 ? 'none' : report.blockers.join(', ')}`,
    '',
  ]
  for (const phase of phaseReadiness.phases ?? []) {
    lines.push(`## ${phase.order}. ${phase.displayName}`)
    lines.push('')
    lines.push(`Status: ${phase.status}`)
    lines.push(`Ready for Public Alpha: ${phase.readyForPublicAlpha}`)
    lines.push(`Active blockers: ${phase.activeBlockers.length === 0 ? 'none' : phase.activeBlockers.join(', ')}`)
    lines.push('')
    lines.push('Blocking checks:')
    for (const check of phase.blockingChecks ?? []) {
      lines.push(`- ${check.id}: ${check.passed ? 'passed' : 'blocked'}`)
    }
    lines.push('')
    lines.push('Next evidence:')
    for (const evidence of phase.nextEvidence ?? []) {
      lines.push(`- ${evidence}`)
    }
    if ((phase.handoffArtifacts ?? []).length > 0) {
      lines.push('')
      lines.push('Handoff files:')
      for (const artifact of phase.handoffArtifacts) {
        const state = artifact.present ? 'present' : 'expected'
        lines.push(`- ${artifact.label}: ${artifact.path} (${state}) - ${artifact.purpose}`)
      }
    }
    lines.push('')
  }
  return `${lines.join('\n').trimEnd()}\n`
}

function editionReport(workspaceRoot, edition, relativePath) {
  const filePath = path.join(workspaceRoot, edition.repo, relativePath)
  return fileExists(filePath) ? readJson(filePath) : null
}

function artifactByFile(releaseModule, filename) {
  return (releaseModule?.artifacts ?? []).find((artifact) => artifact.filename === filename)
}

function buildReport({ moduleRoot, workspaceRoot, releaseRoot, outputPath, dryRun }) {
  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const productionMatrix = readJson(path.join(dataRoot, 'progression', 'production_phase_matrix.json'))
  const runtimeExecution = readJson(path.join(dataRoot, 'systems', 'runtime_execution_acceptance.json'))
  const launcherExecution = readJson(path.join(dataRoot, 'systems', 'launcher_execution_acceptance.json'))
  const finalReview = readJson(path.join(dataRoot, 'systems', 'final_release_review_acceptance.json'))
  const distributionApproval = readJson(path.join(dataRoot, 'systems', 'distribution_approval_acceptance.json'))
  const distribution = readJson(path.join(dataRoot, 'systems', 'distribution_alpha_gates.json'))
  const releasePublicationContract = readJson(path.join(moduleRoot, 'src', 'main', 'resources', RELEASE_PUBLICATION_CONTRACT_PATH))
  const launchRoadmap = readJson(path.join(dataRoot, 'progression', 'launch_roadmap.json'))
  const conformance = readJson(path.join(dataRoot, 'conformance', 'openlands_mvp_registry.json'))
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const releaseIndex = fileExists(releaseIndexPath) ? readJson(releaseIndexPath) : null
  const releaseModule = (releaseIndex?.modules ?? []).find((entry) => entry.moduleId === MODULE_ID && entry.version === VERSION)
  const releasePublicationTemplatePath = path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.template.json')
  const releasePublicationVerifiedPath = path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.verified.json')
  const releasePublicationApprovedPath = path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-manifest.json')
  const releasePublicationManifestPath = fileExists(releasePublicationApprovedPath)
    ? releasePublicationApprovedPath
    : fileExists(releasePublicationVerifiedPath)
      ? releasePublicationVerifiedPath
      : releasePublicationTemplatePath
  const releasePublicationManifestSource = fileExists(releasePublicationApprovedPath)
    ? 'approved'
    : fileExists(releasePublicationVerifiedPath)
      ? 'verified'
      : 'template'
  const releasePublicationManifest = fileExists(releasePublicationManifestPath) ? readJson(releasePublicationManifestPath) : null
  const releasePublicationRehearsalPath = path.join(releaseRoot, MODULE_ID, 'openlands-release-publication-rehearsal-report.json')
  const releasePublicationRehearsal = fileExists(releasePublicationRehearsalPath) ? readJson(releasePublicationRehearsalPath) : null
  const editionManifestIndexPreviewPath = path.join(releaseRoot, MODULE_ID, 'openlands-edition-manifest-index-preview.json')
  const editionManifestIndexPreview = fileExists(editionManifestIndexPreviewPath) ? readJson(editionManifestIndexPreviewPath) : null

  const runtimeGateIds = sortedUnique((runtimeExecution.runtimeGates ?? []).map((gate) => gate.id))
  const launcherGateIds = sortedUnique((launcherExecution.launcherGates ?? []).map((gate) => gate.id))
  const launcherExecutionFlowIds = sortedUnique((launcherExecution.executionFlows ?? []).map((flow) => flow.id))
  const finalReviewGateIds = sortedUnique((finalReview.finalReviewGates ?? []).map((gate) => gate.id))
  const distributionGateIds = sortedUnique((distributionApproval.distributionGates ?? []).map((gate) => gate.id))
  const artifactTargets = distribution.artifactTargets ?? []
  const artifactResults = artifactTargets.map((target) => {
    const artifactPath = path.join(releaseRoot, MODULE_ID, target.file)
    const releaseArtifact = artifactByFile(releaseModule, target.file)
    const exists = fileExists(artifactPath)
    const actualSha256 = exists ? sha256File(artifactPath) : null
    return {
      id: target.id,
      file: target.file,
      requiredForPublicAlpha: target.requiredForPublicAlpha === true,
      exists,
      releaseIndexEntryPresent: releaseArtifact !== undefined,
      sha256Present: typeof releaseArtifact?.sha256 === 'string' && releaseArtifact.sha256.length === 64,
      sha256MatchesFile: exists && releaseArtifact?.sha256 === actualSha256,
      sizeMatchesFile: exists && releaseArtifact?.size === fs.statSync(artifactPath).size,
      downloadUrlPresent: typeof releaseArtifact?.downloadUrl === 'string' && releaseArtifact.downloadUrl.length > 0,
      buildMode: releaseArtifact?.buildMode ?? null,
    }
  })

  const editionResults = EDITIONS.map((edition) => {
    const runtimeReport = editionReport(workspaceRoot, edition, edition.runtimeExecutionReport)
    const localRuntimeRehearsalReport = editionReport(workspaceRoot, edition, edition.localRuntimeRehearsalReport)
    const distributionReport = editionReport(workspaceRoot, edition, edition.distributionReport)
    const launcherFlowReport = editionReport(workspaceRoot, edition, edition.launcherFlowReport)
    const launcherExecutionReport = editionReport(workspaceRoot, edition, edition.launcherExecutionReport)
    const localLauncherRehearsalReport = editionReport(workspaceRoot, edition, edition.localLauncherRehearsalReport)
    const legalReport = editionReport(workspaceRoot, edition, edition.legalReport)
    const finalReviewReport = editionReport(workspaceRoot, edition, edition.finalReviewReport)
    const distributionApprovalReport = editionReport(workspaceRoot, edition, edition.distributionApprovalReport)
    const clearedRuntimeGates = runtimeReport?.clearedRuntimeGates ?? []
    const remainingRuntimeGates = runtimeReport?.remainingRuntimeGates ?? runtimeGateIds
    const clearedLauncherGates = launcherExecutionReport?.clearedLauncherGates ?? []
    const remainingLauncherGates = launcherExecutionReport?.remainingLauncherGates ?? launcherGateIds
    const clearedFinalReviewGates = finalReviewReport?.clearedFinalReviewGates ?? []
    const remainingFinalReviewGates = finalReviewReport?.remainingFinalReviewGates ?? finalReviewGateIds
    const clearedDistributionGates = distributionApprovalReport?.clearedDistributionGates ?? []
    const remainingDistributionGates = distributionApprovalReport?.remainingDistributionGates ?? distributionGateIds
    return {
      id: edition.id,
      repo: edition.repo,
      runtimeTarget: edition.runtimeTarget,
      reportsPresent: {
        runtimeExecution: runtimeReport !== null,
        localRuntimeRehearsal: localRuntimeRehearsalReport !== null,
        distribution: distributionReport !== null,
        launcherFlow: launcherFlowReport !== null,
        launcherExecution: launcherExecutionReport !== null,
        localLauncherRehearsal: localLauncherRehearsalReport !== null,
        legal: legalReport !== null,
        finalReview: finalReviewReport !== null,
        distributionApproval: distributionApprovalReport !== null,
      },
      runtimeExecution: {
        status: runtimeReport?.status ?? 'missing',
        scenarioCount: runtimeReport?.scenarioResults?.length ?? 0,
        clearedRuntimeGates: clearedRuntimeGates.length,
        remainingRuntimeGates: remainingRuntimeGates.length,
        publicAlphaReady: runtimeReport?.publicAlphaReady === true,
      },
      localRuntimeRehearsal: {
        status: localRuntimeRehearsalReport?.status ?? 'missing',
        scenarioCount: localRuntimeRehearsalReport?.scenarioResults?.length ?? 0,
        rehearsalOnly: localRuntimeRehearsalReport?.rehearsalOnly === true,
        clearsRuntimeGates: localRuntimeRehearsalReport?.clearsRuntimeGates === true,
        publicAlphaReady: localRuntimeRehearsalReport?.publicAlphaReady === true,
      },
      distribution: {
        status: distributionReport?.status ?? 'missing',
        publicAlphaReady: distributionReport?.publicAlphaReady === true,
        uploadedArtifactUrlsPresent: distributionReport?.releaseIndex?.uploadedArtifactUrlsPresent === true,
      },
      distributionApproval: {
        status: distributionApprovalReport?.status ?? 'missing',
        approvalAreaCount: distributionApprovalReport?.approvalResults?.length ?? 0,
        clearedDistributionGates: clearedDistributionGates.length,
        remainingDistributionGates: remainingDistributionGates.length,
        publicAlphaReady: distributionApprovalReport?.publicAlphaReady === true,
      },
      launcherFlow: {
        status: launcherFlowReport?.status ?? 'missing',
        publicAlphaReady: launcherFlowReport?.publicAlphaReady === true,
      },
      launcherExecution: {
        status: launcherExecutionReport?.status ?? 'missing',
        flowCount: launcherExecutionReport?.flowResults?.length ?? 0,
        clearedLauncherGates: clearedLauncherGates.length,
        remainingLauncherGates: remainingLauncherGates.length,
        publicAlphaReady: launcherExecutionReport?.publicAlphaReady === true,
      },
      localLauncherRehearsal: {
        status: localLauncherRehearsalReport?.status ?? 'missing',
        flowCount: localLauncherRehearsalReport?.flowResults?.length ?? 0,
        rehearsalOnly: localLauncherRehearsalReport?.rehearsalOnly === true,
        clearsLauncherGates: localLauncherRehearsalReport?.clearsLauncherGates === true,
        publicAlphaReady: localLauncherRehearsalReport?.publicAlphaReady === true,
      },
      legal: {
        status: legalReport?.status ?? 'missing',
        publicReleaseAllowed: legalReport?.publicReleaseAllowed === true,
      },
      finalReview: {
        status: finalReviewReport?.status ?? 'missing',
        reviewAreaCount: finalReviewReport?.reviewResults?.length ?? 0,
        clearedFinalReviewGates: clearedFinalReviewGates.length,
        remainingFinalReviewGates: remainingFinalReviewGates.length,
        publicReleaseReady: finalReviewReport?.publicReleaseReady === true,
      },
    }
  })

  const allArtifactsExist = artifactResults.every((artifact) => artifact.exists && artifact.releaseIndexEntryPresent && artifact.sha256Present && artifact.sha256MatchesFile && artifact.sizeMatchesFile)
  const allArtifactUrlsPresent = artifactResults.every((artifact) => artifact.downloadUrlPresent)
  const allRuntimeReportsPresent = editionResults.every((edition) => edition.reportsPresent.runtimeExecution)
  const allRuntimeGatesCleared = editionResults.every((edition) => edition.runtimeExecution.clearedRuntimeGates === runtimeGateIds.length && edition.runtimeExecution.remainingRuntimeGates === 0)
  const allLocalRuntimeRehearsalsPresent = editionResults.every((edition) => edition.reportsPresent.localRuntimeRehearsal)
  const allLocalRuntimeRehearsalsPassed = allLocalRuntimeRehearsalsPresent && editionResults.every((edition) =>
    edition.localRuntimeRehearsal.status === 'preflight_passed'
    && edition.localRuntimeRehearsal.scenarioCount === (runtimeExecution.scenarios ?? []).length
    && edition.localRuntimeRehearsal.rehearsalOnly === true
    && edition.localRuntimeRehearsal.clearsRuntimeGates === false
    && edition.localRuntimeRehearsal.publicAlphaReady === false)
  const allLauncherReportsPresent = editionResults.every((edition) => edition.reportsPresent.launcherExecution)
  const allLauncherGatesCleared = editionResults.every((edition) => edition.launcherExecution.clearedLauncherGates === launcherGateIds.length && edition.launcherExecution.remainingLauncherGates === 0)
  const allLauncherReady = allLauncherReportsPresent && allLauncherGatesCleared && editionResults.every((edition) => edition.launcherExecution.publicAlphaReady)
  const allLocalLauncherRehearsalsPresent = editionResults.every((edition) => edition.reportsPresent.localLauncherRehearsal)
  const allLocalLauncherRehearsalsPassed = allLocalLauncherRehearsalsPresent && editionResults.every((edition) =>
    edition.localLauncherRehearsal.status === 'preflight_passed'
    && edition.localLauncherRehearsal.flowCount === launcherExecutionFlowIds.length
    && edition.localLauncherRehearsal.rehearsalOnly === true
    && edition.localLauncherRehearsal.clearsLauncherGates === false
    && edition.localLauncherRehearsal.publicAlphaReady === false)
  const allLegalPreflightReportsPresent = editionResults.every((edition) => edition.reportsPresent.legal)
  const allFinalReviewReportsPresent = editionResults.every((edition) => edition.reportsPresent.finalReview)
  const allFinalReviewGatesCleared = editionResults.every((edition) => edition.finalReview.clearedFinalReviewGates === finalReviewGateIds.length && edition.finalReview.remainingFinalReviewGates === 0)
  const allLegalReady = allLegalPreflightReportsPresent && allFinalReviewReportsPresent && allFinalReviewGatesCleared && editionResults.every((edition) => edition.finalReview.publicReleaseReady)
  const allDistributionApprovalReportsPresent = editionResults.every((edition) => edition.reportsPresent.distributionApproval)
  const allDistributionGatesCleared = editionResults.every((edition) => edition.distributionApproval.clearedDistributionGates === distributionGateIds.length && edition.distributionApproval.remainingDistributionGates === 0)
  const allDistributionReady = allDistributionApprovalReportsPresent && allDistributionGatesCleared && editionResults.every((edition) => edition.distributionApproval.publicAlphaReady)
  const releasePublicationManifestPresent = releasePublicationManifest?.schema === releasePublicationContract.reportContract?.schema
  const releasePublicationExpectedFiles = sortedUnique((releasePublicationContract.artifactTargets ?? []).map((target) => target.file))
  const releasePublicationActualFiles = sortedUnique((releasePublicationManifest?.artifactPublications ?? []).map((publication) => publication.file))
  const releasePublicationArtifactCoverageComplete = releasePublicationManifestPresent && JSON.stringify(releasePublicationActualFiles) === JSON.stringify(releasePublicationExpectedFiles)
  const releasePublicationDownloadsVerified = releasePublicationArtifactCoverageComplete && (releasePublicationManifest?.artifactPublications ?? []).every((publication) => {
    const verification = publication.downloadVerification ?? {}
    return publication.downloadUrl
      && verification.downloadAttempted === true
      && verification.sha256Matches === true
      && verification.sizeMatches === true
      && verification.downloadedSha256 === publication.sha256
      && verification.downloadedSize === publication.size
  })
  const releasePublicationApproved = releasePublicationDownloadsVerified
    && releasePublicationManifest?.status === 'approved'
    && (releasePublicationManifest?.artifactPublications ?? []).every((publication) => publication.urlStatus === 'approved' && publication.releaseIndexPatch?.patchApplied === true)
  const releasePublicationMissingDownloadUrlCount = releasePublicationManifest?.summary?.missingDownloadUrlCount
    ?? (releasePublicationManifest?.artifactPublications ?? []).filter((publication) => !publication.downloadUrl).length
  const releasePublicationDownloadVerifiedCount = releasePublicationManifest?.summary?.downloadVerifiedCount
    ?? (releasePublicationManifest?.artifactPublications ?? []).filter((publication) => publication.downloadVerification?.sha256Matches === true && publication.downloadVerification?.sizeMatches === true).length
  const releasePublicationRehearsalPresent = releasePublicationRehearsal?.schema === 'echo.openlands.release_publication_rehearsal_report.v1'
  const releasePublicationRehearsalLocalDownloadVerifiedCount = releasePublicationRehearsal?.summary?.localDownloadVerifiedCount
    ?? (releasePublicationRehearsal?.artifactResults ?? []).filter((artifact) => artifact.localDownloadBack?.sha256Matches === true && artifact.localDownloadBack?.sizeMatches === true).length
  const releasePublicationRehearsalPatchPreviewCount = releasePublicationRehearsal?.summary?.patchPreviewCount
    ?? (releasePublicationRehearsal?.artifactResults ?? []).filter((artifact) => artifact.releaseIndexPatchPreview?.patchApplied === false).length
  const releasePublicationRehearsalPassed = releasePublicationRehearsalPresent
    && releasePublicationRehearsal?.status === 'preflight_passed'
    && releasePublicationRehearsal?.publicAlphaReady === false
    && releasePublicationRehearsal?.rehearsalOnly === true
    && releasePublicationRehearsal?.clearsDistributionGates === false
    && releasePublicationRehearsal?.clearsReleasePublicationGates === false
    && releasePublicationRehearsal?.summary?.artifactCount === releasePublicationExpectedFiles.length
    && releasePublicationRehearsalLocalDownloadVerifiedCount === releasePublicationExpectedFiles.length
    && releasePublicationRehearsalPatchPreviewCount === releasePublicationExpectedFiles.length
  const editionManifestIndexPreviewPresent = editionManifestIndexPreview?.schema === 'echo.openlands.edition_manifest_index_preview.v1'
  const editionManifestIndexPreviewPassed = editionManifestIndexPreviewPresent
    && editionManifestIndexPreview?.status === 'preflight_passed'
    && editionManifestIndexPreview?.publicAlphaReady === false
    && editionManifestIndexPreview?.previewOnly === true
    && editionManifestIndexPreview?.clearsLauncherGates === false
    && editionManifestIndexPreview?.clearsDistributionGates === false
    && editionManifestIndexPreview?.summary?.editionCount === EDITIONS.length
    && editionManifestIndexPreview?.summary?.localArtifactCount === EDITIONS.length
    && editionManifestIndexPreview?.summary?.artifactSha256MatchCount === EDITIONS.length
    && editionManifestIndexPreview?.summary?.artifactSizeMatchCount === EDITIONS.length
    && editionManifestIndexPreview?.summary?.requiredDescriptorMatchCount === EDITIONS.length
    && editionManifestIndexPreview?.moduleRequirementResolution?.passed === true
    && editionManifestIndexPreview?.launcherChannelListing?.editionCount === EDITIONS.length
  const publicAlphaReady = allArtifactsExist
    && allArtifactUrlsPresent
    && releasePublicationManifestPresent
    && releasePublicationArtifactCoverageComplete
    && releasePublicationRehearsalPresent
    && releasePublicationRehearsalPassed
    && editionManifestIndexPreviewPresent
    && editionManifestIndexPreviewPassed
    && releasePublicationDownloadsVerified
    && releasePublicationApproved
    && allLocalLauncherRehearsalsPresent
    && allLocalLauncherRehearsalsPassed
    && allLocalRuntimeRehearsalsPresent
    && allLocalRuntimeRehearsalsPassed
    && allRuntimeReportsPresent
    && allRuntimeGatesCleared
    && allLauncherReady
    && allLegalReady
    && allDistributionReady

  const blockers = []
  if (!allArtifactsExist) blockers.push('local_artifact_or_release_index_metadata_missing')
  if (!allArtifactUrlsPresent) blockers.push('release_index_download_urls_missing')
  if (!releasePublicationManifestPresent) blockers.push('release_publication_manifest_missing')
  if (!releasePublicationArtifactCoverageComplete) blockers.push('release_publication_artifact_coverage_mismatch')
  if (!releasePublicationRehearsalPresent) blockers.push('release_publication_rehearsal_report_missing')
  if (!releasePublicationRehearsalPassed) blockers.push('release_publication_rehearsal_failed')
  if (!editionManifestIndexPreviewPresent) blockers.push('edition_manifest_index_preview_missing')
  if (!editionManifestIndexPreviewPassed) blockers.push('edition_manifest_index_preview_failed')
  if (!releasePublicationDownloadsVerified) blockers.push('download_verification_missing')
  if (!releasePublicationApproved) blockers.push('release_index_patch_not_approved')
  for (const blocker of releasePublicationManifest?.blockedBy ?? []) blockers.push(blocker)
  if (!allRuntimeReportsPresent) blockers.push('runtime_execution_reports_missing')
  if (!allLocalRuntimeRehearsalsPresent) blockers.push('local_runtime_rehearsal_reports_missing')
  if (!allLocalRuntimeRehearsalsPassed) blockers.push('local_runtime_rehearsal_failed')
  if (!allRuntimeGatesCleared) blockers.push('runtime_execution_gates_not_cleared')
  if (!allLauncherReportsPresent) blockers.push('launcher_execution_reports_missing')
  if (!allLauncherGatesCleared) blockers.push('real_launcher_install_update_repair_rollback_missing')
  if (!allLocalLauncherRehearsalsPresent) blockers.push('local_launcher_rehearsal_reports_missing')
  if (!allLocalLauncherRehearsalsPassed) blockers.push('local_launcher_rehearsal_failed')
  if (!allFinalReviewReportsPresent) blockers.push('final_release_review_reports_missing')
  if (!allLegalReady) blockers.push('final_asset_legal_review_missing')
  if (!allDistributionApprovalReportsPresent) blockers.push('distribution_approval_reports_missing')
  if (!allDistributionReady) blockers.push('distribution_approval_missing')

  const sortedBlockers = sortedUnique(blockers)
  const readinessChecks = {
    allArtifactsExist,
    allArtifactUrlsPresent,
    releasePublicationManifestPresent,
    releasePublicationArtifactCoverageComplete,
    releasePublicationRehearsalPresent,
    releasePublicationRehearsalPassed,
    editionManifestIndexPreviewPresent,
    editionManifestIndexPreviewPassed,
    releasePublicationDownloadsVerified,
    releasePublicationApproved,
    allRuntimeReportsPresent,
    allLocalRuntimeRehearsalsPresent,
    allLocalRuntimeRehearsalsPassed,
    allRuntimeGatesCleared,
    allLauncherReportsPresent,
    allLauncherGatesCleared,
    allLauncherReady,
    allLocalLauncherRehearsalsPresent,
    allLocalLauncherRehearsalsPassed,
    allLegalPreflightReportsPresent,
    allFinalReviewReportsPresent,
    allFinalReviewGatesCleared,
    allLegalReady,
    allDistributionApprovalReportsPresent,
    allDistributionGatesCleared,
    allDistributionReady,
  }
  const phaseReadiness = buildPhaseReadiness({
    productionMatrix,
    readinessChecks,
    blockers: sortedBlockers,
    outputPath,
    workspaceRoot,
  })
  const report = {
    schema: 'echo.openlands.release_readiness_report.v1',
    status: publicAlphaReady ? 'ready' : 'blocked',
    publicAlphaReady,
    generatedAt: new Date().toISOString(),
    dryRun,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: releaseIndex?.releaseId ?? null,
    productionMatrix: {
      phases: productionMatrix.counts?.phases ?? 0,
      checkpoints: productionMatrix.counts?.checkpoints ?? 0,
      runtimeGates: productionMatrix.counts?.runtimeGates ?? 0,
      missingEvidence: productionMatrix.counts?.missingEvidence ?? 0,
    },
    currentRegistryCounts: {
      blocks: conformance.blockRegistry?.length ?? 0,
      items: conformance.itemRegistry?.length ?? 0,
      recipes: conformance.recipeRegistry?.length ?? 0,
      biomes: conformance.biomeRegistry?.length ?? 0,
      creatures: conformance.creatureRegistry?.length ?? 0,
    },
    roadmap: {
      defaultRule: launchRoadmap.defaultRule,
      phaseIds: (launchRoadmap.phases ?? []).map((phase) => phase.id),
      invariants: launchRoadmap.nonNegotiableInvariants ?? [],
    },
    runtimeExecution: {
      gateCount: runtimeGateIds.length,
      scenarioCount: (runtimeExecution.scenarios ?? []).length,
      suiteCount: (runtimeExecution.executionSuites ?? []).length,
      reportContract: runtimeExecution.reportContract?.schema,
    },
    localRuntimeRehearsal: {
      reportContract: 'echo.openlands.edition.local_runtime_rehearsal_report.v1',
      requiredScenarioCount: (runtimeExecution.scenarios ?? []).length,
      requiredStatus: 'preflight_passed',
      rehearsalOnlyDoesNotClearGates: true,
      reportsPresent: editionResults.filter((edition) => edition.reportsPresent.localRuntimeRehearsal).length,
      reportsPassed: editionResults.filter((edition) =>
        edition.localRuntimeRehearsal.status === 'preflight_passed'
        && edition.localRuntimeRehearsal.scenarioCount === (runtimeExecution.scenarios ?? []).length
        && edition.localRuntimeRehearsal.rehearsalOnly === true
        && edition.localRuntimeRehearsal.clearsRuntimeGates === false
        && edition.localRuntimeRehearsal.publicAlphaReady === false).length,
      clearsRealRuntimeGates: false,
    },
    launcherExecution: {
      gateCount: launcherGateIds.length,
      flowCount: (launcherExecution.executionFlows ?? []).length,
      reportContract: launcherExecution.reportContract?.schema,
    },
    localLauncherRehearsal: {
      reportContract: 'echo.openlands.edition.local_launcher_rehearsal_report.v1',
      requiredFlowCount: launcherExecutionFlowIds.length,
      requiredStatus: 'preflight_passed',
      rehearsalOnlyDoesNotClearGates: true,
      reportsPresent: editionResults.filter((edition) => edition.reportsPresent.localLauncherRehearsal).length,
      reportsPassed: editionResults.filter((edition) =>
        edition.localLauncherRehearsal.status === 'preflight_passed'
        && edition.localLauncherRehearsal.flowCount === launcherExecutionFlowIds.length
        && edition.localLauncherRehearsal.rehearsalOnly === true
        && edition.localLauncherRehearsal.clearsLauncherGates === false
        && edition.localLauncherRehearsal.publicAlphaReady === false).length,
      clearsRealLauncherGates: false,
    },
    finalReleaseReview: {
      gateCount: finalReviewGateIds.length,
      reviewAreaCount: (finalReview.reviewAreas ?? []).length,
      reportContract: finalReview.reportContract?.schema,
    },
    distributionApproval: {
      gateCount: distributionGateIds.length,
      approvalAreaCount: (distributionApproval.approvalAreas ?? []).length,
      reportContract: distributionApproval.reportContract?.schema,
    },
    releasePublication: {
      contract: RELEASE_PUBLICATION_CONTRACT_PATH,
      templatePath: releasePublicationTemplatePath,
      verifiedPath: releasePublicationVerifiedPath,
      approvedPath: releasePublicationApprovedPath,
      manifestPath: releasePublicationManifestPath,
      manifestSource: releasePublicationManifestSource,
      manifestPresent: releasePublicationManifestPresent,
      manifestStatus: releasePublicationManifest?.status ?? 'missing',
      artifactCount: releasePublicationManifest?.artifactPublications?.length ?? 0,
      expectedArtifactFiles: releasePublicationExpectedFiles,
      actualArtifactFiles: releasePublicationActualFiles,
      missingDownloadUrlCount: releasePublicationMissingDownloadUrlCount,
      downloadVerifiedCount: releasePublicationDownloadVerifiedCount,
      releaseIndexPatchAllowedCount: releasePublicationManifest?.summary?.releaseIndexPatchAllowedCount ?? 0,
      approved: releasePublicationApproved,
      blockedBy: releasePublicationManifest?.blockedBy ?? ['release_publication_manifest_missing'],
    },
    releasePublicationRehearsal: {
      reportContract: 'echo.openlands.release_publication_rehearsal_report.v1',
      reportPath: releasePublicationRehearsalPath,
      reportPresent: releasePublicationRehearsalPresent,
      status: releasePublicationRehearsal?.status ?? 'missing',
      artifactCount: releasePublicationRehearsal?.summary?.artifactCount ?? 0,
      localDownloadVerifiedCount: releasePublicationRehearsalLocalDownloadVerifiedCount,
      patchPreviewCount: releasePublicationRehearsalPatchPreviewCount,
      rehearsalOnlyDoesNotClearGates: releasePublicationRehearsal?.rehearsalOnly === true
        && releasePublicationRehearsal?.clearsDistributionGates === false
        && releasePublicationRehearsal?.clearsReleasePublicationGates === false,
      clearsDistributionGates: releasePublicationRehearsal?.clearsDistributionGates === true,
      clearsReleasePublicationGates: releasePublicationRehearsal?.clearsReleasePublicationGates === true,
      publicAlphaReady: releasePublicationRehearsal?.publicAlphaReady === true,
      passed: releasePublicationRehearsalPassed,
      blockedBy: releasePublicationRehearsal?.blockedBy ?? ['release_publication_rehearsal_report_missing'],
    },
    editionManifestIndexPreview: {
      reportContract: 'echo.openlands.edition_manifest_index_preview.v1',
      reportPath: editionManifestIndexPreviewPath,
      reportPresent: editionManifestIndexPreviewPresent,
      status: editionManifestIndexPreview?.status ?? 'missing',
      editionCount: editionManifestIndexPreview?.summary?.editionCount ?? 0,
      savedArtifactCount: editionManifestIndexPreview?.summary?.savedArtifactCount ?? 0,
      moduleRequirementResolutionPassed: editionManifestIndexPreview?.moduleRequirementResolution?.passed === true,
      launcherChannelListingEditionCount: editionManifestIndexPreview?.launcherChannelListing?.editionCount ?? 0,
      previewOnlyDoesNotClearGates: editionManifestIndexPreview?.previewOnly === true
        && editionManifestIndexPreview?.clearsLauncherGates === false
        && editionManifestIndexPreview?.clearsDistributionGates === false,
      clearsLauncherGates: editionManifestIndexPreview?.clearsLauncherGates === true,
      clearsDistributionGates: editionManifestIndexPreview?.clearsDistributionGates === true,
      publicAlphaReady: editionManifestIndexPreview?.publicAlphaReady === true,
      passed: editionManifestIndexPreviewPassed,
      blockedBy: editionManifestIndexPreview?.blockedBy ?? ['edition_manifest_index_preview_missing'],
    },
    artifactResults,
    editionResults,
    readinessChecks,
    phaseReadiness,
    blockers: sortedBlockers,
    outputPath,
  }

  if (!dryRun) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true })
    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
    fs.writeFileSync(phaseReadiness.markdownPath, renderPhaseReadinessMarkdown(report), 'utf8')
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
  const releaseRoot = args.releaseRoot ? path.resolve(args.releaseRoot) : path.resolve(moduleRoot, '..', '..', 'dist', 'echo-module-release')
  const outputPath = args.output ? path.resolve(args.output) : path.join(releaseRoot, MODULE_ID, 'openlands-release-readiness-report.json')
  const report = buildReport({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    outputPath,
    dryRun: args.dryRun,
  })
  if (args.json) {
    console.log(JSON.stringify(report, null, 2))
  } else {
    const action = args.dryRun ? 'validated' : `wrote ${outputPath}`
    console.log(`Openlands release readiness report ${action}: publicAlphaReady=${report.publicAlphaReady}, blockers=${report.blockers.length}.`)
  }
  return report
}

function printHelp() {
  console.log(`Usage: node generate-openlands-release-readiness-report.mjs [options]

Options:
  --module-root <path>    Openlands module root. Defaults to this script's module.
  --workspace-root <p>    Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>   Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --out <path>            Report output path. Defaults to dist/echo-module-release/echoopenlandsprotocol/openlands-release-readiness-report.json.
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

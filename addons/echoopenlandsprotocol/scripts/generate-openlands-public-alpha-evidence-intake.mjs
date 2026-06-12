import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const SCHEMA = 'echo.openlands.public_alpha_evidence_intake.v1'

const EDITIONS = [
  {
    id: 'native',
    repo: 'ECHO-Openlands-Native-Edition',
    runtimeTarget: 'echo_native',
    runtimeExecutionReport: 'evidence/native-runtime-execution-report.json',
    launcherExecutionReport: 'evidence/native-launcher-execution-report.json',
    finalReviewReport: 'evidence/native-final-release-review-report.json',
    distributionApprovalReport: 'evidence/native-distribution-approval-report.json',
  },
  {
    id: 'neoforge',
    repo: 'ECHO-Openlands-NeoForge-Edition',
    runtimeTarget: 'neoforge',
    runtimeExecutionReport: 'evidence/neoforge-runtime-execution-report.json',
    launcherExecutionReport: 'evidence/neoforge-launcher-execution-report.json',
    finalReviewReport: 'evidence/neoforge-final-release-review-report.json',
    distributionApprovalReport: 'evidence/neoforge-distribution-approval-report.json',
  },
  {
    id: 'standalone',
    repo: 'ECHO-Openlands-Standalone-Edition',
    runtimeTarget: 'echo_runtime_standalone',
    runtimeExecutionReport: 'evidence/standalone-runtime-execution-report.json',
    launcherExecutionReport: 'evidence/standalone-launcher-execution-report.json',
    finalReviewReport: 'evidence/standalone-final-release-review-report.json',
    distributionApprovalReport: 'evidence/standalone-distribution-approval-report.json',
  },
]

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    releaseRoot: null,
    readinessReport: null,
    output: null,
    markdownOut: null,
    dryRun: false,
    json: false,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--workspace-root') args.workspaceRoot = argv[++index]
    else if (arg === '--release-root') args.releaseRoot = argv[++index]
    else if (arg === '--readiness-report') args.readinessReport = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--markdown-out') args.markdownOut = argv[++index]
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
  return JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''))
}

function writeText(filePath, text) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, text, 'utf8')
}

function writeJson(filePath, payload) {
  writeText(filePath, `${JSON.stringify(payload, null, 2)}\n`)
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

function defaultOutputPath(releaseRoot) {
  return path.join(releaseRoot, MODULE_ID, 'openlands-public-alpha-evidence-intake.json')
}

function defaultMarkdownPath(outputPath) {
  const parsed = path.parse(outputPath)
  return path.join(parsed.dir, `${parsed.name}.md`)
}

function safeJsonSummary(filePath) {
  if (!fileExists(filePath) || path.extname(filePath).toLowerCase() !== '.json') return {}
  try {
    const json = readJson(filePath)
    return {
      schema: json.schema ?? null,
      status: json.status ?? null,
      publicAlphaReady: json.publicAlphaReady ?? null,
      publicReleaseReady: json.publicReleaseReady ?? null,
      templateOnly: json.templateOnly ?? null,
      verifiedManifestStatus: json.verifiedManifestStatus ?? null,
      remainingRuntimeGates: Array.isArray(json.remainingRuntimeGates) ? json.remainingRuntimeGates.length : null,
      remainingLauncherGates: Array.isArray(json.remainingLauncherGates) ? json.remainingLauncherGates.length : null,
      remainingFinalReviewGates: Array.isArray(json.remainingFinalReviewGates) ? json.remainingFinalReviewGates.length : null,
      remainingDistributionGates: Array.isArray(json.remainingDistributionGates) ? json.remainingDistributionGates.length : null,
    }
  } catch {
    return {
      parseError: true,
    }
  }
}

function target({ id, label, filePath, purpose, required = true }) {
  const present = fileExists(filePath)
  return {
    id,
    label,
    path: filePath,
    required,
    present,
    sha256: present && fs.statSync(filePath).isFile() ? sha256File(filePath) : '',
    purpose,
    summary: safeJsonSummary(filePath),
  }
}

function editionTarget(workspaceRoot, edition, reportField, label, purpose) {
  return target({
    id: `${edition.id}_${reportField}`,
    label: `${edition.id} ${label}`,
    filePath: path.join(workspaceRoot, edition.repo, edition[reportField]),
    purpose,
  })
}

function impactedPhases(readinessReport, blockerId) {
  return (readinessReport.phaseReadiness?.phases ?? [])
    .filter((phase) => (phase.activeBlockers ?? []).includes(blockerId))
    .map((phase) => ({
      id: phase.id,
      order: phase.order,
      displayName: phase.displayName,
    }))
}

function releaseFile(releaseRoot, name) {
  return path.join(releaseRoot, MODULE_ID, name)
}

function blockerDefinitions({ workspaceRoot, releaseRoot }) {
  return [
    {
      blockerId: 'release_index_download_urls_missing',
      displayName: 'Public Artifact Hosting',
      ownerHint: 'release_publication_owner',
      clearsChecks: ['allArtifactUrlsPresent'],
      proofRequired: [
        'Publish Native, Standalone, NeoForge, and sources artifacts at public HTTPS URLs.',
        'Record the exact URLs in the publication URL map or Release Index patch input.',
        'Keep hashes and byte sizes matched to the current local Release Index entries.',
      ],
      evidenceTargets: [
        target({
          id: 'publication_url_map_template',
          label: 'Publication URL map template',
          filePath: releaseFile(releaseRoot, 'openlands-publication-url-map.template.json'),
          purpose: 'Fill downloadUrl values for all four artifacts before download verification.',
        }),
        target({
          id: 'release_index',
          label: 'Current Release Index',
          filePath: path.join(releaseRoot, 'echo-release.json'),
          purpose: 'Source of expected filenames, SHA-256 hashes, byte sizes, and eventual downloadUrl entries.',
        }),
        target({
          id: 'publication_manifest_template',
          label: 'Publication manifest template',
          filePath: releaseFile(releaseRoot, 'openlands-release-publication-manifest.template.json'),
          purpose: 'Current blocked publication manifest that enumerates the required artifact publications.',
        }),
      ],
      validationCommands: [
        'node addons/echoopenlandsprotocol/scripts/generate-openlands-publication-url-map-template.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release',
        'node addons/echoopenlandsprotocol/scripts/validate-openlands-publication-url-map.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release',
        'node addons/echoopenlandsprotocol/scripts/validate-openlands-publication-url-map.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release --url-map C:/path/to/openlands-publication-urls.json --require-urls',
        'node addons/echoopenlandsprotocol/scripts/verify-openlands-release-publication-downloads.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release --url-map C:/path/to/openlands-publication-urls.json',
        'node addons/echoopenlandsprotocol/scripts/validate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release',
      ],
    },
    {
      blockerId: 'download_verification_missing',
      displayName: 'Public Download Verification',
      ownerHint: 'release_publication_owner',
      clearsChecks: ['releasePublicationDownloadsVerified'],
      proofRequired: [
        'Download every public artifact URL through the verifier.',
        'Record final HTTPS URL, HTTP status, SHA-256, and byte-size evidence for every artifact.',
        'Write a verified publication manifest that still waits for approval before patching the live Release Index.',
      ],
      evidenceTargets: [
        target({
          id: 'verified_publication_manifest',
          label: 'Verified publication manifest',
          filePath: releaseFile(releaseRoot, 'openlands-release-publication-manifest.verified.json'),
          purpose: 'Verifier output after all public artifact downloads match expected hashes and sizes.',
        }),
        target({
          id: 'verification_artifacts',
          label: 'Verification artifact directory',
          filePath: releaseFile(releaseRoot, 'openlands-release-publication-verification-artifacts'),
          purpose: 'Saved download cache, per-artifact evidence JSON, and verification summary.',
        }),
      ],
      validationCommands: [
        'node addons/echoopenlandsprotocol/scripts/verify-openlands-release-publication-downloads.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release --url-map C:/path/to/openlands-publication-urls.json',
        'node addons/echoopenlandsprotocol/scripts/validate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol --release-root dist/echo-module-release --manifest dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.verified.json',
      ],
    },
    {
      blockerId: 'release_index_patch_not_approved',
      displayName: 'Release Index Patch Approval',
      ownerHint: 'release_manager',
      clearsChecks: ['releasePublicationApproved'],
      proofRequired: [
        'Review the verified download URLs and approve the Release Index artifact patch.',
        'Fill a real approval JSON from the generated approvalDraft, including patch id and Release Index commit.',
        'Write the approved publication manifest and reviewed patched Release Index output, or intentionally apply the live Release Index patch.',
      ],
      evidenceTargets: [
        target({
          id: 'publication_approval_template',
          label: 'Publication approval draft template',
          filePath: releaseFile(releaseRoot, 'openlands-release-publication-approval.template.json'),
          purpose: 'Template-only draft with current distribution report paths, hashes, checklist ids, and approvalDraft.',
        }),
        target({
          id: 'approved_publication_manifest',
          label: 'Approved publication manifest',
          filePath: releaseFile(releaseRoot, 'openlands-release-publication-manifest.json'),
          purpose: 'Approval tool output after verified downloads, distribution signoff, and patch approval pass.',
        }),
        target({
          id: 'release_index_patch_summary',
          label: 'Release Index patch summary',
          filePath: releaseFile(releaseRoot, 'openlands-release-index-patch-summary.json'),
          purpose: 'Sidecar proving the approved patch id, commit, target Release Index, and artifact patch list.',
        }),
      ],
      validationCommands: [
        'node addons/echoopenlandsprotocol/scripts/generate-openlands-release-publication-approval-template.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release',
        'node addons/echoopenlandsprotocol/scripts/approve-openlands-release-publication.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release --approval C:/path/to/openlands-publication-approval.json --release-index-out dist/echo-module-release/echoopenlandsprotocol/echo-release.approved.preview.json',
        'node addons/echoopenlandsprotocol/scripts/validate-openlands-release-publication-manifest.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release --manifest dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.json',
      ],
    },
    {
      blockerId: 'runtime_execution_gates_not_cleared',
      displayName: 'Real Runtime Execution',
      ownerHint: 'edition_runtime_owners',
      clearsChecks: ['allRuntimeGatesCleared'],
      proofRequired: [
        'Run real Native, NeoForge, and Standalone adapter runtime scenarios, not local rehearsals.',
        'Clear every runtime gate and keep saved scenario artifacts under each edition evidence root.',
        'Prove first-hour, worldgen, creature, waystone, old-road, homestead, builder/storage, launcher/distribution, and final-review runtime scenario coverage.',
      ],
      evidenceTargets: EDITIONS.map((edition) => editionTarget(
        workspaceRoot,
        edition,
        'runtimeExecutionReport',
        'runtime execution report',
        'Real adapter runtime execution report that must clear all runtime gates.',
      )),
      validationCommands: EDITIONS.map((edition) =>
        `node addons/echoopenlandsprotocol/scripts/validate-openlands-runtime-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition ${edition.id} --edition-root C:/Development/Github/${edition.repo}`),
    },
    {
      blockerId: 'real_launcher_install_update_repair_rollback_missing',
      displayName: 'Real Launcher Execution',
      ownerHint: 'launcher_owner',
      clearsChecks: ['allLauncherReady'],
      proofRequired: [
        'Run real launcher fresh install, artifact update, corrupt-install repair, and manifest rollback flows.',
        'Preserve world/config state and attach saved launcher artifacts for every passed flow.',
        'Clear every launcher gate for Native, NeoForge, and Standalone editions.',
      ],
      evidenceTargets: EDITIONS.map((edition) => editionTarget(
        workspaceRoot,
        edition,
        'launcherExecutionReport',
        'launcher execution report',
        'Real launcher install/update/repair/rollback report that must clear all launcher gates.',
      )),
      validationCommands: EDITIONS.map((edition) =>
        `node addons/echoopenlandsprotocol/scripts/validate-openlands-launcher-execution-report.mjs --module-root addons/echoopenlandsprotocol --edition ${edition.id} --edition-root C:/Development/Github/${edition.repo}`),
    },
    {
      blockerId: 'final_asset_legal_review_missing',
      displayName: 'Final Product, Asset, Audio, And Legal Review',
      ownerHint: 'human_review_owner',
      clearsChecks: ['allLegalReady'],
      proofRequired: [
        'Complete human public identity, art, block/item asset, audio source, and generated-output review for every edition.',
        'Remove placeholder-only public assets, copied/borrowed assets, unapproved public audio, and unbound public sound events.',
        'Clear every final review gate and mark every edition publicReleaseReady.',
      ],
      evidenceTargets: EDITIONS.map((edition) => editionTarget(
        workspaceRoot,
        edition,
        'finalReviewReport',
        'final release review report',
        'Human final review report that must clear all final review gates.',
      )),
      validationCommands: EDITIONS.map((edition) =>
        `node addons/echoopenlandsprotocol/scripts/validate-openlands-final-release-review-report.mjs --module-root addons/echoopenlandsprotocol --edition ${edition.id} --edition-root C:/Development/Github/${edition.repo}`),
    },
    {
      blockerId: 'distribution_approval_missing',
      displayName: 'Final Distribution Approval',
      ownerHint: 'release_approver',
      clearsChecks: ['allDistributionReady'],
      proofRequired: [
        'Attach public artifact publication, verified downloads, non-preview edition manifest indexing, dependency gates, co-op session evidence, rollback plan, and approval signature.',
        'Reopen runtime, launcher, final review, and readiness evidence by hash from approval-input-report-index.json.',
        'Clear every distribution gate and mark every edition publicAlphaReady.',
      ],
      evidenceTargets: [
        ...EDITIONS.map((edition) => editionTarget(
          workspaceRoot,
          edition,
          'distributionApprovalReport',
          'distribution approval report',
          'Final distribution approval report that must clear all distribution gates.',
        )),
        target({
          id: 'publication_approval_template',
          label: 'Publication approval draft template',
          filePath: releaseFile(releaseRoot, 'openlands-release-publication-approval.template.json'),
          purpose: 'Tracks current distribution approval report hashes for the publication approval draft.',
        }),
      ],
      validationCommands: [
        ...EDITIONS.map((edition) =>
          `node addons/echoopenlandsprotocol/scripts/validate-openlands-distribution-approval-report.mjs --module-root addons/echoopenlandsprotocol --edition ${edition.id} --edition-root C:/Development/Github/${edition.repo}`),
        'node addons/echoopenlandsprotocol/scripts/generate-openlands-public-alpha-approval-packet-template.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release',
        'node addons/echoopenlandsprotocol/scripts/validate-openlands-public-alpha-approval-packet-template.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release',
        'node addons/echoopenlandsprotocol/scripts/validate-openlands-release-readiness-report.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release',
      ],
    },
  ]
}

function unique(values) {
  return [...new Set((values ?? []).filter(Boolean))]
}

function compactEvidenceTarget(evidenceTarget) {
  return {
    id: evidenceTarget.id,
    label: evidenceTarget.label,
    path: evidenceTarget.path,
    required: evidenceTarget.required === true,
    present: evidenceTarget.present === true,
    status: evidenceTarget.summary?.status ?? null,
    templateOnly: evidenceTarget.summary?.templateOnly ?? null,
    purpose: evidenceTarget.purpose,
  }
}

function buildPhaseHandoff({ readinessReport, intakeItems }) {
  const itemByBlocker = new Map(intakeItems.map((item) => [item.blockerId, item]))
  return (readinessReport.phaseReadiness?.phases ?? []).map((phase) => {
    const activeBlockers = phase.activeBlockers ?? []
    const blockerRequirements = activeBlockers
      .map((blockerId) => itemByBlocker.get(blockerId))
      .filter(Boolean)
      .map((item) => ({
        blockerId: item.blockerId,
        displayName: item.displayName,
        ownerHint: item.ownerHint,
        clearsChecks: item.clearsChecks ?? [],
        proofRequired: item.proofRequired ?? [],
        evidenceTargets: (item.evidenceTargets ?? []).map(compactEvidenceTarget),
        validationCommands: item.validationCommands ?? [],
      }))
    return {
      id: phase.id,
      order: phase.order,
      displayName: phase.displayName,
      status: phase.status,
      readyForPublicAlpha: phase.readyForPublicAlpha,
      blockingChecks: phase.blockingChecks ?? [],
      activeBlockers,
      ownerHints: unique(blockerRequirements.map((requirement) => requirement.ownerHint)),
      nextEvidence: phase.nextEvidence ?? [],
      handoffArtifacts: phase.handoffArtifacts ?? [],
      blockerRequirements,
      validationCommands: unique(blockerRequirements.flatMap((requirement) => requirement.validationCommands ?? [])),
    }
  })
}

export function buildEvidenceIntake({ moduleRoot, workspaceRoot, releaseRoot, readinessReportPath, outputPath, markdownPath, dryRun }) {
  if (!fileExists(readinessReportPath)) throw new Error(`readiness report not found: ${readinessReportPath}`)
  const readinessReport = readJson(readinessReportPath)
  const blockers = readinessReport.blockers ?? []
  const blockerSet = new Set(blockers)
  const intakeItems = blockerDefinitions({ workspaceRoot, releaseRoot }).map((definition) => {
    const active = blockerSet.has(definition.blockerId)
    return {
      blockerId: definition.blockerId,
      displayName: definition.displayName,
      active,
      status: active ? 'blocked_pending_external_evidence' : 'cleared',
      ownerHint: definition.ownerHint,
      impactedPhases: impactedPhases(readinessReport, definition.blockerId),
      clearsChecks: definition.clearsChecks,
      proofRequired: definition.proofRequired,
      evidenceTargets: definition.evidenceTargets,
      validationCommands: definition.validationCommands,
    }
  })
  const activeItems = intakeItems.filter((item) => item.active)
  const phaseHandoff = buildPhaseHandoff({ readinessReport, intakeItems })

  return {
    schema: SCHEMA,
    generatedAt: new Date().toISOString(),
    dryRun,
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: readinessReport.releaseId ?? null,
    status: activeItems.length === 0 && readinessReport.publicAlphaReady === true ? 'ready_for_public_alpha_review' : 'blocked_pending_external_evidence',
    publicAlphaReady: readinessReport.publicAlphaReady === true,
    readinessReportPath,
    readinessReportSha256: sha256File(readinessReportPath),
    readinessPhaseMarkdownPath: readinessReport.phaseReadiness?.markdownPath ?? defaultMarkdownPath(readinessReportPath),
    releaseRoot,
    workspaceRoot,
    blockerCount: blockers.length,
    blockers,
    activeIntakeItemCount: activeItems.length,
    intakeItems,
    phaseSummary: (readinessReport.phaseReadiness?.phases ?? []).map((phase) => ({
      id: phase.id,
      order: phase.order,
      displayName: phase.displayName,
      status: phase.status,
      readyForPublicAlpha: phase.readyForPublicAlpha,
      activeBlockers: phase.activeBlockers ?? [],
      handoffArtifactCount: (phase.handoffArtifacts ?? []).length,
    })),
    phaseHandoff,
    releasePublication: {
      manifestStatus: readinessReport.releasePublication?.manifestStatus ?? 'missing',
      manifestSource: readinessReport.releasePublication?.manifestSource ?? 'missing',
      missingDownloadUrlCount: readinessReport.releasePublication?.missingDownloadUrlCount ?? null,
      downloadVerifiedCount: readinessReport.releasePublication?.downloadVerifiedCount ?? null,
      approved: readinessReport.releasePublication?.approved === true,
    },
    nextValidationCommands: [
      'node addons/echoopenlandsprotocol/scripts/validate-openlands-public-alpha-evidence-intake.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release',
      'node addons/echoopenlandsprotocol/scripts/validate-openlands-release-readiness-report.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github --release-root dist/echo-module-release',
    ],
    outputPath,
    markdownPath,
  }
}

export function renderEvidenceIntakeMarkdown(intake) {
  const lines = [
    '# Openlands Public Alpha Evidence Intake',
    '',
    `Status: ${intake.status}`,
    `Public Alpha ready: ${intake.publicAlphaReady}`,
    `Readiness report: ${intake.readinessReportPath}`,
    `Blockers: ${intake.blockers.length === 0 ? 'none' : intake.blockers.join(', ')}`,
    '',
    '## Active Intake Items',
    '',
  ]
  for (const item of intake.intakeItems.filter((entry) => entry.active)) {
    lines.push(`### ${item.displayName}`)
    lines.push('')
    lines.push(`Blocker: ${item.blockerId}`)
    lines.push(`Owner hint: ${item.ownerHint}`)
    lines.push(`Impacted phases: ${item.impactedPhases.length === 0 ? 'none' : item.impactedPhases.map((phase) => `${phase.order}. ${phase.displayName}`).join(', ')}`)
    lines.push('')
    lines.push('Required proof:')
    for (const proof of item.proofRequired) lines.push(`- ${proof}`)
    lines.push('')
    lines.push('Evidence targets:')
    for (const evidenceTarget of item.evidenceTargets) {
      const state = evidenceTarget.present ? 'present' : 'expected'
      const status = evidenceTarget.summary?.status ? `, status=${evidenceTarget.summary.status}` : ''
      lines.push(`- ${evidenceTarget.label}: ${evidenceTarget.path} (${state}${status})`)
    }
    lines.push('')
    lines.push('Validation commands:')
    for (const command of item.validationCommands) lines.push(`- ${command}`)
    lines.push('')
  }
  lines.push('## Phase Handoff')
  lines.push('')
  for (const phase of intake.phaseHandoff ?? []) {
    lines.push(`### ${phase.order}. ${phase.displayName}`)
    lines.push('')
    lines.push(`Status: ${phase.status}`)
    lines.push(`Ready for Public Alpha: ${phase.readyForPublicAlpha}`)
    lines.push(`Active blockers: ${phase.activeBlockers.length === 0 ? 'none' : phase.activeBlockers.join(', ')}`)
    if ((phase.ownerHints ?? []).length > 0) lines.push(`Owner hints: ${phase.ownerHints.join(', ')}`)
    lines.push('')
    lines.push('Next evidence:')
    if ((phase.nextEvidence ?? []).length === 0) {
      lines.push('- none')
    } else {
      for (const evidence of phase.nextEvidence ?? []) lines.push(`- ${evidence}`)
    }
    lines.push('')
    lines.push('Proof requirements:')
    if ((phase.blockerRequirements ?? []).length === 0) {
      lines.push('- none')
    } else {
      for (const requirement of phase.blockerRequirements ?? []) {
        lines.push(`- ${requirement.displayName} (${requirement.blockerId})`)
        for (const proof of requirement.proofRequired ?? []) lines.push(`  - ${proof}`)
      }
    }
    lines.push('')
    lines.push('Handoff files:')
    if ((phase.handoffArtifacts ?? []).length === 0) {
      lines.push('- none')
    } else {
      for (const artifact of phase.handoffArtifacts ?? []) {
        const state = artifact.present ? 'present' : 'expected'
        lines.push(`- ${artifact.label}: ${artifact.path} (${state})`)
      }
    }
    lines.push('')
    lines.push('Validation commands:')
    if ((phase.validationCommands ?? []).length === 0) {
      lines.push('- none')
    } else {
      for (const command of phase.validationCommands ?? []) lines.push(`- ${command}`)
    }
    lines.push('')
  }
  lines.push('## Next Validation')
  lines.push('')
  for (const command of intake.nextValidationCommands) lines.push(`- ${command}`)
  return `${lines.join('\n').trimEnd()}\n`
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
  const outputPath = args.output ? path.resolve(args.output) : defaultOutputPath(releaseRoot)
  const markdownPath = args.markdownOut ? path.resolve(args.markdownOut) : defaultMarkdownPath(outputPath)
  const readinessReportPath = args.readinessReport
    ? path.resolve(args.readinessReport)
    : path.join(releaseRoot, MODULE_ID, 'openlands-release-readiness-report.json')
  const intake = buildEvidenceIntake({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    readinessReportPath,
    outputPath,
    markdownPath,
    dryRun: args.dryRun,
  })
  if (!args.dryRun) {
    writeJson(outputPath, intake)
    writeText(markdownPath, renderEvidenceIntakeMarkdown(intake))
  }
  if (args.json) {
    console.log(JSON.stringify(intake, null, 2))
  } else {
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    console.log(`Openlands public alpha evidence intake ${action}: activeItems=${intake.activeIntakeItemCount}, blockers=${intake.blockerCount}.`)
  }
  return intake
}

function printHelp() {
  console.log(`Usage: node generate-openlands-public-alpha-evidence-intake.mjs [options]

Options:
  --module-root <path>        Openlands module root. Defaults to this script's module.
  --workspace-root <path>     Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>       Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --readiness-report <path>   Readiness report path. Defaults to openlands-release-readiness-report.json.
  --out <path>                Intake JSON output. Defaults to openlands-public-alpha-evidence-intake.json.
  --markdown-out <path>       Intake Markdown output. Defaults beside the JSON output.
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

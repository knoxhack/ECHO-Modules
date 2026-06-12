import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const MODULE_ID = 'echoopenlandsprotocol'
const VERSION = '0.1.0'
const SCHEMA = 'echo.openlands.public_alpha_approval_packet_template.v1'
const DATA_ROOT = ['src', 'main', 'resources', 'data', MODULE_ID, 'openlands']
const RELEASE_PUBLICATION_CONTRACT = 'systems/release_publication_manifest_contract.json'
const DISTRIBUTION_APPROVAL_CONTRACT = 'systems/distribution_approval_acceptance.json'
const LAUNCH_ROADMAP = 'progression/launch_roadmap.json'
const PRODUCTION_PHASE_MATRIX = 'progression/production_phase_matrix.json'

const EDITIONS = [
  {
    id: 'native',
    repo: 'ECHO-Openlands-Native-Edition',
    runtimeTarget: 'echo_native',
    reports: {
      runtimeExecution: 'evidence/native-runtime-execution-report.json',
      launcherExecution: 'evidence/native-launcher-execution-report.json',
      finalReview: 'evidence/native-final-release-review-report.json',
      distributionApproval: 'evidence/native-distribution-approval-report.json',
    },
  },
  {
    id: 'neoforge',
    repo: 'ECHO-Openlands-NeoForge-Edition',
    runtimeTarget: 'neoforge',
    reports: {
      runtimeExecution: 'evidence/neoforge-runtime-execution-report.json',
      launcherExecution: 'evidence/neoforge-launcher-execution-report.json',
      finalReview: 'evidence/neoforge-final-release-review-report.json',
      distributionApproval: 'evidence/neoforge-distribution-approval-report.json',
    },
  },
  {
    id: 'standalone',
    repo: 'ECHO-Openlands-Standalone-Edition',
    runtimeTarget: 'echo_runtime_standalone',
    reports: {
      runtimeExecution: 'evidence/standalone-runtime-execution-report.json',
      launcherExecution: 'evidence/standalone-launcher-execution-report.json',
      finalReview: 'evidence/standalone-final-release-review-report.json',
      distributionApproval: 'evidence/standalone-distribution-approval-report.json',
    },
  },
]

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    releaseRoot: null,
    readinessReport: null,
    evidenceIntake: null,
    output: null,
    packetRoot: null,
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
    else if (arg === '--evidence-intake') args.evidenceIntake = argv[++index]
    else if (arg === '--out') args.output = argv[++index]
    else if (arg === '--packet-root') args.packetRoot = argv[++index]
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

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function writeText(filePath, text) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, text, 'utf8')
}

function writeJson(filePath, payload) {
  writeText(filePath, `${JSON.stringify(payload, null, 2)}\n`)
}

function sha256Bytes(bytes) {
  return crypto.createHash('sha256').update(bytes).digest('hex')
}

function sha256File(filePath) {
  return sha256Bytes(fs.readFileSync(filePath))
}

function sha256Text(text) {
  return sha256Bytes(Buffer.from(text, 'utf8'))
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

function defaultOutputPath(releaseRoot) {
  return releaseFile(releaseRoot, 'openlands-public-alpha-approval-packet.template.json')
}

function defaultPacketRoot(outputPath) {
  return path.join(path.dirname(outputPath), 'openlands-public-alpha-approval-packet-template')
}

function dataPath(moduleRoot, relativePath) {
  return path.join(moduleRoot, ...DATA_ROOT, relativePath)
}

function readJsonIfPresent(filePath) {
  if (!fileExists(filePath)) return null
  return readJson(filePath)
}

function fileRecord({ id, category, filePath, requiredForApproval = true }) {
  const present = fileExists(filePath)
  let summary = {}
  if (present && path.extname(filePath).toLowerCase() === '.json') {
    try {
      const payload = readJson(filePath)
      summary = {
        schema: payload.schema ?? null,
        status: payload.status ?? null,
        publicAlphaReady: payload.publicAlphaReady ?? null,
        publicReleaseReady: payload.publicReleaseReady ?? null,
        templateOnly: payload.templateOnly ?? null,
        blockers: Array.isArray(payload.blockers) ? payload.blockers : undefined,
      }
    } catch {
      summary = {
        parseError: true,
      }
    }
  }
  return {
    id,
    category,
    path: filePath,
    requiredForApproval,
    present,
    sha256: present && fs.statSync(filePath).isFile() ? sha256File(filePath) : '',
    summary,
  }
}

function collectReportIndex({ workspaceRoot, releaseRoot, readinessReport, evidenceIntakePath }) {
  const records = [
    fileRecord({
      id: 'release_index',
      category: 'release_publication',
      filePath: path.join(releaseRoot, 'echo-release.json'),
    }),
    fileRecord({
      id: 'release_readiness_report',
      category: 'readiness',
      filePath: releaseFile(releaseRoot, 'openlands-release-readiness-report.json'),
    }),
    fileRecord({
      id: 'release_readiness_by_phase',
      category: 'readiness',
      filePath: readinessReport.phaseReadiness?.markdownPath ?? releaseFile(releaseRoot, 'openlands-release-readiness-report-by-phase.md'),
    }),
    fileRecord({
      id: 'public_alpha_evidence_intake',
      category: 'approval_handoff',
      filePath: evidenceIntakePath,
    }),
    fileRecord({
      id: 'publication_url_map_template',
      category: 'release_publication',
      filePath: releaseFile(releaseRoot, 'openlands-publication-url-map.template.json'),
    }),
    fileRecord({
      id: 'publication_manifest_template',
      category: 'release_publication',
      filePath: releaseFile(releaseRoot, 'openlands-release-publication-manifest.template.json'),
    }),
    fileRecord({
      id: 'publication_manifest_verified',
      category: 'release_publication',
      filePath: releaseFile(releaseRoot, 'openlands-release-publication-manifest.verified.json'),
    }),
    fileRecord({
      id: 'publication_manifest_approved',
      category: 'release_publication',
      filePath: releaseFile(releaseRoot, 'openlands-release-publication-manifest.json'),
    }),
    fileRecord({
      id: 'publication_approval_template',
      category: 'release_publication',
      filePath: releaseFile(releaseRoot, 'openlands-release-publication-approval.template.json'),
    }),
  ]

  for (const edition of EDITIONS) {
    for (const [reportId, relativePath] of Object.entries(edition.reports)) {
      records.push(fileRecord({
        id: `${edition.id}_${reportId}`,
        category: `edition_${reportId}`,
        filePath: path.join(workspaceRoot, edition.repo, relativePath),
      }))
    }
  }
  return records
}

function allEditionReportsPassed(records, reportId, readinessField) {
  return EDITIONS.every((edition) => {
    const record = records.find((entry) => entry.id === `${edition.id}_${reportId}`)
    return record?.summary?.status === 'passed' && record.summary?.[readinessField] === true
  })
}

function buildDependencySummary({ reportIndex, readinessReport, releasePublicationApproved, externalEvidenceRequirements }) {
  return {
    schema: 'echo.openlands.public_alpha_dependency_gate_summary_template.v1',
    templateOnly: true,
    status: 'blocked_pending_external_evidence',
    activeBlockerCount: (readinessReport.blockers ?? []).length,
    externalEvidenceRequirementCount: externalEvidenceRequirements.length,
    externalEvidenceRequirementIds: externalEvidenceRequirements.map((requirement) => requirement.blockerId),
    runtimeExecutionReportsPassed: allEditionReportsPassed(reportIndex, 'runtimeExecution', 'publicAlphaReady'),
    launcherExecutionReportsPassed: allEditionReportsPassed(reportIndex, 'launcherExecution', 'publicAlphaReady'),
    finalReleaseReviewReportsPassed: allEditionReportsPassed(reportIndex, 'finalReview', 'publicReleaseReady'),
    releaseReadinessReportHasNoBlockers: readinessReport.publicAlphaReady === true && (readinessReport.blockers ?? []).length === 0,
    approvedReleaseIndexState: releasePublicationApproved === true,
    notes: [
      'This template records current dependency state only.',
      'The passed distribution approval artifact must be regenerated after real runtime, launcher, final review, publication, and readiness evidence is complete.',
    ],
  }
}

function collectRequiredEvidenceIds(distributionApprovalContract) {
  return [...new Set((distributionApprovalContract.distributionGates ?? [])
    .flatMap((gate) => gate.clearsEvidence ?? []))]
    .sort()
}

function buildExternalEvidenceRequirements({ evidenceIntake, readinessReport }) {
  const readinessBlockers = new Set(readinessReport.blockers ?? [])
  return (evidenceIntake?.intakeItems ?? [])
    .filter((item) => item.active === true || readinessBlockers.has(item.blockerId))
    .map((item) => ({
      blockerId: item.blockerId,
      displayName: item.displayName,
      status: item.status ?? 'blocked_pending_external_evidence',
      ownerHint: item.ownerHint ?? '',
      impactedPhases: (item.impactedPhases ?? []).map((phase) => ({
        id: phase.id,
        order: phase.order,
        displayName: phase.displayName,
      })),
      clearsChecks: item.clearsChecks ?? [],
      proofRequired: item.proofRequired ?? [],
      evidenceTargets: (item.evidenceTargets ?? []).map((target) => ({
        id: target.id,
        label: target.label,
        path: target.path,
        required: target.required === true,
        present: target.present === true,
        status: target.summary?.status ?? null,
        templateOnly: target.summary?.templateOnly ?? null,
      })),
      validationCommands: item.validationCommands ?? [],
    }))
}

function renderApprovalMemoTemplate({ readinessReport, readinessHash, phaseHash, launchRoadmapHash, productionMatrixHash, evidenceIds }) {
  const blockers = readinessReport.blockers ?? []
  const lines = [
    '# Openlands Public Alpha Approval',
    '',
    'Template only. Replace this draft with the final signed approval memo after the readiness report is blocker-free.',
    '',
    `Release ID: ${readinessReport.releaseId ?? 'missing'}`,
    `Module: ${MODULE_ID} ${VERSION}`,
    `Current Public Alpha ready: ${readinessReport.publicAlphaReady === true}`,
    `Current blockers: ${blockers.length === 0 ? 'none' : blockers.join(', ')}`,
    '',
    'Required final memo content:',
    '- Openlands Public Alpha approval decision.',
    '- Approver name and approval date from the final distribution approval report.',
    '- Current readiness report hash and phase readiness hash.',
    '- Launch roadmap and production phase matrix scope confirmation.',
    '- Confirmation that Hardlands remains optional.',
    '- Distribution evidence IDs listed below.',
    '',
    'Hashes to carry into the final approval memo:',
    `- readiness: ${readinessHash}`,
    `- phase readiness: ${phaseHash}`,
    `- launch roadmap: ${launchRoadmapHash}`,
    `- production phase matrix: ${productionMatrixHash}`,
    '',
    'Distribution evidence IDs:',
    ...evidenceIds.map((id) => `- ${id}`),
    '',
    'Final approval cannot be signed while this template reports active blockers.',
  ]
  return `${lines.join('\n')}\n`
}

function renderRollbackPlanTemplate({ releaseIndexPath, releaseIndexHash, artifactTargets }) {
  const lines = [
    '# Openlands Public Alpha Rollback Plan Snapshot',
    '',
    'Template only. Replace this draft with the final rollback plan snapshot after Release Index approval and distribution signoff.',
    '',
    'Required final rollback coverage:',
    '- launcher rollback from current manifest to previous accepted manifest',
    '- manifest rollback for Native, NeoForge, and Standalone edition pack listings',
    '- Release Index rollback for every Openlands artifact download URL',
    '- download cache rollback and redownload verification after rollback',
    '- world and config preservation checks for launcher rollback',
    '- approval contact, rollback owner, and rollback verification timestamp',
    '',
    `Current Release Index: ${releaseIndexPath}`,
    `Current Release Index hash: ${releaseIndexHash}`,
    '',
    'Openlands artifact rollback targets:',
  ]
  for (const target of artifactTargets) {
    lines.push(`- ${target.file}`)
  }
  lines.push('')
  lines.push('The final rollback plan must remove this template-only language before it is attached to a passed distribution approval report.')
  return `${lines.join('\n')}\n`
}

function renderPacketMarkdown(packet) {
  const lines = [
    '# Openlands Public Alpha Approval Packet Template',
    '',
    `Status: ${packet.status}`,
    `Public Alpha ready: ${packet.publicAlphaReady}`,
    `Template only: ${packet.templateOnly}`,
    `Readiness blockers: ${packet.blockers.length === 0 ? 'none' : packet.blockers.join(', ')}`,
    '',
    '## Generated Drafts',
    '',
  ]
  for (const generatedFile of packet.generatedFiles) {
    lines.push(`- ${generatedFile.label}: ${generatedFile.path}`)
  }
  lines.push('')
  lines.push('## Approval Inputs')
  lines.push('')
  for (const record of packet.approvalInputReportIndex.entries) {
    const state = record.present ? 'present' : 'missing'
    const status = record.summary?.status ? `, status=${record.summary.status}` : ''
    lines.push(`- ${record.id}: ${record.path} (${state}${status})`)
  }
  lines.push('')
  lines.push('## Blocking State')
  lines.push('')
  for (const blocker of packet.blockers) lines.push(`- ${blocker}`)
  lines.push('')
  lines.push('## External Evidence Requirements')
  lines.push('')
  for (const requirement of packet.externalEvidenceRequirements ?? []) {
    const phases = (requirement.impactedPhases ?? []).map((phase) => `${phase.order}. ${phase.displayName}`).join(', ')
    lines.push(`### ${requirement.displayName}`)
    lines.push('')
    lines.push(`Blocker: ${requirement.blockerId}`)
    lines.push(`Owner hint: ${requirement.ownerHint || 'unassigned'}`)
    lines.push(`Impacted phases: ${phases || 'none'}`)
    lines.push('')
    lines.push('Required proof:')
    for (const proof of requirement.proofRequired ?? []) lines.push(`- ${proof}`)
    lines.push('')
    lines.push('Evidence targets:')
    for (const target of requirement.evidenceTargets ?? []) {
      const state = target.present ? 'present' : 'missing'
      const status = target.status ? `, status=${target.status}` : ''
      lines.push(`- ${target.label}: ${target.path} (${state}${status})`)
    }
    lines.push('')
    lines.push('Validation commands:')
    for (const command of requirement.validationCommands ?? []) lines.push(`- ${command}`)
    lines.push('')
  }
  return `${lines.join('\n').trimEnd()}\n`
}

function generatedFile(pathRoot, filename, label, content, purpose) {
  return {
    filename,
    label,
    path: path.join(pathRoot, filename),
    contentSha256: sha256Text(content),
    purpose,
    content,
  }
}

export function buildApprovalPacketTemplate({ moduleRoot, workspaceRoot, releaseRoot, readinessReportPath, evidenceIntakePath, outputPath, packetRoot, dryRun }) {
  if (!fileExists(readinessReportPath)) throw new Error(`readiness report not found: ${readinessReportPath}`)
  const readinessReport = readJson(readinessReportPath)
  const evidenceIntake = readJsonIfPresent(evidenceIntakePath)
  const releasePublicationContract = readJson(dataPath(moduleRoot, RELEASE_PUBLICATION_CONTRACT))
  const distributionApprovalContract = readJson(dataPath(moduleRoot, DISTRIBUTION_APPROVAL_CONTRACT))
  const launchRoadmapPath = dataPath(moduleRoot, LAUNCH_ROADMAP)
  const productionMatrixPath = dataPath(moduleRoot, PRODUCTION_PHASE_MATRIX)
  const phaseReadinessPath = readinessReport.phaseReadiness?.markdownPath ?? releaseFile(releaseRoot, 'openlands-release-readiness-report-by-phase.md')
  const releaseIndexPath = path.join(releaseRoot, 'echo-release.json')
  const reportIndex = collectReportIndex({ workspaceRoot, releaseRoot, readinessReport, evidenceIntakePath })
  const releasePublicationApproved = readinessReport.releasePublication?.approved === true
  const evidenceIds = collectRequiredEvidenceIds(distributionApprovalContract)
  const externalEvidenceRequirements = buildExternalEvidenceRequirements({
    evidenceIntake,
    readinessReport,
  })
  const dependencySummary = buildDependencySummary({
    reportIndex,
    readinessReport,
    releasePublicationApproved,
    externalEvidenceRequirements,
  })
  const approvalInputReportIndex = {
    schema: 'echo.openlands.approval_input_report_index_template.v1',
    templateOnly: true,
    status: readinessReport.publicAlphaReady === true ? 'ready_for_final_review' : 'blocked_pending_external_evidence',
    evidenceIntakePath,
    evidenceIntakeSha256: fileExists(evidenceIntakePath) ? sha256File(evidenceIntakePath) : '',
    blockerCount: (readinessReport.blockers ?? []).length,
    blockers: readinessReport.blockers ?? [],
    externalEvidenceRequirements,
    entries: reportIndex,
  }
  const releaseReadinessHashText = `${sha256File(readinessReportPath)}  ${readinessReportPath}\n`
  const approvalMemo = renderApprovalMemoTemplate({
    readinessReport,
    readinessHash: sha256File(readinessReportPath),
    phaseHash: fileExists(phaseReadinessPath) ? sha256File(phaseReadinessPath) : '',
    launchRoadmapHash: sha256File(launchRoadmapPath),
    productionMatrixHash: sha256File(productionMatrixPath),
    evidenceIds,
  })
  const rollbackPlan = renderRollbackPlanTemplate({
    releaseIndexPath,
    releaseIndexHash: fileExists(releaseIndexPath) ? sha256File(releaseIndexPath) : '',
    artifactTargets: releasePublicationContract.artifactTargets ?? [],
  })
  const approvedReadinessCopy = `${JSON.stringify(readinessReport, null, 2)}\n`
  const approvedPhaseReadinessCopy = fileExists(phaseReadinessPath) ? readText(phaseReadinessPath) : ''

  const files = [
    generatedFile(packetRoot, 'approval-input-report-index.template.json', 'Approval input report index draft', `${JSON.stringify(approvalInputReportIndex, null, 2)}\n`, 'Draft for dependency approval report hashes and paths.'),
    generatedFile(packetRoot, 'dependency-gate-summary.template.json', 'Dependency gate summary draft', `${JSON.stringify(dependencySummary, null, 2)}\n`, 'Draft summary of runtime, launcher, final review, readiness, and Release Index dependency state.'),
    generatedFile(packetRoot, 'release-readiness-hash.template.txt', 'Release readiness hash draft', releaseReadinessHashText, 'Draft release-readiness-hash.txt content for final distribution approval.'),
    generatedFile(packetRoot, 'public-alpha-approval.template.md', 'Public Alpha approval memo draft', approvalMemo, 'Draft public-alpha-approval.md content for the final approver.'),
    generatedFile(packetRoot, 'rollback-plan-snapshot.template.md', 'Rollback plan snapshot draft', rollbackPlan, 'Draft rollback-plan-snapshot.md content for the final approver.'),
    generatedFile(packetRoot, 'approved-readiness-report.template.json', 'Approved readiness report draft copy', approvedReadinessCopy, 'Draft copy location for approved-readiness-report.json. Must be regenerated after blockers clear.'),
    generatedFile(packetRoot, 'approved-readiness-report-by-phase.template.md', 'Approved phase readiness draft copy', approvedPhaseReadinessCopy, 'Draft copy location for approved-readiness-report-by-phase.md. Must be regenerated after blockers clear.'),
  ]

  const packet = {
    schema: SCHEMA,
    generatedAt: new Date().toISOString(),
    dryRun,
    templateOnly: true,
    status: readinessReport.publicAlphaReady === true ? 'ready_for_final_approval_packet_review' : 'blocked_pending_external_evidence',
    moduleId: MODULE_ID,
    moduleVersion: VERSION,
    releaseId: readinessReport.releaseId ?? null,
    publicAlphaReady: readinessReport.publicAlphaReady === true,
    blockerCount: (readinessReport.blockers ?? []).length,
    blockers: readinessReport.blockers ?? [],
    readinessReportPath,
    readinessReportSha256: sha256File(readinessReportPath),
    phaseReadinessPath,
    phaseReadinessSha256: fileExists(phaseReadinessPath) ? sha256File(phaseReadinessPath) : '',
    evidenceIntakePath,
    evidenceIntakePresent: evidenceIntake !== null,
    evidenceIntakeSha256: fileExists(evidenceIntakePath) ? sha256File(evidenceIntakePath) : '',
    releaseIndexPath,
    releaseIndexSha256: fileExists(releaseIndexPath) ? sha256File(releaseIndexPath) : '',
    launchRoadmapPath,
    launchRoadmapSha256: sha256File(launchRoadmapPath),
    productionMatrixPath,
    productionMatrixSha256: sha256File(productionMatrixPath),
    requiredSavedArtifacts: (distributionApprovalContract.approvalAreas ?? [])
      .flatMap((area) => (area.requiredSavedArtifacts ?? []).map((artifact) => ({
        approvalArea: area.id,
        artifact,
      }))),
    requiredDistributionEvidenceIds: evidenceIds,
    externalEvidenceRequirements,
    dependencyGateSummaryDraft: dependencySummary,
    approvalInputReportIndex,
    releasePublication: {
      manifestStatus: readinessReport.releasePublication?.manifestStatus ?? 'missing',
      manifestSource: readinessReport.releasePublication?.manifestSource ?? 'missing',
      missingDownloadUrlCount: readinessReport.releasePublication?.missingDownloadUrlCount ?? null,
      downloadVerifiedCount: readinessReport.releasePublication?.downloadVerifiedCount ?? null,
      approved: releasePublicationApproved,
    },
    generatedFiles: files.map(({ content, ...file }) => file),
    packetRoot,
    outputPath,
    markdownPath: outputPath.replace(/\.json$/i, '.md'),
    nextSteps: [
      'Do not attach these template files to a passed distribution approval report.',
      'Regenerate this packet after public downloads verify, runtime and launcher reports pass, final review passes, and readiness has no blockers.',
      'Copy the generated drafts into each edition distribution approval saved-artifact root only after replacing template-only text with final approval evidence.',
    ],
  }

  const packetMarkdown = renderPacketMarkdown(packet)
  return {
    packet,
    files: [
      generatedFile(path.dirname(outputPath), path.basename(packet.markdownPath), 'Approval packet Markdown summary', packetMarkdown, 'Readable summary of the approval packet template.'),
      ...files,
    ],
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
  const outputPath = args.output ? path.resolve(args.output) : defaultOutputPath(releaseRoot)
  const packetRoot = args.packetRoot ? path.resolve(args.packetRoot) : defaultPacketRoot(outputPath)
  const readinessReportPath = args.readinessReport
    ? path.resolve(args.readinessReport)
    : releaseFile(releaseRoot, 'openlands-release-readiness-report.json')
  const evidenceIntakePath = args.evidenceIntake
    ? path.resolve(args.evidenceIntake)
    : releaseFile(releaseRoot, 'openlands-public-alpha-evidence-intake.json')
  const result = buildApprovalPacketTemplate({
    moduleRoot,
    workspaceRoot,
    releaseRoot,
    readinessReportPath,
    evidenceIntakePath,
    outputPath,
    packetRoot,
    dryRun: args.dryRun,
  })
  if (!args.dryRun) {
    writeJson(outputPath, result.packet)
    for (const file of result.files) writeText(file.path, file.content)
  }
  if (args.json) {
    console.log(JSON.stringify(result.packet, null, 2))
  } else {
    const action = args.dryRun ? 'generated dry-run' : `wrote ${outputPath}`
    console.log(`Openlands Public Alpha approval packet template ${action}: generatedFiles=${result.files.length}, blockers=${result.packet.blockerCount}.`)
  }
  return result.packet
}

function printHelp() {
  console.log(`Usage: node generate-openlands-public-alpha-approval-packet-template.mjs [options]

Options:
  --module-root <path>       Openlands module root. Defaults to this script's module.
  --workspace-root <path>    Workspace containing edition repos. Defaults to C:/Development/Github relative to module root.
  --release-root <path>      Release output root. Defaults to ECHO-Modules/dist/echo-module-release.
  --readiness-report <path>  Readiness report path. Defaults to openlands-release-readiness-report.json.
  --evidence-intake <path>   Evidence intake path. Defaults to openlands-public-alpha-evidence-intake.json.
  --out <path>               Packet JSON output. Defaults to openlands-public-alpha-approval-packet.template.json.
  --packet-root <path>       Directory for generated draft artifacts.
  --dry-run                  Generate without writing.
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

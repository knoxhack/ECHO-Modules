import { promises as fs } from 'node:fs'
import path from 'node:path'

const PLAY_AUDIT_SCHEMA = 'echo.module.runtime_play_audit.v1'
const EVIDENCE_MANIFEST_SCHEMA = 'echo.module.runtime_play_evidence_manifest.v1'
const MANUAL_MATRIX_SCHEMA = 'echo.module.manual_acceptance_matrix.v1'
const MODULE_COMPLETION_SCHEMA = 'echo.module.play_completion.v1'
const PLAY_BACKLOG_SCHEMA = 'echo.module.runtime_play_fix_backlog.v1'

const REQUIRED_PACK_CHECKS = [
  'installLaunchSucceeds',
  'freshSessionStarts',
  'hudAppears',
  'inventoryOverlayAndIndexRespond',
  'terminalExecutesAction',
  'holoMapOpens',
  'lensScans',
  'screenCoreScreensRenderAndHandleInput',
  'blockPlaceUseBreakWorks',
  'blockActionMutatesState',
  'worldgenAppears',
  'saveReloadPreservesState',
  'trustedMutationsReported',
]

const RUNTIME_EVIDENCE = {
  neoforge: [
    {
      key: 'neoforgePlayEvidence',
      ownerRepo: 'ECHO-Modules',
      path: 'reports/runtime-parity/neoforge-play-evidence.json',
      requiredFor: ['lifecycle', 'content', 'ui', 'actions', 'blockItems', 'worldgen', 'saveNetwork'],
    },
    {
      key: 'neoforgeGameTestResults',
      ownerRepo: 'ECHO-Modules',
      path: 'reports/runtime-parity/neoforge-module-gametest-results.json',
      requiredFor: ['lifecycle', 'actions', 'blockItems', 'saveNetwork'],
    },
    {
      key: 'neoforgeRegistryContentResults',
      ownerRepo: 'ECHO-Modules',
      path: 'reports/runtime-parity/neoforge-registry-content-results.json',
      requiredFor: ['content', 'blockItems', 'worldgen'],
    },
    {
      key: 'neoforgeClientUiResults',
      ownerRepo: 'ECHO-Modules',
      path: 'reports/runtime-parity/neoforge-client-ui-results.json',
      requiredFor: ['ui'],
    },
  ],
  echo_native: [
    {
      key: 'nativeFullCatalogPlay',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/native-full-catalog-play/native-full-catalog-play.json',
      requiredFor: ['lifecycle', 'content'],
    },
    {
      key: 'nativeAllBridgeableArtifactLoadState',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/native-all-bridgeable-module-artifact-load-state/native-all-bridgeable-module-artifact-load-state.json',
      requiredFor: ['lifecycle'],
    },
    {
      key: 'nativeUiSurfaces',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/native-ui-surfaces/native-ui-surfaces.json',
      requiredFor: ['ui'],
    },
    {
      key: 'nativeAgent5UiBridgeContract',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/agent5/ui-bridge-contract/agent5-ui-bridge-contract.json',
      requiredFor: ['ui', 'saveNetwork'],
    },
    {
      key: 'nativeRegistryContent',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/native-registry-content/native-registry-content.json',
      requiredFor: ['content', 'blockItems', 'worldgen'],
    },
    {
      key: 'nativeAgent4RegistryContent',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/agent4/registry-content/native-agent4-registry-content-state.json',
      requiredFor: ['content', 'blockItems', 'worldgen'],
    },
    {
      key: 'nativeBlockActions',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/native-block-actions/native-block-actions.json',
      requiredFor: ['actions', 'blockItems'],
    },
    {
      key: 'nativeAgent4WorldStartup',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/agent4/world-startup/native-agent4-world-startup.json',
      requiredFor: ['content', 'worldgen'],
    },
    {
      key: 'nativeAgent9MachineRuntimeHost',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/agent9/machine-runtime-host/agent9-machine-runtime-host.json',
      requiredFor: ['actions', 'blockItems', 'saveNetwork'],
    },
    {
      key: 'nativeMutationTruthGate',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/mutation-truth-gate/native-mutation-truth-gate.json',
      requiredFor: ['actions', 'saveNetwork'],
    },
    {
      key: 'nativeSaveNetwork',
      ownerRepo: 'ECHO-Native-Platform',
      path: 'build/native-save-network/native-save-network.json',
      requiredFor: ['saveNetwork'],
    },
  ],
  standalone: [
    {
      key: 'standaloneFullCatalogPlay',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/full-catalog-play.json',
      requiredFor: ['lifecycle', 'content'],
    },
    {
      key: 'standaloneRuntimeModuleStatus',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/runtime-module-status.json',
      requiredFor: ['lifecycle'],
    },
    {
      key: 'standaloneClientUiSurfacesPlay',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/client-ui-surfaces-play.json',
      requiredFor: ['ui'],
    },
    {
      key: 'standaloneAgent5UiParitySmoke',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/agent5-ui-parity-smoke.json',
      requiredFor: ['ui', 'saveNetwork'],
    },
    {
      key: 'standaloneClientScreenCatalogSmoke',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/client-screen-catalog-smoke.json',
      requiredFor: ['ui'],
    },
    {
      key: 'standaloneVoxelContentPlay',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/voxel-content-play.json',
      requiredFor: ['content', 'blockItems'],
    },
    {
      key: 'standaloneClientModsRuntimeContentSmoke',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/client-mods-runtime-content-smoke.json',
      requiredFor: ['content'],
    },
    {
      key: 'standaloneBlockActionMutations',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/block-action-mutations.json',
      requiredFor: ['actions', 'blockItems'],
    },
    {
      key: 'standaloneClientWorldInteractionSmoke',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/client-world-interaction-smoke.json',
      requiredFor: ['actions', 'blockItems', 'worldgen'],
    },
    {
      key: 'standaloneClientHeldItemOverlaySmoke',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/client-held-item-overlay-smoke.json',
      requiredFor: ['ui', 'blockItems'],
    },
    {
      key: 'standaloneWorldgenPlay',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/worldgen-play.json',
      requiredFor: ['worldgen'],
    },
    {
      key: 'standaloneSaveReloadPlay',
      ownerRepo: 'ECHO-Standalone-Runtime',
      path: 'reports/echo/standalone/save-reload-play.json',
      requiredFor: ['saveNetwork'],
    },
  ],
}

const RUNTIME_REPO_NAMES = {
  neoforge: 'ECHO-Modules',
  echo_native: 'ECHO-Native-Platform',
  standalone: 'ECHO-Standalone-Runtime',
}

export async function generateRuntimePlayAudit({
  parityReport,
  repoRoot,
  echoRoot,
  outDir,
}) {
  const normalizedRoot = path.resolve(repoRoot)
  const normalizedEchoRoot = path.resolve(echoRoot)
  const normalizedOutDir = path.resolve(normalizedRoot, outDir)
  const generatedAt = parityReport.generatedAt || new Date().toISOString()
  const evidenceManifest = await evidenceManifestFor({
    generatedAt,
    repoRoot: normalizedRoot,
    echoRoot: normalizedEchoRoot,
    parityReport,
  })
  const manualAcceptanceMatrix = await manualAcceptanceMatrixFor({
    generatedAt,
    echoRoot: normalizedEchoRoot,
    packAudit: parityReport.packAudit,
  })
  const playRows = parityReport.rows.map((row) => playRowFor({
    row,
    evidenceManifest,
    manualAcceptanceMatrix,
  }))
  const modulePlayCompletion = modulePlayCompletionFor({
    generatedAt,
    parityReport,
    playRows,
  })
  const playFixBacklog = playFixBacklogFor({
    generatedAt,
    playRows,
    evidenceManifest,
    manualAcceptanceMatrix,
  })
  const playAudit = {
    schema: PLAY_AUDIT_SCHEMA,
    generatedAt,
    repoRoot: normalizePath(normalizedRoot),
    echoRoot: normalizePath(normalizedEchoRoot),
    successStandard: 'Actual player-facing runtime proof. Static source, descriptor metadata, class-loads, and contract-only evidence are not enough.',
    strictFullSourceReport: 'reports/runtime-parity/echo-module-runtime-parity-audit.json',
    evidenceManifestPath: 'reports/runtime-parity/evidence-manifest.json',
    manualAcceptanceMatrixPath: 'reports/runtime-parity/manual-acceptance-matrix.json',
    modulePlayCompletionPath: 'reports/runtime-parity/module-play-completion.json',
    playFixBacklogPath: 'reports/runtime-parity/echo-module-runtime-play-fix-backlog.json',
    summary: playSummaryFor(playRows, manualAcceptanceMatrix),
    rows: playRows,
  }
  playAudit.strictPlayWouldFail = playAudit.summary.resultCounts.fail > 0
    || playAudit.summary.resultCounts.partial > 0
    || manualAcceptanceMatrix.summary.resultCounts.pass !== manualAcceptanceMatrix.rows.length

  await fs.mkdir(normalizedOutDir, { recursive: true })
  const paths = {
    playAuditJson: path.join(normalizedOutDir, 'echo-module-runtime-play-audit.json'),
    playAuditMarkdown: path.join(normalizedOutDir, 'echo-module-runtime-play-audit.md'),
    evidenceManifest: path.join(normalizedOutDir, 'evidence-manifest.json'),
    manualAcceptanceMatrix: path.join(normalizedOutDir, 'manual-acceptance-matrix.json'),
    modulePlayCompletion: path.join(normalizedOutDir, 'module-play-completion.json'),
    playFixBacklogJson: path.join(normalizedOutDir, 'echo-module-runtime-play-fix-backlog.json'),
    playFixBacklogMarkdown: path.join(normalizedOutDir, 'echo-module-runtime-play-fix-backlog.md'),
  }
  await fs.writeFile(paths.playAuditJson, `${JSON.stringify(playAudit, null, 2)}\n`, 'utf8')
  await fs.writeFile(paths.playAuditMarkdown, markdownPlayAudit(playAudit, evidenceManifest, manualAcceptanceMatrix), 'utf8')
  await fs.writeFile(paths.evidenceManifest, `${JSON.stringify(evidenceManifest, null, 2)}\n`, 'utf8')
  await fs.writeFile(paths.manualAcceptanceMatrix, `${JSON.stringify(manualAcceptanceMatrix, null, 2)}\n`, 'utf8')
  await fs.writeFile(paths.modulePlayCompletion, `${JSON.stringify(modulePlayCompletion, null, 2)}\n`, 'utf8')
  await fs.writeFile(paths.playFixBacklogJson, `${JSON.stringify(playFixBacklog, null, 2)}\n`, 'utf8')
  await fs.writeFile(paths.playFixBacklogMarkdown, markdownPlayFixBacklog(playFixBacklog), 'utf8')

  return {
    playAudit,
    evidenceManifest,
    manualAcceptanceMatrix,
    modulePlayCompletion,
    playFixBacklog,
    paths,
  }
}

async function evidenceManifestFor({ generatedAt, repoRoot, echoRoot, parityReport }) {
  const runtimeEntries = []
  for (const [runtime, definitions] of Object.entries(RUNTIME_EVIDENCE)) {
    const runtimeRoot = runtime === 'neoforge'
      ? repoRoot
      : path.join(echoRoot, RUNTIME_REPO_NAMES[runtime])
    for (const definition of definitions) {
      const absolute = path.join(runtimeRoot, definition.path)
      const report = await readJsonIfExists(absolute)
      const moduleIds = moduleIdsFromReport(report)
      runtimeEntries.push({
        ...definition,
        runtime,
        absolutePath: normalizePath(absolute),
        found: !!report,
        status: reportStatus(report),
        schema: string(report?.schema),
        moduleIds,
        moduleCount: moduleIds.length,
        allModules: reportCoversAllModules(report, parityReport.modules.length),
        blockers: array(report?.blockers).filter((item) => typeof item === 'string'),
        sourceReports: array(report?.sourceReports),
        parseError: string(report?.parseError),
      })
    }
  }
  return {
    schema: EVIDENCE_MANIFEST_SCHEMA,
    generatedAt,
    repoRoot: normalizePath(repoRoot),
    echoRoot: normalizePath(echoRoot),
    runtimeEvidence: runtimeEntries,
    summary: {
      expectedRuntimeEvidenceCount: runtimeEntries.length,
      foundRuntimeEvidenceCount: runtimeEntries.filter((entry) => entry.found).length,
      passingRuntimeEvidenceCount: runtimeEntries.filter((entry) => entry.status === 'PASS').length,
      missingRuntimeEvidenceCount: runtimeEntries.filter((entry) => !entry.found).length,
    },
  }
}

async function manualAcceptanceMatrixFor({ generatedAt, echoRoot, packAudit }) {
  const manifests = packAudit?.preferredManifests ?? []
  const rows = []
  for (const manifest of manifests) {
    const lane = laneToRuntime(manifest.lane)
    const packRoot = path.join(echoRoot, manifest.repo)
    const expectedPath = path.join(packRoot, 'reports', 'pack-acceptance', `${slug(manifest.product)}-${manifest.lane.toLowerCase()}-acceptance.json`)
    const fallbackPath = path.join(packRoot, 'reports', 'pack-acceptance', `${manifest.repo}-acceptance.json`)
    const reportPath = await exists(expectedPath) ? expectedPath : fallbackPath
    const report = await readJsonIfExists(reportPath)
    const checks = {}
    const checkDetails = {}
    for (const check of REQUIRED_PACK_CHECKS) {
      const checkValue = report?.checks?.[check] ?? report?.checkDetails?.[check] ?? report?.[check]
      checks[check] = checkPassed(checkValue)
      checkDetails[check] = checkDetail(checkValue)
    }
    const blockers = []
    if (!report) {
      blockers.push(`missing pack acceptance report: ${normalizePath(reportPath)}`)
    } else if (reportStatus(report) !== 'PASS') {
      blockers.push(`pack acceptance report status is ${reportStatus(report) || 'unknown'}`)
    }
    for (const check of REQUIRED_PACK_CHECKS) {
      if (!checks[check]) blockers.push(`manual acceptance check not proven: ${check}`)
    }
    rows.push({
      product: manifest.product,
      lane: manifest.lane,
      runtime: lane,
      repo: manifest.repo,
      manifestPath: manifest.manifestPath,
      moduleCount: manifest.moduleCount,
      reportPath: normalizePath(reportPath),
      found: !!report,
      status: blockers.length === 0 ? 'PASS' : 'FAIL',
      checks,
      checkDetails,
      blockers,
    })
  }
  const counts = countBy(rows, (row) => row.status)
  return {
    schema: MANUAL_MATRIX_SCHEMA,
    generatedAt,
    rows,
    requiredChecks: REQUIRED_PACK_CHECKS,
    summary: {
      packLaneCount: rows.length,
      resultCounts: {
        pass: counts.PASS ?? 0,
        fail: counts.FAIL ?? 0,
      },
    },
  }
}

function playRowFor({ row, evidenceManifest, manualAcceptanceMatrix }) {
  const blockers = []
  if (row.strictFullBlockers?.length) {
    blockers.push(...row.strictFullBlockers.map((blocker) => `strict-full blocker: ${blocker}`))
  }
  const runtimeEvidence = runtimeEvidenceFor(row.runtime, evidenceManifest)
  const featureNeeds = featureNeedsFor(row.expectedFeatures ?? [])
  for (const need of featureNeeds) {
    const satisfied = runtimeEvidence.some((entry) =>
      entry.status === 'PASS'
        && entry.requiredFor.includes(need)
        && reportCoversModule(entry, row.moduleId))
    if (!satisfied) {
      blockers.push(`missing ${row.runtime} strict-play ${need} evidence for ${row.moduleId}`)
    }
  }
  const packRows = packRowsFor(row, manualAcceptanceMatrix)
  for (const packRow of packRows) {
    if (packRow.status !== 'PASS') {
      blockers.push(`pack acceptance missing or failing for ${packRow.product} ${packRow.lane}`)
    }
  }
  const result = blockers.length === 0 ? 'pass' : (row.strictFullBlockers?.length ? 'fail' : 'partial')
  return {
    moduleId: row.moduleId,
    name: row.name,
    runtime: row.runtime,
    ownerRepo: row.ownerRepo,
    expectedFeatures: row.expectedFeatures,
    packRefs: row.packRefs,
    runtimeEvidence: runtimeEvidence.map((entry) => ({
      key: entry.key,
      path: relativeDisplayPath(entry),
      found: entry.found,
      status: entry.status,
      moduleCovered: reportCoversModule(entry, row.moduleId),
      requiredFor: entry.requiredFor,
    })),
    packAcceptance: packRows.map((packRow) => ({
      product: packRow.product,
      lane: packRow.lane,
      repo: packRow.repo,
      status: packRow.status,
      reportPath: packRow.reportPath,
    })),
    result,
    blockers,
  }
}

function modulePlayCompletionFor({ generatedAt, parityReport, playRows }) {
  const rowsByModule = new Map()
  for (const row of playRows) {
    const values = rowsByModule.get(row.moduleId) ?? []
    values.push(row)
    rowsByModule.set(row.moduleId, values)
  }
  const modules = parityReport.modules.map((module) => {
    const runtimeRows = rowsByModule.get(module.moduleId) ?? []
    const blockers = runtimeRows.flatMap((row) => row.blockers.map((blocker) => `${row.runtime}: ${blocker}`))
    return {
      moduleId: module.moduleId,
      name: module.name,
      expectedFeatures: module.expectedFeatures,
      runtimeResults: Object.fromEntries(runtimeRows.map((row) => [row.runtime, row.result])),
      complete: blockers.length === 0,
      blockers,
    }
  })
  const counts = countBy(modules, (module) => module.complete ? 'complete' : 'incomplete')
  return {
    schema: MODULE_COMPLETION_SCHEMA,
    generatedAt,
    modules,
    summary: {
      moduleCount: modules.length,
      complete: counts.complete ?? 0,
      incomplete: counts.incomplete ?? 0,
    },
  }
}

function playFixBacklogFor({ generatedAt, playRows, evidenceManifest, manualAcceptanceMatrix }) {
  const items = []
  for (const entry of evidenceManifest.runtimeEvidence) {
    if (entry.status === 'PASS') continue
    items.push({
      id: `PLAY-EVIDENCE-${entry.runtime}-${entry.key}`.toUpperCase().replace(/[^A-Z0-9]+/g, '-'),
      priority: 'P0',
      category: 'runtime_evidence',
      ownerRepo: entry.ownerRepo,
      title: `Produce PASS ${entry.runtime} evidence for ${entry.key}`,
      summary: entry.found
        ? `${entry.key} exists but is ${entry.status || 'unknown'}, so strict-play cannot trust it as player-functional proof.`
        : `${entry.key} is missing, so strict-play has no player-functional proof for ${entry.requiredFor.join(', ')}.`,
      runtimes: [entry.runtime],
      requiredFor: entry.requiredFor,
      modules: entry.moduleIds,
      affectedModuleCount: entry.allModules ? 'all' : entry.moduleCount,
      evidencePath: relativeDisplayPath(entry),
      blockers: entryBlockers(entry),
      recommendedFix: recommendedFixForEvidence(entry),
    })
  }

  const coverageGroups = new Map()
  for (const row of playRows) {
    const runtimeEvidence = row.runtimeEvidence ?? []
    for (const need of featureNeedsFor(row.expectedFeatures ?? [])) {
      const satisfied = runtimeEvidence.some((entry) =>
        entry.status === 'PASS'
          && entry.requiredFor.includes(need)
          && entry.moduleCovered)
      if (satisfied) continue
      const key = `${row.runtime}:${need}`
      const group = coverageGroups.get(key) ?? {
        id: `PLAY-COVERAGE-${row.runtime}-${need}`.toUpperCase().replace(/[^A-Z0-9]+/g, '-'),
        priority: 'P0',
        category: 'module_coverage',
        ownerRepo: ownerRepoForRuntime(row.runtime),
        title: `Expand ${row.runtime} ${need} proof to every expected module`,
        summary: '',
        runtimes: [row.runtime],
        requiredFor: [need],
        modules: [],
        affectedModuleCount: 0,
        blockers: [],
        recommendedFix: recommendedFixForCoverage(row.runtime, need),
      }
      group.modules.push(row.moduleId)
      coverageGroups.set(key, group)
    }
  }
  for (const group of coverageGroups.values()) {
    group.modules = unique(group.modules)
    group.affectedModuleCount = group.modules.length
    group.summary = `${group.affectedModuleCount} module(s) still lack PASS ${group.runtimes[0]} ${group.requiredFor[0]} evidence.`
    group.blockers = [group.summary]
    items.push(group)
  }

  for (const packRow of manualAcceptanceMatrix.rows) {
    if (packRow.status === 'PASS') continue
    const missingChecks = REQUIRED_PACK_CHECKS.filter((check) => !packRow.checks?.[check])
    items.push({
      id: `PLAY-PACK-${slug(packRow.product)}-${packRow.lane.toLowerCase()}`.toUpperCase().replace(/[^A-Z0-9]+/g, '-'),
      priority: 'P0',
      category: 'pack_acceptance',
      ownerRepo: packRow.repo,
      title: `Complete ${packRow.product} ${packRow.lane} acceptance evidence`,
      summary: `${packRow.product} ${packRow.lane} has ${missingChecks.length}/${REQUIRED_PACK_CHECKS.length} acceptance check(s) still unproven.`,
      runtimes: [packRow.runtime],
      requiredFor: missingChecks,
      modules: [],
      affectedModuleCount: packRow.moduleCount,
      evidencePath: packRow.reportPath,
      blockers: packRow.blockers,
      recommendedFix: `Run the ${packRow.product} ${packRow.lane} manual acceptance matrix and update ${packRow.reportPath} with PASS checks plus logs/screenshots/save/runtime report evidence.`,
    })
  }

  const sortedItems = items.sort((left, right) =>
    left.priority.localeCompare(right.priority)
      || left.ownerRepo.localeCompare(right.ownerRepo)
      || left.category.localeCompare(right.category)
      || left.id.localeCompare(right.id))
  return {
    schema: PLAY_BACKLOG_SCHEMA,
    generatedAt,
    successStandard: 'Every item represents missing PASS player-functional proof; partial/source/metadata-only evidence remains backlog work.',
    summary: playBacklogSummary(sortedItems),
    items: sortedItems,
  }
}

function playBacklogSummary(items) {
  return {
    itemCount: items.length,
    byPriority: countBy(items, (item) => item.priority),
    byCategory: countBy(items, (item) => item.category),
    byOwnerRepo: countBy(items, (item) => item.ownerRepo),
  }
}

function entryBlockers(entry) {
  if (!entry.found) return [`missing evidence report: ${relativeDisplayPath(entry)}`]
  const blockers = [`evidence report status is ${entry.status || 'unknown'}: ${relativeDisplayPath(entry)}`]
  blockers.push(...array(entry.blockers).filter((item) => typeof item === 'string'))
  if (entry.parseError) blockers.push(`parse error: ${entry.parseError}`)
  return unique(blockers)
}

function recommendedFixForEvidence(entry) {
  if (entry.runtime === 'neoforge') {
    return 'Run or implement executed NeoForge GameTest/live client evidence, then replace PARTIAL source-contract reports with PASS reports that include moduleIds and trusted gameplay mutations.'
  }
  if (entry.runtime === 'echo_native') {
    return 'Expand Native Loader typed host smokes so this report reaches PASS with module-level UI/content/action/world/save evidence, then rerun generateNativeStrictPlayEvidence.'
  }
  return 'Expand Standalone runtime/client smokes so this report reaches PASS with module-level controller, voxel/content, action, worldgen, and save/reload evidence, then rerun generateStandaloneStrictPlayEvidence.'
}

function recommendedFixForCoverage(runtime, need) {
  if (runtime === 'neoforge') {
    return `Produce executed NeoForge ${need} proof with moduleIds for every expected module; source-derived PARTIAL reports do not satisfy strict-play.`
  }
  if (runtime === 'echo_native') {
    return `Expand Native strict-play ${need} evidence to cover every module that declares the related feature bucket.`
  }
  return `Expand Standalone strict-play ${need} evidence to cover every module that declares the related feature bucket.`
}

function ownerRepoForRuntime(runtime) {
  return RUNTIME_REPO_NAMES[runtime] ?? 'ECHO-Modules'
}

function playSummaryFor(rows, manualAcceptanceMatrix) {
  const resultCounts = countBy(rows, (row) => row.result)
  const resultCountsByRuntime = {}
  for (const runtime of ['neoforge', 'echo_native', 'standalone']) {
    resultCountsByRuntime[runtime] = countBy(rows.filter((row) => row.runtime === runtime), (row) => row.result)
  }
  const failingRows = rows.filter((row) => row.result !== 'pass')
  return {
    runtimeRowCount: rows.length,
    resultCounts: {
      pass: resultCounts.pass ?? 0,
      partial: resultCounts.partial ?? 0,
      fail: resultCounts.fail ?? 0,
    },
    resultCountsByRuntime,
    failingRuntimeRowCount: failingRows.length,
    packAcceptance: manualAcceptanceMatrix.summary,
  }
}

function markdownPlayAudit(playAudit, evidenceManifest, manualAcceptanceMatrix) {
  const lines = []
  lines.push('# ECHO Module Runtime Play Audit')
  lines.push('')
  lines.push(`- Generated: ${playAudit.generatedAt}`)
  lines.push(`- Strict-play would fail: ${playAudit.strictPlayWouldFail ? 'YES' : 'no'}`)
  lines.push(`- Passing rows: ${playAudit.summary.resultCounts.pass}`)
  lines.push(`- Partial rows: ${playAudit.summary.resultCounts.partial}`)
  lines.push(`- Failing rows: ${playAudit.summary.resultCounts.fail}`)
  lines.push(`- Pack acceptance pass: ${manualAcceptanceMatrix.summary.resultCounts.pass}/${manualAcceptanceMatrix.summary.packLaneCount}`)
  lines.push('')
  lines.push('## Evidence Manifest')
  lines.push('')
  lines.push('| Runtime | Evidence | Status | Found | Modules | Path |')
  lines.push('| --- | --- | --- | --- | ---: | --- |')
  for (const entry of evidenceManifest.runtimeEvidence) {
    lines.push(`| ${entry.runtime} | ${entry.key} | ${entry.status || 'missing'} | ${entry.found ? 'yes' : 'no'} | ${entry.moduleCount} | \`${entry.path}\` |`)
  }
  lines.push('')
  lines.push('## Pack Acceptance')
  lines.push('')
  lines.push('| Product | Lane | Status | Report | Blockers |')
  lines.push('| --- | --- | --- | --- | --- |')
  for (const row of manualAcceptanceMatrix.rows) {
    lines.push(`| ${row.product} | ${row.lane} | ${row.status} | \`${row.reportPath}\` | ${escapeCell(row.blockers.slice(0, 3).join('; '))} |`)
  }
  lines.push('')
  lines.push('## First Failing Rows')
  lines.push('')
  lines.push('| Module | Runtime | Result | First Blocker |')
  lines.push('| --- | --- | --- | --- |')
  for (const row of playAudit.rows.filter((item) => item.result !== 'pass').slice(0, 80)) {
    lines.push(`| ${row.moduleId} | ${row.runtime} | ${row.result} | ${escapeCell(row.blockers[0] ?? '')} |`)
  }
  lines.push('')
  return `${lines.join('\n')}\n`
}

function markdownPlayFixBacklog(backlog) {
  const lines = []
  lines.push('# ECHO Runtime Play Fix Backlog')
  lines.push('')
  lines.push(`Generated: ${backlog.generatedAt}`)
  lines.push('')
  lines.push(`- Items: ${backlog.summary.itemCount}`)
  lines.push(`- By category: ${inlineObject(backlog.summary.byCategory)}`)
  lines.push(`- By owner: ${inlineObject(backlog.summary.byOwnerRepo)}`)
  lines.push('')
  for (const priority of ['P0', 'P1', 'P2']) {
    const items = backlog.items.filter((item) => item.priority === priority)
    if (items.length === 0) continue
    lines.push(`## ${priority}`)
    lines.push('')
    for (const item of items) {
      lines.push(`### ${item.id} - ${item.title}`)
      lines.push('')
      lines.push(`- Owner: ${item.ownerRepo}`)
      lines.push(`- Category: ${item.category}`)
      if (item.runtimes.length > 0) lines.push(`- Runtime: ${item.runtimes.join(', ')}`)
      if (item.requiredFor.length > 0) lines.push(`- Required proof: ${item.requiredFor.join(', ')}`)
      lines.push(`- Affected modules: ${item.affectedModuleCount}`)
      if (item.evidencePath) lines.push(`- Evidence: \`${item.evidencePath}\``)
      if (item.modules.length > 0) lines.push(`- Modules: ${inlineList(item.modules, 40)}`)
      if (item.blockers.length > 0) lines.push(`- First blocker: ${item.blockers[0]}`)
      lines.push(`- Recommended fix: ${item.recommendedFix}`)
      lines.push('')
    }
  }
  return `${lines.join('\n')}\n`
}

function runtimeEvidenceFor(runtime, evidenceManifest) {
  return evidenceManifest.runtimeEvidence.filter((entry) => entry.runtime === runtime)
}

function featureNeedsFor(features) {
  const needs = new Set(['lifecycle'])
  if (features.length > 0) needs.add('content')
  if (features.some((feature) => ['gui', 'hud', 'screen', 'inventory_overlay', 'terminal', 'index', 'holomap', 'lens', 'audio'].includes(feature))) {
    needs.add('ui')
  }
  if (features.some((feature) => ['block_actions', 'machines', 'missions'].includes(feature))) {
    needs.add('actions')
  }
  if (features.some((feature) => ['blocks', 'items', 'entities', 'machines'].includes(feature))) {
    needs.add('blockItems')
  }
  if (features.includes('worldgen')) needs.add('worldgen')
  if (features.some((feature) => ['save_data', 'networking', 'missions'].includes(feature))) {
    needs.add('saveNetwork')
  }
  return [...needs].sort()
}

function packRowsFor(row, manualAcceptanceMatrix) {
  const runtimeLane = runtimeToLane(row.runtime)
  const refs = row.packRefs ?? []
  return manualAcceptanceMatrix.rows.filter((packRow) =>
    packRow.lane === runtimeLane
      && refs.some((ref) => ref.repo === packRow.repo || (ref.product === packRow.product && ref.lane === packRow.lane)))
}

function reportCoversModule(entry, moduleId) {
  return entry.allModules || entry.moduleIds?.includes(moduleId) || entry.moduleIds?.includes('*')
}

function reportCoversAllModules(report, expectedCount) {
  if (!report || report.parseError) return false
  if (report.allModules === true || report.coversAllModules === true) return true
  const moduleIds = moduleIdsFromReport(report)
  return expectedCount > 0 && moduleIds.length >= expectedCount
}

function moduleIdsFromReport(report) {
  if (!report || report.parseError) return []
  const values = [
    ...array(report.moduleIds),
    ...array(report.modules).map((item) => typeof item === 'string' ? item : item?.moduleId ?? item?.id),
    ...array(report.rows).map((item) => item?.moduleId ?? item?.id),
    ...array(report.results).map((item) => item?.moduleId ?? item?.id),
    ...Object.keys(object(report.runtimeStatuses)),
    ...Object.keys(object(report.lifecycles)),
    ...array(report.passedModuleIds),
    ...array(report.verifiedModuleIds),
    ...array(report.loadedModuleIds),
    ...array(report.lifecycleModuleIds),
  ]
  return unique(values.filter((value) => typeof value === 'string' && value.trim()))
}

function relativeDisplayPath(entry) {
  return `${entry.ownerRepo}/${entry.path}`.replace(/\\/g, '/')
}

async function readJsonIfExists(filePath) {
  if (!(await exists(filePath))) return null
  try {
    const text = await fs.readFile(filePath, 'utf8')
    return JSON.parse(text.charCodeAt(0) === 0xfeff ? text.slice(1) : text)
  } catch (error) {
    return { parseError: error.message }
  }
}

async function exists(filePath) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

function reportStatus(report) {
  if (!report) return ''
  if (report.parseError) return 'PARSE_ERROR'
  if (string(report.schema).includes('core_module_load_state') && Number(report.failedModuleCount ?? 0) === 0) {
    return 'PASS'
  }
  const value = string(report.status ?? report.result ?? report.summary?.status).trim().toUpperCase()
  if (['PASS', 'PASSED', 'SUCCESS', 'OK'].includes(value)) return 'PASS'
  if (['FAIL', 'FAILED', 'ERROR'].includes(value)) return 'FAIL'
  if (['PARTIAL', 'WARN', 'WARNING'].includes(value)) return 'PARTIAL'
  return value
}

function checkPassed(value) {
  if (value === true) return true
  if (typeof value === 'string') return normalizedPassStatus(value) === 'PASS'
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false
  return value.passed === true
    || value.pass === true
    || normalizedPassStatus(value.status ?? value.result ?? value.state) === 'PASS'
}

function checkDetail(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return { status: checkPassed(value) ? 'PASS' : 'MISSING', evidence: [] }
  }
  return {
    status: normalizedPassStatus(value.status ?? value.result ?? value.state) || (checkPassed(value) ? 'PASS' : 'MISSING'),
    evidence: array(value.evidence).filter((item) => typeof item === 'string'),
    notes: string(value.notes),
    owner: string(value.owner),
  }
}

function normalizedPassStatus(value) {
  const normalized = string(value).trim().toUpperCase()
  if (['PASS', 'PASSED', 'SUCCESS', 'OK'].includes(normalized)) return 'PASS'
  if (['FAIL', 'FAILED', 'ERROR'].includes(normalized)) return 'FAIL'
  if (['PENDING', 'TODO', 'NOT_RUN'].includes(normalized)) return 'PENDING'
  if (['PARTIAL', 'WARN', 'WARNING'].includes(normalized)) return 'PARTIAL'
  return normalized
}

function runtimeToLane(runtime) {
  if (runtime === 'echo_native') return 'Native'
  if (runtime === 'standalone') return 'Standalone'
  return 'NeoForge'
}

function laneToRuntime(lane) {
  if (lane === 'Native') return 'echo_native'
  if (lane === 'Standalone') return 'standalone'
  return 'neoforge'
}

function slug(value) {
  return String(value).toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')
}

function array(value) {
  return Array.isArray(value) ? value : []
}

function object(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

function string(value) {
  return typeof value === 'string' ? value : ''
}

function unique(values) {
  return [...new Set(values)].sort()
}

function countBy(values, keyFn) {
  const counts = {}
  for (const value of values) {
    const key = keyFn(value)
    counts[key] = (counts[key] ?? 0) + 1
  }
  return counts
}

function normalizePath(value) {
  return value.replace(/\\/g, '/')
}

function inlineList(values, limit = 18) {
  if (!values || values.length === 0) return ''
  const shown = values.slice(0, limit)
  const suffix = values.length > shown.length ? `, +${values.length - shown.length} more` : ''
  return `${shown.join(', ')}${suffix}`
}

function inlineObject(value) {
  return Object.entries(value ?? {})
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, count]) => `${key}: ${count}`)
    .join(', ')
}

function escapeCell(value) {
  return String(value).replace(/\|/g, '\\|').replace(/\n/g, '<br>')
}

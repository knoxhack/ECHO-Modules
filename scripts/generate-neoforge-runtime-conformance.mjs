#!/usr/bin/env node
import { promises as fs } from 'node:fs'
import crypto from 'node:crypto'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const SCHEMA_VERSION = 'echo.runtime.conformance.v1'
const HOST_ID = 'neoforge'
const GENERATED_AT = new Date().toISOString()
const DEFAULT_OUT = path.join('reports', 'runtime-parity', 'neoforge-runtime-conformance.json')

const SOURCE_REPORTS = {
  runtime: path.join('reports', 'runtime-parity', 'neoforge-runtime-evidence.json'),
  clientUi: path.join('reports', 'runtime-parity', 'neoforge-client-ui-results.json'),
  registryContent: path.join('reports', 'runtime-parity', 'neoforge-registry-content-results.json'),
  gameTests: path.join('reports', 'runtime-parity', 'neoforge-module-gametest-results.json'),
  play: path.join('reports', 'runtime-parity', 'neoforge-play-evidence.json'),
  featureContracts: path.join('reports', 'runtime-parity', 'module-feature-contracts.json'),
  contentGraph: path.join('dist', 'echo-module-release', 'content-graph-evidence.json'),
}

export async function generateNeoForgeRuntimeConformance({
  repoRoot = process.cwd(),
  outPath = DEFAULT_OUT,
} = {}) {
  const root = path.resolve(repoRoot)
  const sources = await readSourceReports(root)
  const moduleGraphFingerprint = await fingerprintSourceReports(root)
  const statuses = {
    runtime: reportPassed(sources.runtime),
    clientUi: reportPassed(sources.clientUi),
    registryContent: reportPassed(sources.registryContent),
    gameTests: reportPassed(sources.gameTests),
    play: reportPassed(sources.play),
    contentGraph: contentGraphUsable(sources.contentGraph),
  }
  const runtimeStatus = statuses.runtime ? 'adapted' : 'blocked'
  const diagnosticStatus = statuses.runtime && statuses.contentGraph ? 'adapted' : 'fallback'
  const uiStatus = statuses.clientUi && statuses.play ? 'adapted' : 'fallback'
  const registryStatus = statuses.registryContent ? 'adapted' : 'fallback'
  const actionStatus = statuses.gameTests ? 'adapted' : 'fallback'
  const saveStatus = gameTestEvidenceCount(sources.gameTests, 'saveEvidence') > 0 ? 'adapted' : actionStatus
  const networkStatus = gameTestEvidenceCount(sources.gameTests, 'networkEvidence') > 0 ? 'adapted' : actionStatus
  const hudEventStatus = statuses.clientUi && statuses.play ? 'adapted' : 'fallback'

  const surfaceResults = [
    surface('echoscreencore:surface/title_menu', 'echoscreencore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoscreencore:surface/pause_menu', 'echoscreencore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoscreencore:surface/world_create', 'echoscreencore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoscreencore:surface/world_load', 'echoscreencore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoscreencore:surface/settings_panel', 'echoscreencore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoscreencore:surface/module_diagnostics', 'echoscreencore', diagnosticStatus, diagnosticEvidence(sources), diagnosticReason(sources)),
    surface('echoscreencore:surface/runtime_blocker', 'echoscreencore', diagnosticStatus, diagnosticEvidence(sources), diagnosticReason(sources)),
    surface('echoscreencore:surface/death_respawn', 'echoscreencore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echohudcore:surface/status_hud', 'echohudcore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echohudcore:surface/hotbar_surface', 'echohudcore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echohudcore:surface/objective_tracker', 'echohudcore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echohudcore:surface/warning_overlay', 'echohudcore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoinputcore:surface/keybind_registry', 'echoinputcore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoindex:surface/index_pages', 'echoindex', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoindex:surface/inventory', 'echoindex', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoindex:surface/crafting', 'echoindex', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoterminal:surface/terminal_shell', 'echoterminal', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echoterminal:surface/diagnostics_page', 'echoterminal', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echomissioncore:surface/mission_tracker', 'echomissioncore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echomissioncore:surface/reward_route', 'echomissioncore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echosessioncore:surface/save_warning', 'echosessioncore', uiStatus, uiEvidence(sources), uiReason(sources)),
    surface('echosessioncore:surface/session_state', 'echosessioncore', diagnosticStatus, diagnosticEvidence(sources), diagnosticReason(sources)),
    surface('echoadaptercore:surface/gameplay_actions', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    surface('echoadaptercore:surface/mutation_ledger', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    surface('echothemecore:surface/theme_tokens', 'echothemecore', uiStatus, uiEvidence(sources), uiReason(sources)),
  ]

  const actionResults = [
    action('echoadaptercore:action/module_runtime_load', 'echoadaptercore', runtimeStatus, runtimeEvidence(sources), runtimeReason(sources)),
    action('echoadaptercore:action/registry_content_load', 'echoadaptercore', registryStatus, registryEvidence(sources), registryReason(sources)),
    action('echoadaptercore:action/inventory_move_stack', 'echoadaptercore', uiStatus, livePlayEvidence(sources), livePlayReason(sources)),
    action('echoadaptercore:action/inventory_use_item', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/craft_recipe', 'echoadaptercore', registryStatus, registryEvidence(sources), registryReason(sources)),
    action('echoadaptercore:action/player_state_update', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/player_respawn', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/world_break_block', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/world_place_block', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/structure_place', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/block_entity_write', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/capability_update', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/event_emit', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/hud_event', 'echoadaptercore', hudEventStatus, uiEvidence(sources), uiReason(sources)),
    action('echoadaptercore:action/packet_event', 'echoadaptercore', networkStatus, networkEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/save_session_write', 'echoadaptercore', saveStatus, saveEvidence(sources), gameTestReason(sources)),
    action('echoadaptercore:action/mutation_receipt_proof', 'echoadaptercore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echoinputcore:action/remap_binding', 'echoinputcore', uiStatus, uiEvidence(sources), uiReason(sources)),
    action('echoterminal:action/safe_action', 'echoterminal', uiStatus, livePlayEvidence(sources), livePlayReason(sources)),
    action('echoindex:action/open_recipe_route', 'echoindex', uiStatus, livePlayEvidence(sources), livePlayReason(sources)),
    action('echomissioncore:action/progress_objective', 'echomissioncore', actionStatus, gameTestEvidence(sources), gameTestReason(sources)),
    action('echosessioncore:action/repair_session', 'echosessioncore', saveStatus, saveEvidence(sources), gameTestReason(sources)),
  ]

  const report = {
    schemaVersion: SCHEMA_VERSION,
    id: 'echo-neoforge:runtime/echo-modules',
    hostId: HOST_ID,
    generatedAt: GENERATED_AT,
    moduleGraphFingerprint,
    surfaceResults,
    actionResults,
    summary: summarize([...surfaceResults, ...actionResults]),
  }

  const output = path.resolve(root, outPath)
  await fs.mkdir(path.dirname(output), { recursive: true })
  await fs.writeFile(output, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  return { report, path: output }
}

async function readSourceReports(root) {
  const entries = {}
  for (const [key, relativePath] of Object.entries(SOURCE_REPORTS)) {
    entries[key] = await readRequiredJson(root, key, relativePath)
  }
  return entries
}

async function readRequiredJson(root, key, relativePath) {
  const absolutePath = path.join(root, relativePath)
  try {
    return JSON.parse(stripBom(await fs.readFile(absolutePath, 'utf8')))
  } catch (error) {
    throw new Error(`Cannot read NeoForge conformance source report ${key} at ${relativePath}: ${error.message}`)
  }
}

async function fingerprintSourceReports(root) {
  const hash = crypto.createHash('sha256')
  for (const relativePath of Object.values(SOURCE_REPORTS).sort()) {
    const absolutePath = path.join(root, relativePath)
    const bytes = await fs.readFile(absolutePath)
    hash.update(relativePath)
    hash.update('\0')
    hash.update(bytes)
    hash.update('\0')
  }
  return `sha256:${hash.digest('hex')}`
}

function reportPassed(report) {
  return normalizeStatus(report?.status) === 'PASS'
}

function contentGraphUsable(report) {
  return report?.schemaVersion === 'echo.content_graph.evidence.v1'
    && ['valid', 'warning'].includes(String(report?.validationState ?? '').toLowerCase())
    && Number(report?.unresolvedReferenceCount ?? 0) === 0
}

function normalizeStatus(status) {
  return String(status ?? '').trim().toUpperCase()
}

function gameTestEvidenceCount(report, field) {
  return Array.isArray(report?.[field]) ? report[field].length : 0
}

function surface(id, ownerModule, status, hostEvidence, reason = '') {
  return withoutEmptyReason({
    id,
    ownerModule,
    status,
    hostEvidence,
    reason,
  })
}

function action(id, ownerModule, status, receiptEvidence, reason = '') {
  return withoutEmptyReason({
    id,
    ownerModule,
    status,
    receiptEvidence,
    reason,
  })
}

function withoutEmptyReason(row) {
  if (!row.reason) delete row.reason
  return row
}

function summarize(rows) {
  const summary = { supported: 0, adapted: 0, fallback: 0, blocked: 0 }
  for (const row of rows) summary[row.status] += 1
  return {
    status: summary.blocked > 0 ? 'fail' : summary.fallback > 0 ? 'warning' : 'pass',
    ...summary,
  }
}

function sourceStatus(report, label) {
  const count = report?.moduleCount ?? (Array.isArray(report?.moduleIds) ? report.moduleIds.length : 0)
  const evidenceKind = report?.evidenceKind ? ` ${report.evidenceKind}` : ''
  return `${label} status=${normalizeStatus(report?.status) || 'UNKNOWN'} modules=${count}${evidenceKind}`
}

function firstBlocker(report) {
  const blockers = Array.isArray(report?.blockers) ? report.blockers.filter(Boolean) : []
  return blockers[0] ?? ''
}

function runtimeEvidence(sources) {
  return `${sourceStatus(sources.runtime, 'NeoForge lifecycle')}; loadedModules=${arrayLength(sources.runtime?.loadedModuleIds)}; visibleRoutes=${arrayLength(sources.runtime?.visibleRoutes)}.`
}

function runtimeReason(sources) {
  if (reportPassed(sources.runtime)) return ''
  return firstBlocker(sources.runtime) || 'NeoForge lifecycle evidence is not PASS.'
}

function uiEvidence(sources) {
  return `${sourceStatus(sources.clientUi, 'NeoForge client UI')}; ${sourceStatus(sources.play, 'NeoForge strict play')}.`
}

function uiReason(sources) {
  if (reportPassed(sources.clientUi) && reportPassed(sources.play)) return ''
  return firstBlocker(sources.clientUi) || firstBlocker(sources.play) || 'Executed NeoForge client UI and live play proof are required before this surface can replace the visible vanilla route.'
}

function diagnosticEvidence(sources) {
  return `${runtimeEvidence(sources)} Content graph modules=${sources.contentGraph.moduleCount} nodes=${sources.contentGraph.nodeCount} exportPlans=${sources.contentGraph.exportPlanCount}.`
}

function diagnosticReason(sources) {
  if (reportPassed(sources.runtime) && contentGraphUsable(sources.contentGraph)) return ''
  return 'Diagnostics need PASS lifecycle evidence and usable Content Graph evidence.'
}

function registryEvidence(sources) {
  return sourceStatus(sources.registryContent, 'NeoForge registry/content')
}

function registryReason(sources) {
  if (reportPassed(sources.registryContent)) return ''
  return firstBlocker(sources.registryContent) || 'Executed NeoForge registry/datapack/worldgen evidence is required.'
}

function gameTestEvidence(sources) {
  return `${sourceStatus(sources.gameTests, 'NeoForge GameTests')}; trustedMutations=${arrayLength(sources.gameTests?.trustedMutations)}.`
}

function gameTestReason(sources) {
  if (reportPassed(sources.gameTests)) return ''
  return firstBlocker(sources.gameTests) || 'Executed NeoForge GameTest mutation evidence is required.'
}

function livePlayEvidence(sources) {
  return sourceStatus(sources.play, 'NeoForge strict play')
}

function livePlayReason(sources) {
  if (reportPassed(sources.play)) return ''
  return firstBlocker(sources.play) || 'Executed NeoForge live play proof is required for visible player action parity.'
}

function saveEvidence(sources) {
  return `${sourceStatus(sources.gameTests, 'NeoForge GameTests')}; saveEvidence=${arrayLength(sources.gameTests?.saveEvidence)}.`
}

function networkEvidence(sources) {
  return `${sourceStatus(sources.gameTests, 'NeoForge GameTests')}; networkEvidence=${arrayLength(sources.gameTests?.networkEvidence)}.`
}

function arrayLength(value) {
  return Array.isArray(value) ? value.length : 0
}

function stripBom(text) {
  return text.charCodeAt(0) === 0xfeff ? text.slice(1) : text
}

function parseArgs(argv) {
  const options = {}
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--repo-root') {
      options.repoRoot = argv[++index]
    } else if (arg === '--out') {
      options.outPath = argv[++index]
    } else if (arg === '--help' || arg === '-h') {
      options.help = true
    } else {
      throw new Error(`Unknown argument: ${arg}`)
    }
  }
  return options
}

const isMain = fileURLToPath(import.meta.url) === path.resolve(process.argv[1] ?? '')
if (isMain) {
  try {
    const options = parseArgs(process.argv.slice(2))
    if (options.help) {
      console.log('Usage: node scripts/generate-neoforge-runtime-conformance.mjs [--repo-root <path>] [--out <path>]')
    } else {
      const { report, path: output } = await generateNeoForgeRuntimeConformance(options)
      console.log(`Wrote NeoForge runtime conformance: ${output}`)
      console.log(`NeoForge runtime conformance ${report.summary.status}: ${report.summary.adapted} adapted, ${report.summary.fallback} fallback, ${report.summary.blocked} blocked`)
      if (report.summary.blocked > 0) {
        throw new Error('NeoForge runtime conformance has blocked rows.')
      }
    }
  } catch (error) {
    console.error(error.stack || error.message)
    process.exitCode = 1
  }
}

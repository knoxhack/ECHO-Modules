#!/usr/bin/env node
import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'

function usage() {
  return `Usage: node addons/echoskyrelayprotocol/scripts/smoke-skyrelay-gameplay-route.mjs [options]

Validates Sky Relay campaign route data for the first 30 minutes, first 2 hours,
and Signal Crown completion contract. This is a deterministic data-contract
smoke, not a replacement for a visible in-game playthrough.

Options:
  --module-root <path>  Sky Relay module root. Default: current working directory.
  --out <path>          Optional JSON evidence output path.
`
}

function parseArgs(argv) {
  const args = {
    moduleRoot: process.cwd(),
    out: null,
    help: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    const next = () => {
      const value = argv[++index]
      if (!value) throw new Error(`${arg} requires a value`)
      return value
    }
    if (arg === '--module-root') args.moduleRoot = path.resolve(next())
    else if (arg === '--out') args.out = path.resolve(next())
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

async function readJson(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'))
}

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

function ids(rows) {
  return new Set(rows.map((row) => row.id))
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function proofResolver({ blocks, items, fragments, terminalPages, lensProfiles, holoMapLayers, weatherRoutes, recoveryBindings }) {
  const externalProofs = new Set([
    'external_block:hydroponic_tray',
    'item:recovered_seed_capsule',
    'power:hand_crank_online',
    'power:small_battery_bank_stable',
    'integration:logistics_first_route',
    'integration:logistics_automated_route',
    'integration:shield_network_online',
    'terminal:final_restoration_sequence',
    'weather:severe_storm_survived',
  ])

  return (proof) => {
    const [type, value] = String(proof).split(':')
    const normalized = `${type}:${value}`
    if (type === 'block' || type === 'block_seen') return { ok: blocks.has(value), scope: 'internal', type, value }
    if (type === 'item') return { ok: items.has(value) || externalProofs.has(normalized), scope: items.has(value) ? 'internal' : 'external', type, value }
    if (type === 'fragment') return { ok: fragments.has(value), scope: 'internal', type, value }
    if (type === 'terminal_page') return { ok: terminalPages.has(value), scope: 'internal', type, value }
    if (type === 'lens_scan') return { ok: lensProfiles.has(value), scope: 'internal', type, value }
    if (type === 'holomap') return { ok: fragments.has(value) || holoMapLayers.has(value), scope: fragments.has(value) ? 'internal-fragment' : 'internal-layer', type, value }
    if (type === 'holomap_layer') return { ok: holoMapLayers.has(value), scope: 'internal', type, value }
    if (type === 'weather') return { ok: weatherRoutes.has(value) || externalProofs.has(normalized), scope: weatherRoutes.has(value) ? 'internal' : 'external', type, value }
    if (type === 'recovery') return { ok: recoveryBindings.has(value), scope: 'internal', type, value }
    if (externalProofs.has(normalized)) return { ok: true, scope: 'external', type, value }
    return { ok: false, scope: 'unknown', type, value }
  }
}

function validateRoute({ route, chapterIds, resolveProof, requiredChapterIds, requiredProofs }) {
  assert(route.schema === 'echo.skyrelay.route.v1', `${route.id}: route schema mismatch`)
  assert(Number(route.targetDurationMinutes) > 0, `${route.id}: targetDurationMinutes must be positive`)
  assert(Array.isArray(route.steps) && route.steps.length > 0, `${route.id}: route must define steps`)

  const seenStepIds = new Set()
  const chapters = new Set()
  const proofs = new Set()
  const externalProofs = []
  const resolvedProofs = []

  for (const step of route.steps) {
    assert(step.id && !seenStepIds.has(step.id), `${route.id}: duplicate or missing step id ${step.id}`)
    seenStepIds.add(step.id)
    assert(chapterIds.has(step.chapterId), `${route.id}: unknown chapter ${step.chapterId}`)
    chapters.add(step.chapterId)
    assert(step.objective && step.proof, `${route.id}: step ${step.id} must define objective and proof`)
    const resolved = resolveProof(step.proof)
    assert(resolved.ok, `${route.id}: step ${step.id} has unresolved proof ${step.proof}`)
    proofs.add(step.proof)
    resolvedProofs.push({ stepId: step.id, proof: step.proof, scope: resolved.scope })
    if (resolved.scope === 'external') externalProofs.push(step.proof)
  }

  for (const chapterId of requiredChapterIds) {
    assert(chapters.has(chapterId), `${route.id}: missing required chapter ${chapterId}`)
  }
  for (const proof of requiredProofs) {
    assert(proofs.has(proof), `${route.id}: missing required proof ${proof}`)
  }

  return {
    id: route.id,
    targetDurationMinutes: route.targetDurationMinutes,
    stepCount: route.steps.length,
    chapters: [...chapters],
    requiredChaptersCovered: requiredChapterIds,
    requiredProofsCovered: requiredProofs,
    externalProofs: [...new Set(externalProofs)].sort(),
    resolvedProofs,
  }
}

function validateSignalCrown({ signalCrown, items, blocks, fragments, anchorRuleIds, resolveProof }) {
  assert(signalCrown.schema === 'echo.skyrelay.signal_crown_requirements.v1', 'signal crown schema mismatch')
  assert(signalCrown.id === 'signal_crown', 'signal crown id mismatch')
  assert(fragments.has('signal_crown'), 'signal_crown fragment must exist')
  assert(anchorRuleIds.has('signal_crown'), 'signal_crown anchor rule must exist')
  assert(items.has(signalCrown.reward), `Signal Crown reward ${signalCrown.reward} is not a registered item`)
  assert(Array.isArray(signalCrown.requirements) && signalCrown.requirements.length >= 6, 'Signal Crown must define at least 6 requirements')

  const requirements = []
  for (const requirement of signalCrown.requirements) {
    assert(requirement.id && Number(requirement.requiredCount) > 0 && requirement.proof, `Invalid Signal Crown requirement ${requirement.id}`)
    const resolved = resolveProof(requirement.proof)
    assert(resolved.ok, `Signal Crown requirement ${requirement.id} has unresolved proof ${requirement.proof}`)
    requirements.push({
      id: requirement.id,
      requiredCount: requirement.requiredCount,
      proof: requirement.proof,
      scope: resolved.scope,
    })
  }

  assert(requirements.some((requirement) => requirement.proof === 'item:stabilized_platform_core'), 'Signal Crown requires stabilized platform cores')
  assert(requirements.some((requirement) => requirement.proof === 'block:relay_signal_array'), 'Signal Crown requires relay signal array')
  assert(requirements.some((requirement) => requirement.proof === 'item:orbital_alloy_scrap'), 'Signal Crown requires orbital alloy scrap')
  assert(blocks.has('signal_crown_interface'), 'Signal Crown Interface block must exist')

  return {
    id: signalCrown.id,
    requirementCount: requirements.length,
    reward: signalCrown.reward,
    fragment: 'signal_crown',
    anchorRule: 'signal_crown',
    requirements,
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    console.log(usage())
    return
  }

  const dataRoot = path.join(args.moduleRoot, 'src', 'main', 'resources', 'data', 'echoskyrelayprotocol', 'skyrelay')
  const blockCatalog = await readJson(path.join(dataRoot, 'content', 'block_catalog.json'))
  const itemCatalog = await readJson(path.join(dataRoot, 'content', 'item_catalog.json'))
  const fragmentCatalog = await readJson(path.join(dataRoot, 'fragments', 'fragment_catalog.json'))
  const anchorRules = await readJson(path.join(dataRoot, 'fragments', 'anchor_rules.json'))
  const chapters = await readJson(path.join(dataRoot, 'progression', 'chapter_catalog.json'))
  const first30 = await readJson(path.join(dataRoot, 'progression', 'first_30_minutes.json'))
  const first2Hours = await readJson(path.join(dataRoot, 'progression', 'first_2_hours.json'))
  const signalCrown = await readJson(path.join(dataRoot, 'progression', 'signal_crown_requirements.json'))
  const terminalPages = await readJson(path.join(dataRoot, 'integrations', 'terminal_pages.json'))
  const lensProfiles = await readJson(path.join(dataRoot, 'integrations', 'lens_scan_profiles.json'))
  const holoMapLayers = await readJson(path.join(dataRoot, 'integrations', 'holomap_layers.json'))
  const weatherRoutes = await readJson(path.join(dataRoot, 'integrations', 'weather_routes.json'))
  const recoveryBindings = await readJson(path.join(dataRoot, 'integrations', 'recovery_bindings.json'))

  const catalog = {
    blocks: ids(blockCatalog.blocks),
    items: ids(itemCatalog.items),
    fragments: ids(fragmentCatalog.fragments),
    terminalPages: ids(terminalPages.pages),
    lensProfiles: ids(lensProfiles.profiles),
    holoMapLayers: ids(holoMapLayers.layers),
    weatherRoutes: new Set(weatherRoutes.routes.flatMap((route) => [route.id, route.event])),
    recoveryBindings: ids(recoveryBindings.bindings),
  }
  const resolveProof = proofResolver(catalog)
  const chapterIds = ids(chapters.chapters)
  const anchorRuleIds = new Set(anchorRules.rules.map((rule) => rule.fragmentId))

  const routes = [
    validateRoute({
      route: first30,
      chapterIds,
      resolveProof,
      requiredChapterIds: ['awakening', 'power_critical', 'first_anchor', 'storm_warning'],
      requiredProofs: ['block_seen:damaged_relay_core', 'terminal_page:relay_status', 'lens_scan:damaged_relay_core', 'fragment:hydroponics_deck'],
    }),
    validateRoute({
      route: first2Hours,
      chapterIds,
      resolveProof,
      requiredChapterIds: ['water_problem', 'salvage_expansion', 'storm_warning', 'solar_recovery', 'weather_control', 'network_routing', 'machine_restoration'],
      requiredProofs: ['block:atmospheric_condenser', 'fragment:aero_salvage_yard', 'block:storm_shield_pylon', 'fragment:solar_wing', 'fragment:weather_mast', 'item:stabilized_platform_core'],
    }),
  ]

  const signalCrownResult = validateSignalCrown({
    signalCrown,
    items: catalog.items,
    blocks: catalog.blocks,
    fragments: catalog.fragments,
    anchorRuleIds,
    resolveProof,
  })

  const report = {
    schemaVersion: 'echo.skyrelay.gameplay-route-smoke.v1',
    ok: true,
    generatedAt: new Date().toISOString(),
    moduleId: 'echoskyrelayprotocol',
    scope: 'data-contract-smoke',
    note: 'This validates route and proof data for gameplay smoke planning. It does not replace a visible in-game playthrough.',
    catalogCounts: {
      blocks: catalog.blocks.size,
      items: catalog.items.size,
      fragments: catalog.fragments.size,
      chapters: chapterIds.size,
      anchorRules: anchorRuleIds.size,
      terminalPages: catalog.terminalPages.size,
      lensProfiles: catalog.lensProfiles.size,
      holoMapLayers: catalog.holoMapLayers.size,
      weatherRoutes: weatherRoutes.routes.length,
      recoveryBindings: catalog.recoveryBindings.size,
    },
    routes,
    signalCrown: signalCrownResult,
    gates: {
      first30RouteContract: 'passed',
      first2HourRouteContract: 'passed',
      signalCrownContract: 'passed',
      realFirst30Playthrough: 'not_started',
      realFirst2HourPlaythrough: 'not_started',
      realSignalCrownPlaythrough: 'not_started',
    },
  }

  if (args.out) await writeJson(args.out, report)
  console.log(JSON.stringify(report, null, 2))
}

main().catch((error) => {
  console.error(error instanceof Error ? error.stack || error.message : String(error))
  process.exit(1)
})

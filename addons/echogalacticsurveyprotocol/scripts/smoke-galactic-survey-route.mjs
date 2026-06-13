#!/usr/bin/env node
import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'

function usage() {
  return `Usage: node addons/echogalacticsurveyprotocol/scripts/smoke-galactic-survey-route.mjs [options]

Validates Galactic Survey route data for the first 30 minutes, first 2 hours,
and Survey Array completion contract. This is a deterministic data-contract
smoke, not a replacement for a visible in-game playthrough.

Options:
  --module-root <path>  Galactic Survey module root. Default: current working directory.
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

function proofResolver({ blocks, items, probes, sectors, bodies, routes, discoveries, salvageSites, depots, terminalPages, lensProfiles, holoMapLayers, missions }) {
  const externalProofs = new Set([
    'power:small_relay_online',
    'catalog:complete_sector_atlas',
  ])

  return (proof) => {
    const [type, value] = String(proof).split(':')
    const normalized = `${type}:${value}`
    if (type === 'block' || type === 'block_seen') return { ok: blocks.has(value), scope: 'internal', type, value }
    if (type === 'item') return { ok: items.has(value), scope: 'internal', type, value }
    if (type === 'probe') return { ok: probes.has(value), scope: 'internal', type, value }
    if (type === 'sector') return { ok: sectors.has(value), scope: 'internal', type, value }
    if (type === 'body') return { ok: bodies.has(value), scope: 'internal', type, value }
    if (type === 'route') return { ok: routes.has(value), scope: 'internal', type, value }
    if (type === 'discovery' || type === 'index') return { ok: discoveries.has(value) || bodies.has(value), scope: 'internal', type, value }
    if (type === 'salvage') return { ok: salvageSites.has(value), scope: 'internal', type, value }
    if (type === 'depot') return { ok: depots.has(value), scope: 'internal', type, value }
    if (type === 'mission') return { ok: missions.has(value), scope: 'internal', type, value }
    if (type === 'terminal_page' || type === 'terminal') return { ok: terminalPages.has(value), scope: 'internal', type, value }
    if (type === 'lens_scan') return { ok: lensProfiles.has(value) || salvageSites.has(value), scope: 'internal', type, value }
    if (type === 'holomap' || type === 'holomap_layer') return { ok: holoMapLayers.has(value) || sectors.has(value), scope: 'internal', type, value }
    if (externalProofs.has(normalized)) return { ok: true, scope: 'external', type, value }
    return { ok: false, scope: 'unknown', type, value }
  }
}

function validateRoute({ route, chapterIds, resolveProof, requiredChapterIds, requiredProofs }) {
  assert(route.schema === 'echo.galactic_survey.route.v1', `${route.id}: route schema mismatch`)
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

function validateSurveyArray({ surveyArray, items, blocks, discoveries, depots, resolveProof }) {
  assert(surveyArray.schema === 'echo.galactic_survey.survey_array_requirements.v1', 'Survey Array schema mismatch')
  assert(surveyArray.id === 'survey_array', 'Survey Array id mismatch')
  assert(items.has(surveyArray.reward), `Survey Array reward ${surveyArray.reward} is not a registered item`)
  assert(blocks.has('survey_array_console'), 'Survey Array Console block must exist')
  assert(discoveries.has('deep_sector_beacon_ks_04'), 'Deep-sector beacon discovery must exist')
  assert(depots.has('cinder_ring_remote_depot'), 'Remote depot requirement must exist')
  assert(Array.isArray(surveyArray.requirements) && surveyArray.requirements.length >= 6, 'Survey Array must define at least 6 requirements')

  const requirements = []
  for (const requirement of surveyArray.requirements) {
    assert(requirement.id && Number(requirement.requiredCount) > 0 && requirement.proof, `Invalid Survey Array requirement ${requirement.id}`)
    const resolved = resolveProof(requirement.proof)
    assert(resolved.ok, `Survey Array requirement ${requirement.id} has unresolved proof ${requirement.proof}`)
    requirements.push({
      id: requirement.id,
      requiredCount: requirement.requiredCount,
      proof: requirement.proof,
      scope: resolved.scope,
    })
  }

  assert(requirements.some((requirement) => requirement.proof === 'catalog:complete_sector_atlas'), 'Survey Array requires complete sector atlas')
  assert(requirements.some((requirement) => requirement.proof === 'item:survey_array_key'), 'Survey Array requires survey array key')
  assert(requirements.some((requirement) => requirement.proof === 'discovery:deep_sector_beacon_ks_04'), 'Survey Array requires deep-sector beacon discovery')

  return {
    id: surveyArray.id,
    requirementCount: requirements.length,
    reward: surveyArray.reward,
    requirements,
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    console.log(usage())
    return
  }

  const dataRoot = path.join(args.moduleRoot, 'src', 'main', 'resources', 'data', 'echogalacticsurveyprotocol', 'galacticsurvey')
  const blockCatalog = await readJson(path.join(dataRoot, 'content', 'block_catalog.json'))
  const itemCatalog = await readJson(path.join(dataRoot, 'content', 'item_catalog.json'))
  const sectorCatalog = await readJson(path.join(dataRoot, 'survey', 'sector_catalog.json'))
  const bodyCatalog = await readJson(path.join(dataRoot, 'survey', 'body_catalog.json'))
  const probeCatalog = await readJson(path.join(dataRoot, 'survey', 'probe_catalog.json'))
  const routeCatalog = await readJson(path.join(dataRoot, 'survey', 'route_catalog.json'))
  const discoveryCatalog = await readJson(path.join(dataRoot, 'survey', 'discovery_catalog.json'))
  const salvageSites = await readJson(path.join(dataRoot, 'survey', 'salvage_sites.json'))
  const depotCatalog = await readJson(path.join(dataRoot, 'survey', 'depot_catalog.json'))
  const chapters = await readJson(path.join(dataRoot, 'progression', 'chapter_catalog.json'))
  const first30 = await readJson(path.join(dataRoot, 'progression', 'first_30_minutes.json'))
  const first2Hours = await readJson(path.join(dataRoot, 'progression', 'first_2_hours.json'))
  const surveyArray = await readJson(path.join(dataRoot, 'progression', 'survey_array_requirements.json'))
  const terminalPages = await readJson(path.join(dataRoot, 'integrations', 'terminal_pages.json'))
  const lensProfiles = await readJson(path.join(dataRoot, 'integrations', 'lens_scan_profiles.json'))
  const holoMapLayers = await readJson(path.join(dataRoot, 'integrations', 'holomap_layers.json'))
  const missions = await readJson(path.join(dataRoot, 'integrations', 'mission_contracts.json'))

  const catalog = {
    blocks: ids(blockCatalog.blocks),
    items: ids(itemCatalog.items),
    probes: ids(probeCatalog.probes),
    sectors: ids(sectorCatalog.sectors),
    bodies: ids(bodyCatalog.bodies),
    routes: ids(routeCatalog.routes),
    discoveries: ids(discoveryCatalog.discoveries),
    salvageSites: ids(salvageSites.sites),
    depots: ids(depotCatalog.depots),
    terminalPages: ids(terminalPages.pages),
    lensProfiles: ids(lensProfiles.profiles),
    holoMapLayers: ids(holoMapLayers.layers),
    missions: ids(missions.missions),
  }
  const resolveProof = proofResolver(catalog)
  const chapterIds = ids(chapters.chapters)

  const routes = [
    validateRoute({
      route: first30,
      chapterIds,
      resolveProof,
      requiredChapterIds: ['outpost_wake', 'network_offline', 'relay_repair', 'first_probe_launch', 'partial_map_reveal', 'first_salvage', 'first_catalog_entry', 'fuel_route_prep'],
      requiredProofs: ['block_seen:survey_terminal', 'terminal_page:survey_network', 'power:small_relay_online', 'probe:starter_probe', 'holomap_layer:scan_cones', 'lens_scan:fallen_orbital_fragment', 'item:burned_navigation_core', 'discovery:barren_moon_kg_01a', 'mission:first_survey_hop', 'item:fuel_canister'],
    }),
    validateRoute({
      route: first2Hours,
      chapterIds,
      resolveProof,
      requiredChapterIds: ['first_probe_launch', 'partial_map_reveal', 'first_catalog_entry', 'survey_circuit', 'remote_depot', 'hazard_salvage'],
      requiredProofs: ['probe:starter_probe', 'discovery:barren_moon_kg_01a', 'discovery:planet_candidate_ks_02', 'discovery:signal_anomaly_veil_trace', 'salvage:derelict_relay_osprey', 'route:near_sector_01_survey_hop', 'item:long_range_probe', 'depot:cinder_ring_remote_depot', 'mission:first_survey_circuit', 'item:catalog_badge'],
    }),
  ]

  const surveyArrayResult = validateSurveyArray({
    surveyArray,
    items: catalog.items,
    blocks: catalog.blocks,
    discoveries: catalog.discoveries,
    depots: catalog.depots,
    resolveProof,
  })

  const report = {
    schemaVersion: 'echo.galactic_survey.gameplay-route-smoke.v1',
    ok: true,
    generatedAt: new Date().toISOString(),
    moduleId: 'echogalacticsurveyprotocol',
    scope: 'data-contract-smoke',
    note: 'This validates route and proof data for gameplay smoke planning. It does not replace a visible in-game playthrough.',
    catalogCounts: {
      blocks: catalog.blocks.size,
      items: catalog.items.size,
      probes: catalog.probes.size,
      sectors: catalog.sectors.size,
      bodies: catalog.bodies.size,
      routes: catalog.routes.size,
      discoveries: catalog.discoveries.size,
      salvageSites: catalog.salvageSites.size,
      depots: catalog.depots.size,
      chapters: chapterIds.size,
      terminalPages: catalog.terminalPages.size,
      lensProfiles: catalog.lensProfiles.size,
      holoMapLayers: catalog.holoMapLayers.size,
      missions: catalog.missions.size,
    },
    routes,
    surveyArray: surveyArrayResult,
    gates: {
      first30RouteContract: 'passed',
      first2HourRouteContract: 'passed',
      surveyArrayContract: 'passed',
      realFirst30Playthrough: 'not_started',
      realFirst2HourPlaythrough: 'not_started',
      realSurveyArrayPlaythrough: 'not_started',
    },
  }

  if (args.out) await writeJson(args.out, report)
  console.log(JSON.stringify(report, null, 2))
}

main().catch((error) => {
  console.error(error instanceof Error ? error.stack || error.message : String(error))
  process.exit(1)
})

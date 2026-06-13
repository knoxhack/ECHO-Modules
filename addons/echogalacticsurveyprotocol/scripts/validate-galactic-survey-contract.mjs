#!/usr/bin/env node
import fs from 'node:fs'
import path from 'node:path'

const moduleRoot = path.resolve(process.argv.includes('--module-root')
  ? process.argv[process.argv.indexOf('--module-root') + 1]
  : 'addons/echogalacticsurveyprotocol')

const readJson = (...parts) => JSON.parse(fs.readFileSync(path.join(moduleRoot, ...parts), 'utf8'))
const readText = (...parts) => fs.readFileSync(path.join(moduleRoot, ...parts), 'utf8')
const fail = (message) => {
  throw new Error(message)
}

const dataRoot = ['src/main/resources/data/echogalacticsurveyprotocol/galacticsurvey']
const blocks = readJson(...dataRoot, 'content/block_catalog.json').blocks
const items = readJson(...dataRoot, 'content/item_catalog.json').items
const sectors = readJson(...dataRoot, 'survey/sector_catalog.json').sectors
const bodies = readJson(...dataRoot, 'survey/body_catalog.json').bodies
const probes = readJson(...dataRoot, 'survey/probe_catalog.json').probes
const routes = readJson(...dataRoot, 'survey/route_catalog.json').routes
const discoveries = readJson(...dataRoot, 'survey/discovery_catalog.json').discoveries
const salvageSites = readJson(...dataRoot, 'survey/salvage_sites.json').sites
const salvageLootTables = readJson(...dataRoot, 'survey/salvage_loot_tables.json').lootTables
const depots = readJson(...dataRoot, 'survey/depot_catalog.json').depots
const phases = readJson(...dataRoot, 'plan/production_phase_matrix.json').phases
const chapters = readJson(...dataRoot, 'progression/chapter_catalog.json').chapters
const first30 = readJson(...dataRoot, 'progression/first_30_minutes.json').steps
const first2Hours = readJson(...dataRoot, 'progression/first_2_hours.json').steps
const surveyArrayRequirements = readJson(...dataRoot, 'progression/survey_array_requirements.json').requirements
const terminalPages = readJson(...dataRoot, 'integrations/terminal_pages.json').pages
const lensProfiles = readJson(...dataRoot, 'integrations/lens_scan_profiles.json').profiles
const holoMapLayers = readJson(...dataRoot, 'integrations/holomap_layers.json').layers
const missions = readJson(...dataRoot, 'integrations/mission_contracts.json').missions
const soundEvents = readJson(...dataRoot, 'integrations/sound_events.json').events
const releaseGates = readJson(...dataRoot, 'release/release_gates.json').gates
const descriptor = readJson('src/main/resources/META-INF/echo.mod.json')
const blockRegistry = readText('src/main/java/com/knoxhack/echogalacticsurveyprotocol/registry/GalacticSurveyBlocks.java')
const itemRegistry = readText('src/main/java/com/knoxhack/echogalacticsurveyprotocol/registry/GalacticSurveyItems.java')
const nativeModule = readText('src/main/java/com/knoxhack/echogalacticsurveyprotocol/EchoGalacticSurveyNativeModule.java')
const runtimeService = readText('src/main/java/com/knoxhack/echogalacticsurveyprotocol/runtime/GalacticSurveyRuntimeService.java')

const ids = (rows, field = 'id') => rows.map((row) => row[field])
const unique = (rows, label) => {
  const seen = new Set()
  for (const id of rows) {
    if (!id) fail(`Missing ${label} id`)
    if (seen.has(id)) fail(`Duplicate ${label} id: ${id}`)
    seen.add(id)
  }
  return seen
}

const blockIds = unique(ids(blocks), 'block')
const itemIds = unique(ids(items), 'item')
const sectorIds = unique(ids(sectors), 'sector')
const bodyIds = unique(ids(bodies), 'body')
const probeIds = unique(ids(probes), 'probe')
const routeIds = unique(ids(routes), 'route')
const discoveryIds = unique(ids(discoveries), 'discovery')
const salvageSiteIds = unique(ids(salvageSites), 'salvage site')
const lootTableIds = unique(ids(salvageLootTables), 'salvage loot table')
const depotIds = unique(ids(depots), 'depot')
const chapterIds = unique(ids(chapters), 'chapter')
const terminalPageIds = unique(ids(terminalPages), 'terminal page')
const lensProfileIds = unique(ids(lensProfiles), 'lens profile')
const holoMapLayerIds = unique(ids(holoMapLayers), 'HoloMap layer')
const missionIds = unique(ids(missions), 'mission')

if (blocks.length !== 10) fail(`Expected 10 Galactic Survey blocks, found ${blocks.length}.`)
if (items.length !== 16) fail(`Expected 16 Galactic Survey items, found ${items.length}.`)
if (phases.length !== 10) fail(`Expected 10 production phases, found ${phases.length}.`)
if (sectors.length !== 4) fail(`Expected 4 sectors, found ${sectors.length}.`)
if (bodies.length !== 5) fail(`Expected 5 bodies, found ${bodies.length}.`)
if (probes.length !== 4) fail(`Expected 4 probes, found ${probes.length}.`)
if (routes.length !== 4) fail(`Expected 4 routes, found ${routes.length}.`)
if (discoveries.length !== 9) fail(`Expected 9 discoveries, found ${discoveries.length}.`)
if (salvageSites.length !== 5) fail(`Expected 5 salvage sites, found ${salvageSites.length}.`)
if (salvageLootTables.length !== 3) fail(`Expected 3 salvage loot tables, found ${salvageLootTables.length}.`)
if (depots.length !== 3) fail(`Expected 3 depots, found ${depots.length}.`)
if (chapters.length !== 12) fail(`Expected 12 chapters, found ${chapters.length}.`)
if (first30.length !== 12) fail(`Expected 12 first-30-minute steps, found ${first30.length}.`)
if (first2Hours.length !== 10) fail(`Expected 10 first-2-hour steps, found ${first2Hours.length}.`)
if (surveyArrayRequirements.length !== 6) fail(`Expected 6 Survey Array requirements, found ${surveyArrayRequirements.length}.`)
if (terminalPages.length !== 4) fail(`Expected 4 terminal pages, found ${terminalPages.length}.`)
if (lensProfiles.length !== 5) fail(`Expected 5 lens profiles, found ${lensProfiles.length}.`)
if (holoMapLayers.length !== 7) fail(`Expected 7 HoloMap layers, found ${holoMapLayers.length}.`)
if (missions.length !== 6) fail(`Expected 6 mission contracts, found ${missions.length}.`)
if (soundEvents.length !== 6) fail(`Expected 6 sound events, found ${soundEvents.length}.`)
if (releaseGates.length !== 14) fail(`Expected 14 release gates, found ${releaseGates.length}.`)

for (const phase of phases) {
  if (!Array.isArray(phase.subphases) || phase.subphases.length !== 5) {
    fail(`Phase ${phase.id} must contain exactly 5 subphases.`)
  }
}

for (const block of blocks) {
  if (!blockRegistry.includes(`"${block.id}"`)) {
    fail(`Block ${block.id} is missing from GalacticSurveyBlocks.`)
  }
}

for (const item of items) {
  if (!itemRegistry.includes(`"${item.id}"`)) {
    fail(`Item ${item.id} is missing from GalacticSurveyItems.`)
  }
}

const externalProofs = new Set([
  'power:small_relay_online',
  'catalog:complete_sector_atlas',
  'launcher:install_update_repair_rollback',
  'manual:real_first_30_playthrough',
  'manual:real_first_2_hour_playthrough',
  'manual:real_survey_array_playthrough',
  'manual:fresh_world_created',
  'manual:save_reload_verified',
  'manual:no_crash_evidence',
])

function resolveProof(proof) {
  const [type, value] = String(proof).split(':')
  if (type === 'block' || type === 'block_seen') return blockIds.has(value)
  if (type === 'item') return itemIds.has(value)
  if (type === 'probe') return probeIds.has(value)
  if (type === 'sector') return sectorIds.has(value)
  if (type === 'body') return bodyIds.has(value)
  if (type === 'route') return routeIds.has(value)
  if (type === 'discovery' || type === 'index') return discoveryIds.has(value) || bodyIds.has(value)
  if (type === 'salvage') return salvageSiteIds.has(value)
  if (type === 'loot') return lootTableIds.has(value)
  if (type === 'depot') return depotIds.has(value)
  if (type === 'mission') return missionIds.has(value)
  if (type === 'terminal_page' || type === 'terminal') return terminalPageIds.has(value)
  if (type === 'lens_scan') return lensProfileIds.has(value) || salvageSiteIds.has(value)
  if (type === 'holomap' || type === 'holomap_layer') return holoMapLayerIds.has(value) || sectorIds.has(value)
  return externalProofs.has(`${type}:${value}`)
}

function assertProof(proof, label) {
  if (!resolveProof(proof)) fail(`${label} references unresolved proof ${proof}`)
}

for (const body of bodies) {
  if (!sectorIds.has(body.sectorId)) fail(`Body ${body.id} references unknown sector ${body.sectorId}.`)
}

for (const probe of probes) {
  if (!itemIds.has(probe.itemId)) fail(`Probe ${probe.id} references unknown item ${probe.itemId}.`)
  assertProof(probe.unlockProof, `Probe ${probe.id}`)
}

for (const route of routes) {
  if (!sectorIds.has(route.destination)) fail(`Route ${route.id} references unknown destination ${route.destination}.`)
  assertProof(route.requiredProof, `Route ${route.id}`)
  assertProof(route.unlocks, `Route ${route.id}`)
}

for (const discovery of discoveries) {
  if (!sectorIds.has(discovery.sectorId)) fail(`Discovery ${discovery.id} references unknown sector ${discovery.sectorId}.`)
  assertProof(discovery.reward, `Discovery ${discovery.id}`)
}

for (const site of salvageSites) {
  if (!sectorIds.has(site.sectorId)) fail(`Salvage site ${site.id} references unknown sector ${site.sectorId}.`)
  if (!lootTableIds.has(site.lootTable)) fail(`Salvage site ${site.id} references unknown loot table ${site.lootTable}.`)
  assertProof(site.requiredPreparation, `Salvage site ${site.id}`)
}

for (const lootTable of salvageLootTables) {
  for (const entry of lootTable.entries) {
    assertProof(entry, `Loot table ${lootTable.id}`)
  }
}

for (const depot of depots) {
  if (!sectorIds.has(depot.sectorId)) fail(`Depot ${depot.id} references unknown sector ${depot.sectorId}.`)
  assertProof(depot.unlocks, `Depot ${depot.id}`)
}

for (const profile of lensProfiles) {
  if (!blockIds.has(profile.target)) fail(`Lens profile ${profile.id} references unknown target block ${profile.target}.`)
}

for (const mission of missions) {
  assertProof(mission.proof, `Mission ${mission.id}`)
}

for (const step of [...first30, ...first2Hours]) {
  if (!chapterIds.has(step.chapterId)) fail(`Route step ${step.id} references unknown chapter ${step.chapterId}.`)
  assertProof(step.proof, `Route step ${step.id}`)
}

for (const requirement of surveyArrayRequirements) {
  if (!Number.isInteger(requirement.requiredCount) || requirement.requiredCount < 1) {
    fail(`Survey Array requirement ${requirement.id} must have a positive integer requiredCount.`)
  }
  assertProof(requirement.proof, `Survey Array requirement ${requirement.id}`)
}

for (const gate of releaseGates) {
  assertProof(gate.proof, `Release gate ${gate.id}`)
}

for (const required of [
  'galacticsurvey.content',
  'galacticsurvey.sectors',
  'galacticsurvey.probes',
  'galacticsurvey.routes',
  'galacticsurvey.salvage',
  'galacticsurvey.release_readiness',
]) {
  if (!descriptor.provides.includes(required)) {
    fail(`Descriptor is missing provided namespace ${required}.`)
  }
}

for (const requiredNativeSurface of [
  'GalacticSurveyProbeRuntime.adapterManifest()',
  'GalacticSurveyProgressionRuntime.adapterManifest()',
  'GalacticSurveyRuntimeService.adapterManifest()',
  'GalacticSurveySystemIntegrationContracts.adapterManifest()',
]) {
  if (!nativeModule.includes(requiredNativeSurface)) {
    fail(`Native module is missing ${requiredNativeSurface}.`)
  }
}

for (const requiredRuntimeSurface of [
  'ProbeLaunchRequest',
  'ProbeLaunchResult',
  'OutpostState',
  'OutpostRepairRequest',
  'OutpostRepairResult',
  'HoloMapMarker',
  'HoloMapPlan',
  'CatalogState',
  'RoutePlanRequest',
  'RoutePlanResult',
  'SalvageAttempt',
  'SalvageResult',
  'DepotBuildRequest',
  'DepotEstablishmentResult',
  'SurveySaveSnapshot',
  'SurveyArrayRequirementStatus',
  'SurveyArrayRestorationResult',
  'ReleaseGateStatus',
  'PublicAlphaReadinessReport',
  'startingOutpost',
  'repairOutpostRelay',
  'buildHoloMapPlan',
  'restoreSurveyArray',
  'evaluatePublicAlphaReadiness',
  'surveyArrayReadySnapshot',
  'firstThirtyMinuteSnapshot',
  'firstTwoHourSnapshot',
]) {
  if (!runtimeService.includes(requiredRuntimeSurface)) {
    fail(`Runtime service is missing ${requiredRuntimeSurface}.`)
  }
}

console.log(JSON.stringify({
  ok: true,
  moduleId: descriptor.id,
  blocks: blocks.length,
  items: items.length,
  sectors: sectors.length,
  bodies: bodies.length,
  probes: probes.length,
  routes: routes.length,
  discoveries: discoveries.length,
  salvageSites: salvageSites.length,
  depots: depots.length,
  chapters: chapters.length,
  first30Steps: first30.length,
  first2HourSteps: first2Hours.length,
  surveyArrayRequirements: surveyArrayRequirements.length,
  terminalPages: terminalPages.length,
  lensProfiles: lensProfiles.length,
  holoMapLayers: holoMapLayers.length,
  missions: missions.length,
  releaseGates: releaseGates.length,
  phases: phases.length,
}, null, 2))

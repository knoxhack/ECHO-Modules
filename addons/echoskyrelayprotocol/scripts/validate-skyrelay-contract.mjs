#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const moduleRoot = path.resolve(process.argv.includes('--module-root')
  ? process.argv[process.argv.indexOf('--module-root') + 1]
  : 'addons/echoskyrelayprotocol');

const readJson = (...parts) => JSON.parse(fs.readFileSync(path.join(moduleRoot, ...parts), 'utf8'));
const readText = (...parts) => fs.readFileSync(path.join(moduleRoot, ...parts), 'utf8');
const fail = (message) => {
  throw new Error(message);
};

const blocks = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/content/block_catalog.json').blocks;
const items = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/content/item_catalog.json').items;
const fragments = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/fragments/fragment_catalog.json').fragments;
const anchorRules = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/fragments/anchor_rules.json').rules;
const phases = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/plan/production_phase_matrix.json').phases;
const chapters = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/progression/chapter_catalog.json').chapters;
const first30 = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/progression/first_30_minutes.json').steps;
const first2Hours = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/progression/first_2_hours.json').steps;
const signalCrownRequirements = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/progression/signal_crown_requirements.json').requirements;
const terminalPages = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/integrations/terminal_pages.json').pages;
const lensProfiles = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/integrations/lens_scan_profiles.json').profiles;
const holoMapLayers = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/integrations/holomap_layers.json').layers;
const weatherRoutes = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/integrations/weather_routes.json').routes;
const recoveryBindings = readJson('src/main/resources/data/echoskyrelayprotocol/skyrelay/integrations/recovery_bindings.json').bindings;
const descriptor = readJson('src/main/resources/META-INF/echo.mod.json');
const blockRegistry = readText('src/main/java/com/knoxhack/echoskyrelayprotocol/registry/SkyRelayBlocks.java');
const itemRegistry = readText('src/main/java/com/knoxhack/echoskyrelayprotocol/registry/SkyRelayItems.java');
const nativeModule = readText('src/main/java/com/knoxhack/echoskyrelayprotocol/EchoSkyRelayNativeModule.java');

const ids = (rows, field = 'id') => rows.map((row) => row[field]);
const unique = (rows, label) => {
  const seen = new Set();
  for (const id of rows) {
    if (seen.has(id)) fail(`Duplicate ${label} id: ${id}`);
    seen.add(id);
  }
  return seen;
};

const blockIds = unique(ids(blocks), 'block');
const itemIds = unique(ids(items), 'item');
const fragmentIds = unique(ids(fragments), 'fragment');
const chapterIds = unique(ids(chapters), 'chapter');

if (blocks.length !== 14) fail(`Expected 14 Sky Relay blocks, found ${blocks.length}.`);
if (items.length !== 16) fail(`Expected 16 Sky Relay core items, found ${items.length}.`);
if (fragments.length !== 9) fail(`Expected 9 Sky Relay fragments, found ${fragments.length}.`);
if (phases.length !== 10) fail(`Expected 10 production phases, found ${phases.length}.`);
if (anchorRules.length !== 9) fail(`Expected 9 anchor rules, found ${anchorRules.length}.`);
if (chapters.length !== 12) fail(`Expected 12 progression chapters, found ${chapters.length}.`);
if (first30.length !== 12) fail(`Expected 12 first-30-minute steps, found ${first30.length}.`);
if (first2Hours.length !== 10) fail(`Expected 10 first-2-hour steps, found ${first2Hours.length}.`);
if (signalCrownRequirements.length !== 6) fail(`Expected 6 Signal Crown requirements, found ${signalCrownRequirements.length}.`);
if (terminalPages.length !== 4) fail(`Expected 4 terminal pages, found ${terminalPages.length}.`);
if (lensProfiles.length !== 5) fail(`Expected 5 lens profiles, found ${lensProfiles.length}.`);
if (holoMapLayers.length !== 4) fail(`Expected 4 HoloMap layers, found ${holoMapLayers.length}.`);
if (weatherRoutes.length !== 4) fail(`Expected 4 weather routes, found ${weatherRoutes.length}.`);
if (recoveryBindings.length !== 1) fail(`Expected 1 recovery binding, found ${recoveryBindings.length}.`);

for (const phase of phases) {
  if (!Array.isArray(phase.subphases) || phase.subphases.length !== 5) {
    fail(`Phase ${phase.id} must contain exactly 5 subphases.`);
  }
}

for (const block of blocks) {
  if (!blockRegistry.includes(`"${block.id}"`)) {
    fail(`Block ${block.id} is missing from SkyRelayBlocks.`);
  }
}

for (const item of items) {
  if (!itemRegistry.includes(`"${item.id}"`)) {
    fail(`Item ${item.id} is missing from SkyRelayItems.`);
  }
}

const anchorFragmentIds = unique(ids(anchorRules, 'fragmentId'), 'anchor rule fragment');
for (const fragmentId of fragmentIds) {
  if (!anchorFragmentIds.has(fragmentId)) {
    fail(`Fragment ${fragmentId} is missing an anchor rule.`);
  }
}

for (const rule of anchorRules) {
  if (!fragmentIds.has(rule.fragmentId)) fail(`Anchor rule references unknown fragment ${rule.fragmentId}.`);
  if (!itemIds.has(rule.anchorItem)) fail(`Anchor rule ${rule.fragmentId} references unknown anchor item ${rule.anchorItem}.`);
  if (!blockIds.has(rule.dockingBlock)) fail(`Anchor rule ${rule.fragmentId} references unknown docking block ${rule.dockingBlock}.`);
  if (!chapterIds.has(rule.unlockChapter)) fail(`Anchor rule ${rule.fragmentId} references unknown chapter ${rule.unlockChapter}.`);
  if (!Number.isInteger(rule.stablePowerRequired) || rule.stablePowerRequired < 0) {
    fail(`Anchor rule ${rule.fragmentId} must have a non-negative integer stablePowerRequired.`);
  }
}

for (const step of [...first30, ...first2Hours]) {
  if (!chapterIds.has(step.chapterId)) fail(`Route step ${step.id} references unknown chapter ${step.chapterId}.`);
  if (step.proof?.startsWith('block:') && !blockIds.has(step.proof.slice('block:'.length))) {
    fail(`Route step ${step.id} references unknown block proof ${step.proof}.`);
  }
}

for (const requirement of signalCrownRequirements) {
  if (!Number.isInteger(requirement.requiredCount) || requirement.requiredCount < 1) {
    fail(`Signal Crown requirement ${requirement.id} must have a positive integer requiredCount.`);
  }
  if (requirement.proof?.startsWith('block:') && !blockIds.has(requirement.proof.slice('block:'.length))) {
    fail(`Signal Crown requirement ${requirement.id} references unknown block proof ${requirement.proof}.`);
  }
  if (requirement.proof?.startsWith('item:') && !itemIds.has(requirement.proof.slice('item:'.length))) {
    fail(`Signal Crown requirement ${requirement.id} references unknown item proof ${requirement.proof}.`);
  }
}

for (const profile of lensProfiles) {
  if (!blockIds.has(profile.target)) fail(`Lens profile ${profile.id} references unknown target block ${profile.target}.`);
}

for (const binding of recoveryBindings) {
  if (!blockIds.has(binding.targetBlock)) fail(`Recovery binding ${binding.id} references unknown target block ${binding.targetBlock}.`);
}

for (const required of ['skyrelay.content', 'skyrelay.missions', 'skyrelay.fragments', 'skyrelay.terminal', 'skyrelay.weather_routes']) {
  if (!descriptor.provides.includes(required)) {
    fail(`Descriptor is missing provided namespace ${required}.`);
  }
}

for (const requiredNativeSurface of [
  'SkyRelayFragmentRuntime.adapterManifest()',
  'SkyRelayProgressionRuntime.adapterManifest()',
  'SkyRelaySystemIntegrationContracts.adapterManifest()'
]) {
  if (!nativeModule.includes(requiredNativeSurface)) {
    fail(`Native module is missing ${requiredNativeSurface}.`);
  }
}

console.log(JSON.stringify({
  ok: true,
  moduleId: descriptor.id,
  blocks: blocks.length,
  coreItems: items.length,
  fragments: fragments.length,
  anchorRules: anchorRules.length,
  chapters: chapters.length,
  first30Steps: first30.length,
  first2HourSteps: first2Hours.length,
  terminalPages: terminalPages.length,
  lensProfiles: lensProfiles.length,
  holoMapLayers: holoMapLayers.length,
  weatherRoutes: weatherRoutes.length,
  recoveryBindings: recoveryBindings.length,
  phases: phases.length
}, null, 2));

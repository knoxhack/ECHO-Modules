import fs from 'node:fs'
import path from 'node:path'

const MODULE_ID = 'echoopenlandsprotocol'
const RUNTIME_TARGETS = ['echo_native', 'echo_runtime_standalone', 'neoforge']
const EDITIONS = [
  { id: 'native', repo: 'ECHO-Openlands-Native-Edition' },
  { id: 'neoforge', repo: 'ECHO-Openlands-NeoForge-Edition' },
  { id: 'standalone', repo: 'ECHO-Openlands-Standalone-Edition' },
]

function parseArgs(argv) {
  const args = {
    moduleRoot: null,
    workspaceRoot: null,
    dryRun: false,
    json: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--module-root') args.moduleRoot = argv[++index]
    else if (arg === '--workspace-root') args.workspaceRoot = argv[++index]
    else if (arg === '--dry-run') args.dryRun = true
    else if (arg === '--json') args.json = true
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return args
}

function usage() {
  return [
    'Usage: node scripts/generate-openlands-production-phase-matrix.mjs [--module-root <path>] [--workspace-root <path>]',
    '',
    'Generates data/echoopenlandsprotocol/openlands/progression/production_phase_matrix.json.',
    '',
    'Options:',
    '  --dry-run  Build without writing.',
    '  --json     Print the generated matrix as JSON.',
  ].join('\n')
}

function findModuleRoot(explicitRoot) {
  if (explicitRoot) return path.resolve(explicitRoot)
  let cursor = process.cwd()
  for (;;) {
    const descriptor = path.join(cursor, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fs.existsSync(descriptor)) return cursor
    const candidate = path.join(cursor, 'addons', MODULE_ID, 'src', 'main', 'resources', 'META-INF', 'echo.mod.json')
    if (fs.existsSync(candidate)) return path.join(cursor, 'addons', MODULE_ID)
    const parent = path.dirname(cursor)
    if (parent === cursor) break
    cursor = parent
  }
  throw new Error('Could not find echoopenlandsprotocol module root. Pass --module-root <path>.')
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function moduleFile(filePath, proves) {
  return {
    kind: 'module_file',
    path: filePath,
    proves,
  }
}

function editionFile(editionOrId, filePath, proves) {
  const edition = typeof editionOrId === 'string'
    ? EDITIONS.find((entry) => entry.id === editionOrId)
    : editionOrId
  if (!edition) throw new Error(`Unknown edition: ${editionOrId}`)
  return {
    kind: 'edition_file',
    edition: edition.id,
    path: `${edition.repo}/${filePath}`,
    proves,
  }
}

function editionFiles(filePath, proves) {
  return EDITIONS.map((edition) => editionFile(edition.id, filePath.replace('{edition}', edition.id), proves))
}

function artifactFile(filePath, proves) {
  return {
    kind: 'artifact_file',
    path: filePath,
    proves,
  }
}

function runtimeGate(id, proves) {
  return {
    kind: 'runtime_gate',
    id,
    proves,
  }
}

function phase(id, order, displayName, objective, checkpoints) {
  return {
    id,
    order,
    displayName,
    objective,
    checkpointCount: checkpoints.length,
    checkpoints,
  }
}

function checkpoint(id, order, title, requirement, state, evidence, acceptance) {
  return {
    id,
    order,
    title,
    requirement,
    currentState: state,
    runtimeParity: RUNTIME_TARGETS,
    evidence,
    acceptance,
  }
}

function evidenceExists(evidence, moduleRoot, workspaceRoot) {
  if (evidence.kind === 'module_file') return fs.existsSync(path.join(moduleRoot, evidence.path))
  if (evidence.kind === 'edition_file') return fs.existsSync(path.join(workspaceRoot, evidence.path))
  if (evidence.kind === 'artifact_file') return fs.existsSync(path.join(workspaceRoot, 'ECHO-Modules', evidence.path))
  return evidence.kind === 'runtime_gate'
}

function annotateEvidence(phases, moduleRoot, workspaceRoot) {
  return phases.map((entry) => ({
    ...entry,
    checkpoints: entry.checkpoints.map((item) => ({
      ...item,
      evidence: item.evidence.map((evidence) => ({
        ...evidence,
        present: evidenceExists(evidence, moduleRoot, workspaceRoot),
      })),
    })),
  }))
}

function buildMatrix(moduleRoot, workspaceRoot) {
  const dataRoot = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands')
  const blocks = readJson(path.join(dataRoot, 'blocks', 'mvp_blocks.json')).blocks ?? []
  const items = readJson(path.join(dataRoot, 'items', 'mvp_items.json')).items ?? []
  const recipes = readJson(path.join(dataRoot, 'recipes', 'mvp_recipes.json')).recipes ?? []
  const biomes = readJson(path.join(dataRoot, 'biomes', 'mvp_biomes.json')).biomes ?? []
  const structures = readJson(path.join(dataRoot, 'structures', 'mvp_landmarks.json')).landmarks ?? []
  const creatures = readJson(path.join(dataRoot, 'creatures', 'mvp_creatures.json')).creatures ?? []
  const launchRoadmap = readJson(path.join(dataRoot, 'progression', 'launch_roadmap.json'))

  const phases = [
    phase(
      'phase_01_product_contract',
      1,
      'Product Contract',
      'Locks Openlands identity, relaxed default rules, legal boundaries, modes, and MVP player promise.',
      [
        checkpoint(
          'official_pack_identity',
          1,
          'Official ECHO Pack #2 identity',
          'Lock namespace echoopenlandsprotocol, public name Openlands, and target identity: relaxed building, farming, exploration, old roads, and waystones.',
          'contract_ready',
          [
            moduleFile('README.md', 'Public module identity and relaxed product positioning.'),
            moduleFile('src/main/resources/META-INF/echo.mod.json', 'Official pack descriptor, namespace, version, runtime targets, and provided contracts.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/index/openlands_overview.json', 'Openlands public name, official pack position, and index categories.'),
          ],
          ['Descriptor id is echoopenlandsprotocol.', 'Overview publicName is Openlands.', 'Positioning includes old roads and waystones.']
        ),
        checkpoint(
          'standard_relaxed_default',
          2,
          'Relaxed Standard config',
          'Define openlands_standard.json behavior: gentle hunger, no stamina, no hydration, no food spoilage, no temperature damage, recoverable death pack, and moderate hostiles.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/config/game_modes.json', 'Openlands Standard default and disabled hardcore meters.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json', 'Runtime hooks that prove Standard rules and hardcore meters off.'),
            moduleFile('src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsStandardRules.java', 'Pure Java relaxed rule contract used by adapters.'),
          ],
          ['Default mode remains openlands_standard.', 'Hardcore meters are disabled outside Hardlands.', 'Death recovery remains recoverable.']
        ),
        checkpoint(
          'mode_overlays',
          3,
          'Mode overlays',
          'Define peaceful, explorer, builder_survival, creative, and hardlands as config overlays; only hardlands enables harsher systems.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/config/game_modes.json', 'Mode overlay list and per-mode hardcore flag settings.'),
            moduleFile('src/main/resources/META-INF/echo.mod.json', 'Declared gameModes exposed to runtime adapters.'),
          ],
          ['Six Openlands modes are declared.', 'Only hardlands enables any hardcore meter.', 'Creative and Builder Survival remain non-hardcore.']
        ),
        checkpoint(
          'legal_content_bible',
          4,
          'Original content boundary',
          'Keep Openlands free of Minecraft names, copied mob silhouettes, copied recipes as identity, copied textures, borrowed audio, and Minecraft branding.',
          'preflight_ready_human_review_required',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/config/content_policy.json', 'Public naming, asset, recipe, and branding policy.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/legal_content_audit.json', 'Machine-readable legal audit scope and forbidden terms.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/final_release_review_acceptance.json', 'Final human art/audio/legal review schema and blocked report contract.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/final_release_review_harness_plan.json', 'Final review harness plan with exact review areas, drivers, captures, and saved artifacts.'),
            ...editionFiles('evidence/{edition}-legal-content-audit.json', 'Edition legal preflight scans public names, ids, artifacts, and descriptors.'),
            ...editionFiles('evidence/{edition}-final-release-review-report.json', 'Edition final release review blocked report until human review passes.'),
            runtimeGate('final_asset_legal_review', 'Final human art/audio/legal approval is still required before public release.'),
          ],
          ['No forbidden public terms appear in validated Openlands text.', 'Placeholder assets are marked non-final.', 'Public release remains blocked until human review.']
        ),
        checkpoint(
          'mvp_player_promise',
          5,
          'MVP player promise',
          'Freeze the route: spawn, gather, build shelter, sleep, explore, restore first waystone, reveal map.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/progression/first_hour_route.json', 'Canonical first-hour route and player promise.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json', 'Machine-readable first-hour acceptance fixture.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/index/openlands_overview.json', 'Player-facing MVP acceptance list.'),
          ],
          ['Route includes safe spawn, first gathering, tools, shelter, sleep, exploration hook, and waystone.', 'Save/load fields are part of the promise.']
        ),
      ]
    ),
    phase(
      'phase_02_repo_and_artifact_setup',
      2,
      'Repo And Artifact Setup',
      'Creates addon structure, edition repos, manifests, docs, and artifact layout for all target runtimes.',
      [
        checkpoint(
          'addon_module_scaffold',
          1,
          'Addon module scaffold',
          'Create ECHO-Modules/addons/echoopenlandsprotocol with README, build.gradle, docs, resources, and templates.',
          'contract_ready',
          [
            moduleFile('README.md', 'Module overview and validation commands.'),
            moduleFile('build.gradle', 'Module build wiring.'),
            moduleFile('docs/artifacts.md', 'Artifact policy and release gate documentation.'),
            moduleFile('src/main/resources', 'Common resource root.'),
            moduleFile('src/main/templates', 'Runtime-specific template root.'),
          ],
          ['Module root exists under addons/echoopenlandsprotocol.', 'Common resources and templates are present.']
        ),
        checkpoint(
          'echo_descriptor',
          2,
          'Echo descriptor',
          'Add META-INF/echo.mod.json declaring echo_native, echo_runtime_standalone, and neoforge with version 0.1.0.',
          'contract_ready',
          [
            moduleFile('src/main/resources/META-INF/echo.mod.json', 'Canonical descriptor for the common addon.'),
            moduleFile('src/main/java/com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.java', 'Java adapter contract exposes runtime targets and artifact targets.'),
          ],
          ['Descriptor version is 0.1.0.', 'All three runtime targets are declared.', 'Openlands provides index, systems, world, recipes, blocks, items, and waystones.']
        ),
        checkpoint(
          'neoforge_template',
          3,
          'NeoForge template',
          'Add neoforge.mods.toml only for the NeoForge artifact family while keeping common IDs in Echo-owned files.',
          'contract_ready',
          [
            moduleFile('src/main/templates/META-INF/neoforge.mods.toml', 'NeoForge-only descriptor template.'),
            moduleFile('src/main/resources/META-INF/echo.mod.json', 'Echo-owned common descriptor remains source of truth.'),
          ],
          ['NeoForge template exists under templates.', 'Common ids remain in Echo descriptor and data.']
        ),
        checkpoint(
          'edition_repos',
          4,
          'Edition repositories',
          'Create thin Native, NeoForge, and Standalone edition repos.',
          'preflight_ready',
          [
            ...EDITIONS.map((edition) => editionFile(edition.id, 'README.md', 'Thin edition repo root exists and describes its target runtime.')),
            ...editionFiles('release-manifest.template.json', 'Edition release manifest template exists.'),
          ],
          ['Native edition repo exists.', 'NeoForge edition repo exists.', 'Standalone edition repo exists.', 'Edition repos stay thin and point back to Echo data.']
        ),
        checkpoint(
          'edition_docs',
          5,
          'Edition docs and rollback flow',
          'Each edition repo gets install, update, rollback, troubleshooting, and module requirements documentation.',
          'preflight_ready',
          [
            ...editionFiles('docs/install.md', 'Edition install flow documentation.'),
            ...editionFiles('docs/update-flow.md', 'Edition update flow documentation.'),
            ...editionFiles('docs/rollback.md', 'Edition rollback documentation.'),
            ...editionFiles('docs/troubleshooting.md', 'Edition troubleshooting documentation.'),
            ...editionFiles('docs/module-requirements.md', 'Edition module requirements documentation.'),
          ],
          ['All required edition docs exist for each runtime target.', 'Rollback and troubleshooting are present before public alpha.']
        ),
      ]
    ),
    phase(
      'phase_03_data_and_schema_layout',
      3,
      'Data And Schema Layout',
      'Defines content roots, asset roots, required fields, and conformance fixtures for cross-runtime parity.',
      [
        checkpoint(
          'content_roots',
          1,
          'Content roots',
          'Add content roots for blocks, items, recipes, loot, tags, biomes, structures, creatures, waystones, progression, tutorials, index, holomap, and sounds.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json', 'Required content root fixture.'),
            moduleFile('src/main/java/com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.java', 'REQUIRED_CONTENT_ROOTS adapter contract.'),
          ],
          ['Every required content root exists on disk.', 'Runtime contract lists the same roots.']
        ),
        checkpoint(
          'asset_roots',
          2,
          'Asset roots',
          'Add textures, models, blockstates, lang, and sounds asset roots.',
          'placeholder_ready_final_art_blocked',
          [
            moduleFile('src/main/resources/assets/echoopenlandsprotocol/asset_manifest.json', 'Owned placeholder coverage policy and manifest.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/final_release_review_acceptance.json', 'Final art/audio review schema and blocked report contract.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/final_release_review_harness_plan.json', 'Final art/audio review harness plan with asset, block, item, audio, and generated-output drivers.'),
            moduleFile('src/main/resources/assets/echoopenlandsprotocol/lang/en_us.json', 'Language strings for MVP content.'),
            moduleFile('src/main/resources/assets/echoopenlandsprotocol/sounds.json', 'Sound event manifest.'),
            ...editionFiles('evidence/{edition}-final-release-review-report.json', 'Edition final art/audio review blocked report until final assets pass.'),
            runtimeGate('final_art_audio_pass', 'Final public art and audio must replace placeholders before release.'),
          ],
          ['Every MVP block and item has placeholder asset coverage.', 'Public release remains blocked until final art/audio review.']
        ),
        checkpoint(
          'block_schema_fields',
          3,
          'Block schema fields',
          'Require id, displayName, hardness, tool, drops, tags, model, texture, and runtimeParity for every block.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json', 'Canonical block registry.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json', 'Block conformance IDs and required content counts.'),
          ],
          ['All block entries include required fields.', `Current MVP block count is ${blocks.length}.`]
        ),
        checkpoint(
          'item_schema_fields',
          4,
          'Item schema fields',
          'Require id, displayName, stackSize, useType, tags, model, texture, recipeRefs, and runtimeParity for every item.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/items/mvp_items.json', 'Canonical item registry.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json', 'Item conformance IDs and required content counts.'),
          ],
          ['All item entries include required fields.', `Current MVP item count is ${items.length}.`]
        ),
        checkpoint(
          'conformance_fixture',
          5,
          'Cross-runtime conformance fixture',
          'Add openlands_mvp_registry.json listing every MVP block, item, recipe, biome, creature, and system expected on Native, Standalone, and NeoForge.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json', 'Canonical conformance fixture.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/cross_platform_parity.json', 'Parity surfaces and test fixture list.'),
            ...editionFiles('evidence/{edition}-registry-parity-report.json', 'Edition registry parity reports generated from the same fixture.'),
          ],
          ['Conformance fixture includes blocks, items, recipes, biomes, creatures, and system contracts.', 'Edition reports use the same expected counts.']
        ),
      ]
    ),
    phase(
      'phase_04_mvp_block_registry',
      4,
      'MVP Block Registry',
      'Defines terrain, stone, ore, wood, utility, old-road, and waystone blocks for the MVP loop.',
      [
        checkpoint(
          'terrain_blocks',
          1,
          'Terrain blocks',
          'Define meadow_grass_block, forest_soil, dry_soil, mud, sand, clay, and gravel with tools, drops, textures, lang keys, and biome placement.',
          'contract_ready',
          [moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json', 'Terrain block registry entries and placement metadata.')],
          ['Terrain block IDs exist.', 'Terrain drops and biome placement resolve.']
        ),
        checkpoint(
          'stone_blocks',
          2,
          'Stone and masonry blocks',
          'Define fieldstone, limestone, granite, shale, deepstone, and planned slab/stair/wall/brick variants.',
          'contract_ready',
          [moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json', 'Stone, masonry, and planned variant metadata.')],
          ['Stone block IDs exist.', 'Building palette variants are represented or planned.']
        ),
        checkpoint(
          'ore_blocks',
          3,
          'Ore blocks',
          'Define copper_ore, tin_ore, iron_ore, and glow_crystal_cluster with pick tiers, drops, spawn layers, and reward behavior.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json', 'Ore block entries.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/loot/mvp_loot.json', 'Ore drop tables.'),
          ],
          ['Ore blocks exist.', 'Required tool tiers and drops resolve.', 'Ore placement supports first-metal progression.']
        ),
        checkpoint(
          'wood_blocks',
          4,
          'Wood families',
          'Define branchwood and pine logs, planks, beams, and posts sharing openlands wood-family tags.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json', 'Branchwood and pine block families.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/tags/mvp_tags.json', 'openlands:wood_family tag membership.'),
          ],
          ['Branchwood and pine families exist.', 'Wood-family tags resolve.']
        ),
        checkpoint(
          'utility_blocks',
          5,
          'Utility and station blocks',
          'Define doors, trapdoors, ladders, storage, signs, bedroll, lights, workbench, kiln, forge, loom, cookpot, mason table, and map table.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json', 'Utility and station block entries.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json', 'Station recipe assignments.'),
            ...editionFiles('evidence/{edition}-crafting-station-report.json', 'Edition station preflight coverage.'),
          ],
          ['Station blocks exist.', 'Crafting station recipes are assigned.', 'Edition station reports prove compiled artifact coverage.']
        ),
      ]
    ),
    phase(
      'phase_05_mvp_item_registry',
      5,
      'MVP Item Registry',
      'Defines raw materials, metals, food, tools, utility, and waystone items for early progression.',
      [
        checkpoint(
          'raw_material_items',
          1,
          'Raw materials',
          'Define branchwood sticks, stone pieces, reed fiber, flint, clay, hide, bone, pitch, resin, and charcoal.',
          'contract_ready',
          [moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/items/mvp_items.json', 'Raw material item registry entries.')],
          ['Starter raw materials exist.', 'Raw material acquisition appears in gameplay catalog.']
        ),
        checkpoint(
          'metal_items',
          2,
          'Metals and crystals',
          'Define ore chunks, copper, tin, bronze, iron ingots, and glow crystal.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/items/mvp_items.json', 'Metal and crystal item registry entries.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json', 'Forge processing recipes.'),
          ],
          ['Metal progression items exist.', 'Copper plus tin unlocks bronze.']
        ),
        checkpoint(
          'food_items',
          3,
          'Gentle food loop',
          'Define berries, mushroom, meat, grain, root crop, fish, and stew with non-punishing hunger values.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/items/mvp_items.json', 'Food item entries and nutrition.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/homestead_alpha.json', 'Relaxed crops and cookpot meals.'),
          ],
          ['Food exists without spoilage pressure.', 'Homestead food loop remains relaxed in Standard.']
        ),
        checkpoint(
          'tool_items',
          4,
          'Tool progression',
          'Define crude, flint, wooden, copper, bronze, and iron tools for starter gathering and mining tiers.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/items/mvp_items.json', 'Tool item registry entries and stats.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json', 'Tool crafting recipes.'),
          ],
          ['Starter tools exist.', 'Metal picks exist.', 'Tool tiers match ore requirements.']
        ),
        checkpoint(
          'utility_waystone_items',
          5,
          'Utility and waystone items',
          'Define bedroll, pack, torch bundle, repair kit, copper fitting, waystone core, region rubbing, and old road token.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/items/mvp_items.json', 'Utility and waystone item entries.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json', 'Waystone repair inputs and route items.'),
          ],
          ['Waystone repair inputs exist.', 'Map and road items exist.', 'Utility items support first-hour recovery.']
        ),
      ]
    ),
    phase(
      'phase_06_crafting_and_stations',
      6,
      'Crafting And Stations',
      'Builds handcrafting, workbench, kiln, forge, cookpot, and map-table recipe systems.',
      [
        checkpoint(
          'handcrafting_recipes',
          1,
          'Handcrafting recipes',
          'Define crude tools, flint knife, torch bundle, campfire, and basic fiber binding.',
          'contract_ready',
          [moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json', 'Handcrafting recipe group.')],
          ['Handcrafting recipes exist.', 'Starter tools and campfire are craftable without a station.']
        ),
        checkpoint(
          'workbench_recipes',
          2,
          'Workbench recipes',
          'Define wood processing, building pieces, storage, signs, hammer, and starter combat/hunting tools.',
          'contract_ready',
          [moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json', 'Workbench recipe group.')],
          ['Workbench recipes exist.', 'Builder palette unlocks from starter wood.']
        ),
        checkpoint(
          'kiln_recipes',
          3,
          'Kiln recipes',
          'Define raw clay to brick, sand to glass, log to charcoal, and pottery hooks.',
          'contract_ready',
          [moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json', 'Kiln recipe group.')],
          ['Kiln recipes exist.', 'Brick, glass, and charcoal processing are represented.']
        ),
        checkpoint(
          'forge_recipes',
          4,
          'Forge recipes',
          'Define ore chunk processing, bronze alloying, metal tools, copper fitting, and lantern frame support.',
          'contract_ready',
          [moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json', 'Forge recipe group.')],
          ['Forge recipes exist.', 'Copper, tin, bronze, and iron progression resolves.']
        ),
        checkpoint(
          'map_table_recipes',
          5,
          'Map table recipes',
          'Define region rubbing, waystone core, route binding, and restored-waystone activation records.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json', 'Map-table route and waystone recipes.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json', 'Waystone state machine consumes map-table outputs.'),
          ],
          ['Map-table recipes exist.', 'Route binding feeds the waystone active-state path.']
        ),
      ]
    ),
    phase(
      'phase_07_first_hour_gameplay',
      7,
      'First-Hour Gameplay',
      'Designs relaxed early gameplay: spawn safety, discovery prompts, shelter scoring, sleep milestone, and save/load acceptance.',
      [
        checkpoint(
          'starter_spawn_generator',
          1,
          'Starter spawn guarantees',
          'Guarantee nearby trees, loose stones, fiber, berries, water, and a cave, road, or ruin hook.',
          'runtime_core_ready_adapter_execution_pending',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json', 'Starter spawn validation contract.'),
            moduleFile('src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsStarterSpawnGuarantees.java', 'Pure Java starter spawn validation.'),
            ...editionFiles('evidence/{edition}-worldgen-exploration-report.json', 'Edition worldgen preflight coverage.'),
            runtimeGate('real_worldgen_spawn_execution', 'Each adapter still needs live worldgen execution evidence.'),
          ],
          ['Valid starter spawn passes the shared runtime core.', 'Invalid starter spawn reports missing signals.', 'Real adapters still need live execution.']
        ),
        checkpoint(
          'discovery_tutorials',
          2,
          'Discovery tutorial prompts',
          'Trigger prompts only on discovery: first stick, first stone, first fiber, first tool craft, campfire, and shelter score.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/tutorials/first_hour_prompts.json', 'Discovery-only tutorial prompt rules.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/progression/first_hour_route.json', 'Route hooks that trigger tutorials.'),
          ],
          ['Prompts are discovery-based.', 'Prompt IDs referenced by route steps resolve.']
        ),
        checkpoint(
          'shelter_score',
          3,
          'Forgiving shelter score',
          'Calculate roof, walls, door, bedroll, light/fire, and hostile distance without requiring perfect construction.',
          'runtime_core_ready_adapter_execution_pending',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json', 'Shelter scoring components and minimum score.'),
            moduleFile('src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsShelterScoring.java', 'Pure Java shelter scoring implementation.'),
            runtimeGate('real_adapter_shelter_execution', 'Each adapter still needs live placed-block shelter checks.'),
          ],
          ['Shelter score minimum remains forgiving.', 'Shared runtime core allows sleep at minimum score.', 'Adapters still need live block-state evidence.']
        ),
        checkpoint(
          'sleep_milestone',
          4,
          'Sleep milestone',
          'Complete the first milestone when the player sleeps in a good-enough shelter, not when every block is perfect.',
          'runtime_core_ready_adapter_execution_pending',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/progression/first_hour_route.json', 'Sleep-and-recover route step.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json', 'First-hour shelter acceptance scenario.'),
            ...editionFiles('evidence/{edition}-first-hour-playtest-report.json', 'Edition first-hour preflight report.'),
            runtimeGate('real_first_hour_playtest', 'Each adapter still needs live first-hour playtest execution.'),
          ],
          ['Sleep milestone is tied to relaxed shelter threshold.', 'Preflight reports align route and scenarios.', 'Live runtime playtest remains required.']
        ),
        checkpoint(
          'first_hour_save_load',
          5,
          'First-hour save/load',
          'Persist inventory, hotbar, placed blocks, chest contents, bedroll spawn, campfire state, shelter score, and route state.',
          'runtime_core_ready_adapter_execution_pending',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json', 'Save/load checkpoints.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/playable_runtime_contract.json', 'Save/load snapshot fields.'),
            ...editionFiles('evidence/{edition}-first-hour-playtest-report.json', 'Edition first-hour save/load preflight report.'),
            runtimeGate('real_save_load_execution', 'Each adapter still needs live save/reload execution evidence.'),
          ],
          ['All required first-hour save fields are declared.', 'Edition preflights prove fixture alignment.', 'Real save/load remains a Public Alpha blocker.']
        ),
      ]
    ),
    phase(
      'phase_08_worldgen_and_exploration',
      8,
      'Worldgen And Exploration',
      'Adds starter biomes, resources, landmarks, ambience, creature spawns, and HoloMap discovery.',
      [
        checkpoint(
          'mvp_biomes',
          1,
          'MVP biomes',
          'Define Meadows, Woodlands, Stonehills, and Marshlands with palettes, resources, spawn tables, ambience, and landmark frequency.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/biomes/mvp_biomes.json', 'Four MVP biome definitions.'),
            ...editionFiles('evidence/{edition}-worldgen-exploration-report.json', 'Edition biome/worldgen preflight report.'),
          ],
          [`Current MVP biome count is ${biomes.length}.`, 'Biome palettes, resources, spawns, and ambience resolve.']
        ),
        checkpoint(
          'biome_resource_identity',
          2,
          'Biome resource identity',
          'Make Meadows starter-safe, Woodlands resin/mushroom-rich, Stonehills mineral-rich, and Marshlands reed/clay-rich.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/biomes/mvp_biomes.json', 'Biome resource sets and spawn tables.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json', 'Creature biome references.'),
          ],
          ['Each biome has a distinct resource role.', 'Starter biomes support safe first-hour gathering.']
        ),
        checkpoint(
          'landmarks',
          3,
          'Exploration landmarks',
          'Generate ruined wells, road markers, tiny camps, watchtowers, old mines, broken bridges, cellar entrances, and waystones.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/structures/mvp_landmarks.json', 'Landmark definitions.'),
            ...editionFiles('evidence/{edition}-worldgen-exploration-report.json', 'Edition landmark preflight report.'),
          ],
          [`Current MVP landmark count is ${structures.length}.`, 'Landmark blocks and HoloMap hints resolve.']
        ),
        checkpoint(
          'creature_spawns_and_ambience',
          4,
          'Creature spawns and ambience',
          'Bind passive, neutral, and moderate hostile creatures with sound keys and safe starter distances.',
          'preflight_ready_runtime_execution_pending',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json', 'Creature spawn, AI, drops, and sounds.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/sounds/mvp_sound_contract.json', 'Creature and ambience sound contract.'),
            ...editionFiles('evidence/{edition}-creature-roster-report.json', 'Edition creature roster preflight report.'),
            runtimeGate('real_creature_runtime_execution', 'Adapters still need live creature spawn/AI/drop tests.'),
          ],
          [`Current MVP creature count is ${creatures.length}.`, 'Creature sound keys resolve.', 'Live runtime creature behavior remains required.']
        ),
        checkpoint(
          'holomap_region_data',
          5,
          'HoloMap region discovery',
          'Store discovered region name, biome type, nearby hints, restored waystones, player markers, and old road segments.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/holomap/mvp_regions.json', 'HoloMap region and layer contracts.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/playtests/mvp_first_hour_acceptance.json', 'HoloMap first-waystone acceptance.'),
          ],
          ['HoloMap layers and hint types exist.', 'Waystone reveal fields are included in save/load acceptance.']
        ),
      ]
    ),
    phase(
      'phase_09_waystones_and_old_roads',
      9,
      'Waystones And Old Roads',
      'Implements old roads, waystone repair progression, map reveal, fast travel, and multiplayer permissions.',
      [
        checkpoint(
          'old_road_blocks',
          1,
          'Old road blocks',
          'Add old_road_block, old_road_marker, broken_waystone, restored_waystone, and waystone_plinth.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/blocks/mvp_blocks.json', 'Old road and waystone block entries.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json', 'Waystone block contract.'),
          ],
          ['Old road and waystone blocks exist.', 'Block IDs appear in waystone contract.']
        ),
        checkpoint(
          'waystone_state_machine',
          2,
          'Waystone state machine',
          'Implement undiscovered -> discovered -> debris_cleared -> stone_repaired -> fitted -> charged -> bound -> active.',
          'runtime_core_ready_adapter_execution_pending',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json', 'Canonical state machine order.'),
            moduleFile('src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsWaystoneRuntime.java', 'Pure Java waystone transition implementation.'),
            ...editionFiles('evidence/{edition}-waystone-save-load-report.json', 'Edition waystone save/load preflight report.'),
            runtimeGate('real_waystone_save_load_execution', 'Adapters still need live waystone save/load execution.'),
          ],
          ['State order has eight states.', 'Shared runtime can advance to active.', 'Live adapter persistence remains required.']
        ),
        checkpoint(
          'repair_inputs',
          3,
          'Repair inputs',
          'Require fieldstone or limestone, repair kit, copper fitting, glow crystal, and map-table route binding.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json', 'Waystone repair input map.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/recipes/mvp_recipes.json', 'Waystone component and route recipes.'),
          ],
          ['Repair inputs resolve to known blocks/items.', 'Route binding is produced by map table.']
        ),
        checkpoint(
          'active_waystone_effects',
          4,
          'Active waystone effects',
          'Reveal map radius, name region, mark nearby hints, attract trader visits, and unlock fast travel after two active stones.',
          'runtime_core_ready_adapter_execution_pending',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json', 'Active waystone effects and fast-travel threshold.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/holomap/mvp_regions.json', 'HoloMap reveal data.'),
            runtimeGate('real_fast_travel_execution', 'Adapters still need live route binding and travel tests.'),
          ],
          ['Fast travel requires two active stones.', 'HoloMap reveal fields exist.', 'Real travel execution remains required.']
        ),
        checkpoint(
          'multiplayer_permissions',
          5,
          'Multiplayer waystone permissions',
          'Store owner/group/public flags, contributors, linked route IDs, rename permission, and public-travel state.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/waystones/waystone_contract.json', 'Waystone multiplayer state fields.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/coop_and_smp.json', 'Shared state, permissions, and network event policy.'),
          ],
          ['Permission fields exist.', 'Co-op/SMP contract defines authority and mutation events.']
        ),
      ]
    ),
    phase(
      'phase_10_alpha_systems_and_distribution',
      10,
      'Alpha Systems And Distribution',
      'Expands homestead, creatures, builder UX, artifacts, release validation, repair, rollback, and parity tests.',
      [
        checkpoint(
          'homestead_systems',
          1,
          'Homestead systems',
          'Add grain, root crops, berries, compost, simple watering, cookpot meals, animal pens, and trader surplus demand.',
          'runtime_core_ready_adapter_execution_pending',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/homestead_alpha.json', 'Homestead alpha contracts.'),
            moduleFile('src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsHomesteadRuntime.java', 'Pure Java crop and cookpot runtime helpers.'),
            ...editionFiles('evidence/{edition}-alpha-systems-report.json', 'Edition alpha-systems preflight report.'),
            runtimeGate('real_homestead_runtime_execution', 'Adapters still need live crop, cookpot, pen, and trader execution.'),
          ],
          ['Homestead contracts exist.', 'Standard mode remains relaxed.', 'Live adapter execution remains required.']
        ),
        checkpoint(
          'creature_roster',
          2,
          'Creature roster',
          'Add hare, deer, boar, goat, marsh hen, fish, greyling, bristleback, hollow stalker, and mire leech with spawn rules, drops, sounds, and AI hints.',
          'preflight_ready_runtime_execution_pending',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/creatures/mvp_creatures.json', 'MVP creature roster.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/loot/mvp_loot.json', 'Creature drops.'),
            ...editionFiles('evidence/{edition}-creature-roster-report.json', 'Edition creature roster preflight report.'),
            runtimeGate('real_creature_runtime_execution', 'Adapters still need live creature tests.'),
          ],
          ['Ten creature definitions exist.', 'Drops and sounds resolve.', 'Live AI/spawn execution remains required.']
        ),
        checkpoint(
          'builder_ux',
          3,
          'Builder UX',
          'Add hammer shape cycling, scaffold, quick stack, quick deposit, sorting, named chests, and craft from nearby storage.',
          'runtime_core_ready_adapter_execution_pending',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/builder_ux_alpha.json', 'Builder UX commands and storage behavior.'),
            moduleFile('src/main/java/com/knoxhack/echoopenlandsprotocol/runtime/OpenlandsBuilderUxRuntime.java', 'Pure Java builder action validation.'),
            ...editionFiles('evidence/{edition}-alpha-systems-report.json', 'Edition builder UX preflight report.'),
            runtimeGate('real_builder_ux_execution', 'Adapters still need live inventory/storage mutation tests.'),
          ],
          ['Builder commands exist.', 'Server authority and permissions are modeled.', 'Live UX execution remains required.']
        ),
        checkpoint(
          'artifact_outputs',
          4,
          'Runtime artifacts',
          'Produce .echo-addon, standalone jar, NeoForge jar, and sources jar artifacts for version 0.1.0.',
          'local_artifacts_ready_upload_blocked',
          [
            artifactFile('dist/echo-module-release/echoopenlandsprotocol/echoopenlandsprotocol-0.1.0.echo-addon', 'Local Native echo-addon artifact.'),
            artifactFile('dist/echo-module-release/echoopenlandsprotocol/echoopenlandsprotocol-0.1.0-standalone.jar', 'Local Standalone artifact.'),
            artifactFile('dist/echo-module-release/echoopenlandsprotocol/echoopenlandsprotocol-0.1.0-neoforge.jar', 'Local NeoForge artifact.'),
            artifactFile('dist/echo-module-release/echoopenlandsprotocol/echoopenlandsprotocol-0.1.0-sources.jar', 'Local sources artifact.'),
            runtimeGate('uploaded_release_artifacts', 'Artifacts still need uploaded URLs and approved Release Index entries.'),
          ],
          ['Local artifact files exist.', 'Release upload remains blocked until URLs and checksums are approved.']
        ),
        checkpoint(
          'release_validation',
          5,
          'Release validation',
          'Release only after launcher install, update, repair, rollback, Release Index validation, and parity tests pass for all three editions.',
          'preflight_ready_real_launcher_blocked',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json', 'Distribution gate contract.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/launcher_flow_acceptance.json', 'Launcher flow acceptance fixture.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/runtime_execution_acceptance.json', 'Real runtime execution report schema for clearing Public Alpha gates.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/runtime_execution_harness_plan.json', 'Real runtime harness driver plan for actions, assertions, captures, and saved artifacts.'),
            moduleFile('scripts/generate-openlands-local-runtime-rehearsal-report.mjs', 'Local runtime rehearsal generator for pure-runtime scenario mapping, fixture checks, and saved artifacts without clearing real adapter gates.'),
            moduleFile('scripts/validate-openlands-local-runtime-rehearsal-report.mjs', 'Validator for local runtime rehearsal reports across all runtime execution scenarios.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/harness_driver_manifest_contract.json', 'Edition-owned driver manifest contract listing required, available, and missing real harness driver surfaces.'),
            moduleFile('scripts/generate-openlands-harness-driver-manifest.mjs', 'Generator that derives edition harness driver manifests from the source contract and optional implemented driver declarations.'),
            moduleFile('scripts/validate-openlands-harness-driver-manifest.mjs', 'Validator for template, partial, and ready harness driver manifests before harness runners consume them.'),
            ...editionFiles('evidence/{edition}-harness-driver-manifest.template.json', 'Edition driver manifest template records missing runtime, launcher, final-review, and distribution harness drivers.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/launcher_execution_acceptance.json', 'Real launcher execution report schema for install, update, repair, rollback, and preservation gates.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/launcher_execution_harness_plan.json', 'Real launcher harness driver plan for install, update, repair, rollback, and preservation flows.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/final_release_review_harness_plan.json', 'Real final review harness driver plan for public identity, assets, audio, and generated runtime output review.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/distribution_approval_acceptance.json', 'Real distribution approval report schema for uploads, indexed manifests, co-op session, and approval signature.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/distribution_approval_harness_plan.json', 'Real distribution approval harness driver plan for Release Index publication, artifact verification, manifest indexing, co-op evidence, dependency gates, rollback, and approval signoff.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/release_publication_manifest_contract.json', 'Publication manifest contract for public download URLs, download verification, Release Index patching, and approval handoff.'),
            moduleFile('scripts/verify-openlands-release-publication-downloads.mjs', 'Public artifact download verifier that downloads uploaded URLs, checks SHA-256 and size, and writes a verified publication manifest without approving Release Index patches.'),
            moduleFile('scripts/approve-openlands-release-publication.mjs', 'Guarded publication approval tool that requires verified downloads, approval/signoff JSON, and explicit Release Index patch intent before writing an approved manifest.'),
            moduleFile('scripts/generate-openlands-release-publication-rehearsal-report.mjs', 'Module-level release publication rehearsal generator for local artifact download-back verification and Release Index patch-preview evidence without publishing public URLs.'),
            moduleFile('scripts/validate-openlands-release-publication-rehearsal-report.mjs', 'Validator for the module-level release publication rehearsal report and saved local download-back artifacts.'),
            moduleFile('scripts/generate-openlands-edition-manifest-index-preview.mjs', 'Module-level edition manifest index preview generator for three modpack entries, module requirement resolution, and launcher channel listing saved artifacts without clearing distribution gates.'),
            moduleFile('scripts/validate-openlands-edition-manifest-index-preview.mjs', 'Validator for the module-level edition manifest index preview and saved manifest indexing artifacts.'),
            moduleFile('scripts/generate-openlands-local-launcher-rehearsal-report.mjs', 'Local launcher rehearsal generator for cache, update, repair, rollback, and preservation mechanics without clearing real launcher gates.'),
            moduleFile('scripts/validate-openlands-local-launcher-rehearsal-report.mjs', 'Validator for local launcher rehearsal reports and saved install/update/repair/rollback artifacts.'),
            artifactFile('dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-manifest.template.json', 'Blocked publication manifest template generated from local artifact hashes and sizes.'),
            artifactFile('dist/echo-module-release/echoopenlandsprotocol/openlands-release-publication-rehearsal-report.json', 'Local publication rehearsal report proving local download-back hashes and Release Index patch previews without clearing public URL blockers.'),
            artifactFile('dist/echo-module-release/echoopenlandsprotocol/openlands-edition-manifest-index-preview.json', 'Edition manifest index preview proving the three edition manifests, module requirement graph, and launcher channel listing without clearing distribution gates.'),
            ...editionFiles('evidence/{edition}-local-runtime-rehearsal-report.json', 'Edition local runtime rehearsal proves all runtime execution scenarios are mapped to fixtures, pure runtime hooks, and saved preflight artifacts without clearing real adapter gates.'),
            ...editionFiles('evidence/{edition}-launcher-flow-report.json', 'Edition launcher-flow preflight report.'),
            ...editionFiles('evidence/{edition}-launcher-execution-report.json', 'Edition launcher execution blocked report until real launcher runs pass.'),
            ...editionFiles('evidence/{edition}-local-launcher-rehearsal-report.json', 'Edition local launcher rehearsal proves cache, update, repair, rollback, and state preservation mechanics without clearing real launcher gates.'),
            ...editionFiles('evidence/{edition}-distribution-roadmap-report.json', 'Edition distribution/roadmap preflight report.'),
            ...editionFiles('evidence/{edition}-distribution-approval-report.json', 'Edition distribution approval blocked report until Release Index and Public Alpha approval pass.'),
            runtimeGate('real_launcher_install_update_repair_rollback', 'Real launcher execution is still required before Public Alpha.'),
          ],
          ['Launcher flow preflight reports exist.', 'Release Index remains warning until real execution and upload evidence exist.']
        ),
      ]
    ),
    phase(
      'final_launch_phase_openlands_1_0_roadmap',
      11,
      'Final Launch Phase',
      'Freezes the relaxed default, ships MVP, expands Public Alpha, launches 1.0, and preserves parity.',
      [
        checkpoint(
          'freeze_relaxed_default',
          1,
          'Freeze relaxed default',
          'Freeze Openlands Standard as the default and keep Hardlands optional.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/config/game_modes.json', 'Default mode and Hardlands optional overlay.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/progression/launch_roadmap.json', 'Default rule stays relaxed through every roadmap phase.'),
          ],
          ['Launch roadmap defaultRule preserves Standard.', 'Hardlands remains optional.']
        ),
        checkpoint(
          'ship_mvp_scope',
          2,
          'Ship MVP scope',
          'Ship MVP with 4 biomes, 50-70 blocks, 45-60 items, shelter loop, first cave/road, first waystone, and save/load.',
          'mvp_contract_ready_real_runtime_blocked',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/progression/launch_roadmap.json', 'MVP scope and required evidence.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/index/mvp_gameplay_catalog.json', 'Every MVP block and item explained for player-facing guide/index.'),
            runtimeGate('real_mvp_runtime_playtest', 'MVP ship still requires live runtime playtest evidence.'),
          ],
          [`Current counts: ${biomes.length} biomes, ${blocks.length} blocks, ${items.length} items, ${recipes.length} recipes.`, 'Real runtime playtest remains required.']
        ),
        checkpoint(
          'public_alpha_scope',
          3,
          'Public Alpha expansion',
          'Expand Public Alpha to 8-10 biomes, 150+ blocks, 120+ items, farming, ruins, Creative mode, and 1-8 co-op.',
          'roadmap_contract_future_scope',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/progression/launch_roadmap.json', 'Public Alpha scope.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/distribution_alpha_gates.json', 'Public Alpha minimum gates.'),
          ],
          ['Public Alpha target scope is documented.', 'Current MVP does not claim Public Alpha content counts are complete.']
        ),
        checkpoint(
          'one_dot_zero_scope',
          4,
          '1.0 scope',
          'Launch 1.0 with full waystone network, old road restoration, traders, settlements, deep caves, Creative tools, and SMP support.',
          'roadmap_contract_future_scope',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/progression/launch_roadmap.json', '1.0 target scope.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/coop_and_smp.json', 'Co-op and SMP state contract.'),
          ],
          ['1.0 target scope is documented.', 'SMP support remains runtime-dependent.']
        ),
        checkpoint(
          'parity_source_of_truth',
          5,
          'Parity source of truth',
          'Maintain Native, NeoForge, and Standalone parity by treating Echo data IDs as source of truth and adapters as output targets.',
          'contract_ready',
          [
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/systems/cross_platform_parity.json', 'Parity surfaces and adapter responsibilities.'),
            moduleFile('src/main/resources/data/echoopenlandsprotocol/openlands/conformance/openlands_mvp_registry.json', 'Canonical registry ID fixture.'),
            moduleFile('src/main/java/com/knoxhack/echoopenlandsprotocol/contract/OpenlandsRuntimeContracts.java', 'Runtime contract exposes common IDs and artifact targets.'),
          ],
          ['Echo data IDs remain canonical.', 'Adapters are output targets.', 'Runtime differences must be documented before promotion.']
        ),
      ]
    ),
  ]

  const annotatedPhases = annotateEvidence(phases, moduleRoot, workspaceRoot)
  const checkpoints = annotatedPhases.flatMap((entry) => entry.checkpoints)
  const evidence = checkpoints.flatMap((item) => item.evidence)

  return {
    schema: 'echo.openlands.progression.production_phase_matrix.v1',
    namespace: MODULE_ID,
    version: '0.1.0',
    runtimeParity: RUNTIME_TARGETS,
    generatedFrom: [
      'README.md',
      'META-INF/echo.mod.json',
      'config/game_modes.json',
      'config/content_policy.json',
      'blocks/mvp_blocks.json',
      'items/mvp_items.json',
      'recipes/mvp_recipes.json',
      'biomes/mvp_biomes.json',
      'structures/mvp_landmarks.json',
      'creatures/mvp_creatures.json',
      'waystones/waystone_contract.json',
      'systems/playable_runtime_contract.json',
      'systems/runtime_adapter_load_plan.json',
      'systems/runtime_execution_acceptance.json',
      'systems/runtime_execution_harness_plan.json',
      'systems/harness_driver_manifest_contract.json',
      'systems/launcher_execution_acceptance.json',
      'systems/launcher_execution_harness_plan.json',
      'systems/final_release_review_acceptance.json',
      'systems/final_release_review_harness_plan.json',
      'systems/distribution_approval_acceptance.json',
      'systems/distribution_approval_harness_plan.json',
      'systems/release_publication_manifest_contract.json',
      'systems/distribution_alpha_gates.json',
      'progression/first_hour_route.json',
      'progression/launch_roadmap.json',
      'index/mvp_gameplay_catalog.json',
    ],
    designRules: [
      'The production matrix must keep all ten production phases plus the final launch phase visible as 55 checkable subphases.',
      'Openlands Standard remains relaxed; blocked runtime work must not be reclassified as complete.',
      'Every checkpoint must list concrete module, edition, artifact, or runtime-gate evidence.',
      'Echo-owned data IDs remain the source of truth for Native, Standalone, and NeoForge adapters.',
    ],
    launchRoadmapDefaultRule: launchRoadmap.defaultRule,
    counts: {
      phases: annotatedPhases.length,
      checkpoints: checkpoints.length,
      moduleEvidence: evidence.filter((entry) => entry.kind === 'module_file').length,
      editionEvidence: evidence.filter((entry) => entry.kind === 'edition_file').length,
      artifactEvidence: evidence.filter((entry) => entry.kind === 'artifact_file').length,
      runtimeGates: evidence.filter((entry) => entry.kind === 'runtime_gate').length,
      presentEvidence: evidence.filter((entry) => entry.present).length,
      missingEvidence: evidence.filter((entry) => !entry.present).length,
    },
    currentRegistryCounts: {
      blocks: blocks.length,
      items: items.length,
      recipes: recipes.length,
      biomes: biomes.length,
      structures: structures.length,
      creatures: creatures.length,
    },
    currentStateSummary: {
      contractReady: checkpoints.filter((item) => item.currentState === 'contract_ready').length,
      preflightReady: checkpoints.filter((item) => item.currentState.includes('preflight_ready')).length,
      runtimeExecutionPending: checkpoints.filter((item) => item.currentState.includes('execution_pending') || item.currentState.includes('runtime_blocked')).length,
      releaseBlocked: checkpoints.filter((item) => item.currentState.includes('blocked')).length,
      futureScope: checkpoints.filter((item) => item.currentState.includes('future_scope')).length,
    },
    phases: annotatedPhases,
  }
}

function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    console.log(usage())
    return
  }
  const moduleRoot = findModuleRoot(args.moduleRoot)
  const workspaceRoot = path.resolve(args.workspaceRoot ?? path.join(moduleRoot, '..', '..', '..'))
  const outputPath = path.join(moduleRoot, 'src', 'main', 'resources', 'data', MODULE_ID, 'openlands', 'progression', 'production_phase_matrix.json')
  const matrix = buildMatrix(moduleRoot, workspaceRoot)
  if (!args.dryRun) fs.writeFileSync(outputPath, `${JSON.stringify(matrix, null, 2)}\n`)
  if (args.json) {
    console.log(JSON.stringify(matrix, null, 2))
  } else {
    const action = args.dryRun ? 'validated' : 'generated'
    console.log(`${action} Openlands production phase matrix: ${matrix.counts.phases} phases, ${matrix.counts.checkpoints} checkpoints, ${matrix.counts.missingEvidence} missing evidence entries.`)
  }
}

main()

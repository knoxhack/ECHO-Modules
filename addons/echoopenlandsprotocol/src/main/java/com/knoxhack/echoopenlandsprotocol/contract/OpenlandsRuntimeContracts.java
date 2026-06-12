package com.knoxhack.echoopenlandsprotocol.contract;

import com.knoxhack.echoopenlandsprotocol.runtime.OpenlandsFirstHourRuntime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenlandsRuntimeContracts {
    public static final String MODULE_ID = "echoopenlandsprotocol";
    public static final String PACK_ID = "openlands";
    public static final String VERSION = "0.1.0";
    public static final String STANDARD_MODE = "openlands_standard";
    public static final String HARDLANDS_MODE = "openlands_hardlands";

    public static final List<String> RUNTIME_TARGETS = List.of(
            "echo_native",
            "echo_runtime_standalone",
            "neoforge"
    );

    public static final List<String> REQUIRED_CONTENT_ROOTS = List.of(
            "config",
            "blocks",
            "items",
            "recipes",
            "loot",
            "tags",
            "biomes",
            "structures",
            "creatures",
            "waystones",
            "progression",
            "playtests",
            "tutorials",
            "index",
            "holomap",
            "sounds",
            "systems",
            "conformance"
    );

    public static final List<String> REQUIRED_ASSET_ROOTS = List.of(
            "textures/block",
            "textures/item",
            "models/block",
            "models/item",
            "blockstates",
            "lang/en_us.json",
            "sounds.json"
    );

    public static final List<String> STANDARD_DISABLED_HARDCORE_FLAGS = List.of(
            "stamina",
            "hydration",
            "foodSpoilage",
            "temperatureDamage"
    );

    public static final List<String> FIRST_HOUR_SAVE_LOAD_FIELDS = List.of(
            "inventory",
            "hotbar",
            "placedBlocks",
            "chestContents",
            "bedrollSpawn",
            "campfireLitState",
            "shelterScore",
            "waystoneState",
            "holomapRegionDiscovery"
    );

    public static final List<OpenlandsArtifactTarget> ARTIFACT_TARGETS =
            OpenlandsArtifactTarget.mvpTargets(VERSION);

    public static final List<OpenlandsAdapterLoadPhase> ADAPTER_LOAD_PHASES = List.of(
            OpenlandsAdapterLoadPhase.DISCOVER,
            OpenlandsAdapterLoadPhase.LOAD_DATA,
            OpenlandsAdapterLoadPhase.REGISTER_CONTENT,
            OpenlandsAdapterLoadPhase.BIND_WORLDGEN,
            OpenlandsAdapterLoadPhase.BIND_GAMEPLAY_STATE,
            OpenlandsAdapterLoadPhase.READY,
            OpenlandsAdapterLoadPhase.RELEASE_GATE
    );

    public static final List<OpenlandsRuntimeEvidence> RUNTIME_EVIDENCE_REQUIREMENTS = List.of(
            evidence("descriptor_resolved", "discovery",
                    "Adapter found META-INF/echo.mod.json from the selected Openlands artifact.",
                    "Descriptor is parsed before any Openlands registry is touched.",
                    "Stop loading the pack and report the missing or unreadable descriptor."),
            evidence("module_identity_verified", "discovery",
                    "Descriptor id, version, role, kind, official flag, and pack id match the Openlands contract.",
                    "Runtime reports echoopenlandsprotocol 0.1.0 as an official pack_root module.",
                    "Reject the artifact as the wrong module or wrong version."),
            evidence("runtime_target_accepted", "discovery",
                    "Current runtime is one of echo_native, echo_runtime_standalone, or neoforge.",
                    "Runtime target appears in descriptor adapterCore runtimes and contract runtimeTargets.",
                    "Reject the artifact for this runtime family."),
            evidence("source_root_mounted", "discovery",
                    "Common data and assets roots are mounted using Echo-owned paths.",
                    "Data root and asset root resolve without runtime-specific renames.",
                    "Stop before registration and report the missing root."),
            evidence("standard_mode_relaxed", "config",
                    "openlands_standard is selected as the default relaxed ruleset.",
                    "Gentle hunger is active and stamina, hydration, spoilage, and temperature damage are off.",
                    "Fallback to a safe menu error instead of starting a harsher default world."),
            evidence("hardlands_optional", "config",
                    "Hardlands exists only as an opt-in overlay.",
                    "No non-Hardlands mode enables hardcore meters.",
                    "Disable the invalid overlay and report the config error."),
            evidence("legal_policy_accepted", "config",
                    "Original naming, asset, recipe, and branding policy is loaded.",
                    "Adapter records the content policy before exposing user-facing pack text.",
                    "Block release promotion until legal content review passes."),
            evidence("registry_json_parsed", "data",
                    "Block, item, recipe, tag, loot, conformance, and system JSON files parse cleanly.",
                    "Every registry payload is available to the adapter with schema version fields intact.",
                    "Stop loading and report the first malformed resource."),
            evidence("runtime_parity_declared", "data",
                    "Each core Openlands payload declares Native, Standalone, and NeoForge parity.",
                    "Parity declarations match echo_native, echo_runtime_standalone, and neoforge.",
                    "Stop release promotion and repair the offending payload."),
            evidence("minimum_content_counts_met", "data",
                    "MVP minimum counts are met for blocks, items, recipes, biomes, creatures, and systems.",
                    "Counts satisfy the conformance fixture and distribution gate minima.",
                    "Keep release index state at warning until content counts are repaired."),
            evidence("registry_ids_resolved", "registration",
                    "Every referenced block, item, recipe, biome, creature, sound, tag, and structure id resolves.",
                    "No recipe, loot, tag, spawn, or structure reference points at an unknown id.",
                    "Stop registration before partially loaded content reaches a world save."),
            evidence("block_ids_registered", "registration",
                    "All MVP block ids are registered from Echo IDs.",
                    "Runtime can resolve each block id listed in conformance/openlands_mvp_registry.json.",
                    "Fail adapter startup for missing block registrations."),
            evidence("item_ids_registered", "registration",
                    "All MVP item ids are registered from Echo IDs.",
                    "Runtime can resolve each item id listed in conformance/openlands_mvp_registry.json.",
                    "Fail adapter startup for missing item registrations."),
            evidence("recipe_ids_registered", "registration",
                    "All MVP recipe ids are registered against the correct crafting station.",
                    "Handcrafting, workbench, kiln, forge, cookpot, and map-table recipes are addressable.",
                    "Disable the pack before crafting screens can open with partial recipes."),
            evidence("tag_ids_registered", "registration",
                    "Canonical block and item tags are available for recipes, shelter, tools, and farming.",
                    "Tags resolve to known registry IDs only.",
                    "Stop registration and report the invalid tag membership."),
            evidence("canonical_echo_ids_retained", "registration",
                    "Runtime-specific ids never replace Echo IDs as the source of truth.",
                    "NeoForge ResourceLocations or Standalone handles map back to the same Echo ids.",
                    "Reject the adapter output until the id map is corrected."),
            evidence("loot_tables_bound", "registration",
                    "Block, creature, and structure loot tables bind to known Openlands ids.",
                    "Drops from terrain, ores, creatures, and landmark caches resolve without null entries.",
                    "Disable affected loot and fail public alpha parity."),
            evidence("station_surfaces_bound", "registration",
                    "Workbench, kiln, forge, loom, cookpot, mason table, and map table expose their recipe groups.",
                    "Each station opens only recipes assigned to that station id.",
                    "Fail station smoke checks and keep the artifact unreleased."),
            evidence("biome_palettes_bound", "worldgen",
                    "Meadows, Woodlands, Stonehills, and Marshlands bind terrain palettes and ambience.",
                    "Biome block palettes use only registered Openlands blocks.",
                    "Disable world creation for Openlands until biome data is fixed."),
            evidence("spawn_tables_bound", "worldgen",
                    "Creature spawn tables bind passive, neutral, and moderate hostile creatures to valid biomes.",
                    "Every creature spawn references a registered creature and biome.",
                    "Suppress invalid spawns and fail parity validation."),
            evidence("landmark_pools_bound", "worldgen",
                    "Road fragments, wells, camps, towers, mines, bridges, cellars, and waystones enter landmark pools.",
                    "Every landmark block palette resolves and each landmark has HoloMap and tutorial hooks.",
                    "Disable the invalid landmark and keep release state at warning."),
            evidence("starter_spawn_guarantees_bound", "worldgen",
                    "New worlds guarantee early access to trees, loose stones, fiber, berries, water, and a nearby landmark.",
                    "Spawn validator can find starter resources inside the configured walking radius.",
                    "Reject the seed or regenerate the starter area."),
            evidence("tutorial_triggers_bound", "gameplay_state",
                    "Discovery-only first-hour tutorial triggers are installed.",
                    "Stick, stone, fiber, first tool, campfire, shelter, route, and waystone prompts fire once.",
                    "Disable tutorial progression and fail first-hour playtest evidence."),
            evidence("shelter_score_bound", "gameplay_state",
                    "Forgiving shelter scoring checks roof, walls, door, bedroll, light/fire, and hostile distance.",
                    "Minimum sleep milestone remains at or below the relaxed Standard threshold.",
                    "Fail the first-hour milestone until shelter scoring is restored."),
            evidence("first_hour_save_load_ready", "gameplay_state",
                    "Inventory, hotbar, placed blocks, chests, bedroll spawn, campfire state, shelter score, and route state persist.",
                    "A save/load cycle preserves every field in FIRST_HOUR_SAVE_LOAD_FIELDS.",
                    "Block public alpha and keep player saves in a compatibility warning state."),
            evidence("waystone_state_persistence_ready", "gameplay_state",
                    "Waystone repair state persists from undiscovered through active.",
                    "Save/load preserves state, contributors, route ids, names, and travel permissions.",
                    "Disable fast travel and fail waystone parity tests."),
            evidence("holomap_region_persistence_ready", "gameplay_state",
                    "HoloMap region names, biome hints, old road segments, waystones, and player markers persist.",
                    "A restored waystone reveals the expected radius and nearby hints after reload.",
                    "Disable map reveal promotion and fail first-waystone playtest."),
            evidence("multiplayer_permissions_bound", "gameplay_state",
                    "Owner, group, public, contributor, rename, and public-travel permissions bind for shared state.",
                    "Co-op and SMP tests can mutate waystone and storage state without losing authority data.",
                    "Disable public travel and co-op promotion until fixed."),
            evidence("homestead_state_bound", "gameplay_state",
                    "Crops, compost, simple watering, cookpot meals, animal pens, and trader surplus hooks bind.",
                    "Homestead systems remain relaxed in Standard and do not add spoilage or death-by-neglect.",
                    "Keep homestead alpha disabled until state schema and Standard rules pass."),
            evidence("builder_ux_bound", "gameplay_state",
                    "Hammer cycling, scaffold, quick stack, quick deposit, sorting, named chests, and craft-from-storage bind.",
                    "Builder actions do not desync inventory or storage state across runtime targets.",
                    "Disable the affected builder command and fail UX parity tests."),
            evidence("sound_events_bound", "assets",
                    "Creature, ambience, block, station, waystone, and UI sound events bind to Openlands sound keys.",
                    "Every sound contract key resolves in assets/echoopenlandsprotocol/sounds.json.",
                    "Mute the missing event and fail asset validation."),
            evidence("missing_asset_policy_applied", "assets",
                    "Missing textures, models, blockstates, and sounds use explicit placeholder policy instead of borrowed assets.",
                    "Runtime reports missing Openlands-owned assets without substituting copyrighted external assets.",
                    "Fail legal/content audit before public release."),
            evidence("adapter_ready_signal", "ready",
                    "Adapter reports Openlands ready only after discovery, data, registration, worldgen, gameplay, and assets pass.",
                    "Ready signal includes module id, pack id, version, runtime target, and load step ids.",
                    "Keep runtime at disabled pack state."),
            evidence("runtime_smoke_test_ready", "ready",
                    "Runtime can start an Openlands Standard world and reach the first interactive frame.",
                    "Smoke test confirms player spawn, hotbar, inventory, block place/break, and menu exit.",
                    "Block release promotion and keep artifacts internal."),
            evidence("hardcore_meters_default_off", "ready",
                    "Standard mode UI and simulation do not expose stamina, hydration, spoilage, or temperature damage.",
                    "Fresh world and loaded world both report hardcore meters disabled.",
                    "Reject the Standard config load and require a runtime fix."),
            evidence("openlands_contract_validator_pass", "release",
                    "Openlands source validator passes against data, assets, Java contracts, systems, and artifacts.",
                    "validate-openlands-contract.mjs exits with no errors.",
                    "Do not promote release index entries beyond warning."),
            evidence("native_standalone_neoforge_artifacts_uploaded_with_sha256", "release",
                    "Native, Standalone, NeoForge, and sources artifacts exist in release storage with sha256 values.",
                    "Release Index entries reference uploaded files and matching hashes.",
                    "Keep pack manifests disabled for public download."),
            evidence("launcher_install_update_repair_rollback_pass", "release",
                    "Launcher install, update, repair, and rollback flows pass for all three edition repos.",
                    "Launcher validates manifest download, artifact hash, repair, rollback, and save preservation.",
                    "Keep launcher channel state at warning."),
            evidence("first_hour_runtime_playtest_pass", "release",
                    "A fresh player can spawn, gather, craft, shelter, sleep, explore, restore a waystone, reveal map, save, and reload.",
                    "Playtest captures the MVP promise in each runtime target.",
                    "Do not ship Public Alpha."),
            evidence("waystone_state_save_load_pass", "release",
                    "Waystone state, route binding, HoloMap reveal, contributor data, and travel permissions survive reload.",
                    "Two active waystones unlock travel only after the required state is reached.",
                    "Disable fast travel and block release."),
            evidence("legal_content_audit_pass", "release",
                    "No Minecraft names, copied assets, copied silhouettes, copied recipe identity, or Minecraft branding ships.",
                    "Audit signs off on all user-facing names, generated assets, pack text, and launcher metadata.",
                    "Block public release until all issues are removed.")
    );

    public static final List<OpenlandsAdapterLoadStep> ADAPTER_LOAD_STEPS = List.of(
            loadStep("discover_module_descriptor", OpenlandsAdapterLoadPhase.DISCOVER,
                    "Read META-INF/echo.mod.json, verify Openlands identity, and accept the current runtime target.",
                    List.of("META-INF/echo.mod.json"),
                    List.of("descriptor_resolved", "module_identity_verified", "runtime_target_accepted", "source_root_mounted"),
                    "openlands_descriptor_loaded"),
            loadStep("load_standard_mode_and_policy", OpenlandsAdapterLoadPhase.LOAD_DATA,
                    "Load relaxed Standard config, Hardlands overlay policy, and original-content/legal boundaries.",
                    List.of("config/game_modes", "config/content_policy"),
                    List.of("standard_mode_relaxed", "hardlands_optional", "legal_policy_accepted"),
                    "openlands_standard_config_loaded"),
            loadStep("load_core_registry_payloads", OpenlandsAdapterLoadPhase.LOAD_DATA,
                    "Parse core registries and conformance fixtures before runtime-specific object creation.",
                    List.of("blocks/mvp_blocks", "items/mvp_items", "recipes/mvp_recipes", "loot/mvp_loot", "tags/mvp_tags", "conformance/openlands_mvp_registry"),
                    List.of("registry_json_parsed", "runtime_parity_declared", "minimum_content_counts_met", "registry_ids_resolved"),
                    "openlands_core_registries_loaded"),
            loadStep("load_world_and_system_payloads", OpenlandsAdapterLoadPhase.LOAD_DATA,
                    "Parse biome, landmark, creature, waystone, first-hour, production phase, playtest, HoloMap, sound, runtime execution, and alpha system contracts.",
                    List.of("biomes/mvp_biomes", "structures/mvp_landmarks", "creatures/mvp_creatures", "waystones/waystone_contract", "progression/first_hour_route", "progression/production_phase_matrix", "playtests/mvp_first_hour_acceptance", "tutorials/first_hour_prompts", "index/openlands_overview", "index/mvp_gameplay_catalog", "holomap/mvp_regions", "sounds/mvp_sound_contract", "systems/homestead_alpha", "systems/builder_ux_alpha", "systems/cross_platform_parity", "systems/playable_runtime_contract", "systems/runtime_adapter_load_plan", "systems/runtime_execution_acceptance", "systems/runtime_execution_harness_plan", "systems/harness_driver_manifest_contract", "systems/legal_content_audit", "systems/launcher_flow_acceptance", "systems/launcher_execution_acceptance", "systems/launcher_execution_harness_plan", "systems/final_release_review_acceptance", "systems/final_release_review_harness_plan", "systems/distribution_approval_acceptance", "systems/distribution_approval_harness_plan", "systems/release_publication_manifest_contract", "systems/distribution_alpha_gates", "systems/coop_and_smp"),
                    List.of("registry_json_parsed", "runtime_parity_declared", "registry_ids_resolved"),
                    "openlands_world_and_system_contracts_loaded"),
            loadStep("register_blocks_items_tags_and_loot", OpenlandsAdapterLoadPhase.REGISTER_CONTENT,
                    "Register MVP blocks, items, canonical tags, block drops, creature drops, and structure loot using Echo IDs.",
                    List.of("blocks/mvp_blocks", "items/mvp_items", "tags/mvp_tags", "loot/mvp_loot"),
                    List.of("block_ids_registered", "item_ids_registered", "tag_ids_registered", "canonical_echo_ids_retained", "loot_tables_bound"),
                    "openlands_content_ids_registered"),
            loadStep("register_recipes_and_station_surfaces", OpenlandsAdapterLoadPhase.REGISTER_CONTENT,
                    "Bind handcrafting, workbench, kiln, forge, cookpot, map-table, loom, and mason-table recipe surfaces.",
                    List.of("recipes/mvp_recipes"),
                    List.of("recipe_ids_registered", "station_surfaces_bound", "canonical_echo_ids_retained"),
                    "openlands_recipe_stations_registered"),
            loadStep("bind_biomes_structures_creatures_and_spawn", OpenlandsAdapterLoadPhase.BIND_WORLDGEN,
                    "Bind starter biomes, landmarks, creature spawns, ambience, and guaranteed first-hour resources.",
                    List.of("biomes/mvp_biomes", "structures/mvp_landmarks", "creatures/mvp_creatures", "loot/mvp_loot"),
                    List.of("biome_palettes_bound", "spawn_tables_bound", "landmark_pools_bound", "starter_spawn_guarantees_bound"),
                    "openlands_worldgen_bound"),
            loadStep("bind_first_hour_shelter_and_save_load", OpenlandsAdapterLoadPhase.BIND_GAMEPLAY_STATE,
                    "Bind discovery prompts, shared runtime-core shelter scoring, bedroll spawn, campfire state, inventory, hotbar, chests, route save fields, and playtest checkpoints.",
                    List.of("progression/first_hour_route", "playtests/mvp_first_hour_acceptance", "tutorials/first_hour_prompts", "systems/playable_runtime_contract", "conformance/openlands_mvp_registry"),
                    List.of("tutorial_triggers_bound", "shelter_score_bound", "first_hour_save_load_ready"),
                    "openlands_first_hour_state_bound"),
            loadStep("bind_waystones_holomap_and_multiplayer_state", OpenlandsAdapterLoadPhase.BIND_GAMEPLAY_STATE,
                    "Bind shared runtime-core waystone repair progression, HoloMap reveal data, old road segments, contributors, ownership, and travel permissions.",
                    List.of("waystones/waystone_contract", "holomap/mvp_regions", "systems/playable_runtime_contract", "systems/coop_and_smp"),
                    List.of("waystone_state_persistence_ready", "holomap_region_persistence_ready", "multiplayer_permissions_bound"),
                    "openlands_waystone_network_bound"),
            loadStep("bind_homestead_builder_and_audio", OpenlandsAdapterLoadPhase.BIND_GAMEPLAY_STATE,
                    "Bind relaxed homestead systems, builder UX commands, and Openlands sound events without enabling hardcore Standard rules.",
                    List.of("systems/homestead_alpha", "systems/builder_ux_alpha", "systems/playable_runtime_contract", "sounds/mvp_sound_contract", "assets/sounds.json"),
                    List.of("homestead_state_bound", "builder_ux_bound", "sound_events_bound", "missing_asset_policy_applied"),
                    "openlands_alpha_surfaces_bound"),
            loadStep("report_runtime_ready", OpenlandsAdapterLoadPhase.READY,
                    "Expose adapter-ready metadata after smoke checks prove Standard starts with hardcore meters off.",
                    List.of("systems/cross_platform_parity", "systems/playable_runtime_contract", "systems/runtime_adapter_load_plan"),
                    List.of("adapter_ready_signal", "runtime_smoke_test_ready", "hardcore_meters_default_off"),
                    "openlands_runtime_ready"),
            loadStep("approve_public_alpha_release", OpenlandsAdapterLoadPhase.RELEASE_GATE,
                    "Promote only after contract validation, uploaded artifacts, launcher flows, parity playtests, waystone reload, and legal audit pass.",
                    List.of("systems/distribution_alpha_gates", "systems/legal_content_audit", "systems/launcher_flow_acceptance", "systems/launcher_execution_acceptance", "systems/launcher_execution_harness_plan", "systems/final_release_review_acceptance", "systems/final_release_review_harness_plan", "systems/distribution_approval_acceptance", "systems/distribution_approval_harness_plan", "systems/release_publication_manifest_contract", "systems/runtime_execution_harness_plan", "systems/harness_driver_manifest_contract", "progression/launch_roadmap", "playtests/mvp_first_hour_acceptance"),
                    List.of("openlands_contract_validator_pass", "native_standalone_neoforge_artifacts_uploaded_with_sha256", "launcher_install_update_repair_rollback_pass", "first_hour_runtime_playtest_pass", "waystone_state_save_load_pass", "legal_content_audit_pass"),
                    "openlands_public_alpha_release_approved")
    );

    public static final List<OpenlandsContractResource> CONTRACT_RESOURCES = List.of(
            resource("config/game_modes", OpenlandsContractKind.CONFIG,
                    "config/game_modes.json", 6,
                    List.of("config", "modes"),
                    List.of("defaultMode", "modes"),
                    "Locks Openlands Standard as the relaxed default and Hardlands as optional."),
            resource("config/content_policy", OpenlandsContractKind.CONFIG,
                    "config/content_policy.json", 1,
                    List.of("legal", "content"),
                    List.of("forbidden", "required"),
                    "Protects original Openlands naming, assets, recipes, and legal boundaries."),
            resource("blocks/mvp_blocks", OpenlandsContractKind.REGISTRY,
                    "blocks/mvp_blocks.json", 50,
                    List.of("blocks", "content"),
                    List.of("id", "displayName", "hardness", "tool", "drops", "tags", "model", "texture"),
                    "MVP block registry for terrain, stone, ores, woods, utilities, old roads, and waystones."),
            resource("items/mvp_items", OpenlandsContractKind.REGISTRY,
                    "items/mvp_items.json", 45,
                    List.of("items", "content"),
                    List.of("id", "displayName", "stackSize", "useType", "tags", "model", "texture", "recipeRefs"),
                    "MVP item registry for materials, food, tools, utilities, and waystone components."),
            resource("recipes/mvp_recipes", OpenlandsContractKind.REGISTRY,
                    "recipes/mvp_recipes.json", 35,
                    List.of("recipes", "stations"),
                    List.of("id", "station", "inputs", "outputs", "timeTicks", "unlockedBy", "parityNotes"),
                    "Handcrafting, workbench, kiln, forge, cookpot, and map-table recipes."),
            resource("loot/mvp_loot", OpenlandsContractKind.LOOT,
                    "loot/mvp_loot.json", 10,
                    List.of("loot", "blocks", "creatures", "structures"),
                    List.of("blockDrops", "creatureDrops", "chestTables"),
                    "Drops for blocks, creatures, and starter landmark caches."),
            resource("tags/mvp_tags", OpenlandsContractKind.REGISTRY,
                    "tags/mvp_tags.json", 8,
                    List.of("tags", "content"),
                    List.of("blockTags", "itemTags"),
                    "Canonical block and item tag sets used by recipes, shelter, farming, and adapters."),
            resource("biomes/mvp_biomes", OpenlandsContractKind.WORLD,
                    "biomes/mvp_biomes.json", 4,
                    List.of("worldgen", "biomes"),
                    List.of("id", "displayName", "blockPalette", "resourceSet", "spawnTable", "ambience", "landmarkFrequency"),
                    "Starter biome definitions and spawn safety guarantees."),
            resource("structures/mvp_landmarks", OpenlandsContractKind.WORLD,
                    "structures/mvp_landmarks.json", 8,
                    List.of("worldgen", "structures", "exploration"),
                    List.of("id", "displayName", "footprint", "preferredBiomes", "blocks", "holoMapHint", "tutorialHook"),
                    "Starter landmarks: wells, roads, camps, towers, mines, bridges, cellars, and waystones."),
            resource("creatures/mvp_creatures", OpenlandsContractKind.REGISTRY,
                    "creatures/mvp_creatures.json", 10,
                    List.of("creatures", "spawns", "ai"),
                    List.of("id", "displayName", "category", "biomes", "spawnRules", "ai", "health", "drops", "sounds"),
                    "Passive, neutral, and moderate hostile creature contracts."),
            resource("waystones/waystone_contract", OpenlandsContractKind.SYSTEM,
                    "waystones/waystone_contract.json", 8,
                    List.of("waystones", "old_roads", "multiplayer"),
                    List.of("blocks", "stateMachine", "effects", "multiplayerState"),
                    "Waystone repair state machine, reveal effects, fast travel, and permissions."),
            resource("progression/first_hour_route", OpenlandsContractKind.PROGRESSION,
                    "progression/first_hour_route.json", 7,
                    List.of("progression", "first_hour", "save_load"),
                    List.of("playerPromise", "firstHour", "shelterScore", "saveLoadAcceptance"),
                    "Relaxed first-hour route from spawn through shelter and first waystone."),
            resource("progression/launch_roadmap", OpenlandsContractKind.PROGRESSION,
                    "progression/launch_roadmap.json", 4,
                    List.of("roadmap", "release"),
                    List.of("phases", "nonNegotiableInvariants"),
                    "MVP, Public Alpha, 1.0, and post-launch roadmap with required evidence."),
            resource("progression/production_phase_matrix", OpenlandsContractKind.PROGRESSION,
                    "progression/production_phase_matrix.json", 55,
                    List.of("roadmap", "production", "evidence"),
                    List.of("phases", "checkpoints", "counts", "currentStateSummary"),
                    "Generated 10-phase plus final-launch production matrix with concrete evidence for every subphase."),
            resource("playtests/mvp_first_hour_acceptance", OpenlandsContractKind.PLAYTEST,
                    "playtests/mvp_first_hour_acceptance.json", 7,
                    List.of("playtests", "first_hour", "parity", "waystones"),
                    List.of("requiredRouteSteps", "acceptanceScenarios", "saveLoadCheckpoints", "waystonePublicAlphaScenario", "holomapAcceptance", "releaseEvidence"),
                    "Machine-readable first-hour, save/load, HoloMap, and waystone parity playtest fixture."),
            resource("tutorials/first_hour_prompts", OpenlandsContractKind.TUTORIAL,
                    "tutorials/first_hour_prompts.json", 10,
                    List.of("tutorials", "first_hour"),
                    List.of("deliveryRules", "prompts"),
                    "Discovery-only tutorial prompts for the first-hour route."),
            resource("index/openlands_overview", OpenlandsContractKind.PROGRESSION,
                    "index/openlands_overview.json", 5,
                    List.of("index", "docs"),
                    List.of("packId", "publicName", "indexCategories", "mvpAcceptance"),
                    "Index overview and MVP acceptance reference for in-game docs."),
            resource("index/mvp_gameplay_catalog", OpenlandsContractKind.PROGRESSION,
                    "index/mvp_gameplay_catalog.json", 50,
                    List.of("index", "gameplay", "blocks", "items"),
                    List.of("blockEntries", "itemEntries", "roleCoverage", "designRules"),
                    "Generated gameplay catalog with acquisition, roles, player uses, progression stages, and runtime notes for every MVP block and item."),
            resource("holomap/mvp_regions", OpenlandsContractKind.MAP,
                    "holomap/mvp_regions.json", 5,
                    List.of("holomap", "regions"),
                    List.of("regionDataContract", "layers", "hintTypes"),
                    "HoloMap region, old road, waystone, nearby hint, and player marker data."),
            resource("sounds/mvp_sound_contract", OpenlandsContractKind.SOUND,
                    "sounds/mvp_sound_contract.json", 10,
                    List.of("sounds", "ambience", "creatures"),
                    List.of("soundFamilies", "creatureSoundTemplate"),
                    "SoundCore and runtime audio event contract."),
            resource("systems/homestead_alpha", OpenlandsContractKind.SYSTEM,
                    "systems/homestead_alpha.json", 3,
                    List.of("farming", "homestead", "traders"),
                    List.of("crops", "soilCare", "cookpotMeals", "animalPens", "traderSurplus"),
                    "Alpha farming, soil care, cookpot, animal pen, and trader surplus contract."),
            resource("systems/builder_ux_alpha", OpenlandsContractKind.SYSTEM,
                    "systems/builder_ux_alpha.json", 5,
                    List.of("builder_ux", "inventory", "storage"),
                    List.of("tools", "temporaryBlocks", "inventoryCommands", "acceptance"),
                    "Hammer, scaffold, quick-stack, quick-deposit, sorting, storage naming, and craft-from-storage."),
            resource("systems/cross_platform_parity", OpenlandsContractKind.SYSTEM,
                    "systems/cross_platform_parity.json", 3,
                    List.of("parity", "adapters"),
                    List.of("runtimeTargets", "paritySurfaces", "testFixtures"),
                    "Native, Standalone, and NeoForge parity surfaces and adapter responsibilities."),
            resource("systems/playable_runtime_contract", OpenlandsContractKind.SYSTEM,
                    "systems/playable_runtime_contract.json", 6,
                    List.of("runtime", "first_hour", "adapters"),
                    List.of("runtimeCore", "firstHourRuntimeHooks", "starterSpawnRules", "shelterScoring", "waystoneTransitions", "adapterBindings"),
                    "Shared Java first-hour runtime core for relaxed Standard rules, starter spawn, shelter scoring, waystones, and adapter readiness."),
            resource("systems/runtime_adapter_load_plan", OpenlandsContractKind.SYSTEM,
                    "systems/runtime_adapter_load_plan.json", 7,
                    List.of("adapters", "runtime", "release"),
                    List.of("phases", "loadSteps", "runtimeEvidenceRequirements", "acceptanceGates"),
                    "Exact adapter boot phases, required resources, success signals, and runtime evidence."),
            resource("systems/runtime_execution_acceptance", OpenlandsContractKind.SYSTEM,
                    "systems/runtime_execution_acceptance.json", 14,
                    List.of("runtime", "execution", "release"),
                    List.of("reportContract", "editionReports", "runtimeGates", "executionSuites", "scenarios"),
                    "Real adapter execution report schema and scenarios required to clear runtime, launcher, artifact, and final-review gates."),
            resource("systems/runtime_execution_harness_plan", OpenlandsContractKind.SYSTEM,
                    "systems/runtime_execution_harness_plan.json", 17,
                    List.of("runtime", "execution", "harness", "adapters"),
                    List.of("editionHarnesses", "driverSurfaces", "scenarioBindings", "reportAssemblyRules"),
                    "Real runtime harness driver plan mapping every execution acceptance scenario to actions, assertions, captures, and saved artifacts."),
            resource("systems/harness_driver_manifest_contract", OpenlandsContractKind.SYSTEM,
                    "systems/harness_driver_manifest_contract.json", 4,
                    List.of("runtime", "launcher", "harness", "drivers", "release"),
                    List.of("reportContract", "editionManifestTemplates", "harnessFamilies", "clearingRules"),
                    "Edition-owned driver manifest contract that records implemented and missing real driver surfaces before harness reports can clear gates."),
            resource("systems/legal_content_audit", OpenlandsContractKind.SYSTEM,
                    "systems/legal_content_audit.json", 5,
                    List.of("legal", "assets", "names", "release"),
                    List.of("auditScope", "forbiddenPublicTerms", "assetRules", "recipeIdentityRules", "releaseEvidence"),
                    "Public naming, asset, recipe identity, and generated-output audit rules."),
            resource("systems/launcher_flow_acceptance", OpenlandsContractKind.SYSTEM,
                    "systems/launcher_flow_acceptance.json", 4,
                    List.of("launcher", "distribution", "release", "rollback"),
                    List.of("editionMatrix", "artifactVerification", "requiredLauncherFlows", "statePreservation", "releaseEvidence"),
                    "Launcher install, update, repair, rollback, artifact hash, and state preservation gates."),
            resource("systems/launcher_execution_acceptance", OpenlandsContractKind.SYSTEM,
                    "systems/launcher_execution_acceptance.json", 5,
                    List.of("launcher", "execution", "distribution", "release"),
                    List.of("reportContract", "editionReports", "launcherGates", "executionSuite", "executionFlows"),
                    "Real launcher execution report schema and flows required to clear install, update, repair, rollback, and preservation gates."),
            resource("systems/launcher_execution_harness_plan", OpenlandsContractKind.SYSTEM,
                    "systems/launcher_execution_harness_plan.json", 4,
                    List.of("launcher", "execution", "harness", "release"),
                    List.of("editionHarnesses", "driverSurfaces", "flowBindings", "reportAssemblyRules"),
                    "Real launcher harness driver plan mapping install, update, repair, and rollback flows to preconditions, actions, assertions, captures, saved artifacts, and world-state policies."),
            resource("systems/final_release_review_acceptance", OpenlandsContractKind.SYSTEM,
                    "systems/final_release_review_acceptance.json", 5,
                    List.of("legal", "assets", "audio", "release"),
                    List.of("reportContract", "editionReports", "finalReviewGates", "reviewAreas", "publicReleaseClearance"),
                    "Final human review report schema for Openlands art, audio, branding, generated outputs, and legal release signoff."),
            resource("systems/final_release_review_harness_plan", OpenlandsContractKind.SYSTEM,
                    "systems/final_release_review_harness_plan.json", 5,
                    List.of("legal", "assets", "audio", "harness", "release"),
                    List.of("editionHarnesses", "driverSurfaces", "reviewAreaBindings", "reportAssemblyRules"),
                    "Final review harness driver plan mapping public identity, art, audio, and generated-output review areas to captures and saved artifacts."),
            resource("systems/distribution_approval_acceptance", OpenlandsContractKind.SYSTEM,
                    "systems/distribution_approval_acceptance.json", 5,
                    List.of("release", "distribution", "launcher", "approval"),
                    List.of("reportContract", "editionReports", "distributionGates", "approvalAreas", "publicAlphaClearance"),
                    "Real distribution approval report schema for Release Index publication, artifact downloads, co-op session testing, and Public Alpha approval."),
            resource("systems/distribution_approval_harness_plan", OpenlandsContractKind.SYSTEM,
                    "systems/distribution_approval_harness_plan.json", 5,
                    List.of("release", "distribution", "harness", "approval"),
                    List.of("editionHarnesses", "driverSurfaces", "approvalAreaBindings", "reportAssemblyRules"),
                    "Distribution approval harness driver plan mapping Release Index, download verification, manifest indexing, co-op, dependencies, and approval signoff to saved evidence."),
            resource("systems/release_publication_manifest_contract", OpenlandsContractKind.SYSTEM,
                    "systems/release_publication_manifest_contract.json", 4,
                    List.of("release", "publication", "artifacts", "download"),
                    List.of("reportContract", "artifactTargets", "releaseIndexPatchRules", "blockedTemplateRules"),
                    "Publication manifest contract for artifact download URLs, download verification, Release Index patching, and approval handoff."),
            resource("systems/distribution_alpha_gates", OpenlandsContractKind.SYSTEM,
                    "systems/distribution_alpha_gates.json", 4,
                    List.of("release", "launcher", "artifacts"),
                    List.of("artifactTargets", "releaseIndexStates", "launcherGates", "publicAlphaMinimum"),
                    "Public Alpha distribution gates for artifacts, launcher flows, and release index approval."),
            resource("systems/coop_and_smp", OpenlandsContractKind.SYSTEM,
                    "systems/coop_and_smp.json", 4,
                    List.of("multiplayer", "smp", "state"),
                    List.of("targetPlayers", "sharedState", "permissions", "networkEvents", "acceptance"),
                    "Co-op and SMP state, permissions, storage transactions, markers, and waystone authority."),
            resource("conformance/openlands_mvp_registry", OpenlandsContractKind.CONFORMANCE,
                    "conformance/openlands_mvp_registry.json", 5,
                    List.of("conformance", "parity"),
                    List.of("blockRegistry", "itemRegistry", "recipeRegistry", "biomeRegistry", "creatureRegistry", "systemContracts"),
                    "MVP registry fixture used by validators and runtime parity tests.")
    );

    private OpenlandsRuntimeContracts() {
    }

    public static List<String> contractIds() {
        return CONTRACT_RESOURCES.stream()
                .map(OpenlandsContractResource::namespacedId)
                .toList();
    }

    public static List<String> contractResourcePaths() {
        return CONTRACT_RESOURCES.stream()
                .map(OpenlandsContractResource::resourcePath)
                .toList();
    }

    public static List<Map<String, Object>> adapterLoadPlan() {
        return ADAPTER_LOAD_STEPS.stream()
                .map(OpenlandsAdapterLoadStep::asAdapterRecord)
                .toList();
    }

    public static List<String> adapterLoadStepIds() {
        return ADAPTER_LOAD_STEPS.stream()
                .map(OpenlandsAdapterLoadStep::id)
                .toList();
    }

    public static List<String> runtimeEvidenceIds() {
        return RUNTIME_EVIDENCE_REQUIREMENTS.stream()
                .map(OpenlandsRuntimeEvidence::id)
                .toList();
    }

    public static List<String> requiredPublicAlphaEvidenceIds() {
        return RUNTIME_EVIDENCE_REQUIREMENTS.stream()
                .filter(OpenlandsRuntimeEvidence::requiredForPublicAlpha)
                .map(OpenlandsRuntimeEvidence::id)
                .toList();
    }

    public static List<OpenlandsContractResource> resourcesByKind(OpenlandsContractKind kind) {
        return CONTRACT_RESOURCES.stream()
                .filter(resource -> resource.kind() == kind)
                .toList();
    }

    public static Map<String, Object> adapterManifest() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleId", MODULE_ID);
        result.put("packId", PACK_ID);
        result.put("version", VERSION);
        result.put("defaultMode", STANDARD_MODE);
        result.put("hardlandsMode", HARDLANDS_MODE);
        result.put("runtimeTargets", RUNTIME_TARGETS);
        result.put("requiredContentRoots", REQUIRED_CONTENT_ROOTS);
        result.put("requiredAssetRoots", REQUIRED_ASSET_ROOTS);
        result.put("standardDisabledHardcoreFlags", STANDARD_DISABLED_HARDCORE_FLAGS);
        result.put("firstHourSaveLoadFields", FIRST_HOUR_SAVE_LOAD_FIELDS);
        result.put("playableRuntimeCore", OpenlandsFirstHourRuntime.adapterBindingManifest());
        result.put("contractResourceCount", CONTRACT_RESOURCES.size());
        result.put("adapterLoadPhases", ADAPTER_LOAD_PHASES.stream()
                .map(OpenlandsAdapterLoadPhase::asAdapterRecord)
                .toList());
        result.put("adapterLoadPlan", adapterLoadPlan());
        result.put("adapterLoadStepIds", adapterLoadStepIds());
        result.put("runtimeEvidenceRequirements", RUNTIME_EVIDENCE_REQUIREMENTS.stream()
                .map(OpenlandsRuntimeEvidence::asAdapterRecord)
                .toList());
        result.put("runtimeEvidenceIds", runtimeEvidenceIds());
        result.put("contractResources", CONTRACT_RESOURCES.stream()
                .map(OpenlandsContractResource::asAdapterRecord)
                .toList());
        result.put("artifactTargets", ARTIFACT_TARGETS.stream()
                .map(OpenlandsArtifactTarget::asAdapterRecord)
                .toList());
        result.put("releaseApprovalRequiredEvidence", requiredPublicAlphaEvidenceIds());
        return Map.copyOf(result);
    }

    private static OpenlandsRuntimeEvidence evidence(
            String id,
            String category,
            String description,
            String successCriteria,
            String failureAction
    ) {
        return new OpenlandsRuntimeEvidence(
                id,
                category,
                description,
                successCriteria,
                failureAction,
                true,
                RUNTIME_TARGETS,
                List.of("contract_validator", "adapter_boot_report", "runtime_parity_test")
        );
    }

    private static OpenlandsAdapterLoadStep loadStep(
            String id,
            OpenlandsAdapterLoadPhase phase,
            String summary,
            List<String> resourceIds,
            List<String> requiredEvidence,
            String successSignal
    ) {
        return new OpenlandsAdapterLoadStep(
                id,
                phase,
                summary,
                resourceIds,
                requiredEvidence,
                RUNTIME_TARGETS,
                successSignal,
                true
        );
    }

    private static OpenlandsContractResource resource(
            String id,
            OpenlandsContractKind kind,
            String relativePath,
            int minimumEntries,
            List<String> domains,
            List<String> requiredFields,
            String description
    ) {
        return new OpenlandsContractResource(
                id,
                kind,
                "data/" + MODULE_ID + "/openlands/" + relativePath,
                minimumEntries,
                domains,
                requiredFields,
                description
        );
    }
}

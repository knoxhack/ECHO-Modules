# Openlands Production Phase Audit

This file maps the Openlands production plan to current source-of-truth files and validation gates.
The machine-readable source of truth is `progression/production_phase_matrix.json`; it contains all 10 production phases, the final launch phase, 55 checkpoints, concrete evidence paths, and blocked runtime gates.

## Phase Coverage

| Phase | Evidence |
| --- | --- |
| Product Contract | `README.md`, `config/game_modes.json`, `config/content_policy.json`, `systems/legal_content_audit.json`, `META-INF/echo.mod.json`, `progression/production_phase_matrix.json` |
| Repo And Artifact Setup | `build.gradle`, `docs/artifacts.md`, generated module release output, Openlands edition repos, `progression/production_phase_matrix.json` |
| Data And Schema Layout | `data/echoopenlandsprotocol/openlands/*`, `assets/echoopenlandsprotocol/*`, `conformance/openlands_mvp_registry.json`, `index/mvp_gameplay_catalog.json`, `progression/production_phase_matrix.json` |
| MVP Block Registry | `blocks/mvp_blocks.json` |
| MVP Item Registry | `items/mvp_items.json` |
| Crafting And Stations | `recipes/mvp_recipes.json` |
| First-Hour Gameplay | `progression/first_hour_route.json`, `playtests/mvp_first_hour_acceptance.json`, `tutorials/first_hour_prompts.json`, `systems/playable_runtime_contract.json`, `src/main/java/com/knoxhack/echoopenlandsprotocol/runtime` |
| Worldgen And Exploration | `biomes/mvp_biomes.json`, `structures/mvp_landmarks.json`, `holomap/mvp_regions.json` |
| Waystones And Old Roads | `waystones/waystone_contract.json`, old road blocks in `blocks/mvp_blocks.json` |
| Alpha Systems And Distribution | `systems/homestead_alpha.json`, `systems/builder_ux_alpha.json`, `systems/cross_platform_parity.json`, `systems/playable_runtime_contract.json`, `systems/runtime_adapter_load_plan.json`, `systems/legal_content_audit.json`, `systems/launcher_flow_acceptance.json`, `systems/runtime_execution_harness_plan.json`, `systems/harness_driver_manifest_contract.json`, `systems/launcher_execution_acceptance.json`, `systems/launcher_execution_harness_plan.json`, `systems/final_release_review_acceptance.json`, `systems/final_release_review_harness_plan.json`, `systems/distribution_approval_acceptance.json`, `systems/distribution_approval_harness_plan.json`, `systems/release_publication_manifest_contract.json`, `systems/distribution_alpha_gates.json`, `systems/coop_and_smp.json` |
| Final Launch Roadmap | `progression/launch_roadmap.json`, `progression/production_phase_matrix.json` |

## Current Gate

The current implementation is a production foundation. It proves the data contract and packaging shape, not the full playable runtime.

Runtime adapters should start from `com.knoxhack.echoopenlandsprotocol.contract.OpenlandsRuntimeContracts`. That class exposes the canonical resource paths, expected minimum counts, runtime targets, relaxed Standard rules, save/load fields, adapter load phases, runtime evidence requirements, and artifact targets without depending on Minecraft-only classes.

The detailed adapter boot contract lives in `docs/runtime-adapter-load-plan.md` and `systems/runtime_adapter_load_plan.json`.
The generated production phase matrix lives in `progression/production_phase_matrix.json` and proves the plan shape: 11 phases, 55 checkpoints, 237 evidence references, zero missing evidence references in this workspace, and explicit runtime gates for work that still needs live adapter, launcher, co-op, publication, distribution approval, or final art/legal execution.
The edition adapter-boot preflight reports live in each edition repo under `evidence/*-adapter-boot-report.json` and prove descriptor identity, runtime target acceptance, phase order, load-step resources, success signals, runtime evidence IDs, and compiled artifact resource coverage.
The edition registry-parity preflight reports live in each edition repo under `evidence/*-registry-parity-report.json` and prove conformance counts, cross-runtime ID parity surfaces, Standard-mode rules, first-hour save/load fields, waystone state-machine parity, and compiled artifact registry contents.
The edition crafting-station preflight reports live in each edition repo under `evidence/*-crafting-station-report.json` and prove 40 recipes across handcrafting, workbench, kiln, forge, cookpot, and map table; they also record deferred loom/mason-table station blocks and unlock hooks for adapter smoke tests.
The edition worldgen/exploration preflight reports live in each edition repo under `evidence/*-worldgen-exploration-report.json` and prove four MVP biomes, eight landmarks, ten creatures, starter spawn safety, HoloMap layers and hints, ambience/sound keys, semantic worldgen markers, load-step evidence, and compiled artifact coverage.
The edition creature-roster preflight reports live in each edition repo under `evidence/*-creature-roster-report.json` and prove ten MVP creatures, spawn rules, biome spawn-table entries, drop tables, sound keys, AI hints, starter-safety distances, and compiled artifact coverage.
The edition old-road network preflight reports live in each edition repo under `evidence/*-old-road-network-report.json` and prove old-road blocks, route records, map-table route recipes, road landmarks, HoloMap oldRoadSegments, linkedRouteIds, public travel permissions, and compiled artifact coverage.
The edition alpha-systems preflight reports live in each edition repo under `evidence/*-alpha-systems-report.json` and prove relaxed homestead rules, crop/cookpot/animal pen/trader surfaces, builder hammer/scaffold/inventory commands, co-op permissions, storage transactions, and compiled artifact alpha-system contents.
The edition distribution/roadmap preflight reports live in each edition repo under `evidence/*-distribution-roadmap-report.json` and prove distribution alpha gates, launch roadmap phases, compiled artifact SHA-256/size metadata, Release Index warning state, edition matrix alignment, Public Alpha MVP floors, launcher-flow mapping, final roadmap invariants, and remaining real-execution blockers.
The edition local launcher rehearsal reports live in each edition repo under `evidence/*-local-launcher-rehearsal-report.json` and prove local artifact cache install, update replacement, corrupt-artifact repair, rollback manifest snapshotting, descriptor inspection, saved rehearsal artifacts, and Openlands Standard world/config preservation without clearing real launcher gates.
The shared first-hour and alpha-systems runtime core lives in `systems/playable_runtime_contract.json` and `src/main/java/com/knoxhack/echoopenlandsprotocol/runtime`.
The real adapter execution report contract lives in `systems/runtime_execution_acceptance.json` and maps every production runtime gate to required Native, Standalone, and NeoForge execution scenarios.
The real adapter execution harness plan lives in `systems/runtime_execution_harness_plan.json` and maps those scenarios to exact driver surfaces, actions, assertions, captures, and saved artifacts.
The edition local runtime rehearsal reports live in each edition repo under `evidence/*-local-runtime-rehearsal-report.json` and prove all 17 runtime execution scenarios are mapped to current fixtures, pure runtime hook proofs, captures, and saved preflight artifacts without clearing real adapter runtime gates.
The harness driver manifest contract lives in `systems/harness_driver_manifest_contract.json` and maps each generated edition manifest template to the required runtime, launcher, final-review, and distribution driver surface ids that remain missing until real adapter entrypoints exist. `scripts/generate-openlands-harness-driver-manifest.mjs` regenerates those manifests, and `scripts/validate-openlands-harness-driver-manifest.mjs` validates template, partial, or ready driver availability before harness runners consume it.
The real launcher execution report contract lives in `systems/launcher_execution_acceptance.json` and maps install, update, repair, rollback, and world/config preservation gates to required Native, Standalone, and NeoForge launcher reports.
The real launcher execution harness plan lives in `systems/launcher_execution_harness_plan.json` and maps those flows to exact launcher driver surfaces, preconditions, actions, assertions, captures, saved artifacts, and world-state policies.
The final release review report contract lives in `systems/final_release_review_acceptance.json` and maps public identity, art, audio, generated output, and legal signoff checklists to required Native, Standalone, and NeoForge reports.
The final release review harness plan lives in `systems/final_release_review_harness_plan.json` and maps those review areas to exact reviewer identity, public text, asset, legal, block render, item render, audio, generated-output drivers, captures, saved artifacts, and report assembly rules.
The distribution approval report contract lives in `systems/distribution_approval_acceptance.json` and maps public artifact URL publication, download hash verification, edition manifest indexing, co-op public-alpha session evidence, and approval signoff to required Native, Standalone, and NeoForge reports.
The distribution approval harness plan lives in `systems/distribution_approval_harness_plan.json` and maps those approval areas to exact Release Index, artifact download, edition manifest, dependency gate, co-op session, roadmap, rollback, approval signature, and report artifact drivers.
The release publication manifest contract lives in `systems/release_publication_manifest_contract.json` and maps the local compiled artifacts to public URL records, download-back hash/size verification, Release Index patch state, and approval evidence before URL blockers can clear. The public download verifier turns uploaded artifact URLs into a verified manifest by downloading all four public artifacts and comparing SHA-256 plus byte size. The approval tool requires explicit signoff evidence before writing the approved manifest and patching Release Index download URLs. The local release publication rehearsal report proves local artifact copy/download-back verification and patch-preview shape without publishing public URLs or clearing distribution gates.
The edition manifest index preview report lives at `dist/echo-module-release/echoopenlandsprotocol/openlands-edition-manifest-index-preview.json` and proves the three thin edition manifests, module requirement graph, per-edition modpack entry previews, and launcher channel listing shape without clearing real launcher or distribution gates.
The generated MVP gameplay catalog lives in `index/mvp_gameplay_catalog.json` and gives every MVP block and item an acquisition path, gameplay role, player use, progression stage, and runtime parity note for ECHO Index/Guide consumption.
The MVP asset scaffold lives in `assets/echoopenlandsprotocol/asset_manifest.json`. It now requires owned placeholder coverage for every MVP block and item, but keeps public release blocked until final Openlands art/audio/legal review.

Required command from `ECHO-Modules`:

```bash
node addons/echoopenlandsprotocol/scripts/validate-openlands-contract.mjs --module-root addons/echoopenlandsprotocol --require-artifacts
```

Runtime-core command from `ECHO-Modules`:

```bash
node addons/echoopenlandsprotocol/scripts/validate-openlands-runtime-core.mjs --module-root addons/echoopenlandsprotocol
```

Cross-edition command from `ECHO-Modules`:

```bash
node addons/echoopenlandsprotocol/scripts/validate-openlands-editions.mjs --module-root addons/echoopenlandsprotocol --workspace-root C:/Development/Github
```

## Remaining Runtime Work

- Native adapter must load and register all Openlands registries.
- Standalone Runtime must implement the block/item/world/save/UI surfaces without Minecraft classes.
- NeoForge adapter must generate runtime data from Echo IDs without importing Minecraft-owned identity.
- All adapters must report the adapter load step ids, success signals, and runtime evidence defined by `runtime_adapter_load_plan.json`.
- Adapter-boot preflight now verifies descriptor identity, runtime target acceptance, seven adapter phases, twelve load steps, source resources, compiled artifact resources, and adapter-ready signal coverage for all three edition repos.
- Registry-parity preflight now verifies 53 blocks, 50 items, 40 recipes, 4 biomes, 8 structures, 10 creatures, 8 waystone states, non-shipping discovery recipe refs, non-registry biome resources, Standard-mode rules, save/load fields, and waystone state-machine parity for all three edition repos.
- Crafting-station preflight now verifies handcrafting, workbench, kiln, forge, cookpot, map table, exact station recipe counts, freeform crafting identity, map-table route recipes, process timing, deferred loom/mason-table station blocks, and compiled artifact recipe coverage for all three edition repos.
- Worldgen/exploration preflight now verifies Meadows, Woodlands, Stonehills, Marshlands, starter spawn guarantees, landmark pools, creature spawn/sound coverage, HoloMap layers and hint types, cave/road/reed semantic markers, and compiled artifact worldgen coverage for all three edition repos.
- Creature-roster preflight now verifies hare, deer, boar, goat, marsh hen, fish, greyling, bristleback, hollow stalker, mire leech, moderate default hostility, no copied silhouettes, spawn groups, biome spawn tables, drop tables, 34 creature sound events, and safe starter creature constraints for all three edition repos.
- Old-road network preflight now verifies old_road_block, old_road_marker, broken_waystone, restored_waystone, waystone_plinth, region_rubbing, old_road_token, waystone_core, route_binding, road-linked landmarks, HoloMap oldRoadSegments, two-discovered-waystone route binding, and public-after-active travel permissions for all three edition repos.
- Alpha-systems preflight now verifies grain, root crop, berries, cookpot stew, three animal pens, trader surplus pools, wooden hammer shape controls, scaffold behavior, quick stack, quick deposit, sort inventory, craft from nearby storage, named chests, co-op permissions, network event surfaces, and relaxed Standard rules for all three edition repos.
- Distribution/roadmap preflight now verifies distribution alpha gates, launch roadmap scope, artifact target hashes and sizes, Release Index warning state, edition manifest/matrix parity, Public Alpha MVP floors, launcher-flow mapping, final roadmap invariants, and the blockers for uploaded URLs, live launcher execution, live runtime parity, co-op session testing, and release approval for all three edition repos.
- Release publication manifest generation now verifies all four local artifact records against `echo-release.json`, records empty public URLs in the blocked template, and keeps download verification plus Release Index patch approval as explicit blockers.
- Public download verification now has a guarded command path that requires four HTTP(S) URLs, downloads each artifact, writes saved verification artifacts, and emits a `verified` manifest while still blocking patch approval.
- Release publication approval now has a guarded command path that requires a verified manifest, approval/signoff JSON, a patch id, a release index commit, and explicit patch intent before writing an approved manifest or mutating `echo-release.json`.
- Release publication rehearsal generation now copies the four local artifacts to a saved local download cache, verifies their hashes and sizes, emits Release Index patch-preview JSON, and keeps public URL, patch approval, and distribution approval blockers intact.
- Edition manifest index preview generation now writes the distribution approval saved-artifact trio: `edition-manifest-index-report.json`, `module-requirement-resolution.json`, and `launcher-channel-listing.json`, plus per-edition modpack entry previews, while keeping real launcher channel indexing and distribution approval blockers intact.
- All adapters must call the shared `OpenlandsFirstHourRuntime` hooks and attach runtime-core reports proving Standard rules, starter spawn, shelter score, waystone transitions, homestead crop/cookpot behavior, builder/storage action validation, first-hour route order, and adapter readiness.
- Gameplay catalog generation must stay in lockstep with block, item, recipe, loot, first-hour, and roadmap changes so the ECHO Index/Guide layer can explain every MVP block and item without runtime-specific naming drift.
- Production phase matrix generation must stay in lockstep with phase evidence, edition reports, artifact outputs, and roadmap blockers so all 55 production checkpoints remain machine-checkable.
- Runtime execution acceptance must stay in lockstep with the production matrix runtime gates so adapters know exactly which reports clear real execution blockers.
- Runtime execution harness plan must stay in lockstep with runtime execution acceptance so every real scenario has a driver surface, runnable action list, assertion list, required captures, and saved artifacts before any runtime gate can clear.
- Local runtime rehearsal now validates all 17 runtime execution scenarios, current fixture hashes, pure runtime hook proofs, scenario captures, and saved preflight artifacts for all three edition repos while keeping real adapter runtime gates uncleared.
- Harness driver manifest generation must stay in lockstep with all harness plans so each edition reports exactly which real driver surfaces are implemented, which are missing, and why blocked harness reports cannot clear gates yet.
- Edition release manifest templates must continue to match the shared runtime adapter load plan.
- Edition runtime evidence templates must continue to match all runtime evidence IDs, first-hour scenarios, save/load checkpoints, and the public-alpha waystone scenario.
- Legal/content audit must keep public Openlands identity free of prohibited Minecraft-owned names, copied assets, copied silhouettes, copied recipe identity, and borrowed branding.
- Legal/content preflight now scans public names, canonical IDs, recipe/station identity, asset paths, generated artifact paths, and adapter descriptor public fields for each edition.
- Final block/item textures, models, blockstates, icons, and sounds must replace the owned placeholder coverage before public release.
- First-hour playtest must prove spawn, gather, shelter, sleep, exploration, waystone repair, HoloMap reveal, and save/load.
- First-hour preflight now keeps the machine-readable first-hour fixture aligned with route steps, registry IDs, runtime actions, save fields, HoloMap fields, and compiled artifact contents for all three edition repos.
- Waystone save/load preflight now keeps waystone states, repair inputs, two-active fast travel, HoloMap persistence, and multiplayer permission fields aligned for all three edition repos.
- Launcher-flow preflight now validates compiled local artifacts, SHA-256/size metadata, descriptor coverage, and install/update/repair/rollback check mapping for all three edition repos.
- Local launcher rehearsal now validates local artifact cache install, update replacement, corrupt-artifact repair, rollback manifest snapshotting, descriptor inspection, saved rehearsal artifacts, and Openlands Standard world/config preservation for all three edition repos while keeping real launcher gates uncleared.
- Launcher must still pass real install, update, repair, and rollback execution for all three edition repos before Public Alpha.
- Launcher flow acceptance must keep install/update/repair/rollback checks aligned with artifact targets, Release Index warning state, real launcher execution reports, and save/config preservation fields.
- Launcher execution harness plan must stay in lockstep with launcher execution acceptance so every launcher flow has driver surfaces, runnable preconditions, action/assertion lists, required captures, saved artifacts, and world-state policies before launcher gates can clear.
- Launcher execution acceptance must keep blocked reports honest until real ECHO Launcher runs clear install, update, repair, rollback, and world/config preservation gates.
- Final release review acceptance must keep blocked reports honest until human art/audio/legal review clears public identity, asset, sound, and generated-output gates.
- Distribution approval acceptance must keep blocked reports honest until Release Index URLs, verified download hashes, indexed edition manifests, co-op public-alpha session evidence, and release approval clear distribution gates.
- Release publication manifest acceptance must keep the Release Index in warning until every artifact has a public URL, a downloaded-back SHA-256/size match, a saved verification artifact, and an approved Release Index patch. The public verifier can prove downloaded-back bytes only; it cannot clear Release Index patch approval or distribution approval. The approval tool can apply the patch only with explicit signoff; it cannot replace the signoff. The local rehearsal can prove the mechanics only; it cannot clear the public URL or approval gates.
- Release Index warning entries must stay warning until real artifacts, checksums, public URLs, download verification, patch approval, and distribution approval exist.

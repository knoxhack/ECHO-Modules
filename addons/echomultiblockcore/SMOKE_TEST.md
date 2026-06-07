# ECHO: MultiblockCore Smoke Test

## Build Gates

1. `.\gradlew :echomultiblockcore:clean :echomultiblockcore:build --stacktrace`
2. `.\gradlew :echomultiblockcore:runGameTestServer --stacktrace`
3. `.\gradlew :echoconvoyprotocol:compileJava --stacktrace`
4. `.\gradlew :echoindustrialnexus:compileJava --stacktrace`
5. `.\gradlew -PechoAddonSet=all buildEchoWorkspace --stacktrace`

The v1.3.0 Full Integration acceptance gate is MultiblockCore build/GameTests, optional-runtime GameTests, and full all-addon workspace compile confidence. Treat new all-addon failures as caused by this pass unless proven otherwise.

The automated MultiblockCore gate currently runs 44+ required GameTests. In a dirty workspace, if unrelated addon sources or resources block all-addon gates, record the exact external error and continue using targeted MultiblockCore compile/GameTest gates until those blockers are assigned.

Optional Terminal/Lens/HoloMap runtime companions are not loaded by default for the module gate. To smoke-test those addons beside MultiblockCore, add `-PechoMultiblockIncludeOptionalRuntime=true`.

## Signal Tower

1. Launch a dev client with the beta addon set.
2. Give yourself `echomultiblockcore:signal_tower_blueprint`, `signal_tower_core`, `reinforced_frame`, and `signal_conduit`.
3. Hold the blueprint and confirm per-cell build-assist outlines appear near the targeted block face.
4. Use the build-assist rotate, layer up/down, and mirror controls; mirror should only affect mirrorable definitions.
5. Confirm the HUD shows structure name, anchor mode, rotation/layer state, completion estimate, inventory-aware exact material counts, and top material/diagnostic lines.
6. Place only the controller and right-click it.
7. Confirm diagnostics show `STRUCTURE STATUS: INCOMPLETE`, grouped missing blocks, and completion percent.
8. Complete the shape from `data/echomultiblockcore/echo_multiblocks/signal_tower_tier_1.json`.
9. Right-click the controller and confirm `STRUCTURE LINK ESTABLISHED`.
10. Break a required frame or conduit, then right-click or wait for revalidation.
11. Confirm the controller moves to damaged/incomplete/offline diagnostics rather than rescanning every tick.
12. Replace the block and right-click to reform.

## Industrial Assembly Line

1. Build the shape from `industrial_assembly_line.json`.
2. Install a Welder Head or Assembler Head into the Robotic Arm.
3. Put 4 Reinforced Frame blocks and 1 Signal Conduit into the Input Crate.
4. Run `/echo_multiblock recipes` and confirm the Assembly Suite recipes are listed.
5. Run `/echo_multiblock task start assemble_reinforced_machine_frame` while standing near the controller.
6. Confirm the task starts, the robot gets an animation/effect packet, heat increases, and the Output Crate receives one Reinforced Machine Frame.
7. Save and reload the world.
8. Confirm controller state, robotic tool head, heat/cooling state, crate inventory, and task queue state persist.

## v1.2 Facility Core Checks

1. Sneak-right-click a controller and confirm the first-party controller screen opens.
2. Use the screen recipe pager, recipe rows, and buttons to queue specific recipes beyond the first page, validate, start the deterministic default recipe, pause/resume, retry blocked work, clear the queue, queue repair, and run auto-builder.
3. Run `/echo_multiblock upgrades list`, install `echomultiblockcore:speed_upgrade`, and confirm `/echo_multiblock snapshot` reports installed upgrades.
4. Temporarily remove the Power Bus or Data Bus from the Industrial Assembly Line and confirm capability diagnostics block recipes instead of consuming inputs.
5. Place `echomultiblockcore:auto_builder` in the optional assembly-line slot, put missing exact-block materials in the Input Crate, break a required exact block, then run `/echo_multiblock autobuild`.
6. Confirm the Auto Builder consumes the material, places only a valid missing cell, refuses unloaded/occupied cells, and reports progress in diagnostics.
7. Use `/echo_multiblock set echomultiblockcore:logistics_depot`, then build/form/break/reform each showcase JSON facility:
   `logistics_depot`, `scanner_array`, `vehicle_repair_gantry`, `orbital_launch_platform`, `archive_data_chamber`, `agriculture_dome`, `nexus_stabilizer`, `armory_fabricator`, and `auto_builder_yard`.
8. Confirm robotic task packets still show visible spark/beam feedback and no dedicated-server client classloading errors occur.

## v1.2 Facility Progression Checks

1. Craft or give every facility blueprint: Signal Tower, Industrial Assembly Line, Logistics Depot, Scanner Array, Vehicle Repair Gantry, Orbital Launch Platform, Archive Data Chamber, Agriculture Dome, Nexus Stabilizer, Armory Fabricator, and Auto Builder Yard.
2. Confirm each blueprint tooltip shows a material summary and the corresponding vanilla advancement appears when the blueprint enters inventory.
3. Run `/echo_multiblock progression list` and confirm all showcase facilities appear with tier numbers.
4. Run `/echo_multiblock progression info echomultiblockcore:scanner_array` and confirm prerequisites, featured recipes, rewards, and guide text are printed.
5. Stand near a formed controller and run `/echo_multiblock progression next`; confirm it recommends the next tier after the current facility.
6. For each showcase facility, build/form it with its blueprint and run its featured recipe once:
   `tune_signal_beacon`, `assemble_reinforced_machine_frame`, `stage_supply_manifest`, `run_scanner_sweep`, `overhaul_vehicle_frame`, `calibrate_launch_guidance`, `encode_archive_memory_cell`, `cultivate_reclamation_matrix`, `stabilize_nexus_field_coil`, `fabricate_armory_pattern_core`, and `stage_construction_planner`.
7. Confirm each featured reward item appears in the Output Crate and unlocks the corresponding recipe-completion advancement.
8. Sneak-right-click a controller and confirm the screen reports progression tier, featured recipe count, queue capacity, blocked task count, capability diagnostic count, and installed/available upgrade slots.
9. Run `/echo_multiblock snapshot` and confirm runtime/status snapshots include progression title/tier/featured recipe details.
10. Temporarily add malformed `data/<namespace>/echo_multiblock_progression/bad.json`, reload, and confirm the bad progression file is skipped without blocking normal multiblock definitions or automation recipes.

## Blocked Task Cases

1. Start the assembly task with missing input and confirm `TASK BLOCKED` names the missing stack.
2. Add the missing materials and wait a few seconds or run the task command again; confirm the queued task retries.
3. Fill the Output Crate, start the task, and confirm it blocks with `Output crate is full` without consuming input.
4. Remove the Welder/Assembler head and confirm diagnostics name the required tool.
5. Reinstall the tool and confirm the same task can start.
6. Use `/echo_multiblock task pause` and confirm tasks stop retrying.
7. Use `/echo_multiblock task resume` and confirm waiting work can continue.
8. Use `/echo_multiblock task retry_blocked` after fixing inputs/output/tooling.
9. Use `/echo_multiblock task clear` and confirm the queue is empty.

## Automation Effects

1. Confirm `/echo_multiblock recipes` lists recipes with optional effect metadata where supplied by addons.
2. Start a Convoy Protocol automation recipe that declares an effect, such as `echoconvoyprotocol:load_field_supply_crate`.
3. Confirm standard MultiblockCore transaction behavior still gates robot/tool/input/output checks.
4. Confirm Convoy state changes happen only after the task completes.
5. Temporarily reference an unregistered effect id in a datapack recipe and reload.
6. Confirm the recipe remains valid, the task can run as a no-op effect, and diagnostics/logs explain that no handler was registered.
7. Temporarily add a malformed effect id and confirm reload reports a warning without discarding the recipe.

## JSON Reload Safety

1. Add a temporary datapack definition with a malformed palette key or duplicate workcell id.
2. Reload datapacks.
3. Confirm the log includes the resource path, definition id when available, and field-path-like error text.
4. Confirm the bad definition is skipped and existing good definitions remain available.
5. Add a temporary `data/<namespace>/echo_multiblock_tasks/bad.json` with an unknown item id.
6. Reload datapacks and confirm only that recipe is skipped while valid recipes remain listed by `/echo_multiblock recipes`.
7. Add a temporary malformed progression file with a self-prerequisite and confirm reload reports only that progression error.
8. Remove the malformed files and reload again.

## Unloaded Chunk / Bounds

1. Create or edit a test definition whose shape would extend into unloaded chunks or beyond the configured max volume.
2. Validate it from a controller.
3. Confirm diagnostics report unloaded-area warnings or max-volume rejection instead of forcing chunk reads.

## Debug Commands

- `/echo_multiblock list`
- `/echo_multiblock validate`
- `/echo_multiblock form`
- `/echo_multiblock break`
- `/echo_multiblock info`
- `/echo_multiblock set echomultiblockcore:logistics_depot`
- `/echo_multiblock upgrades list`
- `/echo_multiblock upgrades install speed_upgrade`
- `/echo_multiblock upgrades remove_last`
- `/echo_multiblock autobuild`
- `/echo_multiblock task start assemble_reinforced_machine_frame`
- `/echo_multiblock task list`
- `/echo_multiblock task clear`
- `/echo_multiblock task pause`
- `/echo_multiblock task resume`
- `/echo_multiblock task retry_blocked`
- `/echo_multiblock recipes`
- `/echo_multiblock progression list`
- `/echo_multiblock progression info echomultiblockcore:scanner_array`
- `/echo_multiblock progression next`
- `/echo_multiblock materials echomultiblockcore:signal_tower_tier_1`
- `/echo_multiblock robotics list`
- `/echo_multiblock preview echomultiblockcore:signal_tower_tier_1`
- `/echo_multiblock integrations`
- `/echo_multiblock snapshot`
- `/echo_multiblock scan`
- `/echo_multiblock markers`

## Integration Core Checks

1. Form a Signal Tower or Industrial Assembly Line.
2. Run `/echo_multiblock integrations` and confirm the default Terminal, Lens scan, DataCore, and map marker providers are registered once.
3. Run `/echo_multiblock snapshot` near the controller and confirm state, integrity, task count, warning count, dimension, role, and category are reported.
4. Run `/echo_multiblock scan` while targeting or standing near a controller, then near a Robotic Arm, and confirm diagnostics/status are returned.
5. Run `/echo_multiblock markers` and confirm formed structures produce stable marker ids, dimension, title, role, state, and color.
6. Save/reload the world and rerun `snapshot` and `markers`; saved-data entries should still be exposed even before the controller chunk is actively ticking.

## Terminal Bridge Checks

1. Launch with `-PechoMultiblockIncludeOptionalRuntime=true` so Terminal is present beside MultiblockCore.
2. Confirm logs include `ECHO MultiblockCore terminal automation bridge registered`.
3. Open Terminal recipe index data and confirm Multiblock Automation recipes appear.
4. Trigger a Terminal action payload for `echomultiblockcore:start_task` with `dimension`, `controller_pos`, and `recipe_id`; confirm the same validation rules as `/echo_multiblock task start`.
5. Try malformed payloads, missing controllers, out-of-range controllers, and unknown recipe ids; confirm they are ignored safely.

## Provider Safety Checks

1. Add or simulate a duplicate provider id in a dev addon and confirm the second registration is ignored with a clear warning.
2. Add or simulate a provider that throws during aggregation and confirm commands still return output from healthy providers.
3. Confirm no Terminal, Lens, HoloMap, MissionCore, or DataCore jars are required for MultiblockCore to load.

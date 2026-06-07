# ECHO: MultiblockCore - Completion Audit

## Baseline (current workspace)

- 143 Java source files
- 11 multiblock definitions (10 route-specific facility templates + 1 assembly line)
- 25 automation recipes
- 11 progression entries
- 4 upgrades
- 47 crafting recipes
- 11 block loot tables
- 44 GameTest registrations
- MultiblockCore compile passes without optional Terminal, RuntimeGuard, or RenderCore source compilation
- v1.3.0 adds facility route/stage metadata, expanded handoff recipes, additive snapshot fields, richer Terminal/Lens/HoloMap hints, and optional dependency alignment.

## Feature Surface vs Claims

| Feature | Status | Notes |
|---------|--------|-------|
| JSON definition loader | OK | Isolated bad files, keeps last-good registry |
| Validation engine | OK | Rotations, mirror, unloaded chunks, max volume, foundation warnings |
| Validation cache | OK | Versioned key, TTL, force option, dirty flag |
| Controller block entity | OK | Tick, form, break, integrity, upgrades, task queue, auto-builder, diagnostics |
| Task queue | OK | WAITING/BLOCKED/ACTIVE/PAUSED/COMPLETED/FAILED/RETRYING, 8 max persisted |
| Automation transaction | OK | Consume + produce with rollback on failure |
| Robotic arm BE | OK | Tool install/remove, heat, cooling, state machine, persistence |
| Input/output crates | OK | 18-slot, insert/consume/extract, matching predicate, status line |
| Blueprint item + build assist | OK | Tooltip, material summary, use-on-controller, preview renderer, keys |
| Controller screen | OK | 8 buttons, data sync, paged recipe selector rows, queue/capability/upgrade counters, progression line |
| Commands | OK | validate, form, break, info, set, task, recipes, progression, upgrades, autobuild, materials, robotics, integrations, snapshot, scan, markers, preview |
| Auto-builder service | OK | Plans missing blocks, consumes exact items from input crate, respects permissions |
| Capability service | OK | Discovers nodes from matched blocks, evaluates recipe + definition requirements |
| Upgrade runtime | OK | JSON definitions, install/remove via commands, snapshot visibility |
| Progression system | OK | Tier, prerequisites, featured recipes, rewards, guide text |
| Optional integrations | OK | Terminal (reflective), Lens (reflective), HoloMap (via EchoCore), DataCore, RenderCore, RuntimeGuard, MissionCore |
| GameTests | OK | 44 registrations covering parse, validation, robotics, tasks, effects, build assist, progression, facility identity, in-world showcase formation, featured recipe transactions, auto-builder refusal/no-loss behavior, and recipe metadata |

## Gaps / Follow-Ups

### 1. IDs and naming
- `industrial_assembly_line` is the canonical first-party facility id.
- `industrial_assembly_line_demo` remains registered as a compatibility alias in `MultiblockContent` so existing worlds/datapacks do not break.

### 2. Showcase facility depth
- All 10 non-assembly facilities are 3x2x3 with nearly identical palette (frame + controller + crate + bus + robot + optional auto-builder).
- They now differ through role, category, capability requirements, upgrade slot rules, workcell tools, allowed task ids, and featured recipe/progression text.
- **Status**: resolved for this pass. Current facilities keep compact footprints for tier-1 accessibility while their runtime identity comes from `capability_requirements`, `upgrade_slots`, workcell metadata, and featured recipes.

### 3. Datapack wording
- Active progression, task, and upgrade text now describes current concrete behavior.
- **Status**: resolved for this pass.

### 4. Optional dependency metadata
- Optional dependency reasons use present-tense integration language.
- Terminal integration is live through recipe provider, addon info, and action handlers.

### 5. Schema completeness
- `multiblock_definition.schema.json` now covers workcells, capability requirements, upgrade slots, robotics, integrity, preview, and palette fields.
- Added schemas for `echo_multiblock_tasks`, `echo_multiblock_progression`, and `echo_multiblock_upgrades`.
- Added datapack examples for definitions, tasks, progression, and upgrades.
- Updated the schema index.

### 6. Recipe selection UX
- Controller screen now exposes paged synced recipe rows and queues the selected allowed recipe through the menu button path.
- The operation panel reports queue capacity, active/blocked task counts, capability diagnostics, repair actions, and installed/available upgrade slots.
- The `START` button remains as a deterministic default sorted by repair priority and id.
- `/echo_multiblock task start <recipe>` still accepts any allowed recipe from commands.

### 7. Automated and manual smoke coverage
- Automated GameTests now cover in-world formation for every showcase facility, one featured transaction per facility, recipe metadata, task states, build assist, provider isolation, progression, and auto-builder occupied/wrong-block no-loss refusal.
- Manual smoke remains for client rendering, controller/crate UI feel, blueprint preview controls, and optional companion addons in a real client.
- Remaining deeper QA: save/reload queue persistence and live malformed datapack reload isolation.

### 8. Missing crate menu
- `MultiblockCrateBlockEntity.createMenu` now opens the 18-slot crate menu.
- **Status**: resolved for this pass; manual smoke should still verify insertion, extraction, and quick-move behavior.

### 9. RenderCore / animation safety
- `EchoMultiblockCoreClient` and `MultiblockControllerBlockEntity` use reflection for RenderCore and Terminal. This is correct for optional deps.
- Need to verify no client class is referenced on dedicated server paths. Current code looks safe (reflective invoke, `level.isClientSide()` guards).

## Punch List (execution order)

- [x] 1. **Rename `industrial_assembly_line_demo` -> `industrial_assembly_line`**
  - Compatibility alias map added to `MultiblockContent`.
  - All first-party references updated (blocks, items, recipes, progression, lang, advancements, tests, docs).
  - Old saves/datapacks with `industrial_assembly_line_demo` still resolve correctly.

- [x] 2. **Refresh active text**
  - Rewrote progression guides for orbital, armory.
  - Rewrote task notes for nexus coil.
  - Updated `neoforge.mods.toml` optional reasons.

- [x] 3. **Add distinct capability requirements to showcase facilities**
  - All 11 showcase facilities now have capability requirements.
  - All 11 showcase facilities now have at least 3 upgrade slots.
  - Workcells now declare task/tool identity for featured recipes.

- [x] 4. **Implement crate GUI**
  - Added `MultiblockCrateMenu` and `MultiblockCrateScreen`.
  - Registered menu type in `ModMenus`.
  - Wired `MultiblockCrateBlockEntity.createMenu` and `MultiblockCrateBlock` open on right-click.

- [x] 5. **Add missing schemas**
  - Update `multiblock_definition.schema.json` with full workcell/capability/upgrade/robotics fields.
  - Add `automation_recipe.schema.json`, `progression.schema.json`, `upgrade.schema.json`.
  - Add datapack examples and update schema index.

- [x] 6. **Add automated smoke GameTests**
  - Added facility identity coverage for every showcase facility.
  - Added in-world formation coverage for every showcase facility.
  - Added one featured recipe transaction path for every showcase facility.
  - Added occupied/wrong-block auto-builder refusal coverage that verifies materials are not lost.
  - Added recipe metadata coverage for allowed multiblock ids used by the controller UI.
  - Existing tests still cover parse, validation, transactions, blocked full-output, task states, provider isolation, build assist, progression, and MissionCore content.
  - Remaining deeper QA: save/reload queue persistence and live malformed datapack reload isolation.

- [ ] 7. **Final gates**
  - `:echomultiblockcore:build`
  - `:echomultiblockcore:runGameTestServer`
  - `:echomultiblockcore:runGameTestServer -PechoMultiblockIncludeOptionalRuntime=true`
  - Dependent addon compiles
  - `buildEchoWorkspace`
  - Current targeted compile status: `:echomultiblockcore:compileJava` passes without optional-addon exclusions.
  - Current targeted GameTest status: `:echomultiblockcore:runGameTestServer` passes 44 required tests without optional-addon exclusions.
  - Current local resource status: 261 MultiblockCore JSON resources parse successfully.
  - Current root resource blocker: PowerGrid has 21 recipe JSON files with UTF-8 BOMs.
  - Current external compile blockers: RuntimeGuard has a compact source/package declaration error in `IntegrationThrottleService.java`; RenderCore has a missing `v21ScreenChromeEvidence(GameTestHelper)` GameTest method reference; dirty NetCore sources have missing EchoCore network/config APIs under forced rebuild.

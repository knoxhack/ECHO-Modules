<!-- CURSEFORGE_README_START -->
# MissionCore by ECHO Labs

![MissionCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echomissioncore/brand-sheet.png)

**Mission backend for chapter definitions, objectives, progression, rewards, actions, and Terminal mission feeds.**

![MissionCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echomissioncore/features-portrait.png)

![MissionCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echomissioncore/features-landscape.png)

## CurseForge Summary

Mission backend for chapter definitions, objectives, progression, rewards, actions, and Terminal mission feeds.

## Main Features

- Mission chapters, phases, definitions, objective types, rewards, completion rules, and repeat policies.
- Core service hooks for no-op-safe mission registration and progress reporting.
- Terminal feed support for shared mission presentation.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echomissioncore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echomissioncore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echomissioncore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: MissionCore

MissionCore is the shared backend for ECHO missions, objectives, rewards, and Terminal mission feeds.

Version `1.0.0` is the direct hook hardening release. Existing addon
`TerminalMissionProvider` mission sets still register into MissionCore without
changing their public mission IDs, while real server-side gameplay events now record
objective progress directly through Core service helpers.

## Addon Registration

Addons should depend on `echocore` and register content through Core:

```java
EchoCoreServices.registerMissionContent("myaddon", registry -> {
    Identifier chapterId = Identifier.fromNamespaceAndPath("myaddon", "field_ops");
    registry.registerChapter("myaddon", new MissionChapterDefinition(
            chapterId, "Field Ops", "Addon objectives.", 50, 0x55FFDD));

    registry.registerMission("myaddon", MissionDefinition.builder(
                    Identifier.fromNamespaceAndPath("myaddon", "first_signal"), chapterId)
            .phase("field_ops", "Field Ops", 0, 1)
            .text("First Signal", "Scan the first signal.", "Signal archived.")
            .objective(ObjectiveDefinition.simple(
                    Identifier.fromNamespaceAndPath("myaddon", "first_signal/scan"),
                    MissionObjectiveType.SCAN_BLOCK,
                    "Scan signal block",
                    "",
                    ItemStack.EMPTY,
                    1))
            .reward(RewardDefinition.item(
                    Identifier.fromNamespaceAndPath("myaddon", "first_signal/reward"),
                    MissionRewardClaimMode.CLAIMABLE,
                    new ItemStack(Items.EMERALD)))
            .build());
});
```

Gameplay code should report progress through `EchoCoreServices.recordMissionObjective(...)`. MissionCore safely no-ops when it is not loaded.

Hook targets should be stable addon namespace IDs:

```java
Identifier target = MissionHookTargets.objectiveTarget(
        "myaddon",
        Identifier.fromNamespaceAndPath("myaddon", "first_signal"),
        "scan");

EchoCoreServices.recordMissionObjective(
        player,
        MissionObjectiveType.SCAN_BLOCK,
        target,
        1,
        MissionHookTargets.context("myaddon", missionId, "action", "scanner"));
```

The target convention is `<addon>:mission/<legacy_mission>/<objective_key>`.
Context maps should include `source`, `legacy_mission`, and one gameplay detail such
as `route`, `machine`, `region`, or `action`.

## Custom Addon Actions

Static JSON missions keep the built-in `start`, `complete`, and `claim` actions. Java
registrations can add addon-specific actions, such as scan, decode, route, or path
choice buttons:

```java
registry.registerMission("myaddon", MissionDefinition.builder(id("decode_cache"), chapterId)
        .text("Decode Cache", "Decode the recovered cache.", "Cache decoded.")
        .actionProvider((player, mission, status, completeNow) ->
                List.of(MissionActionView.enabled("decode_cache", "Decode")))
        .actionHandler((player, mission, actionId) ->
                "decode_cache".equals(actionId) && LegacyTerminalActions.decodeCache(player))
        .build());
```

MissionCore merges custom `MissionActionView`s into Terminal snapshots and delegates
unknown action ids to the mission action handler. Complex actions should stay in Java
adapters; JSON intentionally does not deserialize arbitrary executable handlers.

## JSON Content

Datapacks can register chapters under:

- `data/<namespace>/missioncore/chapters/*.json`
- `data/<namespace>/missioncore/missions/**/*.json`

Mission rewards support `immediate` and `claimable` modes. Objective `type` accepts the shared MissionCore objective ids such as `obtain_item`, `place_block`, `kill_entity`, `establish_route`, and `unlock_research`.
JSON missions can also carry string metadata, including `terminal_route_phase`, `terminal_route_order`, `terminal_route_role`, and `terminal_route_visible`, so static side ops can align with the shared Terminal route without Java-only presentation glue. Reward `mode` is accepted as an alias for `claimMode`, and reward `xp` / `reputation` values are preserved as metadata for owning addons or datapack-aware integrations.

## Validation and Debug

MissionCore is server-authoritative. Operators can inspect and test content with:

- `/echomission list`
- `/echomission inspect <mission>`
- `/echomission start <mission>`
- `/echomission progress <mission> <objective> <amount>`
- `/echomission record <objective_type> <target> <amount>`
- `/echomission complete <mission>`
- `/echomission claim <mission>`
- `/echomission validate`
- `/echomission reload`

JSON missions with duplicate ids, missing chapters, broken prerequisites, unknown objective types, unknown reward modes, or invalid reward items are skipped with warnings instead of crashing world load.

`/echomission validate` also reports migrated-source hook coverage as
`direct-hooks`, `adapter-state`, or `mixed`. Direct hooks mean gameplay events can
advance MissionCore without Terminal snapshots; adapter-state remains supported for
legacy compatibility.
MissionCore validation warnings are also published through ECHO Core diagnostics so Terminal/command surfaces can show mission data issues without depending on RuntimeGuard.

## Migration Notes

When ECHO Terminal is installed, MissionCore registers one shared mission feed. Legacy addon Terminal mission providers should skip their direct mission provider registration when `echomissioncore` is loaded, while keeping non-mission dashboards, archives, and actions registered. Existing addon save data remains authoritative until its adapter mirrors completion and reward claims into MissionCore.

The 1.0.0 hook pass covers Reclamation, Industrial, Convoy, Orbital, Nexus,
Blackbox, and Stationfall provider migration. Each adapter preserves the
`TerminalMissionDefinition.id()` as the MissionCore mission id, delegates custom
Terminal action ids back to the legacy provider logic, and records direct objective
hooks where stable server-side gameplay events exist.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echomissioncore.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echomissioncore.md`.

<!-- CURSEFORGE_README_START -->
# WorldCore by ECHO Labs

![WorldCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoworldcore/brand-sheet.png)

**Foundation module for region definitions, world markers, hazard snapshots, structure discovery, and world event contracts.**

![WorldCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoworldcore/features-portrait.png)

![WorldCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoworldcore/features-landscape.png)

## CurseForge Summary

Foundation module for region definitions, world markers, hazard snapshots, structure discovery, and world event contracts.

## Main Features

- Data-driven world regions.
- Hazard definitions.
- Datapack override support.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoworldcore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoworldcore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoworldcore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: WorldCore

WorldCore is a foundation module for the ECHO ecosystem. It does not own
Ashfall world generation, Convoy routes, Orbital debris generation, Nexus
structures, chapter region definitions, or any player craftable content. It
provides the shared vocabulary and runtime services that those systems use to
describe the same world safely.

## What WorldCore Provides

- Region definitions and active region lookup.
- Hazard definitions and current hazard snapshots.
- Persistent world markers for structures, crash sites, routes, debris, outposts,
  and anomalies.
- Per-player region discovery through ECHO Core discovery data plus WorldCore
  SavedData.
- Runtime bus events for region enter/discover/scan, marker reveal, and hazard
  changes.
- Optional Terminal status and HoloMap feed support through ECHO Core services.
- Permission-gated `/echoworld` validation and inspection commands.
- RenderCore profile resources for shared region and hazard categories.
- Forward-compatible AudioCore ambience profile resources for shared region
  `audioProfileId` values.

## Public Services

Use ECHO Core accessors instead of depending on the implementation class:

- `EchoCoreServices.worldRegions()`
- `EchoCoreServices.worldContext(player)`
- `EchoCoreServices.worldValidationReport(level)`
- `EchoCoreServices.regionService()`
- `EchoCoreServices.hazardService()`
- `EchoCoreServices.worldMarkerService()`
- `EchoCoreServices.structureDiscoveryService()`

WorldCore 1.0.0 adds a compact world context snapshot and structured validation
report while preserving the older service methods. Existing addons can keep
calling region, hazard, marker, and discovery services directly; newer consumers
should prefer the snapshot/report helpers for UI, diagnostics, Lens, SoundCore,
TutorialCore, and RuntimeGuard surfaces.

When WorldCore is absent, these resolve to `NoOpWorldService`, so optional
integrations can call them safely.

## Built-In Integrations

- Ashfall scanner discoveries are recorded as WorldCore structure markers.
- Convoy route start, checkpoint, and destination events create route markers.
- Orbital recovery and debris sites create persistent orbital markers.
- HoloMap consumes WorldCore regions, markers, and hazards through the WorldCore
  map data provider when WorldCore is installed.
- Terminal shows active regions, marker counts, hazard summary, validation state,
  and a HoloMap link when HoloMap is installed.
- DataCore subscribes to WorldCore runtime events and stores last region, marker,
  discovery, and hazard summary keys when DataCore is installed.
- MissionCore subscribes to WorldCore runtime events and records matching
  `enter_region`, `discover_structure`, and custom objective progress.
- Lens reads `WorldContextSnapshot` for current region, hazard, marker, and
  discovery context.
- SoundCore receives region and hazard context patches for ambience/music
  selection when installed.
- TutorialCore listens for WorldCore hazard changes and can surface lightweight
  contextual guidance.
- ECHO diagnostics include WorldCore validation issue counts, marker counts, and
  scan cadence for RuntimeGuard/status surfaces.

## Commands

Player-safe commands:

- `/echoworld current`
- `/echoworld hazard`
- `/echoworld nearby [radius]`

Debug/operator commands require game-master permission and
`debug.commandsEnabled=true`:

- `/echoworld validate`
- `/echoworld list [region|hazard|all]`
- `/echoworld markers [radius]`
- `/echoworld reveal <region_id>`

Debug commands include suggestions for list types and known region ids. The
validation command reports structured categories for reload warnings, duplicate
discovery ids, missing hazard references, marker references, source counts, and
optional integration status.

## Configuration

Common config keys:

- `runtime.playerScanIntervalTicks`
- `runtime.activeRegionRadius`
- `runtime.markerQueryRadiusCap`
- `debug.commandsEnabled`

Defaults preserve the initial WorldCore behavior.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echoworldcore.json`.
3. First action: run the documented command or trigger its in-world behavior.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echoworldcore.md`.

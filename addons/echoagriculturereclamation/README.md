<!-- CURSEFORGE_README_START -->
# Agriculture Reclamation by ECHO Labs

![Agriculture Reclamation by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoagriculturereclamation/brand-sheet.png)

**Field agriculture recovery with seed vaults, hydroponics, greenhouse zones, Pollinator Drones, and local soil restoration.**

![Agriculture Reclamation by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoagriculturereclamation/features-portrait.png)

![Agriculture Reclamation by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoagriculturereclamation/features-landscape.png)

## CurseForge Summary

Field agriculture recovery with seed vaults, hydroponics, greenhouse zones, Pollinator Drones, and local soil restoration.

## Main Features

- Greenhouse reclamation.
- Seed recovery and soil repair.
- Bio-domes in ashlands.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoagriculturereclamation/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoagriculturereclamation/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoagriculturereclamation/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: Agriculture Reclamation

Agriculture Reclamation is the ECHO field recovery chapter for ruined-world farming. It is a standalone NeoForge addon with mod id `echoagriculturereclamation`, package `com.knoxhack.echoagriculturereclamation`, and version `1.0.0`.

## Production Status

- Build and resources are wired into the beta and full ECHO stacks.
- The player route is usable in survival: recover a profiled seed, purify soil or use a Hydroponic Tray, grow and harvest crops, produce Bio-Gel or nutrient mix, stabilize genes, scan greenhouse safety, and raise chunk-local restoration pressure.
- Restoration stays local to blocks and chunks. It does not rewrite biome ids or restore vanilla ecology for free.
- Terminal and Core integration publish FIELD > Reclamation metrics, route records, diagnostics, recovery cache support, six route milestones, and optional Terminal Survival Route placement when the current Terminal API is present.
- The 1.0.0 integration pass turns Agriculture machines into block-entity backed workstations with a shared menu, persisted inventories, progress, blocked reasons, output buffering, and readable client diagnostics.
- The 1.0.0 integration pass adds read-only field snapshots, chunk-level field history, Core map markers, Lens scan rows, TutorialCore hints, WeatherCore growth pressure hooks, PowerGrid throughput diagnostics, SoundCore stingers, and data-driven process definitions while keeping standalone play intact.
- Cross-addon compatibility is optional and registry-id based for Ashfall ruined soils, Restoration Project-style soils, Nexus restore alignment, and ECHO faction preferences.

## Player Smoke Route

1. Recover a `Recovered Seed Capsule` from ECHO ruin loot, or craft one from wheat seeds, bone meal, a glass bottle, and copper.
2. Use the `Seed Vault Terminal` or direct capsule use to get a profiled `Contaminated Seed`.
3. Plant the profiled seed on dirt, grass, farmland, or compatible reclamation soil, or insert it into a `Hydroponic Tray`.
4. Grow and harvest at least one crop output.
5. Craft a `Bio-Reactor` with `Soil Nutrient Mix`, then convert any crop matter into `Bio-Gel`.
6. Craft and use `Gene Stabilizer` with a contaminated seed plus `Gene Sample` or `Bio-Gel`.
7. Build a sealed greenhouse with glass, filters, dock support, trays, and controller scan.
8. Mature restoration crops and scan ecology until local soil conversion pressure is visible.

## Crop Utility Notes

- `Medicinal Aloe` feeds the Bio-Reactor for Bio-Gel and can award Ashfall bandage output when that addon is loaded.
- `Signal Fungus` is a stronger Bio-Reactor input and also composts into extra nutrient mix.
- `Cryo Moss` converts to Bio-Gel plus Purification Enzyme in the Bio-Reactor or extra nutrient mix in the Compost Recycler.
- `Filter Reed` composts into extra nutrient mix, can award Ashfall plant fiber when present, and crafts into paper for Spore Filter recovery.
- `Nexus Orchid` converts to Bio-Gel plus Gene Sample and can award Nexus gel when Nexus Protocol is loaded.

## Validation

Run from the workspace root:

```powershell
.\gradlew.bat :echoagriculturereclamation:build --warning-mode all
.\gradlew.bat :echoagriculturereclamation:runGameTestServer --warning-mode all
.\gradlew.bat :echoterminal:build --warning-mode all
.\gradlew.bat :echoterminal:runGameTestServer --warning-mode all
.\gradlew.bat -PechoAddonSet=beta validateEchoResources buildEchoWorkspace --warning-mode all
.\gradlew.bat -PechoAddonSet=all validateEchoResources buildEchoWorkspace --warning-mode all
.\gradlew.bat -PechoAddonSet=all validateReleaseArtifacts printReleaseManifest --warning-mode all
.\gradlew.bat -PechoAddonSet=all verifyEchoRelease --warning-mode all
```

If Python is not on `PATH`, pass `-PechoPythonExecutable="C:/path/to/python.exe"` to the Gradle validation commands.

## Release Notes

- The pollinator dock contributes greenhouse safety when it can service nearby crops or Hydroponic Trays; no drone entity is spawned.
- Greenhouse safety is enclosure-aware: open support helps, but a sealed Greenhouse Glass shell with overhead glass is required for full safe-envelope rating.
- Crop, soil, machine, and progression rules live under `data/echoagriculturereclamation/echoagriculturereclamation`.
- Global seed and gene recovery injections live under NeoForge `data/echoagriculturereclamation/loot_modifiers`.
- Mature crop loot is conservative: immature crops do not drop produce.
- Generic seed items must carry the `seed_profile` data component before planting or tray growth.
- 1.0.0 machines expose a shared menu and four-slot machine surface: input, catalyst, output, and auxiliary diagnostics. Existing pre-1.0.0 placed machines load as empty workstations.
- 1.0.0 process definitions live under `data/echoagriculturereclamation/echoagriculturereclamation/processes`; they feed Index/Terminal recipe visibility and machine diagnostics.
- 1.0.0 field snapshots are read-only API objects for integrations; Agriculture remains the owner of persistence, rewards, and gameplay state.
- 1.0.0 focuses on Terminal Survival Route placement and release readiness, not new Agriculture gameplay.
- Agriculture remains the owner of FIELD > Reclamation actions, rewards, support caches, and detailed diagnostics.
- No blocks, items, entities, recipes, save fields, data components, datapack schemas, or gameplay migrations are added in 1.0.0.

The 1.0.0 release note lives in the root docs at `../../docs/releases/agriculture_reclamation_1.2.0.md`.

## 1.0.0 Integration Focus

- Adds `ReclamationFieldSnapshot`, `ReclamationFieldQuery`, and `ReclamationIntegrationServices` as the public read-only integration surface.
- Adds optional Codec-backed field history for last scan time, greenhouse quality, restoration threshold, and blocker text; existing 1.0.0 worlds load with defaults.
- Adds process-definition datapack docs and Index recipe cards for Agriculture machines.
- Adds optional Core map data, Lens, TutorialCore, WeatherCore, PowerGrid, and SoundCore bridges. All fail closed when the sibling addon is absent.

## 1.0.0 Machine UX Focus

- Adds `ReclamationMachineMenu` and a shared machine screen for Seed Vault Terminal, Soil Purifier, Gene Stabilizer, Bio-Reactor, Compost Recycler, Greenhouse Controller, Pollinator Drone Dock, Spore Filter, and Ecology Scanner.
- Machines now persist input/catalyst/output slots, active process id, progress, last result, blocked reason, and next action text.
- Quick right-click use inserts the held valid item into the same block-entity process path used by the menu, so menu and fallback behavior stay aligned.
- Powered infrastructure remains optional; when PowerGrid is present and reports available power, process duration is reduced without making power mandatory.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echoagriculturereclamation.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echoagriculturereclamation.md`.

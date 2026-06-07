<!-- CURSEFORGE_README_START -->
# DataCore by ECHO Labs

![DataCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echodatacore/brand-sheet.png)

**Shared persistent data service for player, world, team, progression, sync, and addon-owned state.**

![DataCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echodatacore/features-portrait.png)

![DataCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echodatacore/features-landscape.png)

## CurseForge Summary

Shared persistent data service for player, world, team, progression, sync, and addon-owned state.

## Main Features

- Persistent player data.
- Shared records and diagnostics.
- No-op-safe service contracts.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echodatacore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echodatacore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echodatacore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: DataCore

ECHO: DataCore is the shared persistent data and progression layer for the ECHO/Ashfall addon ecosystem. It owns the concrete data service behind the lightweight contracts in ECHO: Core.

DataCore's required public dependencies are ECHO: Core and ECHO: NetCore. Addons should keep depending on Core contracts when DataCore is optional; DataCore supplies the concrete storage, diagnostics, migration, and sync backend when installed.

## Public Contract

Addons should depend on `echocore` and use `EchoCoreServices.dataService()` or the convenience methods:

- `EchoCoreServices.registerDataKey(IDataKey<T> key)`
- `EchoCoreServices.playerData(player)`
- `EchoCoreServices.worldData(level)`
- `EchoCoreServices.teamData(level, teamId)`
- `EchoCoreServices.dataSyncBridge()`

If DataCore is absent, Core returns `NoOpDataService`. Reads return key defaults, writes return `false`, and registered key metadata remains safe for debug display.

## Key Naming

Use stable ids in the owning addon namespace:

- `echoashfallprotocol:discovery/crash_site`
- `echoorbitalremnants:unlock/telemetry_tier`
- `echoarmory:research/armor_module`
- `echoconvoyprotocol:route/discovered_northern_freight`
- `echomissioncore:objective/first_signal`

Register keys during common setup:

```java
public static final IDataKey<Boolean> CRASH_SITE_DISCOVERED = IDataKey.flag(
    Identifier.fromNamespaceAndPath("echoashfallprotocol", "discovery/crash_site"),
    DataScope.PLAYER,
    false,
    true
);

EchoCoreServices.registerDataKey(CRASH_SITE_DISCOVERED);
```

Read and write through views:

```java
boolean discovered = EchoCoreServices.playerData(player).get(CRASH_SITE_DISCOVERED);
EchoCoreServices.playerData(player).set(CRASH_SITE_DISCOVERED, true);
```

## Save Format

Player data is stored in `player.getPersistentData()["echodatacore"]`:

- `version`: DataCore schema version. Current schema is `2`.
- `values`: map of key id to `{ kind, value, updatedGameTime }`.
- `migrations`: namespace-to-version migration markers.

World data is stored as `SavedData` id `echodatacore:data_world` with equivalent world values and team/base values. Team data uses a caller-supplied `Identifier` and is not bound to scoreboard teams. Schema v2 stores team data in validated team groups, preserves v1 delimiter-style `teamId|key` reads during migration, and records migration markers such as `echodatacore:team_values_v1`.

## Sync Format

DataCore sends `echodatacore:data_sync` from server to client. Packets include:

- scope: player, world, or team.
- owner id: player UUID, dimension id, or team id.
- full snapshot flag.
- revision.
- changed key entries, including clear/tombstone entries.

Repeated unchanged writes do not dirty state. Dirty player, world, and team keys are batched on the configurable tick interval and capped per packet to avoid network spam. Full sync requests still send immediate player/world/team snapshots.

## Datapack Key Metadata

DataCore loads simple metadata from:

- `data/<namespace>/echodatacore/data_keys/*.json`

Supported fields are `id`, `scope`, `kind`, `default`, `synced`, `title`, `description`, `owner`, `legacyRoot`, and `legacyField`. Java registrations remain authoritative for scope, kind, codecs, and conflicts. Datapacks may register simple `FLAG`, `COUNTER`, `STRING`, or `ENUM` keys, or enrich existing Java keys for Terminal and diagnostics. `RECORD` keys require a Java registration so the codec is explicit. Stale datapack-only metadata is removed on reload.

In 1.0.0, synced metadata is sent to clients on login, `/echodata sync full`, `/echodata metadata sync`, and datapack reload. Terminal and Index can therefore show datapack-created titles and legacy hints without hard depending on DataCore internals. Metadata with `synced=false` remains server-side.

The JSON schema is published at:

- `assets/echocore/schemas/datacore_key.schema.json`

### Authoring Examples

Player flag:

```json
{
  "id": "examplepack:discovery/crash_site",
  "scope": "PLAYER",
  "kind": "FLAG",
  "default": false,
  "synced": true,
  "title": "Crash Site Discovered",
  "owner": "examplepack"
}
```

World counter:

```json
{
  "id": "examplepack:world/markers_revealed",
  "scope": "WORLD",
  "kind": "COUNTER",
  "default": 0,
  "synced": true,
  "title": "Markers Revealed",
  "owner": "examplepack"
}
```

Team enum with legacy mirror hints:

```json
{
  "id": "examplepack:team/facility_mode",
  "scope": "TEAM",
  "kind": "ENUM",
  "default": "standby",
  "synced": true,
  "title": "Facility Mode",
  "owner": "examplepack",
  "legacyRoot": "examplepack_team_progress",
  "legacyField": "facility_mode"
}
```

Java-only record keys should be registered in code so their codec is explicit:

```java
public static final IDataKey<CompoundTag> ROUTE_STATE = IDataKey.record(
    Identifier.fromNamespaceAndPath("examplepack", "record/route_state"),
    DataScope.PLAYER,
    CompoundTag.CODEC,
    new CompoundTag(),
    true
);
```

## Legacy Saves

DataCore never deletes or rewrites legacy save roots. It exposes read-through/debug adapters for existing roots such as:

- `echocore_profile`
- `echocore_progress_ledger`
- `echocore_factions`
- `echoorbitalremnants_progress`
- `echoconvoyprotocol`
- `echoagriculturereclamation_progress`
- `echoindustrialnexus_progress`
- `echostationfall_progress`
- `echoblackboxprotocol_progress`
- `echonexusprotocol_progress`
- `echologisticsnetwork_progress`
- `signalos`

Reflection snapshots are provided for loaded Ashfall `QuestData`, Terminal `TerminalPlayerData`, and Nexus `NexusPlayerData` attachments without hard dependencies.

Legacy migration is opt-in through commands. Preview mode reports candidate keys without changing old roots; apply mode copies matching registered key values into DataCore, reports `candidate`, `already_mirrored`, `applied`, and `failed_decode` counts, and emits a `LEGACY_MIRROR` data-bus event for applied mirrors. Legacy roots remain untouched.

## Integration Notes

- Ashfall should register discovery/objective keys and keep `QuestData` authoritative until a safe write-through migration is desired.
- Orbital should register unlock counters/flags for telemetry tiers and route gates while legacy progress remains readable.
- Armory should store researched armor/module unlocks as DataCore player flags because current Armory state is item-component based.
- Convoy should register route discovery/completion keys and may keep route runtime state in its existing progress root.
- Terminal reads Core data contracts only; the built-in Data Core tab shows service status and registered keys.
- MissionCore should check objective flags through `EchoCoreServices.playerData(player)`.
- RenderCore and HoloMap should subscribe to `EchoDataBus` and refresh visual markers on `DataChangeMessage`.

## Debug Commands

- `/echodata status`
- `/echodata keys [namespace]`
- `/echodata keys page <page>`
- `/echodata key <id>`
- `/echodata metadata [namespace]`
- `/echodata metadata page <page>`
- `/echodata metadata sync [target]`
- `/echodata dirty`
- `/echodata inspect player [target]`
- `/echodata inspect world`
- `/echodata inspect team <teamId>`
- `/echodata legacy player [target]`
- `/echodata migrate preview player <target> [namespace]`
- `/echodata migrate preview world`
- `/echodata migrate preview team <teamId>`
- `/echodata migrate apply player <target> [namespace]`
- `/echodata sync full [target]`
- `/echodata flag set <key> <true|false>`
- `/echodata flag unset <key>`

Inspect commands require gamemaster permission. Mutating flag and migration apply commands also require `debug.commandsEnabled=true` or an integrated server.

## Manual Smoke

1. Join a world and open Terminal's Data Core tab.
2. Confirm the backend status, revision, registered key count, metadata count, player/world values, and recent changes.
3. Run `/echodata flag set echodatacore:debug/smoke true`, then `/echodata flag unset echodatacore:debug/smoke`, and confirm Terminal updates after the sync interval.
4. Reveal or enter a WorldCore region and confirm DataCore world/player values update.
5. Run `/echodata legacy player <target>` and `/echodata migrate preview player <target> [namespace]`.
6. Reload the world and confirm player, world, and team values persisted.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echodatacore.json`.
3. First action: review the API/tooling docs before using it in a public pack.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echodatacore.md`.

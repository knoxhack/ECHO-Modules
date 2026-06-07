# WorldCore Data Formats

WorldCore loads datapack definitions from:

- `data/<namespace>/echoworldcore/world_hazards/**/*.json`
- `data/<namespace>/echoworldcore/world_regions/**/*.json`

The `id` field is optional. If omitted, the file path becomes the id. Use an
explicit `id` when describing a region owned by another ECHO chapter.

Ownership convention:

- WorldCore owns shared hazard ids under `echoworldcore:hazard/...`.
- Chapter and addon regions live in the owning addon namespace and resources.
- Examples:
  - Ashfall regions: `addons/echoashfallprotocol/src/main/resources/data/echoashfallprotocol/echoworldcore/world_regions/`
  - Orbital regions: `addons/echoorbitalremnants/src/main/resources/data/echoorbitalremnants/echoworldcore/world_regions/`
  - Convoy regions: `addons/echoconvoyprotocol/src/main/resources/data/echoconvoyprotocol/echoworldcore/world_regions/`
  - Nexus regions: `addons/echonexusprotocol/src/main/resources/data/echonexusprotocol/echoworldcore/world_regions/`
- Java bootstrap registrations are for fallback/service setup only. Datapack JSON
  definitions with the same id override Java definitions and log the override.

## Hazard Definition

```json
{
  "id": "echoworldcore:hazard/radiation",
  "displayName": "Radiation",
  "summary": "Irradiated terrain and unstable fallout pockets.",
  "defaultSeverity": 70,
  "ticking": false
}
```

Validation:

- `defaultSeverity` must be `0..100`.
- `displayName` and `summary` should be non-empty.
- Duplicate ids replace earlier datapack entries and log a warning.
- Validation reports expose both legacy warning strings and structured issue
  categories for diagnostics.

## Region Definition

```json
{
  "id": "echoashfallprotocol:crash_zone_wasteland",
  "type": "crash_zone",
  "displayName": "Crash Zone Wasteland",
  "summary": "Impact-scattered wreckage fields and Ashfall crash debris.",
  "biomeIds": ["echoashfallprotocol:crash_zone_wasteland"],
  "biomeTags": ["echoashfallprotocol:common_wasteland_biomes"],
  "structureIds": ["echoashfallprotocol:drop_pod"],
  "hazardIds": ["echoworldcore:hazard/salvage_debris"],
  "discoveryId": "echoashfallprotocol:crash_zone_wasteland",
  "radius": 96,
  "renderProfileId": "echoworldcore:region/crash_zone_wasteland",
  "audioProfileId": "echoworldcore:ambience/crash_zone_wasteland",
  "sortOrder": 10
}
```

Supported `type` values:

- `CRASH_ZONE` / `crash_zone`
- `RUINED_CITY` / `ruined_city`
- `TOXIC_SWAMP` / `toxic_swamp`
- `RADIATION_ZONE` / `radiation_zone`
- `CRYOGENIC_RUINS` / `cryogenic_ruins`
- `NEXUS_SCAR` / `nexus_scar`
- `ORBITAL_DEBRIS_FIELD` / `orbital_debris_field`
- `CONVOY_ROUTE` / `convoy_route`
- `SECURE_OUTPOST` / `secure_outpost`
- `ANOMALY_ZONE` / `anomaly_zone`

Validation:

- `radius` must be at least `16`.
- `hazardIds` must reference loaded hazard definitions.
- `discoveryId` should be unique per region unless multiple regions intentionally
  share the same player-facing discovery.
- Render and audio profile ids are optional references; WorldCore never loads
  client RenderCore or AudioCore classes from common code.
- Standalone WorldCore with no chapter region definitions is valid. Region
  definitions appear as owning addons or datapacks contribute them.
- Active region ordering is deterministic: lower `sortOrder` first, then nearest
  center/marker, then identifier.

## Runtime Marker Producers

World markers are not loaded from JSON in 1.3.0. Addons publish them through
ECHO Core services:

- `EchoCoreServices.worldMarkerService().revealMarker(...)`
- `EchoCoreServices.structureDiscoveryService().recordStructureScan(...)`
- `EchoCoreServices.structureDiscoveryService().recordStructureEntry(...)`

Marker ids should be stable, dimension-safe, and owned by the producing addon
namespace. WorldCore persists and queries markers; the chapter or addon owns the
gameplay event that creates them.

## RenderCore Profiles

WorldCore ships client resource profiles under:

- `assets/echoworldcore/rendercore/visual_profiles/region/*.json`
- `assets/echoworldcore/rendercore/visual_profiles/hazard/*.json`

The region profile ids match built-in `renderProfileId` values such as
`echoworldcore:region/crash_zone_wasteland`. Hazard profiles use ids such as
`echoworldcore:hazard/radiation` for consumers that want a visual overlay for
the active hazard snapshot.

## AudioCore Profiles

WorldCore v1 has no hard dependency on AudioCore. It ships forward-compatible
ambience descriptors under:

- `assets/echoworldcore/audiocore/ambience_profiles/ambience/*.json`

Their ids match built-in `audioProfileId` values such as
`echoworldcore:ambience/crash_zone_wasteland`. The referenced sound events are
declared in `assets/echoworldcore/sounds.json` with empty sound lists so future
audio packs can supply actual loops without changing WorldCore code.

# ECHO Native RC 1.0.0 Compatibility Matrix

## Runtime Versions

| Component | Required | Recommended | Tested |
|---|---|---|---|
| Java | 25 | 25 | 25 |
| Minecraft / runtime | 26.1.2 | 26.1.2 | 26.1.2 |
| NeoForge backend | 26.1.2.29-beta | 26.1.2.29-beta | 26.1.2.29-beta |
| ECHO Native Loader | 1.0.0-RC | 1.0.0-RC | 1.0.0-RC |
| ECHO SDK | 1.0.0-RC | 1.0.0-RC | 1.0.0-RC |

## Operating Systems

| OS | Status | Notes |
|---|---|---|
| Windows 10/11 desktop | **Supported** | Primary verified lane. |
| Linux (server) | **Supported with evidence** | Run CI/testkit before claiming support. |
| macOS | **Supported with evidence** | Run CI/testkit before claiming support. |

## Per-Module Migration Status

| Module | Role | NeoForge | Native | Standalone | Blocking Issues | Status |
|---|---|:---:|:---:|:---:|---|---|
| echocore | core | yes | yes | yes | none | ready |
| echonetcore | core | yes | yes | partial | direct_sourceSet_output_reference | ready |
| echo-native-platform | runtime | no | yes | yes | none | ready |
| echoadaptercore | addon | yes | yes | yes | none | ready |
| echoaddonapi | api | yes | yes | yes | missing_neoforge.mods.toml | deferred |
| echoaetherworks | addon | yes | yes | yes | none | ready |
| echoagentcore | addon | yes | yes | yes | none | ready |
| echoagriculturereclamation | addon | yes | yes | yes | none | ready |
| echoarcanacore | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked |
| echoarcaneindex | addon | yes | yes | yes | none | ready |
| echoarmory | addon | yes | yes | yes | none | ready |
| echoashfallprotocol | addon | yes | yes | yes | none | ready |
| echoassetcore | addon | yes | yes | yes | none | ready |
| echoatmospherecore | addon | yes | yes | yes | none | ready |
| echobasegrid | addon | yes | yes | yes | none | ready |
| echobiomecore | addon | yes | yes | yes | none | ready |
| echoblackboxprotocol | addon | yes | yes | yes | none | ready |
| echoblockworks | addon | yes | yes | yes | none | ready |
| echobridgecore | addon | yes | yes | yes | none | ready |
| echocameracore | addon | yes | yes | yes | none | ready |
| echocinematiccore | addon | yes | yes | yes | none | ready |
| echocodexcore | addon | yes | yes | yes | none | ready |
| echocombatcore | addon | yes | yes | yes | none | ready |
| echocommunitybridge | addon | yes | yes | yes | none | ready |
| echocontentcore | addon | yes | yes | yes | none | ready |
| echoconvoyprotocol | addon | yes | yes | yes | none | ready |
| echocreatorcore | addon | yes | yes | yes | none | ready |
| echocreaturecore | addon | yes | yes | yes | none | ready |
| echocursecore | addon | yes | yes | yes | none | ready |
| echodatacore | addon | yes | yes | yes | none | ready |
| echodifficultycore | addon | yes | yes | yes | none | ready |
| echoeconomycore | addon | yes | yes | yes | none | ready |
| echoencountercore | addon | yes | yes | yes | none | ready |
| echoeventcore | addon | yes | yes | yes | none | ready |
| echofamiliarcore | addon | yes | yes | yes | none | ready |
| echogalacticcore | addon | yes | yes | yes | missing_neoforge.mods.toml | blocked |
| echogrimoire | addon | yes | yes | yes | none | ready |
| echoguidecore | addon | yes | yes | yes | none | ready |
| echohealthcore | addon | yes | yes | yes | none | ready |
| echoholomap | addon | yes | yes | yes | none | ready |
| echohudcore | addon | yes | yes | yes | none | ready |
| echoindex | addon | yes | yes | yes | none | ready |
| echoindustrialnexus | addon | yes | yes | yes | none | ready |
| echoinputcore | addon | yes | yes | yes | none | ready |
| echolens | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked |
| echologisticscore | addon | yes | yes | yes | none | ready |
| echologisticsnetwork | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked |
| echolootcore | addon | yes | yes | yes | none | ready |
| echolorecore | addon | yes | yes | yes | none | ready |
| echomachinecore | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked |
| echometadatacore | addon | yes | yes | yes | none | ready |
| echomissioncore | addon | yes | yes | yes | none | ready |
| echomodpackcommandcenter | tooling | yes | yes | yes | missing_neoforge.mods.toml | blocked |
| echomodulegraph | addon | yes | yes | yes | none | ready |
| echomultiblockcore | addon | yes | yes | yes | none | ready |
| echonexusprotocol | addon | yes | yes | yes | none | ready |
| echonotificationcore | addon | yes | yes | yes | none | ready |
| echonpcore | addon | yes | yes | yes | none | ready |
| echoorbitalremnants | addon | yes | yes | yes | none | ready |
| echopackcore | addon | yes | yes | yes | none | ready |
| echoplatformcore | addon | yes | yes | yes | none | ready |
| echoplayercore | addon | yes | yes | yes | none | ready |
| echopowercore | addon | yes | yes | yes | none | ready |
| echopowergrid | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked |
| echopresencelink | addon | yes | yes | yes | none | ready |
| echoprimecore | addon | yes | yes | yes | none | ready |
| echoprogressioncore | addon | yes | yes | yes | none | ready |
| echoquestdirector | addon | yes | yes | yes | none | ready |
| echorecipecore | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked |
| echorecovery | addon | yes | yes | yes | none | ready |
| echorelictech | addon | yes | yes | yes | none | ready |
| echorendercore | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked |
| echoreportcore | addon | yes | yes | yes | none | ready |
| echoriftworlds | addon | yes | yes | yes | none | ready |
| echoritualcore | addon | yes | yes | yes | none | ready |
| echoruntimeguard | addon | yes | yes | partial | local_build_output_classpath_fallback, direct_sourceSet_output_reference | ready |
| echoschemacore | addon | yes | yes | yes | none | ready |
| echoscreencore | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked |
| echoscriptcore | addon | yes | yes | yes | none | ready |
| echosignalos | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked |
| echosocialcore | addon | yes | yes | yes | none | ready |
| echosoundcore | addon | yes | yes | yes | none | ready |
| echospawncore | addon | yes | yes | yes | none | ready |
| echospellcore | addon | yes | yes | yes | none | ready |
| echostationfall | addon | yes | yes | yes | none | ready |
| echostatuscore | addon | yes | yes | yes | none | ready |
| echostructurecore | addon | yes | yes | yes | none | ready |
| echoterminal | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked |
| echotextureforge | addon | yes | yes | yes | none | ready |
| echothemecore | addon | yes | yes | yes | none | ready |
| echotutorialcore | addon | yes | yes | yes | none | ready |
| echovalidationcore | addon | yes | yes | yes | none | ready |
| echovehiclecore | addon | yes | yes | yes | none | ready |
| echoweathercore | addon | yes | yes | yes | none | ready |
| echowiki | addon | yes | yes | yes | none | ready |
| echoworldcore | addon | yes | yes | yes | none | ready |
| signalosexample | addon | yes | yes | yes | none | ready |

## Known Unsupported NeoForge Features

- Direct addon runtime imports from NeoForge classpath.
- Dev classpath fallback (`build/classes` direct loading).
- Metadata-only mutation claims without implementations.

## Risk Notes

- Addons in `blocked` status may still run under `NEOFORGE_BRIDGE` but are not yet validated for `NATIVE` lane.
- Standalone lane is best-effort for core services; complex addons should use `NATIVE` or `NEOFORGE_BRIDGE`.
- Always back up saves before switching runtime lanes.

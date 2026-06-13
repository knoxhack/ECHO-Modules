# ECHO Native RC 1.0.0 Compatibility Matrix

## Runtime Versions

| Component | Required | Recommended | Tested |
|---|---|---|---|
| Java | 25 | 25 | 25 |
| Minecraft / runtime | 26.1.2 | 26.1.2 | 26.1.2 |
| NeoForge backend | 26.1.2.29-beta | 26.1.2.29-beta | 26.1.2.29-beta |
| ECHO Native Loader | 1.0.0-RC1 | 1.0.0-RC1 | 1.0.0-RC1 |
| ECHO SDK | 1.0.0-RC1 | 1.0.0-RC1 | 1.0.0-RC1 |

## Operating Systems

| OS | Status | Notes |
|---|---|---|
| Windows 10/11 desktop | **Supported** | Primary verified lane. |
| Linux (server) | **Supported with evidence** | Run CI/testkit before claiming support. |
| macOS | **Supported with evidence** | Run CI/testkit before claiming support. |

## Per-Module Migration Status

Status values:

- `ready-native`: compiled Native artifacts can load without local build output fallback.
- `ready-bridge`: supported through the NeoForge bridge lane only.
- `blocked-with-reason`: not player-release-ready until the blocking issue is removed.
- `not-supported`: intentionally unavailable on this lane.

| Module | Role | NeoForge | Native | Standalone | Blocking Issues | Status |
|---|---|:---:|:---:|:---:|---|---|
| echocore | core | yes | yes | yes | none | ready-native |
| echonetcore | core | yes | yes | yes | none | ready-native |
| echo-native-platform | runtime | no | yes | yes | none | ready-native |
| echoadaptercore | addon | yes | yes | yes | none | ready-native |
| echoaddonapi | api | yes | yes | yes | none | ready-native |
| echoaetherworks | addon | yes | yes | yes | none | ready-native |
| echoagentcore | addon | yes | yes | yes | none | ready-native |
| echoagriculturereclamation | addon | yes | yes | yes | none | ready-native |
| echoarcanacore | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked-with-reason |
| echoarcaneindex | addon | yes | yes | yes | none | ready-native |
| echoarmory | addon | yes | yes | yes | none | ready-native |
| echoashfallprotocol | addon | yes | yes | yes | none | ready-native |
| echoassetcore | addon | yes | yes | yes | none | ready-native |
| echoatmospherecore | addon | yes | yes | yes | none | ready-native |
| echobasegrid | addon | yes | yes | yes | none | ready-native |
| echobiomecore | addon | yes | yes | yes | none | ready-native |
| echoblackboxprotocol | addon | yes | yes | yes | none | ready-native |
| echoblockworks | addon | yes | yes | yes | none | ready-native |
| echobridgecore | addon | yes | yes | yes | none | ready-native |
| echocameracore | addon | yes | yes | yes | none | ready-native |
| echocinematiccore | addon | yes | yes | yes | none | ready-native |
| echocodexcore | addon | yes | yes | yes | none | ready-native |
| echocombatcore | addon | yes | yes | yes | none | ready-native |
| echocommunitybridge | addon | yes | yes | yes | none | ready-native |
| echocontentcore | addon | yes | yes | yes | none | ready-native |
| echoconvoyprotocol | addon | yes | yes | yes | none | ready-native |
| echocreatorcore | addon | yes | yes | yes | none | ready-native |
| echocreaturecore | addon | yes | yes | yes | none | ready-native |
| echocursecore | addon | yes | yes | yes | none | ready-native |
| echodatacore | addon | yes | yes | yes | none | ready-native |
| echodifficultycore | addon | yes | yes | yes | none | ready-native |
| echoeconomycore | addon | yes | yes | yes | none | ready-native |
| echoencountercore | addon | yes | yes | yes | none | ready-native |
| echoeventcore | addon | yes | yes | yes | none | ready-native |
| echofamiliarcore | addon | yes | yes | yes | none | ready-native |
| echogalacticcore | addon | yes | yes | yes | none | ready-native |
| echogrimoire | addon | yes | yes | yes | none | ready-native |
| echoguidecore | addon | yes | yes | yes | none | ready-native |
| echohealthcore | addon | yes | yes | yes | none | ready-native |
| echoholomap | addon | yes | yes | yes | none | ready-native |
| echohudcore | addon | yes | yes | yes | none | ready-native |
| echoindex | addon | yes | yes | yes | none | ready-native |
| echoindustrialnexus | addon | yes | yes | yes | none | ready-native |
| echoinputcore | addon | yes | yes | yes | none | ready-native |
| echolens | addon | yes | yes | yes | none | ready-native |
| echologisticscore | addon | yes | yes | yes | none | ready-native |
| echologisticsnetwork | addon | yes | yes | yes | none | ready-native |
| echolootcore | addon | yes | yes | yes | none | ready-native |
| echolorecore | addon | yes | yes | yes | none | ready-native |
| echomachinecore | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked-with-reason |
| echometadatacore | addon | yes | yes | yes | none | ready-native |
| echomissioncore | addon | yes | yes | yes | none | ready-native |
| echomodulegraph | addon | yes | yes | yes | none | ready-native |
| echomultiblockcore | addon | yes | yes | yes | none | ready-native |
| echonexusprotocol | addon | yes | yes | yes | none | ready-native |
| echonotificationcore | addon | yes | yes | yes | none | ready-native |
| echonpcore | addon | yes | yes | yes | none | ready-native |
| echoorbitalremnants | addon | yes | yes | yes | none | ready-native |
| echopackcore | addon | yes | yes | yes | none | ready-native |
| echoplatformcore | addon | yes | yes | yes | none | ready-native |
| echoplayercore | addon | yes | yes | yes | none | ready-native |
| echopowercore | addon | yes | yes | yes | none | ready-native |
| echopowergrid | addon | yes | yes | yes | none | ready-native |
| echopresencelink | addon | yes | yes | yes | none | ready-native |
| echoprimecore | addon | yes | yes | yes | none | ready-native |
| echoprogressioncore | addon | yes | yes | yes | none | ready-native |
| echoquestdirector | addon | yes | yes | yes | none | ready-native |
| echorecipecore | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked-with-reason |
| echorecovery | addon | yes | yes | yes | none | ready-native |
| echorelictech | addon | yes | yes | yes | none | ready-native |
| echorendercore | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked-with-reason |
| echoreportcore | addon | yes | yes | yes | none | ready-native |
| echoriftworlds | addon | yes | yes | yes | none | ready-native |
| echoritualcore | addon | yes | yes | yes | none | ready-native |
| echoruntimeguard | addon | yes | yes | yes | none | ready-native |
| echoschemacore | addon | yes | yes | yes | none | ready-native |
| echoscreencore | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked-with-reason |
| echoscriptcore | addon | yes | yes | yes | none | ready-native |
| echosignalos | addon | yes | yes | yes | local_build_output_classpath_fallback | blocked-with-reason |
| echosocialcore | addon | yes | yes | yes | none | ready-native |
| echosoundcore | addon | yes | yes | yes | none | ready-native |
| echospawncore | addon | yes | yes | yes | none | ready-native |
| echospellcore | addon | yes | yes | yes | none | ready-native |
| echostationfall | addon | yes | yes | yes | none | ready-native |
| echostatuscore | addon | yes | yes | yes | none | ready-native |
| echostructurecore | addon | yes | yes | yes | none | ready-native |
| echoterminal | addon | yes | yes | yes | none | ready-native |
| echotextureforge | addon | yes | yes | yes | none | ready-native |
| echothemecore | addon | yes | yes | yes | none | ready-native |
| echotutorialcore | addon | yes | yes | yes | none | ready-native |
| echovalidationcore | addon | yes | yes | yes | none | ready-native |
| echovehiclecore | addon | yes | yes | yes | none | ready-native |
| echoweathercore | addon | yes | yes | yes | none | ready-native |
| echowiki | addon | yes | yes | yes | none | ready-native |
| echoworldcore | addon | yes | yes | yes | none | ready-native |
| signalosexample | addon | yes | yes | yes | none | ready-native |

## Known Unsupported NeoForge Features

- Direct addon runtime imports from NeoForge classpath.
- Dev classpath fallback (`build/classes` direct loading).
- Metadata-only mutation claims without implementations.

## Risk Notes

- Addons in `blocked-with-reason` status may still run under `NEOFORGE_BRIDGE` but are not yet validated for `NATIVE` lane.
- Standalone lane is best-effort for core services; complex addons should use `NATIVE` or `NEOFORGE_BRIDGE`.
- Always back up saves before switching runtime lanes.

## Release Evidence Notes

- On June 13, 2026, `.\gradlew.bat generateGalacticSurveyModuleRelease --console=plain` generated 23 strict compiled runtime records for the Galactic Survey module closure, including Foundation dependencies required by Galactic modules.
- `node scripts\verify-module-release.mjs --release-dir dist\echo-module-release` verified the generated `.echo-addon`, `-neoforge.jar`, `-standalone.jar`, sources, sidecars, package manifests, and checksums.
- Rows still marked `blocked-with-reason` must keep that status until their own compiled release task and verifier evidence exists.

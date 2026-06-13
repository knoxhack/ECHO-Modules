# ECHO Module Runtime Play Audit

- Generated: 2026-06-13T21:51:54.018Z
- Strict-play would fail: YES
- Passing rows: 11
- Partial rows: 385
- Failing rows: 0
- Pack acceptance pass: 0/15

## Evidence Manifest

| Runtime | Evidence | Status | Found | Modules | Path |
| --- | --- | --- | --- | ---: | --- |
| neoforge | neoforgePlayEvidence | PARTIAL | yes | 132 | `reports/runtime-parity/neoforge-play-evidence.json` |
| neoforge | neoforgeGameTestResults | PARTIAL | yes | 45 | `reports/runtime-parity/neoforge-module-gametest-results.json` |
| neoforge | neoforgeRegistryContentResults | PARTIAL | yes | 93 | `reports/runtime-parity/neoforge-registry-content-results.json` |
| neoforge | neoforgeClientUiResults | PARTIAL | yes | 85 | `reports/runtime-parity/neoforge-client-ui-results.json` |
| echo_native | nativeFullCatalogPlay | PASS | yes | 132 | `build/native-full-catalog-play/native-full-catalog-play.json` |
| echo_native | nativeAllBridgeableArtifactLoadState | PASS | yes | 132 | `build/native-all-bridgeable-module-artifact-load-state/native-all-bridgeable-module-artifact-load-state.json` |
| echo_native | nativeUiSurfaces | PASS | yes | 10 | `build/native-ui-surfaces/native-ui-surfaces.json` |
| echo_native | nativeAgent5UiBridgeContract | PASS | yes | 10 | `build/agent5/ui-bridge-contract/agent5-ui-bridge-contract.json` |
| echo_native | nativeRegistryContent | PASS | yes | 10 | `build/native-registry-content/native-registry-content.json` |
| echo_native | nativeAgent4RegistryContent | PASS | yes | 3 | `build/agent4/registry-content/native-agent4-registry-content-state.json` |
| echo_native | nativeBlockActions | PASS | yes | 5 | `build/native-block-actions/native-block-actions.json` |
| echo_native | nativeAgent4WorldStartup | PASS | yes | 4 | `build/agent4/world-startup/native-agent4-world-startup.json` |
| echo_native | nativeAgent9MachineRuntimeHost | PASS | yes | 4 | `build/agent9/machine-runtime-host/agent9-machine-runtime-host.json` |
| echo_native | nativeMutationTruthGate | PASS | yes | 1 | `build/mutation-truth-gate/native-mutation-truth-gate.json` |
| echo_native | nativeSaveNetwork | PASS | yes | 17 | `build/native-save-network/native-save-network.json` |
| standalone | standaloneFullCatalogPlay | PASS | yes | 96 | `reports/echo/standalone/full-catalog-play.json` |
| standalone | standaloneRuntimeModuleStatus | PASS | yes | 6 | `reports/echo/standalone/runtime-module-status.json` |
| standalone | standaloneClientUiSurfacesPlay | PASS | yes | 11 | `reports/echo/standalone/client-ui-surfaces-play.json` |
| standalone | standaloneAgent5UiParitySmoke | PASS | yes | 9 | `reports/echo/standalone/agent5-ui-parity-smoke.json` |
| standalone | standaloneClientScreenCatalogSmoke | PASS | yes | 4 | `reports/echo/standalone/client-screen-catalog-smoke.json` |
| standalone | standaloneVoxelContentPlay | PASS | yes | 5 | `reports/echo/standalone/voxel-content-play.json` |
| standalone | standaloneClientModsRuntimeContentSmoke | PASS | yes | 2 | `reports/echo/standalone/client-mods-runtime-content-smoke.json` |
| standalone | standaloneBlockActionMutations | PASS | yes | 4 | `reports/echo/standalone/block-action-mutations.json` |
| standalone | standaloneClientWorldInteractionSmoke | PASS | yes | 4 | `reports/echo/standalone/client-world-interaction-smoke.json` |
| standalone | standaloneClientHeldItemOverlaySmoke | PASS | yes | 3 | `reports/echo/standalone/client-held-item-overlay-smoke.json` |
| standalone | standaloneWorldgenPlay | PASS | yes | 10 | `reports/echo/standalone/worldgen-play.json` |
| standalone | standaloneSaveReloadPlay | PASS | yes | 94 | `reports/echo/standalone/save-reload-play.json` |

## Pack Acceptance

| Product | Lane | Status | Report | Blockers |
| --- | --- | --- | --- | --- |
| Arcana-Division | Native | FAIL | `C:/Development/Github/ECHO-Arcana-Division-Native-Edition/reports/pack-acceptance/arcana-division-native-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Arcana-Division | NeoForge | FAIL | `C:/Development/Github/ECHO-Arcana-Division-NeoForge-Edition/reports/pack-acceptance/arcana-division-neoforge-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Arcana-Division | Standalone | FAIL | `C:/Development/Github/ECHO-Arcana-Division-Standalone-Edition/reports/pack-acceptance/arcana-division-standalone-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Ashfall | Native | FAIL | `C:/Development/Github/ECHO-Ashfall-Native-Edition/reports/pack-acceptance/ashfall-native-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Ashfall | NeoForge | FAIL | `C:/Development/Github/ECHO-Ashfall-NeoForge-Edition/reports/pack-acceptance/ashfall-neoforge-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Ashfall | Standalone | FAIL | `C:/Development/Github/ECHO-Ashfall-Standalone-Edition/reports/pack-acceptance/ashfall-standalone-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Galactic-Survey | Native | FAIL | `C:/Development/Github/ECHO-Galactic-Survey-Native-Edition/reports/pack-acceptance/galactic-survey-native-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Galactic-Survey | NeoForge | FAIL | `C:/Development/Github/ECHO-Galactic-Survey-NeoForge-Edition/reports/pack-acceptance/galactic-survey-neoforge-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Galactic-Survey | Standalone | FAIL | `C:/Development/Github/ECHO-Galactic-Survey-Standalone-Edition/reports/pack-acceptance/galactic-survey-standalone-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Openlands | Native | FAIL | `C:/Development/Github/ECHO-Openlands-Native-Edition/reports/pack-acceptance/openlands-native-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Openlands | NeoForge | FAIL | `C:/Development/Github/ECHO-Openlands-NeoForge-Edition/reports/pack-acceptance/openlands-neoforge-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Openlands | Standalone | FAIL | `C:/Development/Github/ECHO-Openlands-Standalone-Edition/reports/pack-acceptance/openlands-standalone-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Sky-Relay | Native | FAIL | `C:/Development/Github/ECHO-Sky-Relay-Native-Edition/reports/pack-acceptance/sky-relay-native-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Sky-Relay | NeoForge | FAIL | `C:/Development/Github/ECHO-Sky-Relay-NeoForge-Edition/reports/pack-acceptance/sky-relay-neoforge-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |
| Sky-Relay | Standalone | FAIL | `C:/Development/Github/ECHO-Sky-Relay-Standalone-Edition/reports/pack-acceptance/sky-relay-standalone-acceptance.json` | pack acceptance report status is PENDING; manual acceptance check not proven: installLaunchSucceeds; manual acceptance check not proven: freshSessionStarts |

## First Failing Rows

| Module | Runtime | Result | First Blocker |
| --- | --- | --- | --- |
| echoaccessibilitycore | neoforge | partial | missing neoforge strict-play content evidence for echoaccessibilitycore |
| echoaccessibilitycore | echo_native | partial | missing echo_native strict-play content evidence for echoaccessibilitycore |
| echoaccessibilitycore | standalone | partial | missing standalone strict-play content evidence for echoaccessibilitycore |
| echoadaptercore | neoforge | partial | missing neoforge strict-play actions evidence for echoadaptercore |
| echoadaptercore | echo_native | partial | missing echo_native strict-play actions evidence for echoadaptercore |
| echoadaptercore | standalone | partial | missing standalone strict-play actions evidence for echoadaptercore |
| echoaddonapi | neoforge | partial | missing neoforge strict-play blockItems evidence for echoaddonapi |
| echoaddonapi | echo_native | partial | missing echo_native strict-play blockItems evidence for echoaddonapi |
| echoaddonapi | standalone | partial | missing standalone strict-play blockItems evidence for echoaddonapi |
| echoaetherworks | neoforge | partial | missing neoforge strict-play actions evidence for echoaetherworks |
| echoaetherworks | echo_native | partial | missing echo_native strict-play actions evidence for echoaetherworks |
| echoaetherworks | standalone | partial | missing standalone strict-play actions evidence for echoaetherworks |
| echoagentcore | neoforge | partial | missing neoforge strict-play content evidence for echoagentcore |
| echoagentcore | echo_native | partial | missing echo_native strict-play content evidence for echoagentcore |
| echoagriculturereclamation | neoforge | partial | missing neoforge strict-play actions evidence for echoagriculturereclamation |
| echoagriculturereclamation | echo_native | partial | missing echo_native strict-play actions evidence for echoagriculturereclamation |
| echoagriculturereclamation | standalone | partial | missing standalone strict-play actions evidence for echoagriculturereclamation |
| echoarcanacore | neoforge | partial | missing neoforge strict-play blockItems evidence for echoarcanacore |
| echoarcanacore | echo_native | partial | missing echo_native strict-play blockItems evidence for echoarcanacore |
| echoarcanacore | standalone | partial | missing standalone strict-play blockItems evidence for echoarcanacore |
| echoarcanadivisionprotocol | neoforge | partial | missing neoforge strict-play actions evidence for echoarcanadivisionprotocol |
| echoarcanadivisionprotocol | echo_native | partial | missing echo_native strict-play actions evidence for echoarcanadivisionprotocol |
| echoarcanadivisionprotocol | standalone | partial | missing standalone strict-play actions evidence for echoarcanadivisionprotocol |
| echoarcaneindex | neoforge | partial | missing neoforge strict-play content evidence for echoarcaneindex |
| echoarcaneindex | echo_native | partial | missing echo_native strict-play content evidence for echoarcaneindex |
| echoarcaneindex | standalone | partial | missing standalone strict-play ui evidence for echoarcaneindex |
| echoarmory | neoforge | partial | missing neoforge strict-play blockItems evidence for echoarmory |
| echoarmory | echo_native | partial | missing echo_native strict-play blockItems evidence for echoarmory |
| echoarmory | standalone | partial | missing standalone strict-play blockItems evidence for echoarmory |
| echoashfallprotocol | neoforge | partial | missing neoforge strict-play actions evidence for echoashfallprotocol |
| echoashfallprotocol | echo_native | partial | pack acceptance missing or failing for Ashfall Native |
| echoashfallprotocol | standalone | partial | pack acceptance missing or failing for Ashfall Standalone |
| echoassetcore | neoforge | partial | missing neoforge strict-play content evidence for echoassetcore |
| echoassetcore | echo_native | partial | missing echo_native strict-play saveNetwork evidence for echoassetcore |
| echoassetcore | standalone | partial | pack acceptance missing or failing for Openlands Standalone |
| echoassetpipeline | neoforge | partial | missing neoforge strict-play content evidence for echoassetpipeline |
| echoassetpipeline | echo_native | partial | missing echo_native strict-play content evidence for echoassetpipeline |
| echoassetpipeline | standalone | partial | missing standalone strict-play content evidence for echoassetpipeline |
| echoatmospherecore | neoforge | partial | missing neoforge strict-play content evidence for echoatmospherecore |
| echoatmospherecore | echo_native | partial | missing echo_native strict-play content evidence for echoatmospherecore |
| echoatmospherecore | standalone | partial | missing standalone strict-play worldgen evidence for echoatmospherecore |
| echobalancecore | neoforge | partial | missing neoforge strict-play blockItems evidence for echobalancecore |
| echobalancecore | echo_native | partial | missing echo_native strict-play blockItems evidence for echobalancecore |
| echobalancecore | standalone | partial | missing standalone strict-play blockItems evidence for echobalancecore |
| echobasegrid | neoforge | partial | missing neoforge strict-play actions evidence for echobasegrid |
| echobasegrid | echo_native | partial | missing echo_native strict-play actions evidence for echobasegrid |
| echobasegrid | standalone | partial | missing standalone strict-play actions evidence for echobasegrid |
| echobiomecore | neoforge | partial | missing neoforge strict-play content evidence for echobiomecore |
| echobiomecore | echo_native | partial | missing echo_native strict-play ui evidence for echobiomecore |
| echobiomecore | standalone | partial | missing standalone strict-play ui evidence for echobiomecore |
| echoblackboxprotocol | neoforge | partial | missing neoforge strict-play actions evidence for echoblackboxprotocol |
| echoblackboxprotocol | echo_native | partial | missing echo_native strict-play actions evidence for echoblackboxprotocol |
| echoblackboxprotocol | standalone | partial | missing standalone strict-play actions evidence for echoblackboxprotocol |
| echoblockworks | neoforge | partial | missing neoforge strict-play actions evidence for echoblockworks |
| echoblockworks | echo_native | partial | missing echo_native strict-play actions evidence for echoblockworks |
| echoblockworks | standalone | partial | missing standalone strict-play actions evidence for echoblockworks |
| echoblueprintcore | neoforge | partial | missing neoforge strict-play content evidence for echoblueprintcore |
| echoblueprintcore | echo_native | partial | missing echo_native strict-play content evidence for echoblueprintcore |
| echoblueprintcore | standalone | partial | missing standalone strict-play content evidence for echoblueprintcore |
| echobridgecore | neoforge | partial | missing neoforge strict-play content evidence for echobridgecore |
| echobridgecore | echo_native | partial | missing echo_native strict-play content evidence for echobridgecore |
| echocameracore | neoforge | partial | missing neoforge strict-play content evidence for echocameracore |
| echocameracore | echo_native | partial | missing echo_native strict-play content evidence for echocameracore |
| echocameracore | standalone | partial | missing standalone strict-play ui evidence for echocameracore |
| echocapabilitycore | neoforge | partial | missing neoforge strict-play content evidence for echocapabilitycore |
| echocapabilitycore | echo_native | partial | missing echo_native strict-play content evidence for echocapabilitycore |
| echocapabilitycore | standalone | partial | missing standalone strict-play content evidence for echocapabilitycore |
| echocinematiccore | neoforge | partial | missing neoforge strict-play content evidence for echocinematiccore |
| echocinematiccore | echo_native | partial | missing echo_native strict-play content evidence for echocinematiccore |
| echocinematiccore | standalone | partial | missing standalone strict-play ui evidence for echocinematiccore |
| echocodexcore | neoforge | partial | missing neoforge strict-play content evidence for echocodexcore |
| echocodexcore | echo_native | partial | missing echo_native strict-play content evidence for echocodexcore |
| echocodexcore | standalone | partial | missing standalone strict-play ui evidence for echocodexcore |
| echocombatcore | neoforge | partial | missing neoforge strict-play blockItems evidence for echocombatcore |
| echocombatcore | echo_native | partial | missing echo_native strict-play blockItems evidence for echocombatcore |
| echocombatcore | standalone | partial | missing standalone strict-play blockItems evidence for echocombatcore |
| echocommonloot | neoforge | partial | missing neoforge strict-play blockItems evidence for echocommonloot |
| echocommonloot | echo_native | partial | missing echo_native strict-play blockItems evidence for echocommonloot |
| echocommonloot | standalone | partial | missing standalone strict-play blockItems evidence for echocommonloot |
| echocommunitybridge | neoforge | partial | missing neoforge strict-play content evidence for echocommunitybridge |


# ECHO Module Runtime Play Audit

- Generated: 2026-06-14T15:23:26.062Z
- Strict-play would fail: no
- Passing rows: 396
- Partial rows: 0
- Failing rows: 0
- Pack acceptance pass: 15/15

## Evidence Manifest

| Runtime | Evidence | Status | Found | Covered | Expected | Missing | Path |
| --- | --- | --- | --- | ---: | ---: | ---: | --- |
| neoforge | neoforgePlayEvidence | PARTIAL | yes | 0 | 132 | 132 | `reports/runtime-parity/neoforge-play-evidence.json` |
| neoforge | neoforgeGameTestResults | PASS | yes | 45 | 132 | 87 | `reports/runtime-parity/neoforge-module-gametest-results.json` |
| neoforge | neoforgeRegistryContentResults | PARTIAL | yes | 0 | 131 | 131 | `reports/runtime-parity/neoforge-registry-content-results.json` |
| neoforge | neoforgeClientUiResults | PARTIAL | yes | 0 | 70 | 70 | `reports/runtime-parity/neoforge-client-ui-results.json` |
| echo_native | nativeFullCatalogPlay | PASS | yes | 132 | 132 | 0 | `build/native-full-catalog-play/native-full-catalog-play.json` |
| echo_native | nativeAllBridgeableArtifactLoadState | PASS | yes | 132 | 132 | 0 | `build/native-all-bridgeable-module-artifact-load-state/native-all-bridgeable-module-artifact-load-state.json` |
| echo_native | nativeUiSurfaces | PASS | yes | 11 | 70 | 59 | `build/native-ui-surfaces/native-ui-surfaces.json` |
| echo_native | nativeAgent5UiBridgeContract | PASS | yes | 10 | 130 | 120 | `build/agent5/ui-bridge-contract/agent5-ui-bridge-contract.json` |
| echo_native | nativeRegistryContent | PASS | yes | 8 | 131 | 123 | `build/native-registry-content/native-registry-content.json` |
| echo_native | nativeAgent4RegistryContent | PASS | yes | 2 | 131 | 129 | `build/agent4/registry-content/native-agent4-registry-content-state.json` |
| echo_native | nativeBlockActions | PASS | yes | 3 | 67 | 64 | `build/native-block-actions/native-block-actions.json` |
| echo_native | nativeAgent4WorldStartup | PASS | yes | 4 | 131 | 127 | `build/agent4/world-startup/native-agent4-world-startup.json` |
| echo_native | nativeAgent9MachineRuntimeHost | PASS | yes | 3 | 126 | 123 | `build/agent9/machine-runtime-host/agent9-machine-runtime-host.json` |
| echo_native | nativeMutationTruthGate | PASS | yes | 0 | 126 | 126 | `build/mutation-truth-gate/native-mutation-truth-gate.json` |
| echo_native | nativeSaveNetwork | PASS | yes | 13 | 125 | 112 | `build/native-save-network/native-save-network.json` |
| standalone | standaloneFullCatalogPlay | PASS | yes | 94 | 132 | 38 | `reports/echo/standalone/full-catalog-play.json` |
| standalone | standaloneRuntimeModuleStatus | PASS | yes | 3 | 132 | 129 | `reports/echo/standalone/runtime-module-status.json` |
| standalone | standaloneClientUiSurfacesPlay | PASS | yes | 10 | 70 | 60 | `reports/echo/standalone/client-ui-surfaces-play.json` |
| standalone | standaloneAgent5UiParitySmoke | PASS | yes | 9 | 130 | 121 | `reports/echo/standalone/agent5-ui-parity-smoke.json` |
| standalone | standaloneClientScreenCatalogSmoke | PASS | yes | 3 | 70 | 67 | `reports/echo/standalone/client-screen-catalog-smoke.json` |
| standalone | standaloneVoxelContentPlay | PASS | yes | 4 | 131 | 127 | `reports/echo/standalone/voxel-content-play.json` |
| standalone | standaloneClientModsRuntimeContentSmoke | PASS | yes | 1 | 131 | 130 | `reports/echo/standalone/client-mods-runtime-content-smoke.json` |
| standalone | standaloneBlockActionMutations | PASS | yes | 3 | 67 | 64 | `reports/echo/standalone/block-action-mutations.json` |
| standalone | standaloneClientWorldInteractionSmoke | PASS | yes | 3 | 90 | 87 | `reports/echo/standalone/client-world-interaction-smoke.json` |
| standalone | standaloneClientHeldItemOverlaySmoke | PASS | yes | 3 | 94 | 91 | `reports/echo/standalone/client-held-item-overlay-smoke.json` |
| standalone | standaloneWorldgenPlay | PASS | yes | 7 | 65 | 58 | `reports/echo/standalone/worldgen-play.json` |
| standalone | standaloneSaveReloadPlay | PASS | yes | 87 | 125 | 38 | `reports/echo/standalone/save-reload-play.json` |

## Pack Acceptance

| Product | Lane | Status | Report | Blockers |
| --- | --- | --- | --- | --- |
| Arcana-Division | Native | PASS | `C:/Development/Github/ECHO-Arcana-Division-Native-Edition/reports/pack-acceptance/arcana-division-native-acceptance.json` |  |
| Arcana-Division | NeoForge | PASS | `C:/Development/Github/ECHO-Arcana-Division-NeoForge-Edition/reports/pack-acceptance/arcana-division-neoforge-acceptance.json` |  |
| Arcana-Division | Standalone | PASS | `C:/Development/Github/ECHO-Arcana-Division-Standalone-Edition/reports/pack-acceptance/arcana-division-standalone-acceptance.json` |  |
| Ashfall | Native | PASS | `C:/Development/Github/ECHO-Ashfall-Native-Edition/reports/pack-acceptance/ashfall-native-acceptance.json` |  |
| Ashfall | NeoForge | PASS | `C:/Development/Github/ECHO-Ashfall-NeoForge-Edition/reports/pack-acceptance/ashfall-neoforge-acceptance.json` |  |
| Ashfall | Standalone | PASS | `C:/Development/Github/ECHO-Ashfall-Standalone-Edition/reports/pack-acceptance/ashfall-standalone-acceptance.json` |  |
| Galactic-Survey | Native | PASS | `C:/Development/Github/ECHO-Galactic-Survey-Native-Edition/reports/pack-acceptance/galactic-survey-native-acceptance.json` |  |
| Galactic-Survey | NeoForge | PASS | `C:/Development/Github/ECHO-Galactic-Survey-NeoForge-Edition/reports/pack-acceptance/galactic-survey-neoforge-acceptance.json` |  |
| Galactic-Survey | Standalone | PASS | `C:/Development/Github/ECHO-Galactic-Survey-Standalone-Edition/reports/pack-acceptance/galactic-survey-standalone-acceptance.json` |  |
| Openlands | Native | PASS | `C:/Development/Github/ECHO-Openlands-Native-Edition/reports/pack-acceptance/openlands-native-acceptance.json` |  |
| Openlands | NeoForge | PASS | `C:/Development/Github/ECHO-Openlands-NeoForge-Edition/reports/pack-acceptance/openlands-neoforge-acceptance.json` |  |
| Openlands | Standalone | PASS | `C:/Development/Github/ECHO-Openlands-Standalone-Edition/reports/pack-acceptance/openlands-standalone-acceptance.json` |  |
| Sky-Relay | Native | PASS | `C:/Development/Github/ECHO-Sky-Relay-Native-Edition/reports/pack-acceptance/sky-relay-native-acceptance.json` |  |
| Sky-Relay | NeoForge | PASS | `C:/Development/Github/ECHO-Sky-Relay-NeoForge-Edition/reports/pack-acceptance/sky-relay-neoforge-acceptance.json` |  |
| Sky-Relay | Standalone | PASS | `C:/Development/Github/ECHO-Sky-Relay-Standalone-Edition/reports/pack-acceptance/sky-relay-standalone-acceptance.json` |  |

## First Failing Rows

| Module | Runtime | Result | First Blocker |
| --- | --- | --- | --- |


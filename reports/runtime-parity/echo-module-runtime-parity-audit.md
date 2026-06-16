# ECHO Module Runtime Parity Audit

Generated: 2026-06-16T10:24:08.276Z

## Summary

- Modules audited: 133
- Runtime rows: 399
- Passing rows: 399
- Partial rows: 0
- Failing rows: 0
- Preferred pack manifests: 15
- Backlog items: 2
- Strict-full would fail: no

## Strict-Full Summary

- Strict-full passing rows: 399
- Strict-full partial rows: 0
- Strict-full failing rows: 0

| Runtime | Pass | Partial | Fail |
| --- | ---: | ---: | ---: |
| NeoForge | 133 | 0 | 0 |
| ECHO Native Loader | 133 | 0 | 0 |
| ECHO Standalone Runtime | 133 | 0 | 0 |

## Feature Bucket Coverage

| Bucket | Satisfied | Total |
| --- | ---: | ---: |
| content_action | 570 | 570 |
| content_data | 459 | 459 |
| other | 45 | 45 |
| state_sync | 582 | 582 |
| ui_application | 84 | 84 |
| ui_surface | 462 | 462 |

## Seed Findings

- 133 of 133 descriptor(s) declare AdapterCore support for neoforge, echo_native, and echo_runtime_standalone.
- 133 of 133 declared Native entrypoint source class(es) were found.
- No descriptor main entrypoint source mismatch was found.
- Native Loader has UI/resource/network host bridge code, but visible client routes must be accepted by live host evidence before they pass this audit.
- Standalone has surface renderers, but Native activation surface projection is treated as headless until standalone UI/runtime controller evidence proves player-visible behavior.
- Preferred pack module sets are aligned across lanes for every product.

## Runtime Result Counts

| Runtime | Pass | Partial | Fail |
| --- | ---: | ---: | ---: |
| NeoForge | 133 | 0 | 0 |
| ECHO Native Loader | 133 | 0 | 0 |
| ECHO Standalone Runtime | 133 | 0 | 0 |

## Pack Baseline Gaps

| Repo | Family | Modules | Missing visible surfaces | Missing content baseline |
| --- | --- | ---: | --- | --- |
| ECHO-Arcana-Division-Native-Edition | echo-addon | 39 |  |  |
| ECHO-Arcana-Division-NeoForge-Edition | neoforge | 39 |  |  |
| ECHO-Arcana-Division-Standalone-Edition | standalone | 39 |  |  |
| ECHO-Ashfall-Native-Edition | echo-addon | 46 |  |  |
| ECHO-Ashfall-NeoForge-Edition | neoforge | 46 |  |  |
| ECHO-Ashfall-Standalone-Edition | standalone | 46 |  |  |
| ECHO-Galactic-Survey-Native-Edition | echo-addon | 41 |  |  |
| ECHO-Galactic-Survey-NeoForge-Edition | neoforge | 41 |  |  |
| ECHO-Galactic-Survey-Standalone-Edition | standalone | 41 |  |  |
| ECHO-Openlands-Native-Edition | echo-addon | 42 |  |  |
| ECHO-Openlands-NeoForge-Edition | neoforge | 42 |  |  |
| ECHO-Openlands-Standalone-Edition | standalone | 42 |  |  |
| ECHO-Sky-Relay-Native-Edition | echo-addon | 43 |  |  |
| ECHO-Sky-Relay-NeoForge-Edition | neoforge | 43 |  |  |
| ECHO-Sky-Relay-Standalone-Edition | standalone | 43 |  |  |

## Docs Index Drift

- Missing module ids: 1
- Missing directories: 1
- Extra index entries: 0

## Top Backlog Items

| Priority | Owner | Title | Modules |
| --- | --- | --- | ---: |
| P1 | ECHO-Modules | Regenerate module docs index from the full descriptor inventory | 1 |
| P2 | ECHO-Modules | Promote runtime parity audit into release workflow documentation | 0 |

## Creative Tab Gaps

| Module | Runtime | Status | Expected Tabs | Missing Parent Entries | Missing Search Entries |
| --- | --- | --- | --- | --- | --- |

## Module Runtime Matrix

| Module | Runtime | Result | Artifact | Entrypoint | UI | Actions | Block/Item | Creative Tab | Worldgen | Save/Network | Blockers |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| echoaccessibilitycore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echoaccessibilitycore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echoaccessibilitycore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echoadaptercore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoadaptercore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoadaptercore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoaddonapi | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoaddonapi | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoaddonapi | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoaetherworks | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echoaetherworks | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echoaetherworks | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echoagentcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoagentcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoagentcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoagriculturereclamation | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoagriculturereclamation | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoagriculturereclamation | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoarcanacore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoarcanacore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoarcanacore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoarcanadivisionprotocol | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoarcanadivisionprotocol | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoarcanadivisionprotocol | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoarcaneindex | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echoarcaneindex | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echoarcaneindex | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echoarmory | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echoarmory | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echoarmory | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echoashfallprotocol | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoashfallprotocol | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoashfallprotocol | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoassetcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoassetcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoassetcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoassetpipeline | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoassetpipeline | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoassetpipeline | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoatmospherecore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoatmospherecore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoatmospherecore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echobalancecore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echobalancecore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echobalancecore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echobasegrid | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echobasegrid | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echobasegrid | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echobiomecore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echobiomecore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echobiomecore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoblackboxprotocol | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoblackboxprotocol | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoblackboxprotocol | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoblockworks | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoblockworks | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoblockworks | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoblueprintcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoblueprintcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoblueprintcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echobridgecore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echobridgecore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echobridgecore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echocameracore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocameracore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocameracore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocapabilitycore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echocapabilitycore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echocapabilitycore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echocinematiccore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echocinematiccore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echocinematiccore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echocodexcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocodexcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocodexcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocombatcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echocombatcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echocombatcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echocommonloot | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echocommonloot | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echocommonloot | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echocommunitybridge | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocommunitybridge | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocommunitybridge | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocontentcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echocontentcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echocontentcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoconvoyprotocol | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoconvoyprotocol | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoconvoyprotocol | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echocore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echocore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echocore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echocreatorcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocreatorcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocreatorcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echocreaturecore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echocreaturecore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echocreaturecore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echocreatureroles | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echocreatureroles | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echocreatureroles | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echocurationcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echocurationcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echocurationcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echocursecore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echocursecore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echocursecore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echodatacore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echodatacore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echodatacore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echodeepreachprotocol | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echodeepreachprotocol | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echodeepreachprotocol | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echodependencydoctor | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | none expected |  |
| echodependencydoctor | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | none expected |  |
| echodependencydoctor | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | none expected |  |
| echodifficultycore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echodifficultycore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echodifficultycore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echodisastercore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echodisastercore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echodisastercore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoeconomycore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoeconomycore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoeconomycore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoencountercore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoencountercore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoencountercore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoequipmentcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoequipmentcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoequipmentcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoeventcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoeventcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoeventcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoexpeditioncore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoexpeditioncore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoexpeditioncore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echofactioncore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echofactioncore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echofactioncore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echofamiliarcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echofamiliarcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echofamiliarcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echofoundationcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echofoundationcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echofoundationcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echogalacticcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echogalacticcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echogalacticcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echogalacticsurveyprotocol | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echogalacticsurveyprotocol | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echogalacticsurveyprotocol | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echogrimoire | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echogrimoire | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echogrimoire | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echoguidecore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echoguidecore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echoguidecore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echohazardcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echohazardcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echohazardcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echohealthcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echohealthcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echohealthcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoholomap | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoholomap | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoholomap | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echohudcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echohudcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echohudcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoindex | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoindex | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoindex | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoindustrialnexus | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoindustrialnexus | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoindustrialnexus | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoinputcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echoinputcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echoinputcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | none expected |  |
| echolens | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echolens | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echolens | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echolocalizationcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echolocalizationcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echolocalizationcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echologisticscore | neoforge | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echologisticscore | echo_native | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echologisticscore | standalone | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echologisticsnetwork | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echologisticsnetwork | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echologisticsnetwork | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echolootcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echolootcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echolootcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echolorecore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echolorecore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echolorecore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echomachinecore | neoforge | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echomachinecore | echo_native | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echomachinecore | standalone | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echomaterialcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echomaterialcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echomaterialcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echometadatacore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echometadatacore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echometadatacore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echomigrationcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echomigrationcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echomigrationcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echomissioncore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echomissioncore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echomissioncore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echomodulegraph | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echomodulegraph | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echomodulegraph | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echomultiblockcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echomultiblockcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echomultiblockcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echonetcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echonetcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echonetcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echonexusprotocol | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echonexusprotocol | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echonexusprotocol | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echonotificationcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echonotificationcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echonotificationcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echonpcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echonpcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echonpcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoopenlandsprotocol | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoopenlandsprotocol | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoopenlandsprotocol | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoorbitalremnants | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoorbitalremnants | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoorbitalremnants | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echopackcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echopackcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echopackcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echopackdiff | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echopackdiff | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echopackdiff | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoplatformcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoplatformcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoplatformcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoplayercore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoplayercore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoplayercore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoplaytestcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoplaytestcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoplaytestcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echopolicycore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echopolicycore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echopolicycore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echopowercore | neoforge | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echopowercore | echo_native | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echopowercore | standalone | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echopowergrid | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echopowergrid | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echopowergrid | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echopresencelink | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echopresencelink | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echopresencelink | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echoprimecore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoprimecore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoprimecore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoprogressioncore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoprogressioncore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoprogressioncore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoquestdirector | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoquestdirector | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoquestdirector | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echorecipecore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echorecipecore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echorecipecore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echorecovery | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echorecovery | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echorecovery | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echorelictech | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echorelictech | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echorelictech | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echorendercore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echorendercore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echorendercore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoreportcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoreportcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoreportcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoriftworlds | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoriftworlds | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoriftworlds | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoritualcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoritualcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoritualcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoruincore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoruincore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoruincore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoruntimeguard | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoruntimeguard | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoruntimeguard | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoschemacore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoschemacore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoschemacore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoscreencore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoscreencore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoscreencore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoscriptcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoscriptcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoscriptcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoseasoncore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoseasoncore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoseasoncore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echoserveropscore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoserveropscore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoserveropscore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echosessioncore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echosessioncore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echosessioncore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echosettlementcore | neoforge | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echosettlementcore | echo_native | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echosettlementcore | standalone | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echosignalos | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echosignalos | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echosignalos | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echoskillcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoskillcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoskillcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoskyrelayprotocol | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoskyrelayprotocol | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoskyrelayprotocol | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echosocialcore | neoforge | pass | verified | lifecycle-runs | none expected | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echosocialcore | echo_native | pass | verified | lifecycle-runs | none expected | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echosocialcore | standalone | pass | verified | lifecycle-runs | none expected | mutates gameplay | none expected | none expected | none expected | save/reload or sync verified |  |
| echosoundcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | playable | generated in runtime | save/reload or sync verified |  |
| echosoundcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | playable | generated in runtime | save/reload or sync verified |  |
| echosoundcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | playable | generated in runtime | save/reload or sync verified |  |
| echospawncore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echospawncore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echospawncore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echospellcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echospellcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echospellcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echostationcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echostationcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echostationcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echostationfall | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echostationfall | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echostationfall | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echostatuscore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echostatuscore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echostatuscore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echostructurecore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echostructurecore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echostructurecore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echosupplycore | neoforge | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echosupplycore | echo_native | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echosupplycore | standalone | pass | verified | lifecycle-runs | none expected | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echotelemetrycore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echotelemetrycore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echotelemetrycore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echoterminal | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echoterminal | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echoterminal | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | none expected | save/reload or sync verified |  |
| echoterritorycore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoterritorycore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoterritorycore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echotextureforge | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | playable | none expected | save/reload or sync verified |  |
| echotextureforge | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | playable | none expected | save/reload or sync verified |  |
| echotextureforge | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | playable | none expected | save/reload or sync verified |  |
| echothemecore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echothemecore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echothemecore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| echotoolcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echotoolcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echotoolcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echotutorialcore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echotutorialcore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echotutorialcore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echovalidationcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echovalidationcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echovalidationcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | none expected | save/reload or sync verified |  |
| echovehiclecore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echovehiclecore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echovehiclecore | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoweathercore | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoweathercore | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoweathercore | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echowiki | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echowiki | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echowiki | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | playable | generated in runtime | save/reload or sync verified |  |
| echoworldcore | neoforge | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoworldcore | echo_native | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoworldcore | standalone | pass | verified | lifecycle-runs | none expected | none expected | none expected | none expected | generated in runtime | save/reload or sync verified |  |
| echoworldstarter | neoforge | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoworldstarter | echo_native | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| echoworldstarter | standalone | pass | verified | lifecycle-runs | none expected | none expected | place/use/break verified | none expected | generated in runtime | save/reload or sync verified |  |
| signalosexample | neoforge | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| signalosexample | echo_native | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |
| signalosexample | standalone | pass | verified | lifecycle-runs | visible/actionable | mutates gameplay | place/use/break verified | none expected | none expected | save/reload or sync verified |  |

## Strict-Full Feature Gaps by Module

| Module | Runtime | Result | Missing Features |
| --- | --- | --- | --- |


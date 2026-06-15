# ECHO Runtime Parity Fix Backlog

Generated: 2026-06-14T23:44:27.983Z

## P0

### RPA-001 - ECHO Native Loader surfaces need visible/actionable proof

- Owner: ECHO-Native-Platform
- Subsystem: visible UI runtime bridge
- Summary: 71 UI/HUD/screen module row(s) only prove declarations or headless registration.
- Runtimes: echo_native
- Modules (71): echoaccessibilitycore, echoadaptercore, echoaddonapi, echoaetherworks, echoagriculturereclamation, echoarcanadivisionprotocol, echoarcaneindex, echoarmory, echoashfallprotocol, echobasegrid, echobiomecore, echoblackboxprotocol, echoblockworks, echocameracore, echocinematiccore, echocodexcore, echocommunitybridge, echoconvoyprotocol, echocore, echocreatorcore, echocreaturecore, echocursecore, echodeepreachprotocol, echofamiliarcore, echogalacticcore, echogalacticsurveyprotocol, echogrimoire, echoguidecore, echoholomap, echohudcore, echoindex, echoindustrialnexus, echoinputcore, echolens, echolocalizationcore, echologisticsnetwork, echolorecore, echomodulegraph, echomultiblockcore, echonetcore, +31 more
- Recommended fix: Promote module surface declarations into live host routes and add smoke evidence for HUD, screens, overlays, input, and dispatch.

### RPA-002 - ECHO Standalone Runtime surfaces need visible/actionable proof

- Owner: ECHO-Standalone-Runtime
- Subsystem: visible UI runtime bridge
- Summary: 1 UI/HUD/screen module row(s) only prove declarations or headless registration.
- Runtimes: standalone
- Modules (1): echodeepreachprotocol
- Recommended fix: Promote module surface declarations into live host routes and add smoke evidence for HUD, screens, overlays, input, and dispatch.

### RPA-003 - Prove block, item, action, and worldgen behavior through runtime hosts

- Owner: ECHO-Native-Platform / ECHO-Standalone-Runtime
- Subsystem: content and gameplay host proof
- Summary: 81 module(s) have expected content/gameplay buckets without place/use/break/worldgen/action proof.
- Runtimes: echo_native, standalone
- Modules (81): echoadaptercore, echoaddonapi, echoaetherworks, echoagriculturereclamation, echoarcanacore, echoarcanadivisionprotocol, echoarmory, echoashfallprotocol, echoatmospherecore, echobalancecore, echobasegrid, echobiomecore, echoblackboxprotocol, echoblockworks, echocommonloot, echocontentcore, echoconvoyprotocol, echocore, echocreaturecore, echocreatureroles, echocursecore, echodeepreachprotocol, echodisastercore, echoeconomycore, echoencountercore, echoequipmentcore, echoexpeditioncore, echofamiliarcore, echofoundationcore, echogalacticcore, echogalacticsurveyprotocol, echohazardcore, echohudcore, echoindex, echoindustrialnexus, echolens, echologisticscore, echologisticsnetwork, echolootcore, echomachinecore, +41 more
- Recommended fix: Add host-backed smokes that place/use/break module content, generate expected data/world features, and record trusted mutations.

### RPA-004 - Prove all module creative tabs are visible, searchable, selectable, and playable

- Owner: ECHO-Native-Platform / ECHO-Standalone-Runtime
- Subsystem: creative inventory parity
- Summary: 3 module(s) expect creative inventory content without full live creative-tab play proof.
- Runtimes: echo_native, standalone
- Modules (3): echodeepreachprotocol, echoequipmentcore, echosettlementcore
- Recommended fix: Generalize the creative tab bridge beyond Ashfall/fixtures and require per-module parent/search/select/play evidence.

## P1

### RPA-005 - Regenerate module docs index from the full descriptor inventory

- Owner: ECHO-Modules
- Subsystem: docs index
- Summary: Docs index drift detected: 1 missing id(s), 1 missing directorie(s), 0 extra entrie(s).
- Modules (1): echodeepreachprotocol
- Recommended fix: Update the docs index generator/source data so every descriptor appears exactly once.

### RPA-006 - Add save/reload and sync proof for stateful modules

- Owner: ECHO-Native-Platform / ECHO-Standalone-Runtime
- Subsystem: save and network parity
- Summary: 122 module(s) declare stateful/network behavior without save/reload or sync evidence.
- Runtimes: echo_native, standalone
- Modules (122): echoaccessibilitycore, echoadaptercore, echoaetherworks, echoagentcore, echoagriculturereclamation, echoarcanacore, echoarcanadivisionprotocol, echoarmory, echoashfallprotocol, echoassetcore, echoassetpipeline, echoatmospherecore, echobalancecore, echobasegrid, echobiomecore, echoblackboxprotocol, echoblockworks, echoblueprintcore, echobridgecore, echocameracore, echocapabilitycore, echocodexcore, echocombatcore, echocommonloot, echocommunitybridge, echocontentcore, echoconvoyprotocol, echocore, echocreatorcore, echocreaturecore, echocreatureroles, echocurationcore, echocursecore, echodatacore, echodeepreachprotocol, echodifficultycore, echodisastercore, echoeconomycore, echoencountercore, echoequipmentcore, +82 more
- Recommended fix: Extend runtime smokes to create state, save, reload, and verify network/sync receipts for each module domain.

## P2

### RPA-007 - Promote runtime parity audit into release workflow documentation

- Owner: ECHO-Modules
- Subsystem: audit polish
- Summary: The generator is intentionally separate from release mutation until the first backlog is triaged.
- Recommended fix: After the P0/P1 items are understood, decide whether --strict should become a release workflow gate.


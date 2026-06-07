# Changelog

## 0.4.0 - Factory Auto-Restock Ops

- Added factory restock policy metadata to loadout presets, including factory task ids, target/minimum runs, in-flight caps, and cooldowns.
- Upgraded Auto-Restock Stations so Industrial input depots can be refilled from Logistics stock using server-side courier dispatch without an Industrial compile dependency.
- Added factory restock bridge status/results, owner persistence for unattended restock dispatch, and GameTest coverage for parser metadata, dispatch, and duplicate in-flight blocking.

## 0.2.0 - Operations Dashboard

- Added shared dashboard state for Logistics networks, including block and endpoint counts, dock/relay/depot online state, depot cooldowns, selected endpoint/loadout readiness, request payloads, and bounded active delivery rows.
- Upgraded the block-local Logistics screen into an operational dashboard with infrastructure status, low-stock pressure, selected endpoint readiness, active delivery ETA/status, and context-aware dispatch, cancel, relay, offer, and depot controls.
- Aligned the ECHO Terminal Logistics page with the shared dashboard snapshot so Terminal and block views report the same readiness, endpoint, and delivery state.
- Added dashboard GameTest coverage for snapshot derivation, selected endpoint dispatch, active delivery rows, Terminal offer refresh, and block dashboard cancellation.
- Kept Logistics save data, datapack schemas, registry ids, route manifests, supply tags, loadout cards, remote tablets, drone delivery, relays, depots, and optional integrations compatible with 0.1.0 worlds.

## 0.1.0 - Release Candidate

- Added survival discovery advancements for the Logistics progression path from Supply Tag through dispatch-ready infrastructure.
- Added persistence GameTests for Logistics block state and courier payload state.
- Hardened Logistics container removal so stocked crates, docks, lockers, depots, relays, terminals, requesters, and restock stations drop stored items once on block break instead of silently voiding or duplicating contents.
- Added GameTest coverage for breaking a stocked Supply Crate and verifying exact one-time item drops.
- Aligned the bundled salvage-water depot offer with the Ashfall Crashbreak Salvage faction id used by current Core faction data.
- Updated smoke-test documentation with the Java 25 precondition, playable survival loop, break-drop safety check, verification commands, and 25/25 GameTest expectation.
- Kept optional ECHO Terminal, ECHO Core, Industrial Nexus, and sibling integrations guarded while validating the full included workspace build.

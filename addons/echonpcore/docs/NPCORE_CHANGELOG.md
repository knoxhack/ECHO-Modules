# NPCore Changelog

## 1.0.0

Persistence and mission gates:
- Added hybrid NPCore storage with DataCore-backed trade stock, service cooldown, and villager conversion persistence.
- Added DataCore-backed trade restock timers for limited-stock offers.
- Kept in-memory storage as the fallback when DataCore is absent.
- Added MissionCore-backed trade `requiresMission` checks and faction standing gates with compatibility fail-open behavior when optional services are absent.
- Added bounded home-aware wandering, richer smoke diagnostics, and `/echonpcore smoke all`.
- Expanded diagnostics and Terminal addon-info metrics for storage mode, DataCore availability, registered data keys, and MissionCore availability.

## 0.1.1

Integration polish:
- Added real ScreenCore page integration for `echonpcore:npc_interaction`.
- Added ScreenCore `npcore` data provider and NPC action bridge.
- Kept the classic NPC screen as a fallback when ScreenCore is absent, disabled, or unavailable.
- Added Terminal addon-info provider for NPCore diagnostics and smoke-test hints.
- Wired `echonpcore` into the workspace addon set so `buildEchoWorkspace -PechoAddonSet=all` includes it.

## 1.0.0

Initial vertical slice:
- Added `echonpcore` addon module.
- Added custom ECHO NPC entity and spawn egg.
- Added data-driven profiles, visuals, dialogue, trades, services, factions, and replacement mappings.
- Added server-authoritative NPC interaction packets.
- Added classic NPC UI and defensive ScreenCore adapter.
- Added vanilla villager and wandering trader replacement.
- Added Ashfall-style example NPC profiles and placeholder art.

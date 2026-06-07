# ECHO: Orbital Remnants 1.2.0 Known Issues

This file tracks intentional release limits and areas that need playtest feedback.

## Current Release Limits

- Hostile and major encounter entities use tuned vanilla-model renderers instead of bespoke animated models.
- Factions now publish ECHO Core standing, Faction Atlas entries, pledge rewards, NPC outpost contacts, Tier I charters, and support/barter services. Long quest chains, faction bases, and large settlement systems remain deferred.
- Living Route Worlds now cover the expanded Earth-through-Nexus arc with Saturn Ring Graveyard and Titan Methane Shelf. No additional planets or open-ended route worlds are included in this release.
- Route terrain uses deterministic compact feature generation, not a large multichunk structure system.
- Balance numbers for arrival supplies, hazard drain, and deep-site threats are expected to change after player feedback.
- Shared Terminal integration mirrors Orbital state through Core services; the standalone ECHO-7 terminal remains the progression authority when the shared terminal is absent.
- Stationfall handoff is exposed as an ECHO Core route record only. Stationfall boarding UI and station-side content remain owned by the Stationfall addon.

## Playtest Focus

- Can a blind survival player understand the terminal's Next Step and blocked-scan messages?
- Do arrival caches support first visits without skipping too much crafting?
- Do deep-site hazards feel dangerous but recoverable?
- Do Saturn/Titan/Nexus NPC outposts, charters, services, and barter loops feel worth visiting without forcing a pledge path?
- Does Nexus stabilization feel like a clear post-ECHO-0 objective?
- Do What Now diagnostics, Route Records, Vitals, Faction Atlas, and Reward Inbox support caches make the full stack easier to read without replacing the standalone Orbital flow?

## What Feedback We Need

- Balance: oxygen, pressure, radiation, Saturn/Titan route drains, cache support, and machine pacing.
- Soft-locks: any item, route, scan, return vector, or encounter drop that can halt survival progression.
- Route readability: whether deep sites, caches, landmarks, and objective blocks are easy to recognize.
- Charter usefulness: whether outpost charter and service rewards feel worth the trip without replacing crafting.
- Hazard pressure: whether route danger is meaningful, readable, and recoverable.

## Reporting

Use the issue tracker listed in the mod metadata when publishing from the public repository. Include the mod version, NeoForge version, world type, installed companion mods, and a screenshot or terminal report when possible.

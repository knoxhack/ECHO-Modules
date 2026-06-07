# Changelog

## 1.2.0 - Full Integration Public Baseline

- Folded the internal Saturn/Titan, ECHO-0, survey, faction outpost, and Terminal integration milestones into the public `1.2.0` Orbital Remnants metadata line.
- Added an explicit ECHO Core `echoorbitalremnants:orbital_stationfall_handoff` route record so Stationfall can read Station ECHO boarding state without Orbital taking a hard dependency.
- Kept current-canon route scope: Earth, Low Orbit, Station Network, Moon, Mars, Europa, Saturn, Titan, Deep Space Protocol, ECHO-0, surveys, outposts, and final seal.
- Kept the future line clear: no new planets beyond Titan, no large multichunk structure framework, and no bespoke animated encounter dependency in this release.

## Stack 1.2.0 - Orbital Terminal Polish

- Published Orbital mission route placement for the shared Survival Route: Earth/launch/orbit in Phase 06, route worlds and Deep Space Protocol in Phase 07, ECHO-0 in Phase 08, and surveys/outposts/final seal in Phase 09.
- Preserved Orbital as the owner of gameplay state, actions, rewards, packets, saves, recipes, dimensions, and schemas while letting ECHO Terminal and MissionCore project the route consistently.
- Kept RenderCore optional with the existing visual-profile path and tinted renderer fallback.
- Added GameTest coverage for Orbital route placement, shared Survival Route projection, and child mission action preservation.

## Internal Milestones Folded Into Public 1.2.0

The following entries record pre-release Orbital milestone labels that were consolidated into the public `1.2.0` stack line. They are retained for traceability, but they are not separate public jar versions for this release.

## Internal 1.5.3 - Faction Outposts

- Added the persistent `echoorbitalremnants:orbital_faction_npc` contact with synced faction and role identity.
- Added Orbital-native faction dialogue/action packets and a custom ECHO outpost dialogue screen for Talk, Request Service, Barter, Accept Charter, and Complete Charter.
- Added server-side outpost NPC spawning near Saturn Crashbreak, Titan Radwarden, and Nexus Sporebound hub hooks with nearby caps and per-site seeding.
- Replaced the final-seal faction gate with required Tier I outpost charters for Crashbreak, Radwarden, and Sporebound.
- Extended ECHO-7 progress with outpost tiers, active/completed outpost charters, service cooldowns, and migration for old saves with completed legacy faction contracts or already sealed final networks.
- Kept Ashfall optional by using only Orbital/ECHO Core paths while preserving the existing Radwarden, Crashbreak, and Sporebound standing mirror.
- Added GameTest coverage for outpost NPC identity, final-seal gating, legacy migration, and server-side action rejection.

## Internal 1.5.0 - Orbital Cohesion Polish

- Expanded the release route arc through Saturn Ring Graveyard and Titan Methane Shelf before the Nexus finale.
- Added Saturn/Titan terminal state, route guidance, route records, arrival/deep-site cache support, hazards, objectives, encounters, and route-transfer coverage.
- Added faction support/barter kiosks and relay hubs while keeping full NPC vendors, faction bases, and long quest chains deferred.
- Removed the GeckoLib dependency for this beta; encounter visuals remain on tinted vanilla renderers.
- Tuned the longer-route defaults with lower deep-site threat chance, lighter Saturn oxygen drain, lighter Titan pressure drain, and recoverability stacks in Saturn/Titan caches.
- Smoothed route pacing with lighter hazard defaults, less frequent orbital events, faster machine cadence, softer Orbit/Mars/Europa spikes, and clearer cache cadence expectations.
- Added richer optional ECHO Terminal integration with Orbital Command, Survey, and ECHO mission surfaces.
- Added shared Terminal mission records and once-only utility support caches backed by Orbital progress save data.
- Moved Orbital shared-Terminal action and mission registration to common setup so server-side actions work on dedicated servers.
- Expanded Orbital archive handoff notes and tester coverage for Terminal mission cache state and action registration.
- Added ECHO Core route records for Earth Recontact, Launch Chain, Route Worlds, and ECHO-0 Quarantine.
- Added ECHO Core diagnostics, hazard telemetry, and Faction Atlas integration for Radwarden containment, Crashbreak salvage, and Sporebound anomaly standings.
- Hardened the Ashfall handoff so Earth calibration waits for a real Ashfall Nexus choice when Ashfall is installed while standalone Orbital uses the recovered ECHO-7 handoff.
- Added a Core integration contract GameTest as part of the Orbital release coverage.
- Hardened release safety with machine inventory drops, virtual Rocket Assembly Frame output, once-only route arrival seeds, high-altitude scan gating, and ambient threat caps.
- Expanded release coverage to 44 required Orbital GameTests, including a strict playable-path test and a progress persistence round-trip.
- Polished route-vessel and rocket blocked/success feedback with action-bar summaries, route handoff particles, and concrete next-proof guidance.
- Synced the redirected Gradle jar into the addon-local `build/libs` release path and made resource validation part of the release gate.
- Synced docs for the then-current internal milestone line before the public `1.2.0` stack consolidation.

## Internal 1.4.0 - Full Endgame

- Completed the official Earth-to-Nexus arc through ECHO-0, post-ECHO surveys, faction contracts, and final network sealing.
- Upgraded ECHO-0 with phase pressure, oxygen, radiation, quarantine-motive dialogue, one-time rewards, and final protocol reporting.
- Added the Orbital Remnants Complete advancement, final network reward flow, Ashes milestone mirroring, and `/echo7route` QA commands.
- Replaced stale release-candidate wording in player-facing terminal and guide copy with ECHO NOTE, MISSION, and Orbital Remnants completion language.

## Internal 1.3.1 - Mid-Game Hardening

- Hardened mid-game terminal guidance with visible repair counts, consistent ECHO NOTE wording, duplicate-site clarity, and old-save bypass status in the SURVEY tab.
- Made deep-site cache markers easier to notice without adding new block ids.
- Synced release docs, tester checklist, RC notes, and expected jar references for the hardened mid-game release.
- Added focused GameTest coverage for mid-game terminal repair-count guidance.

## Internal 1.3.0 - Mid-Game Route Expansion

- Added four route objective chains between launch and ECHO-0: Station Relay Nodes, Helium Extractor Nodes, Mars Pressure Consoles, and Europa Thermal Arrays.
- Added mid-game route resources, caches, machine recipes, terminal guidance, progress keys, manual advancements, and compatibility-safe gates.
- Added route objective blocks to arrival sites and repeatable deep-site families.
- Added the Europa Cryo Warden encounter with terminal defeat messaging and Europa prep rewards.
- Expanded GameTests for mid-game objective chains, route gates, recipes, sites, and the new encounter.

## Internal 1.2.1-beta.5 - Final Upload Candidate

- Synced terminal ECHO NOTE wording across code, guide, and release notes.
- Updated final beta upload references to the beta.5 jar.
- Locked deferred items and tester feedback asks as intentional beta limits.

## Internal 1.2.1-beta.4 - Beta Polish Candidate

- Tightened ECHO Terminal ECHO NOTE and final end-state wording for small-screen readability.
- Gave deep-site caches clearer route markers and beta support scaling without adding new ids.
- Updated beta release docs, test plan, and RC notes for the polished candidate jar.

## Internal 1.2.1-beta.3 - Beta Release Candidate Completion

- Added ECHO Terminal ECHO NOTE guidance for blocked scans, post-ECHO-0 surveys, faction contracts, and final arc completion.
- Hardened faction contract blocked reports for wrong dimension, missing proof, Sporebound pre-ECHO lock, cooldown, and no-pledge states.
- Expanded GameTests with independent Radwarden, Crashbreak, and Sporebound contract coverage.
- Updated tester docs, test plan, known issues, and release notes for the release-candidate pass.

## Internal 1.2.1-beta.2 - Tester Missing-Work Completion

- Added terminal-driven faction contracts for Radwarden orbital containment, Crashbreak orbital salvage, and Sporebound anomaly interpretation.
- Added persistent faction contract state, cooldowns, one-time completion rewards, and the First Faction Contract advancement.
- Added ECHO Terminal contract status so pledged players can see the active proof and reward loop.
- Updated tester metadata, release URLs, docs, known issues, and manual smoke-test checklist.
- Expanded GameTests for faction contract completion, persistence, cooldown, rewards, and terminal snapshot text.

## Internal 1.2.1-beta.1 - Tester Gameplay Hardening

- Added clearer ECHO Terminal Next Step guidance and blocked-scan reports for blind survival progression.
- Added starter recovery caches and stronger first-arrival support supplies to reduce early soft-lock risk.
- Added beta balance config for arrival support, route hazard drain, and deep-site threat chance.
- Tuned route hazards and ambient deep-site threats to stay dangerous but more recoverable by default.
- Clarified ECHO-0 completion messaging and post-ECHO Nexus stabilization guidance.
- Added beta GameTests for survival acquisition paths, terminal stuck-state guidance, and cache support.

## 1.2.0 - Living Route Worlds

- Expanded `route_terrain` with deterministic route feature zones, surface detail passes, route carvers, and richer deep-site placement.
- Added three repeatable deep-site families per route with fixed caches, objective blocks, traversal hooks, and hazard clues.
- Added terrain-aware terminal hazard text and ambient threat spawning near dense feature zones.
- Added worldgen/event config options for route feature density, deep-site caches, and feature threat spawns.
- Added deep-site discovery advancements for Orbit, Moon, Mars, Europa, and Nexus.
- Expanded GameTests for route site families, cache identities, terminal feature guidance, and feature threat zones.

## 1.1.0 - Current Dimensions Expansion

- Replaced flat route dimensions with deterministic route terrain for Orbit, Moon, Mars, Europa, and Nexus.
- Added route survey objectives, a dedicated terminal SURVEY tab, local hazard reports, and post-ECHO Nexus stabilization.
- Added Signal Analyzer, survey/relay/vent/anchor utility blocks, route terrain blocks, and new survey resources.
- Made the Signal Analyzer craftable through survival progression.
- Made survey scans unique per landmark site and item scans consume one matching survey item outside creative mode.
- Added new `orbital_processing` recipes for survey data, probes, pressure valves, stabilizer shards, and the Stabilized ECHO Core.
- Expanded arrival caches and repeatable landmarks with fixed caches, route objective hooks, and site-aware hazards.
- Added survey/stabilization advancements and GameTests for terrain, recipes, terminal survey data, and survey progression.

## 1.0.0 - Release v1

- Added the full Earth-to-Nexus progression arc.
- Added ECHO Terminal mission-control screen and server-backed scan actions.
- Added launch readiness, rocket assembly, orbital staging, route vessels, and return vectors.
- Added menu-driven machines with data-driven orbital processing recipes.
- Added suit oxygen, pressure, radiation, gravity, recovery tools, and orbital HUD.
- Added Low Earth Orbit, Lunar Scar Zone, Mars Ash Basin, Europa Cryo Ocean, and Nexus Anomaly Belt routes.
- Added generated arrival structures, route caches, hostile entities, encounter bars, and ECHO-0 final protocol.
- Added faction pledge paths, reward bundles, custom weapons, recipes, loot, advancements, docs, CI, and GameTests.

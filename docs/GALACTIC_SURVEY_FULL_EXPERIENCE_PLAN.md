# ECHO: Galactic Survey Full Experience Plan

Galactic Survey is a late high-ceiling ECHO pack about rebuilding a long-range
survey network at the edge of known infrastructure.

The core promise is simple: the player is not conquering space. The player is a
field astronomer, salvage pilot, and logistics planner restoring enough survey
capacity to publish a complete sector atlas.

## Canonical Names

| Surface | Name |
| --- | --- |
| Public pack name | `ECHO: Galactic Survey` |
| Short name | `Galactic Survey` |
| Pack id | `galactic-survey` |
| Main module id | `echogalacticsurveyprotocol` |
| Primary game mode | `long_range_survey` |
| Native pack id | `galactic-survey-native-edition` |
| NeoForge pack id | `galactic-survey-neoforge-edition` |
| Standalone pack id | `galactic-survey-standalone-edition` |
| Module release id | `galactic-survey-0.1.0-alpha` |

## Repository Names

| Purpose | Repository |
| --- | --- |
| Module source | `knoxhack/ECHO-Modules`, path `addons/echogalacticsurveyprotocol` |
| Native edition | `knoxhack/ECHO-Galactic-Survey-Native-Edition` |
| NeoForge edition | `knoxhack/ECHO-Galactic-Survey-NeoForge-Edition` |
| Standalone edition | `knoxhack/ECHO-Galactic-Survey-Standalone-Edition` |
| Launcher consumer | `knoxhack/ECHO-Launcher` |
| Release catalog | `knoxhack/ECHO-Release-Index` |
| Website surface | `knoxhack/ECHO-Platform-Website` |

## Required Module Surfaces

| Module | Responsibility |
| --- | --- |
| `echogalacticcore` | Sectors, celestial bodies, scan data, and catalog primitives. |
| `echoorbitalremnants` | Derelicts, debris fields, salvage events, wreck hazards, and loot hooks. |
| `echovehiclecore` | Probes, survey craft, shuttle handling, and probe recovery state. |
| `echoholomap` | Sector map, route overlays, orbital layers, catalog overlays, and probe status. |
| `echoterminal` | Survey network, probe control, route planner, and salvage log pages. |
| `echoindex` | Planetary catalog, discovery records, and atlas publication. |
| `echolens` | Local scans and salvage identification. |
| `echomissioncore` | Survey contracts and route objectives. |
| `echopowergrid` | Probe charging, relay repair, station power, and launch readiness. |
| `echologisticsnetwork` | Fuel, cargo, depot placement, route depots, and probe recovery. |
| `echoprogressioncore` | Certification tiers, badges, map layer unlocks, and sector access. |
| `echosoundcore` | Telemetry pings, ambience, alert tones, and catalog confirmations. |

## Ten-Phase Implementation

| Phase | Focus | Exit Gate |
| --- | --- | --- |
| 1 | Identity, scope, and ownership lock | Pack promise, boundaries, module ownership, non-goals, and release gates are locked. |
| 2 | Protocol scaffold and pack spine | `echogalacticsurveyprotocol` loads with starter outpost, Terminal, HoloMap, Index, and save-state surfaces. |
| 3 | Survey data contracts and seed content | Sectors, bodies, probes, routes, discoveries, salvage, loot, and depots validate cleanly. |
| 4 | First 30-minute vertical slice | Opening loop plays from outpost wake to first fuel-canister route objective. |
| 5 | Probe launch, scan confidence, and recovery | Probe launches produce imperfect but useful sector decisions across 3-5 launches. |
| 6 | HoloMap as main character | Map shows unknowns, scan cones, fuel range, route risk, depots, and catalog state. |
| 7 | Catalog, missions, and certification | Cataloging bodies, anomalies, wrecks, and routes unlocks missions, badges, and map layers. |
| 8 | Fuel routes, logistics, and depots | Fuel range, quality, return safety, cargo, and one remote depot alter route decisions. |
| 9 | Orbital salvage, hazards, and upgrades | One wreck is playable, readable, risky, and rewards a meaningful probe or route upgrade. |
| 10 | Editions, full arc, and release gate | Native, NeoForge, Standalone, Launcher, first-30, first-2-hour, and Survey Array evidence all pass. |

## First 30 Minutes

The route contract is stored at
`addons/echogalacticsurveyprotocol/src/main/resources/data/echogalacticsurveyprotocol/galacticsurvey/progression/first_30_minutes.json`.

It must prove:

- Wake at the quiet survey outpost.
- Open Terminal and see Survey Network Offline.
- Repair a small power relay.
- Claim or craft `starter_probe`.
- Bring `probe_launcher` online.
- Launch to `near_sector_01`.
- Reveal partial HoloMap scan cones.
- Lens-scan a fallen orbital fragment.
- Recover `burned_navigation_core`.
- Catalog `barren_moon_kg_01a`.
- Unlock `first_survey_hop`.
- Prepare `fuel_canister`.

## First 2 Hours

The route contract is stored at
`addons/echogalacticsurveyprotocol/src/main/resources/data/echogalacticsurveyprotocol/galacticsurvey/progression/first_2_hours.json`.

It must prove:

- Launch 3-5 probes.
- Catalog a moon, planet candidate, anomaly, and derelict-related discovery.
- Recover salvage from one orbital wreck.
- Build a basic fuel-safe route.
- Unlock `long_range_probe`.
- Establish `cinder_ring_remote_depot`.
- Complete `first_survey_circuit`.
- Earn a visible `catalog_badge`.

## Full Arc

Early game covers outpost repair, starter probes, first charts, and short-range
salvage. Midgame adds fuel routes, depots, better probes, and unstable wrecks.
Late game adds multi-sector surveys, rare anomalies, deep-space derelicts, and
advanced scan tools. Endgame restores the Galactic Survey Array and publishes a
complete sector atlas.

## Public Alpha Release Gate

Do not publish a public alpha until all of these have runtime evidence:

- Probe launch works.
- HoloMap reveals meaningful data.
- Catalog entries unlock from discoveries.
- Fuel route limits are understandable.
- One salvage site is playable.
- One vehicle/probe upgrade matters.
- First 2-hour loop has no dead end.
- Launcher install/update/repair/rollback all pass.

The module descriptor intentionally uses the `experimental` channel until those
gates are backed by real playthrough and launcher evidence.

## Validation

```text
node addons/echogalacticsurveyprotocol/scripts/validate-galactic-survey-contract.mjs --module-root addons/echogalacticsurveyprotocol
node addons/echogalacticsurveyprotocol/scripts/smoke-galactic-survey-route.mjs --module-root addons/echogalacticsurveyprotocol
```

These validate contract completeness and route proof integrity. They do not
replace an in-game first-30, first-2-hour, or Survey Array completion test.

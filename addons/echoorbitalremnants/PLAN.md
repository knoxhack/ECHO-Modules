# ECHO-7: Orbital Remnants Release Status

## v1.2.0 Full Integration Scope

ECHO-7 is complete as a standalone NeoForge space-survival arc from ruined Earth recovery through route repair chains, explorable Low Earth Orbit, the Lunar Scar Zone, Mars Ash Basin, Europa Cryo Ocean, Saturn Ring Graveyard, Titan Methane Shelf, the Nexus Anomaly Belt, ECHO-0, post-ECHO-0 surveys, NPC-driven faction outpost charters, and the final survey network seal.

## Implemented Systems

- Terminal-led progression with mission objectives, route locks, scan requirements, saved scan reports, faction state, ECHO memory, and final completion state.
- Launch readiness, Rocket Assembly Frame checklist, Emergency Rocket launch/return, orbital staging, route vessels, and saved return vectors.
- Menu-driven orbital machines with input/output slots, progress, charge, status text, and data-driven `orbital_processing` recipes.
- Release-safety hardening for machine inventory drops, virtual Rocket Assembly Frame output, once-only route arrival cache/threat seeding, high-altitude scan gating, and local ambient threat caps.
- Suit survival with oxygen, pressure, helmet seal, leaks, radiation, gravity, station power, recovery items, modules, and orbital HUD.
- Generated Earth recovery sites and generated route arrival sites with readable landmarks and fixed survival caches.
- Deterministic route terrain for Orbit, Moon, Mars, Europa, Saturn, Titan, and Nexus, including debris belts, cratered lunar ground, Martian ash/basalt, Europa cryo shelves, Saturn ring platforms, Titan methane shelves, and Nexus anomaly islands.
- Terminal-led route surveys for Orbit, Moon, Mars, Europa, Saturn, Titan, and post-ECHO-0 Nexus stabilization.
- Route repair chains with compatibility-safe gates, repair resources, visible terminal counts, and old-save bypass handling.
- Hostile orbital/deep-space mobs, major encounters with command bars, route-relevant rewards, and ECHO-0 final protocol completion.
- Final network completion after ECHO-0, all route surveys, Nexus stabilization, and three Tier I faction outpost charters.
- Faction pledge items, NPC outpost contacts, reward bundles, custom weapons, loot tables, advancements, docs, CI, and required Orbital GameTests.

## Release Checks

- `./gradlew :echoorbitalremnants:build`
- `./gradlew :echoorbitalremnants:runGameTestServer`
- Requires Java 25 with `JAVA_HOME` set or `java` available on `PATH`.
- Confirm the release jar appears in `addons/echoorbitalremnants/build/libs/`; the primary redirected Gradle output remains under `%LOCALAPPDATA%/EchoGradleBuild/Echo/echoorbitalremnants/libs/`.
- Optional manual pass: new survival world, terminal calibration, launch, every route, every machine menu, ECHO-0 completion.

## Known Boundaries

- No external energy API is required.
- No dependency on another space or quest mod is required.
- Existing save ids and progression keys are preserved for the public `1.2.0` stack line.
- Future expansions can add new dimensions or tech tiers after the current-dimensions survey network is stable in play.

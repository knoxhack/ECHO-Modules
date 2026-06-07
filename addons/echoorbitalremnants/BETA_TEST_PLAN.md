# ECHO-7 Release Test Plan

Use this checklist before publishing a release jar.

## Automated Checks

1. Run `.\gradlew.bat :echoorbitalremnants:build` from the ECHO workspace root.
2. Run `.\gradlew.bat validateEchoResources -PechoAddonSet=all -PechoPythonExecutable="<python.exe>"` from the ECHO workspace root.
3. Run `.\gradlew.bat :echoorbitalremnants:runGameTestServer` from the ECHO workspace root.
4. Confirm the release jar is `addons/echoorbitalremnants/build/libs/echoorbitalremnants-1.2.0.jar`.
5. Confirm the primary redirected output is `%LOCALAPPDATA%\EchoGradleBuild\Echo\echoorbitalremnants\libs\echoorbitalremnants-1.2.0.jar`.
6. Confirm all required Orbital GameTests pass, including `outpost_npc_registration`, `outpost_charter_gating`, `outpost_legacy_migration`, `outpost_action_validation`, `terminal_mission_cache_state`, `terminal_mission_integration`, `core_integration_contract`, `machine_break_drops_inventory`, `rocket_assembly_virtual_output`, `route_arrival_seed_once`, `high_altitude_scan_gate`, `ambient_threat_cap`, `strict_playable_path`, and `progress_persistence_round_trip`.
7. Confirm Core/Terminal/Ashfall stack versions are aligned: ECHO Core `1.2.0`, ECHO Terminal `1.2.0`, Ashfall Protocol `1.2.0`, and Orbital Remnants `1.2.0`.

## Clean Survival Smoke Test

1. Create a new survival world with only ECHO-7 enabled.
2. Craft the ECHO-7 Terminal.
3. Sneak-use the terminal on Earth and confirm starter recovery sites generate.
4. Loot the launch pad, crashed satellite, and comms array.
5. Craft or recover the Launch Platform, Rocket Assembly Frame, Fuel Refinery, Oxygen Compressor, pressure suit, Oxygen Tank, and rocket parts.
6. Open the Rocket Assembly Frame and confirm the Emergency Rocket appears only when the checklist is complete.
7. Break a normal processing machine with input/output loaded and confirm stored items drop; break a Rocket Assembly Frame with its rocket output visible and confirm no free rocket drops.
8. Launch to Low Earth Orbit and confirm an Earth return vector is saved.
9. Confirm the terminal names the next missing hook whenever progression is blocked.
10. Confirm climbing to high overworld altitude causes suit exposure but does not mark Low Earth Orbit before a real launch.

## Shared Terminal Smoke Test

1. Launch with ECHO: Terminal and ECHO: Orbital Remnants installed.
2. Open `echoterminal:echo_terminal` and confirm Orbital Command, Survey, and ECHO tabs appear under Addons.
3. Press SCAN from Orbital Command and Orbital Survey; both should execute the same server-side ECHO-7 scan path.
4. Complete Earth calibration and confirm the Earth Calibration mission becomes claimable in Orbital ECHO.
5. Claim the support cache once, verify utility items are delivered or stored through the Terminal reward service, and confirm a second claim does not duplicate rewards.
6. Repeat before and after an Ashfall Nexus choice in the full stack to confirm the lock reason and unlock handoff stay clear.
7. Confirm What Now shows Orbital blockers, Route Records lists Earth Recontact / Launch Chain / Stationfall Handoff / Route Worlds / ECHO-0 Quarantine, Vitals includes Orbital hazard telemetry while in route dimensions, Faction Atlas shows Radwarden / Crashbreak / Sporebound standings, and Reward Inbox agrees with standalone cache state.

## Creative Route Smoke Test

1. Open every machine menu and confirm progress, charge, blocked-output, and bad-input text still update.
2. Travel through Low Earth Orbit, Lunar Scar Zone, Mars Ash Basin, Europa Cryo Ocean, Saturn Ring Graveyard, Titan Methane Shelf, and Nexus Anomaly Belt.
3. Confirm each arrival site has a readable landmark and at least one useful cache.
4. Repair three Station Relay Nodes, three Helium Extractor Nodes, three Mars Pressure Consoles, three Europa Thermal Arrays, three Saturn Ring Relays, and three Titan Methane Pumps.
5. Spawn or encounter Corrupted Docking AI, Lunar Nexus Husk, Abandoned Captain, Europa Cryo Warden, Saturn Relay Sentinel, Titan Methane Stalker, and ECHO-0.
6. Defeat ECHO-0 and confirm the terminal reports quarantine resolution and grants the final-protocol reward once.
7. Confirm Nexus stabilization is locked before ECHO-0, then complete three unique survey logs for Orbit, Moon, Mars, Europa, Saturn, Titan, and Nexus.
8. Confirm Nexus 0/3 through 2/3 guidance names Anchor/Growth sites and Nexus Stabilizer Shard recovery, and that stabilization grants the survey reward once.
9. Complete Crashbreak, Radwarden, and Sporebound Tier I outpost charters, press SCAN to seal the final survey network, and repeat SCAN to confirm only completion guidance refreshes.

## Route Pacing Smoke Test

1. Use the default Adventure preset.
2. On each route, arrive, explore for 3-5 minutes, repair one objective, and open one arrival or deep-site cache.
3. Confirm caches provide route progression value, one crafting support stack, and oxygen/seal recovery.
4. Confirm hazards remain readable and tense without forcing emergency oxygen or sealant recovery every minute.
5. Complete the route survey and confirm the terminal reports reduced local pressure for that route.

## Full Earth-To-Nexus Manual Route Pass

1. Start from a survival-oriented setup after an ECHO: Ashfall Protocol Nexus choice.
2. Run Earth -> Low Earth Orbit -> Moon -> Mars -> Europa -> Saturn -> Titan -> Nexus in order.
3. At every route handoff, confirm the route item gives action-bar feedback, sound/particles, clear lock text when blocked, and does not consume the reusable vessel/key.
4. At every route site, confirm the objective block is visually discoverable, SCAN feedback is audible/visible, and one cache feels worth opening.
5. Repeat one route burn after first arrival and confirm it reuses travel/return-vector behavior without reseeding the first-arrival cache or duplicating the arrival threat wave.
6. At Saturn and Titan, confirm the route identities read differently: Saturn relay ribs/salvage lanes, Titan methane/tholin pressure cues.
7. During encounters, confirm special attacks have a readable tell before pressure/oxygen/radiation effects land and ambient threats do not pile up beyond the local cap.
8. Confirm terminal next-step text, return vectors, faction outpost wording, ECHO-0 guidance, Nexus stabilization, and final seal all match the automated route state.

## Faction Outpost Smoke Test

1. Use each pledge item: Radwarden Orbital Badge, Crashbreak Salvage Marker, and Sporebound Anomaly Sigil.
2. Visit Saturn, Titan, and Nexus faction hubs and confirm the Orbital Faction Contact NPC appears without duplicate stacking.
3. Confirm the custom ECHO dialogue screen opens and shows Talk, Request Service, Barter, Accept Charter, and Complete Charter actions.
4. Complete the Crashbreak Saturn Salvage Charter by scanning a Saturn Ring Relay or delivering Saturn Ring Fragment plus Vacuum Circuit.
5. Complete the Radwarden Titan Containment Charter by scanning a Titan Methane Pump or delivering Titan Methane Cell plus Suit Sealant Patch.
6. Complete the Sporebound Nexus Anchor Charter after ECHO-0 by scanning Nexus Anchor/Growth or delivering a Nexus Stabilizer Shard.
7. Confirm services and barters consume costs, grant rewards once, respect cooldowns, and do not require Ashfall UI/classes.

## Return Vector Checks

1. Try using route vessels from the wrong state and confirm the failure message is clear.
2. Try returning without a saved vector and confirm the failure message is clear.
3. Save a route vector, return, and confirm the player lands safely.

## Release Sanity

1. Check `README.md`, `guide.md`, `CHANGELOG.md`, and `KNOWN_ISSUES.md` for the shipped version.
2. Upload only the latest `1.2.0` jar.
3. Include the known issues and feedback link in the release notes.
4. Record the manual smoke-test result in the release verification notes.

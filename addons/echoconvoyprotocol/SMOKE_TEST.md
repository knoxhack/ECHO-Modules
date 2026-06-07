# Convoy Protocol Smoke Test

1. Set JDK 25:

   ```powershell
   $env:JAVA_HOME='C:\Users\knox\AppData\Local\EchoToolchains\jdk25\jdk-25.0.3+9'
   $env:Path="$env:JAVA_HOME\bin;$env:Path"
   ```

2. Build the dependency and addon:

   ```powershell
   .\gradlew.bat :echologisticsnetwork:build --no-daemon
   .\gradlew.bat :echoholomap:build --no-daemon
   .\gradlew.bat :echomultiblockcore:build --no-daemon
   .\gradlew.bat :echoconvoyprotocol:build --no-daemon
   ```

3. Launch the dev client and open the `ECHO: Convoy Protocol` creative tab.
4. Place a Convoy Depot Controller.
5. Use `/echo_convoy depot validate` and verify incomplete diagnostics list missing depot blocks.
6. Build the Convoy Depot from `data/echoconvoyprotocol/echo_multiblocks/convoy_depot.json`.
7. Place the MultiblockCore Robotic Arm in the depot's robotic slot.
8. Install a Cargo Clamp Head or Heavy Loader Head on the arm.
9. Right-click the controller or run `/echo_convoy depot validate`; the depot should form.
10. Put a Field Supply Crate in the Cargo Input Crate.
11. Run `/echo_convoy task start load_field_supply_crate`.
12. Wait for completion and run `/echo_convoy readiness`; cargo should increase and the crate should consume exactly one Field Supply Crate when present. If no crate item exists, readiness may still use reserve behavior but must report that local inventory is missing.
13. Put a Fuel Cell in the input crate/tank and install a Fuel Injector Head on a reachable arm in a Fuel Refinery Pad.
14. Run `/echo_convoy task start refuel_convoy`; fuel should increase and consume one Fuel Cell, falling back to one Fuel Canister if no Fuel Cell is present.
15. Put a Convoy Route Chip in the input crate and run `/echo_convoy task start prepare_route_dispatch`.
16. If Logistics Network is installed, run `/echo_convoy logistics` and `/echo_convoy task start request_route_supplies`; the command output should show the route network id, loadout id, and an active or blocked delivery state without consuming unrelated Convoy inputs.
17. Run `/echo_convoy task start sync_logistics_inventory`; readiness should report Logistics online/loadout/fuel/cargo state when a compatible network is present, or a clear local fallback diagnostic when it is absent.
18. Run `/echo_convoy ops launch echo_7_ruined_highway`; the controller should stage and launch a Field Ops operation with phase, ETA, and operation score.
19. Run `/echo_convoy ops`; verify phase, current stage, ETA, joined vehicle state, and any incident/failure reason are readable.
20. Optionally start the matching physical vehicle route and trigger a Roadside Signal Marker; `/echo_convoy ops` should show the joined vehicle and advanced stage.
21. If the operation enters `INCIDENT_BLOCKED`, run `/echo_convoy task start resolve_field_incident` or `/echo_convoy ops resolve`; the operation should resume.
22. Run `/echo_convoy markers`; HoloMap-enabled runs should report Convoy Facilities, Convoy Routes, Recovery Signals, route ids, ordered route markers, active operation markers, incident warnings, and any fallback search-zone markers.
23. Let the operation return, or use `/echo_convoy complete echo_7_ruined_highway`; salvage/output state should be ready.
24. Run `/echo_convoy task start export_salvage_manifest`; Logistics should accept the export when available, otherwise Convoy should fall back to local output crate diagnostics.
25. Run `/echo_convoy task start unload_salvage_return`; the output crate should receive reward material if salvage was not exported.
26. Run `/echo_convoy ops recall`, then `/echo_convoy task start recover_failed_operation` or `/echo_convoy ops recover`; a recovery marker should appear, then clear or update after recovery completes.
27. Break a required depot block and rerun `/echo_convoy depot validate`; structure state should become incomplete/damaged.
28. Replace the block and revalidate.
29. Save and reload the world; readiness, active route, Field Ops state, Logistics request state, route marker index, task queue, and controller state should persist or rebuild safely.
30. Check common config behavior:
   - `routes.difficultyMultiplier` changes physical route fuel requirements in Terminal, Core route records, Index route views, and route start blockers.
   - `fieldOps.durationScale` changes depot operation ETA/duration without changing route data files.
   - `fieldOps.vehicleJoinPolicy=VEHICLE_REQUIRED` blocks depot Field Ops launch until a physical vehicle has joined the operation.
   - `fieldOps.incidentFrequencyPercent=0` disables deterministic incidents, while `100` restores all eligible incident checks.
   - `holomap.preciseMarkers=false` leaves Convoy route markers estimated even when a roadside marker was recorded.
   - `diagnostics.debug=true` adds extra Convoy integration, migration, and fallback detail to the log.
31. Spawn or stand near a Convoy vehicle and run `/echo_convoy vehicle`; verify the command reports callsign, kind, fuel, battery, damage, cargo, route, docked state, shielding, and owner.
32. Run `.\gradlew.bat :echoconvoyprotocol:runGameTestServer --no-daemon` for dedicated-server-safe class loading and data sanity.
33. Run:

   ```powershell
   python tools\validate_gameplay_data.py
   python tools\validate_resources.py --addon-set all
   ```

Convoy should add no missing asset, recipe, tag, route, incident profile, or data issues.

## 1.3.0 Vehicle Visual Polish Checks

Run these checks with RenderCore installed, then repeat after temporarily removing RenderCore so the fallback renderer is exercised:

- Spawn Scrap Bike, Wasteland Rover, Cargo Crawler, and Armored Relay Truck from their kits and from `/summon`.
- Confirm entity dimensions feel aligned with the final silhouettes: Scrap Bike `0.95 x 1.35`, Wasteland Rover `2.25 x 1.75`, Cargo Crawler `3.05 x 1.85`, Armored Relay Truck `3.10 x 2.15`.
- Mount each vehicle and check rider placement: Scrap Bike `0.95`, Wasteland Rover `1.35`, Cargo Crawler `1.45`, Armored Relay Truck `1.65` passenger height.
- Inspect side, front three-quarter, and rear three-quarter views for all four vehicles.
- Confirm Scrap Bike reads as a narrow two-wheel field bike, Wasteland Rover has clear tires/cab/scanner hardware, Cargo Crawler sits lower on readable tracks, and Armored Relay Truck reads as a six-wheel signal vehicle.
- Drive each vehicle long enough to see wheel motion and active RenderCore overlays.
- Force or inflict damage, then confirm damage plates/heat overlays appear without hiding the base vehicle material.
- Check low-power/offline states, docked clamps, cargo-loaded markers, and shielding plates where that vehicle supports them.
- Confirm no purple/black missing texture, no severe rider clipping, no severe collision mismatch, and no regression in Route Beacon pairing or field-station deployment.

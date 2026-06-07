# ECHO: Nexus Protocol Public Release Smoke Pass

Status: automated release-risk pass complete; manual interactive client run still required on a graphical modpack instance

Release command setup:

- Set `JAVA_HOME=C:\Users\knox\.jdks\temurin-25` before running Gradle on this workstation; the default shell may not have Java on `PATH`.

Automated coverage added in this polish pass:

- Nexus beta addon set wiring: Core, Terminal, Orbital, Nexus, plus root Ashfall.
- Nexus Field Map telemetry for all five field states, corruption pressure, storms, and reality tears.
- Collapsed-chunk recovery tools: Stabilized Purity Charge and Field Anchor.
- Ending-specific world feedback for restore, control, destroy, and merge.
- Config defaults for balance preset, field-map radius, recovery strength, and recovery quarantine time.
- Nexus resource completeness validation for registered blocks, explicit items, entities, boss loot tables, language keys, and JEI-to-JSON recipe parity.
- Boss loot tables for Corruption Warden and Nexus Guardian, with unique progression rewards guarded in server-side state.
- Deterministic Terminal first-contact scan path so an unlocked player can claim starter Nexus materials without pure random discovery.

Strict functionality matrix:

| Feature | Obtain? | Use? | Server state? | Persists? | Feedback? | Success path? | Failure path? | Safe once-only outputs? | Test/smoke step? | Terminal/Core integration? |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Starter discovery/cache | Yes: Stationfall handoff or Nexus dev unlock exposes SCAN FIELD and starter cache. | Yes: Terminal SCAN FIELD then CLAIM CACHE. | Yes: research, scan id, claimed cache, pending rewards. | Yes: `nexus_player_data_persistence_round_trip`. | Yes: Terminal status/action hint and chat on scan. | Scan, claim, claim Terminal rewards. | Locked reason before handoff; repeated claim is no-op. | Yes: repeated claim does not duplicate pending or fallback inventory rewards. | `strict_playable_starter_loop`, `terminal_starter_path`. | Yes: Terminal mission provider and ECHO Core reward storage. |
| Scanner/field telemetry | Yes: starter shards plus vanilla materials cover the scanner recipe. | Yes: scanner/Terminal scan record field discovery. | Yes: research, scans, field telemetry. | Yes: player telemetry and map arrays round-trip. | Yes: chat, tooltip/UI, Terminal Field/Map tabs. | Scan a Nexus field signature or use SCAN FIELD. | Locked until handoff; scanner misses non-signatures. | Yes: no item output; state is idempotent scan progress. | `scanner_research_unlocks`, `nexus_player_data_persistence_round_trip`; manual Field tab readability. | Yes: Terminal mission and Core discovery state. |
| Recycler/charge loop | Yes: starter shards plus vanilla materials cover the recycler recipe. | Yes: insert Nexus Shard or dirty Nexus input. | Yes: machine progress, charge, contamination, player used-machine state. | Yes: `nexus_machine_persistence_round_trip`. | Yes: machine screen/status line and Terminal Dirty Charge objective. | One Nexus Shard produces Reality Dust and charge. | Invalid input rejected; blocked output pauses. | Yes: one input shrinks once; output count persists without duplication/loss. | `strict_playable_starter_loop`, `machine_recipe_paths`. | Yes: Terminal Dirty Charge advances from machine state. |
| Stabilizer/filter/decoder/forge | Yes: recipes/resources validated; progression rewards guide parts. | Yes: machine recipes and field actions execute. | Yes: charge, contamination, field, fragments, used machines. | Yes: machine persistence plus world/player round trips. | Yes: machine screen/status, chat, Terminal objectives. | Stabilize/filter field, decode fragments, forge Core materials. | Bad input, missing charge, blocked output, and leak states are visible. | Yes: recipe-driven outputs and charge costs are gated. | `machine_recipe_paths`, `field_stabilizer`, `release_risk_fixes`; manual UI pacing. | Yes: Terminal mission sequence reads used-machine state. |
| Monolith/Core key | Yes: fragment/Monolith progression and Terminal reward provide Core Access Key. | Yes: activate Monolith and use Core key. | Yes: monolith, forbidden access, core entered, used gear. | Yes: player and world round trips cover it. | Yes: chat, Terminal Monolith/Core Door missions. | Activate Monolith, claim key, open Core route. | Locked before prerequisites; missing dimension refuses without advancing. | Yes: one-time mission cache and guarded boss rewards. | `progression_and_endings`, `core_access_key_route`. | Yes: ECHO Core milestones and Terminal actions. |
| Core entry/return | Yes: Core Access Key/Core Key Assembly. | Yes: right-click key to enter; right-click in Nexus to return. | Yes: return dimension/position/rotation and coreEntered. | Yes: return vector round-trips through player data. | Yes: chat success/failure lines. | Opens Nexus dimension and restores saved return vector. | Missing Nexus dimension fails safely. | Yes: no item duplication; stale return vector clears after return. | `core_access_key_route` covers safe failure in headless GameTests; manual dimension entry/quit-reload return check required. | Yes: records ECHO Core entry milestone and Terminal Core Door state. |
| Warden/Guardian/final path | Yes: Nexus progression leads to fights and final mission. | Yes: defeat bosses, choose one final path. | Yes: world/player defeated flags and ending state. | Yes: world codec and player round trip cover flags/path. | Yes: boss cues, drops, Terminal final actions, chat path line. | Defeat Guardian, choose restore/control/destroy/merge. | Repeated/different ending choice is blocked. | Yes: one-time reward gates prevent repeat unique drops. | `one_time_boss_reward_gates`, `progression_and_endings`; manual combat feel. | Yes: ECHO Core final path milestones and Terminal final mission. |
| Storm/field hazards | Environmental. | Yes: storms, tears, recovery charges, anchors, seals. | Yes: field, corruption, quarantine, storm ticks, tears. | Yes: world codec and player telemetry round trips. | Yes: Terminal Field/Map tabs, sounds, block effects. | Recover with charges/anchors/seals and stabilize chunks. | Quarantine suppresses storms; local mob cap stops runaway spawns. | Yes: capped spawning and bounded block conversions. | `world_quarantine_storm_tears`, `storm_spawn_cap`, `collapsed_recovery_tools`. | Yes: Field/Map tabs read ECHO hazard telemetry. |

Manual pass checklist:

- New world start.
- Nexus unlock through Stationfall handoff or Nexus dev unlock.
- Terminal mission tab readability.
- Nexus Field tab readability.
- Nexus Field Map tab readability.
- Nexus machine UI: slots, progress, charge, contamination, status.
- Nexus dimension entry and saved return point.
- Blackbox Monolith activation and anomaly storm feedback.
- Corruption Warden fight feel and drops.
- Nexus Guardian phase readability and defeat side effects.
- One final ending choice and permanence message.
- Quit/reload persistence for Nexus player data, world field data, ending state, and return position.

Manual notes:

- 2026-05-06 automated hardening pass:
  - Fixed Core Access Key missing-dimension fallback so progression no longer advances if `echonexusprotocol:nexus` is unavailable.
  - Added safe Nexus entry pad creation at the fixed Core route arrival point.
  - Deduplicated multiplayer field ticking and pruned stale storm telemetry.
  - Made ending world feedback one-shot and protected final path permanence through Terminal and command routes.
  - Reduced Protocol Seal sound spam and made sounds/mission credit happen only when a seal action succeeds.
  - Removed natural Warden biome spawning and blocked Warden/Guardian natural spawn placement.
  - Added recipe-aware machine input filtering, clearer machine status text, Archive Seeker recovery feedback, Data Wraith pulse behavior, Warden summon cap, Guardian phase effects, and Merge-path reality tear traversal.
  - Confirmed beta jar folder contains only Ashfall, Core, Terminal, Orbital, and Nexus jars.
- 2026-05-06 release-polish fix pass:
  - Fixed Protocol Seal relay transfer so charge is consumed only after a target tank accepts it.
  - Added Nexus regular mob loot tables and POI chest loot tables.
  - Added Nexus biome dressing features for White Signal Trees, crystal clusters, Static Fluid pools, debris, ferrite pockets, lab pipes, and reality tear hotspots.
  - Added machine hover/status help for valid inputs, missing charge, output conflicts, contamination risk, and field risk.
  - Added role-specific Nexus mob presentation cues with particles, float/pulse scaling, and phase tinting.
  - Kept the Field Map display radius intentionally fixed at 5x5 for release stability.
  - Fixed the beta release-lane blockers in Orbital/Ashfall verification without adding future addon jars to the beta set.
- Progression clarity: still needs real client playthrough.
- UI readability: machine status text improved; still needs real client visual check.
- Loot feel: not yet manually verified.
- Boss feel: server behavior improved; still needs real combat pass.
- Field recovery feel: automated coverage exists; still needs real client pacing pass.
- Reload persistence: not yet manually verified in a client quit/reload session.
- 2026-05-10 release-ready V1 hardening pass:
  - Added Nexus-specific resource completeness checks to `validateEchoResources`.
  - Added boss entity loot tables for repeatable non-unique drops.
  - Added one-time reward guards for Corruption Warden and Nexus Guardian milestone drops.
  - Added a local anomaly storm mob cap around players.
  - Made Terminal first-contact scan mark real Nexus discovery progress before starter-cache claim.
  - Added automated coverage for starter path, one-time reward gates, storm cap, JEI recipe parity, and world saved-data codec round trips.
- 2026-05-10 strict playability pass:
  - Added `strict_playable_starter_loop` to prove Terminal scan, once-only starter cache claim, recipe availability, first Recycler process, invalid input rejection, player progression, and Dirty Charge Terminal visibility.
  - Added `nexus_player_data_persistence_round_trip` for research, scans, Terminal caches, machine/seal/gear use, fragments, boss flags, Core entry, ending path, return vector, and field map telemetry.
  - Added `nexus_machine_persistence_round_trip` for inventory, charge, contamination, progress, max progress, and status.
  - Added `core_access_key_route`; the current headless GameTest server does not load `echonexusprotocol:nexus`, so this pass verifies safe missing-dimension failure while keeping real entry/return in the manual checklist.

Verification notes:

- `:echonexusprotocol:build -PechoAddonSet=beta --warning-mode all --no-configuration-cache` passed.
- `python tools\validate_resources.py --addon-set beta` passed.
- `python tools\validate_gameplay_data.py` passed.
- `python tools\check_echo_runtime_logs.py --max-age-minutes 180` passed.
- `copyEchoJarsToModpack -PechoAddonSet=beta` passed into `build\tmp\echo-release-mods`.
- `checkEchoModJarSet -PechoAddonSet=beta` passed with exactly five beta jars.
- `:echonexusprotocol:runGameTestServer -PechoAddonSet=beta --warning-mode all --no-configuration-cache` passed.
- `verifyEchoRelease -PechoAddonSet=beta -PechoModpackModsDir="<workspace>\build\tmp\echo-release-mods" --warning-mode all --max-workers=1 --no-configuration-cache` passed.
- `checkEchoModJarSet -PechoAddonSet=beta` passed with exactly five beta jars: Ashfall, Core, Terminal, Orbital, and Nexus.
- 2026-05-10 strict playability pass: `validateEchoResources validateEchoGameplayData :echonexusprotocol:runGameTestServer -PechoAddonSet=beta --warning-mode all --no-configuration-cache` passed; all 31 required Nexus GameTests passed.
- 2026-05-10 strict playability pass: `:echonexusprotocol:build buildEchoWorkspace -PechoAddonSet=beta --warning-mode all --no-configuration-cache` passed.
- 2026-05-10 strict playability pass: `checkEchoRuntimeLogs -PechoAddonSet=beta --warning-mode all --no-configuration-cache -PechoRuntimeLogMaxAgeMinutes=240` was blocked by fresh crash reports in `addons\echoagriculturereclamation\run\crash-reports` and `addons\echologisticsnetwork\run\crash-reports`; the latest Nexus GameTest log passed cleanly.

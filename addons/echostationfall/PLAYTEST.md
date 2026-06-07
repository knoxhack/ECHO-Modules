# Stationfall Playtest Certification

Stationfall is generated manually when the player boards the dead station route. It intentionally does not use normal biome or chunk worldgen hooks.

## Build Checks

Run these from the Echo repository root:

```powershell
.\gradlew.bat -PechoAddonSet=all :echostationfall:compileJava
.\gradlew.bat -PechoAddonSet=all :echostationfall:runGameTestServer
```

If full-stack CI fails, first confirm whether the failure is in `echostationfall`, an upstream required addon, or an unrelated addon pulled in by `-PechoAddonSet=all`.
For release certification, prefer `--no-configuration-cache` until stale configuration cache behavior has been ruled out:

```powershell
.\gradlew.bat --no-daemon --no-configuration-cache -PechoAddonSet=all :echostationfall:compileJava
.\gradlew.bat --no-daemon --no-configuration-cache -PechoAddonSet=all :echostationfall:runGameTestServer
```

## Manual Client Route

Run:

```powershell
.\gradlew.bat -PechoAddonSet=all :echostationfall:runStationfallClient
```

Certification route:

1. Board Stationfall from the Terminal `BOARD STATION` action or a Station Access Card.
2. Confirm no new file appears in `addons/echostationfall/run/crash-reports`.
3. Confirm Docking Ring visibility, corridor light fixtures, readable section interiors, and a dim non-daylight mood.
4. Encounter at least one Stationfall mob and confirm it renders without a client crash.
5. Use `/stationfall debug` as an operator and confirm it reports dimension, section, lighting version, entities, panic, oxygen, pressure, route tracker state, boss state, and blackbox state.
6. Use Terminal `TRACK SECTION` and confirm it mirrors route tracker state.
7. Restore a power node with a Station Battery.
8. Decode at least one crew log terminal.
9. Complete the Terminal `STABILIZE SECTIONS` mission before starting the boss:
   - Hydroponics Bay: purge at least three corrupted growth clusters.
   - Medical Wing: decode the medical crew log/manifest.
   - Engineering Deck: restore the power node.
   - Containment Wing: query at least three containment pods.
   - Observation Deck: align at least two cracked observation glass antenna nodes.
10. Use the Data Core terminal and recover the AI Override.
11. Try the Command Module console before completing stabilization and confirm it gives a guard message instead of starting the boss.
12. Use the Command Module console, spawn Station Mother, defeat it, and claim the Stationfall Blackbox.
13. Attempt the final reward/blackbox state twice and confirm it is duplicate-safe.
14. Return from Stationfall and confirm the saved return vector works.

## Integration Surfaces

- `StationfallRouteService` owns boarding and return.
- `StationfallStationGenerator` owns deterministic station placement, lighting, caches, and dressing.
- `StationfallStationState` stores world-level route state, lighting repair version, boss state, breaches, and section objectives.
- `StationfallProgress` stores player-level route state, logs, power, objectives, terminal claims, return vector, and milestones.
- `StationfallIndustrialCompat` exposes small support hooks for Industrial Nexus: scrubber stabilization, suit support, and AI override component credit.
- `StationfallTerminalCommonIntegration` exposes Terminal missions, archives, rewards, `BOARD STATION`, `RETURN`, and `TRACK SECTION`.

## Known Polish Backlog

- Replace tinted vanilla fallback mob renderers with bespoke Stationfall models, textures, and sounds.
- Add actual sound events and `.ogg` assets for station hum, terminal hum, pressure leaks, panic pulses, mob cues, and Station Mother phases.
- Add custom particles for hull breaches, signal panic, power restoration, and boss phase transitions.
- Continue dressing sections with more deterministic landmarks, signage, decals, cable runs, sealed windows, and damaged consoles.
- Run a full survival balance pass for oxygen drain, pressure damage, panic gain/decay, dampener durability, cache loot, mob spawn pressure, and boss pacing.

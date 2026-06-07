<!-- CURSEFORGE_README_START -->
# SoundCore by ECHO Labs

![SoundCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echosoundcore/brand-sheet.png)

**Display name: SoundCore**

![SoundCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echosoundcore/features-portrait.png)

![SoundCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echosoundcore/features-landscape.png)

## CurseForge Summary

Display name: SoundCore

## Main Features

- Audio profiles.
- Ambience and stingers.
- Soundscape control hooks.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echosoundcore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echosoundcore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echosoundcore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: SoundCore

**Display name:** ECHO: SoundCore  
**Mod ID:** `echosoundcore`  
**Version:** 1.0.0  
**License:** All Rights Reserved

---

## What is SoundCore?

ECHO: SoundCore is the shared first-party audio framework for the ECHO / Ashfall ecosystem. It provides:

- **Adaptive music** with data-driven priority, specificity, weight, and stable-id selection
- **Biome, faction, combat, and boss music**
- **Mission stingers and chapter themes**
- **Terminal, Lens, HoloMap, SignalOS, and PowerGrid UI sounds**
- **Ambience layers** for Nexus, Blackbox, Stationfall, and Orbital
- **Data-driven music and ambience profiles**
- **NetCore-backed server-to-client audio actions**
- **Core sound service registration for optional-safe addon calls**
- **Suno AI music pipeline support**

SoundCore makes Ashfall feel like its own complete game through audio identity, without hard dependencies on any optional ECHO module.

---

## What SoundCore Owns

- All registered `SoundEvent` instances under the `echosoundcore` namespace
- The client-side adaptive music manager (`SoundCoreMusicManager`)
- The client-side ambience layer manager (`SoundCoreAmbienceManager`)
- The public API (`SoundCoreApi`, `SoundCoreClientApi`)
- Dev/test commands (`/echosoundcore`)
- Data-driven profile reload listeners for music and ambience JSON

---

## How It Fits ECHO

SoundCore sits alongside ECHO Core and provides the audio service layer. Other addons call `EchoCoreServices.soundService()` or `SoundCoreApi` to play stingers, push audio contexts, or request music changes. SoundCore requires `echocore` and `echonetcore`; gameplay/content addons remain optional and guarded.

SoundCore keeps the mod JAR lean: finished tracks ship as real normalized `.ogg` files, while unfinished events in `sounds.json` point at one shared silent fallback so missing-asset warnings stay out of the release path without duplicating placeholder audio.

---

## 1.0.0 Integration Surface

- Registers the real SoundCore `ISoundService` through ECHO Core.
- Sends clientbound audio actions for one-shots, context set/patch/clear, profile play, controlled stop, and event stop.
- Publishes diagnostics for current music state, selection reason, loaded profiles, active events, and missing assets.
- Uses data-driven music profiles before hardcoded fallback tracks.
- Maintains historical `ui.signaloos.*` ids while adding canonical `ui.signalos.*` aliases.
- Ships GameTests and catalog validation for service, packet, profile, subtitle, sounds.json, and asset coverage.

---

## Folder Paths for Music Files

Place exported `.ogg` files under:

```
src/main/resources/assets/echosoundcore/sounds/
```

See `src/main/resources/assets/echosoundcore/sounds/README.md` for the full replacement tree. Large soundtrack/stinger drops should be shipped as final OGG replacements or a separate resource/audio pack, not as duplicated placeholders in the core mod JAR.

---

## Exact Required Suno File Names

SoundCore expects these file names (without extension in `sounds.json`, but files must be `.ogg`):

### Menu
- `music/menu/menu_ashfall_theme`
- `music/menu/menu_nexus_theme`
- `music/menu/menu_orbital_theme`
- `music/menu/menu_blackbox_theme`

### Gameplay
- `music/gameplay/gameplay_safe_base_01`
- `music/gameplay/gameplay_exploration_01`
- `music/gameplay/gameplay_night_01`
- `music/gameplay/gameplay_underground_01`

### Biome
- `music/biome/biome_wasteland_01`
- `music/biome/biome_crash_zone_01`
- `music/biome/biome_toxic_swamp_01`
- `music/biome/biome_radiation_zone_01`
- `music/biome/biome_cryogenic_ruins_01`
- `music/biome/biome_ruined_city_01`
- `music/biome/biome_industrial_ruins_01`
- `music/biome/biome_nexus_scar_01`

### Faction
- `music/faction/faction_radwarden_ambient_01`
- `music/faction/faction_crashbreak_ambient_01`
- `music/faction/faction_sporebound_ambient_01`
- `music/faction/faction_radwarden_combat_01`
- `music/faction/faction_crashbreak_combat_01`
- `music/faction/faction_sporebound_combat_01`

### Combat
- `music/combat/combat_light_01`
- `music/combat/combat_heavy_01`
- `music/combat/combat_nexus_01`
- `music/combat/combat_faction_raid_01`
- `music/combat/combat_siege_01`

### Boss
- `music/boss/boss_warden_01`
- `music/boss/boss_guardian_wasteland_01`
- `music/boss/boss_guardian_toxic_01`
- `music/boss/boss_guardian_radiation_01`
- `music/boss/boss_guardian_cryo_01`
- `music/boss/boss_guardian_industrial_01`
- `music/boss/boss_guardian_city_01`
- `music/boss/boss_guardian_nexus_01`
- `music/boss/boss_corruption_bloom_01`
- `music/boss/boss_severance_engine_01`
- `music/boss/boss_mirror_command_01`
- `music/boss/boss_station_mother_01`

### Chapter
- `music/chapter/chapter_ashfall_protocol_01`
- `music/chapter/chapter_orbital_remnants_01`
- `music/chapter/chapter_agriculture_reclamation_01`
- `music/chapter/chapter_stationfall_01`
- `music/chapter/chapter_nexus_protocol_01`
- `music/chapter/chapter_industrial_nexus_01`
- `music/chapter/chapter_logistics_network_01`
- `music/chapter/chapter_convoy_protocol_01`
- `music/chapter/chapter_armory_01`
- `music/chapter/chapter_blackbox_protocol_01`

### Terminal
- `music/terminal/terminal_command_bed_01`
- `music/terminal/terminal_nexus_corrupted_01`

### Nexus
- `music/nexus/nexus_core_ambience_01`
- `music/nexus/nexus_choice_room_01`
- `music/nexus/nexus_siege_01`

### Blackbox
- `music/blackbox/blackbox_memory_fragment_01`
- `music/blackbox/blackbox_false_signal_01`
- `music/blackbox/blackbox_truth_engine_01`

### Stingers
- `stinger/stinger_mission_accept`
- `stinger/stinger_mission_update`
- `stinger/stinger_objective_complete`
- `stinger/stinger_mission_complete`
- `stinger/stinger_signal_detected`
- `stinger/stinger_guardian_located`
- `stinger/stinger_nexus_state_changed`
- `stinger/stinger_chapter_unlocked`
- `stinger/stinger_reward_available`
- `stinger/stinger_faction_radwarden_rep_up`
- `stinger/stinger_faction_crashbreak_rep_up`
- `stinger/stinger_faction_sporebound_rep_up`

---

## How to Convert Suno Music to .ogg

1. Generate instrumental track in Suno.
2. Download the track.
3. Trim intro/outro if needed.
4. Make it loop cleanly.
5. Normalize volume.
6. Convert to `.ogg` (e.g., with Audacity or ffmpeg).
7. Rename using lowercase snake_case.
8. Place in the correct SoundCore folder.
9. Confirm `sounds.json` path matches.
10. Test in-game with `/echosoundcore play <soundId>`.

---

## sounds.json Naming Rules

- Use the registered SoundEvent ID as the JSON key.
- The `name` field uses the format `echosoundcore:<folder>/<file_base_name>` without `.ogg`.
- Long music tracks **must** have `"stream": true`.
- Short stingers and UI sounds do **not** need `"stream": true`.

---

## API Examples

### Server-side stinger
```java
SoundCoreApi.playMissionAccepted(player);
SoundCoreApi.playSignalDetected(player);
```

### Push audio context
```java
SoundCoreContext ctx = new SoundCoreContext()
    .chapter(SoundCoreChapter.ASHFALL)
    .combatIntensity(SoundCoreCombatIntensity.LIGHT)
    .biome(Identifier.fromNamespaceAndPath("echoashfallprotocol", "wasteland"));
SoundCoreApi.pushAudioContext(ctx);
```

### Client-side local UI sound
```java
SoundCoreClientApi.playLocalUi(SoundCoreSounds.UI_TERMINAL_OPEN.get(), 1.0f, 1.0f);
```

---

## Command Examples

```
/echosoundcore play echosoundcore:music.biome.wasteland
/echosoundcore stinger stinger_mission_accept
/echosoundcore combat heavy
/echosoundcore nexus 0.75
/echosoundcore stop
/echosoundcore debug
/echosoundcore reload
```

---

## Config Options

All config is client-side (`client` spec):

- `enableAdaptiveMusic` / `enableMenuMusic` / `enableBiomeMusic` / `enableCombatMusic` / `enableBossMusic`
- `enableMissionStingers` / `enableTerminalSounds` / `enableLensSounds` / `enableHoloMapSounds` / `enableSignalOSSounds`
- `enablePowerGridSounds` / `enableDroneSounds`
- `enableNexusAmbience` / `enableBlackboxAmbience` / `enableStationfallAmbience` / `enableOrbitalAmbience`
- `enableAmbienceLayers` / `enableDebugOverlay`
- Volume multipliers for music, UI, ambience, stingers, combat music
- `nexusDistortionIntensity`
- `musicChangeCooldownTicks`, `minimumTrackPlayTicks`
- `terminalDucksWorldMusic`, `terminalMusicBedEnabled`

---

## Integration Notes

SoundCore provides optional bridge classes loaded via reflection if the corresponding mod is present:
- **ECHO Core** - addon chapter registration
- **Terminal** - terminal UI sounds and command bed context
- **MissionCore** - mission stinger hooks
- **WorldCore** - region/hazard context for ambience
- **Lens** - scan sound helpers
- **HoloMap** - map open/close/waypoint/route sounds
- **PowerGrid** - breaker/brownout/overload/power restored sounds
- **SignalOS** - SignalOS UI sounds
- **Nexus Protocol** - Nexus ambience/music
- **Blackbox Protocol** - Blackbox memory ambience
- **Stationfall** - Stationfall horror ambience

No hard crash occurs if any optional addon is absent.

---

## Known Limitations

- Unfinished events use `fallback/silent_placeholder.ogg`; they intentionally make no audible sound until custom Suno exports or a resource/audio pack provides final OGG replacements.
- Full crossfade blending between tracks is a basic stop/play implementation in this first pass.
- Ambience layers use simple loop instances; seamless crossfading can be enhanced later.
- Server-to-client audio events via packets are not yet implemented; use vanilla level sound methods or client API for now.

---

## Release Checklist

- [ ] Real `.ogg` replacements are present for any audio promised by the release
- [ ] Placeholder-backed events point to `fallback/silent_placeholder.ogg`, not duplicated physical OGG copies
- [ ] Bundled SoundCore OGG payload remains under the lean-mod budget enforced by `tools/validate_resources.py`
- [ ] `sounds.json` keys match registered `SoundEvent` IDs
- [ ] Lang entries cover all subtitles and command feedback
- [ ] Data-driven JSON profiles validate without parse errors
- [ ] No missing asset warnings remain in logs
- [ ] Music loops cleanly and volumes are normalized
- [ ] Tested in-game with `/echosoundcore play` and `/echosoundcore debug`

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echosoundcore.json`.
3. First action: run the documented command or trigger its in-world behavior.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echosoundcore.md`.

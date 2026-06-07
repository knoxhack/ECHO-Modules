# ECHO: SoundCore Asset Placeholders

This directory contains the shipped SoundCore audio assets and the final replacement paths for ECHO: SoundCore.

## Important

- Do **not** commit invalid or empty `.ogg` files. They will crash the game.
- All final music must be exported as `.ogg` and placed in the correct subfolder.
- File names must match exactly what is defined in `sounds.json`.
- Unfinished events should point at `fallback/silent_placeholder.ogg` in `sounds.json`; do not duplicate placeholder OGG files across the tree.
- Large soundtrack/stinger deliveries belong in final OGG replacements or an optional resource/audio pack, not in repeated core-mod placeholders.

## Final Replacement Paths

```
sounds/
  fallback/
    silent_placeholder.ogg
  music/
    menu/
      menu_ashfall_theme.ogg
      menu_nexus_theme.ogg
      menu_orbital_theme.ogg
      menu_blackbox_theme.ogg
    gameplay/
      gameplay_safe_base_01.ogg
      gameplay_exploration_01.ogg
      gameplay_night_01.ogg
      gameplay_underground_01.ogg
    biome/
      biome_wasteland_01.ogg
      biome_crash_zone_01.ogg
      biome_toxic_swamp_01.ogg
      biome_radiation_zone_01.ogg
      biome_cryogenic_ruins_01.ogg
      biome_ruined_city_01.ogg
      biome_industrial_ruins_01.ogg
      biome_nexus_scar_01.ogg
    faction/
      faction_radwarden_ambient_01.ogg
      faction_crashbreak_ambient_01.ogg
      faction_sporebound_ambient_01.ogg
      faction_radwarden_combat_01.ogg
      faction_crashbreak_combat_01.ogg
      faction_sporebound_combat_01.ogg
    combat/
      combat_light_01.ogg
      combat_heavy_01.ogg
      combat_nexus_01.ogg
      combat_faction_raid_01.ogg
      combat_siege_01.ogg
    boss/
      boss_warden_01.ogg
      boss_guardian_wasteland_01.ogg
      boss_guardian_toxic_01.ogg
      boss_guardian_radiation_01.ogg
      boss_guardian_cryo_01.ogg
      boss_guardian_industrial_01.ogg
      boss_guardian_city_01.ogg
      boss_guardian_nexus_01.ogg
      boss_corruption_bloom_01.ogg
      boss_severance_engine_01.ogg
      boss_mirror_command_01.ogg
      boss_station_mother_01.ogg
    chapter/
      chapter_ashfall_protocol_01.ogg
      chapter_orbital_remnants_01.ogg
      chapter_agriculture_reclamation_01.ogg
      chapter_stationfall_01.ogg
      chapter_nexus_protocol_01.ogg
      chapter_industrial_nexus_01.ogg
      chapter_logistics_network_01.ogg
      chapter_convoy_protocol_01.ogg
      chapter_armory_01.ogg
      chapter_blackbox_protocol_01.ogg
    terminal/
      terminal_command_bed_01.ogg
      terminal_nexus_corrupted_01.ogg
    nexus/
      nexus_core_ambience_01.ogg
      nexus_choice_room_01.ogg
      nexus_siege_01.ogg
    blackbox/
      blackbox_memory_fragment_01.ogg
      blackbox_false_signal_01.ogg
      blackbox_truth_engine_01.ogg
  stinger/
    stinger_mission_accept.ogg
    stinger_mission_update.ogg
    stinger_objective_complete.ogg
    stinger_mission_complete.ogg
    stinger_signal_detected.ogg
    stinger_guardian_located.ogg
    stinger_nexus_state_changed.ogg
    stinger_chapter_unlocked.ogg
    stinger_reward_available.ogg
    stinger_faction_radwarden_rep_up.ogg
    stinger_faction_crashbreak_rep_up.ogg
    stinger_faction_sporebound_rep_up.ogg
  ui/terminal/
    ui_terminal_open.ogg
    ui_terminal_close.ogg
    ui_terminal_tab.ogg
    ui_terminal_select.ogg
    ui_terminal_error.ogg
    ui_terminal_reward_claim.ogg
    ui_terminal_new_intel.ogg
    ui_terminal_warning.ogg
    ui_terminal_corrupt_tick.ogg
  ui/lens/
    ui_lens_scan_compact.ogg
    ui_lens_scan_expanded.ogg
    ui_lens_deep_scan_start.ogg
    ui_lens_deep_scan_loop.ogg
    ui_lens_deep_scan_complete.ogg
    ui_lens_scan_invalid.ogg
    ui_lens_hazard_detected.ogg
    ui_lens_machine_diagnostics.ogg
  ui/holomap/
    ui_holomap_open.ogg
    ui_holomap_close.ogg
    ui_holomap_waypoint_place.ogg
    ui_holomap_waypoint_remove.ogg
    ui_holomap_overlay_hazard.ogg
    ui_holomap_route_calculated.ogg
    ui_holomap_unknown.ogg
  ui/signaloos/
    ui_signaloos_boot.ogg
    ui_signaloos_login.ogg
    ui_signaloos_file_open.ogg
    ui_signaloos_file_save.ogg
    ui_signaloos_network_ping.ogg
    ui_signaloos_drive_insert.ogg
    ui_signaloos_drive_remove.ogg
    ui_signaloos_vault_unlock.ogg
    ui_signaloos_app_open.ogg
    ui_signaloos_app_close.ogg
  powergrid/
    powergrid_generator_hand_crank.ogg
    powergrid_generator_scrap_burner.ogg
    powergrid_battery_hum.ogg
    powergrid_cable_low_voltage.ogg
    powergrid_substation_loop.ogg
    powergrid_breaker_trip.ogg
    powergrid_brownout.ogg
    powergrid_overload_warning.ogg
    powergrid_overload_failure.ogg
    powergrid_power_restored.ogg
  drone/
    drone_boot.ogg
    drone_follow.ogg
    drone_scout_ping.ogg
    drone_target_mark.ogg
    drone_low_power.ogg
    drone_damaged.ogg
    drone_repaired.ogg
    drone_mode_switch.ogg
    drone_combat_suppression.ogg
    drone_return.ogg
  nexus/
    nexus_ambience_low_hum.ogg
    nexus_ambience_glitch_pulse.ogg
    nexus_ambience_memory_whisper.ogg
    nexus_ambience_reality_tear.ogg
    nexus_core_heartbeat.ogg
    nexus_choice_room.ogg
```

## Adding Custom Audio

1. Generate or export your audio as `.ogg`.
2. Normalize volume and ensure it loops cleanly if it is music or ambience.
3. Place the file in the matching folder with the exact file name from `sounds.json`.
4. The game will log a missing-sound warning during dev if the file is absent; this is expected until assets are added.

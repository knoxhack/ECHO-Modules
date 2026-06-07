# ScriptCore Schema

Every ScriptCore JSON definition requires:

```json
{
  "schema_version": 1,
  "id": "my_pack:definition_id",
  "type": "mission"
}
```

Common optional fields:
- `pack`
- `title`
- `description`
- `source`
- `tags`
- `unlock_conditions`
- `conditions`
- `actions`
- `metadata`

Supported `type` values:

```text
mission, archive_entry, lens_scan, holomap_layer, holomap_marker,
weather_event, faction, world_state, tutorial_hint, dialogue,
ending, recipe_unlock, loot_profile, generic
```

## Mission

Key fields:
- `route`
- `phase`
- `role`: `main`, `optional`, `hidden`, or `repeatable`
- `briefing`
- `objectives`
- `rewards`
- `prerequisites`
- `on_start`
- `on_complete`
- `on_fail`
- `terminal`, `lens`, `holomap`

Objective types:

```text
collect_item, craft_item, obtain_item, scan_block, scan_entity,
scan_item, visit_poi, enter_region, kill_entity, survive_weather,
interact_block, build_structure, complete_mission, set_world_state, custom
```

## Other Definitions

Archive entries use `category`, `subtitle`, `content`, related IDs, and `importance`.

Lens scans use `target`, `target_type`, `summary`, `details`, and `danger`.

HoloMap layers use `locked_by_default` and optional inline `markers`.

HoloMap markers use `x`, `y`, `z`, `dimension`, `icon`, `danger`, and `layer`.

Weather events use `duration_ticks`, `warning_seconds`, `effects`, `terminal_warning`, and `sound_stinger`.

Factions use `display_name`, `description`, `starting_reputation`, `ranks`, and `reputation_events`.

World states use `set_by` and `effects`.

Tutorial hints use `message`, `trigger_conditions`, `priority`, `once`, and `terminal_card`.

Dialogue uses `speaker`, `lines`, `choices`, `conditions`, and `actions`.

Endings use `priority`, `conditions`, `actions`, and `terminal_summary`.

Recipe unlocks use `recipe`, `unlock_conditions`, and `actions`.

Loot profiles use `table`, `entries`, and `unlock_conditions`.

## Conditions

Supported condition types include:

```text
always, never, all, any, not, mission_complete, mission_active,
mission_started, objective_complete, item_obtained, item_crafted,
item_count_at_least, block_scanned, entity_scanned, item_scanned,
poi_discovered, region_entered, weather_survived,
faction_reputation_at_least, faction_reputation_below, world_state_set,
dimension_entered, biome_entered, time_survived, player_level_at_least,
custom_metric_at_least, custom_metric_below, custom
```

## Actions

Supported action types include:

```text
noop, unlock_mission, start_mission, complete_mission, complete_objective,
unlock_lore, unlock_archive_entry, unlock_terminal_tab,
unlock_holomap_layer, add_holomap_marker, change_reputation,
trigger_weather, play_sound, show_tutorial_hint, give_item,
set_world_state, clear_world_state, spawn_poi, start_dialogue,
unlock_index_recipe, add_terminal_alert, set_custom_metric,
change_custom_metric, custom
```

## Validation

ScriptCore validates required fields, id format, duplicate IDs, unknown types, objective requirements, mission references, circular mission prerequisites, marker coordinate sanity, weather duration, faction rank ordering, archive content, Lens target/title, and ending conditions.

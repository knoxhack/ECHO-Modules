# Companion Drone Utility Tags

Other ECHO addons can extend the Ashfall companion drone without Java hooks by adding values to these tags:

- `echo:drone_scan_containers` for blocks treated as containers or caches.
- `echo:drone_scan_resources` for ores/resource nodes.
- `echo:drone_scan_hazards` for hazard blocks.
- `echo:drone_scan_objectives` for mission or route objective blocks.
- `echo:drone_ignore_blocks` for blocks the scanner should skip.
- `echo:drone_salvage_items` for dropped items salvage mode may collect.
- `echo:drone_ignore_items` for dropped items the drone must not collect.
- `echo:drone_upgrade_items` for items that represent future upgrade modules.
- `echo:drone_hostile_priority` for high-priority threat entity types.
- `echo:drone_ignore_entities` for entities the scanner should ignore.
- `echo:drone_scan_interest` for non-hostile entities worth pinging.

Use `replace: false` so datapacks and addons merge cleanly.

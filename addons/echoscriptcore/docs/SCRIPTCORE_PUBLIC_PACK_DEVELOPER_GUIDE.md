# Public Pack Developer Guide

ScriptCore lets you create your own scripted modpack campaign without making your pack an Ashfall fork.

## Pack Naming

Use one stable namespace for your pack:

```text
config/echo/scripts/my_pack/
my_pack:first_mission
my_pack:first_archive
my_pack:final_ending
```

Avoid using another pack's namespace unless you intentionally depend on that pack.

## Recommended Flow

Common patterns:
- A Lens scan unlocks lore.
- A mission reveals a HoloMap marker.
- A mission sets a world state.
- Faction reputation unlocks an optional route.
- An ending checks multiple world states.

Example:

```json
{
  "schema_version": 1,
  "pack": "my_pack",
  "id": "my_pack:restore_beacon",
  "type": "mission",
  "title": "Restore the Beacon",
  "objectives": [
    { "id": "parts", "type": "collect_item", "item": "minecraft:redstone", "count": 8 }
  ],
  "on_complete": [
    { "type": "set_world_state", "state": "my_pack:beacon_restored" },
    { "type": "add_holomap_marker", "marker": "my_pack:beacon_marker" }
  ]
}
```

## Shipping Scripts

Ship ScriptCore files in your modpack's config folder or copy them on first launch. Keep examples and optional content in separate folders, and use `metadata` for optional mod IDs until your pack actually requires those mods.

## Avoiding Hardcoded Pack IDs

Do not put Ashfall IDs in generic content. Use your own namespace, and place compatibility content in a clearly named optional pack folder such as `my_pack_ashfall_bridge`.

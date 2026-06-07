# ECHO Recovery Configuration

## Grave Settings

- `enable_graves` - Enable grave creation on death (default: true)
- `store_items` - Store inventory items (default: true)
- `store_armor` - Store armor (default: true)
- `store_offhand` - Store offhand item (default: true)
- `store_xp` - Store experience points (default: true)
- `store_curios` - Store Curios/Trinkets if installed (default: true)
- `keep_hotbar_order` - Preserve hotbar slot order (default: true)
- `max_graves_per_player` - Maximum graves per player (default: 10)
- `grave_expiration_minutes` - Grave expiration time, -1 = never (default: -1)
- `drop_overflow_items` - Drop items that don't fit on recovery (default: true)
- `delete_empty_graves` - Remove empty graves automatically (default: true)
- `create_grave_on_pvp` - Create graves in PvP deaths (default: true)
- `create_grave_on_void_death` - Create graves for void deaths (default: true)
- `create_grave_on_lava_death` - Create graves for lava deaths (default: true)

## Placement Settings

- `safe_placement` - Search for safe placement nearby (default: true)
- `safe_placement_radius` - Search radius (default: 8)
- `void_death_mode` - Where to place grave on void death:
  - `LAST_SAFE_POSITION`
  - `BED`
  - `WORLD_SPAWN`
  - `TEAM_SPAWN`
  - `DISABLED`
- `lava_death_safe_placement` - Use safe placement for lava deaths (default: true)
- `create_temporary_platform` - Create platform if needed (default: false)
- `fallback_to_spawn` - Fallback to world spawn (default: true)
- `fallback_to_bed` - Fallback to bed/respawn point (default: true)

## Protection Settings

- `owner_only` - Only owner can open (default: true)
- `team_access` - Allow team access (default: false)
- `public_access_after_minutes` - Public access after timer, -1 = never (default: -1)
- `admin_bypass` - Admins can bypass restrictions (default: true)
- `grave_theft` - Allow non-owners to steal (default: false)
- `allow_grave_breaking` - Allow breaking graves (default: false)
- `explosion_proof` - Graves immune to explosions (default: true)
- `fireproof` - Graves immune to fire (default: true)
- `wither_proof` - Graves immune to wither (default: true)
- `mob_grief_proof` - Graves immune to mob griefing (default: true)

## Item Settings

- `grave_key_enabled` - Give grave key on death (default: true)
- `grave_key_required` - Require key to open grave (default: false)
- `grave_key_consumed` - Consume key on use (default: false)
- `grave_key_craftable` - Grave key has recipe (default: true)
- `recovery_compass_enabled` - Enable recovery compass (default: true)
- `recovery_compass_craftable` - Compass has recipe (default: true)
- `recovery_compass_tracks_selected_grave` - Track selected grave (default: true)
- `recovery_compass_works_cross_dimension` - Cross-dimension tracking (default: false)

## History

- `enable_death_history` - Track death history (default: true)
- `max_death_history` - Max history entries (default: 25)

## Difficulty Presets

- `difficulty_preset`:
  - `FORGIVING` - Never expires, owner only, all items + XP
  - `VANILLA_PLUS` - Balanced defaults
  - `ADVENTURE` - Moderate restrictions
  - `RPG` - Themed for RPG packs
  - `HARDCORE` - Expires, limited count, public access after timer
  - `SKYBLOCK` - Void-safe caches
  - `HORROR` - Weaker markers, ominous sounds
  - `ASHFALL` - Field Recovery with hazards

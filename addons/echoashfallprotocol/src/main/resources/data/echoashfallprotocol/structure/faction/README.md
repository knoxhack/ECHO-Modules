# Faction Village Structures

This folder contains NBT structure files for Ashfall faction hubs.

## Structure Requirements

### Radwarden Field Compound (`radwarden_outpost/`)
- **Theme:** Military bunker, concrete walls, defensive positions
- **Key Blocks:** Weapon Rack (profession block), Supply Crate (profession block)
- **Building Types:**
  - `command_bunker.nbt` - Central town center with command table
  - `barracks.nbt` - Sleeping quarters for soldiers
  - `armory.nbt` - Weapon storage with Weapon Rack blocks
  - `guard_post.nbt` - Watch towers with Supply Crate
  - `supply_depot.nbt` - Storage with Supply Crate blocks
  - `street_straight.nbt`, `street_corner.nbt`, `street_cross.nbt` - Pathways
  - `wall_section.nbt`, `wall_corner.nbt` - Perimeter walls

### Crashbreak Salvage Yard (`crashbreak_salvage/`)
- **Theme:** Converted train station, market stalls, scrap metal
- **Key Blocks:** Trade Counter (profession block), Map Table (profession block)
- **Building Types:**
  - `market_plaza.nbt` - Central town center with trading posts
  - `warehouse.nbt` - Storage building with Trade Counter
  - `workshop.nbt` - Crafting area with Map Table
  - `living_quarters.nbt` - NPC housing
  - `street_plaza.nbt` - Open market areas

### Sporebound Sanctum (`sporebound_sanctum/`)
- **Theme:** Bio-dome, organic growth, glowing plants
- **Key Blocks:** Bio Processing Station (profession block), Spore Garden (profession block)
- **Building Types:**
  - `biodome_hub.nbt` - Central greenhouse structure
  - `processing_hut.nbt` - Bio Processing Station workshop
  - `living_pod.nbt` - Organic housing
  - `spore_garden.nbt` - Decorative garden with Spore Garden blocks
  - `overgrown_path.nbt` - Natural pathways

## Creating NBT Files

1. Build the structure in-game using creative mode
2. Use the `/structure` command to save:
   ```
   /structure save echoashfallprotocol:faction/radwarden_outpost/barracks <x> <y> <z> <x2> <y2> <z2> true
   ```
3. The NBT file will be saved in your `.minecraft/saves/<world>/generated/echoashfallprotocol/structure/faction/` folder
4. Copy it to this directory

## Jigsaw Markers

Place jigsaw blocks in your structures to connect pieces:
- **Target Pool:** `echoashfallprotocol:<faction>/houses`
- **Attachment Type:** `minecraft:building`
- **Turns into:** The profession block for this building type

## Entity Spawning

Place entity markers (armor stands) where you want faction NPCs to spawn:
- Use Naming tags to mark spawn points
- The structure processor will replace these with actual NPCs

## Profession Block Placement

Make sure to place the profession blocks in your NBT files:
- Radwarden: `echoashfallprotocol:weapon_rack`, `echoashfallprotocol:supply_crate`
- Crashbreak: `echoashfallprotocol:trade_counter`, `echoashfallprotocol:map_table`
- Sporebound: `echoashfallprotocol:bio_processing_station`, `echoashfallprotocol:spore_garden`

Villagers will automatically pathfind and claim these blocks to gain the corresponding profession.

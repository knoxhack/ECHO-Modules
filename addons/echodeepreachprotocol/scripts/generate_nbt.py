#!/usr/bin/env python3
"""Generate expanded structure NBT templates for ECHO: Deep Reach Protocol."""

import os
import nbtlib

NAMESPACE = "echodeepreachprotocol"
STRUCTURE_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src", "main", "resources", "data", NAMESPACE, "structure",
)
DATA_VERSION = 4790  # Minecraft 26.1.2


def make_structure(size, block_map):
    """Build a StructureTemplate NBT from a dict of (x,y,z) -> block_id or (block_id, nbt)."""
    palette = []
    palette_index = {}
    blocks = []
    sx, sy, sz = size

    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                val = block_map.get((x, y, z))
                if val is None:
                    continue
                if isinstance(val, tuple):
                    block_id, block_nbt = val
                else:
                    block_id, block_nbt = val, None

                if block_id not in palette_index:
                    palette_index[block_id] = len(palette)
                    palette.append(nbtlib.Compound({"Name": nbtlib.String(block_id)}))
                entry = nbtlib.Compound({
                    "pos": nbtlib.IntArray([x, y, z]),
                    "state": nbtlib.Int(palette_index[block_id]),
                })
                if block_nbt is not None:
                    entry["nbt"] = block_nbt
                blocks.append(entry)

    root = nbtlib.Compound({
        "DataVersion": nbtlib.Int(DATA_VERSION),
        "size": nbtlib.IntArray(list(size)),
        "palette": nbtlib.List[nbtlib.Compound](palette),
        "blocks": nbtlib.List[nbtlib.Compound](blocks),
        "entities": nbtlib.List[nbtlib.Compound]([]),
    })
    return nbtlib.File(root, gzipped=True)


def chest(loot_table):
    block_id = "minecraft:chest"
    nbt = nbtlib.Compound({
        "id": nbtlib.String("minecraft:chest"),
        "LootTable": nbtlib.String(loot_table),
    })
    return block_id, nbt


def collapsed_tunnel():
    sx, sy, sz = 9, 5, 7
    blocks = {}
    loot = f"{NAMESPACE}:chests/collapsed_tunnel"
    stone = f"{NAMESPACE}:abyssal_stone"

    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                edge = x == 0 or x == sx - 1 or z == 0 or z == sz - 1 or y == 0 or y == sy - 1
                if not edge:
                    continue
                # collapsed ceiling/floor with rubble
                if y == 0 or y == sy - 1:
                    if (x + z) % 3 == 0:
                        blocks[(x, y, z)] = "minecraft:cobblestone"
                    elif (x + z) % 5 == 0:
                        blocks[(x, y, z)] = "minecraft:gravel"
                    else:
                        blocks[(x, y, z)] = stone
                # side walls with breaches
                elif x == 0 or x == sx - 1:
                    if y == 2 and z in (2, 4):
                        blocks[(x, y, z)] = "minecraft:water"
                    elif (y + z) % 4 == 0:
                        blocks[(x, y, z)] = "minecraft:mossy_cobblestone"
                    else:
                        blocks[(x, y, z)] = stone
                elif z == 0 or z == sz - 1:
                    if (x + y) % 4 == 0:
                        blocks[(x, y, z)] = "minecraft:water"
                    else:
                        blocks[(x, y, z)] = stone

    # support beams
    for z in range(1, sz - 1):
        blocks[(2, 1, z)] = "minecraft:oak_planks"
        blocks[(sx - 3, 1, z)] = "minecraft:oak_planks"

    # flooded floor pockets
    blocks[(1, 1, 1)] = "minecraft:water"
    blocks[(3, 1, 3)] = "minecraft:water"
    blocks[(sx - 2, 1, sz - 2)] = "minecraft:water"

    # chest in a side alcove
    blocks[(sx - 2, 1, sz // 2)] = chest(loot)
    return make_structure((sx, sy, sz), blocks)


def geothermal_station():
    sx, sy, sz = 11, 6, 9
    blocks = {}
    loot = f"{NAMESPACE}:chests/geothermal_station"

    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                wall = x == 0 or x == sx - 1 or z == 0 or z == sz - 1 or y == 0 or y == sy - 1
                corner = (x in (0, sx - 1)) and (z in (0, sz - 1))
                if wall:
                    if corner:
                        blocks[(x, y, z)] = "minecraft:deepslate"
                    elif y in (1, 2) and (x in (2, sx - 3) or z in (2, sz - 3)):
                        blocks[(x, y, z)] = "minecraft:iron_bars"
                    else:
                        blocks[(x, y, z)] = "minecraft:polished_deepslate"

    cx, cz = sx // 2, sz // 2
    # central magma column
    for y in range(1, sy - 1):
        blocks[(cx, y, cz)] = "minecraft:magma_block" if y == 1 else f"{NAMESPACE}:thermal_vent"
    # thermal vent ring
    for dx, dz in [(-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, 1), (-1, 1), (1, -1)]:
        blocks[(cx + dx, 1, cz + dz)] = f"{NAMESPACE}:thermal_vent"

    # machinery platforms
    for x in (2, sx - 3):
        for z in (2, sz - 3):
            blocks[(x, 1, z)] = "minecraft:polished_deepslate"

    # chest
    blocks[(sx - 2, 1, 1)] = chest(loot)
    return make_structure((sx, sy, sz), blocks)


def lattice_archive():
    sx, sy, sz = 13, 7, 13
    blocks = {}
    loot = f"{NAMESPACE}:chests/lattice_archive"

    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                wall = x == 0 or x == sx - 1 or z == 0 or z == sz - 1 or y == 0 or y == sy - 1
                if not wall:
                    continue
                if y == 0 or y == sy - 1:
                    blocks[(x, y, z)] = "minecraft:polished_deepslate"
                elif (x + z) % 4 == 0:
                    blocks[(x, y, z)] = f"{NAMESPACE}:lattice_crystal"
                else:
                    blocks[(x, y, z)] = "minecraft:bookshelf"

    # corner lattice spires
    for y in range(1, sy - 1):
        blocks[(1, y, 1)] = f"{NAMESPACE}:lattice_crystal"
        blocks[(sx - 2, y, 1)] = f"{NAMESPACE}:lattice_crystal"
        blocks[(1, y, sz - 2)] = f"{NAMESPACE}:lattice_crystal"
        blocks[(sx - 2, y, sz - 2)] = f"{NAMESPACE}:lattice_crystal"

    # central reading pedestal
    cx, cz = sx // 2, sz // 2
    blocks[(cx, 1, cz)] = "minecraft:polished_deepslate"
    blocks[(cx, 2, cz)] = f"{NAMESPACE}:lattice_crystal"

    # inner ring of bookshelves
    for z in range(3, sz - 3):
        for x in range(3, sx - 3):
            if x == 3 or x == sx - 4 or z == 3 or z == sz - 4:
                if (x + z) % 2 == 0:
                    blocks[(x, 1, z)] = "minecraft:bookshelf"

    # chest
    blocks[(1, 1, cz)] = chest(loot)
    return make_structure((sx, sy, sz), blocks)


def abyssal_temple():
    sx, sy, sz = 15, 8, 15
    blocks = {}
    loot = f"{NAMESPACE}:chests/abyssal_temple"
    stone = f"{NAMESPACE}:abyssal_stone"

    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                wall = x == 0 or x == sx - 1 or z == 0 or z == sz - 1 or y == 0 or y == sy - 1
                pillar = (x in (2, sx - 3)) and (z in (2, sz - 3))
                if wall:
                    if pillar:
                        blocks[(x, y, z)] = f"{NAMESPACE}:lattice_crystal"
                    else:
                        blocks[(x, y, z)] = stone

    # floor lattice pattern
    for z in range(2, sz - 2):
        for x in range(2, sx - 2):
            if (x + z) % 4 == 0:
                blocks[(x, 1, z)] = f"{NAMESPACE}:lattice_crystal"

    # central altar
    cx, cz = sx // 2, sz // 2
    for dx in (-2, -1, 0, 1, 2):
        for dz in (-2, -1, 0, 1, 2):
            blocks[(cx + dx, 1, cz + dz)] = stone
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            blocks[(cx + dx, 2, cz + dz)] = stone
    blocks[(cx, 3, cz)] = "minecraft:gold_block"

    # side offering chests
    blocks[(3, 1, cz)] = chest(loot)
    blocks[(sx - 4, 1, cz)] = chest(loot)

    # four corner altars
    for px, pz in [(3, 3), (sx - 4, 3), (3, sz - 4), (sx - 4, sz - 4)]:
        blocks[(px, 1, pz)] = f"{NAMESPACE}:lattice_crystal"
        blocks[(px, 2, pz)] = f"{NAMESPACE}:lattice_crystal"
    return make_structure((sx, sy, sz), blocks)


def main():
    os.makedirs(STRUCTURE_DIR, exist_ok=True)
    structures = {
        "collapsed_tunnel": collapsed_tunnel(),
        "geothermal_station": geothermal_station(),
        "lattice_archive": lattice_archive(),
        "abyssal_temple": abyssal_temple(),
    }
    for name, nbt_file in structures.items():
        path = os.path.join(STRUCTURE_DIR, f"{name}.nbt")
        nbt_file.save(path)
        size = os.path.getsize(path)
        print(f"Wrote {path} ({size} bytes)")


if __name__ == "__main__":
    main()

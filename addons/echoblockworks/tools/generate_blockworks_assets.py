from __future__ import annotations

import json
import gzip
import math
import os
import struct
import zlib
from pathlib import Path

MODID = "echoblockworks"
ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources"
# v3 treats textures as committed generated art. Set this to 1 only when changing
# data files without refreshing PNGs.
SKIP_TEXTURES = os.environ.get("ECHO_BLOCKWORKS_SKIP_TEXTURES") == "1"

FAMILIES = [
    ("reinforced_metal", "Reinforced Metal", "industrial", True,
     ["panel", "riveted", "grate", "frame", "cracked", "hazard_stripe", "lit_panel", "pillar"]),
    ("rusted_metal", "Rusted Metal", "ruined", True,
     ["panel", "riveted", "grate", "pipe_wall", "cracked", "hazard_stripe", "dark_plate", "pillar"]),
    ("ashstone", "Ashstone", "ruined", True,
     ["raw", "brick", "cracked_brick", "chiseled", "tile", "pillar", "debris", "smooth"]),
    ("charred_concrete", "Charred Concrete", "ruined", True,
     ["smooth", "cracked", "tile", "rebar", "road_plate", "warning_stripe", "scorched", "broken"]),
    ("terminal_panel", "Terminal Panel", "terminal", False,
     ["wall_panel", "screen", "trim", "cyan_lit", "dark_panel", "data_panel", "warning_panel", "server_rack"]),
    ("echo_circuit", "ECHO Circuit", "echo_tech", False,
     ["circuit_panel", "data_conduit", "service_node", "matrix", "glowing_circuit", "offline_circuit", "warning_circuit", "encrypted_circuit"]),
    ("orbital_hull", "Orbital Hull", "orbital", True,
     ["hull_panel", "thermal_tile", "airlock_frame", "docking_trim", "lit_strip", "damaged_hull", "white_hull", "black_hull"]),
    ("nexus_crystal", "Nexus Crystal", "nexus", False,
     ["nexus_glass", "nexus_frame", "glowing_crystal", "cracked_crystal", "energy_conduit", "anomaly_tile", "pillar", "rift_panel"]),
    ("blackbox_vault", "Blackbox Vault", "blackbox", True,
     ["vault_wall", "locked_panel", "archive_panel", "memory_glass", "warning_light", "dark_alloy", "secure_frame", "cracked_vault"]),
    ("reclamation_glass", "Reclamation Glass", "reclamation", False,
     ["clear_glass", "framed_glass", "green_glass", "overgrown_glass", "hydroponic_panel", "lit_grow_panel", "dome_panel", "irrigation_pipe"]),
]

DETAILS = [
    ("echo_strip_light", "ECHO Strip Light", "ceiling", "echo_tech"),
    ("warning_beacon", "Warning Beacon", "full", "hazard"),
    ("flickering_warning_light", "Flickering Warning Light", "wall", "hazard"),
    ("data_wall", "Data Wall", "directional", "terminal"),
    ("broken_monitor", "Broken Monitor", "wall", "ruined"),
    ("server_cabinet", "Server Cabinet", "directional", "terminal"),
    ("cable_bundle", "Cable Bundle", "wall", "industrial"),
    ("wall_pipe", "Wall Pipe", "wall", "industrial"),
    ("ceiling_pipe", "Ceiling Pipe", "ceiling", "industrial"),
    ("steam_vent", "Steam Vent", "low", "industrial"),
    ("sparking_cable_panel", "Sparking Cable Panel", "wall", "hazard"),
    ("rubble_pile", "Rubble Pile", "low", "ruined"),
    ("scattered_debris", "Scattered Debris", "low", "ruined"),
    ("hanging_wire", "Hanging Wire", "ceiling", "ruined"),
    ("hologram_floor_projector", "Hologram Floor Projector", "low", "echo_tech"),
    ("signal_dish_decorative", "Signal Dish Decorative", "directional", "orbital"),
]

BASE_RECIPES = {
    "reinforced_metal_panel": (["ICI", "RSR", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone", "S": "minecraft:smooth_stone"}, 4),
    "rusted_metal_panel": (["NCN", "DID", "NCN"], {"N": "minecraft:iron_nugget", "C": "minecraft:copper_ingot", "D": "minecraft:brown_dye", "I": "minecraft:iron_ingot"}, 4),
    "ashstone_raw": (["SCS", "CCC", "SCS"], {"S": "minecraft:stone", "C": "minecraft:charcoal"}, 8),
    "charred_concrete_smooth": (["CBC", "BGB", "CBC"], {"C": "minecraft:gray_concrete", "B": "minecraft:black_dye", "G": "minecraft:charcoal"}, 8),
    "terminal_panel_wall_panel": (["RGR", "CRC", "RGR"], {"R": f"{MODID}:reinforced_metal_panel", "G": "minecraft:glass_pane", "C": "minecraft:redstone"}, 4),
    "echo_circuit_circuit_panel": (["CRC", "RLR", "CRC"], {"C": "minecraft:copper_ingot", "R": "minecraft:redstone", "L": "minecraft:lapis_lazuli"}, 4),
    "orbital_hull_hull_panel": (["IQI", "CRC", "IQI"], {"I": "minecraft:iron_ingot", "Q": "minecraft:quartz", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, 4),
    "nexus_crystal_nexus_glass": (["ALA", "GCG", "ALA"], {"A": "minecraft:amethyst_shard", "L": "minecraft:lapis_lazuli", "G": "minecraft:glass", "C": "minecraft:cyan_dye"}, 4),
    "blackbox_vault_vault_wall": (["OIO", "IRI", "OIO"], {"O": "minecraft:obsidian", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, 4),
    "reclamation_glass_clear_glass": (["GCG", "DLD", "GCG"], {"G": "minecraft:glass", "C": "minecraft:copper_ingot", "D": "minecraft:green_dye", "L": "minecraft:glowstone_dust"}, 6),
}

PALETTES = {
    "ashfall_ruined_city": ["ashstone_raw", "ashstone_brick", "ashstone_cracked_brick", "charred_concrete_cracked", "charred_concrete_broken", "rusted_metal_panel", "rubble_pile", "scattered_debris"],
    "crash_zone": ["charred_concrete_scorched", "charred_concrete_rebar", "rusted_metal_cracked", "orbital_hull_damaged_hull", "sparking_cable_panel", "steam_vent"],
    "terminal_bunker": ["terminal_panel_wall_panel", "terminal_panel_screen", "terminal_panel_data_panel", "echo_circuit_matrix", "data_wall", "server_cabinet"],
    "orbital_station": ["orbital_hull_hull_panel", "orbital_hull_thermal_tile", "orbital_hull_airlock_frame", "orbital_hull_lit_strip", "ceiling_pipe", "signal_dish_decorative"],
    "nexus_gate": ["nexus_crystal_nexus_glass", "nexus_crystal_glowing_crystal", "nexus_crystal_energy_conduit", "nexus_crystal_rift_panel", "echo_circuit_encrypted_circuit"],
    "blackbox_vault": ["blackbox_vault_vault_wall", "blackbox_vault_locked_panel", "blackbox_vault_archive_panel", "blackbox_vault_memory_glass", "blackbox_vault_warning_light"],
    "reclamation_dome": ["reclamation_glass_clear_glass", "reclamation_glass_framed_glass", "reclamation_glass_green_glass", "reclamation_glass_overgrown_glass", "reclamation_glass_lit_grow_panel", "hologram_floor_projector"],
    "convoy_depot": ["charred_concrete_road_plate", "charred_concrete_warning_stripe", "reinforced_metal_frame", "rusted_metal_pipe_wall", "warning_beacon", "wall_pipe"],
}

PALETTE_KITS = [
    {
        "id": "ashfall_ruined_city",
        "display_name": "Ashfall Ruined City",
        "description": "Ashy stone, scorched concrete, rusted salvage, and ground debris for collapsed streets.",
        "recommended_usage": "Ruined Ashfall streets, survivor bases, and old civic blocks.",
        "theme": "ruined",
        "family_ids": ["ashstone", "charred_concrete", "rusted_metal"],
        "featured_block_ids": ["ashstone_brick", "ashstone_cracked_brick", "charred_concrete_cracked", "charred_concrete_broken"],
        "accent_block_ids": ["rusted_metal_panel", "rusted_metal_cracked", "rubble_pile", "scattered_debris"],
        "worldgen_site_id": "ashfall_street_ruin",
    },
    {
        "id": "crash_zone",
        "display_name": "Crash Zone",
        "description": "Scorched concrete, exposed rebar, damaged hull, sparks, vents, and debris.",
        "recommended_usage": "Impact scars, wreck interiors, drop zones, and damaged landing paths.",
        "theme": "ruined",
        "family_ids": ["charred_concrete", "rusted_metal", "orbital_hull"],
        "featured_block_ids": ["charred_concrete_scorched", "charred_concrete_rebar", "rusted_metal_cracked", "orbital_hull_damaged_hull"],
        "accent_block_ids": ["sparking_cable_panel", "steam_vent", "rubble_pile", "scattered_debris"],
        "worldgen_site_id": "crash_zone_fragment",
    },
    {
        "id": "terminal_bunker",
        "display_name": "Terminal Bunker",
        "description": "Terminal screens, ECHO circuitry, server cabinets, data walls, and cyan-lit command surfaces.",
        "recommended_usage": "Command bunkers, restored control rooms, diagnostics labs, and mission terminals.",
        "theme": "terminal",
        "family_ids": ["terminal_panel", "echo_circuit", "reinforced_metal"],
        "featured_block_ids": ["terminal_panel_wall_panel", "terminal_panel_screen", "terminal_panel_data_panel", "echo_circuit_matrix"],
        "accent_block_ids": ["data_wall", "server_cabinet", "echo_strip_light", "reinforced_metal_frame"],
        "worldgen_site_id": "terminal_bunker_alcove",
    },
    {
        "id": "orbital_station",
        "display_name": "Orbital Station",
        "description": "Station hull plating, thermal tiles, airlock frames, white/black hull contrast, and signal hardware.",
        "recommended_usage": "Orbital interiors, airlock remnants, station corridors, and launch infrastructure.",
        "theme": "orbital",
        "family_ids": ["orbital_hull", "reinforced_metal"],
        "featured_block_ids": ["orbital_hull_hull_panel", "orbital_hull_thermal_tile", "orbital_hull_airlock_frame", "orbital_hull_lit_strip"],
        "accent_block_ids": ["orbital_hull_white_hull", "orbital_hull_black_hull", "ceiling_pipe", "signal_dish_decorative"],
        "worldgen_site_id": "orbital_airlock_remnant",
    },
    {
        "id": "nexus_gate",
        "display_name": "Nexus Gate",
        "description": "Nexus glass, glowing crystal, rift panels, encrypted circuits, and anomaly accents.",
        "recommended_usage": "Nexus chambers, gate shards, anomaly containment rooms, and reality-breach set pieces.",
        "theme": "nexus",
        "family_ids": ["nexus_crystal", "echo_circuit"],
        "featured_block_ids": ["nexus_crystal_nexus_glass", "nexus_crystal_glowing_crystal", "nexus_crystal_energy_conduit", "nexus_crystal_rift_panel"],
        "accent_block_ids": ["nexus_crystal_anomaly_tile", "echo_circuit_encrypted_circuit", "hologram_floor_projector"],
        "worldgen_site_id": "nexus_gate_shard",
    },
    {
        "id": "blackbox_vault",
        "display_name": "Blackbox Vault",
        "description": "Dark archive alloy, locked panels, memory glass, secure frames, and orange warning light.",
        "recommended_usage": "Blackbox archives, breached vaults, sealed data rooms, and high-security interiors.",
        "theme": "blackbox",
        "family_ids": ["blackbox_vault", "echo_circuit"],
        "featured_block_ids": ["blackbox_vault_vault_wall", "blackbox_vault_locked_panel", "blackbox_vault_archive_panel", "blackbox_vault_memory_glass"],
        "accent_block_ids": ["blackbox_vault_warning_light", "blackbox_vault_secure_frame", "echo_circuit_encrypted_circuit"],
        "worldgen_site_id": "blackbox_vault_breach",
    },
    {
        "id": "reclamation_dome",
        "display_name": "Reclamation Dome",
        "description": "Transparent greenhouse glass, overgrowth, hydroponic panels, grow lights, and irrigation detail.",
        "recommended_usage": "Agriculture domes, reclamation labs, greenhouse ruins, and restored food-route spaces.",
        "theme": "reclamation",
        "family_ids": ["reclamation_glass", "echo_circuit"],
        "featured_block_ids": ["reclamation_glass_clear_glass", "reclamation_glass_framed_glass", "reclamation_glass_green_glass", "reclamation_glass_overgrown_glass"],
        "accent_block_ids": ["reclamation_glass_lit_grow_panel", "reclamation_glass_hydroponic_panel", "reclamation_glass_irrigation_pipe", "hologram_floor_projector"],
        "worldgen_site_id": "reclamation_dome_remnant",
    },
    {
        "id": "convoy_depot",
        "display_name": "Convoy Depot",
        "description": "Road plates, warning stripes, frames, rusted pipe walls, beacons, and wall pipes.",
        "recommended_usage": "Convoy depots, roadside pullouts, repair pads, checkpoints, and loading bays.",
        "theme": "convoy",
        "family_ids": ["charred_concrete", "reinforced_metal", "rusted_metal"],
        "featured_block_ids": ["charred_concrete_road_plate", "charred_concrete_warning_stripe", "reinforced_metal_frame", "rusted_metal_pipe_wall"],
        "accent_block_ids": ["warning_beacon", "wall_pipe", "signal_dish_decorative"],
        "worldgen_site_id": "convoy_depot_pullout",
    },
    {
        "id": "industrial_factory",
        "display_name": "Industrial Factory",
        "description": "Reinforced metal, rusted plates, hazard markings, grates, pipes, and steam vents.",
        "recommended_usage": "Factories, machine halls, depot interiors, and future MultiblockCore casing sets.",
        "theme": "industrial",
        "family_ids": ["reinforced_metal", "rusted_metal", "charred_concrete"],
        "featured_block_ids": ["reinforced_metal_panel", "reinforced_metal_riveted", "reinforced_metal_grate", "reinforced_metal_hazard_stripe"],
        "accent_block_ids": ["rusted_metal_pipe_wall", "rusted_metal_dark_plate", "ceiling_pipe", "steam_vent"],
    },
    {
        "id": "starter_base",
        "display_name": "Starter Base",
        "description": "Cheap, sturdy starter palette with Ashstone, smooth charred concrete, rusted panels, and simple details.",
        "recommended_usage": "Early player bases, field shelters, safe rooms, and first-night Ashfall workshops.",
        "theme": "ruined",
        "family_ids": ["ashstone", "charred_concrete", "rusted_metal", "reinforced_metal"],
        "featured_block_ids": ["ashstone_raw", "ashstone_smooth", "charred_concrete_smooth", "rusted_metal_panel"],
        "accent_block_ids": ["reinforced_metal_panel", "rubble_pile", "wall_pipe", "echo_strip_light"],
    },
]

WORLDGEN_SITES = [
    ("ashfall_street_ruin", "Ashfall Street Ruin", "ashfall_ruined_city", "showcase/ashfall_street_ruin", 2),
    ("crash_zone_fragment", "Crash Zone Fragment", "crash_zone", "showcase/crash_zone_fragment", 2),
    ("terminal_bunker_alcove", "Terminal Bunker Alcove", "terminal_bunker", "showcase/terminal_bunker_alcove", 1),
    ("orbital_airlock_remnant", "Orbital Airlock Remnant", "orbital_station", "showcase/orbital_airlock_remnant", 1),
    ("nexus_gate_shard", "Nexus Gate Shard", "nexus_gate", "showcase/nexus_gate_shard", 1),
    ("blackbox_vault_breach", "Blackbox Vault Breach", "blackbox_vault", "showcase/blackbox_vault_breach", 1),
    ("reclamation_dome_remnant", "Reclamation Dome Remnant", "reclamation_dome", "showcase/reclamation_dome_remnant", 1),
    ("convoy_depot_pullout", "Convoy Depot Pullout", "convoy_depot", "showcase/convoy_depot_pullout", 1),
]

ANIMATED_TEXTURES = {
    "flickering_warning_light": 5,
    "steam_vent": 6,
    "sparking_cable_panel": 4,
    "hologram_floor_projector": 5,
    "reinforced_metal_lit_panel": 4,
    "terminal_panel_screen": 4,
    "terminal_panel_cyan_lit": 4,
    "echo_circuit_glowing_circuit": 4,
    "orbital_hull_lit_strip": 4,
    "blackbox_vault_warning_light": 4,
    "reclamation_glass_lit_grow_panel": 4,
    "nexus_crystal_rift_panel": 4,
}

LIT_VARIANTS = {
    "lit_panel",
    "screen",
    "cyan_lit",
    "data_panel",
    "service_node",
    "glowing_circuit",
    "lit_strip",
    "glowing_crystal",
    "energy_conduit",
    "rift_panel",
    "warning_light",
    "lit_grow_panel",
}

THEME_BASE = {
    "industrial": (54, 61, 64, 255),
    "ruined": (74, 70, 64, 255),
    "terminal": (22, 36, 44, 255),
    "echo_tech": (18, 42, 48, 255),
    "orbital": (94, 101, 105, 255),
    "nexus": (68, 45, 108, 210),
    "blackbox": (23, 24, 27, 255),
    "reclamation": (42, 82, 57, 170),
    "hazard": (76, 54, 38, 255),
}

ACCENT = {
    "industrial": (89, 232, 255, 255),
    "ruined": (175, 98, 44, 255),
    "terminal": (64, 218, 255, 255),
    "echo_tech": (88, 231, 255, 255),
    "orbital": (210, 224, 226, 255),
    "nexus": (182, 100, 255, 230),
    "blackbox": (255, 140, 52, 255),
    "reclamation": (112, 224, 118, 190),
    "hazard": (255, 202, 55, 255),
}


def j(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def png(path: Path, pixels: list[list[tuple[int, int, int, int]]]):
    height = len(pixels)
    width = len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in pixels)

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    path.parent.mkdir(parents=True, exist_ok=True)
    payload = b"\x89PNG\r\n\x1a\n"
    payload += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    payload += chunk(b"IDAT", zlib.compress(raw, 9))
    payload += chunk(b"IEND", b"")
    path.write_bytes(payload)


def clamp(value):
    return max(0, min(255, int(value)))


def mix(color, delta):
    r, g, b, a = color
    return (clamp(r + delta), clamp(g + delta), clamp(b + delta), a)


def full_id(family: str, variant: str) -> str:
    return f"{family}_{variant}"


def structural_ids(block_id: str):
    return [f"{block_id}_slab", f"{block_id}_stairs", f"{block_id}_wall"]


def title(s: str) -> str:
    special = {"echo": "ECHO", "cyan": "Cyan"}
    return " ".join(special.get(part, part.capitalize()) for part in s.split("_"))


def display_name(family_id: str, family_name: str, variant: str, shape: str = "") -> str:
    variant_name = title(variant)
    if family_id == "echo_circuit":
        base = f"ECHO {variant_name}" if variant_name.startswith("Circuit") or variant_name.endswith("Circuit") else f"ECHO Circuit {variant_name}"
    elif family_id == "terminal_panel":
        base = "Terminal Screen Panel" if variant == "screen" else f"Terminal {variant_name}"
    elif family_id == "nexus_crystal":
        base = f"Nexus {variant_name.replace('Nexus ', '')}"
    elif family_id == "blackbox_vault":
        base = f"Blackbox {variant_name}"
    elif family_id == "orbital_hull":
        base = f"Orbital {variant_name}"
    elif family_id == "reclamation_glass":
        base = f"Reclamation {variant_name}"
    else:
        base = f"{family_name} {variant_name}"
    if not shape:
        return base
    if shape == "wall" and base.endswith(" Wall"):
        return f"{base} Segment"
    return f"{base} {title(shape)}"


def all_full_blocks():
    return [full_id(f, variant) for f, _, _, _, variants in FAMILIES for variant in variants]


def all_blocks():
    ids = []
    for family, _, _, structural, variants in FAMILIES:
        for variant in variants:
            block_id = full_id(family, variant)
            ids.append(block_id)
            if structural:
                ids.extend(structural_ids(block_id))
    ids.extend(detail[0] for detail in DETAILS)
    ids.append("echo_blockworks_table")
    return ids


def family_blocks(family_id: str, shapes=True):
    out = []
    family = next(f for f in FAMILIES if f[0] == family_id)
    for variant in family[4]:
        block_id = full_id(family_id, variant)
        out.append(block_id)
        if shapes and family[3]:
            out.extend(structural_ids(block_id))
    return out


def block_ref(block_id: str) -> str:
    return f"{MODID}:{block_id}"


def weighted_model_eligible(block_id: str, variant: str) -> bool:
    if block_id in ANIMATED_TEXTURES or variant in LIT_VARIANTS:
        return False
    return not any(part in variant for part in ("glass", "crystal", "dome"))


def write_texture(block_id: str, theme: str, variant: str, detail_kind: str | None = None):
    base = THEME_BASE[theme]
    accent = ACCENT[theme]
    pixels = []
    seed = sum(ord(c) for c in block_id)
    for y in range(16):
        row = []
        for x in range(16):
            noise = ((x * 13 + y * 17 + seed) % 7) - 3
            color = mix(base, noise * 5)
            if x in (0, 15) or y in (0, 15):
                color = mix(base, -22)
            row.append(color)
        pixels.append(row)

    def line(x0, y0, x1, y1, color):
        dx = abs(x1 - x0)
        dy = -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        x, y = x0, y0
        while True:
            if 0 <= x < 16 and 0 <= y < 16:
                pixels[y][x] = color
            if x == x1 and y == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x += sx
            if e2 <= dx:
                err += dx
                y += sy

    if "riveted" in variant or "vault" in variant or "frame" in variant:
        for pt in [(3, 3), (12, 3), (3, 12), (12, 12)]:
            pixels[pt[1]][pt[0]] = accent
            pixels[pt[1]][pt[0] + (1 if pt[0] < 8 else -1)] = mix(accent, -35)
    if "grate" in variant:
        for i in range(2, 16, 4):
            for y in range(16):
                pixels[y][i] = mix(base, -45)
    if "stripe" in variant or "warning" in variant or theme == "hazard":
        for d in range(-8, 24, 6):
            line(max(0, d), max(0, -d), min(15, d + 15), min(15, 15 - d), accent)
    if "cracked" in variant or "broken" in variant or "scorched" in variant or "debris" in variant:
        line(4, 1, 6, 7, mix(base, -70))
        line(6, 7, 3, 13, mix(base, -60))
        line(7, 7, 12, 10, mix(base, -65))
    if "brick" in variant:
        for y in (4, 8, 12):
            for x in range(16):
                pixels[y][x] = mix(base, -35)
        for x in (5, 11):
            for y in range(16):
                if y % 8 < 4:
                    pixels[y][x] = mix(base, -35)
    if "tile" in variant or "panel" in variant or "hull" in variant:
        for i in (7, 8):
            for x in range(16):
                pixels[i][x] = mix(base, -30)
                pixels[x][i] = mix(base, -30)
        for i in (1, 14):
            for x in range(2, 14):
                pixels[i][x] = mix(base, 18)
                pixels[x][i] = mix(base, 12)
        for x in (3, 12):
            pixels[3][x] = mix(accent, -45)
            pixels[12][x] = mix(accent, -55)
    if "circuit" in variant or "data" in variant or "matrix" in variant or theme == "echo_tech":
        line(2, 5, 13, 5, accent)
        line(6, 2, 6, 13, accent)
        for pt in [(3, 5), (6, 9), (11, 5), (6, 3)]:
            pixels[pt[1]][pt[0]] = mix(accent, 35)
    if "lit" in variant or "glowing" in variant or "screen" in variant or "rift" in variant or "light" in variant:
        for y in range(5, 11):
            for x in range(4, 12):
                if (x + y + seed) % 2 == 0:
                    pixels[y][x] = accent
        for y in range(6, 10):
            for x in range(5, 11):
                if (x + y) % 2 == 0:
                    pixels[y][x] = mix(accent, 42)
    if "glass" in variant or "crystal" in variant or "dome" in variant:
        for y in range(16):
            for x in range(16):
                r, g, b, a = pixels[y][x]
                pixels[y][x] = (r, g, b, min(a, 170))
        line(2, 13, 13, 2, (255, 255, 255, 120))
        for x in range(1, 15):
            pixels[1][x] = mix(accent, -20)
            pixels[14][x] = mix(accent, -35)
        for y in range(1, 15):
            pixels[y][1] = mix(accent, -30)
            pixels[y][14] = mix(accent, -45)
    if "rusted" in block_id or theme == "ruined":
        for i in range(26):
            x = (seed + i * 5) % 16
            y = (seed // 3 + i * 7) % 16
            pixels[y][x] = (150 + (i % 3) * 18, 73 + (i % 4) * 8, 30, pixels[y][x][3])
            if i % 5 == 0 and x + 1 < 16:
                pixels[y][x + 1] = (116, 58, 32, pixels[y][x][3])
    if "overgrown" in variant or theme == "reclamation":
        for i in range(0, 16, 3):
            line(i, 15, min(15, i + 4), 9 + ((seed + i) % 4), (72, 170, 83, min(220, base[3] + 30)))
    if "trim" in variant or "frame" in variant or "airlock" in variant or "secure" in variant:
        for x in range(16):
            pixels[2][x] = mix(accent, -50)
            pixels[13][x] = mix(accent, -60)
        for y in range(16):
            pixels[y][2] = mix(accent, -55)
            pixels[y][13] = mix(accent, -65)
    if "blackbox" in block_id or theme == "blackbox":
        line(1, 14, 14, 14, (255, 132, 42, 255))
        line(14, 1, 14, 14, (255, 132, 42, 255))
    if "nexus" in block_id or theme == "nexus":
        line(1, 8, 14, 3, (93, 230, 255, 210))
        line(2, 12, 12, 2, (190, 82, 255, 220))
    if "scorched" in variant or "charred" in block_id:
        for i in range(14):
            cx = (seed + i * 11) % 16
            cy = (seed + i * 13) % 16
            for yy in range(max(0, cy - 1), min(16, cy + 2)):
                for xx in range(max(0, cx - 1), min(16, cx + 2)):
                    pixels[yy][xx] = mix(pixels[yy][xx], -45)
    if "cracked" in variant or "broken" in variant:
        line(11, 2, 9, 6, mix(base, -75))
        line(9, 6, 13, 14, mix(base, -65))
    if detail_kind == "low":
        for y in range(0, 9):
            for x in range(16):
                pixels[y][x] = (0, 0, 0, 0)
    if detail_kind == "wall":
        for y in range(3, 13):
            for x in range(2, 14):
                if x in (2, 13) or y in (3, 12):
                    pixels[y][x] = accent
    if detail_kind == "ceiling":
        for y in (6, 7, 8, 9):
            for x in range(16):
                pixels[y][x] = accent if "light" in block_id else mix(base, -35)
    if detail_kind == "directional":
        for y in range(4, 12):
            for x in range(4, 12):
                pixels[y][x] = accent if (x + y) % 3 == 0 else mix(base, 15)

    if block_id in ANIMATED_TEXTURES:
        frames = []
        for frame in range(ANIMATED_TEXTURES[block_id]):
            delta = [0, 18, -10, 28, -18, 10][frame % 6]
            frame_pixels = []
            for y, row in enumerate(pixels):
                out_row = []
                for x, color in enumerate(row):
                    if color[3] > 0 and ((x + y + frame + seed) % 3 == 0 or "steam" in block_id or "hologram" in block_id):
                        out_row.append(mix(color, delta))
                    else:
                        out_row.append(color)
                frame_pixels.append(out_row)
            frames.extend(frame_pixels)
        png(RES / "assets" / MODID / "textures" / "block" / f"{block_id}.png", frames)
        j(RES / "assets" / MODID / "textures" / "block" / f"{block_id}.png.mcmeta",
          {"animation": {"frametime": 4, "interpolate": True}})
    else:
        png(RES / "assets" / MODID / "textures" / "block" / f"{block_id}.png", pixels)


def write_placeholder_texture(block_id: str, theme: str, variant: str, detail_kind: str | None = None):
    if not SKIP_TEXTURES:
        write_texture(block_id, theme, variant, detail_kind)


def write_item_texture():
    pixels = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    metal = (92, 101, 106, 255)
    glow = (88, 231, 255, 255)
    for i in range(2, 14):
        pixels[i][15 - i] = metal
        if 15 - i - 1 >= 0:
            pixels[i][15 - i - 1] = mix(metal, -35)
    for y in range(3, 8):
        for x in range(9, 14):
            if abs((x - 11) + (y - 5)) < 4:
                pixels[y][x] = glow
    for y in range(10, 15):
        for x in range(1, 5):
            pixels[y][x] = mix(metal, -20)
    png(RES / "assets" / MODID / "textures" / "item" / "echo_pattern_cutter.png", pixels)


def write_assets():
    lang = {
        "itemGroup.echoblockworks.blockworks": "ECHO Blockworks",
        "block.echoblockworks.echo_blockworks_table": "ECHO Blockworks Table",
        "item.echoblockworks.echo_pattern_cutter": "ECHO Pattern Cutter",
        "container.echoblockworks.blockworks_table": "ECHO Blockworks Table",
        "tooltip.echoblockworks.echo_pattern_cutter.forward": "Right-click a Blockworks block to cycle variants.",
        "tooltip.echoblockworks.echo_pattern_cutter.backward": "Sneak-right-click cycles variants backward.",
        "tooltip.echoblockworks.echo_pattern_cutter.durability": "Consumes durability on successful survival conversions.",
    }
    for kit in PALETTE_KITS:
        lang[f"palette_kit.{MODID}.{kit['id']}"] = kit["display_name"]
        lang[f"palette_kit.{MODID}.{kit['id']}.description"] = kit["description"]
        lang[f"palette_kit.{MODID}.{kit['id']}.usage"] = kit["recommended_usage"]

    for family, family_name, theme, structural, variants in FAMILIES:
        for variant in variants:
            block_id = full_id(family, variant)
            name = display_name(family, family_name, variant)
            write_placeholder_texture(block_id, theme, variant)
            write_full_block(block_id, theme, variant, weighted=weighted_model_eligible(block_id, variant))
            lang[f"block.{MODID}.{block_id}"] = name
            if structural:
                for shape in ("slab", "stairs", "wall"):
                    sid = f"{block_id}_{shape}"
                    write_structural_model(sid, block_id, shape, theme)
                    lang[f"block.{MODID}.{sid}"] = display_name(family, family_name, variant, shape)

    for block_id, name, kind, theme in DETAILS:
        write_placeholder_texture(block_id, theme, block_id, kind)
        write_detail_model(block_id, kind)
        lang[f"block.{MODID}.{block_id}"] = name

    write_placeholder_texture("echo_blockworks_table", "industrial", "table")
    write_full_block("echo_blockworks_table", "industrial", "table", directional=True)
    write_item_texture()
    j(RES / "assets" / MODID / "models" / "item" / "echo_pattern_cutter.json",
      {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MODID}:item/echo_pattern_cutter"}})
    j(RES / "assets" / MODID / "items" / "echo_pattern_cutter.json",
      {"model": {"type": "minecraft:model", "model": f"{MODID}:item/echo_pattern_cutter"}})
    j(RES / "assets" / MODID / "lang" / "en_us.json", dict(sorted(lang.items())))


def write_full_block(block_id: str, theme: str, variant: str, directional: bool = False, weighted: bool = False):
    model = {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{block_id}"}}
    if "glass" in variant or "crystal" in variant or "dome" in variant:
        model["render_type"] = "minecraft:translucent"
    j(RES / "assets" / MODID / "models" / "block" / f"{block_id}.json", model)
    if directional:
        j(RES / "assets" / MODID / "blockstates" / f"{block_id}.json",
          {"variants": {
              "facing=north": {"model": f"{MODID}:block/{block_id}"},
              "facing=east": {"model": f"{MODID}:block/{block_id}", "y": 90},
              "facing=south": {"model": f"{MODID}:block/{block_id}", "y": 180},
              "facing=west": {"model": f"{MODID}:block/{block_id}", "y": 270},
          }})
    else:
        if weighted:
            j(RES / "assets" / MODID / "blockstates" / f"{block_id}.json",
              {"variants": {"": [
                  {"model": f"{MODID}:block/{block_id}", "weight": 5},
                  {"model": f"{MODID}:block/{block_id}", "y": 90, "weight": 2},
                  {"model": f"{MODID}:block/{block_id}", "y": 180, "weight": 2},
                  {"model": f"{MODID}:block/{block_id}", "y": 270, "weight": 1},
              ]}})
        else:
            j(RES / "assets" / MODID / "blockstates" / f"{block_id}.json",
              {"variants": {"": {"model": f"{MODID}:block/{block_id}"}}})
    j(RES / "assets" / MODID / "items" / f"{block_id}.json",
      {"model": {"type": "minecraft:model", "model": f"{MODID}:block/{block_id}"}})


def write_structural_model(block_id: str, base_id: str, shape: str, theme: str):
    texture = f"{MODID}:block/{base_id}"
    transparent = "glass" in base_id or "crystal" in base_id or "dome" in base_id
    if shape == "slab":
        for suffix, parent in [("", "minecraft:block/slab"), ("_top", "minecraft:block/slab_top"), ("_double", "minecraft:block/cube_all")]:
            data = {"parent": parent, "textures": {"bottom": texture, "top": texture, "side": texture} if suffix != "_double" else {"all": texture}}
            if transparent:
                data["render_type"] = "minecraft:translucent"
            j(RES / "assets" / MODID / "models" / "block" / f"{block_id}{suffix}.json", data)
        j(RES / "assets" / MODID / "blockstates" / f"{block_id}.json",
          {"variants": {
              "type=bottom": {"model": f"{MODID}:block/{block_id}"},
              "type=top": {"model": f"{MODID}:block/{block_id}_top"},
              "type=double": {"model": f"{MODID}:block/{block_id}_double"},
          }})
    elif shape == "stairs":
        for suffix, parent in [("", "minecraft:block/stairs"), ("_inner", "minecraft:block/inner_stairs"), ("_outer", "minecraft:block/outer_stairs")]:
            data = {"parent": parent, "textures": {"bottom": texture, "top": texture, "side": texture}}
            if transparent:
                data["render_type"] = "minecraft:translucent"
            j(RES / "assets" / MODID / "models" / "block" / f"{block_id}{suffix}.json", data)
        variants = {}
        for facing, y in [("east", 0), ("south", 90), ("west", 180), ("north", 270)]:
            for half in ("bottom", "top"):
                xrot = 180 if half == "top" else 0
                for stair_shape, suffix in [("straight", ""), ("inner_left", "_inner"), ("inner_right", "_inner"), ("outer_left", "_outer"), ("outer_right", "_outer")]:
                    rot = y + (270 if "right" in stair_shape else 0)
                    entry = {"model": f"{MODID}:block/{block_id}{suffix}", "y": rot % 360}
                    if xrot:
                        entry["x"] = xrot
                    variants[f"facing={facing},half={half},shape={stair_shape}"] = entry
        j(RES / "assets" / MODID / "blockstates" / f"{block_id}.json", {"variants": variants})
    elif shape == "wall":
        for suffix, parent in [("_post", "minecraft:block/template_wall_post"), ("_side", "minecraft:block/template_wall_side"), ("_side_tall", "minecraft:block/template_wall_side_tall"), ("_inventory", "minecraft:block/wall_inventory")]:
            data = {"parent": parent, "textures": {"wall": texture}}
            if transparent:
                data["render_type"] = "minecraft:translucent"
            j(RES / "assets" / MODID / "models" / "block" / f"{block_id}{suffix}.json", data)
        j(RES / "assets" / MODID / "blockstates" / f"{block_id}.json",
          {"multipart": [
              {"when": {"up": "true"}, "apply": {"model": f"{MODID}:block/{block_id}_post"}},
              {"when": {"north": "low"}, "apply": {"model": f"{MODID}:block/{block_id}_side", "uvlock": True}},
              {"when": {"east": "low"}, "apply": {"model": f"{MODID}:block/{block_id}_side", "y": 90, "uvlock": True}},
              {"when": {"south": "low"}, "apply": {"model": f"{MODID}:block/{block_id}_side", "y": 180, "uvlock": True}},
              {"when": {"west": "low"}, "apply": {"model": f"{MODID}:block/{block_id}_side", "y": 270, "uvlock": True}},
              {"when": {"north": "tall"}, "apply": {"model": f"{MODID}:block/{block_id}_side_tall", "uvlock": True}},
              {"when": {"east": "tall"}, "apply": {"model": f"{MODID}:block/{block_id}_side_tall", "y": 90, "uvlock": True}},
              {"when": {"south": "tall"}, "apply": {"model": f"{MODID}:block/{block_id}_side_tall", "y": 180, "uvlock": True}},
              {"when": {"west": "tall"}, "apply": {"model": f"{MODID}:block/{block_id}_side_tall", "y": 270, "uvlock": True}},
          ]})
    j(RES / "assets" / MODID / "items" / f"{block_id}.json",
      {"model": {"type": "minecraft:model", "model": f"{MODID}:block/{block_id}{'_inventory' if shape == 'wall' else ''}"}})


def write_detail_model(block_id: str, kind: str):
    texture = f"{MODID}:block/{block_id}"
    if kind == "low":
        model = {
            "parent": "minecraft:block/block",
            "textures": {"particle": texture, "all": texture},
            "elements": [{"from": [1, 0, 1], "to": [15, 4, 15],
                          "faces": {face: {"texture": "#all"} for face in ["down", "up", "north", "south", "west", "east"]}}],
        }
        j(RES / "assets" / MODID / "models" / "block" / f"{block_id}.json", model)
        j(RES / "assets" / MODID / "blockstates" / f"{block_id}.json", {"variants": {"": {"model": f"{MODID}:block/{block_id}"}}})
    elif kind == "ceiling":
        model = {
            "parent": "minecraft:block/block",
            "textures": {"particle": texture, "all": texture},
            "elements": [{"from": [1, 13, 1], "to": [15, 16, 15],
                          "faces": {face: {"texture": "#all"} for face in ["down", "up", "north", "south", "west", "east"]}}],
        }
        j(RES / "assets" / MODID / "models" / "block" / f"{block_id}.json", model)
        j(RES / "assets" / MODID / "blockstates" / f"{block_id}.json", {"variants": {"": {"model": f"{MODID}:block/{block_id}"}}})
    elif kind == "wall":
        model = {
            "parent": "minecraft:block/block",
            "textures": {"particle": texture, "all": texture},
            "elements": [{"from": [2, 2, 0], "to": [14, 14, 3],
                          "faces": {face: {"texture": "#all"} for face in ["down", "up", "north", "south", "west", "east"]}}],
        }
        j(RES / "assets" / MODID / "models" / "block" / f"{block_id}.json", model)
        write_directional_blockstate(block_id)
    elif kind == "directional":
        j(RES / "assets" / MODID / "models" / "block" / f"{block_id}.json",
          {"parent": "minecraft:block/cube_all", "textures": {"all": texture}})
        write_directional_blockstate(block_id)
    else:
        write_full_block(block_id, "industrial", block_id)
    j(RES / "assets" / MODID / "items" / f"{block_id}.json",
      {"model": {"type": "minecraft:model", "model": f"{MODID}:block/{block_id}"}})


def write_directional_blockstate(block_id: str):
    j(RES / "assets" / MODID / "blockstates" / f"{block_id}.json",
      {"variants": {
          "facing=north": {"model": f"{MODID}:block/{block_id}"},
          "facing=east": {"model": f"{MODID}:block/{block_id}", "y": 90},
          "facing=south": {"model": f"{MODID}:block/{block_id}", "y": 180},
          "facing=west": {"model": f"{MODID}:block/{block_id}", "y": 270},
      }})


def nbt_string(value: str) -> bytes:
    data = value.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def nbt_named(tag_type: int, name: str, payload: bytes) -> bytes:
    return bytes([tag_type]) + nbt_string(name) + payload


def nbt_int(value: int) -> bytes:
    return struct.pack(">i", value)


def nbt_list(element_type: int, elements: list[bytes]) -> bytes:
    return bytes([element_type]) + struct.pack(">i", len(elements)) + b"".join(elements)


def nbt_compound(tags: list[bytes]) -> bytes:
    return b"".join(tags) + b"\x00"


def nbt_structure(path: Path, size: tuple[int, int, int], placements: list[tuple[int, int, int, str]]):
    palette_ids = []
    for _, _, _, block_id in placements:
        if block_id not in palette_ids:
            palette_ids.append(block_id)
    palette = []
    for block_id in palette_ids:
        palette.append(nbt_compound([
            nbt_named(8, "Name", nbt_string(block_ref(block_id))),
        ]))
    blocks = []
    for x, y, z, block_id in placements:
        blocks.append(nbt_compound([
            nbt_named(9, "pos", nbt_list(3, [nbt_int(x), nbt_int(y), nbt_int(z)])),
            nbt_named(3, "state", nbt_int(palette_ids.index(block_id))),
        ]))
    root = nbt_compound([
        nbt_named(3, "DataVersion", nbt_int(4325)),
        nbt_named(9, "size", nbt_list(3, [nbt_int(size[0]), nbt_int(size[1]), nbt_int(size[2])])),
        nbt_named(9, "palette", nbt_list(10, palette)),
        nbt_named(9, "blocks", nbt_list(10, blocks)),
        nbt_named(9, "entities", nbt_list(10, [])),
    ])
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.GzipFile(filename=path, mode="wb", compresslevel=9, mtime=0) as fh:
        fh.write(nbt_named(10, "", root))


def site_template_blocks(site_id: str, palette: list[str]) -> tuple[tuple[int, int, int], list[tuple[int, int, int, str]]]:
    blocks: list[tuple[int, int, int, str]] = []
    floor = palette[0]
    wall = palette[min(1, len(palette) - 1)]
    accent = palette[min(2, len(palette) - 1)]
    detail = palette[-1]
    for x in range(7):
        for z in range(7):
            if (x, z) in {(0, 0), (6, 6)} and "ruin" in site_id:
                continue
            blocks.append((x, 0, z, floor if (x + z) % 3 else accent))
    for i in range(7):
        if i not in (2, 3):
            blocks.append((i, 1, 0, wall))
            blocks.append((0, 1, i, wall))
        if i in (0, 6):
            blocks.append((i, 2, 0, accent))
            blocks.append((0, 2, i, accent))
    if "terminal" in site_id:
        blocks.extend([(3, 1, 1, "terminal_panel_screen"), (4, 1, 1, "data_wall"), (5, 1, 1, "server_cabinet")])
    elif "nexus" in site_id:
        blocks.extend([(3, 1, 3, "nexus_crystal_rift_panel"), (3, 2, 3, "nexus_crystal_glowing_crystal"), (2, 1, 3, "nexus_crystal_energy_conduit")])
    elif "blackbox" in site_id:
        blocks.extend([(3, 1, 3, "blackbox_vault_locked_panel"), (3, 2, 3, "blackbox_vault_warning_light"), (4, 1, 3, "blackbox_vault_memory_glass")])
    elif "reclamation" in site_id:
        blocks.extend([(2, 1, 2, "reclamation_glass_framed_glass"), (3, 2, 3, "reclamation_glass_dome_panel"), (4, 1, 2, "reclamation_glass_overgrown_glass")])
    elif "orbital" in site_id:
        blocks.extend([(2, 1, 3, "orbital_hull_airlock_frame"), (3, 1, 3, "orbital_hull_lit_strip"), (4, 1, 3, "signal_dish_decorative")])
    elif "convoy" in site_id:
        blocks.extend([(2, 1, 3, "charred_concrete_warning_stripe"), (3, 1, 3, "warning_beacon"), (4, 1, 3, "wall_pipe")])
    else:
        blocks.extend([(2, 1, 2, detail), (4, 1, 4, "rubble_pile"), (5, 1, 2, "scattered_debris")])
    return (7, 4, 7), blocks


def write_worldgen():
    j(RES / "data" / MODID / "worldgen" / "structure" / "blockworks_showcase_site.json",
      {
          "type": "minecraft:jigsaw",
          "biomes": f"#{MODID}:has_structure/blockworks_showcase_site",
          "adapt_noise": True,
          "spawn_overrides": {},
          "step": "surface_structures",
          "terrain_adaptation": "beard_thin",
          "start_pool": f"{MODID}:blockworks_showcase_sites",
          "size": 1,
          "start_height": {"absolute": 0},
          "project_start_to_heightmap": "WORLD_SURFACE_WG",
          "max_distance_from_center": 24,
          "use_expansion_hack": False,
          "pool_aliases": [],
      })
    j(RES / "data" / MODID / "worldgen" / "structure_set" / "blockworks_showcase_sites.json",
      {
          "placement": {"type": "minecraft:random_spread", "spacing": 56, "separation": 20, "salt": 42026020},
          "structures": [{"structure": f"{MODID}:blockworks_showcase_site", "weight": 1}],
      })
    j(RES / "data" / MODID / "worldgen" / "template_pool" / "blockworks_showcase_sites.json",
      {
          "name": f"{MODID}:blockworks_showcase_sites",
          "fallback": "minecraft:empty",
          "elements": [
              {
                  "weight": weight,
                  "element": {
                      "element_type": "minecraft:single_pool_element",
                      "projection": "terrain_matching",
                      "location": f"{MODID}:{template}",
                      "processors": "minecraft:empty",
                  },
              }
              for _, _, _, template, weight in WORLDGEN_SITES
          ],
      })
    write_tag(MODID, "worldgen/biome/has_structure", "blockworks_showcase_site", ["#minecraft:is_overworld"])
    for site_id, _, palette_id, template, _ in WORLDGEN_SITES:
        size, blocks = site_template_blocks(site_id, PALETTES[palette_id])
        nbt_structure(RES / "data" / MODID / "structures" / f"{template}.nbt", size, blocks)


def write_loot_recipes_tags_palettes():
    for block_id in all_blocks():
        j(RES / "data" / MODID / "loot_table" / "blocks" / f"{block_id}.json",
          {"type": "minecraft:block", "pools": [{"bonus_rolls": 0.0, "conditions": [{"condition": "minecraft:survives_explosion"}], "entries": [{"type": "minecraft:item", "name": block_ref(block_id)}], "rolls": 1.0}], "random_sequence": f"{MODID}:blocks/{block_id}"})

    for recipe_id, (pattern, key, count) in BASE_RECIPES.items():
        j(RES / "data" / MODID / "recipe" / f"{recipe_id}.json",
          {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern, "key": key, "result": {"id": block_ref(recipe_id), "count": count}})

    j(RES / "data" / MODID / "recipe" / "echo_blockworks_table.json",
      {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["ICI", "RTR", "SSS"],
       "key": {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone", "T": "minecraft:crafting_table", "S": "minecraft:smooth_stone"},
       "result": {"id": block_ref("echo_blockworks_table")}})
    j(RES / "data" / MODID / "recipe" / "echo_pattern_cutter.json",
      {"type": "minecraft:crafting_shaped", "category": "tools", "pattern": [" C ", "ICI", " S "],
       "key": {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "S": "minecraft:stick"},
       "result": {"id": block_ref("echo_pattern_cutter")}})

    for family, _, _, structural, variants in FAMILIES:
        base = full_id(family, variants[0])
        for variant in variants:
            target = full_id(family, variant)
            j(RES / "data" / MODID / "recipe" / f"{target}_from_{base}_stonecutting.json",
              {"type": "minecraft:stonecutting", "ingredient": block_ref(base), "result": {"id": block_ref(target)}})
            if structural:
                j(RES / "data" / MODID / "recipe" / f"{target}_slab_from_{target}_stonecutting.json",
                  {"type": "minecraft:stonecutting", "ingredient": block_ref(target), "result": {"id": block_ref(f"{target}_slab"), "count": 2}})
                j(RES / "data" / MODID / "recipe" / f"{target}_stairs_from_{target}_stonecutting.json",
                  {"type": "minecraft:stonecutting", "ingredient": block_ref(target), "result": {"id": block_ref(f"{target}_stairs")}})
                j(RES / "data" / MODID / "recipe" / f"{target}_wall_from_{target}_stonecutting.json",
                  {"type": "minecraft:stonecutting", "ingredient": block_ref(target), "result": {"id": block_ref(f"{target}_wall")}})

    write_tags()
    for name, values in PALETTES.items():
        j(RES / "data" / MODID / "palettes" / f"{name}.json",
          {"name": title(name), "description": f"Suggested ECHO Blockworks palette for {title(name)} structures.", "blocks": [block_ref(v) for v in values]})
    for kit in PALETTE_KITS:
        data = {
            "id": f"{MODID}:{kit['id']}",
            "display_name": kit["display_name"],
            "description": kit["description"],
            "recommended_usage": kit["recommended_usage"],
            "theme": kit["theme"],
            "family_ids": kit["family_ids"],
            "featured_blocks": [block_ref(v) for v in kit["featured_block_ids"]],
            "accent_blocks": [block_ref(v) for v in kit["accent_block_ids"]],
        }
        if "worldgen_site_id" in kit:
            data["worldgen_site"] = f"{MODID}:{kit['worldgen_site_id']}"
        j(RES / "data" / MODID / "palette_kits" / f"{kit['id']}.json", data)
    write_worldgen()


def write_tag(namespace: str, root: str, name: str, values: list[str]):
    j(RES / "data" / namespace / "tags" / root / f"{name}.json", {"replace": False, "values": values})


def write_tags():
    full_blocks = [block_ref(v) for v in all_full_blocks()]
    all_block_refs = [block_ref(v) for v in all_blocks()]
    structural = {
        "slabs": [block_ref(b) for b in all_blocks() if b.endswith("_slab")],
        "stairs": [block_ref(b) for b in all_blocks() if b.endswith("_stairs")],
        "walls": [block_ref(b) for b in all_blocks() if b.endswith("_wall")],
    }

    tag_values = {
        "reinforced_frames": [block_ref(v) for v in family_blocks("reinforced_metal") if "frame" in v or "pillar" in v],
        "industrial_panels": [block_ref(v) for v in family_blocks("reinforced_metal")],
        "rusted_panels": [block_ref(v) for v in family_blocks("rusted_metal")],
        "ruined_city_blocks": [block_ref(v) for f in ("rusted_metal", "ashstone", "charred_concrete") for v in family_blocks(f)] + [block_ref("rubble_pile"), block_ref("scattered_debris")],
        "ashfall_ruin_blocks": [block_ref(v) for f in ("ashstone", "charred_concrete", "rusted_metal") for v in family_blocks(f)],
        "terminal_panels": [block_ref(v) for v in family_blocks("terminal_panel")],
        "echo_circuit_blocks": [block_ref(v) for v in family_blocks("echo_circuit")],
        "orbital_hulls": [block_ref(v) for v in family_blocks("orbital_hull")],
        "nexus_blocks": [block_ref(v) for v in family_blocks("nexus_crystal")],
        "nexus_glass": [block_ref(v) for v in family_blocks("nexus_crystal", False) if "glass" in v or "crystal" in v],
        "blackbox_vault_blocks": [block_ref(v) for v in family_blocks("blackbox_vault")],
        "reclamation_blocks": [block_ref(v) for v in family_blocks("reclamation_glass")],
        "convoy_depot_blocks": [block_ref(v) for v in ["charred_concrete_road_plate", "charred_concrete_warning_stripe", "reinforced_metal_frame", "rusted_metal_pipe_wall", "warning_beacon", "wall_pipe", "signal_dish_decorative"]],
        "hazard_blocks": [block_ref(v) for v in all_blocks() if "hazard" in v or "warning" in v or "sparking" in v],
        "lab_blocks": [block_ref(v) for f in ("terminal_panel", "echo_circuit", "reclamation_glass") for v in family_blocks(f)] + [block_ref("hologram_floor_projector")],
        "multiblock_valid_frames": [block_ref(v) for v in all_blocks() if "frame" in v or "pillar" in v or "vault_wall" in v],
        "multiblock_decorative_valid": full_blocks,
        "worldgen_ruin_materials": [block_ref(v) for f in ("rusted_metal", "ashstone", "charred_concrete") for v in family_blocks(f)] + [block_ref("rubble_pile"), block_ref("scattered_debris")],
    }
    for name, values in tag_values.items():
        write_tag(MODID, "block", name, values)

    item_tag_values = {
        "blockworks_blocks": full_blocks,
        "pattern_cuttable": full_blocks + structural["slabs"] + structural["stairs"] + structural["walls"],
        "industrial_theme": [block_ref(v) for v in family_blocks("reinforced_metal") + family_blocks("rusted_metal")],
        "orbital_theme": [block_ref(v) for v in family_blocks("orbital_hull")],
        "nexus_theme": [block_ref(v) for v in family_blocks("nexus_crystal")],
        "blackbox_theme": [block_ref(v) for v in family_blocks("blackbox_vault")],
        "reclamation_theme": [block_ref(v) for v in family_blocks("reclamation_glass")],
    }
    for name, values in item_tag_values.items():
        write_tag(MODID, "item", name, values)

    write_tag("minecraft", "block/mineable", "pickaxe", all_block_refs)
    write_tag("minecraft", "block", "needs_stone_tool", all_block_refs)
    write_tag("minecraft", "block", "needs_iron_tool", [block_ref(v) for v in all_blocks() if any(k in v for k in ["metal", "terminal", "circuit", "orbital", "blackbox", "nexus", "beacon", "pipe", "cable", "table"])])
    write_tag("minecraft", "block", "needs_diamond_tool", [block_ref(v) for v in all_blocks() if "blackbox_vault" in v or "nexus_crystal" in v])
    for name, values in structural.items():
        write_tag("minecraft", "block", name, values)
        write_tag("minecraft", "item", name, values)


def main():
    write_assets()
    write_loot_recipes_tags_palettes()
    print(f"Generated {len(all_blocks())} block resources for {MODID}.")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Generate deterministic Orbital-style pixel-art textures for ECHO mods.

The default target preserves the original Orbital Remnants workflow. Use
``--target all`` to recreate every current texture PNG in Ashfall Protocol and
Orbital Remnants while keeping texture paths and dimensions stable.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFilter


REPO_ROOT = Path(__file__).resolve().parents[3]
ADDON_ROOT = Path(__file__).resolve().parents[1]
BUILD_OUT = REPO_ROOT / "build" / "texture_previews"


@dataclass(frozen=True)
class TextureTarget:
    key: str
    modid: str
    root: Path

    @property
    def assets(self) -> Path:
        return self.root / "src" / "main" / "resources" / "assets" / self.modid

    @property
    def textures(self) -> Path:
        return self.assets / "textures"

    @property
    def item_defs(self) -> Path:
        return self.assets / "items"

    @property
    def block_models(self) -> Path:
        return self.assets / "models" / "block"

    @property
    def item_models(self) -> Path:
        return self.assets / "models" / "item"


@dataclass(frozen=True)
class TextureSpec:
    target: str
    modid: str
    kind: str
    name: str
    rel_path: str
    path: Path
    width: int
    height: int

    def manifest_row(self) -> dict[str, object]:
        data = asdict(self)
        data["path"] = self.rel_path
        return data


@dataclass(frozen=True)
class Palette:
    base: tuple[int, int, int]
    dark: tuple[int, int, int]
    mid: tuple[int, int, int]
    light: tuple[int, int, int]
    accent: tuple[int, int, int]
    glow: tuple[int, int, int]


@dataclass(frozen=True)
class RolePalette:
    shadow: tuple[int, int, int]
    dark: tuple[int, int, int]
    base: tuple[int, int, int]
    mid: tuple[int, int, int]
    light: tuple[int, int, int]
    accent: tuple[int, int, int]
    glow: tuple[int, int, int]


TARGETS = {
    "ashfall": TextureTarget("ashfall", "echoashfallprotocol", REPO_ROOT),
    "orbital": TextureTarget("orbital", "echoorbitalremnants", ADDON_ROOT),
}

ADVANCED_BLOCKS = {
    "rocket_assembly_frame",
    "fuel_refinery",
    "oxygen_compressor",
    "heat_shield_fabricator",
    "orbital_fabricator",
    "vacuum_smelter",
    "solar_reclaimer",
    "suit_charging_station",
    "navigation_console",
    "station_life_support_core",
    "docking_beacon",
    "launch_platform",
}

MULTIFACE_MACHINE_BLOCKS = {
    "rocket_assembly_frame",
    "fuel_refinery",
    "oxygen_compressor",
    "heat_shield_fabricator",
    "orbital_fabricator",
    "vacuum_smelter",
    "solar_reclaimer",
    "suit_charging_station",
    "signal_analyzer",
    "navigation_console",
    "station_life_support_core",
}

ROUTE_ITEMS = {
    "emergency_rocket",
    "orbital_shuttle",
    "mars_transfer_window",
    "europa_transfer_window",
    "nexus_drive_vessel",
}

ASHFALL_BOSS_ENTITIES = {
    "warden_boss",
    "wasteland_sentinel",
    "plains_warlord",
    "city_ruin_stalker",
    "industrial_juggernaut",
    "toxic_hive_matriarch",
    "crash_zone_colossus",
    "radiation_behemoth",
    "cryogenic_overseer",
    "nexus_scar_avatar",
}

ASHFALL_BIOME_GUARDIAN_ENTITIES = ASHFALL_BOSS_ENTITIES - {"warden_boss"}

ASHFALL_GUARDIAN_GLOW_ENTITIES = {f"{name}_glow" for name in ASHFALL_BIOME_GUARDIAN_ENTITIES}

GUARDIAN_VARIANTS = {
    "wasteland_sentinel": "sentinel",
    "plains_warlord": "warlord",
    "city_ruin_stalker": "stalker",
    "industrial_juggernaut": "juggernaut",
    "toxic_hive_matriarch": "matriarch",
    "crash_zone_colossus": "colossus",
    "radiation_behemoth": "behemoth",
    "cryogenic_overseer": "overseer",
    "nexus_scar_avatar": "nexus",
}

ASHFALL_DRONE_ENTITIES = {
    "echo_companion_drone",
    "echo_drone",
    "scout_drone",
}

ASHFALL_WOLF_ENTITIES = {
    "irradiated_wolf",
    "wild_dog",
}

ASHFALL_SLIME_ENTITIES = {
    "toxic_slime",
}

ASHFALL_CRAWLER_ENTITIES = {
    "mutated_crawler",
}

ORBITAL_VEX_ENTITIES = {
    "echo_defense_drone",
    "vacuum_wraith",
    "corrupted_docking_ai",
    "europa_cryo_warden",
}

ORBITAL_ZOMBIE_ENTITIES = {
    "broken_astronaut",
    "nexus_husk",
    "lunar_nexus_husk",
    "abandoned_captain",
    "echo_zero",
}

EXTRA_ENTITY_TEXTURES = {
    "ashfall": ASHFALL_BOSS_ENTITIES
    | ASHFALL_GUARDIAN_GLOW_ENTITIES
    | ASHFALL_DRONE_ENTITIES
    | ASHFALL_WOLF_ENTITIES
    | ASHFALL_SLIME_ENTITIES
    | ASHFALL_CRAWLER_ENTITIES,
    "orbital": ORBITAL_VEX_ENTITIES | ORBITAL_ZOMBIE_ENTITIES,
}

SUIT_WORDS = {
    "helmet",
    "chestplate",
    "leggings",
    "boots",
    "visor",
    "oxygen_tank",
    "booster",
    "liner",
    "sealant",
    "armor",
    "wrap",
}

WEAPON_WORDS = {
    "knife",
    "blade",
    "spear",
    "hammer",
    "launcher",
    "lance",
    "cutter",
    "annihilator",
}

COMPONENT_WORDS = {
    "rocket",
    "engine",
    "fuel",
    "shield",
    "gear",
    "cargo",
    "support",
    "flight",
    "navigation",
    "circuit",
    "cell",
    "membrane",
    "filter",
    "casing",
    "coil",
    "servo",
    "module",
    "plate",
    "ingot",
    "scrap",
    "plastic",
    "metal",
}

MACHINE_WORDS = {
    "machine",
    "generator",
    "scrubber",
    "condenser",
    "terminal",
    "controller",
    "purifier",
    "refiner",
    "grinder",
    "press",
    "scanner",
    "relay",
    "battery",
    "pipe",
    "cable",
    "array",
    "burner",
    "workbench",
    "med_bay",
    "bank",
    "station",
    "node",
    "collector",
    "garden",
    "crate",
    "table",
    "rack",
    "lab",
    "hopper",
    "synthesizer",
    "miner",
    "recycler",
    "counter",
    "core",
    "barrel",
    "bench",
    "fabricator",
    "compressor",
    "beacon",
    "platform",
}

PLANT_WORDS = {
    "grass",
    "fern",
    "bush",
    "sapling",
    "reed",
    "wheat",
    "cactus",
    "fungus",
    "flower",
    "vine",
}

SOIL_WORDS = {
    "dirt",
    "mud",
    "dust",
    "sand",
    "ash",
    "soil",
    "wasteland",
    "crash_zone",
    "regolith",
}

STONE_WORDS = {
    "stone",
    "cobble",
    "gravel",
    "slate",
    "basalt",
    "concrete",
    "brick",
    "rubble",
    "rock",
}

METAL_WORDS = {
    "metal",
    "steel",
    "iron",
    "alloy",
    "titanium",
    "plating",
    "plate",
    "panel",
    "wall",
    "frame",
    "crate",
    "barrel",
    "scrap",
}

ORGANIC_WORDS = {
    "toxic",
    "mutated",
    "mutant",
    "flesh",
    "tissue",
    "organic",
    "hive",
    "spore",
    "fungus",
    "bio",
    "bone",
}

CRYSTAL_WORDS = {
    "crystal",
    "ore",
    "uranium",
    "shard",
    "geode",
    "meteorite",
}

TECH_WORDS = {
    "circuit",
    "terminal",
    "console",
    "relay",
    "battery",
    "core",
    "node",
    "scanner",
    "beacon",
}

WOOD_WORDS = {
    "wood",
    "plank",
    "log",
    "pallet",
}


def clamp(value: int) -> int:
    return max(0, min(255, value))


def adjust(color: tuple[int, int, int], amount: int) -> tuple[int, int, int]:
    return tuple(clamp(channel + amount) for channel in color)


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(clamp(round(a[i] * (1.0 - t) + b[i] * t)) for i in range(3))


def rgba(color: tuple[int, int, int], alpha: int = 255) -> tuple[int, int, int, int]:
    return color[0], color[1], color[2], alpha


def stable_rng(name: str) -> random.Random:
    digest = hashlib.sha256(name.encode("utf-8")).digest()
    return random.Random(int.from_bytes(digest[:8], "big"))


def palette_for(name: str) -> Palette:
    if "nexus" in name or "anomaly" in name or "choir" in name:
        base = (79, 42, 116)
        accent = (189, 93, 255)
        glow = (233, 178, 255)
    elif any(word in name for word in ("cryo", "frozen", "europa", "ice")):
        base = (54, 126, 154)
        accent = (111, 229, 255)
        glow = (212, 252, 255)
    elif any(word in name for word in ("mars", "martian", "rust", "crimson")):
        base = (143, 61, 45)
        accent = (230, 120, 69)
        glow = (255, 184, 120)
    elif any(word in name for word in ("lunar", "moon", "helium", "alloy", "titanium", "steel")):
        base = (118, 121, 124)
        accent = (184, 196, 215)
        glow = (236, 245, 255)
    elif any(word in name for word in ("solar", "heat", "thermal", "burner")):
        base = (140, 104, 42)
        accent = (255, 191, 64)
        glow = (255, 236, 146)
    elif any(word in name for word in ("oxygen", "life_support", "water", "purifier", "filter")):
        base = (34, 110, 122)
        accent = (67, 220, 210)
        glow = (173, 255, 241)
    elif any(word in name for word in ("toxic", "radiation", "rad_", "uranium", "contaminated")):
        base = (78, 96, 48)
        accent = (164, 232, 78)
        glow = (229, 255, 126)
    elif any(word in name for word in ("stone", "cobble", "gravel", "rubble", "rock", "slate", "basalt")):
        base = (103, 106, 99)
        accent = (145, 139, 118)
        glow = (190, 184, 158)
    elif any(word in name for word in ("dirt", "mud", "soil", "regolith", "sand")):
        base = (117, 101, 78)
        accent = (154, 133, 91)
        glow = (198, 177, 122)
    elif any(word in name for word in ("ash", "charred", "wasteland", "dust", "concrete")):
        base = (104, 98, 86)
        accent = (188, 151, 91)
        glow = (236, 198, 121)
    elif any(word in name for word in ("schematic", "data_log", "book", "note", "map", "document", "archive")):
        base = (172, 159, 126)
        accent = (104, 130, 138)
        glow = (221, 211, 172)
    elif any(word in name for word in MACHINE_WORDS | METAL_WORDS):
        base = (91, 101, 106)
        accent = (80, 166, 184)
        glow = (178, 238, 245)
    elif any(word in name for word in ("mutated", "mutant", "bone", "hide", "flesh", "tissue", "organic")):
        base = (122, 72, 76)
        accent = (146, 204, 92)
        glow = (217, 237, 110)
    elif any(word in name for word in ("void", "vacuum", "deep_space")):
        base = (50, 64, 89)
        accent = (100, 160, 220)
        glow = (178, 226, 255)
    elif any(word in name for word in ("orbital", "satellite", "station", "rocket", "shuttle")):
        base = (77, 86, 94)
        accent = (92, 210, 238)
        glow = (188, 246, 255)
    elif "fuel" in name:
        base = (70, 90, 58)
        accent = (138, 210, 85)
        glow = (208, 255, 134)
    else:
        rng = stable_rng(name)
        base = (rng.randrange(70, 145), rng.randrange(70, 145), rng.randrange(80, 155))
        accent = adjust(base, 70)
        glow = adjust(accent, 45)
    return Palette(
        base=base,
        dark=adjust(base, -50),
        mid=adjust(base, 12),
        light=adjust(base, 58),
        accent=accent,
        glow=glow,
    )


def style_colors(name: str) -> list[tuple[int, int, int]]:
    palette = palette_for(name)
    colors = [
        palette.base,
        palette.dark,
        palette.mid,
        palette.light,
        palette.accent,
        palette.glow,
        mix(palette.base, palette.dark, 0.5),
        mix(palette.base, palette.accent, 0.35),
    ]
    unique: list[tuple[int, int, int]] = []
    for color in colors:
        if color not in unique:
            unique.append(color)
    return unique


def color_distance(a: tuple[int, int, int], b: tuple[int, int, int]) -> int:
    return sum((a[i] - b[i]) ** 2 for i in range(3))


def nearest_style_color(color: tuple[int, int, int], colors: list[tuple[int, int, int]]) -> tuple[int, int, int]:
    return min(colors, key=lambda candidate: color_distance(color, candidate))


def normalize_texture_palette(
    img: Image.Image,
    name: str,
    partial_alpha: bool = False,
    force_opaque: bool = False,
    colors_override: list[tuple[int, int, int]] | None = None,
) -> Image.Image:
    colors = colors_override or style_colors(name)
    out = img.convert("RGBA")
    px = out.load()
    alpha = 160 if partial_alpha else 255
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if force_opaque:
                px[x, y] = rgba(nearest_style_color((r, g, b), colors), 255)
            elif a < 32:
                px[x, y] = (0, 0, 0, 0)
            else:
                px[x, y] = rgba(nearest_style_color((r, g, b), colors), alpha)
    return out


def block_style_colors(name: str) -> list[tuple[int, int, int]]:
    role = block_role(name)
    colors = role_palette(name, role)
    values = [
        colors.shadow,
        colors.dark,
        colors.base,
        colors.mid,
        colors.light,
        colors.accent,
        colors.glow,
        mix(colors.base, colors.dark, 0.5),
        mix(colors.base, colors.light, 0.35),
    ]
    unique: list[tuple[int, int, int]] = []
    for color in values:
        if color not in unique:
            unique.append(color)
    return unique


def put_noise(
    draw: ImageDraw.ImageDraw,
    rng: random.Random,
    width: int,
    height: int,
    palette: Palette,
    strength: int = 20,
) -> None:
    for y in range(height):
        for x in range(width):
            if rng.random() < 0.42:
                amount = rng.randrange(-strength, strength + 1)
                draw.point((x, y), fill=rgba(adjust(palette.base, amount)))


def bevel_rect(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    palette: Palette,
    fill: tuple[int, int, int] | None = None,
    outline: tuple[int, int, int] | None = None,
) -> None:
    x0, y0, x1, y1 = box
    if x1 <= x0 or y1 <= y0:
        return
    draw.rectangle(box, fill=rgba(fill or palette.mid), outline=rgba(outline or palette.dark))
    draw.line((x0 + 1, y0 + 1, x1 - 1, y0 + 1), fill=rgba(palette.light))
    draw.line((x0 + 1, y0 + 1, x0 + 1, y1 - 1), fill=rgba(palette.light))
    draw.line((x0 + 1, y1 - 1, x1 - 1, y1 - 1), fill=rgba(palette.dark))
    draw.line((x1 - 1, y0 + 1, x1 - 1, y1 - 1), fill=rgba(palette.dark))


def draw_glow(draw: ImageDraw.ImageDraw, center: tuple[int, int], radius: int, color: tuple[int, int, int]) -> None:
    cx, cy = center
    for r in range(radius, 0, -1):
        alpha = int(38 * (1.0 - r / (radius + 1)))
        draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=rgba(color, alpha))
    draw.point((cx, cy), fill=rgba(color))


def has_word(name: str, words: set[str] | tuple[str, ...]) -> bool:
    return any(word in name for word in words)


def material_family(name: str) -> str:
    if "nexus" in name or "anomaly" in name or "choir" in name:
        return "nexus"
    if has_word(name, CRYSTAL_WORDS):
        return "crystal"
    if has_word(name, ORGANIC_WORDS):
        return "organic"
    if has_word(name, TECH_WORDS):
        return "tech"
    if has_word(name, METAL_WORDS):
        return "metal"
    if has_word(name, WOOD_WORDS):
        return "wood"
    if has_word(name, SOIL_WORDS):
        return "soil"
    if has_word(name, STONE_WORDS):
        return "stone"
    return "stone"


def fill_material_base(
    draw: ImageDraw.ImageDraw,
    rng: random.Random,
    width: int,
    height: int,
    palette: Palette,
    family: str,
) -> None:
    """Fill in chunky pixel clusters instead of per-pixel static."""
    cell = 2 if min(width, height) <= 16 else 3
    colors = {
        "soil": (palette.dark, palette.base, palette.mid, mix(palette.base, palette.accent, 0.25)),
        "stone": (palette.dark, palette.base, palette.mid, palette.light),
        "metal": (palette.dark, palette.base, palette.mid, palette.light),
        "tech": (palette.dark, palette.base, palette.mid, mix(palette.base, palette.accent, 0.3)),
        "organic": (palette.dark, palette.base, palette.mid, mix(palette.base, palette.accent, 0.45)),
        "crystal": (palette.dark, palette.base, mix(palette.base, palette.accent, 0.35), palette.light),
        "nexus": (palette.dark, palette.base, mix(palette.base, palette.accent, 0.35), palette.accent),
        "wood": (palette.dark, palette.base, palette.mid, mix(palette.base, palette.accent, 0.18)),
    }.get(family, (palette.dark, palette.base, palette.mid, palette.light))

    for y in range(0, height, cell):
        for x in range(0, width, cell):
            shade = (x + y) / max(1, width + height - 2)
            roll = rng.random()
            if shade < 0.28 and roll < 0.52:
                color = colors[3]
            elif shade > 0.72 and roll < 0.58:
                color = colors[0]
            elif roll < 0.32:
                color = colors[1]
            elif roll < 0.72:
                color = colors[2]
            else:
                color = mix(colors[1], colors[0 if shade > 0.5 else 3], 0.45)
            draw.rectangle((x, y, min(width - 1, x + cell - 1), min(height - 1, y + cell - 1)), fill=rgba(color))


def wrapped_box(
    draw: ImageDraw.ImageDraw,
    width: int,
    height: int,
    box: tuple[int, int, int, int],
    fill: tuple[int, int, int],
    outline: tuple[int, int, int] | None = None,
    alpha: int = 255,
) -> None:
    x0, y0, x1, y1 = box
    for ox in (-width, 0, width):
        for oy in (-height, 0, height):
            bx0, by0, bx1, by1 = x0 + ox, y0 + oy, x1 + ox, y1 + oy
            if bx1 < 0 or by1 < 0 or bx0 >= width or by0 >= height:
                continue
            clipped = (max(0, bx0), max(0, by0), min(width - 1, bx1), min(height - 1, by1))
            draw.rectangle(clipped, fill=rgba(fill, alpha), outline=rgba(outline or fill, alpha))


def block_cluster(
    draw: ImageDraw.ImageDraw,
    width: int,
    height: int,
    x: int,
    y: int,
    w: int,
    h: int,
    palette: Palette,
    fill: tuple[int, int, int],
    wrap: bool = False,
) -> None:
    box = (x, y, x + w - 1, y + h - 1)
    if wrap:
        wrapped_box(draw, width, height, box, fill, palette.dark)
    else:
        draw.rectangle(box, fill=rgba(fill), outline=rgba(palette.dark))
    x0, y0, x1, y1 = box
    for ox in (-width, 0, width) if wrap else (0,):
        for oy in (-height, 0, height) if wrap else (0,):
            bx0, by0, bx1, by1 = x0 + ox, y0 + oy, x1 + ox, y1 + oy
            if bx1 < 0 or by1 < 0 or bx0 >= width or by0 >= height:
                continue
            cx0, cy0, cx1, cy1 = max(0, bx0), max(0, by0), min(width - 1, bx1), min(height - 1, by1)
            if cx1 - cx0 >= 1:
                draw.line((cx0, cy0, cx1, cy0), fill=rgba(mix(fill, palette.light, 0.55), 180))
                draw.line((cx0, cy1, cx1, cy1), fill=rgba(mix(fill, palette.dark, 0.55), 200))
            if cy1 - cy0 >= 1:
                draw.line((cx0, cy0, cx0, cy1), fill=rgba(mix(fill, palette.light, 0.35), 150))
                draw.line((cx1, cy0, cx1, cy1), fill=rgba(mix(fill, palette.dark, 0.45), 180))


def draw_controlled_grain(
    draw: ImageDraw.ImageDraw,
    rng: random.Random,
    width: int,
    height: int,
    palette: Palette,
    count: int,
    colors: tuple[tuple[int, int, int], ...],
) -> None:
    for _ in range(count):
        w = rng.choice((1, 2, 2, 3))
        h = rng.choice((1, 1, 2))
        x = rng.randrange(0, max(1, width - w + 1))
        y = rng.randrange(0, max(1, height - h + 1))
        wrap = x == 0 or y == 0 or x + w >= width or y + h >= height
        block_cluster(draw, width, height, x, y, w, h, palette, rng.choice(colors), wrap=wrap and rng.random() < 0.45)


def draw_crack_path(
    draw: ImageDraw.ImageDraw,
    rng: random.Random,
    width: int,
    height: int,
    palette: Palette,
    start: tuple[int, int] | None = None,
) -> None:
    x, y = start or (rng.randrange(2, width - 2), rng.randrange(1, max(2, height // 2)))
    length = rng.randrange(max(4, width // 3), max(5, width // 2 + height // 4))
    for i in range(length):
        nx = max(0, min(width - 1, x + rng.choice((-1, 0, 1))))
        ny = max(0, min(height - 1, y + rng.choice((0, 1, 1))))
        draw.line((x, y, nx, ny), fill=rgba(palette.dark, 220))
        if i % 3 == 0 and nx + 1 < width:
            draw.point((nx + 1, ny), fill=rgba(mix(palette.dark, palette.base, 0.35), 190))
        x, y = nx, ny


def draw_soil_language(draw: ImageDraw.ImageDraw, rng: random.Random, name: str, width: int, height: int, palette: Palette) -> None:
    draw_controlled_grain(
        draw,
        rng,
        width,
        height,
        palette,
        max(8, width * height // 26),
        (palette.dark, palette.mid, mix(palette.base, palette.accent, 0.22), mix(palette.light, palette.base, 0.45)),
    )
    if "grass" in name or "moss" in name or "wasteland" in name:
        green = mix((73, 112, 64), palette.accent, 0.18)
        for y in (0, 1, height - 1):
            for x in range(0, width, 4):
                draw.line((x, y, min(width - 1, x + 2), y), fill=rgba(green, 210))
        for _ in range(max(3, width // 3)):
            x = rng.randrange(width)
            y = rng.randrange(height)
            block_cluster(draw, width, height, x, y, rng.choice((2, 3)), 1, palette, green, wrap=x < 2 or x > width - 3)
    if "ash" in name or "dust" in name:
        for _ in range(max(3, width // 4)):
            y = rng.randrange(height)
            draw.line((0, y, width - 1, y), fill=rgba(mix(palette.base, palette.light, 0.28), 90))


def draw_stone_language(draw: ImageDraw.ImageDraw, rng: random.Random, name: str, width: int, height: int, palette: Palette) -> None:
    step = 4 if width <= 16 else 6
    for y in range(rng.randrange(0, 2), height, step):
        draw.line((0, y, width - 1, y), fill=rgba(mix(palette.dark, palette.base, 0.25), 150))
    for x in range(rng.randrange(0, 2), width, step + 1):
        draw.line((x, 0, x, height - 1), fill=rgba(mix(palette.light, palette.base, 0.4), 85))
    draw_controlled_grain(draw, rng, width, height, palette, max(6, width * height // 34), (palette.dark, palette.mid, palette.light))
    if "brick" in name or "tile" in name:
        row = max(4, height // 4)
        for y in range(row, height, row):
            draw.line((0, y, width - 1, y), fill=rgba(palette.dark, 210))
        for r, y in enumerate(range(0, height, row)):
            offset = (width // 4) if r % 2 else 0
            for x in range(offset, width, max(6, width // 2)):
                draw.line((x, y, x, min(height - 1, y + row - 1)), fill=rgba(palette.dark, 185))
    else:
        for _ in range(2):
            draw_crack_path(draw, rng, width, height, palette)


def draw_metal_language(draw: ImageDraw.ImageDraw, rng: random.Random, name: str, width: int, height: int, palette: Palette) -> None:
    panel_w = max(5, width // 2)
    panel_h = max(5, height // 2)
    for y in range(0, height, panel_h):
        for x in range(0, width, panel_w):
            x1 = min(width - 1, x + panel_w - 1)
            y1 = min(height - 1, y + panel_h - 1)
            fill = mix(palette.base, palette.light if (x + y) % 2 == 0 else palette.dark, 0.18)
            draw.rectangle((x, y, x1, y1), fill=rgba(fill), outline=rgba(palette.dark, 205))
            draw.line((x + 1, y + 1, x1 - 1, y + 1), fill=rgba(palette.light, 150))
            draw.line((x + 1, y1 - 1, x1 - 1, y1 - 1), fill=rgba(palette.dark, 185))
    for x, y in ((2, 2), (width - 3, 2), (2, height - 3), (width - 3, height - 3)):
        if 0 <= x < width and 0 <= y < height:
            draw.rectangle((x - 1, y - 1, x, y), fill=rgba(palette.light), outline=rgba(palette.dark))
    if "hazard" in name or "radiation" in name:
        for x in range(-height, width, 5):
            draw.line((x, height - 1, x + height, 0), fill=rgba(palette.accent, 190))
    elif "rust" in name or "scrap" in name:
        draw_controlled_grain(draw, rng, width, height, palette, max(5, width * height // 42), (palette.accent, mix(palette.accent, palette.dark, 0.4)))


def draw_crystal_language(draw: ImageDraw.ImageDraw, rng: random.Random, name: str, width: int, height: int, palette: Palette) -> None:
    draw_stone_language(draw, rng, name, width, height, palette)
    for _ in range(max(3, width // 3)):
        cx = rng.randrange(2, width - 2)
        cy = rng.randrange(2, height - 2)
        h = rng.randrange(3, max(4, height // 2))
        points = ((cx, cy - h // 2), (min(width - 1, cx + 2), cy), (cx + 1, min(height - 1, cy + h // 2)), (max(0, cx - 2), cy + 1))
        draw.polygon(points, fill=rgba(palette.accent), outline=rgba(palette.glow))
        draw.line((cx, max(0, cy - h // 2 + 1), cx, min(height - 1, cy + h // 2 - 1)), fill=rgba(palette.light, 210))


def draw_organic_language(draw: ImageDraw.ImageDraw, rng: random.Random, name: str, width: int, height: int, palette: Palette) -> None:
    draw_controlled_grain(draw, rng, width, height, palette, max(9, width * height // 30), (palette.dark, palette.mid, mix(palette.base, palette.accent, 0.45)))
    for _ in range(max(3, width // 4)):
        cx = rng.randrange(3, width - 3)
        cy = rng.randrange(3, height - 3)
        rx = rng.choice((2, 2, 3))
        ry = rng.choice((1, 2, 2))
        draw.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=rgba(mix(palette.base, palette.accent, 0.32)), outline=rgba(palette.dark, 210))
        draw.point((cx - 1, cy - 1), fill=rgba(palette.glow, 210))


def draw_nexus_language(draw: ImageDraw.ImageDraw, rng: random.Random, width: int, height: int, palette: Palette) -> None:
    for i in range(3):
        x0 = rng.randrange(0, max(1, width // 3))
        y0 = rng.randrange(0, height)
        x1 = min(width - 1, x0 + rng.randrange(width // 2, width))
        y1 = max(0, min(height - 1, y0 + rng.choice((-1, 1)) * rng.randrange(height // 3, max(height // 3 + 1, height))))
        draw.line((x0, y0, x1, y1), fill=rgba(palette.accent, 230))
        draw.line((x0, y0 + 1, x1, min(height - 1, y1 + 1)), fill=rgba(palette.dark, 140))
    draw_glow(draw, (width // 2, height // 2), max(3, width // 4), palette.glow)


def draw_wood_language(draw: ImageDraw.ImageDraw, rng: random.Random, width: int, height: int, palette: Palette) -> None:
    stripe = max(3, width // 5)
    for x in range(0, width, stripe):
        fill = mix(palette.base, palette.dark if (x // stripe) % 2 else palette.light, 0.18)
        draw.rectangle((x, 0, min(width - 1, x + stripe - 1), height - 1), fill=rgba(fill), outline=rgba(palette.dark, 140))
        for y in range(2, height, 5):
            draw.line((x + 1, y, min(width - 1, x + stripe - 2), y), fill=rgba(mix(palette.dark, palette.base, 0.35), 155))
    for _ in range(max(2, width // 4)):
        x = rng.randrange(width)
        y = rng.randrange(height)
        draw.point((x, y), fill=rgba(palette.light, 180))


def png_dimensions(path: Path) -> tuple[int, int]:
    with Image.open(path) as img:
        return img.size


def texture_kind_from_path(path: Path, texture_root: Path) -> str:
    parts = path.relative_to(texture_root).parts
    if parts[0] == "models" and len(parts) > 1 and parts[1] == "armor":
        return "armor"
    return parts[0]


def default_dimensions(kind: str, name: str, target: TextureTarget) -> tuple[int, int]:
    if kind == "block":
        return (16, 16)
    if kind == "item":
        return (16, 16)
    if kind == "gui":
        return (256, 166)
    if kind == "armor":
        return (64, 32)
    if kind == "entity":
        return entity_dimensions(target.key, name)
    return (64, 64)


def entity_dimensions(target_key: str, name: str) -> tuple[int, int]:
    if target_key == "ashfall":
        if name in ASHFALL_BOSS_ENTITIES or name in ASHFALL_GUARDIAN_GLOW_ENTITIES:
            return (128, 64)
        if name in ASHFALL_DRONE_ENTITIES | ASHFALL_WOLF_ENTITIES | ASHFALL_SLIME_ENTITIES | ASHFALL_CRAWLER_ENTITIES:
            return (64, 32)
    return (64, 64)


def ids_from_json_dir(path: Path) -> set[str]:
    if not path.exists():
        return set()
    return {file.stem for file in path.glob("*.json")}


def discover_specs(target: TextureTarget) -> list[TextureSpec]:
    paths: dict[str, Path] = {}
    if target.textures.exists():
        for path in target.textures.rglob("*.png"):
            paths[path.relative_to(target.root).as_posix()] = path

    for entity_id in sorted(EXTRA_ENTITY_TEXTURES.get(target.key, set())):
        path = target.textures / "entity" / f"{entity_id}.png"
        paths[path.relative_to(target.root).as_posix()] = path

    if target.key == "orbital":
        block_ids = ids_from_json_dir(target.block_models)
        item_ids = ids_from_json_dir(target.item_defs)
        for block_id in block_ids:
            path = target.textures / "block" / f"{block_id}.png"
            paths[path.relative_to(target.root).as_posix()] = path
        for item_id in sorted(item_ids - block_ids):
            path = target.textures / "item" / f"{item_id}.png"
            paths[path.relative_to(target.root).as_posix()] = path

    specs: list[TextureSpec] = []
    for path in sorted(paths.values(), key=lambda p: p.relative_to(target.root).as_posix()):
        kind = texture_kind_from_path(path, target.textures)
        name = path.stem
        if kind == "block":
            width, height = default_dimensions(kind, name, target)
        elif kind == "entity":
            width, height = entity_dimensions(target.key, name)
        else:
            width, height = png_dimensions(path) if path.exists() else default_dimensions(kind, name, target)
        specs.append(
            TextureSpec(
                target=target.key,
                modid=target.modid,
                kind=kind,
                name=name,
                rel_path=path.relative_to(REPO_ROOT).as_posix(),
                path=path,
                width=width,
                height=height,
            )
        )
    return specs


def is_cutout_block(name: str) -> bool:
    if name.endswith("_grass_block") or "wasteland_grass_block" in name:
        return False
    if "leaves" in name:
        return False
    return (
        any(word in name for word in PLANT_WORDS)
        or any(word in name for word in ("glass", "layer", "puddle", "crystal_cluster"))
    )


def is_machine_block(name: str) -> bool:
    return name in ADVANCED_BLOCKS or any(word in name for word in MACHINE_WORDS)


def block_role(name: str) -> str:
    if "glass" in name:
        return "glass"
    if "layer" in name or "puddle" in name:
        return "layer"
    if is_cutout_block(name):
        return "plant"
    if is_machine_block(name):
        return "machine"
    if "nexus" in name or "anomaly" in name or "choir" in name:
        return "nexus"
    if "ore" in name or has_word(name, CRYSTAL_WORDS):
        return "ore"
    if has_word(name, ORGANIC_WORDS):
        return "organic"
    if has_word(name, METAL_WORDS) or has_word(name, TECH_WORDS):
        return "metal"
    if "sand" in name or "regolith" in name:
        return "sand"
    if "ash" in name or "dust" in name or "wasteland" in name:
        return "ash"
    if "rubble" in name or "debris" in name:
        return "rubble"
    if has_word(name, WOOD_WORDS):
        return "wood"
    if has_word(name, SOIL_WORDS):
        return "terrain"
    if has_word(name, STONE_WORDS):
        return "stone"
    return "stone"


def role_palette(name: str, role: str) -> RolePalette:
    palette = palette_for(name)
    presets = {
        "ash": ((69, 68, 61), (88, 86, 76), (112, 107, 92), (129, 123, 105), (154, 145, 121)),
        "terrain": ((72, 60, 47), (92, 75, 56), (119, 95, 68), (139, 113, 79), (163, 136, 95)),
        "sand": ((105, 93, 70), (130, 113, 82), (156, 139, 99), (181, 164, 121), (205, 188, 145)),
        "stone": ((61, 63, 61), (83, 86, 82), (105, 108, 103), (126, 129, 123), (151, 153, 145)),
        "rubble": ((58, 58, 54), (80, 79, 72), (105, 101, 89), (127, 120, 101), (151, 141, 116)),
        "ore": ((58, 60, 58), (80, 83, 79), (104, 108, 101), (126, 130, 121), (151, 153, 143)),
        "metal": ((42, 49, 53), (61, 71, 76), (88, 101, 106), (113, 127, 131), (150, 161, 161)),
        "machine": ((35, 42, 46), (54, 65, 70), (82, 96, 102), (111, 126, 130), (151, 164, 162)),
        "organic": ((53, 66, 42), (69, 88, 51), (92, 117, 63), (117, 143, 78), (147, 169, 96)),
        "nexus": ((38, 28, 49), (58, 42, 78), (82, 57, 112), (107, 75, 142), (142, 98, 177)),
        "wood": ((69, 47, 31), (92, 62, 38), (122, 81, 47), (148, 101, 60), (178, 127, 77)),
        "plant": ((36, 62, 33), (55, 88, 44), (75, 117, 54), (101, 145, 68), (134, 171, 84)),
        "glass": ((35, 72, 82), (53, 100, 112), (72, 133, 148), (102, 169, 180), (150, 210, 217)),
    }
    shadow, dark, base, mid, light = presets.get(role, presets["stone"])
    accent = mix(palette.accent, mid, 0.35)
    glow = mix(palette.glow, light, 0.25)
    if role in {"ash", "terrain", "sand", "stone", "rubble"}:
        accent = mix(accent, base, 0.55)
        glow = mix(glow, light, 0.55)
    return RolePalette(shadow, dark, base, mid, light, accent, glow)


def draw_soft_patch(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    fill: tuple[int, int, int],
    light: tuple[int, int, int],
    shadow: tuple[int, int, int],
) -> None:
    x0, y0, x1, y1 = xy
    draw.rectangle(xy, fill=rgba(fill))
    if x1 > x0:
        draw.line((x0, y0, x1, y0), fill=rgba(mix(fill, light, 0.35)))
    if y1 > y0:
        draw.line((x0, y0, x0, y1), fill=rgba(mix(fill, light, 0.22)))
    if x1 > x0 and y1 > y0:
        draw.point((x1, y1), fill=rgba(mix(fill, shadow, 0.35)))


def draw_vanilla_base(draw: ImageDraw.ImageDraw, colors: RolePalette, width: int, height: int) -> None:
    draw.rectangle((0, 0, width - 1, height - 1), fill=rgba(colors.base))
    for x, y, color in (
        (3, 2, mix(colors.base, colors.light, 0.20)),
        (12, 4, mix(colors.base, colors.dark, 0.16)),
        (5, 11, mix(colors.base, colors.shadow, 0.14)),
        (10, 13, mix(colors.base, colors.mid, 0.18)),
    ):
        if x < width and y < height:
            draw.point((x, y), fill=rgba(color))


def scatter_template(
    draw: ImageDraw.ImageDraw,
    rng: random.Random,
    colors: RolePalette,
    width: int,
    height: int,
    templates: tuple[tuple[int, int, int, int, str], ...],
    jitter: int = 1,
) -> None:
    palette_map = {
        "shadow": colors.shadow,
        "dark": colors.dark,
        "base": colors.base,
        "mid": colors.mid,
        "light": colors.light,
        "accent": colors.accent,
        "glow": colors.glow,
    }
    for x, y, w, h, key in templates:
        jx = rng.randrange(-jitter, jitter + 1) if jitter else 0
        jy = rng.randrange(-jitter, jitter + 1) if jitter else 0
        x0 = max(1, min(width - 2, x + jx))
        y0 = max(1, min(height - 2, y + jy))
        x1 = max(x0, min(width - 2, x0 + w - 1))
        y1 = max(y0, min(height - 2, y0 + h - 1))
        draw_soft_patch(draw, (x0, y0, x1, y1), palette_map[key], colors.light, colors.shadow)


def draw_vanilla_soil(draw: ImageDraw.ImageDraw, rng: random.Random, name: str, colors: RolePalette, width: int, height: int) -> None:
    draw_vanilla_base(draw, colors, width, height)
    scatter_template(
        draw,
        rng,
        colors,
        width,
        height,
        (
            (2, 2, 3, 2, "mid"),
            (8, 1, 4, 3, "dark"),
            (4, 6, 4, 2, "shadow"),
            (11, 7, 3, 3, "mid"),
            (1, 11, 4, 2, "dark"),
            (7, 12, 5, 2, "mid"),
        ),
    )
    for x, y in ((3, 5), (13, 3), (6, 10), (10, 14)):
        draw.point((x, y), fill=rgba(colors.light if rng.random() < 0.55 else colors.dark))
    if "grass" in name or "moss" in name or "wasteland" in name:
        moss = mix((70, 104, 55), colors.accent, 0.18)
        for x in (0, 4, 9, 13):
            draw.line((x, 0, min(width - 1, x + 2), 0), fill=rgba(moss))
        for x, y in ((2, 3), (12, 4), (6, 13)):
            draw.rectangle((x, y, min(width - 1, x + 1), y), fill=rgba(moss))


def draw_vanilla_stone(draw: ImageDraw.ImageDraw, rng: random.Random, colors: RolePalette, width: int, height: int, cracked: bool = True) -> None:
    draw_vanilla_base(draw, colors, width, height)
    scatter_template(
        draw,
        rng,
        colors,
        width,
        height,
        (
            (1, 2, 4, 3, "dark"),
            (7, 1, 3, 2, "light"),
            (11, 3, 4, 3, "mid"),
            (4, 7, 5, 3, "shadow"),
            (10, 10, 4, 3, "dark"),
            (2, 12, 3, 2, "mid"),
        ),
    )
    if cracked:
        x, y = rng.choice(((5, 1), (9, 2), (4, 4)))
        for dx, dy in ((0, 0), (0, 1), (1, 1), (1, 2), (2, 2), (2, 3)):
            if 0 <= x + dx < width and 0 <= y + dy < height:
                draw.point((x + dx, y + dy), fill=rgba(colors.shadow))


def draw_vanilla_ore(draw: ImageDraw.ImageDraw, rng: random.Random, colors: RolePalette, width: int, height: int) -> None:
    draw_vanilla_stone(draw, rng, colors, width, height, cracked=False)
    deposits = rng.sample(
        [(3, 3), (10, 2), (6, 7), (12, 8), (2, 11), (8, 12)],
        3,
    )
    for x, y in deposits:
        draw.rectangle((x, y, x + 1, y + 1), fill=rgba(colors.accent))
        draw.point((x, y), fill=rgba(colors.glow))
        if rng.random() < 0.65:
            draw.point((min(width - 1, x + 2), y + 1), fill=rgba(mix(colors.accent, colors.base, 0.25)))


def draw_vanilla_metal(draw: ImageDraw.ImageDraw, rng: random.Random, colors: RolePalette, width: int, height: int) -> None:
    draw.rectangle((0, 0, width - 1, height - 1), fill=rgba(colors.base))
    draw.line((0, 0, width - 1, 0), fill=rgba(colors.light))
    draw.line((0, height - 1, width - 1, height - 1), fill=rgba(colors.shadow))
    for y in (4, 10):
        draw.line((0, y, width - 1, y), fill=rgba(colors.dark, 190))
        draw.line((1, y + 1, width - 2, y + 1), fill=rgba(mix(colors.base, colors.light, 0.22), 130))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (7, 6), (9, 11)):
        draw.point((x, y), fill=rgba(colors.light))
        draw.point((min(width - 1, x + 1), y), fill=rgba(colors.shadow))
    for x, y in ((5, 8), (11, 5)):
        draw.line((x, y, min(width - 1, x + 2), y + 1), fill=rgba(mix(colors.base, colors.shadow, 0.35), 150))


def draw_vanilla_machine(draw: ImageDraw.ImageDraw, rng: random.Random, name: str, colors: RolePalette, width: int, height: int) -> None:
    draw_vanilla_metal(draw, rng, colors, width, height)
    if any(word in name for word in ("terminal", "console", "navigation", "scanner", "controller", "beacon")):
        draw.rectangle((4, 3, 11, 7), fill=rgba(mix(colors.dark, colors.accent, 0.25)), outline=rgba(colors.shadow))
        draw.line((5, 4, 10, 4), fill=rgba(colors.glow))
        draw.rectangle((5, 10, 6, 11), fill=rgba(colors.accent))
        draw.rectangle((9, 10, 10, 11), fill=rgba(colors.light))
    elif any(word in name for word in ("crate", "barrel", "rack", "hopper", "storage", "bank")):
        draw.rectangle((2, 2, 13, 13), fill=rgba(colors.mid), outline=rgba(colors.shadow))
        draw.line((2, 6, 13, 6), fill=rgba(colors.dark))
        draw.line((6, 2, 6, 13), fill=rgba(colors.dark))
        draw.line((3, 3, 12, 3), fill=rgba(colors.light))
        draw.point((10, 10), fill=rgba(colors.accent))
    elif any(word in name for word in ("pipe", "cable", "conduit")):
        draw.rectangle((0, 6, 15, 10), fill=rgba(colors.dark))
        draw.rectangle((0, 7, 15, 8), fill=rgba(colors.mid))
        draw.line((0, 7, 15, 7), fill=rgba(colors.light))
        for x in (3, 8, 13):
            draw.rectangle((x, 4, x + 1, 12), fill=rgba(colors.shadow))
    elif any(word in name for word in ("fuel", "tank", "refinery", "oxygen", "life_support", "purifier")):
        draw.rectangle((5, 2, 10, 13), fill=rgba(mix(colors.base, colors.accent, 0.18)), outline=rgba(colors.shadow))
        draw.line((6, 4, 9, 4), fill=rgba(colors.light))
        draw.line((6, 10, 9, 10), fill=rgba(colors.accent))
        draw.point((8, 7), fill=rgba(colors.glow))
    elif any(word in name for word in ("fabricator", "compressor", "press", "grinder", "recycler", "synthesizer")):
        draw.rectangle((3, 3, 12, 12), fill=rgba(colors.dark), outline=rgba(colors.light))
        for y in (5, 8, 11):
            draw.line((4, y, 11, y), fill=rgba(colors.mid))
        draw.rectangle((6, 5, 9, 8), fill=rgba(mix(colors.base, colors.accent, 0.25)), outline=rgba(colors.shadow))
        draw.point((8, 6), fill=rgba(colors.glow))
    elif any(word in name for word in ("heat", "burner", "smelter", "thermal")):
        draw.rectangle((4, 4, 11, 12), fill=rgba(colors.shadow), outline=rgba(colors.light))
        for x in (5, 7, 9):
            draw.line((x, 11, x + rng.choice((-1, 0, 1)), 6), fill=rgba(colors.accent))
        draw.line((5, 12, 10, 12), fill=rgba(colors.glow))
    elif any(word in name for word in ("solar", "array")):
        for x in (3, 8):
            for y in (3, 8):
                draw.rectangle((x, y, x + 4, y + 4), fill=rgba(mix(colors.dark, colors.accent, 0.35)), outline=rgba(colors.shadow))
                draw.point((x + 1, y + 1), fill=rgba(colors.glow))
    elif any(word in name for word in ("rocket", "launch", "frame", "platform")):
        draw.line((4, 12, 11, 3), fill=rgba(colors.light))
        draw.line((4, 3, 11, 12), fill=rgba(colors.accent))
        draw.rectangle((6, 5, 9, 10), fill=rgba(colors.base), outline=rgba(colors.shadow))
    elif any(word in name for word in ("bench", "table", "station", "garden")):
        draw.rectangle((2, 4, 13, 7), fill=rgba(colors.mid), outline=rgba(colors.shadow))
        draw.rectangle((4, 8, 5, 13), fill=rgba(colors.dark))
        draw.rectangle((10, 8, 11, 13), fill=rgba(colors.dark))
        draw.line((3, 5, 12, 5), fill=rgba(colors.light))
    else:
        draw.rectangle((5, 5, 10, 10), fill=rgba(mix(colors.base, colors.accent, 0.18)), outline=rgba(colors.shadow))
        draw.line((6, 6, 9, 6), fill=rgba(colors.light))
        draw.point((8, 8), fill=rgba(colors.glow))


def draw_vanilla_organic(draw: ImageDraw.ImageDraw, rng: random.Random, colors: RolePalette, width: int, height: int) -> None:
    draw_vanilla_base(draw, colors, width, height)
    for box in ((2, 2, 5, 4), (9, 3, 13, 6), (4, 8, 8, 11), (11, 10, 14, 13), (1, 12, 4, 14)):
        draw.ellipse(box, fill=rgba(colors.mid), outline=rgba(colors.dark))
    for x, y in ((4, 3), (11, 4), (6, 9), (13, 11)):
        draw.point((x, y), fill=rgba(colors.glow))


def draw_vanilla_nexus(draw: ImageDraw.ImageDraw, rng: random.Random, colors: RolePalette, width: int, height: int) -> None:
    draw_vanilla_stone(draw, rng, colors, width, height, cracked=False)
    for line in (((2, 13), (6, 8), (5, 4)), ((11, 2), (9, 7), (13, 11))):
        last = line[0]
        for point in line[1:]:
            draw.line((*last, *point), fill=rgba(colors.accent))
            last = point
    draw.point((8, 8), fill=rgba(colors.glow))


def render_cutout_block(name: str, width: int, height: int) -> Image.Image:
    rng = stable_rng("cutout-block:" + name)
    palette = palette_for(name)
    colors = role_palette(name, block_role(name))
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")

    if "glass" in name:
        draw.rectangle((1, 1, width - 2, height - 2), fill=rgba(mix(colors.base, colors.glow, 0.2), 78))
        draw.rectangle((0, 0, width - 1, height - 1), outline=rgba(colors.glow, 215))
        draw.line((1, 1, width - 2, 1), fill=rgba(colors.light, 160))
        draw.line((2, height - 3, width - 3, height - 3), fill=rgba(colors.dark, 115))
        draw.line((4, 2, 9, 7), fill=rgba(colors.light, 95))
        return img

    if "layer" in name or "puddle" in name:
        y0 = max(1, height // 2)
        for y in range(y0, height - 1):
            color = mix(colors.base, colors.dark if y > height * 3 // 4 else colors.light, 0.18)
            draw.line((1, y, width - 2, y), fill=rgba(color, 205))
        for x in range(2, width - 2, 4):
            draw.line((x, y0 + 1, min(width - 2, x + 2), y0 + 1), fill=rgba(colors.light, 190))
            draw.point((x + 1, height - 2), fill=rgba(colors.accent, 190))
        return img

    stem_x = width // 2
    draw.line((stem_x, height - 2, stem_x, max(3, height // 3)), fill=rgba(colors.dark))
    leaf_shapes = (
        ((stem_x - 5, height - 9), (stem_x - 1, height - 11), (stem_x - 1, height - 7)),
        ((stem_x + 5, height - 10), (stem_x + 1, height - 12), (stem_x + 1, height - 8)),
        ((stem_x - 4, height - 5), (stem_x - 1, height - 7), (stem_x - 1, height - 3)),
        ((stem_x + 4, height - 6), (stem_x + 1, height - 8), (stem_x + 1, height - 4)),
        ((stem_x, height - 13), (stem_x - 2, height - 9), (stem_x + 2, height - 9)),
    )
    for i, points in enumerate(leaf_shapes):
        color = (colors.mid, colors.base, colors.light)[i % 3]
        draw.polygon(points, fill=rgba(color, 235), outline=rgba(colors.dark, 210))
    if "toxic" in name or "mutated" in name or "fungus" in name:
        for x, y in ((stem_x - 2, height // 2), (stem_x + 3, height // 2 + 3), (stem_x, height // 3)):
            draw.point((max(0, min(width - 1, x)), max(0, min(height - 1, y))), fill=rgba(colors.glow))
    return img


def render_terrain_block(name: str, width: int, height: int) -> Image.Image:
    rng = stable_rng("block:" + name)
    role = block_role(name)
    colors = role_palette(name, role)
    img = Image.new("RGBA", (width, height), rgba(colors.base))
    draw = ImageDraw.Draw(img, "RGBA")

    if role in {"ash", "terrain", "sand"}:
        draw_vanilla_soil(draw, rng, name, colors, width, height)
    elif role == "ore":
        draw_vanilla_ore(draw, rng, colors, width, height)
    elif role == "metal":
        draw_vanilla_metal(draw, rng, colors, width, height)
    elif role == "organic":
        draw_vanilla_organic(draw, rng, colors, width, height)
    elif role == "nexus":
        draw_vanilla_nexus(draw, rng, colors, width, height)
    elif role == "wood":
        draw_wood_language(draw, rng, width, height, palette_for(name))
    elif role == "rubble":
        draw_vanilla_stone(draw, rng, colors, width, height, cracked=True)
        for x, y in ((3, 12), (11, 4), (8, 9)):
            draw.rectangle((x, y, x + 1, y + 1), fill=rgba(colors.mid))
    else:
        draw_vanilla_stone(draw, rng, colors, width, height, cracked=True)
    return img


def render_machine_block(name: str, width: int, height: int) -> Image.Image:
    rng = stable_rng("machine:" + name)
    colors = role_palette(name, "machine")
    img = Image.new("RGBA", (width, height), rgba(colors.shadow))
    draw = ImageDraw.Draw(img, "RGBA")
    draw_vanilla_machine(draw, rng, name, colors, width, height)
    return img


def render_block(spec: TextureSpec) -> Image.Image:
    if is_cutout_block(spec.name):
        return render_cutout_block(spec.name, spec.width, spec.height)
    if spec.width > 16 or spec.height > 16 or is_machine_block(spec.name):
        return render_machine_block(spec.name, spec.width, spec.height)
    return render_terrain_block(spec.name, spec.width, spec.height)


def draw_weapon(draw: ImageDraw.ImageDraw, name: str, palette: Palette) -> None:
    if "hammer" in name:
        draw.rectangle((4, 2, 11, 5), fill=rgba(palette.dark), outline=rgba(palette.light))
        draw.rectangle((6, 1, 9, 6), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.line((8, 6, 5, 14), fill=rgba(palette.dark))
        draw.line((9, 6, 6, 14), fill=rgba(palette.accent))
        draw.point((7, 2), fill=rgba(palette.light))
    elif "launcher" in name or "gun" in name:
        draw.rectangle((2, 5, 13, 8), fill=rgba(palette.dark), outline=rgba(palette.light))
        draw.rectangle((4, 8, 7, 11), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.rectangle((9, 4, 14, 5), fill=rgba(palette.accent))
        draw.point((12, 5), fill=rgba(palette.glow))
    elif "lance" in name or "spear" in name:
        draw.line((3, 14, 12, 3), fill=rgba(palette.dark))
        draw.line((4, 14, 13, 3), fill=rgba(palette.light))
        draw.polygon(((12, 1), (15, 4), (11, 5)), fill=rgba(palette.accent), outline=rgba(palette.dark))
        draw.point((13, 3), fill=rgba(palette.glow))
    elif "blade" in name or "knife" in name:
        draw.polygon(((10, 1), (13, 4), (9, 12), (6, 14), (6, 9)), fill=rgba(palette.light), outline=rgba(palette.dark))
        draw.line((10, 3, 8, 11), fill=rgba(palette.accent))
        draw.rectangle((4, 12, 8, 14), fill=rgba(palette.dark))
        draw.point((11, 3), fill=rgba(palette.glow))
    else:
        draw.line((4, 13, 11, 4), fill=rgba(palette.dark))
        draw.line((5, 13, 12, 4), fill=rgba(palette.light))
        draw.rectangle((9, 3, 13, 7), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.point((12, 5), fill=rgba(palette.glow))


def draw_route_item(draw: ImageDraw.ImageDraw, name: str, palette: Palette) -> None:
    if "rocket" in name or "shuttle" in name or "vessel" in name:
        draw.polygon(((8, 0), (12, 6), (10, 12), (8, 15), (6, 12), (4, 6)), fill=rgba(palette.light), outline=rgba(palette.dark))
        draw.rectangle((6, 7, 10, 11), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.polygon(((5, 11), (2, 15), (6, 13)), fill=rgba(palette.accent), outline=rgba(palette.dark))
        draw.polygon(((11, 11), (14, 15), (10, 13)), fill=rgba(palette.accent), outline=rgba(palette.dark))
        draw.point((8, 4), fill=rgba(palette.glow))
    else:
        draw.ellipse((2, 2, 13, 13), fill=rgba(palette.dark), outline=rgba(palette.light))
        draw.arc((4, 4, 11, 11), 30, 330, fill=rgba(palette.accent), width=2)
        draw.rectangle((7, 7, 9, 9), fill=rgba(palette.glow))


def draw_suit_item(draw: ImageDraw.ImageDraw, name: str, palette: Palette) -> None:
    if "helmet" in name or "visor" in name:
        draw.rectangle((4, 4, 12, 12), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.line((5, 4, 11, 4), fill=rgba(palette.light))
        draw.rectangle((5, 6, 11, 8), fill=rgba(mix(palette.dark, palette.accent, 0.45)), outline=rgba(palette.dark))
        draw.point((10, 6), fill=rgba(palette.glow))
    elif "chestplate" in name or "liner" in name or "wrap" in name:
        draw.polygon(((5, 2), (11, 2), (13, 14), (3, 14)), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.line((6, 3, 10, 3), fill=rgba(palette.light))
        draw.line((8, 4, 8, 13), fill=rgba(palette.dark))
        draw.rectangle((6, 7, 10, 9), fill=rgba(mix(palette.base, palette.accent, 0.25)))
    elif "leggings" in name:
        draw.rectangle((4, 3, 7, 14), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.rectangle((9, 3, 12, 14), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.line((4, 4, 12, 4), fill=rgba(palette.light))
        draw.line((6, 8, 6, 13), fill=rgba(palette.dark))
        draw.line((10, 8, 10, 13), fill=rgba(palette.dark))
    elif "boots" in name:
        draw.rectangle((3, 6, 7, 12), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.rectangle((9, 6, 13, 12), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.line((3, 13, 13, 13), fill=rgba(palette.accent))
    elif "tank" in name or "booster" in name or "oxygen" in name:
        draw.rectangle((5, 2, 10, 14), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.rectangle((6, 0, 9, 2), fill=rgba(palette.dark))
        draw.line((6, 3, 9, 3), fill=rgba(palette.light))
        draw.line((6, 11, 9, 11), fill=rgba(palette.accent))
    else:
        draw.rectangle((4, 4, 12, 12), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.line((5, 8, 11, 8), fill=rgba(palette.accent))


def draw_badge(draw: ImageDraw.ImageDraw, name: str, palette: Palette) -> None:
    draw.polygon(((8, 1), (13, 4), (12, 11), (8, 15), (4, 11), (3, 4)), fill=rgba(palette.mid), outline=rgba(palette.dark))
    draw.line((5, 4, 11, 4), fill=rgba(palette.light))
    if "nexus" in name:
        draw.line((5, 5, 11, 11), fill=rgba(palette.glow))
        draw.line((11, 5, 5, 11), fill=rgba(palette.glow))
    elif "void" in name:
        draw.rectangle((5, 6, 11, 9), fill=rgba(palette.dark))
        draw.point((8, 7), fill=rgba(palette.glow))
    else:
        draw.rectangle((6, 4, 10, 12), fill=rgba(palette.accent))
        draw.line((4, 8, 12, 8), fill=rgba(palette.glow))


def draw_component(draw: ImageDraw.ImageDraw, name: str, palette: Palette) -> None:
    if "engine" in name:
        draw.rectangle((5, 2, 11, 12), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.line((6, 3, 10, 3), fill=rgba(palette.light))
        draw.rectangle((6, 6, 10, 9), fill=rgba(mix(palette.dark, palette.accent, 0.28)))
        draw.polygon(((5, 12), (8, 15), (11, 12)), fill=rgba(palette.glow), outline=rgba(palette.dark))
    elif "gear" in name:
        draw.rectangle((3, 4, 13, 7), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.line((4, 5, 12, 5), fill=rgba(palette.light))
        draw.rectangle((4, 8, 6, 13), fill=rgba(palette.dark))
        draw.rectangle((10, 8, 12, 13), fill=rgba(palette.dark))
    elif any(word in name for word in ("computer", "chip", "circuit", "cell", "module")):
        draw.rectangle((3, 3, 13, 13), fill=rgba(palette.dark), outline=rgba(palette.accent))
        draw.rectangle((5, 5, 11, 11), fill=rgba(mix(palette.base, palette.accent, 0.25)), outline=rgba(palette.dark))
        for x in (4, 12):
            for y in (5, 8, 11):
                draw.point((x, y), fill=rgba(palette.light))
        draw.point((8, 8), fill=rgba(palette.glow))
    elif "membrane" in name or "filter" in name:
        draw.rectangle((3, 3, 13, 13), fill=rgba(mix(palette.base, palette.accent, 0.18), 220), outline=rgba(palette.dark))
        for y in range(5, 12, 3):
            draw.line((4, y, 12, y), fill=rgba(palette.glow))
    else:
        draw.rectangle((3, 4, 12, 12), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.line((4, 5, 11, 5), fill=rgba(palette.light))
        draw.rectangle((6, 7, 10, 10), fill=rgba(palette.dark), outline=rgba(palette.accent))


def polish_item_pixels(img: Image.Image, palette: Palette) -> Image.Image:
    out = img.convert("RGBA")
    src = out.copy()
    px = src.load()
    shadow = Image.new("RGBA", out.size, (0, 0, 0, 0))
    spx = shadow.load()
    width, height = out.size

    for y in range(height):
        for x in range(width):
            if px[x, y][3] < 32:
                continue
            for nx, ny in ((x + 1, y), (x, y + 1), (x + 1, y + 1)):
                if nx < width and ny < height and px[nx, ny][3] < 32:
                    spx[nx, ny] = rgba(palette.dark, 150)

    out = Image.alpha_composite(shadow, out)
    px = out.load()
    for y in range(height):
        for x in range(width):
            r, g, b, a = px[x, y]
            if a < 32:
                continue
            exposed_top = y == 0 or px[x, y - 1][3] < 32
            exposed_left = x == 0 or px[x - 1, y][3] < 32
            exposed_bottom = y == height - 1 or px[x, y + 1][3] < 32
            exposed_right = x == width - 1 or px[x + 1, y][3] < 32
            if exposed_top or exposed_left:
                px[x, y] = rgba(mix((r, g, b), palette.light, 0.35), a)
            elif exposed_bottom or exposed_right:
                px[x, y] = rgba(mix((r, g, b), palette.dark, 0.30), a)
    return out


def draw_resource(draw: ImageDraw.ImageDraw, name: str, palette: Palette, rng: random.Random) -> None:
    if "ingot" in name or "bar" in name:
        draw.polygon(((4, 7), (8, 4), (13, 6), (12, 10), (7, 13), (3, 10)), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.line((6, 6, 11, 7), fill=rgba(palette.light))
        draw.line((5, 10, 10, 11), fill=rgba(mix(palette.dark, palette.base, 0.4)))
    elif "shard" in name or "crystal" in name:
        draw.polygon(((9, 1), (13, 5), (10, 15), (5, 13), (3, 8)), fill=rgba(palette.accent), outline=rgba(palette.dark))
        draw.line((9, 2, 8, 13), fill=rgba(palette.light))
        draw.line((6, 8, 12, 5), fill=rgba(palette.glow))
        draw.point((7, 11), fill=rgba(mix(palette.dark, palette.accent, 0.3)))
    elif "scrap" in name or "plate" in name:
        draw.polygon(((3, 5), (10, 3), (13, 7), (11, 12), (4, 13)), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.rectangle((5, 6, 12, 8), fill=rgba(mix(palette.base, palette.light, 0.25)), outline=rgba(palette.dark))
        draw.line((4, 11, 10, 4), fill=rgba(palette.accent))
    elif "dust" in name or "powder" in name or "ash" in name:
        for box, color in (
            ((4, 9, 11, 12), palette.mid),
            ((5, 7, 8, 9), palette.light),
            ((9, 8, 12, 10), palette.dark),
        ):
            draw.rectangle(box, fill=rgba(color), outline=rgba(palette.dark, 180))
        draw.point((7, 6), fill=rgba(palette.accent))
        draw.point((11, 7), fill=rgba(palette.light))
    else:
        points = ((8, 2), (12, 5), (13, 10), (9, 14), (4, 13), (2, 8), (4, 4))
        draw.polygon(points, fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.polygon(((7, 3), (11, 6), (8, 8), (5, 7)), fill=rgba(mix(palette.mid, palette.light, 0.42)))
        draw.line((5, 12, 12, 5), fill=rgba(mix(palette.dark, palette.base, 0.25)))
        draw.point((9, 6), fill=rgba(palette.glow))


def draw_food_or_bottle(draw: ImageDraw.ImageDraw, name: str, palette: Palette) -> None:
    if any(word in name for word in ("bottle", "vial", "water", "rad_away", "stim")):
        liquid = mix(palette.base, palette.accent, 0.35)
        draw.rectangle((6, 1, 9, 3), fill=rgba(palette.dark))
        draw.rectangle((5, 4, 10, 13), fill=rgba(mix(liquid, palette.light, 0.2), 180), outline=rgba(palette.dark))
        draw.rectangle((6, 8, 9, 12), fill=rgba(liquid, 230))
        draw.point((6, 5), fill=rgba(palette.light, 220))
        draw.point((9, 10), fill=rgba(palette.glow, 220))
    else:
        draw.ellipse((3, 4, 12, 13), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.arc((4, 5, 11, 12), 210, 330, fill=rgba(palette.light))
        draw.rectangle((5, 3, 10, 5), fill=rgba(palette.dark))
        draw.point((8, 8), fill=rgba(palette.glow))


def draw_document(draw: ImageDraw.ImageDraw, name: str, palette: Palette) -> None:
    paper = mix((224, 214, 176), palette.base, 0.18)
    draw.polygon(((4, 2), (10, 2), (13, 5), (13, 14), (3, 14), (3, 3)), fill=rgba(paper), outline=rgba(palette.dark))
    draw.line((10, 2, 13, 5), fill=rgba(mix(paper, palette.dark, 0.25)))
    draw.line((5, 3, 9, 3), fill=rgba(mix(paper, (255, 255, 255), 0.45)))
    for y in (6, 8, 10):
        draw.line((5, y, 11, y), fill=rgba(palette.accent))
    if "nexus" in name or "schematic" in name:
        draw.point((8, 12), fill=rgba(palette.glow))


def render_item(spec: TextureSpec) -> Image.Image:
    rng = stable_rng("item:" + spec.name)
    palette = palette_for(spec.name)
    img = Image.new("RGBA", (spec.width, spec.height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")

    name = spec.name
    if name.endswith("_spawn_egg"):
        draw.ellipse((3, 1, 12, 14), fill=rgba(palette.mid), outline=rgba(palette.dark))
        draw.arc((4, 2, 11, 13), 190, 300, fill=rgba(palette.light))
        for x, y, color in (
            (6, 5, palette.glow),
            (10, 8, palette.accent),
            (7, 11, palette.dark),
            (9, 3, palette.light),
        ):
            draw.point((x, y), fill=rgba(color))
    elif any(word in name for word in WEAPON_WORDS):
        draw_weapon(draw, name, palette)
    elif name in ROUTE_ITEMS or any(word in name for word in ("transfer_window", "rocket", "shuttle", "vessel")):
        draw_route_item(draw, name, palette)
    elif any(word in name for word in SUIT_WORDS):
        draw_suit_item(draw, name, palette)
    elif any(word in name for word in ("badge", "marker", "sigil", "token")):
        draw_badge(draw, name, palette)
    elif any(word in name for word in ("bottle", "vial", "water", "ration", "food", "stim", "rad_away")):
        draw_food_or_bottle(draw, name, palette)
    elif any(word in name for word in ("schematic", "data_log", "book", "note", "map", "key")):
        draw_document(draw, name, palette)
    elif any(word in name for word in COMPONENT_WORDS):
        draw_component(draw, name, palette)
    else:
        draw_resource(draw, name, palette, rng)

    return polish_item_pixels(img, palette).filter(ImageFilter.UnsharpMask(radius=0.35, percent=105, threshold=0))


def entity_group(spec: TextureSpec) -> str:
    if spec.target == "ashfall":
        if spec.name in ASHFALL_GUARDIAN_GLOW_ENTITIES:
            return "guardian_glow"
        if spec.name in ASHFALL_BOSS_ENTITIES:
            return "boss"
        if spec.name in ASHFALL_DRONE_ENTITIES:
            return "drone"
        if spec.name in ASHFALL_WOLF_ENTITIES:
            return "wolf"
        if spec.name in ASHFALL_SLIME_ENTITIES:
            return "slime"
        if spec.name in ASHFALL_CRAWLER_ENTITIES:
            return "crawler"
    if spec.target == "orbital" and spec.name in ORBITAL_VEX_ENTITIES:
        return "vex"
    return "humanoid"


def guardian_base_name(name: str) -> str:
    return name[:-5] if name.endswith("_glow") else name


def guardian_variant(name: str) -> str:
    return GUARDIAN_VARIANTS.get(guardian_base_name(name), "warden")


def rect(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], color: tuple[int, int, int], outline: tuple[int, int, int] | None = None, alpha: int = 255) -> None:
    draw.rectangle(xy, fill=rgba(color, alpha), outline=rgba(outline or color, alpha))


def detail(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], palette: Palette, rng: random.Random, density: int = 4) -> None:
    x0, y0, x1, y1 = xy
    for _ in range(max(1, ((x1 - x0 + 1) * (y1 - y0 + 1)) // max(1, density))):
        x = rng.randrange(x0, x1 + 1)
        y = rng.randrange(y0, y1 + 1)
        roll = rng.random()
        if roll < 0.20:
            color = palette.light
        elif roll < 0.42:
            color = palette.dark
        elif roll < 0.58:
            color = palette.accent
        else:
            color = mix(palette.base, palette.dark if rng.random() < 0.5 else palette.light, 0.35)
        if rng.random() < 0.58 and x + 1 <= x1:
            draw.line((x, y, x + 1, y), fill=rgba(color, 215))
            if y + 1 <= y1 and rng.random() < 0.25:
                draw.point((x, y + 1), fill=rgba(mix(color, palette.dark, 0.35), 180))
        elif y + 1 <= y1:
            draw.line((x, y, x, y + 1), fill=rgba(color, 205))
        else:
            draw.point((x, y), fill=rgba(color, 205))


def panel(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], palette: Palette, color: tuple[int, int, int], rng: random.Random, accent: bool = False) -> None:
    rect(draw, xy, color, palette.dark)
    x0, y0, x1, y1 = xy
    if x1 - x0 > 3 and y1 - y0 > 3:
        draw.line((x0 + 1, y0 + 1, x1 - 1, y0 + 1), fill=rgba(palette.light, 170))
        draw.line((x0 + 1, y1 - 1, x1 - 1, y1 - 1), fill=rgba(palette.dark, 180))
        draw.line((x0 + 1, y0 + 2, x0 + 1, y1 - 1), fill=rgba(mix(color, palette.light, 0.35), 120))
        draw.line((x1 - 1, y0 + 2, x1 - 1, y1 - 1), fill=rgba(mix(color, palette.dark, 0.35), 140))
        if accent:
            draw.line((x0 + 1, y0 + 2, x1 - 1, y0 + 2), fill=rgba(palette.glow, 210))
    detail(draw, xy, palette, rng, density=22 if accent else 28)


def chip_marks(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], palette: Palette, rng: random.Random, count: int) -> None:
    x0, y0, x1, y1 = xy
    if x1 <= x0 or y1 <= y0:
        return
    for _ in range(count):
        x = rng.randrange(x0, x1 + 1)
        y = rng.randrange(y0, y1 + 1)
        roll = rng.random()
        if roll < 0.45 and x + 2 <= x1:
            draw.line((x, y, x + 2, y), fill=rgba(palette.dark, 220))
            draw.point((x, y), fill=rgba(mix(palette.dark, palette.base, 0.35), 190))
        elif roll < 0.78 and y + 2 <= y1:
            draw.line((x, y, x, y + 2), fill=rgba(palette.light, 195))
            draw.point((x, y + 2), fill=rgba(palette.dark, 160))
        elif x + 1 <= x1 and y + 1 <= y1:
            draw.rectangle((x, y, x + 1, y + 1), fill=rgba(mix(palette.base, palette.accent, 0.32), 190), outline=rgba(palette.dark, 150))
        else:
            draw.point((x, y), fill=rgba(palette.accent, 205))


def draw_eye_pair(
    draw: ImageDraw.ImageDraw,
    left: tuple[int, int],
    right: tuple[int, int],
    glow: tuple[int, int, int],
    socket: tuple[int, int, int],
    bright: bool = True,
) -> None:
    for x, y in (left, right):
        draw.rectangle((x - 1, y - 1, x + 1, y + 1), fill=rgba(socket, 245))
        draw.point((x, y), fill=rgba(glow, 255 if bright else 220))
        if bright:
            draw.point((x + 1, y), fill=rgba(mix(glow, (255, 255, 255), 0.45), 245))


def draw_humanoid_face(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    skin: tuple[int, int, int],
    palette: Palette,
    boss: bool,
) -> None:
    x0, y0, x1, y1 = xy
    rect(draw, xy, skin, palette.dark)
    draw.line((x0 + 1, y0 + 1, x1 - 1, y0 + 1), fill=rgba(mix(skin, palette.light, 0.45), 210))
    draw.line((x0 + 1, y1 - 1, x1 - 1, y1 - 1), fill=rgba(mix(skin, palette.dark, 0.45), 230))
    socket = mix(palette.dark, (0, 0, 0), 0.45)
    if boss:
        draw.rectangle((x0 + 1, y0 + 2, x1 - 1, y0 + 4), fill=rgba(socket, 245))
        draw.rectangle((x0 + 2, y0 + 3, x1 - 2, y0 + 3), fill=rgba(palette.glow, 255))
        draw.point((x1 - 1, y0 + 3), fill=rgba(mix(palette.glow, (255, 255, 255), 0.5), 255))
    else:
        draw_eye_pair(draw, (x0 + 2, y0 + 3), (x1 - 2, y0 + 3), palette.glow, socket)
        draw.line((x0 + 3, y0 + 6, x1 - 3, y0 + 6), fill=rgba(socket, 230))
    draw.point((x0 + 1, y0 + 5), fill=rgba(mix(skin, palette.light, 0.35), 220))
    draw.point((x1 - 1, y0 + 5), fill=rgba(mix(skin, palette.dark, 0.35), 220))


def draw_helmet_face(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    helmet: tuple[int, int, int],
    palette: Palette,
    boss: bool,
) -> None:
    x0, y0, x1, y1 = xy
    rect(draw, xy, helmet, palette.dark)
    draw.line((x0 + 1, y0 + 1, x1 - 1, y0 + 1), fill=rgba(palette.light, 190))
    socket = mix(palette.dark, (0, 0, 0), 0.55)
    if boss:
        draw.rectangle((x0 + 1, y0 + 2, x1 - 1, y0 + 4), fill=rgba(socket, 245))
        draw.rectangle((x0 + 2, y0 + 3, x1 - 2, y0 + 3), fill=rgba(palette.glow, 255))
    else:
        draw.rectangle((x0 + 1, y0 + 2, x0 + 3, y0 + 4), fill=rgba(socket, 245))
        draw.rectangle((x1 - 3, y0 + 2, x1 - 1, y0 + 4), fill=rgba(socket, 245))
        draw.point((x0 + 2, y0 + 3), fill=rgba(palette.glow, 255))
        draw.point((x1 - 2, y0 + 3), fill=rgba(palette.glow, 255))
    draw.line((x0 + 2, y1 - 2, x1 - 2, y1 - 2), fill=rgba(palette.dark, 210))


def draw_body_language(
    draw: ImageDraw.ImageDraw,
    palette: Palette,
    rng: random.Random,
    body_xy: tuple[int, int, int, int],
    accent: tuple[int, int, int],
) -> None:
    x0, y0, x1, y1 = body_xy
    inset = 2 if x1 - x0 <= 9 else 4
    draw.rectangle((x0 + inset, y0 + 2, x1 - inset, y0 + 3), fill=rgba(mix(palette.dark, accent, 0.25), 230))
    core_inset = 2 if x1 - x0 <= 9 else 4
    draw.rectangle((x0 + core_inset, y0 + 5, x1 - core_inset, y0 + 8), outline=rgba(accent, 230), fill=rgba(mix(palette.base, accent, 0.18), 180))
    draw.line((x0 + 2, y1 - 3, x1 - 2, y1 - 3), fill=rgba(palette.dark, 230))
    chip_marks(draw, body_xy, palette, rng, 3)


def render_humanoid_entity(spec: TextureSpec, palette: Palette, rng: random.Random, boss: bool = False) -> Image.Image:
    img = Image.new("RGBA", (spec.width, spec.height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    suit = mix(palette.base, (86, 88, 82), 0.35)
    armor = mix(palette.base, (148, 152, 150), 0.35)
    cloth = mix(palette.dark, palette.base, 0.35)
    glow = palette.glow
    variant = guardian_variant(spec.name) if boss and spec.width >= 128 else "humanoid"

    panel(draw, (0, 0, 31, 15), palette, mix(suit, palette.mid, 0.35), rng, True)
    panel(draw, (32, 0, 63, 15), palette, mix(armor, glow, 0.12), rng, boss)
    panel(draw, (16, 16, 39, 31), palette, suit, rng, boss)
    panel(draw, (40, 16, 55, 31), palette, armor, rng, False)
    panel(draw, (32, 48, 47, 63), palette, mix(armor, palette.dark, 0.2), rng, False)
    panel(draw, (0, 16, 15, 31), palette, cloth, rng, False)
    panel(draw, (16, 48, 31, 63), palette, mix(cloth, palette.dark, 0.25), rng, False)

    if spec.width >= 128:
        panel(draw, (64, 16, 95, 31), palette, mix(armor, palette.dark, 0.18), rng, True)
        panel(draw, (64, 32, 91, 47), palette, mix(suit, glow, 0.18), rng, True)
        panel(draw, (96, 16, 123, 47), palette, mix(palette.dark, glow, 0.18), rng, False)
        for x in range(70, 119, 8):
            draw.line((x, 19, x + 4, 29), fill=rgba(glow, 220))
        draw_guardian_variant_base(draw, palette, rng, variant)

    face = mix(palette.base, (142, 132, 104), 0.45)
    draw_humanoid_face(draw, (8, 8, 15, 15), face, palette, boss)
    draw_helmet_face(draw, (40, 8, 47, 15), mix(armor, palette.dark, 0.12), palette, boss)
    if boss:
        draw_humanoid_face(draw, (9, 9, 17, 17), face, palette, True)
        draw_helmet_face(draw, (41, 9, 49, 17), mix(armor, glow, 0.08), palette, True)

    draw_body_language(draw, palette, rng, (20, 20, 27, 31), glow)
    draw_body_language(draw, palette, rng, (18, 20, 35, 31), glow if boss else palette.accent)
    draw.rectangle((44, 20, 47, 31), fill=rgba(mix(armor, palette.dark, 0.15)), outline=rgba(palette.dark))
    draw.rectangle((4, 20, 7, 31), fill=rgba(mix(cloth, palette.dark, 0.15)), outline=rgba(palette.dark))
    for y in (23, 28):
        draw.line((44, y, 47, y), fill=rgba(palette.light, 185))
        draw.line((4, y, 7, y), fill=rgba(palette.accent, 185))
    for region in (
        (0, 0, 31, 15),
        (32, 0, 63, 15),
        (16, 16, 39, 31),
        (40, 16, 55, 31),
        (0, 16, 15, 31),
        (16, 48, 31, 63),
        (32, 48, 47, 63),
    ):
        if region[2] < spec.width and region[3] < spec.height:
            chip_marks(draw, region, palette, rng, 2)
    return img


def draw_guardian_variant_base(draw: ImageDraw.ImageDraw, palette: Palette, rng: random.Random, variant: str) -> None:
    glow = palette.glow
    metal = mix(palette.base, (128, 130, 126), 0.45)
    hazard = mix(glow, palette.accent, 0.35)

    if variant == "sentinel":
        panel(draw, (96, 0, 107, 15), palette, mix(metal, glow, 0.16), rng, True)
        draw.rectangle((100, 0, 103, 2), fill=rgba(glow))
        draw.line((18, 18, 36, 18), fill=rgba(glow, 230))
    elif variant == "warlord":
        panel(draw, (64, 0, 91, 15), palette, mix(metal, hazard, 0.18), rng, True)
        for x in range(66, 90, 6):
            draw.line((x, 3, x + 3, 12), fill=rgba(hazard, 220))
    elif variant == "stalker":
        panel(draw, (112, 0, 123, 15), palette, mix(palette.dark, glow, 0.14), rng, True)
        draw.line((9, 9, 17, 9), fill=rgba(glow, 230))
        draw.line((112, 2, 123, 14), fill=rgba(glow, 190))
    elif variant == "juggernaut":
        panel(draw, (64, 36, 91, 47), palette, mix(metal, palette.dark, 0.18), rng, True)
        panel(draw, (96, 16, 123, 31), palette, mix(metal, hazard, 0.16), rng, True)
        draw.line((68, 39, 88, 39), fill=rgba(hazard, 230))
    elif variant == "matriarch":
        panel(draw, (96, 32, 123, 47), palette, mix(palette.base, glow, 0.28), rng, True)
        for cx, cy in ((101, 37), (113, 41), (119, 35)):
            draw.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=rgba(glow, 190), outline=rgba(palette.light, 180))
    elif variant == "colossus":
        panel(draw, (96, 48, 123, 63), palette, mix(metal, hazard, 0.22), rng, True)
        draw.line((98, 50, 121, 61), fill=rgba(glow, 220))
        draw.line((105, 48, 117, 63), fill=rgba(palette.dark, 190))
    elif variant == "behemoth":
        panel(draw, (112, 16, 123, 27), palette, mix(palette.dark, glow, 0.24), rng, True)
        draw.rectangle((115, 19, 120, 24), fill=rgba(glow, 230), outline=rgba(palette.light, 210))
        for x in (20, 26, 32):
            draw.line((x, 19, x + 3, 25), fill=rgba(hazard, 220))
    elif variant == "overseer":
        panel(draw, (96, 0, 107, 15), palette, mix(metal, glow, 0.18), rng, True)
        panel(draw, (112, 28, 127, 39), palette, mix(palette.dark, glow, 0.18), rng, True)
        draw.line((98, 2, 105, 13), fill=rgba(glow, 230))
        draw.line((114, 31, 126, 31), fill=rgba(glow, 230))
    elif variant == "nexus":
        panel(draw, (112, 32, 127, 47), palette, mix(palette.dark, glow, 0.3), rng, True)
        for y in range(33, 47, 4):
            draw.line((113, y, 126, y + 3), fill=rgba(glow, 240))
        draw.rectangle((20, 18, 34, 21), fill=rgba(glow, 215), outline=rgba(palette.light, 180))


def render_guardian_glow_entity(spec: TextureSpec, palette: Palette) -> Image.Image:
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    glow = palette.glow
    variant = guardian_variant(spec.name)

    draw.rectangle((9, 10, 16, 11), fill=rgba(glow, 255))
    draw.rectangle((18, 19, 35, 21), fill=rgba(glow, 230))
    draw.line((68, 18, 90, 18), fill=rgba(glow, 210))
    draw.line((68, 35, 90, 35), fill=rgba(glow, 180))

    if variant == "sentinel":
        draw.rectangle((100, 0, 103, 2), fill=rgba(glow, 255))
        draw.line((97, 1, 97, 13), fill=rgba(glow, 230))
        draw.line((106, 1, 106, 13), fill=rgba(glow, 230))
    elif variant == "warlord":
        for x in range(66, 90, 6):
            draw.line((x, 3, x + 3, 12), fill=rgba(glow, 235))
        draw.rectangle((18, 23, 35, 24), fill=rgba(glow, 200))
    elif variant == "stalker":
        draw.line((9, 9, 17, 9), fill=rgba(glow, 255))
        draw.line((112, 2, 123, 14), fill=rgba(glow, 230))
        draw.line((112, 7, 123, 4), fill=rgba(glow, 190))
    elif variant == "juggernaut":
        draw.line((68, 39, 88, 39), fill=rgba(glow, 245))
        draw.rectangle((100, 20, 119, 22), fill=rgba(glow, 220))
        draw.rectangle((100, 28, 119, 29), fill=rgba(glow, 180))
    elif variant == "matriarch":
        for cx, cy, r in ((101, 37, 3), (113, 41, 4), (119, 35, 3)):
            draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=rgba(glow, 210))
        draw.rectangle((20, 19, 34, 22), fill=rgba(glow, 160))
    elif variant == "colossus":
        draw.line((98, 50, 121, 61), fill=rgba(glow, 245))
        draw.line((105, 49, 118, 63), fill=rgba(glow, 180))
        draw.rectangle((20, 18, 34, 22), fill=rgba(glow, 210))
    elif variant == "behemoth":
        draw.rectangle((115, 19, 120, 24), fill=rgba(glow, 255))
        for x in (20, 26, 32):
            draw.line((x, 19, x + 3, 25), fill=rgba(glow, 235))
    elif variant == "overseer":
        draw.line((98, 2, 105, 13), fill=rgba(glow, 245))
        draw.line((114, 31, 126, 31), fill=rgba(glow, 235))
        draw.line((114, 35, 126, 35), fill=rgba(glow, 190))
    elif variant == "nexus":
        for y in range(33, 47, 4):
            draw.line((113, y, 126, y + 3), fill=rgba(glow, 255))
        draw.rectangle((20, 18, 34, 21), fill=rgba(glow, 240))
        draw.line((10, 8, 17, 13), fill=rgba(glow, 230))

    return img.filter(ImageFilter.GaussianBlur(radius=0.15))


def render_drone_entity(spec: TextureSpec, palette: Palette, rng: random.Random) -> Image.Image:
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    hull = mix(palette.base, (84, 96, 104), 0.4)
    panel(draw, (0, 0, 25, 15), palette, hull, rng, True)
    panel(draw, (0, 16, 11, 23), palette, mix(hull, palette.dark, 0.28), rng, False)
    panel(draw, (12, 16, 19, 23), palette, palette.glow, rng, True)
    panel(draw, (36, 0, 51, 7), palette, mix(palette.dark, palette.accent, 0.25), rng, True)
    rect(draw, (37, 5, 47, 7), mix(palette.glow, palette.dark, 0.18), palette.light)
    draw.rectangle((39, 5, 41, 7), fill=rgba(palette.glow, 255))
    draw.rectangle((44, 5, 46, 7), fill=rgba(palette.glow, 255))
    draw.point((40, 6), fill=rgba((255, 255, 255), 245))
    draw.point((45, 6), fill=rgba((255, 255, 255), 245))
    panel(draw, (52, 0, 59, 7), palette, palette.glow, rng, True)
    for x, y in ((28, 3), (30, 9), (28, 17), (34, 22), (48, 18), (6, 5), (18, 6), (53, 4)):
        draw.point((x, y), fill=rgba(palette.light))
    for region in ((0, 0, 25, 15), (0, 16, 11, 23), (12, 16, 19, 23), (36, 0, 51, 7), (52, 0, 59, 7)):
        chip_marks(draw, region, palette, rng, 4)
    return img


def render_wolf_entity(spec: TextureSpec, palette: Palette, rng: random.Random) -> Image.Image:
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    fur = mix(palette.base, (106, 91, 72), 0.42)
    panel(draw, (0, 0, 15, 15), palette, fur, rng, False)
    panel(draw, (16, 0, 39, 15), palette, mix(fur, palette.dark, 0.14), rng, True)
    panel(draw, (40, 0, 47, 15), palette, mix(fur, palette.light, 0.2), rng, False)
    panel(draw, (0, 16, 31, 31), palette, mix(fur, palette.dark, 0.25), rng, False)
    panel(draw, (32, 16, 47, 31), palette, mix(fur, palette.accent, 0.18), rng, False)
    muzzle = mix(fur, (188, 174, 144), 0.28)
    for face in ((4, 4, 13, 12), (18, 4, 27, 12)):
        rect(draw, face, fur, palette.dark)
        x0, y0, x1, y1 = face
        draw_eye_pair(draw, (x0 + 3, y0 + 3), (x1 - 3, y0 + 3), palette.glow, mix(palette.dark, (0, 0, 0), 0.45))
        draw.rectangle((x0 + 4, y0 + 6, x1 - 4, y0 + 8), fill=rgba(muzzle, 245), outline=rgba(palette.dark, 220))
        draw.line((x0 + 4, y0 + 8, x1 - 4, y0 + 8), fill=rgba(mix(palette.dark, (0, 0, 0), 0.35), 240))
    for x in range(3, 45, 5):
        draw.line((x, 18, min(47, x + 2), 21), fill=rgba(mix(fur, palette.light, 0.35), 165))
    chip_marks(draw, (0, 0, 47, 31), palette, rng, 9)
    return img


def render_slime_entity(spec: TextureSpec, palette: Palette, rng: random.Random) -> Image.Image:
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    gel = mix(palette.base, (56, 194, 95), 0.35)
    panel(draw, (0, 0, 31, 15), palette, gel, rng, True)
    panel(draw, (0, 16, 31, 31), palette, mix(gel, palette.dark, 0.2), rng, False)
    panel(draw, (32, 0, 47, 15), palette, mix(gel, palette.glow, 0.25), rng, True)
    slime_shadow = mix(palette.dark, (0, 0, 0), 0.35)
    for face in ((8, 8, 23, 15), (32, 8, 47, 15)):
        x0, y0, x1, y1 = face
        draw_eye_pair(draw, (x0 + 4, y0 + 2), (x1 - 4, y0 + 2), palette.glow, slime_shadow)
        draw.rectangle((x0 + 6, y0 + 5, x1 - 6, y0 + 6), fill=rgba(slime_shadow, 230))
    filled_regions = ((4, 3, 30, 29), (33, 3, 46, 14))
    for _ in range(22):
        rx0, ry0, rx1, ry1 = rng.choice(filled_regions)
        x = rng.randrange(rx0, rx1 + 1)
        y = rng.randrange(ry0, ry1 + 1)
        color = palette.glow if rng.random() < 0.5 else mix(gel, palette.light, 0.35)
        draw.point((x, y), fill=rgba(color, 220))
        if rng.random() < 0.25 and x + 1 < 48:
            draw.point((x + 1, y), fill=rgba(mix(color, gel, 0.5), 180))
    return img


def render_crawler_entity(spec: TextureSpec, palette: Palette, rng: random.Random) -> Image.Image:
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    chitin = mix(palette.dark, palette.base, 0.28)
    panel(draw, (0, 0, 31, 15), palette, chitin, rng, True)
    panel(draw, (32, 0, 47, 15), palette, mix(chitin, palette.accent, 0.22), rng, False)
    panel(draw, (0, 16, 63, 31), palette, mix(chitin, palette.dark, 0.3), rng, False)
    for x in range(4, 60, 8):
        draw.line((x, 18, x + 5, 30), fill=rgba(palette.light, 180))
        draw.line((x + 2, 16, x + 9, 24), fill=rgba(palette.dark, 220))
    socket = mix(palette.dark, (0, 0, 0), 0.5)
    for face in ((0, 0, 15, 15), (32, 0, 47, 15)):
        x0, y0, x1, y1 = face
        for ex, ey in ((x0 + 4, y0 + 5), (x0 + 7, y0 + 4), (x1 - 7, y0 + 4), (x1 - 4, y0 + 5)):
            draw.rectangle((ex - 1, ey - 1, ex + 1, ey + 1), fill=rgba(socket, 245))
            draw.point((ex, ey), fill=rgba(palette.glow, 255))
        draw.line((x0 + 3, y0 + 9, x1 - 3, y0 + 9), fill=rgba(palette.dark, 230))
    for region in ((0, 0, 31, 15), (32, 0, 47, 15), (0, 16, 63, 31)):
        chip_marks(draw, region, palette, rng, 5)
    return img


def render_vex_entity(spec: TextureSpec, palette: Palette, rng: random.Random) -> Image.Image:
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    shell = mix(palette.base, (180, 205, 220), 0.35)
    panel(draw, (0, 0, 31, 15), palette, shell, rng, True)
    panel(draw, (16, 16, 39, 31), palette, mix(shell, palette.dark, 0.2), rng, True)
    panel(draw, (40, 16, 55, 31), palette, mix(shell, palette.glow, 0.18), rng, False)
    panel(draw, (0, 32, 31, 47), palette, mix(palette.dark, palette.glow, 0.12), rng, True)
    panel(draw, (32, 32, 63, 47), palette, mix(palette.dark, palette.accent, 0.14), rng, True)
    draw_humanoid_face(draw, (8, 8, 15, 15), mix(shell, palette.base, 0.25), palette, True)
    draw_helmet_face(draw, (40, 8, 47, 15), mix(shell, palette.dark, 0.08), palette, True)
    draw.rectangle((20, 20, 35, 22), fill=rgba(palette.glow, 210))
    sparkle_regions = ((0, 0, 31, 15), (16, 16, 39, 31), (40, 16, 55, 31), (0, 32, 31, 47), (32, 32, 63, 47))
    for _ in range(7):
        rx0, ry0, rx1, ry1 = rng.choice(sparkle_regions)
        x = rng.randrange(rx0 + 1, rx1)
        y = rng.randrange(ry0 + 1, ry1)
        draw.point((x, y), fill=rgba(palette.glow, 220))
        if rng.random() < 0.45 and y + 1 <= ry1:
            draw.point((x, y + 1), fill=rgba(mix(palette.glow, palette.base, 0.45), 180))
    for region in ((0, 0, 31, 15), (16, 16, 39, 31), (40, 16, 55, 31), (0, 32, 31, 47), (32, 32, 63, 47)):
        chip_marks(draw, region, palette, rng, 3)
    return img


def render_entity(spec: TextureSpec) -> Image.Image:
    rng = stable_rng("entity:" + spec.name)
    group = entity_group(spec)
    palette = palette_for(guardian_base_name(spec.name) if group == "guardian_glow" else spec.name)
    if group == "guardian_glow":
        return render_guardian_glow_entity(spec, palette)
    if group == "boss":
        return render_humanoid_entity(spec, palette, rng, boss=True)
    if group == "drone":
        return render_drone_entity(spec, palette, rng)
    if group == "wolf":
        return render_wolf_entity(spec, palette, rng)
    if group == "slime":
        return render_slime_entity(spec, palette, rng)
    if group == "crawler":
        return render_crawler_entity(spec, palette, rng)
    if group == "vex":
        return render_vex_entity(spec, palette, rng)
    return render_humanoid_entity(spec, palette, rng)


def render_armor(spec: TextureSpec) -> Image.Image:
    palette = palette_for(spec.name)
    img = Image.new("RGBA", (spec.width, spec.height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    armor = mix(palette.base, (170, 178, 184), 0.45)
    accent = palette.glow if "nexus" in spec.name else palette.accent
    regions = [
        (0, 0, 15, 15),
        (16, 0, 31, 15),
        (32, 0, 47, 15),
        (48, 0, 63, 15),
        (16, 16, 31, 31),
        (40, 16, 55, 31),
    ]
    for i, region in enumerate(regions):
        fill = mix(armor, palette.dark, 0.15 if i % 2 == 0 else 0.35)
        draw.rectangle(region, fill=rgba(fill), outline=rgba(palette.dark))
        x0, y0, x1, y1 = region
        draw.line((x0 + 2, y0 + 2, x1 - 2, y0 + 2), fill=rgba(palette.light))
        draw.line((x0 + 3, y1 - 3, x1 - 3, y1 - 3), fill=rgba(accent))
    return img


def render_gui(spec: TextureSpec) -> Image.Image:
    palette = palette_for(spec.name)
    img = Image.new("RGBA", (spec.width, spec.height), rgba(adjust(palette.dark, -20)))
    draw = ImageDraw.Draw(img, "RGBA")
    w, h = spec.width, spec.height
    bevel_rect(draw, (0, 0, w - 1, h - 1), palette, mix(palette.base, palette.dark, 0.4))
    bevel_rect(draw, (8, 8, w - 9, h - 9), palette, mix(palette.base, palette.dark, 0.1))
    bevel_rect(draw, (18, 16, 96, 56), palette, mix(palette.dark, palette.accent, 0.18))
    bevel_rect(draw, (110, 16, 154, 56), palette, mix(palette.base, palette.light, 0.15))
    bevel_rect(draw, (170, 18, 238, 54), palette, mix(palette.dark, palette.accent, 0.12))
    for x in range(24, 92, 12):
        draw.line((x, 24, x + 6, 47), fill=rgba(palette.glow if x % 24 == 0 else palette.accent))
    for x in range(112, 151, 9):
        draw.rectangle((x, 24, x + 4, 49), fill=rgba(palette.dark), outline=rgba(palette.light, 120))
    for row in range(3):
        for col in range(9):
            x = 28 + col * 18
            y = 72 + row * 18
            bevel_rect(draw, (x, y, x + 15, y + 15), palette, mix(palette.dark, palette.base, 0.35))
    draw.rectangle((187, 71, 226, 120), outline=rgba(palette.accent), fill=rgba(palette.dark, 160))
    draw_glow(draw, (207, 96), 16, palette.glow)
    img.putalpha(255)
    return img


def render_texture(spec: TextureSpec) -> Image.Image:
    if spec.kind == "item":
        return normalize_texture_palette(render_item(spec), spec.name)
    if spec.kind == "block":
        cutout = is_cutout_block(spec.name)
        return normalize_texture_palette(
            render_block(spec),
            spec.name,
            partial_alpha="glass" in spec.name,
            force_opaque=not cutout,
            colors_override=block_style_colors(spec.name),
        )
    if spec.kind == "entity":
        return render_entity(spec)
    if spec.kind == "armor":
        return render_armor(spec)
    if spec.kind == "gui":
        return render_gui(spec)
    raise ValueError(f"Unsupported texture kind: {spec.kind} ({spec.rel_path})")


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, separators=(",", ":")) + "\n", encoding="utf-8")


def refresh_orbital_models(dry_run: bool = False) -> int:
    target = TARGETS["orbital"]
    block_ids = ids_from_json_dir(target.block_models)
    item_ids = ids_from_json_dir(target.item_defs)
    standalone_item_ids = sorted(item_ids - block_ids)
    written = 0

    for block_id in sorted(block_ids):
        if not dry_run:
            face_textures = {
                "particle": f"{target.modid}:block/{block_id}_front",
                "north": f"{target.modid}:block/{block_id}_front",
                "south": f"{target.modid}:block/{block_id}_side",
                "east": f"{target.modid}:block/{block_id}_side",
                "west": f"{target.modid}:block/{block_id}_side",
                "up": f"{target.modid}:block/{block_id}_top",
                "down": f"{target.modid}:block/{block_id}_bottom",
            }
            has_face_textures = block_id in MULTIFACE_MACHINE_BLOCKS and all(
                (target.textures / "block" / f"{block_id}_{face}.png").exists()
                for face in ("front", "side", "top", "bottom")
            )
            block_model = (
                {
                    "parent": "minecraft:block/cube",
                    "textures": face_textures,
                }
                if has_face_textures
                else {
                    "parent": "minecraft:block/cube_all",
                    "textures": {
                        "all": f"{target.modid}:block/{block_id}",
                    },
                }
            )
            write_json(
                target.block_models / f"{block_id}.json",
                block_model,
            )
            write_json(
                target.item_defs / f"{block_id}.json",
                {
                    "model": {
                        "type": "minecraft:model",
                        "model": f"{target.modid}:block/{block_id}",
                    },
                },
            )
        written += 2

    for item_id in standalone_item_ids:
        if not dry_run:
            write_json(
                target.item_models / f"{item_id}.json",
                {
                    "parent": "minecraft:item/generated",
                    "textures": {
                        "layer0": f"{target.modid}:item/{item_id}",
                    },
                },
            )
            write_json(
                target.item_defs / f"{item_id}.json",
                {
                    "model": {
                        "type": "minecraft:model",
                        "model": f"{target.modid}:item/{item_id}",
                    },
                },
            )
        written += 2
    return written


def selected_targets(target: str) -> list[TextureTarget]:
    if target == "all":
        return [TARGETS["ashfall"], TARGETS["orbital"]]
    return [TARGETS[target]]


def write_catalog(specs: list[TextureSpec]) -> None:
    BUILD_OUT.mkdir(parents=True, exist_ok=True)
    counts: dict[tuple[str, str], int] = {}
    for spec in specs:
        key = (spec.modid, spec.kind)
        counts[key] = counts.get(key, 0) + 1

    lines = [
        "# Generated Texture Catalog",
        "",
        "Deterministic Minecraft-style texture pass with controlled pixel clusters.",
        "",
    ]
    for (modid, kind), count in sorted(counts.items()):
        lines.append(f"- {modid} {kind}: {count}")
    lines.append("")
    lines.append("## Textures")
    for spec in sorted(specs, key=lambda s: (s.modid, s.kind, s.rel_path)):
        lines.append(f"- `{spec.rel_path}` ({spec.width}x{spec.height}, {spec.kind})")
    (BUILD_OUT / "texture_catalog.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    write_json(BUILD_OUT / "texture_manifest.generated.json", [spec.manifest_row() for spec in specs])


def write_sheet(specs: list[TextureSpec], output: Path) -> None:
    if not specs:
        return
    cell = 40
    cols = 16
    rows = math.ceil(len(specs) / cols)
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (22, 24, 27, 255))
    for index, spec in enumerate(specs):
        with Image.open(spec.path) as img:
            thumb = img.convert("RGBA")
        max_side = cell - 8
        scale = min(max_side / max(1, thumb.width), max_side / max(1, thumb.height))
        if scale >= 1:
            factor = max(1, int(scale))
            size = (thumb.width * factor, thumb.height * factor)
        else:
            size = (max(1, int(thumb.width * scale)), max(1, int(thumb.height * scale)))
        thumb = thumb.resize(size, Image.Resampling.NEAREST)
        x = (index % cols) * cell + (cell - thumb.width) // 2
        y = (index // cols) * cell + (cell - thumb.height) // 2
        sheet.alpha_composite(thumb, (x, y))
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)


def write_tiling_preview(specs: list[TextureSpec], output: Path) -> None:
    block_specs = [spec for spec in specs if spec.kind == "block" and not is_cutout_block(spec.name)]
    if not block_specs:
        return
    cell = 56
    cols = 12
    rows = math.ceil(len(block_specs) / cols)
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (18, 20, 22, 255))
    for index, spec in enumerate(block_specs):
        with Image.open(spec.path) as img:
            tile = img.convert("RGBA").resize((16, 16), Image.Resampling.NEAREST)
        preview = Image.new("RGBA", (48, 48), (0, 0, 0, 0))
        for ty in range(3):
            for tx in range(3):
                preview.alpha_composite(tile, (tx * 16, ty * 16))
        x = (index % cols) * cell + 4
        y = (index // cols) * cell + 4
        sheet.alpha_composite(preview, (x, y))
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)


def write_entity_face_preview(specs: list[TextureSpec], output: Path) -> None:
    entity_specs = [spec for spec in specs if spec.kind == "entity" and not spec.name.endswith("_glow")]
    if not entity_specs:
        return
    scale = 8
    cell = 72
    cols = 8
    rows = math.ceil(len(entity_specs) / cols)
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (18, 20, 22, 255))
    for index, spec in enumerate(entity_specs):
        with Image.open(spec.path) as img:
            tex = img.convert("RGBA")
        crops = []
        if spec.width >= 128:
            crops = [(8, 8, 18, 18), (40, 8, 50, 18)]
        elif spec.width == 64 and spec.height == 32:
            crops = [(0, 0, 16, 16), (32, 0, 48, 16)]
        else:
            crops = [(8, 8, 16, 16), (40, 8, 48, 16)]
        x = (index % cols) * cell + 4
        y = (index // cols) * cell + 4
        for crop_index, box in enumerate(crops[:2]):
            face = tex.crop(box)
            face = face.resize((face.width * scale // 2, face.height * scale // 2), Image.Resampling.NEAREST)
            sheet.alpha_composite(face, (x + crop_index * 34, y))
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)


def pixel_busyness(pixels: list[tuple[int, int, int, int]], width: int, height: int) -> float:
    visible = 0
    diffs = 0
    comparisons = 0
    for y in range(height):
        for x in range(width):
            i = y * width + x
            if pixels[i][3] < 32:
                continue
            visible += 1
            for nx, ny in ((x + 1, y), (x, y + 1)):
                if nx >= width or ny >= height:
                    continue
                j = ny * width + nx
                if pixels[j][3] < 32:
                    continue
                comparisons += 1
                if color_distance(pixels[i][:3], pixels[j][:3]) > 900:
                    diffs += 1
    if visible == 0 or comparisons == 0:
        return 0.0
    return diffs / comparisons


def block_seam_score(pixels: list[tuple[int, int, int, int]], width: int, height: int) -> float:
    if width < 2 or height < 2:
        return 0.0
    total = 0
    count = 0
    for y in range(height):
        total += color_distance(pixels[y * width][:3], pixels[y * width + width - 1][:3])
        count += 1
    for x in range(width):
        total += color_distance(pixels[x][:3], pixels[(height - 1) * width + x][:3])
        count += 1
    return total / max(1, count)


def texture_quality_issues(spec: TextureSpec) -> list[str]:
    issues: list[str] = []
    with Image.open(spec.path) as img:
        rgba_img = img.convert("RGBA")
    if rgba_img.size != (spec.width, spec.height):
        issues.append(f"size {rgba_img.width}x{rgba_img.height}, expected {spec.width}x{spec.height}")
    raw_pixels = rgba_img.tobytes()
    pixels = [
        (raw_pixels[i], raw_pixels[i + 1], raw_pixels[i + 2], raw_pixels[i + 3])
        for i in range(0, len(raw_pixels), 4)
    ]
    visible = [pixel for pixel in pixels if pixel[3] >= 32]
    if not visible:
        issues.append("no visible pixels")
        return issues
    if spec.kind == "item" and len(visible) == len(pixels):
        issues.append("item has no transparent background")
    if spec.kind == "gui" and len(visible) < len(pixels):
        issues.append("opaque texture kind contains transparent pixels")
    if spec.kind == "block" and not is_cutout_block(spec.name) and len(visible) < len(pixels):
        issues.append("solid block contains transparent pixels")
    palette_size = len({pixel[:3] for pixel in visible})
    busyness = pixel_busyness(pixels, rgba_img.width, rgba_img.height)
    role = block_role(spec.name) if spec.kind == "block" else spec.kind
    seam_checked = spec.kind == "block" and role in {"ash", "terrain", "sand", "stone", "rubble", "ore", "organic", "nexus"}
    seam = block_seam_score(pixels, rgba_img.width, rgba_img.height) if seam_checked else 0.0
    if spec.kind in {"block", "item"} and palette_size > 18:
        issues.append(f"high palette count ({palette_size})")
    if spec.kind == "entity" and palette_size > 64:
        issues.append(f"high entity palette count ({palette_size})")
    if spec.kind == "block" and busyness > 0.62:
        issues.append(f"high busyness ({busyness:.2f})")
    if spec.kind == "item" and busyness > 0.72:
        issues.append(f"busy item silhouette ({busyness:.2f})")
    if seam_checked and seam > 5400:
        issues.append(f"high seam risk ({seam:.0f})")
    return issues


def texture_quality_metrics(spec: TextureSpec) -> dict[str, object]:
    with Image.open(spec.path) as img:
        rgba_img = img.convert("RGBA")
    raw_pixels = rgba_img.tobytes()
    pixels = [
        (raw_pixels[i], raw_pixels[i + 1], raw_pixels[i + 2], raw_pixels[i + 3])
        for i in range(0, len(raw_pixels), 4)
    ]
    visible = [pixel for pixel in pixels if pixel[3] >= 32]
    return {
        "path": spec.rel_path,
        "kind": spec.kind,
        "role": block_role(spec.name) if spec.kind == "block" else spec.kind,
        "size": f"{rgba_img.width}x{rgba_img.height}",
        "visiblePixels": len(visible),
        "paletteColors": len({pixel[:3] for pixel in visible}),
        "busyness": round(pixel_busyness(pixels, rgba_img.width, rgba_img.height), 4),
        "seamScore": round(block_seam_score(pixels, rgba_img.width, rgba_img.height), 1)
        if spec.kind == "block" and block_role(spec.name) in {"ash", "terrain", "sand", "stone", "rubble", "ore", "organic", "nexus"}
        else 0.0,
    }


def write_quality_report(specs: list[TextureSpec]) -> None:
    rows: list[dict[str, object]] = []
    metrics = [texture_quality_metrics(spec) for spec in specs]
    lines = [
        "# Texture Quality Report",
        "",
        "Checks dimensions, visibility, alpha use, palette discipline, busyness, and block seam risk.",
        "",
    ]
    for spec in specs:
        issues = texture_quality_issues(spec)
        if issues:
            rows.append({"path": spec.rel_path, "kind": spec.kind, "issues": issues})
            lines.append(f"- `{spec.rel_path}`: {', '.join(issues)}")
    if not rows:
        lines.append("- No quality issues detected.")
    (BUILD_OUT / "texture_quality_report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    write_json(BUILD_OUT / "texture_quality_report.json", rows)
    write_json(BUILD_OUT / "texture_quality_metrics.json", metrics)


def write_previews(specs: list[TextureSpec]) -> None:
    write_catalog(specs)
    write_quality_report(specs)
    for modid in sorted({spec.modid for spec in specs}):
        mod_specs = [spec for spec in specs if spec.modid == modid]
        for kind in sorted({spec.kind for spec in mod_specs}):
            write_sheet(
                [spec for spec in mod_specs if spec.kind == kind],
                BUILD_OUT / f"{modid}_{kind}_sheet.png",
            )
    write_tiling_preview(specs, BUILD_OUT / "block_tiling_3x3_sheet.png")
    write_entity_face_preview(specs, BUILD_OUT / "entity_faces_8x.png")
    write_sheet(specs, BUILD_OUT / "all_textures_sheet.png")


def generate(target: str = "orbital", dry_run: bool = False, write_models: bool = False, kind: str | None = None) -> tuple[list[TextureSpec], int]:
    specs: list[TextureSpec] = []
    for texture_target in selected_targets(target):
        specs.extend(discover_specs(texture_target))
    if kind:
        specs = [spec for spec in specs if spec.kind == kind]

    model_count = refresh_orbital_models(dry_run=dry_run) if write_models and target in {"orbital", "all"} else 0

    if not dry_run:
        for spec in specs:
            spec.path.parent.mkdir(parents=True, exist_ok=True)
            render_texture(spec).save(spec.path)
        write_previews(specs)

    return specs, model_count


def print_summary(specs: list[TextureSpec], model_count: int, dry_run: bool) -> None:
    action = "Would generate" if dry_run else "Generated"
    print(f"{action} {len(specs)} texture PNGs.")
    counts: dict[tuple[str, str], int] = {}
    for spec in specs:
        key = (spec.modid, spec.kind)
        counts[key] = counts.get(key, 0) + 1
    for (modid, kind), count in sorted(counts.items()):
        print(f"  {modid} {kind}: {count}")
    if model_count:
        model_action = "Would refresh" if dry_run else "Refreshed"
        print(f"{model_action} {model_count} Orbital model definitions.")
    if not dry_run:
        print(f"Wrote previews and catalog to {BUILD_OUT.relative_to(REPO_ROOT)}")


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="Report counts without writing files.")
    parser.add_argument("--target", choices=("orbital", "ashfall", "all"), default="orbital", help="Texture set to regenerate.")
    parser.add_argument("--kind", choices=("block", "item", "entity", "armor", "gui"), help="Restrict generation to one texture kind.")
    parser.add_argument("--write-models", action="store_true", help="Also refresh Orbital item/block model JSON.")
    parser.add_argument("--no-models", action="store_true", help="Disable the default Orbital model refresh.")
    args = parser.parse_args(list(argv) if argv is not None else None)

    if args.write_models and args.no_models:
        parser.error("--write-models and --no-models cannot be used together")

    write_models = args.write_models or (args.target == "orbital" and not args.no_models)
    specs, model_count = generate(target=args.target, dry_run=args.dry_run, write_models=write_models, kind=args.kind)
    print_summary(specs, model_count, args.dry_run)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

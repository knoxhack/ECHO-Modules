#!/usr/bin/env python3
"""Generate EchoArmory pixel textures, model JSON, and previews.

The project keeps Codex image-generation sheets in art/source_sheets as the
visual source pass. This script turns that pass into hard-edged Minecraft
assets with deterministic local repair so the final sprites are exactly sized
and stable in git.
"""

from __future__ import annotations

import argparse
import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/echoarmory"
TEXTURES = ASSETS / "textures"
MODELS = ASSETS / "models"
ITEM_DEFS = ASSETS / "items"
EQUIPMENT = ASSETS / "equipment"
SOURCE = ROOT / "art/source_sheets"
PREVIEWS = ROOT / "build/texture_previews"
MODID = "echoarmory"


RGBA = tuple[int, int, int, int]


@dataclass(frozen=True)
class ItemSpec:
    name: str
    kind: str
    accent: str
    parent: str


@dataclass(frozen=True)
class BlockSpec:
    name: str
    role: str
    accent: str


ITEMS: list[ItemSpec] = [
    ItemSpec("armory_alloy_plate", "plate", "#bfc7c8", "minecraft:item/generated"),
    ItemSpec("veil_crystal", "crystal", "#9a4dff", "minecraft:item/generated"),
    ItemSpec("resonance_shard", "shard", "#45d7ff", "minecraft:item/generated"),
    ItemSpec("blackbox_fragment", "fragment", "#3a333f", "minecraft:item/generated"),
    ItemSpec("ammo_crystals", "ammo", "#49e6ff", "minecraft:item/generated"),
    ItemSpec("alloy_sword", "sword", "#bfc7c8", "minecraft:item/handheld"),
    ItemSpec("frost_blade", "sword", "#8fe9ff", "minecraft:item/handheld"),
    ItemSpec("veil_sabre", "sword", "#9a4dff", "minecraft:item/handheld"),
    ItemSpec("harmonic_staff", "staff", "#ffd45a", "minecraft:item/handheld"),
    ItemSpec("arcane_dagger", "dagger", "#aa62ff", "minecraft:item/handheld"),
    ItemSpec("energy_rifle", "rifle", "#36d9ff", "minecraft:item/generated"),
    ItemSpec("veil_bow", "bow", "#a45cff", "minecraft:item/generated"),
    ItemSpec("convergence_gun", "rifle", "#68f59a", "minecraft:item/generated"),
    ItemSpec("resonance_hammer", "hammer", "#58dbff", "minecraft:item/handheld"),
    ItemSpec("sigil_chakram", "chakram", "#ffd45a", "minecraft:item/generated"),
    ItemSpec("construct_gauntlet", "gauntlet", "#55e585", "minecraft:item/generated"),
    ItemSpec("arcane_shield", "shield", "#43cfff", "minecraft:item/generated"),
    ItemSpec("veil_resistant_helm", "helmet", "#9a4dff", "minecraft:item/generated"),
    ItemSpec("thermal_chestplate", "chestplate", "#ff9142", "minecraft:item/generated"),
    ItemSpec("drone_leggings", "leggings", "#39d9ff", "minecraft:item/generated"),
    ItemSpec("orbital_boots", "boots", "#56d8ff", "minecraft:item/generated"),
    ItemSpec("construct_harness", "harness", "#55e585", "minecraft:item/generated"),
    ItemSpec("sigil_augmented_suit", "suit", "#ffd45a", "minecraft:item/generated"),
    ItemSpec("fire_core", "core", "#ff7a2d", "minecraft:item/generated"),
    ItemSpec("frost_core", "core", "#80eaff", "minecraft:item/generated"),
    ItemSpec("lightning_core", "core", "#ffe96b", "minecraft:item/generated"),
    ItemSpec("void_core", "core", "#8f49ff", "minecraft:item/generated"),
    ItemSpec("stability_rune", "rune", "#4bdcff", "minecraft:item/generated"),
    ItemSpec("life_leech_sigil", "sigil", "#bd3a55", "minecraft:item/generated"),
    ItemSpec("veil_shield", "module_shield", "#9654ff", "minecraft:item/generated"),
    ItemSpec("thermal_regulator", "regulator", "#ff9142", "minecraft:item/generated"),
    ItemSpec("gas_mask_filter", "filter", "#8b9292", "minecraft:item/generated"),
    ItemSpec("radiation_shield", "module_shield", "#75e45a", "minecraft:item/generated"),
    ItemSpec("mobility_servo", "servo", "#43cfff", "minecraft:item/generated"),
    ItemSpec("drone_dock", "dock_item", "#55e585", "minecraft:item/generated"),
]


BLOCKS: list[BlockSpec] = [
    BlockSpec("armory_bench", "bench", "#44d9ff"),
    BlockSpec("weapon_forge", "forge", "#ff7a2d"),
    BlockSpec("armor_forge", "forge", "#67e0ff"),
    BlockSpec("energy_core_charging_station", "charger", "#49e6ff"),
    BlockSpec("module_upgrade_table", "upgrade", "#36d9ff"),
    BlockSpec("sigil_engraver", "engraver", "#ffd45a"),
    BlockSpec("loadout_terminal", "terminal", "#49dfff"),
    BlockSpec("weapon_rack", "rack", "#c49455"),
    BlockSpec("armor_stand", "stand", "#c4c9ca"),
    BlockSpec("veil_infuser", "infuser", "#9a4dff"),
    BlockSpec("construct_dock", "dock", "#55e585"),
]


ARMOR = [
    ("veil_resistant_helm", "humanoid", "outer", "#9a4dff"),
    ("thermal_chestplate", "humanoid", "outer", "#ff9142"),
    ("drone_leggings", "humanoid_leggings", "inner", "#39d9ff"),
    ("orbital_boots", "humanoid", "outer", "#56d8ff"),
    ("construct_harness", "humanoid", "outer", "#55e585"),
    ("sigil_augmented_suit", "humanoid", "outer", "#ffd45a"),
]


def hex_rgba(value: str, alpha: int = 255) -> RGBA:
    value = value.strip().lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def shade(color: RGBA, factor: float) -> RGBA:
    return (
        max(0, min(255, int(color[0] * factor))),
        max(0, min(255, int(color[1] * factor))),
        max(0, min(255, int(color[2] * factor))),
        color[3],
    )


def img16() -> Image.Image:
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def rect(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], color: RGBA) -> None:
    draw.rectangle(xy, fill=color)


def put(img: Image.Image, x: int, y: int, color: RGBA) -> None:
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color)


def harden(img: Image.Image, palette_size: int = 16) -> Image.Image:
    img = img.convert("RGBA")
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    alpha = img.getchannel("A")
    rgb = img.convert("RGB").quantize(colors=palette_size, method=Image.Quantize.MEDIANCUT).convert("RGB")
    out.paste(rgb, mask=alpha.point(lambda a: 255 if a >= 160 else 0))
    return out


def draw_plate(spec: ItemSpec) -> Image.Image:
    img = img16()
    d = ImageDraw.Draw(img)
    metal = hex_rgba(spec.accent)
    dark = (30, 35, 38, 255)
    rect(d, (3, 5, 12, 10), dark)
    rect(d, (4, 4, 13, 9), shade(metal, 0.85))
    d.line((4, 4, 12, 4), fill=shade(metal, 1.35))
    d.line((5, 9, 13, 9), fill=shade(metal, 0.55))
    d.line((9, 5, 12, 8), fill=shade(metal, 1.2))
    return img


def draw_crystal(spec: ItemSpec) -> Image.Image:
    img = img16()
    d = ImageDraw.Draw(img)
    c = hex_rgba(spec.accent)
    dark = shade(c, 0.35)
    mid = shade(c, 0.8)
    light = shade(c, 1.5)
    d.polygon([(7, 1), (11, 6), (9, 14), (5, 14), (3, 6)], fill=dark)
    d.polygon([(7, 2), (10, 6), (8, 12), (6, 12), (4, 6)], fill=mid)
    d.line((7, 2, 7, 12), fill=light)
    d.line((5, 7, 9, 5), fill=light)
    return img


def draw_shard(spec: ItemSpec) -> Image.Image:
    img = img16()
    d = ImageDraw.Draw(img)
    c = hex_rgba(spec.accent)
    d.polygon([(4, 13), (7, 2), (12, 4), (10, 12)], fill=shade(c, 0.45))
    d.polygon([(6, 12), (8, 3), (11, 5), (9, 11)], fill=shade(c, 1.0))
    d.line((8, 3, 8, 11), fill=shade(c, 1.55))
    return img


def draw_core(spec: ItemSpec) -> Image.Image:
    img = img16()
    d = ImageDraw.Draw(img)
    c = hex_rgba(spec.accent)
    metal = (51, 58, 60, 255)
    dark = (20, 23, 25, 255)
    rect(d, (4, 3, 11, 12), dark)
    rect(d, (5, 4, 10, 11), metal)
    rect(d, (6, 5, 9, 10), shade(c, 0.9))
    rect(d, (7, 6, 8, 9), shade(c, 1.55))
    put(img, 5, 3, shade(metal, 1.35))
    put(img, 10, 12, shade(metal, 0.5))
    return img


def draw_weapon(spec: ItemSpec) -> Image.Image:
    img = img16()
    d = ImageDraw.Draw(img)
    c = hex_rgba(spec.accent)
    metal = (188, 196, 196, 255)
    dark = (22, 24, 26, 255)
    if spec.kind in {"sword", "dagger"}:
        length = 11 if spec.kind == "sword" else 8
        d.line((3, 13, 3 + length, 2), fill=dark, width=3)
        d.line((4, 12, 4 + length, 1), fill=shade(c, 1.2), width=1)
        d.line((2, 11, 6, 15), fill=dark, width=1)
        rect(d, (2, 13, 4, 15), (72, 46, 32, 255))
    elif spec.kind == "staff":
        d.line((4, 14, 11, 3), fill=(93, 55, 24, 255), width=2)
        rect(d, (9, 1, 13, 5), dark)
        rect(d, (10, 2, 12, 4), shade(c, 1.3))
    elif spec.kind == "hammer":
        d.line((4, 13, 9, 8), fill=(92, 60, 35, 255), width=2)
        rect(d, (7, 3, 13, 7), dark)
        rect(d, (8, 4, 12, 6), metal)
        put(img, 10, 5, shade(c, 1.3))
    elif spec.kind in {"rifle"}:
        rect(d, (3, 8, 12, 11), dark)
        rect(d, (4, 7, 10, 9), (72, 80, 82, 255))
        rect(d, (11, 8, 14, 9), metal)
        rect(d, (5, 8, 9, 8), shade(c, 1.25))
        d.line((6, 11, 8, 14), fill=dark, width=2)
    elif spec.kind == "bow":
        d.arc((3, 2, 13, 14), 260, 100, fill=dark, width=2)
        d.line((9, 3, 9, 13), fill=shade(c, 1.2), width=1)
        d.line((5, 8, 13, 8), fill=shade(c, 1.3), width=1)
    return img


def draw_shield(spec: ItemSpec) -> Image.Image:
    img = img16()
    d = ImageDraw.Draw(img)
    c = hex_rgba(spec.accent)
    dark = (24, 28, 30, 255)
    d.polygon([(3, 3), (12, 3), (12, 9), (8, 14), (3, 9)], fill=dark)
    d.polygon([(4, 4), (11, 4), (11, 9), (8, 12), (4, 9)], fill=(78, 87, 90, 255))
    d.line((5, 5, 10, 5), fill=shade(c, 1.25))
    d.line((8, 5, 8, 11), fill=shade(c, 1.45))
    return img


def draw_armor_icon(spec: ItemSpec) -> Image.Image:
    img = img16()
    d = ImageDraw.Draw(img)
    c = hex_rgba(spec.accent)
    dark = (22, 25, 27, 255)
    metal = (75, 82, 84, 255)
    if spec.kind == "helmet":
        rect(d, (4, 4, 11, 10), dark)
        rect(d, (5, 5, 10, 9), metal)
        rect(d, (5, 8, 10, 9), shade(c, 1.25))
    elif spec.kind in {"chestplate", "harness", "suit"}:
        d.polygon([(4, 3), (11, 3), (13, 8), (11, 14), (4, 14), (2, 8)], fill=dark)
        rect(d, (5, 5, 10, 13), metal)
        d.line((7, 5, 7, 13), fill=shade(c, 1.2))
        d.line((9, 5, 9, 13), fill=shade(c, 0.9))
    elif spec.kind == "leggings":
        rect(d, (4, 3, 11, 7), dark)
        rect(d, (5, 4, 10, 6), metal)
        rect(d, (4, 8, 6, 14), metal)
        rect(d, (9, 8, 11, 14), metal)
        put(img, 5, 10, shade(c, 1.3))
        put(img, 10, 10, shade(c, 1.3))
    elif spec.kind == "boots":
        rect(d, (3, 8, 6, 13), dark)
        rect(d, (9, 8, 12, 13), dark)
        rect(d, (4, 9, 7, 12), metal)
        rect(d, (8, 9, 11, 12), metal)
        d.line((4, 12, 7, 12), fill=shade(c, 1.2))
        d.line((8, 12, 11, 12), fill=shade(c, 1.2))
    return img


def draw_chakram(spec: ItemSpec) -> Image.Image:
    img = img16()
    d = ImageDraw.Draw(img)
    c = hex_rgba(spec.accent)
    d.ellipse((3, 3, 12, 12), outline=(24, 27, 29, 255), width=2)
    d.ellipse((5, 5, 10, 10), outline=shade(c, 1.25), width=2)
    d.line((8, 2, 8, 5), fill=shade(c, 1.4))
    d.line((8, 10, 8, 13), fill=shade(c, 1.4))
    d.line((2, 8, 5, 8), fill=shade(c, 1.4))
    d.line((10, 8, 13, 8), fill=shade(c, 1.4))
    return img


def draw_misc(spec: ItemSpec) -> Image.Image:
    if spec.kind in {"plate", "fragment"}:
        return draw_plate(spec)
    if spec.kind == "crystal":
        return draw_crystal(spec)
    if spec.kind == "shard":
        return draw_shard(spec)
    if spec.kind == "ammo":
        return draw_core(spec)
    if spec.kind in {"sword", "dagger", "staff", "rifle", "bow", "hammer"}:
        return draw_weapon(spec)
    if spec.kind == "chakram":
        return draw_chakram(spec)
    if spec.kind == "gauntlet":
        img = draw_armor_icon(ItemSpec(spec.name, "harness", spec.accent, spec.parent))
        d = ImageDraw.Draw(img)
        rect(d, (10, 7, 13, 12), (26, 29, 31, 255))
        rect(d, (11, 8, 12, 11), shade(hex_rgba(spec.accent), 1.2))
        return img
    if spec.kind in {"shield", "module_shield"}:
        return draw_shield(spec)
    if spec.kind in {"helmet", "chestplate", "leggings", "boots", "harness", "suit"}:
        return draw_armor_icon(spec)
    if spec.kind == "core":
        return draw_core(spec)
    if spec.kind in {"rune", "sigil"}:
        img = img16()
        d = ImageDraw.Draw(img)
        c = hex_rgba(spec.accent)
        rect(d, (4, 2, 11, 13), (27, 31, 33, 255))
        rect(d, (5, 3, 10, 12), (62, 68, 70, 255))
        d.line((6, 10, 9, 5), fill=shade(c, 1.45))
        d.line((6, 5, 10, 10), fill=shade(c, 1.1))
        return img
    if spec.kind == "regulator":
        return draw_core(spec)
    if spec.kind == "filter":
        img = img16()
        d = ImageDraw.Draw(img)
        c = hex_rgba(spec.accent)
        rect(d, (4, 3, 11, 12), (21, 24, 26, 255))
        rect(d, (5, 4, 10, 11), shade(c, 0.85))
        for y in (5, 7, 9):
            d.line((5, y, 10, y), fill=shade(c, 0.45))
        return img
    if spec.kind == "servo":
        img = img16()
        d = ImageDraw.Draw(img)
        c = hex_rgba(spec.accent)
        rect(d, (3, 5, 9, 11), (30, 34, 36, 255))
        rect(d, (5, 7, 7, 9), shade(c, 1.35))
        d.line((9, 8, 13, 4), fill=(84, 92, 94, 255), width=2)
        d.line((9, 8, 13, 12), fill=(84, 92, 94, 255), width=2)
        return img
    if spec.kind == "dock_item":
        img = img16()
        d = ImageDraw.Draw(img)
        c = hex_rgba(spec.accent)
        rect(d, (3, 7, 12, 12), (25, 29, 31, 255))
        rect(d, (4, 8, 11, 11), (63, 70, 70, 255))
        rect(d, (6, 4, 9, 8), shade(c, 0.95))
        put(img, 7, 5, shade(c, 1.5))
        return img
    return draw_plate(spec)


def draw_item(spec: ItemSpec) -> Image.Image:
    return harden(draw_misc(spec), palette_size=14)


def draw_block_texture(spec: BlockSpec, detail: bool = False) -> Image.Image:
    img = Image.new("RGBA", (16, 16), (52, 59, 60, 255))
    d = ImageDraw.Draw(img)
    accent = hex_rgba(spec.accent)
    base = (55, 62, 64, 255)
    dark = (26, 30, 32, 255)
    light = (112, 122, 122, 255)
    for y in range(0, 16, 4):
        for x in range(0, 16, 4):
            fill = base if ((x + y) // 4) % 2 else (45, 52, 54, 255)
            rect(d, (x, y, x + 3, y + 3), fill)
            put(img, x, y, light)
            put(img, x + 3, y + 3, dark)
    if detail:
        if spec.role in {"forge", "charger", "infuser"}:
            rect(d, (5, 4, 10, 11), shade(accent, 0.75))
            rect(d, (6, 5, 9, 10), shade(accent, 1.45))
        elif spec.role in {"terminal", "bench", "upgrade"}:
            rect(d, (3, 4, 12, 9), (18, 28, 31, 255))
            rect(d, (4, 5, 11, 8), shade(accent, 1.05))
            d.line((5, 10, 10, 10), fill=shade(accent, 1.45))
        elif spec.role == "rack":
            rect(d, (2, 3, 13, 13), (91, 62, 35, 255))
            for x in (4, 8, 11):
                d.line((x, 4, x, 12), fill=(180, 188, 184, 255))
        elif spec.role == "stand":
            d.line((8, 2, 8, 13), fill=light, width=2)
            rect(d, (5, 5, 11, 8), (133, 139, 139, 255))
            rect(d, (4, 13, 12, 14), dark)
        elif spec.role == "engraver":
            d.ellipse((3, 3, 12, 12), outline=shade(accent, 1.2), width=2)
            d.line((8, 4, 8, 11), fill=shade(accent, 1.4))
        elif spec.role == "dock":
            rect(d, (4, 3, 11, 12), (18, 28, 24, 255))
            d.line((5, 5, 10, 10), fill=shade(accent, 1.2))
            d.line((10, 5, 5, 10), fill=shade(accent, 1.2))
    return harden(img, palette_size=16)


def armor_uv(name: str, accent_hex: str, inner: bool = False) -> Image.Image:
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    accent = hex_rgba(accent_hex)
    dark = (22, 25, 27, 255)
    plate = (58, 65, 67, 255)
    light = (120, 130, 130, 255)
    shadow = (32, 37, 39, 255)
    islands = [
        (8, 8, 15, 15),
        (20, 20, 27, 31),
        (36, 20, 43, 31),
        (44, 20, 51, 31),
        (4, 20, 11, 31),
        (12, 20, 19, 31),
    ]
    if inner:
        islands = [(4, 8, 11, 19), (12, 8, 19, 19), (20, 8, 27, 19), (28, 8, 35, 19)]
    for i, (x0, y0, x1, y1) in enumerate(islands):
        rect(d, (x0, y0, x1, y1), dark)
        rect(d, (x0 + 1, y0 + 1, x1 - 1, y1 - 1), plate)
        d.line((x0 + 1, y0 + 1, x1 - 1, y0 + 1), fill=light)
        d.line((x0 + 1, y1 - 1, x1 - 1, y1 - 1), fill=shadow)
        put(img, x0 + 2, y0 + 3, shade(accent, 1.3))
        if i % 2 == 0:
            d.line((x0 + 2, y0 + 5, x1 - 2, y0 + 5), fill=shade(accent, 0.9))
    return harden(img, palette_size=16)


def element(from_: list[int], to: list[int], texture: str = "#base", faces: Iterable[str] | None = None) -> dict:
    face_names = list(faces or ("north", "south", "east", "west", "up", "down"))
    return {"from": from_, "to": to, "faces": {face: {"texture": texture} for face in face_names}}


def station_elements(spec: BlockSpec) -> list[dict]:
    base = [
        element([1, 0, 1], [15, 3, 15], "#base"),
        element([2, 3, 2], [14, 4, 14], "#detail", ("up", "north", "south", "east", "west")),
    ]
    role = spec.role
    if role == "bench":
        return base + [
            element([2, 4, 3], [14, 8, 12], "#base"),
            element([4, 8, 4], [12, 10, 10], "#detail"),
            element([12, 5, 4], [15, 10, 11], "#detail"),
        ]
    if role == "forge":
        return base + [
            element([2, 3, 2], [14, 13, 14], "#base"),
            element([4, 5, 1], [12, 11, 3], "#detail", ("north",)),
            element([5, 13, 5], [11, 15, 11], "#base"),
        ]
    if role == "charger":
        return base + [
            element([3, 3, 3], [13, 13, 13], "#base"),
            element([5, 4, 1], [11, 12, 3], "#detail", ("north",)),
            element([6, 13, 6], [10, 16, 10], "#detail"),
        ]
    if role == "upgrade":
        return base + [
            element([2, 3, 3], [14, 8, 13], "#base"),
            element([4, 8, 5], [12, 10, 11], "#detail"),
            element([3, 5, 1], [13, 7, 3], "#detail", ("north",)),
        ]
    if role == "engraver":
        return base + [
            element([2, 3, 2], [14, 7, 14], "#base"),
            element([4, 7, 4], [12, 9, 12], "#detail"),
            element([3, 4, 1], [13, 11, 3], "#detail", ("north",)),
            element([1, 7, 1], [4, 13, 4], "#base"),
            element([12, 7, 1], [15, 13, 4], "#base"),
        ]
    if role == "terminal":
        return base + [
            element([2, 3, 3], [14, 10, 13], "#base"),
            element([4, 7, 1], [12, 14, 3], "#detail", ("north",)),
            element([5, 4, 0], [11, 6, 2], "#detail", ("north",)),
        ]
    if role == "rack":
        return [
            element([1, 0, 2], [15, 2, 14], "#base"),
            element([2, 2, 3], [14, 13, 5], "#detail"),
            element([3, 3, 5], [5, 13, 6], "#base"),
            element([7, 3, 5], [9, 13, 6], "#base"),
            element([11, 3, 5], [13, 13, 6], "#base"),
        ]
    if role == "stand":
        return [
            element([5, 0, 5], [11, 2, 11], "#base"),
            element([7, 2, 7], [9, 14, 9], "#base"),
            element([4, 6, 5], [12, 9, 11], "#detail"),
            element([3, 14, 6], [13, 16, 10], "#detail"),
        ]
    if role == "infuser":
        return base + [
            element([2, 3, 2], [14, 14, 14], "#base"),
            element([5, 4, 1], [11, 13, 3], "#detail", ("north",)),
            element([4, 14, 4], [12, 16, 12], "#detail"),
        ]
    if role == "dock":
        return base + [
            element([2, 3, 3], [14, 8, 13], "#base"),
            element([4, 8, 4], [12, 15, 12], "#detail"),
            element([1, 5, 5], [3, 13, 11], "#base"),
            element([13, 5, 5], [15, 13, 11], "#base"),
        ]
    return base


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def write_models() -> None:
    for spec in BLOCKS:
        write_json(
            MODELS / "block" / f"{spec.name}.json",
            {
                "parent": "minecraft:block/block",
                "ambientocclusion": True,
                "textures": {
                    "particle": f"{MODID}:block/{spec.name}",
                    "base": f"{MODID}:block/{spec.name}",
                    "detail": f"{MODID}:block/{spec.name}_detail",
                },
                "elements": station_elements(spec),
            },
        )
        write_json(MODELS / "item" / f"{spec.name}.json", {"parent": f"{MODID}:block/{spec.name}"})
        write_json(ITEM_DEFS / f"{spec.name}.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:block/{spec.name}"}})
        write_json(
            ASSETS / "blockstates" / f"{spec.name}.json",
            {
                "variants": {
                    "facing=north": {"model": f"{MODID}:block/{spec.name}"},
                    "facing=east": {"model": f"{MODID}:block/{spec.name}", "y": 90},
                    "facing=south": {"model": f"{MODID}:block/{spec.name}", "y": 180},
                    "facing=west": {"model": f"{MODID}:block/{spec.name}", "y": 270},
                }
            },
        )
    for spec in ITEMS:
        write_json(
            MODELS / "item" / f"{spec.name}.json",
            {"parent": spec.parent, "textures": {"layer0": f"{MODID}:item/{spec.name}"}},
        )
        write_json(ITEM_DEFS / f"{spec.name}.json", {"model": {"type": "minecraft:model", "model": f"{MODID}:item/{spec.name}"}})


def write_equipment() -> None:
    for name, layer, slot_name, _accent in ARMOR:
        write_json(EQUIPMENT / f"{name}.json", {"layers": {layer: [{"texture": f"{MODID}:{name}/{slot_name}"}]}})


def write_textures() -> None:
    for spec in ITEMS:
        out = TEXTURES / "item" / f"{spec.name}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        draw_item(spec).save(out)
    for spec in BLOCKS:
        for detail in (False, True):
            suffix = "_detail" if detail else ""
            out = TEXTURES / "block" / f"{spec.name}{suffix}.png"
            out.parent.mkdir(parents=True, exist_ok=True)
            draw_block_texture(spec, detail).save(out)
    for name, layer, slot_name, accent in ARMOR:
        out = TEXTURES / "entity/equipment" / layer / name / f"{slot_name}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        armor_uv(name, accent, inner=layer == "humanoid_leggings").save(out)


def sheet_crop_map() -> dict:
    def dims(name: str) -> tuple[int, int]:
        path = SOURCE / name
        if not path.exists():
            return (0, 0)
        with Image.open(path) as img:
            return img.size

    iw, ih = dims("items_source_transparent.png")
    bw, bh = dims("blocks_source_transparent.png")
    aw, ah = dims("armor_source_transparent.png")
    item_cells = []
    cols, rows = 8, 5
    for i, spec in enumerate(ITEMS):
        col, row = i % cols, i // cols
        x0, y0 = round(col * iw / cols), round(row * ih / rows)
        x1, y1 = round((col + 1) * iw / cols), round((row + 1) * ih / rows)
        item_cells.append({"id": spec.name, "source": "items_source_transparent.png", "cell": i, "crop": [x0, y0, x1, y1], "repair": "local pixel redraw"})

    block_cells = []
    cols, rows = 3, 4
    for i, spec in enumerate(BLOCKS):
        col, row = i % cols, i // cols
        x0, y0 = round(col * bw / cols), round(row * bh / rows)
        x1, y1 = round((col + 1) * bw / cols), round((row + 1) * bh / rows)
        block_cells.append({"id": spec.name, "source": "blocks_source_transparent.png", "cell": i, "crop": [x0, y0, x1, y1], "repair": "local model/texture redraw"})

    armor_cells = []
    cols, rows = 2, 3
    for i, (name, layer, slot_name, _accent) in enumerate(ARMOR):
        col, row = i % cols, i // cols
        x0, y0 = round(col * aw / cols), round(row * ah / rows)
        x1, y1 = round((col + 1) * aw / cols), round((row + 1) * ah / rows)
        armor_cells.append({"id": name, "source": "armor_source_transparent.png", "layer": layer, "slot_texture": slot_name, "cell": i, "crop": [x0, y0, x1, y1], "repair": "local UV redraw"})

    return {"items": item_cells, "blocks": block_cells, "armor": armor_cells}


def paste_scaled(sheet: Image.Image, sprite: Image.Image, x: int, y: int, scale: int) -> None:
    sheet.alpha_composite(sprite.resize((sprite.width * scale, sprite.height * scale), Image.Resampling.NEAREST), (x, y))


def write_previews() -> None:
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    item_cols = 12
    item_cell = 24
    item_rows = math.ceil(len(ITEMS) / item_cols)
    item_sheet = Image.new("RGBA", (item_cols * item_cell, item_rows * item_cell), (24, 26, 27, 255))
    for i, spec in enumerate(ITEMS):
        with Image.open(TEXTURES / "item" / f"{spec.name}.png") as icon:
            paste_scaled(item_sheet, icon.convert("RGBA"), (i % item_cols) * item_cell + 4, (i // item_cols) * item_cell + 4, 1)
    item_sheet.save(PREVIEWS / "items_inventory_sheet.png")

    block_cols = 6
    block_cell = 48
    block_rows = math.ceil(len(BLOCKS) / block_cols)
    block_sheet = Image.new("RGBA", (block_cols * block_cell, block_rows * block_cell), (24, 26, 27, 255))
    for i, spec in enumerate(BLOCKS):
        with Image.open(TEXTURES / "block" / f"{spec.name}_detail.png") as tex:
            paste_scaled(block_sheet, tex.convert("RGBA"), (i % block_cols) * block_cell, (i // block_cols) * block_cell, 3)
    block_sheet.save(PREVIEWS / "blocks_1x_contact_sheet.png")

    armor_sheet = Image.new("RGBA", (3 * 144, 2 * 96), (24, 26, 27, 255))
    for i, (name, layer, slot_name, _accent) in enumerate(ARMOR):
        with Image.open(TEXTURES / "entity/equipment" / layer / name / f"{slot_name}.png") as tex:
            paste_scaled(armor_sheet, tex.convert("RGBA"), (i % 3) * 144 + 8, (i // 3) * 96 + 16, 2)
    armor_sheet.save(PREVIEWS / "armor_equipment_sheet.png")


def validate() -> list[str]:
    errors: list[str] = []
    for spec in ITEMS:
        path = TEXTURES / "item" / f"{spec.name}.png"
        if not path.exists():
            errors.append(f"missing item texture {path}")
            continue
        with Image.open(path) as img:
            rgba = img.convert("RGBA")
            if rgba.size != (16, 16):
                errors.append(f"{path} is {rgba.size}, expected 16x16")
            for corner in ((0, 0), (15, 0), (0, 15), (15, 15)):
                if rgba.getpixel(corner)[3] != 0:
                    errors.append(f"{path} corner {corner} is not transparent")
            colors = rgba.getcolors(maxcolors=10_000) or []
            if len(colors) > 32:
                errors.append(f"{path} has {len(colors)} colors, expected <= 32")
            if any(0 < px[3] < 255 for _count, px in colors):
                errors.append(f"{path} has semi-transparent pixels")
    for spec in BLOCKS:
        for suffix in ("", "_detail"):
            path = TEXTURES / "block" / f"{spec.name}{suffix}.png"
            if not path.exists():
                errors.append(f"missing block texture {path}")
                continue
            with Image.open(path) as img:
                rgba = img.convert("RGBA")
                if rgba.size != (16, 16):
                    errors.append(f"{path} is {rgba.size}, expected 16x16")
                colors = rgba.getcolors(maxcolors=10_000) or []
                if len(colors) > 32:
                    errors.append(f"{path} has {len(colors)} colors, expected <= 32")
                if any(0 < px[3] < 255 for _count, px in colors):
                    errors.append(f"{path} has semi-transparent pixels")
    for name, layer, slot_name, _accent in ARMOR:
        path = TEXTURES / "entity/equipment" / layer / name / f"{slot_name}.png"
        if not path.exists():
            errors.append(f"missing armor texture {path}")
            continue
        with Image.open(path) as img:
            rgba = img.convert("RGBA")
            if rgba.size != (64, 32):
                errors.append(f"{path} is {rgba.size}, expected 64x32")
            colors = rgba.getcolors(maxcolors=10_000) or []
            if len(colors) > 32:
                errors.append(f"{path} has {len(colors)} colors, expected <= 32")
            if any(0 < px[3] < 255 for _count, px in colors):
                errors.append(f"{path} has semi-transparent pixels")
    return errors


def generate() -> None:
    write_textures()
    write_models()
    write_equipment()
    write_json(SOURCE / "crop_map.json", sheet_crop_map())
    write_previews()
    errors = validate()
    if errors:
        raise SystemExit("Asset validation failed:\n" + "\n".join(f"- {error}" for error in errors))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="validate generated assets without writing them")
    args = parser.parse_args()
    if args.check:
        errors = validate()
        if errors:
            raise SystemExit("Asset validation failed:\n" + "\n".join(f"- {error}" for error in errors))
        print("EchoArmory pixel assets validated.")
        return 0
    generate()
    print(f"Generated {len(ITEMS)} item textures, {len(BLOCKS)} block model sets, and {len(ARMOR)} armor equipment textures.")
    print(f"Wrote previews to {PREVIEWS}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

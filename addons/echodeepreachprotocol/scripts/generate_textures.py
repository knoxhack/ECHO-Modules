#!/usr/bin/env python3
"""Generate deterministic placeholder entity textures for ECHO: Deep Reach Protocol."""

from __future__ import annotations

import random
from pathlib import Path

from PIL import Image, ImageDraw


REPO_ROOT = Path(__file__).resolve().parents[3]
ADDON_ROOT = Path(__file__).resolve().parents[1]
TEXTURES_DIR = ADDON_ROOT / "src" / "main" / "resources" / "assets" / "echodeepreachprotocol" / "textures" / "entity"

SIZE = 64


def seed_for(name: str) -> random.Random:
    rng = random.Random()
    rng.seed(name, version=2)
    return rng


def lighten(color: tuple[int, int, int], amount: int) -> tuple[int, int, int]:
    return tuple(min(255, c + amount) for c in color)


def darken(color: tuple[int, int, int], amount: int) -> tuple[int, int, int]:
    return tuple(max(0, c - amount) for c in color)


def noise_overlay(draw: ImageDraw.ImageDraw, rng: random.Random, box: tuple[int, int, int, int],
                  color: tuple[int, int, int], density: float = 0.15) -> None:
    x0, y0, x1, y1 = box
    for x in range(x0, x1):
        for y in range(y0, y1):
            if rng.random() < density:
                draw.point((x, y), fill=color)


def draw_eyes(draw: ImageDraw.ImageDraw, rng: random.Random, cx: int, cy: int, spacing: int = 10,
              color: tuple[int, int, int] = (255, 80, 80)) -> None:
    draw.ellipse([cx - spacing - 2, cy - 2, cx - spacing + 2, cy + 2], fill=color)
    draw.ellipse([cx + spacing - 2, cy - 2, cx + spacing + 2, cy + 2], fill=color)


def make_twilight_stalker(name: str) -> Image.Image:
    rng = seed_for(name)
    base = (42, 18, 62)
    mid = (78, 36, 108)
    light = (130, 80, 160)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # tall slender body
    draw.polygon([(32, 8), (48, 56), (32, 48), (16, 56)], fill=mid, outline=base)
    # shoulders/head
    draw.ellipse([22, 6, 42, 26], fill=light, outline=base)
    draw_eyes(draw, rng, 32, 16, spacing=5, color=(220, 40, 80))
    noise_overlay(draw, rng, (16, 8, 48, 56), base, 0.08)
    return img


def make_vent_crab(name: str) -> Image.Image:
    rng = seed_for(name)
    shell = (160, 48, 24)
    belly = (200, 110, 60)
    legs = (120, 36, 18)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # legs
    for angle in range(-30, 31, 15):
        x = int(32 + 22 * (angle / 45.0))
        y = int(44 - abs(angle) / 3)
        draw.line([(32, 38), (x, y)], fill=legs, width=3)
    # shell
    draw.ellipse([14, 24, 50, 46], fill=shell, outline=darken(shell, 30))
    draw.ellipse([18, 28, 46, 42], fill=belly, outline=None)
    draw_eyes(draw, rng, 32, 30, spacing=6, color=(40, 220, 180))
    # steam vents
    draw.rectangle([28, 18, 30, 24], fill=(80, 80, 80))
    draw.rectangle([34, 18, 36, 24], fill=(80, 80, 80))
    return img


def make_abyssal_leviathan(name: str) -> Image.Image:
    rng = seed_for(name)
    body = (18, 42, 82)
    belly = (42, 90, 140)
    fin = (12, 30, 66)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # serpentine body
    draw.ellipse([8, 20, 56, 44], fill=body, outline=darken(body, 30))
    draw.ellipse([12, 24, 52, 40], fill=belly, outline=None)
    # tail
    draw.polygon([(8, 32), (2, 24), (2, 40)], fill=fin, outline=darken(fin, 20))
    # dorsal fins
    draw.polygon([(24, 20), (28, 10), (32, 20)], fill=fin)
    draw.polygon([(40, 20), (44, 10), (48, 20)], fill=fin)
    # head
    draw.ellipse([48, 22, 58, 42], fill=body, outline=darken(body, 30))
    draw_eyes(draw, rng, 54, 30, spacing=3, color=(120, 220, 255))
    return img


def make_lattice_sentinel(name: str) -> Image.Image:
    rng = seed_for(name)
    core = (24, 120, 120)
    crystal = (80, 255, 240)
    dark = (8, 40, 50)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # floating crystal shards
    for ox, oy in [(12, 12), (44, 14), (10, 44), (46, 42)]:
        draw.polygon([(ox, oy), (ox + 8, oy + 4), (ox + 4, oy + 12)], fill=core, outline=crystal)
    # central body
    draw.ellipse([20, 20, 44, 44], fill=dark, outline=core)
    draw.ellipse([24, 24, 40, 40], fill=core, outline=None)
    draw_eyes(draw, rng, 32, 32, spacing=4, color=(255, 255, 255))
    return img


def make_bloater(name: str) -> Image.Image:
    rng = seed_for(name)
    body = (140, 170, 60)
    spots = (80, 110, 30)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # puffy body
    draw.ellipse([14, 16, 50, 48], fill=body, outline=darken(body, 30))
    # little fins
    draw.polygon([(14, 32), (8, 26), (8, 38)], fill=darken(body, 20))
    draw.polygon([(50, 32), (56, 26), (56, 38)], fill=darken(body, 20))
    draw_eyes(draw, rng, 32, 28, spacing=5, color=(40, 40, 40))
    # spots
    for _ in range(8):
        x = rng.randint(18, 46)
        y = rng.randint(20, 44)
        r = rng.randint(1, 3)
        draw.ellipse([x - r, y - r, x + r, y + r], fill=spots)
    return img


def make_hadal_wraith(name: str) -> Image.Image:
    rng = seed_for(name)
    body = (12, 12, 20)
    glow = (60, 120, 220)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # hooded shape
    draw.polygon([(32, 6), (50, 56), (32, 48), (14, 56)], fill=body, outline=(0, 0, 0))
    # face void with glow
    draw.ellipse([24, 14, 40, 30], fill=(0, 0, 0), outline=glow)
    draw_eyes(draw, rng, 32, 22, spacing=4, color=glow)
    # trailing energy particles
    for _ in range(10):
        x = rng.randint(20, 44)
        y = rng.randint(40, 60)
        draw.point((x, y), fill=glow)
    return img


def make_lattice_bolt(name: str) -> Image.Image:
    rng = seed_for(name)
    core = (80, 255, 240)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # elongated energy bolt
    draw.ellipse([20, 24, 44, 40], fill=core, outline=(200, 255, 255))
    draw.ellipse([26, 28, 38, 36], fill=(200, 255, 255))
    # trailing sparks
    for _ in range(6):
        x = rng.randint(8, 22)
        y = rng.randint(28, 36)
        draw.point((x, y), fill=core)
    return img


def make_remora(name: str) -> Image.Image:
    rng = seed_for(name)
    hull = (180, 160, 60)
    cockpit = (80, 200, 220)
    metal = (90, 90, 90)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # submarine hull
    draw.ellipse([6, 20, 58, 44], fill=hull, outline=darken(hull, 40))
    # cockpit dome
    draw.ellipse([36, 16, 50, 30], fill=cockpit, outline=metal)
    # conning tower
    draw.rectangle([34, 10, 42, 22], fill=darken(hull, 20), outline=metal)
    # periscope
    draw.line([(38, 10), (38, 4)], fill=metal, width=2)
    draw.ellipse([36, 2, 40, 6], fill=metal)
    # propeller / tail
    draw.polygon([(6, 26), (2, 22), (2, 42), (6, 38)], fill=metal, outline=darken(metal, 30))
    # portholes
    for x in (16, 24):
        draw.ellipse([x - 2, 28, x + 2, 32], fill=(60, 60, 60), outline=metal)
    return img


TEXTURES = {
    "twilight_stalker": make_twilight_stalker,
    "vent_crab": make_vent_crab,
    "abyssal_leviathan": make_abyssal_leviathan,
    "lattice_sentinel": make_lattice_sentinel,
    "bloater": make_bloater,
    "hadal_wraith": make_hadal_wraith,
    "remora": make_remora,
    "lattice_bolt": make_lattice_bolt,
}


def main() -> None:
    TEXTURES_DIR.mkdir(parents=True, exist_ok=True)
    for name, factory in TEXTURES.items():
        img = factory(name)
        path = TEXTURES_DIR / f"{name}.png"
        img.save(path)
        print(f"Wrote {path} ({img.width}x{img.height})")


if __name__ == "__main__":
    main()

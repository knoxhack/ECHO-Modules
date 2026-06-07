# ECHO ThemeForge Image Pipeline

ThemeForge is the development-time art pipeline for ECHO ThemeCore 1.3.0. It organizes prompts and generated PNG assets for CyberGlass, Nexus, RenderCore references, vanilla UI skin assets, icons, Blockworks palettes, and publishing art.

The Minecraft mod must not call image-generation APIs at runtime. Keep generated PNGs in resources and ship them like normal mod assets.

## Paths

```text
tools/echo-themeforge/themeforge.py
tools/echo-themeforge/input/themeforge_config.json
tools/echo-themeforge/generated/prompts/
tools/echo-themeforge/generated/assets/<theme>/
tools/echo-themeforge/generated/previews/
tools/echo-themeforge/generated/reports/
addons/echothemecore/src/main/resources/assets/echothemecore/textures/gui/themes/<theme>/
```

## Commands

```powershell
python tools/echo-themeforge/themeforge.py prompts
python tools/echo-themeforge/themeforge.py validate
python tools/echo-themeforge/themeforge.py validate --theme cyberglass --strict
python tools/echo-themeforge/themeforge.py validate --theme nexus --strict
python tools/echo-themeforge/themeforge.py report
python tools/echo-themeforge/themeforge.py report --theme cyberglass
python tools/echo-themeforge/themeforge.py report --theme nexus
python tools/echo-themeforge/themeforge.py apply
```

Use `apply --force` only when replacing an existing approved resource intentionally.
Use strict validation for both CyberGlass and Nexus as the 1.3.0 release gate. It fails on missing referenced assets, unreadable packaged PNGs, missing generated assets, missing source sheets, lowercase filename violations, unusual PNG dimensions, and forbidden legacy display-line terms.

## Naming Rules

- PNG only.
- Lowercase filenames.
- No spaces.
- Keep UI textures small and tile-friendly.
- Icons should be centered, transparent, and readable at 16x16, 32x32, and 64x64.
- Overlays should use transparent backgrounds.
- Publishing art may include minimal text: `ECHO THEMECORE`, `CYBERGLASS`, or `NEXUS`.

## CyberGlass Art Direction

CyberGlass is clean cyberpunk glass: dark navy/black translucent panels, cyan hologram glow, magenta/violet accents, thin neon borders, smooth gradients, soft bloom, and clean geometric circuit patterns.

## Nexus Art Direction

Nexus is cold anomaly glass: deep blue-black panels, cyan energy, violet phase light, crystalline edges, subtle ripple motion, and endgame mystery without dirty grunge.

## Asset Checklist

- Terminal UI kit
- HoloMap UI kit
- Lens UI kit
- Icons
- RenderCore references
- Blockworks palette references
- Vanilla UI skin assets
- Publishing art

## RenderCore Workflow

Generate reference sheets under `generated/assets/<theme>/rendercore/`, review them manually, then run `themeforge.py apply` to copy approved PNGs into ThemeCore resources. RenderCore consumers should resolve colors/intensities from `EchoRenderThemeProvider` and use preset JSONs under `data/<namespace>/render_presets/` through the ThemeCore render preset registry.

## Vanilla UI Workflow

Generate frame, tooltip, toast, boss-bar, hotbar, and widget-outline assets for each theme. ThemeCore 1.3.0 uses overlays and accents only. Do not replace vanilla screens or mutate slot/widget behavior.

## Publishing Art Workflow

Generate banner, desktop wallpaper, mobile wallpaper, overview card, and feature sheet assets for release pages. Keep text minimal and do not include external brands.

Visual rule: ThemeCore assets must avoid legacy CRT line overlays and old-monitor effects. Use glass panels, hologram glow, neon borders, energy edges, glints, and phase ripples instead.

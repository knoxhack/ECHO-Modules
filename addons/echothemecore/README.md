<!-- CURSEFORGE_README_START -->
# ThemeCore by ECHO Labs

![ThemeCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echothemecore/brand-sheet.png)

**ThemeCore is the shared visual, audio, UI, and vanilla Minecraft skin service for the ECHO/Ashfall ecosystem.**

![ThemeCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echothemecore/features-portrait.png)

![ThemeCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echothemecore/features-landscape.png)

## CurseForge Summary

ThemeCore is the shared visual, audio, UI, and vanilla Minecraft skin service for the ECHO/Ashfall ecosystem.

## Main Features

- Theme palettes and tokens.
- Cyberglass UI styling.
- Reusable visual kits.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echothemecore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echothemecore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echothemecore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO ThemeCore 1.3.0

ECHO ThemeCore is the shared visual, audio, UI, and vanilla Minecraft skin service for the ECHO/Ashfall ecosystem. It is a first-party beta service/API addon with mod id `echothemecore` and package `com.knoxhack.echothemecore`.

ThemeCore hard-requires `echocore` and `echonetcore` for shared services and theme sync. Terminal, SignalOS, HoloMap, Lens, RenderCore, SoundCore, Blockworks, RuntimeGuard, and future addons consume it through optional-safe APIs and provider interfaces.

## Themes

- `echothemecore:cyberglass` is the default and fallback theme.
- `echothemecore:nexus` is the endgame anomaly theme.

Themes are loaded from datapacks at:

```text
data/<namespace>/themes/*.json
```

The legacy `data/<namespace>/echothemecore/themes/*.json` path is still scanned as a compatibility alias, but new datapacks should use the root `themes` folder.

Render presets are reloadable data at:

```text
data/<namespace>/render_presets/*.json
```

Bad theme JSON is logged and skipped. Missing selections fall back to CyberGlass.
When ECHO Terminal is loaded, ThemeCore registers CyberGlass as a Terminal theme and makes it the Terminal default for fresh client configs. Existing valid Terminal theme selections are preserved.

## Config

ThemeCore publishes common and client config groups:

- `[theme]` default/fallback ids, player overrides, server sync, and module scope flags
- `[client]` overlays, transitions, and particle/glow controls
- `[accessibility]` high contrast, reduced glow, distortion/noise controls
- `[vanilla_ui]` vanilla screen surface toggles
- `[vanilla_ui_safety]` conservative vanilla UI safety behavior
- `[vanilla_ui_style]` vanilla UI visual styling toggles

The vanilla UI layer is client-only and uses conservative overlays. It does not replace screens, mutate slots, move widgets, change recipes, or alter gameplay behavior.

## Commands

```text
/echo_theme current
/echo_theme list
/echo_theme set <theme_id>
/echo_theme player set <player> <theme_id>
/echo_theme player clear <player>
/echo_theme reset
/echo_theme reload
/echo_theme preview <theme_id>
/echo_theme preset list
/echo_theme preset preview <preset_id>
/echo_theme visual current
/echo_theme visual test terminal
/echo_theme visual test holomap
/echo_theme visual test lens
/echo_theme visual test particles
/echo_theme visual intensity <0.0-2.0>
/echo_theme vanilla current
```

Low-permission players can inspect current/list/preview. Theme mutation, reload guidance, and visual intensity require game master permissions.

## API Example

```java
EchoTheme theme = EchoThemeApi.getTheme(player);
int panel = EchoThemeApi.color(player, EchoThemeColorKey.PANEL);
Optional<Identifier> panelTexture = EchoThemeApi.getTexture(player, EchoThemeTextureKey.PANEL);
ThemeVisualSettings visuals = EchoThemeApi.getEffectiveVisualSettings(player);
List<EchoThemeRenderPreset> presets = EchoThemeApi.getRenderPresets(theme.id());
```

## Optional Integrations

ThemeCore exposes provider interfaces for ECHO modules:

- `EchoThemedUiProvider` for Terminal and SignalOS UI colors/textures
- `EchoHoloMapThemeProvider` for map lines, markers, opacity, and overlays
- `EchoLensThemeProvider` for scan rings and target highlights
- `EchoSoundThemeProvider` for SoundCore sound/theme music references
- `EchoBlockPaletteProvider` for Blockworks palettes
- `EchoRenderThemeProvider` for RenderCore visual profiles
- `EchoRuntimeGuardThemeProvider` for performance-aware visual reductions

RenderCore remains the engine for advanced VFX. ThemeCore supplies style data and a no-op-safe bridge when RenderCore is absent.

## ScreenCore Cyberglass Kit

ScreenCore pages can opt in to the shared Cyberglass kit with `styles="echothemecore:cyberglass_kit"` or the same style id in an EUI manifest. The kit provides ThemeCore-owned transparent PNG surfaces, buttons, chips, focus rings, edge rails, progress styling, and reusable templates such as `echothemecore:cyberglass_panel_shell` and `echothemecore:cyberglass_confirm_dialog`.

See `docs/CYBERGLASS_SCREENCORE_KIT.md` for addon usage examples and supported `screencore.*` texture tokens.

## Vanilla UI Skin Layer

The client layer classifies vanilla screens and applies safe accents to:

- title, pause, options, world select, multiplayer, and loading screens
- inventory, creative inventory, containers, crafting, furnace, anvil, enchanting, grindstone, and smithing screens
- advancements and recipe-book-adjacent screens where safe
- hotbar, selected slot, boss bar, chat accent, tooltips, and toasts where hooks are available

Container handling protects slot interiors by drawing only around panel bounds or at screen edges.

## Adding A Theme

1. Add `data/<namespace>/themes/<theme>.json`.
2. Include required `id`, `display_name`, `colors`, and optional `ui`, `render`, `sound`, `block_palette`, `vanilla_ui`, and `metadata`.
3. Add referenced PNGs under `assets/<namespace>/textures/gui/themes/<theme>/`.
4. Run `/reload`.
5. Use `/echo_theme list` and `/echo_theme preview <theme_id>`.

## ThemeForge

ThemeForge lives at `tools/echo-themeforge/`. It generates development-time prompt packs, validates PNG outputs, creates reports, and safely copies approved generated PNGs into ThemeCore resources. It has no runtime dependency and never belongs in the Minecraft game loop.
Use `python tools/echo-themeforge/themeforge.py validate --theme cyberglass --strict` and `python tools/echo-themeforge/themeforge.py validate --theme nexus --strict` for 1.3.0 release gates. Strict validation checks packaged PNG references, generated assets, source sheets, lowercase filenames, readable PNG signatures/dimensions, and forbidden legacy display-line terms.

Visual rule: ThemeCore uses clean futuristic glass, hologram glow, thin neon borders, geometric circuitry, edge pulses, energy overlays, glints, and phase ripples. Legacy CRT line-overlay styling is forbidden for ThemeCore assets and theme data.

## 1.3.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echothemecore.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echothemecore.md`.

# Universal Theme Asset Kits

Theme assets live under:

```text
src/main/resources/assets/echothemecore/textures/gui/themes/<theme>/
```

Required folders:

- `icons/`
- `rendercore/`
- `screencore/`
- `loading/`
- `menu/`
- `hud/`
- `item_icon/`

Opaque assets are used for backgrounds, loading screens, wallpapers, banners, overview cards, and feature sheets. Transparent assets are used for UI chrome, overlays, icons, HUD accents, item frames, badges, rings, Lens/HoloMap overlays, ScreenCore surfaces, and RenderCore reference overlays.

## Required Universal Tokens

Loading:

- `loading.background`
- `loading.panel`
- `loading.progress_bar`
- `loading.spinner`
- `loading.logo_mark`

Menu:

- `menu.main_backplate`
- `menu.pause_panel`
- `menu.options_panel`
- `menu.world_row`
- `menu.mods_panel`

HUD:

- `hud.hotbar_frame`
- `hud.selected_slot`
- `hud.crosshair_accent`
- `hud.boss_bar`
- `hud.chat_panel`
- `hud.notification_chip`

Item icon:

- `item_icon.frame`
- `item_icon.rarity_ring`
- `item_icon.badge`
- `item_icon.lock_overlay`
- `item_icon.mission_marker`

## ThemeForge

The root `tools/echo-themeforge` config includes `cyberglass`, `nexus`, `cyberconsole`, `ashfall`, and `magic`. Asset groups include:

- `screencore_ui`
- `loading_ui`
- `menu_ui`
- `hud_ui`
- `item_icon_ui`

Validate a theme:

```bash
python tools/echo-themeforge/themeforge.py validate --theme cyberglass
```

Apply generated assets:

```bash
python tools/echo-themeforge/themeforge.py apply --theme ashfall --force
```

Theme JSON `module_assets` must reference all required groups before a theme should be marked public.

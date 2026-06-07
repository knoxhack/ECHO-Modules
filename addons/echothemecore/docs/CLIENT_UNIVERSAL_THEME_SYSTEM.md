# Client Universal Theme System

ThemeCore now treats the active visual theme as client-local state. `ClientThemeState` is the rendering authority for menus, ScreenCore pages, HUD overlays, loading screens, buttons, RenderCore styling, Lens, HoloMap, Index, Terminal surfaces, and item icon chrome.

Servers do not select, sync, validate, or observe the client visual theme. Older common APIs and packets remain for compatibility, but client render paths resolve through:

- `EchoThemeApi.getClientTheme()`
- `EchoThemeApi.getClientThemeId()`
- `EchoThemeApi.setClientTheme(Identifier id)`
- `EchoThemeApi.cycleClientTheme(int direction)`
- `EchoThemeApi.listPublicClientThemes()`

Optional client integrations, including Terminal, follow the same local state. When the local theme changes, ThemeCore registers the matching public ThemeCore terminal adapter if needed and selects it through Terminal's own client options; no server packet participates in that flow.

## Config

Client config owns the local selection and replacement behavior:

- `local_client_theme`
- `client_theme_mode = FULL | HYBRID | OVERLAY`
- `enable_button_replacement`
- `enable_loading_replacement`
- `enable_menu_replacement`
- `enable_inventory_replacement`
- `enable_item_icon_chrome`
- `enable_safe_fallback`

`FULL` enables mixin replacement paths where available. `HYBRID` keeps replacement renderers available for guarded surfaces. `OVERLAY` leaves the event-based overlay layer as the visual path.

## Controls

Client key mappings:

- Next Theme
- Previous Theme
- Open Theme Picker

Client command:

```mcfunction
/echo_theme_client current
/echo_theme_client list
/echo_theme_client set echothemecore:ashfall
/echo_theme_client cycle next
/echo_theme_client cycle previous
/echo_theme_client reset
```

Invalid stored ids fall back to `echothemecore:cyberglass`.

## Public Cycle

Public themes are sorted by `cycle_order`, then display name, then id. Canonical public ids:

- `echothemecore:cyberglass`
- `echothemecore:cyberconsole`
- `echothemecore:ashfall`
- `echothemecore:magic`
- `echothemecore:nexus`

Compatibility aliases:

- `echothemecore:tech_console` -> `echothemecore:cyberconsole`
- `echothemecore:magic_grimoire` -> `echothemecore:magic`

## Safety

Replacement rendering must not move slots, alter recipes, change input behavior, or affect gameplay state. When replacement hooks fail and `enable_safe_fallback` is true, vanilla rendering continues and the existing overlay layer remains available.

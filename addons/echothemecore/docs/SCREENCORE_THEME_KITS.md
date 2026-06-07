# ScreenCore Theme Kits

ScreenCore should treat ThemeCore as the universal token provider when ThemeCore is loaded.

Preferred kit:

```text
echothemecore:universal_theme_kit
```

Compatibility alias:

```text
echothemecore:cyberglass_kit
```

Use `theme(...)` for color tokens and `theme-texture(...)` for texture tokens:

```css
panel {
  background: theme(panel);
  border-color: theme(borderStrong);
  background-texture: theme-texture(screencore.surface.raised);
}

button:hover {
  border-color: theme(accent);
  background-texture: theme-texture(screencore.button.hover);
}
```

Universal ScreenCore texture tokens:

- `screencore.surface.base`
- `screencore.surface.raised`
- `screencore.surface.floating`
- `screencore.button`
- `screencore.button.hover`
- `screencore.status_chip`
- `screencore.progress_bar`
- `screencore.focus_ring`
- `screencore.corner_cuts`
- `screencore.edge_rails`
- `screencore.panel_sheen`
- `screencore.micro_ticks`

The client theme picker page is `echothemecore:client_theme_picker`. Its buttons call ScreenCore actions registered by ThemeCore when ScreenCore is present:

- `echothemecore.set_client_theme`
- `echothemecore.cycle_client_theme`

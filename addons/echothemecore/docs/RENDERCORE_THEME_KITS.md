# RenderCore Theme Kits

RenderCore integrations should read the local client theme through `EchoThemeApi.getClientTheme()` or the ThemeCore RenderCore provider. Do not use server-selected theme state for client visuals.

Theme JSON render fields drive:

- hologram color and secondary color
- particle primary and secondary colors
- emissive colors
- edge, warning, success, and error glow colors
- glow, emissive, glass, hologram, particle, and animation intensities
- hologram, particle, distortion, overlay, and transition styles

Reference texture tokens:

- `rendercore.glow_overlay`
- `rendercore.distortion_overlay`
- `rendercore.entity_highlight`
- `rendercore.multiblock_energy`

Asset location:

```text
assets/echothemecore/textures/gui/themes/<theme>/rendercore/
```

Expected files:

- `glow_overlay_reference.png`
- `distortion_overlay.png`
- `entity_highlight_reference.png`
- `multiblock_energy_lines.png`
- `hologram_style_reference.png`
- `particle_style_reference.png`
- `terminal_boot_effect_reference.png`
- `lens_scan_effect_reference.png`
- `holomap_route_effect_reference.png`

RenderCore effects should remain visual-only. They may add glow, particles, distortion, outlines, or screen-space overlays, but they must not change gameplay state or server logic.

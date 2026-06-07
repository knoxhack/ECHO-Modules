# RenderCore V21 Screen Chrome Guide

V21 screen chrome defaults to clean cyberglass: translucent panes, crisp edges, cyan/magenta accent rails, subtle glints, and no scanline banding.

## Builder Presets

```java
RenderCoreScreenFrameOptions.terminal("ECHO TERMINAL").build();
RenderCoreScreenFrameOptions.cyberglass("").backdrop(false).quietFallback(true).build();
RenderCoreScreenFrameOptions.hologram("HOLOMAP").chromaticEdge(true).build();
RenderCoreScreenFrameOptions.neon("ALERT").build();
RenderCoreScreenFrameOptions.minimal().build();
RenderCoreScreenFrameOptions.fromProfile(profile, RenderCoreScreenFrameOptions.cyberglass("").build());
```

Use `TERMINAL` for dense command screens, `CYBERGLASS` for overlays, `HOLOGRAM` for maps and lenses, `NEON` for high-emphasis showcases, and `MINIMAL` for fallback UI. V12 `screen_chrome` metadata can drive frame options through `fromProfile(...)`; if metadata is unavailable the supplied fallback options are used, and unsupported style ids inherit the fallback style before defaulting to cyberglass. `scanlines(true)` is explicit opt-in only; release QA rejects unintended scanline bands.

## Required QA Surfaces

Capture `echo_terminal`, `echo_terminal_reduced_motion`, `signalos_terminal`, `signalos_rack`, `holomap_minimap`, `index_overlay`, `lens_overlay`, and `rendercore_cyberglass_example` with `/rendercore debug screenchrome evidence capture <surface>`.

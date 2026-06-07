# Terminal Cyberglass Theme Pass

## Why This Pass Exists

The old cyberglass Terminal was still the classic console renderer wearing cyan and violet paint. It kept the same dense frame, nested debug boxes, thin browser tabs, and table-like mission rows, so it read as a console skin instead of an ECHO OS glass interface.

This pass rebuilds cyberglass as an angular ScreenCore surface:

- Beveled translucent glass panes with layered fill, shadow, edge glow, inner highlight, and alpha texture overlays.
- ScreenCore-first Terminal shell when cyberglass ScreenCore mode is enabled, with the Java renderer kept as fallback.
- RenderCore cyberglass chrome and ThemeCore color resolution used where available, with a local ScreenCore painter as the safe path.
- Faceted top module tabs, nav rail, mission roadmap, briefing pane, action dock, and keyblock footer.
- Generated transparent glass assets for sheen, noise, edge bloom, vignette depth, and the Podfall hero backdrop.
- ScriptCore executable slots surfaced as ScreenCore actions, using trusted definition ids and slots instead of raw executable JSON.

## Smoke Test Checklist

1. Open Terminal with cyberglass enabled.
2. Verify it opens the ScreenCore shell by default.
3. Switch to Progress.
4. Select the active mission.
5. Select a locked mission.
6. Expand and collapse phases.
7. Use Jump to Active.
8. Verify mission requirements, side ops, action state, and tracking still update.
9. Open Command, Intel, Index, HoloMap, System, and ScriptCore tabs.
10. In ScriptCore, verify executable slots show the bridge/config state and disabled reasons.
11. Test compact, comfortable, and cinematic densities.
12. Disable cyberglass classic layout fallback and verify the classic renderer still opens.
13. Disable ScreenCore and verify Terminal still opens with no crash.
14. Compare before and after screenshots.

## Before

- Console layout with a theme recolor.
- Flat left sidebar list.
- Tiny boxed browser tabs.
- Table-like mission rows.
- Rectangular detail pane with nested boxes.
- Debug-looking footer and harsh progress bars.

## After

- ScreenCore angular cyberglass shell with translucent stacked panes.
- Beveled glass panels, faceted tabs, active nav rails, and keyblock footer controls.
- Mission roadmap with phase rows, mission records, status chips, and integrated progress.
- Briefing pane with hero art, mission status/type chips, next-step pane, side ops, requirements, and action dock.
- ScriptCore page that can send trusted `scriptcore.execute` intents and is ready for `scriptcore.preview` result feedback.
- Classic theme preserved as fallback.

## Known Limits

- Minecraft GUI rendering does not provide true blur here, so depth is simulated with translucent fills, alpha textures, shadow layers, and glow edges.
- ScriptCore execution remains opt-in through `scriptcore.allow_screencore_ui_actions`; disabled config is shown in the Terminal instead of bypassed.
- `scriptcore.preview` and typed UI params are forward-compatible hooks until the V2 bridge registers them.

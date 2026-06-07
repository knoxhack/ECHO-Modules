# Terminal ScreenCore Theme Guide

## Cyberglass Direction

`BuiltinTerminalThemes.CYBERGLASS` registers the Terminal theme under `echothemecore:cyberglass` so ThemeCore can provide the active palette. When ScreenCore is present and `useCyberglassScreenCoreTheme=true`, Terminal opens through the ScreenCore shell by default. The legacy Java renderer remains available through fallback and classic layout options.

Cyberglass is intentionally angular. The design uses beveled/faceted glass panes, thin low-alpha strokes, directional glints, and selective edge bloom. It should not use soft card rounding as the main identity.

Primary token roles:

- `bgVoid`: near-black blue background.
- `bgVeil`: dimmed navy overlay.
- `glassBase`: translucent deep-blue surfaces.
- `glassRaised`: elevated blue-gray panes and action docks.
- `glassHover`: cyan-blue hover surfaces.
- `glassActive`: cyan/violet active record base.
- `strokeSoft`: low-alpha cyan glass stroke.
- `strokeActive`: cyan active edge.
- `strokePurple`: violet glow accent.
- `textPrimary`: pale blue-white.
- `textSecondary`: desaturated blue-gray.
- `accentCyan`, `accentViolet`, `accentMint`, `accentAmber`: state and route accents.

## ScreenCore Surface Properties

Cyberglass v2 uses ScreenCore style properties that route common components through the shared glass painter:

- `surface: glass`
- `surface-depth: base|raised|floating`
- `corner-treatment: bevel`
- `background-texture`
- `texture-alpha`
- `background-texture-2`, `background-texture-3`
- `texture-alpha-2`, `texture-alpha-3`
- `texture-inset`, `texture-inset-2`, `texture-inset-3`
- `glow-strength`
- `shadow-strength`
- `inner-highlight`
- `accent-color`
- `track-color`, `fill-color`, `segmented`, `segment-size` for glass progress rails

ScreenCore components using `surface: glass` include panels, cards, list rows, buttons, status chips, inputs, empty states, and scroll panels. RenderCore cyberglass frame chrome is attempted for larger glowing panes; if RenderCore is absent or fails, ScreenCore's local painter keeps the UI alive.

Texture alpha is applied through cached dynamic texture variants instead of per-frame shader color state. This keeps cyberglass overlays tunable on the current Minecraft renderer while avoiding per-frame image work.

## Density Options

Client options for Terminal presentation:

- `useCyberglassScreenCoreTheme=true`
- `cyberglassDensity=COMFORTABLE`
- `cyberglassMotion=true`
- `cyberglassBackgroundEffects=true`
- `cyberglassGlowStrength=0.75`
- `cyberglassReduceVisualNoise=false`
- `cyberglassUseClassicLayout=false`

Density behavior:

- Compact: tighter rows and smaller chrome.
- Comfortable: default spacing.
- Cinematic: taller shell chrome, larger mission rows, deeper briefing hero, and more dramatic action dock spacing.

## Component Map

- Shell: `terminal_shell.eui.xml` plus `terminal_cyberglass_v2.eui.css`.
- Glass painter: `EchoComponentSurfaces` and `EchoRenderBridge.glassPanel`.
- Nav rail: beveled `terminal-nav-row` records.
- Module tabs: faceted `terminal-module-tab` records.
- Mission roadmap: `mission-phase-row`, `mission-record-row`, status chips, and progress bars.
- Mission briefing: hero image pane, status/type chips, next-step pane, side-op records, requirement records, and action dock.
- ScriptCore: `terminal_scriptcore_browser.eui.xml`, trusted execute slots, preview-ready controls, and result bindings.

## Transparent Material Assets

Cyberglass v2 uses transparent PNG overlays under `textures/gui/cyberglass/`:

- `hud_grid_alpha.png`: sparse tactical grid for shell depth.
- `edge_rails_alpha.png`: segmented top, bottom, tab, and footer rails.
- `corner_cuts_alpha.png`: faceted pane corner glints.
- `nav_wedge_active_alpha.png`: active left-nav wedge glow.
- `panel_sheen_alpha.png`: diagonal glass sheen for raised panes.
- `micro_ticks_alpha.png`: sparse telemetry specks for detail surfaces.
- `mission_row_active_alpha.png`: selected mission/action row edge flare.
- `status_chip_glint_alpha.png`: beveled button/chip reflection.

These assets are decorative only. UI text, icons, states, and actions remain ScreenCore-rendered and data-bound.

## ScriptCore Bridge Usage

The Terminal ScriptCore page uses ScreenCore actions as a UI intent layer:

- `action="scriptcore.execute"`
- `action-value="{definition.id}"`
- `action-param-slot="{definition.slot|actions}"`

The UI never sends raw executable JSON. Server-side ScriptCore resolves the loaded definition and slot, checks config, conditions, unknown/custom actions, and action count limits, then executes through the adapter registry.

The page also includes disabled `scriptcore.preview` controls and `scriptcore.last.*` result bindings so the V2 bridge can provide non-mutating validation and feedback without another Terminal rewrite.

## Fallback Path

Classic console and Nexus themes remain registered and use the existing renderer style. If cyberglass is disabled, `cyberglassUseClassicLayout=true`, ScreenCore is unavailable, or the ScreenCore shell throws during construction, Terminal falls back to `EchoTerminalScreen` and logs the fallback once.

## Future Polish

- Add designer-facing typed-param input components once ScriptCore V2 exposes declared UI params.
- Add a compact confirmation modal component for high-impact ScriptCore execution.
- Tune generated glass overlays after in-game screenshot review at common Minecraft GUI scales.

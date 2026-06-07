# RenderCore V21 Creator QA Guide

Creator exports aggregate profile cards, validation, performance, advanced-FX evidence, screen chrome evidence, world-surface evidence, migration reports, addon integration status, and per-profile V12 artifacts under `rendercore_creator`.

## Evidence Commands

```text
/rendercore debug advancedfx evidence start
/rendercore debug advancedfx evidence capture isolated
/rendercore debug advancedfx evidence export
/rendercore debug screenchrome evidence start
/rendercore debug screenchrome evidence capture echo_terminal
/rendercore debug screenchrome evidence export
/rendercore creator export all
/rendercore creator certify all
```

Exports are deterministic: paths are relative, no timestamps are required, and machine-local absolute screenshot paths are rejected.
`index.creator.json` keeps the aggregate report. `visual_qa/advancedfx.evidence.json` contains only advanced-FX snapshots and fallback blockers, while `visual_qa/screenchrome.evidence.json` contains only screen chrome surface evidence and blockers.
`visual_qa/worldsurface.evidence.json` contains world-surface evidence and blockers. Screen-only QA exports do not report world-surface blockers unless V12 `qa` metadata or exporter input declares required world-surface entries.

## Acceptance

Advanced-FX evidence must cover isolated bloom, fullscreen fallback, stable fallback, shader unavailable, resize/reload, entity masks, and block masks. Screen chrome evidence must cover every V21 required surface, include the expected style and reduced-motion policy, and show clean no-scanline glass. World-surface evidence should cover required entity, block entity, static block, particle-only, weather, and mob-family surfaces declared by V12 `qa` metadata.

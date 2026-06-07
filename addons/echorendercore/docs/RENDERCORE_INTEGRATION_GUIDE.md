# RenderCore V21 Integration Guide

RenderCore is the shared visual backbone for the ECHO stack. It stays standalone-light: `echocore` is the required dependency, while each consuming addon owns its optional RenderCore wiring.

## Stable Entry Points

- Common/entity/block hosts: `com.knoxhack.echorendercore.api`.
- Runtime profile records, builders, parsers, validators, migration, composition, data generation, particles, and creator exports: `com.knoxhack.echorendercore.profile`.
- Animation timelines and playback records: `com.knoxhack.echorendercore.animation`.
- Client renderers, debug HUD, Workbench, particles, block parts, and screen chrome: `com.knoxhack.echorendercore.client`.

## Consumer Pattern

Declare RenderCore as an optional dependency when the addon can run without it. Guard client registration with the owning addon mod check, then expose profile ids through the local entity/block/screen host. Do not hardcode another addon namespace from RenderCore itself.

World visual consumers should prefer profile-driven entity/block/static/particle surfaces. Screen consumers should use `RenderCoreScreenFrameOptions` presets. RuntimeGuard-style consumers should read diagnostics and fallback reasons instead of duplicating renderer state.

## V21 Contract

Runtime visual profiles use schema V12. V11 profiles auto-migrate in memory at runtime; V7-V10 source profiles remain creator-migration inputs and must be converted before activation. Creator-pack and QA metadata export as V21. Existing constructor calls, screen frame builder presets, creator exports, migration tools, and debug commands remain compatible.

V12 profiles should declare `surface`, `fallback`, `budget`, `screen_chrome`, and `qa` metadata so consumers can identify ownership, fallback behavior, performance budgets, screen chrome policy, and release evidence expectations without hard dependencies on other addons.

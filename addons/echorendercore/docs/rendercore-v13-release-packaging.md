# RenderCore V13 Release Packaging

V13 keeps runtime visual profiles at schema 11 and ships creator/export tooling schema 13.

## Release Gate

- `..\..\gradlew.bat :echorendercore:check`
- `..\..\gradlew.bat :echorendercore:runGameTestServer`
- Advanced FX QA evidence from `docs/rendercore-v13-advanced-fx-qa.md`
- `/rendercore creator certify all` returns `pass` or `warn`

## Package Contents

- V11 runtime profile schema and V13 creator-pack schema.
- Certified creator export with certification and addon integration coverage.
- Showcase examples for neon cube, hologram display, terminal HUD, orbit particles, atmosphere field, advanced bloom, and fallback-safe rendering.
- Migration guide for V7-V10 authored profiles.
- Compatibility matrix covering every `echo*` addon.
- Config notes for advanced FX isolated/fullscreen/stable fallback behavior.

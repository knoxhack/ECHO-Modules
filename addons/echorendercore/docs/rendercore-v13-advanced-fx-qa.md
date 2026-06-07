# RenderCore V13 Advanced FX QA

This checklist is release-blocking visual QA for the V13 complete-vision push.

## Environment

- Use Java 25 and run `..\..\gradlew.bat :echorendercore:check`.
- Run `..\..\gradlew.bat :echorendercore:runGameTestServer`.
- Launch a client with RenderCore and at least one addon showcase surface loaded.

## Required Checks

- Enable `/rendercore debug advancedfx true` and confirm status reports `effects advanced isolated` when the mask target and post chain are available.
- Resize the window and reload resources; isolated mode must keep rendering or report a truthful fallback reason.
- Force shader/post-chain failure by removing or overriding the post asset in a test resource pack; status must report fullscreen or stable fallback without losing base rendering.
- Verify entity mask submissions, baked block mask submissions, pulsing alpha, hue-shift color, orbit particles, aura particles, clean terminal HUD overlays, and any explicit opt-in scanline overlays.
- Run `/rendercore validate all` and `/rendercore debug advancedfx status`; capture the chat/log output for release evidence.
- Reset with `/rendercore debug advancedfx reset` and confirm config-disabled stable fallback is truthful.

## Evidence

Capture at least one screenshot/log pair for:

- isolated bloom active;
- fullscreen fallback active;
- stable fallback active;
- advanced FX unavailable/compile-failed path;
- base rendering after resource reload and resize.

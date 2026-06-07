# RenderCore V21 Migration Guide

V21 makes visual profile schema V12 the runtime contract. Existing V11 profiles auto-migrate in memory at runtime, preserving textures, layers, materials, includes, animations, particles, anchors, and effects. V7-V10 source profiles are migration-tool inputs and do not activate at runtime until converted.

## Profile Migration

Use `/rendercore creator migrate <namespace> dryrun` to inspect changes and `/rendercore creator migrate <namespace> write` to create generated V12 output. Migration does not overwrite source files.

## Screen Chrome Migration

Replace ad hoc frame drawing with `RenderCoreScreenFrameOptions` presets. Keep old constructors only for compatibility shims. Remove default scanline fields from clean-glass surfaces; use `scanlines(true)` only for an intentional legacy display.

## Export Migration

Creator-pack metadata now reports schema version 21. Runtime cards target visual profile schema 12 and include V12 surface, fallback, budget, screen chrome, and QA metadata.

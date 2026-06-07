<!-- CURSEFORGE_README_START -->
# RuntimeGuard by ECHO Labs

![RuntimeGuard by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoruntimeguard/brand-sheet.png)

**Find the lag.**

![RuntimeGuard by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoruntimeguard/features-portrait.png)

![RuntimeGuard by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoruntimeguard/features-landscape.png)

## CurseForge Summary

Find the lag.

## Main Features

- Runtime safety checks.
- Protected server operations.
- Clear diagnostics and guardrails.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoruntimeguard/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoruntimeguard/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoruntimeguard/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO RuntimeGuard

Find the lag. Protect the signal. Restore performance.

ECHO RuntimeGuard is the shared performance manager for the ECHO/Ashfall ecosystem. It detects server and client pressure, explains what RuntimeGuard can actually observe, and exposes opt-in APIs that ECHO modules can use to reduce expensive work safely.

RuntimeGuard 1.0.0 also registers ECHO Core's `IRuntimeBudgetService`, so generic addons can query `EchoOptionalServices.runtimeGuard()` without depending on RuntimeGuard-specific classes.

## What It Improves

- TPS and MSPT monitoring with lag spike reporting.
- Client FPS monitoring through a client-only runtime.
- Runtime modes: Potato, Balanced, Cinematic, Server, Debug, and Emergency.
- Smart tick recommendations for block entities, UI refreshes, particles, robotics, convoys, Lens scans, HoloMap refreshes, and Nexus effects.
- Particle budgeting with priority levels.
- Multiblock validation queueing and de-duplication.
- Network budget tracking for packets, bytes, duplicates, and non-critical batching decisions.
- Opt-in block entity sleep and entity AI throttle helpers.
- Stable status snapshots for commands, diagnostics, Terminal, and other stack health surfaces.
- Runtime reports that mark unavailable metrics honestly.

## Commands

- `/echo_perf status`
- `/echo_perf mode <potato|balanced|cinematic|server|debug|emergency>`
- `/echo_perf emergency <on|off>`
- `/echo_perf dump`
- `/echo_perf top`
- `/echo_perf particles`
- `/echo_perf multiblocks`
- `/echo_perf holomap`
- `/echo_perf lens`
- `/echo_perf network`
- `/echo_perf entities`
- `/echo_perf blockentities`
- `/echo_perf reset`

Server-impacting commands require gamemaster permissions.

## API Examples

```java
if (RuntimeGuardServices.smartTicks().shouldRun("echoholomap:markers", level, pos, TickPriority.BACKGROUND)) {
    refreshMarkers();
}
```

```java
if (RuntimeGuardServices.particles().canSpawnParticle(ParticlePriority.DECORATIVE, pos)) {
    RuntimeGuardServices.particles().recordParticleSpawn(ParticlePriority.DECORATIVE);
    spawnParticle();
}
```

```java
RuntimeGuardServices.multiblocks().requestValidation(
        id("factory"),
        level,
        controllerPos,
        ValidationPriority.BLOCK_CHANGED,
        controller::validateStructure);
```

```java
RuntimeGuardProfiler.time(id("holomap/refresh"), this::refreshMarkers);
```

```java
if (RuntimeGuardServices.integrations().shouldRunWork(
        id("powergrid/rebuild"),
        level,
        origin,
        RuntimeWorkType.MACHINE_TICK,
        TickPriority.BACKGROUND,
        12)) {
    rebuildNetwork();
}
```

Use `RuntimeGuardServices.status()` when an addon needs a read-only snapshot for UI, diagnostics, or reports. Budget and throttle APIs fail open when RuntimeGuard is disabled so optional consumers can keep normal gameplay behavior.

Generic addons should prefer the Core optional-service facade when they only need high-level budget visibility:

```java
EchoOptionalServices.runtimeGuard().ifPresent(runtime -> {
    boolean pressured = runtime.isOverBudget("server_tick");
});
```

Stable Core budget categories are:

- `server_tick`
- `client_frame`
- `particles`
- `network`
- `multiblock_validation`
- `lens_scan`
- `holomap_refresh`
- `block_entity`
- `entity_ai`
- `worldgen`
- `profiled_work`

## First-Party Integrations

- MultiblockCore can route scheduled controller revalidation through RuntimeGuard's validation scheduler when RuntimeGuard is loaded. Player-requested validation remains synchronous.
- HoloMap can defer non-manual marker refreshes to RuntimeGuard's HoloMap interval and records HoloMap sync packet estimates with the network guard.
- Lens can rate-limit server Deep Scan requests through RuntimeGuard and records scan accounting when a verified scan completes.
- ThemeCore can guide client-side glow, particle, and animation reductions through an optional reflection adapter when present.
- NetCore records successful first-party sends through a reflection-only RuntimeGuard bridge and performs advisory non-critical duplicate/background packet checks.
- PowerGrid guards and profiles repeated network rebuild, dirty update, and sync work when RuntimeGuard is loaded.

RuntimeGuard also registers an EchoCore diagnostic provider. Stack status pages can report disabled state, emergency mode, server tick pressure, validation backlog, network pressure, and repeated lag spikes without hardcoding RuntimeGuard internals.

Reports are written below the active game run directory at `run/echo-runtimeguard/reports/`. `/echo_perf dump` writes the existing text report plus a same-timestamp JSON sidecar for tools such as Command Center and release QA.

These integrations are optional-safe. Each module falls back to its existing behavior when RuntimeGuard is absent or the relevant guard is disabled.

## Known Limitations

RuntimeGuard v1.3.0 does not globally alter vanilla block entities, vanilla AI, or vanilla particles. ECHO modules opt into RuntimeGuard APIs. Metrics that cannot be measured safely are reported as `unavailable` rather than guessed.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echoruntimeguard.json`.
3. First action: review the API/tooling docs before using it in a public pack.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echoruntimeguard.md`.

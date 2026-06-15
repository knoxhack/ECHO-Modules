# ECHO: AdapterCore

Provides `adapter.neoforge`, `adapter.echo_native`, `adapter.echo_runtime_standalone`, `echo.native.registry`, `echo.native.lifecycle`, `echo.native.events`, `echo.native.commands`, `echo.native.config`, `echo.native.network`, `echo.native.resources`, `echo.native.capabilities`, `echo.native.attachments`, `echo.native.worldgen`, `echo.native.render`, `echo.native.screens`, `echo.native.save_data` for the ECHO module graph.

## Module Identity

| Field | Value |
| --- | --- |
| Module ID | `echoadaptercore` |
| Version | `1.0.0` |
| Type | `addon` |
| Kind | `library` |
| Role | `platform` |
| Side | `common` |
| Trust | `official` |

## Runtime Targets

| Runtime | Status |
| --- | --- |
| ECHO native | Supported through `.echo-addon` packaging. |
| Minecraft/NeoForge | Supported through `-neoforge.jar` packaging. |
| ECHO standalone | Supported through `-standalone.jar` packaging. |

Declared adapter runtimes: `echo_native`, `echo_runtime_standalone`, `neoforge`

## Gameplay Mutation Spine

AdapterCore is the authoritative gameplay mutation spine for beta runtime proof. `EchoNativeRuntimeHost.NativeResult` now distinguishes status from proof: `MUTATED` is accepted for release evidence only when it carries `NativeMutationReceipt` data with a release-grade proof kind.

Required receipt fields include `operationId`, `runtimeHostId`, `moduleId`, `nativeInterface`, `nativeMethod`, `status`, `proofKind`, `beforeSummary`, `afterSummary`, `saveTouched`, `hudOrEventEmitted`, and `idempotencyKey`. `HOST_STATE`, `SAVE_WRITE`, `HUD_EVENT`, and `PACKET_EVENT` can satisfy release proof. `DIAGNOSTIC_ONLY` and `QUEUED_ONLY` never do.

Ashfall is the first consumer path: first-join grants, drop-pod placement, teleport/respawn binding, save writes, HUD/packet feedback, machine use, ticks, capabilities, energy, and output events dispatch through AdapterCore and write receipt-backed ledger entries.

## Dependencies

Required modules: `echocore`, `echoplatformcore`

Optional modules: None declared.

Provides: `adapter.neoforge`, `adapter.echo_native`, `adapter.echo_runtime_standalone`, `echo.native.registry`, `echo.native.lifecycle`, `echo.native.events`, `echo.native.commands`, `echo.native.config`, `echo.native.network`, `echo.native.resources`, `echo.native.capabilities`, `echo.native.attachments`, `echo.native.worldgen`, `echo.native.render`, `echo.native.screens`, `echo.native.save_data`

Consumes: `echo.core`, `platform.contracts`

## Consumed By Editions

- Ashfall Native Edition consumes the `.echo-addon` artifact.
- Ashfall NeoForge Edition consumes the `-neoforge.jar` artifact.
- Ashfall Standalone Edition consumes the `-standalone.jar` artifact.

## Generated Release Files

| File | Requirement |
| --- | --- |
| `echoadaptercore-1.0.0-neoforge.jar` | Required for Ashfall NeoForge Edition. |
| `echoadaptercore-1.0.0.echo-addon` | Required for Ashfall Native Edition. |
| `echoadaptercore-1.0.0-standalone.jar` | Required for Ashfall Standalone Edition. |
| `echoadaptercore-1.0.0-sources.jar` | Always required for traceability and developer debugging. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required in NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

The launcher resolves this module independently through `moduleRequirements`, compares the installed file hash/version against release metadata, and downloads only the changed module artifact when an individual asset URL is available.

## Descriptor Files

- ECHO descriptor: [src/main/resources/META-INF/echo.mod.json](src/main/resources/META-INF/echo.mod.json)
- NeoForge TOML: [src/main/templates/META-INF/neoforge.mods.toml](src/main/templates/META-INF/neoforge.mods.toml)

## Build And Release

Run module builds from the `ECHO-Modules` repository root. Release generation is owned by `scripts/generate-module-release.mjs`.

```sh
node scripts/generate-module-release.mjs --module echoadaptercore
```

Use `--package-from-source` only for source-packaged visibility releases. Replace those artifacts with compiled runtime jars before marking a release player-ready.

## More Detail

- [Artifact contract](../../docs/module-artifact-contract.md)
- [Module artifact notes](docs/artifacts.md)

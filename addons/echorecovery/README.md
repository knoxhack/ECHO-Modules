# ECHO: Recovery

Provides `recovery.commands`, `recovery.compass`, `recovery.field_caches`, `recovery.graves`, `recovery.plans`, `recovery.rules`, `recovery.safe_modes` for the ECHO module graph.

## Module Identity

| Field | Value |
| --- | --- |
| Module ID | `echorecovery` |
| Version | `1.3.0` |
| Type | `addon` |
| Kind | `addon` |
| Role | `recovery` |
| Side | `common` |
| Trust | `official` |

## Runtime Targets

| Runtime | Status |
| --- | --- |
| ECHO native | Supported through `.echo-addon` packaging. |
| Minecraft/NeoForge | Supported through `-neoforge.jar` packaging. |
| ECHO standalone | Supported through `-standalone.jar` packaging. |

Declared adapter runtimes: `echo_native`, `echo_runtime_standalone`, `neoforge`

## Dependencies

Required modules: `echoadaptercore`, `echocore`, `echonetcore`

Optional modules: `echoarmory`, `echoashfallprotocol`, `echoblackboxprotocol`, `echoconvoyprotocol`, `echodatacore`, `echohealthcore`, `echoholomap`, `echoindex`, `echolens`, `echologisticsnetwork`, `echomissioncore`, `echomodulegraph`, `echonexusprotocol`, `echopackcore`, `echoplayercore`, `echoplatformcore`, `echopowergrid`, `echorelictech`, `echorendercore`, `echoruntimeguard`, `echosoundcore`, `echoterminal`, `echothemecore`, `echotutorialcore`, `echovalidationcore`, `echoweathercore`, `echoworldcore`

Provides: `recovery.commands`, `recovery.compass`, `recovery.field_caches`, `recovery.graves`, `recovery.plans`, `recovery.rules`, `recovery.safe_modes`

Consumes: `echo.core`, `echo.net`

## Consumed By Editions

- Ashfall Native Edition consumes the `.echo-addon` artifact.
- Ashfall NeoForge Edition consumes the `-neoforge.jar` artifact.
- Ashfall Standalone Edition consumes the `-standalone.jar` artifact.

## Generated Release Files

| File | Requirement |
| --- | --- |
| `echorecovery-1.3.0-neoforge.jar` | Required for Ashfall NeoForge Edition. |
| `echorecovery-1.3.0.echo-addon` | Required for Ashfall Native Edition. |
| `echorecovery-1.3.0-standalone.jar` | Required for Ashfall Standalone Edition. |
| `echorecovery-1.3.0-sources.jar` | Always required for traceability and developer debugging. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required in NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

The launcher resolves this module independently through `moduleRequirements`, compares the installed file hash/version against release metadata, and downloads only the changed module artifact when an individual asset URL is available.

## Descriptor Files

- ECHO descriptor: [src/main/resources/META-INF/echo.mod.json](src/main/resources/META-INF/echo.mod.json)
- NeoForge TOML: [src/main/resources/META-INF/neoforge.mods.toml](src/main/resources/META-INF/neoforge.mods.toml)

## Build And Release

Run module builds from the `ECHO-Modules` repository root. Release generation is owned by `scripts/generate-module-release.mjs`.

```sh
node scripts/generate-module-release.mjs --module echorecovery
```

Use `--package-from-source` only for source-packaged visibility releases. Replace those artifacts with compiled runtime jars before marking a release player-ready.

## More Detail

- [Artifact contract](../../docs/module-artifact-contract.md)
- [Module artifact notes](docs/artifacts.md)

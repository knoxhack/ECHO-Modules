# ECHO: Core

Provides `echo.core`, `echo.services`, `echo.runtime_modules`, `echo.spine`, `echo.config`, `echo.missions`, `echo.network`, `echo.index`, `echo.world`, `echo.map`, `echo.factions`, and `echo.diagnostics` for the ECHO module graph.

## Module Identity

| Field | Value |
| --- | --- |
| Module ID | `echocore` |
| Version | `1.0.0` |
| Type | `addon` |
| Kind | `library` |
| Role | `foundation` |
| Side | `common` |
| Trust | `official` |

## Runtime Targets

| Runtime | Status |
| --- | --- |
| ECHO native | Supported through `.echo-addon` packaging. |
| Minecraft/NeoForge | Supported through `-neoforge.jar` packaging. |
| ECHO standalone | Supported through `-standalone.jar` packaging. |

## Dependencies

Required modules: None declared.

Optional modules: None declared.

Provides: `echo.core`, `echo.services`, `echo.runtime_modules`, `echo.spine`, `echo.config`, `echo.missions`, `echo.network`, `echo.index`, `echo.world`, `echo.map`, `echo.factions`, `echo.diagnostics`

Consumes: None declared.

## Consumed By Editions

- Ashfall Native Edition consumes the `.echo-addon` artifact.
- Ashfall NeoForge Edition consumes the `-neoforge.jar` artifact.
- Ashfall Standalone Edition consumes the `-standalone.jar` artifact.

## Generated Release Files

| File | Requirement |
| --- | --- |
| `echocore-1.0.0-neoforge.jar` | Required for Ashfall NeoForge Edition. |
| `echocore-1.0.0.echo-addon` | Required for Ashfall Native Edition. |
| `echocore-1.0.0-standalone.jar` | Required for Ashfall Standalone Edition. |
| `echocore-1.0.0-sources.jar` | Always required for traceability and developer debugging. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required in NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

## Descriptor Files

- ECHO descriptor: [src/main/resources/META-INF/echo.mod.json](src/main/resources/META-INF/echo.mod.json)
- NeoForge TOML: [src/main/templates/META-INF/neoforge.mods.toml](src/main/templates/META-INF/neoforge.mods.toml)

## Build And Release

Run module builds from the `ECHO-Modules` repository root. Release generation is owned by `scripts/generate-module-release.mjs`.

```sh
node scripts/generate-module-release.mjs --module echocore
```

Use `--package-from-source` only for source-packaged visibility releases. Replace those artifacts with compiled runtime jars before marking a release player-ready.

## More Detail

- [Artifact contract](../../docs/module-artifact-contract.md)
- [Module artifact notes](docs/artifacts.md)

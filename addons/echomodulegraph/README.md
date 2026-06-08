# ECHO: ModuleGraph

Provides `module.graph`, `module.scanner`, `feature.graph`, `dependency.resolver` for the ECHO module graph.

## Module Identity

| Field | Value |
| --- | --- |
| Module ID | `echomodulegraph` |
| Version | `1.0.0` |
| Type | `addon` |
| Kind | `library` |
| Role | `module_graph` |
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

Required modules: `echoadaptercore`, `echocore`, `echoplatformcore`, `echoschemacore`, `echovalidationcore`, `echopackcore`, `echometadatacore`

Optional modules: None declared.

Provides: `module.graph`, `module.scanner`, `feature.graph`, `dependency.resolver`

Consumes: `echo.core`, `platform.contracts`, `schema.registry`, `validation.pack`, `pack.profile`, `metadata.manifest`

## Consumed By Editions

- Ashfall Native Edition consumes the `.echo-addon` artifact.
- Ashfall NeoForge Edition consumes the `-neoforge.jar` artifact.
- Ashfall Standalone Edition consumes the `-standalone.jar` artifact.

## Generated Release Files

| File | Requirement |
| --- | --- |
| `echomodulegraph-1.0.0-neoforge.jar` | Required for Ashfall NeoForge Edition. |
| `echomodulegraph-1.0.0.echo-addon` | Required for Ashfall Native Edition. |
| `echomodulegraph-1.0.0-standalone.jar` | Required for Ashfall Standalone Edition. |
| `echomodulegraph-1.0.0-sources.jar` | Always required for traceability and developer debugging. |
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
node scripts/generate-module-release.mjs --module echomodulegraph
```

Use `--package-from-source` only for source-packaged visibility releases. Replace those artifacts with compiled runtime jars before marking a release player-ready.

## More Detail

- [Artifact contract](../../docs/module-artifact-contract.md)
- [Module artifact notes](docs/artifacts.md)

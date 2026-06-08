# ECHO Add-on API

Provides `echo.sdk.public_api` for the ECHO module graph.

## Module Identity

| Field | Value |
| --- | --- |
| Module ID | `echoaddonapi` |
| Version | `0.1.0` |
| Type | `api` |
| Kind | `public_api` |
| Role | `sdk_spine` |
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

Required modules: None declared.

Optional modules: None declared.

Provides: `echo.sdk.public_api`

Consumes: None declared.

## Consumed By Editions

- Ashfall Native Edition consumes the `.echo-addon` artifact.
- Ashfall NeoForge Edition consumes the `-neoforge.jar` artifact.
- Ashfall Standalone Edition consumes the `-standalone.jar` artifact.

## Generated Release Files

| File | Requirement |
| --- | --- |
| `echoaddonapi-0.1.0-neoforge.jar` | Required for Ashfall NeoForge Edition. |
| `echoaddonapi-0.1.0.echo-addon` | Required for Ashfall Native Edition. |
| `echoaddonapi-0.1.0-standalone.jar` | Required for Ashfall Standalone Edition. |
| `echoaddonapi-0.1.0-sources.jar` | Always required for traceability and developer debugging. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required in NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

The launcher resolves this module independently through `moduleRequirements`, compares the installed file hash/version against release metadata, and downloads only the changed module artifact when an individual asset URL is available.

## Descriptor Files

- ECHO descriptor: [src/main/resources/META-INF/echo.mod.json](src/main/resources/META-INF/echo.mod.json)
- NeoForge TOML: Not present.

## Build And Release

Run module builds from the `ECHO-Modules` repository root. Release generation is owned by `scripts/generate-module-release.mjs`.

```sh
node scripts/generate-module-release.mjs --module echoaddonapi
```

Use `--package-from-source` only for source-packaged visibility releases. Replace those artifacts with compiled runtime jars before marking a release player-ready.

## More Detail

- [Artifact contract](../../docs/module-artifact-contract.md)
- [Module artifact notes](docs/artifacts.md)

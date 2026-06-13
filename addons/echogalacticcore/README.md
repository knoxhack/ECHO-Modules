# ECHO: GalacticCore

Provides `galacticcore.content`, `galacticcore.celestial_routes`, `galacticcore.life_support`, `galacticcore.rockets`, `galacticcore.legacy_parity_map` for the ECHO module graph.

## Module Identity

| Field | Value |
| --- | --- |
| Module ID | `echogalacticcore` |
| Version | `0.1.0-native-alpha` |
| Type | `addon` |
| Kind | `addon` |
| Role | `space_exploration` |
| Side | `common` |
| Trust | `community` |
| Public label | Unofficial ECHO Platform port/fork of Galacticraft Legacy |

## Runtime Targets

| Runtime | Status |
| --- | --- |
| ECHO native | Supported through `.echo-addon` packaging. |
| Minecraft/NeoForge | Supported as a metadata-compatible compiled jar. |
| ECHO standalone | Supported through `-standalone.jar` packaging. |

Declared adapter runtimes: `neoforge`, `echo_native`, `echo_runtime_standalone`

## Dependencies

Required modules: `echoaddonapi`, `echoadaptercore`

Optional modules: `echopackcore`, `echoindex`, `echolens`, `echoholomap`, `echoscreencore`, `echoashfallprotocol`, `echoterminal`, `echosoundcore`, `echorendercore`, `echoatmospherecore`, `echopowercore`, `echomachinecore`, `echovehiclecore`

Provides: `galacticcore.content`, `galacticcore.celestial_routes`, `galacticcore.life_support`, `galacticcore.rockets`, `galacticcore.legacy_parity_map`

Consumes: `platform.contracts`, `echo.sdk.public_api`, `adapter.echo_native`, `echo.native.registry`, `echo.native.lifecycle`, `echo.native.events`, `echo.native.commands`, `echo.native.config`, `echo.native.network`, `echo.native.resources`, `echo.native.capabilities`, `echo.native.attachments`, `echo.native.worldgen`, `echo.native.render`, `echo.native.screens`, `echo.native.save_data`

## Consumed By Editions

- Galactic Survey Native Edition consumes the `.echo-addon` artifact.
- Galactic Survey NeoForge Edition consumes the `-neoforge.jar` artifact.
- Galactic Survey Standalone Edition consumes the `-standalone.jar` artifact.

## Generated Release Files

| File | Requirement |
| --- | --- |
| `echogalacticcore-0.1.0-native-alpha-neoforge.jar` | Required for Galactic Survey NeoForge Edition. |
| `echogalacticcore-0.1.0-native-alpha.echo-addon` | Required for Galactic Survey Native Edition. |
| `echogalacticcore-0.1.0-native-alpha-standalone.jar` | Required for Galactic Survey Standalone Edition. |
| `echogalacticcore-0.1.0-native-alpha-sources.jar` | Always required for traceability and developer debugging. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required for NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

The launcher resolves this module independently through `moduleRequirements`, compares the installed file hash/version against release metadata, and downloads only the changed module artifact when an individual asset URL is available.

## Descriptor Files

- ECHO descriptor: [src/main/resources/META-INF/echo.mod.json](src/main/resources/META-INF/echo.mod.json)
- NeoForge TOML: [src/main/templates/META-INF/neoforge.mods.toml](src/main/templates/META-INF/neoforge.mods.toml)

## Build And Release

Run module builds from the `ECHO-Modules` repository root. Release generation is owned by `scripts/generate-module-release.mjs`.

```sh
node scripts/generate-module-release.mjs --module echogalacticcore
```

Use `--package-from-source` only for source-packaged visibility releases. Replace those artifacts with compiled runtime jars before marking a release player-ready.

## More Detail

- [Artifact contract](../../docs/module-artifact-contract.md)
- [Module artifact notes](docs/artifacts.md)

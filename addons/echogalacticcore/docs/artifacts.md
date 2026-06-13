# ECHO: GalacticCore Artifact Notes

This file documents the release outputs expected for `echogalacticcore` version `0.1.0-native-alpha`.

## Artifact Matrix

| File | Requirement |
| --- | --- |
| `echogalacticcore-0.1.0-native-alpha-neoforge.jar` | Required for Galactic Survey NeoForge Edition. |
| `echogalacticcore-0.1.0-native-alpha.echo-addon` | Required for Galactic Survey Native Edition. |
| `echogalacticcore-0.1.0-native-alpha-standalone.jar` | Required for Galactic Survey Standalone Edition. |
| `echogalacticcore-0.1.0-native-alpha-sources.jar` | Always required for traceability and developer debugging. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required for NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

## Edition Mapping

- Galactic Survey Native Edition consumes the `.echo-addon` artifact.
- Galactic Survey NeoForge Edition consumes the `-neoforge.jar` artifact.
- Galactic Survey Standalone Edition consumes the `-standalone.jar` artifact.

## Package Metadata

- `META-INF/echo.mod.json` is the cross-runtime descriptor and must match the released module ID/version.
- `META-INF/neoforge.mods.toml` is included for NeoForge artifacts.
- `echo-addon-package.json` is included only inside `.echo-addon` packages and points the native loader at the embedded descriptor and payload files.

## Launcher Update Behavior

Launcher modpack updates treat this module as an individual file when the edition manifest declares a matching `moduleRequirements` entry. The launcher expands the requirement into a normal managed file, verifies SHA-256 and size, downloads only changed files, and leaves unchanged module artifacts in place.

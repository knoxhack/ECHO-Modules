# ECHO: Core Artifact Notes

This file documents the release outputs expected for `echocore` version `1.0.0`.

## Artifact Matrix

| File | Requirement |
| --- | --- |
| `echocore-1.0.0-neoforge.jar` | Required for Ashfall NeoForge Edition. |
| `echocore-1.0.0.echo-addon` | Required for Ashfall Native Edition. |
| `echocore-1.0.0-standalone.jar` | Required for Ashfall Standalone Edition. |
| `echocore-1.0.0-sources.jar` | Always required for traceability and developer debugging. |
| `echocore-1.0.0-content-graph.json` | Required; Release-Index catalogable sidecar containing the canonical `.ECHO Content Graph`. |
| `.echo/content-graph/*` | Required; embedded in every runtime archive and also available via the content-graph sidecar. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required in NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

## Edition Mapping

- Ashfall Native Edition consumes the `.echo-addon` artifact.
- Ashfall NeoForge Edition consumes the `-neoforge.jar` artifact.
- Ashfall Standalone Edition consumes the `-standalone.jar` artifact.

## Package Metadata

- `META-INF/echo.mod.json` is the cross-runtime descriptor and must match the released module ID/version.
- `META-INF/neoforge.mods.toml` is included only for NeoForge artifacts.
- `echo-addon-package.json` is included only inside `.echo-addon` packages and points the native loader at the embedded descriptor and payload files.

## Launcher Update Behavior

Launcher modpack updates treat this module as an individual file when the edition manifest declares a matching `moduleRequirements` entry. The launcher expands the requirement into a normal managed file, verifies SHA-256 and size, downloads only changed files, and leaves unchanged module artifacts in place.

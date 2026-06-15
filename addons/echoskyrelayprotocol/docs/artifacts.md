# ECHO: Sky Relay Protocol Artifact Notes

This file documents the release outputs expected for `echoskyrelayprotocol`
version `0.1.0`.

## Artifact Matrix

| File | Requirement |
| --- | --- |
| `echoskyrelayprotocol-0.1.0-neoforge.jar` | Required for Sky Relay NeoForge Edition. |
| `echoskyrelayprotocol-0.1.0.echo-addon` | Required for Sky Relay Native Edition. |
| `echoskyrelayprotocol-0.1.0-standalone.jar` | Required for Sky Relay Standalone Edition. |
| `echoskyrelayprotocol-0.1.0-sources.jar` | Always required for traceability and developer debugging. |
| `echoskyrelayprotocol-0.1.0-content-graph.json` | Required; Release-Index catalogable sidecar containing the canonical `.ECHO Content Graph`. |
| `.echo/content-graph/*` | Required; embedded in every runtime archive and also available via the content-graph sidecar. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required in NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

## Edition Mapping

- Sky Relay Native Edition consumes the `.echo-addon` artifact.
- Sky Relay NeoForge Edition consumes the `-neoforge.jar` artifact.
- Sky Relay Standalone Edition consumes the `-standalone.jar` artifact.

## Package Metadata

- `META-INF/echo.mod.json` is the cross-runtime descriptor and must match the
  released module ID, version, dependency contracts, and Sky Relay namespaces.
- `META-INF/neoforge.mods.toml` is included only for NeoForge artifacts.
- `echo-addon-package.json` is included only inside `.echo-addon` packages and
  points the native loader at the embedded descriptor and payload files.

## Launcher Update Behavior

Launcher modpack updates treat Sky Relay Protocol as an individual managed
module when the edition manifest declares a matching `moduleRequirements`
entry. The launcher expands the requirement into a managed file, verifies
SHA-256 and size, downloads only changed artifacts, and leaves unchanged module
artifacts in place.

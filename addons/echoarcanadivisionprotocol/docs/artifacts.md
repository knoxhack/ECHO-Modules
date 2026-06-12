# ECHO: Arcana Division Protocol Artifact Notes

This file documents the release outputs expected for `echoarcanadivisionprotocol`
version `1.0.0` on the `beta` channel.

## Artifact Matrix

| File | Requirement |
| --- | --- |
| `echoarcanadivisionprotocol-1.0.0-neoforge.jar` | Required for ECHO NeoForge editions. |
| `echoarcanadivisionprotocol-1.0.0.echo-addon` | Required for ECHO Native editions. |
| `echoarcanadivisionprotocol-1.0.0-standalone.jar` | Required for ECHO Standalone editions. |
| `echoarcanadivisionprotocol-1.0.0-sources.jar` | Always required for traceability and developer debugging. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required in NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

## Edition Mapping

- Native editions consume the `.echo-addon` artifact.
- NeoForge editions consume the `-neoforge.jar` artifact.
- Standalone editions consume the `-standalone.jar` artifact.

## Package Metadata

- `META-INF/echo.mod.json` is the cross-runtime descriptor and must match the released module ID, version, dependency contracts, and Arcana Division namespaces.
- `META-INF/neoforge.mods.toml` is included only for NeoForge artifacts.
- `echo-addon-package.json` is included only inside `.echo-addon` packages and points the native loader at the embedded descriptor and payload files.

## Launcher Update Behavior

Launcher modpack updates treat Arcana Division Protocol as an individual managed
pack root. Edition manifests pin the 24 playable/runtime module requirements;
Release Index entries depend on `echoarcanadivisionprotocol` so the catalog can
verify the root artifact, release tag, source repository, and checksums before
approval.

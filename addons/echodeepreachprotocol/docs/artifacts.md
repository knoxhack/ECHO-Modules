# ECHO: Deep Reach Protocol Artifact Notes

This file documents the release outputs expected for `echodeepreachprotocol` version `0.1.0`.

## Artifact Matrix

| File | Requirement |
| --- | --- |
| `echodeepreachprotocol-0.1.0-neoforge.jar` | Required for NeoForge editions when this module is selected. |
| `echodeepreachprotocol-0.1.0.echo-addon` | Required for ECHO Native editions when this module is selected. |
| `echodeepreachprotocol-0.1.0-standalone.jar` | Required for Standalone editions when this module is selected. |
| `echodeepreachprotocol-0.1.0-sources.jar` | Always required for traceability and developer debugging. |
| `echodeepreachprotocol-0.1.0-content-graph.json` | Required; Release-Index catalogable sidecar containing the canonical `.ECHO Content Graph`. |
| `.echo/content-graph/*` | Required; embedded in every runtime archive and also available via the content-graph sidecar. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required in NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

## Release Boundary

Status: Not Player Ready.

This pack-root module is under active development. It consumes Foundation modules and the hazard/equipment/settlement runtimes. Compiled runtime artifacts and release evidence are required before public alpha promotion.

## Review Checklist

- Descriptor ID, version, channel, API stability, trust level, side, and standalone support match the pack contract.
- Native entrypoint reports runtime state and lists registered content.
- All required modules are present in the module graph.
- Any later gameplay implementation must update tests and keep this artifact boundary accurate.

## Shared Contract

- [Module artifact contract](../../../docs/module-artifact-contract.md)

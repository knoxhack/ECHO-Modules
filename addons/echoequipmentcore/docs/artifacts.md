# ECHO: EquipmentCore Artifact Notes

This file documents the release outputs expected for `echoequipmentcore` version `0.1.0`.

## Artifact Matrix

| File | Requirement |
| --- | --- |
| `echoequipmentcore-0.1.0-neoforge.jar` | Required for NeoForge editions when this module is selected. |
| `echoequipmentcore-0.1.0.echo-addon` | Required for ECHO Native editions when this module is selected. |
| `echoequipmentcore-0.1.0-standalone.jar` | Required for Standalone editions when this module is selected. |
| `echoequipmentcore-0.1.0-sources.jar` | Always required for traceability and developer debugging. |
| `echoequipmentcore-0.1.0-content-graph.json` | Required; Release-Index catalogable sidecar containing the canonical `.ECHO Content Graph`. |
| `.echo/content-graph/*` | Required; embedded in every runtime archive and also available via the content-graph sidecar. |
| `META-INF/echo.mod.json` | Always required and embedded in runtime artifacts where applicable. |
| `META-INF/neoforge.mods.toml` | Required in NeoForge artifacts. |
| `echo-addon-package.json` | Required in `.echo-addon` packages. |

## Release Boundary

Status: Runtime Ready.

This module contains a compiled runtime implementation. Equipment slots, suit tiers, upgrade modifiers, durability rules, and hazard resistance integration are active.

## Review Checklist

- Descriptor ID, version, channel, API stability, trust level, side, and standalone support match the roadmap contract.
- Native entrypoint reports runtime state and lists registered slots/items.
- `echo.platform_roadmap.module_contract.v1` data remains the authoritative small contract JSON for this roadmap module.
- Any later gameplay implementation must update tests and keep this artifact boundary accurate.

## Shared Contract

- [Module artifact contract](../../../docs/module-artifact-contract.md)

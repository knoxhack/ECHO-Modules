# Release Gate

ECHO: GalacticCore is releasable only when all of the following are true.

- `LICENSE` and `CREDITS.md` preserve Galacticraft Legacy MIT attribution.
- Public copy says "Unofficial ECHO Platform port/fork of Galacticraft Legacy."
- Public release-facing copy does not use the forbidden official-project phrasing listed in `docs/LEGAL_NOTES.md`.
- `echo.mod.json` declares `official: false`, uses the native entrypoint, and does not require loader internals.
- `packs/galacticcore/echo.pack.json` uses `echogalacticcore` as `rootModule` and carries the MIT attribution label.
- Production code has no dependency on `echo-native-loader`.
- Production code has no NeoForge or Forge dependency.
- Production code has no `activateNative(Map)` path.
- Old `micdoodle8` source is excluded from the ASDK production build.
- Migrated recipe JSON has no Forge recipe or ore-dict runtime markers.
- Legacy ore-dict recipe ingredients use ECHO-owned replacement tags.
- Migrated blockstate JSON has no Forge markers or Forge helper model references.
- Legacy Forge fluid and multi-layer blockstate helpers use ECHO-owned model replacements.
- Migrated language assets are JSON-only and no legacy `.lang` files remain in runtime assets.
- Migrated loot tables use namespaced entry/function ids and ECHO legacy-data migration functions.
- Runtime sound event keys use ECHO-owned naming rather than legacy Galacticraft-branded ids.
- Migrated models, blockstates, and material files use native singular `block/` and `item/` texture ids.
- Runtime textures live under `textures/block` and `textures/item`; legacy plural texture roots are absent.
- Every runtime mutation uses a typed ASDK service.
- Mutated state is backed by `EchoNativeMutationReceipt` evidence.
- Foundation, machine, life-support, energy bridge, player gear, rocket progression, celestial route, celestial environment, outer planet progression, dungeon reward, dungeon/boss, and ECHO integration manifests are present, schema-tagged, source-attributed, and resource-service registered.
- `echogalacticcore:runtime` is registered and typed receipts install executable gameplay runtime models, including transfer placement/execution, HoloMap route surface/rendered menu/interactions, ScreenCore checklist surface/rendered menu/interactions, boss entity spawn, boss encounter, boss AI intent, treasure interaction, and treasure chest screen/rendered menu contracts.
- `echogalacticcore:runtime_gateway` is registered and typed receipts publish event, network, screen, and worldgen actions backed by runtime decisions, including transfer placement `placeStructure` actions, transfer execution event actions, HoloMap route screens/rendered menus/interactions, ScreenCore checklist screens/rendered menus/interactions, treasure chest screens/rendered menus, boss spawn events, boss tick events, boss AI step events, and treasure interaction payloads.
- `echogalacticcore:runtime_adapters` is registered and typed receipts install block entity, dimension transfer, transfer placement/execution, HoloMap route surface/rendered menu/interaction, ScreenCore checklist surface/rendered menu/interaction, treasure chest screen/rendered menu, dungeon structure, boss entity spawn, boss encounter, boss AI, treasure interaction, dungeon encounter, and treasure reward adapter contracts.
- `echogalacticcore:host_callbacks` is registered and typed receipts install host callback contracts for block ticks, life support, route selection, HoloMap route surface opening/rendered menu/route interaction, ScreenCore checklist opening/rendered menu/checklist interaction, dimension transfer, transfer placement preparation, transfer execution, environment scans, dungeon structure preparation, boss entity spawn, boss encounter ticks, boss AI steps, treasure interactions, treasure chest screens/rendered menus, and dungeon treasure claims.
- `echogalacticcore:host_execution_bridge` is registered and typed receipts install host execution bridge contracts for dimension transfer commits, boss spawn commits, and rendered menu binding.
- `echogalacticcore:host_binding_contracts` is registered and typed receipts install concrete ASDK owner bindings for world transfer placement, boss entity spawn integration, and HoloMap/ScreenCore/treasure menu hosts.
- `echogalacticcore:live_host_adapters` is registered and typed receipts install live adapter plans for destination load/teleport/progression sync, boss entity construction/state attach/room lock, and screen renderer/widget/action mounting.
- `echogalacticcore:live_host_entrypoints` is registered and typed receipts install callable ASDK-safe host entrypoints for world transfer, boss spawn, and HoloMap/ScreenCore/treasure menu opening.
- `echogalacticcore:platform_executors` is registered and typed receipts install ASDK-safe platform executor facade contracts for world transfer, boss spawn, and HoloMap/ScreenCore/treasure menu opening while deferring direct Minecraft object mutation to the host-owned boundary.
- `echogalacticcore:live_session_mutations` is registered and typed receipts install host-owned mutation sink contracts and payload evidence for non-dry-run world transfer, boss spawn, and HoloMap/ScreenCore/treasure menu opening.
- Runtime model tests cover machine ticks, oxygen checks, energy transfer, player gear, rocket launch readiness, route unlocks, environment hazards, transfer placement/execution gates, HoloMap route surfaces/rendered menus/interactions, ScreenCore checklist readiness/rendered menus/interactions, deterministic dungeon structure plans, boss entity spawn source/attribute/action contracts, boss encounter key drops, boss AI movement/attack/room-lock intent, treasure interaction gating, treasure chest screen/menu payloads, and dungeon reward progression.
- Runtime gateway tests cover machine tick events, life-support events, route network actions, HoloMap route interaction packets, ScreenCore checklist interaction packets, transfer placement worldgen actions, transfer execution events, HoloMap route screens/rendered menus, launch checklist screens, ScreenCore checklist screens/rendered menus, treasure chest screens/rendered menus, environment scans, dungeon structure worldgen feature actions, boss spawn events, boss encounter events, boss AI step events, dungeon reward broadcasts, and treasure interaction broadcasts.
- Runtime adapter tests cover block entity tick adaptation, dimension transfer planning, locked destination blocking, transfer placement/execution plans, HoloMap route surface/rendered menu/interaction plans, ScreenCore checklist surface/rendered menu/interaction plans, treasure chest screen/rendered menu plans, dungeon structure worldgen/save-data planning, boss entity spawn events/save targets, boss encounter events/save targets, boss AI events/save targets, treasure interaction broadcasts/save targets, and dungeon reward broadcasts.
- Host callback tests cover host-sourced evidence, accepted/blocked callback results, route locking, transfer placement/execution, HoloMap route surfaces/rendered menus/interactions, ScreenCore checklist surfaces/rendered menus/interactions, treasure chest surfaces/rendered menus, transfer planning, dungeon structure preparation, boss entity spawn, boss encounter ticks, boss AI steps, treasure interactions, and treasure claims.
- Host execution bridge tests cover transfer commit order, boss spawn commit order, rendered menu host binding, release smoke receipts, and ASDK module receipt visibility.
- Concrete host binding tests cover ASDK owner-service mapping, worldgen placement binding, capability-backed entity spawn binding, screen menu binding, typed receipt evidence, and ASDK module receipt visibility.
- Live host adapter tests cover world transfer executor steps, boss entity executor steps, screen/menu executor steps, binding evidence preservation, typed receipt evidence, and ASDK module receipt visibility.
- Live host entrypoint tests cover callable world transfer, boss spawn, screen/menu entrypoints, adapter and binding evidence preservation, host lane/subject evidence, typed receipt evidence, and ASDK module receipt visibility.
- Platform executor tests cover world transfer, boss spawn, screen/menu executor facade receipts, entrypoint/adapter/binding evidence preservation, dry-run mutation deferral evidence, and ASDK module receipt visibility.
- Live-session mutation tests cover non-dry-run host mutation request dispatch, host sink receipt evidence, host payload API/state/safety contracts, contract-only registration honesty, world/entity/screen live-session receipt targets, and ASDK module receipt visibility.
- Side-specific client/server registrations use client/server side receipts.
- Duplicate ids fail in the ASDK testkit.
- Descriptor validates and exposes the native entrypoint.
- Tests pass.

Current foundation gate:

```powershell
.\gradlew.bat :echogalacticcore:check
```

Identity/legal-only gate:

```powershell
.\gradlew.bat :echogalacticcore:verifyIdentityLegal
```

Gameplay manifest contract gate:

```powershell
.\gradlew.bat :echogalacticcore:verifyGameplayManifestCoverage
```

Native package gate once the workspace includes the module in a product profile:

```powershell
.\gradlew.bat packageNativeProductLayout
```

GalacticCore-specific package gate:

```powershell
.\gradlew.bat packageNativeProductLayout -PechoAddonSet=all '-PechoNativePackProfileRoot=packs/galacticcore/echo.pack.json'
```

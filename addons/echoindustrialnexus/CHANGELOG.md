# Changelog

## 1.3.0 - Full Stack Integration Completion

- Added the public Industrial backbone API surface for read-only factory snapshots, machine telemetry, process source cards, and sibling support hooks.
- Registered Industrial Index source facts for machine outputs, byproducts, MultiblockCore factory task outputs, POI schematics, and Furnace Warden sources.
- Added a full-stack backbone readiness report and GameTest coverage for the new public API/source-fact contract.
- Added stage-aware optional route placement for Industrial Terminal missions and preserved route metadata through MissionCore imports.
- Added named fluid aliases to `echoindustrialnexus:industrial_processing` recipes while keeping legacy numeric fluid ids valid.
- Updated Terminal Recipe Index and Echo Index Industrial recipe surfaces to show named fluids, catalysts, byproducts, Thermal Flux, heat, and duration consistently.
- Added Industrial MultiblockCore upgrade definitions and runtime behavior for speed, efficiency, cooling, overclock, emergency shutdown, and Factory Link effects.
- Let Industrial multiblock controllers install compatible Industrial upgrade items directly.
- Expanded Lens Deep Scan coverage to Industrial machines, item ducts, fluid pipes, and Flux ducts in addition to controllers, robotic arms, and depot crates.
- Made Terminal POI hints use generated Industrial POI coordinates when world progress has recorded them.
- Added schema/example coverage for Industrial processing recipes and updated MultiblockCore upgrade schema modifiers.
- Added GameTests for Industrial stage route placement and named-fluid Terminal recipe parsing.

## 1.2.0 - Playable Nexus Furnace Array

- Added the dedicated `nexus_furnace_array_controller` block and `nexus_furnace_array_blueprint` so players can form the array without commands.
- Promoted `echoindustrialnexus:nexus_furnace_array` into a playable late-game MultiblockCore route with explicit Matrix Processing task permissions.
- Added data-driven Stabilize Hybrid Thermal Core and Forge Core Key Assembly automation tasks.
- Added the `nexus_array_pressure` soft automation effect so Nexus thermal pressure records locally and calls Nexus Protocol only when that optional chapter is loaded.
- Added the Terminal/Core `nexus_furnace_array` mission between Recipe Matrix Encoding and Production Survived, completed by Core Key Assembly forging.
- Added optional Logistics loadouts for both Nexus Furnace Array tasks while preserving safe behavior when Logistics is absent.
- Added GameTest coverage for definition loading, controller formation, blocked tool-head behavior, task outputs, Terminal mission progress, soft Nexus pressure, and Factory Command/HoloMap/Lens provider snapshots.
- Updated smoke and release notes for the 1.2.0 stack-minor release path.

## 1.1.3 - Priority Fix RC

- Fixed NeoForge fluid capability transaction semantics for Industrial machines and fluid pipes with snapshot rollback behavior.
- Added data-driven tank fluid recipes for Water Purifier and Fluid Refiner and decoded fluid fields in `IndustrialProcessingRecipe` JSON.
- Upgraded Smart Ducts with a one-slot filter, whitelist/blacklist wrench toggle, Nexus-safe override behavior, persistence, and loop/backtrack guarding.
- Cached Factory Controller scan state and stopped passive Terminal mission snapshots from rescanning the factory every mission read.
- Improved machine GUI readouts with named fluids, warning causes, side-config text, and early-chain route hints.
- Added ECHO Terminal Recipe Index coverage for `echoindustrialnexus:industrial_processing` JSON, including item/tag ingredients, catalysts, byproducts, fluids, Thermal Flux, heat, and duration notes.
- Added POI terrain/biome/loaded-footprint safety checks and Warden phase telegraphs, Furnace Drone minions, and marked cooling objectives.
- Added focused regression coverage for fluid transaction rollback, mixed-fluid rejection, pipe filtering, and Smart Duct filtering.

## 1.1.0 - Production Completion

- Promoted Industrial Nexus into the default included addon set.
- Added registered Industrial fluids and NeoForge fluid capabilities for machine tanks.
- Added active Rusted, Reinforced, Pressurized, Shielded, and Static Pipe block entities with fluid transfer, filtering, tier capacities, transfer rates, loss, and hazardous leak behavior.
- Kept bucket/cell workflows as fallback automation for fluid recipes.
- Hardened ECHO Terminal compatibility so common and client bridges load reflectively only when `echoterminal` is present.
- Tuned Thermal Flux, capacitor, duct, scrubber, and POI spacing defaults for production gameplay.
- Added optional soft compat hooks for Ashfall air/radiation cleanup, Nexus thermal pressure, and Stationfall oxygen support while preserving safe fallback behavior.
- Updated Terminal mission metadata from Functional V1 to Production Complete.
- Documented release jar path, dependencies, validation commands, and manual smoke checklist.

## Pre-1.1 - Gameplay Polish Baseline

- Added custom Industrial machine menus/screens.
- Added progression storage, mission snapshots, reward idempotency, procedural POIs, Furnace Warden progression, and focused GameTests.
- Added generated vanilla-style block/item textures and JSON resources for the Functional V1 content set.

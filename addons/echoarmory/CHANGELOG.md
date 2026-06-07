# Changelog

## 1.3.0 - Full Vision Integration

- Added Armory projectile entities for energy bolts, veil arrows, and sigil chakrams; accepted ranged shots now spawn projectiles instead of applying hitscan damage.
- Expanded the shared readiness report with score, route family, merged route-profile protections, energy/ammo status, active synergies, staged rack/stand support, and a next action.
- Added schemaVersion 2 Armory content roots for `station_recipes`, `firing_modes`, and `route_profiles`; bundled gear, modules, loadouts, boss recommendations, faction unlocks, and synergies now declare v2.
- Removed bundled `minProtection` usage from loadout data while keeping legacy no-schema parsing as a soft fallback.
- Added public read-only report APIs for readiness reports, route profiles, gear state summaries, and station operation previews.
- Upgraded station behavior and UI telemetry with station-specific preview readiness, blocker text, fuel counts, and action hints before `APPLY`.
- Added guarded optional integration summaries for Terminal, Logistics, MissionCore, Index, Lens, HoloMap, WorldCore, RuntimeGuard, SoundCore, ThemeCore, RenderCore, TutorialCore, Ashfall, Orbital, Stationfall, Nexus, Industrial, and Blackbox.
- Expanded GameTests for v2 content contracts, extended readiness reports, station preview consistency, staged rack readiness, and projectile spawn/resource safety.

## 1.2.0 - Route Kit Readiness

- Added shared route-kit readiness reports for Armory loadouts with `READY`, `STAGED`, `MISSING`, and `LOCKED` states.
- Extended Armory loadout JSON with optional `requiredProtections` for toxic, radiation, cold, heat, and fracture thresholds while preserving legacy `minProtection` as a fracture fallback.
- Updated bundled Toxic Breach, Fracture Guardian, and Orbital Assault kits to use per-hazard readiness requirements.
- Reworked Core route records, diagnostics, hazard telemetry, Terminal actions, Loadout Terminal binding, and Logistics dispatch paths to use the shared readiness model.
- Added MissionCore side ops for preparing and dispatching route kits.
- Expanded GameTests for required-protection parsing, route-kit readiness states, and MissionCore route-kit registration.

## 0.1.0 - First Playable Release

- Completed the v1 survival loop: craft Armory Alloy Plate, craft Armory Bench, craft first gear, craft Module Upgrade Table, install modules, recharge energy gear with real fuel, and inspect readiness through Terminal/Core diagnostics.
- Fixed `veil_shield` resources for Minecraft 26.1 by replacing removed `minecraft:scute` references with `minecraft:turtle_scute`.
- Added Armory-specific resource validation for unresolved recipe item references and vanilla item-model texture references.
- Hardened module installation so duplicate, incompatible, full-slot, and locked installs reject before consuming module items.
- Removed the unsafe post-craft deletion guard for faction gear; locked gear now stays in the stack but cannot provide survival use/protection until unlocked.
- Made station and Terminal recharge consume `veil_crystal` or `resonance_shard`; failed recharge now leaves gear and resources unchanged.
- Split station behavior for v1: Armory Bench repairs/tunes, Weapon Forge upgrades weapons, Armor Forge upgrades armor, Energy Core Charging Station recharges, Sigil Engraver trims, Module Upgrade Table/Veil Infuser/Construct Dock install modules, and racks are storage/display-lite scans.
- Updated the Terminal Armory tab with selected loadout, augment, and boss rows so Equip, Install, Preview, and Logistics send explicit server-authoritative payloads.
- Moved synergy checks to data-driven `SynergyDefinition` effects while retaining legacy fallbacks for existing gear combinations.
- Expanded Armory GameTests to cover duplicate/incompatible/full-slot install safety, no-target ranged resource safety, ammo-before-energy ranged shots, fuel-gated recharge, faction locks that do not delete gear, Terminal actions, and bundled gameplay-data validation.
- Added smoke-test and acquisition-path docs for first survival, module, energy, faction depot, Terminal, Logistics, and reload checks.

## Non-Armory Follow-Up

- Full gameplay-data validation is blocked by existing Orbital/Ashfall source-token checks outside Armory.
- Root release manifest/artifact tasks require `--no-configuration-cache` until their execution-time project references are made configuration-cache compatible.

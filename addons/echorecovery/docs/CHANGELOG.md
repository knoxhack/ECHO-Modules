# ECHO Recovery Changelog

## 1.3.0 - Full Beta Integration

### Added

- Recovery now registers as a beta stack module at version `1.3.0`.
- Core recovery service dispatch is registered through `EchoCoreServices`.
- Local extension APIs were added for placement providers, rule providers, event hooks, and grave snapshots.
- Datapack reloaders now load grave types, item rules, and presets from `data/<namespace>/echorecovery/...`.
- Recovery-only GameTests cover metadata/registry, access/recovery behavior, datapack parsing, and Core service dispatch.
- Optional integrations now register concrete support where public APIs exist: Terminal, MissionCore, TutorialCore, SoundCore, ThemeCore, RenderCore, HoloMap, Index, Lens, RuntimeGuard, DataCore, WorldCore, WeatherCore, Ashfall, Nexus, Blackbox, RelicTech, PlayerCore, Armory, Logistics, Convoy, and PowerGrid.

### Changed

- Death capture now preserves grave ids, source dimension, XP, item rule outcomes, access/share state, hazard notes, and placement context.
- Recovery compass tooltips report target, distance, dimension mismatch, and guidance status.
- Grave UI and Recover All server packet now validate access and overflow more carefully.
- `/graves` command paths now use real grave ids, support sharing/admin restore/admin delete flows, and keep remote recovery behind config.
- HoloMap waypoints are created and removed with the grave lifecycle.
- Recovery recipes and advancements were updated to the current stack resource format.

## 1.2.0 - Polish & UX

### Added

- 54-slot grave UI with player inventory integration.
- Grave Key and Recovery Compass recipes.
- Advancement tree for Recovery onboarding.
- Loot tables and sound event ids for grave interactions.
- Serverbound `RecoverAllPacket`.

### Changed

- Grave blocks open the grave menu instead of immediately recovering everything.

## 1.0.0 - Initial Release

### Added

- Grave block creation on player death.
- Safe placement logic with configurable radius.
- Grave ownership, protection, and timed public access.
- `/graves` command tree.
- Grave Key and Recovery Compass items.
- NeoForge config.
- Prototype datapack and optional integration hooks.

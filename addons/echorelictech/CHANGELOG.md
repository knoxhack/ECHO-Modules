# ECHO: RelicTech Changelog

## 0.2.0-beta (2026-05-13)

### New Features
- **Terminal Custom Tab**: Full `TerminalTab` implementation with custom render dashboard showing core machines, MVP relics, and a "SCAN RELICS" action button. Registered under ADDONS group with RT icon and RelicTech chapter navigation.
- **Echo Mirror Entity Decoys**: Spawns 3 armor-stand decoys equipped with the player's gear, made invisible (only equipment visible), positioned in a circle around the player. Decoys divert hostile mob attention for 10 seconds before auto-despawning. Integrated with `EchoMirrorDecoyTracker` tick handler.
- **Null Charge Consumption**: Echo Mirror now correctly consumes 1 Null Charge per activation (matching its relic definition). Insufficient charge fails gracefully with a lang-key message.

## 0.1.0-beta (2026-05-13)

### Beta-Ready Core Loop
- **Relic Analyzer**: Accepts Unidentified Relics, reads hidden target relic data, outputs the correct relic item with identification state. Supports data-driven random selection from loaded definitions as fallback.
- **Prototype Workbench**: Uses each relic JSON repair definition for material validation. Supports stabilize, contain, overclock, and purge with clear failure messages and fallback hardcoded checks.
- **Containment Locker**: Real 5-slot inventory that persists contained relics, marks containment flag on relic data, suppresses passive warnings, and exposes contents.
- **Null Battery Dock**: Stores batteries and cells, supports Null Cell charging fallback, and requests PowerGrid power via reflection-safe bridge when available.

### Data-Driven Relic Lifecycle
- Unidentified relic loot carries optional `unidentified_relic_data` component with hidden target relic ID.
- Relic definitions drive cooldown, null charge cost, instability cost, terminal copy, lens rows, repair materials, and failure table lookup.
- Failure tables are consulted after successful relic use with per-condition chance multipliers and instability level bonuses.

### All Five MVP Relics
- **Phase Anchor**: Bind/recall with safe-position validation, dimension policy, cooldown from definition/config, null charge cost, post-use failure trigger.
- **Null Battery**: Charge display, storage, dock compatibility, consumption by other relics.
- **Guardian Lens**: Scans for RelicTech machines within configurable radius, feeds discovery data.
- **Echo Mirror**: Defensive projection with invisibility and resistance effects, cooldown, instability, SoundCore cue integration path.
- **Matter Stitcher**: Heal/armor repair, null charge cost from definition, cooldown, high-instability weakness drawback.

### Vault Worldgen
- Added `pre_gridfall_research_vault/start.nbt` — a 9x5x9 stone-brick vault room with analyzer, workbench, and loot chests.
- Validated jigsaw pool, structure set, biome tag, and natural spawn frequency.

### Echo Stack Integrations
- **Terminal**: Addon info provider with metrics/sections, recipe provider for machines. *Upgraded to full custom tab in 0.2.0-beta.*
- **Lens**: Block/Machine lens providers for all four RelicTech machines with status rows.
- **HoloMap**: Map data provider registering a "Relic Vaults" layer.
- **MissionCore**: Reflection-safe mission chapter "Relic Operations" with objectives for recover, analyze, and stabilize; hooks analyze and vault-discover events.
- **PowerGrid**: Dock requests power via reflection; backfire support via breaker trip reflection.
- **WorldCore**: Vault discovery emits structure scan telemetry.
- **SoundCore**: Integration helper for scan, guardian located, machine complete, and relic malfunction stingers.
- **NexusProtocol**: Records machine usage and research unlocks via reflection.
- **DataCore/EchoCore profile**: `RelicPlayerSavedData` persists discovered relics, analyzed count, first vault discovery, total uses.
- **RuntimeGuard**: Smart ticking via reflection in Null Battery Dock charging loop.

### Beta Polish
- Replaced nearly all hardcoded player-facing strings with lang keys.
- Updated README to remove completed limitations and add beta tester instructions.
- Added changelog and known-issues outline.
- Preserved existing uncommitted work; additive changes only.

## Known Issues
- Faction reputation reactions are data-ready but not gated in this beta.
- Additional vault variants and relics can be added post-beta via datapack.

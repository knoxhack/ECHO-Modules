<!-- CURSEFORGE_README_START -->
# RelicTech by ECHO Labs

![RelicTech by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echorelictech/brand-sheet.png)

**Display name: RelicTech**

![RelicTech by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echorelictech/features-portrait.png)

![RelicTech by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echorelictech/features-landscape.png)

## CurseForge Summary

Display name: RelicTech

## Main Features

- Ancient relic devices.
- Forbidden artifact records.
- Relic recovery lab.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echorelictech/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echorelictech/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echorelictech/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: RelicTech

**Display name:** ECHO: RelicTech  
**Mod ID:** `echorelictech`

ECHO: RelicTech is Ashfall's legendary loot chase — but not fantasy magic loot.

Players recover damaged old-world prototypes from vaults, labs, blacksites, guardian sites, Nexus scars, orbital wreckage, cryogenic ruins, Stationfall sections, and Blackbox memory chambers. They must identify, repair, stabilize, overclock, contain, or risk using them.

## Design Rule

**Every relic must be powerful enough to be exciting, but dangerous enough to respect.**

These are broken pre-Gridfall devices, failed ECHO experiments, forbidden Nexus prototypes, lost military hardware, damaged AI cores, and experimental survival tools.

## Gameplay Loop

1. **Recover** an `Unidentified Relic` from a vault or ruin.
2. **Analyze** it in a `Relic Analyzer` to discover what it is.
3. **Repair / Stabilize** it at a `Prototype Workbench` using materials.
4. **Use** it when you need it most — and accept the risk.
5. **Contain** dangerous relics in a `Containment Locker` if they become too unstable.

## Relic Conditions

- `UNKNOWN` — Cannot be used.
- `DAMAGED` — Can activate, but higher failure chance.
- `STABILIZED` — Normal operation.
- `OVERCLOCKED` — Stronger effects, but riskier.
- `CONTAINED` — Safer, but weaker / longer cooldown.
- `CORRUPTED` — Powerful and unreliable.

## Relic Tiers

- `FIELD` — Common survivor tech.
- `PROTOTYPE` — Pre-Gridfall experimental devices.
- `FORBIDDEN` — Decommissioned or outlawed tech.
- `NEXUS` — Nexus-touched or post-Gridfall anomaly tech.

## Relic Categories

MOBILITY, SURVIVAL, COMBAT, UTILITY, AI, WORLD, POWER, SCANNER, ARMOR, WEAPON

## MVP Relics

| Relic | Category | Tier | Behavior |
|-------|----------|------|----------|
| Phase Anchor | Mobility | Prototype | Bind and recall to safe anchor point |
| Null Battery | Power | Prototype | Stores Null Charge for relics |
| Guardian Lens | Scanner | Prototype | Detects nearby relic machines/traces |
| Echo Mirror | Combat | Forbidden | Creates defensive echo projection |
| Matter Stitcher | Survival | Forbidden | Repairs armor or heals player |

## Instability System

Using dangerous relics increases **Player Relic Instability**. At higher levels, relics become more likely to malfunction, attract hostile signals, or cause reality bleeds.

Levels:
- 0 STABLE
- 1 ECHO STATIC
- 2 SYSTEM DRIFT
- 3 NEXUS ATTENTION
- 4 REALITY BLEED
- 5 CRITICAL INSTABILITY

Instability decays over time if configured.

## Failure System

When a relic malfunctions, it consults a data-driven **Failure Table**. Failures range from minor fizzles to major backfires, each with appropriate effects.

## Machines

- **Relic Analyzer** — Identifies unknown relics. Data-driven output from hidden relic data or random selection.
- **Prototype Workbench** — Repairs, stabilizes, overclocks, contains, or purges relics using JSON repair definitions.
- **Containment Locker** — Stores dangerous relics safely with persistent 5-slot inventory.
- **Null Battery Dock** — Charges Null Batteries via Null Cells or PowerGrid when available.

## Containment

High-tier or corrupted relics carried without containment may trigger passive warnings. Stored relics in a Containment Locker do not contribute to passive warnings.

## Data-Driven Relics

Relic definitions and failure tables are loaded from JSON and support addon namespaces:
- `data/*/relics/*.json`
- `data/*/relic_failures/*.json`

## Integrations

Real integrations are provided for:
- ECHO Terminal (addon info, recipes)
- ECHO Lens (machine status providers)
- ECHO HoloMap (vault marker layer)
- ECHO MissionCore (Relic Operations missions)
- ECHO PowerGrid (dock charging, backfire support)
- ECHO WorldCore (vault discovery telemetry)
- ECHO SoundCore (stinger hooks)
- ECHO NexusProtocol (research milestones)
- ECHO DataCore (player relic profile persistence)

All optional integrations degrade cleanly if a module is absent.

## Commands

- `/echorelictech give_relic <player> <relicId>`
- `/echorelictech instability get <player>`
- `/echorelictech instability set <player> <amount>`
- `/echorelictech instability add <player> <amount>`
- `/echorelictech identify_held`
- `/echorelictech corrupt_held`
- `/echorelictech stabilize_held`
- `/echorelictech contain_held`
- `/echorelictech overclock_held`
- `/echorelictech trigger_failure <severity>`
- `/echorelictech bind_phase_anchor`
- `/echorelictech locate_vault`
- `/echorelictech debug`
- `/echorelictech reload`

## Config

See `RelicTechConfig.java` for server-side balance options including:
- `enableRelicInstability`
- `phaseAnchorCooldownTicks`
- `nullBatteryMaxCharge`
- Failure chance multipliers per condition
- Instability thresholds and decay settings

## API

`RelicTechApi` provides public methods for:
- Querying relic state
- Modifying relic condition
- Managing player instability
- Consuming Null Charge
- Triggering failures
- Recording player relic summary, vault discoveries, and machine status snapshots

## Beta Tester Instructions

1. Start a fresh world or dedicated server.
2. Locate a `pre_gridfall_research_vault` underground (y ~ -24) or use `/locate structure echorelictech:pre_gridfall_research_vault`.
3. Loot the vault for an `Unidentified Relic`, materials, and diagnostic reports.
4. Craft or place a `Relic Analyzer` and insert the Unidentified Relic.
5. Take the identified relic to a `Prototype Workbench` with the required materials to stabilize.
6. Craft a `Null Battery` and charge it at a `Null Battery Dock` using `Null Cells`.
7. Use each MVP relic and observe cooldowns, instability, and occasional failures.
8. If a relic becomes too unstable, store it in a `Containment Locker`.
9. With the full Echo stack, verify Terminal info, Lens scans, HoloMap layers, and MissionCore objectives.

## Known Limitations / Future Passes

- Terminal custom tab rendering is out of scope for beta; addon info and recipes are registered.
- Custom entity decoys (Echo Mirror) use effect-based MVP; full entity decoy is post-beta.
- Faction reputation reactions are data-ready but not fully gated in this beta.
- Additional relics, vault variants, and lore entries can be added post-beta via datapack.

## License

All Rights Reserved

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echorelictech.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echorelictech.md`.

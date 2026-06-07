# Reclamation by ECHO Labs

Agriculture Reclamation is the standalone ECHO ecology recovery addon. It runs with ECHO Core and normal Minecraft crafting, and deepens into Ashfall, WeatherCore, PowerGrid, Logistics, MissionCore, Terminal, Index, HoloMap, TutorialCore, ThemeCore, and SoundCore when those addons are installed.

## Core Loop

1. Craft or loot a Recovered Seed Capsule.
2. Analyze capsules in the Seed Vault Terminal to discover crop profiles.
3. Clean Dead, Contaminated, Irradiated, or Toxic soil with the Soil Purifier.
4. Grow recovered crops in cleansed soil or in a Hydroponic Tray.
5. Stabilize contaminated seed profiles in the Gene Stabilizer.
6. Build a Greenhouse Controller zone and scan it for safety.
7. Deploy Pollinator Drone Docks to support crops and trays.
8. Raise chunk restoration through crop maturity, scanner pulses, and purifier work.

## Standalone Access

- `/reclamation status` and `/agriculturereclamation status` show player-facing field metrics.
- `/reclamation scan` reports local soil, greenhouse, crop, drone, and restoration state.
- `/reclamation debug` is GM-only diagnostics.
- `/reclamation give_seed_capsule` is GM-only testing access.
- Tooltips and recipes remain available without Terminal, ThemeCore, or Ashfall.

## Main Blocks

- Seed Vault Terminal: seed capsule recovery and profile discovery.
- Soil Purifier: local soil conversion using enzyme or nutrient passes.
- Hydroponic Tray: seed, nutrient, growth, greenhouse, and harvest telemetry UI.
- Greenhouse Controller: zone scan and greenhouse quality control.
- Gene Stabilizer: contaminated seed stabilization.
- Pollinator Drone Dock: greenhouse pollination support.
- Bio-Reactor and Compost Recycler: recovered crop conversion into bio gel and nutrient mix.
- Ecology Scanner: local restoration and soil diagnostics.

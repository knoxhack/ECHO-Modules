# Agriculture Reclamation Integrations

All integrations are optional. Standalone play requires ECHO Core plus this addon.

## ECHO Core

Registers the Reclamation chapter, route diagnostics, field query APIs, map providers, Index providers, and mission hook coverage.

## Ashfall

Ashfall soil, salvage, faction preference, and toxic recovery behavior are guarded behind loaded-mod checks and registry ids. No Ashfall classes are used on the standalone path.

## Index

The ECHO Core Index provider exposes crop cultivation, soil purification, machine processes, greenhouse zone guidance, and hydroponic requirements.

## MissionCore

When `echomissioncore` is installed, Reclamation registers Field Reclamation / Ecology Recovery missions directly through `EchoCoreServices.registerMissionContent`.

## Terminal

When `echoterminal` is installed, Reclamation contributes dashboard and mission views. MissionCore content no longer depends on Terminal.

## Logistics

When `echologisticsnetwork` is installed, Reclamation exposes trays and machines as external supply endpoints:

- Nutrients: Hydroponic Tray, Soil Purifier, Compost Recycler.
- Seed Stock: Seed Vault Terminal, Gene Stabilizer.
- Machine Parts: Bio-Reactor, Greenhouse Controller, Pollinator Dock, Ecology Scanner.
- Food: recovered crop output through the Logistics food tag.

## WeatherCore

WeatherCore growth penalties apply only to exposed crops and trays. Safe greenhouse zones mitigate the hazard. Operators can disable or scale penalties in config.

## PowerGrid

PowerGrid acceleration is optional and configurable. Machines continue to work without power.

## HoloMap

Shared map data providers expose field and greenhouse markers through ECHO Core/HoloMap surfaces when available.

## ThemeCore

Screens use Cyberglass token lookups with built-in dark fallback colors.

## SoundCore

Shared ECHO cues play for restoration, completion, and reward moments when SoundCore is installed.

# ECHO Recovery

ECHO Recovery is a standalone-first death recovery addon for the ECHO ecosystem.

## Overview

- Public name: ECHO Recovery
- In-game name: Graves / Recovery
- Ashfall name: Field Recovery Cache
- Mod ID: `echorecovery`
- Current version: `1.3.0`

## Standalone Behavior

Works with only ECHO Core and NetCore installed. When a player dies:

- A protected grave/cache is created at a safe recovery position.
- Inventory, armor, offhand items, and XP are captured according to config and item rules.
- Grave ids, source dimension, hazard notes, expiry, sharing, and recovered state are saved with world data.
- A Grave Key can be created when key mode is enabled.
- A Recovery Compass can locate the nearest active grave and report distance/status.
- Right-clicking a grave opens the 54-slot grave UI; Recover All uses a validated server packet.

Players can use `/graves` to list, locate, recover, delete, share, inspect history, and use admin restore/delete paths when permitted.

## Datapacks

Recovery 1.3.0 loads content from:

- `data/<namespace>/echorecovery/recovery_grave_type/`
- `data/<namespace>/echorecovery/recovery_rule/`
- `data/<namespace>/echorecovery/recovery_preset/`

See `DATAPACKS.md` for examples.

## Integrations

Optional integrations are loaded only when the target addon is present. Remote recovery and powered delivery style features remain disabled by default unless server config enables them.

| Module | 1.3.0 behavior |
| --- | --- |
| Terminal | Registers Recovery archive/status/tool guidance and support missions. |
| MissionCore | Registers Recovery support missions and recovery progress hooks. |
| TutorialCore | Registers first-death, key, compass, and recover-all guidance. |
| SoundCore | Registers contextual grave create/open/recover/expired sound hooks. |
| ThemeCore/RenderCore | Registers Recovery theme and screen/profile metadata for UI consumers. |
| HoloMap | Adds/removes personal grave waypoints with the grave lifecycle. |
| Index | Adds Recovery entries for graves, keys, compass, rules, and presets. |
| Lens | Provides server-safe grave/cache scan details. |
| WorldCore/WeatherCore | Adds hazard and signal-quality copy without making defaults punitive. |
| RuntimeGuard/DataCore | Reports diagnostics and records death/recovery counters. |
| Ashfall/Nexus/Blackbox | Adds field-cache/corruption/evidence context through Recovery state and copy. |
| Armory/RelicTech/PlayerCore | Preserves stack metadata and exposes rule/cooldown-aware hooks where available. |
| Logistics/Convoy/PowerGrid | Registers disabled-by-default remote support hooks until the server enables compatible flows. |

## Commands

- `/graves list`
- `/graves locate <id>`
- `/graves recover <id>`
- `/graves delete <id>`
- `/graves history`
- `/graves share <player>`
- `/graves team`
- `/graves debug`
- `/graves reload`
- `/graves admin list <player>`
- `/graves admin restore <player> <id>`
- `/graves admin delete <player> <id>`

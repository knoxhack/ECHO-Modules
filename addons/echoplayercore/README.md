<!-- CURSEFORGE_README_START -->
# PlayerCore by ECHO Labs

![PlayerCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoplayercore/brand-sheet.png)

**Player utility, homes, random teleport, back, spawn, cooldown, and travel QoL systems for the ECHO/Ashfall ecosystem.**

![PlayerCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoplayercore/features-portrait.png)

![PlayerCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoplayercore/features-landscape.png)

## CurseForge Summary

Player utility, homes, random teleport, back, spawn, cooldown, and travel QoL systems for the ECHO/Ashfall ecosystem.

## Main Features

- Player profile data.
- Vitals and progression hooks.
- Shared player services.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoplayercore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoplayercore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoplayercore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO PlayerCore

Player utility, homes, random teleport, back, spawn, cooldown, and travel QoL systems for the ECHO/Ashfall ecosystem.

## Purpose
ECHO PlayerCore is a first-party ECHO addon that owns player utility commands and quality-of-life features. It is intentionally separate from ECHO Ashfall Protocol (survival campaign/content) and ECHO WorldCore (world regions/hazards/marker services).

## Commands
- `/sethome [name]` - Save your current location as a home.
- `/home [name]` - Teleport to a saved home.
- `/delhome [name]` - Delete a saved home.
- `/homes` - List your saved homes.
- `/back` - Return to your last teleport/death location.
- `/rtp` - Random teleport to a safe surface location.
- `/spawn` - Teleport to world spawn.
- `/echo sethome [name]`, `/echo home [name]`, `/echo delhome [name]`, `/echo homes`, `/echo back`, `/echo rtp`, `/echo spawn` - ECHO namespace aliases.

## Config
All config lives in `echoplayercore.toml` (COMMON side).

### Categories
- `general` - Enable/disable module and aliases.
- `homes` - Max homes, cross-dimension rules, naming rules.
- `random_teleport` - Radius, cooldown, safety checks, allowed dimensions.
- `back` - Cooldown, store-back rules, death recovery.
- `spawn` - Spawn command settings, cross-dimension rules.
- `permissions` - Op bypass, permission levels for admin commands.
- `performance` - RTP scan limits and optional RuntimeGuard integration.
- `messages` - Prefix and message style settings.

## Data Storage
Player travel data (homes, back, death, cooldowns) is stored via Minecraft `SavedData` per overworld by default. If ECHO DataCore is present, future integration may migrate to DataCore keys.

## Optional Integrations
- **DataCore** - Future persistent data bridge if safe.
- **WorldCore** - Safe-location search and hazard avoidance if available.
- **HoloMap** - Future home/warp/death markers provider.
- **Terminal** - Future Player/Travel tab DTOs.
- **RuntimeGuard** - RTP search budgeting if available.
- **ClaimCore** - Future claim-aware RTP landing.

## Future Roadmap
- TPA system (`/tpa`, `/tpaccept`, `/tpdeny`)
- Warps (`/warp`, `/warps`, `/setwarp`, `/delwarp`)
- Terminal Travel tab integration
- HoloMap home/warp markers
- Outpost fast travel network
- ClaimCore support for RTP

## License
All Rights Reserved

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echoplayercore.json`.
3. First action: run the documented command or trigger its in-world behavior.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echoplayercore.md`.

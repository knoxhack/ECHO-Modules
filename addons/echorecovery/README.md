<!-- CURSEFORGE_README_START -->
# Recovery by ECHO Labs

![Recovery by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echorecovery/brand-sheet.png)

**Recovery is the standalone-first ECHO death recovery addon.**

![Recovery by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echorecovery/features-portrait.png)

![Recovery by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echorecovery/features-landscape.png)

## CurseForge Summary

Recovery is the standalone-first ECHO death recovery addon.

## Main Features

- Recovery beacons.
- Rescue and restore workflows.
- Post-failure support.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echorecovery/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echorecovery/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echorecovery/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: Recovery

Recovery is the standalone-first ECHO death recovery addon. It provides graves, death caches, recovery keys, recovery compass support, grave UI, configurable recovery rules, and optional hooks into the wider ECHO stack.

## Player Usage

- Deaths create a recoverable grave or cache according to config and datapack grave type rules.
- Use the recovery compass, grave key, commands, or grave UI to find and recover stored items.
- If recovery fails, read the grave status and command feedback first; the module reports ownership, expiry, overflow, access, contamination, and placement fallback details where available.

## Standalone Use

Recovery runs with ECHO Core and NetCore without Terminal, ThemeCore, HoloMap, MissionCore, or Ashfall. Standalone servers can use commands and the grave UI as the primary access path.

## Optional Integrations

- ThemeCore skins the grave UI when present.
- HoloMap can show recovery markers.
- MissionCore and TutorialCore can react to recovery milestones.
- SoundCore can provide recovery cues.
- WorldCore, WeatherCore, and Ashfall can add hazard or profile context.

## Modpack Developer Notes

Detailed docs live under `docs/` in this addon. Start with `docs/README.md`, `docs/CONFIG.md`, `docs/INTEGRATIONS.md`, and `docs/DATAPACKS.md`.

## 1.3.0 Status

Release grade: B. The recovery loop is functional and validator-clean, but the UI still needs a dedicated visual/readability smoke pass before a public RC.

## 1.3.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echorecovery.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echorecovery.md`.

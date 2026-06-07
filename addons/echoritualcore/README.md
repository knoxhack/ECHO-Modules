<!-- CURSEFORGE_README_START -->
# RitualCore by ECHO Labs

![RitualCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoritualcore/brand-sheet.png)

****

![RitualCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoritualcore/features-portrait.png)

![RitualCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoritualcore/features-landscape.png)

## CurseForge Summary



## Main Features

- Ritual circles and altars.
- Rune channel automation.
- Arcane ceremony systems.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoritualcore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoritualcore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoritualcore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: RitualCore

Version: `1.0.0`

RitualCore is the first playable ritual slice for Arcana Division. It provides Basic Altar structure checks, Offering Pedestal input storage, ritual execution events, altar diagnostics, RelicTech stabilization, curse cleansing, and route hooks for Index, Grimoire, Lens, HoloMap, MissionCore, and Terminal.

## Role

- Owns reusable ritual definitions and altar runtime behavior.
- Publishes ritual completion/failure events for other ECHO systems.
- Keeps ritual content optional and bridgeable instead of tying it to Ashfall.

## Integrations

- Required: `echocore`, `echoarcanacore`.
- Optional: `echorelictech`, `echoterminal`, `echomissioncore`, `echolens`, `echoholomap`, `echoarcaneindex`, `echogrimoire`, and external `arcanaveil`.

## Validation

Run:

```bash
gradlew.bat :echoritualcore:compileJava
gradlew.bat validateArcanaDivision validateMissionRoutes
```

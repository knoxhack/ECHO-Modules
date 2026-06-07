<!-- CURSEFORGE_README_START -->
# CurseCore by ECHO Labs

![CurseCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echocursecore/brand-sheet.png)

****

![CurseCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echocursecore/features-portrait.png)

![CurseCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echocursecore/features-landscape.png)

## CurseForge Summary



## Main Features

- Curse containment wards.
- Corrupted artifact systems.
- Risk and warning sigils.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echocursecore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echocursecore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echocursecore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: CurseCore

Version: `1.0.0`

CurseCore is the persistent consequence layer for Arcana Division. It tracks live curse targets such as Echo Rot and Glass Veins, exposes curse stages to diagnostics, and bridges cleansing through RitualCore while leaving spell backlash and campaign-specific curse sources optional.

## Role

- Owns shared curse definitions and player curse state.
- Publishes curse gained/cleansed route hooks for MissionCore.
- Surfaces curse diagnostics through Terminal, Lens, Arcane Index, and Grimoire when present.

## Integrations

- Required: `echocore`, `echoarcanacore`.
- Optional: `echoritualcore`, `echospellcore`, `echoterminal`, `echomissioncore`, `echolens`, `echoarcaneindex`, `echogrimoire`, and external `arcanaveil`.

## Validation

Run:

```bash
gradlew.bat :echocursecore:compileJava
gradlew.bat validateArcanaDivision validateMissionRoutes
```

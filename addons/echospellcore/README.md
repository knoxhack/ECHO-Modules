<!-- CURSEFORGE_README_START -->
# SpellCore by ECHO Labs

![SpellCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echospellcore/brand-sheet.png)

****

![SpellCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echospellcore/features-portrait.png)

![SpellCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echospellcore/features-landscape.png)

## CurseForge Summary



## Main Features

- Modular spell systems.
- Casting glyphs and targeting.
- Arcana spell services.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echospellcore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echospellcore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echospellcore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: SpellCore

Version: `1.0.0`

SpellCore is the starter casting slice for Arcana Division. It introduces Signal Focus casting, starter Signal/Aether/Ash spell definitions, cooldown tracking, Aether Signal costs, HUD diagnostics, and optional bridges into RitualCore, CurseCore, Lens, MissionCore, Arcane Index, and Grimoire.

## Role

- Provides shared spell definitions and spell runtime hooks.
- Tracks focus status, awakened spell-core gating, cooldowns, and starter spell outcomes.
- Keeps spell backlash bridgeable through CurseCore when that addon is present.

## Integrations

- Required: `echocore`, `echoarcanacore`, `echonetcore`.
- Optional: `echoritualcore`, `echocursecore`, `echoterminal`, `echomissioncore`, `echolens`, `echoarcaneindex`, `echogrimoire`, and external `arcanaveil`.

## Validation

Run:

```bash
gradlew.bat :echospellcore:compileJava
gradlew.bat validateArcanaDivision validateMissionRoutes
```

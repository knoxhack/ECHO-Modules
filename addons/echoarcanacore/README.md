<!-- CURSEFORGE_README_START -->
# Arcana Core by ECHO Labs

![Arcana Core by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoarcanacore/brand-sheet.png)

****

![Arcana Core by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoarcanacore/features-portrait.png)

![Arcana Core by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoarcanacore/features-landscape.png)

## CurseForge Summary



## Main Features

- Aether Signal contracts.
- Arcana provider APIs.
- Magic addon bridge points.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoarcanacore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoarcanacore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoarcanacore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: Arcana Core

Version: `1.0.0`

Arcana Core is the shared foundation for ECHO's magic branch. It owns Aether Signal contracts, spell/ritual/curse/relic provider APIs, Arcana discovery records, and bridge points used by Arcane Index, Grimoire, RitualCore, SpellCore, CurseCore, RelicTech, Lens, HoloMap, MissionCore, and optional ARCANA campaign addons.

## Role

- Provides stable Arcana provider interfaces and service registration.
- Keeps magic systems data-driven and Index-first instead of hardcoding campaign ownership.
- Allows optional addons to publish Arcana records without requiring Ashfall.

## Integrations

- Required: `echocore`.
- Optional: `echonetcore`, `echodatacore`, `echoterminal`, `echoindex`, and external `arcanaveil`.

## Validation

Run:

```bash
gradlew.bat :echoarcanacore:compileJava
gradlew.bat validateArcanaDivision
```

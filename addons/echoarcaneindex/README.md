<!-- CURSEFORGE_README_START -->
# Arcane Index by ECHO Labs

![Arcane Index by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoarcaneindex/brand-sheet.png)

****

![Arcane Index by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoarcaneindex/features-portrait.png)

![Arcane Index by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoarcaneindex/features-landscape.png)

## CurseForge Summary



## Main Features

- Arcane record index.
- Rune and spell discoveries.
- Magic knowledge catalog.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoarcaneindex/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoarcaneindex/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoarcaneindex/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: Arcane Index

Version: `1.0.0`

Arcane Index is the official magic knowledge browser for Arcana Division. It extends ECHO: Index with Aether Signal, spell, ritual, curse, relic, Veilbound bridge, and discovery-state pages so players can learn magic through reference records before crafting blindly.

## Role

- Publishes Arcana reference pages through the shared Index surface.
- Reads Arcana Core providers for dynamic relic, ritual, spell, and curse entries.
- Keeps JEI optional; JEI may mirror recipes, but Arcane Index remains the source of truth.

## Integrations

- Required: `echocore`, `echoarcanacore`, `echoindex`.
- Optional: external `arcanaveil` and JEI.

## Validation

Run:

```bash
gradlew.bat :echoarcaneindex:compileJava
gradlew.bat validateArcaneIndex
```

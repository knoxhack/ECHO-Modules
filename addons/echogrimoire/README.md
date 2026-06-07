<!-- CURSEFORGE_README_START -->
# Grimoire by ECHO Labs

![Grimoire by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echogrimoire/brand-sheet.png)

****

![Grimoire by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echogrimoire/features-portrait.png)

![Grimoire by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echogrimoire/features-landscape.png)

## CurseForge Summary



## Main Features

- Spell book interface.
- Mystic pages and records.
- Arcana learning workflow.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echogrimoire/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echogrimoire/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echogrimoire/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: Grimoire

Version: `1.0.0`

Grimoire is the Terminal archive surface for Arcana Division. It presents lore, warnings, forbidden-page context, Veilbound summaries, relic notes, ritual field records, spell primers, and curse consequences through ECHO Terminal archives.

## Role

- Adds Arcana lore and progression records to Terminal archives.
- Bridges Arcana Core providers into readable field notes.
- Links back to Arcane Index pages where reference detail belongs.

## Integrations

- Required: `echocore`, `echoarcanacore`, `echoterminal`.
- Optional: `echoarcaneindex` and external `arcanaveil`.

## Validation

Run:

```bash
gradlew.bat :echogrimoire:compileJava
gradlew.bat validateGrimoire
```

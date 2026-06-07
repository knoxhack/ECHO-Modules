<!-- CURSEFORGE_README_START -->
# Wiki by ECHO Labs

![Wiki by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echowiki/brand-sheet.png)

**Wiki is the in-game documentation/content registry module for ECHO.**

![Wiki by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echowiki/features-portrait.png)

![Wiki by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echowiki/features-landscape.png)

## CurseForge Summary

Wiki is the in-game documentation/content registry module for ECHO.

## Main Features

- In-game guide books.
- Searchable article collections.
- Lore and tutorial records.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echowiki/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echowiki/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echowiki/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: Wiki

Wiki is the in-game documentation/content registry module for ECHO. It is intended to expose article collections, module docs, and datapack-authored reference content without making Ashfall the only documentation path.

## Standalone Use

Wiki can run as a documentation addon with Core, NetCore, and ScreenCore. Module content should be generic by default; Ashfall article packs belong in Ashfall-specific namespaces.

## Integration Rules

- Index can cross-link wiki articles when both modules are installed.
- Terminal can expose wiki pages as an optional documentation page.
- ThemeCore may skin article surfaces, but Wiki must keep a readable fallback style.
- Datapack content should use stable schema-backed article and collection definitions.

## 1.0.0 Status

Release grade: Experimental. The addon is build-included and resource-valid after pack metadata repair, and 1.0.0 documents it as an active documentation surface. It still needs fuller article examples, player-facing browsing polish, and UI smoke tests before it should be treated as release-polished.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echowiki.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echowiki.md`.

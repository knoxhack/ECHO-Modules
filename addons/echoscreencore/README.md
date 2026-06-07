<!-- CURSEFORGE_README_START -->
# ScreenCore by ECHO Labs

![ScreenCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoscreencore/brand-sheet.png)

**ScreenCore is the shared ECHO UI contract layer.**

![ScreenCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoscreencore/features-portrait.png)

![ScreenCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoscreencore/features-landscape.png)

## CurseForge Summary

ScreenCore is the shared ECHO UI contract layer.

## Main Features

- EUI screen layouts.
- Reusable UI components.
- Input and render bridges.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoscreencore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoscreencore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoscreencore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: ScreenCore

ScreenCore is the shared ECHO UI contract layer. It provides EUI action contexts, data contexts, screen-control contracts, and client bridge patterns for modules that want richer screens without forcing those contracts onto standalone stacks.

## Standalone Use

ScreenCore can be installed with Core and NetCore as a UI framework module. Other addons should treat it as optional unless their `neoforge.mods.toml` declares it as a required dependency.

## Integration Rules

- Guard ScreenCore-only client classes behind `ModList.get().isLoaded("echoscreencore")`.
- Avoid importing ScreenCore action/control classes from common startup paths in modules where ScreenCore is optional.
- Prefer reflection or small guarded bridge classes for optional tests and diagnostics.
- ThemeCore is optional; screens must keep a Default Dark fallback.

## 1.0.0 Status

Release grade: Beta. The framework is real and build-included, and 1.0.0 tracks it as an active optional UI layer. Remaining RC polish is downstream optional-classloading smoke coverage, compact-layout review, and fuller developer examples for guarded bridges.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echoscreencore.json`.
3. First action: review the API/tooling docs before using it in a public pack.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echoscreencore.md`.

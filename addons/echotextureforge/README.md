<!-- CURSEFORGE_README_START -->
# TextureForge by ECHO Labs

![TextureForge by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echotextureforge/brand-sheet.png)

**TextureForge is the ECHO asset-audit and texture-pipeline tooling module.**

![TextureForge by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echotextureforge/features-portrait.png)

![TextureForge by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echotextureforge/features-landscape.png)

## CurseForge Summary

TextureForge is the ECHO asset-audit and texture-pipeline tooling module.

## Main Features

- Material generation workflows.
- Texture preview benches.
- Creator-facing asset tools.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echotextureforge/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echotextureforge/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echotextureforge/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: TextureForge

TextureForge is the ECHO asset-audit and texture-pipeline tooling module. It keeps source-sheet manifests, texture refresh reports, and command/tool hooks close to the playable mod stack so pack developers can audit visual readiness.

## Standalone Use

TextureForge is useful as a developer/admin module with Core and NetCore. It is not required for normal player-facing modules and should not be a hard dependency for gameplay content.

## Integration Rules

- Command Center may link to TextureForge audit outputs when present.
- ThemeCore may provide styling for TextureForge UI surfaces, but Default Dark fallback is required.
- Generated source sheets remain art sources; runtime resource JSON must reference final promoted textures only.

## 1.0.0 Status

Release grade: Experimental. The texture refresh v2 validation path is passing and remains the authoritative asset baseline for 1.0.0. Remaining RC polish is dashboard bridge clarity, in-game review ergonomics, and human visual review of promoted assets.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echotextureforge.json`.
3. First action: review the API/tooling docs before using it in a public pack.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echotextureforge.md`.

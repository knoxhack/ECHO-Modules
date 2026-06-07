<!-- CURSEFORGE_README_START -->
# Base Grid by ECHO Labs

![Base Grid by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echobasegrid/brand-sheet.png)

**Claim chunks, trust members, and protect builds with a standalone ScreenCore base-management grid.**

![Base Grid by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echobasegrid/features-portrait.png)

![Base Grid by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echobasegrid/features-landscape.png)

## CurseForge Summary

Base Grid is the ECHO chunk-claiming and base protection addon for servers, SMP packs, and custom survival worlds. It adds a ScreenCore claim map, server-authoritative claim actions, trusted-member permissions, player commands, and optional Terminal navigation without requiring Ashfall.

## Main Features

- ScreenCore local chunk grid with owned, trusted, open, and occupied claim states.
- Server-authoritative chunk claiming, unclaiming, and member permission actions.
- Trusted-member roles for build, interact, storage, and management access.
- Player commands: `/basegrid`, `/basegrid status`, `/basegrid claim`, `/basegrid unclaim`, and `/basegrid inspect`.
- Optional ECHO Terminal tab when Terminal is installed.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echobasegrid/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echobasegrid/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echobasegrid/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: Base Grid

Version: `1.0.0`

Base Grid provides the shared base-claiming and protection layer for ECHO servers and packs. It uses ScreenCore pages for claim management, NetCore packets for guarded claim actions, player commands for quick chunk operations, and optional Terminal navigation so players can manage chunks, members, and permissions without requiring Ashfall.

## Role

- Tracks claimed chunks, member roles, permissions, and base protection state.
- Uses ScreenCore for the management UI and NetCore for server-authoritative actions.
- Provides `/basegrid` and `/echo_basegrid` commands for status, claim, unclaim, and inspection flows.
- Stays standalone-first so custom packs can use claims without the Ashfall campaign.

## Integrations

- Required: `echocore`, `echonetcore`, `echoscreencore`.
- Optional: `echoterminal`.

## Validation

Run:

```bash
gradlew.bat :echobasegrid:compileJava
gradlew.bat buildEchoWorkspace -PechoAddonSet=all
```

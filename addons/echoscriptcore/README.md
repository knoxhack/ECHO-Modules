<!-- CURSEFORGE_README_START -->
# ScriptCore by ECHO Labs

![ScriptCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoscriptcore/brand-sheet.png)

**ScriptCore by ECHO Labs is a JSON-first campaign authoring framework for standalone modpack creators and ECHO addon stacks.**

![ScriptCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoscriptcore/features-portrait.png)

![ScriptCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoscriptcore/features-landscape.png)

## CurseForge Summary

ScriptCore by ECHO Labs is a JSON-first campaign authoring framework for standalone modpack creators and ECHO addon stacks.

## Main Features

- JSON-first campaign authoring.
- Missions, lore, scans, and world states.
- Safe condition/action flows.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoscriptcore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoscriptcore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoscriptcore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: ScriptCore

ScriptCore by ECHO Labs is a JSON-first campaign authoring framework for standalone modpack creators and ECHO addon stacks.

## Standalone Use

ScriptCore can run with only ECHO Core. It loads safe JSON definitions from `config/echo/scripts`, validates them, exposes a registry/API, generates examples, and provides `/echo scriptcore` diagnostics without requiring Ashfall or optional UI/runtime addons.

## Integration Rules

- Ashfall is content, not a ScriptCore dependency.
- Optional integrations are adapter driven and must degrade to diagnostics when unavailable.
- ScriptCore 1.0.0 never executes JavaScript, Groovy, Lua, shell commands, reflection targets from JSON, or class names from JSON.
- Other packs should use their own namespaces and pack folders.

## 1.0.0 Status

Release grade: integration polish. The addon provides loader, registry, validation, commands, docs, examples, DataCore-backed runtime state, runtime migration tools, Terminal diagnostics, and a trusted ScreenCore/NetCore UI bridge for previewing and executing preloaded JSON actions.

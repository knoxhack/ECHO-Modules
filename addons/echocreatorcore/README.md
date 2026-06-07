<!-- CURSEFORGE_README_START -->
# CreatorCore by ECHO Labs

![CreatorCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echocreatorcore/brand-sheet.png)

**CreatorCore by ECHO Labs is the in-game creator/admin authoring suite for ECHO-powered Minecraft modpacks.**

![CreatorCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echocreatorcore/features-portrait.png)

![CreatorCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echocreatorcore/features-landscape.png)

## CurseForge Summary

CreatorCore by ECHO Labs is the in-game creator/admin authoring suite for ECHO-powered Minecraft modpacks.

## Main Features

- Visual creator dashboard.
- Draft, validate, and export tools.
- Authoring layer for ECHO packs.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echocreatorcore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echocreatorcore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echocreatorcore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: CreatorCore

CreatorCore by ECHO Labs is the in-game creator/admin authoring suite for ECHO-powered Minecraft modpacks. It is a visual dashboard and authoring layer that sits above ScriptCore and the existing ECHO runtime systems.

CreatorCore does not replace ScriptCore, Terminal, MissionCore, ScreenCore, Lens, HoloMap, TextureForge, Wiki, or Command Center:

- ScriptCore owns JSON/data definitions, validation, reload, and export APIs.
- CreatorCore owns in-game editor UI, dashboards, drafts, previews, and validation screens.
- MissionCore, Terminal, Lens, HoloMap, WeatherCore, TutorialCore, ThemeCore, TextureForge, and Wiki remain runtime/gameplay systems.

## Current 0.2.1 Features

- Creator dashboard shell with Overview, Definitions, Detail, Validation, Drafts, Mission Studio, Previews, Adapters, Export, and Roadmap panels.
- Public CreatorCore API for adapters, definition details, preview summaries, form schemas, panels, sessions, drafts, validation, exports, and projects.
- Guarded ScriptCore bridge for real definition summaries, detail views, diagnostics, reload, draft save, validation, and export delegation.
- ScreenCore EUI dashboard page when `echoscreencore` is present, with first-definition detail, lock reasons, last export state, and the vanilla dashboard retained as fallback.
- Terminal tab and addon summary card when `echoterminal` is present.
- Read-only MissionCore mission previews and detail views through `MissionCoreService`.
- Read-only Lens provider and ScriptCore `lens_scan` previews.
- Read-only HoloMap layer, marker, route, and ScriptCore HoloMap previews.
- Draft service with generic templates for mission, archive entry, Lens scan, HoloMap marker, weather event, faction, world state, and tutorial hint content.
- Mission Studio first-pass form schema for draft-only mission authoring.
- Mission Studio draft command that creates consistent mission draft JSON with briefing, chapter, phase, prerequisites, objectives, and rewards.
- Validation center backend and doctor report.
- Export service that delegates to ScriptCore authoring/export hooks when available and falls back to conservative JSON export only when ScriptCore is unavailable.
- Codex Studio foundation that can talk to a local Echo Codex Bridge sidecar for repo-editing creator jobs after explicit config unlocks.
- Focused GameTests for definition aggregation, ScriptCore export delegation, optional missing-adapter behavior, and Mission Studio schema coverage.
- Shared `/echo` commands with permission and config gates.
- Safe-by-default config: dashboard is read-only, draft writes and exports are locked until explicitly enabled.

## Commands

- `/echo creatorcore status`
- `/echo creatorcore open`
- `/echo creatorcore doctor`
- `/echo creatorcore adapters`
- `/echo creatorcore drafts list`
- `/echo creatorcore drafts create <type> <pack> <id>`
- `/echo creatorcore mission draft <pack> <id>`
- `/echo creatorcore drafts validate <id>`
- `/echo creatorcore drafts export <id>`
- `/echo creatorcore codex status`
- `/echo creatorcore codex run <profile> [prompt]`
- `/echo creatorcore codex refresh <job>`
- `/echo creatorcore codex validate <job>`
- `/echo creatorcore codex cancel <job>`
- `/echo creatorcore reload`
- `/echo creatorcore report`
- `/echo creatorcore help`

Aliases:

- `/echo creator status`
- `/echo creator open`

## Safety Notes

CreatorCore defaults to read-only:

- `allow_in_game_editing=false`
- `allow_draft_writes=false`
- `allow_exports=false`
- `allow_codex_bridge=false`
- `allow_codex_repo_edits=false`
- `require_operator=true`
- `operator_permission_level=2`

File writes require both config unlocks and command permission. Export paths and draft paths are normalized and checked to prevent path traversal.

Codex Studio requires the local bridge to be running outside Minecraft:

```bash
python tools/echo_codex_bridge.py --workspace C:\Github\Echo --allow-repo-edits
```

Use `--allow-repo-edits` when you want the bridge to accept edit jobs, and optionally `--auth-token <token>` with matching `codex_bridge_token` in CreatorCore config. The bridge keeps Codex/OpenAI auth in the local Codex CLI config. Minecraft only talks to `codex_bridge_url` on localhost and never stores API keys.

## Optional Dependencies

CreatorCore requires `echocore`. ScriptCore is optional but, when present, CreatorCore uses its public registry, validation, reload, draft, and export APIs. ScreenCore and Terminal are optional client integrations. MissionCore, Lens, and HoloMap expose read-only previews when present. WeatherCore, TutorialCore, ThemeCore, TextureForge, and Wiki remain safe status/reporting stubs until their creator-facing APIs are ready.

## Roadmap

0.3.0 focuses on richer Mission Studio editing, Lore Archive Studio, Lens Scan Studio, and HoloMap Marker Studio. Later releases add mission graph editing, condition/action builders, faction/world-state/weather editors, AI-assisted hooks, Command Center integration, and ECHO Launcher publishing.

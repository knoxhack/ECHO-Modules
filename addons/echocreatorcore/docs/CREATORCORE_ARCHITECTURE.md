# CreatorCore Architecture

CreatorCore is organized as a dashboard and authoring foundation, not a runtime replacement.

## Layers

- `api`: public contracts for adapters, sessions, permissions, projects, definitions, diagnostics, panels, drafts, and export results.
- `adapter`: optional addon bridge registry. Adapters report detected/missing/API-wired status and expose capabilities.
- `definition`: aggregate service for definition summaries, details, preview summaries, and form schemas.
- `session`: per-player creator sessions, project metadata, and permission checks.
- `draft`: generic JSON draft templates, in-memory examples, file-backed draft store, and validation helpers.
- `validation`: diagnostic index and doctor report shared by commands and UI.
- `export`: ScriptCore-first export delegation with conservative JSON fallback when ScriptCore is unavailable.
- `ui`: common panel models plus the client-only vanilla dashboard screen.
- `client`: optional ScreenCore and Terminal entry points isolated from common/server class loading.
- `command`: shared `/echo creatorcore` command tree.

## Adapter Model

Adapters are intentionally small:

- `id`
- display name
- availability
- status
- capabilities
- definition summaries
- definition detail views
- preview summaries
- form schemas
- diagnostics
- optional panel provider
- draft creation/export hooks
- reload hook
- debug info

0.2.1 uses ModList, client-only isolation, and reflection for optional runtime bridges. Missing optional addons do not crash CreatorCore.

## Drafts

Drafts are generic JSON objects wrapped in a CreatorCore envelope. They are not a second ScriptCore format. They are temporary authoring documents that can be exported to ScriptCore-compatible JSON once the pack owner unlocks writes/exports.

## Validation

The validation center combines:

- CreatorCore internal diagnostics
- adapter diagnostics
- draft diagnostics
- ScriptCore diagnostics when the optional ScriptCore bridge is available

Commands and UI both use `CreatorValidationService` and `CreatorDoctorReport`.

## Export

The export service validates drafts, checks config gates, normalizes paths, prevents traversal, and then delegates to ScriptCore when available. ScriptCore receives the draft JSON through its authoring service, validates it, and exports through its own hooks. If ScriptCore is unavailable, CreatorCore uses the conservative JSON writer and creates backups before overwriting.

## UI

The dashboard can open as a ScreenCore EUI page at `echocreatorcore:creator_dashboard` when ScreenCore is present. The ScreenCore page exposes dashboard status, adapters, diagnostics, definitions, first-definition detail, drafts, Mission Studio field state, last export state, and lock reasons. The vanilla `GuiGraphicsExtractor` dashboard remains the fallback. Terminal integration registers a client tab and addon summary card when Terminal is present.

## Mission Studio

Mission Studio 0.2.1 is a draft-only form surface. It publishes a mission form schema through the internal adapter and writes only through CreatorCore draft services when config and permissions allow writes. The `/echo creatorcore mission draft <pack> <id>` helper creates a schema-aligned mission draft with briefing, chapter, phase, prerequisites, objectives, and rewards fields. Runtime MissionCore data remains read-only.

## Test Coverage

CreatorCore registers focused GameTests for definition service aggregation, ScriptCore export delegation success/failure behavior, missing optional-adapter safety, and Mission Studio schema/draft field coverage. These are intentionally lightweight hardening tests for the authoring foundation rather than full UI automation.

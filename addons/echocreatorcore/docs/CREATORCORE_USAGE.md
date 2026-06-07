# CreatorCore Usage

## Opening The Dashboard

Use:

```mcfunction
/echo creatorcore open
```

On the physical client, CreatorCore also registers an unbound keybind named `Open CreatorCore Dashboard` under the `ECHO: CreatorCore` category.

When ScreenCore is installed and `prefer_screencore_ui=true`, CreatorCore opens the native ScreenCore page `echocreatorcore:creator_dashboard`. If ScreenCore is missing or unavailable, it falls back to the vanilla cyberglass dashboard.

When Terminal is installed, CreatorCore also registers a CreatorCore Terminal tab and an addon summary card in Terminal's mod route area.

The dashboard is client-only. On a dedicated server, the command reports that the UI must be opened from a client context instead of trying to load client screen classes server-side.

## Running Doctor

Use:

```mcfunction
/echo creatorcore doctor
/echo creatorcore report
```

Doctor reports adapter availability, definition counts, draft counts, diagnostic totals, ScriptCore status, write/export lock state, root paths, and path safety.

The ScreenCore dashboard also surfaces the same write/export lock reasons, the first available definition detail in the inspector, and the last export result.

## Creating Drafts

Draft writes are locked by default. Enable `allow_draft_writes=true` and use a player/console with CreatorCore `CREATOR` or `DEVELOPER` permission.

```mcfunction
/echo creatorcore drafts create mission example example:repair_radio
```

Mission Studio provides a mission-specific helper:

```mcfunction
/echo creatorcore mission draft example example:repair_radio
```

That helper creates the same draft envelope but guarantees the mission content has `briefing`, `chapter`, `phase`, `prerequisites`, `objectives`, and `rewards` fields for the form surface.

Available templates:

- `mission`
- `archive_entry`
- `lens_scan`
- `holomap_marker`
- `weather_event`
- `faction`
- `world_state`
- `tutorial_hint`

Drafts are stored under:

```text
config/echo/creatorcore/drafts/<pack>/<type>/<namespace>/<path>.json
```

## Validating Drafts

Use:

```mcfunction
/echo creatorcore drafts validate example:repair_radio
```

Validation checks the generic CreatorCore draft envelope and content presence. When ScriptCore is installed, the dashboard and reports include live ScriptCore definition diagnostics, and export flows ask ScriptCore to validate drafts before writing.

## Exporting Drafts

Exports are locked by default. Enable `allow_exports=true` and use `DEVELOPER` permission.

```mcfunction
/echo creatorcore drafts export example:repair_radio
```

CreatorCore 0.2.1 exports through ScriptCore first when ScriptCore is installed and its authoring service accepts the draft. If ScriptCore is unavailable, CreatorCore falls back to conservative JSON export under:

```text
config/echo/scripts/<pack>/<type>s/<namespace>/<path>.json
```

Existing files are backed up with a timestamped `.bak.<millis>` suffix before overwrite.

If ScriptCore is installed but refuses the draft save, validation, or export, CreatorCore reports that failure instead of pretending the export succeeded.

## Mission Studio

Mission Studio is draft-only in 0.2.1. It exposes form fields for pack, id, title, briefing, chapter, phase, kind, prerequisites, objectives, and rewards, but it does not mutate live MissionCore definitions.

Create a base mission draft with:

```mcfunction
/echo creatorcore mission draft example example:repair_radio
```

Then use the dashboard's Mission Studio and validation panels to inspect the draft and export only after config and permissions allow it.

## Read-Only Previews

MissionCore previews show registered mission definitions, objectives, rewards, chapter, phase, and status metadata. Lens previews show provider diagnostics and ScriptCore `lens_scan` definitions. HoloMap previews show runtime layers, markers, routes, provider diagnostics, and ScriptCore HoloMap definitions.

## Permissions

- `VIEWER`: dashboard/status/diagnostics/adapter status
- `OPERATOR`: doctor, validation center, reload requests
- `CREATOR`: create/edit/validate drafts
- `DEVELOPER`: export/delete/migrate/dangerous development tools

Default server players without operator permission are blocked because `require_operator=true`.

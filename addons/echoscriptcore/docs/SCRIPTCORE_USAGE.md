# ScriptCore Usage

ScriptCore by ECHO Labs is a JSON-first modpack campaign authoring framework. It loads safe JSON definitions for missions, archive entries, Lens scans, HoloMap markers, weather events, factions, world states, tutorial hints, dialogue, endings, recipe unlocks, loot profiles, and generic condition/action flows.

## Installation

Required:
- `echocore`
- `echoscriptcore`

Optional enhanced runtimes:
- `echomissioncore` for mission runtime and mission actions.
- `echoterminal` for archive/status surfaces.
- `echolens` for scan registrations.
- `echoholomap` for map layers and markers.
- `echoweathercore`, `echotutorialcore`, `echosoundcore`, `echoindex`, `echodatacore`, and `echoworldcore` for richer future adapters.

Ashfall is not required.

## Folder Layout

ScriptCore reads only `.json` files under:

```text
config/echo/scripts/
```

Recommended pack layout:

```text
config/echo/scripts/<pack_id>/
  missions/
  archive/
  lore/
  lens/
  holomap/
  weather/
  factions/
  world_state/
  tutorials/
  dialogue/
  endings/
  recipes/
  loot/
  generic/
```

Shared definitions can live under `config/echo/scripts/global/`. Flat files such as `config/echo/scripts/test.echo.json` are accepted, but a missing `pack` will be reported as `unknown`.

## Reload And Validation

Commands:

```text
/echo scriptcore status
/echo scriptcore reload
/echo scriptcore reload pack <pack>
/echo scriptcore reload type <type>
/echo scriptcore validate
/echo scriptcore validate verbose
/echo scriptcore doctor
```

`status`, `list`, and `show` are read-only. Reload, validate, doctor, export, and draft commands require gamemaster permission.

## Examples

On first run, ScriptCore can generate examples under:

```text
config/echo/scripts/examples/generic_survival/
config/echo/scripts/examples/tech_progression/
config/echo/scripts/examples/ashfall/
```

The generic examples use vanilla IDs where possible. Ashfall examples are marked as example-only and are never required by ScriptCore.

## ScreenCore Bridge

When `echoscreencore` and `echonetcore` are present, trusted screens can use `scriptcore.preview` and `scriptcore.execute` against loaded definitions. The generated `generic_survival:repair_radio` mission demonstrates an executable `on_complete` slot, and `generic_survival:radio_choice` demonstrates declared typed params through `metadata.screencore_ui.params`.

Enable the bridge with `scriptcore.allow_screencore_ui_actions=true`. ScriptCore still rejects raw executable JSON, arbitrary script text, unknown actions, `custom` actions, invalid slots, unmet conditions, undeclared params, and embedded placeholders.

## Safety

ScriptCore 1.0.0 never executes arbitrary scripts. It does not load `.js`, `.kjs`, `.txt`, `.disabled`, files outside `config/echo/scripts`, or files larger than `max_file_size_kb`.

Reloads parse into temporary data first. If `fail_pack_on_error=true`, the previous registry stays active when errors are found. Otherwise valid definitions load and invalid definitions are skipped.

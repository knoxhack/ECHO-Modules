# SignalOS Data Formats

SignalOS loads datapack JSON from each namespace under:

- `data/<namespace>/signalos/apps/*.json`
- `data/<namespace>/signalos/data_records/*.json`
- `data/<namespace>/signalos/drive_templates/*.json`
- `data/<namespace>/signalos/net_sites/*.json`
- `data/<namespace>/signalos/chapters/*.json`
- `data/<namespace>/signalos/missions/*.json`
- `data/<namespace>/signalos/archives/*.json`

## App

```json
{
  "title": "Field Files",
  "type": "files",
  "summary": "Browse recovered field records.",
  "order": 25,
  "accentColor": 6737151,
  "icon": "minecraft:compass",
  "permission": "user",
  "view": "",
  "recordTypes": [],
  "recordSources": [],
  "includeArchived": false,
  "emptyText": "NO RECORDS AVAILABLE"
}
```

Known built-in app types include `home`, `files`, `notes`, `logs`, `network`, `settings`, `data_vault`, `echo_link`, `missions`, `archives`, `rewards`, and `diagnostics`.

Terminal app rendering resolves in this order:

1. Built-in app `type`.
2. Client Java renderer registered with `SignalOsAppRenderers.register(type, renderer)`.
3. Config record view when `view` is `"records"`.
4. Unsupported metadata view.

Set `view` to `"records"` for a JSON-only record browser. `recordTypes` filters record `type` values, `recordSources` filters record `source` values, `includeArchived` controls whether archived records are visible, and `emptyText` replaces the default empty-state text.

```json
{
  "title": "Field Records",
  "type": "field_records",
  "summary": "Focused records from field modules.",
  "order": 40,
  "accentColor": 6737151,
  "icon": "minecraft:writable_book",
  "view": "records",
  "recordTypes": ["record", "diagnostic"],
  "recordSources": ["Example Module"],
  "includeArchived": false,
  "emptyText": "NO FIELD RECORDS"
}
```

## Data Record

```json
{
  "title": "Desktop Shell",
  "type": "record",
  "source": "SignalOS Core",
  "order": 0,
  "archived": false,
  "lines": [
    "SignalOS now boots into a desktop shell.",
    "Server racks expose installed data drives to the current operator network."
  ]
}
```

`body` can be used instead of `lines` when the record is a single string. If both are present, `lines` wins and is joined with newlines.

## Drive Template

```json
{
  "schemaVersion": 2,
  "label": "Field Drive",
  "settings": {
    "theme": "signal"
  },
  "session": {
    "selected_app": "signalos:files"
  },
  "records": [
    {
      "id": "example:drive/field_boot",
      "title": "Field Boot",
      "type": "record",
      "source": "Example Drive Template",
      "body": "Portable data drives can carry records into a rack-backed SignalOS network.",
      "order": 0,
      "metadata": {
        "signalos.path": "/records/field_boot.txt",
        "signalos.mime": "text/plain"
      }
    }
  ]
}
```

Drive template records use the same fields as data records. Each embedded record can provide an explicit `id`; if omitted, SignalOS derives one from the template id and record index.

Drive templates must declare `schemaVersion: 2`. Drives/components without a schema version still decode as legacy V1 data, but SignalOS Drive API V2 treats them as unsupported/read-only and rejects terminal boot, rack insertion, and filesystem writes. New blank drive items initialize as V2.

`metadata` is optional on any data record. SignalOS Drive API V2 reserves `signalos.path`, `signalos.mime`, `signalos.created`, `signalos.modified`, `signalos.readonly`, and `signalos.tags`; filesystem paths are absolute `/`-prefixed paths stored in `signalos.path`.

`settings` and `session` are optional maps used by the focused OS shell. Terminal and workstation boot-drive slots store OS settings, session hints, files, and notes on the inserted V2 data drive.

Rack player actions apply templates through the server-rack menu and cap player-edited drives at 64 records. Addon code can still construct drive data directly through `SignalOsDriveData` helper methods when it needs a custom workflow.
Java addons can inspect loaded templates through `SignalOsApi.driveTemplate(id)` and `SignalOsApi.driveTemplates()` after datapack content has loaded.
Provider health rows are available through `SignalOsApi.providerStatuses(player)`. Data, peripheral, and diagnostic providers can override `providerStatus(player)` to add richer Diagnostics metadata without changing their content methods.

## SignalNet Site

```json
{
  "address": "echo.home",
  "title": "ECHO Home",
  "summary": "Recovered landing page for the local SignalNet.",
  "requiredTier": 0,
  "order": 0,
  "tags": ["home", "status"],
  "pages": [
    {
      "path": "/",
      "title": "Home",
      "lines": ["SignalNet pages are curated and do not load external URLs."],
      "links": [
        { "label": "Network Status", "address": "echo.home/status" }
      ]
    }
  ]
}
```

SignalNet addresses are scheme-free curated addresses, not HTTP URLs. Site access is filtered by the active SignalOS network tier; terminal access starts at tier 1, workstations and racks can raise it. Java addons can publish generated sites through `SignalOsApi.registerNetProvider(...)`.

## Chapter

```json
{
  "title": "Field Ops",
  "section": "progress",
  "order": 10,
  "accentColor": 65535,
  "icon": "minecraft:compass",
  "visible": true,
  "pages": ["missions", "archives", "rewards", "diagnostics"]
}
```

## Mission

```json
{
  "chapter": "example:field_ops",
  "title": "Secure Cache",
  "description": "Recover the first support cache.",
  "objectives": ["Find shelter", "Open SignalOS"],
  "order": 10,
  "icon": "minecraft:chest",
  "completionAdvancement": "minecraft:story/root",
  "rewardClaim": true,
  "displayRewards": [
    { "item": "minecraft:bread", "count": 4, "label": "Emergency rations" }
  ]
}
```

## Archive

```json
{
  "chapter": "example:field_ops",
  "title": "Field Brief",
  "group": "Briefings",
  "status": "OPEN",
  "order": 10,
  "locked": false,
  "lines": ["SignalOS records should be short, searchable, and chapter-owned."]
}
```

## Validation

SignalOS rejects malformed JSON, duplicate data IDs, invalid identifiers, blank objectives or archive lines, missing chapter references, reward counts below one, and reward items that do not resolve in the loaded item registry. Java and script-registered chapters can satisfy JSON mission/archive chapter references.
Server-side rack actions also validate the open menu, rack block position, selected drive slot, held drive item, payload size, record id, and template id before mutating drive data.

## Server App Actions

Java addons can register server-side app actions without inventing their own packet path:

```java
SignalOsApi.registerAppAction(
    SignalOsApi.id("example:field_records"),
    SignalOsApi.id("example:refresh"),
    (context, payload) -> {
        // context.player(), context.networkId(), context.accessTier(),
        // context.activeDrivePresent(), and context.activeDriveLabel()
        // describe the active server-owned SignalOS network and boot drive.
});
```

New write-aware actions should use `SignalOsApi.registerAppActionResult(...)` and return `SignalOsActionResult`, so missing drives, legacy drives, invalid paths, full drives, and read-only records can produce structured result codes plus user-facing status text. Old void handlers still work through an adapter.

Client renderers can dispatch those actions with `SignalOsAppRenderContext#sendAction(appId, actionId, payload)`. The same terminal action rate limits and resync path apply.

## Schemas

The shared schema index includes SignalOS schemas for apps, data records, drive templates, chapters, missions, and archives under `assets/echocore/schemas/`. They are intentionally lenient and match the datapack paths listed at the top of this file.

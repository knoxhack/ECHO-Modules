<!-- CURSEFORGE_README_START -->
# SignalOS by ECHO Labs

![SignalOS by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/signalos/brand-sheet.png)

**Standalone Echo-compatible computer tech addon with desktop shell, apps, data drives, server racks, missions, archives, and diagnostics.**

![SignalOS by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/signalos/features-portrait.png)

![SignalOS by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/signalos/features-landscape.png)

## CurseForge Summary

Standalone Echo-compatible computer tech addon with desktop shell, apps, data drives, server racks, missions, archives, and diagnostics.

## Main Features

- Signal dashboards.
- Status cards and workflows.
- Modular operating surface.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/signalos/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/signalos/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/signalos/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# SignalOS

SignalOS is a standalone Echo-compatible computer tech addon for NeoForge. It provides a full-screen desktop shell, utility apps, networked computer blocks, portable data drives, and a defensive bridge into Echo Core state.

SignalOS is not a replacement for `echoterminal`. It owns the computer OS fantasy while continuing to expose legacy-compatible mission, archive, reward, and diagnostic surfaces inside the new shell.

## 1.0.0 Scope

SignalOS 1.0.0 ships:

- `signalos:terminal` as the base access point and `signalos:workstation` as the stronger access tier.
- `signalos:server_rack`, `signalos:network_relay`, and `signalos:data_drive` for computer-network gameplay.
- A desktop shell with an app launcher, status bar, active app view, notifications, settings surface, and shared visual tokens.
- Built-in apps: Home, Files, Notes, Logs, Network Monitor, Settings, Data Vault, Echo Link, Missions, Archives, Rewards, and Diagnostics.
- Editable operator notes with selected-note editing, title/body drafts, Save, New, Delete, and Clear actions. Notes are stored on the terminal/workstation boot drive and are capped at 64 drive records, 80-character titles, and 2000-character bodies.
- A server-rack screen opened by empty-hand right-click, with four drive bays, player inventory transfer, selected drive details, drive records, network records, drive templates, rename, clear, copy, remove, and apply-template actions.
- Server-owned network discovery around owned terminals/workstations, including linked racks, relays, drives, and data records.
- Drive-backed OS settings, operator notes, file records, and session hints, plus server-owned archive read state, mission claimed state, and pending terminal reward counts.
- Java registration APIs and datapack JSON loading for apps, custom record views, data records, drive templates, chapters, missions, and archives.
- Client-only Java app renderers keyed by app `type`, with render, click, key, character input, and terminal-action helper hooks.
- Optional Echo Core integration through `EchoCoreServices` for module reports, profile summaries, hazards, diagnostics, route records, discovery records, faction records, ThemeCore/SoundCore status, and platform summaries.
- Built-in SignalOS onboarding content for booting the shell, writing notes, bringing rack storage online, applying templates, copying network records, and checking Echo Link.
- Public server app-action helpers, structured action results, provider health metadata, Drive API V2 filesystem helpers, and drive-template lookup APIs for addons that need interactive custom app surfaces.
- A soft KubeJS-friendly bridge through `Java.loadClass`, without a hard KubeJS runtime dependency.

Current limitations:

- SignalOS uses one active app at a time, not draggable multi-window management.
- Notes, settings, and drive records are intentionally text-oriented and bounded for persistence safety.
- Player-facing rack actions cap data drives at 64 records.
- Computer block and item resources use the shared GPT-image-2 texture replacement manifest with production-style block geometry polish.

## Java API

```java
SignalOsApi.registerApp(SignalOsApp.builder("example:field_files")
        .title("Field Files")
        .type("files")
        .summary("Browse recovered field records.")
        .order(25)
        .accentColor(0x66E8FF)
        .build());

SignalOsApi.registerDataProvider(new SignalOsDataProvider() {
    @Override
    public Identifier id() {
        return SignalOsApi.id("example:cache_records");
    }

    @Override
    public List<SignalOsDataRecord> records(Player player) {
        return List.of(SignalOsDataRecord.of(
                "example:records/cache_note",
                "Cache Note",
                "record",
                "Example Module",
                "A server-synced record visible in Files and Data Vault.",
                10));
    }
});

SignalOsApi.registerComputerPeripheral(new SignalOsPeripheralProvider() {
    @Override
    public Identifier id() {
        return SignalOsApi.id("example:beacons");
    }

    @Override
    public List<SignalOsPeripheralProvider.Peripheral> peripherals(Player player) {
        return List.of(new SignalOsPeripheralProvider.Peripheral(
                SignalOsApi.id("example:peripherals/beacon"),
                "relay",
                "Beacon Peripheral",
                "ONLINE",
                player.blockPosition(),
                1));
    }

    @Override
    public SignalOsProviderStatus providerStatus(Player player) {
        return new SignalOsProviderStatus(id(), "Beacon Provider", "ONLINE",
                TerminalDiagnosticProvider.Severity.INFO, "1 beacon");
    }
});
```

Custom record-view apps can be registered without a renderer:

```java
SignalOsApi.registerApp(SignalOsApp.builder("example:field_records")
        .title("Field Records")
        .type("field_records")
        .summary("Focused field cache records.")
        .view("records")
        .recordTypes(List.of("record", "diagnostic"))
        .recordSources(List.of("Example Module"))
        .includeArchived(false)
        .emptyText("NO FIELD RECORDS")
        .order(40)
        .build());
```

Client code can provide a richer renderer for a custom type:

```java
SignalOsAppRenderers.register("field_records", new SignalOsAppRenderer() {
    @Override
    public void render(SignalOsAppRenderContext context, GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float partialTick) {
        graphics.drawString(context.minecraft().font, "Custom SignalOS surface",
                context.x(), context.y(), 0xFF66E8FF, false);
    }

    @Override
    public boolean mouseClicked(SignalOsAppRenderContext context, double mouseX, double mouseY, int button) {
        context.sendAction(SignalOsApi.id("field_records"), SignalOsApi.id("refresh"), "clicked");
        return true;
    }
});
```

Server-side app actions can receive the active SignalOS network context:

```java
SignalOsApi.registerAppAction(
        SignalOsApi.id("example:field_records"),
        SignalOsApi.id("example:refresh"),
        (context, payload) -> {
            ServerPlayer player = context.player();
            String networkId = context.networkId();
            int accessTier = context.accessTier();
            boolean hasDrive = context.activeDrivePresent();
            String driveLabel = context.activeDriveLabel();
            boolean writableDrive = context.activeDriveWritable();
        });
```

Write-aware addons can return structured results and use the Drive API V2 filesystem facade:

```java
SignalOsApi.registerAppActionResult(
        SignalOsApi.id("example:field_records"),
        SignalOsApi.id("example:save"),
        (context, payload) -> context.requireWritableDrive().success()
                ? SignalOsActionResult.fromDriveResult(SignalOsApi.updateActiveDriveFileSystem(
                        context.player(), fs -> fs.createFile("/files/example.txt", "Example", payload, "text/plain")))
                : context.requireWritableDrive());
```

Addons can inspect loaded V2 drive templates and provider health:

```java
SignalOsDriveData template = SignalOsApi.driveTemplate(SignalOsApi.id("signalos:diagnostics_drive"));
List<SignalOsProviderStatus> statuses = SignalOsApi.providerStatuses(player);
```

Legacy terminal content remains supported:

```java
SignalOsApi.registerChapter(TerminalChapter.builder("example:field_ops")
        .title("Field Ops")
        .section("progress")
        .page("missions")
        .page("archives")
        .build());

SignalOsApi.registerMission(TerminalMission.builder("example:secure_cache")
        .chapter("example:field_ops")
        .title("Secure the Cache")
        .description("Find shelter and recover the field cache.")
        .objective("Find shelter")
        .completionAdvancement("minecraft:story/root")
        .reward("minecraft:bread", 4)
        .build());
```

## JSON Content

Datapacks can place content in:

- `data/<namespace>/signalos/apps/*.json`
- `data/<namespace>/signalos/data_records/*.json`
- `data/<namespace>/signalos/drive_templates/*.json`
- `data/<namespace>/signalos/chapters/*.json`
- `data/<namespace>/signalos/missions/*.json`
- `data/<namespace>/signalos/archives/*.json`

KubeJS packs can put the same files under the KubeJS `data/` folder. See [DATA_FORMATS.md](DATA_FORMATS.md) for field-level examples.

SignalOS intentionally ships a `kubejs.classfilter.txt` soft bridge instead of `kubejs.plugins.txt`; KubeJS' addon guidance reserves `kubejs.plugins.txt` for compile-time KubeJS plugin classes.

## Computer Gameplay

- Terminals and workstations anchor an operator network when owned by the player.
- Right-click a terminal or workstation with a SignalOS Data Drive to install it as the focused OS boot drive; sneak empty-hand right-click ejects it.
- Empty-hand right-click opens the server-rack screen; right-clicking with a data drive inserts it; sneak empty-hand right-click ejects the last installed drive.
- Server racks store up to four installed data drives and expose their records to Files, Logs, Data Vault, and Echo Link views.
- Rack actions validate the open menu, rack position, selected slot, held drive component, network snapshot records, and loaded drive templates before changing drive data.
- The rack screen can copy a selected network record to a drive, remove a drive record, apply a drive template, clear drive records, and rename the drive label.
- Network relays increase the discovered network footprint and report as first-class network peripherals.
- Data drives carry portable `SignalOsDriveData` components. New blank drives initialize as schema V2; legacy V1 components still decode but are rejected by terminal/rack slots and write APIs.
- Drive API V2 stores files, folders, notes, imported records, settings, and session hints on the active terminal boot drive. Filesystem paths are absolute `/` paths stored in record metadata under `signalos.path`.
- Player persistent data remains reserved for progression-style state such as mission claims, archive read state, and terminal reward inbox counts.
- Provider status metadata appears in Diagnostics and can be queried through `SignalOsApi.providerStatuses(player)`.

Network identity is server-owned and derived from dimension, anchor position, and owner. SignalOS works without rich Echo addons, then surfaces more records when Echo Core providers return module, profile, hazard, diagnostic, route, discovery, faction, theme, or sound data.

## KubeJS Example

For reloadable pack content, prefer JSON in the KubeJS `data/` folder. Use the soft bridge when a script needs to assemble content procedurally.

```js
const SignalOSEvents = Java.loadClass('com.knoxhack.signalos.kubejs.SignalOSEvents')

ServerEvents.loaded(event => {
  SignalOSEvents.content(event => {
    event.clear()
    event.chapter('signalosexample:field_ops')
      .title('Field Ops')
      .section('progress')
      .page('missions')
      .page('archives')
      .register()

    event.mission('signalosexample:secure_cache')
      .chapter('signalosexample:field_ops')
      .title('Secure the Cache')
      .description('Find shelter and recover the field cache.')
      .objective('Find shelter')
      .completionAdvancement('minecraft:story/root')
      .reward('minecraft:bread', 4)
      .register()
  })
})
```

## Build And Release Checks

From the workspace root:

```powershell
.\gradlew.bat :echosignalos:build --warning-mode all
.\gradlew.bat :signalosexample:build --warning-mode all
.\gradlew.bat :echosignalos:runGameTestServer --warning-mode all
python tools\validate_resources.py --addon-set beta
```

Release safety notes:

- Keep `echoterminal` imports and content separate; SignalOS integrates through Echo Core and shared service contracts.
- Keep Echo integration optional and defensive.
- Terminal app rendering resolves in order: built-in app type, registered Java renderer, JSON `view: "records"`, then unsupported metadata view.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/signalos.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/signalos.md`.

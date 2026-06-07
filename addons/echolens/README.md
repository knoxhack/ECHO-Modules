<!-- CURSEFORGE_README_START -->
# Lens by ECHO Labs

![Lens by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echolens/brand-sheet.png)

**Modern inspection HUD with compact scans, expanded local details, server-verified Deep Scan, privacy rules, and provider APIs.**

![Lens by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echolens/features-portrait.png)

![Lens by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echolens/features-landscape.png)

## CurseForge Summary

Modern inspection HUD with compact scans, expanded local details, server-verified Deep Scan, privacy rules, and provider APIs.

## Main Features

- Field scanner profiles.
- Target analysis reticles.
- Contextual discovery scans.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echolens/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echolens/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echolens/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: Lens

ECHO: Lens is a hybrid framework/UI addon for the ECHO stack. It provides a modern inspection HUD for players and a structured provider API for addons that want to contribute scanner data.

Lens intentionally registers no gameplay items, blocks, block entities, entities, menus, recipes, loot tables, or tags. The player loop is simple: install the addon, look at a block, fluid, or entity, read the compact overlay, hold Shift for expanded local details, and hold the Deep Scan key for categorized diagnostics with server-verified public rows.

## Player Controls

- Look at a block, fluid, or entity to show the compact HUD.
- Hold Shift to show expanded block/entity/fluid stats.
- Hold Left Alt by default for Deep Scan. This key is configurable and requests public server-verified rows through NetCore.
- Press `R`, `U`, or `T` while looking at a block item target to open ECHO: Index recipes, uses, or tracking when ECHO: Index is installed.

## Privacy

Lens is public-first by default. Compact and expanded scans stay client-local. Deep Scan may send a small NetCore request to the server for public verified facts, but it never requests inventory contents. The built-in inventory provider only reports safe public state according to the common `inventory_access_policy` config.

## Configuration

NeoForge config owns persistence for Lens settings. ECHO Core also receives Lens config metadata through `EchoConfigRegistry` under module id `echolens`.

Important client settings include:

- HUD position, offsets, scale, opacity, animation, reduced motion, and max scan distance.
- Theme selection: ECHO Dark, Clean Minimal, Vanilla Compact, and Ashfall Hazard.
- Visible data categories and row limits for compact, expanded, and deep scans.
- Server Deep Scan timeout, cache duration, and status badge visibility.

Important common settings include:

- Inventory access policy.
- Machine status visibility.
- Beginner hints.
- Debug command availability.
- Server Deep Scan enablement, distance, rate limit, and protected-target redaction.

## Provider API

Addons register structured providers through `LensProviderRegistry`. Providers return `LensInfoSection` values with typed categories, tones, visibility, rows, and optional actions.

```java
LensProviderRegistry.register(new BlockLensProvider() {
    @Override
    public Identifier id() {
        return Identifier.fromNamespaceAndPath("examplemod", "machine_status");
    }

    @Override
    public int priority() {
        return 250;
    }

    @Override
    public List<LensInfoSection> inspectBlock(LensContext context, BlockState state, BlockPos pos) {
        return List.of(LensInfoSection.of(
                Identifier.fromNamespaceAndPath("examplemod", "section/machine_status"),
                LensDataCategory.MACHINE,
                "Machine",
                "#",
                LensTone.INFO,
                LensVisibility.EXPANDED,
                List.of(LensInfoRow.of("Power", "Stable", "P", LensTone.GOOD, LensVisibility.EXPANDED))));
    }
});
```

For batch registration, use `LensProviderRegistry.registerAll(...)`. The registry rejects duplicate provider ids, sorts by priority, isolates provider exceptions during scans, and exposes immutable diagnostics for commands and Terminal integration. Providers that are safe to run during server-assisted Deep Scan can also implement `ServerLensProvider`.

Other addons can safely call `EchoServiceRegistry.find(ILensInspectionService.class).orElse(ILensInspectionService.NOOP)` when Lens may be absent.

## Optional Integrations

- ECHO Core and ECHO NetCore are required. Core receives the Lens service, addon chapter, route record, diagnostics, and config metadata; NetCore carries rate-limited Deep Scan requests and responses.
- ECHO: Terminal is optional and receives Lens addon info through reflection when present.
- ECHO: Index is optional and powers recipe, uses, and track shortcuts when present.
- ECHO: RenderCore remains optional; Lens 1.0.0 uses a 2D client HUD and does not require RenderCore.

## Commands

The `/echolens` command requires gamemaster permission.

- `/echolens status` reports provider count, server provider count, packet registration, and optional addon availability.
- `/echolens providers` lists provider diagnostics and whether each provider can run on the server scan path.
- `/echolens validate` checks registry, server scan, and privacy basics when debug commands are enabled.

## Validation

Recommended release checks:

```powershell
$env:JAVA_HOME='C:\Github\Echo\.local\jdk25'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew :echonetcore:compileJava
.\gradlew :echolens:compileJava
.\gradlew :echolens:build
.\gradlew :echolens:runGameTestServer
.\gradlew validateEchoResources
.\gradlew buildEchoWorkspace
```

Manual checks:

1. Look at stone with the wrong and correct tools.
2. Look at water, lava, powered redstone blocks, a chest, a zombie, a passive mob, and a tame wolf.
3. Verify compact, Shift-expanded, and Deep Scan modes.
4. Confirm Deep Scan shows Querying and then Verified, Redacted, Unavailable, or Stale.
5. Test HUD positions, GUI scales, opacity, reduced motion, and category toggles.
6. Install ECHO: Index and test Recipes, Uses, and Track shortcuts.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echolens.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echolens.md`.

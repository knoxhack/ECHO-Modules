<!-- CURSEFORGE_README_START -->
# Terminal by ECHO Labs

![Terminal by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoterminal/brand-sheet.png)

**Shared ECHO terminal block, UI shell, mission browser, archive surface, recipe index, action routing, and addon integration hub.**

![Terminal by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoterminal/features-portrait.png)

![Terminal by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoterminal/features-landscape.png)

## CurseForge Summary

Shared ECHO terminal block, UI shell, mission browser, archive surface, recipe index, action routing, and addon integration hub.

## Main Features

- Terminal block, menu, client screen, and configurable key binding.
- Command, Progress, Intel, and System shell with navigation profiles.
- Mission browser with filters, search, detail panes, keyboard navigation, reward claims, and server-authoritative actions.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoterminal/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoterminal/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoterminal/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: Terminal

ECHO: Terminal is the shared command surface for the Echo stack. It provides the terminal block, menu, client screen, mission provider API, action routing, navigation sections, archive records, rewards inbox, diagnostics, route records, faction atlas, vitals telemetry, and addon chapter hub.

## Release Feature Set

- Four-section command shell: Command, Progress, Intel, and System.
- Command Deck summary with mode, chapter, route, blocker, and reward counts.
- Inline Command Deck diagnostics for the next actionable blocker or release condition.
- Survival Route tab that aggregates vanilla progression and installed ECHO chapter missions into one guided route.
- Mission browser with visual/guided/minimal views, filters, search, scrollable detail panes, keyboard navigation, and claim/action routing.
- Provider-backed transient mission HUD notices for newly available missions, ready objectives, reward caches, claimed caches, and newly online phases.
- Recipe Index with searchable ECHO items, Recipes/Uses modes, provider category filters, item detail panes, machine/input/catalyst/output slots, process time, notes, and locked schematic hints.
- Baseline vanilla journey rewards route support caches through the Reward Inbox on an owned terminal.
- Shared Reward Inbox for support caches exposed through Echo Core.
- Addon Chapters hub, Route Records, Faction Atlas, Vitals, Mission Graph, and Field Archive tabs.
- Configurable client key binding for opening the terminal.

## Controls

- Open terminal: `M` by default, configurable in Minecraft Controls under `ECHO: Terminal`.
- Close terminal: `Esc` or the configured terminal key.
- Navigate terminal sections/pages: arrow keys or WASD.
- Mission browser: type to search, `Ctrl+F` focuses search, left/right cycles filters, up/down changes selected mission, `Enter` or `Space` activates the first enabled mission command.
- Recipe Index: type to search, click an ECHO item, switch Recipes/Uses, choose category chips, right-click an item slot to inspect uses, and scroll item/result/detail panes independently.
- Mission HUD notices: enabled by default and toggleable from Interface Settings as `MISSION HUD`.
- Scroll long content with mouse wheel or Page Up/Page Down.

## Addon Integration

Addons should register `TerminalMissionProvider` implementations for mission records and use `TerminalMissionActions.registerForTab` when a tab needs server-authoritative mission commands. Addons can also expose archive entries, navigation profiles, chapter metadata, route records, faction profiles, diagnostics, hazard telemetry, and pending rewards through Echo Core services.

Registered mission providers also feed the client mission HUD notification controller. Providers do not need a separate packet or notification API: the controller polls synced provider snapshots, skips the aggregate Survival Route provider to avoid duplicates, and shows transient cards for status transitions while leaving persistent objective pinning to chapter HUDs.

Explicit `TerminalNavigationProfile` registration is the public routing contract. Use `TerminalNavigationProfiles.register` to place tabs in Command, Progress, Intel, or System and to group chapter-owned pages. `TerminalTabChrome` group fallbacks remain for compatibility only.

Recipe-aware addons should register `TerminalRecipeProvider` implementations with `TerminalRecipeRegistry`. Providers publish `TerminalRecipeCategory` and `TerminalRecipeEntry` data built from `TerminalRecipeSlot`, `TerminalRecipeNote`, and `TerminalRecipeSnapshot` semantics so the Recipe Index can search outputs, uses, catalysts, machines, info-only entries, and locked notes.

For release stability, mission and recipe providers should tolerate missing players, return empty lists instead of throwing when content is unavailable, keep action handlers authoritative on the server, and leave recipe authority with the owning addon.

## ScreenCore Shell

Terminal includes a ScreenCore-backed rendering layer for the existing TerminalTab app. It keeps `TerminalTabRegistry`, `TerminalNavigationProfiles`, `TerminalMissionProvider`, addon tabs, and the current Java renderer intact.

When ScreenCore is installed, Terminal can use the ScreenCore bridge for built-in tabs, but the legacy renderer remains the default unless `terminal.screenCoreExperimentalTabs=true` is set in client config. This keeps the classic fallback safe for players while testers can opt into the experimental shell. See [docs/SCREENCORE_TERMINAL_MIGRATION.md](docs/SCREENCORE_TERMINAL_MIGRATION.md) for the current tab map, fallback rules, data provider/action strategy, hot reload notes, and addon guidance.

## Terminal Themes

Echo Terminal ships a Java-only theme engine for the release stack. Themes are registered through `TerminalThemeRegistry` and are client presentation only: they do not change packets, world data, mission authority, or reward behavior.

Built-in themes:

- `echoterminal:echo_console` keeps the current cyan ECHO terminal style as a tokenized default.
- `echoterminal:nexus_modpack` adds an ECHO/Nexus/modded-Minecraft questbook style with chapter overlays, HD panels, banners, borders, and semantic icons.

Themes are selected from Command Deck through the compact `Theme` command or the `Appearance` selector. The selected theme persists in the local Minecraft config file `config/echoterminal-client.properties`.

Theme authors should provide:

- `TerminalThemeTokens` for colors, typography, panels, borders, prompts, output text, states, dividers, effects, and core assets.
- `TerminalIconSet` entries for semantic keys such as navigation groups, pages, actions, status states, rewards, blockers, mission categories, chapters, settings, locked, empty, and unknown.
- `TerminalChapterStyle` overrides for chapter namespaces when a theme needs biome, dimension, magic, tech, archive, or hazard flavor.

Register addon themes during client setup before any terminal screen opens. Theme ids must be lowercase resource identifiers; duplicate ids are rejected, missing selected themes fall back to `echoterminal:echo_console`, and missing icon keys fall back through the theme icon set before using ECHO's built-in vector glyphs.

Example:

```java
TerminalThemeRegistry.register(TerminalTheme.builder(id("my_theme"), "My Theme")
        .tokens(myTokens)
        .icons(TerminalIconSet.builder()
                .icon(TerminalIconKey.action("claim"), id("textures/gui/theme/icons/action_claim.png"))
                .icon(TerminalIconKey.chapter("myaddon"), id("textures/gui/theme/icons/chapter_myaddon.png"))
                .build())
        .chapterStyle(TerminalChapterStyle.builder("myaddon", "My Chapter")
                .colors(0xFFB6F06C, 0xFF77DDEE)
                .banner(id("textures/gui/theme/banners/my_chapter.png"))
                .build())
        .build());
```

Generated or hand-authored assets should avoid baked-in text. Labels are rendered by code for localization, readability, scaling, and accessibility.

## Release Notes And Limits

- Mission HUD notices are transient status cards, not a persistent objective pin; chapter HUDs may still render their own trackers.
- Most chapter-specific behavior is provided by the owning addon. If an addon is missing or disabled, the terminal shows only the providers that are registered.
- Visual assets can be reduced through `TerminalClientOptions`, but there is not yet an in-game options screen for every terminal presentation setting beyond theme selection.
- `TerminalBadgeProvider` and a generalized `TerminalNotificationProvider` remain deferred APIs; mission HUD notices are currently wired through `TerminalMissionProvider` snapshots.
- Baseline cache claims require a placed or recently opened owned terminal so the Reward Inbox has a storage target.
- Save compatibility is expected for normal release use, but fresh-world smoke testing is recommended before public release candidates.

## Release Checklist

- Run `.\gradlew.bat :echoterminal:runGameTestServer --warning-mode all`.
- Run the workspace release verification after packaging paths are aligned.
- Smoke test block opening, keybind opening, every tab, narrow UI widths, mission search/filter, Recipe Index search/category/Recipes/Uses flows, vanilla reward claim, no-addon install, and full-addon install.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echoterminal.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echoterminal.md`.

# ECHO Terminal ScreenCore Migration

This document tracks the ScreenCore-backed Terminal migration layer. The goal is to keep ECHO Terminal as the same extensible `TerminalTab` application players already know, while letting migrated tabs render through ScreenCore pages, reusable components, data providers, actions, and hot reload.

The ScreenCore bridge is available when `echoscreencore` is installed. Cyberglass now opens through the ScreenCore shell by default when `useCyberglassScreenCoreTheme=true`; classic and parity-sensitive layouts still keep the Java renderer fallback available.

## Current TerminalTab Map

Built-in tabs are registered by `BuiltinTerminalTabs.register()` during client setup from `EchoTerminalClient`. Addons can still register additional tabs through `TerminalTabRegistry`.

| Order | Tab ID | Page | Purpose |
| ---: | --- | --- | --- |
| 0 | `echoterminal:overview` | Command Deck | Main dashboard, next action, inline diagnostics, quick links, theme selection |
| 50 | `VanillaJourneyProvider.TAB_ID` | Baseline | Vanilla advancement route through the mission browser |
| 120 | `echoterminal:mission_graph` | Route Sources | Registered mission provider summary, excluding the main survival aggregate |
| 125 | `echoterminal:route_records` | Route Records | Shared route records from Echo Core services |
| 126 | `echoterminal:discovery_grid` | Discovery Grid | Spoiler-safe discovery browser |
| 128 | `echoterminal:faction_atlas` | Factions | Faction profile, standing, contracts, services, route and POI context |
| 130 | `echoterminal:vitals` | Vitals | Hazard telemetry and warning status |
| 140 | `echoterminal:reward_inbox` | Reward Inbox | Pending rewards and claim-all routing |
| 145 | `echoterminal:recipe_index` | Recipe Index | Provider-backed recipe/search/uses/source browser |
| 145 | `echoterminal:data_core` | Data Core | Data service diagnostics and debug visibility |
| 150 | `echoterminal:addons` | Mods | Installed chapter hub and inferred links |
| 170 | `MainSurvivalQuestProvider.TAB_ID` | Survival Route | Main survival mission browser |
| 175 | `echoterminal:settings` | Interface Settings | Client-only Terminal options |
| 950 | `echoterminal:archives` | Field Archive | Shared dossier/archive records |

The table is intentionally based on the current code, not on a redesigned information architecture.

## Migration Strategy

The ScreenCore bridge lives under `com.knoxhack.echoterminal.client.screencore`:

- `TerminalScreenCoreBridge` registers the bridge only when `echoscreencore` is loaded.
- `TerminalScreenCoreScreen` wraps `EchoScreenEngine` in the existing `EchoTerminalMenu` container screen contract.
- `TerminalScreenCoreDataProviders` maps existing Terminal services and registries into ScreenCore view data.
- `TerminalScreenCoreActions` routes supported ScreenCore actions back into existing Terminal options, packets, tabs, and services.
- Unknown or external tabs keep using the existing TerminalTab fallback unless they later expose ScreenCore metadata.

The bridge registers as an `EchoTerminalScreens` primary provider. When ScreenCore is disabled, unavailable, classic layout fallback is requested, or the ScreenCore shell throws during construction, the current `EchoTerminalScreen` remains the renderer.

## Config Flags

The client properties file persists these flags:

- `terminal.useScreenCore`: allows the bridge when ScreenCore is present.
- `terminal.screenCoreMatchExistingLayout`: keeps the migration constrained to the current layout identity.
- `terminal.useCyberglassScreenCoreTheme`: opens cyberglass through the ScreenCore shell when ScreenCore is available.
- `terminal.screenCoreDebug`: enables ScreenCore debug hints.
- `terminal.screenCoreExperimentalTabs`: compatibility key for the ScreenCore shell toggle.

Default behavior keeps classic layouts conservative, but cyberglass uses the ScreenCore shell when the cyberglass ScreenCore flag is enabled. Existing configs can still force the legacy renderer through the classic layout fallback.

## ScreenCore Pages

EUI pages are registered from `assets/echoterminal/eui/eui_manifest.json`.

Current page resources:

- `terminal_overview.eui.xml`
- `terminal_mission_graph.eui.xml`
- `terminal_mission_browser.eui.xml`
- `terminal_addons.eui.xml`
- `terminal_recipe_index.eui.xml`
- `terminal_route_records.eui.xml`
- `terminal_discovery_grid.eui.xml`
- `terminal_faction_atlas.eui.xml`
- `terminal_archives.eui.xml`
- `terminal_vitals.eui.xml`
- `terminal_reward_inbox.eui.xml`
- `terminal_data_core.eui.xml`
- `terminal_settings.eui.xml`
- `terminal_fallback.eui.xml`

All migrated pages share `terminal_shell.eui.xml` and Terminal-specific components/styles. The pages should continue to mirror the old layout zones: header, navigation, main content, detail/inspector, and footer/status controls where those exist in the current tab.

## Enabled And Fallback State

ScreenCore resources exist for every built-in tab. A tab can be in one of four renderer states:

- `experimental`: compatibility/internal state for a built-in ScreenCore page with the shell enabled.
- `fallback-default`: a built-in ScreenCore page exists, but the shell toggle is disabled.
- `external-fallback`: an addon tab has no ScreenCore page mapping yet.
- `fallback`: missing or invalid tab state.

The UI presents these as ScreenCore or Legacy Ready rather than migration jargon.

## Data Providers

The bridge registers one provider namespace, `terminal`, with nested paths such as:

- `terminal.activeTab`
- `terminal.navigation.items`
- `terminal.overview.bestNextAction`
- `terminal.overview.blockerCards`
- `terminal.missionGraph.providers`
- `terminal.missionBrowser.visibleMissions`
- `terminal.addons.visibleChapters`
- `terminal.recipeIndex.visibleItems`
- `terminal.discoveryGrid.visibleCards`
- `terminal.archives.visibleRecords`
- `terminal.settings.options`

Providers read from existing Terminal and Echo Core sources. They return empty lists or unavailable rows when optional services are absent, and they avoid revealing locked discovery/archive body text.

## Actions

ScreenCore actions must call existing behavior. Currently wired actions include:

- `terminal.open_tab` and related tab-open actions
- `terminal.theme_changed`
- `terminal.rewardInbox.claim_all`
- `terminal.archives.mark_read`
- ScreenCore and debug setting toggles
- `terminal.close`

Visible ScreenCore controls either update client UI state, forward to existing terminal packets/actions, open real pages, or expose Legacy Controls for deeper Java-only behavior.

## Extensibility

The migration preserves public Terminal extension points:

- `TerminalTabRegistry` remains the tab source of truth.
- `TerminalNavigationProfiles` still determines grouping and placement.
- `TerminalMissionProvider` pages keep using the mission provider registry and the mission browser model.
- The Mods page still reads chapter metadata from Echo Core and inferred links from registered tabs/providers.

Addon tabs appear in navigation in the same order/grouping as before. If a future addon wants native ScreenCore rendering, it should expose a page mapping or bridge metadata; otherwise the old `ClientTerminalTab` renderer remains valid.

External tabs can opt into ScreenCore by implementing `TerminalScreenCorePageMetadata` and returning a ScreenCore page id. The bridge still honors the global experimental gate, and tabs without that metadata use the fallback page.

## Adding A ScreenCore Terminal Page

1. Register the existing `TerminalTab` normally.
2. Add or expose a ScreenCore page ID for that tab, typically by implementing `TerminalScreenCorePageMetadata`.
3. Add data provider paths under `terminal.<pageName>`.
4. Wire actions to existing Terminal services or packets.
5. Keep old rendering available until the ScreenCore page matches current controls, empty/locked states, and actions.
6. Add the page/components/styles to `eui_manifest.json`.
7. Test with ScreenCore off, ScreenCore on, and `screenCoreExperimentalTabs` on.

## Hot Reload

ScreenCore reload commands are expected to work for the EUI resources:

```text
/echoscreencore reload
/echoscreencore reload styles
/echoscreencore reload page terminal_overview
/echoscreencore list pages
/echoscreencore list styles
/echoscreencore list components
```

Use `terminal.screenCoreDebug=true` when checking page IDs, provider paths, and action routing.

## Known Limits

- The Recipe Index and Mission Browser are ScreenCore-first visually, but the legacy renderer remains available for full keyboard/action parity.
- A small number of non-visible future action IDs remain reserved for compatibility.
- ScreenCore pages should not be promoted to default if they expose less data or fewer controls than the Java tabs.

## Verification Checklist

Before promoting any tab to default ScreenCore rendering:

- Confirm the tab ID, order, navigation placement, and title match the current Terminal.
- Compare layout zones against the Java tab.
- Confirm every visible button has a real action.
- Confirm empty, locked, unavailable, and error states match or improve the old behavior.
- Confirm locked discovery/archive data stays spoiler-safe.
- Confirm ScreenCore disabled fallback opens the old renderer.
- Confirm addon-provided tabs and mission-provider pages still appear.
- Run `:echoterminal:build`, `:echoscreencore:build`, and full workspace verification before release.

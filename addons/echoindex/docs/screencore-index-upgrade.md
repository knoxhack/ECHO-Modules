# ECHO Index ScreenCore Upgrade

## Overview

Index now has a ScreenCore-powered database UI layered on top of the existing Index services. The legacy `IndexCatalogScreen`, recipe screen, and inventory overlay remain available as fallback surfaces. The ScreenCore path is enabled by `ui.use_screencore` and is registered only on the client when `echoscreencore` is present.

The bridge entrypoint is `IndexScreenCoreBridge`. It registers:

- `IndexDataProviders` for provider key `index`
- `IndexActions` for `index.*` action IDs
- ScreenCore style sheets under `assets/echoindex/eui/styles`
- EUI pages/components from `assets/echoindex/eui`

## Data Providers

The `index` provider exposes stable binding paths for ScreenCore pages:

- `index.dashboard`
- `index.nav.sections`
- `index.items.all`, `index.items.visible`, `index.items.groupedByMod`, `index.items.selected`
- `index.mods.all`, `index.mods.visible`, `index.mod.sections`
- `index.recipes.all`, `index.recipes.visible`, `index.recipe.selected`, `index.recipe.inputs`, `index.recipe.outputs`, `index.recipe.machine`
- `index.usages.visible`, `index.usage.selected`, `index.usage.categories`
- `index.machines.all`, `index.machines.visible`, `index.machine.selected`, `index.machine.recipeTypes`, `index.machine.recipes`
- `index.favorites.items`, `index.favorites.recipes`, `index.favorites.machines`
- `index.history.recent`
- `index.filters.mods`, `index.filters.categories`, `index.filters.recipeTypes`, `index.filters.machines`, `index.filters.sortOptions`
- `index.search.query`, `index.search.results`, `index.search.suggestions`
- `index.settings`
- `index.debug.stats`, `index.debug.warnings`

Providers return safe empty lists/maps when no player, selection, or recipe data is available.

## Cache

`IndexRecipeCache` adapts the existing `IndexService` item catalog and `IndexRecipeSnapshot` into ScreenCore-ready item, recipe, machine, and mod rows. It rebuilds when the item catalog revision, recipe snapshot generation, client sync revision, local favorites revision, or history revision changes.

The cache avoids scanning registries from EUI render paths. Search/filter operations work over cached row records and are capped by `ui.max_rendered_items`.

## Actions

`IndexActions` validates action values, updates `IndexUiState`, refreshes ScreenCore data, and mirrors legacy server-backed actions where appropriate:

- item favorites send `BOOKMARK` / `UNBOOKMARK`
- recipe bookmarks send `PIN_RECIPE` / `UNPIN_RECIPE`
- local favorites/history are persisted per player profile when possible

Every visible Index button is wired to a registered action or a ScreenCore built-in action.

## Recipe Type Rendering

The ScreenCore UI currently renders every indexed recipe through a generic provider-backed recipe flow:

- input slots
- output slots
- machine/type summary
- processing time
- notes
- debug layout metadata when `debug.recipe_parsing` is enabled

To add a richer recipe type renderer, extend `IndexRecipeCache.IndexRecipeData` with the layout fields needed by the EUI component, then add or update an EUI component under `assets/echoindex/eui/components`.

Unknown recipe types remain visible through the generic card and do not crash the UI.

## Machine Cards

Machine cards are derived from recipe machine stacks when available. If a recipe has no explicit machine item, Index uses the real indexed recipe category as a virtual machine/type bucket. This keeps vanilla crafting, smelting, and custom provider recipes browsable without inventing fake recipe data.

## EUI Pages And Hot Reload

Pages live under `assets/echoindex/eui/pages` and are listed in `eui_manifest.json`.

Useful commands:

- `/echoscreencore reload`
- `/echoscreencore reload styles`
- `/echoscreencore reload page echoindex:index_items`
- `/echoscreencore list pages`
- `/echoscreencore list styles`
- `/echoscreencore list components`

## Config

Client UI config:

- `ui.use_screencore`
- `ui.group_by_mod`
- `ui.compact_grid`
- `ui.remember_last_page`
- `ui.show_locked_items`
- `ui.max_rendered_items`

Debug config:

- `debug.screencore`
- `debug.providers`
- `debug.actions`
- `debug.recipe_parsing`
- `debug.show_recipe_ids`

## Limitations And Roadmap

This pass establishes the production ScreenCore bridge, cache, providers, actions, pages, and fallback behavior. Future targeted passes can deepen specialized recipe layouts, progression-specific locked-state reasons, fluid/energy extraction for third-party machines, and richer per-recipe transfer helpers.

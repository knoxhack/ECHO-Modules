# Unified ECHO Native Player Runtime

ECHO-Modules is the owner of player-facing feature definitions. Runtimes adapt module contracts; they do not invent their own player routes.

Ashfall is a fixture. Module contracts, surface manifests, content graph nodes, export plans, and validation gates must remain pack-neutral.

## Module Ownership

- `echoscreencore` owns title, pause, world creation/load, settings, module diagnostics, blocker screens, death/respawn, screen stack, modal stack, focus, data binding, and action dispatch contracts.
- `echothemecore` owns theme tokens, density, contrast, readable mode, and visual skin ids.
- `echohudcore` owns HUD widgets, overlay priority, safe areas, meters, prompts, objective tracker, and notification queue contracts.
- `echoinputcore` owns input contexts, keybind registry, radial menus, remap metadata, conflict diagnostics, and controller prompt metadata.
- `echoindex` owns index pages, item/block references, recipes, inventory overlay hooks, and recipe navigation.
- `echoterminal` owns terminal shell, tabs, command deck, route planning, diagnostics pages, and safe terminal actions.
- `echomissioncore` owns objective state, mission routes, rewards, and active tracker data.
- `echosessioncore` owns onboarding state, active objective, route history, hazard state, pack phase, and save/session-facing state.
- `echoadaptercore` owns gameplay actions, mutation receipts, and mutation ledger contracts.
- `echoplaytestcore` owns scenario definitions and release evidence.

## Content Graph Rule

Every player-facing module resource needs graph coverage. EUI manifests and RenderCore screen profiles produce `echo:ui_intent` nodes. Generated graphs must include runtime adaptation evidence for:

- `neoforge`
- `echo_native`
- `echo_runtime_standalone`
- `standalone_engine`

Use player intent values such as `title_menu`, `pause_menu`, `world_create`, `world_load`, `settings_panel`, `module_diagnostics`, `inventory_surface`, `crafting_surface`, `hotbar_surface`, `hud_widget`, `overlay`, `keybind_action`, `terminal_page`, `index_page`, `mission_tracker`, `death_respawn`, `save_warning`, and `runtime_blocker`.

Canonical module-owned player surfaces live at `data/<module>/echo_native/player_surfaces.json` and use `schemaVersion: "echo.native.player_surface_manifest.v1"`. These manifests must name `ownerModule`, `requiredHostServices`, `hostTargets`, and the surfaces/actions/session references the module owns.

## Release Rule

`scripts/generate-content-graph.mjs` and `scripts/validate-content-graph.mjs --strict --sdk-root ../ECHO-SDK` must prove that required player-facing surfaces are module-owned, graph-covered, and export-planned. Runtime-owned one-off screens, HUDs, keybinds, inventory flows, terminal routes, index routes, or gameplay mutations are release blockers until represented by module contracts.

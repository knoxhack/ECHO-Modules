# ECHO: GalacticCore ASDK Porting Plan

## Scope

GalacticCore is being rebuilt as a native-first ASDK addon. The legacy Forge 1.12.2 source remains reference material while production code moves to `com.knoxhack.echogalacticcore`.

## Phase Status

| Phase | Status | Evidence |
| --- | --- | --- |
| Lock identity/legal | Implemented foundation | `LICENSE`, `CREDITS.md`, `README.md`, `docs/LEGAL_NOTES.md` |
| Create ASDK-native addon shell | Implemented foundation | `build.gradle`, `META-INF/echo.mod.json`, `EchoGalacticCoreNativeModule` |
| Build typed service layer | Implemented foundation | `GalacticCoreServices`, `GalacticCoreNativeMutations` |
| Create content registries | Implemented foundation | `content/GalacticCore*.java` registrars |
| Migrate assets/resources | Recipe/blockstate/language/loot/sound/model-texture migration foundation | `assets/echogalacticcore`, JSON language files, ECHO legacy ore-dict tags, native blockstate helper models, native loot/sound ids, singular `textures/block` and `textures/item` roots, `data/echogalacticcore/port` |
| Replace MicCore/transformers | Boundary implemented, gameplay replacement ongoing | old packages excluded from build; ASDK attachments/capabilities added |
| Port gameplay in slices | Executable core-loop foundation; runtime gameplay ongoing | content catalog plus foundation, machine, life-support, energy bridge, player gear, rocket progression, celestial route, celestial environment, transfer placement/execution, HoloMap/ScreenCore surface, rendered menu, and interaction contracts, outer planet progression, dungeon/boss, dungeon structure planning, boss entity spawn intent, boss encounter, boss AI intent, treasure interaction, treasure chest screen/menu, and dungeon reward manifests; `GalacticCoreRuntimeService` |
| Add ECHO integrations | Implemented foundation | `GalacticCoreEchoIntegrations` |
| Add parity/testkit coverage | Implemented foundation | ASDK testkit tests |
| Release gate | Implemented foundation | `verifyNativeBoundary`, migration gates, docs, tests |

## Gameplay Slice Order

1. Materials and base items.
2. Basic blocks and ores.
3. Recipes and schematics.
4. Machines.
5. Energy.
6. Oxygen and atmosphere.
7. Player gear and attachments.
8. Rocket loop.
9. Moon loop.
10. Mars, Asteroids, Venus.
11. Dungeons and bosses foundation.
12. Dungeon room generation, boss AI, treasure unlocks, and schematic rewards.

## Native Boundary

Production code must not import or depend on:

- `echo-native-loader`
- NeoForge
- Forge
- `activateNative(Map)`
- legacy MicCore transformers

All runtime mutations must flow through typed ASDK services and return `EchoNativeMutationReceipt` evidence.

## Gameplay Data Contracts

The native port now publishes data-backed parity manifests for the first playable loops:

- `data/echogalacticcore/port/gameplay_machines.json`
- `data/echogalacticcore/port/gameplay_foundation.json`
- `data/echogalacticcore/port/life_support_contracts.json`
- `data/echogalacticcore/port/energy_bridge.json`
- `data/echogalacticcore/port/player_gear.json`
- `data/echogalacticcore/port/rocket_progression.json`
- `data/echogalacticcore/port/celestial_routes.json`
- `data/echogalacticcore/port/celestial_environments.json`
- `data/echogalacticcore/port/outer_planet_progression.json`
- `data/echogalacticcore/port/dungeon_reward_progression.json`
- `data/echogalacticcore/port/dungeon_boss_parity.json`
- `data/echogalacticcore/port/echo_integrations.json`

These are registered through `EchoNativeResourceService` so PackOS, Index, Lens, HoloMap, ScreenCore, and tests can inspect the current port contract without depending on legacy Forge classes.

The manifest set is validated by `verifyGameplayManifestCoverage`. This gate proves the data contracts are present, schema-tagged, source-attributed, and registered through typed ASDK resource receipts. It does not claim that all machine ticking, oxygen simulation, rocket flight, chunk placement, boss AI, or treasure reward runtime behavior is complete.

## Executable Runtime Foundation

`GalacticCoreRuntimeService` provides deterministic ASDK-native state transitions for the first gameplay runtime slice:

- oxygen collector production from energy and leaf scan input
- oxygen sealer oxygen/energy consumption and sealed volume state
- fuel loader transfer into linked rockets
- rocket workbench schematic readiness and tier-one rocket output
- energy buffer transfer
- player oxygen and thermal protection checks
- rocket launch readiness for pad, fuel, oxygen, crew, route unlock, and vehicle tier
- celestial route tier and unlock evaluation
- Moon, Mars, Asteroids, Venus, and Orbit environment scan models
- deterministic dimension-transfer placement plans with landing/orbit coordinates, entry mode, parachute and landing-pad gates, and required host actions
- deterministic dimension-transfer execution plans with destination loading, chunk ticketing, player placement, progression sync, and countdown clearing host actions
- deterministic HoloMap route surface state for Orbit, Moon, Mars, Asteroids, and Venus route locks and hazards
- ScreenCore launch checklist state combining pad, fuel, oxygen check, crew, vehicle tier, route unlock, and life-support readiness
- typed HoloMap route selection/preview interactions and ScreenCore checklist controls for countdown, refresh, and abort actions
- rendered menu layout contracts for HoloMap routes, ScreenCore launch checklist, and treasure chest rewards, including regions, widgets, renderer ids, action ids, and save-data targets
- deterministic Moon, Mars, and Venus dungeon structure plans with boss rooms, locked treasure rooms, keys, loot, schematic rewards, and route unlock targets
- deterministic dungeon boss spawn intent, legacy-derived entity source names, attributes, encounter state attachment actions, phase transitions, defeat/key-drop checks, safe reward unlock signals, and host-neutral boss AI movement/attack/room-lock intent
- treasure room interaction gating and ScreenCore treasure chest surfaces for room lock state, defeated boss state, player key state, loot previews, schematic rewards, route unlocks, and claim actions
- dungeon reward claiming for boss keys, loot, schematics, and route unlocks

The service is registered as `echogalacticcore:runtime` and installed through typed capability/save-data receipts. This is the runtime foundation that later world hooks, block entity adapters, ScreenCore surfaces, HoloMap actions, and server game tests should call into.

Route, environment, transfer placement/execution, HoloMap route surface/interactions/rendered menu, ScreenCore checklist surface/interactions/rendered menu, dungeon structure, boss entity spawn, boss encounter, boss AI intent, treasure interaction, treasure chest screen/rendered menu, and dungeon progression are also installed through attachment, save-data, event, network, screen, and worldgen receipts. The current runtime can decide whether Mars, Asteroids, and Venus are unlocked, can plan landing/orbit placement through typed worldgen `placeStructure` receipts, can publish host transfer execution intent for destination loading, chunk ticketing, player placement, and progression sync, can expose route, checklist, and treasure chest screen payloads, can publish rendered menu layout contracts for those screens, can publish route-selection and launch-checklist widget packets, can prepare deterministic dungeon structure plans, can publish boss entity spawn intent with legacy-derived source names and host actions, can tick boss encounter state into key-drop evidence, can publish boss movement/attack/room-lock intent for host entity execution, and can gate treasure room interactions without direct player inventory mutation. `GalacticCoreHostExecutionBridge` now turns the highest-value intent paths into host execution receipts for transfer commits, boss spawn commits, and rendered menu binding. `GalacticCoreHostBindingContracts` now assigns those receipts to concrete ASDK owner surfaces: worldgen for transfer placement, capabilities for boss entity spawn integration, and screens for menu hosts. `GalacticCoreLiveHostAdapters` now records live executor plans behind those bindings for destination load/teleport/progression sync, boss entity construction/state attach/room lock, and screen renderer/widget/action mounting. `GalacticCoreLiveHostEntrypoints` now exposes ASDK-safe callable entrypoints for host world transfer, boss spawn, and menu-open requests while preserving adapter and binding evidence. `GalacticCorePlatformExecutors` now consumes those entrypoint results and emits explicit platform execution receipts while keeping Minecraft object mutation deferred to a host-owned ASDK boundary. `GalacticCoreLiveSessionMutations` now defines that host-owned boundary as a pluggable mutation sink and dispatches non-dry-run world/entity/screen mutation requests through it. Remaining work is to provide the real platform host sink implementation that mutates live Minecraft objects instead of the contract-only registration sink.

`GalacticCoreRuntimeGateway` now bridges the runtime models into ASDK-style actions:

- event publish payloads for machine ticks, life support ticks, and environment scans
- event publish payloads for dimension transfer execution intents
- event publish payloads for dungeon boss entity spawn, boss encounter ticks, and boss AI steps
- worldgen feature payloads for dungeon structure plans
- worldgen placement payloads for transfer landing/orbit zones
- network payloads for route actions, HoloMap route interactions, ScreenCore checklist interactions, dungeon reward claims, and treasure interactions
- screen open payloads for celestial selection, HoloMap route, legacy launch checklist, ScreenCore checklist, treasure chest reward surfaces, and rendered layout contracts

The gateway is registered as `echogalacticcore:runtime_gateway` and replayed through typed event, network, screen, worldgen, and capability receipts during module load. Real platform callbacks should call this gateway instead of reintroducing `PacketSimple`, `GuiHandler`, or Forge event-bus code.

`GalacticCoreRuntimeAdapters` now provides the next host-facing layer:

- machine block entity adapter ticks that produce runtime state and save-data targets
- dimension transfer plans that combine launch checklist, route action, environment scan, and transfer placement outputs
- transfer placement plans that produce worldgen placement receipts and save-data targets
- transfer execution plans that produce host event receipts and save-data targets
- HoloMap route, ScreenCore launch checklist, rendered menu layout, route/checklist interaction, and treasure chest screen plans with save-data targets
- dungeon structure plans that produce collision-safe worldgen receipts and save-data targets
- dungeon encounter plans that validate boss/key rewards and broadcast attachment-backed reward claims
- boss entity spawn, boss encounter, and boss AI plans that publish event evidence and save deterministic encounter/entity intent state
- treasure interaction plans that broadcast network evidence and save loot/schematic unlock state

The adapters are registered as `echogalacticcore:runtime_adapters` and installed through typed capability, save-data, worldgen, event, network, and screen receipts. Remaining work is to connect actual ASDK host callbacks from block entity ticks, dimension transfer, chunk placement, boss entity construction/rendering, concrete treasure chest menus, and UI surfaces into these adapters.

## Host Execution Bridge

`GalacticCoreHostExecutionBridge` is registered as `echogalacticcore:host_execution_bridge` and installed through typed capability, lifecycle, save-data, event, and screen receipts. It consumes the host callback facade and emits executable host binding plans for:

- dimension transfer execution commit order: load destination, ticket chunk, place player, sync progression
- boss entity spawn commit order: load boss room, spawn entity, attach encounter state, lock room
- rendered menu binding: bind renderer, mount widgets, wire actions, sync screen state

This bridge is still host-neutral: it proves ASDK-facing execution contracts and mutation receipts.

## Concrete Host Binding Contracts

`GalacticCoreHostBindingContracts` is registered as `echogalacticcore:host_binding_contracts` and installed through typed capability, lifecycle, save-data, worldgen, and screen receipts. It maps host execution plans onto concrete ASDK owner services:

- dimension transfer placement is owned by `EchoNativeWorldgenService.placeStructure`
- dungeon boss entity spawning is owned by `EchoNativeCapabilityService.registerIntegration`
- HoloMap route, ScreenCore launch checklist, and treasure chest menus are owned by `EchoNativeScreenService.registerMenu`

This proves which ASDK service must return the mutation receipt for each live operation. The next implementation step is live adapter code that performs the platform world load/teleport, entity construction/attachment, and screen/menu mounting while preserving these binding ids and receipt evidence.

## Live Host Adapter Plans

`GalacticCoreLiveHostAdapters` is registered as `echogalacticcore:live_host_adapters` and installed through typed capability, lifecycle, save-data, worldgen, capability-integration, and screen receipts. It converts concrete host bindings into live executor plans:

- world transfer executor: resolve destination level, ticket destination chunk, place player at anchor, sync progression attachment
- boss entity executor: resolve boss room, instantiate boss entity, attach boss encounter state, lock treasure room
- screen executor: resolve screen factory, mount renderer, mount widgets, wire actions, sync screen state

These plans are still ASDK-safe and do not import Forge, NeoForge, or loader internals. They define the live execution steps and receipt ids the eventual platform callback implementation must preserve.

## Live Host Entrypoints

`GalacticCoreLiveHostEntrypoints` is registered as `echogalacticcore:live_host_entrypoints` and installed through typed capability, lifecycle, save-data, worldgen, capability-integration, and screen receipts. It exposes callable boundaries for:

- world transfer: host calls `executeWorldTransfer`, which resolves the live adapter plan and emits `live_callback/world_dimension_transfer`
- boss spawn: host calls `executeBossSpawn`, which resolves the entity adapter plan and emits `live_callback/entity_boss_spawn`
- menu opening: host calls `openScreenHost`, which resolves HoloMap route, ScreenCore checklist, or treasure screen adapter plans and emits `live_callback/screen_*`

Each result preserves the host entrypoint id, subject id, host lane, adapter target, binding target, completed executor steps, and save-data target. The class still avoids direct Minecraft object mutation; the platform executor should perform the actual teleport/entity/screen work after receiving these ASDK-safe entrypoint results.

## Platform Executor Facade

`GalacticCorePlatformExecutors` is registered as `echogalacticcore:platform_executors` and installed through typed capability, lifecycle, save-data, worldgen, capability-integration, and screen receipts. It consumes the live host entrypoint results for:

- world transfer execution
- dungeon boss spawn execution
- HoloMap route, ScreenCore checklist, and treasure chest menu opening

The executor facade emits `platform_executor/*` receipt targets with entrypoint, adapter, binding, subject, host lane, dry-run, and save-data evidence. It deliberately records `platformMutationDeferred=true` and `mutatesMinecraftObjects=false`, so the native module remains ASDK-safe until the host-owned platform implementation performs the real teleport, entity construction, attachment mutation, and screen opening inside a live game session.

## Live Session Mutation Bridge

`GalacticCoreLiveSessionMutations` is registered as `echogalacticcore:live_session_mutations` and installed through typed capability, lifecycle, save-data, worldgen, capability-integration, and screen receipts. It turns non-dry-run `GalacticCorePlatformExecutors` results into host-owned mutation requests for:

- world transfer commits
- dungeon boss spawn commits
- HoloMap route, ScreenCore checklist, and treasure chest menu opens

The bridge introduces a pluggable `HostMutationSink` contract. Each `LiveSessionMutationRequest` now carries a `HostMutationPayload` that names the host API to call, ASDK owner service, payload target, subject, host lane, required step order, state keys, and safety checks. Tests can attach a recording sink that reports real Minecraft-object mutation acceptance, while module-load smoke registration uses a contract-only sink that reports `mutatesMinecraftObjects=false` and `hostRuntimeRequired=true`. This keeps the addon independent from Forge, NeoForge, and loader internals while making the live platform mutation boundary explicit enough for the eventual platform sink.

`GalacticCoreHostCallbacks` now provides that normalized host invocation facade:

- machine block entity tick callback
- player life-support tick callback
- celestial route selection callback
- HoloMap route surface callback
- HoloMap route rendered menu callback
- HoloMap route interaction callback
- ScreenCore launch checklist callback
- ScreenCore launch checklist rendered menu callback
- ScreenCore launch checklist interaction callback
- dimension transfer request callback
- transfer placement prepare callback
- dimension transfer execution callback
- environment scan callback
- dungeon structure prepare callback
- dungeon boss entity spawn callback
- dungeon boss encounter tick callback
- dungeon treasure interaction and treasure chest screen callbacks
- dungeon treasure rendered menu callback
- dungeon treasure claim callback

The callback facade is registered as `echogalacticcore:host_callbacks`, installs typed capability evidence, subscribes host callback event names, and publishes host-sourced event/network/screen/worldgen smoke actions. Actual ASDK host implementations should call this facade when block entities tick, players request travel, transfer landing zones are prepared, transfer execution is requested, HoloMap route surfaces open or dispatch route interactions, ScreenCore launch checklists open or dispatch checklist interactions, Lens scans run, dungeon structures are prepared, dungeon bosses tick, boss entities request AI intent, treasure rooms are opened, treasure chest screens are requested, and treasure claims are made.

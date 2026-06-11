# ECHO: Sky Relay Full Experience Plan

Sky Relay is Official ECHO Pack #3: a vertical survival-restoration pack about
rebuilding a broken atmospheric relay network above a lethal storm layer.

The core promise is simple: the player is not building an island in the sky.
The player is bringing an impossible machine back online.

## Canonical Names

| Surface | Name |
| --- | --- |
| Public pack name | `ECHO: Sky Relay` |
| Short name | `Sky Relay` |
| Pack id | `sky-relay` |
| Main module id | `echoskyrelayprotocol` |
| Primary game mode | `skyrelay_restoration` |
| Native pack id | `sky-relay-native-edition` |
| NeoForge pack id | `sky-relay-neoforge-edition` |
| Standalone pack id | `sky-relay-standalone-edition` |
| Module release id | `sky-relay-0.1.0-alpha` |
| Native release id | `sky-relay-native-0.1.0-alpha` |
| NeoForge release id | `sky-relay-neoforge-0.1.0-alpha` |
| Standalone release id | `sky-relay-standalone-0.1.0-alpha` |

## Repository Names

| Purpose | Repository |
| --- | --- |
| Module source | `knoxhack/ECHO-Modules`, path `addons/echoskyrelayprotocol` |
| Native edition | `knoxhack/ECHO-Sky-Relay-Native-Edition` |
| NeoForge edition | `knoxhack/ECHO-Sky-Relay-NeoForge-Edition` |
| Standalone edition | `knoxhack/ECHO-Sky-Relay-Standalone-Edition` |
| Launcher consumer | `knoxhack/ECHO-Launcher` |
| Release catalog | `knoxhack/ECHO-Release-Index` |
| Website surface | `knoxhack/ECHO-Platform-Website` |
| SDK templates | `knoxhack/ECHO-SDK` |

## Required Module Surfaces

`echoskyrelayprotocol` provides:

- `skyrelay.content`
- `skyrelay.missions`
- `skyrelay.fragments`
- `skyrelay.anchors`
- `skyrelay.resources`
- `skyrelay.terminal`
- `skyrelay.weather_routes`
- `skyrelay.holomap_layers`
- `skyrelay.lens_profiles`
- `skyrelay.release_readiness`

Required modules:

- `echocore`
- `echonetcore`
- `echoadaptercore`
- `echoruntimeguard`

High-value optional modules:

- `echoterminal`
- `echoindex`
- `echolens`
- `echoholomap`
- `echomissioncore`
- `echotutorialcore`
- `echothemecore`
- `echopowergrid`
- `echoweathercore`
- `echoworldcore`
- `echologisticsnetwork`
- `echoagriculturereclamation`
- `echorecovery`
- `echoprogressioncore`
- `echorecipecore`
- `echoindustrialnexus`
- `echomultiblockcore`
- `echoeconomycore`
- `echosocialcore`
- `echogalacticcore`
- `echoorbitalremnants`

## 10 Phases, 5 Subphases Each

### Phase 1: Repo Foundation

1. Audit dirty repos, especially `ECHO-Modules` and `ECHO-Release-Index`.
2. Create branch `feature/sky-relay-protocol`.
3. Reserve repo names: `ECHO-Sky-Relay-Native-Edition`, `ECHO-Sky-Relay-NeoForge-Edition`, `ECHO-Sky-Relay-Standalone-Edition`.
4. Add this plan at `docs/SKY_RELAY_FULL_EXPERIENCE_PLAN.md`.
5. Define release naming: `sky-relay-0.1.0-alpha`, `sky-relay-native-0.1.0-alpha`, `sky-relay-neoforge-0.1.0-alpha`, `sky-relay-standalone-0.1.0-alpha`.

### Phase 2: Protocol Module

1. Create `addons/echoskyrelayprotocol`.
2. Add `build.gradle`, `gradle.properties`, and `README.md`.
3. Add Java entrypoints: `EchoSkyRelayProtocol`, `EchoSkyRelayNativeModule`.
4. Add `META-INF/echo.mod.json`.
5. Wire the module into `settings.gradle` and root `build.gradle`.

### Phase 3: Identity And Metadata

1. Set module name `ECHO: Sky Relay Protocol`.
2. Set pack role `official_pack`.
3. Provide namespaces for content, missions, fragments, terminal, HoloMap, Lens, weather routes, and release readiness.
4. Declare dependencies on Terminal, Lens, HoloMap, Weather, Power, Recovery, and Logistics as integration surfaces.
5. Add first Release Index metadata later as warning/planned until compiled artifacts and checksums exist.

### Phase 4: Core Blocks

1. Add start/progression blocks: `damaged_relay_core`, `relay_anchor_node`, `fragment_docking_clamp`.
2. Add survival blocks: `atmospheric_condenser`, `storm_shield_pylon`, `pressure_bulkhead`.
3. Add network blocks: `sky_fragment_beacon`, `relay_signal_array`, `relay_marker_light`.
4. Add loot/recovery blocks: `aero_salvage_crate`, `void_recovery_cache`.
5. Add endgame blocks: `skybridge_projector`, `signal_crown_interface`, `storm_output_collector`.

### Phase 5: Core Items

1. Add progression items: `operator_badge`, `relay_anchor_key`, `sky_fragment_chart`.
2. Add repair items: `charged_relay_coil`, `relay_alloy_plate`, `signal_calibration_chip`.
3. Add survival items: `atmospheric_filter`, `stormproof_wrap`, `relay_firmware_shard`.
4. Add gated items: `stabilized_platform_core`, `fragment_access_cipher`.
5. Add rare/endgame items: `static_filament`, `orbital_alloy_scrap`, `satellite_lens`, `echo_crystal_charge`, `sky_relay_badge`.

### Phase 6: Fragments And World Loop

1. Define `starter_relay`.
2. Define early fragments: `hydroponics_deck`, `aero_salvage_yard`, `solar_wing`.
3. Define midgame fragments: `weather_mast`, `machine_bay`, `logistics_spur`.
4. Define late fragments: `orbital_debris_dock`, `signal_crown`.
5. Add anchor rules, power cost, scan requirement, and storm risk per fragment.

### Phase 7: Player Progression

1. Chapter `awakening`: wake at the Damaged Relay Core.
2. Chapter `power_critical`: repair hand crank, battery, and power meter flow.
3. Chapter `first_anchor`: unlock and attach Hydroponics Deck.
4. Chapter `storm_warning`: shelter, storm shield, condenser, and first severe weather.
5. Chapter `signal_crown`: final restoration sequence and `sky_relay_badge`.

### Phase 8: Systems Integration

1. Terminal pages: relay status, missions, storm forecast, fragment registry.
2. Lens scan profiles for core, anchors, crates, storm devices, and locked fragments.
3. HoloMap layers for nearby fragments, power grid, shield coverage, and logistics.
4. Weather bindings for storms, collectors, shield failures, and rare output.
5. Recovery binding for `void_recovery_cache`.

### Phase 9: Editions And Launcher

1. Create Native edition repo and manifest.
2. Create NeoForge edition repo and manifest.
3. Create Standalone edition repo and manifest.
4. Add Launcher pack card: `ECHO: Sky Relay`.
5. Test launcher install, update, repair, rollback, and deep links.

### Phase 10: Release And Public Alpha

1. Build compiled module artifacts.
2. Generate `echo-release.json` for `echoskyrelayprotocol`.
3. Publish draft GitHub releases.
4. Add Release Index entries for addon, packs, and modpacks as `warning` until verified.
5. Promote to public alpha only after checksum, download, launcher, and gameplay smoke tests pass.

## Core Block Catalog

| ID | Name | Phase | Purpose |
| --- | --- | --- | --- |
| `damaged_relay_core` | Damaged Relay Core | 4.1 | Starting objective and progression root |
| `relay_anchor_node` | Relay Anchor Node | 4.1 | Powers and validates fragment attachment |
| `fragment_docking_clamp` | Fragment Docking Clamp | 4.1 | Physical connector for new platforms |
| `atmospheric_condenser` | Atmospheric Condenser | 4.2 | Produces dirty water from air and storms |
| `storm_shield_pylon` | Storm Shield Pylon | 4.2 | Protects a small platform area |
| `pressure_bulkhead` | Pressure Bulkhead | 4.2 | Storm-safe shelter door or block |
| `sky_fragment_beacon` | Sky Fragment Beacon | 4.3 | Reveals nearby fragments on HoloMap |
| `relay_signal_array` | Relay Signal Array | 4.3 | Midgame network restoration block |
| `relay_marker_light` | Relay Marker Light | 4.3 | Visual platform status marker |
| `aero_salvage_crate` | Aero Salvage Crate | 4.4 | Loot and salvage source |
| `void_recovery_cache` | Void Recovery Cache | 4.4 | Stores items after void deaths |
| `skybridge_projector` | Skybridge Projector | 4.5 | Late-game route bridge |
| `signal_crown_interface` | Signal Crown Interface | 4.5 | Endgame restoration console |
| `storm_output_collector` | Storm Output Collector | 4.5 | Converts storm exposure into rare materials |

## Core Item Catalog

| ID | Name | Phase | Purpose |
| --- | --- | --- | --- |
| `operator_badge` | Operator Badge | 5.1 | Player identity and progression token |
| `relay_anchor_key` | Relay Anchor Key | 5.1 | Unlocks early platform anchors |
| `sky_fragment_chart` | Sky Fragment Chart | 5.1 | Reveals fragment candidates |
| `charged_relay_coil` | Charged Relay Coil | 5.2 | Repairs relay machinery |
| `relay_alloy_plate` | Relay Alloy Plate | 5.2 | Main construction material |
| `signal_calibration_chip` | Signal Calibration Chip | 5.2 | Unlocks scan and HoloMap upgrades |
| `atmospheric_filter` | Atmospheric Filter | 5.3 | Used in condensers and shelters |
| `stormproof_wrap` | Stormproof Wrap | 5.3 | Temporary exposure protection |
| `relay_firmware_shard` | Relay Firmware Shard | 5.3 | Mission and Terminal unlock item |
| `stabilized_platform_core` | Stabilized Platform Core | 5.4 | Required for large fragments |
| `fragment_access_cipher` | Fragment Access Cipher | 5.4 | Opens rare or locked fragments |
| `static_filament` | Static Filament | 5.5 | Rare storm output |
| `orbital_alloy_scrap` | Orbital Alloy Scrap | 5.5 | Late-game orbital material |
| `satellite_lens` | Satellite Lens | 5.5 | Weather and HoloMap upgrade material |
| `echo_crystal_charge` | Echo Crystal Charge | 5.5 | Signal Crown power component |
| `sky_relay_badge` | Sky Relay Badge | 5.5 | Completion trophy |

## Fragment Catalog

| ID | Name | Tier | Gate |
| --- | --- | --- | --- |
| `starter_relay` | Starter Relay | 0 | Spawn platform |
| `hydroponics_deck` | Hydroponics Deck | 1 | `relay_anchor_key` |
| `aero_salvage_yard` | Aero Salvage Yard | 1 | `sky_fragment_chart` |
| `solar_wing` | Solar Wing | 1 | `charged_relay_coil` |
| `weather_mast` | Weather Mast | 2 | `signal_calibration_chip` |
| `machine_bay` | Machine Bay | 2 | `stabilized_platform_core` |
| `logistics_spur` | Logistics Spur | 2 | Logistics route proof |
| `orbital_debris_dock` | Orbital Debris Dock | 3 | `fragment_access_cipher` |
| `signal_crown` | Signal Crown | 4 | Endgame restoration sequence |

## Public Alpha Gate

Sky Relay cannot be marked public alpha until this evidence exists:

- module graph validation passes
- `:echoskyrelayprotocol:jar` builds
- strict module release generation emits compiled runtime artifacts
- Native, NeoForge, and Standalone edition manifests exist
- Release Index strict validation passes with real checksums
- Launcher install/update/repair/rollback passes
- first 30-minute playthrough passes
- first 2-hour playthrough passes
- Signal Crown completion path passes
- support, rollback, known-issues, and release notes are published

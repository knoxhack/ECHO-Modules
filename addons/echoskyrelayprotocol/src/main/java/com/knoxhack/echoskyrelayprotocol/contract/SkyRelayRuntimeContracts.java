package com.knoxhack.echoskyrelayprotocol.contract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkyRelayRuntimeContracts {
    public static final String MODULE_ID = "echoskyrelayprotocol";
    public static final String PACK_ID = "sky-relay";
    public static final String RESTORATION_MODE = "skyrelay_restoration";

    public static final List<String> PHASE_IDS = List.of(
            "repo_foundation",
            "protocol_module",
            "identity_metadata",
            "core_blocks",
            "core_items",
            "fragments_world_loop",
            "player_progression",
            "systems_integration",
            "editions_launcher",
            "release_public_alpha"
    );

    public static final List<String> BLOCK_IDS = List.of(
            "damaged_relay_core",
            "relay_anchor_node",
            "fragment_docking_clamp",
            "atmospheric_condenser",
            "storm_shield_pylon",
            "pressure_bulkhead",
            "sky_fragment_beacon",
            "relay_signal_array",
            "relay_marker_light",
            "aero_salvage_crate",
            "void_recovery_cache",
            "skybridge_projector",
            "signal_crown_interface",
            "storm_output_collector"
    );

    public static final List<String> ITEM_IDS = List.of(
            "operator_badge",
            "relay_anchor_key",
            "sky_fragment_chart",
            "charged_relay_coil",
            "relay_alloy_plate",
            "signal_calibration_chip",
            "atmospheric_filter",
            "stormproof_wrap",
            "relay_firmware_shard",
            "stabilized_platform_core",
            "fragment_access_cipher",
            "static_filament",
            "orbital_alloy_scrap",
            "satellite_lens",
            "echo_crystal_charge",
            "sky_relay_badge"
    );

    public static final List<String> FRAGMENT_IDS = List.of(
            "starter_relay",
            "hydroponics_deck",
            "aero_salvage_yard",
            "solar_wing",
            "weather_mast",
            "machine_bay",
            "logistics_spur",
            "orbital_debris_dock",
            "signal_crown"
    );

    public static final List<String> CHAPTER_IDS = List.of(
            "awakening",
            "power_critical",
            "first_anchor",
            "water_problem",
            "storm_warning",
            "salvage_expansion",
            "solar_recovery",
            "weather_control",
            "machine_restoration",
            "network_routing",
            "orbital_reach",
            "signal_crown"
    );

    public static final List<String> RUNTIME_TARGETS = List.of(
            "echo_native",
            "neoforge",
            "echo_runtime_standalone"
    );

    public static final List<String> EDITION_IDS = List.of(
            "sky-relay-native-edition",
            "sky-relay-neoforge-edition",
            "sky-relay-standalone-edition"
    );

    public static final List<String> RELEASE_REPOSITORIES = List.of(
            "knoxhack/ECHO-Sky-Relay-Native-Edition",
            "knoxhack/ECHO-Sky-Relay-NeoForge-Edition",
            "knoxhack/ECHO-Sky-Relay-Standalone-Edition"
    );

    private SkyRelayRuntimeContracts() {
    }

    public static Map<String, Object> adapterManifest() {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema", "echo.skyrelay.adapter_manifest.v1");
        manifest.put("moduleId", MODULE_ID);
        manifest.put("packId", PACK_ID);
        manifest.put("defaultMode", RESTORATION_MODE);
        manifest.put("phaseIds", PHASE_IDS);
        manifest.put("blockIds", BLOCK_IDS);
        manifest.put("itemIds", ITEM_IDS);
        manifest.put("fragmentIds", FRAGMENT_IDS);
        manifest.put("chapterIds", CHAPTER_IDS);
        manifest.put("runtimeTargets", RUNTIME_TARGETS);
        manifest.put("editionIds", EDITION_IDS);
        manifest.put("releaseRepositories", RELEASE_REPOSITORIES);
        return Map.copyOf(manifest);
    }

    public static List<String> contractIds() {
        return List.of(
                "skyrelay.content",
                "skyrelay.missions",
                "skyrelay.fragments",
                "skyrelay.anchors",
                "skyrelay.resources",
                "skyrelay.terminal",
                "skyrelay.weather_routes",
                "skyrelay.holomap_layers",
                "skyrelay.lens_profiles",
                "skyrelay.release_readiness"
        );
    }

    public static List<String> contractResourcePaths() {
        return List.of(
                "data/echoskyrelayprotocol/skyrelay/plan/production_phase_matrix.json",
                "data/echoskyrelayprotocol/skyrelay/content/block_catalog.json",
                "data/echoskyrelayprotocol/skyrelay/content/item_catalog.json",
                "data/echoskyrelayprotocol/skyrelay/fragments/fragment_catalog.json",
                "data/echoskyrelayprotocol/skyrelay/fragments/anchor_rules.json",
                "data/echoskyrelayprotocol/skyrelay/progression/chapter_catalog.json",
                "data/echoskyrelayprotocol/skyrelay/progression/first_30_minutes.json",
                "data/echoskyrelayprotocol/skyrelay/progression/first_2_hours.json",
                "data/echoskyrelayprotocol/skyrelay/progression/signal_crown_requirements.json",
                "data/echoskyrelayprotocol/skyrelay/integrations/system_surfaces.json",
                "data/echoskyrelayprotocol/skyrelay/integrations/terminal_pages.json",
                "data/echoskyrelayprotocol/skyrelay/integrations/lens_scan_profiles.json",
                "data/echoskyrelayprotocol/skyrelay/integrations/holomap_layers.json",
                "data/echoskyrelayprotocol/skyrelay/integrations/weather_routes.json",
                "data/echoskyrelayprotocol/skyrelay/integrations/recovery_bindings.json",
                "data/echoskyrelayprotocol/skyrelay/release/repository_map.json"
        );
    }
}

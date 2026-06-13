package com.knoxhack.echogalacticsurveyprotocol.contract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GalacticSurveyRuntimeContracts {
    public static final String MODULE_ID = "echogalacticsurveyprotocol";
    public static final String PACK_ID = "galactic-survey";
    public static final String LONG_RANGE_SURVEY_MODE = "long_range_survey";

    public static final List<String> PHASE_IDS = List.of(
            "identity_scope",
            "protocol_scaffold",
            "survey_data_contracts",
            "first_30_minute_slice",
            "probe_first_loop",
            "holomap_main_character",
            "catalog_missions_certification",
            "fuel_routes_depots",
            "orbital_salvage_upgrades",
            "editions_release_gate"
    );

    public static final List<String> BLOCK_IDS = List.of(
            "survey_terminal",
            "probe_launcher",
            "fuel_mixer",
            "signal_dish",
            "navigation_table",
            "orbital_salvage_crate",
            "remote_depot_anchor",
            "survey_beacon_pylon",
            "route_stabilizer_station",
            "survey_array_console"
    );

    public static final List<String> ITEM_IDS = List.of(
            "starter_probe",
            "survey_beacon",
            "fuel_canister",
            "navigation_core",
            "burned_navigation_core",
            "orbital_scrap",
            "catalog_badge",
            "long_range_probe",
            "radiation_shielding",
            "route_stabilizer",
            "deep_space_lens",
            "stellar_chart_fragment",
            "survey_array_key",
            "depot_manifest",
            "fuel_quality_sample",
            "galactic_survey_badge"
    );

    public static final List<String> SECTOR_IDS = List.of(
            "near_sector_01",
            "outer_sector_02",
            "derelict_corridor_03",
            "deep_sector_04"
    );

    public static final List<String> DISCOVERY_IDS = List.of(
            "barren_moon_kg_01a",
            "ice_body_kg_01b",
            "planet_candidate_ks_02",
            "signal_anomaly_veil_trace",
            "debris_belt_cinder_ring",
            "derelict_relay_osprey",
            "lost_survey_craft_lysander",
            "unstable_orbital_platform_ariadne",
            "deep_sector_beacon_ks_04"
    );

    public static final List<String> PROBE_IDS = List.of(
            "starter_probe",
            "long_range_probe",
            "salvage_mapper_probe",
            "anomaly_lens_probe"
    );

    public static final List<String> CHAPTER_IDS = List.of(
            "outpost_wake",
            "network_offline",
            "relay_repair",
            "first_probe_launch",
            "partial_map_reveal",
            "first_salvage",
            "first_catalog_entry",
            "fuel_route_prep",
            "survey_circuit",
            "remote_depot",
            "hazard_salvage",
            "survey_array_restoration"
    );

    public static final List<String> RUNTIME_TARGETS = List.of(
            "echo_native",
            "neoforge",
            "echo_runtime_standalone"
    );

    public static final List<String> EDITION_IDS = List.of(
            "galactic-survey-native-edition",
            "galactic-survey-neoforge-edition",
            "galactic-survey-standalone-edition"
    );

    public static final List<String> RELEASE_REPOSITORIES = List.of(
            "knoxhack/ECHO-Galactic-Survey-Native-Edition",
            "knoxhack/ECHO-Galactic-Survey-NeoForge-Edition",
            "knoxhack/ECHO-Galactic-Survey-Standalone-Edition"
    );

    private GalacticSurveyRuntimeContracts() {
    }

    public static Map<String, Object> adapterManifest() {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema", "echo.galactic_survey.adapter_manifest.v1");
        manifest.put("moduleId", MODULE_ID);
        manifest.put("packId", PACK_ID);
        manifest.put("defaultMode", LONG_RANGE_SURVEY_MODE);
        manifest.put("phaseIds", PHASE_IDS);
        manifest.put("blockIds", BLOCK_IDS);
        manifest.put("itemIds", ITEM_IDS);
        manifest.put("sectorIds", SECTOR_IDS);
        manifest.put("discoveryIds", DISCOVERY_IDS);
        manifest.put("probeIds", PROBE_IDS);
        manifest.put("chapterIds", CHAPTER_IDS);
        manifest.put("runtimeTargets", RUNTIME_TARGETS);
        manifest.put("editionIds", EDITION_IDS);
        manifest.put("releaseRepositories", RELEASE_REPOSITORIES);
        return Map.copyOf(manifest);
    }

    public static List<String> contractIds() {
        return List.of(
                "galacticsurvey.content",
                "galacticsurvey.sectors",
                "galacticsurvey.bodies",
                "galacticsurvey.probes",
                "galacticsurvey.routes",
                "galacticsurvey.discoveries",
                "galacticsurvey.salvage",
                "galacticsurvey.depots",
                "galacticsurvey.runtime",
                "galacticsurvey.terminal",
                "galacticsurvey.holomap_layers",
                "galacticsurvey.lens_profiles",
                "galacticsurvey.missions",
                "galacticsurvey.release_readiness"
        );
    }

    public static List<String> contractResourcePaths() {
        return List.of(
                "data/echogalacticsurveyprotocol/galacticsurvey/plan/production_phase_matrix.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/content/block_catalog.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/content/item_catalog.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/survey/sector_catalog.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/survey/body_catalog.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/survey/probe_catalog.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/survey/route_catalog.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/survey/discovery_catalog.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/survey/salvage_sites.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/survey/salvage_loot_tables.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/survey/depot_catalog.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/progression/chapter_catalog.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/progression/first_30_minutes.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/progression/first_2_hours.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/progression/survey_array_requirements.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/integrations/system_surfaces.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/integrations/terminal_pages.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/integrations/lens_scan_profiles.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/integrations/holomap_layers.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/integrations/mission_contracts.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/integrations/sound_events.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/release/repository_map.json",
                "data/echogalacticsurveyprotocol/galacticsurvey/release/release_gates.json"
        );
    }
}

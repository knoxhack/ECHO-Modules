package com.knoxhack.echogalacticsurveyprotocol.runtime;

import com.knoxhack.echogalacticsurveyprotocol.contract.GalacticSurveyRuntimeContracts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GalacticSurveyProgressionRuntime {
    public record RouteStep(String id, String chapterId, String objective, String proof) {
        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("chapterId", chapterId);
            result.put("objective", objective);
            result.put("proof", proof);
            return Map.copyOf(result);
        }
    }

    public record EndgameRequirement(String id, int requiredCount, String proof) {
        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("requiredCount", requiredCount);
            result.put("proof", proof);
            return Map.copyOf(result);
        }
    }

    private static final List<RouteStep> FIRST_30_MINUTES = List.of(
            new RouteStep("wake_at_outpost", "outpost_wake", "Wake at the quiet survey outpost.", "block_seen:survey_terminal"),
            new RouteStep("open_survey_terminal", "network_offline", "Open Terminal and see Survey Network Offline.", "terminal_page:survey_network"),
            new RouteStep("repair_small_relay", "relay_repair", "Repair the small power relay.", "power:small_relay_online"),
            new RouteStep("claim_starter_probe", "first_probe_launch", "Claim or craft a starter probe.", "item:starter_probe"),
            new RouteStep("charge_probe_launcher", "first_probe_launch", "Bring the probe launcher online.", "block:probe_launcher"),
            new RouteStep("launch_near_sector_probe", "first_probe_launch", "Launch probe to near_sector_01.", "probe:starter_probe"),
            new RouteStep("reveal_partial_signals", "partial_map_reveal", "Reveal partial signals on HoloMap.", "holomap_layer:scan_cones"),
            new RouteStep("lens_scan_orbital_fragment", "first_salvage", "Scan a fallen orbital fragment with Lens.", "lens_scan:fallen_orbital_fragment"),
            new RouteStep("recover_burned_core", "first_salvage", "Recover a burned navigation core.", "item:burned_navigation_core"),
            new RouteStep("catalog_first_body", "first_catalog_entry", "Catalog the first moon/body in Index.", "discovery:barren_moon_kg_01a"),
            new RouteStep("unlock_route_objective", "fuel_route_prep", "Unlock the first short survey hop objective.", "mission:first_survey_hop"),
            new RouteStep("prepare_fuel_canister", "fuel_route_prep", "Build or fill a fuel canister.", "item:fuel_canister")
    );

    private static final List<RouteStep> FIRST_TWO_HOURS = List.of(
            new RouteStep("launch_probe_batch", "first_probe_launch", "Launch 3-5 probes into local sectors.", "probe:starter_probe"),
            new RouteStep("catalog_local_moon", "first_catalog_entry", "Catalog a local barren moon.", "discovery:barren_moon_kg_01a"),
            new RouteStep("catalog_planet_candidate", "first_catalog_entry", "Catalog a planet candidate.", "discovery:planet_candidate_ks_02"),
            new RouteStep("catalog_anomaly", "partial_map_reveal", "Catalog a signal anomaly.", "discovery:signal_anomaly_veil_trace"),
            new RouteStep("recover_derelict_salvage", "hazard_salvage", "Recover salvage from one orbital wreck.", "salvage:derelict_relay_osprey"),
            new RouteStep("build_basic_fuel_route", "survey_circuit", "Build a basic fuel-safe route.", "route:near_sector_01_survey_hop"),
            new RouteStep("unlock_better_probe", "survey_circuit", "Unlock a better probe chassis.", "item:long_range_probe"),
            new RouteStep("establish_remote_depot", "remote_depot", "Establish one remote depot.", "depot:cinder_ring_remote_depot"),
            new RouteStep("complete_first_survey_circuit", "survey_circuit", "Complete a first survey circuit.", "mission:first_survey_circuit"),
            new RouteStep("earn_first_badge", "first_catalog_entry", "Earn the first catalog rank badge.", "item:catalog_badge")
    );

    private static final List<EndgameRequirement> SURVEY_ARRAY_REQUIREMENTS = List.of(
            new EndgameRequirement("survey_array_console", 1, "block:survey_array_console"),
            new EndgameRequirement("complete_sector_atlas", 1, "catalog:complete_sector_atlas"),
            new EndgameRequirement("deep_sector_beacon", 1, "discovery:deep_sector_beacon_ks_04"),
            new EndgameRequirement("remote_depot_network", 1, "depot:cinder_ring_remote_depot"),
            new EndgameRequirement("advanced_probe_network", 1, "item:long_range_probe"),
            new EndgameRequirement("array_key_recovered", 1, "item:survey_array_key")
    );

    private GalacticSurveyProgressionRuntime() {
    }

    public static List<RouteStep> first30Minutes() {
        return FIRST_30_MINUTES;
    }

    public static List<RouteStep> firstTwoHours() {
        return FIRST_TWO_HOURS;
    }

    public static List<EndgameRequirement> surveyArrayRequirements() {
        return SURVEY_ARRAY_REQUIREMENTS;
    }

    public static Map<String, Object> adapterManifest() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", "echo.galactic_survey.progression_runtime.v1");
        result.put("moduleId", GalacticSurveyRuntimeContracts.MODULE_ID);
        result.put("chapterIds", GalacticSurveyRuntimeContracts.CHAPTER_IDS);
        result.put("first30Minutes", FIRST_30_MINUTES.stream().map(RouteStep::asMap).toList());
        result.put("firstTwoHours", FIRST_TWO_HOURS.stream().map(RouteStep::asMap).toList());
        result.put("surveyArrayRequirements", SURVEY_ARRAY_REQUIREMENTS.stream().map(EndgameRequirement::asMap).toList());
        return Map.copyOf(result);
    }
}

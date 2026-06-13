package com.knoxhack.echogalacticsurveyprotocol.integration;

import com.knoxhack.echogalacticsurveyprotocol.contract.GalacticSurveyRuntimeContracts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GalacticSurveySystemIntegrationContracts {
    public record TerminalPage(String id, String title, String purpose) {
        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("title", title);
            result.put("purpose", purpose);
            return Map.copyOf(result);
        }
    }

    public record LensProfile(String id, String target, String unlocks) {
        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("target", target);
            result.put("unlocks", unlocks);
            return Map.copyOf(result);
        }
    }

    public record HoloMapLayer(String id, String title, String purpose) {
        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("title", title);
            result.put("purpose", purpose);
            return Map.copyOf(result);
        }
    }

    public record MissionContract(String id, String title, String proof) {
        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("title", title);
            result.put("proof", proof);
            return Map.copyOf(result);
        }
    }

    private static final List<TerminalPage> TERMINAL_PAGES = List.of(
            new TerminalPage("survey_network", "Survey Network", "Shows network state, probe queue, sector unlocks, and route objectives."),
            new TerminalPage("probe_control", "Probe Control", "Launches probes, reports scan confidence, and lists recovery state."),
            new TerminalPage("route_planner", "Route Planner", "Shows fuel quality, return margin, depot coverage, and route risk."),
            new TerminalPage("salvage_log", "Salvage Log", "Tracks wreck hazards, recovered parts, and upgrade pressure.")
    );

    private static final List<LensProfile> LENS_PROFILES = List.of(
            new LensProfile("fallen_orbital_fragment", "orbital_salvage_crate", "burned_navigation_core and first salvage catalog proof"),
            new LensProfile("derelict_relay_osprey", "signal_dish", "derelict relay classification and route beacon data"),
            new LensProfile("fuel_mixer_diagnostics", "fuel_mixer", "fuel quality and safe return margin"),
            new LensProfile("remote_depot_anchor", "remote_depot_anchor", "depot inventory and probe recovery range"),
            new LensProfile("survey_array_console", "survey_array_console", "final restoration sequence and atlas publication")
    );

    private static final List<HoloMapLayer> HOLOMAP_LAYERS = List.of(
            new HoloMapLayer("sector_grid", "Sector Grid", "Shows known sectors and locked deep-space adjacency."),
            new HoloMapLayer("scan_cones", "Scan Cones", "Shows probe coverage, confidence, and unresolved signal arcs."),
            new HoloMapLayer("fuel_range", "Fuel Range", "Shows outbound, return-safe, marginal, and unsafe travel bands."),
            new HoloMapLayer("orbital_layers", "Orbital Layers", "Shows debris belts, orbital platforms, and salvage altitude bands."),
            new HoloMapLayer("derelict_beacons", "Derelict Beacons", "Shows wreck signatures and hazard preview status."),
            new HoloMapLayer("depot_coverage", "Depot Coverage", "Shows remote depot reach, fuel cache state, and probe recovery coverage."),
            new HoloMapLayer("catalog_overlay", "Catalog Overlay", "Shows cataloged bodies, missing discovery classes, and atlas completion.")
    );

    private static final List<MissionContract> MISSION_CONTRACTS = List.of(
            new MissionContract("first_survey_hop", "Prepare First Survey Hop", "item:fuel_canister"),
            new MissionContract("first_survey_circuit", "Complete First Survey Circuit", "route:near_sector_01_survey_hop"),
            new MissionContract("catalog_local_bodies", "Catalog Local Bodies", "discovery:barren_moon_kg_01a"),
            new MissionContract("recover_orbital_wreck", "Recover Orbital Wreck", "salvage:derelict_relay_osprey"),
            new MissionContract("establish_remote_depot", "Establish Remote Depot", "depot:cinder_ring_remote_depot"),
            new MissionContract("restore_survey_array", "Restore Galactic Survey Array", "block:survey_array_console")
    );

    private GalacticSurveySystemIntegrationContracts() {
    }

    public static List<TerminalPage> terminalPages() {
        return TERMINAL_PAGES;
    }

    public static List<LensProfile> lensProfiles() {
        return LENS_PROFILES;
    }

    public static List<HoloMapLayer> holoMapLayers() {
        return HOLOMAP_LAYERS;
    }

    public static List<MissionContract> missionContracts() {
        return MISSION_CONTRACTS;
    }

    public static Map<String, Object> adapterManifest() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", "echo.galactic_survey.system_integrations.v1");
        result.put("moduleId", GalacticSurveyRuntimeContracts.MODULE_ID);
        result.put("terminalPages", TERMINAL_PAGES.stream().map(TerminalPage::asMap).toList());
        result.put("lensProfiles", LENS_PROFILES.stream().map(LensProfile::asMap).toList());
        result.put("holoMapLayers", HOLOMAP_LAYERS.stream().map(HoloMapLayer::asMap).toList());
        result.put("missionContracts", MISSION_CONTRACTS.stream().map(MissionContract::asMap).toList());
        return Map.copyOf(result);
    }
}

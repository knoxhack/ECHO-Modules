package com.knoxhack.echoskyrelayprotocol.runtime;

import com.knoxhack.echoskyrelayprotocol.contract.SkyRelayRuntimeContracts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkyRelayProgressionRuntime {
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
            new RouteStep("wake_at_core", "awakening", "Wake beside the Damaged Relay Core.", "block_seen:damaged_relay_core"),
            new RouteStep("open_terminal", "awakening", "Open Terminal and read relay status.", "terminal_page:relay_status"),
            new RouteStep("scan_relay_core", "awakening", "Scan the Damaged Relay Core with Lens.", "lens_scan:damaged_relay_core"),
            new RouteStep("repair_hand_crank", "power_critical", "Repair hand-crank power path.", "power:hand_crank_online"),
            new RouteStep("charge_small_battery", "power_critical", "Charge the first Small Battery Bank.", "power:small_battery_bank_stable"),
            new RouteStep("claim_anchor_key", "first_anchor", "Claim the Relay Anchor Key.", "item:relay_anchor_key"),
            new RouteStep("reveal_hydroponics", "first_anchor", "Reveal Hydroponics Deck on HoloMap.", "holomap:hydroponics_deck"),
            new RouteStep("attach_hydroponics", "first_anchor", "Attach Hydroponics Deck.", "fragment:hydroponics_deck"),
            new RouteStep("place_hydroponic_tray", "water_problem", "Place first hydroponic tray.", "external_block:hydroponic_tray"),
            new RouteStep("open_seed_capsule", "water_problem", "Open first recovered seed capsule.", "item:recovered_seed_capsule"),
            new RouteStep("receive_forecast", "storm_warning", "Receive first storm forecast.", "terminal_page:storm_forecast"),
            new RouteStep("build_basic_shelter", "storm_warning", "Build basic storm shelter.", "block:pressure_bulkhead")
    );

    private static final List<RouteStep> FIRST_TWO_HOURS = List.of(
            new RouteStep("stabilize_food", "water_problem", "Stabilize early food production.", "fragment:hydroponics_deck"),
            new RouteStep("build_condenser", "water_problem", "Build Atmospheric Condenser.", "block:atmospheric_condenser"),
            new RouteStep("attach_salvage_yard", "salvage_expansion", "Attach Aero Salvage Yard.", "fragment:aero_salvage_yard"),
            new RouteStep("process_relay_plates", "salvage_expansion", "Process scrap into Relay Alloy Plates.", "item:relay_alloy_plate"),
            new RouteStep("build_shield_pylon", "storm_warning", "Build Storm Shield Pylon.", "block:storm_shield_pylon"),
            new RouteStep("attach_solar_wing", "solar_recovery", "Attach Solar Wing.", "fragment:solar_wing"),
            new RouteStep("start_logistics_route", "network_routing", "Build first logistics route.", "integration:logistics_first_route"),
            new RouteStep("unlock_weather_mast", "weather_control", "Unlock Weather Mast.", "fragment:weather_mast"),
            new RouteStep("survive_severe_storm", "storm_warning", "Survive a severe storm.", "weather:severe_storm_survived"),
            new RouteStep("craft_platform_core", "machine_restoration", "Craft first Stabilized Platform Core.", "item:stabilized_platform_core")
    );

    private static final List<EndgameRequirement> SIGNAL_CROWN_REQUIREMENTS = List.of(
            new EndgameRequirement("stabilized_platform_cores", 5, "item:stabilized_platform_core"),
            new EndgameRequirement("relay_signal_array", 1, "block:relay_signal_array"),
            new EndgameRequirement("storm_shield_network", 1, "integration:shield_network_online"),
            new EndgameRequirement("automated_logistics_route", 1, "integration:logistics_automated_route"),
            new EndgameRequirement("orbital_alloy_components", 3, "item:orbital_alloy_scrap"),
            new EndgameRequirement("terminal_restoration_sequence", 1, "terminal:final_restoration_sequence")
    );

    private SkyRelayProgressionRuntime() {
    }

    public static List<RouteStep> first30Minutes() {
        return FIRST_30_MINUTES;
    }

    public static List<RouteStep> firstTwoHours() {
        return FIRST_TWO_HOURS;
    }

    public static List<EndgameRequirement> signalCrownRequirements() {
        return SIGNAL_CROWN_REQUIREMENTS;
    }

    public static Map<String, Object> adapterManifest() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", "echo.skyrelay.progression_runtime.v1");
        result.put("moduleId", SkyRelayRuntimeContracts.MODULE_ID);
        result.put("chapterIds", SkyRelayRuntimeContracts.CHAPTER_IDS);
        result.put("first30Minutes", FIRST_30_MINUTES.stream().map(RouteStep::asMap).toList());
        result.put("firstTwoHours", FIRST_TWO_HOURS.stream().map(RouteStep::asMap).toList());
        result.put("signalCrownRequirements", SIGNAL_CROWN_REQUIREMENTS.stream().map(EndgameRequirement::asMap).toList());
        return Map.copyOf(result);
    }
}

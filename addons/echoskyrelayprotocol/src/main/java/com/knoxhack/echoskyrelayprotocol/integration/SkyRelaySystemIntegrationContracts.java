package com.knoxhack.echoskyrelayprotocol.integration;

import com.knoxhack.echoskyrelayprotocol.contract.SkyRelayRuntimeContracts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkyRelaySystemIntegrationContracts {
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

    public record WeatherRoute(String id, String event, String output) {
        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("event", event);
            result.put("output", output);
            return Map.copyOf(result);
        }
    }

    public record RecoveryBinding(String id, String trigger, String targetBlock) {
        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("trigger", trigger);
            result.put("targetBlock", targetBlock);
            return Map.copyOf(result);
        }
    }

    private static final List<TerminalPage> TERMINAL_PAGES = List.of(
            new TerminalPage("relay_status", "Relay Status", "Shows power stability, attached fragments, shield coverage, and next repair."),
            new TerminalPage("mission_log", "Mission Log", "Tracks Sky Relay chapters and route objectives."),
            new TerminalPage("storm_forecast", "Storm Forecast", "Shows incoming storm windows, exposure risk, and storm output opportunities."),
            new TerminalPage("fragment_registry", "Fragment Registry", "Lists scanned fragments, anchor gates, docking status, and HoloMap links.")
    );

    private static final List<LensProfile> LENS_PROFILES = List.of(
            new LensProfile("damaged_relay_core", "damaged_relay_core", "relay_status and operator_badge"),
            new LensProfile("relay_anchor_node", "relay_anchor_node", "anchor validation state"),
            new LensProfile("aero_salvage_crate", "aero_salvage_crate", "salvage table and fragment chart hints"),
            new LensProfile("storm_device", "storm_shield_pylon", "storm output and shield diagnostics"),
            new LensProfile("locked_fragment", "fragment_docking_clamp", "fragment access cipher requirement")
    );

    private static final List<HoloMapLayer> HOLOMAP_LAYERS = List.of(
            new HoloMapLayer("nearby_fragments", "Nearby Fragments", "Shows scanned drift platforms and anchor candidates."),
            new HoloMapLayer("power_grid", "Power Grid", "Shows generators, batteries, substations, and brownout risk."),
            new HoloMapLayer("shield_coverage", "Shield Coverage", "Shows protected shelter zones and exposed machinery."),
            new HoloMapLayer("logistics_routes", "Logistics Routes", "Shows drone routes, supply requests, and reward relays.")
    );

    private static final List<WeatherRoute> WEATHER_ROUTES = List.of(
            new WeatherRoute("incoming_storm_warning", "storm_warning", "terminal_page:storm_forecast"),
            new WeatherRoute("collector_output_window", "severe_storm", "item:static_filament"),
            new WeatherRoute("shield_failure_risk", "brownout_during_storm", "holomap_layer:shield_coverage"),
            new WeatherRoute("orbital_static_front", "endgame_static_front", "item:echo_crystal_charge")
    );

    private static final List<RecoveryBinding> RECOVERY_BINDINGS = List.of(
            new RecoveryBinding("void_recovery_cache", "void_death", "void_recovery_cache")
    );

    private SkyRelaySystemIntegrationContracts() {
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

    public static List<WeatherRoute> weatherRoutes() {
        return WEATHER_ROUTES;
    }

    public static List<RecoveryBinding> recoveryBindings() {
        return RECOVERY_BINDINGS;
    }

    public static Map<String, Object> adapterManifest() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", "echo.skyrelay.system_integrations.v1");
        result.put("moduleId", SkyRelayRuntimeContracts.MODULE_ID);
        result.put("terminalPages", TERMINAL_PAGES.stream().map(TerminalPage::asMap).toList());
        result.put("lensProfiles", LENS_PROFILES.stream().map(LensProfile::asMap).toList());
        result.put("holoMapLayers", HOLOMAP_LAYERS.stream().map(HoloMapLayer::asMap).toList());
        result.put("weatherRoutes", WEATHER_ROUTES.stream().map(WeatherRoute::asMap).toList());
        result.put("recoveryBindings", RECOVERY_BINDINGS.stream().map(RecoveryBinding::asMap).toList());
        return Map.copyOf(result);
    }

}

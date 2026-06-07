package com.knoxhack.echoashfallprotocol.nativebridge;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketConsumerBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketOrchestrator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeRuntimePacketBindings {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeRuntimePacketBindings() {
    }

    public static Map<String, Object> describe(
            Map<String, Object> firstJoinExecution,
            Map<String, Object> recoveryHandoff,
            Map<String, Object> uiHudHandoff,
            Map<String, Object> routeHazardHandoff) {
        Map<String, Object> packetReport = new EchoNativeRuntimePacketBridge(MODULE_ID)
                .packet(
                        "echoashfallprotocol:first_join_player_recovery_runtime_packet",
                        "player_recovery",
                        "echoashfallprotocol:first_join_player_recovery_runtime_target",
                        childMap(firstJoinExecution, "firstJoinRuntimeTarget"),
                        List.of(
                                "echoashfallprotocol:native_player_bridge",
                                "echoashfallprotocol:native_world_structure_bridge",
                                "echoashfallprotocol:native_respawn_bridge",
                                "echoashfallprotocol:native_advancement_bridge",
                                "echorecovery:field_cache_service",
                                "echoholomap:map_state_service",
                                "echoscreencore:welcome_surface",
                                "echohudcore:mission_tracker"))
                .packet(
                        "echoashfallprotocol:first_join_recovery_navigation_runtime_packet",
                        "holomap_lens_codex_wiki",
                        "echoashfallprotocol:first_join_recovery_navigation_runtime_target",
                        childMap(recoveryHandoff, "recoveryNavigationRuntimeTarget"),
                        List.of(
                                "echorecovery:field_cache_service",
                                "echoholomap:opening_recovery_layers",
                                "echoashfallprotocol:native_player_recovery_repair_bridge"))
                .packet(
                        "echoashfallprotocol:first_join_ui_hud_runtime_packet",
                        "ui_hud_screen_safe",
                        "echoashfallprotocol:first_join_ui_hud_runtime_target",
                        childMap(uiHudHandoff, "uiHudRuntimeTarget"),
                        List.of(
                                "echohudcore:mission_tracker",
                                "echohudcore:hazard_readout",
                                "echoscreencore:welcome_surface",
                                "echoterminal:first_ten_minutes_card",
                                "echowiki:ashfall",
                                "echolens:opening_route_scan",
                                "echoholomap:opening_recovery_layers",
                                "echocodexcore:opening_entries"))
                .packet(
                        "echoashfallprotocol:route_hazard_atmosphere_runtime_packet",
                        "weather_sound_atmosphere",
                        "echoashfallprotocol:route_hazard_atmosphere_runtime_target",
                        childMap(routeHazardHandoff, "atmosphereRuntimeTarget"),
                        List.of(
                                "echoweathercore:weather_state",
                                "echosoundcore:ambience",
                                "echoatmospherecore:storm_visibility",
                                "echoatmospherecore:ambient_particles",
                                "echohudcore:hazard_readout"))
                .describe("echoashfallprotocol:agent3_runtime_packet_bindings");
        Map<String, Object> report = new LinkedHashMap<>(packetReport);
        List<Map<String, Object>> consumerApplications = consumerApplications(packetReport);
        ArrayList<String> diagnostics = new ArrayList<>(diagnostics(packetReport, consumerApplications));
        report.put("consumerApplications", consumerApplications);
        report.put("consumerApplicationCount", consumerApplications.size());
        report.put("consumerApplicationStatus", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        Map<String, Object> orchestration = new EchoNativeRuntimePacketOrchestrator(MODULE_ID).orchestrate(
                "echoashfallprotocol:agent3_runtime_packet_orchestration",
                report);
        if (!"PASS".equals(orchestration.get("status"))) {
            diagnostics.add("Runtime packet orchestration failed.");
        }
        report.put("runtimePacketOrchestration", orchestration);
        report.put("runtimePacketOrchestrationStatus", orchestration.get("status"));
        report.put("runtimePacketOrchestrationConsumerCount", orchestration.get("orchestratedConsumerCount"));
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared and orchestrated Agent 3 runtime packets for player/recovery, Recovery, HoloMap, UI/HUD, Terminal, Wiki, Lens, Codex, WeatherCore, SoundCore, and AtmosphereCore surfaces without claiming live host mutation."
                : "AdapterCore runtime packet binding, consumer application, or orchestration is missing required evidence.");
        return Map.copyOf(report);
    }

    private static List<Map<String, Object>> consumerApplications(Map<String, Object> packetReport) {
        return List.of(
                consumer("echoashfallprotocol", packetReport,
                        "echoashfallprotocol:native_player_bridge",
                        "echoashfallprotocol:native_world_structure_bridge",
                        "echoashfallprotocol:native_respawn_bridge",
                        "echoashfallprotocol:native_advancement_bridge",
                        "echoashfallprotocol:native_player_recovery_repair_bridge"),
                consumer("echorecovery", packetReport,
                        "echorecovery:field_cache_service"),
                consumer("echoholomap", packetReport,
                        "echoholomap:map_state_service",
                        "echoholomap:opening_recovery_layers"),
                consumer("echoscreencore", packetReport,
                        "echoscreencore:welcome_surface"),
                consumer("echohudcore", packetReport,
                        "echohudcore:mission_tracker",
                        "echohudcore:hazard_readout"),
                consumer("echoterminal", packetReport,
                        "echoterminal:first_ten_minutes_card"),
                consumer("echowiki", packetReport,
                        "echowiki:ashfall"),
                consumer("echolens", packetReport,
                        "echolens:opening_route_scan"),
                consumer("echocodexcore", packetReport,
                        "echocodexcore:opening_entries"),
                consumer("echoweathercore", packetReport,
                        "echoweathercore:weather_state"),
                consumer("echosoundcore", packetReport,
                        "echosoundcore:ambience"),
                consumer("echoatmospherecore", packetReport,
                        "echoatmospherecore:storm_visibility",
                        "echoatmospherecore:ambient_particles")
        );
    }

    private static Map<String, Object> consumer(String moduleId, Map<String, Object> packetReport, String... consumers) {
        return new EchoNativeRuntimePacketConsumerBridge(moduleId).consume(
                moduleId + ":ashfall_runtime_packet_consumers",
                packetReport,
                List.of(consumers));
    }

    private static List<String> diagnostics(
            Map<String, Object> packetReport,
            List<Map<String, Object>> consumerApplications) {
        java.util.ArrayList<String> diagnostics = new java.util.ArrayList<>();
        if (!"PASS".equals(packetReport.get("status"))) {
            diagnostics.add("Runtime packet binding report failed.");
        }
        for (Map<String, Object> application : consumerApplications) {
            if (!"PASS".equals(application.get("status"))) {
                diagnostics.add("Consumer application " + application.get("id") + " failed.");
            }
            if (Boolean.TRUE.equals(application.get("minecraftRuntimeAccessed"))) {
                diagnostics.add("Consumer application " + application.get("id") + " accessed Minecraft runtime.");
            }
        }
        return List.copyOf(diagnostics);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> childMap(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}

package com.knoxhack.echoashfallprotocol.nativebridge;

import com.knoxhack.echo.adaptercore.EchoNativeCommandBridge;
import com.knoxhack.echo.adaptercore.EchoNativeSurfaceHostInvocationContract;
import com.knoxhack.echo.adaptercore.EchoNativeSurfaceHostCallQueue;
import com.knoxhack.echo.adaptercore.EchoNativeUiHudStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeUiHudRuntimeTarget;

import java.util.List;
import java.util.Map;

public final class AshfallNativeUiHudHandoff {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeUiHudHandoff() {
    }

    public static Map<String, Object> describe(Map<String, Object> firstJoinProfile,
                                               Map<String, Object> uiHudConsumers) {
        Map<String, Object> screenSafeSurfaces = childMap(firstJoinProfile, "screenSafeSurfaces");
        Map<String, Object> recoveryPlan = childMap(firstJoinProfile, "recoveryPlan");
        Map<String, Object> atmosphereHooks = childMap(firstJoinProfile, "atmosphereHooks");

        EchoNativeCommandBridge commandBridge = new EchoNativeCommandBridge(MODULE_ID)
                .command(10, "ui_hud_screen_safe", "hud.publish_mission_tracker_line",
                        "echohudcore:mission_tracker",
                        "AshfallNativeFirstJoinProfile.screenSafeSurfaces.hudMissionLine",
                        map("line", screenSafeSurfaces.getOrDefault("hudMissionLine", "Place an Ash Campfire near the crash site"),
                                "anchor", screenSafeSurfaces.getOrDefault("notificationAnchor", "top_left_safe_area"),
                                "sourceProfile", valueOrDefault(firstJoinProfile, "id", "echoashfallprotocol:first_join_crash_recovery")))
                .command(20, "ui_hud_screen_safe", "hud.publish_hazard_weather_readout",
                        "echohudcore:hazard_readout",
                        "AshfallNativeFirstJoinProfile screen-safe hazard line and route hazards",
                        map("line", screenSafeSurfaces.getOrDefault("hudHazardLine", "AIR stable; hazards marked."),
                                "weatherReadout", atmosphereHooks.getOrDefault("defaultWeatherReadout", "CLEAR"),
                                "routeHazards", atmosphereHooks.getOrDefault("routeHazards", List.of()),
                                "screenSafe", true))
                .command(30, "ui_hud_screen_safe", "screen.dispatch_welcome_onboarding_surface",
                        "echoscreencore:welcome_surface",
                        "WelcomeScreenPacket and native screen-safe onboarding target",
                        map("screen", screenSafeSurfaces.getOrDefault("welcomeScreen", "echoashfallprotocol:welcome_screen"),
                                "packet", "echoashfallprotocol:welcome_screen",
                                "packetKind", "CLIENTBOUND_SYNC",
                                "screenSafe", true))
                .command(40, "ui_hud_screen_safe", "terminal.publish_first_ten_minutes_link",
                        "echoterminal:first_ten_minutes_card",
                        "Terminal first-ten-minutes card surfaced through AdapterCore",
                        map("card", screenSafeSurfaces.getOrDefault("terminalCard", "ashfall:first_ten_minutes"),
                                "condition", "module_loaded:echoterminal",
                                "standaloneCopy", false))
                .command(50, "ui_hud_screen_safe", "wiki.publish_ashfall_guide_link",
                        "echowiki:ashfall",
                        "Wiki guide link surfaced through AdapterCore",
                        map("guide", screenSafeSurfaces.getOrDefault("wikiGuide", "echowiki:ashfall"),
                                "condition", "module_loaded:echowiki",
                                "standaloneCopy", false))
                .command(60, "holomap_lens_codex_wiki", "lens.publish_opening_route_scan",
                        "echolens:opening_route_scan",
                        "Lens route scan profile surfaced through AdapterCore",
                        map("profiles", recoveryPlan.getOrDefault("lensProfiles", List.of()),
                                "source", "first_join_recovery_profile"))
                .command(70, "holomap_lens_codex_wiki", "holomap.publish_opening_recovery_layers",
                        "echoholomap:opening_recovery_layers",
                        "HoloMap opening route and recovery layers surfaced through AdapterCore",
                        map("layers", recoveryPlan.getOrDefault("mapLayerHints", List.of()),
                                "fieldCacheVisibility", true))
                .command(80, "holomap_lens_codex_wiki", "codex.publish_opening_route_entries",
                        "echocodexcore:opening_entries",
                        "Codex opening entries surfaced through AdapterCore",
                        map("entries", recoveryPlan.getOrDefault("codexEntries", List.of()),
                                "standaloneCopy", false));

        Map<String, Object> report = commandBridge.describe(
                "echoashfallprotocol:first_join_ui_hud_handoff",
                "AdapterCore UI HUD screen-safe first-join command handoff",
                valueOrDefault(firstJoinProfile, "id", "echoashfallprotocol:first_join_crash_recovery"),
                valueOrDefault(uiHudConsumers, "id", "echoashfallprotocol:first_join_ui_hud_consumers"),
                requiredOperationIds(),
                pendingConcreteRuntimeBridges(),
                "AdapterCore command handoff publishes the native HUD mission tracker line, hazard/weather readout, welcome/onboarding surface, and Terminal/Wiki/Lens/HoloMap/Codex links without standalone UI copies.",
                "AdapterCore UI/HUD handoff is missing required screen-safe commands.");
        report.put("surface", "ui_hud_screen_safe");
        report.put("surfaces", List.of("ui_hud_screen_safe", "holomap_lens_codex_wiki"));
        report.put("consumerEvidence", valueOrDefault(uiHudConsumers, "id",
                "echoashfallprotocol:first_join_ui_hud_consumers"));
        report.put("appliedUiHudState", new EchoNativeUiHudStateBridge(MODULE_ID).apply(
                "echoashfallprotocol:first_join_ui_hud_state_application",
                childList(report, "commands")));
        Map<String, Object> uiHudRuntimeTarget = new EchoNativeUiHudRuntimeTarget(MODULE_ID).execute(
                "echoashfallprotocol:first_join_ui_hud_runtime_target",
                childList(report, "commands"));
        report.put("uiHudRuntimeTarget", uiHudRuntimeTarget);
        Map<String, Object> uiHudHostInvocationContract = new EchoNativeSurfaceHostInvocationContract(MODULE_ID).prepare(
                "echoashfallprotocol:first_join_ui_hud_host_invocation_contract",
                "adaptercore.ui_hud_surface_host_invocation",
                "ui_hud_screen_safe",
                childList(report, "commands"),
                uiHudRuntimeTarget,
                uiHudHostRequirements());
        report.put("uiHudHostInvocationContract", uiHudHostInvocationContract);
        report.put("uiHudHostCallQueue", new EchoNativeSurfaceHostCallQueue(MODULE_ID).prepare(
                "echoashfallprotocol:first_join_ui_hud_host_call_queue",
                uiHudHostInvocationContract));
        return report;
    }

    private static List<String> requiredOperationIds() {
        return List.of(
                "hud.publish_mission_tracker_line",
                "hud.publish_hazard_weather_readout",
                "screen.dispatch_welcome_onboarding_surface",
                "terminal.publish_first_ten_minutes_link",
                "wiki.publish_ashfall_guide_link",
                "lens.publish_opening_route_scan",
                "holomap.publish_opening_recovery_layers",
                "codex.publish_opening_route_entries"
        );
    }

    private static List<String> pendingConcreteRuntimeBridges() {
        return List.of(
                "native_hud_notification_bridge",
                "native_screen_packet_bridge",
                "native_terminal_surface_bridge",
                "native_wiki_surface_bridge",
                "native_lens_surface_bridge",
                "native_holomap_surface_bridge",
                "native_codex_surface_bridge"
        );
    }

    private static List<EchoNativeSurfaceHostInvocationContract.HostRequirement> uiHudHostRequirements() {
        return List.of(
                hostRequirement("native_hud_mission_tracker_adapter", "echohudcore:mission_tracker",
                        "hudcore.publish_mission_tracker", "missionTracker"),
                hostRequirement("native_hud_hazard_readout_adapter", "echohudcore:hazard_readout",
                        "hudcore.publish_hazard_readout", "hazardReadout"),
                hostRequirement("native_welcome_surface_adapter", "echoscreencore:welcome_surface",
                        "screencore.dispatch_welcome_surface", "welcomeSurface"),
                hostRequirement("native_terminal_surface_adapter", "echoterminal:first_ten_minutes_card",
                        "terminal.publish_first_ten_minutes_card", "terminalLink"),
                hostRequirement("native_wiki_surface_adapter", "echowiki:ashfall",
                        "wiki.publish_guide_link", "wikiLink"),
                hostRequirement("native_lens_surface_adapter", "echolens:opening_route_scan",
                        "lens.publish_scan_profiles", "lensProfiles"),
                hostRequirement("native_holomap_surface_adapter", "echoholomap:opening_recovery_layers",
                        "holomap.publish_recovery_layers", "holomapLayers"),
                hostRequirement("native_codex_surface_adapter", "echocodexcore:opening_entries",
                        "codex.publish_entries", "codexEntries")
        );
    }

    private static EchoNativeSurfaceHostInvocationContract.HostRequirement hostRequirement(
            String invocationId,
            String targetBridgeId,
            String hostSurfaceApi,
            String payloadSource) {
        return new EchoNativeSurfaceHostInvocationContract.HostRequirement(
                invocationId,
                targetBridgeId,
                hostSurfaceApi,
                payloadSource);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> childMap(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> childList(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static Object valueOrDefault(Map<String, Object> map, String key, Object fallback) {
        return map == null ? fallback : map.getOrDefault(key, fallback);
    }

    private static Map<String, Object> map(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("map entries must be key/value pairs");
        }
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], entries[index + 1]);
        }
        return result;
    }
}

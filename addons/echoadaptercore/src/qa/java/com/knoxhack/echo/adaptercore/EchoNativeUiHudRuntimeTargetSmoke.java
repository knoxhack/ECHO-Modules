package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeUiHudRuntimeTargetSmoke {
    private static final String MODULE_ID = "echoashfallprotocol";

    private EchoNativeUiHudRuntimeTargetSmoke() {
    }

    public static void main(String[] args) {
        Map<String, Object> report = capture();
        if (!Boolean.TRUE.equals(report.get("passed"))) {
            throw new AssertionError("EchoNativeUiHudRuntimeTargetSmoke failed: " + report);
        }
        System.out.println("echo native ui hud runtime target smoke PASS commands=8");
    }

    public static Map<String, Object> capture() {
        Map<String, Object> accepted = execute(commands());
        Map<String, Object> rejectedMissingHazard = execute(without("hud.publish_hazard_weather_readout"));
        Map<String, Object> rejectedStandaloneCopy = execute(withTerminalStandaloneCopy());
        Map<String, Object> rejectedUnexecutedCommands = execute(pendingCommands());
        Map<String, Object> snapshot = object(accepted.get("runtimeSnapshot"));
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && Boolean.TRUE.equals(accepted.get("serviceCodeExecuted"))
                && "ui_hud_host_call_queue_replay:prepared:8".equals(accepted.get("effect"))
                && Integer.valueOf(8).equals(accepted.get("preparedCommandCount"))
                && Integer.valueOf(0).equals(accepted.get("executedCommandCount"))
                && Integer.valueOf(8).equals(accepted.get("mutatingOperationCount"))
                && "Place an Ash Campfire near the crash site".equals(snapshot.get("missionTrackerLine"))
                && "AIR stable; hazards marked.".equals(snapshot.get("hazardReadoutLine"))
                && "echoashfallprotocol:welcome_screen".equals(snapshot.get("welcomeScreen"))
                && "echoashfallprotocol:welcome_screen".equals(snapshot.get("welcomePacket"))
                && "ashfall:first_ten_minutes".equals(snapshot.get("terminalCard"))
                && "echowiki:ashfall".equals(snapshot.get("wikiGuide"))
                && Integer.valueOf(1).equals(snapshot.get("lensProfileCount"))
                && Integer.valueOf(2).equals(snapshot.get("holomapLayerCount"))
                && Integer.valueOf(1).equals(snapshot.get("codexEntryCount"))
                && Boolean.FALSE.equals(accepted.get("minecraftRuntimeAccessed"))
                && rejectsWithoutServiceExecution(rejectedMissingHazard)
                && rejectsWithoutServiceExecution(rejectedStandaloneCopy)
                && rejectsWithoutServiceExecution(rejectedUnexecutedCommands);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("nativeUiHudRuntimeTargetSmokeClass",
                EchoNativeUiHudRuntimeTargetSmoke.class.getSimpleName());
        report.put("adapterCoreContract", "adaptercore:agent5_ui_hud_host_call_queue_replay");
        report.put("accepted", accepted);
        report.put("rejectedMissingHazard", rejectedMissingHazard);
        report.put("rejectedStandaloneCopy", rejectedStandaloneCopy);
        report.put("rejectedUnexecutedCommands", rejectedUnexecutedCommands);
        report.put("requiredOperationIds", requiredOperationIds());
        report.put("adapterCoreBridge", true);
        report.put("serviceCodeExecuted", passed);
        report.put("passed", passed);
        return Map.copyOf(report);
    }

    private static Map<String, Object> execute(List<Map<String, Object>> commands) {
        return new EchoNativeUiHudRuntimeTarget(MODULE_ID).execute(
                "echoashfallprotocol:first_join_ui_hud_runtime_target_native_smoke",
                commands
        );
    }

    private static boolean rejectsWithoutServiceExecution(Map<String, Object> report) {
        return Boolean.FALSE.equals(report.get("accepted"))
                && Boolean.FALSE.equals(report.get("serviceCodeExecuted"));
    }

    private static List<Map<String, Object>> commands() {
        return List.of(
                command(10, "ui_hud_screen_safe", "hud.publish_mission_tracker_line",
                        "echohudcore:mission_tracker",
                        map("line", "Place an Ash Campfire near the crash site",
                                "anchor", "top_left_safe_area",
                                "sourceProfile", "echoashfallprotocol:first_join_crash_recovery")),
                command(20, "ui_hud_screen_safe", "hud.publish_hazard_weather_readout",
                        "echohudcore:hazard_readout",
                        map("line", "AIR stable; hazards marked.",
                                "weatherReadout", "CLEAR",
                                "routeHazards", List.of("ashfall", "low_visibility"),
                                "screenSafe", true)),
                command(30, "ui_hud_screen_safe", "screen.dispatch_welcome_onboarding_surface",
                        "echoscreencore:welcome_surface",
                        map("screen", "echoashfallprotocol:welcome_screen",
                                "packet", "echoashfallprotocol:welcome_screen",
                                "packetKind", "CLIENTBOUND_SYNC",
                                "screenSafe", true)),
                command(40, "ui_hud_screen_safe", "terminal.publish_first_ten_minutes_link",
                        "echoterminal:first_ten_minutes_card",
                        map("card", "ashfall:first_ten_minutes",
                                "condition", "module_loaded:echoterminal",
                                "standaloneCopy", false)),
                command(50, "ui_hud_screen_safe", "wiki.publish_ashfall_guide_link",
                        "echowiki:ashfall",
                        map("guide", "echowiki:ashfall",
                                "condition", "module_loaded:echowiki",
                                "standaloneCopy", false)),
                command(60, "holomap_lens_codex_wiki", "lens.publish_opening_route_scan",
                        "echolens:opening_route_scan",
                        map("profiles", List.of("echoashfallprotocol:ashfall_major_route_scans"),
                                "source", "first_join_recovery_profile")),
                command(70, "holomap_lens_codex_wiki", "holomap.publish_opening_recovery_layers",
                        "echoholomap:opening_recovery_layers",
                        map("layers", List.of(
                                        "echoashfallprotocol:first_month_field_intel",
                                        "echoashfallprotocol:first_major_route"),
                                "fieldCacheVisibility", true)),
                command(80, "holomap_lens_codex_wiki", "codex.publish_opening_route_entries",
                        "echocodexcore:opening_entries",
                        map("entries", List.of("echoashfallprotocol:hazard_route_prep"),
                                "standaloneCopy", false))
        );
    }

    private static List<Map<String, Object>> without(String operationId) {
        return commands().stream()
                .filter(command -> !operationId.equals(command.get("operationId")))
                .toList();
    }

    private static List<Map<String, Object>> withTerminalStandaloneCopy() {
        List<Map<String, Object>> mutated = new ArrayList<>();
        for (Map<String, Object> command : commands()) {
            if ("terminal.publish_first_ten_minutes_link".equals(command.get("operationId"))) {
                Map<String, Object> copy = new LinkedHashMap<>(command);
                Map<String, Object> payload = new LinkedHashMap<>(object(command.get("payload")));
                payload.put("standaloneCopy", true);
                copy.put("payload", Map.copyOf(payload));
                mutated.add(Map.copyOf(copy));
            } else {
                mutated.add(command);
            }
        }
        return List.copyOf(mutated);
    }

    private static List<Map<String, Object>> pendingCommands() {
        List<Map<String, Object>> pending = new ArrayList<>();
        for (Map<String, Object> command : commands()) {
            Map<String, Object> copy = new LinkedHashMap<>(command);
            copy.put("status", "pending_runtime_bridge");
            pending.add(Map.copyOf(copy));
        }
        return List.copyOf(pending);
    }

    private static Map<String, Object> command(
            int order,
            String targetSurface,
            String operationId,
            String targetBridge,
            Map<String, Object> payload
    ) {
        return Map.of(
                "order", order,
                "targetSurface", targetSurface,
                "operationId", operationId,
                "targetBridge", targetBridge,
                "payload", payload,
                "status", "prepared_as_adaptercore_command"
        );
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

    private static Map<String, Object> map(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("map entries must be key/value pairs");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], entries[index + 1]);
        }
        return Map.copyOf(values);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}

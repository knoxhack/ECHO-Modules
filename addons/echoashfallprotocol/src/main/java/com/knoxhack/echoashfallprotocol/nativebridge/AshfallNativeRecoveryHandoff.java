package com.knoxhack.echoashfallprotocol.nativebridge;

import com.knoxhack.echo.adaptercore.EchoNativeRecoveryNavigationStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRecoveryNavigationHostInvocationContract;
import com.knoxhack.echo.adaptercore.EchoNativeRecoveryNavigationRuntimeTarget;
import com.knoxhack.echo.adaptercore.EchoNativeSurfaceHostCallQueue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeRecoveryHandoff {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeRecoveryHandoff() {
    }

    public static Map<String, Object> describe(Map<String, Object> firstJoinProfile, Map<String, Object> firstJoinExecution) {
        Map<String, Object> recoveryPlan = childMap(firstJoinProfile, "recoveryPlan");
        Map<String, Object> recoveryCommand = command(firstJoinExecution, "recovery.publish_drop_pod_field_cache_context");
        Map<String, Object> holomapCommand = command(firstJoinExecution, "holomap.publish_recovery_route_markers");
        Map<String, Object> repairCommand = command(firstJoinExecution, "repair.rescue_underground_or_missing_respawn");

        List<Map<String, Object>> handoffs = List.of(
                handoff("echorecovery:field_cache_service", "death_cache_grave_handoff",
                        "Receives the first drop-pod and crash-cache context for future death/cache/grave recovery.",
                        "player_recovery", recoveryCommand, map(
                                "deathRecoveryHandoff", recoveryPlan.getOrDefault("deathRecoveryHandoff", "echorecovery:field_cache_service"),
                                "routeObjective", recoveryPlan.getOrDefault("routeObjective", "ashfall:recover_crash_cache"),
                                "recoveryCompassVisible", true,
                                "graveContextSource", "first_join_drop_pod")),
                handoff("echoholomap:opening_recovery_layers", "recovery_compass_field_cache_visibility",
                        "Receives map layers for recovery compass hints, field-cache visibility, and opening route markers.",
                        "holomap_lens_codex_wiki", holomapCommand, map(
                                "layers", recoveryPlan.getOrDefault("mapLayerHints", List.of()),
                                "fieldCacheVisibility", true,
                                "compassRouteSource", "ashfall:recover_crash_cache")),
                handoff("echoashfallprotocol:existing_player_repair", "existing_player_repair_path",
                        "Receives the native repair path for unsafe underground pods, missing respawn, and Terminal remote reissue.",
                        "player_recovery", repairCommand, map(
                                "repairs", childMap(repairCommand, "payload").getOrDefault("repairs", List.of()),
                                "safeDuringActivation", true))
        );

        List<String> diagnostics = validate(handoffs);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:first_join_recovery_handoff");
        report.put("moduleId", MODULE_ID);
        report.put("adapterCoreBridge", true);
        report.put("bridge", "adaptercore.native_command");
        report.put("implementationTarget", "AdapterCore Recovery and HoloMap native handoff");
        report.put("sourceProfile", valueOrDefault(firstJoinProfile, "id", "echoashfallprotocol:first_join_crash_recovery"));
        report.put("sourceExecution", valueOrDefault(firstJoinExecution, "id", "echoashfallprotocol:first_join_crash_recovery_execution"));
        report.put("surface", "holomap_lens_codex_wiki");
        report.put("surfaces", List.of("player_recovery", "holomap_lens_codex_wiki"));
        report.put("handoffCount", handoffs.size());
        report.put("handoffs", handoffs);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("executionMode", "adaptercore_jdk_only_recovery_handoff");
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRegistryMutated", false);
        report.put("liveRuntimeMutation", false);
        report.put("appliedRecoveryNavigationState", new EchoNativeRecoveryNavigationStateBridge(MODULE_ID).apply(
                "echoashfallprotocol:first_join_recovery_navigation_state_application",
                handoffs));
        Map<String, Object> recoveryNavigationRuntimeTarget = new EchoNativeRecoveryNavigationRuntimeTarget(MODULE_ID).execute(
                "echoashfallprotocol:first_join_recovery_navigation_runtime_target",
                handoffs);
        report.put("recoveryNavigationRuntimeTarget", recoveryNavigationRuntimeTarget);
        Map<String, Object> recoveryNavigationHostInvocationContract =
                new EchoNativeRecoveryNavigationHostInvocationContract(MODULE_ID).prepare(
                        "echoashfallprotocol:first_join_recovery_navigation_host_invocation_contract",
                        handoffs,
                        recoveryNavigationRuntimeTarget);
        report.put("recoveryNavigationHostInvocationContract", recoveryNavigationHostInvocationContract);
        report.put("recoveryNavigationHostCallQueue", new EchoNativeSurfaceHostCallQueue(MODULE_ID).prepare(
                "echoashfallprotocol:first_join_recovery_navigation_host_call_queue",
                recoveryNavigationHostInvocationContract));
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore recovery handoff exposes death/cache/grave context, recovery compass and field-cache visibility, HoloMap route layers, and existing-player repair commands from the first-join execution."
                : "AdapterCore recovery handoff is missing required Recovery or HoloMap command evidence.");
        return report;
    }

    private static Map<String, Object> handoff(String id, String kind, String summary, String targetSurface,
                                               Map<String, Object> command, Map<String, Object> payload) {
        Map<String, Object> handoff = new LinkedHashMap<>();
        handoff.put("id", id);
        handoff.put("kind", kind);
        handoff.put("summary", summary);
        handoff.put("targetSurface", targetSurface);
        handoff.put("sourceCommand", command.getOrDefault("operationId", ""));
        handoff.put("targetBridge", command.getOrDefault("targetBridge", ""));
        handoff.put("adapterCoreConsumer", true);
        handoff.put("adapterCoreCommandBacked", "prepared_as_adaptercore_command".equals(command.get("status")));
        handoff.put("minecraftRuntimeAccessed", false);
        handoff.put("payload", payload);
        return handoff;
    }

    private static List<String> validate(List<Map<String, Object>> handoffs) {
        List<String> diagnostics = new ArrayList<>();
        requireHandoff(handoffs, "echorecovery:field_cache_service", diagnostics);
        requireHandoff(handoffs, "echoholomap:opening_recovery_layers", diagnostics);
        requireHandoff(handoffs, "echoashfallprotocol:existing_player_repair", diagnostics);
        for (Map<String, Object> handoff : handoffs) {
            if (!Boolean.TRUE.equals(handoff.get("adapterCoreConsumer"))) {
                diagnostics.add("Handoff " + handoff.get("id") + " is not AdapterCore-backed.");
            }
            if (!Boolean.TRUE.equals(handoff.get("adapterCoreCommandBacked"))) {
                diagnostics.add("Handoff " + handoff.get("id") + " does not reference a prepared AdapterCore command.");
            }
            if (Boolean.TRUE.equals(handoff.get("minecraftRuntimeAccessed"))) {
                diagnostics.add("Handoff " + handoff.get("id") + " accessed Minecraft runtime.");
            }
        }
        return List.copyOf(diagnostics);
    }

    private static void requireHandoff(List<Map<String, Object>> handoffs, String id, List<String> diagnostics) {
        boolean found = handoffs.stream().anyMatch(handoff -> id.equals(handoff.get("id")));
        if (!found) {
            diagnostics.add("Missing recovery handoff " + id + ".");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> command(Map<String, Object> execution, String operationId) {
        Object commands = execution == null ? null : execution.get("commands");
        if (commands instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> command && operationId.equals(command.get("operationId"))) {
                    return (Map<String, Object>) command;
                }
            }
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> childMap(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static Object valueOrDefault(Map<String, Object> map, String key, Object fallback) {
        return map == null ? fallback : map.getOrDefault(key, fallback);
    }

    private static Map<String, Object> map(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("map entries must be key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], entries[index + 1]);
        }
        return result;
    }
}

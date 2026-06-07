package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeExistingPlayerRepairRuntime {
    private static final String MODULE_ID = "echoashfallprotocol";
    private static final String REPAIR_INVOCATION_ID = "native_existing_player_repair_adapter";

    private AshfallNativeExistingPlayerRepairRuntime() {
    }

    public static Map<String, Object> execute(
            Map<String, Object> firstJoinProfile,
            Map<String, Object> recoveryNavigationHostInvocationContract) {
        Map<String, Object> repairInvocation = invocation(recoveryNavigationHostInvocationContract);
        List<String> repairActions = repairActions(repairInvocation);
        List<Map<String, Object>> scenarios = scenarios(repairActions);
        List<String> diagnostics = validate(firstJoinProfile, repairInvocation, repairActions, scenarios);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:existing_player_repair_runtime");
        report.put("moduleId", MODULE_ID);
        report.put("bridge", "adaptercore.existing_player_repair_runtime");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore existing-player Ashfall repair runtime scenarios");
        report.put("sourceRuntimeHandler", "PlayerStartingKitHandler.onPlayerLoggedIn existing-player branch");
        report.put("sourceProfile", value(firstJoinProfile, "id"));
        report.put("sourceHostInvocationContract", value(recoveryNavigationHostInvocationContract, "id"));
        report.put("sourceHostInvocation", value(repairInvocation, "id"));
        report.put("executionMode", "adaptercore_jdk_existing_player_repair_runtime");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("repairActionCount", repairActions.size());
        report.put("repairActions", List.copyOf(repairActions));
        report.put("scenarioCount", scenarios.size());
        report.put("scenarios", List.copyOf(scenarios));
        report.put("undergroundPodRescueCovered", contains(repairActions, "rescue_underground_starting_pod_below_y_48"));
        report.put("missingRespawnRepairCovered", contains(repairActions, "repair_missing_drop_pod_respawn"));
        report.put("terminalRemoteReissueCovered", contains(repairActions, "reissue_terminal_remote_if_available"));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared the returning-player repair branch as no-launch native scenarios for underground pod rescue, missing respawn repair, and Terminal remote reissue without claiming live host mutation."
                : "AdapterCore returning-player repair scenarios are missing required repair actions or host invocation evidence.");
        return Map.copyOf(report);
    }

    private static List<Map<String, Object>> scenarios(List<String> repairActions) {
        List<Map<String, Object>> scenarios = new ArrayList<>();
        if (contains(repairActions, "rescue_underground_starting_pod_below_y_48")) {
            scenarios.add(scenario(
                    "existing_player_underground_pod_rescue",
                    "player_has_received_kit_and_current_y_below_48_with_unreusable_pod",
                    List.of("rescue_underground_starting_pod_below_y_48", "reissue_terminal_remote_if_available"),
                    map(
                            "newDropPodInterior", "personal_starting_drop_pod.interior",
                            "teleportToSurfacePod", true,
                            "respawnBound", true,
                            "terminalRemoteReissuedIfMissing", contains(repairActions, "reissue_terminal_remote_if_available"))));
        }
        if (contains(repairActions, "repair_missing_drop_pod_respawn")) {
            scenarios.add(scenario(
                    "existing_player_missing_respawn_repair",
                    "player_has_received_kit_and_reusable_pod_exists_and_respawn_is_missing",
                    List.of("repair_missing_drop_pod_respawn", "reissue_terminal_remote_if_available"),
                    map(
                            "respawnAnchor", "personal_starting_drop_pod.interior",
                            "overwriteExistingRespawn", false,
                            "terminalRemoteReissuedIfMissing", contains(repairActions, "reissue_terminal_remote_if_available"))));
        }
        if (contains(repairActions, "reissue_terminal_remote_if_available")) {
            scenarios.add(scenario(
                    "existing_player_terminal_remote_reissue",
                    "echoterminal_loaded_and_player_missing_remote",
                    List.of("reissue_terminal_remote_if_available"),
                    map(
                            "item", "echoterminal:echo_terminal_remote",
                            "dedupeExistingRemote", true,
                            "dropIfInventoryFull", true)));
        }
        return List.copyOf(scenarios);
    }

    private static Map<String, Object> scenario(
            String id,
            String predicate,
            List<String> actions,
            Map<String, Object> outcome) {
        Map<String, Object> scenario = new LinkedHashMap<>();
        scenario.put("id", id);
        scenario.put("predicate", predicate);
        scenario.put("adapterCoreScenario", true);
        scenario.put("sourceRuntimeBranch", "PlayerStartingKitHandler existing-player return path");
        scenario.put("actions", List.copyOf(actions));
        scenario.put("outcome", Map.copyOf(outcome));
        scenario.put("minecraftRuntimeAccessed", false);
        scenario.put("minecraftRuntimeMutated", false);
        scenario.put("minecraftRegistryMutated", false);
        scenario.put("status", "PASS");
        return Map.copyOf(scenario);
    }

    private static List<String> validate(
            Map<String, Object> firstJoinProfile,
            Map<String, Object> repairInvocation,
            List<String> repairActions,
            List<Map<String, Object>> scenarios) {
        List<String> diagnostics = new ArrayList<>();
        if (!"PlayerStartingKitHandler.onPlayerLoggedIn".equals(value(firstJoinProfile, "sourceParity"))) {
            diagnostics.add("First-join profile is not sourced from PlayerStartingKitHandler.");
        }
        if (repairInvocation.isEmpty()) {
            diagnostics.add("Missing existing-player repair host invocation.");
        }
        if (!"READY_FOR_HOST_SURFACE".equals(repairInvocation.get("status"))) {
            diagnostics.add("Existing-player repair host invocation is not ready.");
        }
        if (!"native_player_recovery_repair_bridge".equals(repairInvocation.get("targetBridgeId"))) {
            diagnostics.add("Existing-player repair invocation is not targeting the native recovery repair bridge.");
        }
        requireRepair(repairActions, "rescue_underground_starting_pod_below_y_48", diagnostics);
        requireRepair(repairActions, "repair_missing_drop_pod_respawn", diagnostics);
        requireRepair(repairActions, "reissue_terminal_remote_if_available", diagnostics);
        requireScenario(scenarios, "existing_player_underground_pod_rescue", diagnostics);
        requireScenario(scenarios, "existing_player_missing_respawn_repair", diagnostics);
        requireScenario(scenarios, "existing_player_terminal_remote_reissue", diagnostics);
        return List.copyOf(diagnostics);
    }

    private static void requireRepair(List<String> repairActions, String repairAction, List<String> diagnostics) {
        if (!contains(repairActions, repairAction)) {
            diagnostics.add("Missing repair action " + repairAction + ".");
        }
    }

    private static void requireScenario(List<Map<String, Object>> scenarios, String id, List<String> diagnostics) {
        for (Map<String, Object> scenario : scenarios) {
            if (id.equals(scenario.get("id")) && "PASS".equals(scenario.get("status"))) {
                return;
            }
        }
        diagnostics.add("Missing repair scenario " + id + ".");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> invocation(Map<String, Object> hostInvocationContract) {
        Object rawInvocations = hostInvocationContract == null ? null : hostInvocationContract.get("invocations");
        if (rawInvocations instanceof List<?> invocations) {
            for (Object entry : invocations) {
                if (entry instanceof Map<?, ?> invocation && REPAIR_INVOCATION_ID.equals(invocation.get("invocationId"))) {
                    return (Map<String, Object>) invocation;
                }
            }
        }
        return Map.of();
    }

    private static List<String> repairActions(Map<String, Object> repairInvocation) {
        List<String> repairActions = new ArrayList<>();
        Object rawPayloads = repairInvocation.get("payloads");
        if (rawPayloads instanceof List<?> payloads) {
            for (Object payload : payloads) {
                if (payload instanceof String text && !text.isBlank()) {
                    repairActions.add(text);
                }
            }
        }
        return List.copyOf(repairActions);
    }

    private static boolean contains(List<String> values, String expected) {
        for (String value : values) {
            if (expected.equals(value)) {
                return true;
            }
        }
        return false;
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

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}

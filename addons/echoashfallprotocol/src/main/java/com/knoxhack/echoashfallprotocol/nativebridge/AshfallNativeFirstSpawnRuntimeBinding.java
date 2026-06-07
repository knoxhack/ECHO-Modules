package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AshfallNativeFirstSpawnRuntimeBinding {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeFirstSpawnRuntimeBinding() {
    }

    public static Map<String, Object> describe(
            Map<String, Object> firstJoinExecution,
            Map<String, Object> firstSpawnEquivalenceHarness) {
        List<String> implementedOperationIds = implementedOperationIds();
        Set<String> sourceOperationIds = sourceOperationIds(firstJoinExecution);
        List<String> diagnostics = validate(
                firstJoinExecution,
                firstSpawnEquivalenceHarness,
                implementedOperationIds,
                sourceOperationIds);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:first_spawn_native_runtime_binding");
        report.put("moduleId", MODULE_ID);
        report.put("bridge", "adaptercore.native_first_spawn_runtime");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "Phase 3 AdapterCore first-spawn runtime source binding descriptor");
        report.put("sourceRuntimeExecutor", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreFirstSpawnRuntime.execute");
        report.put("sourceEventHook", "PlayerStartingKitHandler.onPlayerLoggedIn");
        report.put("sourceAdapterCoreExecution", value(firstJoinExecution, "id"));
        report.put("sourceEquivalenceHarness", value(firstSpawnEquivalenceHarness, "id"));
        report.put("executionMode", "native_live_adaptercore_first_spawn_runtime");
        report.put("runtimeClassRequiresMinecraft", true);
        report.put("safeToEvaluateDuringNativeActivation", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("realNativeStateMutationImplemented", false);
        report.put("liveRuntimeMutationImplemented", false);
        report.put("firstSpawnRuntimeBound", false);
        report.put("firstSpawnRuntimeBindingPrepared", diagnostics.isEmpty());
        report.put("implementedHostAdapterCount", 0);
        report.put("declaredHostAdapterCount", 8);
        report.put("implementedNativeInterfaces", List.of());
        report.put("declaredNativeInterfaces", List.of(
                "EchoNativeRuntimeHost.PlayerInventory",
                "EchoNativeRuntimeHost.Structures",
                "EchoNativeRuntimeHost.PlayerState",
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.Hud"));
        report.put("implementedOperationCount", 0);
        report.put("implementedOperationIds", List.of());
        report.put("declaredOperationCount", implementedOperationIds.size());
        report.put("declaredOperationIds", implementedOperationIds);
        report.put("sourceOperationIds", List.copyOf(sourceOperationIds));
        report.put("hardenedRuntimeChecks", List.of(
                "invalid_player",
                "gametest_server",
                "echoprimecore_loaded",
                "server_level_missing",
                "first_join_flag_idempotency",
                "terminal_optional_module_loaded",
                "terminal_remote_dedupe",
                "reusable_drop_pod_reuse",
                "underground_drop_pod_rescue",
                "missing_respawn_repair"));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "Ashfall first-spawn activation validated the source operations for the live native AdapterCore runtime executor; live mutation is claimed only by post-mutation evidence."
                : "Ashfall first-spawn runtime binding is missing source operations or live executor parity.");
        return Map.copyOf(report);
    }

    private static List<String> implementedOperationIds() {
        return List.of(
                "inventory.write_starter_note",
                "inventory.write_terminal_remote_if_loaded",
                "world.place_personal_drop_pod",
                "player.teleport_to_drop_pod_interior",
                "player.bind_drop_pod_respawn",
                "player.write_first_join_state",
                "player.grant_find_drop_pod_advancement",
                "ui.dispatch_welcome_screen",
                "hud.publish_opening_recovery_notice",
                "repair.rescue_underground_or_missing_respawn");
    }

    private static List<String> validate(
            Map<String, Object> firstJoinExecution,
            Map<String, Object> firstSpawnEquivalenceHarness,
            List<String> implementedOperationIds,
            Set<String> sourceOperationIds) {
        List<String> diagnostics = new ArrayList<>();
        if (!"PASS".equals(value(firstJoinExecution, "status"))) {
            diagnostics.add("First-join AdapterCore execution did not pass.");
        }
        if (!"PASS".equals(value(firstSpawnEquivalenceHarness, "status"))) {
            diagnostics.add("First-spawn equivalence harness did not pass.");
        }
        for (String operationId : implementedOperationIds) {
            if (!sourceOperationIds.contains(operationId)
                    && !operationId.startsWith("repair.")
                    && !operationId.equals("player.write_first_join_state")) {
                diagnostics.add("Missing source operation " + operationId + ".");
            }
        }
        return List.copyOf(diagnostics);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> sourceOperationIds(Map<String, Object> firstJoinExecution) {
        Set<String> operationIds = new LinkedHashSet<>();
        Object rawCommands = firstJoinExecution == null ? null : firstJoinExecution.get("commands");
        if (rawCommands instanceof List<?> commands) {
            for (Object rawCommand : commands) {
                if (rawCommand instanceof Map<?, ?> map) {
                    Object operationId = map.get("operationId");
                    if (operationId instanceof String id && !id.isBlank()) {
                        operationIds.add(id);
                    }
                }
            }
        }

        Map<String, Object> hostCallQueue = childMap(firstJoinExecution, "minecraftRuntimeHostCallQueue");
        Object rawHostCalls = hostCallQueue.get("hostCalls");
        if (rawHostCalls instanceof List<?> hostCalls) {
            for (Object rawHostCall : hostCalls) {
                if (rawHostCall instanceof Map<?, ?> map && map.get("sourceOperationIds") instanceof List<?> ids) {
                    for (Object rawId : ids) {
                        if (rawId instanceof String id && !id.isBlank()) {
                            operationIds.add(id);
                        }
                    }
                }
            }
        }
        return operationIds;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> childMap(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}

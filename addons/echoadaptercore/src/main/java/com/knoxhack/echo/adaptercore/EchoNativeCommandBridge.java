package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeCommandBridge {
    private static final String PREPARED = "prepared_as_adaptercore_command";
    private static final String SKIPPED = "skipped_module_not_loaded";

    private final String moduleId;
    private final List<Map<String, Object>> commands = new ArrayList<>();

    public EchoNativeCommandBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoNativeCommandBridge command(
            String targetSurface,
            String operationId,
            String targetBridge,
            String sourceParity,
            Map<String, Object> payload) {
        return command(commands.size() + 1, targetSurface, operationId, targetBridge, sourceParity, payload, PREPARED);
    }

    public EchoNativeCommandBridge command(
            int order,
            String targetSurface,
            String operationId,
            String targetBridge,
            String sourceParity,
            Map<String, Object> payload) {
        return command(order, targetSurface, operationId, targetBridge, sourceParity, payload, PREPARED);
    }

    public EchoNativeCommandBridge skippedCommand(
            String targetSurface,
            String operationId,
            String targetBridge,
            String sourceParity,
            Map<String, Object> payload) {
        return command(commands.size() + 1, targetSurface, operationId, targetBridge, sourceParity, payload, SKIPPED);
    }

    public EchoNativeCommandBridge skippedCommand(
            int order,
            String targetSurface,
            String operationId,
            String targetBridge,
            String sourceParity,
            Map<String, Object> payload) {
        return command(order, targetSurface, operationId, targetBridge, sourceParity, payload, SKIPPED);
    }

    private EchoNativeCommandBridge command(
            int order,
            String targetSurface,
            String operationId,
            String targetBridge,
            String sourceParity,
            Map<String, Object> payload,
            String status) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("id", AdapterContractGuards.requireText(operationId, "operation id") + ".adaptercore_command");
        command.put("operationId", AdapterContractGuards.requireText(operationId, "operation id"));
        command.put("order", order);
        command.put("targetSurface", AdapterContractGuards.requireText(targetSurface, "target surface"));
        command.put("targetBridge", AdapterContractGuards.requireText(targetBridge, "target bridge"));
        command.put("sourceParity", AdapterContractGuards.optionalText(sourceParity));
        command.put("adapterCoreCommand", true);
        command.put("adapterCoreBridge", "adaptercore.native_command");
        command.put("idempotencyKey", moduleId + ":" + operationId);
        command.put("status", status);
        command.put("commandQueuePrepared", PREPARED.equals(status));
        command.put("commandQueueConsumed", false);
        command.put("minecraftRuntimeAccessed", false);
        command.put("liveRuntimeMutation", false);
        command.put("standaloneDuplicateGameplaySystem", false);
        command.put("payload", payload == null ? Map.of() : Map.copyOf(payload));
        commands.add(command);
        return this;
    }

    /**
     * Returns this command bridge as a truth-layer {@link EchoNativeRuntimeHost.NativeResult}
     * with status {@code QUEUED}. A command bridge is never considered done until consumed.
     */
    public EchoNativeRuntimeHost.NativeResult asNativeResult(String bridgeId) {
        return EchoNativeRuntimeHost.NativeResult.queued(
                "AdapterCore command bridge is prepared but not consumed.",
                Map.of(
                        "bridgeId", AdapterContractGuards.requireText(bridgeId, "bridge id"),
                        "moduleId", moduleId,
                        "commandCount", commands.size(),
                        "preparedCommandCount", countStatus(PREPARED)));
    }

    public Map<String, Object> describe(
            String id,
            String implementationTarget,
            Object sourceProfile,
            Object sourceTransaction,
            List<String> requiredOperationIds,
            List<String> pendingConcreteRuntimeBridges,
            String successSummary,
            String failureSummary) {
        List<String> diagnostics = validate(requiredOperationIds);
        Map<String, Integer> surfaceCommandCounts = surfaceCommandCounts();

        Map<String, Object> execution = new LinkedHashMap<>();
        execution.put("id", AdapterContractGuards.requireText(id, "execution id"));
        execution.put("moduleId", moduleId);
        execution.put("bridge", "adaptercore.native_command");
        execution.put("adapterCoreBridge", true);
        execution.put("implementationTarget", AdapterContractGuards.optionalText(implementationTarget));
        execution.put("sourceProfile", sourceProfile == null ? "" : sourceProfile);
        execution.put("sourceTransaction", sourceTransaction == null ? "" : sourceTransaction);
        execution.put("standaloneDuplicateGameplaySystem", false);
        execution.put("executionMode", "adaptercore_jdk_only_command_queue");
        execution.put("liveRuntimeMutation", false);
        execution.put("minecraftRuntimeAccessed", false);
        execution.put("minecraftRegistryMutated", false);
        execution.put("commandQueuePrepared", diagnostics.isEmpty());
        execution.put("commandQueueConsumed", false);
        execution.put("commandCount", commands.size());
        execution.put("preparedCommandCount", countStatus(PREPARED));
        execution.put("executedCommandCount", 0);
        execution.put("skippedCommandCount", countStatus(SKIPPED));
        execution.put("surfaceCommandCounts", Map.copyOf(surfaceCommandCounts));
        execution.put("commands", List.copyOf(commands));
        execution.put("committedAdapterCoreSurfaces", List.copyOf(surfaceCommandCounts.keySet()));
        execution.put("pendingConcreteRuntimeBridges",
                pendingConcreteRuntimeBridges == null ? List.of() : List.copyOf(pendingConcreteRuntimeBridges));
        execution.put("diagnostics", diagnostics);
        execution.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        execution.put("summary", diagnostics.isEmpty()
                ? AdapterContractGuards.optionalText(successSummary)
                : AdapterContractGuards.optionalText(failureSummary));
        return execution;
    }

    private List<String> validate(List<String> requiredOperationIds) {
        List<String> diagnostics = new ArrayList<>();
        if (commands.isEmpty()) {
            diagnostics.add("Expected at least one AdapterCore command.");
        }
        if (requiredOperationIds != null) {
            for (String operationId : requiredOperationIds) {
                if (!hasPreparedOperation(operationId)) {
                    diagnostics.add("Missing prepared AdapterCore command for operation " + operationId + ".");
                }
            }
        }
        for (Map<String, Object> command : commands) {
            if (!Boolean.TRUE.equals(command.get("adapterCoreCommand"))) {
                diagnostics.add("Command " + command.get("id") + " is not AdapterCore-backed.");
            }
            if (Boolean.TRUE.equals(command.get("minecraftRuntimeAccessed"))) {
                diagnostics.add("Command " + command.get("id") + " accessed Minecraft runtime.");
            }
            if (Boolean.TRUE.equals(command.get("standaloneDuplicateGameplaySystem"))) {
                diagnostics.add("Command " + command.get("id") + " is a standalone duplicate gameplay system.");
            }
        }
        return List.copyOf(diagnostics);
    }

    private boolean hasPreparedOperation(String operationId) {
        for (Map<String, Object> command : commands) {
            if (operationId.equals(command.get("operationId")) && PREPARED.equals(command.get("status"))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Integer> surfaceCommandCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map<String, Object> command : commands) {
            if (PREPARED.equals(command.get("status"))) {
                String surface = String.valueOf(command.get("targetSurface"));
                counts.put(surface, counts.getOrDefault(surface, 0) + 1);
            }
        }
        return counts;
    }

    private int countStatus(String status) {
        int count = 0;
        for (Map<String, Object> command : commands) {
            if (status.equals(command.get("status"))) {
                count++;
            }
        }
        return count;
    }
}

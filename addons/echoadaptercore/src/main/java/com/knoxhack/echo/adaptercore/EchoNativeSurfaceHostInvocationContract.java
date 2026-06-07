package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeSurfaceHostInvocationContract {
    private final String moduleId;

    public EchoNativeSurfaceHostInvocationContract(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> prepare(
            String id,
            String bridge,
            String family,
            List<Map<String, Object>> commands,
            Map<String, Object> runtimeTarget,
            List<HostRequirement> requirements) {
        List<HostRequirement> safeRequirements = requirements == null ? List.of() : requirements;
        List<Map<String, Object>> invocations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (HostRequirement requirement : safeRequirements) {
            Map<String, Object> invocation = invocation(bridge, requirement, commands, runtimeTarget);
            invocations.add(invocation);
            if (!"READY_FOR_HOST_SURFACE".equals(invocation.get("status"))) {
                diagnostics.add("Surface host invocation " + invocation.get("id") + " is not ready.");
            }
        }
        if (safeRequirements.isEmpty()) {
            diagnostics.add("Expected at least one surface host requirement.");
        }
        if (!"PASS".equals(value(runtimeTarget, "status"))) {
            diagnostics.add("Runtime target did not pass before surface host contract preparation.");
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "surface host invocation contract id"));
        report.put("moduleId", moduleId);
        report.put("bridge", AdapterContractGuards.requireText(bridge, "surface host invocation bridge"));
        report.put("family", AdapterContractGuards.requireText(family, "surface host invocation family"));
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore host invocation contract for native gameplay surfaces");
        report.put("executionMode", "adaptercore_jdk_surface_host_invocation_contract");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("hostSurfaceContractPrepared", diagnostics.isEmpty());
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("sourceRuntimeTarget", value(runtimeTarget, "id"));
        report.put("requiredInvocationCount", safeRequirements.size());
        report.put("readyInvocationCount", countReady(invocations));
        report.put("invocations", List.copyOf(invocations));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared host invocations for native gameplay surfaces without standalone copies or Minecraft runtime access."
                : "AdapterCore could not prepare every native gameplay surface host invocation.");
        return Map.copyOf(report);
    }

    private static Map<String, Object> invocation(
            String bridge,
            HostRequirement requirement,
            List<Map<String, Object>> commands,
            Map<String, Object> runtimeTarget) {
        List<Object> payloads = payloads(requirement.payloadSource(), runtimeTarget);
        List<String> sourceOperationIds = sourceOperationIds(requirement.targetBridgeId(), commands);
        List<String> diagnostics = new ArrayList<>();
        if (sourceOperationIds.isEmpty()) {
            diagnostics.add("Missing AdapterCore command for target bridge " + requirement.targetBridgeId() + ".");
        }
        if (payloads.isEmpty()) {
            diagnostics.add("Missing runtime payloads for " + requirement.invocationId() + ".");
        }

        Map<String, Object> invocation = new LinkedHashMap<>();
        invocation.put("id", requirement.invocationId() + ".host_surface_invocation");
        invocation.put("invocationId", requirement.invocationId());
        invocation.put("targetBridgeId", requirement.targetBridgeId());
        invocation.put("adapterCoreBridge", bridge);
        invocation.put("hostSurfaceApi", requirement.hostSurfaceApi());
        invocation.put("nativeInterface", EchoNativeRuntimeHost.interfaceForHostApi(requirement.hostSurfaceApi()));
        invocation.put("nativeMethod", EchoNativeRuntimeHost.methodForHostApi(requirement.hostSurfaceApi()));
        invocation.put("payloadSource", requirement.payloadSource());
        invocation.put("payloads", List.copyOf(payloads));
        invocation.put("payloadCount", payloads.size());
        invocation.put("sourceOperationIds", sourceOperationIds);
        invocation.put("idempotencyKey", "adaptercore:" + requirement.invocationId() + ":" + requirement.payloadSource());
        invocation.put("adapterCoreContractOnly", true);
        invocation.put("standaloneDuplicateGameplaySystem", false);
        invocation.put("minecraftRuntimeAccessed", false);
        invocation.put("minecraftRuntimeMutated", false);
        invocation.put("minecraftRegistryMutated", false);
        invocation.put("nativeStateConsumed", false);
        invocation.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        invocation.put("liveRuntimeMutationConsumed", false);
        invocation.put("diagnostics", List.copyOf(diagnostics));
        invocation.put("status", diagnostics.isEmpty() ? "READY_FOR_HOST_SURFACE" : "BLOCKED");
        return Map.copyOf(invocation);
    }

    private static List<String> sourceOperationIds(String targetBridgeId, List<Map<String, Object>> commands) {
        if (commands == null) {
            return List.of();
        }
        List<String> operationIds = new ArrayList<>();
        for (Map<String, Object> command : commands) {
            if (targetBridgeId.equals(command.get("targetBridge"))
                    && "prepared_as_adaptercore_command".equals(command.get("status"))) {
                String operationId = value(command, "operationId");
                if (!operationId.isBlank()) {
                    operationIds.add(operationId);
                }
            }
        }
        return List.copyOf(operationIds);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> payloads(String payloadSource, Map<String, Object> runtimeTarget) {
        Object rawPayload = runtimeTarget == null ? null : runtimeTarget.get(payloadSource);
        if (rawPayload instanceof List<?> list) {
            return List.copyOf(list);
        }
        if (rawPayload instanceof Map<?, ?> map && !map.isEmpty()) {
            return List.of(Map.copyOf(map));
        }
        if (rawPayload instanceof String text && !text.isBlank()) {
            return List.of(text);
        }
        return List.of();
    }

    private static int countReady(List<Map<String, Object>> invocations) {
        int count = 0;
        for (Map<String, Object> invocation : invocations) {
            if ("READY_FOR_HOST_SURFACE".equals(invocation.get("status"))) {
                count++;
            }
        }
        return count;
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public record HostRequirement(
            String invocationId,
            String targetBridgeId,
            String hostSurfaceApi,
            String payloadSource) {
        public HostRequirement {
            invocationId = AdapterContractGuards.requireText(invocationId, "host invocation id");
            targetBridgeId = AdapterContractGuards.requireText(targetBridgeId, "host invocation target bridge");
            hostSurfaceApi = AdapterContractGuards.requireText(hostSurfaceApi, "host surface api");
            payloadSource = AdapterContractGuards.requireText(payloadSource, "host invocation payload source");
        }
    }
}

package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeRecoveryNavigationHostInvocationContract {
    private final String moduleId;

    public EchoNativeRecoveryNavigationHostInvocationContract(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> prepare(
            String id,
            List<Map<String, Object>> handoffs,
            Map<String, Object> runtimeTarget) {
        List<RecoveryRequirement> requirements = requirements();
        List<Map<String, Object>> invocations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (RecoveryRequirement requirement : requirements) {
            Map<String, Object> invocation = invocation(requirement, handoffs, runtimeTarget);
            invocations.add(invocation);
            if (!"READY_FOR_HOST_SURFACE".equals(invocation.get("status"))) {
                diagnostics.add("Recovery host invocation " + invocation.get("id") + " is not ready.");
            }
        }
        if (!"PASS".equals(value(runtimeTarget, "status"))) {
            diagnostics.add("Recovery/navigation runtime target did not pass before host invocation preparation.");
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "recovery host invocation contract id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.recovery_navigation_host_invocation");
        report.put("family", "recovery_navigation");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore host invocation contract for Recovery and HoloMap navigation surfaces");
        report.put("executionMode", "adaptercore_jdk_recovery_navigation_host_invocation_contract");
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
        report.put("requiredInvocationCount", requirements.size());
        report.put("readyInvocationCount", countReady(invocations));
        report.put("invocations", List.copyOf(invocations));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared host invocations for Recovery field-cache/grave, HoloMap visibility, and existing-player repair surfaces without Minecraft runtime access."
                : "AdapterCore could not prepare every Recovery/HoloMap host invocation.");
        return Map.copyOf(report);
    }

    private static Map<String, Object> invocation(
            RecoveryRequirement requirement,
            List<Map<String, Object>> handoffs,
            Map<String, Object> runtimeTarget) {
        Map<String, Object> handoff = handoff(requirement.handoffId(), handoffs);
        List<Object> payloads = payloads(requirement.payloadSource(), runtimeTarget);
        List<String> diagnostics = new ArrayList<>();
        if (handoff.isEmpty()) {
            diagnostics.add("Missing handoff " + requirement.handoffId() + ".");
        }
        if (!handoff.isEmpty() && !Boolean.TRUE.equals(handoff.get("adapterCoreCommandBacked"))) {
            diagnostics.add("Handoff " + requirement.handoffId() + " is not backed by an executed AdapterCore command.");
        }
        if (payloads.isEmpty()) {
            diagnostics.add("Missing runtime payloads for " + requirement.invocationId() + ".");
        }

        Map<String, Object> invocation = new LinkedHashMap<>();
        invocation.put("id", requirement.invocationId() + ".recovery_host_invocation");
        invocation.put("invocationId", requirement.invocationId());
        invocation.put("handoffId", requirement.handoffId());
        invocation.put("handoffKind", handoff.getOrDefault("kind", ""));
        invocation.put("targetBridgeId", requirement.targetBridgeId());
        invocation.put("adapterCoreBridge", "adaptercore.recovery_navigation_host_invocation");
        invocation.put("hostSurfaceApi", requirement.hostSurfaceApi());
        invocation.put("nativeInterface", EchoNativeRuntimeHost.interfaceForHostApi(requirement.hostSurfaceApi()));
        invocation.put("nativeMethod", EchoNativeRuntimeHost.methodForHostApi(requirement.hostSurfaceApi()));
        invocation.put("payloadSource", requirement.payloadSource());
        invocation.put("payloads", List.copyOf(payloads));
        invocation.put("payloadCount", payloads.size());
        invocation.put("sourceOperationIds", sourceOperationIds(handoff));
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

    private static List<RecoveryRequirement> requirements() {
        return List.of(
                requirement("native_recovery_field_cache_adapter", "echorecovery:field_cache_service",
                        "echorecovery:field_cache_service", "recovery.publish_death_cache_grave_context",
                        "recoveryContexts"),
                requirement("native_holomap_recovery_visibility_adapter", "echoholomap:opening_recovery_layers",
                        "echoholomap:map_state_service", "holomap.publish_recovery_compass_field_cache_visibility",
                        "mapVisibilityContexts"),
                requirement("native_existing_player_repair_adapter", "echoashfallprotocol:existing_player_repair",
                        "native_player_recovery_repair_bridge", "player_recovery.publish_existing_player_repair_actions",
                        "repairActions")
        );
    }

    private static RecoveryRequirement requirement(
            String invocationId,
            String handoffId,
            String targetBridgeId,
            String hostSurfaceApi,
            String payloadSource) {
        return new RecoveryRequirement(invocationId, handoffId, targetBridgeId, hostSurfaceApi, payloadSource);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> handoff(String handoffId, List<Map<String, Object>> handoffs) {
        if (handoffs != null) {
            for (Map<String, Object> handoff : handoffs) {
                if (handoffId.equals(handoff.get("id"))) {
                    return handoff;
                }
            }
        }
        return Map.of();
    }

    private static List<String> sourceOperationIds(Map<String, Object> handoff) {
        String sourceCommand = value(handoff, "sourceCommand");
        return sourceCommand.isBlank() ? List.of() : List.of(sourceCommand);
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

    private record RecoveryRequirement(
            String invocationId,
            String handoffId,
            String targetBridgeId,
            String hostSurfaceApi,
            String payloadSource) {
    }
}

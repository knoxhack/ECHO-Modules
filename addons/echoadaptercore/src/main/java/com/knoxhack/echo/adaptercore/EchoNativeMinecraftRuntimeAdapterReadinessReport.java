package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeMinecraftRuntimeAdapterReadinessReport {
    private final String moduleId;

    public EchoNativeMinecraftRuntimeAdapterReadinessReport(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> audit(String id, Map<String, Object> adapterContract) {
        List<Map<String, Object>> audits = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (Map<String, Object> invocation : invocations(adapterContract)) {
            Map<String, Object> audit = auditInvocation(invocation);
            audits.add(audit);
            if (!"PASS".equals(audit.get("status"))) {
                diagnostics.add("Host adapter readiness audit failed for " + audit.get("adapterId") + ".");
            }
        }
        if (!"PASS".equals(value(adapterContract, "status"))) {
            diagnostics.add("Minecraft runtime adapter contract did not pass.");
        }
        if (audits.size() != 8) {
            diagnostics.add("Expected eight Minecraft-backed first-join host adapter audits.");
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "minecraft runtime adapter readiness id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.minecraft_runtime_adapter_readiness");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore readiness audit for Minecraft-backed native first-join host adapters");
        report.put("executionMode", "adaptercore_jdk_host_adapter_readiness_audit");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("sourceAdapterContract", value(adapterContract, "id"));
        report.put("requiredAdapterCount", 8);
        report.put("auditedAdapterCount", audits.size());
        report.put("readyAdapterCount", readyCount(audits));
        report.put("hostRuntimeAdaptersImplemented", false);
        report.put("nativeHostImplementationReady", false);
        report.put("nativeHostImplementationMetadataReady", diagnostics.isEmpty());
        report.put("remainingExternalRuntimeWork", list(adapterContract.get("remainingExternalRuntimeWork")));
        report.put("adapterAudits", List.copyOf(audits));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore verified every first-join Minecraft-backed host adapter invocation has ready payloads, source operations, idempotency keys, and host API targets for native implementation."
                : "AdapterCore host adapter readiness audit found missing invocation metadata.");
        return Map.copyOf(report);
    }

    private static Map<String, Object> auditInvocation(Map<String, Object> invocation) {
        List<String> diagnostics = new ArrayList<>();
        String adapterId = value(invocation, "adapterId");
        String hostRuntimeApi = value(invocation, "hostRuntimeApi");
        String idempotencyKey = value(invocation, "idempotencyKey");
        String payloadSource = value(invocation, "payloadSource");
        List<?> sourceOperationIds = list(invocation.get("sourceOperationIds"));

        if (adapterId.isBlank()) {
            diagnostics.add("Missing adapter id.");
        }
        if (!"READY_FOR_HOST_ADAPTER".equals(invocation.get("status"))) {
            diagnostics.add("Invocation is not ready for host adapter implementation.");
        }
        if (hostRuntimeApi.isBlank()) {
            diagnostics.add("Missing host runtime API.");
        }
        if (payloadSource.isBlank()) {
            diagnostics.add("Missing payload source.");
        }
        if (!(invocation.get("payloadCount") instanceof Number number) || number.intValue() < 1) {
            diagnostics.add("Missing payloads.");
        }
        if (sourceOperationIds.isEmpty()) {
            diagnostics.add("Missing source operation ids.");
        }
        if (idempotencyKey.isBlank()) {
            diagnostics.add("Missing idempotency key.");
        }
        if (!Boolean.TRUE.equals(invocation.get("requiresMinecraftRuntime"))) {
            diagnostics.add("Invocation must explicitly require Minecraft runtime.");
        }
        if (!Boolean.TRUE.equals(invocation.get("adapterCoreContractOnly"))) {
            diagnostics.add("Invocation must remain contract-only until native host APIs are bound.");
        }
        if (Boolean.TRUE.equals(invocation.get("minecraftRuntimeAccessed"))) {
            diagnostics.add("Readiness audit cannot include runtime access.");
        }

        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("adapterId", adapterId);
        audit.put("targetBridgeId", value(invocation, "targetBridgeId"));
        audit.put("hostRuntimeApi", hostRuntimeApi);
        audit.put("nativeInterface", EchoNativeRuntimeHost.interfaceForHostApi(hostRuntimeApi));
        audit.put("nativeMethod", EchoNativeRuntimeHost.methodForHostApi(hostRuntimeApi));
        audit.put("payloadSource", payloadSource);
        audit.put("payloadCount", invocation.getOrDefault("payloadCount", 0));
        audit.put("sourceOperationIds", List.copyOf(sourceOperationIds));
        audit.put("idempotencyKey", idempotencyKey);
        audit.put("implementationStatus", "READY_FOR_NATIVE_HOST_IMPLEMENTATION");
        audit.put("requiresMinecraftRuntime", true);
        audit.put("hostRuntimeAdapterImplemented", false);
        audit.put("adapterCoreContractOnly", true);
        audit.put("standaloneDuplicateGameplaySystem", false);
        audit.put("minecraftRuntimeAccessed", false);
        audit.put("minecraftRuntimeMutated", false);
        audit.put("minecraftRegistryMutated", false);
        audit.put("diagnostics", List.copyOf(diagnostics));
        audit.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        return Map.copyOf(audit);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> invocations(Map<String, Object> adapterContract) {
        Object rawInvocations = adapterContract == null ? null : adapterContract.get("invocations");
        if (rawInvocations instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static int readyCount(List<Map<String, Object>> audits) {
        int count = 0;
        for (Map<String, Object> audit : audits) {
            if ("PASS".equals(audit.get("status"))) {
                count++;
            }
        }
        return count;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? List.copyOf(list) : List.of();
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}

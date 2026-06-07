package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeSurfaceHostCallQueue {
    private final String moduleId;

    public EchoNativeSurfaceHostCallQueue(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    /**
     * Returns this queue as a truth-layer {@link EchoNativeRuntimeHost.NativeResult}
     * with status {@code QUEUED}. A queue is never considered done until consumed.
     */
    public EchoNativeRuntimeHost.NativeResult asNativeResult(String queueId) {
        return EchoNativeRuntimeHost.NativeResult.queued(
                "AdapterCore surface host call queue is prepared but not consumed.",
                Map.of(
                        "queueId", AdapterContractGuards.requireText(queueId, "queue id"),
                        "moduleId", moduleId,
                        "bridge", "adaptercore.surface_host_call_queue"));
    }

    /**
     * Checks whether the prepared report can be consumed by a runtime host
     * with the given capabilities.
     */
    public boolean canConsume(EchoRuntimeHostCapabilities capabilities, Map<String, Object> preparedReport) {
        if (capabilities == null) {
            return false;
        }
        for (Map<String, Object> hostCall : hostCalls(preparedReport)) {
            String nativeInterface = String.valueOf(hostCall.get("nativeInterface"));
            if (!nativeInterface.isBlank() && !capabilities.supportsNativeInterface(nativeInterface)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> hostCalls(Map<String, Object> preparedReport) {
        Object raw = preparedReport == null ? null : preparedReport.get("hostCalls");
        if (raw instanceof List<?> list) {
            List<Map<String, Object>> calls = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    calls.add((Map<String, Object>) map);
                }
            }
            return calls;
        }
        return List.of();
    }

    public Map<String, Object> prepare(String id, Map<String, Object> hostInvocationContract) {
        List<Map<String, Object>> hostCalls = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        int order = 1;
        for (Map<String, Object> invocation : invocations(hostInvocationContract)) {
            Map<String, Object> hostCall = hostCall(order++, hostInvocationContract, invocation);
            hostCalls.add(hostCall);
            if (!"READY_FOR_LIVE_HOST_SURFACE".equals(hostCall.get("status"))) {
                diagnostics.add("Surface host call " + hostCall.get("id") + " is not ready.");
            }
        }
        if (!"PASS".equals(value(hostInvocationContract, "status"))) {
            diagnostics.add("Surface host invocation contract did not pass.");
        }
        int readyInvocationCount = numberValue(hostInvocationContract, "readyInvocationCount");
        if (hostCalls.size() != readyInvocationCount) {
            diagnostics.add("Surface host call count does not match ready invocation count.");
        }
        if (hostCalls.isEmpty()) {
            diagnostics.add("Expected at least one surface host call.");
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "surface host call queue id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.surface_host_call_queue");
        report.put("family", value(hostInvocationContract, "family"));
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore live host call queue for native gameplay surfaces");
        report.put("executionMode", "adaptercore_jdk_live_surface_host_call_queue");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationQueued", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("hostCallQueuePrepared", diagnostics.isEmpty());
        report.put("hostCallQueueDone", false);
        report.put("safeNativeHostAdaptersRequired", true);
        report.put("hostSurfaceAdaptersImplemented", false);
        report.put("liveSurfaceIntegrationReady", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("sourceHostInvocationContract", value(hostInvocationContract, "id"));
        report.put("sourceHostInvocationBridge", value(hostInvocationContract, "bridge"));
        report.put("requiredHostCallCount", readyInvocationCount);
        report.put("queuedHostCallCount", hostCalls.size());
        report.put("hostCalls", List.copyOf(hostCalls));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PREPARED_UNCONSUMED" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared native gameplay surface invocations as ordered host calls, but this queue is not complete until a live surface host consumes it and returns a mutating NativeResult."
                : "AdapterCore could not prepare a complete native gameplay surface host call queue.");
        return Map.copyOf(report);
    }

    private static Map<String, Object> hostCall(
            int order,
            Map<String, Object> contract,
            Map<String, Object> invocation) {
        List<String> diagnostics = new ArrayList<>();
        String invocationId = value(invocation, "invocationId");
        String hostSurfaceApi = value(invocation, "hostSurfaceApi");
        String idempotencyKey = value(invocation, "idempotencyKey");
        List<?> payloads = list(invocation.get("payloads"));
        List<?> sourceOperationIds = list(invocation.get("sourceOperationIds"));

        if (!"READY_FOR_HOST_SURFACE".equals(invocation.get("status"))) {
            diagnostics.add("Source invocation is not ready for a host surface.");
        }
        if (invocationId.isBlank()) {
            diagnostics.add("Missing invocation id.");
        }
        if (hostSurfaceApi.isBlank()) {
            diagnostics.add("Missing host surface API.");
        }
        if (idempotencyKey.isBlank()) {
            diagnostics.add("Missing idempotency key.");
        }
        if (payloads.isEmpty()) {
            diagnostics.add("Missing payloads.");
        }
        if (sourceOperationIds.isEmpty()) {
            diagnostics.add("Missing source operation ids.");
        }

        Map<String, Object> hostCall = new LinkedHashMap<>();
        hostCall.put("id", invocationId + ".adaptercore_surface_host_call");
        hostCall.put("order", order);
        hostCall.put("invocationId", invocationId);
        hostCall.put("family", value(contract, "family"));
        hostCall.put("targetBridgeId", value(invocation, "targetBridgeId"));
        hostCall.put("adapterCoreBridge", "adaptercore.surface_host_call_queue");
        hostCall.put("sourceInvocationBridge", value(invocation, "adapterCoreBridge"));
        hostCall.put("hostSurfaceApi", hostSurfaceApi);
        hostCall.put("nativeInterface", EchoNativeRuntimeHost.interfaceForHostApi(hostSurfaceApi));
        hostCall.put("nativeMethod", EchoNativeRuntimeHost.methodForHostApi(hostSurfaceApi));
        hostCall.put("payloadSource", value(invocation, "payloadSource"));
        hostCall.put("payloads", List.copyOf(payloads));
        hostCall.put("payloadCount", payloads.size());
        hostCall.put("sourceOperationIds", List.copyOf(sourceOperationIds));
        hostCall.put("idempotencyKey", idempotencyKey);
        hostCall.put("executionBoundary", "native_loader_surface_adapter");
        hostCall.put("requiresNativeHostSurface", true);
        hostCall.put("adapterCoreSurfaceHostCall", true);
        hostCall.put("hostAdapterImplementationRequired", true);
        hostCall.put("liveRuntimeMutationPending", true);
        hostCall.put("liveRuntimeMutationConsumed", false);
        hostCall.put("hostCallDone", false);
        hostCall.put("standaloneDuplicateGameplaySystem", false);
        hostCall.put("minecraftRuntimeAccessed", false);
        hostCall.put("minecraftRuntimeMutated", false);
        hostCall.put("minecraftRegistryMutated", false);
        hostCall.put("diagnostics", List.copyOf(diagnostics));
        hostCall.put("status", diagnostics.isEmpty() ? "READY_FOR_LIVE_HOST_SURFACE" : "BLOCKED");
        return Map.copyOf(hostCall);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> invocations(Map<String, Object> hostInvocationContract) {
        Object rawInvocations = hostInvocationContract == null ? null : hostInvocationContract.get("invocations");
        if (rawInvocations instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? List.copyOf(list) : List.of();
    }

    private static int numberValue(Map<String, Object> values, String key) {
        return values != null && values.get(key) instanceof Number number ? number.intValue() : 0;
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}

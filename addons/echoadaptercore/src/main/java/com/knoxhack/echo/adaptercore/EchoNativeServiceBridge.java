package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeServiceBridge {
    private final String moduleId;
    private final List<Map<String, Object>> services = new ArrayList<>();

    public EchoNativeServiceBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoNativeServiceBridge service(String id, String role, String summary, String... features) {
        return service(List.of(), id, role, summary, features);
    }

    public EchoNativeServiceBridge surfaceService(String surface, String id, String role, String summary, String... features) {
        return service(List.of(AdapterContractGuards.requireText(surface, "service surface")), id, role, summary, features);
    }

    public EchoNativeServiceBridge executedSurfaceService(
            String surface,
            String id,
            String role,
            String summary,
            Map<String, Object> executionEvidence,
            String... features) {
        return service(
                List.of(AdapterContractGuards.requireText(surface, "service surface")),
                id,
                role,
                summary,
                true,
                executionEvidence,
                features);
    }

    public EchoNativeServiceBridge preparedSurfaceService(
            String surface,
            String id,
            String role,
            String summary,
            Map<String, Object> preparedEvidence,
            String... features) {
        return service(
                List.of(AdapterContractGuards.requireText(surface, "service surface")),
                id,
                role,
                summary,
                false,
                "prepared_as_adaptercore_surface_report",
                preparedEvidence,
                features);
    }

    public EchoNativeServiceBridge service(List<String> surfaces, String id, String role, String summary, String... features) {
        return service(surfaces, id, role, summary, false, Map.of(), features);
    }

    private EchoNativeServiceBridge service(
            List<String> surfaces,
            String id,
            String role,
            String summary,
            boolean serviceCodeExecuted,
            Map<String, Object> executionEvidence,
            String... features) {
        return service(
                surfaces,
                id,
                role,
                summary,
                serviceCodeExecuted,
                serviceCodeExecuted
                        ? "executed_as_adaptercore_jdk_only_service"
                        : "started_as_adaptercore_native_service_handle",
                executionEvidence,
                features);
    }

    private EchoNativeServiceBridge service(
            List<String> surfaces,
            String id,
            String role,
            String summary,
            boolean serviceCodeExecuted,
            String state,
            Map<String, Object> evidence,
            String... features) {
        Map<String, Object> safeEvidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("id", AdapterContractGuards.requireText(id, "service id"));
        service.put("role", AdapterContractGuards.requireText(role, "service role"));
        service.put("summary", AdapterContractGuards.optionalText(summary));
        service.put("surfaces", surfaces == null ? List.of() : List.copyOf(surfaces));
        service.put("approved", true);
        service.put("started", true);
        service.put("state", AdapterContractGuards.requireText(state, "service state"));
        service.put("runtimeStateInitialized", true);
        service.put("serviceCodeExecuted", serviceCodeExecuted);
        service.put("minecraftRuntimeAccessed", false);
        service.put("executionEvidence", serviceCodeExecuted ? safeEvidence : Map.of());
        service.put("preparedEvidence", serviceCodeExecuted ? Map.of() : safeEvidence);
        service.put("evidenceMode", serviceCodeExecuted ? "post_code_execution" : "prepared_report");
        service.put("features", List.of(features == null ? new String[0] : features));
        services.add(service);
        return this;
    }

    public Map<String, Object> describe() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moduleId", moduleId);
        data.put("bridge", "adaptercore.native_service");
        data.put("applied", !services.isEmpty());
        data.put("approvedServiceCount", services.size());
        data.put("startedServiceCount", services.size());
        data.put("preparedServiceCount", countState("prepared_as_adaptercore_surface_report"));
        data.put("runtimeInitializedServiceCount", countTrue("runtimeStateInitialized"));
        data.put("executedServiceCount", countTrue("serviceCodeExecuted"));
        data.put("serviceCodeExecuted", countTrue("serviceCodeExecuted") > 0);
        data.put("minecraftRuntimeAccessed", false);
        data.put("serviceExecutionMode", countTrue("serviceCodeExecuted") > 0
                ? "adaptercore_mixed_surface_reports"
                : "adaptercore_surface_reports_prepared");
        data.put("services", List.copyOf(services));
        data.put("summary", services.isEmpty()
                ? "Native service bridge has no approved services to start."
                : "Native service bridge prepared approved AdapterCore surface reports; no entry counts as live Minecraft implementation without mutation evidence.");
        return data;
    }

    private int countTrue(String key) {
        int count = 0;
        for (Map<String, Object> service : services) {
            if (Boolean.TRUE.equals(service.get(key))) {
                count++;
            }
        }
        return count;
    }

    private int countState(String state) {
        int count = 0;
        for (Map<String, Object> service : services) {
            if (state.equals(service.get("state"))) {
                count++;
            }
        }
        return count;
    }
}

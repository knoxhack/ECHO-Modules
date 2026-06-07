package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeRuntimePacketOrchestrator {
    private final String moduleId;

    public EchoNativeRuntimePacketOrchestrator(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> orchestrate(String id, Map<String, Object> packetBindingReport) {
        List<Map<String, Object>> consumerApplications = consumerApplications(packetBindingReport);
        List<String> diagnostics = validate(packetBindingReport, consumerApplications);
        List<Map<String, Object>> dispatches = dispatches(consumerApplications);
        Set<String> moduleIds = moduleIds(consumerApplications);
        Set<String> packetIds = acceptedPacketIds(consumerApplications);
        int acceptedConsumerCount = acceptedConsumerCount(consumerApplications);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "runtime packet orchestration id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_runtime_packet_orchestrator");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore native runtime packet orchestration target");
        report.put("executionMode", "adaptercore_jdk_runtime_packet_orchestration");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("runtimePacketDispatchInitialized", diagnostics.isEmpty());
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("sourcePacketBindingReport", packetBindingReport == null
                ? ""
                : packetBindingReport.getOrDefault("id", ""));
        report.put("packetCount", numberValue(packetBindingReport, "packetCount"));
        report.put("boundConsumerCount", numberValue(packetBindingReport, "boundConsumerCount"));
        report.put("consumerApplicationCount", consumerApplications.size());
        report.put("orchestratedPacketCount", packetIds.size());
        report.put("orchestratedConsumerCount", acceptedConsumerCount);
        report.put("orchestratedModuleCount", moduleIds.size());
        report.put("orchestratedModuleIds", List.copyOf(moduleIds));
        report.put("acceptedPacketIds", List.copyOf(packetIds));
        report.put("dispatches", dispatches);
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared native runtime packet orchestration across module consumer applications without claiming live host mutation."
                : "AdapterCore could not orchestrate every native runtime packet consumer application.");
        return Map.copyOf(report);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> consumerApplications(Map<String, Object> packetBindingReport) {
        Object rawApplications = packetBindingReport == null ? null : packetBindingReport.get("consumerApplications");
        if (rawApplications instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static List<String> validate(
            Map<String, Object> packetBindingReport,
            List<Map<String, Object>> consumerApplications) {
        List<String> diagnostics = new ArrayList<>();
        if (packetBindingReport == null || packetBindingReport.isEmpty()) {
            diagnostics.add("Missing runtime packet binding report.");
            return diagnostics;
        }
        if (!"PASS".equals(packetBindingReport.get("status"))) {
            diagnostics.add("Runtime packet binding report did not pass.");
        }
        if (!"adaptercore.native_runtime_packet".equals(packetBindingReport.get("bridge"))) {
            diagnostics.add("Runtime packet binding report was not produced by AdapterCore.");
        }
        if (!Boolean.TRUE.equals(packetBindingReport.get("adapterCoreBridge"))) {
            diagnostics.add("Runtime packet binding report is not AdapterCore-backed.");
        }
        if (Boolean.TRUE.equals(packetBindingReport.get("minecraftRuntimeAccessed"))) {
            diagnostics.add("Runtime packet binding report accessed Minecraft runtime.");
        }
        if (Boolean.TRUE.equals(packetBindingReport.get("minecraftRuntimeMutated"))) {
            diagnostics.add("Runtime packet binding report mutated Minecraft runtime.");
        }
        if (Boolean.TRUE.equals(packetBindingReport.get("minecraftRegistryMutated"))) {
            diagnostics.add("Runtime packet binding report mutated Minecraft registries.");
        }
        if (consumerApplications.isEmpty()) {
            diagnostics.add("No runtime packet consumer applications were provided.");
        }
        for (Map<String, Object> application : consumerApplications) {
            validateApplication(application, diagnostics);
        }
        return diagnostics;
    }

    private static void validateApplication(Map<String, Object> application, List<String> diagnostics) {
        String id = String.valueOf(application.getOrDefault("id", ""));
        if (!"PASS".equals(application.get("status"))) {
            diagnostics.add("Runtime packet consumer application " + id + " did not pass.");
        }
        if (!"adaptercore.native_runtime_packet_consumer".equals(application.get("bridge"))) {
            diagnostics.add("Runtime packet consumer application " + id + " was not produced by AdapterCore.");
        }
        if (!Boolean.TRUE.equals(application.get("adapterCoreBridge"))) {
            diagnostics.add("Runtime packet consumer application " + id + " is not AdapterCore-backed.");
        }
        if (!Boolean.TRUE.equals(application.get("nativeStateValidatedForHostDispatch"))) {
            diagnostics.add("Runtime packet consumer application " + id + " did not validate native state for host dispatch.");
        }
        if (Boolean.TRUE.equals(application.get("minecraftRuntimeAccessed"))) {
            diagnostics.add("Runtime packet consumer application " + id + " accessed Minecraft runtime.");
        }
        if (Boolean.TRUE.equals(application.get("minecraftRuntimeMutated"))) {
            diagnostics.add("Runtime packet consumer application " + id + " mutated Minecraft runtime.");
        }
        if (Boolean.TRUE.equals(application.get("minecraftRegistryMutated"))) {
            diagnostics.add("Runtime packet consumer application " + id + " mutated Minecraft registries.");
        }
        if (!(application.get("acceptedConsumers") instanceof List<?> consumers) || consumers.isEmpty()) {
            diagnostics.add("Runtime packet consumer application " + id + " accepted no consumers.");
        }
        if (!(application.get("acceptedPacketIds") instanceof List<?> packetIds) || packetIds.isEmpty()) {
            diagnostics.add("Runtime packet consumer application " + id + " accepted no packets.");
        }
    }

    private static List<Map<String, Object>> dispatches(List<Map<String, Object>> consumerApplications) {
        List<Map<String, Object>> dispatches = new ArrayList<>();
        for (Map<String, Object> application : consumerApplications) {
            Map<String, Object> dispatch = new LinkedHashMap<>();
            dispatch.put("applicationId", String.valueOf(application.getOrDefault("id", "")));
            dispatch.put("moduleId", String.valueOf(application.getOrDefault("moduleId", "")));
            dispatch.put("adapterCoreDispatch", true);
            dispatch.put("acceptedConsumerCount", numberValue(application, "acceptedConsumerCount"));
            dispatch.put("acceptedConsumers", stringList(application.get("acceptedConsumers")));
            dispatch.put("acceptedPacketIds", stringList(application.get("acceptedPacketIds")));
            dispatches.add(Map.copyOf(dispatch));
        }
        return List.copyOf(dispatches);
    }

    private static Set<String> moduleIds(List<Map<String, Object>> consumerApplications) {
        LinkedHashSet<String> moduleIds = new LinkedHashSet<>();
        for (Map<String, Object> application : consumerApplications) {
            String moduleId = String.valueOf(application.getOrDefault("moduleId", "")).trim();
            if (!moduleId.isEmpty()) {
                moduleIds.add(moduleId);
            }
        }
        return moduleIds;
    }

    private static Set<String> acceptedPacketIds(List<Map<String, Object>> consumerApplications) {
        LinkedHashSet<String> packetIds = new LinkedHashSet<>();
        for (Map<String, Object> application : consumerApplications) {
            packetIds.addAll(stringList(application.get("acceptedPacketIds")));
        }
        return packetIds;
    }

    private static int acceptedConsumerCount(List<Map<String, Object>> consumerApplications) {
        int count = 0;
        for (Map<String, Object> application : consumerApplications) {
            count += numberValue(application, "acceptedConsumerCount");
        }
        return count;
    }

    private static int numberValue(Map<String, Object> values, String key) {
        if (values != null && values.get(key) instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : list) {
            String text = String.valueOf(item).trim();
            if (!text.isEmpty()) {
                values.add(text);
            }
        }
        return List.copyOf(values);
    }
}

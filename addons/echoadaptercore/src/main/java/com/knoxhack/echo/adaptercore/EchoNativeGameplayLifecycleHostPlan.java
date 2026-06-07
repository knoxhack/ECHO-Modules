package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeGameplayLifecycleHostPlan {
    private final String moduleId;

    public EchoNativeGameplayLifecycleHostPlan(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> plan(
            String id,
            Map<String, Object> firstJoinHostContract,
            Map<String, Object> recoveryHostContract,
            Map<String, Object> uiHudHostContract,
            Map<String, Object> atmosphereHostContract) {
        List<Map<String, Object>> steps = List.of(
                step(10, "player.first_join", "first_join_minecraft_runtime_adapters",
                        firstJoinHostContract, "READY_FOR_HOST_ADAPTER"),
                step(20, "player.recovery_navigation", "recovery_navigation_host_surfaces",
                        recoveryHostContract, "READY_FOR_HOST_SURFACE"),
                step(30, "ui_hud_screen_safe", "ui_hud_screen_safe_host_surfaces",
                        uiHudHostContract, "READY_FOR_HOST_SURFACE"),
                step(40, "weather_sound_atmosphere", "weather_sound_atmosphere_host_surfaces",
                        atmosphereHostContract, "READY_FOR_HOST_SURFACE")
        );
        List<String> diagnostics = validate(steps);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "gameplay lifecycle host plan id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.gameplay_lifecycle_host_plan");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore ordered gameplay lifecycle plan for native host invocations");
        report.put("executionMode", "adaptercore_jdk_gameplay_lifecycle_host_plan");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("hostLifecyclePlanPrepared", diagnostics.isEmpty());
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("lifecycleStepCount", steps.size());
        report.put("readyInvocationCount", readyInvocationCount(steps));
        report.put("steps", steps);
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared an ordered native gameplay lifecycle plan for first-join, Recovery/HoloMap, UI/HUD, and weather/sound/atmosphere host invocations."
                : "AdapterCore gameplay lifecycle host plan is missing required host invocation readiness.");
        return Map.copyOf(report);
    }

    private static Map<String, Object> step(
            int order,
            String lifecycleEvent,
            String stage,
            Map<String, Object> contract,
            String readyStatus) {
        List<Map<String, Object>> invocations = invocations(contract);
        List<String> invocationIds = new ArrayList<>();
        List<String> hostApis = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (Map<String, Object> invocation : invocations) {
            String status = value(invocation, "status");
            if (!readyStatus.equals(status)) {
                diagnostics.add("Invocation " + invocationId(invocation) + " is not ready.");
            }
            invocationIds.add(invocationId(invocation));
            String hostApi = value(invocation, "hostRuntimeApi");
            if (hostApi.isBlank()) {
                hostApi = value(invocation, "hostSurfaceApi");
            }
            if (!hostApi.isBlank()) {
                hostApis.add(hostApi);
            }
        }
        if (!"PASS".equals(value(contract, "status"))) {
            diagnostics.add("Source host contract " + value(contract, "id") + " did not pass.");
        }
        if (invocations.isEmpty()) {
            diagnostics.add("Source host contract " + value(contract, "id") + " has no invocations.");
        }

        Map<String, Object> step = new LinkedHashMap<>();
        step.put("order", order);
        step.put("lifecycleEvent", lifecycleEvent);
        step.put("stage", stage);
        step.put("sourceContract", value(contract, "id"));
        step.put("sourceBridge", value(contract, "bridge"));
        step.put("expectedReadyStatus", readyStatus);
        step.put("invocationCount", invocations.size());
        step.put("readyInvocationCount", invocations.size() - diagnostics.size());
        step.put("invocationIds", List.copyOf(invocationIds));
        step.put("hostApis", List.copyOf(hostApis));
        step.put("adapterCoreLifecycleStep", true);
        step.put("minecraftRuntimeAccessed", false);
        step.put("minecraftRuntimeMutated", false);
        step.put("minecraftRegistryMutated", false);
        step.put("diagnostics", List.copyOf(diagnostics));
        step.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        return Map.copyOf(step);
    }

    private static List<String> validate(List<Map<String, Object>> steps) {
        List<String> diagnostics = new ArrayList<>();
        if (steps.size() != 4) {
            diagnostics.add("Expected four gameplay lifecycle host steps.");
        }
        int previousOrder = 0;
        for (Map<String, Object> step : steps) {
            int order = step.get("order") instanceof Number number ? number.intValue() : 0;
            if (order <= previousOrder) {
                diagnostics.add("Lifecycle step order is not strictly increasing.");
            }
            previousOrder = order;
            if (!"PASS".equals(step.get("status"))) {
                diagnostics.add("Lifecycle step " + step.get("stage") + " did not pass.");
            }
            if (Boolean.TRUE.equals(step.get("minecraftRuntimeAccessed"))) {
                diagnostics.add("Lifecycle step " + step.get("stage") + " accessed Minecraft runtime.");
            }
        }
        if (readyInvocationCount(steps) != 23) {
            diagnostics.add("Expected 23 ready host invocations across Agent 3 lifecycle plan.");
        }
        return diagnostics;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> invocations(Map<String, Object> contract) {
        Object rawInvocations = contract == null ? null : contract.get("invocations");
        if (rawInvocations instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static int readyInvocationCount(List<Map<String, Object>> steps) {
        int count = 0;
        for (Map<String, Object> step : steps) {
            if (step.get("readyInvocationCount") instanceof Number number) {
                count += number.intValue();
            }
        }
        return count;
    }

    private static String invocationId(Map<String, Object> invocation) {
        String id = value(invocation, "adapterId");
        if (id.isBlank()) {
            id = value(invocation, "invocationId");
        }
        return id;
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}

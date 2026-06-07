package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeRecoveryNavigationRuntimeTarget {
    private final String moduleId;
    private final List<Map<String, Object>> recoveryContexts = new ArrayList<>();
    private final List<Map<String, Object>> mapVisibilityContexts = new ArrayList<>();
    private final List<String> holomapLayers = new ArrayList<>();
    private final List<String> repairActions = new ArrayList<>();
    private final List<Map<String, Object>> mutationLog = new ArrayList<>();
    private final Set<String> mutationSurfaces = new LinkedHashSet<>();
    private boolean recoveryCompassVisible = false;
    private boolean fieldCacheVisible = false;
    private int executedHandoffCount = 0;

    public EchoNativeRecoveryNavigationRuntimeTarget(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> execute(String id, List<Map<String, Object>> handoffs) {
        if (handoffs != null) {
            for (Map<String, Object> handoff : handoffs) {
                if (Boolean.TRUE.equals(handoff.get("adapterCoreCommandBacked"))) {
                    executedHandoffCount++;
                    executeHandoff(handoff);
                }
            }
        }

        List<String> diagnostics = validate();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "runtime target id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_recovery_navigation_runtime");
        report.put("adapterCoreBridge", true);
        report.put("adapterSurface", "player_recovery.navigation_runtime_target");
        report.put("implementationTarget", "AdapterCore stateful native Recovery and HoloMap runtime target");
        report.put("executionMode", "adaptercore_jdk_stateful_recovery_navigation_runtime_target");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("liveRuntimeMutation", false);
        report.put("nativeStateMutated", !mutationLog.isEmpty());
        report.put("noLaunchNativeStateMutated", !mutationLog.isEmpty());
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("unsafeRuntimeWorkStarted", false);
        report.put("executedHandoffCount", executedHandoffCount);
        report.put("mutatingOperationCount", mutationLog.size());
        report.put("mutationSurfaces", List.copyOf(mutationSurfaces));
        report.put("mutationLog", List.copyOf(mutationLog));
        report.put("recoveryContexts", List.copyOf(recoveryContexts));
        report.put("mapVisibilityContexts", List.copyOf(mapVisibilityContexts));
        report.put("holomapLayers", List.copyOf(holomapLayers));
        report.put("recoveryCompassVisible", recoveryCompassVisible);
        report.put("fieldCacheVisible", fieldCacheVisible);
        report.put("repairActions", List.copyOf(repairActions));
        report.put("runtimeSnapshot", snapshot());
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared Ashfall Recovery and HoloMap handoffs against stateful no-launch death/cache/grave, compass, field-cache, map-layer, and repair packets without claiming live host mutation."
                : "AdapterCore Recovery/HoloMap runtime target is missing required recovery, navigation, or repair mutations.");
        return report;
    }

    @SuppressWarnings("unchecked")
    private void executeHandoff(Map<String, Object> handoff) {
        String kind = String.valueOf(handoff.get("kind"));
        String targetBridge = String.valueOf(handoff.get("targetBridge"));
        Map<String, Object> payload = handoff.get("payload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        switch (kind) {
            case "death_cache_grave_handoff" -> {
                recoveryContexts.add(Map.copyOf(payload));
                recoveryCompassVisible |= Boolean.TRUE.equals(payload.get("recoveryCompassVisible"));
                logMutation(kind, targetBridge, payload.getOrDefault("deathRecoveryHandoff", "recovery_context"));
            }
            case "recovery_compass_field_cache_visibility" -> {
                mapVisibilityContexts.add(Map.copyOf(payload));
                fieldCacheVisible |= Boolean.TRUE.equals(payload.get("fieldCacheVisibility"));
                addStrings(holomapLayers, payload.get("layers"));
                logMutation(kind, targetBridge, "field_cache_visibility");
            }
            case "existing_player_repair_path" -> {
                addStrings(repairActions, payload.get("repairs"));
                logMutation(kind, targetBridge, "existing_player_repairs");
            }
            default -> {
            }
        }
    }

    private void logMutation(String kind, String targetBridge, Object target) {
        Map<String, Object> mutation = new LinkedHashMap<>();
        mutation.put("handoffKind", AdapterContractGuards.requireText(kind, "mutation handoff kind"));
        mutation.put("targetBridge", AdapterContractGuards.requireText(targetBridge, "mutation target bridge"));
        mutation.put("target", String.valueOf(target));
        mutation.put("adapterCoreMutation", true);
        mutation.put("minecraftRuntimeAccessed", false);
        mutationLog.add(mutation);
        mutationSurfaces.add(targetBridge);
    }

    private void addStrings(List<String> target, Object values) {
        if (values instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof String text && !text.isBlank()) {
                    target.add(text);
                }
            }
        }
    }

    private Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("recoveryContextCount", recoveryContexts.size());
        snapshot.put("mapVisibilityContextCount", mapVisibilityContexts.size());
        snapshot.put("holomapLayerCount", holomapLayers.size());
        snapshot.put("repairActionCount", repairActions.size());
        snapshot.put("recoveryCompassVisible", recoveryCompassVisible);
        snapshot.put("fieldCacheVisible", fieldCacheVisible);
        snapshot.put("nativeStateMutated", !mutationLog.isEmpty());
        snapshot.put("minecraftRuntimeAccessed", false);
        return snapshot;
    }

    private List<String> validate() {
        List<String> diagnostics = new ArrayList<>();
        if (recoveryContexts.isEmpty()) {
            diagnostics.add("Missing runtime Recovery field-cache/grave context.");
        }
        if (!recoveryCompassVisible) {
            diagnostics.add("Missing runtime recovery compass visibility.");
        }
        if (mapVisibilityContexts.isEmpty()) {
            diagnostics.add("Missing runtime HoloMap visibility context.");
        }
        if (!fieldCacheVisible) {
            diagnostics.add("Missing runtime field-cache visibility.");
        }
        if (!holomapLayers.contains("echoashfallprotocol:first_month_field_intel")) {
            diagnostics.add("Missing runtime first-month HoloMap layer.");
        }
        if (!holomapLayers.contains("echoashfallprotocol:first_major_route")) {
            diagnostics.add("Missing runtime first-major-route HoloMap layer.");
        }
        requireRepair("rescue_underground_starting_pod_below_y_48", diagnostics);
        requireRepair("repair_missing_drop_pod_respawn", diagnostics);
        requireRepair("reissue_terminal_remote_if_available", diagnostics);
        if (mutationLog.size() < 3) {
            diagnostics.add("Expected all recovery/navigation handoffs to mutate native state.");
        }
        return List.copyOf(diagnostics);
    }

    private void requireRepair(String repair, List<String> diagnostics) {
        if (!repairActions.contains(repair)) {
            diagnostics.add("Missing runtime repair action " + repair + ".");
        }
    }
}

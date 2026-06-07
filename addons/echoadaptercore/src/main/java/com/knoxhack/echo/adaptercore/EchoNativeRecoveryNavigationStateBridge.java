package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeRecoveryNavigationStateBridge {
    private final String moduleId;
    private final List<Map<String, Object>> recoveryContexts = new ArrayList<>();
    private final List<Map<String, Object>> mapVisibilityContexts = new ArrayList<>();
    private final List<String> holomapLayers = new ArrayList<>();
    private final List<String> repairActions = new ArrayList<>();
    private boolean recoveryCompassVisible = false;
    private boolean fieldCacheVisible = false;

    public EchoNativeRecoveryNavigationStateBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> apply(String id, List<Map<String, Object>> handoffs) {
        if (handoffs != null) {
            for (Map<String, Object> handoff : handoffs) {
                if (Boolean.TRUE.equals(handoff.get("adapterCoreCommandBacked"))) {
                    applyHandoff(handoff);
                }
            }
        }

        List<String> diagnostics = validate();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "state report id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_recovery_navigation_state");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore native Recovery and HoloMap command application");
        report.put("executionMode", "adaptercore_jdk_only_recovery_navigation_state_application");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRegistryMutated", false);
        report.put("liveRuntimeMutation", false);
        report.put("recoveryContexts", List.copyOf(recoveryContexts));
        report.put("mapVisibilityContexts", List.copyOf(mapVisibilityContexts));
        report.put("holomapLayers", List.copyOf(holomapLayers));
        report.put("recoveryCompassVisible", recoveryCompassVisible);
        report.put("fieldCacheVisible", fieldCacheVisible);
        report.put("repairActions", List.copyOf(repairActions));
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore applied first-join Recovery and HoloMap handoffs to a JDK-only recovery/navigation state report for field-cache, compass, grave context, route layers, and existing-player repairs."
                : "AdapterCore native recovery/navigation state report is missing required handoff outcomes.");
        return report;
    }

    @SuppressWarnings("unchecked")
    private void applyHandoff(Map<String, Object> handoff) {
        String kind = String.valueOf(handoff.get("kind"));
        Map<String, Object> payload = handoff.get("payload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        switch (kind) {
            case "death_cache_grave_handoff" -> {
                recoveryContexts.add(Map.copyOf(payload));
                recoveryCompassVisible |= Boolean.TRUE.equals(payload.get("recoveryCompassVisible"));
            }
            case "recovery_compass_field_cache_visibility" -> {
                mapVisibilityContexts.add(Map.copyOf(payload));
                fieldCacheVisible |= Boolean.TRUE.equals(payload.get("fieldCacheVisibility"));
                addStrings(holomapLayers, payload.get("layers"));
            }
            case "existing_player_repair_path" ->
                    addStrings(repairActions, payload.get("repairs"));
            default -> {
            }
        }
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

    private List<String> validate() {
        List<String> diagnostics = new ArrayList<>();
        if (recoveryContexts.isEmpty()) {
            diagnostics.add("Missing Recovery field-cache/grave context.");
        }
        if (!recoveryCompassVisible) {
            diagnostics.add("Missing recovery compass visibility.");
        }
        if (!fieldCacheVisible) {
            diagnostics.add("Missing field-cache map visibility.");
        }
        if (!holomapLayers.contains("echoashfallprotocol:first_month_field_intel")) {
            diagnostics.add("Missing first-month HoloMap recovery layer.");
        }
        if (!holomapLayers.contains("echoashfallprotocol:first_major_route")) {
            diagnostics.add("Missing first-major-route HoloMap layer.");
        }
        requireRepair("rescue_underground_starting_pod_below_y_48", diagnostics);
        requireRepair("repair_missing_drop_pod_respawn", diagnostics);
        return List.copyOf(diagnostics);
    }

    private void requireRepair(String repair, List<String> diagnostics) {
        if (!repairActions.contains(repair)) {
            diagnostics.add("Missing repair action " + repair + ".");
        }
    }
}

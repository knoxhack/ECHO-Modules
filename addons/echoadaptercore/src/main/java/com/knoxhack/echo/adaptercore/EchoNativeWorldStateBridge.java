package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeWorldStateBridge {
    private final String moduleId;
    private final List<Map<String, Object>> structurePlacements = new ArrayList<>();
    private Map<String, Object> placementConstraints = Map.of();

    public EchoNativeWorldStateBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> apply(String id, List<Map<String, Object>> commands, Map<String, Object> constraints) {
        placementConstraints = constraints == null ? Map.of() : Map.copyOf(constraints);
        if (commands != null) {
            for (Map<String, Object> command : commands) {
                if ("prepared_as_adaptercore_command".equals(command.get("status"))) {
                    applyCommand(command);
                }
            }
        }

        List<String> diagnostics = validate();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "state report id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_world_state");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore native world-state command application");
        report.put("executionMode", "adaptercore_jdk_only_world_state_application");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRegistryMutated", false);
        report.put("liveRuntimeMutation", false);
        report.put("structurePlacements", List.copyOf(structurePlacements));
        report.put("placementConstraints", placementConstraints);
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore applied first-join world commands to a JDK-only drop-pod placement state report with NeoForge radius, spacing, surface, air, support, and sky constraints."
                : "AdapterCore native world-state report is missing required drop-pod placement constraints.");
        return report;
    }

    @SuppressWarnings("unchecked")
    private void applyCommand(Map<String, Object> command) {
        if (!"world.place_personal_drop_pod".equals(command.get("operationId"))) {
            return;
        }
        Map<String, Object> payload = command.get("payload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        Map<String, Object> placement = new LinkedHashMap<>();
        placement.putAll(payload);
        placement.put("interiorAnchor", placementConstraints.getOrDefault("interiorAnchor", ""));
        placement.put("serverSpawnReference", placementConstraints.getOrDefault("serverSpawnReference", ""));
        placement.put("candidateAttemptsPerBand", placementConstraints.getOrDefault("candidateAttemptsPerBand", 0));
        placement.put("startingSurfaceSearchRadius", placementConstraints.getOrDefault("startingSurfaceSearchRadius", 0));
        placement.put("startingSurfaceFallbackSearchRadius", placementConstraints.getOrDefault("startingSurfaceFallbackSearchRadius", 0));
        placement.put("requiresSkyVisible", placementConstraints.getOrDefault("requiresSkyVisible", false));
        placement.put("requiresTwoAirBlocks", placementConstraints.getOrDefault("requiresTwoAirBlocks", false));
        placement.put("requiresPlacementSupport", placementConstraints.getOrDefault("requiresPlacementSupport", false));
        placement.put("fallbackBehavior", placementConstraints.getOrDefault("fallbackBehavior", List.of()));
        structurePlacements.add(Map.copyOf(placement));
    }

    private List<String> validate() {
        List<String> diagnostics = new ArrayList<>();
        Map<String, Object> placement = structurePlacements.isEmpty() ? Map.of() : structurePlacements.get(0);
        requireEquals(placement, "structure", "echoashfallprotocol:drop_pod", "Missing Ashfall drop-pod structure placement.", diagnostics);
        requireNumber(placement, "minRadiusChunks", 2, "Drop-pod min radius drifted.", diagnostics);
        requireNumber(placement, "maxRadiusChunks", 8, "Drop-pod max radius drifted.", diagnostics);
        requireNumber(placement, "minSpacingChunks", 3, "Drop-pod minimum spacing drifted.", diagnostics);
        requireNumber(placement, "minimumStartingSurfaceY", 48, "Drop-pod minimum surface Y drifted.", diagnostics);
        requireEquals(placement, "interiorAnchor", "personal_starting_drop_pod.interior", "Missing drop-pod interior anchor.", diagnostics);
        requireEquals(placement, "serverSpawnReference", "level.respawnData.pos", "Missing server spawn reference.", diagnostics);
        requireNumber(placement, "candidateAttemptsPerBand", 16, "Drop-pod candidate attempts drifted.", diagnostics);
        requireNumber(placement, "startingSurfaceSearchRadius", 24, "Drop-pod surface search radius drifted.", diagnostics);
        requireNumber(placement, "startingSurfaceFallbackSearchRadius", 96, "Drop-pod fallback search radius drifted.", diagnostics);
        requireBoolean(placement, "requiresSkyVisible", "Drop-pod placement must require sky visibility.", diagnostics);
        requireBoolean(placement, "requiresTwoAirBlocks", "Drop-pod placement must require two air blocks.", diagnostics);
        requireBoolean(placement, "requiresPlacementSupport", "Drop-pod placement must require support.", diagnostics);
        Object fallback = placement.get("fallbackBehavior");
        if (!(fallback instanceof List<?> list) || !list.contains("try_nearest_safe_surface")) {
            diagnostics.add("Drop-pod placement must retain nearest safe surface fallback.");
        }
        return List.copyOf(diagnostics);
    }

    private static void requireEquals(Map<String, Object> state, String key, Object expected, String message,
                                      List<String> diagnostics) {
        if (!expected.equals(state.get(key))) {
            diagnostics.add(message);
        }
    }

    private static void requireNumber(Map<String, Object> state, String key, long expected, String message,
                                      List<String> diagnostics) {
        Object value = state.get(key);
        if (!(value instanceof Number number) || number.longValue() != expected) {
            diagnostics.add(message);
        }
    }

    private static void requireBoolean(Map<String, Object> state, String key, String message,
                                       List<String> diagnostics) {
        if (!Boolean.TRUE.equals(state.get(key))) {
            diagnostics.add(message);
        }
    }
}

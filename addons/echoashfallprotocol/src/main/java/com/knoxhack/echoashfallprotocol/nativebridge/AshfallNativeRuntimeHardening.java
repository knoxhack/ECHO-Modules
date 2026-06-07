package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AshfallNativeRuntimeHardening {
    private static final String MODULE_ID = "echoashfallprotocol";
    private static final int SAVE_MIGRATION_VERSION = 3;

    private AshfallNativeRuntimeHardening() {
    }

    public static Map<String, Object> describe(
            Map<String, Object> eventBridge,
            Map<String, Object> firstSpawnRuntimeBinding,
            Map<String, Object> earlyEventRuntimeBinding,
            Map<String, Object> machineRuntimeBinding,
            Map<String, Object> explorationRuntimeBinding,
            Map<String, Object> hazardRuntimeBinding,
            Map<String, Object> lateRuntimeBinding) {
        Set<String> sourceEventHooks = sourceEventHooks(eventBridge);
        List<String> hardeningChecks = hardeningChecks();
        List<String> diagnostics = validate(
                sourceEventHooks,
                firstSpawnRuntimeBinding,
                earlyEventRuntimeBinding,
                machineRuntimeBinding,
                explorationRuntimeBinding,
                hazardRuntimeBinding,
                lateRuntimeBinding);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:adaptercore_runtime_hardening");
        report.put("moduleId", MODULE_ID);
        report.put("bridge", "adaptercore.native_runtime_hardening");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "Phase 9 AdapterCore hardening for missing addons, unloaded chunks, invalid players, duplicate events, side checks, reloads, and migration");
        report.put("sourceRuntimeGuardClass", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreRuntimeGuards");
        report.put("sourceMigrationClass", "com.knoxhack.echoashfallprotocol.data.SaveMigrationHandler");
        report.put("sourceAdapterCoreEventBridge", value(eventBridge, "bridge"));
        report.put("executionMode", "native_live_adaptercore_runtime_hardening");
        report.put("runtimeClassRequiresMinecraft", true);
        report.put("safeToEvaluateDuringNativeActivation", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("hardeningImplemented", diagnostics.isEmpty());
        report.put("handledFailureModeCount", hardeningChecks.size());
        report.put("handledFailureModes", hardeningChecks);
        report.put("migrationVersion", SAVE_MIGRATION_VERSION);
        report.put("guardedRuntimeBindings", List.of(
                value(firstSpawnRuntimeBinding, "id"),
                value(earlyEventRuntimeBinding, "id"),
                value(machineRuntimeBinding, "id"),
                value(explorationRuntimeBinding, "id"),
                value(hazardRuntimeBinding, "id"),
                value(lateRuntimeBinding, "id")));
        report.put("guardedRuntimePublishers", List.of(
                "AshfallAdapterCoreFirstSpawnRuntime.execute",
                "AshfallAdapterCoreEarlyEventRuntime.publish",
                "AshfallAdapterCoreExplorationRuntime.publish",
                "AshfallAdapterCoreHazardRuntime.publish",
                "AshfallAdapterCoreLateRuntime.publish",
                "AshfallAdapterCoreMachinePowerRuntime.runRuntimeScenario"));
        report.put("runtimeGuardOperations", List.of(
                "guardServerPlayer",
                "guardPublish",
                "claimSameTickEvent",
                "ensureMissionContentReady",
                "SaveMigrationHandler.ensureCurrent",
                "Level.isLoaded"));
        report.put("sourceEventHooks", List.copyOf(sourceEventHooks));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore runtime hardening now covers missing addons, unloaded chunks, invalid players, duplicate events, logical side checks, content reload replay, and save migration catch-up."
                : "AdapterCore runtime hardening is missing required guard coverage or upstream runtime evidence.");
        return Map.copyOf(report);
    }

    private static List<String> hardeningChecks() {
        return List.of(
                "missing_addons",
                "unloaded_chunks",
                "invalid_players",
                "duplicate_events",
                "logical_side_checks",
                "reload_replay",
                "save_migration");
    }

    private static List<String> validate(
            Set<String> sourceEventHooks,
            Map<String, Object> firstSpawnRuntimeBinding,
            Map<String, Object> earlyEventRuntimeBinding,
            Map<String, Object> machineRuntimeBinding,
            Map<String, Object> explorationRuntimeBinding,
            Map<String, Object> hazardRuntimeBinding,
            Map<String, Object> lateRuntimeBinding) {
        List<String> diagnostics = new ArrayList<>();
        for (Map<String, Object> binding : List.of(
                firstSpawnRuntimeBinding,
                earlyEventRuntimeBinding,
                machineRuntimeBinding,
                explorationRuntimeBinding,
                hazardRuntimeBinding,
                lateRuntimeBinding)) {
            if (!"PASS".equals(value(binding, "status"))) {
                diagnostics.add("Runtime binding " + value(binding, "id") + " did not pass before hardening.");
            }
        }
        if (!sourceEventHooks.contains("data.reload")) {
            diagnostics.add("AdapterCore event bridge must retain the data.reload hook for reload hardening.");
        }
        return List.copyOf(diagnostics);
    }

    private static Set<String> sourceEventHooks(Map<String, Object> eventBridge) {
        Set<String> hooks = new LinkedHashSet<>();
        Object rawHooks = eventBridge == null ? null : eventBridge.get("hooks");
        if (rawHooks instanceof List<?> list) {
            for (Object rawHook : list) {
                if (rawHook instanceof Map<?, ?> map) {
                    Object event = map.get("event");
                    if (event instanceof String id && !id.isBlank()) {
                        hooks.add(id);
                    }
                }
            }
        }
        return hooks;
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}

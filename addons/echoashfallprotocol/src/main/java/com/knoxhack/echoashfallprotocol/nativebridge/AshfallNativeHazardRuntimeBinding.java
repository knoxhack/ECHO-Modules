package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AshfallNativeHazardRuntimeBinding {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeHazardRuntimeBinding() {
    }

    public static Map<String, Object> describe(
            Map<String, Object> eventBridge,
            Map<String, Object> midgameRouteBootstrap,
            Map<String, Object> machineRuntimeBinding) {
        List<String> implementedOperationIds = implementedOperationIds();
        Set<String> sourceEventHooks = sourceEventHooks(eventBridge);
        List<String> diagnostics = validate(eventBridge, midgameRouteBootstrap, machineRuntimeBinding, sourceEventHooks);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:hazard_native_runtime_binding");
        report.put("moduleId", MODULE_ID);
        report.put("bridge", "adaptercore.native_hazard_runtime");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "Phase 7 AdapterCore hazard runtime source binding descriptor");
        report.put("sourceRuntimeClass", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime");
        report.put("sourceRuntimePublisher", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime.publish");
        report.put("sourceAdapterCoreEventBridge", value(eventBridge, "bridge"));
        report.put("sourceMidgameRouteBootstrap", value(midgameRouteBootstrap, "id"));
        report.put("sourceMachineRuntimeBinding", value(machineRuntimeBinding, "id"));
        report.put("executionMode", "native_live_adaptercore_hazard_runtime");
        report.put("runtimeClassRequiresMinecraft", true);
        report.put("safeToEvaluateDuringNativeActivation", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("realNativeStateMutationImplemented", false);
        report.put("liveRuntimeMutationImplemented", false);
        report.put("hazardRuntimeBound", false);
        report.put("hazardRuntimeBindingPrepared", diagnostics.isEmpty());
        report.put("implementedNativeInterfaces", List.of());
        report.put("declaredNativeInterfaces", List.of(
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.PlayerState",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.SaveData"));
        report.put("implementedOperationCount", 0);
        report.put("implementedOperationIds", List.of());
        report.put("declaredOperationCount", implementedOperationIds.size());
        report.put("declaredOperationIds", implementedOperationIds);
        report.put("implementedRuntimeEvents", List.of());
        report.put("declaredRuntimeEvents", List.of(
                "ashfall.radiation_changed",
                "ashfall.mutation_gained",
                "ashfall.treatment_applied",
                "ashfall.med_bay_used",
                "ashfall.cleanser_used",
                "ashfall.scrubber_used",
                "ashfall.lab_objective",
                "ashfall.vault_objective",
                "ashfall.hazard_route_check"));
        report.put("sourceRuntimeHandlers", List.of(
                "SurvivalTickHandler.onPlayerTick",
                "RadiationHelper.addRadiation",
                "MutationManager.tryMutate",
                "RadAwayItem.use",
                "FieldMedBayBlockEntity.serverTick",
                "RadiationCleanserBlockEntity.completeCleansing",
                "AtmosphericScrubberBlockEntity.serverTick",
                "ResearchLabBlock.useWithoutItem",
                "RareTechSchematicItem.decodeAtResearchLab",
                "AshfallAdapterCoreExplorationRuntime.poiDiscovered"));
        report.put("sourceEventHooks", List.copyOf(sourceEventHooks));
        report.put("realMutationTargets", List.of(
                "SurvivalData.radiationLevel",
                "SurvivalData.hazardSnapshot",
                "MutationData.activeMutations",
                "QuestData.visitLocation",
                "QuestData.discoverPOI",
                "QuestData.saveAndSync",
                "EchoCoreServices.recordMissionObjective",
                "ServerPlayer.getPersistentData",
                "RadiationCleanserBlockEntity.inventory",
                "AtmosphericScrubberBlockEntity.safeZone"));
        report.put("hardenedRuntimeChecks", List.of(
                "server_player_only",
                "idempotent_special_marker",
                "hazard_snapshot_nullable",
                "nearby_player_cleanser_dispatch",
                "route_objective_mapping_guard",
                "non_minecraft_activation_safe_descriptor"));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "Ashfall hazard activation validated source hooks for radiation, mutation, treatment, lab, vault, and route-check AdapterCore events; live mutation is claimed only by post-mutation evidence."
                : "Ashfall hazard runtime binding is missing required source hooks or route/runtime evidence.");
        return Map.copyOf(report);
    }

    private static List<String> implementedOperationIds() {
        return List.of(
                "hazards.publish_radiation_changed",
                "hazards.publish_mutation_gained",
                "hazards.publish_treatment_applied",
                "hazards.publish_med_bay_used",
                "hazards.publish_cleanser_used",
                "hazards.publish_scrubber_used",
                "hazards.publish_lab_objective",
                "hazards.publish_vault_objective",
                "hazards.publish_route_check");
    }

    private static List<String> validate(
            Map<String, Object> eventBridge,
            Map<String, Object> midgameRouteBootstrap,
            Map<String, Object> machineRuntimeBinding,
            Set<String> sourceEventHooks) {
        List<String> diagnostics = new ArrayList<>();
        if (!"adaptercore.native_event".equals(value(eventBridge, "bridge"))) {
            diagnostics.add("AdapterCore native event bridge is missing.");
        }
        if (!"PASS".equals(value(midgameRouteBootstrap, "status"))) {
            diagnostics.add("Midgame route bootstrap did not pass.");
        }
        if (!"PASS".equals(value(machineRuntimeBinding, "status"))) {
            diagnostics.add("Machine runtime binding did not pass.");
        }
        for (String eventHook : List.of(
                "ashfall.radiation_changed",
                "ashfall.mutation_gained",
                "ashfall.treatment_applied",
                "ashfall.hazard_route_check",
                "ashfall.lab_objective",
                "ashfall.vault_objective",
                "ashfall.location_visited")) {
            if (!sourceEventHooks.contains(eventHook)) {
                diagnostics.add("Missing source event hook " + eventHook + ".");
            }
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

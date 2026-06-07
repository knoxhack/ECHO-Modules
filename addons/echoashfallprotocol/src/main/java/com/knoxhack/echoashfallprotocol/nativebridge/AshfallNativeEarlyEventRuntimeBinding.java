package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AshfallNativeEarlyEventRuntimeBinding {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeEarlyEventRuntimeBinding() {
    }

    public static Map<String, Object> describe(
            Map<String, Object> eventBridge,
            Map<String, Object> gameplayBootstrap) {
        List<String> implementedOperationIds = implementedOperationIds();
        Set<String> sourceEventHooks = sourceEventHooks(eventBridge);
        List<String> diagnostics = validate(eventBridge, gameplayBootstrap, sourceEventHooks);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:early_event_native_runtime_binding");
        report.put("moduleId", MODULE_ID);
        report.put("bridge", "adaptercore.native_early_event_runtime");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "Phase 4 AdapterCore early-event runtime source binding descriptor");
        report.put("sourceRuntimeClass", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime");
        report.put("sourceRuntimePublisher", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime.publish");
        report.put("sourceAdapterCoreEventBridge", value(eventBridge, "bridge"));
        report.put("sourceGameplayBootstrap", value(gameplayBootstrap, "id"));
        report.put("executionMode", "native_live_adaptercore_early_event_runtime");
        report.put("runtimeClassRequiresMinecraft", true);
        report.put("safeToEvaluateDuringNativeActivation", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("realNativeStateMutationImplemented", false);
        report.put("liveRuntimeMutationImplemented", false);
        report.put("earlyEventRuntimeBound", false);
        report.put("earlyEventRuntimeBindingPrepared", diagnostics.isEmpty());
        report.put("implementedNativeInterfaces", List.of());
        report.put("declaredNativeInterfaces", List.of("EchoNativeRuntimeHost.Events"));
        report.put("implementedEventCount", 0);
        report.put("implementedOperationIds", List.of());
        report.put("declaredEventCount", implementedOperationIds.size());
        report.put("declaredOperationIds", implementedOperationIds);
        report.put("implementedRuntimeEvents", List.of());
        report.put("declaredRuntimeEvents", List.of(
                "player.item_collected",
                "player.item_used",
                "player.block_placed",
                "player.recipe_crafted",
                "player.shelter_slept",
                "ashfall.special_marker"));
        report.put("implementedSpecialMarkers", List.of(
                "water:dirty_collected",
                "water:emergency_filtered",
                "water:clean_consumed",
                "shelter:slept",
                "power:priority_set"));
        report.put("sourceRuntimeHandlers", List.of(
                "AshfallAdapterCoreEarlyEventRuntime.onItemObtained",
                "AshfallAdapterCoreEarlyEventRuntime.onItemConsumed",
                "AshfallAdapterCoreEarlyEventRuntime.onRecipeCrafted",
                "AshfallAdapterCoreEarlyEventRuntime.onPlayerWakeUp",
                "MissionBlockPlaceTracker.onPlace",
                "WaterCollectionHandler.fillDirtyWater",
                "RainCollectorBlockEntity.fillBottle",
                "CleanWaterItem.applyWaterEffects",
                "EmergencyBunkBlock.useWithoutItem",
                "LoadDistributorBlock.useWithoutItem"));
        report.put("sourceEventHooks", List.copyOf(sourceEventHooks));
        report.put("realMutationTargets", List.of(
                "EchoCoreServices.recordMissionObjective",
                "QuestData.visitLocation",
                "QuestData.saveAndSync",
                "ServerPlayer.getPersistentData"));
        report.put("hardenedRuntimeChecks", List.of(
                "server_player_only",
                "empty_stack_ignored",
                "identifier_parse_guard",
                "idempotent_special_marker",
                "server_side_block_interactions",
                "mission_target_validation"));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "Ashfall early-event activation validated source hooks for AdapterCore Events runtime publishing; live mutation is claimed only by post-mutation evidence."
                : "Ashfall early-event runtime binding is missing required source hooks or bootstrap evidence.");
        return Map.copyOf(report);
    }

    private static List<String> implementedOperationIds() {
        return List.of(
                "events.publish_item_obtained",
                "events.publish_item_consumed",
                "events.publish_block_placed",
                "events.publish_recipe_crafted",
                "events.publish_dirty_water_collected",
                "events.publish_water_filtered",
                "events.publish_shelter_slept",
                "events.publish_special_marker");
    }

    private static List<String> validate(
            Map<String, Object> eventBridge,
            Map<String, Object> gameplayBootstrap,
            Set<String> sourceEventHooks) {
        List<String> diagnostics = new ArrayList<>();
        if (!"adaptercore.native_event".equals(value(eventBridge, "bridge"))) {
            diagnostics.add("AdapterCore native event bridge is missing.");
        }
        if (!"PASS".equals(value(gameplayBootstrap, "status"))) {
            diagnostics.add("Gameplay bootstrap did not pass.");
        }
        for (String eventHook : List.of(
                "player.block_placed",
                "player.inventory_changed",
                "player.consume_item",
                "player.sleep",
                "ashfall.special_marker",
                "player.craft_item")) {
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

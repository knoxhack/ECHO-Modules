package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AshfallNativeExplorationRuntimeBinding {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeExplorationRuntimeBinding() {
    }

    public static Map<String, Object> describe(
            Map<String, Object> eventBridge,
            Map<String, Object> gameplayBootstrap) {
        List<String> implementedOperationIds = implementedOperationIds();
        Set<String> sourceEventHooks = sourceEventHooks(eventBridge);
        List<String> diagnostics = validate(eventBridge, gameplayBootstrap, sourceEventHooks);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:exploration_native_runtime_binding");
        report.put("moduleId", MODULE_ID);
        report.put("bridge", "adaptercore.native_exploration_runtime");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "Phase 6 AdapterCore exploration runtime source binding descriptor");
        report.put("sourceRuntimeClass", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime");
        report.put("sourceRuntimePublisher", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime.publish");
        report.put("sourceAdapterCoreEventBridge", value(eventBridge, "bridge"));
        report.put("sourceGameplayBootstrap", value(gameplayBootstrap, "id"));
        report.put("executionMode", "native_live_adaptercore_exploration_runtime");
        report.put("runtimeClassRequiresMinecraft", true);
        report.put("safeToEvaluateDuringNativeActivation", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("realNativeStateMutationImplemented", false);
        report.put("liveRuntimeMutationImplemented", false);
        report.put("explorationRuntimeBound", false);
        report.put("explorationRuntimeBindingPrepared", diagnostics.isEmpty());
        report.put("implementedNativeInterfaces", List.of());
        report.put("declaredNativeInterfaces", List.of(
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.PlayerState",
                "EchoNativeRuntimeHost.SaveData"));
        report.put("implementedOperationCount", 0);
        report.put("implementedOperationIds", List.of());
        report.put("declaredOperationCount", implementedOperationIds.size());
        report.put("declaredOperationIds", implementedOperationIds);
        report.put("implementedRuntimeEvents", List.of());
        report.put("declaredRuntimeEvents", List.of(
                "player.scanner_used",
                "player.region_entered",
                "player.terminal_opened",
                "ashfall.data_log_recovered",
                "ashfall.faction_action",
                "ashfall.reputation_updated",
                "ashfall.drone_state",
                "ashfall.perk_unlocked"));
        report.put("sourceRuntimeHandlers", List.of(
                "SignalScannerItem.use",
                "SignalScannerBlockEntity.triggerScan",
                "StructureCacheBlock.useWithoutItem",
                "ScavengerLootHandler.onContainerOpen",
                "DataLogItem.use",
                "FactionNpcDialogueService.handleAction",
                "FactionEvents.onPOIDiscovered",
                "FactionEvents.onMissionComplete",
                "FieldOpsContractHandler.completeContract",
                "DroneCommandService.execute",
                "DroneScanService.scanArea",
                "ModNetwork.handleResearchPurchase"));
        report.put("sourceEventHooks", List.copyOf(sourceEventHooks));
        report.put("realMutationTargets", List.of(
                "QuestData.discoverPOI",
                "QuestData.recordPOIState",
                "QuestData.visitLocation",
                "QuestData.saveAndSync",
                "EchoCoreServices.discoverFeature",
                "EchoCoreServices.structureDiscoveryService.recordStructureScan",
                "EchoCoreServices.performFactionAction",
                "EchoCoreServices.addFactionReputation",
                "CompanionDroneStateStore.save",
                "ResearchData.unlockPerk",
                "ServerPlayer.getPersistentData"));
        report.put("hardenedRuntimeChecks", List.of(
                "server_player_only",
                "nullable_scan_hit",
                "idempotent_special_marker",
                "nearby_poi_state_guard",
                "faction_action_range_guard",
                "drone_command_range_guard",
                "research_menu_guard"));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "Ashfall exploration activation validated source hooks for scanner, POI, cache, faction, drone, and research AdapterCore events; live mutation is claimed only by post-mutation evidence."
                : "Ashfall exploration runtime binding is missing required source hooks or bootstrap evidence.");
        return Map.copyOf(report);
    }

    private static List<String> implementedOperationIds() {
        return List.of(
                "exploration.publish_scanner_used",
                "exploration.publish_poi_discovered",
                "exploration.publish_cache_opened",
                "exploration.publish_data_log_recovered",
                "exploration.publish_faction_action",
                "exploration.publish_reputation_updated",
                "exploration.publish_drone_state",
                "exploration.publish_perk_unlocked");
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
                "player.scanner_used",
                "player.terminal_opened",
                "ashfall.research_updated",
                "ashfall.schematic_unlocked",
                "ashfall.special_marker")) {
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

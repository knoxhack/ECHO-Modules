package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AshfallNativeLateRuntimeBinding {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeLateRuntimeBinding() {
    }

    public static Map<String, Object> describe(
            Map<String, Object> eventBridge,
            Map<String, Object> hazardRuntimeBinding,
            Map<String, Object> lateGameRouteBootstrap) {
        List<String> implementedOperationIds = implementedOperationIds();
        Set<String> sourceEventHooks = sourceEventHooks(eventBridge);
        List<String> diagnostics = validate(eventBridge, hazardRuntimeBinding, lateGameRouteBootstrap, sourceEventHooks);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:late_native_runtime_binding");
        report.put("moduleId", MODULE_ID);
        report.put("bridge", "adaptercore.native_late_runtime");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "Phase 8 AdapterCore late runtime source binding descriptor");
        report.put("sourceRuntimeClass", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime");
        report.put("sourceRuntimePublisher", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime.publish");
        report.put("sourceAdapterCoreEventBridge", value(eventBridge, "bridge"));
        report.put("sourceHazardRuntimeBinding", value(hazardRuntimeBinding, "id"));
        report.put("sourceLateGameRouteBootstrap", value(lateGameRouteBootstrap, "id"));
        report.put("executionMode", "native_live_adaptercore_late_runtime");
        report.put("runtimeClassRequiresMinecraft", true);
        report.put("safeToEvaluateDuringNativeActivation", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("realNativeStateMutationImplemented", false);
        report.put("liveRuntimeMutationImplemented", false);
        report.put("lateRuntimeBound", false);
        report.put("lateRuntimeBindingPrepared", diagnostics.isEmpty());
        report.put("implementedNativeInterfaces", List.of());
        report.put("declaredNativeInterfaces", List.of(
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.PlayerState",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.SaveData",
                "EchoNativeRuntimeHost.Packets"));
        report.put("implementedOperationCount", 0);
        report.put("implementedOperationIds", List.of());
        report.put("declaredOperationCount", implementedOperationIds.size());
        report.put("declaredOperationIds", implementedOperationIds);
        report.put("implementedRuntimeEvents", List.of());
        report.put("declaredRuntimeEvents", List.of(
                "ashfall.boss_defeated",
                "ashfall.relay_activated",
                "player.machine_powered",
                "ashfall.scout_drone_route",
                "ashfall.nexus_state",
                "ashfall.prime_relay_resolved",
                "ashfall.ending_choice",
                "ashfall.post_nexus_persisted"));
        report.put("sourceRuntimeHandlers", List.of(
                "PostNexusEventHandler.creditWardenDefeat",
                "NexusCampaignActions.creditFinaleBoss",
                "NexusRelaySiteService.startOrRecoverEncounter",
                "NexusRelaySiteService.recordPressureMobKill",
                "NexusCampaignActions.resolveRelay",
                "NexusCampaignActions.awakenCore",
                "NexusChoiceService.applyChoice",
                "RelayStationBlock.activateStation",
                "PowerNodeBlock.useWithoutItem",
                "ScoutDroneItem.recordScoutSupport",
                "PostNexusEventHandler.commitProgress"));
        report.put("sourceEventHooks", List.copyOf(sourceEventHooks));
        report.put("realMutationTargets", List.of(
                "PostNexusData.saveAndSync",
                "NexusCampaignData.setDirty",
                "NexusWorldData.recordPowerNodeActivated",
                "NexusWorldData.setChoice",
                "QuestData.visitLocation",
                "QuestData.recordPOIState",
                "QuestData.saveAndSync",
                "RadioNetwork.activateStation",
                "EchoCoreServices.recordMissionObjective",
                "ServerPlayer.getPersistentData",
                "EchoNetSend.toPlayer"));
        report.put("hardenedRuntimeChecks", List.of(
                "server_player_only",
                "nullable_position_guard",
                "idempotent_special_marker",
                "shared_world_state_snapshot",
                "path_choice_guard",
                "non_minecraft_activation_safe_descriptor"));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "Ashfall late-game activation validated source hooks for boss, relay, power node, Nexus, ending-choice, and post-Nexus AdapterCore events; live mutation is claimed only by post-mutation evidence."
                : "Ashfall late runtime binding is missing required source hooks or upstream runtime evidence.");
        return Map.copyOf(report);
    }

    private static List<String> implementedOperationIds() {
        return List.of(
                "late.publish_boss_defeated",
                "late.publish_relay_activated",
                "late.publish_power_node_state",
                "late.publish_scout_drone_route",
                "late.publish_nexus_state",
                "late.publish_prime_relay_resolved",
                "late.publish_ending_choice",
                "late.publish_post_nexus_persisted");
    }

    private static List<String> validate(
            Map<String, Object> eventBridge,
            Map<String, Object> hazardRuntimeBinding,
            Map<String, Object> lateGameRouteBootstrap,
            Set<String> sourceEventHooks) {
        List<String> diagnostics = new ArrayList<>();
        if (!"adaptercore.native_event".equals(value(eventBridge, "bridge"))) {
            diagnostics.add("AdapterCore native event bridge is missing.");
        }
        if (!"PASS".equals(value(hazardRuntimeBinding, "status"))) {
            diagnostics.add("Hazard runtime binding did not pass.");
        }
        if (!"PASS".equals(value(lateGameRouteBootstrap, "status"))) {
            diagnostics.add("Late-game route bootstrap did not pass.");
        }
        for (String eventHook : List.of(
                "ashfall.boss_defeated",
                "ashfall.relay_activated",
                "player.machine_powered",
                "ashfall.scout_drone_route",
                "ashfall.nexus_state",
                "ashfall.prime_relay_resolved",
                "ashfall.ending_choice",
                "ashfall.post_nexus_persisted")) {
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

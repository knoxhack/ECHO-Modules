package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeMinecraftRuntimeAdapterContract {
    private final String moduleId;

    public EchoNativeMinecraftRuntimeAdapterContract(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> prepare(
            String id,
            Map<String, Object> bridgeDispatch,
            Map<String, Object> runtimeTarget) {
        List<AdapterRequirement> requirements = requirements();
        List<Map<String, Object>> invocations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (AdapterRequirement requirement : requirements) {
            Map<String, Object> invocation = invocation(requirement, bridgeDispatch, runtimeTarget);
            invocations.add(invocation);
            if (!"READY_FOR_HOST_ADAPTER".equals(invocation.get("status"))) {
                diagnostics.add("Minecraft runtime adapter invocation " + invocation.get("id") + " is not ready.");
            }
        }
        if (!"PASS".equals(value(bridgeDispatch, "status"))) {
            diagnostics.add("Bridge dispatch did not pass before adapter contract preparation.");
        }
        if (!"PASS".equals(value(runtimeTarget, "status"))) {
            diagnostics.add("Runtime target did not pass before adapter contract preparation.");
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "minecraft runtime adapter contract id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.minecraft_runtime_adapter_contract");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore host invocation contract for Minecraft-backed first-join adapters");
        report.put("executionMode", "adaptercore_jdk_host_invocation_contract");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("hostAdapterContractPrepared", diagnostics.isEmpty());
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("sourceBridgeDispatch", value(bridgeDispatch, "id"));
        report.put("sourceRuntimeTarget", value(runtimeTarget, "id"));
        report.put("requiredAdapterCount", requirements.size());
        report.put("readyInvocationCount", countReady(invocations));
        report.put("invocations", List.copyOf(invocations));
        report.put("remainingExternalRuntimeWork", remainingExternalRuntimeWork());
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared machine-readable host invocations for every Minecraft-backed first-join adapter target without importing or touching Minecraft runtime APIs."
                : "AdapterCore could not prepare every Minecraft-backed first-join adapter invocation.");
        return Map.copyOf(report);
    }

    private static Map<String, Object> invocation(
            AdapterRequirement requirement,
            Map<String, Object> bridgeDispatch,
            Map<String, Object> runtimeTarget) {
        List<Object> payloads = payloads(requirement, runtimeTarget);
        Map<String, Object> dispatchApplication = dispatchApplication(requirement.targetBridgeId(), bridgeDispatch);
        List<String> diagnostics = new ArrayList<>();
        if (dispatchApplication.isEmpty()) {
            diagnostics.add("Missing AdapterCore bridge dispatch application " + requirement.targetBridgeId() + ".");
        }
        if (payloads.isEmpty()) {
            diagnostics.add("Missing runtime payloads for " + requirement.adapterId() + ".");
        }
        if (!dispatchApplication.isEmpty() && !"PASS".equals(dispatchApplication.get("status"))) {
            diagnostics.add("Bridge dispatch application " + requirement.targetBridgeId() + " did not pass.");
        }

        Map<String, Object> invocation = new LinkedHashMap<>();
        invocation.put("id", requirement.adapterId() + ".host_invocation");
        invocation.put("adapterId", requirement.adapterId());
        invocation.put("targetBridgeId", requirement.targetBridgeId());
        invocation.put("adapterCoreBridge", "adaptercore.minecraft_runtime_adapter_contract");
        invocation.put("hostRuntimeApi", requirement.hostRuntimeApi());
        invocation.put("nativeInterface", EchoNativeRuntimeHost.interfaceForHostApi(requirement.hostRuntimeApi()));
        invocation.put("nativeMethod", EchoNativeRuntimeHost.methodForHostApi(requirement.hostRuntimeApi()));
        invocation.put("payloadSource", requirement.payloadSource());
        invocation.put("payloads", List.copyOf(payloads));
        invocation.put("payloadCount", payloads.size());
        invocation.put("sourceOperationIds", dispatchApplication.getOrDefault("operationIds", List.of()));
        invocation.put("idempotencyKey", "adaptercore:" + requirement.adapterId() + ":" + requirement.payloadSource());
        invocation.put("requiresMinecraftRuntime", true);
        invocation.put("adapterCoreContractOnly", true);
        invocation.put("standaloneDuplicateGameplaySystem", false);
        invocation.put("minecraftRuntimeAccessed", false);
        invocation.put("minecraftRuntimeMutated", false);
        invocation.put("minecraftRegistryMutated", false);
        invocation.put("nativeStateConsumed", false);
        invocation.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        invocation.put("liveRuntimeMutationConsumed", false);
        invocation.put("diagnostics", List.copyOf(diagnostics));
        invocation.put("status", diagnostics.isEmpty() ? "READY_FOR_HOST_ADAPTER" : "BLOCKED");
        return Map.copyOf(invocation);
    }

    private static List<AdapterRequirement> requirements() {
        return List.of(
                requirement("minecraft_backed_inventory_writer", "native_player_inventory_bridge",
                        "inventory.add_or_replace_item", "inventoryWrites"),
                requirement("minecraft_backed_structure_placer", "native_world_structure_placement_bridge",
                        "world.place_structure", "structurePlacements"),
                requirement("minecraft_backed_player_positioner", "native_player_position_bridge",
                        "player.teleport", "teleportAnchor"),
                requirement("minecraft_backed_respawn_binder", "native_respawn_position_bridge",
                        "player.bind_respawn", "respawnAnchor"),
                requirement("minecraft_backed_player_state_writer", "native_player_state_bridge",
                        "player.persistent_state.write", "playerStateWrites"),
                requirement("minecraft_backed_advancement_granter", "native_advancement_bridge",
                        "player.advancements.grant", "advancements"),
                requirement("minecraft_backed_screen_packet_sender", "native_screen_packet_bridge",
                        "network.send_clientbound_screen_packet", "screenPackets"),
                requirement("minecraft_backed_hud_notification_sender", "native_hud_notification_bridge",
                        "hud.publish_notification", "hudNotifications")
        );
    }

    private static AdapterRequirement requirement(
            String adapterId,
            String targetBridgeId,
            String hostRuntimeApi,
            String payloadSource) {
        return new AdapterRequirement(adapterId, targetBridgeId, hostRuntimeApi, payloadSource);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dispatchApplication(String bridgeId, Map<String, Object> bridgeDispatch) {
        Object rawApplications = bridgeDispatch == null ? null : bridgeDispatch.get("applications");
        if (rawApplications instanceof List<?> applications) {
            for (Object application : applications) {
                if (application instanceof Map<?, ?> map && bridgeId.equals(map.get("bridgeId"))) {
                    return (Map<String, Object>) map;
                }
            }
        }
        return Map.of();
    }

    private static List<Object> payloads(AdapterRequirement requirement, Map<String, Object> runtimeTarget) {
        if (runtimeTarget == null || runtimeTarget.isEmpty()) {
            return List.of();
        }
        Object rawPayload = runtimeTarget.get(requirement.payloadSource());
        if (rawPayload instanceof List<?> list) {
            return List.copyOf(list);
        }
        if (rawPayload instanceof Map<?, ?> map && !map.isEmpty()) {
            return List.of(Map.copyOf(map));
        }
        if (rawPayload instanceof String text && !text.isBlank()) {
            return List.of(text);
        }
        return List.of();
    }

    private static int countReady(List<Map<String, Object>> invocations) {
        int count = 0;
        for (Map<String, Object> invocation : invocations) {
            if ("READY_FOR_HOST_ADAPTER".equals(invocation.get("status"))) {
                count++;
            }
        }
        return count;
    }

    private static List<String> remainingExternalRuntimeWork() {
        return List.of(
                "implement host adapter minecraft_backed_inventory_writer against the native loader player inventory API",
                "implement host adapter minecraft_backed_structure_placer against the native loader world/structure API",
                "implement host adapter minecraft_backed_player_positioner against the native loader player teleport API",
                "implement host adapter minecraft_backed_respawn_binder against the native loader respawn API",
                "implement host adapter minecraft_backed_player_state_writer against the native loader player data API",
                "implement host adapter minecraft_backed_advancement_granter against the native loader advancement API",
                "implement host adapter minecraft_backed_screen_packet_sender against the native loader networking API",
                "implement host adapter minecraft_backed_hud_notification_sender against the native loader HUD API"
        );
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private record AdapterRequirement(
            String adapterId,
            String targetBridgeId,
            String hostRuntimeApi,
            String payloadSource) {
    }
}

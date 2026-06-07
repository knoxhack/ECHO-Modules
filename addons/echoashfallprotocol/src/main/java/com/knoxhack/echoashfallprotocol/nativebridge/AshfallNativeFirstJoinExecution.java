package com.knoxhack.echoashfallprotocol.nativebridge;

import com.knoxhack.echo.adaptercore.EchoNativeCommandBridge;
import com.knoxhack.echo.adaptercore.EchoNativeMinecraftRuntimeAdapterContract;
import com.knoxhack.echo.adaptercore.EchoNativeMinecraftRuntimeHostCallQueue;
import com.knoxhack.echo.adaptercore.EchoNativePlayerRecoveryBridgeDispatch;
import com.knoxhack.echo.adaptercore.EchoNativePlayerStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativePlayerRecoveryRuntimeTarget;
import com.knoxhack.echo.adaptercore.EchoNativeWorldStateBridge;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class AshfallNativeFirstJoinExecution {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeFirstJoinExecution() {
    }

    public static Map<String, Object> execute(Map<String, Object> firstJoinProfile, Map<String, String> context) {
        Map<String, Object> transaction = childMap(firstJoinProfile, "adapterCoreTransaction");
        List<Map<String, Object>> operations = childList(transaction, "operations");
        Set<String> loadedModules = loadedModules(firstJoinProfile, context);
        EchoNativeCommandBridge commandBridge = new EchoNativeCommandBridge(MODULE_ID);

        for (Map<String, Object> operation : operations) {
            command(operation, loadedModules, commandBridge);
        }

        Map<String, Object> execution = commandBridge.describe(
                "echoashfallprotocol:first_join_crash_recovery_execution",
                "AdapterCore native first-join command execution",
                valueOrDefault(firstJoinProfile, "id", "echoashfallprotocol:first_join_crash_recovery"),
                valueOrDefault(transaction, "id", "echoashfallprotocol:first_join_crash_recovery_transaction"),
                requiredOperationIds(),
                pendingConcreteRuntimeBridges(),
                "AdapterCore prepared the first-join recovery profile as an ordered native command queue for player, inventory, world, respawn, advancement, UI/HUD, Recovery, and HoloMap bridge targets without claiming live host execution.",
                "AdapterCore first-join command execution is missing required bridge commands.");
        execution.put("loadedModuleHints", List.copyOf(loadedModules));
        execution.put("appliedPlayerState", new EchoNativePlayerStateBridge(MODULE_ID).apply(
                "echoashfallprotocol:first_join_player_state_application",
                childList(execution, "commands")));
        execution.put("appliedWorldState", new EchoNativeWorldStateBridge(MODULE_ID).apply(
                "echoashfallprotocol:first_join_world_state_application",
                childList(execution, "commands"),
                childMap(firstJoinProfile, "dropPodPlacement")));
        Map<String, Object> firstJoinRuntimeTarget = new EchoNativePlayerRecoveryRuntimeTarget(MODULE_ID).execute(
                "echoashfallprotocol:first_join_player_recovery_runtime_target",
                childList(execution, "commands"),
                childMap(firstJoinProfile, "dropPodPlacement"));
        execution.put("firstJoinRuntimeTarget", firstJoinRuntimeTarget);
        Map<String, Object> firstJoinBridgeDispatch = new EchoNativePlayerRecoveryBridgeDispatch(MODULE_ID).dispatch(
                "echoashfallprotocol:first_join_player_recovery_bridge_dispatch",
                childList(execution, "commands"),
                firstJoinRuntimeTarget);
        execution.put("firstJoinBridgeDispatch", firstJoinBridgeDispatch);
        Map<String, Object> minecraftRuntimeAdapterContract = new EchoNativeMinecraftRuntimeAdapterContract(MODULE_ID).prepare(
                "echoashfallprotocol:first_join_minecraft_runtime_adapter_contract",
                firstJoinBridgeDispatch,
                firstJoinRuntimeTarget);
        execution.put("minecraftRuntimeAdapterContract", minecraftRuntimeAdapterContract);
        execution.put("minecraftRuntimeHostCallQueue", new EchoNativeMinecraftRuntimeHostCallQueue(MODULE_ID).prepare(
                "echoashfallprotocol:first_join_minecraft_runtime_host_call_queue",
                minecraftRuntimeAdapterContract));
        return execution;
    }

    private static void command(Map<String, Object> operation, Set<String> loadedModules, EchoNativeCommandBridge commandBridge) {
        String operationId = stringValue(operation, "id");
        String bridge = stringValue(operation, "bridge");
        String targetSurface = targetSurface(operationId, bridge);
        Map<String, Object> payload = childMap(operation, "payload");
        boolean optionalModulePresent = optionalModulePresent(payload, loadedModules);
        boolean skipped = operationId.equals("inventory.write_terminal_remote_if_loaded") && !optionalModulePresent;
        int order = order(operation);
        String sourceParity = String.valueOf(operation.getOrDefault("sourceParity", ""));
        if (skipped) {
            commandBridge.skippedCommand(order, targetSurface, operationId, bridge, sourceParity, payload);
        } else {
            commandBridge.command(order, targetSurface, operationId, bridge, sourceParity, payload);
        }
    }

    private static boolean optionalModulePresent(Map<String, Object> payload, Set<String> loadedModules) {
        Object condition = payload.get("condition");
        if (!(condition instanceof String text) || !text.startsWith("module_loaded:")) {
            return true;
        }
        String module = text.substring("module_loaded:".length()).toLowerCase(Locale.ROOT);
        return loadedModules.contains(module);
    }

    private static String targetSurface(String operationId, String bridge) {
        if (operationId.startsWith("ui.") || operationId.startsWith("hud.")) {
            return "ui_hud_screen_safe";
        }
        if (operationId.startsWith("recovery.")) {
            return "player_recovery";
        }
        if (operationId.startsWith("holomap.")) {
            return "holomap_lens_codex_wiki";
        }
        if (bridge.startsWith("echorecovery:")) {
            return "player_recovery";
        }
        if (bridge.startsWith("echoholomap:")) {
            return "holomap_lens_codex_wiki";
        }
        return "player_recovery";
    }

    private static List<String> pendingConcreteRuntimeBridges() {
        return List.of(
                "native_player_inventory_bridge",
                "native_world_structure_placement_bridge",
                "native_player_position_bridge",
                "native_respawn_position_bridge",
                "native_player_state_bridge",
                "native_advancement_bridge",
                "native_screen_packet_bridge",
                "native_hud_notification_bridge"
        );
    }

    private static List<String> requiredOperationIds() {
        return List.of(
                "inventory.write_starter_note",
                "world.place_personal_drop_pod",
                "player.teleport_to_drop_pod_interior",
                "player.bind_drop_pod_respawn",
                "player.write_first_join_state",
                "player.grant_find_drop_pod_advancement",
                "ui.dispatch_welcome_screen",
                "hud.publish_opening_recovery_notice",
                "recovery.publish_drop_pod_field_cache_context",
                "holomap.publish_recovery_route_markers",
                "repair.rescue_underground_or_missing_respawn"
        );
    }

    private static int order(Map<String, Object> operation) {
        Object value = operation.get("order");
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Set<String> loadedModules(Map<String, Object> firstJoinProfile, Map<String, String> context) {
        TreeSet<String> modules = new TreeSet<>();
        Object hints = firstJoinProfile == null ? null : firstJoinProfile.get("loadedModuleHints");
        if (hints instanceof List<?> list) {
            for (Object hint : list) {
                if (hint instanceof String module && !module.isBlank()) {
                    modules.add(module.toLowerCase(Locale.ROOT));
                }
            }
        }
        if (context != null) {
            addModules(modules, context.get("loadedModules"));
            addModules(modules, context.get("modules"));
            addModules(modules, context.get("optionalModules"));
        }
        return modules;
    }

    private static void addModules(Set<String> modules, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String part : raw.split("[,;\\s]+")) {
            String module = part.trim().toLowerCase(Locale.ROOT);
            if (!module.isEmpty()) {
                modules.add(module);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> childMap(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> childList(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static Object valueOrDefault(Map<String, Object> map, String key, Object fallback) {
        return map == null ? fallback : map.getOrDefault(key, fallback);
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof String text ? text : "";
    }
}

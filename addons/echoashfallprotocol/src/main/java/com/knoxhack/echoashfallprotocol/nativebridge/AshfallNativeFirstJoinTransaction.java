package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeFirstJoinTransaction {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeFirstJoinTransaction() {
    }

    public static Map<String, Object> describe(Map<String, Object> firstJoinProfile) {
        List<Map<String, Object>> operations = operations();
        List<String> diagnostics = validate(operations);
        boolean verified = diagnostics.isEmpty();

        Map<String, Object> transaction = new LinkedHashMap<>();
        transaction.put("id", "echoashfallprotocol:first_join_crash_recovery_transaction");
        transaction.put("moduleId", MODULE_ID);
        transaction.put("adapterCoreBridge", true);
        transaction.put("implementationTarget", "AdapterCore native player/recovery bridge");
        transaction.put("sourceProfile", firstJoinProfile == null
                ? "echoashfallprotocol:first_join_crash_recovery"
                : firstJoinProfile.getOrDefault("id", "echoashfallprotocol:first_join_crash_recovery"));
        transaction.put("minecraftRuntimeAccessed", false);
        transaction.put("minecraftRegistryMutated", false);
        transaction.put("standaloneDuplicateGameplaySystem", false);
        transaction.put("executionMode", "jdk_only_ordered_operation_rehearsal");
        transaction.put("operationCount", operations.size());
        transaction.put("operations", operations);
        transaction.put("liveBridgeStatus", "pending_adaptercore_native_player_world_ui_bridges");
        transaction.put("diagnostics", diagnostics);
        transaction.put("status", verified ? "PASS" : "FAIL");
        transaction.put("summary", verified
                ? "AdapterCore first-join transaction rehearsal emits ordered native operations for inventory, drop pod placement, respawn, advancement, welcome/HUD dispatch, recovery/HoloMap handoff, and existing-player repair."
                : "AdapterCore first-join transaction rehearsal is missing required native operations.");
        return transaction;
    }

    private static List<Map<String, Object>> operations() {
        List<Map<String, Object>> operations = new ArrayList<>();
        operations.add(operation(10, "inventory.write_starter_note", "native_player_inventory_bridge",
                "Give the ECHO-7 first-ten-minutes field manual with the same custom name/data as the native contract.",
                "PlayerStartingKitHandler welcomeNote", "pending_live_bridge",
                map("item", "echoashfallprotocol:field_manual",
                        "customNameKey", "item.EchoAshfallProtocol.echo_starter_note.name",
                        "payload", "firstTenMinuteChecklist")));
        operations.add(operation(20, "inventory.write_terminal_remote_if_loaded", "native_player_inventory_bridge",
                "Give the Terminal remote when echoterminal is loaded and the player does not already have one.",
                "PlayerStartingKitHandler.giveTerminalRemoteIfAvailable", "pending_live_bridge",
                map("item", "echoterminal:echo_terminal_remote",
                        "condition", "module_loaded:echoterminal",
                        "dedupe", true)));
        operations.add(operation(30, "world.place_personal_drop_pod", "native_world_structure_placement_bridge",
                "Place or reuse the player's personal drop pod using the native surface-search radius and fallback contract.",
                "ProceduralStructureGenerator.placeStartingDropPod", "pending_live_bridge",
                map("structure", "echoashfallprotocol:drop_pod",
                        "minRadiusChunks", 2,
                        "maxRadiusChunks", 8,
                        "minSpacingChunks", 3,
                        "minimumStartingSurfaceY", 48)));
        operations.add(operation(40, "player.teleport_to_drop_pod_interior", "native_player_position_bridge",
                "Move the player to the resolved personal drop-pod interior and reset view rotation.",
                "ServerPlayer.teleportTo", "pending_live_bridge",
                map("anchor", "personal_starting_drop_pod.interior",
                        "yaw", 0.0F,
                        "pitch", 0.0F)));
        operations.add(operation(50, "player.bind_drop_pod_respawn", "native_respawn_position_bridge",
                "Bind the player's respawn position to the personal drop-pod interior.",
                "PlayerStartingKitHandler.setDropPodRespawn", "pending_live_bridge",
                map("anchor", "personal_starting_drop_pod.interior",
                        "forced", true)));
        operations.add(operation(60, "player.write_first_join_state", "native_player_state_bridge",
                "Persist the Ashfall first-join flag and quest drop-pod initialized bit.",
                "player persistent data and ModAttachments.QUEST_DATA", "pending_live_bridge",
                map("firstJoinFlag", "ashes_of_tomorrow.received_kit",
                        "firstJoinFlagValue", true,
                        "questDropPodInitialized", true)));
        operations.add(operation(70, "player.grant_find_drop_pod_advancement", "native_advancement_bridge",
                "Grant the same find_drop_pod advancement criterion as the native contract.",
                "PlayerStartingKitHandler.grantFindDropPodAdvancement", "pending_live_bridge",
                map("advancement", "echoashfallprotocol:find_drop_pod",
                        "criterion", "found_drop_pod")));
        operations.add(operation(80, "ui.dispatch_welcome_screen", "native_screen_packet_bridge",
                "Dispatch the Ashfall welcome screen packet to the joining player.",
                "WelcomeScreenPacket", "pending_live_bridge",
                map("packet", "echoashfallprotocol:welcome_screen",
                        "kind", "CLIENTBOUND_SYNC")));
        operations.add(operation(90, "hud.publish_opening_recovery_notice", "native_hud_notification_bridge",
                "Publish the opening mission and hazard lines through screen-safe HUD surfaces.",
                "SurvivalHudOverlay/TerminalHudNoticeSurface", "pending_live_bridge",
                map("missionLine", "Place an Ash Campfire near the crash site",
                        "hazardLine", "AIR stable; hazards marked.",
                        "anchor", "top_left_safe_area")));
        operations.add(operation(100, "recovery.publish_drop_pod_field_cache_context", "echorecovery:field_cache_service",
                "Expose the drop-pod and first cache context to Recovery for future death/cache handoff.",
                "echorecovery native player_recovery surface", "pending_live_bridge",
                map("deathRecoveryHandoff", "echorecovery:field_cache_service",
                        "routeObjective", "ashfall:recover_crash_cache")));
        operations.add(operation(110, "holomap.publish_recovery_route_markers", "echoholomap:map_state_service",
                "Expose first-month and first-major-route recovery markers to HoloMap consumers.",
                "HoloMap native route layers", "pending_live_bridge",
                map("layers", List.of(
                        "echoashfallprotocol:first_month_field_intel",
                        "echoashfallprotocol:first_major_route"))));
        operations.add(operation(120, "repair.rescue_underground_or_missing_respawn", "native_player_recovery_repair_bridge",
                "Run the existing-player repair path for unsafe underground pods, missing respawn, and Terminal remote reissue.",
                "PlayerStartingKitHandler rescue/repair branches", "pending_live_bridge",
                map("repairs", List.of(
                        "rescue_underground_starting_pod_below_y_48",
                        "repair_missing_drop_pod_respawn",
                        "reissue_terminal_remote_if_available"))));
        return List.copyOf(operations);
    }

    private static Map<String, Object> operation(int order, String id, String bridge, String summary,
                                                 String sourceParity, String status, Map<String, Object> payload) {
        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("order", order);
        operation.put("id", id);
        operation.put("bridge", bridge);
        operation.put("summary", summary);
        operation.put("sourceParity", sourceParity);
        operation.put("status", status);
        operation.put("adapterCoreOperation", true);
        operation.put("minecraftRuntimeAccessed", false);
        operation.put("payload", payload);
        return operation;
    }

    private static List<String> validate(List<Map<String, Object>> operations) {
        List<String> diagnostics = new ArrayList<>();
        requireOperation(operations, "inventory.write_starter_note", diagnostics);
        requireOperation(operations, "inventory.write_terminal_remote_if_loaded", diagnostics);
        requireOperation(operations, "world.place_personal_drop_pod", diagnostics);
        requireOperation(operations, "player.bind_drop_pod_respawn", diagnostics);
        requireOperation(operations, "player.grant_find_drop_pod_advancement", diagnostics);
        requireOperation(operations, "ui.dispatch_welcome_screen", diagnostics);
        requireOperation(operations, "hud.publish_opening_recovery_notice", diagnostics);
        requireOperation(operations, "recovery.publish_drop_pod_field_cache_context", diagnostics);
        requireOperation(operations, "holomap.publish_recovery_route_markers", diagnostics);
        requireOperation(operations, "repair.rescue_underground_or_missing_respawn", diagnostics);
        return List.copyOf(diagnostics);
    }

    private static void requireOperation(List<Map<String, Object>> operations, String id, List<String> diagnostics) {
        boolean found = operations.stream().anyMatch(operation -> id.equals(operation.get("id")));
        if (!found) {
            diagnostics.add("Missing first-join transaction operation " + id + ".");
        }
    }

    private static Map<String, Object> map(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("map entries must be key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], entries[index + 1]);
        }
        return result;
    }
}

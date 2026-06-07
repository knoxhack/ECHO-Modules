package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativePlayerStateBridge {
    private final String moduleId;
    private final List<Map<String, Object>> inventoryWrites = new ArrayList<>();
    private final List<Map<String, Object>> skippedOptionalInventoryWrites = new ArrayList<>();
    private final List<Map<String, Object>> placedStructures = new ArrayList<>();
    private final List<Map<String, Object>> screenPackets = new ArrayList<>();
    private final List<Map<String, Object>> hudNotifications = new ArrayList<>();
    private final List<Map<String, Object>> recoveryHandoffs = new ArrayList<>();
    private final List<Map<String, Object>> holomapMarkers = new ArrayList<>();
    private final List<String> repairActions = new ArrayList<>();
    private final Map<String, Object> playerStateWrites = new LinkedHashMap<>();
    private final List<String> advancements = new ArrayList<>();
    private String teleportAnchor = "";
    private String respawnAnchor = "";
    private boolean optionalTerminalRemoteSkipped = false;

    public EchoNativePlayerStateBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> apply(String id, List<Map<String, Object>> commands) {
        if (commands != null) {
            for (Map<String, Object> command : commands) {
                if ("prepared_as_adaptercore_command".equals(command.get("status"))) {
                    applyCommand(command);
                } else if ("skipped_module_not_loaded".equals(command.get("status"))) {
                    applySkippedCommand(command);
                }
            }
        }

        List<String> diagnostics = validate();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "state report id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_player_state");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore native player-state command application");
        report.put("executionMode", "adaptercore_jdk_only_player_state_application");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRegistryMutated", false);
        report.put("liveRuntimeMutation", false);
        report.put("inventoryWrites", List.copyOf(inventoryWrites));
        report.put("skippedOptionalInventoryWrites", List.copyOf(skippedOptionalInventoryWrites));
        report.put("optionalTerminalRemoteSkipped", optionalTerminalRemoteSkipped);
        report.put("placedStructures", List.copyOf(placedStructures));
        report.put("teleportAnchor", teleportAnchor);
        report.put("respawnAnchor", respawnAnchor);
        report.put("playerStateWrites", Map.copyOf(playerStateWrites));
        report.put("advancements", List.copyOf(advancements));
        report.put("screenPackets", List.copyOf(screenPackets));
        report.put("hudNotifications", List.copyOf(hudNotifications));
        report.put("recoveryHandoffs", List.copyOf(recoveryHandoffs));
        report.put("holomapMarkers", List.copyOf(holomapMarkers));
        report.put("repairActions", List.copyOf(repairActions));
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore applied first-join commands to a JDK-only native player-state report for inventory, drop pod, respawn, advancement, screen, HUD, recovery, HoloMap, and repair outcomes."
                : "AdapterCore native player-state report is missing required first-join outcomes.");
        return report;
    }

    @SuppressWarnings("unchecked")
    private void applyCommand(Map<String, Object> command) {
        String operationId = String.valueOf(command.get("operationId"));
        Map<String, Object> payload = command.get("payload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        switch (operationId) {
            case "inventory.write_starter_note", "inventory.write_terminal_remote_if_loaded" ->
                    inventoryWrites.add(Map.copyOf(payload));
            case "world.place_personal_drop_pod" ->
                    placedStructures.add(Map.copyOf(payload));
            case "player.teleport_to_drop_pod_interior" ->
                    teleportAnchor = String.valueOf(payload.getOrDefault("anchor", ""));
            case "player.bind_drop_pod_respawn" ->
                    respawnAnchor = String.valueOf(payload.getOrDefault("anchor", ""));
            case "player.write_first_join_state" ->
                    playerStateWrites.putAll(payload);
            case "player.grant_find_drop_pod_advancement" ->
                    advancements.add(String.valueOf(payload.getOrDefault("advancement", "")));
            case "ui.dispatch_welcome_screen" ->
                    screenPackets.add(Map.copyOf(payload));
            case "hud.publish_opening_recovery_notice" ->
                    hudNotifications.add(Map.copyOf(payload));
            case "recovery.publish_drop_pod_field_cache_context" ->
                    recoveryHandoffs.add(Map.copyOf(payload));
            case "holomap.publish_recovery_route_markers" ->
                    holomapMarkers.add(Map.copyOf(payload));
            case "repair.rescue_underground_or_missing_respawn" ->
                    addRepairActions(payload.get("repairs"));
            default -> {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applySkippedCommand(Map<String, Object> command) {
        String operationId = String.valueOf(command.get("operationId"));
        Map<String, Object> payload = command.get("payload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        if ("inventory.write_terminal_remote_if_loaded".equals(operationId)) {
            skippedOptionalInventoryWrites.add(Map.copyOf(payload));
            optionalTerminalRemoteSkipped = true;
        }
    }

    private void addRepairActions(Object repairs) {
        if (repairs instanceof List<?> list) {
            for (Object repair : list) {
                if (repair instanceof String text && !text.isBlank()) {
                    repairActions.add(text);
                }
            }
        }
    }

    private List<String> validate() {
        List<String> diagnostics = new ArrayList<>();
        requireInventory("echoashfallprotocol:field_manual", diagnostics);
        if (!optionalTerminalRemoteSkipped) {
            requireInventory("echoterminal:echo_terminal_remote", diagnostics);
        }
        requireStructure("echoashfallprotocol:drop_pod", diagnostics);
        requireText(teleportAnchor, "Missing first-join teleport anchor.", diagnostics);
        requireText(respawnAnchor, "Missing first-join respawn anchor.", diagnostics);
        if (!Boolean.TRUE.equals(playerStateWrites.get("firstJoinFlagValue"))) {
            diagnostics.add("Missing first-join persistent flag write.");
        }
        if (!Boolean.TRUE.equals(playerStateWrites.get("questDropPodInitialized"))) {
            diagnostics.add("Missing quest drop-pod initialized state write.");
        }
        if (!advancements.contains("echoashfallprotocol:find_drop_pod")) {
            diagnostics.add("Missing find_drop_pod advancement grant.");
        }
        requirePacket("echoashfallprotocol:welcome_screen", diagnostics);
        if (hudNotifications.isEmpty()) {
            diagnostics.add("Missing opening HUD notification.");
        }
        if (recoveryHandoffs.isEmpty()) {
            diagnostics.add("Missing Recovery field-cache handoff.");
        }
        if (holomapMarkers.isEmpty()) {
            diagnostics.add("Missing HoloMap recovery marker handoff.");
        }
        requireRepair("rescue_underground_starting_pod_below_y_48", diagnostics);
        requireRepair("repair_missing_drop_pod_respawn", diagnostics);
        return List.copyOf(diagnostics);
    }

    private void requireInventory(String item, List<String> diagnostics) {
        boolean found = inventoryWrites.stream().anyMatch(write -> item.equals(write.get("item")));
        if (!found) {
            diagnostics.add("Missing inventory write for " + item + ".");
        }
    }

    private void requireStructure(String structure, List<String> diagnostics) {
        boolean found = placedStructures.stream().anyMatch(write -> structure.equals(write.get("structure")));
        if (!found) {
            diagnostics.add("Missing structure placement for " + structure + ".");
        }
    }

    private void requirePacket(String packet, List<String> diagnostics) {
        boolean found = screenPackets.stream().anyMatch(write -> packet.equals(write.get("packet")));
        if (!found) {
            diagnostics.add("Missing screen packet " + packet + ".");
        }
    }

    private void requireRepair(String repair, List<String> diagnostics) {
        if (!repairActions.contains(repair)) {
            diagnostics.add("Missing repair action " + repair + ".");
        }
    }

    private static void requireText(String value, String message, List<String> diagnostics) {
        if (value == null || value.isBlank()) {
            diagnostics.add(message);
        }
    }
}

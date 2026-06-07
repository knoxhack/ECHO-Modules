package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativePlayerRecoveryRuntimeTarget {
    private static final String PREPARED = "prepared_as_adaptercore_command";
    private static final String SKIPPED = "skipped_module_not_loaded";

    private final String moduleId;
    private final List<Map<String, Object>> inventoryWrites = new ArrayList<>();
    private final List<Map<String, Object>> skippedOptionalInventoryWrites = new ArrayList<>();
    private final List<Map<String, Object>> structurePlacements = new ArrayList<>();
    private final List<Map<String, Object>> screenPackets = new ArrayList<>();
    private final List<Map<String, Object>> hudNotifications = new ArrayList<>();
    private final List<Map<String, Object>> recoveryContexts = new ArrayList<>();
    private final List<String> holomapLayers = new ArrayList<>();
    private final List<String> repairActions = new ArrayList<>();
    private final List<Map<String, Object>> mutationLog = new ArrayList<>();
    private final Set<String> mutationSurfaces = new LinkedHashSet<>();
    private final Set<String> advancements = new LinkedHashSet<>();
    private final Map<String, Object> playerStateWrites = new LinkedHashMap<>();
    private String teleportAnchor = "";
    private String respawnAnchor = "";
    private boolean optionalTerminalRemoteSkipped = false;
    private int preparedCommandCount = 0;

    public EchoNativePlayerRecoveryRuntimeTarget(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> execute(
            String id,
            List<Map<String, Object>> commands,
            Map<String, Object> dropPodPlacementConstraints) {
        if (commands != null) {
            for (Map<String, Object> command : commands) {
                Object status = command.get("status");
                if (PREPARED.equals(status)) {
                    preparedCommandCount++;
                    executeCommand(command, dropPodPlacementConstraints == null ? Map.of() : dropPodPlacementConstraints);
                } else if (SKIPPED.equals(status)) {
                    executeSkippedCommand(command);
                }
            }
        }

        List<String> diagnostics = validate();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "runtime target id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_player_recovery_runtime");
        report.put("adapterCoreBridge", true);
        report.put("adapterSurface", "player_recovery.runtime_target");
        report.put("implementationTarget", "AdapterCore stateful native first-join player/recovery runtime target");
        report.put("executionMode", "adaptercore_jdk_stateful_runtime_target");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("liveRuntimeMutation", false);
        report.put("nativeStateMutated", !mutationLog.isEmpty());
        report.put("noLaunchNativeStateMutated", !mutationLog.isEmpty());
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("unsafeRuntimeWorkStarted", false);
        report.put("preparedCommandCount", preparedCommandCount);
        report.put("executedCommandCount", 0);
        report.put("mutatingOperationCount", mutationLog.size());
        report.put("mutationSurfaces", List.copyOf(mutationSurfaces));
        report.put("mutationLog", List.copyOf(mutationLog));
        report.put("inventoryWrites", List.copyOf(inventoryWrites));
        report.put("skippedOptionalInventoryWrites", List.copyOf(skippedOptionalInventoryWrites));
        report.put("optionalTerminalRemoteSkipped", optionalTerminalRemoteSkipped);
        report.put("structurePlacements", List.copyOf(structurePlacements));
        report.put("teleportAnchor", teleportAnchor);
        report.put("respawnAnchor", respawnAnchor);
        report.put("playerStateWrites", Map.copyOf(playerStateWrites));
        report.put("advancements", List.copyOf(advancements));
        report.put("screenPackets", List.copyOf(screenPackets));
        report.put("hudNotifications", List.copyOf(hudNotifications));
        report.put("recoveryContexts", List.copyOf(recoveryContexts));
        report.put("holomapLayers", List.copyOf(holomapLayers));
        report.put("repairActions", List.copyOf(repairActions));
        report.put("runtimeSnapshot", snapshot());
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared the Ashfall first-join command queue against a stateful no-launch player/recovery runtime packet for inventory, world placement, respawn, advancement, UI/HUD, Recovery, HoloMap, and repair state without claiming live host mutation."
                : "AdapterCore first-join player/recovery runtime target is missing required native state mutations.");
        return report;
    }

    @SuppressWarnings("unchecked")
    private void executeCommand(Map<String, Object> command, Map<String, Object> dropPodPlacementConstraints) {
        String operationId = String.valueOf(command.get("operationId"));
        String targetBridge = String.valueOf(command.get("targetBridge"));
        Map<String, Object> payload = command.get("payload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        switch (operationId) {
            case "inventory.write_starter_note", "inventory.write_terminal_remote_if_loaded" -> {
                inventoryWrites.add(Map.copyOf(payload));
                logMutation(operationId, targetBridge, payload.getOrDefault("item", "inventory"));
            }
            case "world.place_personal_drop_pod" -> {
                Map<String, Object> placement = new LinkedHashMap<>(dropPodPlacementConstraints);
                placement.putAll(payload);
                structurePlacements.add(Map.copyOf(placement));
                logMutation(operationId, targetBridge, placement.getOrDefault("structure", "structure"));
            }
            case "player.teleport_to_drop_pod_interior" -> {
                teleportAnchor = String.valueOf(payload.getOrDefault("anchor", ""));
                logMutation(operationId, targetBridge, teleportAnchor);
            }
            case "player.bind_drop_pod_respawn" -> {
                respawnAnchor = String.valueOf(payload.getOrDefault("anchor", ""));
                logMutation(operationId, targetBridge, respawnAnchor);
            }
            case "player.write_first_join_state" -> {
                playerStateWrites.putAll(payload);
                logMutation(operationId, targetBridge, payload.getOrDefault("firstJoinFlag", "player_state"));
            }
            case "player.grant_find_drop_pod_advancement" -> {
                advancements.add(String.valueOf(payload.getOrDefault("advancement", "")));
                logMutation(operationId, targetBridge, payload.getOrDefault("advancement", "advancement"));
            }
            case "ui.dispatch_welcome_screen" -> {
                screenPackets.add(Map.copyOf(payload));
                logMutation(operationId, targetBridge, payload.getOrDefault("packet", "screen_packet"));
            }
            case "hud.publish_opening_recovery_notice" -> {
                hudNotifications.add(Map.copyOf(payload));
                logMutation(operationId, targetBridge, payload.getOrDefault("missionLine", "hud_notice"));
            }
            case "recovery.publish_drop_pod_field_cache_context" -> {
                recoveryContexts.add(Map.copyOf(payload));
                logMutation(operationId, targetBridge, payload.getOrDefault("deathRecoveryHandoff", "recovery_context"));
            }
            case "holomap.publish_recovery_route_markers" -> {
                addHolomapLayers(payload.get("layers"));
                logMutation(operationId, targetBridge, "holomap_layers");
            }
            case "repair.rescue_underground_or_missing_respawn" -> {
                addRepairActions(payload.get("repairs"));
                logMutation(operationId, targetBridge, "existing_player_repairs");
            }
            default -> {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void executeSkippedCommand(Map<String, Object> command) {
        String operationId = String.valueOf(command.get("operationId"));
        Map<String, Object> payload = command.get("payload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        if ("inventory.write_terminal_remote_if_loaded".equals(operationId)) {
            skippedOptionalInventoryWrites.add(Map.copyOf(payload));
            optionalTerminalRemoteSkipped = true;
        }
    }

    private void logMutation(String operationId, String targetBridge, Object target) {
        Map<String, Object> mutation = new LinkedHashMap<>();
        mutation.put("operationId", AdapterContractGuards.requireText(operationId, "mutation operation id"));
        mutation.put("targetBridge", AdapterContractGuards.requireText(targetBridge, "mutation target bridge"));
        mutation.put("target", String.valueOf(target));
        mutation.put("adapterCoreMutation", true);
        mutation.put("minecraftRuntimeAccessed", false);
        mutationLog.add(mutation);
        mutationSurfaces.add(targetBridge);
    }

    private void addHolomapLayers(Object layers) {
        if (layers instanceof List<?> list) {
            for (Object layer : list) {
                if (layer instanceof String text && !text.isBlank()) {
                    holomapLayers.add(text);
                }
            }
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

    private Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("inventoryItemCount", inventoryWrites.size());
        snapshot.put("structurePlacementCount", structurePlacements.size());
        snapshot.put("teleportAnchor", teleportAnchor);
        snapshot.put("respawnAnchor", respawnAnchor);
        snapshot.put("firstJoinFlagValue", playerStateWrites.getOrDefault("firstJoinFlagValue", false));
        snapshot.put("questDropPodInitialized", playerStateWrites.getOrDefault("questDropPodInitialized", false));
        snapshot.put("advancementCount", advancements.size());
        snapshot.put("screenPacketCount", screenPackets.size());
        snapshot.put("hudNotificationCount", hudNotifications.size());
        snapshot.put("recoveryContextCount", recoveryContexts.size());
        snapshot.put("holomapLayerCount", holomapLayers.size());
        snapshot.put("repairActionCount", repairActions.size());
        snapshot.put("nativeStateMutated", !mutationLog.isEmpty());
        snapshot.put("minecraftRuntimeAccessed", false);
        return snapshot;
    }

    private List<String> validate() {
        List<String> diagnostics = new ArrayList<>();
        requireInventory("echoashfallprotocol:field_manual", diagnostics);
        if (!optionalTerminalRemoteSkipped) {
            requireInventory("echoterminal:echo_terminal_remote", diagnostics);
        }
        requireStructure("echoashfallprotocol:drop_pod", diagnostics);
        requireText(teleportAnchor, "Missing runtime teleport anchor mutation.", diagnostics);
        requireText(respawnAnchor, "Missing runtime respawn anchor mutation.", diagnostics);
        if (!Boolean.TRUE.equals(playerStateWrites.get("firstJoinFlagValue"))) {
            diagnostics.add("Missing runtime first-join flag mutation.");
        }
        if (!Boolean.TRUE.equals(playerStateWrites.get("questDropPodInitialized"))) {
            diagnostics.add("Missing runtime quest drop-pod initialized mutation.");
        }
        if (!advancements.contains("echoashfallprotocol:find_drop_pod")) {
            diagnostics.add("Missing runtime advancement mutation.");
        }
        requirePacket("echoashfallprotocol:welcome_screen", diagnostics);
        if (hudNotifications.isEmpty()) {
            diagnostics.add("Missing runtime HUD notification mutation.");
        }
        if (recoveryContexts.isEmpty()) {
            diagnostics.add("Missing runtime Recovery context mutation.");
        }
        if (!holomapLayers.contains("echoashfallprotocol:first_month_field_intel")
                || !holomapLayers.contains("echoashfallprotocol:first_major_route")) {
            diagnostics.add("Missing runtime HoloMap layer mutations.");
        }
        requireRepair("rescue_underground_starting_pod_below_y_48", diagnostics);
        requireRepair("repair_missing_drop_pod_respawn", diagnostics);
        if (mutationLog.isEmpty()) {
            diagnostics.add("Runtime target did not mutate AdapterCore native state.");
        }
        return List.copyOf(diagnostics);
    }

    private void requireInventory(String item, List<String> diagnostics) {
        boolean found = inventoryWrites.stream().anyMatch(write -> item.equals(write.get("item")));
        if (!found) {
            diagnostics.add("Missing runtime inventory mutation for " + item + ".");
        }
    }

    private void requireStructure(String structure, List<String> diagnostics) {
        boolean found = structurePlacements.stream().anyMatch(write -> structure.equals(write.get("structure")));
        if (!found) {
            diagnostics.add("Missing runtime structure mutation for " + structure + ".");
        }
    }

    private void requirePacket(String packet, List<String> diagnostics) {
        boolean found = screenPackets.stream().anyMatch(write -> packet.equals(write.get("packet")));
        if (!found) {
            diagnostics.add("Missing runtime screen packet mutation for " + packet + ".");
        }
    }

    private void requireRepair(String repair, List<String> diagnostics) {
        if (!repairActions.contains(repair)) {
            diagnostics.add("Missing runtime repair mutation " + repair + ".");
        }
    }

    private static void requireText(String value, String message, List<String> diagnostics) {
        if (value == null || value.isBlank()) {
            diagnostics.add(message);
        }
    }
}

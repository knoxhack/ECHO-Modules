package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativePlayerRecoveryBridgeDispatch {
    private static final String PREPARED = "prepared_as_adaptercore_command";
    private static final String SKIPPED = "skipped_module_not_loaded";

    private final String moduleId;

    public EchoNativePlayerRecoveryBridgeDispatch(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> dispatch(
            String id,
            List<Map<String, Object>> commands,
            Map<String, Object> runtimeTarget) {
        List<BridgeRequirement> requirements = requirements();
        List<Map<String, Object>> applications = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (BridgeRequirement requirement : requirements) {
            Map<String, Object> application = apply(requirement, commands, runtimeTarget);
            applications.add(application);
            if (!"PASS".equals(application.get("status"))) {
                diagnostics.add("Bridge dispatch " + application.get("id") + " failed.");
            }
        }
        if (!"PASS".equals(value(runtimeTarget, "status"))) {
            diagnostics.add("First-join runtime target did not pass before bridge dispatch.");
        }
        if (!Boolean.TRUE.equals(runtimeTarget == null ? null : runtimeTarget.get("nativeStateMutated"))) {
            diagnostics.add("First-join runtime target did not expose mutated native state.");
        }

        List<String> bridgeIds = bridgeIds(applications);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "player/recovery bridge dispatch id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_player_recovery_bridge_dispatch");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore live native player/recovery first-join bridge dispatch");
        report.put("executionMode", "adaptercore_jdk_live_bridge_dispatch");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("liveAdapterCoreExecution", false);
        report.put("bridgeDispatchPrepared", diagnostics.isEmpty());
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("minecraftBackedAdaptersPending", true);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("sourceRuntimeTarget", value(runtimeTarget, "id"));
        report.put("bridgeApplicationCount", applications.size());
        report.put("preparedBridgeCount", countPassing(applications));
        report.put("executedBridgeCount", 0);
        report.put("preparedBridgeIds", bridgeIds);
        report.put("committedBridgeIds", List.of());
        report.put("applications", List.copyOf(applications));
        report.put("remainingMinecraftRuntimeAdapters", remainingMinecraftRuntimeAdapters());
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared the native first-join runtime target for player, inventory, world, respawn, advancement, UI/HUD, Recovery, HoloMap, and repair bridge applications without claiming live host mutation."
                : "AdapterCore could not dispatch every first-join player/recovery bridge application.");
        return Map.copyOf(report);
    }

    private static Map<String, Object> apply(
            BridgeRequirement requirement,
            List<Map<String, Object>> commands,
            Map<String, Object> runtimeTarget) {
        List<Map<String, Object>> acceptedCommands = acceptedCommands(requirement, commands);
        List<String> diagnostics = new ArrayList<>();
        if (acceptedCommands.isEmpty()) {
            diagnostics.add("Missing prepared command for bridge " + requirement.bridgeId() + ".");
        }
        if (!runtimeEvidencePresent(requirement, runtimeTarget)) {
            diagnostics.add("Missing runtime evidence for bridge " + requirement.bridgeId() + ".");
        }

        Map<String, Object> application = new LinkedHashMap<>();
        application.put("id", requirement.bridgeId() + ".adaptercore_live_dispatch");
        application.put("bridgeId", requirement.bridgeId());
        application.put("adapterCoreBridge", "adaptercore.native_player_recovery_bridge_dispatch");
        application.put("adapterSurface", requirement.surface());
        application.put("operationIds", operationIds(acceptedCommands));
        application.put("acceptedCommandCount", acceptedCommands.size());
        application.put("requiredRuntimeEvidence", requirement.runtimeEvidence());
        application.put("runtimeEvidencePresent", diagnostics.isEmpty());
        application.put("nativeStateConsumed", false);
        application.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        application.put("liveRuntimeMutationConsumed", false);
        application.put("liveAdapterCoreExecution", false);
        application.put("bridgeApplicationPrepared", diagnostics.isEmpty());
        application.put("minecraftRuntimeAccessed", false);
        application.put("minecraftRuntimeMutated", false);
        application.put("minecraftRegistryMutated", false);
        application.put("diagnostics", List.copyOf(diagnostics));
        application.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        return Map.copyOf(application);
    }

    private static List<Map<String, Object>> acceptedCommands(
            BridgeRequirement requirement,
            List<Map<String, Object>> commands) {
        if (commands == null) {
            return List.of();
        }
        List<Map<String, Object>> accepted = new ArrayList<>();
        for (Map<String, Object> command : commands) {
            String operationId = value(command, "operationId");
            if (requirement.operationIds().contains(operationId) && PREPARED.equals(value(command, "status"))) {
                accepted.add(command);
            }
        }
        if (accepted.isEmpty() && requirement.optionalWhenSkipped()) {
            for (Map<String, Object> command : commands) {
                String operationId = value(command, "operationId");
                if (requirement.operationIds().contains(operationId) && SKIPPED.equals(value(command, "status"))) {
                    accepted.add(command);
                }
            }
        }
        return List.copyOf(accepted);
    }

    private static boolean runtimeEvidencePresent(BridgeRequirement requirement, Map<String, Object> runtimeTarget) {
        if (runtimeTarget == null || runtimeTarget.isEmpty()) {
            return false;
        }
        return switch (requirement.runtimeEvidence()) {
            case "inventoryWrites" -> !list(runtimeTarget.get("inventoryWrites")).isEmpty();
            case "structurePlacements" -> !list(runtimeTarget.get("structurePlacements")).isEmpty();
            case "teleportAnchor" -> !value(runtimeTarget, "teleportAnchor").isBlank();
            case "respawnAnchor" -> !value(runtimeTarget, "respawnAnchor").isBlank();
            case "playerStateWrites" -> !map(runtimeTarget.get("playerStateWrites")).isEmpty();
            case "advancements" -> !list(runtimeTarget.get("advancements")).isEmpty();
            case "screenPackets" -> !list(runtimeTarget.get("screenPackets")).isEmpty();
            case "hudNotifications" -> !list(runtimeTarget.get("hudNotifications")).isEmpty();
            case "recoveryContexts" -> !list(runtimeTarget.get("recoveryContexts")).isEmpty();
            case "holomapLayers" -> !list(runtimeTarget.get("holomapLayers")).isEmpty();
            case "repairActions" -> !list(runtimeTarget.get("repairActions")).isEmpty();
            default -> false;
        };
    }

    private static List<BridgeRequirement> requirements() {
        return List.of(
                requirement("native_player_inventory_bridge", "player_recovery.inventory",
                        "inventoryWrites", false,
                        "inventory.write_starter_note", "inventory.write_terminal_remote_if_loaded"),
                requirement("native_world_structure_placement_bridge", "player_recovery.world_structure",
                        "structurePlacements", false,
                        "world.place_personal_drop_pod"),
                requirement("native_player_position_bridge", "player_recovery.position",
                        "teleportAnchor", false,
                        "player.teleport_to_drop_pod_interior"),
                requirement("native_respawn_position_bridge", "player_recovery.respawn",
                        "respawnAnchor", false,
                        "player.bind_drop_pod_respawn"),
                requirement("native_player_state_bridge", "player_recovery.player_state",
                        "playerStateWrites", false,
                        "player.write_first_join_state"),
                requirement("native_advancement_bridge", "player_recovery.advancement",
                        "advancements", false,
                        "player.grant_find_drop_pod_advancement"),
                requirement("native_screen_packet_bridge", "ui_hud_screen_safe.welcome",
                        "screenPackets", false,
                        "ui.dispatch_welcome_screen"),
                requirement("native_hud_notification_bridge", "ui_hud_screen_safe.hud",
                        "hudNotifications", false,
                        "hud.publish_opening_recovery_notice"),
                requirement("echorecovery:field_cache_service", "player_recovery.field_cache",
                        "recoveryContexts", false,
                        "recovery.publish_drop_pod_field_cache_context"),
                requirement("echoholomap:map_state_service", "holomap_lens_codex_wiki.map_state",
                        "holomapLayers", false,
                        "holomap.publish_recovery_route_markers"),
                requirement("native_player_recovery_repair_bridge", "player_recovery.existing_player_repair",
                        "repairActions", false,
                        "repair.rescue_underground_or_missing_respawn")
        );
    }

    private static BridgeRequirement requirement(
            String bridgeId,
            String surface,
            String runtimeEvidence,
            boolean optionalWhenSkipped,
            String... operationIds) {
        return new BridgeRequirement(bridgeId, surface, runtimeEvidence, optionalWhenSkipped, List.of(operationIds));
    }

    private static List<String> remainingMinecraftRuntimeAdapters() {
        return List.of(
                "minecraft_backed_inventory_writer",
                "minecraft_backed_structure_placer",
                "minecraft_backed_player_positioner",
                "minecraft_backed_respawn_binder",
                "minecraft_backed_player_state_writer",
                "minecraft_backed_advancement_granter",
                "minecraft_backed_screen_packet_sender",
                "minecraft_backed_hud_notification_sender"
        );
    }

    private static List<String> operationIds(List<Map<String, Object>> commands) {
        List<String> operationIds = new ArrayList<>();
        for (Map<String, Object> command : commands) {
            operationIds.add(value(command, "operationId"));
        }
        return List.copyOf(operationIds);
    }

    private static List<String> bridgeIds(List<Map<String, Object>> applications) {
        LinkedHashSet<String> bridgeIds = new LinkedHashSet<>();
        for (Map<String, Object> application : applications) {
            String bridgeId = value(application, "bridgeId");
            if (!bridgeId.isBlank()) {
                bridgeIds.add(bridgeId);
            }
        }
        return List.copyOf(bridgeIds);
    }

    private static int countPassing(List<Map<String, Object>> applications) {
        int count = 0;
        for (Map<String, Object> application : applications) {
            if ("PASS".equals(application.get("status"))) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return value instanceof List<?> list ? (List<Object>) list : List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private record BridgeRequirement(
            String bridgeId,
            String surface,
            String runtimeEvidence,
            boolean optionalWhenSkipped,
            List<String> operationIds) {
    }
}

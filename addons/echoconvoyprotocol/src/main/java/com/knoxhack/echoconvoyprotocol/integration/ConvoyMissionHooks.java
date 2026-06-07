package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.progress.ConvoyProgress;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class ConvoyMissionHooks {
    private ConvoyMissionHooks() {
    }

    public static void registerCoverage() {
        register("prep_vehicle", 2);
        register("start_route_mission", 0);
        register("start_route_mission", 1);
        register("close_route", 0);
        register("close_route", 1);
        register("close_route", 2);
        register("close_route", 3);
        register("depot_formation", 0);
        register("refuel_repair", 0);
        register("field_operation_staging", 0);
        register("incident_resolution", 0);
        register("convoy_recovery", 0);
        register("salvage_export", 0);
    }

    public static void recordVehiclePrepared(Player player) {
        record(player, "prep_vehicle", 2, MissionObjectiveType.DRIVE_VEHICLE, 1, "action", "vehicle_ready");
    }

    public static void recordRouteActivated(Player player, Identifier routeId) {
        String route = routeId == null ? "unknown" : routeId.toString();
        record(player, "start_route_mission", 0, MissionObjectiveType.ESTABLISH_ROUTE, 1, "route", route);
        record(player, "close_route", 0, MissionObjectiveType.ESTABLISH_ROUTE, 1, "route", route);
        record(player, "close_route", 1, MissionObjectiveType.DRIVE_VEHICLE, 1, "route", route);
    }

    public static void recordCheckpoint(Player player, Identifier routeId) {
        record(player, "close_route", 2, MissionObjectiveType.ESTABLISH_ROUTE, 1, "route", routeId == null ? "unknown" : routeId.toString());
    }

    public static void recordRouteCompleted(Player player, Identifier routeId) {
        record(player, "start_route_mission", 1, MissionObjectiveType.ESTABLISH_ROUTE, 1, "route", routeId == null ? "unknown" : routeId.toString());
    }

    public static void recordRouteClaimed(Player player, Identifier routeId) {
        record(player, "close_route", 3, MissionObjectiveType.DELIVER_ITEM, 1, "route", routeId == null ? "unknown" : routeId.toString());
    }

    public static void recordDepotFormation(Player player, Identifier depotId) {
        mark(player, "depot_formation");
        record(player, "depot_formation", 0, MissionObjectiveType.SCAN_BLOCK, 1, "depot", depotId == null ? "convoy_depot" : depotId.toString());
    }

    public static void recordRefuelRepair(Player player, String action) {
        mark(player, "refuel_repair");
        record(player, "refuel_repair", 0, MissionObjectiveType.REPAIR_MACHINE, 1, "action", action == null ? "vehicle_support" : action);
    }

    public static void recordFieldOperationStaged(Player player, Identifier routeId) {
        mark(player, "field_operation_staging");
        record(player, "field_operation_staging", 0, MissionObjectiveType.ESTABLISH_ROUTE, 1, "route", routeId == null ? "unknown" : routeId.toString());
    }

    public static void recordIncidentResolved(Player player, Identifier incidentId) {
        mark(player, "incident_resolution");
        record(player, "incident_resolution", 0, MissionObjectiveType.CUSTOM, 1, "incident", incidentId == null ? "field_incident" : incidentId.toString());
    }

    public static void recordRecovery(Player player, String source) {
        mark(player, "convoy_recovery");
        record(player, "convoy_recovery", 0, MissionObjectiveType.CUSTOM, 1, "source", source == null ? "recovery" : source);
    }

    public static void recordSalvageExport(Player player, Identifier routeId) {
        mark(player, "salvage_export");
        record(player, "salvage_export", 0, MissionObjectiveType.DELIVER_ITEM, 1, "route", routeId == null ? "unknown" : routeId.toString());
    }

    public static void recordRefuelRepairNear(Level level, BlockPos pos, String action) {
        recordNear(level, pos, player -> recordRefuelRepair(player, action));
    }

    public static void recordFieldOperationStagedNear(Level level, BlockPos pos, Identifier routeId) {
        recordNear(level, pos, player -> recordFieldOperationStaged(player, routeId));
    }

    public static void recordIncidentResolvedNear(Level level, BlockPos pos, Identifier incidentId) {
        recordNear(level, pos, player -> recordIncidentResolved(player, incidentId));
    }

    public static void recordRecoveryNear(Level level, BlockPos pos, String source) {
        recordNear(level, pos, player -> recordRecovery(player, source));
    }

    public static void recordSalvageExportNear(Level level, BlockPos pos, Identifier routeId) {
        recordNear(level, pos, player -> recordSalvageExport(player, routeId));
    }

    private static void register(String missionPath, int objectiveIndex) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoConvoyProtocol.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoConvoyProtocol.MODID, mission, objectiveIndex));
    }

    private static void record(Player player, String missionPath, int objectiveIndex, MissionObjectiveType type, int amount, String detailKey, String detail) {
        if (!(player instanceof ServerPlayer serverPlayer) || amount <= 0) {
            return;
        }
        Identifier mission = mission(missionPath);
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                type,
                MissionHookTargets.objectiveTarget(EchoConvoyProtocol.MODID, mission, objectiveIndex),
                amount,
                MissionHookTargets.context(EchoConvoyProtocol.MODID, mission, detailKey, detail));
    }

    private static void mark(Player player, String key) {
        if (player != null && key != null && !key.isBlank()) {
            ConvoyProgress.get(player).mark(key);
        }
    }

    private static void recordNear(Level level, BlockPos pos, NearbyRecorder recorder) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null || recorder == null) {
            return;
        }
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(24.0D))) {
            recorder.record(player);
        }
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, path);
    }

    @FunctionalInterface
    private interface NearbyRecorder {
        void record(ServerPlayer player);
    }
}

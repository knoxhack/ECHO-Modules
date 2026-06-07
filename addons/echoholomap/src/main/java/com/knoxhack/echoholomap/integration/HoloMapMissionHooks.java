package com.knoxhack.echoholomap.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoholomap.EchoHoloMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class HoloMapMissionHooks {
    private HoloMapMissionHooks() {
    }

    public static void registerCoverage() {
        register("discover_terrain", "terrain");
        register("create_waypoint", "waypoint");
        register("reveal_marker", "marker");
        register("sync_route", "sync");
    }

    public static void recordTerrainDiscovered(Player player, int chunks) {
        record(player, "discover_terrain", "terrain", MissionObjectiveType.ENTER_REGION, Math.max(1, chunks), "terrain", "chunk_sample");
    }

    public static void recordWaypointCreated(Player player, String scope) {
        record(player, "create_waypoint", "waypoint", MissionObjectiveType.CUSTOM, 1, "scope", scope);
    }

    public static void recordMarkerRevealed(Player player, String layer) {
        record(player, "reveal_marker", "marker", MissionObjectiveType.DISCOVER_STRUCTURE, 1, "layer", layer);
    }

    public static void recordRouteSynced(Player player, int markers) {
        record(player, "sync_route", "sync", MissionObjectiveType.ESTABLISH_ROUTE, 1, "markers", Integer.toString(Math.max(0, markers)));
    }

    private static void register(String missionPath, String objectiveKey) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoHoloMap.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoHoloMap.MODID, mission, objectiveKey));
    }

    private static void record(
            Player player,
            String missionPath,
            String objectiveKey,
            MissionObjectiveType type,
            int amount,
            String detailKey,
            String detail) {
        if (!(player instanceof ServerPlayer serverPlayer) || amount <= 0) {
            return;
        }
        Identifier mission = mission(missionPath);
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                type,
                MissionHookTargets.objectiveTarget(EchoHoloMap.MODID, mission, objectiveKey),
                amount,
                MissionHookTargets.context(EchoHoloMap.MODID, mission, detailKey, detail));
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, path);
    }
}

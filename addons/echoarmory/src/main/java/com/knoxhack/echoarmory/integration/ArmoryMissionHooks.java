package com.knoxhack.echoarmory.integration;

import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ArmoryMissionHooks {
    private ArmoryMissionHooks() {
    }

    public static void registerCoverage() {
        register("inspect_loadout", "scan");
        register("forge_upgrade", "upgrade");
        register("install_module", "module");
        register("recharge_core", "recharge");
        register("bind_loadout", "bind");
        register("prepare_route_kit", "prepare");
        register("dispatch_route_kit", "dispatch");
    }

    public static void recordInspectLoadout(Player player, String station) {
        record(player, "inspect_loadout", "scan", MissionObjectiveType.SCAN_ENTITY, "station", station);
    }

    public static void recordForgeUpgrade(Player player, String station) {
        record(player, "forge_upgrade", "upgrade", MissionObjectiveType.CRAFT_ITEM, "station", station);
    }

    public static void recordInstallModule(Player player, String station) {
        record(player, "install_module", "module", MissionObjectiveType.REPAIR_MACHINE, "station", station);
    }

    public static void recordRechargeCore(Player player, String station) {
        record(player, "recharge_core", "recharge", MissionObjectiveType.REPAIR_MACHINE, "station", station);
    }

    public static void recordBindLoadout(Player player, String station) {
        record(player, "bind_loadout", "bind", MissionObjectiveType.SCAN_ENTITY, "station", station);
    }

    public static void recordPrepareRouteKit(Player player, String loadoutId) {
        record(player, "prepare_route_kit", "prepare", MissionObjectiveType.CUSTOM, "loadout", loadoutId);
    }

    public static void recordDispatchRouteKit(Player player, String loadoutId) {
        record(player, "dispatch_route_kit", "dispatch", MissionObjectiveType.ESTABLISH_ROUTE, "loadout", loadoutId);
    }

    private static void register(String missionPath, String objectiveKey) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoArmory.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoArmory.MODID, mission, objectiveKey));
    }

    private static void record(
            Player player,
            String missionPath,
            String objectiveKey,
            MissionObjectiveType type,
            String detailKey,
            String detail) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Identifier mission = mission(missionPath);
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                type,
                MissionHookTargets.objectiveTarget(EchoArmory.MODID, mission, objectiveKey),
                1,
                MissionHookTargets.context(EchoArmory.MODID, mission, detailKey, detail));
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoArmory.MODID, path);
    }
}

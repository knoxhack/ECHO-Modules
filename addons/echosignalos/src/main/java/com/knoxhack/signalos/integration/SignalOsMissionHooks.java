package com.knoxhack.signalos.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.signalos.SignalOS;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class SignalOsMissionHooks {
    private SignalOsMissionHooks() {
    }

    public static void registerCoverage() {
        register("boot_terminal", "boot");
        register("rack_network_online", "rack");
        register("drive_record_flow", "record");
    }

    public static void recordBootTerminal(Player player, String block) {
        record(player, "boot_terminal", "boot", MissionObjectiveType.SCAN_BLOCK, "block", block);
    }

    public static void recordRackNetworkOnline(Player player, String detail) {
        record(player, "rack_network_online", "rack", MissionObjectiveType.ESTABLISH_ROUTE, "rack", detail);
    }

    public static void recordDriveRecordFlow(Player player, String action) {
        record(player, "drive_record_flow", "record", MissionObjectiveType.UNLOCK_RESEARCH, "action", action);
    }

    private static void register(String missionPath, String objectiveKey) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                SignalOS.MODID,
                mission,
                MissionHookTargets.objectiveTarget(SignalOS.MODID, mission, objectiveKey));
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
                MissionHookTargets.objectiveTarget(SignalOS.MODID, mission, objectiveKey),
                1,
                MissionHookTargets.context(SignalOS.MODID, mission, detailKey, detail));
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(SignalOS.MODID, path);
    }
}

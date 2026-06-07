package com.knoxhack.echostationfall.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echostationfall.EchoStationfall;
import com.knoxhack.echostationfall.progression.StationfallObjective;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class StationfallMissionHooks {
    private StationfallMissionHooks() {
    }

    public static void registerCoverage() {
        for (String key : new String[] {
                "board_station",
                "restore_power",
                "decode_logs",
                "stabilize_sections",
                "ai_override",
                "station_mother",
                "blackbox"
        }) {
            register(key, 0);
        }
        for (int index = 1; index <= 3 + StationfallObjective.values().length; index++) {
            register("stabilize_sections", index);
        }
    }

    public static void record(Player player, String missionPath, MissionObjectiveType type, int amount, String detailKey, String detail) {
        record(player, missionPath, 0, type, amount, detailKey, detail);
    }

    public static void recordPressureStabilized(Player player, String source) {
        record(player, "stabilize_sections", 1, MissionObjectiveType.REPAIR_MACHINE, 1, "pressure", source == null ? "pressure" : source);
    }

    public static void recordOxygenStabilized(Player player, String source) {
        record(player, "stabilize_sections", 2, MissionObjectiveType.CUSTOM, 1, "oxygen", source == null ? "oxygen" : source);
    }

    public static void recordPanicDampened(Player player, String source) {
        record(player, "stabilize_sections", 3, MissionObjectiveType.CUSTOM, 1, "panic", source == null ? "panic" : source);
    }

    public static void recordSectionStabilized(Player player, StationfallObjective objective) {
        if (objective == null) {
            return;
        }
        record(player, "stabilize_sections", 4 + objective.ordinal(), MissionObjectiveType.REPAIR_MACHINE, 1, "section", objective.key());
    }

    public static void record(Player player, String missionPath, int objectiveIndex, MissionObjectiveType type, int amount, String detailKey, String detail) {
        if (!(player instanceof ServerPlayer serverPlayer) || amount <= 0) {
            return;
        }
        Identifier mission = mission(missionPath);
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                type,
                MissionHookTargets.objectiveTarget(EchoStationfall.MODID, mission, objectiveIndex),
                amount,
                MissionHookTargets.context(EchoStationfall.MODID, mission, detailKey, detail));
    }

    private static void register(String missionPath, int objectiveIndex) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoStationfall.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoStationfall.MODID, mission, objectiveIndex));
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoStationfall.MODID, path);
    }
}

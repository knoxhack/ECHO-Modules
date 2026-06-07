package com.knoxhack.echoorbitalremnants.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class OrbitalMissionHooks {
    private OrbitalMissionHooks() {
    }

    public static void registerCoverage() {
        register("earth_calibration", 0);
        register("launch_chain", 0);
        register("launch_chain", 1);
        for (String key : new String[] {
                "low_orbit",
                "station_network",
                "lunar_signal",
                "mars_route",
                "europa_route",
                "saturn_route",
                "titan_route",
                "deep_space_protocol",
                "echo_zero",
                "survey_network",
                "faction_contract",
                "final_seal"
        }) {
            register(key, 0);
        }
    }

    public static void record(Player player, String missionPath, MissionObjectiveType type, int amount, String detailKey, String detail) {
        record(player, missionPath, 0, type, amount, detailKey, detail);
    }

    public static void record(Player player, String missionPath, int objectiveIndex, MissionObjectiveType type, int amount, String detailKey, String detail) {
        if (!(player instanceof ServerPlayer serverPlayer) || amount <= 0) {
            return;
        }
        Identifier mission = mission(missionPath);
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                type,
                MissionHookTargets.objectiveTarget(EchoOrbitalRemnants.MODID, mission, objectiveIndex),
                amount,
                MissionHookTargets.context(EchoOrbitalRemnants.MODID, mission, detailKey, detail));
    }

    private static void register(String missionPath, int objectiveIndex) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoOrbitalRemnants.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoOrbitalRemnants.MODID, mission, objectiveIndex));
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, path);
    }
}

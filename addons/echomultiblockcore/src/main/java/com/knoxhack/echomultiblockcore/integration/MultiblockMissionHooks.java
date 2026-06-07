package com.knoxhack.echomultiblockcore.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class MultiblockMissionHooks {
    private MultiblockMissionHooks() {
    }

    public static void registerCoverage() {
        register("validate_first_structure", "validate");
        register("install_robot_tool", "tool");
        register("complete_automation_task", "task");
        register("repair_integrity", "repair");
        register("use_auto_builder", "builder");
    }

    public static void recordStructureValidated(Player player, Identifier definitionId) {
        record(player, "validate_first_structure", "validate", MissionObjectiveType.BUILD_MULTIBLOCK, "structure", detail(definitionId, "structure"));
    }

    public static void recordRobotToolInstalled(Player player, String tool) {
        record(player, "install_robot_tool", "tool", MissionObjectiveType.REPAIR_MACHINE, "tool", tool);
    }

    public static void recordAutoBuilder(Player player, String structure) {
        record(player, "use_auto_builder", "builder", MissionObjectiveType.BUILD_MULTIBLOCK, "structure", structure);
    }

    public static void recordTaskCompleted(ServerLevel level, BlockPos pos, Identifier taskId, boolean repairedIntegrity) {
        if (level == null || pos == null) {
            return;
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(24.0D))) {
            record(player, "complete_automation_task", "task", MissionObjectiveType.CUSTOM, "task", detail(taskId, "automation_task"));
            if (repairedIntegrity) {
                record(player, "repair_integrity", "repair", MissionObjectiveType.REPAIR_MACHINE, "task", detail(taskId, "repair_task"));
            }
        }
    }

    private static void register(String missionPath, String objectiveKey) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoMultiblockCore.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoMultiblockCore.MODID, mission, objectiveKey));
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
                MissionHookTargets.objectiveTarget(EchoMultiblockCore.MODID, mission, objectiveKey),
                1,
                MissionHookTargets.context(EchoMultiblockCore.MODID, mission, detailKey, detail));
    }

    private static String detail(Identifier id, String fallback) {
        return id == null ? fallback : id.toString();
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoMultiblockCore.MODID, path);
    }
}

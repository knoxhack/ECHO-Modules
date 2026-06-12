package com.knoxhack.echolens.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echolens.EchoLens;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class LensMissionHooks {
    private LensMissionHooks() {
    }

    public static void registerCoverage() {
        register("verified_deep_scan", "scan");
        register("machine_diagnostic", "diagnostic");
        register("index_shortcut", "shortcut");
    }

    public static void recordVerifiedDeepScan(Player player, String targetKind) {
        record(player, "verified_deep_scan", "scan", MissionObjectiveType.SCAN_BLOCK, "target", targetKind);
    }

    public static void recordMachineDiagnostic(Player player, String targetId) {
        record(player, "machine_diagnostic", "diagnostic", MissionObjectiveType.SCAN_BLOCK, "machine", targetId);
    }

    public static void recordActualBlockScan(Player player, Identifier targetId) {
        if (!(player instanceof ServerPlayer serverPlayer) || targetId == null) {
            return;
        }
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                MissionObjectiveType.SCAN_BLOCK,
                targetId,
                1,
                Map.of("source", EchoLens.MODID, "scan_target", targetId.toString()));
    }

    public static void recordIndexShortcut(Player player, String action) {
        record(player, "index_shortcut", "shortcut", MissionObjectiveType.UNLOCK_RESEARCH, "action", action);
    }

    private static void register(String missionPath, String objectiveKey) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoLens.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoLens.MODID, mission, objectiveKey));
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
                MissionHookTargets.objectiveTarget(EchoLens.MODID, mission, objectiveKey),
                1,
                MissionHookTargets.context(EchoLens.MODID, mission, detailKey, detail));
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoLens.MODID, path);
    }
}

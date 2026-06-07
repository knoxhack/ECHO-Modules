package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ReclamationMissionHooks {
    private ReclamationMissionHooks() {
    }

    public static void registerCoverage() {
        for (String key : new String[] {
                "recover_seed",
                "analyze_soil",
                "first_growth",
                "gene_stabilization",
                "greenhouse_online",
                "restore_chunk"
        }) {
            register(key, 0);
        }
    }

    public static void recordFlag(Player player, String flag) {
        String mission = switch (flag == null ? "" : flag) {
            case "seed_recovered", "seed_analyzed" -> "recover_seed";
            case "soil_analyzed" -> "analyze_soil";
            case "first_growth" -> "first_growth";
            case "gene_stabilization" -> "gene_stabilization";
            case "greenhouse_online" -> "greenhouse_online";
            case "restore_chunk" -> "restore_chunk";
            default -> "";
        };
        if (!mission.isBlank()) {
            record(player, mission, MissionObjectiveType.CUSTOM, 1, "action", flag);
        }
    }

    public static void recordCounter(Player player, String key, int amount) {
        if (amount <= 0) {
            return;
        }
        String mission = switch (key == null ? "" : key) {
            case "crops_grown" -> "first_growth";
            case "stabilized_seeds" -> "gene_stabilization";
            case "restoration_crop_growth", "restoration_score" -> "restore_chunk";
            default -> "";
        };
        if (!mission.isBlank()) {
            record(player, mission, MissionObjectiveType.CUSTOM, amount, "counter", key);
        }
    }

    private static void register(String missionKey, int objectiveIndex) {
        Identifier mission = mission(missionKey);
        EchoCoreServices.registerMissionHookCoverage(
                EchoAgricultureReclamation.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoAgricultureReclamation.MODID, mission, objectiveIndex));
    }

    private static void record(Player player, String missionKey, MissionObjectiveType type, int amount, String detailKey, String detail) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Identifier mission = mission(missionKey);
        EchoCoreServices.recordMissionObjective(
                serverPlayer,
                type,
                MissionHookTargets.objectiveTarget(EchoAgricultureReclamation.MODID, mission, 0),
                amount,
                MissionHookTargets.context(EchoAgricultureReclamation.MODID, mission, detailKey, detail));
    }

    private static Identifier mission(String key) {
        return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, "mission/" + key);
    }
}

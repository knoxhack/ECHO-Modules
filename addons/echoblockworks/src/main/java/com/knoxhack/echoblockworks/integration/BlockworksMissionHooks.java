package com.knoxhack.echoblockworks.integration;

import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class BlockworksMissionHooks {
    private BlockworksMissionHooks() {
    }

    public static void registerCoverage() {
        register("use_table", "table");
        register("convert_variant", "convert");
        register("use_pattern_cutter", "cutter");
        register("discover_showcase_site", "showcase");
    }

    public static void recordTableUsed(Player player, String target) {
        record(player, "use_table", "table", MissionObjectiveType.CUSTOM, 1, "target", target);
    }

    public static void recordVariantConverted(Player player, String target, int count) {
        record(player, "convert_variant", "convert", MissionObjectiveType.CRAFT_ITEM, Math.max(1, count), "target", target);
    }

    public static void recordPatternCutter(Player player, String target) {
        record(player, "use_pattern_cutter", "cutter", MissionObjectiveType.PLACE_BLOCK, 1, "target", target);
    }

    public static void recordShowcaseSite(Player player, String source) {
        record(player, "discover_showcase_site", "showcase", MissionObjectiveType.DISCOVER_STRUCTURE, 1, "source", source);
    }

    private static void register(String missionPath, String objectiveKey) {
        Identifier mission = mission(missionPath);
        EchoCoreServices.registerMissionHookCoverage(
                EchoBlockworks.MODID,
                mission,
                MissionHookTargets.objectiveTarget(EchoBlockworks.MODID, mission, objectiveKey));
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
                MissionHookTargets.objectiveTarget(EchoBlockworks.MODID, mission, objectiveKey),
                amount,
                MissionHookTargets.context(EchoBlockworks.MODID, mission, detailKey, detail));
    }

    private static Identifier mission(String path) {
        return Identifier.fromNamespaceAndPath(EchoBlockworks.MODID, path);
    }
}

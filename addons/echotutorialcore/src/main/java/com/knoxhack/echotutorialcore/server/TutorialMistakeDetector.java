package com.knoxhack.echotutorialcore.server;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import com.knoxhack.echotutorialcore.config.TutorialConfig;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class TutorialMistakeDetector {
    private static final Identifier NO_POWER = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "no_power");
    private static final Identifier MISSING_FILTER = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "missing_filter");
    private static final Identifier DIRTY_WATER = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "dirty_water_reliance");
    private static final Identifier HAZARD_UNPREPARED = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "toxic_without_filter");
    private static final Identifier RECIPE_LOCKED = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "recipe_locked");
    private static final Identifier UNCLAIMED_REWARD = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "unclaimed_reward");
    private static final Identifier NO_ACTIVE_MISSION = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "no_active_mission");
    private static final Identifier SCANNER_IGNORED = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "scanner_not_used");
    private static final Identifier HOLOMAP_IGNORED = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "holomap_not_opened");
    private static final Identifier REPEATED_FAILURE = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "repeated_hazard_death");

    private TutorialMistakeDetector() {}

    public static void reportNoPower(Player player) {
        report(player, NO_POWER);
    }

    public static void reportMissingFilter(Player player) {
        report(player, MISSING_FILTER);
    }

    public static void reportDirtyWaterReliance(Player player) {
        report(player, DIRTY_WATER);
    }

    public static void reportHazardUnprepared(Player player) {
        report(player, HAZARD_UNPREPARED);
    }

    public static void reportRecipeLocked(Player player) {
        report(player, RECIPE_LOCKED);
    }

    public static void reportUnclaimedReward(Player player) {
        report(player, UNCLAIMED_REWARD);
    }

    public static void reportNoActiveMission(Player player) {
        report(player, NO_ACTIVE_MISSION);
    }

    public static void reportScannerIgnored(Player player) {
        report(player, SCANNER_IGNORED);
    }

    public static void reportHoloMapIgnored(Player player) {
        report(player, HOLOMAP_IGNORED);
    }

    public static void reportRepeatedFailure(Player player) {
        report(player, REPEATED_FAILURE);
    }

    public static void report(Player player, Identifier mistakeId) {
        if (player == null || mistakeId == null) return;
        if (!TutorialConfig.ENABLE_MISTAKE_DETECTION.get()) return;
        TutorialGuideMode mode = TutorialProgressManager.getGuideMode(player);
        if (mode == TutorialGuideMode.OFF) return;

        TutorialPlayerData data = TutorialPlayerData.get(player);
        data.incrementMistake(mistakeId.toString());
        data.setLastRecommendationReason("mistake:" + mistakeId);
        TutorialProgressManager.saveMirrorAndSync(player, data);

        int count = data.getMistakeCount(mistakeId.toString());
        if (count < 2 && mode != TutorialGuideMode.ASSISTED) return;

        // Show hint if registered for this mistake id
        TutorialHintManager.showHint(player, mistakeId);
    }
}

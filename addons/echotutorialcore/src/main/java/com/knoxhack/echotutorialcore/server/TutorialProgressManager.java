package com.knoxhack.echotutorialcore.server;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import com.knoxhack.echotutorialcore.config.TutorialConfig;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import com.knoxhack.echotutorialcore.integration.datacore.TutorialDataCoreIntegration;
import com.knoxhack.echotutorialcore.network.TutorialNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class TutorialProgressManager {
    private TutorialProgressManager() {}

    public static void markProgress(Player player, Identifier progressId) {
        if (player == null || progressId == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        boolean changed = data.markProgressIfNew(progressId);
        if (!changed) return;
        data.setLastProgressGameTime(player.level().getGameTime());
        saveMirrorAndSync(player, data);
        unlockCardsForProgress(player, progressId);
        TutorialFlowManager.reportProgress(player, progressId);
        EchoTutorialCore.LOGGER.debug("Tutorial progress marked for {}: {}", player.getScoreboardName(), progressId);
    }

    public static boolean hasProgress(Player player, Identifier progressId) {
        if (player == null || progressId == null) return false;
        return TutorialPlayerData.get(player).hasProgress(progressId);
    }

    public static TutorialGuideMode getGuideMode(Player player) {
        if (player == null) return TutorialGuideMode.NORMAL;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        if (TutorialConfig.FORCE_GUIDE_MODE_ENABLED.get()) {
            TutorialGuideMode forced = TutorialConfig.FORCED_GUIDE_MODE.get();
            return forced == null ? TutorialGuideMode.OFF : forced;
        }
        return data.guideMode();
    }

    public static void setGuideMode(Player player, TutorialGuideMode mode) {
        if (player == null || mode == null) return;
        if (mode == TutorialGuideMode.ASSISTED && !TutorialConfig.ALLOW_ASSISTED_GUIDE_MODE.get()) {
            mode = TutorialGuideMode.NORMAL;
        }
        TutorialPlayerData data = TutorialPlayerData.get(player);
        data.setGuideMode(mode);
        saveMirrorAndSync(player, data);
    }

    public static void resetPlayer(Player player) {
        if (player == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        data.resetAll();
        data.setLastProgressGameTime(player.level().getGameTime());
        saveMirrorAndSync(player, data);
    }

    public static void recordTerminalOpened(Player player) {
        recordTimedProgress(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "opened_terminal"),
                data -> data.setLastTerminalOpenTime(player.level().getGameTime()));
    }

    public static void recordScannerUsed(Player player) {
        recordTimedProgress(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "used_scanner"),
                data -> data.setLastScannerUseTime(player.level().getGameTime()));
    }

    public static void recordHoloMapOpened(Player player) {
        recordTimedProgress(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "opened_holomap"),
                data -> data.setLastHoloMapOpenTime(player.level().getGameTime()));
    }

    public static void recordLensScan(Player player, Identifier target) {
        if (player == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        data.setLastLensScanTime(player.level().getGameTime());
        saveMirrorAndSync(player, data);
        markProgress(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "used_lens"));
        TutorialFlowManager.reportTrigger(player, com.knoxhack.echotutorialcore.api.trigger.TutorialTriggerType.CUSTOM,
                target == null ? Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "used_lens") : target);
    }

    public static void recordPowerAlert(Player player, String alertId) {
        if (player == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        data.setLastPowerAlert(alertId);
        saveMirrorAndSync(player, data);
    }

    public static void recordMissionState(Player player, String missionState) {
        if (player == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        data.setLastMissionState(missionState);
        saveMirrorAndSync(player, data);
    }

    public static void recordWorldHazards(Player player, Identifier regionId, java.util.Set<Identifier> hazardIds) {
        if (player == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        data.setLastRegionId(regionId == null ? "" : regionId.toString());
        java.util.Set<String> hazards = new java.util.LinkedHashSet<>();
        if (hazardIds != null) {
            for (Identifier hazardId : hazardIds) {
                if (hazardId != null) {
                    hazards.add(hazardId.toString());
                }
            }
        }
        data.setLastHazardIds(hazards);
        saveMirrorAndSync(player, data);
    }

    public static void saveMirrorAndSync(Player player, TutorialPlayerData data) {
        if (player instanceof ServerPlayer sp) {
            TutorialPlayerData.saveAndSync(sp, data);
            TutorialDataCoreIntegration.mirrorPlayer(sp);
            TutorialNetworking.sendSyncProgress(sp);
        } else {
            TutorialPlayerData.save(player, data);
            TutorialDataCoreIntegration.mirrorPlayer(player);
        }
    }

    private static void recordTimedProgress(Player player, Identifier progressId, java.util.function.Consumer<TutorialPlayerData> updater) {
        if (player == null || progressId == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        updater.accept(data);
        saveMirrorAndSync(player, data);
        markProgress(player, progressId);
    }

    private static void unlockCardsForProgress(Player player, Identifier progressId) {
        for (TutorialCard card : TutorialCoreRegistries.allCards()) {
            for (String trigger : card.unlockTriggers()) {
                if (matchesTrigger(trigger, progressId)) {
                    TutorialCardManager.unlockCard(player, card.id());
                    break;
                }
            }
        }
    }

    private static boolean matchesTrigger(String trigger, Identifier progressId) {
        if (trigger == null || trigger.isBlank() || progressId == null) {
            return false;
        }
        String clean = trigger.trim();
        return clean.equals(progressId.toString())
                || clean.equals(progressId.getPath())
                || clean.equals("progress:" + progressId)
                || clean.equals("custom:" + progressId);
    }
}

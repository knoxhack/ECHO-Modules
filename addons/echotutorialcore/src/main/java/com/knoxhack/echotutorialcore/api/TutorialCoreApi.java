package com.knoxhack.echotutorialcore.api;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.api.hint.TutorialHint;
import com.knoxhack.echotutorialcore.api.tooltip.TutorialTooltip;
import com.knoxhack.echotutorialcore.api.trigger.TutorialFlow;
import com.knoxhack.echotutorialcore.api.trigger.TutorialTriggerType;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import com.knoxhack.echotutorialcore.network.TutorialNetworking;
import com.knoxhack.echotutorialcore.server.TutorialCardManager;
import com.knoxhack.echotutorialcore.server.TutorialFlowManager;
import com.knoxhack.echotutorialcore.server.TutorialHintManager;
import com.knoxhack.echotutorialcore.server.TutorialMistakeDetector;
import com.knoxhack.echotutorialcore.server.TutorialProgressManager;
import com.knoxhack.echotutorialcore.server.TutorialRequirementResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class TutorialCoreApi {
    private TutorialCoreApi() {}

    public static void registerCard(TutorialCard card) {
        TutorialCoreRegistries.registerCard(card);
    }

    public static void registerHint(TutorialHint hint) {
        TutorialCoreRegistries.registerHint(hint);
    }

    public static void registerFlow(TutorialFlow flow) {
        TutorialCoreRegistries.registerFlow(flow);
    }

    public static void registerTooltip(TutorialTooltip tooltip) {
        TutorialCoreRegistries.registerTooltip(tooltip);
    }

    public static void unlockCard(ServerPlayer player, Identifier cardId) {
        TutorialCardManager.unlockCard(player, cardId);
    }

    public static void showCard(ServerPlayer player, Identifier cardId) {
        if (player != null && cardId != null) {
            TutorialNetworking.sendShowCard(player, cardId);
        }
    }

    public static void markCardRead(Player player, Identifier cardId) {
        TutorialCardManager.markCardRead(player, cardId);
    }

    public static void dismissCard(ServerPlayer player, Identifier cardId) {
        TutorialCardManager.dismissCard(player, cardId);
    }

    public static void showHint(ServerPlayer player, Identifier hintId) {
        TutorialHintManager.showHint(player, hintId);
    }

    public static void showHint(ServerPlayer player, TutorialHint hint) {
        TutorialHintManager.showHint(player, hint);
    }

    public static void markProgress(ServerPlayer player, Identifier progressId) {
        reportProgress(player, progressId);
    }

    public static void reportProgress(Player player, Identifier progressId) {
        TutorialProgressManager.markProgress(player, progressId);
    }

    public static boolean hasProgress(ServerPlayer player, Identifier progressId) {
        return TutorialProgressManager.hasProgress(player, progressId);
    }

    public static void reportTrigger(Player player, TutorialTriggerType type, Identifier target) {
        TutorialFlowManager.reportTrigger(player, type, target);
    }

    public static void recordTrigger(ServerPlayer player, TutorialTriggerType type, Identifier target, Map<String, String> context) {
        if (player == null) return;
        TutorialFlowManager.reportTrigger(player, type, target);
        if (context != null && !context.isEmpty()) {
            String progress = context.getOrDefault("progress", "");
            Identifier progressId = Identifier.tryParse(progress);
            if (progressId != null) {
                TutorialProgressManager.markProgress(player, progressId);
            }
        }
    }

    public static void completeFlow(ServerPlayer player, Identifier flowId) {
        TutorialFlowManager.completeFlow(player, flowId);
    }

    public static void dismissHint(ServerPlayer player, Identifier hintId) {
        TutorialHintManager.dismissHint(player, hintId);
    }

    public static void reportItemObtained(Player player, Identifier itemId) {
        if (player == null || itemId == null) return;
        TutorialFlowManager.reportTrigger(player, TutorialTriggerType.OBTAIN_ITEM, itemId);
        TutorialProgressManager.markProgress(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "obtained_" + itemId.getPath()));
    }

    public static void reportBlockInteracted(Player player, Identifier blockId) {
        TutorialFlowManager.reportTrigger(player, TutorialTriggerType.INTERACT_BLOCK, blockId);
    }

    public static void reportTerminalOpened(Player player) {
        TutorialProgressManager.recordTerminalOpened(player);
    }

    public static void reportScannerUsed(Player player) {
        TutorialProgressManager.recordScannerUsed(player);
    }

    public static void reportHoloMapOpened(Player player) {
        TutorialProgressManager.recordHoloMapOpened(player);
    }

    public static void reportLensScan(Player player, Identifier target) {
        TutorialProgressManager.recordLensScan(player, target);
    }

    public static void reportPowerAlert(Player player, String alertId) {
        TutorialProgressManager.recordPowerAlert(player, alertId);
        if (alertId != null && !alertId.isBlank()) {
            TutorialHintManager.showHint(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "no_power"));
        }
    }

    public static void reportPowerEvent(Player player, BlockPos pos, TutorialPowerEventType type) {
        if (player == null) return;
        TutorialPowerEventType safeType = type == null ? TutorialPowerEventType.NO_POWER : type;
        TutorialProgressManager.recordPowerAlert(player, safeType.name().toLowerCase(java.util.Locale.ROOT));
        switch (safeType) {
            case NO_POWER -> TutorialMistakeDetector.reportNoPower(player);
            case BROWNOUT -> TutorialHintManager.showHint(player, id("no_power"));
            case OVERLOAD, BREAKER_TRIPPED -> TutorialHintManager.showHint(player, id("no_power"));
        }
        if (pos != null) {
            TutorialProgressManager.markProgress(player, id("power_event_at_" + pos.asLong()));
        }
    }

    public static void reportHazardContext(Player player, Identifier hazardId, Identifier regionId) {
        Set<Identifier> hazards = hazardId == null ? Set.of() : Set.of(hazardId);
        reportWorldHazardChanged(player, regionId, hazards);
    }

    public static void reportWorldHazardChanged(Player player, Identifier regionId, Set<Identifier> hazardIds) {
        TutorialProgressManager.recordWorldHazards(player, regionId, hazardIds);
        if (regionId != null) {
            TutorialFlowManager.reportTrigger(player, TutorialTriggerType.ENTER_REGION, regionId);
        }
        if (hazardIds == null) return;
        for (Identifier hazardId : hazardIds) {
            if (hazardId == null) continue;
            String path = hazardId.getPath();
            if (path.contains("toxic")) {
                TutorialHintManager.showHint(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "toxic_without_filter"));
            } else if (path.contains("radiation") || path.contains("rad")) {
                TutorialHintManager.showHint(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "radiation_without_protection"));
            } else if (path.contains("cold") || path.contains("cryo")) {
                TutorialHintManager.showHint(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "cold_without_thermal"));
            } else if (path.contains("nexus")) {
                TutorialCardManager.unlockCard(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "nexus_warning"));
            }
        }
    }

    public static void reportMissionState(Player player, Identifier missionId, String state) {
        TutorialProgressManager.recordMissionState(player, state);
        if (missionId != null && "complete".equalsIgnoreCase(state)) {
            TutorialFlowManager.reportTrigger(player, TutorialTriggerType.MISSION_COMPLETE, missionId);
        }
    }

    public static void setGuideMode(ServerPlayer player, TutorialGuideMode mode) {
        TutorialProgressManager.setGuideMode(player, mode);
    }

    public static TutorialGuideMode getGuideMode(ServerPlayer player) {
        return TutorialProgressManager.getGuideMode(player);
    }

    public static TutorialGuideMode getGuideMode(Player player) {
        return TutorialProgressManager.getGuideMode(player);
    }

    public static void reportMistake(ServerPlayer player, Identifier mistakeId) {
        TutorialMistakeDetector.report(player, mistakeId);
    }

    public static void reportMissingRequirement(ServerPlayer player, Identifier requirementId) {
        TutorialRequirementResolver.showRequirementHint(player, requirementId);
    }

    public static void explainRequirement(Player player, Identifier requirementId) {
        TutorialRequirementResolver.showRequirementHint(player, requirementId);
    }

    public static List<TutorialCard> getVisibleCards(Player player) {
        return TutorialCardManager.getVisibleCards(player);
    }

    public static List<TutorialCard> getVisibleCards(Player player, TutorialCategory category) {
        return TutorialCardManager.getVisibleCards(player, category);
    }

    public static List<TutorialCard> getRecommendedCards(Player player, int limit) {
        return TutorialCardManager.getRecommendedCards(player, Math.max(1, limit));
    }

    public static int getUnreadCardCount(Player player) {
        return TutorialCardManager.unreadCount(player);
    }

    public static String getCurrentFlowStatus(Player player, Identifier flowId) {
        if (player == null || flowId == null) return "";
        TutorialFlow flow = TutorialCoreRegistries.getFlow(flowId).orElse(null);
        if (flow == null) return "";
        TutorialPlayerData data = TutorialPlayerData.get(player);
        int done = data.flowStepIds(flowId).size();
        return flow.title() + ": " + done + "/" + flow.steps().size()
                + (data.isFlowCompleted(flowId) ? " complete" : " in progress");
    }

    public static List<String> getRecommendedNextSteps(Player player) {
        TutorialPlayerData data = TutorialPlayerData.get(player);
        List<String> steps = new ArrayList<>();
        if (data.unreadCardCount() > 0) {
            steps.add("Review " + data.unreadCardCount() + " unread Guide card(s).");
        }
        if (!data.hasProgress(id("opened_terminal"))) {
            steps.add("Open the ECHO Terminal and check the Guide tab.");
        }
        if (!data.hasProgress(id("created_clean_water"))) {
            steps.add("Stabilize clean water before long routes.");
        }
        if (!data.hasProgress(id("powered_first_machine"))) {
            steps.add("Build a small power loop: generator, storage, machine.");
        }
        if (!data.hasProgress(id("used_scanner"))) {
            steps.add("Use the scanner to locate the next signal lead.");
        }
        if (!data.hasProgress(id("opened_holomap"))) {
            steps.add("Open HoloMap before committing to a route.");
        }
        if (!data.hasProgress(id("used_lens"))) {
            steps.add("Use Lens on silent machines, filters, and unknown blocks.");
        }
        if (data.lastPowerAlert() != null && !data.lastPowerAlert().isBlank()) {
            steps.add("Resolve power alert: " + data.lastPowerAlert() + ".");
        }
        if (!data.lastHazardIds().isEmpty()) {
            steps.add("Prepare protection for active hazard(s): " + String.join(", ", data.lastHazardIds()) + ".");
        }
        if (steps.isEmpty()) {
            steps.add("Pick a route, scan ahead, and keep one exit plan.");
        }
        data.setLastRecommendationReason("what_now");
        TutorialProgressManager.saveMirrorAndSync(player, data);
        return steps.stream().limit(8).toList();
    }

    public static void reportNoPower(Player player, BlockPos pos) {
        TutorialMistakeDetector.reportNoPower(player);
    }

    public static void reportMissingFilter(Player player) {
        TutorialMistakeDetector.reportMissingFilter(player);
    }

    public static void reportRecipeLocked(Player player) {
        TutorialMistakeDetector.reportRecipeLocked(player);
    }

    public static void reportHazardUnprepared(Player player) {
        TutorialMistakeDetector.reportHazardUnprepared(player);
    }

    public static void reportRewardAvailable(Player player) {
        TutorialMistakeDetector.reportUnclaimedReward(player);
    }

    public static void reportSignalDetected(Player player) {
        TutorialProgressManager.markProgress(player, id("detected_first_signal"));
    }

    public static void reportGuardianLocated(Player player) {
        TutorialProgressManager.markProgress(player, id("located_first_guardian"));
    }

    public static void reportFactionContact(Player player) {
        TutorialProgressManager.markProgress(player, id("faction_contact"));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, path);
    }
}

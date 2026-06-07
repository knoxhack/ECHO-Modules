package com.knoxhack.echotutorialcore.server;

import com.knoxhack.echotutorialcore.api.trigger.TutorialFlow;
import com.knoxhack.echotutorialcore.api.trigger.TutorialStep;
import com.knoxhack.echotutorialcore.api.trigger.TutorialTriggerType;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import com.knoxhack.echotutorialcore.network.TutorialNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class TutorialFlowManager {
    private TutorialFlowManager() {}

    public static void reportProgress(Player player, Identifier progressId) {
        reportTrigger(player, TutorialTriggerType.CUSTOM, progressId);
    }

    public static void reportTrigger(Player player, TutorialTriggerType triggerType, Identifier target) {
        if (player == null || triggerType == null || target == null) {
            return;
        }
        TutorialPlayerData data = TutorialPlayerData.get(player);
        boolean changed = false;
        for (TutorialFlow flow : TutorialCoreRegistries.allFlows()) {
            if (data.isFlowCompleted(flow.id())) {
                continue;
            }
            for (TutorialStep step : flow.steps()) {
                if (matches(step, triggerType, target) && data.markFlowStep(flow.id(), stepKey(step))) {
                    changed = true;
                }
            }
            if (isComplete(flow, data)) {
                data.completeFlow(flow.id());
                changed = true;
                for (Identifier cardId : flow.unlockCards()) {
                    TutorialCardManager.unlockCard(player, cardId);
                }
            }
        }
        if (changed) {
            TutorialProgressManager.saveMirrorAndSync(player, data);
            if (player instanceof ServerPlayer sp) {
                TutorialNetworking.sendSyncProgress(sp);
            }
        }
    }

    public static void completeFlow(Player player, Identifier flowId) {
        if (player == null || flowId == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        if (data.isFlowCompleted(flowId)) return;
        data.completeFlow(flowId);
        TutorialProgressManager.saveMirrorAndSync(player, data);

        TutorialFlow flow = TutorialCoreRegistries.getFlow(flowId).orElse(null);
        if (flow != null) {
            for (Identifier cardId : flow.unlockCards()) {
                TutorialCardManager.unlockCard(player, cardId);
            }
        }
    }

    public static boolean isFlowCompleted(Player player, Identifier flowId) {
        if (player == null || flowId == null) return false;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        return data.isFlowCompleted(flowId);
    }

    public static int completedStepCount(Player player, Identifier flowId) {
        if (player == null || flowId == null) {
            return 0;
        }
        return TutorialPlayerData.get(player).flowStepIds(flowId).size();
    }

    private static boolean matches(TutorialStep step, TutorialTriggerType triggerType, Identifier target) {
        if (step == null || step.type() != triggerType || target == null) {
            return false;
        }
        Identifier stepTarget = step.target();
        return stepTarget == null
                || stepTarget.equals(target)
                || stepTarget.toString().equals(target.getPath())
                || stepTarget.getPath().equals(target.getPath());
    }

    private static boolean isComplete(TutorialFlow flow, TutorialPlayerData data) {
        if (flow == null || data == null || flow.steps().isEmpty()) {
            return false;
        }
        for (TutorialStep step : flow.steps()) {
            if (!step.optional() && !data.hasFlowStep(flow.id(), stepKey(step))) {
                return false;
            }
        }
        return true;
    }

    private static String stepKey(TutorialStep step) {
        if (step.id() != null && !step.id().isBlank()) {
            return step.id();
        }
        return step.type().name().toLowerCase(java.util.Locale.ROOT) + ":" + step.target();
    }
}

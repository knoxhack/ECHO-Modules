package com.knoxhack.echoashfallprotocol.echo;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.integration.AshfallMissionCoreIntegration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class AshfallMissionActions {
    private static final String NO_TURN_IN = "This protocol is not ready for turn-in.";
    private static final String NO_CACHE = "No sealed support cache is waiting for this protocol.";

    private AshfallMissionActions() {
    }

    public static Mission resolveTarget(QuestData quest, String payload) {
        Mission current = currentMission(quest);
        String targetId = cleanMissionPayload(payload);
        if (targetId.isBlank()) {
            return current;
        }
        return MissionRegistry.getMissionById(targetId);
    }

    public static String cleanMissionPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return "";
        }
        String value = payload.trim();
        Identifier parsed = Identifier.tryParse(value);
        if (parsed != null && EchoAshfallProtocol.MODID.equals(parsed.getNamespace())) {
            return parsed.getPath();
        }
        int separator = value.indexOf(':');
        return separator >= 0 ? value.substring(separator + 1) : value;
    }

    public static boolean hasClaimableRewards(Player player, QuestData quest, Mission mission) {
        return quest != null
                && mission != null
                && (quest.hasPendingRewards(mission.id())
                || AshfallMissionCoreIntegration.hasClaimableReward(player, mission));
    }

    public static boolean canTurnIn(Player player, QuestData quest, Mission mission) {
        if (player == null || quest == null || mission == null) {
            return false;
        }
        QuestData.MissionStatus status = quest.getMissionStatus(mission.id());
        return status == QuestData.MissionStatus.UNLOCKED
                && !quest.isMissionCompleted(mission.id())
                && !isPathPreview(player, mission)
                && MissionUxSummary.isCurrentMission(quest, mission)
                && mission.isTurnInMission()
                && missionSatisfied(player, mission);
    }

    public static String turnInRejection(ServerPlayer player, QuestData quest, Mission mission) {
        if (mission == null) {
            return NO_TURN_IN;
        }
        QuestData.MissionStatus status = quest.getMissionStatus(mission.id());
        boolean preview = isPathPreview(player, mission);
        boolean current = MissionUxSummary.isCurrentMission(quest, mission);
        boolean completeNow = missionSatisfied(player, mission);
        if (canTurnIn(player, quest, mission)) {
            return "";
        }
        return turnInReason(player, quest, mission, status, current, completeNow, preview);
    }

    public static String turnInReason(Player player, QuestData quest, Mission mission,
            QuestData.MissionStatus status, boolean current, boolean completeNow, boolean preview) {
        if (mission == null) {
            return NO_TURN_IN;
        }
        if (status == QuestData.MissionStatus.LOCKED || preview || !quest.isMissionUnlocked(mission.id())) {
            return MissionUxSummary.unlockReason(player, quest, mission);
        }
        if (status == QuestData.MissionStatus.COMPLETED || quest.isMissionCompleted(mission.id())) {
            return hasClaimableRewards(player, quest, mission)
                    ? "Claim rewards for this completed protocol."
                    : "This protocol is already complete.";
        }
        if (!current) {
            Mission active = currentMission(quest);
            return "Finish/turn in "
                    + (active == null ? "the current active protocol" : active.objectiveText())
                    + " first.";
        }
        if (!mission.isTurnInMission()) {
            return "This protocol completes automatically when ECHO confirms the field state.";
        }
        if (!completeNow) {
            String missingRequirement = MissionUxSummary.turnInMissingRequirement(player, quest, mission);
            return missingRequirement.isBlank()
                    ? "Finish the visible requirements before returning this protocol."
                    : missingRequirement;
        }
        return "ECHO validation required.";
    }

    public static String claimReason(Player player, QuestData quest, Mission mission) {
        if (mission == null) {
            return NO_CACHE;
        }
        if (!quest.isMissionCompleted(mission.id())) {
            return "Complete this protocol before claiming rewards.";
        }
        return hasClaimableRewards(player, quest, mission)
                ? ""
                : NO_CACHE;
    }

    public static void sendTurnInRejection(ServerPlayer player, String rejection) {
        if (player == null || rejection == null || rejection.isBlank()) {
            return;
        }
        player.sendSystemMessage(Component.literal("[ECHO-7] " + rejection)
                .withStyle(ChatFormatting.YELLOW), true);
    }

    public static Mission currentMission(QuestData quest) {
        if (quest == null) {
            return null;
        }
        return MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
    }

    private static boolean missionSatisfied(Player player, Mission mission) {
        if (player == null || mission == null) {
            return false;
        }
        try {
            if (player instanceof ServerPlayer serverPlayer) {
                return EchoGuideManager.hasAllRequirements(serverPlayer, mission);
            }
            return mission.isComplete(player);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isPathPreview(Player player, Mission mission) {
        try {
            return player != null && mission.isPathPreview(player);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}

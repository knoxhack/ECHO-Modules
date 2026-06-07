package com.knoxhack.echo.npcore.service;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.IMissionProgressView;
import com.knoxhack.echocore.api.mission.IMissionService;
import com.knoxhack.echocore.api.mission.MissionStatus;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class NpcMissionBridge {
    private static final Set<String> IGNORED_REQUIREMENTS = ConcurrentHashMap.newKeySet();

    private NpcMissionBridge() {
    }

    public static List<Identifier> availableMissions(ServerPlayer player, Identifier npcProfileId) {
        IMissionService service = EchoCoreServices.missionService();
        if (player == null || service == null || !service.available()) {
            return List.of();
        }
        return service.missions(player).stream()
                .map(IMissionProgressView::id)
                .toList();
    }

    public static boolean missionRequirementMet(ServerPlayer player, String requirementId) {
        return checkMissionRequirement(player, requirementId).allowed();
    }

    public static RequirementResult checkMissionRequirement(ServerPlayer player, String requirementId) {
        String requirement = requirementId == null ? "" : requirementId.trim();
        if (requirement.isBlank()) {
            return RequirementResult.allow();
        }

        IMissionService service = EchoCoreServices.missionService();
        if (service == null || !service.available()) {
            if (IGNORED_REQUIREMENTS.add(requirement)) {
                EchoNpcCore.LOGGER.debug("NPCore ignored mission requirement {} because MissionCore is unavailable.", requirement);
            }
            return RequirementResult.allow();
        }

        Identifier missionId = Identifier.tryParse(requirement);
        if (missionId == null) {
            return RequirementResult.denied("Invalid mission requirement: " + requirement + ".");
        }
        if (service.missionDefinition(missionId).isEmpty()) {
            return RequirementResult.denied("Mission required but unavailable: " + missionId + ".");
        }
        IMissionProgressView view = service.mission(player, missionId).orElse(null);
        if (view == null || !allowedStatus(view.status())) {
            MissionStatus status = view == null ? MissionStatus.LOCKED : view.status();
            return RequirementResult.denied("Mission required: " + missionId + " (" + label(status) + ").");
        }
        return RequirementResult.allow();
    }

    private static boolean allowedStatus(MissionStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case ACTIVE, COMPLETED, CLAIMABLE, CLAIMED, VIEW_ONLY -> true;
            case LOCKED, UNLOCKED -> false;
        };
    }

    private static String label(MissionStatus status) {
        return status == null ? "unknown" : status.name().toLowerCase(Locale.ROOT);
    }

    public record RequirementResult(boolean allowed, String message) {
        public static RequirementResult allow() {
            return new RequirementResult(true, "");
        }

        public static RequirementResult denied(String message) {
            return new RequirementResult(false, message == null || message.isBlank() ? "Mission requirement not met." : message);
        }
    }
}

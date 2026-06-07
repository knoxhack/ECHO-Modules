package com.knoxhack.echo.npcore.service;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class NpcAvailability {
    private NpcAvailability() {
    }

    public static Result check(ServerPlayer player, Identifier factionId, String missionId, int factionStanding,
            String configuredReason) {
        NpcMissionBridge.RequirementResult mission = NpcMissionBridge.checkMissionRequirement(player, missionId);
        if (!mission.allowed()) {
            return Result.denied(reason(configuredReason, mission.message()));
        }
        NpcFactionBridge.RequirementResult faction =
                NpcFactionBridge.checkStanding(player, factionId, factionStanding);
        if (!faction.allowed()) {
            return Result.denied(reason(configuredReason, faction.message()));
        }
        return Result.allow();
    }

    private static String reason(String configured, String fallback) {
        return configured == null || configured.isBlank() ? fallback : configured.trim();
    }

    public record Result(boolean allowed, String message) {
        public static Result allow() {
            return new Result(true, "");
        }

        public static Result denied(String message) {
            return new Result(false, message == null || message.isBlank() ? "Requirement not met." : message);
        }
    }
}

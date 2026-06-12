package com.knoxhack.echo.npcore.service;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoFactionActionResult;
import com.echoplatform.echocore.api.EchoFactionProfile;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class NpcFactionBridge {
    private NpcFactionBridge() {
    }

    public static RequirementResult checkStanding(Player player, Identifier factionId, int requiredStanding) {
        if (requiredStanding == Integer.MIN_VALUE || factionId == null) {
            return RequirementResult.allow();
        }
        Optional<EchoFactionProfile> profile = EchoCoreServices.factionProfile(player, factionId);
        if (profile.isEmpty()) {
            return requiredStanding <= 0
                    ? RequirementResult.allow()
                    : RequirementResult.denied("Faction standing unavailable: " + factionId + ".");
        }
        if (profile.get().reputation() < requiredStanding) {
            return RequirementResult.denied("Requires " + factionId + " standing " + requiredStanding
                    + " (current " + profile.get().reputation() + ").");
        }
        return RequirementResult.allow();
    }

    public static void recordContact(ServerPlayer player, Identifier factionId, String roleId) {
        if (player == null || factionId == null) {
            return;
        }
        EchoCoreServices.markFactionContacted(player, factionId);
        EchoCoreServices.recordFactionInteraction(player, factionId, roleId, player.level().getGameTime());
    }

    public static String relationshipLabel(Player player, Identifier factionId) {
        return EchoCoreServices.factionProfile(player, factionId)
                .map(profile -> profile.standing().displayName() + " (" + profile.reputation() + ")")
                .orElse("Contacted");
    }

    public static EchoFactionActionResult perform(ServerPlayer player, Identifier factionId, String roleId,
            Identifier actionId, Identifier targetId) {
        return EchoCoreServices.performFactionAction(player, factionId, actionId, roleId, targetId);
    }

    public record RequirementResult(boolean allowed, String message) {
        public static RequirementResult allow() {
            return new RequirementResult(true, "");
        }

        public static RequirementResult denied(String message) {
            return new RequirementResult(false, message == null || message.isBlank()
                    ? "Faction standing requirement not met." : message);
        }
    }
}

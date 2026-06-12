package com.echoplatform.echocore.api.mission;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record MissionRuntimeEvent(
        Identifier eventType,
        ServerPlayer player,
        Identifier missionId,
        Identifier objectiveId,
        Identifier rewardId,
        int amount,
        Map<String, String> context) {
    public static final Identifier MISSION_STARTED = Identifier.fromNamespaceAndPath("echomissioncore", "mission_started");
    public static final Identifier OBJECTIVE_PROGRESSED = Identifier.fromNamespaceAndPath("echomissioncore", "objective_progressed");
    public static final Identifier MISSION_COMPLETED = Identifier.fromNamespaceAndPath("echomissioncore", "mission_completed");
    public static final Identifier REWARD_CLAIMED = Identifier.fromNamespaceAndPath("echomissioncore", "reward_claimed");
    public static final Identifier CHAPTER_UNLOCKED = Identifier.fromNamespaceAndPath("echomissioncore", "chapter_unlocked");

    public MissionRuntimeEvent {
        eventType = eventType == null ? Identifier.fromNamespaceAndPath("echomissioncore", "unknown") : eventType;
        amount = Math.max(0, amount);
        context = context == null ? Map.of() : Map.copyOf(context);
    }

    public static MissionRuntimeEvent of(
            Identifier eventType,
            ServerPlayer player,
            Identifier missionId,
            Identifier objectiveId,
            Identifier rewardId,
            int amount,
            Map<String, String> context) {
        return new MissionRuntimeEvent(eventType, player, missionId, objectiveId, rewardId, amount, context);
    }
}

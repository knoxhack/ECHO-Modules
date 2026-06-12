package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record EchoFactionContract(
        Identifier id,
        String title,
        String summary,
        int requiredReputation,
        int reputationReward,
        String objective,
        String reward,
        String route) {
    public EchoFactionContract {
        title = title == null ? "" : title;
        summary = summary == null ? "" : summary;
        objective = objective == null ? "" : objective;
        reward = reward == null ? "" : reward;
        route = route == null ? "" : route;
    }
}

package com.knoxhack.echo.questdirector;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoRecoveryAssistancePacing(
        String pacingId,
        EchoContentReference recoveryReference,
        double assistanceThreshold,
        int cooldownTicks,
        boolean allowOptionalRelief,
        boolean preserveChallenge,
        Map<String, String> attributes
) {
    public EchoRecoveryAssistancePacing {
        pacingId = QuestDirectorContractGuards.id(pacingId, "recovery assistance pacing id");
        assistanceThreshold = QuestDirectorContractGuards.ratio(assistanceThreshold, "assistance threshold");
        cooldownTicks = QuestDirectorContractGuards.nonNegative(cooldownTicks, "cooldown ticks");
        attributes = QuestDirectorContractGuards.immutableMap(attributes);
    }
}

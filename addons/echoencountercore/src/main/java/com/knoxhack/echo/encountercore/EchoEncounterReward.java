package com.knoxhack.echo.encountercore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Map;

public record EchoEncounterReward(
        String rewardId,
        EchoContentReference rewardReference,
        EchoFeatureId providedFeature,
        double weight,
        boolean repeatable,
        String playerSummary,
        Map<String, String> attributes
) {
    public EchoEncounterReward {
        rewardId = EncounterContractGuards.id(rewardId, "encounter reward id");
        weight = EncounterContractGuards.nonNegative(weight, "reward weight");
        playerSummary = EncounterContractGuards.optionalText(playerSummary);
        attributes = EncounterContractGuards.immutableMap(attributes);
    }
}

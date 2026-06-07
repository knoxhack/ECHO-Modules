package com.knoxhack.echo.difficultycore;

import java.util.Map;

public record EchoServerDifficultyPolicy(
        String policyId,
        EchoDifficultyProfileId forcedProfile,
        boolean serverAuthoritative,
        boolean allowClientHints,
        boolean allowTemporaryRelief,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoServerDifficultyPolicy {
        policyId = DifficultyContractGuards.id(policyId, "server difficulty policy id");
        playerSummary = DifficultyContractGuards.optionalText(playerSummary);
        developerDetails = DifficultyContractGuards.optionalText(developerDetails);
        attributes = DifficultyContractGuards.immutableMap(attributes);
    }
}

package com.knoxhack.echo.socialcore;

import java.util.Map;
import java.util.Objects;

public record EchoFactionReputation(
        String reputationId,
        EchoFactionId factionId,
        String subjectId,
        int score,
        EchoReputationBand band,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoFactionReputation {
        reputationId = SocialContractGuards.requireText(reputationId, "faction reputation id");
        Objects.requireNonNull(factionId, "factionId");
        subjectId = SocialContractGuards.requireText(subjectId, "reputation subject id");
        score = SocialContractGuards.boundedReputation(score);
        band = band == null ? EchoReputationBand.fromScore(score) : band;
        playerSummary = SocialContractGuards.optionalText(playerSummary);
        developerDetails = SocialContractGuards.optionalText(developerDetails);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean hostile() {
        return band.hostile();
    }
}

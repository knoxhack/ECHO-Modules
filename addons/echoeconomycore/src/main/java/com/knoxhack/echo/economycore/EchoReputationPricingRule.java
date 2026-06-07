package com.knoxhack.echo.economycore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.socialcore.EchoFactionId;
import com.knoxhack.echo.socialcore.EchoReputationBand;

import java.util.Map;

public record EchoReputationPricingRule(
        String ruleId,
        EchoFactionId factionId,
        EchoReputationBand minimumBand,
        int minimumReputation,
        double priceMultiplier,
        EchoContentGate gate,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoReputationPricingRule {
        ruleId = EconomyContractGuards.requireText(ruleId, "reputation pricing rule id");
        minimumBand = minimumBand == null ? EchoReputationBand.UNKNOWN : minimumBand;
        priceMultiplier = EconomyContractGuards.positiveMultiplier(priceMultiplier);
        gate = gate == null ? EchoContentGate.open() : gate;
        playerSummary = EconomyContractGuards.optionalText(playerSummary);
        developerDetails = EconomyContractGuards.optionalText(developerDetails);
        attributes = EconomyContractGuards.immutableMap(attributes);
    }
}

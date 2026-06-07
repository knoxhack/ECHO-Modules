package com.knoxhack.echo.lootcore;

import com.knoxhack.echo.contentcore.EchoContentGate;

import java.util.Map;

public record EchoAntiDuplicationRule(
        String ruleId,
        EchoDuplicationPolicy policy,
        int cooldownTicks,
        EchoContentGate gate,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoAntiDuplicationRule {
        ruleId = LootContractGuards.requireText(ruleId, "anti-duplication rule id");
        policy = policy == null ? EchoDuplicationPolicy.UNKNOWN : policy;
        cooldownTicks = LootContractGuards.nonNegative(cooldownTicks, "anti-duplication cooldown ticks");
        gate = gate == null ? EchoContentGate.open() : gate;
        playerSummary = LootContractGuards.optionalText(playerSummary);
        developerDetails = LootContractGuards.optionalText(developerDetails);
        attributes = LootContractGuards.immutableMap(attributes);
    }
}

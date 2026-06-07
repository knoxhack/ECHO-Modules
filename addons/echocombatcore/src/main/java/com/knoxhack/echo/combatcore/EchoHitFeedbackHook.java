package com.knoxhack.echo.combatcore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Map;
import java.util.Set;

public record EchoHitFeedbackHook(
        String hookId,
        EchoDamageTypeId damageTypeId,
        EchoContentReference soundProfileReference,
        EchoContentReference particleProfileReference,
        EchoContentReference renderProfileReference,
        Set<EchoFeatureId> optionalIntegrationFeatures,
        String playerSummary,
        Map<String, String> attributes
) {
    public EchoHitFeedbackHook {
        hookId = CombatContractGuards.requireText(hookId, "hit feedback hook id");
        optionalIntegrationFeatures = CombatContractGuards.immutableSet(optionalIntegrationFeatures);
        playerSummary = CombatContractGuards.optionalText(playerSummary);
        attributes = CombatContractGuards.immutableMap(attributes);
    }
}

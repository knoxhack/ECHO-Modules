package com.knoxhack.echo.lorecore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record EchoLoreUnlockCondition(
        String conditionId,
        EchoContentGate gate,
        Set<EchoFeatureId> requiredFeatures,
        List<EchoContentReference> requiredContent,
        boolean optional,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoLoreUnlockCondition {
        conditionId = LoreContractGuards.id(conditionId, "lore unlock condition id");
        gate = gate == null ? EchoContentGate.open() : gate;
        requiredFeatures = LoreContractGuards.immutableSet(requiredFeatures);
        requiredContent = LoreContractGuards.immutableList(requiredContent);
        playerSummary = LoreContractGuards.optionalText(playerSummary);
        developerDetails = LoreContractGuards.optionalText(developerDetails);
        attributes = LoreContractGuards.immutableMap(attributes);
    }

    public boolean blocksUnlock() {
        return !optional && (gate.blocksWhenMissing() || !requiredFeatures.isEmpty() || !requiredContent.isEmpty());
    }
}

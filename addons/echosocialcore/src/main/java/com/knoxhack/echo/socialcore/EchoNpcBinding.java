package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.progressioncore.EchoObjectiveId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;

import java.util.Map;

public record EchoNpcBinding(
        String bindingId,
        EchoNpcBindingKind kind,
        EchoNpcProfileId npcProfileId,
        EchoFactionId factionId,
        EchoDialogueTreeId dialogueTreeId,
        EchoProgressionId progressionId,
        EchoObjectiveId objectiveId,
        EchoContentReference contentReference,
        EchoFeatureId optionalIntegrationFeature,
        boolean optional,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoNpcBinding {
        bindingId = SocialContractGuards.requireText(bindingId, "npc binding id");
        kind = kind == null ? EchoNpcBindingKind.UNKNOWN : kind;
        playerSummary = SocialContractGuards.optionalText(playerSummary);
        developerDetails = SocialContractGuards.optionalText(developerDetails);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean hardDependency() {
        return !optional && optionalIntegrationFeature == null;
    }
}

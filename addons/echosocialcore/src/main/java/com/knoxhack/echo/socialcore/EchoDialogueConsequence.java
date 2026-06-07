package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.progressioncore.EchoObjectiveId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;
import com.knoxhack.echo.progressioncore.EchoUnlockNodeId;

import java.util.Map;

public record EchoDialogueConsequence(
        String consequenceId,
        EchoDialogueConsequenceKind kind,
        EchoDialogueNodeId targetNodeId,
        EchoFactionId factionId,
        int reputationDelta,
        EchoProgressionId progressionId,
        EchoUnlockNodeId unlockNodeId,
        EchoObjectiveId objectiveId,
        EchoContentReference contentReference,
        EchoFeatureId featureId,
        EchoContentId voiceLineContent,
        EchoContentId cinematicCameraContent,
        String translationKey,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoDialogueConsequence {
        consequenceId = SocialContractGuards.requireText(consequenceId, "dialogue consequence id");
        kind = kind == null ? EchoDialogueConsequenceKind.UNKNOWN : kind;
        reputationDelta = SocialContractGuards.boundedReputation(reputationDelta);
        translationKey = SocialContractGuards.optionalText(translationKey);
        playerSummary = SocialContractGuards.optionalText(playerSummary);
        developerDetails = SocialContractGuards.optionalText(developerDetails);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean externalReferenceOnly() {
        return contentReference != null || featureId != null || cinematicCameraContent != null || voiceLineContent != null;
    }
}

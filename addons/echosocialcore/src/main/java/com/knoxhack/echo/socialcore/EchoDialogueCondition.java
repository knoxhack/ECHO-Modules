package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.progressioncore.EchoObjectiveId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;
import com.knoxhack.echo.progressioncore.EchoUnlockNodeId;

import java.util.Map;

public record EchoDialogueCondition(
        String conditionId,
        EchoDialogueConditionKind kind,
        EchoFactionId factionId,
        EchoReputationBand reputationBand,
        int minimumReputation,
        EchoHostilityState hostilityState,
        EchoAllianceState allianceState,
        EchoProgressionId progressionId,
        EchoUnlockNodeId unlockNodeId,
        EchoObjectiveId objectiveId,
        EchoContentReference contentReference,
        EchoContentGate gate,
        EchoFeatureId featureId,
        boolean inverted,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoDialogueCondition {
        conditionId = SocialContractGuards.requireText(conditionId, "dialogue condition id");
        kind = kind == null ? EchoDialogueConditionKind.UNKNOWN : kind;
        reputationBand = reputationBand == null ? EchoReputationBand.UNKNOWN : reputationBand;
        minimumReputation = SocialContractGuards.boundedReputation(minimumReputation);
        hostilityState = hostilityState == null ? EchoHostilityState.UNKNOWN : hostilityState;
        allianceState = allianceState == null ? EchoAllianceState.UNKNOWN : allianceState;
        gate = gate == null ? EchoContentGate.open() : gate;
        playerSummary = SocialContractGuards.optionalText(playerSummary);
        developerDetails = SocialContractGuards.optionalText(developerDetails);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean integrationReferenceOnly() {
        return featureId != null || contentReference != null || gate.gated();
    }
}

package com.knoxhack.echo.progressioncore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.recipecore.EchoRecipeId;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoObjectiveCondition(
        String conditionId,
        EchoObjectiveType type,
        EchoContentId targetContent,
        EchoRecipeId targetRecipe,
        EchoFeatureId targetFeature,
        EchoContentGate gate,
        int requiredProgress,
        Set<EchoUnlockNodeId> requiredUnlocks,
        Set<EchoFeatureId> requiredFeatures,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoObjectiveCondition {
        conditionId = ProgressionContractGuards.requireText(conditionId, "objective condition id");
        type = type == null ? EchoObjectiveType.UNKNOWN : type;
        gate = Objects.requireNonNullElseGet(gate, EchoContentGate::open);
        requiredProgress = ProgressionContractGuards.positiveOrOne(requiredProgress);
        requiredUnlocks = ProgressionContractGuards.immutableSet(requiredUnlocks);
        requiredFeatures = ProgressionContractGuards.immutableSet(requiredFeatures);
        playerSummary = ProgressionContractGuards.optionalText(playerSummary);
        developerDetails = ProgressionContractGuards.optionalText(developerDetails);
        attributes = ProgressionContractGuards.immutableMap(attributes);
    }

    public static EchoObjectiveCondition simple(String conditionId, EchoObjectiveType type, int requiredProgress) {
        return new EchoObjectiveCondition(
                conditionId,
                type,
                null,
                null,
                null,
                EchoContentGate.open(),
                requiredProgress,
                Set.of(),
                Set.of(),
                "",
                "",
                Map.of()
        );
    }

    public boolean gated() {
        return gate.gated() || !requiredUnlocks.isEmpty() || !requiredFeatures.isEmpty();
    }
}

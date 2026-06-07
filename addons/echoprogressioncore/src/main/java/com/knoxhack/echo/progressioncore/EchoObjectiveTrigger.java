package com.knoxhack.echo.progressioncore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.recipecore.EchoRecipeId;

import java.util.Map;

public record EchoObjectiveTrigger(
        String triggerId,
        EchoObjectiveTriggerKind kind,
        EchoObjectiveType objectiveType,
        EchoContentId sourceContent,
        EchoContentId targetContent,
        EchoRecipeId recipeId,
        EchoFeatureId featureId,
        EchoObjectiveScope scope,
        String eventName,
        Map<String, String> filters,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoObjectiveTrigger {
        triggerId = ProgressionContractGuards.requireText(triggerId, "objective trigger id");
        kind = kind == null ? EchoObjectiveTriggerKind.UNKNOWN : kind;
        objectiveType = objectiveType == null ? EchoObjectiveType.UNKNOWN : objectiveType;
        scope = scope == null ? EchoObjectiveScope.PLAYER : scope;
        eventName = ProgressionContractGuards.optionalText(eventName);
        filters = ProgressionContractGuards.immutableMap(filters);
        playerSummary = ProgressionContractGuards.optionalText(playerSummary);
        developerDetails = ProgressionContractGuards.optionalText(developerDetails);
        attributes = ProgressionContractGuards.immutableMap(attributes);
    }

    public boolean teamOrServerScoped() {
        return scope == EchoObjectiveScope.TEAM || scope == EchoObjectiveScope.SERVER;
    }
}

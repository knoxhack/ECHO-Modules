package com.knoxhack.echo.progressioncore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.recipecore.EchoRecipeId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoObjectiveDefinition(
        EchoObjectiveId id,
        EchoObjectiveType type,
        String title,
        String summary,
        EchoObjectiveScope scope,
        EchoModuleId owningModule,
        EchoContentId targetContent,
        EchoRecipeId targetRecipe,
        EchoFeatureId targetFeature,
        List<EchoObjectiveCondition> conditions,
        List<EchoObjectiveTrigger> triggers,
        int requiredProgress,
        boolean hiddenUntilUnlocked,
        List<EchoUnlockNodeId> unlocks,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoObjectiveDefinition {
        Objects.requireNonNull(id, "id");
        type = type == null ? EchoObjectiveType.UNKNOWN : type;
        title = ProgressionContractGuards.requireText(title, "objective title");
        summary = ProgressionContractGuards.optionalText(summary);
        scope = scope == null ? EchoObjectiveScope.PLAYER : scope;
        conditions = ProgressionContractGuards.immutableList(conditions);
        triggers = ProgressionContractGuards.immutableList(triggers);
        requiredProgress = ProgressionContractGuards.positiveOrOne(requiredProgress);
        unlocks = ProgressionContractGuards.immutableList(unlocks);
        diagnostics = ProgressionContractGuards.immutableList(diagnostics);
        attributes = ProgressionContractGuards.immutableMap(attributes);
    }

    public boolean sharedObjective() {
        return scope.shared();
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}

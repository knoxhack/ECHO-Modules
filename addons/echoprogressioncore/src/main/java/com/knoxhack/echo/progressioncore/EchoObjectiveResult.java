package com.knoxhack.echo.progressioncore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.recipecore.EchoRecipeId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoObjectiveResult(
        EchoObjectiveId objectiveId,
        EchoObjectiveResultStatus status,
        EchoObjectiveProgress progress,
        List<EchoUnlockNodeId> unlockedNodes,
        List<EchoRecipeId> unlockedRecipes,
        List<EchoContentId> unlockedContent,
        List<EchoDiagnostic> diagnostics,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoObjectiveResult {
        Objects.requireNonNull(objectiveId, "objectiveId");
        status = status == null ? EchoObjectiveResultStatus.UNKNOWN : status;
        progress = progress == null ? EchoObjectiveProgress.empty(objectiveId, 1) : progress;
        unlockedNodes = ProgressionContractGuards.immutableList(unlockedNodes);
        unlockedRecipes = ProgressionContractGuards.immutableList(unlockedRecipes);
        unlockedContent = ProgressionContractGuards.immutableList(unlockedContent);
        diagnostics = ProgressionContractGuards.immutableList(diagnostics);
        playerSummary = ProgressionContractGuards.optionalText(playerSummary);
        developerDetails = ProgressionContractGuards.optionalText(developerDetails);
        attributes = ProgressionContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return status == EchoObjectiveResultStatus.BLOCKED
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}

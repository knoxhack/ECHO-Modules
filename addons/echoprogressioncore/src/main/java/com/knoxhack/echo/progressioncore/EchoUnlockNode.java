package com.knoxhack.echo.progressioncore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.recipecore.EchoRecipeId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoUnlockNode(
        EchoUnlockNodeId id,
        EchoProgressionId progressionId,
        EchoUnlockKind kind,
        String title,
        String summary,
        EchoModuleId owningModule,
        EchoContentId contentId,
        EchoRecipeId recipeId,
        EchoFeatureId featureId,
        EchoUnlockState state,
        EchoUnlockCondition condition,
        Set<EchoObjectiveId> objectiveIds,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoUnlockNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(progressionId, "progressionId");
        kind = kind == null ? EchoUnlockKind.UNKNOWN : kind;
        title = ProgressionContractGuards.requireText(title, "unlock node title");
        summary = ProgressionContractGuards.optionalText(summary);
        state = state == null ? EchoUnlockState.LOCKED : state;
        condition = condition == null ? EchoUnlockCondition.open(id + "/condition", kind) : condition;
        objectiveIds = ProgressionContractGuards.immutableSet(objectiveIds);
        diagnostics = ProgressionContractGuards.immutableList(diagnostics);
        attributes = ProgressionContractGuards.immutableMap(attributes);
    }

    public boolean blocksProgression() {
        return state == EchoUnlockState.BLOCKED
                || condition.blocksProgression()
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }

    public boolean uiSurfaceUnlock() {
        return kind.uiSurfaceUnlock();
    }
}

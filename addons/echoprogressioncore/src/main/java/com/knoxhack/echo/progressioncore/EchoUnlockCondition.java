package com.knoxhack.echo.progressioncore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoGameModeId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.recipecore.EchoRecipeId;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoUnlockCondition(
        String conditionId,
        EchoUnlockKind kind,
        Set<EchoFeatureId> requiredFeatures,
        Set<EchoModuleId> requiredModules,
        Set<EchoContentId> requiredContent,
        Set<EchoRecipeId> requiredRecipes,
        Set<EchoObjectiveId> requiredObjectives,
        Set<EchoProgressionId> requiredProgression,
        Set<EchoGameModeId> gameModes,
        EchoContentGate gate,
        boolean optional,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoUnlockCondition {
        conditionId = ProgressionContractGuards.requireText(conditionId, "unlock condition id");
        kind = kind == null ? EchoUnlockKind.UNKNOWN : kind;
        requiredFeatures = ProgressionContractGuards.immutableSet(requiredFeatures);
        requiredModules = ProgressionContractGuards.immutableSet(requiredModules);
        requiredContent = ProgressionContractGuards.immutableSet(requiredContent);
        requiredRecipes = ProgressionContractGuards.immutableSet(requiredRecipes);
        requiredObjectives = ProgressionContractGuards.immutableSet(requiredObjectives);
        requiredProgression = ProgressionContractGuards.immutableSet(requiredProgression);
        gameModes = ProgressionContractGuards.immutableSet(gameModes);
        gate = Objects.requireNonNullElseGet(gate, EchoContentGate::open);
        playerSummary = ProgressionContractGuards.optionalText(playerSummary);
        developerDetails = ProgressionContractGuards.optionalText(developerDetails);
        attributes = ProgressionContractGuards.immutableMap(attributes);
    }

    public static EchoUnlockCondition open(String conditionId, EchoUnlockKind kind) {
        return new EchoUnlockCondition(
                conditionId,
                kind,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                EchoContentGate.open(),
                true,
                "",
                "",
                Map.of()
        );
    }

    public boolean gated() {
        return !requiredFeatures.isEmpty()
                || !requiredModules.isEmpty()
                || !requiredContent.isEmpty()
                || !requiredRecipes.isEmpty()
                || !requiredObjectives.isEmpty()
                || !requiredProgression.isEmpty()
                || !gameModes.isEmpty()
                || gate.gated();
    }

    public boolean blocksProgression() {
        return gated() && !optional;
    }
}

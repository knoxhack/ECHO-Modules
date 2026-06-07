package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoGameModeId;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoRecipeUnlockCondition(
        String conditionId,
        EchoRecipeId recipeId,
        Set<EchoFeatureId> requiredFeatures,
        Set<EchoModuleId> requiredModules,
        Set<EchoContentId> requiredContent,
        Set<EchoGameModeId> gameModes,
        EchoContentGate gate,
        boolean hiddenUntilUnlocked,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoRecipeUnlockCondition {
        conditionId = RecipeContractGuards.requireText(conditionId, "recipe unlock condition id");
        Objects.requireNonNull(recipeId, "recipeId");
        requiredFeatures = RecipeContractGuards.immutableSet(requiredFeatures);
        requiredModules = RecipeContractGuards.immutableSet(requiredModules);
        requiredContent = RecipeContractGuards.immutableSet(requiredContent);
        gameModes = RecipeContractGuards.immutableSet(gameModes);
        gate = gate == null ? EchoContentGate.open() : gate;
        playerSummary = RecipeContractGuards.optionalText(playerSummary);
        developerDetails = RecipeContractGuards.optionalText(developerDetails);
        attributes = RecipeContractGuards.immutableMap(attributes);
    }

    public static EchoRecipeUnlockCondition open(EchoRecipeId recipeId) {
        return new EchoRecipeUnlockCondition(
                recipeId + "/open",
                recipeId,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                EchoContentGate.open(),
                false,
                "",
                "",
                Map.of()
        );
    }

    public boolean gated() {
        return !requiredFeatures.isEmpty()
                || !requiredModules.isEmpty()
                || !requiredContent.isEmpty()
                || !gameModes.isEmpty()
                || gate.gated();
    }
}

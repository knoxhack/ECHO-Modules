package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.contentcore.EchoContentSource;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record EchoRecipeDisplayModel(
        EchoRecipeId recipeId,
        EchoRecipeType type,
        String categoryId,
        String displayName,
        String summary,
        List<EchoRecipeIngredient> ingredients,
        List<EchoRecipeOutput> outputs,
        List<EchoRecipeUsage> usages,
        EchoContentId iconContent,
        EchoRecipeUnlockCondition unlockCondition,
        EchoContentSource source,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoRecipeDisplayModel {
        Objects.requireNonNull(recipeId, "recipeId");
        type = type == null ? EchoRecipeType.UNKNOWN : type;
        categoryId = RecipeContractGuards.optionalText(categoryId);
        displayName = RecipeContractGuards.optionalText(displayName);
        summary = RecipeContractGuards.optionalText(summary);
        ingredients = RecipeContractGuards.immutableList(ingredients);
        outputs = RecipeContractGuards.immutableList(outputs);
        usages = RecipeContractGuards.immutableList(usages);
        unlockCondition = unlockCondition == null ? EchoRecipeUnlockCondition.open(recipeId) : unlockCondition;
        diagnostics = RecipeContractGuards.immutableList(diagnostics);
        attributes = RecipeContractGuards.immutableMap(attributes);
    }

    public Optional<EchoRecipeOutput> primaryOutput() {
        return outputs.stream()
                .filter(EchoRecipeOutput::primary)
                .findFirst()
                .or(() -> outputs.stream().findFirst());
    }

    public boolean locked() {
        return unlockCondition.gated();
    }

    public boolean hasBlockingDiagnostics() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || ingredients.stream().anyMatch(EchoRecipeIngredient::blockingWhenUnavailable);
    }
}

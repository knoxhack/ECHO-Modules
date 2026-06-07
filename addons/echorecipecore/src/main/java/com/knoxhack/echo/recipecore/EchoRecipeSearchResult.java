package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoRecipeSearchResult(
        EchoRecipeQuery query,
        List<EchoRecipeDisplayModel> recipes,
        List<EchoMachineRecipeView> machineRecipes,
        List<EchoRecipeUsage> usages,
        List<EchoDiagnostic> diagnostics,
        long generatedAtEpochMillis,
        boolean partial,
        String summary,
        Map<String, String> attributes
) {
    public EchoRecipeSearchResult {
        Objects.requireNonNull(query, "query");
        recipes = RecipeContractGuards.immutableList(recipes);
        machineRecipes = RecipeContractGuards.immutableList(machineRecipes);
        usages = RecipeContractGuards.immutableList(usages);
        diagnostics = RecipeContractGuards.immutableList(diagnostics);
        generatedAtEpochMillis = RecipeContractGuards.nonNegativeLong(generatedAtEpochMillis, "recipe search generated time");
        summary = RecipeContractGuards.optionalText(summary);
        attributes = RecipeContractGuards.immutableMap(attributes);
    }

    public int resultCount() {
        return recipes.size() + machineRecipes.size() + usages.size();
    }

    public boolean hasBlockingDiagnostics() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || recipes.stream().anyMatch(EchoRecipeDisplayModel::hasBlockingDiagnostics)
                || machineRecipes.stream().anyMatch(EchoMachineRecipeView::blocked);
    }
}

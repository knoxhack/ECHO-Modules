package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record EchoRecipeGraph(
        String graphId,
        long createdAtEpochMillis,
        List<EchoRecipeCategory> categories,
        List<EchoRecipeDisplayModel> recipes,
        List<EchoMachineRecipeView> machineRecipes,
        List<EchoRecipeUsage> usages,
        List<EchoRecipeUnlockCondition> unlockConditions,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoRecipeGraph {
        graphId = RecipeContractGuards.requireText(graphId, "recipe graph id");
        createdAtEpochMillis = RecipeContractGuards.nonNegativeLong(createdAtEpochMillis, "recipe graph created time");
        categories = RecipeContractGuards.immutableList(categories);
        recipes = RecipeContractGuards.immutableList(recipes);
        machineRecipes = RecipeContractGuards.immutableList(machineRecipes);
        usages = RecipeContractGuards.immutableList(usages);
        unlockConditions = RecipeContractGuards.immutableList(unlockConditions);
        diagnostics = RecipeContractGuards.immutableList(diagnostics);
        attributes = RecipeContractGuards.immutableMap(attributes);
    }

    public Optional<EchoRecipeDisplayModel> recipe(EchoRecipeId recipeId) {
        return recipes.stream()
                .filter(recipe -> recipe.recipeId().equals(recipeId))
                .findFirst();
    }

    public List<EchoRecipeDisplayModel> recipesInCategory(String categoryId) {
        String normalized = RecipeContractGuards.optionalText(categoryId);
        return recipes.stream()
                .filter(recipe -> recipe.categoryId().equals(normalized))
                .toList();
    }

    public List<EchoRecipeUsage> usagesFor(EchoContentId contentId) {
        return usages.stream()
                .filter(usage -> usage.contentId().equals(contentId))
                .toList();
    }

    public List<EchoRecipeDisplayModel> recipesForOutput(EchoContentId contentId) {
        return recipes.stream()
                .filter(recipe -> recipe.outputs().stream()
                        .anyMatch(output -> output.contentId().equals(contentId)))
                .toList();
    }

    public boolean hasBlockingDiagnostics() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || recipes.stream().anyMatch(EchoRecipeDisplayModel::hasBlockingDiagnostics)
                || machineRecipes.stream().anyMatch(EchoMachineRecipeView::blocked);
    }
}

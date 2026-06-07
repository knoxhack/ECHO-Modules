package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Map;
import java.util.Set;

public record EchoRecipeQuery(
        String queryId,
        String text,
        Set<EchoRecipeType> types,
        Set<String> categoryIds,
        Set<EchoContentId> contentIds,
        Set<EchoModuleId> moduleIds,
        Set<EchoFeatureId> featureIds,
        boolean includeLocked,
        boolean includeHidden,
        boolean includeDiagnostics,
        int limit,
        Map<String, String> attributes
) {
    public EchoRecipeQuery {
        queryId = RecipeContractGuards.requireText(queryId, "recipe query id");
        text = RecipeContractGuards.optionalText(text);
        types = RecipeContractGuards.immutableSet(types);
        categoryIds = RecipeContractGuards.immutableSet(categoryIds);
        contentIds = RecipeContractGuards.immutableSet(contentIds);
        moduleIds = RecipeContractGuards.immutableSet(moduleIds);
        featureIds = RecipeContractGuards.immutableSet(featureIds);
        limit = limit <= 0 ? 100 : Math.min(limit, 10_000);
        attributes = RecipeContractGuards.immutableMap(attributes);
    }

    public static EchoRecipeQuery all(String queryId) {
        return new EchoRecipeQuery(queryId, "", Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), true, false, true, 100, Map.of());
    }

    public boolean filtered() {
        return !text.isEmpty()
                || !types.isEmpty()
                || !categoryIds.isEmpty()
                || !contentIds.isEmpty()
                || !moduleIds.isEmpty()
                || !featureIds.isEmpty();
    }
}

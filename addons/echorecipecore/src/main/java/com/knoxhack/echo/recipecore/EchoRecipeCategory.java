package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Map;
import java.util.Set;

public record EchoRecipeCategory(
        String categoryId,
        String displayName,
        String summary,
        EchoRecipeType defaultType,
        EchoContentId iconContent,
        EchoModuleId ownerModule,
        Set<EchoFeatureId> providedFeatures,
        int sortOrder,
        String color,
        Map<String, String> attributes
) {
    public EchoRecipeCategory {
        categoryId = RecipeContractGuards.requireText(categoryId, "recipe category id");
        displayName = RecipeContractGuards.requireText(displayName, "recipe category display name");
        summary = RecipeContractGuards.optionalText(summary);
        defaultType = defaultType == null ? EchoRecipeType.UNKNOWN : defaultType;
        providedFeatures = RecipeContractGuards.immutableSet(providedFeatures);
        sortOrder = RecipeContractGuards.nonNegative(sortOrder, "recipe category sort order");
        color = RecipeContractGuards.optionalText(color);
        attributes = RecipeContractGuards.immutableMap(attributes);
    }
}

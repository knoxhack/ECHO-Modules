package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.contentcore.EchoContentKind;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Map;
import java.util.Objects;

public record EchoRecipeUsage(
        String usageId,
        EchoRecipeId recipeId,
        EchoContentId contentId,
        EchoContentKind contentKind,
        EchoRecipeUsageKind usageKind,
        EchoModuleId declaringModule,
        boolean required,
        String summary,
        Map<String, String> attributes
) {
    public EchoRecipeUsage {
        usageId = RecipeContractGuards.requireText(usageId, "recipe usage id");
        Objects.requireNonNull(recipeId, "recipeId");
        Objects.requireNonNull(contentId, "contentId");
        contentKind = contentKind == null ? EchoContentKind.ITEM : contentKind;
        usageKind = usageKind == null ? EchoRecipeUsageKind.UNKNOWN : usageKind;
        summary = RecipeContractGuards.optionalText(summary);
        attributes = RecipeContractGuards.immutableMap(attributes);
    }

    public boolean outputLike() {
        return usageKind == EchoRecipeUsageKind.OUTPUT || usageKind == EchoRecipeUsageKind.BYPRODUCT;
    }

    public boolean inputLike() {
        return usageKind == EchoRecipeUsageKind.INPUT
                || usageKind == EchoRecipeUsageKind.CATALYST
                || usageKind == EchoRecipeUsageKind.TOOL;
    }
}

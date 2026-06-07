package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.contentcore.EchoContentAvailability;
import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.contentcore.EchoContentKind;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record EchoRecipeIngredient(
        String ingredientId,
        List<EchoContentId> acceptedContent,
        Set<String> acceptedTags,
        EchoContentKind contentKind,
        int count,
        boolean optional,
        boolean consumed,
        EchoContentAvailability availability,
        EchoContentGate gate,
        String label,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoRecipeIngredient {
        ingredientId = RecipeContractGuards.requireText(ingredientId, "recipe ingredient id");
        acceptedContent = RecipeContractGuards.immutableList(acceptedContent);
        acceptedTags = RecipeContractGuards.immutableSet(acceptedTags);
        contentKind = contentKind == null ? EchoContentKind.ITEM : contentKind;
        count = RecipeContractGuards.positive(count, "recipe ingredient count");
        availability = availability == null ? EchoContentAvailability.UNKNOWN : availability;
        gate = gate == null ? EchoContentGate.open() : gate;
        label = RecipeContractGuards.optionalText(label);
        developerDetails = RecipeContractGuards.optionalText(developerDetails);
        attributes = RecipeContractGuards.immutableMap(attributes);
    }

    public boolean tagBacked() {
        return !acceptedTags.isEmpty();
    }

    public boolean itemBacked() {
        return !acceptedContent.isEmpty();
    }

    public boolean blockingWhenUnavailable() {
        return !optional && !availability.available();
    }
}

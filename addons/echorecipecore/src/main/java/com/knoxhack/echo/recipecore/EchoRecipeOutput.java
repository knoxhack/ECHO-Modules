package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.contentcore.EchoContentAvailability;
import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.contentcore.EchoContentKind;

import java.util.Map;
import java.util.Objects;

public record EchoRecipeOutput(
        String outputId,
        EchoContentId contentId,
        EchoContentKind contentKind,
        int count,
        double chance,
        boolean primary,
        EchoContentAvailability availability,
        EchoContentGate gate,
        String label,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoRecipeOutput {
        outputId = RecipeContractGuards.requireText(outputId, "recipe output id");
        Objects.requireNonNull(contentId, "contentId");
        contentKind = contentKind == null ? EchoContentKind.ITEM : contentKind;
        count = RecipeContractGuards.positive(count, "recipe output count");
        chance = RecipeContractGuards.boundedChance(chance);
        availability = availability == null ? EchoContentAvailability.UNKNOWN : availability;
        gate = gate == null ? EchoContentGate.open() : gate;
        label = RecipeContractGuards.optionalText(label);
        developerDetails = RecipeContractGuards.optionalText(developerDetails);
        attributes = RecipeContractGuards.immutableMap(attributes);
    }

    public boolean guaranteed() {
        return chance >= 1.0D;
    }
}

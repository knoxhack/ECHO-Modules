package dev.echo.api.recipe;

import java.util.List;

public record EchoRecipeDescriptor(
        EchoRecipeId id,
        EchoRecipeType type,
        List<EchoIngredient> ingredients,
        EchoRecipeOutput output
) {
    public EchoRecipeDescriptor {
        ingredients = List.copyOf(ingredients);
    }
}

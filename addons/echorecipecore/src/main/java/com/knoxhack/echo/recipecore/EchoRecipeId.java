package com.knoxhack.echo.recipecore;

import java.util.Locale;

public record EchoRecipeId(String value) {
    public EchoRecipeId {
        value = RecipeContractGuards.requireText(value, "recipe id").toLowerCase(Locale.ROOT);
    }

    public static EchoRecipeId of(String value) {
        return new EchoRecipeId(value);
    }

    public static EchoRecipeId of(String namespace, String path) {
        return new EchoRecipeId(
                RecipeContractGuards.requireText(namespace, "recipe id namespace")
                        + ":"
                        + RecipeContractGuards.requireText(path, "recipe id path")
        );
    }

    public String namespace() {
        int split = value.indexOf(':');
        return split < 0 ? "" : value.substring(0, split);
    }

    public String path() {
        int split = value.indexOf(':');
        return split < 0 ? value : value.substring(split + 1);
    }

    public boolean namespaced() {
        return value.indexOf(':') > 0 && value.indexOf(':') < value.length() - 1;
    }

    @Override
    public String toString() {
        return value;
    }
}

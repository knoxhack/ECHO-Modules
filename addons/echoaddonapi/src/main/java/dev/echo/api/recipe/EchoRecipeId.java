package dev.echo.api.recipe;

import java.util.Objects;

public record EchoRecipeId(String value) {
    public EchoRecipeId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("recipe id must not be blank");
        }
    }
}

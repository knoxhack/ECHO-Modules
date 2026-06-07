package dev.echo.api.recipe;

import java.util.Set;

public record EchoIngredient(Set<String> acceptedItemIds, int count) {
    public EchoIngredient {
        acceptedItemIds = Set.copyOf(acceptedItemIds);
        if (acceptedItemIds.isEmpty()) {
            throw new IllegalArgumentException("acceptedItemIds must not be empty");
        }
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive");
        }
    }
}

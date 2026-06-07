package dev.echo.api.recipe;

import java.util.List;

public record EchoRecipeMatchContext(List<String> inputItemIds) {
    public EchoRecipeMatchContext {
        inputItemIds = List.copyOf(inputItemIds);
    }
}

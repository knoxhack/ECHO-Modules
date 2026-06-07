package dev.echo.api.recipe;

public record EchoRecipeOutput(String itemId, int count) {
    public EchoRecipeOutput {
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive");
        }
    }
}

package dev.echo.api.item;

import java.util.Map;

public record EchoItemStackView(EchoItemId itemId, int count, Map<String, String> components) {
    public EchoItemStackView {
        components = Map.copyOf(components);
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
    }
}

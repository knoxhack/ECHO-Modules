package dev.echo.api.item;

import java.util.Set;

public record EchoItemSettings(int maxStackSize, int durability, Set<String> tags) {
    public EchoItemSettings {
        tags = Set.copyOf(tags);
        if (maxStackSize < 1) {
            throw new IllegalArgumentException("maxStackSize must be positive");
        }
        if (durability < 0) {
            throw new IllegalArgumentException("durability must not be negative");
        }
    }

    public static EchoItemSettings defaults() {
        return new EchoItemSettings(64, 0, Set.of());
    }
}

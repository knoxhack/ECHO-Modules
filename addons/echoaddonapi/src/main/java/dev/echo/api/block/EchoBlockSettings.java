package dev.echo.api.block;

import java.util.Set;

public record EchoBlockSettings(float hardness, float resistance, boolean requiresTool, Set<String> tags) {
    public EchoBlockSettings {
        tags = Set.copyOf(tags);
        if (hardness < 0.0F || resistance < 0.0F) {
            throw new IllegalArgumentException("hardness and resistance must not be negative");
        }
    }

    public static EchoBlockSettings defaults() {
        return new EchoBlockSettings(1.0F, 1.0F, false, Set.of());
    }
}

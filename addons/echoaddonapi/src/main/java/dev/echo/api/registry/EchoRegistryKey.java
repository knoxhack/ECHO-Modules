package dev.echo.api.registry;

import java.util.Objects;

public record EchoRegistryKey<T>(String value) {
    public EchoRegistryKey {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("registry key must not be blank");
        }
    }
}

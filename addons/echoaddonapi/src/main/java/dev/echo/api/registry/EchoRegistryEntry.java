package dev.echo.api.registry;

import java.util.Objects;

public record EchoRegistryEntry<T>(EchoRegistryKey<T> key, T value) {
    public EchoRegistryEntry {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
    }
}

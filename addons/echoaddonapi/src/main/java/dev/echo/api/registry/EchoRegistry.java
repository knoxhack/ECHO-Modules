package dev.echo.api.registry;

import java.util.Optional;

public interface EchoRegistry<T> {
    Optional<T> find(EchoRegistryKey<T> key);

    EchoRegistryHandle<T> register(EchoRegistryEntry<T> entry);
}

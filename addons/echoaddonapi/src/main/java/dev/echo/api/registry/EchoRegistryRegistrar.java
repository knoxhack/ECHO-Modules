package dev.echo.api.registry;

public interface EchoRegistryRegistrar {
    <T> EchoRegistryHandle<T> register(String registryName, EchoRegistryEntry<T> entry);
}

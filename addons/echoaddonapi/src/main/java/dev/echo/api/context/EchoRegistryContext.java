package dev.echo.api.context;

import dev.echo.api.registry.EchoRegistryRegistrar;

public interface EchoRegistryContext extends EchoContext {
    EchoRegistryRegistrar registrar();
}

package dev.echo.api.context;

public interface EchoCommonContext extends EchoContext {
    EchoRegistryContext registries();

    EchoServiceContext services();
}

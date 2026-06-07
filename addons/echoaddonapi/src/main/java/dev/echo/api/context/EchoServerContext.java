package dev.echo.api.context;

public interface EchoServerContext extends EchoCommonContext {
    int activePlayerCount();
}

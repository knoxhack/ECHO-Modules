package dev.echo.api.context;

public interface EchoWorldContext extends EchoContext {
    String worldId();

    long gameTime();
}

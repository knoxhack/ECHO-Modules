package dev.echo.api.context;

public interface EchoPlayerContext extends EchoContext {
    String playerId();

    EchoWorldContext world();
}

package dev.echo.api.command;

@FunctionalInterface
public interface EchoCommand {
    EchoCommandResult execute(EchoCommandContext context);
}

package dev.echo.api.command;

public record EchoCommandResult(boolean successful, String message) {
    public static EchoCommandResult success(String message) {
        return new EchoCommandResult(true, message);
    }

    public static EchoCommandResult failure(String message) {
        return new EchoCommandResult(false, message);
    }
}

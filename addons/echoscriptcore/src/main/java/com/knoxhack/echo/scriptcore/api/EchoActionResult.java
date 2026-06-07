package com.knoxhack.echo.scriptcore.api;

public record EchoActionResult(boolean supported, boolean success, String message) {
    public static EchoActionResult unsupported(String message) {
        return new EchoActionResult(false, false, message);
    }

    public static EchoActionResult success(String message) {
        return new EchoActionResult(true, true, message);
    }

    public static EchoActionResult failure(String message) {
        return new EchoActionResult(true, false, message);
    }
}

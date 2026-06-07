package com.knoxhack.echo.scriptcore.api;

public record EchoConditionResult(boolean supported, boolean matched, String message) {
    public static EchoConditionResult unsupported(String message) {
        return new EchoConditionResult(false, false, message);
    }

    public static EchoConditionResult matched(String message) {
        return new EchoConditionResult(true, true, message);
    }

    public static EchoConditionResult unmatched(String message) {
        return new EchoConditionResult(true, false, message);
    }
}

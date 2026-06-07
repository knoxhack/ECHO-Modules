package com.knoxhack.echo.inputcore;

public record EchoInputContextId(String value) {
    public EchoInputContextId {
        value = InputContractGuards.normalizedId(value, "input context id");
    }

    public static EchoInputContextId of(String value) {
        return new EchoInputContextId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

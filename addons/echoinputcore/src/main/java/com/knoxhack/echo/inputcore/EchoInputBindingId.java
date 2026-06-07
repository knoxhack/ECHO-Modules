package com.knoxhack.echo.inputcore;

public record EchoInputBindingId(String value) {
    public EchoInputBindingId {
        value = InputContractGuards.normalizedId(value, "input binding id");
    }

    public static EchoInputBindingId of(String value) {
        return new EchoInputBindingId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

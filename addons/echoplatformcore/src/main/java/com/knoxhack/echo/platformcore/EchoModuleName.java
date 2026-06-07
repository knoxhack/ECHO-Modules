package com.knoxhack.echo.platformcore;

public record EchoModuleName(String value) {
    public EchoModuleName {
        value = EchoContractGuards.requireText(value, "module name");
    }

    public static EchoModuleName of(String value) {
        return new EchoModuleName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

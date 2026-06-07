package com.knoxhack.echo.platformcore;

public record EchoModuleVersion(String value) {
    public EchoModuleVersion {
        value = EchoContractGuards.requireText(value, "module version");
    }

    public static EchoModuleVersion of(String value) {
        return new EchoModuleVersion(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

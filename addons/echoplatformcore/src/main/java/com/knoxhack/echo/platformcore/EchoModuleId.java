package com.knoxhack.echo.platformcore;

import java.util.Locale;

public record EchoModuleId(String value) {
    public EchoModuleId {
        value = EchoContractGuards.requireText(value, "module id").toLowerCase(Locale.ROOT);
    }

    public static EchoModuleId of(String value) {
        return new EchoModuleId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

package com.knoxhack.echo.platformcore;

import java.util.Locale;

public record EchoPackId(String value) {
    public EchoPackId {
        value = EchoContractGuards.requireText(value, "pack id").toLowerCase(Locale.ROOT);
    }

    public static EchoPackId of(String value) {
        return new EchoPackId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

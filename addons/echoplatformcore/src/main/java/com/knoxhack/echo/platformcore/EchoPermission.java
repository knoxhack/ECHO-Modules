package com.knoxhack.echo.platformcore;

import java.util.Locale;

public record EchoPermission(String value) {
    public EchoPermission {
        value = EchoContractGuards.requireText(value, "permission").toLowerCase(Locale.ROOT);
    }

    public static EchoPermission of(String value) {
        return new EchoPermission(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

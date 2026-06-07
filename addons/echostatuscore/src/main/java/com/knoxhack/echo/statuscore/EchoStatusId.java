package com.knoxhack.echo.statuscore;

import java.util.Locale;

public record EchoStatusId(String value) {
    public EchoStatusId {
        value = StatusContractGuards.requireText(value, "status id").toLowerCase(Locale.ROOT);
    }

    public static EchoStatusId of(String value) {
        return new EchoStatusId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

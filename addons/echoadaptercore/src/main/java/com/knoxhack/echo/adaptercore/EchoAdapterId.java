package com.knoxhack.echo.adaptercore;

import java.util.Locale;

public record EchoAdapterId(String value) {
    public EchoAdapterId {
        value = AdapterContractGuards.requireText(value, "adapter id").toLowerCase(Locale.ROOT);
    }

    public static EchoAdapterId of(String value) {
        return new EchoAdapterId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

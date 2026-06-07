package com.knoxhack.echo.packcore;

import java.util.Locale;

public record EchoPackVariantId(String value) {
    public EchoPackVariantId {
        value = PackContractGuards.requireText(value, "pack variant id").toLowerCase(Locale.ROOT);
    }

    public static EchoPackVariantId of(String value) {
        return new EchoPackVariantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

package com.knoxhack.echo.platformcore;

import java.util.Locale;

public record EchoFeatureId(String value) {
    public EchoFeatureId {
        value = EchoContractGuards.requireText(value, "feature id").toLowerCase(Locale.ROOT);
    }

    public static EchoFeatureId of(String value) {
        return new EchoFeatureId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

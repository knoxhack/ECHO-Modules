package com.knoxhack.echo.platformcore;

import java.util.Locale;

public record EchoCapabilityId(String value) {
    public EchoCapabilityId {
        value = EchoContractGuards.requireText(value, "capability id").toLowerCase(Locale.ROOT);
    }

    public static EchoCapabilityId of(String value) {
        return new EchoCapabilityId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

package com.knoxhack.echo.packcore;

import java.util.Locale;

public record EchoPackChannelId(String value) {
    public EchoPackChannelId {
        value = PackContractGuards.requireText(value, "pack channel id").toLowerCase(Locale.ROOT);
    }

    public static EchoPackChannelId of(String value) {
        return new EchoPackChannelId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

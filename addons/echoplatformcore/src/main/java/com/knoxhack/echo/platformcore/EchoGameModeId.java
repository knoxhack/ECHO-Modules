package com.knoxhack.echo.platformcore;

import java.util.Locale;

public record EchoGameModeId(String value) {
    public EchoGameModeId {
        value = EchoContractGuards.requireText(value, "game mode id").toLowerCase(Locale.ROOT);
    }

    public static EchoGameModeId of(String value) {
        return new EchoGameModeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

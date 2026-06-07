package com.knoxhack.echo.bridgecore;

import java.util.Locale;

public record EchoBridgeSessionId(String value) {
    public EchoBridgeSessionId {
        value = BridgeContractGuards.requireText(value, "bridge session id").toLowerCase(Locale.ROOT);
    }

    public static EchoBridgeSessionId of(String value) {
        return new EchoBridgeSessionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

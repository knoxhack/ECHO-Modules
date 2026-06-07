package com.knoxhack.echo.bridgecore;

import java.util.Locale;

public record EchoBridgeJobId(String value) {
    public EchoBridgeJobId {
        value = BridgeContractGuards.requireText(value, "bridge job id").toLowerCase(Locale.ROOT);
    }

    public static EchoBridgeJobId of(String value) {
        return new EchoBridgeJobId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

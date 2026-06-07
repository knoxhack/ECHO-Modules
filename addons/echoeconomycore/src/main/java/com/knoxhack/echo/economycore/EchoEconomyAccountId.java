package com.knoxhack.echo.economycore;

import java.util.Locale;

public record EchoEconomyAccountId(String value) {
    public EchoEconomyAccountId {
        value = EconomyContractGuards.requireText(value, "economy account id").toLowerCase(Locale.ROOT);
    }

    public static EchoEconomyAccountId of(String value) {
        return new EchoEconomyAccountId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

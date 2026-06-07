package com.knoxhack.echo.economycore;

import java.util.Locale;

public record EchoCurrencyId(String value) {
    public EchoCurrencyId {
        value = EconomyContractGuards.requireText(value, "currency id").toLowerCase(Locale.ROOT);
    }

    public static EchoCurrencyId of(String value) {
        return new EchoCurrencyId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

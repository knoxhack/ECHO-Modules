package com.knoxhack.echo.economycore;

import java.util.Locale;

public record EchoShopId(String value) {
    public EchoShopId {
        value = EconomyContractGuards.requireText(value, "shop id").toLowerCase(Locale.ROOT);
    }

    public static EchoShopId of(String value) {
        return new EchoShopId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

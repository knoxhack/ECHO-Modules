package com.knoxhack.echo.economycore;

import java.util.Map;
import java.util.Objects;

public record EchoCurrencyAmount(
        EchoCurrencyId currencyId,
        long amount,
        Map<String, String> attributes
) {
    public EchoCurrencyAmount {
        Objects.requireNonNull(currencyId, "currencyId");
        amount = EconomyContractGuards.nonNegative(amount, "currency amount");
        attributes = EconomyContractGuards.immutableMap(attributes);
    }
}

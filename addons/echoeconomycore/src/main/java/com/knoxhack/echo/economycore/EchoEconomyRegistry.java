package com.knoxhack.echo.economycore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoEconomyRegistry(
        Map<EchoCurrencyId, EchoCurrency> currencies,
        Map<EchoShopId, EchoFactionShop> factionShops,
        List<EchoMissionRewardValue> missionRewards,
        List<EchoTradeValidationResult> validationResults,
        List<EchoDiagnostic> diagnostics
) {
    public EchoEconomyRegistry {
        currencies = EconomyContractGuards.immutableMap(currencies);
        factionShops = EconomyContractGuards.immutableMap(factionShops);
        missionRewards = EconomyContractGuards.immutableList(missionRewards);
        validationResults = EconomyContractGuards.immutableList(validationResults);
        diagnostics = EconomyContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || currencies.values().stream().anyMatch(EchoCurrency::blocking)
                || factionShops.values().stream().anyMatch(EchoFactionShop::blocking)
                || validationResults.stream().anyMatch(EchoTradeValidationResult::blocking);
    }
}

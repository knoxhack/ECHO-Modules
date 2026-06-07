package com.knoxhack.echo.economycore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoEconomyState(
        EchoEconomyAccountId accountId,
        String subjectId,
        Map<EchoCurrencyId, Long> balances,
        boolean serverAuthoritative,
        long revision,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoEconomyState {
        Objects.requireNonNull(accountId, "accountId");
        subjectId = EconomyContractGuards.requireText(subjectId, "economy subject id");
        balances = EconomyContractGuards.immutableMap(balances);
        revision = EconomyContractGuards.nonNegative(revision, "economy state revision");
        diagnostics = EconomyContractGuards.immutableList(diagnostics);
        attributes = EconomyContractGuards.immutableMap(attributes);
    }
}

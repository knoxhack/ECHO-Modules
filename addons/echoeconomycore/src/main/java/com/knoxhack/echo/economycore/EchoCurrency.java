package com.knoxhack.echo.economycore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoCurrency(
        EchoCurrencyId id,
        String displayName,
        EchoCurrencyKind kind,
        EchoModuleId owningModule,
        EchoContentReference itemBacking,
        EchoContentReference iconReference,
        boolean serverAuthoritative,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoCurrency {
        Objects.requireNonNull(id, "id");
        displayName = EconomyContractGuards.requireText(displayName, "currency display name");
        kind = kind == null ? EchoCurrencyKind.UNKNOWN : kind;
        diagnostics = EconomyContractGuards.immutableList(diagnostics);
        attributes = EconomyContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}

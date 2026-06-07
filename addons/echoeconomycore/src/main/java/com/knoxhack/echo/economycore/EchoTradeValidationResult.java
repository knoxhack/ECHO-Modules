package com.knoxhack.echo.economycore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoTradeValidationResult(
        String validationId,
        EchoTradeValidationStatus status,
        EchoShopId shopId,
        String tradeId,
        List<EchoContentReference> checkedCosts,
        List<EchoContentReference> checkedOutputs,
        List<EchoDiagnostic> diagnostics,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoTradeValidationResult {
        validationId = EconomyContractGuards.requireText(validationId, "trade validation id");
        status = status == null ? EchoTradeValidationStatus.UNKNOWN : status;
        checkedCosts = EconomyContractGuards.immutableList(checkedCosts);
        checkedOutputs = EconomyContractGuards.immutableList(checkedOutputs);
        diagnostics = EconomyContractGuards.immutableList(diagnostics);
        playerSummary = EconomyContractGuards.optionalText(playerSummary);
        developerDetails = EconomyContractGuards.optionalText(developerDetails);
        attributes = EconomyContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return status.blocking() || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}

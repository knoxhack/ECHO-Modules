package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Objects;

public record EchoBridgeSafeActionResult(
        String requestId,
        EchoBridgeJobId jobId,
        EchoBridgeSafeActionStatus status,
        boolean confirmed,
        boolean actionCompleted,
        String summary,
        String developerDetails,
        List<EchoDiagnostic> diagnostics,
        long completedAtEpochMillis
) {
    public EchoBridgeSafeActionResult {
        requestId = BridgeContractGuards.requireText(requestId, "safe action result request id");
        status = Objects.requireNonNullElse(status, EchoBridgeSafeActionStatus.PENDING_CONFIRMATION);
        summary = BridgeContractGuards.optionalText(summary);
        developerDetails = BridgeContractGuards.optionalText(developerDetails);
        diagnostics = BridgeContractGuards.immutableList(diagnostics);
        completedAtEpochMillis = BridgeContractGuards.nonNegativeLong(completedAtEpochMillis, "safe action result timestamp");
    }

    public boolean denied() {
        return status == EchoBridgeSafeActionStatus.REJECTED || status == EchoBridgeSafeActionStatus.BLOCKED;
    }
}

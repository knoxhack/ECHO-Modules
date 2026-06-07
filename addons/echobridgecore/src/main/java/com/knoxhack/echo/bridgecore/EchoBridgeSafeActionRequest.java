package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.agentcore.EchoAiCommandRisk;
import com.knoxhack.echo.agentcore.EchoAiTaskId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Objects;

public record EchoBridgeSafeActionRequest(
        String requestId,
        EchoBridgeSessionId sessionId,
        EchoBridgeJobId jobId,
        EchoAiTaskId taskId,
        EchoBridgeCommand command,
        EchoAiCommandRisk risk,
        boolean requiresConfirmation,
        String requestedBy,
        String playerSummary,
        String developerDetails,
        List<EchoDiagnostic> relatedDiagnostics,
        long createdAtEpochMillis,
        long expiresAtEpochMillis
) {
    public EchoBridgeSafeActionRequest {
        requestId = BridgeContractGuards.requireText(requestId, "safe action request id");
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        command = Objects.requireNonNull(command, "command");
        risk = risk == null ? command.risk() : risk;
        requiresConfirmation = requiresConfirmation || command.requiresConfirmation() || risk.requiresConfirmation();
        requestedBy = BridgeContractGuards.optionalText(requestedBy);
        playerSummary = BridgeContractGuards.optionalText(playerSummary);
        developerDetails = BridgeContractGuards.optionalText(developerDetails);
        relatedDiagnostics = BridgeContractGuards.immutableList(relatedDiagnostics);
        createdAtEpochMillis = BridgeContractGuards.nonNegativeLong(createdAtEpochMillis, "safe action request timestamp");
        expiresAtEpochMillis = BridgeContractGuards.nonNegativeLong(expiresAtEpochMillis, "safe action expiry timestamp");
    }

    public boolean expiredAt(long epochMillis) {
        return expiresAtEpochMillis > 0L && epochMillis >= expiresAtEpochMillis;
    }
}

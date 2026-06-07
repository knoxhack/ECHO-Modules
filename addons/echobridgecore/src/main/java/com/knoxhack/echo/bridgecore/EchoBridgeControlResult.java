package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.agentcore.EchoAiNextPhasePrompt;
import com.knoxhack.echo.agentcore.EchoAiRunReport;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoBridgeControlResult(
        String requestId,
        EchoBridgeControlAction action,
        EchoBridgeControlStatus status,
        EchoBridgeSession session,
        EchoBridgeJob job,
        List<EchoBridgeEvent> events,
        List<EchoBridgeLogChunk> logChunks,
        List<EchoDiagnostic> diagnostics,
        EchoAiRunReport runReport,
        EchoAiNextPhasePrompt nextPhasePrompt,
        EchoBridgeRunReportReference runReportReference,
        String summary,
        String failureReason,
        boolean localOnly,
        boolean redacted,
        long createdAtEpochMillis,
        long updatedAtEpochMillis,
        Map<String, String> attributes
) {
    public EchoBridgeControlResult {
        requestId = BridgeContractGuards.requireText(requestId, "bridge control result request id");
        action = Objects.requireNonNull(action, "action");
        status = status == null ? EchoBridgeControlStatus.ACCEPTED : status;
        events = BridgeContractGuards.immutableList(events);
        logChunks = BridgeContractGuards.immutableList(logChunks);
        diagnostics = BridgeContractGuards.immutableList(diagnostics);
        summary = BridgeContractGuards.optionalText(summary);
        failureReason = BridgeContractGuards.optionalText(failureReason);
        localOnly = true;
        redacted = true;
        createdAtEpochMillis = BridgeContractGuards.nonNegativeLong(createdAtEpochMillis, "bridge control result created timestamp");
        updatedAtEpochMillis = BridgeContractGuards.nonNegativeLong(updatedAtEpochMillis, "bridge control result updated timestamp");
        attributes = BridgeContractGuards.immutableMap(attributes);
    }

    public boolean requiresAttention() {
        return status.requiresAttention() || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}

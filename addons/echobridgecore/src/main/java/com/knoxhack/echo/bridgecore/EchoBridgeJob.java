package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.agentcore.EchoAiTask;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Objects;

public record EchoBridgeJob(
        EchoBridgeJobId id,
        EchoBridgeSessionId sessionId,
        EchoBridgeJobStatus status,
        EchoAiTask task,
        EchoBridgePromptSubmission promptSubmission,
        List<EchoBridgeCommand> commands,
        List<EchoBridgeSafeActionRequest> safeActionRequests,
        List<EchoBridgeLogChunk> recentLogChunks,
        List<EchoDiagnostic> diagnostics,
        EchoBridgeStreamCursor outputCursor,
        EchoBridgeStreamCursor errorCursor,
        String summary,
        long createdAtEpochMillis,
        long updatedAtEpochMillis
) {
    public EchoBridgeJob {
        id = Objects.requireNonNull(id, "id");
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        status = status == null ? EchoBridgeJobStatus.QUEUED : status;
        commands = BridgeContractGuards.immutableList(commands);
        safeActionRequests = BridgeContractGuards.immutableList(safeActionRequests);
        recentLogChunks = BridgeContractGuards.immutableList(recentLogChunks);
        diagnostics = BridgeContractGuards.immutableList(diagnostics);
        outputCursor = outputCursor == null ? EchoBridgeStreamCursor.beginning(id + ".stdout") : outputCursor;
        errorCursor = errorCursor == null ? EchoBridgeStreamCursor.beginning(id + ".stderr") : errorCursor;
        summary = BridgeContractGuards.optionalText(summary);
        createdAtEpochMillis = BridgeContractGuards.nonNegativeLong(createdAtEpochMillis, "bridge job created timestamp");
        updatedAtEpochMillis = BridgeContractGuards.nonNegativeLong(updatedAtEpochMillis, "bridge job updated timestamp");
        if (!status.terminal() && safeActionRequests.stream().anyMatch(EchoBridgeSafeActionRequest::requiresConfirmation)) {
            status = EchoBridgeJobStatus.NEEDS_CONFIRMATION;
        }
    }

    public boolean supportsStreamingState() {
        return status.active() || !recentLogChunks.isEmpty();
    }

    public boolean requiresHumanAttention() {
        return status.requiresAttention()
                || safeActionRequests.stream().anyMatch(EchoBridgeSafeActionRequest::requiresConfirmation)
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}

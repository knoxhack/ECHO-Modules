package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.agentcore.EchoAiNextPhasePrompt;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Objects;

public record EchoBridgeEvent(
        String eventId,
        EchoBridgeEventKind kind,
        EchoBridgeSessionId sessionId,
        EchoBridgeJobId jobId,
        EchoBridgeStreamCursor cursor,
        String summary,
        String payloadJson,
        EchoBridgeLogChunk logChunk,
        EchoBridgeSafeActionRequest safeActionRequest,
        EchoBridgeSafeActionResult safeActionResult,
        List<EchoDiagnostic> diagnostics,
        EchoAiNextPhasePrompt nextPhasePrompt,
        long createdAtEpochMillis
) {
    public EchoBridgeEvent {
        eventId = BridgeContractGuards.requireText(eventId, "bridge event id");
        kind = Objects.requireNonNullElse(kind, EchoBridgeEventKind.LOG_CHUNK);
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        cursor = cursor == null ? EchoBridgeStreamCursor.beginning(sessionId + ".events") : cursor;
        summary = BridgeContractGuards.optionalText(summary);
        payloadJson = BridgeContractGuards.optionalText(payloadJson);
        diagnostics = BridgeContractGuards.immutableList(diagnostics);
        createdAtEpochMillis = BridgeContractGuards.nonNegativeLong(createdAtEpochMillis, "bridge event timestamp");
    }

    public boolean carriesConfirmationRequest() {
        return kind == EchoBridgeEventKind.CONFIRMATION_REQUIRED
                || (safeActionRequest != null && safeActionRequest.requiresConfirmation());
    }
}

package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.agentcore.EchoAiTaskId;

import java.util.Map;
import java.util.Objects;

public record EchoBridgeControlRequest(
        String requestId,
        EchoBridgeControlAction action,
        EchoBridgeSessionId sessionId,
        EchoBridgeJobId jobId,
        EchoAiTaskId taskId,
        EchoBridgePromptSubmission promptSubmission,
        EchoBridgeSafeActionResult safeActionResult,
        String requestedBy,
        boolean requiresConfirmation,
        boolean localOnly,
        long createdAtEpochMillis,
        Map<String, String> attributes
) {
    public EchoBridgeControlRequest {
        requestId = BridgeContractGuards.requireText(requestId, "bridge control request id");
        action = Objects.requireNonNull(action, "action");
        requestedBy = BridgeContractGuards.optionalText(requestedBy);
        requiresConfirmation = requiresConfirmation
                || action == EchoBridgeControlAction.START_CODEX_JOB
                || action == EchoBridgeControlAction.RESUME_AFTER_CONFIRMATION
                || (safeActionResult != null && safeActionResult.confirmed());
        localOnly = true;
        createdAtEpochMillis = BridgeContractGuards.nonNegativeLong(createdAtEpochMillis, "bridge control request timestamp");
        attributes = BridgeContractGuards.immutableMap(attributes);
    }
}

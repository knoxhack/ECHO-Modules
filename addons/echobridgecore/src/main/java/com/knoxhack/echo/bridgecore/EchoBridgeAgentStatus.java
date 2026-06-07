package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.agentcore.EchoAiAgentLane;
import com.knoxhack.echo.agentcore.EchoAiTaskId;
import com.knoxhack.echo.healthcore.EchoHealthStatus;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Objects;

public record EchoBridgeAgentStatus(
        EchoBridgeSessionId sessionId,
        EchoHealthStatus healthStatus,
        EchoAiAgentLane activeLane,
        EchoAiTaskId activeTaskId,
        EchoBridgeJobStatus currentJobStatus,
        int queuedJobs,
        int activeJobs,
        boolean connected,
        boolean waitingForConfirmation,
        String summary,
        List<EchoDiagnostic> diagnostics,
        long observedAtEpochMillis
) {
    public EchoBridgeAgentStatus {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        healthStatus = healthStatus == null ? EchoHealthStatus.UNKNOWN : healthStatus;
        currentJobStatus = currentJobStatus == null ? EchoBridgeJobStatus.QUEUED : currentJobStatus;
        queuedJobs = BridgeContractGuards.nonNegative(queuedJobs, "queued jobs");
        activeJobs = BridgeContractGuards.nonNegative(activeJobs, "active jobs");
        waitingForConfirmation = waitingForConfirmation || currentJobStatus == EchoBridgeJobStatus.NEEDS_CONFIRMATION;
        summary = BridgeContractGuards.optionalText(summary);
        diagnostics = BridgeContractGuards.immutableList(diagnostics);
        observedAtEpochMillis = BridgeContractGuards.nonNegativeLong(observedAtEpochMillis, "agent status timestamp");
    }

    public boolean requiresAttention() {
        return waitingForConfirmation || healthStatus.requiresAttention() || currentJobStatus.requiresAttention();
    }
}

package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoBridgeSession(
        EchoBridgeSessionId id,
        String displayName,
        EchoRuntimeSide side,
        EchoBridgeCapabilities capabilities,
        EchoBridgeAgentStatus agentStatus,
        EchoBridgeCodexSession codexSession,
        List<EchoBridgeJob> jobs,
        List<EchoBridgeStreamCursor> cursors,
        List<EchoDiagnostic> diagnostics,
        long createdAtEpochMillis,
        long updatedAtEpochMillis,
        Map<String, String> attributes
) {
    public EchoBridgeSession {
        id = Objects.requireNonNull(id, "id");
        displayName = BridgeContractGuards.optionalText(displayName);
        side = side == null ? EchoRuntimeSide.DEV : side;
        capabilities = capabilities == null ? EchoBridgeCapabilities.contractOnly() : capabilities;
        jobs = BridgeContractGuards.immutableList(jobs);
        cursors = BridgeContractGuards.immutableList(cursors);
        diagnostics = BridgeContractGuards.immutableList(diagnostics);
        createdAtEpochMillis = BridgeContractGuards.nonNegativeLong(createdAtEpochMillis, "bridge session created timestamp");
        updatedAtEpochMillis = BridgeContractGuards.nonNegativeLong(updatedAtEpochMillis, "bridge session updated timestamp");
        attributes = BridgeContractGuards.immutableMap(attributes);
    }

    public List<EchoBridgeJob> activeJobs() {
        return jobs.stream().filter(job -> job.status().active()).toList();
    }

    public boolean requiresAttention() {
        return (agentStatus != null && agentStatus.requiresAttention())
                || jobs.stream().anyMatch(EchoBridgeJob::requiresHumanAttention)
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}

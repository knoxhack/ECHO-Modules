package com.knoxhack.echo.bridgecore;

import com.knoxhack.echo.healthcore.EchoHealthStatus;

import java.util.Map;
import java.util.Objects;

public record EchoBridgeHeartbeat(
        EchoBridgeSessionId sessionId,
        long sequence,
        long sentAtEpochMillis,
        EchoBridgeAgentStatus agentStatus,
        EchoHealthStatus healthStatus,
        EchoBridgeStreamCursor latestCursor,
        boolean localOnly,
        String summary,
        Map<String, String> attributes
) {
    public EchoBridgeHeartbeat {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        sequence = BridgeContractGuards.nonNegativeLong(sequence, "heartbeat sequence");
        sentAtEpochMillis = BridgeContractGuards.nonNegativeLong(sentAtEpochMillis, "heartbeat timestamp");
        healthStatus = healthStatus == null
                ? (agentStatus == null ? EchoHealthStatus.UNKNOWN : agentStatus.healthStatus())
                : healthStatus;
        latestCursor = latestCursor == null ? EchoBridgeStreamCursor.beginning(sessionId + ".events") : latestCursor;
        localOnly = true;
        summary = BridgeContractGuards.optionalText(summary);
        attributes = BridgeContractGuards.immutableMap(attributes);
    }
}

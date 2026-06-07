package com.knoxhack.echo.bridgecore;

import java.util.Map;
import java.util.Objects;

public record EchoBridgeCodexSession(
        EchoBridgeSessionId sessionId,
        String codexSessionId,
        String workspaceRoot,
        String model,
        String sandboxPolicy,
        String approvalPolicy,
        EchoBridgeTransportHint transportHint,
        EchoBridgeStreamCursor outputCursor,
        boolean connected,
        boolean acceptsRepoEdits,
        long connectedAtEpochMillis,
        long lastSeenAtEpochMillis,
        Map<String, String> attributes
) {
    public EchoBridgeCodexSession {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        codexSessionId = BridgeContractGuards.optionalText(codexSessionId);
        workspaceRoot = BridgeContractGuards.optionalText(workspaceRoot);
        model = BridgeContractGuards.optionalText(model);
        sandboxPolicy = BridgeContractGuards.optionalText(sandboxPolicy);
        approvalPolicy = BridgeContractGuards.optionalText(approvalPolicy);
        transportHint = transportHint == null ? EchoBridgeTransportHint.loopback("127.0.0.1") : transportHint;
        outputCursor = outputCursor == null ? EchoBridgeStreamCursor.beginning(sessionId + ".codex") : outputCursor;
        connectedAtEpochMillis = BridgeContractGuards.nonNegativeLong(connectedAtEpochMillis, "Codex session connected timestamp");
        lastSeenAtEpochMillis = BridgeContractGuards.nonNegativeLong(lastSeenAtEpochMillis, "Codex session last-seen timestamp");
        attributes = BridgeContractGuards.immutableMap(attributes);
    }
}

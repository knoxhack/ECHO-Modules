package com.knoxhack.echo.bridgecore;

import java.util.Map;

public record EchoBridgeCodexExecutorStatus(
        String executorId,
        EchoBridgeControlStatus status,
        String commandLabel,
        String version,
        boolean configured,
        boolean available,
        boolean canEditWorkspace,
        boolean localOnly,
        String blockedReason,
        Map<String, String> attributes
) {
    public EchoBridgeCodexExecutorStatus {
        executorId = BridgeContractGuards.requireText(executorId, "Codex executor id");
        status = status == null ? EchoBridgeControlStatus.NOT_CONFIGURED : status;
        commandLabel = BridgeContractGuards.optionalText(commandLabel);
        version = BridgeContractGuards.optionalText(version);
        configured = configured && status != EchoBridgeControlStatus.NOT_CONFIGURED;
        available = available && configured && status != EchoBridgeControlStatus.UNAVAILABLE;
        localOnly = true;
        blockedReason = BridgeContractGuards.optionalText(blockedReason);
        attributes = BridgeContractGuards.immutableMap(attributes);
    }

    public static EchoBridgeCodexExecutorStatus notConfigured(String reason) {
        return new EchoBridgeCodexExecutorStatus(
                "codex.local",
                EchoBridgeControlStatus.NOT_CONFIGURED,
                "Codex CLI",
                "",
                false,
                false,
                false,
                true,
                reason,
                Map.of()
        );
    }
}

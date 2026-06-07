package com.knoxhack.echo.bridgecore;

import java.util.Map;

public record EchoBridgeRunReportReference(
        String reportId,
        EchoBridgeSessionId sessionId,
        EchoBridgeJobId jobId,
        String reportPath,
        String nextPhasePromptPath,
        String aiTaskReportPath,
        boolean localOnly,
        boolean redacted,
        long savedAtEpochMillis,
        Map<String, String> attributes
) {
    public EchoBridgeRunReportReference {
        reportId = BridgeContractGuards.requireText(reportId, "bridge run report id");
        reportPath = BridgeContractGuards.optionalText(reportPath);
        nextPhasePromptPath = BridgeContractGuards.optionalText(nextPhasePromptPath);
        aiTaskReportPath = BridgeContractGuards.optionalText(aiTaskReportPath);
        localOnly = true;
        redacted = true;
        savedAtEpochMillis = BridgeContractGuards.nonNegativeLong(savedAtEpochMillis, "bridge run report saved timestamp");
        attributes = BridgeContractGuards.immutableMap(attributes);
    }
}

package com.knoxhack.echo.reportcore;

import java.util.List;
import java.util.Map;

public record EchoReportIssue(
        String code,
        EchoReportIssueSeverity severity,
        String summary,
        List<String> likelyFiles,
        String suggestedFix,
        Map<String, String> attributes
) {
    public EchoReportIssue {
        code = ReportContractGuards.normalizedId(code, "report issue code");
        severity = severity == null ? EchoReportIssueSeverity.UNKNOWN : severity;
        summary = ReportContractGuards.requireText(summary, "report issue summary");
        likelyFiles = ReportContractGuards.immutableList(likelyFiles);
        suggestedFix = ReportContractGuards.optionalText(suggestedFix);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return severity.blocking();
    }
}

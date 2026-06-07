package com.knoxhack.echo.reportcore;

import java.util.Map;

public record EchoReportSummary(
        int warnings,
        int errors,
        int notices,
        int fatals,
        Map<String, String> attributes
) {
    public EchoReportSummary {
        warnings = Math.max(0, warnings);
        errors = Math.max(0, errors);
        notices = Math.max(0, notices);
        fatals = Math.max(0, fatals);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public static EchoReportSummary empty() {
        return new EchoReportSummary(0, 0, 0, 0, Map.of());
    }
}

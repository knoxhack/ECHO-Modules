package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.healthcore.EchoRuntimeHealthReport;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoHealthReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        EchoRuntimeHealthReport healthReport,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoHealthReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.HEALTH) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean localOnly() {
        return healthReport == null || healthReport.localOnly();
    }
}

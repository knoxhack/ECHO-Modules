package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;
import com.knoxhack.echo.validationcore.EchoDiagnosticReport;

import java.util.List;
import java.util.Map;

public record EchoDiagnosticsReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        EchoDiagnosticReport diagnosticReport,
        List<EchoDiagnostic> extraDiagnostics,
        Map<String, String> attributes
) {
    public EchoDiagnosticsReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.DIAGNOSTICS) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        extraDiagnostics = ReportContractGuards.immutableList(extraDiagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnosticReport != null && diagnosticReport.hasBlockingDiagnostics()
                || extraDiagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}

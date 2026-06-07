package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.packcore.EchoPackProfile;
import com.knoxhack.echo.packcore.EchoPackReadiness;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoPackReadinessReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        List<EchoPackProfile> packProfiles,
        List<EchoPackReadiness> readiness,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoPackReadinessReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.PACK_READINESS) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        packProfiles = ReportContractGuards.immutableList(packProfiles);
        readiness = ReportContractGuards.immutableList(readiness);
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean launchable() {
        return diagnostics.stream().noneMatch(EchoDiagnostic::blocking)
                && readiness.stream().allMatch(EchoPackReadiness::launchable);
    }
}

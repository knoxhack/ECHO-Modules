package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.adaptercore.EchoCompatibilityMatrix;
import com.knoxhack.echo.healthcore.EchoRuntimeHealthReport;
import com.knoxhack.echo.packcore.EchoPackReadiness;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoLauncherStatusReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        EchoPackId activePack,
        EchoPackReadiness packReadiness,
        EchoRuntimeHealthReport healthReport,
        EchoCompatibilityMatrix compatibilityMatrix,
        boolean launchable,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> statusLabels,
        Map<String, String> attributes
) {
    public EchoLauncherStatusReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.LAUNCHER_STATUS) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        statusLabels = ReportContractGuards.immutableMap(statusLabels);
        attributes = ReportContractGuards.immutableMap(attributes);
        launchable = launchable
                && (packReadiness == null || packReadiness.launchable())
                && diagnostics.stream().noneMatch(EchoDiagnostic::blocking);
    }
}

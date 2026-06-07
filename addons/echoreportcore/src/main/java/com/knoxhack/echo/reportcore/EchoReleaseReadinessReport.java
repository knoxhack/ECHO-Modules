package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.healthcore.EchoRuntimeHealthReport;
import com.knoxhack.echo.modulegraph.EchoModuleGraph;
import com.knoxhack.echo.packcore.EchoPackReadiness;
import com.knoxhack.echo.validationcore.EchoDiagnosticReport;

import java.util.List;
import java.util.Map;

public record EchoReleaseReadinessReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        EchoPackReadiness packReadiness,
        EchoModuleGraph moduleGraph,
        EchoDiagnosticReport diagnostics,
        EchoRuntimeHealthReport healthReport,
        EchoCompatibilityMatrixReport compatibilityMatrix,
        List<EchoReportArtifact> releaseArtifacts,
        EchoReportStatus status,
        Map<String, String> attributes
) {
    public EchoReleaseReadinessReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.RELEASE_READINESS) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        releaseArtifacts = ReportContractGuards.immutableList(releaseArtifacts);
        status = status == null ? deriveStatus(packReadiness, moduleGraph, diagnostics, compatibilityMatrix) : status;
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean releasable() {
        return status == EchoReportStatus.PASS;
    }

    private static EchoReportStatus deriveStatus(
            EchoPackReadiness packReadiness,
            EchoModuleGraph moduleGraph,
            EchoDiagnosticReport diagnostics,
            EchoCompatibilityMatrixReport compatibilityMatrix
    ) {
        if (packReadiness != null && !packReadiness.launchable()) {
            return EchoReportStatus.BLOCKED;
        }
        if (moduleGraph != null && !moduleGraph.valid()) {
            return EchoReportStatus.BLOCKED;
        }
        if (diagnostics != null && diagnostics.hasBlockingDiagnostics()) {
            return EchoReportStatus.BLOCKED;
        }
        if (compatibilityMatrix != null && !compatibilityMatrix.compatible()) {
            return EchoReportStatus.BLOCKED;
        }
        return EchoReportStatus.PASS;
    }
}

package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.adaptercore.EchoCompatibilityMatrix;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoCompatibilityMatrixReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        List<EchoCompatibilityMatrix> matrices,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoCompatibilityMatrixReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.COMPATIBILITY_MATRIX) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        matrices = ReportContractGuards.immutableList(matrices);
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean compatible() {
        return diagnostics.stream().noneMatch(EchoDiagnostic::blocking)
                && matrices.stream().allMatch(matrix -> matrix.issues().isEmpty());
    }
}

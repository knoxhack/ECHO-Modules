package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.modulegraph.EchoModuleGraph;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoModuleGraphReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        EchoModuleGraph moduleGraph,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoModuleGraphReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.MODULE_GRAPH) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean valid() {
        return (moduleGraph == null || moduleGraph.valid())
                && diagnostics.stream().noneMatch(EchoDiagnostic::blocking);
    }
}

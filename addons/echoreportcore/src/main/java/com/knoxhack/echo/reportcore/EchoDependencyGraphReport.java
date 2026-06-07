package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.modulegraph.EchoModuleGraphEdge;
import com.knoxhack.echo.modulegraph.EchoModuleGraphNode;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoDependencyGraphReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        List<EchoModuleGraphNode> nodes,
        List<EchoModuleGraphEdge> edges,
        List<List<String>> cycles,
        List<EchoModuleGraphEdge> missingRequired,
        List<EchoModuleGraphEdge> optionalMissing,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoDependencyGraphReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.DEPENDENCY_GRAPH) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        nodes = ReportContractGuards.immutableList(nodes);
        edges = ReportContractGuards.immutableList(edges);
        cycles = cycles == null ? List.of() : cycles.stream().map(List::copyOf).toList();
        missingRequired = ReportContractGuards.immutableList(missingRequired);
        optionalMissing = ReportContractGuards.immutableList(optionalMissing);
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean valid() {
        return diagnostics.stream().noneMatch(EchoDiagnostic::blocking);
    }
}

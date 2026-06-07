package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.modulegraph.EchoFeatureGraph;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoFeatureGraphReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        EchoFeatureGraph featureGraph,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoFeatureGraphReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.FEATURE_GRAPH) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean hasMissingRequiredProviders() {
        return featureGraph != null && !featureGraph.missingProviderNodes().isEmpty();
    }
}

package com.knoxhack.echo.agentcore;

import com.knoxhack.echo.healthcore.EchoRuntimeHealthReport;
import com.knoxhack.echo.modulegraph.EchoFeatureGraph;
import com.knoxhack.echo.modulegraph.EchoModuleGraph;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
public record EchoAiProjectGraph(
        String id,
        EchoPackId packId,
        EchoModuleGraph moduleGraph,
        EchoFeatureGraph featureGraph,
        EchoRuntimeHealthReport healthReport,
        List<EchoAiModuleSummary> modules,
        List<EchoAiDiagnosticHint> diagnosticHints,
        List<EchoDiagnostic> diagnostics,
        List<String> sourceReferences,
        String summary
) {
    public EchoAiProjectGraph {
        id = AgentContractGuards.requireText(id, "AI project graph id");
        modules = AgentContractGuards.immutableList(modules);
        diagnosticHints = AgentContractGuards.immutableList(diagnosticHints);
        diagnostics = AgentContractGuards.immutableList(diagnostics);
        sourceReferences = AgentContractGuards.immutableList(sourceReferences);
        summary = AgentContractGuards.optionalText(summary);
    }

    public static EchoAiProjectGraph empty(String id) {
        return new EchoAiProjectGraph(id, null, null, null, null, List.of(), List.of(), List.of(), List.of(), "");
    }

    public boolean hasGraphContext() {
        return moduleGraph != null || featureGraph != null || !modules.isEmpty();
    }
}

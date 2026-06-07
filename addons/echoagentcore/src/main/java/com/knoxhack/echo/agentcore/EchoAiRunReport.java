package com.knoxhack.echo.agentcore;

import com.knoxhack.echo.healthcore.EchoRuntimeHealthReport;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;

public record EchoAiRunReport(
        String id,
        EchoAiTaskStatus status,
        String summary,
        EchoAiProjectGraph projectGraph,
        EchoRuntimeHealthReport healthReport,
        List<EchoAiTaskResult> taskResults,
        List<EchoDiagnostic> diagnostics,
        List<String> safeCommandReferences,
        List<String> filesChanged,
        EchoAiNextPhasePrompt nextPhasePrompt,
        long startedAtEpochMillis,
        long finishedAtEpochMillis
) {
    public EchoAiRunReport {
        id = AgentContractGuards.requireText(id, "AI run report id");
        status = status == null ? EchoAiTaskStatus.NEEDS_REVIEW : status;
        summary = AgentContractGuards.optionalText(summary);
        projectGraph = projectGraph == null ? EchoAiProjectGraph.empty(id + ".project_graph") : projectGraph;
        taskResults = AgentContractGuards.immutableList(taskResults);
        diagnostics = AgentContractGuards.immutableList(diagnostics);
        safeCommandReferences = AgentContractGuards.immutableList(safeCommandReferences);
        filesChanged = AgentContractGuards.immutableList(filesChanged);
        startedAtEpochMillis = Math.max(0L, startedAtEpochMillis);
        finishedAtEpochMillis = Math.max(startedAtEpochMillis, finishedAtEpochMillis);
    }

    public boolean requiresHumanReview() {
        return status == EchoAiTaskStatus.NEEDS_REVIEW
                || taskResults.stream().anyMatch(EchoAiTaskResult::requiresHumanReview);
    }
}

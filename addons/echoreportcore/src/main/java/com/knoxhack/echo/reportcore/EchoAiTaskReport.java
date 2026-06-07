package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.agentcore.EchoAiRunReport;
import com.knoxhack.echo.agentcore.EchoAiTask;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoAiTaskReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        List<EchoAiTask> tasks,
        List<EchoAiRunReport> runReports,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoAiTaskReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.AI_TASKS) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        tasks = ReportContractGuards.immutableList(tasks);
        runReports = ReportContractGuards.immutableList(runReports);
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean requiresHumanReview() {
        return tasks.stream().anyMatch(EchoAiTask::requiresHumanReview)
                || runReports.stream().anyMatch(EchoAiRunReport::requiresHumanReview);
    }
}

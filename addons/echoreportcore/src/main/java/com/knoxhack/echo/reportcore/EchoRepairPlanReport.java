package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.packcore.EchoRepairPlan;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoRepairPlanReport(
        EchoReportDescriptor descriptor,
        EchoReportGenerationContext context,
        List<EchoRepairPlan> repairPlans,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoRepairPlanReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.REPAIR_PLAN) : descriptor;
        context = context == null ? EchoReportGenerationContext.empty() : context;
        repairPlans = ReportContractGuards.immutableList(repairPlans);
        diagnostics = ReportContractGuards.immutableList(diagnostics);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public boolean requiresConfirmation() {
        return repairPlans.stream().anyMatch(EchoRepairPlan::requiresConfirmation);
    }
}

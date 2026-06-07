package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoRepairPlanIssue(
        String code,
        String severity,
        String summary,
        String moduleId,
        String packId,
        List<String> relatedDiagnostics,
        List<String> likelyFiles
) {
    public EchoRepairPlanIssue {
        code = PackContractGuards.requireText(code, "repair plan issue code");
        severity = PackContractGuards.optionalText(severity);
        summary = PackContractGuards.optionalText(summary);
        moduleId = PackContractGuards.optionalText(moduleId);
        packId = PackContractGuards.optionalText(packId);
        relatedDiagnostics = PackContractGuards.immutableList(relatedDiagnostics);
        likelyFiles = PackContractGuards.immutableList(likelyFiles);
    }
}

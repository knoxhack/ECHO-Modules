package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoRepairActionResult(
        String actionId,
        boolean executed,
        boolean success,
        String summary,
        List<String> changedPaths,
        List<String> diagnostics
) {
    public EchoRepairActionResult {
        actionId = PackContractGuards.requireText(actionId, "repair action result action id");
        summary = PackContractGuards.optionalText(summary);
        changedPaths = PackContractGuards.immutableList(changedPaths);
        diagnostics = PackContractGuards.immutableList(diagnostics);
    }
}

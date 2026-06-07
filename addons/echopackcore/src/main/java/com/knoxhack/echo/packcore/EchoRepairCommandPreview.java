package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoRepairCommandPreview(
        String id,
        String command,
        String summary,
        boolean mutating,
        boolean requiresConfirmation,
        List<String> relatedDiagnostics
) {
    public EchoRepairCommandPreview {
        id = PackContractGuards.requireText(id, "repair command preview id");
        command = PackContractGuards.optionalText(command);
        summary = PackContractGuards.optionalText(summary);
        relatedDiagnostics = PackContractGuards.immutableList(relatedDiagnostics);
    }
}

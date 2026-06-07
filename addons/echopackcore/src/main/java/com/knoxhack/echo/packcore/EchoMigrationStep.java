package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoMigrationStep(
        String id,
        String label,
        String summary,
        boolean destructive,
        boolean requiresBackup,
        List<String> affectedPaths
) {
    public EchoMigrationStep {
        id = PackContractGuards.requireText(id, "migration step id");
        label = PackContractGuards.requireText(label, "migration step label");
        summary = PackContractGuards.optionalText(summary);
        affectedPaths = PackContractGuards.immutableList(affectedPaths);
    }
}

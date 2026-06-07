package com.knoxhack.echo.packcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;

public record EchoSaveCompatibilityResult(
        EchoSaveCompatibility compatibility,
        EchoSaveMetadata saveMetadata,
        EchoMigrationPlan migrationPlan,
        List<EchoDiagnostic> diagnostics
) {
    public EchoSaveCompatibilityResult {
        compatibility = compatibility == null ? EchoSaveCompatibility.UNKNOWN : compatibility;
        diagnostics = PackContractGuards.immutableList(diagnostics);
    }

    public boolean canOpen() {
        return compatibility != EchoSaveCompatibility.INCOMPATIBLE;
    }
}

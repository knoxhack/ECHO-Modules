package com.knoxhack.echo.packcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;

public record EchoPackReadiness(
        EchoPackState state,
        boolean launchable,
        boolean repairable,
        boolean migrationRequired,
        EchoRepairPlan repairPlan,
        EchoSaveCompatibilityResult saveCompatibility,
        List<EchoDiagnostic> diagnostics
) {
    public EchoPackReadiness {
        state = state == null ? EchoPackState.UNKNOWN : state;
        diagnostics = PackContractGuards.immutableList(diagnostics);
        repairable = repairable || repairPlan != null;
        migrationRequired = migrationRequired
                || saveCompatibility != null && saveCompatibility.compatibility() == EchoSaveCompatibility.MIGRATION_REQUIRED;
    }
}

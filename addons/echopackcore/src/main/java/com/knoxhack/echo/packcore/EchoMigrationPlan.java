package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.List;
import java.util.Objects;

public record EchoMigrationPlan(
        String id,
        EchoPackId packId,
        String fromVersion,
        String toVersion,
        boolean requiresBackup,
        List<EchoMigrationStep> steps,
        List<String> warnings
) {
    public EchoMigrationPlan {
        id = PackContractGuards.requireText(id, "migration plan id");
        Objects.requireNonNull(packId, "packId");
        fromVersion = PackContractGuards.optionalText(fromVersion);
        toVersion = PackContractGuards.optionalText(toVersion);
        steps = PackContractGuards.immutableList(steps);
        warnings = PackContractGuards.immutableList(warnings);
        requiresBackup = requiresBackup || steps.stream().anyMatch(EchoMigrationStep::requiresBackup);
    }
}

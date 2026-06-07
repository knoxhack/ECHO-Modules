package com.knoxhack.echo.spawncore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoSpawnRegistry(
        Map<EchoSpawnProfileId, EchoSpawnProfile> profiles,
        List<EchoDiagnostic> diagnostics
) {
    public EchoSpawnRegistry {
        profiles = SpawnContractGuards.immutableMap(profiles);
        diagnostics = SpawnContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || profiles.values().stream().anyMatch(EchoSpawnProfile::blocking);
    }
}

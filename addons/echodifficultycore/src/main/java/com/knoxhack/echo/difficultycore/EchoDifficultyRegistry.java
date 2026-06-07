package com.knoxhack.echo.difficultycore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoDifficultyRegistry(
        Map<EchoDifficultyProfileId, EchoDifficultyProfile> profiles,
        List<EchoPackVariantDifficultyPolicy> packVariantPolicies,
        List<EchoServerDifficultyPolicy> serverPolicies,
        List<EchoDifficultyTelemetry> telemetry,
        List<EchoDiagnostic> diagnostics
) {
    public EchoDifficultyRegistry {
        profiles = DifficultyContractGuards.immutableMap(profiles);
        packVariantPolicies = DifficultyContractGuards.immutableList(packVariantPolicies);
        serverPolicies = DifficultyContractGuards.immutableList(serverPolicies);
        telemetry = DifficultyContractGuards.immutableList(telemetry);
        diagnostics = DifficultyContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || profiles.values().stream().anyMatch(EchoDifficultyProfile::blocking);
    }
}

package com.knoxhack.echo.encountercore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoEncounterRegistry(
        Map<EchoEncounterId, EchoEncounterDefinition> encounters,
        List<EchoDiagnostic> diagnostics
) {
    public EchoEncounterRegistry {
        encounters = EncounterContractGuards.immutableMap(encounters);
        diagnostics = EncounterContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || encounters.values().stream().anyMatch(EchoEncounterDefinition::blocking);
    }
}

package com.knoxhack.echo.statuscore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoStatusRegistry(
        Map<EchoStatusId, EchoStatusEffectProfile> statuses,
        List<EchoStatusExposure> exposures,
        List<EchoStatusResistanceProfile> resistances,
        List<EchoDiagnostic> diagnostics
) {
    public EchoStatusRegistry {
        statuses = StatusContractGuards.immutableMap(statuses);
        exposures = StatusContractGuards.immutableList(exposures);
        resistances = StatusContractGuards.immutableList(resistances);
        diagnostics = StatusContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || statuses.values().stream().anyMatch(EchoStatusEffectProfile::blocking)
                || exposures.stream().anyMatch(EchoStatusExposure::blocking)
                || resistances.stream().anyMatch(EchoStatusResistanceProfile::blocking);
    }
}

package com.knoxhack.echo.eventcore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;

public record EchoEventValidation(
        EchoWorldEventId eventId,
        boolean canSchedule,
        boolean canStart,
        boolean degraded,
        List<EchoContentReference> missingReferences,
        List<EchoDiagnostic> diagnostics
) {
    public EchoEventValidation {
        missingReferences = EventContractGuards.immutableList(missingReferences);
        diagnostics = EventContractGuards.immutableList(diagnostics);
        degraded = degraded || !missingReferences.isEmpty();
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}

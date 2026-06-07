package com.knoxhack.echo.eventcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoEventRegistry(
        Map<EchoWorldEventId, EchoWorldEventDefinition> events,
        List<EchoEventState> activeStates,
        List<EchoDiagnostic> diagnostics
) {
    public EchoEventRegistry {
        events = EventContractGuards.immutableMap(events);
        activeStates = EventContractGuards.immutableList(activeStates);
        diagnostics = EventContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || events.values().stream().anyMatch(EchoWorldEventDefinition::blocking);
    }
}

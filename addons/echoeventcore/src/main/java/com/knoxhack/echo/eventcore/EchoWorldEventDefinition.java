package com.knoxhack.echo.eventcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoWorldEventDefinition(
        EchoWorldEventId id,
        String displayName,
        EchoWorldEventType type,
        EchoModuleId owningModule,
        EchoEventSchedule schedule,
        List<EchoEventTrigger> triggers,
        EchoContentReference regionReference,
        EchoContentReference weatherEventReference,
        EchoContentReference factionReference,
        EchoContentReference structureReference,
        EchoContentReference encounterReference,
        EchoContentReference rewardReference,
        EchoContentGate gate,
        EchoEventValidation validation,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoWorldEventDefinition {
        Objects.requireNonNull(id, "id");
        displayName = EventContractGuards.requireText(displayName, "world event display name");
        type = type == null ? EchoWorldEventType.UNKNOWN : type;
        triggers = EventContractGuards.immutableList(triggers);
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = EventContractGuards.immutableList(diagnostics);
        attributes = EventContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return gate.blocksWhenMissing()
                || triggers.stream().anyMatch(EchoEventTrigger::blocking)
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || (validation != null && validation.blocking());
    }
}

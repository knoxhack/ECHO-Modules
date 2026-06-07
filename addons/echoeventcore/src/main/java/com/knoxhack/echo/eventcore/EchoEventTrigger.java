package com.knoxhack.echo.eventcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoEventTrigger(
        String triggerId,
        EchoEventTriggerKind kind,
        EchoContentReference sourceReference,
        EchoFeatureId requiredFeature,
        double threshold,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoEventTrigger {
        triggerId = EventContractGuards.id(triggerId, "event trigger id");
        kind = kind == null ? EchoEventTriggerKind.UNKNOWN : kind;
        threshold = EventContractGuards.nonNegative(threshold, "event trigger threshold");
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = EventContractGuards.immutableList(diagnostics);
        attributes = EventContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking) || gate.blocksWhenMissing();
    }
}

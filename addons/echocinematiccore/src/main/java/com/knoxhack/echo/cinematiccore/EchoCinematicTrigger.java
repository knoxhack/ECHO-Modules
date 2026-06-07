package com.knoxhack.echo.cinematiccore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;

public record EchoCinematicTrigger(
        String triggerId,
        EchoCinematicTriggerKind kind,
        List<EchoContentReference> triggerSources,
        boolean repeatable,
        Map<String, String> attributes
) {
    public EchoCinematicTrigger {
        triggerId = CinematicContractGuards.normalizedId(triggerId, "cinematic trigger id");
        kind = kind == null ? EchoCinematicTriggerKind.UNKNOWN : kind;
        triggerSources = CinematicContractGuards.immutableList(triggerSources);
        attributes = CinematicContractGuards.immutableMap(attributes);
    }
}

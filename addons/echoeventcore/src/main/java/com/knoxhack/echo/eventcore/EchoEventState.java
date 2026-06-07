package com.knoxhack.echo.eventcore;

import java.util.Map;

public record EchoEventState(
        EchoWorldEventId eventId,
        EchoEventPhase phase,
        boolean active,
        boolean completed,
        boolean degraded,
        long lastStartedAtEpochMillis,
        long expiresAtEpochMillis,
        String summary,
        Map<String, String> attributes
) {
    public EchoEventState {
        phase = phase == null ? EchoEventPhase.UNKNOWN : phase;
        lastStartedAtEpochMillis = EventContractGuards.nonNegative(lastStartedAtEpochMillis, "last started millis");
        expiresAtEpochMillis = EventContractGuards.nonNegative(expiresAtEpochMillis, "expires millis");
        summary = EventContractGuards.optionalText(summary);
        attributes = EventContractGuards.immutableMap(attributes);
    }
}

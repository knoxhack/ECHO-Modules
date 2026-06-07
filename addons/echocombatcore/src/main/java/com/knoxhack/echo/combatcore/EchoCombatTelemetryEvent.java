package com.knoxhack.echo.combatcore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Map;

public record EchoCombatTelemetryEvent(
        String eventId,
        EchoTelemetryKind kind,
        EchoModuleId sourceModule,
        EchoContentReference attackerReference,
        EchoContentReference targetReference,
        EchoDamageTypeId damageTypeId,
        double amount,
        long gameTime,
        Map<String, String> attributes
) {
    public EchoCombatTelemetryEvent {
        eventId = CombatContractGuards.requireText(eventId, "combat telemetry event id");
        kind = kind == null ? EchoTelemetryKind.UNKNOWN : kind;
        amount = CombatContractGuards.nonNegative(amount, "combat telemetry amount");
        if (gameTime < 0L) {
            throw new IllegalArgumentException("combat telemetry game time must not be negative");
        }
        attributes = CombatContractGuards.immutableMap(attributes);
    }
}

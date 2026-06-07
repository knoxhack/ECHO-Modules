package com.knoxhack.echo.difficultycore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Map;

public record EchoDifficultyTelemetry(
        String telemetryId,
        EchoDifficultyMetricKind kind,
        EchoModuleId sourceModule,
        double observedValue,
        double targetValue,
        double recommendedMultiplier,
        String summary,
        Map<String, String> attributes
) {
    public EchoDifficultyTelemetry {
        telemetryId = DifficultyContractGuards.id(telemetryId, "difficulty telemetry id");
        kind = kind == null ? EchoDifficultyMetricKind.UNKNOWN : kind;
        observedValue = DifficultyContractGuards.finite(observedValue, "observed value");
        targetValue = DifficultyContractGuards.finite(targetValue, "target value");
        recommendedMultiplier = DifficultyContractGuards.nonNegative(recommendedMultiplier, "recommended multiplier");
        summary = DifficultyContractGuards.optionalText(summary);
        attributes = DifficultyContractGuards.immutableMap(attributes);
    }
}

package com.knoxhack.echo.difficultycore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoDifficultyTuning(
        String tuningId,
        EchoDifficultyMetricKind kind,
        EchoContentReference targetReference,
        double baseMultiplier,
        double minMultiplier,
        double maxMultiplier,
        double adaptiveWeight,
        Map<String, String> attributes
) {
    public EchoDifficultyTuning {
        tuningId = DifficultyContractGuards.id(tuningId, "difficulty tuning id");
        kind = kind == null ? EchoDifficultyMetricKind.UNKNOWN : kind;
        baseMultiplier = DifficultyContractGuards.nonNegative(baseMultiplier, "base multiplier");
        minMultiplier = DifficultyContractGuards.nonNegative(minMultiplier, "min multiplier");
        maxMultiplier = DifficultyContractGuards.nonNegative(maxMultiplier, "max multiplier");
        if (maxMultiplier < minMultiplier) {
            throw new IllegalArgumentException("max multiplier must be greater than or equal to min multiplier");
        }
        adaptiveWeight = DifficultyContractGuards.nonNegative(adaptiveWeight, "adaptive weight");
        attributes = DifficultyContractGuards.immutableMap(attributes);
    }
}

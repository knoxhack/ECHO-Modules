package com.knoxhack.echo.spawncore;

import com.knoxhack.echo.platformcore.EchoGameModeId;

import java.util.Map;

public record EchoDifficultyScale(
        String scaleId,
        EchoGameModeId gameModeId,
        double spawnWeightMultiplier,
        double maxCountMultiplier,
        double hazardIntensityMultiplier,
        double bossChanceMultiplier,
        Map<String, String> attributes
) {
    public EchoDifficultyScale {
        scaleId = SpawnContractGuards.requireText(scaleId, "difficulty scale id");
        spawnWeightMultiplier = SpawnContractGuards.nonNegative(spawnWeightMultiplier, "spawn weight multiplier");
        maxCountMultiplier = SpawnContractGuards.nonNegative(maxCountMultiplier, "max count multiplier");
        hazardIntensityMultiplier = SpawnContractGuards.nonNegative(hazardIntensityMultiplier, "hazard intensity multiplier");
        bossChanceMultiplier = SpawnContractGuards.nonNegative(bossChanceMultiplier, "boss chance multiplier");
        attributes = SpawnContractGuards.immutableMap(attributes);
    }
}

package com.knoxhack.echo.combatcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoGameModeId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;

import java.util.Map;

public record EchoEnemyScalingProfile(
        String scalingId,
        EchoContentReference creatureReference,
        EchoGameModeId gameModeId,
        EchoProgressionId progressionId,
        double healthMultiplier,
        double damageMultiplier,
        double speedMultiplier,
        double lootMultiplier,
        EchoContentGate gate,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoEnemyScalingProfile {
        scalingId = CombatContractGuards.requireText(scalingId, "enemy scaling id");
        healthMultiplier = CombatContractGuards.nonNegative(healthMultiplier, "enemy scaling health multiplier");
        damageMultiplier = CombatContractGuards.nonNegative(damageMultiplier, "enemy scaling damage multiplier");
        speedMultiplier = CombatContractGuards.nonNegative(speedMultiplier, "enemy scaling speed multiplier");
        lootMultiplier = CombatContractGuards.nonNegative(lootMultiplier, "enemy scaling loot multiplier");
        gate = gate == null ? EchoContentGate.open() : gate;
        developerDetails = CombatContractGuards.optionalText(developerDetails);
        attributes = CombatContractGuards.immutableMap(attributes);
    }
}

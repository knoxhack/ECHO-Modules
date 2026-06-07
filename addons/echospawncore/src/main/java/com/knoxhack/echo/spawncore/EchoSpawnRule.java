package com.knoxhack.echo.spawncore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoSpawnRule(
        String ruleId,
        EchoSpawnRuleKind kind,
        EchoContentReference creatureReference,
        int weight,
        int minCount,
        int maxCount,
        EchoSpawnCondition condition,
        List<EchoDifficultyScale> difficultyScales,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoSpawnRule {
        ruleId = SpawnContractGuards.requireText(ruleId, "spawn rule id");
        kind = kind == null ? EchoSpawnRuleKind.UNKNOWN : kind;
        weight = SpawnContractGuards.nonNegative(weight, "spawn rule weight");
        minCount = SpawnContractGuards.nonNegative(minCount, "spawn rule min count");
        maxCount = Math.max(minCount, SpawnContractGuards.nonNegative(maxCount, "spawn rule max count"));
        difficultyScales = SpawnContractGuards.immutableList(difficultyScales);
        diagnostics = SpawnContractGuards.immutableList(diagnostics);
        attributes = SpawnContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}

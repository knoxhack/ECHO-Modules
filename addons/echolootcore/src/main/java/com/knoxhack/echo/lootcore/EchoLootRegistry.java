package com.knoxhack.echo.lootcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoLootRegistry(
        Map<EchoLootProfileId, EchoLootProfile> profiles,
        Map<EchoLootPoolId, EchoLootPool> pools,
        List<EchoMissionRewardPool> missionRewardPools,
        List<EchoAntiDuplicationRule> antiDuplicationRules,
        List<EchoDiagnostic> diagnostics
) {
    public EchoLootRegistry {
        profiles = LootContractGuards.immutableMap(profiles);
        pools = LootContractGuards.immutableMap(pools);
        missionRewardPools = LootContractGuards.immutableList(missionRewardPools);
        antiDuplicationRules = LootContractGuards.immutableList(antiDuplicationRules);
        diagnostics = LootContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || profiles.values().stream().anyMatch(EchoLootProfile::blocking)
                || pools.values().stream().anyMatch(EchoLootPool::blocking);
    }
}

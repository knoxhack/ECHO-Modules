package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoDifficultyApplicationRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoDifficultyApplicationResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeDifficultyApplicationBridge {
    private final String moduleId;

    public EchoNativeDifficultyApplicationBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoDifficultyApplicationResult apply(EchoDifficultyApplicationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("difficulty application request must not be null");
        }
        boolean hasHazard = !request.appliedHazardId().isBlank() && request.scaledHazardDamage() >= 0.0D;
        boolean hasSpawn = !request.appliedSpawnRuleId().isBlank() || request.scaledSpawnBudget() > 0;
        return new EchoDifficultyApplicationResult(
                request.playerId(),
                request.regionId(),
                request.difficulty().id(),
                request.difficulty().hazardMultiplier(),
                request.difficulty().spawnMultiplier(),
                request.appliedHazardId(),
                request.baseHazardDamage(),
                request.scaledHazardDamage(),
                request.appliedSpawnRuleId(),
                request.maxSpawnCount(),
                request.scaledSpawnBudget(),
                request.activeSpawnPopulation(),
                request.gameTick(),
                request.sourceReason(),
                hasHazard || hasSpawn
        );
    }

    public Map<String, Object> report(EchoDifficultyApplicationRequest request) {
        EchoDifficultyApplicationResult result = apply(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_difficulty_application");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("difficultyApplicationResult", result);
        report.put("status", result.applied() ? "PASS" : "NOOP");
        report.put("summary", "Native Loader backend materialized difficulty hazard and spawn modifiers as AdapterCore runtime state.");
        return report;
    }
}

package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoSpawnRuleEventRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoSpawnRuleEventResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeSpawnRuleEventBridge {
    private final String moduleId;

    public EchoNativeSpawnRuleEventBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native spawn rule event module id");
    }

    public EchoSpawnRuleEventResult plan(EchoSpawnRuleEventRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("spawn rule event request must not be null");
        }
        int scaledBudget = Math.max(0, (int) Math.round(
                request.spawnRule().maxCount() * request.difficulty().spawnMultiplier()));
        int spawnCount = Math.max(0, scaledBudget - request.activeMobCount());
        String eventType = spawnCount > 0 ? "SPAWN_ALLOWED" : "SPAWN_CAPPED";
        return new EchoSpawnRuleEventResult(
                request.playerId(),
                request.spawnRule().id(),
                request.spawnRule().entityId(),
                request.regionId(),
                request.difficulty().id(),
                request.spawnRule().maxCount(),
                request.activeMobCount(),
                scaledBudget,
                spawnCount,
                request.difficulty().spawnMultiplier(),
                request.spawnRule().difficultyWeight(),
                eventType,
                request.x(),
                request.y(),
                request.z(),
                request.gameTick(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoSpawnRuleEventRequest request) {
        EchoSpawnRuleEventResult result = plan(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_spawn_rule_event");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("spawnRuleEventResult", result);
        report.put("status", "PASS");
        report.put("summary", "Native Loader backend planned a data-backed spawn event with difficulty-scaled budget through AdapterCore world contracts.");
        return report;
    }
}

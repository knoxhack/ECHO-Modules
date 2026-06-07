package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoSpawnZoneStateRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoSpawnZoneStateResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeSpawnZoneStateBridge {
    private final String moduleId;

    public EchoNativeSpawnZoneStateBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native spawn zone state module id");
    }

    public EchoSpawnZoneStateResult persist(EchoSpawnZoneStateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("spawn zone state request must not be null");
        }
        int activePopulation = request.event().activeMobCount() + request.event().spawnCount();
        String zoneKey = zoneKey(request.event().regionId(), request.event().ruleId());
        return new EchoSpawnZoneStateResult(
                request.playerId(),
                request.event().regionId(),
                request.event().ruleId(),
                zoneKey,
                request.event().entityId(),
                request.event().difficultyId(),
                request.event().maxCount(),
                request.event().activeMobCount(),
                request.event().scaledBudget(),
                request.event().spawnCount(),
                activePopulation,
                request.event().spawnMultiplier(),
                request.event().difficultyWeight(),
                request.event().eventType(),
                request.event().x(),
                request.event().y(),
                request.event().z(),
                request.event().gameTick(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoSpawnZoneStateRequest request) {
        EchoSpawnZoneStateResult result = persist(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_spawn_zone_state");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("spawnZoneStateResult", result);
        report.put("status", result.spawnCount() > 0 ? "SPAWN_ALLOWED" : "SPAWN_CAPPED");
        report.put("summary", "Native Loader backend retained spawn-zone population state through AdapterCore world contracts.");
        return report;
    }

    private static String zoneKey(String regionId, String ruleId) {
        return regionId + "|" + ruleId;
    }
}

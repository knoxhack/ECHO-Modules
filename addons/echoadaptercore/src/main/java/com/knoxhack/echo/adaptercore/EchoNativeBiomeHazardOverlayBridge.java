package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoBiomeHazardOverlayRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoBiomeHazardOverlayResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeBiomeHazardOverlayBridge {
    private final String moduleId;

    public EchoNativeBiomeHazardOverlayBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native biome hazard overlay module id");
    }

    public EchoBiomeHazardOverlayResult resolve(EchoBiomeHazardOverlayRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("biome hazard overlay request must not be null");
        }
        boolean active = request.inRegion() && request.inHazard() && !request.biome().hazardTag().isBlank();
        double intensity = active ? Math.max(1.0D, request.hazard().damagePerTick()) : 0.0D;
        String overlayId = request.biome().id() + "|" + (active ? request.hazard().id() : "no_hazard");
        return new EchoBiomeHazardOverlayResult(
                request.playerId(),
                request.worldId(),
                request.biome().id(),
                request.biome().biomeTag(),
                request.biome().hazardTag(),
                active ? request.hazard().id() : "",
                overlayId,
                cellKey(request.worldId(), request.x(), request.y(), request.z()),
                intensity,
                active,
                active,
                request.gameTick(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoBiomeHazardOverlayRequest request) {
        EchoBiomeHazardOverlayResult result = resolve(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_biome_hazard_overlay");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("biomeHazardOverlayResult", result);
        report.put("status", result.active() ? "PASS" : "INACTIVE");
        report.put("summary", "Native Loader backend resolved a BiomeCore hazard overlay for a sampled world cell.");
        return report;
    }

    private static String cellKey(String worldId, int x, int y, int z) {
        return worldId + ":" + x + ":" + y + ":" + z;
    }
}

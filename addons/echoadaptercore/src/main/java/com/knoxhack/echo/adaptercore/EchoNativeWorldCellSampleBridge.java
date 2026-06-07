package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldCellSampleRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldCellSampleResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeWorldCellSampleBridge {
    private final String moduleId;

    public EchoNativeWorldCellSampleBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native world cell sample module id");
    }

    public EchoWorldCellSampleResult sample(EchoWorldCellSampleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("world cell sample request must not be null");
        }
        boolean inRegion = request.region().contains(request.x(), request.z());
        boolean inHazard = request.hazard().affects(request.x(), request.z());
        return new EchoWorldCellSampleResult(
                request.playerId(),
                request.worldId(),
                inRegion ? request.region().id() : "",
                inHazard ? request.hazard().id() : "",
                inRegion ? request.biome().id() : "",
                inRegion ? request.structure().id() : "",
                inRegion ? request.structure().poiId() : "",
                cellKey(request.worldId(), request.x(), request.y(), request.z()),
                request.x(),
                request.y(),
                request.z(),
                inRegion,
                inHazard,
                request.gameTick(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoWorldCellSampleRequest request) {
        EchoWorldCellSampleResult result = sample(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_world_cell_sample");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("worldCellSampleResult", result);
        report.put("status", result.inRegion() ? "PASS" : "OUT_OF_REGION");
        report.put("summary", "Native Loader backend sampled a region cell and hazard field through AdapterCore world contracts.");
        return report;
    }

    private static String cellKey(String worldId, int x, int y, int z) {
        return worldId + ":" + x + ":" + y + ":" + z;
    }
}

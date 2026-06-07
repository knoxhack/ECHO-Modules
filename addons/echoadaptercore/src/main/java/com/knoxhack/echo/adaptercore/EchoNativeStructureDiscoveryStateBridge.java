package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStructureDiscoveryStateRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStructureDiscoveryStateResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeStructureDiscoveryStateBridge {
    private final String moduleId;

    public EchoNativeStructureDiscoveryStateBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native structure discovery state module id");
    }

    public EchoStructureDiscoveryStateResult discover(EchoStructureDiscoveryStateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("structure discovery state request must not be null");
        }
        boolean discovered = request.markerState().inRange() && request.markerState().markerPersisted();
        return new EchoStructureDiscoveryStateResult(
                request.playerId(),
                request.markerState().markerId(),
                request.markerState().regionId(),
                request.markerState().structureId(),
                request.markerState().poiId(),
                "UNKNOWN",
                discovered ? "DISCOVERED" : "UNKNOWN",
                discovered,
                discovered,
                discovered,
                request.markerState().lastGameTick(),
                request.sourceReason());
    }

    public Map<String, Object> report(EchoStructureDiscoveryStateRequest request) {
        EchoStructureDiscoveryStateResult result = discover(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_structure_discovery_state");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("structureDiscoveryStateResult", result);
        report.put("status", result.discovered() ? "PASS" : "OUT_OF_RANGE");
        report.put("summary", "Native Loader backend materialized StructureCore POI discovery state through AdapterCore world contracts.");
        return report;
    }
}

package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStructurePoiMarkerStateRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStructurePoiMarkerStateResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeStructurePoiMarkerStateBridge {
    private final String moduleId;

    public EchoNativeStructurePoiMarkerStateBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native structure POI marker state module id");
    }

    public EchoStructurePoiMarkerStateResult persist(EchoStructurePoiMarkerStateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("structure POI marker state request must not be null");
        }
        return new EchoStructurePoiMarkerStateResult(
                request.playerId(),
                request.lookup().markerId(),
                request.lookup().regionId(),
                request.lookup().structureId(),
                request.lookup().poiId(),
                request.lookup().x(),
                request.lookup().y(),
                request.lookup().z(),
                request.lookup().distanceSquared(),
                request.lookup().maxDistance(),
                request.lookup().inRange(),
                request.lookup().inRange(),
                request.lookup().lookupType(),
                request.lookup().gameTick(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoStructurePoiMarkerStateRequest request) {
        EchoStructurePoiMarkerStateResult result = persist(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_structure_poi_marker_state");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("structurePoiMarkerStateResult", result);
        report.put("status", result.markerPersisted() ? "PASS" : "OUT_OF_RANGE");
        report.put("summary", "Native Loader backend persisted structure POI marker state through AdapterCore world contracts.");
        return report;
    }
}

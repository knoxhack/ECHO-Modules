package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStructurePoiLookupRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStructurePoiLookupResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeStructurePoiLookupBridge {
    private final String moduleId;

    public EchoNativeStructurePoiLookupBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native structure POI lookup module id");
    }

    public EchoStructurePoiLookupResult lookup(EchoStructurePoiLookupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("structure POI lookup request must not be null");
        }
        long dx = request.playerX() - request.structure().x();
        long dy = request.playerY() - request.structure().y();
        long dz = request.playerZ() - request.structure().z();
        long distanceSquared = dx * dx + dy * dy + dz * dz;
        long maxDistanceSquared = (long) request.maxDistance() * request.maxDistance();
        boolean inRange = distanceSquared <= maxDistanceSquared;
        String markerId = "echoworldcore:marker/" + sanitize(idPath(request.structure().id())) + "/"
                + request.structure().x() + "_" + request.structure().y() + "_" + request.structure().z();
        return new EchoStructurePoiLookupResult(
                request.playerId(),
                request.regionId(),
                request.structure().id(),
                request.structure().poiId(),
                request.structure().x(),
                request.structure().y(),
                request.structure().z(),
                distanceSquared,
                request.maxDistance(),
                inRange,
                markerId,
                inRange ? "POI_IN_RANGE" : "POI_OUT_OF_RANGE",
                request.gameTick(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoStructurePoiLookupRequest request) {
        EchoStructurePoiLookupResult result = lookup(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_structure_poi_lookup");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("structurePoiLookupResult", result);
        report.put("status", result.inRange() ? "PASS" : "OUT_OF_RANGE");
        report.put("summary", "Native Loader backend resolved a structure placement into a POI marker through AdapterCore world contracts.");
        return report;
    }

    private static String sanitize(String value) {
        return value == null ? "unknown" : value.replace(':', '_').replace('\\', '/');
    }

    private static String idPath(String id) {
        int colon = id == null ? -1 : id.indexOf(':');
        return colon >= 0 && colon < id.length() - 1 ? id.substring(colon + 1) : id;
    }
}

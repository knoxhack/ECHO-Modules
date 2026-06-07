package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldChunkStateRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldChunkStateResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeWorldChunkStateBridge {
    private final String moduleId;

    public EchoNativeWorldChunkStateBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native world chunk state module id");
    }

    public EchoWorldChunkStateResult resolve(EchoWorldChunkStateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("world chunk state request must not be null");
        }
        int chunkX = Math.floorDiv(request.x(), 16);
        int chunkZ = Math.floorDiv(request.z(), 16);
        return new EchoWorldChunkStateResult(
                request.playerId(),
                request.worldId(),
                chunkKey(request.worldId(), request.x(), request.z()),
                chunkX,
                chunkZ,
                request.cellSample().cellKey(),
                request.cellSample().x(),
                request.cellSample().y(),
                request.cellSample().z(),
                request.cellSample().activeRegionId(),
                request.cellSample().activeHazardId(),
                request.cellSample().biomeProfileId(),
                request.cellSample().structureId(),
                request.cellSample().poiId(),
                request.cellSample().inRegion(),
                request.cellSample().inHazard(),
                request.gameTick(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoWorldChunkStateRequest request) {
        EchoWorldChunkStateResult result = resolve(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_world_chunk_state");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("worldChunkStateResult", result);
        report.put("status", result.inRegion() ? "PASS" : "OUT_OF_REGION");
        report.put("summary", "Native Loader backend materialized a chunk snapshot from an AdapterCore world cell sample.");
        return report;
    }

    private static String chunkKey(String worldId, int x, int z) {
        return worldId + ":chunk:" + Math.floorDiv(x, 16) + ":" + Math.floorDiv(z, 16);
    }
}

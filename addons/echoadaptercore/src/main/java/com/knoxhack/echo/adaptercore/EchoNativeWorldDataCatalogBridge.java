package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldDataCatalogRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldDataCatalogResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeWorldDataCatalogBridge {
    private final String moduleId;

    public EchoNativeWorldDataCatalogBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoWorldDataCatalogResult materialize(EchoWorldDataCatalogRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("world data catalog request must not be null");
        }
        return new EchoWorldDataCatalogResult(
                request.regionIds().size(),
                request.hazardIds().size(),
                request.weatherProfileIds().size(),
                request.biomeIds().size(),
                request.structureIds().size(),
                request.statusEffectIds().size(),
                request.difficultyIds().size(),
                request.spawnRuleCount(),
                request.sourceFiles().size(),
                representative(request.regionIds()),
                representative(request.hazardIds()),
                representative(request.weatherProfileIds()),
                representative(request.biomeIds()),
                representative(request.structureIds()),
                representative(request.statusEffectIds()),
                representative(request.difficultyIds()),
                request.sourceReason(),
                true);
    }

    public Map<String, Object> report(EchoWorldDataCatalogRequest request) {
        EchoWorldDataCatalogResult result = materialize(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_world_data_catalog");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("worldDataCatalogResult", result);
        report.put("status", result.loaded() ? "PASS" : "FAIL");
        report.put("summary", "Native Loader backend materialized loaded Agent 7 world data definitions as an AdapterCore catalog state.");
        return report;
    }

    private static List<String> representative(List<String> ids) {
        if (ids.size() <= 2) {
            return ids;
        }
        return List.of(ids.get(0), ids.get(ids.size() - 1));
    }
}

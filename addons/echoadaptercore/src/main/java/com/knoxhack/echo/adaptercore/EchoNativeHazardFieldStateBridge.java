package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoHazardFieldStateRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoHazardFieldStateResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeHazardFieldStateBridge {
    private final String moduleId;

    public EchoNativeHazardFieldStateBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native hazard field state module id");
    }

    public EchoHazardFieldStateResult resolve(EchoHazardFieldStateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("hazard field state request must not be null");
        }
        return new EchoHazardFieldStateResult(
                request.playerId(),
                request.worldId(),
                request.hazard().id(),
                request.hazard().type(),
                request.hazard().centerX(),
                request.hazard().centerZ(),
                request.hazard().radius(),
                request.hazard().damagePerTick(),
                request.hazard().statusEffectId(),
                request.cellSample().cellKey(),
                request.cellSample().inHazard(),
                request.gameTick(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoHazardFieldStateRequest request) {
        EchoHazardFieldStateResult result = resolve(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_hazard_field_state");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("hazardFieldStateResult", result);
        report.put("status", result.sampledInside() ? "PASS" : "OUT_OF_FIELD");
        report.put("summary", "Native Loader backend materialized a hazard-field snapshot from an AdapterCore world cell sample.");
        return report;
    }
}

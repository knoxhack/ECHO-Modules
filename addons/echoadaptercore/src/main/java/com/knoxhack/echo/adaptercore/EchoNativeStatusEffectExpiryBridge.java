package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusEffectExpiryRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusEffectExpiryResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeStatusEffectExpiryBridge {
    private final String moduleId;

    public EchoNativeStatusEffectExpiryBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native status effect expiry module id");
    }

    public EchoStatusEffectExpiryResult evaluate(EchoStatusEffectExpiryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("status effect expiry request must not be null");
        }
        long tick = Math.max(0L, request.gameTick());
        boolean expired = tick >= request.expiresAtTick();
        return new EchoStatusEffectExpiryResult(
                request.playerId(),
                request.hazardId(),
                request.effectId(),
                request.saveKey(),
                request.appliedGameTick(),
                request.expiresAtTick(),
                tick,
                expired,
                !expired,
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoStatusEffectExpiryRequest request) {
        EchoStatusEffectExpiryResult result = evaluate(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_status_effect_expiry");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("statusEffectExpiryResult", result);
        report.put("status", result.expired() ? "EXPIRED" : "RETAINED");
        report.put("summary", "Native Loader backend evaluated hazard status effect duration expiry through AdapterCore world contracts.");
        return report;
    }
}

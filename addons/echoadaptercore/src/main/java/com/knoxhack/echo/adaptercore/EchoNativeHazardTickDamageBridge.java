package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoHazardTickDamageRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoHazardTickDamageResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeHazardTickDamageBridge {
    private final String moduleId;

    public EchoNativeHazardTickDamageBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native hazard tick damage module id");
    }

    public EchoHazardTickDamageResult apply(EchoHazardTickDamageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("hazard tick damage request must not be null");
        }
        double baseDamage = Math.max(0.0D, request.hazard().damagePerTick());
        double damage = baseDamage * request.difficulty().hazardMultiplier();
        double healthAfter = Math.max(0.0D, request.healthBefore() - damage);
        return new EchoHazardTickDamageResult(
                request.playerId(),
                request.hazard().id(),
                request.hazard().statusEffectId(),
                request.difficulty().id(),
                request.healthBefore(),
                healthAfter,
                baseDamage,
                damage,
                request.difficulty().hazardMultiplier(),
                request.severity(),
                request.gameTick(),
                request.sourceReason(),
                damage > 0.0D && healthAfter < request.healthBefore()
        );
    }

    public Map<String, Object> report(EchoHazardTickDamageRequest request) {
        EchoHazardTickDamageResult result = apply(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_hazard_tick_damage");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("hazardTickDamageResult", result);
        report.put("status", result.damaged() ? "PASS" : "SKIPPED");
        report.put("summary", "Native Loader backend applied hazard tick damage through AdapterCore hazard and difficulty contracts.");
        return report;
    }
}

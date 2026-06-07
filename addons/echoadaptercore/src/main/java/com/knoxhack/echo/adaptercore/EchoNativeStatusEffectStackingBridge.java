package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusEffectStackingRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusEffectStackingResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeStatusEffectStackingBridge {
    private final String moduleId;

    public EchoNativeStatusEffectStackingBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native status effect stacking module id");
    }

    public EchoStatusEffectStackingResult stack(EchoStatusEffectStackingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("status effect stacking request must not be null");
        }
        String policy = request.stackingPolicy();
        boolean refreshDuration = "REFRESH_DURATION".equals(policy);
        int durationTicks = refreshDuration && request.hadPrevious()
                ? Math.max(request.previousDurationTicks(), request.statusEffect().durationTicks())
                : request.statusEffect().durationTicks();
        int amplifier = refreshDuration && request.hadPrevious()
                ? Math.max(request.previousAmplifier(), request.statusEffect().amplifier())
                : request.statusEffect().amplifier();
        double damageApplied = Math.max(request.previousDamageApplied(), request.damageApplied());
        long appliedGameTick = Math.max(0L, request.gameTick());
        long expiresAtTick = appliedGameTick + Math.max(0, durationTicks);
        boolean refreshed = request.hadPrevious() && refreshDuration;
        boolean upgraded = request.hadPrevious() && amplifier > request.previousAmplifier();
        boolean stacked = request.hadPrevious() && !refreshDuration;
        return new EchoStatusEffectStackingResult(
                request.playerId(),
                request.hazardId(),
                request.statusEffect().id(),
                request.statusEffect().saveKey(),
                policy,
                durationTicks,
                amplifier,
                damageApplied,
                appliedGameTick,
                expiresAtTick,
                request.hadPrevious(),
                refreshed,
                upgraded,
                stacked,
                true,
                request.loaded(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoStatusEffectStackingRequest request) {
        EchoStatusEffectStackingResult result = stack(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_status_effect_stacking");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("statusEffectStackingResult", result);
        report.put("status", result.refreshed() ? "REFRESHED" : "APPLIED");
        report.put("summary", "Native Loader backend resolved status effect stacking into live duration and amplifier state.");
        return Map.copyOf(report);
    }
}

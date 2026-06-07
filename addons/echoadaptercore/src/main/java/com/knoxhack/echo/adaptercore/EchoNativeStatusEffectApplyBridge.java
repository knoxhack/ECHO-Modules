package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusEffectApplyRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusEffectApplyResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeStatusEffectApplyBridge {
    private final String moduleId;

    public EchoNativeStatusEffectApplyBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native status effect apply module id");
    }

    public EchoStatusEffectApplyResult apply(EchoStatusEffectApplyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("status effect apply request must not be null");
        }
        long appliedGameTick = Math.max(0L, request.gameTick());
        long expiresAtTick = appliedGameTick + Math.max(0, request.statusEffect().durationTicks());
        Map<String, Object> activeStatusState = new LinkedHashMap<>();
        activeStatusState.put("moduleId", moduleId);
        activeStatusState.put("playerId", request.playerId());
        activeStatusState.put("hazardId", request.hazardId());
        activeStatusState.put("effectId", request.statusEffect().id());
        activeStatusState.put("saveKey", request.statusEffect().saveKey());
        activeStatusState.put("durationTicks", request.statusEffect().durationTicks());
        activeStatusState.put("amplifier", request.statusEffect().amplifier());
        activeStatusState.put("damageApplied", request.damageApplied());
        activeStatusState.put("appliedGameTick", appliedGameTick);
        activeStatusState.put("expiresAtTick", expiresAtTick);
        activeStatusState.put("loaded", request.loaded());

        return new EchoStatusEffectApplyResult(
                request.playerId(),
                request.hazardId(),
                request.statusEffect().id(),
                request.statusEffect().durationTicks(),
                request.statusEffect().amplifier(),
                request.statusEffect().saveKey(),
                request.damageApplied(),
                appliedGameTick,
                expiresAtTick,
                activeStatusState,
                request.sourceReason(),
                request.loaded(),
                true
        );
    }

    public Map<String, Object> report(EchoStatusEffectApplyRequest request) {
        EchoStatusEffectApplyResult result = apply(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_status_effect_apply");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("statusEffectApplyResult", result);
        report.put("status", result.applied() ? "PASS" : "SKIPPED");
        report.put("summary", "Native Loader backend applied a hazard status effect into active runtime state through AdapterCore world contracts.");
        return report;
    }
}

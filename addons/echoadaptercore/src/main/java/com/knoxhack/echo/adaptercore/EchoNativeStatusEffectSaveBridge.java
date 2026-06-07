package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusEffectSaveRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusEffectSaveResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeStatusEffectSaveBridge {
    private final String moduleId;

    public EchoNativeStatusEffectSaveBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native status effect save module id");
    }

    public EchoStatusEffectSaveResult persist(EchoStatusEffectSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("status effect save request must not be null");
        }
        Map<String, Object> statusPayload = new LinkedHashMap<>();
        statusPayload.put("effectId", request.statusEffect().id());
        statusPayload.put("durationTicks", request.statusEffect().durationTicks());
        statusPayload.put("amplifier", request.statusEffect().amplifier());
        statusPayload.put("hazardId", request.hazardId());
        statusPayload.put("damageApplied", request.damageApplied());
        statusPayload.put("gameTick", request.gameTick());

        Map<String, Object> savedStatus = new LinkedHashMap<>();
        savedStatus.put(request.statusEffect().saveKey(), Map.copyOf(statusPayload));
        savedStatus.put("adapterCoreModule", moduleId);

        return new EchoStatusEffectSaveResult(
                request.playerId(),
                request.hazardId(),
                request.statusEffect().id(),
                request.statusEffect().durationTicks(),
                request.statusEffect().amplifier(),
                request.statusEffect().saveKey(),
                request.damageApplied(),
                request.gameTick(),
                savedStatus,
                request.sourceReason(),
                true
        );
    }

    public Map<String, Object> report(EchoStatusEffectSaveRequest request) {
        EchoStatusEffectSaveResult result = persist(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_status_effect_save");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("statusEffectSaveResult", result);
        report.put("status", result.saved() ? "PASS" : "SKIPPED");
        report.put("summary", "Native Loader backend converted a hazard status effect into save/load state through AdapterCore world contracts.");
        return report;
    }
}

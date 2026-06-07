package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusExposureMitigationRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusExposureMitigationResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeStatusExposureMitigationBridge {
    private final String moduleId;

    public EchoNativeStatusExposureMitigationBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native status exposure mitigation module id");
    }

    public EchoStatusExposureMitigationResult mitigate(EchoStatusExposureMitigationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("status exposure mitigation request must not be null");
        }
        double mitigationFactor = 1.0D - request.mitigationRatio();
        double effectiveIntensity = request.exposureIntensity() * mitigationFactor;
        boolean immune = effectiveIntensity <= request.immunityThreshold();
        int effectiveDurationTicks = immune ? 0 : (int) Math.round(request.durationTicks() * mitigationFactor);
        double effectiveAccumulation = immune ? 0.0D : request.accumulationPerSecond() * mitigationFactor;
        if (immune) {
            effectiveIntensity = 0.0D;
        }

        Map<String, Object> exposureState = new LinkedHashMap<>();
        exposureState.put("adapterCoreContract", "echostatuscore:status/exposure_mitigation");
        exposureState.put("adapterCoreBridge", true);
        exposureState.put("nativeLoaderBackend", true);
        exposureState.put("moduleId", moduleId);
        exposureState.put("playerId", request.playerId());
        exposureState.put("exposureId", request.exposureId());
        exposureState.put("hazardId", request.hazardId());
        exposureState.put("effectId", request.statusEffect().id());
        exposureState.put("statusKind", request.statusKind());
        exposureState.put("resistanceId", request.resistanceId());
        exposureState.put("mitigationRatio", request.mitigationRatio());
        exposureState.put("immunityThreshold", request.immunityThreshold());
        exposureState.put("originalIntensity", request.exposureIntensity());
        exposureState.put("effectiveIntensity", effectiveIntensity);
        exposureState.put("originalDurationTicks", request.durationTicks());
        exposureState.put("effectiveDurationTicks", effectiveDurationTicks);
        exposureState.put("originalAccumulationPerSecond", request.accumulationPerSecond());
        exposureState.put("effectiveAccumulationPerSecond", effectiveAccumulation);
        exposureState.put("immune", immune);
        exposureState.put("gameTick", request.gameTick());
        exposureState.put("sourceReason", request.sourceReason());

        return new EchoStatusExposureMitigationResult(
                request.playerId(),
                request.exposureId(),
                request.hazardId(),
                request.statusEffect().id(),
                request.statusKind(),
                request.exposureIntensity(),
                effectiveIntensity,
                request.durationTicks(),
                effectiveDurationTicks,
                request.accumulationPerSecond(),
                effectiveAccumulation,
                request.resistanceId(),
                request.mitigationRatio(),
                request.immunityThreshold(),
                immune,
                exposureState,
                request.gameTick(),
                request.sourceReason(),
                true
        );
    }

    public Map<String, Object> report(EchoStatusExposureMitigationRequest request) {
        EchoStatusExposureMitigationResult result = mitigate(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_status_exposure_mitigation");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("statusExposureMitigationResult", result);
        report.put("status", result.immune() ? "IMMUNE" : "MITIGATED");
        report.put("summary", "Native Loader backend applied StatusCore resistance to hazard exposure runtime state.");
        return Map.copyOf(report);
    }
}

package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoAtmosphereState;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoAtmosphereStateApplyRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoAtmosphereStateApplyResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAtmosphereStateApplyBridge {
    private final String moduleId;

    public EchoNativeAtmosphereStateApplyBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native atmosphere state apply module id");
    }

    public EchoAtmosphereStateApplyResult apply(EchoAtmosphereStateApplyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("atmosphere state apply request must not be null");
        }
        EchoAtmosphereState atmosphere = request.atmosphere();
        Map<String, Object> renderState = new LinkedHashMap<>();
        renderState.put("atmosphere", atmosphere.id());
        renderState.put("visibility", atmosphere.visibility());
        renderState.put("particles", atmosphere.particleProfile());
        renderState.put("skyFog", atmosphere.skyFog());
        renderState.put("phase", request.phase());

        Map<String, Object> runtimeBindings = new LinkedHashMap<>();
        runtimeBindings.put("moduleId", moduleId);
        runtimeBindings.put("render.visibility", atmosphere.visibility());
        runtimeBindings.put("render.particles", atmosphere.particleProfile());
        runtimeBindings.put("render.skyFog", atmosphere.skyFog());
        runtimeBindings.put("weatherId", request.weatherId());

        return new EchoAtmosphereStateApplyResult(
                request.eventId(),
                request.weatherId(),
                request.regionId(),
                request.phase(),
                renderState,
                runtimeBindings,
                request.gameTick(),
                request.sourceReason(),
                true);
    }

    public Map<String, Object> report(EchoAtmosphereStateApplyRequest request) {
        EchoAtmosphereStateApplyResult result = apply(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_atmosphere_state_apply");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("atmosphereStateApplyResult", result);
        report.put("status", result.applied() ? "PASS" : "SKIPPED");
        report.put("summary", "Native Loader backend applied atmosphere visibility, particles, and sky-fog state through AdapterCore world contracts.");
        return report;
    }
}

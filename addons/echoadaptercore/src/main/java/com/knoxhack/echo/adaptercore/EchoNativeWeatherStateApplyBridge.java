package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherStateApplyRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherStateApplyResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeWeatherStateApplyBridge {
    private final String moduleId;

    public EchoNativeWeatherStateApplyBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native weather state apply module id");
    }

    public EchoWeatherStateApplyResult apply(EchoWeatherStateApplyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("weather state apply request must not be null");
        }
        Map<String, Object> hudState = new LinkedHashMap<>();
        hudState.put("weather", request.weather().hudLine());
        hudState.put("phase", request.phase());
        hudState.put("eventId", request.eventId());

        Map<String, Object> audioState = new LinkedHashMap<>();
        audioState.put("cue", request.weather().audioCue());
        audioState.put("region", request.regionId());
        audioState.put("phase", request.phase());

        Map<String, Object> renderState = new LinkedHashMap<>();
        renderState.put("weatherProfile", request.weather().renderProfile());
        renderState.put("atmosphere", request.atmosphere().id());
        renderState.put("visibility", request.atmosphere().visibility());
        renderState.put("particles", request.atmosphere().particleProfile());
        renderState.put("skyFog", request.atmosphere().skyFog());

        return new EchoWeatherStateApplyResult(
                request.eventId(),
                request.weather().id(),
                request.regionId(),
                request.phase(),
                hudState,
                audioState,
                renderState,
                request.gameTick(),
                request.sourceReason(),
                true
        );
    }

    public Map<String, Object> report(EchoWeatherStateApplyRequest request) {
        EchoWeatherStateApplyResult result = apply(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_weather_state_apply");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("weatherStateApplyResult", result);
        report.put("status", result.applied() ? "PASS" : "SKIPPED");
        report.put("summary", "Native Loader backend applied weather HUD, audio, and render state through AdapterCore world contracts.");
        return report;
    }
}

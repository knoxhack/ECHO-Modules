package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoEmergencySirenUseRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoEmergencySirenUseResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeEmergencySirenBridge {
    private final String moduleId;

    public EchoNativeEmergencySirenBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native emergency siren module id");
    }

    public EchoEmergencySirenUseResult use(EchoEmergencySirenUseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("emergency siren request must not be null");
        }
        String phase = request.phase().toUpperCase(java.util.Locale.ROOT);
        String severity = request.severity().toUpperCase(java.util.Locale.ROOT);
        String message = "Emergency Siren: "
                + (request.activeWeatherDetected() ? "ACTIVE WEATHER DETECTED" : "All clear.");
        Map<String, Object> hudState = new LinkedHashMap<>();
        hudState.put("emergencySiren", message);
        hudState.put("activeWeatherDetected", request.activeWeatherDetected());
        hudState.put("phase", phase);
        hudState.put("severity", severity);
        hudState.put("weatherIds", request.weatherIds());
        Map<String, Object> audioState = new LinkedHashMap<>();
        audioState.put("cue", request.activeWeatherDetected()
                ? "echoweathercore:siren/active_weather"
                : "echoweathercore:siren/all_clear");
        audioState.put("activeWeatherDetected", request.activeWeatherDetected());
        Map<String, Object> renderState = new LinkedHashMap<>();
        renderState.put("warningLight", request.activeWeatherDetected() ? "RED_PULSE" : "GREEN_STEADY");
        renderState.put("overlay", request.activeWeatherDetected()
                ? "echoweathercore:emergency_siren/active"
                : "echoweathercore:emergency_siren/clear");
        renderState.put("severity", severity);
        return new EchoEmergencySirenUseResult(
                request.playerId(),
                request.weatherIds(),
                request.activeWeatherDetected(),
                phase,
                severity,
                request.x(),
                request.y(),
                request.z(),
                message,
                hudState,
                audioState,
                renderState,
                request.gameTick(),
                request.sourceReason(),
                !request.playerId().isBlank());
    }

    public Map<String, Object> report(EchoEmergencySirenUseRequest request) {
        EchoEmergencySirenUseResult result = use(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_emergency_siren");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("emergencySirenResult", result);
        report.put("status", result.activeWeatherDetected() ? "ACTIVE_WEATHER_DETECTED" : "ALL_CLEAR");
        report.put("summary", "Native Loader backend materialized an Emergency Siren interaction into HUD/audio/render warning state.");
        return Map.copyOf(report);
    }
}

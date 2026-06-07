package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherWarningRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherWarningResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeWeatherWarningBridge {
    private final String moduleId;

    public EchoNativeWeatherWarningBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native weather warning module id");
    }

    public EchoWeatherWarningResult issue(EchoWeatherWarningRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("weather warning request must not be null");
        }
        Map<String, Object> hudState = new LinkedHashMap<>();
        hudState.put("weatherWarning", request.message());
        hudState.put("weatherId", request.weatherId());
        hudState.put("phase", request.phase());
        hudState.put("channel", request.channel());
        Map<String, Object> audioState = new LinkedHashMap<>();
        audioState.put("cue", "echoweathercore:warning/" + safePath(request.weatherId()));
        audioState.put("phase", request.phase());
        audioState.put("channel", request.channel());
        Map<String, Object> renderState = new LinkedHashMap<>();
        renderState.put("warningOverlay", "echoweathercore:overlay/" + safePath(request.weatherId()));
        renderState.put("severityPulse", request.phase().equals("CRITICAL") || request.phase().equals("ACTIVE"));
        renderState.put("recipientCount", request.recipientPlayerIds().size());
        return new EchoWeatherWarningResult(
                request.eventId(),
                request.weatherId(),
                request.regionId(),
                request.phase(),
                request.channel(),
                request.message(),
                request.recipientPlayerIds(),
                request.recipientPlayerIds().size(),
                hudState,
                audioState,
                renderState,
                request.gameTick(),
                request.sourceReason(),
                !request.recipientPlayerIds().isEmpty()
        );
    }

    public Map<String, Object> report(EchoWeatherWarningRequest request) {
        EchoWeatherWarningResult result = issue(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_weather_warning");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("weatherWarningResult", result);
        report.put("status", result.delivered() ? "DELIVERED" : "NO_RECIPIENTS");
        report.put("summary", "Native Loader backend materialized WeatherCore warning delivery into HUD/audio/render alert state.");
        return Map.copyOf(report);
    }

    private static String safePath(String id) {
        String value = AdapterContractGuards.requireText(id, "weather warning id");
        int separator = value.indexOf(':');
        return separator >= 0 ? value.substring(separator + 1) : value;
    }
}

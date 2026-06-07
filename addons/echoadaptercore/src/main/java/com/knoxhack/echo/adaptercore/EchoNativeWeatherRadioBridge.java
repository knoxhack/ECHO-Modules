package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherRadioUseRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherRadioUseResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeWeatherRadioBridge {
    private final String moduleId;

    public EchoNativeWeatherRadioBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native weather radio module id");
    }

    public EchoWeatherRadioUseResult use(EchoWeatherRadioUseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("weather radio request must not be null");
        }
        String severity = request.strongestSeverity().toUpperCase(Locale.ROOT);
        String routeRisk = request.routeRisk().toUpperCase(Locale.ROOT);
        List<String> messageLines = new ArrayList<>();
        if (request.forecastsAvailable()) {
            messageLines.add("Weather Radio - Regional Forecast:");
            messageLines.addAll(request.forecastLines());
        } else {
            messageLines.add("Weather Radio: No regional weather events.");
        }
        Map<String, Object> hudState = new LinkedHashMap<>();
        hudState.put("weatherRadio", messageLines.get(0));
        hudState.put("forecastCount", request.forecastLines().size());
        hudState.put("routeRisk", routeRisk);
        hudState.put("strongestSeverity", severity);
        hudState.put("weatherIds", request.weatherIds());
        Map<String, Object> audioState = new LinkedHashMap<>();
        audioState.put("cue", request.forecastsAvailable()
                ? "echoweathercore:weather_radio/forecast"
                : "echoweathercore:weather_radio/clear");
        audioState.put("forecastCount", request.forecastLines().size());
        Map<String, Object> renderState = new LinkedHashMap<>();
        renderState.put("readout", request.forecastsAvailable()
                ? "echoweathercore:weather_radio/forecast"
                : "echoweathercore:weather_radio/clear");
        renderState.put("routeRisk", routeRisk);
        renderState.put("strongestSeverity", severity);
        return new EchoWeatherRadioUseResult(
                request.playerId(),
                request.weatherIds(),
                request.forecastLines(),
                request.forecastsAvailable(),
                severity,
                routeRisk,
                request.cooldownTicks(),
                messageLines,
                hudState,
                audioState,
                renderState,
                request.gameTick(),
                request.sourceReason(),
                !request.playerId().isBlank());
    }

    public Map<String, Object> report(EchoWeatherRadioUseRequest request) {
        EchoWeatherRadioUseResult result = use(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_weather_radio");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("weatherRadioResult", result);
        report.put("status", result.forecastsAvailable() ? "FORECAST_DELIVERED" : "NO_FORECASTS");
        report.put("summary", "Native Loader backend materialized a Weather Radio interaction into HUD/audio/render forecast state.");
        return Map.copyOf(report);
    }
}

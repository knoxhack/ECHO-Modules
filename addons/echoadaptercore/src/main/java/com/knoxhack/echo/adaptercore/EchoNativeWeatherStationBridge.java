package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherStationUseRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherStationUseResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeWeatherStationBridge {
    private final String moduleId;

    public EchoNativeWeatherStationBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native weather station module id");
    }

    public EchoWeatherStationUseResult use(EchoWeatherStationUseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("weather station request must not be null");
        }
        String severity = request.strongestSeverity().toUpperCase(Locale.ROOT);
        String routeRisk = request.routeRisk().toUpperCase(Locale.ROOT);
        List<String> messageLines = new ArrayList<>();
        if (request.forecastsAvailable()) {
            messageLines.add("=== Weather Station Forecast ===");
            messageLines.addAll(request.forecastLines());
        } else {
            messageLines.add("Weather Station: No active or forecasted weather.");
        }
        Map<String, Object> hudState = new LinkedHashMap<>();
        hudState.put("weatherStation", messageLines.get(0));
        hudState.put("forecastCount", request.forecastLines().size());
        hudState.put("routeRisk", routeRisk);
        hudState.put("strongestSeverity", severity);
        hudState.put("weatherIds", request.weatherIds());
        hudState.put("stationPosition", List.of(request.x(), request.y(), request.z()));
        Map<String, Object> audioState = new LinkedHashMap<>();
        audioState.put("cue", request.forecastsAvailable()
                ? "echoweathercore:weather_station/forecast"
                : "echoweathercore:weather_station/clear");
        audioState.put("forecastCount", request.forecastLines().size());
        Map<String, Object> renderState = new LinkedHashMap<>();
        renderState.put("readout", request.forecastsAvailable()
                ? "echoweathercore:weather_station/forecast"
                : "echoweathercore:weather_station/clear");
        renderState.put("routeRisk", routeRisk);
        renderState.put("strongestSeverity", severity);
        renderState.put("stationPosition", List.of(request.x(), request.y(), request.z()));
        return new EchoWeatherStationUseResult(
                request.playerId(),
                request.weatherIds(),
                request.forecastLines(),
                request.forecastsAvailable(),
                severity,
                routeRisk,
                request.x(),
                request.y(),
                request.z(),
                messageLines,
                hudState,
                audioState,
                renderState,
                request.gameTick(),
                request.sourceReason(),
                !request.playerId().isBlank());
    }

    public Map<String, Object> report(EchoWeatherStationUseRequest request) {
        EchoWeatherStationUseResult result = use(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_weather_station");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("weatherStationResult", result);
        report.put("status", result.forecastsAvailable() ? "FORECAST_DELIVERED" : "NO_FORECASTS");
        report.put("summary", "Native Loader backend materialized a Weather Station interaction into retained station forecast state.");
        return Map.copyOf(report);
    }
}

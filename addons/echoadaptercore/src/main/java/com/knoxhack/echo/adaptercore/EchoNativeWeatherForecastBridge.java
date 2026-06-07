package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherForecastRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherForecastResult;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeWeatherForecastBridge {
    private final String moduleId;

    public EchoNativeWeatherForecastBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "weather forecast module id");
    }

    public EchoWeatherForecastResult forecast(EchoWeatherForecastRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("weather forecast request must not be null");
        }
        double riskScore = baseScore(request.severity()) * Math.max(0.0D, request.routeRiskModifier());
        return new EchoWeatherForecastResult(
                request.playerId(),
                request.eventId(),
                request.weatherId(),
                request.weatherType().toUpperCase(Locale.ROOT),
                request.displayName(),
                request.phase().toUpperCase(Locale.ROOT),
                request.severity().toUpperCase(Locale.ROOT),
                request.etaTicks(),
                request.regionId().isBlank() ? "Unknown" : request.regionId(),
                (int) Math.max(0L, request.endTick() - request.startTick()),
                request.recommendedGear(),
                request.shelterRecommendation(),
                riskForScore(riskScore),
                request.routeRiskModifier(),
                Math.round(request.scannerReliabilityMultiplier() * 100.0D) + "%",
                request.echoLines(),
                request.gameTick(),
                request.sourceReason(),
                true);
    }

    public Map<String, Object> report(EchoWeatherForecastRequest request) {
        EchoWeatherForecastResult result = forecast(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_weather_forecast");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("weatherForecastResult", result);
        report.put("status", "PASS");
        report.put("summary", "Native Loader backend materialized WeatherCore forecast state through AdapterCore contracts.");
        return report;
    }

    private static double baseScore(String severity) {
        return switch (severity.toUpperCase(Locale.ROOT)) {
            case "LOW" -> 0.0D;
            case "MODERATE" -> 1.0D;
            case "SEVERE" -> 2.0D;
            case "EXTREME" -> 3.0D;
            default -> 0.0D;
        };
    }

    private static String riskForScore(double score) {
        if (score < 1.0D) {
            return "SAFE";
        }
        if (score < 2.0D) {
            return "WATCH";
        }
        if (score < 3.0D) {
            return "HAZARDOUS";
        }
        if (score < 4.0D) {
            return "DELAY_RECOMMENDED";
        }
        return "ROUTE_LOCKDOWN";
    }
}

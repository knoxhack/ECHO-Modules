package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherRouteRiskRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherRouteRiskResult;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeWeatherRouteRiskBridge {
    private final String moduleId;

    public EchoNativeWeatherRouteRiskBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "weather route risk module id");
    }

    public EchoWeatherRouteRiskResult evaluate(EchoWeatherRouteRiskRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("weather route risk request must not be null");
        }
        double baseScore = baseScore(request.severity());
        double riskScore = baseScore * Math.max(0.0D, request.routeRiskModifier());
        return new EchoWeatherRouteRiskResult(
                request.playerId(),
                request.weatherId(),
                request.severity().toUpperCase(Locale.ROOT),
                request.routeRiskModifier(),
                riskScore,
                riskForScore(riskScore),
                request.gameTick(),
                request.sourceReason());
    }

    public Map<String, Object> report(EchoWeatherRouteRiskRequest request) {
        EchoWeatherRouteRiskResult result = evaluate(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_weather_route_risk");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("weatherRouteRiskResult", result);
        report.put("status", "PASS");
        report.put("summary", "Native Loader backend evaluated mitigated weather route risk through AdapterCore weather contracts.");
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

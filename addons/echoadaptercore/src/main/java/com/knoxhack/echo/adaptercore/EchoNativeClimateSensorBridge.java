package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoClimateSensorReadRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoClimateSensorReadResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeClimateSensorBridge {
    private final String moduleId;

    public EchoNativeClimateSensorBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native climate sensor module id");
    }

    public EchoClimateSensorReadResult read(EchoClimateSensorReadRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("climate sensor request must not be null");
        }
        int visibilityPercent = percent(request.visibilityMultiplier());
        int scannerReliabilityPercent = percent(request.scannerReliabilityMultiplier());
        List<String> messageLines = List.of(
                "Climate Sensor Reading:",
                "Sheltered: " + request.sheltered(),
                "Visibility: " + visibilityPercent + "%",
                "Scanner Reliability: " + scannerReliabilityPercent + "%");
        Map<String, Object> hudState = new LinkedHashMap<>();
        hudState.put("climateSensor", "Visibility " + visibilityPercent + "% / Scanner "
                + scannerReliabilityPercent + "%");
        hudState.put("sheltered", request.sheltered());
        hudState.put("visibilityPercent", visibilityPercent);
        hudState.put("scannerReliabilityPercent", scannerReliabilityPercent);
        hudState.put("weatherIds", request.weatherIds());
        Map<String, Object> audioState = new LinkedHashMap<>();
        audioState.put("cue", request.sheltered()
                ? "echoweathercore:climate_sensor/sheltered"
                : "echoweathercore:climate_sensor/exposed");
        audioState.put("sheltered", request.sheltered());
        Map<String, Object> renderState = new LinkedHashMap<>();
        renderState.put("readout", "echoweathercore:climate_sensor/readout");
        renderState.put("visibilityPercent", visibilityPercent);
        renderState.put("scannerReliabilityPercent", scannerReliabilityPercent);
        renderState.put("routeRiskModifier", request.routeRiskModifier());
        return new EchoClimateSensorReadResult(
                request.playerId(),
                request.weatherIds(),
                request.sheltered(),
                visibilityPercent,
                scannerReliabilityPercent,
                request.filterDrainMultiplier(),
                request.toxicExposureMultiplier(),
                request.routeRiskModifier(),
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

    public Map<String, Object> report(EchoClimateSensorReadRequest request) {
        EchoClimateSensorReadResult result = read(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_climate_sensor");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("climateSensorResult", result);
        report.put("status", result.delivered() ? "READING_DELIVERED" : "NO_PLAYER");
        report.put("summary", "Native Loader backend materialized a Climate Sensor interaction into weather modifier readout state.");
        return Map.copyOf(report);
    }

    private static int percent(double multiplier) {
        return (int) Math.round(Math.max(0.0D, multiplier) * 100.0D);
    }
}

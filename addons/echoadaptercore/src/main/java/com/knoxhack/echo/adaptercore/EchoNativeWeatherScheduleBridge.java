package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherScheduleRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherScheduleResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeWeatherScheduleBridge {
    private final String moduleId;

    public EchoNativeWeatherScheduleBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoWeatherScheduleResult schedule(EchoWeatherScheduleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("weather schedule request must not be null");
        }
        long warningTicks = Math.max(request.profile().warningTicks(), request.minimumWarningTicks());
        long warningStart = request.currentTick();
        long start = warningStart + warningTicks;
        long end = start + request.profile().durationTicks();
        boolean scheduled = request.profile().enabled()
                && request.profile().durationTicks() > 0
                && request.profile().weight() > 0;
        return new EchoWeatherScheduleResult(
                request.profile().id(),
                request.profile().type(),
                request.profile().severity(),
                request.profile().scope(),
                scheduled ? "FORECAST" : "SKIPPED",
                warningStart,
                start,
                end,
                request.centerX(),
                request.centerY(),
                request.centerZ(),
                request.radius(),
                request.sourceReason(),
                scheduled
        );
    }

    public Map<String, Object> report(EchoWeatherScheduleRequest request) {
        EchoWeatherScheduleResult result = schedule(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_weather_schedule");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("weatherScheduleResult", result);
        report.put("status", result.scheduled() ? "PASS" : "SKIPPED");
        report.put("summary", "Native Loader backend planned warning, start, and end ticks for a WeatherCore event through AdapterCore world contracts.");
        return report;
    }
}

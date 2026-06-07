package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherScheduleTickRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherScheduleTickResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeWeatherScheduleTickBridge {
    private final String moduleId;

    public EchoNativeWeatherScheduleTickBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoWeatherScheduleTickResult tick(EchoWeatherScheduleTickRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("weather schedule tick request must not be null");
        }
        String phase = phaseFor(request.schedule(), request.gameTick());
        boolean ended = "ENDED".equals(phase);
        return new EchoWeatherScheduleTickResult(
                request.eventId(),
                request.schedule().profileId(),
                request.schedule().type(),
                request.schedule().severity(),
                request.schedule().scope(),
                request.schedule().phase(),
                phase,
                request.gameTick(),
                request.schedule().warningStartTick(),
                request.schedule().startTick(),
                request.schedule().endTick(),
                request.schedule().centerX(),
                request.schedule().centerY(),
                request.schedule().centerZ(),
                request.schedule().radius(),
                request.schedule().sourceReason(),
                request.schedule().scheduled() && !ended,
                ended,
                !request.schedule().phase().equals(phase)
        );
    }

    public Map<String, Object> report(EchoWeatherScheduleTickRequest request) {
        EchoWeatherScheduleTickResult result = tick(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_weather_schedule_tick");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("weatherScheduleTickResult", result);
        report.put("status", result.ended() ? "ENDED" : "PASS");
        report.put("summary", "Native Loader backend advanced a WeatherCore schedule phase through AdapterCore world contracts.");
        return report;
    }

    private static String phaseFor(EchoWorldContracts.EchoWeatherScheduleResult schedule, long tick) {
        long safeTick = Math.max(0L, tick);
        if (safeTick >= schedule.endTick()) {
            return "ENDED";
        }
        long activeWindow = Math.max(1L, schedule.endTick() - schedule.startTick());
        if (safeTick >= schedule.startTick() + Math.round(activeWindow * 0.85D)) {
            return "CLEARING";
        }
        if (safeTick >= schedule.startTick() + Math.round(activeWindow * 0.6D)) {
            return "CRITICAL";
        }
        if (safeTick >= schedule.startTick()) {
            return "ACTIVE";
        }
        long warningWindow = Math.max(1L, schedule.startTick() - schedule.warningStartTick());
        if (safeTick >= schedule.warningStartTick() + Math.round(warningWindow * 0.5D)) {
            return "INCOMING";
        }
        return "FORECAST";
    }
}

package com.knoxhack.echoweathercore.server;

import com.knoxhack.echo.adaptercore.EchoNativeWeatherForecastBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.knoxhack.echoweathercore.api.forecast.WeatherForecast;
import com.knoxhack.echoweathercore.api.weather.ActiveWeatherEvent;
import com.knoxhack.echoweathercore.api.weather.WeatherEffectModifiers;
import com.knoxhack.echoweathercore.api.weather.WeatherPhase;
import com.knoxhack.echoweathercore.api.weather.WeatherProfile;
import com.knoxhack.echoweathercore.api.weather.WeatherRouteRisk;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import com.knoxhack.echoweathercore.data.WeatherDataReloadListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class WeatherForecastManager {
    private static final Map<String, List<EchoWorldContracts.EchoWeatherForecastResult>> LAST_FORECASTS =
            new ConcurrentHashMap<>();

    private WeatherForecastManager() {}

    public static List<WeatherForecast> getForecastForPlayer(ServerPlayer player) {
        Level level = player.level();
        if (level.isClientSide()) return List.of();
        List<WeatherForecast> forecasts = new ArrayList<>();
        List<EchoWorldContracts.EchoWeatherForecastResult> retainedForecasts = new ArrayList<>();
        long tick = level.getGameTime();
        String playerId = player.getUUID().toString();

        for (ActiveWeatherEvent event : WeatherStateManager.getInstance().getEventsAt(level, player.blockPosition())) {
            WeatherProfile profile = WeatherDataReloadListener.INSTANCE.getProfile(event.profileId());
            if (profile == null) continue;

            long eta = event.startTick() > tick ? event.startTick() - tick : 0;
            String shelter = event.type().name().toLowerCase().contains("radiation") ? "Shielded shelter recommended." : "Seek shelter if available.";
            EchoWorldContracts.EchoWeatherForecastResult forecastResult =
                    new EchoNativeWeatherForecastBridge("echoweathercore")
                            .forecast(new EchoWorldContracts.EchoWeatherForecastRequest(
                                    playerId,
                                    event.eventId().toString(),
                                    event.profileId().toString(),
                                    event.type().name(),
                                    profile.displayName(),
                                    event.phase().name(),
                                    event.severity().name(),
                                    event.regionId() != null ? event.regionId().toString() : "Unknown",
                                    tick,
                                    event.startTick(),
                                    event.endTick(),
                                    eta,
                                    profile.effects().routeRiskModifier(),
                                    profile.effects().scannerReliabilityMultiplier(),
                                    profile.recommendedGear().stream().map(Identifier::toString).toList(),
                                    shelter,
                                    profile.echoLines(),
                                    "weather-forecast-manager"));
            retainedForecasts.add(forecastResult);

            forecasts.add(new WeatherForecast(
                Identifier.parse(forecastResult.weatherId()), WeatherType.valueOf(forecastResult.weatherType()),
                forecastResult.displayName(), WeatherPhase.valueOf(forecastResult.phase()),
                WeatherSeverity.valueOf(forecastResult.severity()), forecastResult.etaTicks(),
                forecastResult.regionName(), forecastResult.durationEstimateTicks(), profile.effects(),
                profile.recommendedGear(), forecastResult.shelterRecommendation(),
                WeatherRouteRisk.valueOf(forecastResult.routeRisk()), forecastResult.scannerReliability(),
                forecastResult.echoLines()
            ));
        }
        LAST_FORECASTS.put(playerId, List.copyOf(retainedForecasts));
        return forecasts;
    }

    public static List<EchoWorldContracts.EchoWeatherForecastResult> lastForecastsForPlayer(ServerPlayer player) {
        return player == null ? List.of() : LAST_FORECASTS.getOrDefault(player.getUUID().toString(), List.of());
    }

    public static Map<String, List<EchoWorldContracts.EchoWeatherForecastResult>> lastForecasts() {
        return Map.copyOf(LAST_FORECASTS);
    }

    public static WeatherForecast getCurrentWeatherForPlayer(ServerPlayer player) {
        List<WeatherForecast> forecasts = getForecastForPlayer(player);
        for (WeatherForecast f : forecasts) {
            if (f.phase() == WeatherPhase.ACTIVE || f.phase() == WeatherPhase.CRITICAL) return f;
        }
        return forecasts.isEmpty() ? null : forecasts.get(0);
    }

    public static String formatForecast(WeatherForecast forecast) {
        StringBuilder sb = new StringBuilder();
        sb.append("Weather: ").append(forecast.displayName()).append("\n");
        sb.append("Phase: ").append(forecast.phase()).append("\n");
        sb.append("Severity: ").append(forecast.severity()).append("\n");
        if (forecast.etaTicks() > 0) sb.append("ETA: ").append(forecast.etaTicks() / 20).append("s\n");
        sb.append("Route Risk: ").append(forecast.routeRisk()).append("\n");
        if (!forecast.recommendedGear().isEmpty()) {
            sb.append("Recommended Gear:\n");
            for (Identifier gear : forecast.recommendedGear()) sb.append(" - ").append(gear).append("\n");
        }
        return sb.toString();
    }
}

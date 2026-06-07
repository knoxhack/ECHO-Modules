package com.knoxhack.echoweathercore.api.forecast;

import com.knoxhack.echoweathercore.api.weather.WeatherEffectModifiers;
import com.knoxhack.echoweathercore.api.weather.WeatherPhase;
import com.knoxhack.echoweathercore.api.weather.WeatherRouteRisk;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import java.util.List;
import net.minecraft.resources.Identifier;

public record WeatherForecast(
    Identifier eventId,
    WeatherType type,
    String displayName,
    WeatherPhase phase,
    WeatherSeverity severity,
    long etaTicks,
    String regionName,
    int durationEstimateTicks,
    WeatherEffectModifiers expectedEffects,
    List<Identifier> recommendedGear,
    String shelterRecommendation,
    WeatherRouteRisk routeRisk,
    String scannerReliability,
    List<String> echoLines
) {
    public WeatherForecast {
        if (recommendedGear == null) recommendedGear = List.of();
        if (echoLines == null) echoLines = List.of();
    }
}

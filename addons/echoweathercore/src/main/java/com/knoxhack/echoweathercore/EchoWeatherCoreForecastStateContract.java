package com.knoxhack.echoweathercore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoWeatherCoreForecastStateContract {
    public static final String MODULE_ID = "echoweathercore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echoweathercore:weather/forecast_state_tick";
    public static final String REFERENCE_PROFILE_ID = "ashfall:ash_storm";
    public static final String REFERENCE_REGION_ID = "echoashfallprotocol:wasteland_surface";

    private EchoWeatherCoreForecastStateContract() {
    }

    public static Map<String, Object> executeReferenceForecastTick(String packId) {
        Map<String, Object> tick = new LinkedHashMap<>();
        tick.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        tick.put("service", "echoweathercore:weather_state");
        tick.put("forecastTickExecuted", true);
        tick.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        tick.put("profileId", REFERENCE_PROFILE_ID);
        tick.put("regionId", REFERENCE_REGION_ID);
        tick.put("phase", "ACTIVE");
        tick.put("severity", "SEVERE");
        tick.put("etaTicks", 0);
        tick.put("durationTicks", 7200);
        tick.put("weatherField", Map.of(
                "ashDensity", 0.72D,
                "windSpeed", 22.0D,
                "visibility", 0.35D,
                "temperatureCelsius", 41.0D
        ));
        tick.put("gameplayEffects", Map.of(
                "heatStressDelta", 3.10D,
                "warning", "Ash storm pressure is rising.",
                "routeRisk", "HIGH"
        ));
        tick.put("warnings", List.of(
                warning("weather.warning.ash_storm", "Weather radio warning issued", "weather_radio"),
                warning("weather.warning.shelter", "Shelter recommended before visibility collapse", "portable_shelter_beacon")
        ));
        tick.put("countermeasures", List.of(
                "echoweathercore:ash_filter_wrap",
                "echoweathercore:storm_scanner",
                "echoweathercore:portable_shelter_beacon"
        ));
        tick.put("integrations", Map.of(
                "soundCoreAmbienceId", "echosoundcore:ambience/ash_storm",
                "holoMapLayerId", "echoholomap:layer/weather_ash_storm",
                "terminalWarningPage", "echoterminal:weather/ash_storm_warning"
        ));
        tick.put("diagnostics", List.of(
                "weather.forecast.ready",
                "weather.state.active",
                "weather.warning.broadcast",
                "weather.countermeasures.resolved"
        ));
        tick.put("referenceBehavior", "weathercore_applies_forecast_state_tick");
        return Map.copyOf(tick);
    }

    public static boolean referenceForecastTickPassed(Map<String, Object> tick) {
        return Boolean.TRUE.equals(tick.get("forecastTickExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(tick.get("adapterCoreContract"))
                && REFERENCE_PROFILE_ID.equals(tick.get("profileId"))
                && REFERENCE_REGION_ID.equals(tick.get("regionId"))
                && "ACTIVE".equals(tick.get("phase"))
                && "SEVERE".equals(tick.get("severity"))
                && String.valueOf(tick.get("weatherField")).contains("ashDensity=0.72")
                && String.valueOf(tick.get("gameplayEffects")).contains("heatStressDelta=3.1")
                && String.valueOf(tick.get("warnings")).contains("weather.warning.ash_storm")
                && String.valueOf(tick.get("countermeasures")).contains("echoweathercore:portable_shelter_beacon")
                && String.valueOf(tick.get("integrations")).contains("echosoundcore:ambience/ash_storm");
    }

    private static Map<String, String> warning(String id, String message, String device) {
        Map<String, String> warning = new LinkedHashMap<>();
        warning.put("id", id);
        warning.put("message", message);
        warning.put("device", device);
        return Map.copyOf(warning);
    }
}

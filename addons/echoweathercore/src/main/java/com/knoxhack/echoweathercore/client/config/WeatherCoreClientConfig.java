package com.knoxhack.echoweathercore.client.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class WeatherCoreClientConfig {
    public static final EchoNativeConfigSpec CLIENT_SPEC;

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_WEATHER_PARTICLES;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_WEATHER_SCREEN_EFFECTS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_WEATHER_SOUNDS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_STORM_WARNINGS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_HOLOMAP_WEATHER_OVERLAY;
    public static final EchoNativeConfigSpec.EnumValue<ParticleDensity> WEATHER_PARTICLE_DENSITY;
    public static final EchoNativeConfigSpec.EnumValue<ScreenIntensity> SCREEN_DISTORTION_INTENSITY;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_FORECAST_TOASTS;

    public enum ParticleDensity { LOW, NORMAL, HIGH }
    public enum ScreenIntensity { LOW, NORMAL, HIGH }

    static {
        EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();

        builder.push("visuals");
        ENABLE_WEATHER_PARTICLES = builder.comment("Enable weather particles.").define("enableWeatherParticles", true);
        ENABLE_WEATHER_SCREEN_EFFECTS = builder.comment("Enable weather screen effects.").define("enableWeatherScreenEffects", true);
        ENABLE_WEATHER_SOUNDS = builder.comment("Enable weather sounds.").define("enableWeatherSounds", true);
        ENABLE_STORM_WARNINGS = builder.comment("Enable storm warning overlays.").define("enableStormWarnings", true);
        ENABLE_HOLOMAP_WEATHER_OVERLAY = builder.comment("Enable HoloMap weather overlay.").define("enableHoloMapWeatherOverlay", true);
        WEATHER_PARTICLE_DENSITY = builder.comment("Weather particle density.").defineEnum("weatherParticleDensity", ParticleDensity.NORMAL);
        SCREEN_DISTORTION_INTENSITY = builder.comment("Screen distortion intensity.").defineEnum("screenDistortionIntensity", ScreenIntensity.NORMAL);
        SHOW_FORECAST_TOASTS = builder.comment("Show forecast toasts.").define("showForecastToasts", true);
        builder.pop();

        CLIENT_SPEC = builder.build();
    }

    private WeatherCoreClientConfig() {}
}

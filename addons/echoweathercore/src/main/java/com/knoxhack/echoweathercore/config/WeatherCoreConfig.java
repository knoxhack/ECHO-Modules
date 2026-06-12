package com.knoxhack.echoweathercore.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class WeatherCoreConfig {
    public static final EchoNativeConfigSpec SERVER_SPEC;
    public static final EchoNativeConfigSpec CLIENT_SPEC;

    // Server toggles
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_WEATHER_CORE;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ASH_STORMS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_TOXIC_RAIN;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_RADIATION_STORMS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_CRYO_FRONTS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_HEAT_SURGES;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_NEXUS_SIGNAL_STORMS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ORBITAL_DEBRIS_SHOWERS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ELECTROMAGNETIC_BLACKOUTS;

    public static final EchoNativeConfigSpec.BooleanValue ALLOW_WEATHER_DAMAGE;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_MACHINE_WEAR;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_POWER_GRID_DISRUPTION;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_DRONE_WEATHER_EFFECTS;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_MOB_BEHAVIOR_CHANGES;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_FACTION_PATROL_RETREAT;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_WEATHER_RESOURCE_SPAWNS;

    public static final EchoNativeConfigSpec.EnumValue<Frequency> GLOBAL_WEATHER_FREQUENCY;
    public static final EchoNativeConfigSpec.IntValue MINIMUM_WARNING_TICKS;
    public static final EchoNativeConfigSpec.IntValue MAX_SIMULTANEOUS_REGIONAL_EVENTS;
    public static final EchoNativeConfigSpec.BooleanValue EARLY_GAME_SEVERE_WEATHER;
    public static final EchoNativeConfigSpec.IntValue WEATHER_CHECK_INTERVAL_TICKS;

    public static final EchoNativeConfigSpec.IntValue ASH_STORM_BASE_WEIGHT;
    public static final EchoNativeConfigSpec.IntValue TOXIC_RAIN_BASE_WEIGHT;
    public static final EchoNativeConfigSpec.IntValue RADIATION_STORM_BASE_WEIGHT;
    public static final EchoNativeConfigSpec.IntValue CRYO_FRONT_BASE_WEIGHT;
    public static final EchoNativeConfigSpec.IntValue HEAT_SURGE_BASE_WEIGHT;
    public static final EchoNativeConfigSpec.IntValue NEXUS_SIGNAL_STORM_BASE_WEIGHT;
    public static final EchoNativeConfigSpec.IntValue ORBITAL_DEBRIS_SHOWER_BASE_WEIGHT;
    public static final EchoNativeConfigSpec.IntValue ELECTROMAGNETIC_BLACKOUT_BASE_WEIGHT;

    // Client toggles
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_WEATHER_PARTICLES;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_WEATHER_SCREEN_EFFECTS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_WEATHER_SOUNDS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_STORM_WARNINGS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_HOLOMAP_WEATHER_OVERLAY;
    public static final EchoNativeConfigSpec.EnumValue<ParticleDensity> WEATHER_PARTICLE_DENSITY;
    public static final EchoNativeConfigSpec.EnumValue<ScreenIntensity> SCREEN_DISTORTION_INTENSITY;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_FORECAST_TOASTS;

    public enum Frequency { LOW, NORMAL, HIGH, EXTREME, DISABLED }
    public enum ParticleDensity { LOW, NORMAL, HIGH }
    public enum ScreenIntensity { LOW, NORMAL, HIGH }

    static {
        EchoNativeConfigSpec.Builder server = new EchoNativeConfigSpec.Builder();
        server.push("toggles");
        ENABLE_WEATHER_CORE = server.comment("Enable WeatherCore systems.").define("enableWeatherCore", true);
        ENABLE_ASH_STORMS = server.comment("Enable ash storms.").define("enableAshStorms", true);
        ENABLE_TOXIC_RAIN = server.comment("Enable toxic rain.").define("enableToxicRain", true);
        ENABLE_RADIATION_STORMS = server.comment("Enable radiation storms.").define("enableRadiationStorms", true);
        ENABLE_CRYO_FRONTS = server.comment("Enable cryo fronts.").define("enableCryoFronts", true);
        ENABLE_HEAT_SURGES = server.comment("Enable heat surges.").define("enableHeatSurges", true);
        ENABLE_NEXUS_SIGNAL_STORMS = server.comment("Enable Nexus signal storms.").define("enableNexusSignalStorms", true);
        ENABLE_ORBITAL_DEBRIS_SHOWERS = server.comment("Enable orbital debris showers.").define("enableOrbitalDebrisShowers", true);
        ENABLE_ELECTROMAGNETIC_BLACKOUTS = server.comment("Enable electromagnetic blackouts.").define("enableElectromagneticBlackouts", true);
        server.pop();

        server.push("behavior");
        ALLOW_WEATHER_DAMAGE = server.comment("Allow direct weather damage to players.").define("allowWeatherDamage", false);
        ALLOW_MACHINE_WEAR = server.comment("Allow weather-driven machine wear.").define("allowMachineWear", false);
        ALLOW_POWER_GRID_DISRUPTION = server.comment("Allow weather-driven power grid disruption.").define("allowPowerGridDisruption", true);
        ALLOW_DRONE_WEATHER_EFFECTS = server.comment("Allow weather effects on drones.").define("allowDroneWeatherEffects", true);
        ALLOW_MOB_BEHAVIOR_CHANGES = server.comment("Allow weather-driven mob behavior changes.").define("allowMobBehaviorChanges", true);
        ALLOW_FACTION_PATROL_RETREAT = server.comment("Allow faction patrol retreat during severe weather.").define("allowFactionPatrolRetreat", true);
        ALLOW_WEATHER_RESOURCE_SPAWNS = server.comment("Allow weather resource spawns.").define("allowWeatherResourceSpawns", true);
        server.pop();

        server.push("scheduling");
        GLOBAL_WEATHER_FREQUENCY = server.comment("Global weather event frequency.").defineEnum("globalWeatherFrequency", Frequency.NORMAL);
        MINIMUM_WARNING_TICKS = server.comment("Minimum warning ticks before weather starts.").defineInRange("minimumWarningTicks", 2400, 200, 24000);
        MAX_SIMULTANEOUS_REGIONAL_EVENTS = server.comment("Maximum simultaneous regional weather events.").defineInRange("maxSimultaneousRegionalEvents", 2, 0, 8);
        EARLY_GAME_SEVERE_WEATHER = server.comment("Allow severe weather early in the game.").define("earlyGameSevereWeather", false);
        WEATHER_CHECK_INTERVAL_TICKS = server.comment("Ticks between weather scheduling checks.").defineInRange("weatherCheckIntervalTicks", 1200, 200, 24000);
        server.pop();

        server.push("weights");
        ASH_STORM_BASE_WEIGHT = server.comment("Base weight for ash storms.").defineInRange("ashStormBaseWeight", 40, 0, 1000);
        TOXIC_RAIN_BASE_WEIGHT = server.comment("Base weight for toxic rain.").defineInRange("toxicRainBaseWeight", 30, 0, 1000);
        RADIATION_STORM_BASE_WEIGHT = server.comment("Base weight for radiation storms.").defineInRange("radiationStormBaseWeight", 20, 0, 1000);
        CRYO_FRONT_BASE_WEIGHT = server.comment("Base weight for cryo fronts.").defineInRange("cryoFrontBaseWeight", 20, 0, 1000);
        HEAT_SURGE_BASE_WEIGHT = server.comment("Base weight for heat surges.").defineInRange("heatSurgeBaseWeight", 20, 0, 1000);
        NEXUS_SIGNAL_STORM_BASE_WEIGHT = server.comment("Base weight for Nexus signal storms.").defineInRange("nexusSignalStormBaseWeight", 10, 0, 1000);
        ORBITAL_DEBRIS_SHOWER_BASE_WEIGHT = server.comment("Base weight for orbital debris showers.").defineInRange("orbitalDebrisShowerBaseWeight", 8, 0, 1000);
        ELECTROMAGNETIC_BLACKOUT_BASE_WEIGHT = server.comment("Base weight for EM blackouts.").defineInRange("electromagneticBlackoutBaseWeight", 12, 0, 1000);
        server.pop();

        SERVER_SPEC = server.build();

        EchoNativeConfigSpec.Builder client = new EchoNativeConfigSpec.Builder();
        client.push("visuals");
        ENABLE_WEATHER_PARTICLES = client.comment("Enable weather particles.").define("enableWeatherParticles", true);
        ENABLE_WEATHER_SCREEN_EFFECTS = client.comment("Enable weather screen effects.").define("enableWeatherScreenEffects", true);
        ENABLE_WEATHER_SOUNDS = client.comment("Enable weather sounds.").define("enableWeatherSounds", true);
        ENABLE_STORM_WARNINGS = client.comment("Enable storm warning overlays.").define("enableStormWarnings", true);
        ENABLE_HOLOMAP_WEATHER_OVERLAY = client.comment("Enable HoloMap weather overlay.").define("enableHoloMapWeatherOverlay", true);
        WEATHER_PARTICLE_DENSITY = client.comment("Weather particle density.").defineEnum("weatherParticleDensity", ParticleDensity.NORMAL);
        SCREEN_DISTORTION_INTENSITY = client.comment("Screen distortion intensity.").defineEnum("screenDistortionIntensity", ScreenIntensity.NORMAL);
        SHOW_FORECAST_TOASTS = client.comment("Show forecast toasts.").define("showForecastToasts", true);
        client.pop();

        CLIENT_SPEC = client.build();
    }

    private WeatherCoreConfig() {}
}

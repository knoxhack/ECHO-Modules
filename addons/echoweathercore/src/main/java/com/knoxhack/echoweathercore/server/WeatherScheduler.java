package com.knoxhack.echoweathercore.server;

import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.weather.WeatherProfile;
import com.knoxhack.echoweathercore.api.weather.WeatherScope;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import com.knoxhack.echoweathercore.config.WeatherCoreConfig;
import com.knoxhack.echoweathercore.data.WeatherDataReloadListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class WeatherScheduler {
    private static final Random RANDOM = new Random();
    private static long lastCheckTick = -1;

    private WeatherScheduler() {}

    public static void tick(Level level) {
        if (level.isClientSide() || !(level instanceof ServerLevel sl)) return;
        if (!WeatherCoreConfig.ENABLE_WEATHER_CORE.get()) return;

        long tick = level.getGameTime();
        int interval = WeatherCoreConfig.WEATHER_CHECK_INTERVAL_TICKS.get();
        if (tick - lastCheckTick < interval) return;
        lastCheckTick = tick;

        if (WeatherCoreConfig.GLOBAL_WEATHER_FREQUENCY.get() == WeatherCoreConfig.Frequency.DISABLED) return;

        Map<Identifier, WeatherProfile> profiles = WeatherDataReloadListener.INSTANCE.getProfiles();
        if (profiles.isEmpty()) return;

        List<WeatherProfile> candidates = new ArrayList<>();
        for (WeatherProfile profile : profiles.values()) {
            if (!profile.enabled()) continue;
            if (!isEnabledByConfig(profile.type())) continue;
            if (!profile.allowedDimensions().isEmpty() && !profile.allowedDimensions().contains(level.dimension().identifier())) continue;
            candidates.add(profile);
        }

        if (candidates.isEmpty()) return;

        int activeRegional = countRegionalEvents(sl);
        if (activeRegional >= WeatherCoreConfig.MAX_SIMULTANEOUS_REGIONAL_EVENTS.get()) return;

        int totalWeight = candidates.stream().mapToInt(WeatherProfile::weight).sum();
        if (totalWeight <= 0) return;

        double freqMultiplier = switch (WeatherCoreConfig.GLOBAL_WEATHER_FREQUENCY.get()) {
            case LOW -> 0.3;
            case NORMAL -> 0.6;
            case HIGH -> 1.0;
            case EXTREME -> 1.5;
            default -> 0.0;
        };

        if (RANDOM.nextDouble() > freqMultiplier) return;

        int roll = RANDOM.nextInt(totalWeight);
        WeatherProfile selected = null;
        for (WeatherProfile candidate : candidates) {
            roll -= candidate.weight();
            if (roll < 0) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) selected = candidates.get(candidates.size() - 1);

        WeatherSeverity severity = selected.defaultSeverity();
        if (!WeatherCoreConfig.EARLY_GAME_SEVERE_WEATHER.get() && severity.ordinal() >= WeatherSeverity.SEVERE.ordinal()) {
            severity = WeatherSeverity.MODERATE;
        }

        BlockPos center = new BlockPos(sl.getRespawnData().pos());
        int radius = selected.scope() == WeatherScope.LOCAL ? 800 : selected.scope() == WeatherScope.REGIONAL ? 2400 : 0;

        WeatherStateManager.getInstance().startEvent(sl, selected, severity, center, radius, "scheduler");
    }

    private static boolean isEnabledByConfig(WeatherType type) {
        return switch (type) {
            case ASH_STORM -> WeatherCoreConfig.ENABLE_ASH_STORMS.get();
            case TOXIC_RAIN -> WeatherCoreConfig.ENABLE_TOXIC_RAIN.get();
            case RADIATION_STORM -> WeatherCoreConfig.ENABLE_RADIATION_STORMS.get();
            case CRYO_FRONT -> WeatherCoreConfig.ENABLE_CRYO_FRONTS.get();
            case HEAT_SURGE -> WeatherCoreConfig.ENABLE_HEAT_SURGES.get();
            case NEXUS_SIGNAL_STORM -> WeatherCoreConfig.ENABLE_NEXUS_SIGNAL_STORMS.get();
            case ORBITAL_DEBRIS_SHOWER -> WeatherCoreConfig.ENABLE_ORBITAL_DEBRIS_SHOWERS.get();
            case ELECTROMAGNETIC_BLACKOUT -> WeatherCoreConfig.ENABLE_ELECTROMAGNETIC_BLACKOUTS.get();
            default -> false;
        };
    }

    private static int countRegionalEvents(ServerLevel level) {
        int count = 0;
        for (var event : WeatherStateManager.getInstance().getActiveEvents(level)) {
            if (event.scope() == WeatherScope.REGIONAL || event.scope() == WeatherScope.LOCAL) count++;
        }
        return count;
    }
}

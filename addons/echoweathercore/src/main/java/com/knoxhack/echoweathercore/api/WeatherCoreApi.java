package com.knoxhack.echoweathercore.api;

import com.knoxhack.echo.adaptercore.EchoNativeWeatherExposureMitigationBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWeatherRouteRiskBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.knoxhack.echoweathercore.api.forecast.WeatherForecast;
import com.knoxhack.echoweathercore.api.weather.ActiveWeatherEvent;
import com.knoxhack.echoweathercore.api.weather.WeatherEffectModifiers;
import com.knoxhack.echoweathercore.api.weather.WeatherPhase;
import com.knoxhack.echoweathercore.api.weather.WeatherProfile;
import com.knoxhack.echoweathercore.api.weather.WeatherRouteRisk;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import com.knoxhack.echoweathercore.config.WeatherCoreConfig;
import com.knoxhack.echoweathercore.data.WeatherDataReloadListener;
import com.knoxhack.echoweathercore.server.WeatherCountermeasureManager;
import com.knoxhack.echoweathercore.server.WeatherForecastManager;
import com.knoxhack.echoweathercore.server.WeatherStateManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class WeatherCoreApi {
    private static final List<Consumer<ActiveWeatherEvent>> listeners = new ArrayList<>();

    private WeatherCoreApi() {}

    public static List<ActiveWeatherEvent> getCurrentWeather(Level level, BlockPos pos) {
        if (level.isClientSide()) return List.of();
        return WeatherStateManager.getInstance().getEventsAt(level, pos);
    }

    public static List<ActiveWeatherEvent> getActiveWeather(Level level) {
        if (level.isClientSide()) return List.of();
        return WeatherStateManager.getInstance().getActiveEvents(level);
    }

    public static List<WeatherForecast> getForecast(ServerPlayer player) {
        if (player.level().isClientSide()) return List.of();
        return WeatherForecastManager.getForecastForPlayer(player);
    }

    public static boolean isWeatherActive(Level level, WeatherType type) {
        if (level.isClientSide()) return false;
        for (ActiveWeatherEvent event : getActiveWeather(level)) {
            if (event.type() == type) return true;
        }
        return false;
    }

    public static WeatherSeverity getWeatherSeverity(Level level, BlockPos pos) {
        WeatherSeverity max = null;
        for (ActiveWeatherEvent event : getCurrentWeather(level, pos)) {
            if (max == null || event.severity().ordinal() > max.ordinal()) max = event.severity();
        }
        return max != null ? max : WeatherSeverity.LOW;
    }

    public static WeatherPhase getWeatherPhase(Level level, BlockPos pos) {
        WeatherPhase dominant = WeatherPhase.ENDED;
        for (ActiveWeatherEvent event : getCurrentWeather(level, pos)) {
            if (event.phase().ordinal() > dominant.ordinal()) dominant = event.phase();
        }
        return dominant;
    }

    public static WeatherEffectModifiers getWeatherModifiers(Level level, BlockPos pos) {
        WeatherEffectModifiers combined = WeatherEffectModifiers.DEFAULT;
        boolean sheltered = isSheltered(level, pos);
        for (ActiveWeatherEvent event : getCurrentWeather(level, pos)) {
            WeatherProfile profile = WeatherDataReloadListener.INSTANCE.getProfile(event.profileId());
            if (profile != null) {
                WeatherEffectModifiers effects = applyExposureMitigation(level, pos, event, profile.effects(), sheltered);
                combined = combined.merge(effects);
            }
        }
        return combined;
    }

    public static double getScannerReliability(ServerPlayer player) {
        WeatherEffectModifiers mods = getWeatherModifiers(player.level(), player.blockPosition());
        return mods.scannerReliabilityMultiplier();
    }

    public static double getScannerRangeMultiplier(ServerPlayer player) {
        WeatherEffectModifiers mods = getWeatherModifiers(player.level(), player.blockPosition());
        return mods.scannerRangeMultiplier();
    }

    public static double getFilterDrainMultiplier(ServerPlayer player) {
        WeatherEffectModifiers mods = getWeatherModifiers(player.level(), player.blockPosition());
        return mods.filterDrainMultiplier();
    }

    public static double getPowerGridInstability(Level level, BlockPos pos) {
        if (!WeatherCoreConfig.ALLOW_POWER_GRID_DISRUPTION.get()) return 1.0;
        WeatherEffectModifiers mods = getWeatherModifiers(level, pos);
        return mods.powerGridInstabilityMultiplier();
    }

    public static WeatherRouteRisk getRouteWeatherRisk(ServerPlayer player, Identifier routeId) {
        return getRouteWeatherRisk(player.level(), player.blockPosition(), null);
    }

    public static WeatherRouteRisk getRouteWeatherRisk(Level level, BlockPos start, BlockPos end) {
        WeatherSeverity severity = getWeatherSeverity(level, start);
        WeatherEffectModifiers modifiers = getWeatherModifiers(level, start);
        return routeRisk(severity, modifiers.routeRiskModifier());
    }

    public static List<Identifier> getRecommendedGear(ServerPlayer player) {
        List<Identifier> gear = new ArrayList<>();
        for (ActiveWeatherEvent event : getCurrentWeather(player.level(), player.blockPosition())) {
            WeatherProfile profile = WeatherDataReloadListener.INSTANCE.getProfile(event.profileId());
            if (profile != null) gear.addAll(profile.recommendedGear());
        }
        return Collections.unmodifiableList(gear);
    }

    public static boolean isSheltered(Entity entity) {
        boolean sheltered = isSheltered(entity.level(), entity.blockPosition());
        if (sheltered && entity instanceof ServerPlayer player) {
            reportShelterEntered(player, entity.blockPosition());
        }
        return sheltered;
    }

    public static boolean isSheltered(Level level, BlockPos pos) {
        return !level.canSeeSky(pos.above());
    }

    public static void registerWeatherProfile(WeatherProfile profile) {
        WeatherDataReloadListener.INSTANCE.registerProfile(profile);
    }

    public static ActiveWeatherEvent triggerWeather(ServerLevel level, Identifier profileId, WeatherSeverity severity, BlockPos center, int radius) {
        WeatherProfile profile = WeatherDataReloadListener.INSTANCE.getProfile(profileId);
        if (profile == null) return null;
        return WeatherStateManager.getInstance().startEvent(level, profile, severity, center, radius, "api");
    }

    public static void clearWeather(ServerLevel level, WeatherType type) {
        WeatherStateManager.getInstance().clearEvents(level, type);
    }

    public static void clearAllWeather(ServerLevel level) {
        WeatherStateManager.getInstance().clearAllEvents(level);
    }

    public static void addWeatherListener(Consumer<ActiveWeatherEvent> listener) {
        listeners.add(listener);
    }

    public static void registerWeatherCountermeasure(WeatherType type, WeatherEffectModifiers modifiers) {
        WeatherCountermeasureManager.registerCountermeasure(type, modifiers);
    }

    public static void reportShelterEntered(ServerPlayer player, BlockPos shelterPos) {
        WeatherCountermeasureManager.reportShelterEntered(player, shelterPos, "WeatherCoreApi.reportShelterEntered");
    }

    public static String getDroneWeatherRisk(ServerPlayer player) {
        if (!WeatherCoreConfig.ALLOW_DRONE_WEATHER_EFFECTS.get()) return "Clear";
        WeatherEffectModifiers mods = getWeatherModifiers(player.level(), player.blockPosition());
        if (mods.droneScoutReliability() < 0.5) return "High Risk";
        if (mods.droneScoutReliability() < 0.8) return "Moderate Risk";
        return "Low Risk";
    }

    public static double getFactionWeatherActivity(String factionId, WeatherType weather) {
        if (!WeatherCoreConfig.ALLOW_FACTION_PATROL_RETREAT.get()) return 1.0;
        WeatherEffectModifiers mods = WeatherCountermeasureManager.getCountermeasureModifiers(weather);
        return mods.factionPatrolActivityMultiplier();
    }

    public static List<String> getLensRows(Level level, BlockPos pos) {
        List<String> rows = new ArrayList<>();
        for (ActiveWeatherEvent event : getCurrentWeather(level, pos)) {
            WeatherProfile profile = WeatherDataReloadListener.INSTANCE.getProfile(event.profileId());
            if (profile != null) {
                rows.add("Event: " + profile.displayName());
                rows.add("Intensity: " + event.severity());
                rows.add("Filter Drain: " + (int) ((profile.effects().filterDrainMultiplier() - 1.0) * 100) + "%");
                rows.add("Scanner Reliability: " + (int) (profile.effects().scannerReliabilityMultiplier() * 100) + "%");
            }
        }
        return rows;
    }

    private static WeatherEffectModifiers applyExposureMitigation(Level level,
            BlockPos pos,
            ActiveWeatherEvent event,
            WeatherEffectModifiers effects,
            boolean sheltered) {
        WeatherEffectModifiers countermeasure = WeatherCountermeasureManager.getCountermeasureModifiers(event.type());
        EchoWorldContracts.EchoWeatherExposureMitigationResult result =
                new EchoNativeWeatherExposureMitigationBridge("echoweathercore").mitigate(
                        new EchoWorldContracts.EchoWeatherExposureMitigationRequest(
                                exposurePlayerId(level, pos),
                                event.profileId().toString(),
                                event.type().name(),
                                sheltered,
                                level == null ? 0L : Math.max(0L, level.getGameTime()),
                                "WeatherCoreApi.getWeatherModifiers",
                                exposureModifier(event.type(), effects),
                                exposureModifier(event.type(), countermeasure)));
        Map<String, Object> state = result.modifierState();
        return new WeatherEffectModifiers(
                effects.visibilityMultiplier(),
                effects.scannerRangeMultiplier(),
                effects.scannerReliabilityMultiplier(),
                effects.holomapReliabilityMultiplier(),
                number(state, "filterDrainMultiplier", effects.filterDrainMultiplier()),
                number(state, "radiationExposureMultiplier", effects.radiationExposureMultiplier()),
                number(state, "toxicExposureMultiplier", effects.toxicExposureMultiplier()),
                number(state, "coldExposureMultiplier", effects.coldExposureMultiplier()),
                number(state, "heatExposureMultiplier", effects.heatExposureMultiplier()),
                effects.hydrationDrainMultiplier(),
                effects.solarPowerMultiplier(),
                effects.powerGridInstabilityMultiplier(),
                effects.batteryEfficiencyMultiplier(),
                effects.machineHeatMultiplier(),
                effects.droneScoutReliability(),
                effects.droneRecallRisk(),
                effects.mobSightMultiplier(),
                effects.mobAggressionMultiplier(),
                effects.factionPatrolActivityMultiplier(),
                number(state, "routeRiskModifier", effects.routeRiskModifier()));
    }

    private static EchoWorldContracts.EchoWeatherExposureModifier exposureModifier(WeatherType type,
            WeatherEffectModifiers modifiers) {
        return new EchoWorldContracts.EchoWeatherExposureModifier(
                type.name(),
                modifiers.filterDrainMultiplier(),
                modifiers.radiationExposureMultiplier(),
                modifiers.toxicExposureMultiplier(),
                modifiers.coldExposureMultiplier(),
                modifiers.heatExposureMultiplier(),
                modifiers.routeRiskModifier());
    }

    private static String exposurePlayerId(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return "";
        }
        return level.dimension().identifier() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static double number(Map<String, Object> state, String key, double fallback) {
        Object value = state.get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static WeatherRouteRisk routeRisk(WeatherSeverity severity, double routeRiskModifier) {
        EchoWorldContracts.EchoWeatherRouteRiskResult result =
                new EchoNativeWeatherRouteRiskBridge("echoweathercore").evaluate(
                        new EchoWorldContracts.EchoWeatherRouteRiskRequest(
                                "",
                                "",
                                severity.name(),
                                routeRiskModifier,
                                0L,
                                "WeatherCoreApi.getRouteWeatherRisk"));
        return WeatherRouteRisk.valueOf(result.risk());
    }
}

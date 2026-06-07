package com.knoxhack.echoweathercore.blockentity;

import com.knoxhack.echo.adaptercore.EchoNativeWeatherStationBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.WeatherCoreApi;
import com.knoxhack.echoweathercore.api.forecast.WeatherForecast;
import com.knoxhack.echoweathercore.api.weather.WeatherRouteRisk;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.registry.WeatherCoreBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WeatherStationBlockEntity extends BlockEntity {
    private static final Map<UUID, EchoWorldContracts.EchoWeatherStationUseResult> LAST_STATION_USES =
            new ConcurrentHashMap<>();
    private static final Map<String, EchoWorldContracts.EchoWeatherStationUseResult> STATION_POSITIONS =
            new ConcurrentHashMap<>();

    public WeatherStationBlockEntity(BlockPos pos, BlockState state) {
        super(WeatherCoreBlockEntities.WEATHER_STATION.get(), pos, state);
    }

    public void onUse(Player player) {
        if (level == null || level.isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        resolveStationUse(level, worldPosition, sp, WeatherCoreApi.getForecast(sp), "WeatherStationBlockEntity.onUse")
                .ifPresent(result -> result.messageLines()
                        .forEach(line -> player.sendSystemMessage(Component.literal(line))));
    }

    public static Optional<EchoWorldContracts.EchoWeatherStationUseResult> resolveStationUse(Level level,
            BlockPos pos,
            ServerPlayer player,
            List<WeatherForecast> forecasts,
            String sourceReason) {
        if (level == null || pos == null || player == null || level.isClientSide()) {
            return Optional.empty();
        }
        List<WeatherForecast> safeForecasts = forecasts == null ? List.of() : forecasts;
        List<String> weatherIds = new ArrayList<>();
        List<String> forecastLines = new ArrayList<>();
        WeatherSeverity strongestSeverity = WeatherSeverity.LOW;
        WeatherRouteRisk strongestRisk = WeatherRouteRisk.SAFE;
        for (WeatherForecast forecast : safeForecasts) {
            weatherIds.add(forecast.eventId().toString());
            forecastLines.add(" - " + forecast.displayName() + " [" + forecast.phase() + "]");
            if (forecast.severity().ordinal() > strongestSeverity.ordinal()) {
                strongestSeverity = forecast.severity();
            }
            if (forecast.routeRisk().ordinal() > strongestRisk.ordinal()) {
                strongestRisk = forecast.routeRisk();
            }
        }
        EchoWorldContracts.EchoWeatherStationUseResult result =
                new EchoNativeWeatherStationBridge(EchoWeatherCore.MODID).use(
                        new EchoWorldContracts.EchoWeatherStationUseRequest(
                                player.getUUID().toString(),
                                weatherIds,
                                forecastLines,
                                !safeForecasts.isEmpty(),
                                strongestSeverity.name(),
                                strongestRisk.name(),
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                Math.max(0L, level.getGameTime()),
                                sourceReason));
        LAST_STATION_USES.put(player.getUUID(), result);
        STATION_POSITIONS.put(stationKey(level, pos), result);
        return Optional.of(result);
    }

    public static Optional<EchoWorldContracts.EchoWeatherStationUseResult> lastStationUse(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(LAST_STATION_USES.get(player.getUUID()));
    }

    public static Map<UUID, EchoWorldContracts.EchoWeatherStationUseResult> lastStationUses() {
        return Map.copyOf(LAST_STATION_USES);
    }

    public static Map<String, EchoWorldContracts.EchoWeatherStationUseResult> stationPositions() {
        return Map.copyOf(STATION_POSITIONS);
    }

    public static void clearForTests() {
        LAST_STATION_USES.clear();
        STATION_POSITIONS.clear();
    }

    private static String stationKey(Level level, BlockPos pos) {
        return level.dimension().identifier() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}

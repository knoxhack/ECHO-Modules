package com.knoxhack.echoweathercore.block;

import com.knoxhack.echo.adaptercore.EchoNativeClimateSensorBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.WeatherCoreApi;
import com.knoxhack.echoweathercore.api.weather.ActiveWeatherEvent;
import com.knoxhack.echoweathercore.api.weather.WeatherEffectModifiers;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ClimateSensorBlock extends Block {
    private static final Map<UUID, EchoWorldContracts.EchoClimateSensorReadResult> LAST_SENSOR_READINGS =
            new ConcurrentHashMap<>();
    private static final Map<String, EchoWorldContracts.EchoClimateSensorReadResult> SENSOR_POSITIONS =
            new ConcurrentHashMap<>();

    public ClimateSensorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        resolveClimateReading(level, pos, player, "ClimateSensorBlock.useWithoutItem")
                .ifPresent(result -> result.messageLines()
                        .forEach(line -> player.sendSystemMessage(Component.literal(line))));
        return InteractionResult.SUCCESS;
    }

    public static Optional<EchoWorldContracts.EchoClimateSensorReadResult> resolveClimateReading(Level level,
            BlockPos pos,
            Player player,
            String sourceReason) {
        if (level == null || pos == null || player == null || level.isClientSide()) {
            return Optional.empty();
        }
        WeatherEffectModifiers modifiers = WeatherCoreApi.getWeatherModifiers(level, pos);
        boolean sheltered = WeatherCoreApi.isSheltered(level, pos);
        List<String> weatherIds = WeatherCoreApi.getCurrentWeather(level, pos).stream()
                .map(ActiveWeatherEvent::profileId)
                .map(Object::toString)
                .toList();
        EchoWorldContracts.EchoClimateSensorReadResult result =
                new EchoNativeClimateSensorBridge(EchoWeatherCore.MODID).read(
                        new EchoWorldContracts.EchoClimateSensorReadRequest(
                                player.getUUID().toString(),
                                weatherIds,
                                sheltered,
                                modifiers.visibilityMultiplier(),
                                modifiers.scannerReliabilityMultiplier(),
                                modifiers.filterDrainMultiplier(),
                                modifiers.toxicExposureMultiplier(),
                                modifiers.routeRiskModifier(),
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                Math.max(0L, level.getGameTime()),
                                sourceReason));
        LAST_SENSOR_READINGS.put(player.getUUID(), result);
        SENSOR_POSITIONS.put(sensorKey(level, pos), result);
        return Optional.of(result);
    }

    public static Optional<EchoWorldContracts.EchoClimateSensorReadResult> lastClimateReading(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(LAST_SENSOR_READINGS.get(player.getUUID()));
    }

    public static Map<UUID, EchoWorldContracts.EchoClimateSensorReadResult> lastClimateReadings() {
        return Map.copyOf(LAST_SENSOR_READINGS);
    }

    public static Map<String, EchoWorldContracts.EchoClimateSensorReadResult> sensorPositions() {
        return Map.copyOf(SENSOR_POSITIONS);
    }

    public static void clearForTests() {
        LAST_SENSOR_READINGS.clear();
        SENSOR_POSITIONS.clear();
    }

    private static String sensorKey(Level level, BlockPos pos) {
        return level.dimension().identifier() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}

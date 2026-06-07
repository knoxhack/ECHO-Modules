package com.knoxhack.echoweathercore.block;

import com.knoxhack.echo.adaptercore.EchoNativeEmergencySirenBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.WeatherCoreApi;
import com.knoxhack.echoweathercore.api.weather.ActiveWeatherEvent;
import com.knoxhack.echoweathercore.api.weather.WeatherPhase;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import java.util.ArrayList;
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

public class EmergencySirenBlock extends Block {
    private static final Map<UUID, EchoWorldContracts.EchoEmergencySirenUseResult> LAST_SIREN_USES =
            new ConcurrentHashMap<>();
    private static final Map<String, EchoWorldContracts.EchoEmergencySirenUseResult> SIREN_POSTS =
            new ConcurrentHashMap<>();

    public EmergencySirenBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        resolveSirenUse(level, pos, player, "EmergencySirenBlock.useWithoutItem")
                .ifPresent(result -> player.sendSystemMessage(Component.literal(result.message())));
        return InteractionResult.SUCCESS;
    }

    public static Optional<EchoWorldContracts.EchoEmergencySirenUseResult> resolveSirenUse(Level level,
            BlockPos pos,
            Player player,
            String sourceReason) {
        if (level == null || pos == null || player == null || level.isClientSide()) {
            return Optional.empty();
        }
        List<ActiveWeatherEvent> activeEvents = WeatherCoreApi.getActiveWeather(level);
        boolean active = false;
        for (WeatherType type : WeatherType.values()) {
            if (WeatherCoreApi.isWeatherActive(level, type)) {
                active = true;
                break;
            }
        }
        WeatherPhase phase = activeEvents.isEmpty() ? WeatherPhase.ENDED : activeEvents.get(0).phase();
        WeatherSeverity severity = WeatherSeverity.LOW;
        List<String> weatherIds = new ArrayList<>();
        for (ActiveWeatherEvent event : activeEvents) {
            weatherIds.add(event.profileId().toString());
            if (event.severity().ordinal() > severity.ordinal()) {
                severity = event.severity();
            }
            if (event.affectsPosition(pos)) {
                phase = event.phase();
            }
        }
        EchoWorldContracts.EchoEmergencySirenUseResult result =
                new EchoNativeEmergencySirenBridge(EchoWeatherCore.MODID).use(
                        new EchoWorldContracts.EchoEmergencySirenUseRequest(
                                player.getUUID().toString(),
                                weatherIds,
                                active,
                                phase.name(),
                                severity.name(),
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                Math.max(0L, level.getGameTime()),
                                sourceReason));
        LAST_SIREN_USES.put(player.getUUID(), result);
        SIREN_POSTS.put(sirenKey(level, pos), result);
        return Optional.of(result);
    }

    public static Optional<EchoWorldContracts.EchoEmergencySirenUseResult> lastSirenUse(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(LAST_SIREN_USES.get(player.getUUID()));
    }

    public static Map<UUID, EchoWorldContracts.EchoEmergencySirenUseResult> lastSirenUses() {
        return Map.copyOf(LAST_SIREN_USES);
    }

    public static Map<String, EchoWorldContracts.EchoEmergencySirenUseResult> sirenPosts() {
        return Map.copyOf(SIREN_POSTS);
    }

    public static void clearForTests() {
        LAST_SIREN_USES.clear();
        SIREN_POSTS.clear();
    }

    private static String sirenKey(Level level, BlockPos pos) {
        return level.dimension().identifier() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}

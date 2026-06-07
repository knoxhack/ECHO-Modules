package com.knoxhack.echoweathercore.block;

import com.knoxhack.echo.adaptercore.EchoNativeRouteWarningPostBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.WeatherCoreApi;
import com.knoxhack.echoweathercore.api.weather.ActiveWeatherEvent;
import com.knoxhack.echoweathercore.api.weather.WeatherEffectModifiers;
import com.knoxhack.echoweathercore.api.weather.WeatherRouteRisk;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
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

public class RouteWarningPostBlock extends Block {
    private static final Map<UUID, EchoWorldContracts.EchoRouteWarningPostUseResult> LAST_ROUTE_WARNINGS =
            new ConcurrentHashMap<>();
    private static final Map<String, EchoWorldContracts.EchoRouteWarningPostUseResult> ROUTE_WARNING_POSTS =
            new ConcurrentHashMap<>();

    public RouteWarningPostBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        resolveRouteWarning(level, pos, player, "RouteWarningPostBlock.useWithoutItem")
                .ifPresent(result -> player.sendSystemMessage(Component.literal(result.message())));
        return InteractionResult.SUCCESS;
    }

    public static Optional<EchoWorldContracts.EchoRouteWarningPostUseResult> resolveRouteWarning(Level level,
            BlockPos pos,
            Player player,
            String sourceReason) {
        if (level == null || pos == null || player == null || level.isClientSide()) {
            return Optional.empty();
        }
        WeatherRouteRisk risk = WeatherCoreApi.getRouteWeatherRisk(level, pos, null);
        WeatherSeverity severity = WeatherCoreApi.getWeatherSeverity(level, pos);
        WeatherEffectModifiers modifiers = WeatherCoreApi.getWeatherModifiers(level, pos);
        String weatherId = WeatherCoreApi.getCurrentWeather(level, pos).stream()
                .findFirst()
                .map(ActiveWeatherEvent::profileId)
                .map(Object::toString)
                .orElse("");
        EchoWorldContracts.EchoRouteWarningPostUseResult result =
                new EchoNativeRouteWarningPostBridge(EchoWeatherCore.MODID).use(
                        new EchoWorldContracts.EchoRouteWarningPostUseRequest(
                                player.getUUID().toString(),
                                weatherId,
                                severity.name(),
                                risk.name(),
                                modifiers.routeRiskModifier(),
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                Math.max(0L, level.getGameTime()),
                                sourceReason));
        LAST_ROUTE_WARNINGS.put(player.getUUID(), result);
        ROUTE_WARNING_POSTS.put(postKey(level, pos), result);
        return Optional.of(result);
    }

    public static Optional<EchoWorldContracts.EchoRouteWarningPostUseResult> lastRouteWarning(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(LAST_ROUTE_WARNINGS.get(player.getUUID()));
    }

    public static Map<UUID, EchoWorldContracts.EchoRouteWarningPostUseResult> lastRouteWarnings() {
        return Map.copyOf(LAST_ROUTE_WARNINGS);
    }

    public static Map<String, EchoWorldContracts.EchoRouteWarningPostUseResult> routeWarningPosts() {
        return Map.copyOf(ROUTE_WARNING_POSTS);
    }

    public static void clearForTests() {
        LAST_ROUTE_WARNINGS.clear();
        ROUTE_WARNING_POSTS.clear();
    }

    private static String postKey(Level level, BlockPos pos) {
        return level.dimension().identifier() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}

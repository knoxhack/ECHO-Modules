package com.knoxhack.echoweathercore.item;

import com.knoxhack.echo.adaptercore.EchoNativeWeatherRadioBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.WeatherCoreApi;
import com.knoxhack.echoweathercore.api.forecast.WeatherForecast;
import com.knoxhack.echoweathercore.api.weather.WeatherRouteRisk;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class WeatherRadioItem extends Item {
    private static final Map<UUID, EchoWorldContracts.EchoWeatherRadioUseResult> LAST_RADIO_USES =
            new ConcurrentHashMap<>();
    private static final Map<String, EchoWorldContracts.EchoWeatherRadioUseResult> RADIO_PLAYERS =
            new ConcurrentHashMap<>();

    public WeatherRadioItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            List<WeatherForecast> forecasts = WeatherCoreApi.getForecast(sp);
            resolveRadioUse(sp, forecasts, 40, "WeatherRadioItem.use")
                    .ifPresent(result -> result.messageLines()
                            .forEach(line -> player.sendSystemMessage(Component.literal(line))));
            player.getCooldowns().addCooldown(stack, 40);
        }
        return InteractionResult.SUCCESS;
    }

    public static Optional<EchoWorldContracts.EchoWeatherRadioUseResult> resolveRadioUse(ServerPlayer player,
            List<WeatherForecast> forecasts,
            int cooldownTicks,
            String sourceReason) {
        if (player == null) {
            return Optional.empty();
        }
        List<WeatherForecast> safeForecasts = forecasts == null ? List.of() : forecasts;
        List<String> weatherIds = new ArrayList<>();
        List<String> forecastLines = new ArrayList<>();
        WeatherSeverity strongestSeverity = WeatherSeverity.LOW;
        WeatherRouteRisk strongestRisk = WeatherRouteRisk.SAFE;
        for (WeatherForecast forecast : safeForecasts) {
            weatherIds.add(forecast.eventId().toString());
            forecastLines.add(" - " + forecast.displayName() + " [" + forecast.phase() + ", "
                    + forecast.severity() + "]");
            if (forecast.severity().ordinal() > strongestSeverity.ordinal()) {
                strongestSeverity = forecast.severity();
            }
            if (forecast.routeRisk().ordinal() > strongestRisk.ordinal()) {
                strongestRisk = forecast.routeRisk();
            }
        }
        EchoWorldContracts.EchoWeatherRadioUseResult result =
                new EchoNativeWeatherRadioBridge(EchoWeatherCore.MODID).use(
                        new EchoWorldContracts.EchoWeatherRadioUseRequest(
                                player.getUUID().toString(),
                                weatherIds,
                                forecastLines,
                                !safeForecasts.isEmpty(),
                                strongestSeverity.name(),
                                strongestRisk.name(),
                                Math.max(0, cooldownTicks),
                                Math.max(0L, player.level().getGameTime()),
                                sourceReason));
        LAST_RADIO_USES.put(player.getUUID(), result);
        RADIO_PLAYERS.put(result.playerId(), result);
        return Optional.of(result);
    }

    public static Optional<EchoWorldContracts.EchoWeatherRadioUseResult> lastRadioUse(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(LAST_RADIO_USES.get(player.getUUID()));
    }

    public static Map<UUID, EchoWorldContracts.EchoWeatherRadioUseResult> lastRadioUses() {
        return Map.copyOf(LAST_RADIO_USES);
    }

    public static Map<String, EchoWorldContracts.EchoWeatherRadioUseResult> radioPlayers() {
        return Map.copyOf(RADIO_PLAYERS);
    }

    public static void clearForTests() {
        LAST_RADIO_USES.clear();
        RADIO_PLAYERS.clear();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.literal("Shows regional weather forecast."));
    }
}

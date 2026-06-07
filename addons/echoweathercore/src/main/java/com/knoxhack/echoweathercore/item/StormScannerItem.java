package com.knoxhack.echoweathercore.item;

import com.knoxhack.echoweathercore.api.WeatherCoreApi;
import com.knoxhack.echoweathercore.api.forecast.WeatherForecast;
import com.knoxhack.echoweathercore.server.WeatherForecastManager;
import java.util.List;
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

public class StormScannerItem extends Item {
    public StormScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            WeatherForecast current = WeatherForecastManager.getCurrentWeatherForPlayer(sp);
            if (current != null) {
                player.sendSystemMessage(Component.literal("Storm Scanner: " + current.displayName()));
                player.sendSystemMessage(Component.literal("Phase: " + current.phase() + " | Severity: " + current.severity()));
                player.sendSystemMessage(Component.literal("Scanner Reliability: " + current.scannerReliability()));
            } else {
                List<WeatherForecast> forecasts = WeatherCoreApi.getForecast(sp);
                if (!forecasts.isEmpty()) {
                    WeatherForecast f = forecasts.get(0);
                    player.sendSystemMessage(Component.literal("Storm Scanner Forecast: " + f.displayName()));
                    player.sendSystemMessage(Component.literal("ETA: " + (f.etaTicks() / 20) + "s | Severity: " + f.severity()));
                } else {
                    player.sendSystemMessage(Component.literal("Storm Scanner: No weather detected."));
                }
            }
            player.getCooldowns().addCooldown(stack, 60);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.literal("Shows local current weather and forecast."));
        tooltip.accept(Component.literal("Right-click to scan."));
    }
}

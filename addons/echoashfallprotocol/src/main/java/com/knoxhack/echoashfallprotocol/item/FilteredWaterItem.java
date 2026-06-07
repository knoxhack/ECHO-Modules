package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Filtered Water Bottle - produced by crude filter.
 * Better than dirty water but not as good as clean water.
 */
public class FilteredWaterItem extends WaterBottleItem {

    public FilteredWaterItem(Properties properties) {
        super(properties, 30, 1, 0.1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.filtered_water.desc"));
        tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.filtered_water.hydration"));
    }
}

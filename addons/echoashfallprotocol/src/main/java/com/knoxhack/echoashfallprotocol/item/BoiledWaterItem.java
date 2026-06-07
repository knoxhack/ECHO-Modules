package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Boiled Water Bottle - produced by smelting dirty water.
 * Basic purification without advanced filtering.
 */
public class BoiledWaterItem extends WaterBottleItem {

    public BoiledWaterItem(Properties properties) {
        super(properties, 0, 0, 0.0f);
    }

    @Override
    protected int runtimeHydrationGain() {
        return 25;
    }

    @Override
    protected int hydrationAfterUse(int hydration) {
        return clampHydration(hydration + 25);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.boiled_water.desc"));
        tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.boiled_water.hydration"));
    }
}

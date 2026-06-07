package com.knoxhack.echoashfallprotocol.client;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

record ClientTooltipEventView(
        ItemStack itemStack,
        List<Component> tooltip,
        Item.TooltipContext context,
        TooltipFlag flags
) {
    @SuppressWarnings("unchecked")
    static ClientTooltipEventView from(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object stack = event.getClass().getMethod("getItemStack").invoke(event);
            Object tooltip = event.getClass().getMethod("getToolTip").invoke(event);
            Object context = event.getClass().getMethod("getContext").invoke(event);
            Object flags = event.getClass().getMethod("getFlags").invoke(event);
            if (stack instanceof ItemStack itemStack
                    && tooltip instanceof List<?> lines
                    && context instanceof Item.TooltipContext tooltipContext
                    && flags instanceof TooltipFlag tooltipFlags) {
                return new ClientTooltipEventView(
                        itemStack,
                        (List<Component>) lines,
                        tooltipContext,
                        tooltipFlags);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return null;
    }
}

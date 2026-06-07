package com.knoxhack.echorelictech.item;

import com.knoxhack.echorelictech.config.RelicTechConfig;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class NullBatteryItem extends Item {
    public NullBatteryItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        int charge = stack.getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0);
        int max = RelicTechConfig.NULL_BATTERY_MAX_CHARGE.get();
        tooltip.accept(Component.translatable("item.echorelictech.null_battery.charge", charge, max));
        tooltip.accept(Component.translatable("item.echorelictech.null_battery.warning"));
    }
}

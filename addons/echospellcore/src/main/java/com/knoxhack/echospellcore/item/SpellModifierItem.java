package com.knoxhack.echospellcore.item;

import com.knoxhack.echospellcore.spell.SpellModifier;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class SpellModifierItem extends Item {
    private final SpellModifier modifier;

    public SpellModifierItem(Properties properties, SpellModifier modifier) {
        super(properties.stacksTo(16));
        this.modifier = modifier;
    }

    public SpellModifier modifier() {
        return modifier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.echospellcore.modifier_socket.cost",
                modifier.socketCost(), modifier.description()));
    }
}

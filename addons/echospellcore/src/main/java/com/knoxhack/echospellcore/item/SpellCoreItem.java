package com.knoxhack.echospellcore.item;

import com.knoxhack.echospellcore.api.SpellCoreApi;
import com.knoxhack.echospellcore.registry.ModItems;
import com.knoxhack.echospellcore.spell.StarterSpell;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class SpellCoreItem extends Item {
    public SpellCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        StarterSpell spell = SpellCoreApi.spellForCoreItem(stack);
        tooltip.accept(Component.translatable("tooltip.echospellcore.spell_core.spell",
                spell == null ? StarterSpell.SIGNAL_PULSE.title() : spell.title()));
        if (stack.is(ModItems.OVERCHARGED_SPELL_CORE.get())) {
            tooltip.accept(Component.translatable("tooltip.echospellcore.spell_core.overcharged"));
        }
    }
}

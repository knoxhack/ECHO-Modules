package com.knoxhack.echo.equipmentcore.item;

import com.knoxhack.echo.equipmentcore.api.EquipmentStats;
import com.knoxhack.echo.equipmentcore.api.IEquipmentProvider;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class RebreatherItem extends Item implements IEquipmentProvider {
    private final String rebreatherId;
    private final float oxygenBonus;

    public RebreatherItem(String rebreatherId, float oxygenBonus, Properties properties) {
        super(properties);
        this.rebreatherId = rebreatherId;
        this.oxygenBonus = oxygenBonus;
    }

    @Override
    public EquipmentStats getStats(ItemStack stack) {
        return new EquipmentStats(0.0F, oxygenBonus, 0.0F, 0.0F, 0.0F, stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Oxygen bonus: " + oxygenBonus));
    }
}

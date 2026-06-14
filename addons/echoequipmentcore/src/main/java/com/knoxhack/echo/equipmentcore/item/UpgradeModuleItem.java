package com.knoxhack.echo.equipmentcore.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class UpgradeModuleItem extends Item {
    private final String upgradeId;

    public UpgradeModuleItem(String upgradeId, Properties properties) {
        super(properties);
        this.upgradeId = upgradeId;
    }

    public String upgradeId() {
        return upgradeId;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Upgrade: " + upgradeId));
    }
}

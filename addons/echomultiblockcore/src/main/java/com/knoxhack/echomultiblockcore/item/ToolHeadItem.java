package com.knoxhack.echomultiblockcore.item;

import com.knoxhack.echomultiblockcore.api.RobotToolType;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ToolHeadItem extends Item {
    private final RobotToolType toolType;

    public ToolHeadItem(RobotToolType toolType, Properties properties) {
        super(properties);
        this.toolType = toolType == null ? RobotToolType.GRIPPER : toolType;
    }

    public RobotToolType toolType() {
        return toolType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.echomultiblockcore.tool_head",
                Component.translatable("tool.echomultiblockcore." + toolType.name().toLowerCase(java.util.Locale.ROOT))));
    }
}

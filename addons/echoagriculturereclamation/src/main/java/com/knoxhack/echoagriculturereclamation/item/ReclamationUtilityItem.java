package com.knoxhack.echoagriculturereclamation.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ReclamationUtilityItem extends Item {
   private final String tooltipKey;

   public ReclamationUtilityItem(String tooltipKey, Properties properties) {
      super(properties);
      this.tooltipKey = tooltipKey;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      tooltip.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
   }
}

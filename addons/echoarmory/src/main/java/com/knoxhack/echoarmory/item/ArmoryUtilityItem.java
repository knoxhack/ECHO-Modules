package com.knoxhack.echoarmory.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ArmoryUtilityItem extends Item implements ArmoryGearItem {
   private final String id;
   private final String tooltip;

   public ArmoryUtilityItem(String id, String tooltip, Properties properties) {
      super(properties);
      this.id = id;
      this.tooltip = tooltip;
   }

   @Override
   public String gearId() {
      return id;
   }

   @Override
   public ArmoryGearKind gearKind() {
      return ArmoryGearKind.UTILITY;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipConsumer, TooltipFlag flag) {
      tooltipConsumer.accept(Component.literal(tooltip));
   }
}

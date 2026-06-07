package com.knoxhack.echoindustrialnexus.item;

import com.knoxhack.echoindustrialnexus.block.IndustrialItemDuctBlock;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialItemDuctBlockEntity;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

public class ThermalWrenchItem extends Item {
   public ThermalWrenchItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      Player player = context.getPlayer();
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (player == null) {
         return InteractionResult.CONSUME;
      } else if (level.getBlockEntity(context.getClickedPos()) instanceof IndustrialMachineBlockEntity machine) {
         boolean serviced = machine.serviceWithWrench(player);
         if (serviced) {
            damage(context.getItemInHand(), player, context);
         }

         return InteractionResult.SUCCESS;
      } else if (level.getBlockEntity(context.getClickedPos()) instanceof IndustrialItemDuctBlockEntity duct
         && level.getBlockState(context.getClickedPos()).getBlock() instanceof IndustrialItemDuctBlock ductBlock
         && ductBlock.smart()) {
         duct.toggleFilterMode();
         player.sendSystemMessage(duct.filterStatus(ductBlock));
         damage(context.getItemInHand(), player, context);
         return InteractionResult.SUCCESS;
      } else {
         player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Wrench found no configurable industrial machine."));
         return InteractionResult.SUCCESS;
      }
   }

   private static void damage(ItemStack stack, Player player, UseOnContext context) {
      stack.hurtAndBreak(1, player, context.getHand());
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      tooltip.accept(Component.translatable("tooltip.echoindustrialnexus.thermal_wrench"));
      tooltip.accept(Component.translatable("tooltip.echoindustrialnexus.thermal_wrench.shift"));
   }
}

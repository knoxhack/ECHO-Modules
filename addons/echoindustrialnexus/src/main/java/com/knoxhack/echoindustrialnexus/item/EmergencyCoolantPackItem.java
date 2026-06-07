package com.knoxhack.echoindustrialnexus.item;

import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EmergencyCoolantPackItem extends Item {
   public EmergencyCoolantPackItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (context.getLevel() instanceof ServerLevel level && player != null) {
         int cooled = 0;
         BlockPos center = context.getClickedPos();

         for (BlockPos scanPos : BlockPos.betweenClosed(center.offset(-4, -4, -4), center.offset(4, 4, 4))) {
            BlockEntity blockEntity = level.getBlockEntity(scanPos);
            if (blockEntity instanceof IndustrialMachineBlockEntity machine
               && blockEntity.getBlockPos().closerThan(center, 4.5)
               && machine.applyEmergencyCoolant()) {
               cooled++;
            }
         }

         if (cooled > 0) {
            context.getItemInHand().shrink(1);
            player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Emergency coolant deployed. Machines stabilized: " + cooled + "."));
            return InteractionResult.SUCCESS_SERVER;
         } else {
            player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // No overheated machines in coolant radius."));
            return InteractionResult.CONSUME;
         }
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      tooltip.accept(Component.translatable("tooltip.echoindustrialnexus.emergency_coolant_pack"));
   }
}

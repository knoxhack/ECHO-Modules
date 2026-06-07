package com.knoxhack.echoindustrialnexus.item;

import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import com.knoxhack.echoindustrialnexus.flux.ThermalFluxNetwork;
import com.knoxhack.echoindustrialnexus.flux.ThermalFluxStorage;
import java.util.function.Consumer;
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
import net.minecraft.world.level.block.entity.BlockEntity;

public class FluxMultimeterItem extends Item {
   public FluxMultimeterItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      Player player = context.getPlayer();
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (player == null) {
         return InteractionResult.CONSUME;
      } else {
         BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
         if (blockEntity instanceof IndustrialMachineBlockEntity machine) {
            player.sendSystemMessage(Component.literal(machine.diagnosticLine()));
         } else if (blockEntity instanceof ThermalFluxStorage storage) {
            player.sendSystemMessage(
               Component.literal("ECHO INDUSTRIAL DIAGNOSTIC // Thermal Flux storage " + storage.getFluxStored() + "/" + storage.getMaxFluxStored() + " TF.")
            );
         }

         ThermalFluxNetwork.ScanReport report = ThermalFluxNetwork.scan(level, context.getClickedPos());
         if (report.detected()) {
            player.sendSystemMessage(
               Component.literal(
                  "ECHO INDUSTRIAL NETWORK // Ducts: "
                     + report.ducts()
                     + " | Suppliers: "
                     + report.suppliers()
                     + " | Receivers: "
                     + report.receivers()
                     + " | Stored: "
                     + report.storedFlux()
                     + "/"
                     + report.capacity()
                     + " TF."
               )
            );
            damage(context.getItemInHand(), player, context);
            return InteractionResult.SUCCESS;
         } else {
            player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL DIAGNOSTIC // No Thermal Flux signal detected."));
            return InteractionResult.SUCCESS;
         }
      }
   }

   private static void damage(ItemStack stack, Player player, UseOnContext context) {
      stack.hurtAndBreak(1, player, context.getHand());
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      tooltip.accept(Component.translatable("tooltip.echoindustrialnexus.flux_multimeter"));
      tooltip.accept(Component.translatable("tooltip.echoindustrialnexus.flux_multimeter.network"));
   }
}

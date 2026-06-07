package com.knoxhack.echonexusprotocol.item;

import com.knoxhack.echonexusprotocol.block.entity.NexusMachineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class NexusChargeItem extends Item {
   private final int charge;
   private final int corruption;

   public NexusChargeItem(int charge, int corruption, Properties properties) {
      super(properties);
      this.charge = Math.max(0, charge);
      this.corruption = Math.max(0, corruption);
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (!level.isClientSide() && level.getBlockEntity(context.getClickedPos()) instanceof NexusMachineBlockEntity machine) {
         machine.receiveCharge(this.charge);
         machine.addContamination(this.corruption);
         if (!context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
         }

         context.getPlayer().sendSystemMessage(Component.literal("ECHO-7 // Nexus Charge injected."));
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.PASS;
      }
   }
}

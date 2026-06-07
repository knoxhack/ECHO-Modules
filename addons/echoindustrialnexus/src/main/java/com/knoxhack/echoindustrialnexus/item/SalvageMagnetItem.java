package com.knoxhack.echoindustrialnexus.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class SalvageMagnetItem extends Item {
   public SalvageMagnetItem(Properties properties) {
      super(properties);
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!(level instanceof ServerLevel serverLevel)) {
         return InteractionResult.SUCCESS;
      } else {
         AABB area = player.getBoundingBox().inflate(8.0);
         List<ItemEntity> items = serverLevel.getEntitiesOfClass(ItemEntity.class, area, entity -> entity.isAlive() && !entity.hasPickUpDelay());
         int moved = 0;

         for (ItemEntity itemEntity : items) {
            itemEntity.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            moved++;
         }

         if (moved > 0) {
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // Salvage magnet pulled " + moved + " loose item stacks."));
         } else {
            player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // No loose salvage detected nearby."));
         }

         return InteractionResult.SUCCESS_SERVER;
      }
   }
}

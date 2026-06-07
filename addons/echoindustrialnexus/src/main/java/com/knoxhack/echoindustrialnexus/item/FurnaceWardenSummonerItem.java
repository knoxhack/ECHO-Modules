package com.knoxhack.echoindustrialnexus.item;

import com.knoxhack.echoindustrialnexus.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;

public class FurnaceWardenSummonerItem extends Item {
   public FurnaceWardenSummonerItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (context.getLevel() instanceof ServerLevel level && player != null) {
         EntityType<?> type = (EntityType<?>)ModEntities.FURNACE_WARDEN.get();
         Zombie warden = (Zombie)type.create(level, EntitySpawnReason.EVENT);
         if (warden == null) {
            return InteractionResult.CONSUME;
         } else {
            warden.setPos(context.getClickedPos().getX() + 0.5, context.getClickedPos().getY() + 1.0, context.getClickedPos().getZ() + 0.5);
            level.addFreshEntity(warden);
            if (!player.hasInfiniteMaterials()) {
               context.getItemInHand().shrink(1);
            }

            player.sendSystemMessage(Component.literal("ECHO-7 // Furnace Warden wake signal accepted. Production lock engaged."));
            return InteractionResult.SUCCESS_SERVER;
         }
      } else {
         return InteractionResult.SUCCESS;
      }
   }
}

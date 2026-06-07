package com.knoxhack.echoblackboxprotocol.item;

import com.knoxhack.echoblackboxprotocol.progression.BlackboxEnding;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEndings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class EndingDirectiveItem extends Item {
   private final BlackboxEnding ending;

   public EndingDirectiveItem(BlackboxEnding ending, Properties properties) {
      super(properties);
      this.ending = ending;
   }

   public BlackboxEnding ending() {
      return this.ending;
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         BlackboxEndings.apply(serverPlayer, this.ending, player.blockPosition());
      }

      return InteractionResult.SUCCESS_SERVER;
   }
}

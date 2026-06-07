package com.knoxhack.echoblackboxprotocol.item;

import com.knoxhack.echoblackboxprotocol.integration.BlackboxCoreIntegration;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxProgress;
import com.knoxhack.echoblackboxprotocol.progression.MemoryType;
import com.knoxhack.echoblackboxprotocol.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class BlackboxFragmentItem extends Item {
   private final MemoryType type;

   public BlackboxFragmentItem(MemoryType type, Properties properties) {
      super(properties);
      this.type = type;
   }

   public MemoryType type() {
      return this.type;
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         decode(serverPlayer, player.getItemInHand(hand));
      }

      return InteractionResult.SUCCESS_SERVER;
   }

   public static boolean decode(Player player, ItemStack stack) {
      if (stack.getItem() instanceof BlackboxFragmentItem fragment) {
         BlackboxProgress progress = BlackboxProgress.get(player);
         boolean first = progress.decode(player, fragment.type());
         if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
         }

         give(player, new ItemStack((ItemLike)ModItems.recordFor(fragment.type()).get()));
         player.sendSystemMessage(Component.literal("ECHO-7 // " + fragment.type().displayName() + " decoded. Memory stability " + progress.stability() + "%."));
         if (player instanceof ServerPlayer serverPlayer) {
            BlackboxCoreIntegration.mirrorDecodedMemory(serverPlayer, fragment.type(), first);
         }

         return true;
      } else {
         return false;
      }
   }

   private static void give(Player player, ItemStack stack) {
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }
}

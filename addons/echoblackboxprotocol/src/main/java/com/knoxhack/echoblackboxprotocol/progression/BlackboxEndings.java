package com.knoxhack.echoblackboxprotocol.progression;

import com.knoxhack.echoblackboxprotocol.integration.BlackboxCoreIntegration;
import com.knoxhack.echoblackboxprotocol.registry.ModBlocks;
import com.knoxhack.echoblackboxprotocol.registry.ModItems;
import com.knoxhack.echoblackboxprotocol.world.BlackboxWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public final class BlackboxEndings {
   private BlackboxEndings() {
   }

   public static boolean apply(Player player, BlackboxEnding ending, BlockPos pos) {
      if (ending != null && ending != BlackboxEnding.NONE) {
         BlackboxProgress progress = BlackboxProgress.get(player);
         if (progress.ending() != BlackboxEnding.NONE) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Truth Engine already committed: " + progress.ending().displayName() + "."));
            return false;
         } else if (!eligible(player, progress, ending)) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Truth Engine rejects " + ending.displayName() + ". Required proof is incomplete."));
            return false;
         } else if (!progress.setEnding(ending)) {
            return false;
         } else {
            ServerLevel level = (ServerLevel)player.level();
            BlackboxWorldData data = BlackboxWorldData.get(level.getServer().overworld());
            data.recordEnding(ending, pos, player.getName().getString());
            applyOutcome(player, ending, data);
            if (player instanceof ServerPlayer serverPlayer) {
               BlackboxCoreIntegration.recordEnding(serverPlayer, ending);
            }

            Component line = Component.literal("ECHO-7 // " + ending.finalLine());
            level.getServer().getPlayerList().broadcastSystemMessage(line, false);
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean eligible(Player player, BlackboxProgress progress, BlackboxEnding ending) {
      boolean finalBoss = progress.bossDefeated("nexus_guardian") || player.hasInfiniteMaterials();
      boolean key = progress.hasNexusCoreAccessKey()
         || player.getInventory().contains(new ItemStack((ItemLike)ModItems.NEXUS_CORE_ACCESS_KEY.get()))
         || player.hasInfiniteMaterials();
      return ending != BlackboxEnding.MERGE
         ? finalBoss && key
         : finalBoss
            && key
            && progress.hasAllDeletedLogs()
            && has(player, (Item)ModItems.ECHO_IDENTITY_FRAGMENT.get())
            && has(player, (Item)ModItems.MEMORY_STABILIZER_CORE.get())
            && has(player, (Item)ModItems.COMMAND_KEY.get())
            && has(player, (Item)ModItems.PROTOCOL_EXTRACTOR_SCHEMATIC.get())
            && (has(player, (Item)ModItems.GUARDIAN_CORE.get()) || player.hasInfiniteMaterials())
            && (has(player, ((Block)ModBlocks.PROTOCOL_EXTRACTOR.get()).asItem()) || player.hasInfiniteMaterials());
   }

   private static void applyOutcome(Player player, BlackboxEnding ending, BlackboxWorldData data) {
      BlackboxProgress progress = BlackboxProgress.get(player);
      switch (ending) {
         case RESTORE:
            progress.markRestoreStabilized();
            progress.stability(100);
            progress.falseSignals(Math.max(0, progress.falseSignalCount() - 6));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 0));
            break;
         case CONTROL:
            progress.markCorruptionDirected();
            progress.markEchoDistrust();
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 1800, 1));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0));
            break;
         case DESTROY:
            progress.markNexusSpreadStopped();
            progress.falseSignals(0);
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1800, 1));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 400, 0));
            break;
         case MERGE:
            progress.markMergedIdentity();
            progress.stability(100);
            progress.falseSignals(0);
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 3600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 3600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 4800, 0));
            break;
         case NONE:
            break;
      }

      if (!data.globalApplied()) {
         data.markGlobalApplied();
      }
   }

   private static boolean has(Player player, Item item) {
      return player.getInventory().contains(new ItemStack(item));
   }
}

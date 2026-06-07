package com.knoxhack.echonexusprotocol.item;

import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.integration.NexusMissionHooks;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NexusScannerVisorItem extends Item {
   public NexusScannerVisorItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (context.getPlayer() != null) {
         scanBlock(context.getPlayer(), context.getClickedPos());
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.PASS;
      }
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         if (player instanceof ServerPlayer serverPlayer) {
            NexusPlayerData data = NexusPlayerData.get(serverPlayer);
            data.markGearUsed("nexus_scanner_visor");
            data.unlockResearch("nexus_theory");
            NexusPlayerData.saveAndSync(serverPlayer, data);
            NexusMissionHooks.recordScan(serverPlayer, "field_scan");
            NexusWorldData worldData = NexusWorldData.get(serverPlayer.level());
            ChunkPos chunk = serverPlayer.chunkPosition();
            serverPlayer.sendSystemMessage(Component.literal("ECHO-7 // NEXUS FIELD SCAN"));
            serverPlayer.sendSystemMessage(
               Component.literal(
                  "FIELD: "
                     + worldData.fieldState(chunk)
                     + " | STABILITY: "
                     + worldData.fieldValue(chunk)
                     + "% | CORRUPTION: "
                     + worldData.corruptionPressure(chunk)
                     + "% | SIGNAL: "
                     + (data.blackboxMonolithActivated() ? "BLACKBOX ROUTE" : "DISTORTED")
               )
            );
         }

         return InteractionResult.SUCCESS_SERVER;
      }
   }

   public static void scanBlock(Player player, BlockPos pos) {
      NexusPlayerData data = NexusPlayerData.get(player);
      Level level = player.level();
      BlockState state = level.getBlockState(pos);
      Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
      data.markScanned(id);
      data.markGearUsed("nexus_scanner_visor");
      data.unlockResearch("nexus_theory");
      unlockForBlock(data, state.getBlock());
      if (player instanceof ServerPlayer serverPlayer) {
         NexusPlayerData.saveAndSync(serverPlayer, data);
         NexusMissionHooks.recordScan(serverPlayer, id.toString());
      }

      if (level instanceof ServerLevel serverLevel) {
         NexusWorldData worldData = NexusWorldData.get(serverLevel);
         ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
         player.sendSystemMessage(
            Component.literal(
               "ECHO-7 // SCAN "
                  + id
                  + " | FIELD "
                  + worldData.fieldState(chunk)
                  + " "
                  + worldData.fieldValue(chunk)
                  + "% | CORRUPTION "
                  + worldData.corruptionPressure(chunk)
                  + "%"
            )
         );
      }
   }

   private static void unlockForBlock(NexusPlayerData data, Block block) {
      if (block == ModBlocks.CORRUPTION_FILTER.get()
         || block == ModBlocks.NEXUS_FIELD_STABILIZER.get()
         || block == ModBlocks.WHITE_SIGNAL_LOG.get()
         || block == ModBlocks.WHITE_SIGNAL_LEAVES.get()) {
         data.unlockResearch("field_stabilization");
      }

      if (block == ModBlocks.NEXUS_INFUSER.get()
         || block == ModBlocks.REALITY_FORGE.get()
         || block == ModBlocks.DATA_CRACKED_STONE.get()
         || block == ModBlocks.BLACKBOX_PLATE.get()) {
         data.unlockResearch("matter_rewriting");
      }

      if (block == ModBlocks.MEMORY_DECODER.get()
         || block == ModBlocks.BLACKBOX_DEEPSLATE.get()
         || block == ModBlocks.BLACKBOX_PLATE.get()
         || block == ModBlocks.CORE_GLASS_BLOCK.get()) {
         data.unlockResearch("memory_recovery");
      }

      if (block == ModBlocks.PROTOCOL_SEAL.get() || block == ModBlocks.NEXUS_CHARGE_TANK.get() || block == ModBlocks.SIGNAL_GLASS.get()) {
         data.unlockResearch("protocol_automation");
      }

      if (block == ModBlocks.CORRUPTION_REACTOR.get() || block == ModBlocks.REALITY_TEAR.get() || block == ModBlocks.CORE_BRICK.get()) {
         data.unlockResearch("forbidden_core_access");
      }
   }
}

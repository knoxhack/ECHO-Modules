package com.knoxhack.echonexusprotocol.item;

import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.integration.NexusMissionHooks;
import com.knoxhack.echonexusprotocol.integration.NexusProgression;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.world.ModDimensions;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CoreAccessKeyItem extends Item {
   private static final BlockPos NEXUS_ENTRY_POS = BlockPos.containing(0.5D, 96.0D, 0.5D);
   private static final int SAFE_PAD_RADIUS = 3;

   public CoreAccessKeyItem(Properties properties) { super(properties); }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide()) return InteractionResult.SUCCESS;
      if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;
      if (!NexusProgression.isNexusUnlocked(player) && !player.hasInfiniteMaterials()) {
         player.sendSystemMessage(Component.literal("ECHO-7 // Core Access denied. Recover " + NexusProgression.STATIONFALL_GATE + " through Stationfall handoff, or enable the Nexus dev unlock config for testing."));
         return InteractionResult.CONSUME;
      }
      MinecraftServer server = serverPlayer.level().getServer();
      NexusPlayerData data = NexusPlayerData.get(player);
      if (level.dimension() == ModDimensions.NEXUS) {
         ServerLevel returnLevel = server.getLevel(dimensionKey(data.nexusReturnDimension()));
         if (returnLevel == null) returnLevel = server.overworld();
         serverPlayer.teleportTo(returnLevel, data.nexusReturnX(), data.nexusReturnY(), data.nexusReturnZ(), Set.of(), data.nexusReturnYRot(), data.nexusReturnXRot(), false);
         data.clearNexusReturn();
         NexusPlayerData.saveAndSync(serverPlayer, data);
         player.sendSystemMessage(Component.literal("ECHO-7 // Nexus return vector restored."));
         return InteractionResult.SUCCESS_SERVER;
      }
      ServerLevel targetLevel = server.getLevel(ModDimensions.NEXUS);
      if (targetLevel == null) {
         player.sendSystemMessage(Component.literal("ECHO-7 // Core Access failed. The echonexusprotocol:nexus dimension is not registered in this world; progression was not advanced."));
         return InteractionResult.CONSUME;
      }
      ensureSafeArrivalPad(targetLevel, NEXUS_ENTRY_POS);
      data.setNexusReturn(player);
      data.markCoreEntered();
      data.markGearUsed(this == com.knoxhack.echonexusprotocol.registry.ModItems.CORE_KEY_ASSEMBLY.get() ? "core_key_assembly" : "core_access_key");
      NexusPlayerData.saveAndSync(serverPlayer, data);
      NexusMissionHooks.recordCoreEntered(serverPlayer);
      BlockPos target = NEXUS_ENTRY_POS;
      serverPlayer.teleportTo(targetLevel, target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, Set.of(), player.getYRot(), player.getXRot(), false);
      player.sendSystemMessage(Component.literal("ECHO-7 // Nexus Core route opened. Reality lock engaged."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private static void ensureSafeArrivalPad(ServerLevel level, BlockPos center) {
      BlockState floor = ((Block)ModBlocks.CORE_BRICK.get()).defaultBlockState();
      for (int dx = -SAFE_PAD_RADIUS; dx <= SAFE_PAD_RADIUS; dx++) {
         for (int dz = -SAFE_PAD_RADIUS; dz <= SAFE_PAD_RADIUS; dz++) {
            BlockPos base = center.offset(dx, -1, dz);
            if (!level.getBlockState(base).isSolid()) {
               level.setBlock(base, floor, 3);
            }
            clearIfUnsafe(level, center.offset(dx, 0, dz));
            clearIfUnsafe(level, center.offset(dx, 1, dz));
            clearIfUnsafe(level, center.offset(dx, 2, dz));
         }
      }
      level.setBlock(center.offset(0, -1, 0), ((Block)ModBlocks.SIGNAL_GLASS.get()).defaultBlockState(), 3);
   }

   private static void clearIfUnsafe(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) {
         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      }
   }

   public static ResourceKey<Level> dimensionKey(String id) {
      Identifier parsed;
      try {
         parsed = Identifier.parse(id == null || id.isBlank() ? "minecraft:overworld" : id);
      } catch (Exception ignored) {
         parsed = Level.OVERWORLD.identifier();
      }
      return ResourceKey.create(Registries.DIMENSION, parsed);
   }
}

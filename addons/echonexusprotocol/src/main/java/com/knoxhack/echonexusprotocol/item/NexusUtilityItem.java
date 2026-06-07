package com.knoxhack.echonexusprotocol.item;

import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.entity.NexusMobEntity;
import com.knoxhack.echonexusprotocol.Config;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class NexusUtilityItem extends Item {
   private static final int ANCHOR_TICKS = 1200;
   private final NexusUtilityItem.Mode mode;

   public NexusUtilityItem(NexusUtilityItem.Mode mode, Properties properties) {
      super(properties);
      this.mode = mode;
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (level instanceof ServerLevel serverLevel) {
         switch (this.mode) {
            case PICKAXE:
               int scans = fieldPulse(serverLevel, player, player.blockPosition());
               markGear(player, "nexus_pickaxe");
               player.getItemInHand(hand).hurtAndBreak(1, player, hand);
               player.sendSystemMessage(Component.literal("ECHO-7 // Field Pulse complete. Hidden Nexus signatures: " + scans + "."));
               break;
            case SIGNAL_BLADE:
               int hit = signalBladePulse(serverLevel, player.blockPosition(), player);
               markGear(player, "signal_blade");
               player.getItemInHand(hand).hurtAndBreak(2, player, hand);
               player.sendSystemMessage(Component.literal("ECHO-7 // Pulse Cut discharged. Targets destabilized: " + hit + "."));
               break;
            case REALITY_ANCHOR:
               anchorReality(serverLevel, player.blockPosition());
               markGear(player, "reality_anchor");
               player.sendSystemMessage(Component.literal("ECHO-7 // Reality Anchor engaged. Local teleport drift and rift pull suppressed."));
               break;
            case FIELD_ANCHOR:
               anchorField(serverLevel, player.blockPosition());
               markGear(player, "field_anchor");
               if (!player.getAbilities().instabuild) {
                  player.getItemInHand(hand).shrink(1);
               }
               player.sendSystemMessage(Component.literal("ECHO-7 // Field Anchor deployed. Collapse recovery window established."));
         }

         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.PASS;
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (level instanceof ServerLevel serverLevel && context.getPlayer() != null) {
         Player player = context.getPlayer();
         switch (this.mode) {
            case PICKAXE:
               int scans = fieldPulse(serverLevel, player, context.getClickedPos());
               markGear(player, "nexus_pickaxe");
               context.getItemInHand().hurtAndBreak(1, player, context.getHand());
               player.sendSystemMessage(Component.literal("ECHO-7 // Field Pulse complete. Hidden Nexus signatures: " + scans + "."));
               return InteractionResult.SUCCESS_SERVER;
            case REALITY_ANCHOR:
               anchorReality(serverLevel, context.getClickedPos());
               markGear(player, "reality_anchor");
               player.sendSystemMessage(Component.literal("ECHO-7 // Reality Anchor pinned this chunk."));
               return InteractionResult.SUCCESS_SERVER;
            case FIELD_ANCHOR:
               anchorField(serverLevel, context.getClickedPos());
               markGear(player, "field_anchor");
               if (!player.getAbilities().instabuild) {
                  context.getItemInHand().shrink(1);
               }
               player.sendSystemMessage(Component.literal("ECHO-7 // Field Anchor pinned this chunk. Stabilize from this point outward."));
               return InteractionResult.SUCCESS_SERVER;
            default:
               return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public static int fieldPulse(ServerLevel level, Player player, BlockPos center) {
      int revealed = revealHiddenCrystal(level, center);
      int scans = revealed;
      NexusPlayerData data = NexusPlayerData.get(player);
      data.unlockResearch("nexus_theory");

      for (BlockPos target : BlockPos.betweenClosed(center.offset(-5, -4, -5), center.offset(5, 4, 5))) {
         BlockState state = level.getBlockState(target);
         if (isNexusSignature(state.getBlock())) {
            if (data.markScanned(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
               scans++;
            }

            unlockForSignature(data, state.getBlock());
         }
      }

      ChunkPos chunk = chunkPos(center);
      NexusWorldData worldData = NexusWorldData.get(level);
      if (scans > 0) {
         worldData.addFieldValue(chunk, -1);
      }

      if (player instanceof ServerPlayer serverPlayer) {
         NexusPlayerData.saveAndSync(serverPlayer, data);
      }

      return scans;
   }

   public static int signalBladePulse(ServerLevel level, BlockPos center, Player source) {
      int hit = 0;

      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center).inflate(4.5))) {
         if (entity != source && entity.isAlive() && (entity instanceof NexusMobEntity || entity instanceof Monster)) {
            entity.hurt(level.damageSources().magic(), 8.0F);
            hit++;
         }
      }

      NexusWorldData data = NexusWorldData.get(level);
      ChunkPos chunk = chunkPos(center);
      data.addFieldValue(chunk, 1);
      data.addCorruptionPressure(chunk, hit > 0 ? -16 : -4);
      return hit;
   }

   public static void anchorReality(ServerLevel level, BlockPos center) {
      NexusWorldData data = NexusWorldData.get(level);
      ChunkPos chunk = chunkPos(center);
      data.quarantineChunk(chunk, ANCHOR_TICKS);
      data.addFieldValue(chunk, 6);
      data.addCorruptionPressure(chunk, -12);
   }

   public static void anchorField(ServerLevel level, BlockPos center) {
      NexusWorldData data = NexusWorldData.get(level);
      ChunkPos chunk = chunkPos(center);
      data.quarantineChunk(chunk, (Integer)Config.FIELD_ANCHOR_TICKS.get());
      data.addFieldValue(chunk, (Integer)Config.FIELD_ANCHOR_FIELD_GAIN.get());
      data.addCorruptionPressure(chunk, -(Integer)Config.FIELD_ANCHOR_CORRUPTION_REDUCTION.get());
      if (data.fieldState(chunk) == NexusWorldData.FieldState.COLLAPSED) {
         data.addFieldValue(chunk, 8);
      }
   }

   private static int revealHiddenCrystal(ServerLevel level, BlockPos center) {
      ChunkPos chunk = chunkPos(center);
      NexusWorldData data = NexusWorldData.get(level);
      if (data.fieldState(chunk) == NexusWorldData.FieldState.STABLE && data.corruptionPressure(chunk) <= 0) {
         return 0;
      } else {
         for (BlockPos target : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 2, 3))) {
            BlockState state = level.getBlockState(target);
            if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is((Block)ModBlocks.DATA_CRACKED_STONE.get())) {
               level.setBlock(target, ((Block)ModBlocks.NEXUS_CRYSTAL_CLUSTER.get()).defaultBlockState(), 3);
               return 1;
            }
         }

         return 0;
      }
   }

   private static boolean isNexusSignature(Block block) {
      return block == ModBlocks.NEXUS_CRYSTAL_CLUSTER.get()
         || block == ModBlocks.CORRUPTED_FERRITE_ORE.get()
         || block == ModBlocks.BLACKBOX_DEEPSLATE.get()
         || block == ModBlocks.DATA_CRACKED_STONE.get()
         || block == ModBlocks.BLACKBOX_PLATE.get()
         || block == ModBlocks.CORE_BRICK.get()
         || block == ModBlocks.REALITY_TEAR.get();
   }

   private static void unlockForSignature(NexusPlayerData data, Block block) {
      if (block == ModBlocks.NEXUS_CRYSTAL_CLUSTER.get()
         || block == ModBlocks.CORRUPTED_FERRITE_ORE.get()
         || block == ModBlocks.DATA_CRACKED_STONE.get()
         || block == ModBlocks.BLACKBOX_PLATE.get()) {
         data.unlockResearch("matter_rewriting");
      }

      if (block == ModBlocks.BLACKBOX_DEEPSLATE.get() || block == ModBlocks.BLACKBOX_PLATE.get()) {
         data.unlockResearch("memory_recovery");
      }

      if (block == ModBlocks.CORE_BRICK.get() || block == ModBlocks.REALITY_TEAR.get()) {
         data.unlockResearch("forbidden_core_access");
      }
   }

   private static ChunkPos chunkPos(BlockPos pos) {
      return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
   }

   private static void markGear(Player player, String id) {
      if (player instanceof ServerPlayer serverPlayer) {
         NexusPlayerData data = NexusPlayerData.get(serverPlayer);
         data.markGearUsed(id);
         NexusPlayerData.saveAndSync(serverPlayer, data);
      }
   }

   public static enum Mode {
      PICKAXE,
      SIGNAL_BLADE,
      REALITY_ANCHOR,
      FIELD_ANCHOR;
   }
}

package com.knoxhack.echonexusprotocol.item;

import com.knoxhack.echonexusprotocol.entity.NexusMobEntity;
import com.knoxhack.echonexusprotocol.Config;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class NexusFieldChargeItem extends Item {
   private final NexusFieldChargeItem.Mode mode;

   public NexusFieldChargeItem(NexusFieldChargeItem.Mode mode, Properties properties) {
      super(properties);
      this.mode = mode;
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (level instanceof ServerLevel serverLevel) {
         apply(serverLevel, context.getClickedPos(), this.mode);
         Player player = context.getPlayer();
         if (player != null) {
            if (player instanceof ServerPlayer serverPlayer) {
               NexusPlayerData data = NexusPlayerData.get(serverPlayer);
               data.markGearUsed(this.mode.gearId());
               NexusPlayerData.saveAndSync(serverPlayer, data);
            }
            if (!player.getAbilities().instabuild) {
               context.getItemInHand().shrink(1);
            }

            player.sendSystemMessage(Component.literal("ECHO-7 // " + this.mode.displayName().toUpperCase(Locale.ROOT) + " field charge discharged."));
         }

         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.PASS;
      }
   }

   public static int apply(ServerLevel level, BlockPos center, NexusFieldChargeItem.Mode mode) {
      return switch (mode) {
         case PURITY -> applyPurity(level, center);
         case STABILIZED_PURITY -> applyStabilizedPurity(level, center);
         case COLLAPSE -> applyCollapse(level, center);
      };
   }

   private static int applyPurity(ServerLevel level, BlockPos center) {
      int changed = 0;

      for (BlockPos target : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 2, 3))) {
         BlockState clean = ModBlocks.cleanVariant(level.getBlockState(target));
         if (clean != null) {
            level.setBlock(target, clean, 3);
            if (++changed >= 12) {
               break;
            }
         }
      }

      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center).inflate(4.0))) {
         if (entity instanceof NexusMobEntity || entity instanceof Monster) {
            entity.hurt(level.damageSources().magic(), 6.0F);
         }
      }

      NexusWorldData data = NexusWorldData.get(level);
      ChunkPos chunk = chunkPos(center);
      data.addFieldValue(chunk, 8);
      data.addCorruptionPressure(chunk, -16);
      return changed;
   }

   private static int applyStabilizedPurity(ServerLevel level, BlockPos center) {
      int changed = 0;

      for (BlockPos target : BlockPos.betweenClosed(center.offset(-5, -3, -5), center.offset(5, 3, 5))) {
         BlockState clean = ModBlocks.cleanVariant(level.getBlockState(target));
         if (clean != null) {
            level.setBlock(target, clean, 3);
            if (++changed >= 32) {
               break;
            }
         }
      }

      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center).inflate(6.0))) {
         if (entity instanceof NexusMobEntity || entity instanceof Monster) {
            entity.hurt(level.damageSources().magic(), 10.0F);
         }
      }

      NexusWorldData data = NexusWorldData.get(level);
      ChunkPos chunk = chunkPos(center);
      data.quarantineChunk(chunk, Math.max(600, (Integer)Config.FIELD_ANCHOR_TICKS.get() / 2));
      data.addFieldValue(chunk, (Integer)Config.STABILIZED_PURITY_FIELD_GAIN.get());
      data.addCorruptionPressure(chunk, -(Integer)Config.STABILIZED_PURITY_CORRUPTION_REDUCTION.get());
      return changed;
   }

   private static int applyCollapse(ServerLevel level, BlockPos center) {
      int changed = 0;

      for (BlockPos target : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 2, 3))) {
         BlockState corrupted = ModBlocks.corruptedVariant(level.getBlockState(target));
         if (corrupted != null) {
            level.setBlock(target, corrupted, 3);
            if (++changed >= 12) {
               break;
            }
         }
      }

      NexusWorldData data = NexusWorldData.get(level);
      ChunkPos chunk = chunkPos(center);
      data.addFieldValue(chunk, -10);
      data.addCorruptionPressure(chunk, 18);
      return changed;
   }

   private static ChunkPos chunkPos(BlockPos pos) {
      return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
   }

   public static enum Mode {
      PURITY("Purity"),
      STABILIZED_PURITY("Stabilized Purity"),
      COLLAPSE("Collapse");

      private final String displayName;

      private Mode(String displayName) {
         this.displayName = displayName;
      }

      public String displayName() {
         return this.displayName;
      }

      public String gearId() {
         return switch (this) {
            case PURITY -> "purity_charge";
            case STABILIZED_PURITY -> "stabilized_purity_charge";
            case COLLAPSE -> "collapse_charge";
         };
      }
   }
}

package com.knoxhack.echonexusprotocol.event;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echonexusprotocol.Config;
import com.knoxhack.echonexusprotocol.entity.NexusMobEntity;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.registry.ModEntities;
import com.knoxhack.echonexusprotocol.registry.ModSounds;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;

public class NexusWorldEvents {
   public static final int STORM_MOB_CAP = 5;
   public static final int STORM_MOB_CAP_RADIUS = 18;

   public void onLevelTick(Object event) {
      ServerLevel level = EchoBackendWorldEventBridge.postTickServerLevel(event);
      if (level != null && (Boolean)Config.CHUNK_CORRUPTION_TICK_ENABLED.get()) {
         int interval = Math.max(20, (Integer)Config.FIELD_TICK_INTERVAL.get());
         if (level.getGameTime() % interval == 0L) {
            NexusWorldData data = NexusWorldData.get(level);
            long stormWindow = interval * (long)(Integer)Config.STORM_WINDOW_MULTIPLIER.get();
            data.pruneExpiredStorms(level.getGameTime(), stormWindow);
            Set<ChunkPos> affectedChunks = new LinkedHashSet<>();

            for (ServerPlayer player : level.players()) {
               ChunkPos center = player.chunkPosition();

               for (int dx = -1; dx <= 1; dx++) {
                  for (int dz = -1; dz <= 1; dz++) {
                     affectedChunks.add(new ChunkPos(center.x() + dx, center.z() + dz));
                  }
               }
            }
            for (ChunkPos chunk : affectedChunks) {
               data.tickAffectedChunk(level, chunk);
               if (data.fieldValue(chunk) < 60 || data.corruptionPressure(chunk) > 0) {
                  mutateOneBlock(level, data, chunk);
               }
            }
            for (ServerPlayer player : level.players()) {
               ChunkPos chunk = player.chunkPosition();
               if (data.hasActiveStorm(chunk, level.getGameTime(), stormWindow)) {
                  applyAnomalyStorm(level, player, data, chunk);
               }
            }
         }
      }
   }

   private static void applyAnomalyStorm(ServerLevel level, ServerPlayer player, NexusWorldData data, ChunkPos chunk) {
      if (!player.chunkPosition().equals(chunk)) {
         return;
      }

      player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0));
      if (data.fieldValue(chunk) < 30) {
         player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
      }
      if (data.fieldValue(chunk) < 20) {
         player.hurtServer(level, level.damageSources().magic(), 1.5F);
      }
      if (level.getGameTime() % 160L == 0L) {
         level.playSound(null, player.blockPosition(), ModSounds.REALITY_TEAR_PULSE.get(), SoundSource.AMBIENT, 0.35F, data.fieldValue(chunk) < 20 ? 0.65F : 0.9F);
      }
      if (level.getRandom().nextInt(data.fieldValue(chunk) < 20 ? 3 : 8) == 0 && canSpawnStormMob(level, player.blockPosition())) {
         EntityType<?> type = data.fieldValue(chunk) < 20 ? (EntityType<?>)ModEntities.DATA_WRAITH.get() : (EntityType<?>)ModEntities.STATIC_CRAWLER.get();
         if (type.create(level, EntitySpawnReason.EVENT) instanceof NexusMobEntity mob) {
            BlockPos pos = player.blockPosition().offset(level.getRandom().nextInt(9) - 4, 0, level.getRandom().nextInt(9) - 4);
            BlockPos surface = level.getHeightmapPos(Types.WORLD_SURFACE, pos);
            mob.setPos(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
            level.addFreshEntity(mob);
         }
      }
   }

   public static boolean canSpawnStormMob(ServerLevel level, BlockPos center) {
      return nearbyStormMobCount(level, center) < STORM_MOB_CAP;
   }

   public static int nearbyStormMobCount(ServerLevel level, BlockPos center) {
      return level.getEntitiesOfClass(NexusMobEntity.class, new AABB(center).inflate(STORM_MOB_CAP_RADIUS), NexusMobEntity::isAlive).size();
   }

   private static void mutateOneBlock(ServerLevel level, NexusWorldData data, ChunkPos chunk) {
      if (!data.isQuarantined(chunk)
         && (data.fieldValue(chunk) < (Integer)Config.CORRUPTION_SPREAD_FIELD_THRESHOLD.get()
            || data.corruptionPressure(chunk) >= (Integer)Config.CORRUPTION_SPREAD_PRESSURE_THRESHOLD.get())) {
         int salt = (int)(level.getGameTime() / Math.max(1, (Integer)Config.FIELD_TICK_INTERVAL.get()));
         int x = chunk.getBlockX(Math.floorMod(chunk.x() * 3 + salt, 16));
         int z = chunk.getBlockZ(Math.floorMod(chunk.z() * 5 + salt, 16));
         BlockPos surface = level.getHeightmapPos(Types.WORLD_SURFACE, new BlockPos(x, 0, z)).below();
         if (data.fieldValue(chunk) < (Integer)Config.REALITY_TEAR_FIELD_THRESHOLD.get() && data.realityTearCount(chunk) == 0) {
            BlockPos tearPos = surface.above();
            if (level.getBlockState(tearPos).isAir()) {
               level.setBlock(tearPos, ((Block)ModBlocks.REALITY_TEAR.get()).defaultBlockState(), 3);
               data.markRealityTearActive(chunk);
               level.playSound(null, tearPos, ModSounds.REALITY_TEAR_PULSE.get(), SoundSource.BLOCKS, 0.65F, 0.75F);
               return;
            }
         }

         BlockState corrupted = ModBlocks.corruptedVariant(level.getBlockState(surface));
         if (corrupted != null) {
            level.setBlock(surface, corrupted, 3);
            data.addCorruptionPressure(chunk, 1);
         }
      }
   }
}

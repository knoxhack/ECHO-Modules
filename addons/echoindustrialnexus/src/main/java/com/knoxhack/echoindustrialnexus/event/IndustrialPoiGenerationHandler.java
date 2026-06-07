package com.knoxhack.echoindustrialnexus.event;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoindustrialnexus.Config;
import com.knoxhack.echoindustrialnexus.progress.IndustrialWorldProgress;
import com.knoxhack.echoindustrialnexus.worldgen.IndustrialPoiGenerator;
import com.knoxhack.echoindustrialnexus.worldgen.IndustrialPoiType;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class IndustrialPoiGenerationHandler {
   private IndustrialPoiGenerationHandler() {
   }

   public static void onChunkLoad(Object event) {
      ServerLevel level = EchoBackendWorldEventBridge.chunkLoadServerLevel(event);
      if (level == null || !EchoBackendWorldEventBridge.isNewServerChunkLoad(event) || !Config.PROCEDURAL_POIS_ENABLED.get()) {
         return;
      }
      ChunkAccess chunk = EchoBackendWorldEventBridge.chunkLoadChunk(event);
      if (chunk == null) {
         return;
      }
      int chunkX = chunk.getPos().getWorldPosition().getX() >> 4;
      int chunkZ = chunk.getPos().getWorldPosition().getZ() >> 4;
      for (IndustrialPoiType type : IndustrialPoiType.values()) {
         if (selected(level, chunkX, chunkZ, type)) {
            BlockPos center = chunk.getPos().getWorldPosition().offset(8, 0, 8);
            IndustrialWorldProgress.get(level).schedulePendingPoi(type.id(), center);
            return;
         }
      }
   }

   public static void onLevelTick(Object event) {
      ServerLevel level = EchoBackendWorldEventBridge.postTickServerLevel(event);
      if (level == null || !Config.PROCEDURAL_POIS_ENABLED.get() || level.getGameTime() % 40L != 0L) {
         return;
      }
      IndustrialWorldProgress progress = IndustrialWorldProgress.get(level);
      for (IndustrialWorldProgress.PendingPoi pending : progress.pendingPoiCandidates(4)) {
         IndustrialPoiType type = typeById(pending.type());
         if (type == null) {
            progress.removePendingPoi(pending);
            continue;
         }
         if (IndustrialPoiGenerator.canGenerate(level, pending.pos(), type)) {
            IndustrialPoiGenerator.generate(level, pending.pos(), type, level.getRandom());
            progress.removePendingPoi(pending);
         } else {
            progress.reschedulePendingPoi(pending);
         }
      }
   }

   private static IndustrialPoiType typeById(String id) {
      for (IndustrialPoiType type : IndustrialPoiType.values()) {
         if (type.id().equals(id)) {
            return type;
         }
      }
      return null;
   }

   private static boolean selected(ServerLevel level, int chunkX, int chunkZ, IndustrialPoiType type) {
      int spacing = Math.max(12, Config.POI_SPACING_CHUNKS.get() + spacingBonus(type));
      int regionX = Math.floorDiv(chunkX, spacing);
      int regionZ = Math.floorDiv(chunkZ, spacing);
      Random random = new Random(level.getSeed() + (long)regionX * 341873128712L + (long)regionZ * 132897987541L + type.salt());
      int selectedX = regionX * spacing + random.nextInt(spacing);
      int selectedZ = regionZ * spacing + random.nextInt(spacing);
      return chunkX == selectedX && chunkZ == selectedZ;
   }

   private static int spacingBonus(IndustrialPoiType type) {
      return switch (type) {
         case ABANDONED_THERMAL_PLANT -> Config.THERMAL_PLANT_SPACING_BONUS.get();
         case RUSTED_FACTORY_COMPLEX -> Config.FACTORY_SPACING_BONUS.get();
         case GEOTHERMAL_DRILL_SITE -> Config.GEOTHERMAL_SPACING_BONUS.get();
         case REACTOR_COOLING_STATION -> Config.REACTOR_SPACING_BONUS.get();
         case NEXUS_HEAT_EXCHANGER_RUINS -> Config.NEXUS_EXCHANGER_SPACING_BONUS.get();
      };
   }
}

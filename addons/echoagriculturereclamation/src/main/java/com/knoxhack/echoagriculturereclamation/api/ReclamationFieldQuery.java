package com.knoxhack.echoagriculturereclamation.api;

import com.knoxhack.echoagriculturereclamation.content.ReclamationMetrics;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ReclamationFieldQuery {
   private ReclamationFieldQuery() {
   }

   public static ReclamationFieldSnapshot player(Player player) {
      if (player == null) {
         return null;
      }
      return at(player.level(), player.blockPosition(), player);
   }

   public static ReclamationFieldSnapshot at(Level level, BlockPos center, Player player) {
      if (level == null) {
         return null;
      }
      BlockPos safeCenter = center == null ? BlockPos.ZERO : center.immutable();
      ReclamationProgress.GreenhouseContext greenhouse = ReclamationProgress.greenhouseContext(level, safeCenter);
      SoilState soil = ReclamationProgress.detectSoil(level, safeCenter);
      int restoration = 0;
      long lastScan = 0L;
      int lastThreshold = 0;
      String blocker = greenhouse.nextAction();
      if (level instanceof ServerLevel serverLevel) {
         ReclamationWorldData world = ReclamationWorldData.get(serverLevel);
         ChunkPos chunk = chunk(safeCenter);
         restoration = world.restorationScore(chunk);
         ReclamationWorldData.FieldHistory history = world.fieldHistory(chunk);
         lastScan = history.lastScanTime();
         lastThreshold = history.lastRestorationThreshold();
         blocker = history.lastMeaningfulBlocker().isBlank() ? blocker : history.lastMeaningfulBlocker();
      }
      int knownSeeds = 0;
      int food = 0;
      if (player != null) {
         ReclamationMetrics metrics = ReclamationProgress.metrics(player);
         knownSeeds = metrics.knownSeeds();
         food = metrics.foodSecurity();
      }
      BlockPos controller = greenhouse.zone() == null ? BlockPos.ZERO : greenhouse.zone().controllerPos();
      ReclamationFieldSnapshot snapshot = new ReclamationFieldSnapshot(
         level.dimension(),
         chunk(safeCenter),
         safeCenter,
         soil,
         restoration,
         greenhouse.qualityLabel(),
         controller,
         greenhouse.scan().cropTargets(),
         greenhouse.scan().deployedDrones(),
         greenhouse.scan().serviceTargets(),
         knownSeeds,
         food,
         greenhouse.nextAction(),
         lastScan,
         lastThreshold,
         blocker
      );
      ReclamationIntegrationServices.publishFieldSnapshot(snapshot);
      return snapshot;
   }

   public static ReclamationFieldSnapshot controller(ServerLevel level, BlockPos controller, Player player) {
      return at(level, controller, player);
   }

   public static ReclamationFieldSnapshot chunk(ServerLevel level, ChunkPos chunk, Player player) {
      if (level == null || chunk == null) {
         return null;
      }
      return at(level, new BlockPos(chunk.getMiddleBlockX(), level.getSeaLevel(), chunk.getMiddleBlockZ()), player);
   }

   private static ChunkPos chunk(BlockPos pos) {
      return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
   }
}

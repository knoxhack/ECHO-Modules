package com.knoxhack.echonexusprotocol.integration;

import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class NexusIndustrialCompat {
   private NexusIndustrialCompat() {
   }

   public static void stabilizeIndustrialBlight(ServerLevel level, BlockPos pos, int radius, int intensity) {
      if (level == null || pos == null) {
         return;
      }
      int safeRadius = Math.max(1, radius);
      int safeIntensity = Math.max(1, intensity);
      NexusWorldData data = NexusWorldData.get(level);
      for (ChunkPos chunk : affectedChunks(pos, safeRadius)) {
         data.addFieldValue(chunk, safeIntensity);
         data.addCorruptionPressure(chunk, -safeIntensity * 2);
         data.quarantineChunk(chunk, Math.max(80, safeIntensity * 80));
      }
   }

   public static void recordThermalPressure(ServerLevel level, BlockPos pos, int intensity) {
      if (level == null || pos == null) {
         return;
      }
      int safeIntensity = Math.max(1, intensity);
      NexusWorldData data = NexusWorldData.get(level);
      ChunkPos chunk = chunk(pos);
      data.addFieldValue(chunk, -safeIntensity);
      data.addCorruptionPressure(chunk, safeIntensity * 3);
      if (safeIntensity >= 3) {
         data.markRealityTearActive(chunk);
      }
   }

   public static void recordStaticFluidLeak(ServerLevel level, BlockPos pos, int amount) {
      if (level == null || pos == null || amount <= 0) {
         return;
      }
      int intensity = Math.max(1, amount / 100);
      NexusWorldData data = NexusWorldData.get(level);
      ChunkPos chunk = chunk(pos);
      data.addFieldValue(chunk, -intensity);
      data.addCorruptionPressure(chunk, intensity * 2);
   }

   private static java.util.List<ChunkPos> affectedChunks(BlockPos pos, int radius) {
      java.util.LinkedHashSet<ChunkPos> chunks = new java.util.LinkedHashSet<>();
      chunks.add(chunk(pos));
      chunks.add(chunk(pos.offset(radius, 0, 0)));
      chunks.add(chunk(pos.offset(-radius, 0, 0)));
      chunks.add(chunk(pos.offset(0, 0, radius)));
      chunks.add(chunk(pos.offset(0, 0, -radius)));
      return java.util.List.copyOf(chunks);
   }

   private static ChunkPos chunk(BlockPos pos) {
      return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
   }
}

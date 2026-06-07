package com.knoxhack.echorendercore.client;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class RenderCoreStaticSurfaceRegistry {
   private static final Map<Identifier, StaticSurface> SURFACES = new LinkedHashMap<>();
   private static final int DEFAULT_RADIUS = 10;
   private static final int MAX_SURFACES_PER_SCAN = 24;
   private static long lastScanTick = Long.MIN_VALUE;

   private RenderCoreStaticSurfaceRegistry() {
   }

   public static void register(Identifier blockId, Identifier profileId, String surfaceType) {
      if (blockId == null || profileId == null) {
         return;
      }
      SURFACES.put(blockId, new StaticSurface(blockId, profileId,
         surfaceType == null || surfaceType.isBlank() ? "static_block" : surfaceType));
   }

   public static int registeredCount() {
      return SURFACES.size();
   }

   public static void onClientTick() {
      if (SURFACES.isEmpty()) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.player == null) {
         return;
      }
      Level level = minecraft.level;
      long tick = level.getGameTime();
      if (tick - lastScanTick < 8L) {
         return;
      }
      lastScanTick = tick;
      BlockPos center = minecraft.player.blockPosition();
      int submitted = 0;
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
      for (int y = -3; y <= 3 && submitted < MAX_SURFACES_PER_SCAN; y++) {
         for (int x = -DEFAULT_RADIUS; x <= DEFAULT_RADIUS && submitted < MAX_SURFACES_PER_SCAN; x++) {
            for (int z = -DEFAULT_RADIUS; z <= DEFAULT_RADIUS && submitted < MAX_SURFACES_PER_SCAN; z++) {
               cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
               Identifier blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(cursor).getBlock());
               StaticSurface surface = SURFACES.get(blockId);
               if (surface == null) {
                  continue;
               }
               RenderCoreBlockVisuals.RenderData data = RenderCoreBlockVisuals.resolve(
                  level,
                  cursor,
                  level.getBlockState(cursor),
                  RenderCoreBlockVisuals.staticHost(surface.profileId(), surface.surfaceType()),
                  0.0F,
                  0
               );
               RenderCoreBlockVisuals.spawnParticlesOncePerTick(data);
               submitted++;
            }
         }
      }
   }

   public record StaticSurface(Identifier blockId, Identifier profileId, String surfaceType) {
   }
}

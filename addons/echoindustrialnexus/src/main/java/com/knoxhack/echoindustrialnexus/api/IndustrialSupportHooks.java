package com.knoxhack.echoindustrialnexus.api;

import com.knoxhack.echoindustrialnexus.integration.IndustrialCompat;
import com.knoxhack.echoindustrialnexus.integration.IndustrialMissionHooks;
import com.knoxhack.echoindustrialnexus.progress.IndustrialProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Optional-safe hooks for sibling ECHO modules that need to report or consume Industrial support.
 */
public final class IndustrialSupportHooks {
   private IndustrialSupportHooks() {
   }

   public static void applyScrubber(ServerLevel level, BlockPos pos, String mode) {
      IndustrialCompat.applyScrubber(level, pos, mode);
   }

   public static void recordNexusThermalPressure(ServerLevel level, BlockPos pos, int intensity) {
      IndustrialCompat.recordNexusThermalPressure(level, pos, intensity);
   }

   public static void recordStaticFluidLeak(ServerLevel level, BlockPos pos, int fluidId, int amount) {
      IndustrialCompat.recordStaticFluidLeak(level, pos, fluidId, amount);
   }

   public static void recordIndustrialOutput(Level level, BlockPos pos, ItemStack output) {
      IndustrialCompat.recordIndustrialOutput(level, pos, output);
   }

   public static void recordFluxGeneratedNearby(Level level, BlockPos pos, int amount) {
      IndustrialProgress.recordFluxGeneratedNearby(level, pos, amount);
   }

   public static void recordPoiGenerated(ServerLevel level, String type, BlockPos pos) {
      IndustrialProgress.recordPoiGenerated(level, type, pos);
   }

   public static void markMultiblockFormed(Player player, Identifier definitionId) {
      IndustrialProgress.markMultiblockFormed(player, definitionId);
   }

   public static void recordAutomationTask(Player player, Identifier taskId) {
      IndustrialMissionHooks.recordAutomationTask(player, taskId);
   }

   public static void markWardenDefeated(Player player) {
      IndustrialProgress.markWardenDefeated(player);
   }
}

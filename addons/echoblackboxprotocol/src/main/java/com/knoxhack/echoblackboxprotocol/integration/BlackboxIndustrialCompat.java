package com.knoxhack.echoblackboxprotocol.integration;

import com.knoxhack.echoblackboxprotocol.progression.BlackboxProgress;
import net.minecraft.world.entity.player.Player;

public final class BlackboxIndustrialCompat {
   private BlackboxIndustrialCompat() {
   }

   public static void recordIndustrialComponent(Player player, String componentId) {
      if (player == null || componentId == null || componentId.isBlank()) {
         return;
      }
      BlackboxProgress progress = BlackboxProgress.get(player);
      switch (componentId) {
         case "memory_stabilizer_casing", "blackbox_decoder_cooling_system" -> {
            progress.stability(Math.min(100, progress.stability() + 10));
            progress.falseSignals(Math.max(0, progress.falseSignalCount() - 1));
         }
         case "core_key_assembly" -> progress.markNexusCoreAccessKey();
         case "truth_engine_part", "protocol_extractor_coil" -> progress.markRestoreStabilized();
         default -> {
         }
      }
   }

   public static void stabilizeMemory(Player player, int stabilityGain, int falseSignalRelief) {
      if (player == null) {
         return;
      }
      BlackboxProgress progress = BlackboxProgress.get(player);
      if (stabilityGain > 0) {
         progress.stability(Math.min(100, progress.stability() + stabilityGain));
      }
      if (falseSignalRelief > 0) {
         progress.falseSignals(Math.max(0, progress.falseSignalCount() - falseSignalRelief));
      }
   }
}

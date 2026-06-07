package com.knoxhack.echoagriculturereclamation.api;

import com.knoxhack.echoagriculturereclamation.content.SoilState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record ReclamationFieldSnapshot(
   ResourceKey<Level> dimension,
   ChunkPos chunk,
   BlockPos center,
   SoilState soilState,
   int restorationScore,
   String greenhouseQuality,
   BlockPos controllerPos,
   int cropTargetCount,
   int deployedDroneCount,
   int serviceTargetCount,
   int knownSeedCount,
   int foodSecurity,
   String nextAction,
   long lastScanTime,
   int lastRestorationThreshold,
   String lastMeaningfulBlocker
) {
   public ReclamationFieldSnapshot {
      soilState = soilState == null ? SoilState.DEAD : soilState;
      greenhouseQuality = greenhouseQuality == null || greenhouseQuality.isBlank() ? "unregistered" : greenhouseQuality.strip();
      center = center == null ? BlockPos.ZERO : center.immutable();
      controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
      cropTargetCount = Math.max(0, cropTargetCount);
      deployedDroneCount = Math.max(0, deployedDroneCount);
      serviceTargetCount = Math.max(0, serviceTargetCount);
      knownSeedCount = Math.max(0, knownSeedCount);
      foodSecurity = Math.max(0, Math.min(100, foodSecurity));
      restorationScore = Math.max(0, Math.min(100, restorationScore));
      lastRestorationThreshold = Math.max(0, Math.min(100, lastRestorationThreshold));
      nextAction = nextAction == null || nextAction.isBlank() ? "Scan FIELD > Reclamation for current status." : nextAction.strip();
      lastMeaningfulBlocker = lastMeaningfulBlocker == null ? "" : lastMeaningfulBlocker.strip();
   }

   public boolean hasController() {
      return !BlockPos.ZERO.equals(controllerPos);
   }

   public boolean restored() {
      return restorationScore >= 100;
   }
}

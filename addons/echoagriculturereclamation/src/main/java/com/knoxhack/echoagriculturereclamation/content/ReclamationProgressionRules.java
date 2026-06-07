package com.knoxhack.echoagriculturereclamation.content;

import net.minecraft.util.RandomSource;

public record ReclamationProgressionRules(
   int greenhouseSafeThreshold,
   int foodKnownSeedBonus,
   int foodItemValue,
   int recoveredSeedMinStability,
   int recoveredSeedStabilityRange,
   int recoveredSeedMinContamination,
   int recoveredSeedContaminationRange,
   int scannerUnsafeRestorationGain,
   int scannerSafeRestorationGain,
   int purifyThreshold,
   int stabilizeThreshold,
   int restoreThreshold,
   int cropPurifyMaxLow,
   int cropPurifyMaxHigh,
   int cropStabilizeMax,
   int cropRestoreMax,
   int scannerPurifyMaxLow,
   int scannerPurifyMaxHigh,
   int scannerStabilizeMax,
   int scannerRestoreMax,
   int restorationCropWeightForStabilization
) {
   public static ReclamationProgressionRules defaults() {
      return new ReclamationProgressionRules(70, 3, 1, 24, 32, 1, 3, 1, 2, 25, 60, 100, 4, 8, 4, 12, 2, 4, 2, 4, 2);
   }

   public SeedProfile recoveredProfile(CropSpec spec, RandomSource random) {
      int stability = recoveredSeedMinStability + random.nextInt(Math.max(1, recoveredSeedStabilityRange));
      int contamination = recoveredSeedMinContamination + random.nextInt(Math.max(1, recoveredSeedContaminationRange));
      return new SeedProfile(spec.path(), contamination, stability);
   }

   public ReclamationProgressionRules normalized() {
      return new ReclamationProgressionRules(
         clamp(greenhouseSafeThreshold, 0, 100),
         Math.max(0, foodKnownSeedBonus),
         Math.max(0, foodItemValue),
         clamp(recoveredSeedMinStability, 0, 100),
         Math.max(1, recoveredSeedStabilityRange),
         clamp(recoveredSeedMinContamination, 0, 5),
         Math.max(1, recoveredSeedContaminationRange),
         Math.max(0, scannerUnsafeRestorationGain),
         Math.max(0, scannerSafeRestorationGain),
         clamp(purifyThreshold, 0, 100),
         clamp(stabilizeThreshold, 0, 100),
         clamp(restoreThreshold, 0, 100),
         Math.max(0, cropPurifyMaxLow),
         Math.max(0, cropPurifyMaxHigh),
         Math.max(0, cropStabilizeMax),
         Math.max(0, cropRestoreMax),
         Math.max(0, scannerPurifyMaxLow),
         Math.max(0, scannerPurifyMaxHigh),
         Math.max(0, scannerStabilizeMax),
         Math.max(0, scannerRestoreMax),
         Math.max(0, restorationCropWeightForStabilization)
      );
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}

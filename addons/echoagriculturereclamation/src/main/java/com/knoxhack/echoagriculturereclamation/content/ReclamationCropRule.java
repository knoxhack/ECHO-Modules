package com.knoxhack.echoagriculturereclamation.content;

public record ReclamationCropRule(
   String cropId,
   int baseGrowthChance,
   int baseYield,
   int restorationWeight,
   int greenhouseBypassThreshold,
   int soilGrowthDivisor,
   int greenhouseGrowthDivisor,
   int stabilityGrowthDivisor,
   int nutrientGrowthBonus,
   int stableGrowthBonus,
   int hydroponicYieldBonus,
   int stableYieldBonus,
   int safeGreenhouseYieldBonus,
   int contaminatedSeedReturnCeiling,
   int seedStabilityLoss,
   int seedContaminationIncrease,
   int failedGrowthDeathChance
) {
   public static ReclamationCropRule defaultFor(CropSpec spec) {
      return new ReclamationCropRule(spec.path(), spec.baseGrowthChance(), spec.baseYield(), spec.restorationWeight(), 55, 2, 5, 6, 4, 10, 0, 1, 1, 95, 15, 1, 4);
   }

   public ReclamationCropRule normalized(CropSpec spec) {
      return new ReclamationCropRule(
         spec.path(),
         clamp(baseGrowthChance, 0, 95),
         Math.max(1, baseYield),
         Math.max(0, restorationWeight),
         clamp(greenhouseBypassThreshold, 0, 100),
         Math.max(1, soilGrowthDivisor),
         Math.max(1, greenhouseGrowthDivisor),
         Math.max(1, stabilityGrowthDivisor),
         Math.max(0, nutrientGrowthBonus),
         Math.max(0, stableGrowthBonus),
         Math.max(0, hydroponicYieldBonus),
         Math.max(0, stableYieldBonus),
         Math.max(0, safeGreenhouseYieldBonus),
         clamp(contaminatedSeedReturnCeiling, 0, 100),
         Math.max(0, seedStabilityLoss),
         Math.max(0, seedContaminationIncrease),
         clamp(failedGrowthDeathChance, 0, 100)
      );
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}

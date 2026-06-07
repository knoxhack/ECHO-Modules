package com.knoxhack.echoagriculturereclamation.content;

import com.knoxhack.echoagriculturereclamation.config.ReclamationConfig;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress.GreenhouseContext;
import net.minecraft.util.RandomSource;

public final class ReclamationCropLogic {
   private ReclamationCropLogic() {
   }

   public static SeedProfile fallbackProfile(CropSpec spec, boolean stable) {
      return stable ? new SeedProfile(spec.path(), 0, 100) : new SeedProfile(spec.path(), 1, 35);
   }

   public static boolean stable(SeedProfile profile) {
      return profile != null && profile.contaminationTier() == 0 && profile.stability() >= 80;
   }

   public static boolean canGrow(CropSpec spec, SoilState soil, SeedProfile profile, int greenhouseSafety) {
      ReclamationCropRule crop = ReclamationContent.crop(spec);
      return soil.canSupport(spec, profile.stability()) || greenhouseSafety >= crop.greenhouseBypassThreshold();
   }

   public static boolean canGrow(CropSpec spec, SoilState soil, SeedProfile profile, GreenhouseContext greenhouse) {
      return canGrow(spec, soil, profile, greenhouse.score());
   }

   public static int growthChance(CropSpec spec, SoilState soil, SeedProfile profile, int greenhouseSafety, int nutrient) {
      ReclamationCropRule crop = ReclamationContent.crop(spec);
      int stableBonus = stable(profile) ? crop.stableGrowthBonus() + ReclamationConfig.stableSeedGrowthBonus() : 0;
      return Math.min(95, crop.baseGrowthChance()
         + soil.growthChance() / crop.soilGrowthDivisor()
         + greenhouseSafety / crop.greenhouseGrowthDivisor()
         + profile.stability() / crop.stabilityGrowthDivisor()
         + nutrient * crop.nutrientGrowthBonus()
         + stableBonus
         + ReclamationConfig.globalGrowthChanceBonus());
   }

   public static int growthChance(CropSpec spec, SoilState soil, SeedProfile profile, GreenhouseContext greenhouse, int nutrient) {
      return Math.max(1, growthChance(spec, soil, profile, greenhouse.score(), nutrient) - greenhouse.growthPenalty());
   }

   public static int yield(CropSpec spec, SeedProfile profile, int greenhouseSafety, boolean hydroponic) {
      ReclamationCropRule crop = ReclamationContent.crop(spec);
      int safeGreenhouseBonus = greenhouseSafety >= ReclamationContent.progression().greenhouseSafeThreshold() ? crop.safeGreenhouseYieldBonus() : 0;
      return Math.max(1, crop.baseYield()
         + (hydroponic ? crop.hydroponicYieldBonus() : 0)
         + (stable(profile) ? crop.stableYieldBonus() : 0)
         + safeGreenhouseBonus);
   }

   public static int yield(CropSpec spec, SeedProfile profile, GreenhouseContext greenhouse, boolean hydroponic) {
      return ReclamationCropLogic.yield(spec, profile, greenhouse.score(), hydroponic);
   }

   public static boolean shouldReturnContaminatedSeed(RandomSource random, SeedProfile profile, int greenhouseSafety) {
      if (stable(profile)) {
         return false;
      }
      int ceiling = ReclamationContent.crop(profile.spec()).contaminatedSeedReturnCeiling();
      return random.nextInt(100) >= Math.min(ceiling, greenhouseSafety + profile.stability());
   }

   public static boolean shouldReturnContaminatedSeed(RandomSource random, SeedProfile profile, GreenhouseContext greenhouse) {
      return shouldReturnContaminatedSeed(random, profile, greenhouse.seedSafety());
   }

   public static SeedProfile degradedSeed(SeedProfile profile) {
      ReclamationCropRule crop = ReclamationContent.crop(profile.spec());
      return new SeedProfile(
         profile.cropId(),
         Math.min(5, profile.contaminationTier() + crop.seedContaminationIncrease()),
         Math.max(10, profile.stability() - crop.seedStabilityLoss())
      );
   }

   public static int restorationGain(CropSpec spec, SeedProfile profile, int greenhouseSafety) {
      return Math.max(1, ReclamationContent.crop(spec).restorationWeight() + (stable(profile) ? 1 : 0) + greenhouseSafety / 40);
   }

   public static int restorationGain(CropSpec spec, SeedProfile profile, GreenhouseContext greenhouse) {
      return greenhouse.restorationGain(restorationGain(spec, profile, greenhouse.score()));
   }
}

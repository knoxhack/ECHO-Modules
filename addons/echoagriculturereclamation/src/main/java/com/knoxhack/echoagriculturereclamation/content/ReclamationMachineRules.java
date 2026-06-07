package com.knoxhack.echoagriculturereclamation.content;

public record ReclamationMachineRules(
   int soilPurifierRadius,
   int soilPurifierEnzymeBlocks,
   int soilPurifierNutrientBlocks,
   int hydroponicGrowthTicks,
   int hydroponicNutrientCap,
   int hydroponicNutrientPerMix,
   int bioReactorOrganicOutput,
   int bioReactorGeneSampleOutput,
   int compostRecyclerOutput,
   int greenhouseHorizontalRange,
   int greenhouseDownRange,
   int greenhouseUpRange,
   int greenhouseGlassWeight,
   int greenhouseFilterWeight,
   int greenhouseDockWeight,
   int greenhouseControllerWeight,
   int greenhouseTrayWeight,
   int pollinatorDroneServiceRadius,
   int pollinatorDroneHomeRadius,
   int pollinatorDroneServiceTicks,
   int pollinatorDroneGrowthBonus
) {
   public static ReclamationMachineRules defaults() {
      return new ReclamationMachineRules(3, 12, 5, 180, 8, 3, 1, 2, 1, 6, 4, 6, 2, 18, 14, 10, 4, 4, 8, 120, 12);
   }

   public ReclamationMachineRules normalized() {
      return new ReclamationMachineRules(
         Math.max(0, soilPurifierRadius),
         Math.max(0, soilPurifierEnzymeBlocks),
         Math.max(0, soilPurifierNutrientBlocks),
         Math.max(1, hydroponicGrowthTicks),
         Math.max(1, hydroponicNutrientCap),
         Math.max(1, hydroponicNutrientPerMix),
         Math.max(1, bioReactorOrganicOutput),
         Math.max(1, bioReactorGeneSampleOutput),
         Math.max(1, compostRecyclerOutput),
         Math.max(0, greenhouseHorizontalRange),
         Math.max(0, greenhouseDownRange),
         Math.max(0, greenhouseUpRange),
         Math.max(0, greenhouseGlassWeight),
         Math.max(0, greenhouseFilterWeight),
         Math.max(0, greenhouseDockWeight),
         Math.max(0, greenhouseControllerWeight),
         Math.max(0, greenhouseTrayWeight),
         Math.max(1, pollinatorDroneServiceRadius),
         Math.max(1, pollinatorDroneHomeRadius),
         Math.max(20, pollinatorDroneServiceTicks),
         Math.max(0, pollinatorDroneGrowthBonus)
      );
   }
}

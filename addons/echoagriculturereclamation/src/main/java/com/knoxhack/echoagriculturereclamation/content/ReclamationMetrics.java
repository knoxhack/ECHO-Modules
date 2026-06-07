package com.knoxhack.echoagriculturereclamation.content;

public record ReclamationMetrics(
   int knownSeeds,
   SoilState soilState,
   int greenhouseSafety,
   int cropStability,
   int foodSecurity,
   int restorationScore
) {
   public String soilLabel() {
      return soilState.displayName();
   }
}

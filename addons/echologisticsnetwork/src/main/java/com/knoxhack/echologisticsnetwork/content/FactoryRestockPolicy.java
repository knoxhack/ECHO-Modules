package com.knoxhack.echologisticsnetwork.content;

import net.minecraft.resources.Identifier;

public record FactoryRestockPolicy(
   Identifier factoryTaskId,
   int targetRuns,
   int minRuns,
   int maxInFlight,
   int cooldownTicks
) {
   private static final FactoryRestockPolicy DISABLED =
      new FactoryRestockPolicy(null, 0, 0, 0, 200);

   public FactoryRestockPolicy {
      targetRuns = Math.max(0, targetRuns);
      minRuns = targetRuns <= 0 ? 0 : Math.max(1, Math.min(minRuns, targetRuns));
      maxInFlight = targetRuns <= 0 ? 0 : Math.max(1, maxInFlight);
      cooldownTicks = Math.max(20, cooldownTicks);
   }

   public static FactoryRestockPolicy disabled() {
      return DISABLED;
   }

   public boolean enabled() {
      return factoryTaskId != null && targetRuns > 0;
   }

   public FactoryRestockPolicy withTargetRuns(int targetRuns) {
      return new FactoryRestockPolicy(factoryTaskId, targetRuns, Math.min(minRuns, Math.max(0, targetRuns)), maxInFlight, cooldownTicks);
   }
}

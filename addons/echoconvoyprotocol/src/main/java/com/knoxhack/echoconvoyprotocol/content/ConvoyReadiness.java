package com.knoxhack.echoconvoyprotocol.content;

public record ConvoyReadiness(
   int vehicleIntegrity,
   int fuelLevel,
   int cargoLoaded,
   int cargoCapacity,
   int armorRating,
   int threatPreparedness,
   boolean signalLinked,
   int depotIntegrity,
   String routeId,
   boolean logisticsNetworkOnline,
   boolean logisticsLoadoutReady,
   boolean cargoRequestActive,
   boolean cargoDelivered,
   boolean fuelResupplyAvailable,
   boolean dispatchReady
) {
   public ConvoyReadiness {
      vehicleIntegrity = clamp(vehicleIntegrity);
      fuelLevel = clamp(fuelLevel);
      cargoLoaded = Math.max(0, cargoLoaded);
      cargoCapacity = Math.max(1, cargoCapacity);
      armorRating = Math.max(0, armorRating);
      threatPreparedness = clamp(threatPreparedness);
      depotIntegrity = clamp(depotIntegrity);
      routeId = routeId == null || routeId.isBlank() ? "none" : routeId;
   }

   public String summaryLine() {
      return "CONVOY READINESS // Integrity " + vehicleIntegrity + "%, Fuel " + fuelLevel
         + "%, Cargo " + cargoLoaded + "/" + cargoCapacity + ", Armor Tier " + armorRating
         + ", Route " + routeId + ", Logistics " + logisticsSummary()
         + ", Dispatch " + (dispatchReady ? "Ready" : "Blocked");
   }

   public String logisticsSummary() {
      if (!logisticsNetworkOnline) {
         return "offline";
      }
      if (cargoRequestActive) {
         return "delivery active";
      }
      if (cargoDelivered) {
         return "delivered";
      }
      return logisticsLoadoutReady ? "loadout ready" : "waiting";
   }

   public int operationScore() {
      int cargoScore = cargoCapacity <= 0 ? 0 : clamp((int)Math.round(cargoLoaded * 100.0D / cargoCapacity));
      int logisticsScore = logisticsNetworkOnline
         ? (cargoDelivered ? 100 : (logisticsLoadoutReady ? 75 : 45))
         : 35;
      int score = (int)Math.round(
         vehicleIntegrity * 0.22D
            + fuelLevel * 0.18D
            + cargoScore * 0.14D
            + threatPreparedness * 0.14D
            + depotIntegrity * 0.12D
            + (signalLinked ? 10.0D : 0.0D)
            + Math.min(10, armorRating * 2.5D)
            + logisticsScore * 0.10D
      );
      return clamp(score);
   }

   private static int clamp(int value) {
      return Math.max(0, Math.min(100, value));
   }
}

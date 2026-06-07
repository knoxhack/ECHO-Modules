package com.knoxhack.echoconvoyprotocol.upgrade;

public record VehicleUpgradeStats(
   double speedMultiplier,
   float turnBonus,
   double cargoPenaltyReduction,
   int cargoSlotsBonus,
   int maxBatteryBonus,
   int maxDamageBonus,
   int scannerRangeBonus,
   double armorBonus
) {
   public static final VehicleUpgradeStats NONE = new VehicleUpgradeStats(0.0D, 0.0F, 0.0D, 0, 0, 0, 0, 0.0D);
}

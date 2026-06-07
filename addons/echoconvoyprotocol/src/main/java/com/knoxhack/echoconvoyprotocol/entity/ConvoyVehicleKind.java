package com.knoxhack.echoconvoyprotocol.entity;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum ConvoyVehicleKind implements StringRepresentable {
   SCRAP_BIKE("scrap_bike", "Scrap Bike", 80, 0, 45, 6, 0.19D, 4.8F, 0.04D, 0.95D, 0.55D, 0, 0, false),
   WASTELAND_ROVER("wasteland_rover", "Wasteland Rover", 160, 80, 80, 18, 0.16D, 3.5F, 0.12D, 1.35D, 0.72D, 2, 96, false),
   CARGO_CRAWLER("cargo_crawler", "Cargo Crawler", 260, 120, 120, 36, 0.10D, 2.4F, 0.2D, 1.45D, 0.9D, 2, 48, false),
   ARMORED_RELAY_TRUCK("armored_relay_truck", "Armored Relay Truck", 220, 180, 160, 27, 0.13D, 2.8F, 0.32D, 1.65D, 0.82D, 4, 144, true);

   private final String id;
   private final String displayName;
   private final int maxFuel;
   private final int maxBattery;
   private final int maxDamage;
   private final int cargoSlots;
   private final double speed;
   private final float turnRate;
   private final double armor;
   private final double passengerHeight;
   private final double cargoWeightPenalty;
   private final int maxShieldingPlates;
   private final int scannerRange;
   private final boolean deploysFieldStation;

   ConvoyVehicleKind(String id, String displayName, int maxFuel, int maxBattery, int maxDamage, int cargoSlots,
         double speed, float turnRate, double armor, double passengerHeight, double cargoWeightPenalty,
         int maxShieldingPlates, int scannerRange, boolean deploysFieldStation) {
      this.id = id;
      this.displayName = displayName;
      this.maxFuel = maxFuel;
      this.maxBattery = maxBattery;
      this.maxDamage = maxDamage;
      this.cargoSlots = cargoSlots;
      this.speed = speed;
      this.turnRate = turnRate;
      this.armor = armor;
      this.passengerHeight = passengerHeight;
      this.cargoWeightPenalty = cargoWeightPenalty;
      this.maxShieldingPlates = maxShieldingPlates;
      this.scannerRange = scannerRange;
      this.deploysFieldStation = deploysFieldStation;
   }

   @Override
   public String getSerializedName() {
      return id;
   }

   public String displayName() {
      return displayName;
   }

   public int maxFuel() {
      return maxFuel;
   }

   public int maxBattery() {
      return maxBattery;
   }

   public int maxDamage() {
      return maxDamage;
   }

   public int cargoSlots() {
      return cargoSlots;
   }

   public double speed() {
      return speed;
   }

   public float turnRate() {
      return turnRate;
   }

   public double armor() {
      return armor;
   }

   public double passengerHeight() {
      return passengerHeight;
   }

   public double cargoWeightPenalty() {
      return cargoWeightPenalty;
   }

   public int maxShieldingPlates() {
      return maxShieldingPlates;
   }

   public int scannerRange() {
      return scannerRange;
   }

   public boolean deploysFieldStation() {
      return deploysFieldStation;
   }

   public static ConvoyVehicleKind byName(String name) {
      String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
      for (ConvoyVehicleKind kind : values()) {
         if (kind.id.equals(normalized)) {
            return kind;
         }
      }
      return SCRAP_BIKE;
   }

   public static ConvoyVehicleKind byId(int id) {
      ConvoyVehicleKind[] values = values();
      return id >= 0 && id < values.length ? values[id] : SCRAP_BIKE;
   }
}

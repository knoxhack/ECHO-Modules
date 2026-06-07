package com.knoxhack.echoconvoyprotocol.content;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ConvoyFacilityState {
   private int vehicleIntegrity = 70;
   private int fuelLevel;
   private int cargoLoaded;
   private int cargoCapacity = 6;
   private int armorRating;
   private int threatPreparedness = 35;
   private int depotIntegrity = 100;
   private boolean signalLinked;
   private boolean preparedContract;
   private boolean damagedConvoy;
   private int completedMissions;
   private String routeId = "";
   private String activeRouteId = "";
   private String logisticsNetworkId = "global";
   private String logisticsLoadoutId = "";
   private boolean logisticsNetworkOnline;
   private boolean logisticsLoadoutReady;
   private boolean logisticsCargoRequestActive;
   private boolean logisticsCargoDelivered;
   private boolean logisticsFuelResupplyAvailable;
   private int logisticsActiveDeliveries;
   private String lastDiagnostic = "Depot waiting for route data.";

   public ConvoyReadiness readiness() {
      boolean routeReady = !routeId.isBlank() || preparedContract;
      boolean logisticsReady = logisticsLoadoutId == null || logisticsLoadoutId.isBlank()
         || logisticsCargoDelivered
         || (!logisticsCargoRequestActive && cargoLoaded > 0);
      boolean ready = vehicleIntegrity >= 55 && fuelLevel >= 40 && cargoLoaded > 0 && routeReady && signalLinked && depotIntegrity >= 50
         && logisticsReady;
      return new ConvoyReadiness(
         vehicleIntegrity,
         fuelLevel,
         cargoLoaded,
         cargoCapacity,
         armorRating,
         threatPreparedness,
         signalLinked,
         depotIntegrity,
         routeId.isBlank() ? "none" : routeId,
         logisticsNetworkOnline,
         logisticsLoadoutReady,
         logisticsCargoRequestActive,
         logisticsCargoDelivered,
         logisticsFuelResupplyAvailable,
         ready
      );
   }

   public void repairVehicle() {
      vehicleIntegrity = Math.min(100, vehicleIntegrity + 35);
      damagedConvoy = vehicleIntegrity < 60;
      lastDiagnostic = "Vehicle integrity restored to " + vehicleIntegrity + "%.";
   }

   public void installArmor() {
      armorRating = Math.min(4, armorRating + 1);
      threatPreparedness = Math.min(100, threatPreparedness + 20);
      vehicleIntegrity = Math.min(100, vehicleIntegrity + 10);
      lastDiagnostic = "Armor tier increased to " + armorRating + ".";
   }

   public void loadCargo() {
      if (cargoLoaded < cargoCapacity) {
         cargoLoaded++;
      }
      lastDiagnostic = "Cargo manifest updated: " + cargoLoaded + "/" + cargoCapacity + ".";
   }

   public void refuel() {
      fuelLevel = Math.min(100, fuelLevel + 35);
      lastDiagnostic = "Fuel level increased to " + fuelLevel + "%.";
   }

   public void prepareRoute(String routeId) {
      this.routeId = routeId == null || routeId.isBlank() ? "echoconvoyprotocol:echo_7_ruined_highway" : routeId;
      preparedContract = true;
      signalLinked = true;
      lastDiagnostic = "Dispatch contract prepared for " + this.routeId + ".";
   }

   public void prepareRoute(ConvoyRouteDefinition route) {
      if (route == null) {
         prepareRoute("");
         return;
      }
      prepareRoute(route.id().toString());
      logisticsNetworkId = route.logisticsNetworkId();
      logisticsLoadoutId = route.logisticsLoadoutId() == null ? "" : route.logisticsLoadoutId().toString();
      if (route.autoRequestCargo() && !logisticsCargoDelivered) {
         logisticsCargoRequestActive = true;
         lastDiagnostic = "Dispatch contract prepared for " + this.routeId + "; Logistics cargo request is required.";
      }
   }

   public boolean dispatch() {
      ConvoyReadiness readiness = readiness();
      if (!readiness.dispatchReady()) {
         lastDiagnostic = "Dispatch blocked by readiness gate.";
         return false;
      }
      activeRouteId = routeId;
      preparedContract = false;
      logisticsCargoRequestActive = false;
      fuelLevel = Math.max(0, fuelLevel - 25);
      lastDiagnostic = "Convoy dispatched to " + activeRouteId + ".";
      return true;
   }

   public void completeActiveRoute() {
      if (!activeRouteId.isBlank()) {
         completedMissions++;
         cargoLoaded = Math.max(0, cargoLoaded - 1);
         lastDiagnostic = "Convoy returned from " + activeRouteId + " with salvage ready.";
         activeRouteId = "";
      } else {
         lastDiagnostic = "No active route to complete.";
      }
   }

   public void recoverConvoy() {
      damagedConvoy = false;
      vehicleIntegrity = Math.max(vehicleIntegrity, 45);
      activeRouteId = "";
      lastDiagnostic = "Recovery beacon brought a damaged convoy back to depot state.";
   }

   public void updateLogistics(String networkId, String loadoutId, boolean networkOnline, boolean loadoutReady, int activeDeliveries) {
      logisticsNetworkId = networkId == null || networkId.isBlank() ? "global" : networkId;
      logisticsLoadoutId = loadoutId == null ? "" : loadoutId;
      logisticsNetworkOnline = networkOnline;
      logisticsLoadoutReady = loadoutReady;
      logisticsActiveDeliveries = Math.max(0, activeDeliveries);
      logisticsCargoRequestActive = logisticsActiveDeliveries > 0;
      lastDiagnostic = "Logistics sync: network " + logisticsNetworkId + " "
         + (logisticsNetworkOnline ? "online" : "offline")
         + ", loadout " + (logisticsLoadoutReady ? "ready" : "not ready")
         + ", active deliveries " + logisticsActiveDeliveries + ".";
   }

   public void markLogisticsRequestStarted(String networkId, String loadoutId) {
      logisticsNetworkId = networkId == null || networkId.isBlank() ? logisticsNetworkId : networkId;
      logisticsLoadoutId = loadoutId == null ? logisticsLoadoutId : loadoutId;
      logisticsNetworkOnline = true;
      logisticsCargoRequestActive = true;
      logisticsCargoDelivered = false;
      logisticsActiveDeliveries = Math.max(1, logisticsActiveDeliveries);
      lastDiagnostic = "Logistics courier requested for loadout " + logisticsLoadoutId + " on " + logisticsNetworkId + ".";
   }

   public void markLogisticsRequestCancelled() {
      logisticsCargoRequestActive = false;
      logisticsActiveDeliveries = 0;
      lastDiagnostic = "Logistics supply request cancelled.";
   }

   public void markCargoDelivered(int deliveredCargo, int deliveredFuelCells) {
      if (deliveredCargo > 0) {
         cargoLoaded = Math.min(cargoCapacity, cargoLoaded + deliveredCargo);
         logisticsCargoDelivered = true;
      }
      if (deliveredFuelCells > 0) {
         fuelLevel = Math.min(100, fuelLevel + deliveredFuelCells * 20);
         logisticsFuelResupplyAvailable = true;
      }
      logisticsCargoRequestActive = false;
      logisticsActiveDeliveries = 0;
      lastDiagnostic = "Logistics inventory synced: cargo +" + Math.max(0, deliveredCargo)
         + ", fuel cells +" + Math.max(0, deliveredFuelCells) + ".";
   }

   public void markSalvageExported(boolean exported) {
      lastDiagnostic = exported
         ? "Salvage manifest exported into the configured Logistics network."
         : "Logistics export unavailable; salvage remains in the Convoy output crate.";
   }

   public void applyFieldOperationEffect(int fuelDelta, int integrityDelta, int cargoDelta) {
      fuelLevel = Math.max(0, Math.min(100, fuelLevel + fuelDelta));
      vehicleIntegrity = Math.max(0, Math.min(100, vehicleIntegrity + integrityDelta));
      cargoLoaded = Math.max(0, Math.min(cargoCapacity, cargoLoaded + cargoDelta));
      damagedConvoy = vehicleIntegrity < 45;
      lastDiagnostic = "Field operation effect applied: fuel " + signed(fuelDelta)
         + ", integrity " + signed(integrityDelta)
         + ", cargo " + signed(cargoDelta) + ".";
   }

   public void applyCargoCapacityUpgrade() {
      cargoCapacity = Math.min(12, cargoCapacity + 2);
      lastDiagnostic = "Cargo capacity upgraded to " + cargoCapacity + ".";
   }

   public void applyFuelEfficiencyUpgrade() {
      fuelLevel = Math.min(100, fuelLevel + 15);
      lastDiagnostic = "Fuel efficiency calibration improved convoy range.";
   }

   public void applyArmorUpgrade() {
      installArmor();
   }

   public String activeRouteId() {
      return activeRouteId;
   }

   public String routeId() {
      return routeId;
   }

   public String logisticsNetworkId() {
      return logisticsNetworkId == null || logisticsNetworkId.isBlank() ? "global" : logisticsNetworkId;
   }

   public String logisticsLoadoutId() {
      return logisticsLoadoutId == null ? "" : logisticsLoadoutId;
   }

   public boolean logisticsNetworkOnline() {
      return logisticsNetworkOnline;
   }

   public boolean logisticsLoadoutReady() {
      return logisticsLoadoutReady;
   }

   public boolean logisticsCargoRequestActive() {
      return logisticsCargoRequestActive;
   }

   public boolean logisticsCargoDelivered() {
      return logisticsCargoDelivered;
   }

   public boolean logisticsFuelResupplyAvailable() {
      return logisticsFuelResupplyAvailable;
   }

   public int logisticsActiveDeliveries() {
      return logisticsActiveDeliveries;
   }

   public int completedMissions() {
      return completedMissions;
   }

   public boolean damagedConvoy() {
      return damagedConvoy;
   }

   public String lastDiagnostic() {
      return lastDiagnostic;
   }

   public void setLastDiagnostic(String lastDiagnostic) {
      this.lastDiagnostic = lastDiagnostic == null || lastDiagnostic.isBlank() ? "Convoy state updated." : lastDiagnostic;
   }

   private static String signed(int value) {
      return value >= 0 ? "+" + value : Integer.toString(value);
   }

   public void load(ValueInput input) {
      vehicleIntegrity = input.getIntOr("convoy_vehicle_integrity", vehicleIntegrity);
      fuelLevel = input.getIntOr("convoy_fuel_level", fuelLevel);
      cargoLoaded = input.getIntOr("convoy_cargo_loaded", cargoLoaded);
      cargoCapacity = input.getIntOr("convoy_cargo_capacity", cargoCapacity);
      armorRating = input.getIntOr("convoy_armor_rating", armorRating);
      threatPreparedness = input.getIntOr("convoy_threat_preparedness", threatPreparedness);
      depotIntegrity = input.getIntOr("convoy_depot_integrity", depotIntegrity);
      signalLinked = input.getBooleanOr("convoy_signal_linked", signalLinked);
      preparedContract = input.getBooleanOr("convoy_prepared_contract", preparedContract);
      damagedConvoy = input.getBooleanOr("convoy_damaged", damagedConvoy);
      completedMissions = input.getIntOr("convoy_completed_missions", completedMissions);
      routeId = input.getStringOr("convoy_route_id", routeId);
      activeRouteId = input.getStringOr("convoy_active_route_id", activeRouteId);
      logisticsNetworkId = input.getStringOr("convoy_logistics_network_id", logisticsNetworkId);
      logisticsLoadoutId = input.getStringOr("convoy_logistics_loadout_id", logisticsLoadoutId);
      logisticsNetworkOnline = input.getBooleanOr("convoy_logistics_network_online", logisticsNetworkOnline);
      logisticsLoadoutReady = input.getBooleanOr("convoy_logistics_loadout_ready", logisticsLoadoutReady);
      logisticsCargoRequestActive = input.getBooleanOr("convoy_logistics_request_active", logisticsCargoRequestActive);
      logisticsCargoDelivered = input.getBooleanOr("convoy_logistics_cargo_delivered", logisticsCargoDelivered);
      logisticsFuelResupplyAvailable = input.getBooleanOr("convoy_logistics_fuel_resupply", logisticsFuelResupplyAvailable);
      logisticsActiveDeliveries = input.getIntOr("convoy_logistics_active_deliveries", logisticsActiveDeliveries);
      lastDiagnostic = input.getStringOr("convoy_last_diagnostic", lastDiagnostic);
   }

   public void save(ValueOutput output) {
      output.putInt("convoy_vehicle_integrity", vehicleIntegrity);
      output.putInt("convoy_fuel_level", fuelLevel);
      output.putInt("convoy_cargo_loaded", cargoLoaded);
      output.putInt("convoy_cargo_capacity", cargoCapacity);
      output.putInt("convoy_armor_rating", armorRating);
      output.putInt("convoy_threat_preparedness", threatPreparedness);
      output.putInt("convoy_depot_integrity", depotIntegrity);
      output.putBoolean("convoy_signal_linked", signalLinked);
      output.putBoolean("convoy_prepared_contract", preparedContract);
      output.putBoolean("convoy_damaged", damagedConvoy);
      output.putInt("convoy_completed_missions", completedMissions);
      output.putString("convoy_route_id", routeId == null ? "" : routeId);
      output.putString("convoy_active_route_id", activeRouteId == null ? "" : activeRouteId);
      output.putString("convoy_logistics_network_id", logisticsNetworkId == null ? "global" : logisticsNetworkId);
      output.putString("convoy_logistics_loadout_id", logisticsLoadoutId == null ? "" : logisticsLoadoutId);
      output.putBoolean("convoy_logistics_network_online", logisticsNetworkOnline);
      output.putBoolean("convoy_logistics_loadout_ready", logisticsLoadoutReady);
      output.putBoolean("convoy_logistics_request_active", logisticsCargoRequestActive);
      output.putBoolean("convoy_logistics_cargo_delivered", logisticsCargoDelivered);
      output.putBoolean("convoy_logistics_fuel_resupply", logisticsFuelResupplyAvailable);
      output.putInt("convoy_logistics_active_deliveries", logisticsActiveDeliveries);
      output.putString("convoy_last_diagnostic", lastDiagnostic == null ? "" : lastDiagnostic);
   }
}

package com.knoxhack.echoarmory.api;

import com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind;
import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.GearDefinition;
import com.knoxhack.echoarmory.content.ModuleDefinition;
import com.knoxhack.echoarmory.content.RouteProfileDefinition;
import com.knoxhack.echoarmory.data.EnergyState;
import com.knoxhack.echoarmory.data.EquipmentTier;
import com.knoxhack.echoarmory.data.InstabilityState;
import com.knoxhack.echoarmory.item.ArmoryData;
import com.knoxhack.echoarmory.item.ArmoryGearItem;
import com.knoxhack.echoarmory.registry.ModDataComponents;
import com.knoxhack.echoarmory.registry.ModItems;
import com.knoxhack.echoarmory.service.ArmoryReadinessService;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ArmoryReports {
   private ArmoryReports() {
   }

   public static List<ArmoryReadinessService.Report> readinessReports(Player player) {
      return player == null ? List.of() : ArmoryReadinessService.reports(player);
   }

   public static Optional<ArmoryReadinessService.Report> bestReadiness(Player player) {
      return player == null ? Optional.empty() : ArmoryReadinessService.bestReport(player);
   }

   public static List<RouteProfileDefinition> routeProfiles() {
      return ArmoryContent.routeProfiles();
   }

   public static GearStateSummary gearState(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return new GearStateSummary(Identifier.withDefaultNamespace("air"), "empty", "empty", 0, 0, 0, 0, 0, List.of());
      }
      Optional<GearDefinition> gearDefinition = ArmoryData.gear(stack);
      Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
      String title = gearDefinition.map(gear -> gear.title()).orElseGet(() -> stack.getHoverName().getString());
      String kind = stack.getItem() instanceof ArmoryGearItem gearItem ? gearItem.gearKind().name().toLowerCase(java.util.Locale.ROOT) : "item";
      EquipmentTier tier = stack.get(ModDataComponents.EQUIPMENT_TIER.get());
      int tierValue = tier == null ? gearDefinition.map(gear -> gear.tier()).orElse(0) : tier.tier();
      EnergyState energy = stack.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY);
      int energyCapacity = energy.capacity() > 0 ? energy.capacity() : gearDefinition.map(GearDefinition::energyCapacity).orElse(0);
      int energyStored = energy.capacity() > 0 ? energy.stored() : energyCapacity;
      InstabilityState instability = stack.getOrDefault(ModDataComponents.INSTABILITY_STATE.get(), InstabilityState.STABLE);
      List<String> modules = ArmoryData.moduleDefinitions(stack).stream().map(module -> module.id().toString()).toList();
      return new GearStateSummary(
         itemId == null ? Identifier.withDefaultNamespace("air") : itemId,
         title,
         kind,
         tierValue,
         modules.size(),
         energyStored,
         energyCapacity,
         instability.instability(),
         modules
      );
   }

   public static StationOperationPreview stationPreview(StationKind kind, ItemStack gear, ItemStack module, ItemStack aux) {
      StationKind stationKind = kind == null ? StationKind.ARMORY_BENCH : kind;
      String station = stationKind.getSerializedName();
      if (gear == null || gear.isEmpty()) {
         return StationOperationPreview.blocked(station, operation(stationKind), "Insert Armory gear in the gear slot.");
      }
      if (!(gear.getItem() instanceof ArmoryGearItem gearItem) || gearItem.gearKind() == ArmoryGearItem.ArmoryGearKind.MODULE) {
         return StationOperationPreview.blocked(station, operation(stationKind), "Gear slot accepts weapons, armor, and support gear.");
      }
      GearStateSummary summary = gearState(gear);
      int fuel = fuelCount(aux);
      return switch (stationKind) {
         case ARMORY_BENCH -> new StationOperationPreview(station, true, "repair_or_tune",
            "", gear.isDamaged() && aux != null && aux.is(ModItems.ARMORY_ALLOY_PLATE.get()) ? "Repair durability" : "Tune and initialize telemetry",
            "May improve station diagnostics and readiness scoring", fuel, summary.energy(), summary.moduleCount());
         case WEAPON_FORGE -> forgePreview(station, "upgrade_weapon", gearItem, aux, true, summary);
         case ARMOR_FORGE -> forgePreview(station, "upgrade_armor", gearItem, aux, false, summary);
         case MODULE_UPGRADE_TABLE -> modulePreview(station, "install_module", gear, module, summary, "");
         case VEIL_INFUSER -> modulePreview(station, "install_veil_module", gear, module, summary, "veil");
         case CONSTRUCT_DOCK -> modulePreview(station, "install_construct_module", gear, module, summary, "construct");
         case ENERGY_CORE_CHARGING_STATION -> energyPreview(station, gear, aux, summary);
         case SIGIL_ENGRAVER -> new StationOperationPreview(station, true, "engrave", "", "Apply cosmetic sigil trim",
            "No readiness resources are consumed until APPLY succeeds", fuel, summary.energy(), summary.moduleCount());
         case LOADOUT_TERMINAL -> new StationOperationPreview(station, true, "bind_route_kit", "", "Bind best matching route kit",
            "Attaches the highest-scoring defined loadout report", fuel, summary.energy(), summary.moduleCount());
         case WEAPON_RACK, ARMOR_STAND -> new StationOperationPreview(station, true, "stage_readiness", "", "Stage item for nearby route-kit readiness",
            "Nearby readiness scans can count this staged gear", fuel, summary.energy(), summary.moduleCount());
      };
   }

   private static StationOperationPreview forgePreview(
      String station,
      String operation,
      ArmoryGearItem gearItem,
      ItemStack aux,
      boolean weaponStation,
      GearStateSummary summary
   ) {
      ArmoryGearItem.ArmoryGearKind expected = weaponStation ? ArmoryGearItem.ArmoryGearKind.WEAPON : ArmoryGearItem.ArmoryGearKind.ARMOR;
      if (gearItem.gearKind() != expected) {
         return StationOperationPreview.blocked(station, operation, weaponStation ? "Weapon Forge requires weapon gear." : "Armor Forge requires armor gear.");
      }
      if (!upgradeMaterialMatches(aux, summary.tier(), weaponStation)) {
         return StationOperationPreview.blocked(station, operation, "Insert the tier's upgrade material in AUX.");
      }
      return new StationOperationPreview(station, true, operation, "", "Increase gear tier",
         "Higher tier improves route-kit score and combat output", fuelCount(aux), summary.energy(), summary.moduleCount());
   }

   private static StationOperationPreview modulePreview(
      String station,
      String operation,
      ItemStack gear,
      ItemStack module,
      GearStateSummary summary,
      String requiredTag
   ) {
      Optional<GearDefinition> gearDefinition = ArmoryData.gear(gear);
      if (module == null || module.isEmpty()) {
         return StationOperationPreview.blocked(station, operation, "Insert a compatible module in the module slot.");
      }
      Optional<ModuleDefinition> moduleDefinition = ArmoryData.module(module);
      if (moduleDefinition.isEmpty()) {
         return StationOperationPreview.blocked(station, operation, "Insert a compatible module in the module slot.");
      }
      ModuleDefinition definition = moduleDefinition.get();
      if (ArmoryData.modules(gear).contains(definition.id().toString())) {
         return StationOperationPreview.blocked(station, operation, "Module is already installed.");
      }
      if (gearDefinition.isPresent() && ArmoryData.modules(gear).modules().size() >= gearDefinition.get().moduleSlots()) {
         return StationOperationPreview.blocked(station, operation, "Selected gear has no open module slots.");
      }
      if (!requiredTag.isBlank()
         && !definition.effectType().contains(requiredTag)
         && !definition.slotType().contains(requiredTag)
         && !definition.synergyTags().contains(requiredTag)) {
         return StationOperationPreview.blocked(station, operation, "This station requires a " + requiredTag + " module.");
      }
      boolean compatible = gearDefinition
         .map(selectedGear -> selectedGear.allows(definition) && definition.compatibleWith(selectedGear))
         .orElse(false);
      if (!compatible) {
         return StationOperationPreview.blocked(station, operation, "Module is not compatible with the selected gear.");
      }
      return new StationOperationPreview(station, true, operation, "", "Install selected module",
         "Installed modules update readiness protections, synergies, and combat behavior", fuelCount(module), summary.energy(), summary.moduleCount());
   }

   private static StationOperationPreview energyPreview(String station, ItemStack gear, ItemStack aux, GearStateSummary summary) {
      EnergyState energy = gear.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY);
      int capacity = energy.capacity() > 0 ? energy.capacity() : ArmoryData.gear(gear).map(GearDefinition::energyCapacity).orElse(0);
      int stored = energy.capacity() > 0 ? energy.stored() : capacity;
      if (capacity <= 0) {
         return StationOperationPreview.blocked(station, "recharge", "Selected gear has no energy core.");
      }
      if (stored >= capacity) {
         return StationOperationPreview.blocked(station, "recharge", "Energy core is already full.");
      }
      if (!ArmoryData.isRechargeFuel(aux)) {
         return StationOperationPreview.blocked(station, "recharge", "Insert a Veil Crystal or Resonance Shard in AUX.");
      }
      return new StationOperationPreview(station, true, "recharge", "", "Restore energy core",
         "Ready reports can leave EMPTY energy state after recharge", fuelCount(aux), summary.energy(), summary.moduleCount());
   }

   private static boolean upgradeMaterialMatches(ItemStack material, int currentTier, boolean weaponStation) {
      if (material == null || material.isEmpty() || currentTier >= 4) {
         return false;
      }
      if (currentTier <= 1) {
         return weaponStation ? material.is(ModItems.RESONANCE_SHARD.get()) : material.is(ModItems.ARMORY_ALLOY_PLATE.get());
      }
      if (currentTier == 2) {
         return material.is(ModItems.VEIL_CRYSTAL.get());
      }
      return material.is(ModItems.BLACKBOX_FRAGMENT.get());
   }

   private static int fuelCount(ItemStack stack) {
      return stack == null || stack.isEmpty() ? 0 : stack.getCount();
   }

   private static String operation(StationKind kind) {
      return switch (kind) {
         case ARMORY_BENCH -> "repair_or_tune";
         case WEAPON_FORGE -> "upgrade_weapon";
         case ARMOR_FORGE -> "upgrade_armor";
         case ENERGY_CORE_CHARGING_STATION -> "recharge";
         case MODULE_UPGRADE_TABLE -> "install_module";
         case SIGIL_ENGRAVER -> "engrave";
         case LOADOUT_TERMINAL -> "bind_route_kit";
         case WEAPON_RACK, ARMOR_STAND -> "stage_readiness";
         case VEIL_INFUSER -> "install_veil_module";
         case CONSTRUCT_DOCK -> "install_construct_module";
      };
   }
}

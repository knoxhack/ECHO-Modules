package com.knoxhack.echoarmory.integration;

import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiagnosticBlocker;
import com.echoplatform.echocore.api.EchoDiagnosticService;
import com.echoplatform.echocore.api.EchoHazardTelemetry;
import com.echoplatform.echocore.api.EchoHazardTelemetryService;
import com.echoplatform.echocore.api.EchoRouteRecord;
import com.echoplatform.echocore.api.EchoRouteRecordService;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.ArmoryLoadoutDefinition;
import com.knoxhack.echoarmory.item.ArmoryData;
import com.knoxhack.echoarmory.registry.ModItems;
import com.knoxhack.echoarmory.service.ArmoryReadinessService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ArmoryCoreIntegration {
   public static final String CHAPTER_ID = "armory";
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final EchoRouteRecordService ROUTE_SERVICE = ArmoryCoreIntegration::routeRecords;
   private static final EchoDiagnosticService DIAGNOSTIC_SERVICE = ArmoryCoreIntegration::diagnostics;
   private static final EchoHazardTelemetryService HAZARD_SERVICE = ArmoryCoreIntegration::hazardTelemetry;

   private ArmoryCoreIntegration() {
   }

   public static void registerAddonChapter() {
      if (REGISTERED.compareAndSet(false, true) && !EchoAddonRegistry.isRegistered(CHAPTER_ID)) {
         EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
               return CHAPTER_ID;
            }

            @Override
            public String modId() {
               return EchoArmory.MODID;
            }

            @Override
            public String displayName() {
               return "ECHO: Armory";
            }

            @Override
            public String summary() {
               return "Modular weapons, armor, energy cores, Ashfall faction unlock hints, and Terminal-managed combat loadouts.";
            }

            @Override
            public boolean isAvailable(Player player) {
               return player != null;
            }

            @Override
            public String statusLine(Player player) {
               if (player == null) {
                  return "ARMORY: telemetry offline until player context is available.";
               }
               return ArmoryReadinessService.bestReport(player)
                  .map(report -> "ARMORY: " + report.loadout().title() + " " + report.summaryLine() + ".")
                  .orElseGet(() -> "ARMORY: tier " + equippedTier(player) + ", modules " + equippedModules(player) + ".");
            }
         });
      }
      EchoCoreServices.registerRouteRecordService(ROUTE_SERVICE);
      EchoCoreServices.registerDiagnosticService(DIAGNOSTIC_SERVICE);
      EchoCoreServices.registerHazardTelemetryService(HAZARD_SERVICE);
      EchoArmory.LOGGER.info("ECHO platform providers after Armory setup: {}", EchoCoreServices.platformProviderSummary());
   }

   private static List<EchoRouteRecord> routeRecords(Player player) {
      List<EchoRouteRecord> records = new ArrayList<>();
      for (ArmoryLoadoutDefinition loadout : ArmoryContent.loadouts()) {
         ArmoryReadinessService.Report report = ArmoryReadinessService.report(player, loadout);
         records.add(new EchoRouteRecord(
            ArmoryTerminalIds.id("route/" + loadout.id().getPath()),
            CHAPTER_ID,
            loadout.title(),
            "Armory",
            "Mission kit",
            report.state().name(),
            report.summaryLine(),
            report.ready()
         ));
      }
      return List.copyOf(records);
   }

   private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
      if (player == null) {
         return List.of();
      }
      ArrayList<EchoDiagnosticBlocker> blockers = new ArrayList<>();
      if (equippedTier(player) == 0 && !hasInventoryItem(player, "echoarmory:armory_bench")) {
         blockers.add(new EchoDiagnosticBlocker(
            ArmoryTerminalIds.id("diagnostic/first_craft_path"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.INFO,
            "Armory not started",
            "No Armory gear is equipped and no Armory Bench is in inventory.",
            "Craft Armory Alloy Plates, then craft an Armory Bench and Alloy Sword or Thermal Chestplate."
         ));
      }
      if (equippedTier(player) > 0 && !hasInventoryItem(player, "echoarmory:module_upgrade_table") && equippedModules(player) <= 0) {
         blockers.add(new EchoDiagnosticBlocker(
            ArmoryTerminalIds.id("diagnostic/missing_module_table"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.INFO,
            "Module table missing",
            "Armory gear is available, but no Module Upgrade Table is ready for installing modules.",
            "Craft a Module Upgrade Table, then install a Stability Rune, Frost Core, or Gas Mask Module."
         ));
      }
      if (needsRecharge(player) && !hasRechargeMaterial(player)) {
         blockers.add(new EchoDiagnosticBlocker(
            ArmoryTerminalIds.id("diagnostic/missing_recharge_material"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.WARNING,
            "No recharge reserve",
            "Equipped energy gear is below capacity and no Veil Crystal or Resonance Shard is in inventory.",
            "Carry Veil Crystals or Resonance Shards before using energy weapons or recharge stations."
         ));
      }
      ArmoryData.gear(player.getMainHandItem()).ifPresent(gear -> {
         if (!ArmoryData.factionGateSatisfied(player, gear)) {
            blockers.add(new EchoDiagnosticBlocker(
               ArmoryTerminalIds.id("diagnostic/faction_locked_held"),
               CHAPTER_ID,
               EchoDiagnosticBlocker.Severity.WARNING,
               "Held gear faction locked",
               ArmoryData.factionGateLine(gear),
               "Earn the required faction reputation or switch to unlocked Armory gear."
            ));
         }
      });
      ArmoryReadinessService.bestReport(player).ifPresent(report -> {
         if (!report.ready()) {
            blockers.add(new EchoDiagnosticBlocker(
               ArmoryTerminalIds.id("diagnostic/route_kit_" + report.loadout().id().getPath()),
               CHAPTER_ID,
               readinessSeverity(report),
               "Route kit " + report.state().name().toLowerCase(java.util.Locale.ROOT),
               report.loadout().title() + ": " + report.firstBlocker(),
               report.nextAction()
            ));
         }
      });
      if (equippedTier(player) < 2) {
         blockers.add(new EchoDiagnosticBlocker(
            ArmoryTerminalIds.id("diagnostic/low_tier"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.INFO,
            "Armory tier low",
            "The current kit is below mid-tech Armory readiness.",
            "Craft or tune Tier 2 weapons and armor at Armory stations."
         ));
      }
      if (equippedModules(player) <= 0) {
         blockers.add(new EchoDiagnosticBlocker(
            ArmoryTerminalIds.id("diagnostic/no_modules"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.INFO,
            "No modules installed",
            "Armory gear is present but has no installed module telemetry.",
            "Use the Module Upgrade Table to install a core, rune, sigil, or protection module."
         ));
      }
      return List.copyOf(blockers);
   }

   private static EchoHazardTelemetry hazardTelemetry(Player player) {
      if (player == null) {
         return EchoHazardTelemetry.nominal();
      }
      int toxic = ArmoryData.protection(player, ArmoryData.ProtectionType.TOXIC);
      int radiation = ArmoryData.protection(player, ArmoryData.ProtectionType.RADIATION);
      int cold = ArmoryData.protection(player, ArmoryData.ProtectionType.COLD);
      int heat = ArmoryData.protection(player, ArmoryData.ProtectionType.HEAT);
      int fracture = ArmoryData.protection(player, ArmoryData.ProtectionType.FRACTURE);
      int exposureRisk = Math.max(0, 100 - fracture);
      String readiness = ArmoryReadinessService.bestReport(player)
         .map(report -> " Best kit " + report.loadout().title() + " is " + report.state().name() + ".")
         .orElse("");
      return new EchoHazardTelemetry(
         100,
         0,
         0,
         100,
         100,
         0,
         0,
         exposureRisk,
         "Armory protection T/R/C/H/F " + toxic + "/" + radiation + "/" + cold + "/" + heat + "/" + fracture + "." + readiness
      );
   }

   private static EchoDiagnosticBlocker.Severity readinessSeverity(ArmoryReadinessService.Report report) {
      return switch (report.state()) {
         case LOCKED -> EchoDiagnosticBlocker.Severity.BLOCKED;
         case MISSING -> EchoDiagnosticBlocker.Severity.WARNING;
         case STAGED, READY -> EchoDiagnosticBlocker.Severity.INFO;
      };
   }

   private static int equippedTier(Player player) {
      if (player == null) {
         return 0;
      }
      int tier = 0;
      for (ItemStack stack : ArmoryData.armorStacks(player)) {
         tier = Math.max(tier, ArmoryData.gear(stack).map(definition -> definition.tier()).orElse(0));
      }
      tier = Math.max(tier, ArmoryData.gear(player.getMainHandItem()).map(definition -> definition.tier()).orElse(0));
      return tier;
   }

   private static int equippedModules(Player player) {
      if (player == null) {
         return 0;
      }
      int modules = ArmoryData.modules(player.getMainHandItem()).modules().size();
      for (ItemStack stack : ArmoryData.armorStacks(player)) {
         modules += ArmoryData.modules(stack).modules().size();
      }
      return modules;
   }

   private static boolean hasInventoryItem(Player player, String itemId) {
      if (player == null || itemId == null || itemId.isBlank()) {
         return false;
      }
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (ArmoryData.displayId(stack).equals(itemId)) {
            return true;
         }
      }
      return false;
   }

   private static boolean hasRechargeMaterial(Player player) {
      if (player == null) {
         return false;
      }
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (stack.is(ModItems.VEIL_CRYSTAL.get()) || stack.is(ModItems.RESONANCE_SHARD.get())) {
            return true;
         }
      }
      return false;
   }

   private static boolean needsRecharge(Player player) {
      if (player == null) {
         return false;
      }
      for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
         var energy = stack.get(com.knoxhack.echoarmory.registry.ModDataComponents.ENERGY_STATE.get());
         if (energy != null && energy.capacity() > 0 && energy.stored() < energy.capacity()) {
            return true;
         }
      }
      return false;
   }
}

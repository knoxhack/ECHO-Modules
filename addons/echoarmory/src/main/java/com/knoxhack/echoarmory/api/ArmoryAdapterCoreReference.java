package com.knoxhack.echoarmory.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ArmoryAdapterCoreReference {
   private ArmoryAdapterCoreReference() {
   }

   public static ItemStateProbe itemStateProbe() {
      ModuleState modules = new ModuleState(List.of(
         " echoarmory:veil_regulator ",
         "",
         "echoarmory:veil_regulator",
         "echoarmory:fracture_baffle",
         "echoarmory:thermal_liner"
      )).with("echoarmory:overcharge_cell", 3);
      EnergyProbe energy = new EnergyProbe(120, 80, true).spend(25);
      TierProbe tier = new TierProbe(7, "");
      InstabilityProbe instability = new InstabilityProbe(125, -40).decay();
      return new ItemStateProbe(
         modules.modules().size() == 3
            && modules.contains("echoarmory:fracture_baffle")
            && !modules.contains("echoarmory:overcharge_cell")
            && energy.stored() == 55
            && energy.capacity() == 80
            && tier.tier() == 4
            && tier.stage().equals("Tier 4")
            && instability.instability() == 99
            && instability.cooldownTicks() == 0,
         modules.modules(),
         energy.stored(),
         energy.capacity(),
         tier.tier(),
         tier.stage(),
         instability.instability()
      );
   }

   public static RecipePreviewProbe recipePreviewProbe() {
      StationOperationPreview blocked = StationOperationPreview.blocked(" ", " ", " Insert Armory gear. ");
      StationOperationPreview accepted = new StationOperationPreview(
         " weapon_forge ",
         true,
         " upgrade_weapon ",
         "",
         " Increase gear tier ",
         " Higher tier improves route-kit score ",
         -3,
         -10,
         2
      );
      return new RecipePreviewProbe(
         blocked.stationKind().equals("armory_bench")
            && blocked.operation().equals("inspect")
            && blocked.blocker().equals("Insert Armory gear.")
            && accepted.stationKind().equals("weapon_forge")
            && accepted.accepted()
            && accepted.fuelCount() == 0
            && accepted.energy() == 0
            && accepted.moduleCount() == 2,
         blocked.stationKind(),
         blocked.operation(),
         blocked.blocker(),
         accepted.stationKind(),
         accepted.operation(),
         accepted.moduleCount()
      );
   }

   public static ReadinessProbe readinessProbe() {
      int ready = readinessScore(ReadinessState.READY, 3, 75, 0, 0, 0, 2, "READY", "READY");
      int staged = readinessScore(ReadinessState.STAGED, 2, 30, 0, 2, 0, 1, "PARTIAL", "ENERGY_FALLBACK");
      int locked = readinessScore(ReadinessState.LOCKED, 1, 0, 1, 0, 2, 0, "EMPTY", "MISSING");
      String stagedAction = nextAction(ReadinessState.STAGED, List.of(), List.of(), List.of("tier 3 gear staged"));
      String lockedAction = nextAction(ReadinessState.LOCKED, List.of("Faction route gate"), List.of(), List.of());
      return new ReadinessProbe(
         ready == 1120
            && staged == 787
            && locked == 70
            && stagedAction.equals("Apply staged action: tier 3 gear staged.")
            && lockedAction.equals("Clear lock: Faction route gate"),
         ready,
         staged,
         locked,
         stagedAction,
         lockedAction
      );
   }

   public static int readinessScore(
      ReadinessState state,
      int tier,
      int protectionTotal,
      int missingCount,
      int stagedCount,
      int lockedCount,
      int activeSynergyCount,
      String energyState,
      String ammoState
   ) {
      int value = rank(state) * 250 + Math.max(0, tier) * 25 + Math.max(0, protectionTotal) / 5
         + Math.max(0, activeSynergyCount) * 15;
      value -= Math.max(0, missingCount) * 35 + Math.max(0, stagedCount) * 10 + Math.max(0, lockedCount) * 60;
      String energy = normalizeState(energyState);
      if ("EMPTY".equals(energy)) {
         value -= 25;
      } else if ("PARTIAL".equals(energy)) {
         value -= 8;
      }
      String ammo = normalizeState(ammoState);
      if ("MISSING".equals(ammo)) {
         value -= 25;
      } else if ("ENERGY_FALLBACK".equals(ammo)) {
         value -= 6;
      }
      return Math.max(0, value);
   }

   public static String nextAction(ReadinessState state, List<String> locked, List<String> missing, List<String> staged) {
      ReadinessState selected = state == null ? ReadinessState.MISSING : state;
      List<String> safeLocked = locked == null ? List.of() : locked;
      List<String> safeMissing = missing == null ? List.of() : missing;
      List<String> safeStaged = staged == null ? List.of() : staged;
      return switch (selected) {
         case LOCKED -> safeLocked.isEmpty() ? "Clear faction or route locks." : "Clear lock: " + safeLocked.get(0);
         case MISSING -> safeMissing.isEmpty() ? "Acquire missing route-kit supplies." : "Acquire " + safeMissing.get(0) + ".";
         case STAGED -> safeStaged.isEmpty() ? "Equip staged route-kit gear." : "Apply staged action: " + safeStaged.get(0) + ".";
         case READY -> "Deploy, dispatch Logistics, or bind the ready route kit.";
      };
   }

   private static int rank(ReadinessState state) {
      return switch (state == null ? ReadinessState.MISSING : state) {
         case READY -> 4;
         case STAGED -> 3;
         case MISSING -> 2;
         case LOCKED -> 1;
      };
   }

   private static String normalizeState(String value) {
      return value == null || value.isBlank() ? "UNKNOWN" : value.strip().toUpperCase(Locale.ROOT);
   }

   public enum ReadinessState {
      READY,
      STAGED,
      MISSING,
      LOCKED
   }

   public record ModuleState(List<String> modules) {
      public ModuleState {
         Set<String> distinct = new LinkedHashSet<>();
         if (modules != null) {
            for (String module : modules) {
               if (module != null && !module.isBlank()) {
                  distinct.add(module.strip());
               }
               if (distinct.size() >= 8) {
                  break;
               }
            }
         }
         modules = List.copyOf(distinct);
      }

      public boolean contains(String moduleId) {
         return moduleId != null && modules.contains(moduleId);
      }

      public ModuleState with(String moduleId, int maxSlots) {
         if (moduleId == null || moduleId.isBlank() || contains(moduleId) || modules.size() >= Math.max(0, maxSlots)) {
            return this;
         }
         ArrayList<String> next = new ArrayList<>(modules);
         next.add(moduleId.strip());
         return new ModuleState(next);
      }
   }

   public record EnergyProbe(int stored, int capacity, boolean overloaded) {
      public EnergyProbe {
         capacity = Math.max(0, capacity);
         stored = Math.max(0, Math.min(stored, capacity));
      }

      public EnergyProbe spend(int amount) {
         return new EnergyProbe(Math.max(0, stored - Math.max(0, amount)), capacity, overloaded);
      }
   }

   public record TierProbe(int tier, String stage) {
      public TierProbe {
         tier = Math.max(1, Math.min(4, tier));
         stage = stage == null || stage.isBlank() ? "Tier " + tier : stage.strip();
      }
   }

   public record InstabilityProbe(int instability, int cooldownTicks) {
      public InstabilityProbe {
         instability = Math.max(0, Math.min(100, instability));
         cooldownTicks = Math.max(0, cooldownTicks);
      }

      public InstabilityProbe decay() {
         return new InstabilityProbe(Math.max(0, instability - 1), Math.max(0, cooldownTicks - 20));
      }
   }

   public record ItemStateProbe(
      boolean passed,
      List<String> modules,
      int energyStored,
      int energyCapacity,
      int tier,
      String stage,
      int instability
   ) {
   }

   public record RecipePreviewProbe(
      boolean passed,
      String blockedStation,
      String blockedOperation,
      String blockedReason,
      String acceptedStation,
      String acceptedOperation,
      int acceptedModuleCount
   ) {
   }

   public record ReadinessProbe(
      boolean passed,
      int readyScore,
      int stagedScore,
      int lockedScore,
      String stagedAction,
      String lockedAction
   ) {
   }
}

package com.knoxhack.echoarmory.service;

import com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind;
import com.knoxhack.echoarmory.block.entity.ArmoryStationBlockEntity;
import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.ArmoryLoadoutDefinition;
import com.knoxhack.echoarmory.content.GearDefinition;
import com.knoxhack.echoarmory.content.ModuleDefinition;
import com.knoxhack.echoarmory.content.RouteProfileDefinition;
import com.knoxhack.echoarmory.data.EnergyState;
import com.knoxhack.echoarmory.data.EquipmentTier;
import com.knoxhack.echoarmory.item.ArmoryData;
import com.knoxhack.echoarmory.registry.ModDataComponents;
import com.knoxhack.echoarmory.registry.ModItems;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ArmoryReadinessService {
   private ArmoryReadinessService() {
   }

   public static List<Report> reports(Player player) {
      return ArmoryContent.loadouts().stream().map(loadout -> report(player, loadout)).toList();
   }

   public static Optional<Report> report(Player player, String loadoutId) {
      if (loadoutId == null || loadoutId.isBlank()) {
         return Optional.empty();
      }
      String selected = loadoutId.strip();
      return ArmoryContent.loadouts().stream()
         .filter(loadout -> loadout.id().toString().equals(selected) || loadout.id().getPath().equals(selected))
         .findFirst()
         .map(loadout -> report(player, loadout));
   }

   public static Optional<Report> bestReport(Player player) {
      Report best = null;
      for (Report report : reports(player)) {
         if (best == null || better(report, best)) {
            best = report;
         }
      }
      return Optional.ofNullable(best);
   }

   public static Report report(Player player, ArmoryLoadoutDefinition loadout) {
      if (loadout == null) {
         throw new IllegalArgumentException("Loadout is required.");
      }
      Optional<RouteProfileDefinition> routeProfile = ArmoryContent.routeProfileFor(loadout);
      String routeFamily = routeProfile.map(RouteProfileDefinition::routeFamily).orElse(loadout.id().getPath());
      Map<ArmoryData.ProtectionType, Integer> requiredProtections = requiredProtections(loadout, routeProfile);
      int minTier = Math.max(loadout.minTier(), routeProfile.map(RouteProfileDefinition::minTier).orElse(loadout.minTier()));
      if (player == null) {
         return new Report(loadout, State.MISSING, 0, zeroProtections(), List.of("player telemetry"), List.of(), List.of(), List.of(),
            logisticsAvailable(loadout), routeFamily, 0, "offline", "offline", List.of(), "Open a player context before evaluating Armory readiness.");
      }

      ArrayList<String> missing = new ArrayList<>();
      ArrayList<String> staged = new ArrayList<>();
      ArrayList<String> locked = new ArrayList<>();
      Set<String> installedModules = installedModules(player);
      int tier = equippedTier(player);
      Map<ArmoryData.ProtectionType, Integer> protections = equippedProtections(player);
      String energyState = energyState(player, loadout);
      String ammoState = ammoState(player, loadout);

      for (String gearId : requiredGear(loadout)) {
         Optional<GearDefinition> gear = ArmoryContent.gear(gearId);
         String label = gear.map(GearDefinition::title).orElse(gearId);
         if (gear.isPresent() && !ArmoryData.factionGateSatisfied(player, gear.get())) {
            locked.add(ArmoryData.factionGateLine(gear.get()));
            continue;
         }
         if (gear.isPresent() && equippedFor(player, gearId, gear.get())) {
            continue;
         }
         if (hasStagedItem(player, gearId)) {
            staged.add(label + " staged");
         } else {
            missing.add(label);
         }
      }

      if (tier < minTier) {
         if (hasInventoryTier(player, minTier)) {
            staged.add("tier " + minTier + " gear staged");
         } else {
            missing.add("tier " + minTier + " gear");
         }
      }

      for (String moduleId : loadout.modules()) {
         Optional<ModuleDefinition> module = ArmoryContent.module(moduleId);
         String label = module.map(ModuleDefinition::title).orElse(moduleId);
         String fullId = module.map(definition -> definition.id().toString()).orElse(moduleId);
         if (installedModules.contains(fullId)) {
            continue;
         }
         if (hasStagedItem(player, moduleId)) {
            staged.add(label + " ready to install");
         } else {
            missing.add(label);
         }
      }

      for (Map.Entry<ArmoryData.ProtectionType, Integer> requirement : requiredProtections.entrySet()) {
         ArmoryData.ProtectionType type = requirement.getKey();
         int required = requirement.getValue();
         int actual = protections.getOrDefault(type, 0);
         if (actual >= required) {
            continue;
         }
         String label = type.name().toLowerCase(java.util.Locale.ROOT) + " protection " + actual + "/" + required;
         if (potentialProtection(player, type) >= required) {
            staged.add(label + " staged");
         } else {
            missing.add(label);
         }
      }
      if ("MISSING".equals(ammoState)) {
         missing.add("Ammo Crystals");
      }

      for (String gearId : requiredGear(loadout)) {
         ArmoryContent.gear(gearId).ifPresent(gear -> {
            if (gear.energyCapacity() <= 0) {
               return;
            }
            ItemStack stack = equippedStack(player, gearId, gear);
            if (stack.isEmpty()) {
               return;
            }
            EnergyState energy = stack.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY);
            if (energy.capacity() > 0 && energy.stored() <= 0) {
               if (hasRechargeMaterial(player)) {
                  staged.add(gear.title() + " energy empty");
               } else {
                  missing.add("recharge fuel for " + gear.title());
               }
            }
         });
      }

      State state = locked.isEmpty()
         ? (missing.isEmpty() ? (staged.isEmpty() ? State.READY : State.STAGED) : State.MISSING)
         : State.LOCKED;
      List<String> activeSynergies = ArmoryData.activeSynergies(player).stream().map(synergy -> synergy.title()).toList();
      int score = score(state, tier, protections, missing, staged, locked, activeSynergies, energyState, ammoState);
      return new Report(loadout, state, tier, protections, missing, staged, locked, List.copyOf(installedModules),
         logisticsAvailable(loadout), routeFamily, score, energyState, ammoState, activeSynergies, nextAction(state, missing, staged, locked));
   }

   public static String protectionSummary(Map<ArmoryData.ProtectionType, Integer> protections) {
      return "T/R/C/H/F "
         + protections.getOrDefault(ArmoryData.ProtectionType.TOXIC, 0) + "/"
         + protections.getOrDefault(ArmoryData.ProtectionType.RADIATION, 0) + "/"
         + protections.getOrDefault(ArmoryData.ProtectionType.COLD, 0) + "/"
         + protections.getOrDefault(ArmoryData.ProtectionType.HEAT, 0) + "/"
         + protections.getOrDefault(ArmoryData.ProtectionType.FRACTURE, 0);
   }

   private static boolean better(Report candidate, Report current) {
      if (rank(candidate.state()) != rank(current.state())) {
         return rank(candidate.state()) > rank(current.state());
      }
      if (candidate.blockerCount() != current.blockerCount()) {
         return candidate.blockerCount() < current.blockerCount();
      }
      if (candidate.score() != current.score()) {
         return candidate.score() > current.score();
      }
      return candidate.loadout().order() < current.loadout().order();
   }

   private static int rank(State state) {
      return switch (state) {
         case READY -> 4;
         case STAGED -> 3;
         case MISSING -> 2;
         case LOCKED -> 1;
      };
   }

   private static Map<ArmoryData.ProtectionType, Integer> requiredProtections(
      ArmoryLoadoutDefinition loadout,
      Optional<RouteProfileDefinition> routeProfile
   ) {
      EnumMap<ArmoryData.ProtectionType, Integer> merged = new EnumMap<>(ArmoryData.ProtectionType.class);
      loadout.requiredProtections().forEach((type, value) -> merged.put(type, Math.max(0, value)));
      routeProfile.ifPresent(profile -> profile.requiredProtections()
         .forEach((type, value) -> merged.merge(type, Math.max(0, value), Math::max)));
      return Map.copyOf(merged);
   }

   private static int score(
      State state,
      int tier,
      Map<ArmoryData.ProtectionType, Integer> protections,
      List<String> missing,
      List<String> staged,
      List<String> locked,
      List<String> activeSynergies,
      String energyState,
      String ammoState
   ) {
      int protectionTotal = protections.values().stream().mapToInt(Integer::intValue).sum();
      int value = rank(state) * 250 + tier * 25 + protectionTotal / 5 + activeSynergies.size() * 15;
      value -= missing.size() * 35 + staged.size() * 10 + locked.size() * 60;
      if ("EMPTY".equals(energyState)) {
         value -= 25;
      } else if ("PARTIAL".equals(energyState)) {
         value -= 8;
      }
      if ("MISSING".equals(ammoState)) {
         value -= 25;
      } else if ("ENERGY_FALLBACK".equals(ammoState)) {
         value -= 6;
      }
      return Math.max(0, value);
   }

   private static String nextAction(State state, List<String> missing, List<String> staged, List<String> locked) {
      return switch (state) {
         case LOCKED -> locked.isEmpty() ? "Clear faction or route locks." : "Clear lock: " + locked.getFirst();
         case MISSING -> missing.isEmpty() ? "Acquire missing route-kit supplies." : "Acquire " + missing.getFirst() + ".";
         case STAGED -> staged.isEmpty() ? "Equip staged route-kit gear." : "Apply staged action: " + staged.getFirst() + ".";
         case READY -> "Deploy, dispatch Logistics, or bind the ready route kit.";
      };
   }

   private static String energyState(Player player, ArmoryLoadoutDefinition loadout) {
      boolean any = false;
      boolean empty = false;
      boolean partial = false;
      for (String gearId : requiredGear(loadout)) {
         Optional<GearDefinition> gear = ArmoryContent.gear(gearId);
         if (gear.isEmpty() || gear.get().energyCapacity() <= 0) {
            continue;
         }
         ItemStack stack = equippedStack(player, gearId, gear.get());
         if (stack.isEmpty()) {
            continue;
         }
         any = true;
         EnergyState energy = stack.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY);
         if (energy.stored() <= 0) {
            empty = true;
         } else if (energy.stored() < energy.capacity()) {
            partial = true;
         }
      }
      if (!any) {
         return "NONE";
      }
      if (empty) {
         return "EMPTY";
      }
      return partial ? "PARTIAL" : "READY";
   }

   private static String ammoState(Player player, ArmoryLoadoutDefinition loadout) {
      boolean required = false;
      boolean energyFallback = false;
      for (String gearId : requiredGear(loadout)) {
         Optional<GearDefinition> gear = ArmoryContent.gear(gearId);
         if (gear.isEmpty()) {
            continue;
         }
         if (ArmoryContent.firingModeFor(gear.get()).map(mode -> mode.ammoCost() > 0).orElse(false)) {
            required = true;
            ItemStack stack = equippedStack(player, gearId, gear.get());
            EnergyState energy = stack.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY);
            energyFallback |= energy.capacity() > 0 && energy.stored() > 0;
         }
      }
      if (!required) {
         return "NONE";
      }
      if (hasAmmo(player)) {
         return "READY";
      }
      return energyFallback ? "ENERGY_FALLBACK" : "MISSING";
   }

   private static List<String> requiredGear(ArmoryLoadoutDefinition loadout) {
      LinkedHashSet<String> gear = new LinkedHashSet<>();
      if (!loadout.weapon().isBlank()) {
         gear.add(loadout.weapon());
      }
      gear.addAll(loadout.armor());
      return List.copyOf(gear);
   }

   private static boolean equippedFor(Player player, String itemId, GearDefinition gear) {
      return !equippedStack(player, itemId, gear).isEmpty();
   }

   private static ItemStack equippedStack(Player player, String itemId, GearDefinition gear) {
      EquipmentSlot slot = slotFor(gear);
      if (slot != null) {
         ItemStack stack = player.getItemBySlot(slot);
         return matchesItem(stack, itemId) ? stack : ItemStack.EMPTY;
      }
      if ("shield".equals(gear.baseType()) && matchesItem(player.getOffhandItem(), itemId)) {
         return player.getOffhandItem();
      }
      return matchesItem(player.getMainHandItem(), itemId) ? player.getMainHandItem() : ItemStack.EMPTY;
   }

   private static EquipmentSlot slotFor(GearDefinition gear) {
      return switch (gear.baseType()) {
         case "armor_head" -> EquipmentSlot.HEAD;
         case "armor_chest" -> EquipmentSlot.CHEST;
         case "armor_legs" -> EquipmentSlot.LEGS;
         case "armor_feet" -> EquipmentSlot.FEET;
         default -> null;
      };
   }

   private static Set<String> installedModules(Player player) {
      LinkedHashSet<String> modules = new LinkedHashSet<>();
      for (ItemStack stack : equippedStacks(player)) {
         modules.addAll(ArmoryData.modules(stack).modules());
      }
      return modules;
   }

   private static int equippedTier(Player player) {
      int tier = 0;
      for (ItemStack stack : equippedStacks(player)) {
         tier = Math.max(tier, tier(stack));
      }
      return tier;
   }

   private static int tier(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0;
      }
      EquipmentTier component = stack.get(ModDataComponents.EQUIPMENT_TIER.get());
      if (component != null) {
         return component.tier();
      }
      return ArmoryData.gear(stack).map(GearDefinition::tier).orElse(0);
   }

   private static boolean hasInventoryTier(Player player, int minTier) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         if (tier(player.getInventory().getItem(i)) >= minTier) {
            return true;
         }
      }
      for (ItemStack stack : nearbyStagedStacks(player)) {
         if (tier(stack) >= minTier) {
            return true;
         }
      }
      return false;
   }

   private static boolean hasStagedItem(Player player, String itemId) {
      if (hasInventoryItem(player, itemId)) {
         return true;
      }
      for (ItemStack stack : nearbyStagedStacks(player)) {
         if (matchesItem(stack, itemId)) {
            return true;
         }
      }
      return false;
   }

   private static List<ItemStack> equippedStacks(Player player) {
      ArrayList<ItemStack> stacks = new ArrayList<>(ArmoryData.armorStacks(player));
      stacks.add(player.getItemInHand(InteractionHand.MAIN_HAND));
      stacks.add(player.getItemInHand(InteractionHand.OFF_HAND));
      return List.copyOf(stacks);
   }

   private static Map<ArmoryData.ProtectionType, Integer> equippedProtections(Player player) {
      EnumMap<ArmoryData.ProtectionType, Integer> protections = zeroProtectionMap();
      for (ArmoryData.ProtectionType type : ArmoryData.ProtectionType.values()) {
         protections.put(type, ArmoryData.protection(player, type));
      }
      return Map.copyOf(protections);
   }

   private static int potentialProtection(Player player, ArmoryData.ProtectionType type) {
      int total = ArmoryData.protection(player, type);
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         Optional<ModuleDefinition> module = ArmoryData.module(stack);
         if (module.isPresent()) {
            total += protection(module.get(), type) * Math.max(1, stack.getCount());
         }
      }
      for (ItemStack stack : nearbyStagedStacks(player)) {
         Optional<ModuleDefinition> module = ArmoryData.module(stack);
         if (module.isPresent()) {
            total += protection(module.get(), type) * Math.max(1, stack.getCount());
         }
      }
      return Math.min(100, total);
   }

   private static int protection(ModuleDefinition module, ArmoryData.ProtectionType type) {
      return switch (type) {
         case TOXIC -> module.toxicProtection();
         case RADIATION -> module.radiationProtection();
         case COLD -> module.coldProtection();
         case HEAT -> module.heatProtection();
         case FRACTURE -> module.fractureProtection();
      };
   }

   private static boolean hasInventoryItem(Player player, String itemId) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         if (matchesItem(player.getInventory().getItem(i), itemId)) {
            return true;
         }
      }
      return false;
   }

   private static boolean matchesItem(ItemStack stack, String itemId) {
      if (stack.isEmpty() || itemId == null || itemId.isBlank()) {
         return false;
      }
      Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id != null && (id.toString().equals(itemId) || id.getPath().equals(itemId));
   }

   private static boolean hasRechargeMaterial(Player player) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (stack.is(ModItems.VEIL_CRYSTAL.get()) || stack.is(ModItems.RESONANCE_SHARD.get())) {
            return true;
         }
      }
      return false;
   }

   private static boolean hasAmmo(Player player) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (stack.is(ModItems.AMMO_CRYSTALS.get())) {
            return true;
         }
      }
      return false;
   }

   private static List<ItemStack> nearbyStagedStacks(Player player) {
      if (player == null || player.level() == null) {
         return List.of();
      }
      ArrayList<ItemStack> stacks = new ArrayList<>();
      BlockPos center = player.blockPosition();
      int radius = 8;
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))) {
         if (player.level().getBlockEntity(pos) instanceof ArmoryStationBlockEntity station
            && (station.kind() == StationKind.WEAPON_RACK || station.kind() == StationKind.ARMOR_STAND)) {
            for (int slot = 0; slot < station.getContainerSize(); slot++) {
               ItemStack stack = station.getItem(slot);
               if (!stack.isEmpty()) {
                  stacks.add(stack);
               }
            }
         }
      }
      return List.copyOf(stacks);
   }

   private static Map<ArmoryData.ProtectionType, Integer> zeroProtections() {
      return Map.copyOf(zeroProtectionMap());
   }

   private static EnumMap<ArmoryData.ProtectionType, Integer> zeroProtectionMap() {
      EnumMap<ArmoryData.ProtectionType, Integer> protections = new EnumMap<>(ArmoryData.ProtectionType.class);
      for (ArmoryData.ProtectionType type : ArmoryData.ProtectionType.values()) {
         protections.put(type, 0);
      }
      return protections;
   }

   private static boolean logisticsAvailable(ArmoryLoadoutDefinition loadout) {
      return loadout != null && !loadout.logisticsPreset().isBlank() && EchoRuntimeModules.isLoaded("echologisticsnetwork");
   }

   public enum State {
      READY,
      STAGED,
      MISSING,
      LOCKED
   }

   public record Report(
      ArmoryLoadoutDefinition loadout,
      State state,
      int tier,
      Map<ArmoryData.ProtectionType, Integer> protections,
      List<String> missing,
      List<String> staged,
      List<String> locked,
      List<String> installedModules,
      boolean logisticsAvailable,
      String routeFamily,
      int score,
      String energyState,
      String ammoState,
      List<String> activeSynergies,
      String nextAction
   ) {
      public Report(
         ArmoryLoadoutDefinition loadout,
         State state,
         int tier,
         Map<ArmoryData.ProtectionType, Integer> protections,
         List<String> missing,
         List<String> staged,
         List<String> locked,
         List<String> installedModules,
         boolean logisticsAvailable
      ) {
         this(loadout, state, tier, protections, missing, staged, locked, installedModules, logisticsAvailable,
            loadout == null ? "unknown" : loadout.id().getPath(), rank(state), "unknown", "unknown", List.of(), "");
      }

      public Report {
         protections = Map.copyOf(protections == null ? Map.of() : protections);
         missing = List.copyOf(missing == null ? List.of() : missing);
         staged = List.copyOf(staged == null ? List.of() : staged);
         locked = List.copyOf(locked == null ? List.of() : locked);
         installedModules = List.copyOf(installedModules == null ? List.of() : installedModules);
         routeFamily = routeFamily == null || routeFamily.isBlank() ? "unknown" : routeFamily.strip();
         score = Math.max(0, score);
         energyState = energyState == null || energyState.isBlank() ? "unknown" : energyState.strip();
         ammoState = ammoState == null || ammoState.isBlank() ? "unknown" : ammoState.strip();
         activeSynergies = List.copyOf(activeSynergies == null ? List.of() : activeSynergies);
         nextAction = nextAction == null || nextAction.isBlank() ? firstNonBlank(locked, missing, staged) : nextAction.strip();
      }

      public boolean ready() {
         return state == State.READY;
      }

      public int blockerCount() {
         return missing.size() + staged.size() + locked.size();
      }

      public String firstBlocker() {
         if (!locked.isEmpty()) {
            return locked.getFirst();
         }
         if (!missing.isEmpty()) {
            return "Missing " + missing.getFirst();
         }
         if (!staged.isEmpty()) {
            return staged.getFirst();
         }
         return "Ready for deployment";
      }

      public String summaryLine() {
         return state + " // score " + score + " | tier " + tier + " | route " + routeFamily
            + " | " + protectionSummary(protections) + " | E " + energyState + " | A " + ammoState + " | " + firstBlocker();
      }

      private static String firstNonBlank(List<String> locked, List<String> missing, List<String> staged) {
         if (locked != null && !locked.isEmpty()) {
            return "Clear lock: " + locked.getFirst();
         }
         if (missing != null && !missing.isEmpty()) {
            return "Acquire " + missing.getFirst() + ".";
         }
         if (staged != null && !staged.isEmpty()) {
            return "Apply staged action: " + staged.getFirst() + ".";
         }
         return "Deploy with the selected Armory kit.";
      }
   }
}

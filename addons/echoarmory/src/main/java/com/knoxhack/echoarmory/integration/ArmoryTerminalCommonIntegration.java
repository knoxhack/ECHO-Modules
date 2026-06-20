package com.knoxhack.echoarmory.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.ArmoryLoadoutDefinition;
import com.knoxhack.echoarmory.content.BossRecommendationDefinition;
import com.knoxhack.echoarmory.content.GearDefinition;
import com.knoxhack.echoarmory.data.EnergyState;
import com.knoxhack.echoarmory.item.ArmoryData;
import com.knoxhack.echoarmory.registry.ModDataComponents;
import com.knoxhack.echoarmory.registry.ModItems;
import com.knoxhack.echoarmory.service.ArmoryReadinessService;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArmoryTerminalCommonIntegration {
   private static boolean registered;

   private ArmoryTerminalCommonIntegration() {
   }

   public static void register() {
      if (registered) {
         return;
      }
      registered = true;
      TerminalActionRegistry.register(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.SCAN_ACTION, ArmoryTerminalCommonIntegration::scan);
      TerminalActionRegistry.register(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.EQUIP_ACTION, ArmoryTerminalCommonIntegration::equip);
      TerminalActionRegistry.register(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.INSTALL_ACTION, ArmoryTerminalCommonIntegration::install);
      TerminalActionRegistry.register(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.STANCE_ACTION, ArmoryTerminalCommonIntegration::stance);
      TerminalActionRegistry.register(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.RECHARGE_ACTION, ArmoryTerminalCommonIntegration::recharge);
      TerminalActionRegistry.register(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.PREVIEW_ACTION, ArmoryTerminalCommonIntegration::preview);
      TerminalActionRegistry.register(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.LOGISTICS_ACTION, ArmoryTerminalCommonIntegration::logistics);
      registerArchive();
      EchoArmory.LOGGER.info("ECHO Armory terminal actions registered.");
   }

   public static void resetForTests() {
      registered = false;
   }

   private static void scan(ServerPlayer player, String payload) {
      if (player == null) {
         return;
      }
      ArmoryReadinessService.Report report = selectedReport(player, payload).orElse(null);
      if (report == null) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Scan complete. No Armory loadouts are published."));
         return;
      }
      recordReadyReport(player, report);
      player.sendSystemMessage(Component.literal("ECHO ARMORY // " + report.loadout().title() + " readiness: " + report.summaryLine()
         + " | Synergies " + report.activeSynergies().size() + " | Next " + report.nextAction()
         + " | Logistics " + (report.logisticsAvailable() ? "available" : "unavailable") + "."));
      EchoCoreServices.discoverVisibleRouteRecords(player);
   }

   private static void equip(ServerPlayer player, String payload) {
      if (player == null) {
         return;
      }
      Optional<ArmoryReadinessService.Report> selected = selectedReport(player, payload);
      if (selected.isEmpty()) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // No Armory loadout definition is available."));
         return;
      }
      ArmoryLoadoutDefinition loadout = selected.get().loadout();
      int equipped = 0;
      int missing = 0;
      if (!loadout.weapon().isBlank()) {
         EquipResult result = equipItem(player, loadout.weapon(), EquipmentSlot.MAINHAND);
         equipped += result.equipped() ? 1 : 0;
         missing += result.equipped() ? 0 : 1;
      }
      for (String armorId : loadout.armor()) {
         EquipmentSlot slot = armorSlot(armorId);
         if (slot == null) {
            continue;
         }
         EquipResult result = equipItem(player, armorId, slot);
         equipped += result.equipped() ? 1 : 0;
         missing += result.equipped() ? 0 : 1;
      }
      markEquippedLoadout(player, loadout);
      ArmoryReadinessService.Report updated = ArmoryReadinessService.report(player, loadout);
      recordReadyReport(player, updated);
      player.sendSystemMessage(Component.literal("ECHO ARMORY // " + loadout.title() + " equip pass: " + equipped + " equipped"
         + (missing > 0 ? ", " + missing + " missing or locked. " : ". ")
         + updated.state() + ": " + updated.firstBlocker()));
      EchoCoreServices.discoverVisibleRouteRecords(player);
   }

   private static void install(ServerPlayer player, String payload) {
      if (player == null) {
         return;
      }
      String requestedModule = payload == null ? "" : payload.strip();
      for (ItemStack target : armoryTargets(player)) {
         if (target.isEmpty() || ArmoryData.gear(target).isEmpty()) {
            continue;
         }
         if (!ArmoryData.factionGateSatisfied(player, target)) {
            ArmoryData.gear(target).ifPresent(gear -> player.sendSystemMessage(Component.literal("ECHO ARMORY // " + ArmoryData.factionGateLine(gear))));
            continue;
         }
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack module = player.getInventory().getItem(i);
            if (module.isEmpty() || ArmoryData.module(module).isEmpty()) {
               continue;
            }
            String moduleId = ArmoryData.module(module).map(definition -> definition.id().toString()).orElse("");
            if (!requestedModule.isBlank() && !requestedModule.equals(moduleId)) {
               continue;
            }
            if (ArmoryData.installModule(player, target, module)) {
               player.sendSystemMessage(Component.literal("ECHO ARMORY // Installed " + moduleId + " into " + target.getHoverName().getString() + "."));
               ArmoryReadinessService.bestReport(player).ifPresent(report -> recordReadyReport(player, report));
               return;
            }
         }
      }
      player.sendSystemMessage(Component.literal(requestedModule.isBlank()
         ? "ECHO ARMORY // No compatible inventory module found for equipped Armory gear."
         : "ECHO ARMORY // Requested module " + requestedModule + " was not found or not compatible."));
   }

   private static void stance(ServerPlayer player, String payload) {
      if (player == null) {
         return;
      }
      ItemStack stack = player.getMainHandItem();
      ArmoryData.initialize(stack);
      com.knoxhack.echoarmory.data.ArmoryStance next = stack.getOrDefault(ModDataComponents.STANCE.get(), com.knoxhack.echoarmory.data.ArmoryStance.BALANCED).next();
      stack.set(ModDataComponents.STANCE.get(), next);
      player.sendSystemMessage(Component.literal("ECHO ARMORY // Stance set to " + next.label() + "."));
   }

   private static void recharge(ServerPlayer player, String payload) {
      if (player == null) {
         return;
      }
      ItemStack target = player.getMainHandItem();
      if (target.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY).capacity() <= 0) {
         ArmoryData.initialize(target);
      }
      if (rechargeFromInventory(player, target)) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Main-hand energy core recharged using inventory reserve."));
         ArmoryReadinessService.bestReport(player).ifPresent(report -> recordReadyReport(player, report));
      } else {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Recharge requires energy gear that is not full and one Veil Crystal or Resonance Shard."));
      }
   }

   private static void preview(ServerPlayer player, String payload) {
      if (player == null) {
         return;
      }
      selectedBoss(payload).ifPresentOrElse(recommendation -> {
         String kit = ArmoryReadinessService.bestReport(player)
            .map(report -> " Best kit " + report.loadout().title() + " is " + report.state() + ": " + report.firstBlocker())
            .orElse("");
         player.sendSystemMessage(Component.literal(
            "ECHO ARMORY // " + recommendation.bossName() + " recommends tier " + recommendation.minTier()
               + ", fracture " + recommendation.fractureProtection() + ". Current fracture "
               + ArmoryData.protection(player, ArmoryData.ProtectionType.FRACTURE) + "." + kit
               + ArmoryReadinessService.bestReport(player).map(report ->
                  " Route " + report.routeFamily() + ", energy " + report.energyState() + ", ammo " + report.ammoState()
                     + ", next " + report.nextAction()).orElse("")));
      },
         () -> player.sendSystemMessage(Component.literal("ECHO ARMORY // No boss recommendation selected.")));
   }

   private static void logistics(ServerPlayer player, String payload) {
      if (player == null) {
         return;
      }
      Optional<ArmoryReadinessService.Report> selected = selectedReport(player, payload);
      if (selected.isEmpty()) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // No Armory loadout selected for Logistics dispatch."));
         return;
      }
      ArmoryReadinessService.Report report = selected.get();
      ArmoryLoadoutDefinition loadout = report.loadout();
      if (loadout.logisticsPreset().isBlank()) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // " + loadout.title() + " has no Logistics preset."));
         return;
      }
      if (report.state() == ArmoryReadinessService.State.LOCKED) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Logistics blocked by readiness lock: " + report.firstBlocker()));
         return;
      }
      requestLogisticsBridge(player, loadout.logisticsPreset());
   }

   private static Optional<ArmoryLoadoutDefinition> selectedLoadout(ServerPlayer player, String payload) {
      return selectedReport(player, payload).map(ArmoryReadinessService.Report::loadout);
   }

   private static Optional<ArmoryReadinessService.Report> selectedReport(ServerPlayer player, String payload) {
      String selected = payload == null ? "" : payload.strip();
      if (selected.isBlank()) {
         selected = player.getMainHandItem().getOrDefault(ModDataComponents.ARMORY_LOADOUT.get(), com.knoxhack.echoarmory.data.ArmoryLoadout.EMPTY).loadoutId();
      }
      if (!selected.isBlank()) {
         Optional<ArmoryReadinessService.Report> match = ArmoryReadinessService.report(player, selected);
         if (match.isPresent()) {
            return match;
         }
      }
      return ArmoryReadinessService.bestReport(player);
   }

   private static Optional<BossRecommendationDefinition> selectedBoss(String payload) {
      String selected = payload == null ? "" : payload.strip();
      if (!selected.isBlank()) {
         Optional<BossRecommendationDefinition> match = ArmoryContent.bossRecommendations().stream()
            .filter(recommendation -> recommendation.id().toString().equals(selected) || recommendation.id().getPath().equals(selected))
            .findFirst();
         if (match.isPresent()) {
            return match;
         }
      }
      return ArmoryContent.bossRecommendations().stream().findFirst();
   }

   private static EquipResult equipItem(ServerPlayer player, String itemId, EquipmentSlot slot) {
      Optional<GearDefinition> gear = ArmoryContent.gear(itemId);
      if (gear.isPresent() && !ArmoryData.factionGateSatisfied(player, gear.get())) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // " + ArmoryData.factionGateLine(gear.get())));
         return new EquipResult(false);
      }
      ItemStack current = slot == EquipmentSlot.MAINHAND ? player.getMainHandItem() : player.getItemBySlot(slot);
      if (matchesItem(current, itemId)) {
         ArmoryData.initialize(current);
         return new EquipResult(true);
      }
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack candidate = player.getInventory().getItem(i);
         if (matchesItem(candidate, itemId)) {
            ArmoryData.initialize(candidate);
            ItemStack replacement = candidate.copy();
            if (slot == EquipmentSlot.MAINHAND) {
               player.getInventory().setItem(i, player.getMainHandItem());
               player.setItemInHand(InteractionHand.MAIN_HAND, replacement);
            } else {
               player.getInventory().setItem(i, player.getItemBySlot(slot));
               player.setItemSlot(slot, replacement);
            }
            return new EquipResult(true);
         }
      }
      return new EquipResult(false);
   }

   private static boolean matchesItem(ItemStack stack, String itemId) {
      if (stack.isEmpty() || itemId == null || itemId.isBlank()) {
         return false;
      }
      Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id != null && (id.toString().equals(itemId) || id.getPath().equals(itemId));
   }

   private static EquipmentSlot armorSlot(String itemId) {
      return ArmoryContent.gear(itemId).map(gear -> switch (gear.baseType()) {
         case "armor_head" -> EquipmentSlot.HEAD;
         case "armor_chest" -> EquipmentSlot.CHEST;
         case "armor_legs" -> EquipmentSlot.LEGS;
         case "armor_feet" -> EquipmentSlot.FEET;
         default -> null;
      }).orElse(null);
   }

   private static void markEquippedLoadout(ServerPlayer player, ArmoryLoadoutDefinition loadout) {
      com.knoxhack.echoarmory.data.ArmoryLoadout marker = new com.knoxhack.echoarmory.data.ArmoryLoadout(loadout.id().toString(), loadout.title());
      for (ItemStack stack : armoryTargets(player)) {
         if (!stack.isEmpty() && ArmoryData.gear(stack).isPresent()) {
            stack.set(ModDataComponents.ARMORY_LOADOUT.get(), marker);
         }
      }
   }

   private static List<ItemStack> armoryTargets(ServerPlayer player) {
      return List.of(
         player.getMainHandItem(),
         player.getItemBySlot(EquipmentSlot.HEAD),
         player.getItemBySlot(EquipmentSlot.CHEST),
         player.getItemBySlot(EquipmentSlot.LEGS),
         player.getItemBySlot(EquipmentSlot.FEET)
      );
   }

   private static int equippedTier(ServerPlayer player) {
      int tier = ArmoryData.gear(player.getMainHandItem()).map(GearDefinition::tier).orElse(0);
      for (ItemStack stack : ArmoryData.armorStacks(player)) {
         tier = Math.max(tier, ArmoryData.gear(stack).map(GearDefinition::tier).orElse(0));
      }
      return tier;
   }

   private static boolean rechargeFromInventory(ServerPlayer player, ItemStack target) {
      if (player.getAbilities().instabuild) {
         EnergyState energy = target.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY);
         if (energy.capacity() <= 0 || energy.stored() >= energy.capacity()) {
            return false;
         }
         ArmoryData.recharge(target);
         return true;
      }
      for (Item fuel : List.of(
         fuelItem(() -> ModItems.VEIL_CRYSTAL.get(), Items.AMETHYST_SHARD),
         fuelItem(() -> ModItems.RESONANCE_SHARD.get(), Items.PRISMARINE_SHARD)
      )) {
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate.is(fuel) && ArmoryData.rechargeWithFuel(target, candidate)) {
               return true;
            }
         }
      }
      return false;
   }

   private static Item fuelItem(Supplier<Item> supplier, Item fallback) {
      try {
         Item item = supplier.get();
         return item == null ? fallback : item;
      } catch (RuntimeException | LinkageError exception) {
         return fallback;
      }
   }

   private static void requestLogisticsBridge(ServerPlayer player, String logisticsPreset) {
      try {
         Class<?> bridge = Class.forName("com.knoxhack.echologisticsnetwork.integration.LogisticsDispatchBridge");
         Object result = bridge.getMethod("requestNearestLoadout", ServerPlayer.class, String.class).invoke(null, player, logisticsPreset);
         boolean dispatched = (boolean)result.getClass().getMethod("dispatched").invoke(result);
         String message = String.valueOf(result.getClass().getMethod("message").invoke(result));
         if (dispatched) {
            ArmoryMissionHooks.recordDispatchRouteKit(player, logisticsPreset);
         }
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Logistics " + (dispatched ? "dispatch queued: " : "dispatch blocked: ") + message));
      } catch (ClassNotFoundException exception) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Logistics Network is not loaded; mission kit dispatch is unavailable."));
      } catch (ReflectiveOperationException | RuntimeException exception) {
         EchoArmory.LOGGER.warn("Armory Logistics dispatch failed for preset {}.", logisticsPreset, exception);
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Logistics dispatch failed. Check network endpoints and loaded integrations."));
      }
   }

   private static void recordReadyReport(ServerPlayer player, ArmoryReadinessService.Report report) {
      if (report != null && report.ready()) {
         ArmoryMissionHooks.recordPrepareRouteKit(player, report.loadout().id().toString());
      }
   }

   private record EquipResult(boolean equipped) {
   }

   private static void registerArchive() {
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(ArmoryTerminalIds.id("archive/modular_weapons"),
         "Armory", "Modular Weapons", "ONLINE",
         List.of("Weapons use ItemStack components for installed cores, runes, utilities, stance, energy, and instability.",
            "Shift-use Armory weapons to cycle Heavy, Quick, and Balanced stance."), false));
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(ArmoryTerminalIds.id("archive/modular_armor"),
         "Armory", "Modular Armor", "ACTIVE",
         List.of("Armor modules publish protection telemetry for toxic air, radiation, cold, heat, and fracture pressure.",
            "Drone Dock modules provide periodic repair and shielding support."), false));
   }
}

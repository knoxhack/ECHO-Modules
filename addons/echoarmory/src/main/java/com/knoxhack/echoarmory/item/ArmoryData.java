package com.knoxhack.echoarmory.item;

import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.FactionUnlockDefinition;
import com.knoxhack.echoarmory.content.GearDefinition;
import com.knoxhack.echoarmory.content.ModuleDefinition;
import com.knoxhack.echoarmory.content.SynergyDefinition;
import com.knoxhack.echoarmory.data.ArmoryStance;
import com.knoxhack.echoarmory.data.CosmeticTrim;
import com.knoxhack.echoarmory.data.EnergyState;
import com.knoxhack.echoarmory.data.EquipmentTier;
import com.knoxhack.echoarmory.data.InstalledModules;
import com.knoxhack.echoarmory.data.InstabilityState;
import com.knoxhack.echoarmory.registry.ModDataComponents;
import com.knoxhack.echoarmory.registry.ModItems;
import com.knoxhack.echocore.api.EchoCoreServices;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ArmoryData {
   private ArmoryData() {
   }

   public static void initialize(ItemStack stack) {
      Optional<GearDefinition> gear = gear(stack);
      if (gear.isEmpty()) {
         return;
      }
      GearDefinition definition = gear.get();
      if (stack.get(ModDataComponents.INSTALLED_MODULES.get()) == null) {
         stack.set(ModDataComponents.INSTALLED_MODULES.get(), InstalledModules.EMPTY);
      }
      if (stack.get(ModDataComponents.EQUIPMENT_TIER.get()) == null) {
         stack.set(ModDataComponents.EQUIPMENT_TIER.get(), new EquipmentTier(definition.tier(), definition.craftingStage()));
      }
      if (definition.energyCapacity() > 0 && stack.get(ModDataComponents.ENERGY_STATE.get()) == null) {
         stack.set(ModDataComponents.ENERGY_STATE.get(), new EnergyState(definition.energyCapacity(), definition.energyCapacity(), false));
      }
      if (stack.get(ModDataComponents.STANCE.get()) == null) {
         stack.set(ModDataComponents.STANCE.get(), ArmoryStance.BALANCED);
      }
      if (stack.get(ModDataComponents.COSMETIC_TRIM.get()) == null) {
         stack.set(ModDataComponents.COSMETIC_TRIM.get(), CosmeticTrim.EMPTY);
      }
      if (stack.get(ModDataComponents.INSTABILITY_STATE.get()) == null) {
         stack.set(ModDataComponents.INSTABILITY_STATE.get(), InstabilityState.STABLE);
      }
   }

   public static Optional<GearDefinition> gear(ItemStack stack) {
      if (stack.isEmpty() || !(stack.getItem() instanceof ArmoryGearItem gearItem)) {
         return Optional.empty();
      }
      return ArmoryContent.gear(gearItem.gearId());
   }

   public static Optional<ModuleDefinition> module(ItemStack stack) {
      if (stack.isEmpty() || !(stack.getItem() instanceof ArmoryGearItem gearItem)) {
         return Optional.empty();
      }
      return ArmoryContent.module(gearItem.gearId());
   }

   public static InstalledModules modules(ItemStack stack) {
      InstalledModules modules = stack.get(ModDataComponents.INSTALLED_MODULES.get());
      return modules == null ? InstalledModules.EMPTY : modules;
   }

   public static List<ModuleDefinition> moduleDefinitions(ItemStack stack) {
      List<ModuleDefinition> definitions = new ArrayList<>();
      for (String moduleId : modules(stack).modules()) {
         ArmoryContent.module(moduleId).ifPresent(definitions::add);
      }
      return definitions;
   }

   public static int protection(Player player, ProtectionType type) {
      if (player == null) {
         return 0;
      }
      int total = 0;
      for (ItemStack stack : armorStacks(player)) {
         if (factionGateSatisfied(player, stack)) {
            total += protection(stack, type);
         }
      }
      ItemStack mainHand = player.getMainHandItem();
      if (!mainHand.isEmpty() && factionGateSatisfied(player, mainHand)) {
         total += protection(mainHand, type) / 2;
      }
      return Math.min(100, total);
   }

   public static List<ItemStack> armorStacks(Player player) {
      if (player == null) {
         return List.of();
      }
      return List.of(
         player.getItemBySlot(EquipmentSlot.HEAD),
         player.getItemBySlot(EquipmentSlot.CHEST),
         player.getItemBySlot(EquipmentSlot.LEGS),
         player.getItemBySlot(EquipmentSlot.FEET)
      );
   }

   public static int protection(ItemStack stack, ProtectionType type) {
      int total = 0;
      for (ModuleDefinition module : moduleDefinitions(stack)) {
         total += switch (type) {
            case TOXIC -> module.toxicProtection();
            case RADIATION -> module.radiationProtection();
            case COLD -> module.coldProtection();
            case HEAT -> module.heatProtection();
            case FRACTURE -> module.fractureProtection();
         };
      }
      return Math.min(100, total);
   }

   public static float damageBonus(ItemStack stack) {
      float total = 0.0F;
      for (ModuleDefinition module : moduleDefinitions(stack)) {
         total += module.damageBonus();
      }
      EquipmentTier tier = stack.get(ModDataComponents.EQUIPMENT_TIER.get());
      if (tier != null) {
         total += Math.max(0, tier.tier() - 1) * 0.75F;
      }
      return total;
   }

   public static int defenseBonus(ItemStack stack) {
      int total = 0;
      for (ModuleDefinition module : moduleDefinitions(stack)) {
         total += module.defenseBonus();
      }
      return total;
   }

   public static boolean installModule(ItemStack gearStack, ItemStack moduleStack) {
      return installModule(null, gearStack, moduleStack);
   }

   public static boolean installModule(Player player, ItemStack gearStack, ItemStack moduleStack) {
      Optional<GearDefinition> gear = gear(gearStack);
      Optional<ModuleDefinition> module = module(moduleStack);
      if (gear.isEmpty() || module.isEmpty()) {
         return false;
      }
      GearDefinition gearDefinition = gear.get();
      ModuleDefinition moduleDefinition = module.get();
      InstalledModules modules = modules(gearStack);
      if (!factionGateSatisfied(player, gearDefinition)) {
         return false;
      }
      String moduleId = moduleDefinition.id().toString();
      if (modules.contains(moduleId)
         || !gearDefinition.allows(moduleDefinition)
         || !moduleDefinition.compatibleWith(gearDefinition)
         || modules.modules().size() >= gearDefinition.moduleSlots()) {
         return false;
      }
      InstalledModules updated = modules.with(moduleId, gearDefinition.moduleSlots());
      if (updated.equals(modules)) {
         return false;
      }
      gearStack.set(ModDataComponents.INSTALLED_MODULES.get(), updated);
      InstabilityState instability = gearStack.getOrDefault(ModDataComponents.INSTABILITY_STATE.get(), InstabilityState.STABLE);
      gearStack.set(ModDataComponents.INSTABILITY_STATE.get(), new InstabilityState(instability.instability() + moduleDefinition.instability(), instability.cooldownTicks()));
      initialize(gearStack);
      moduleStack.shrink(1);
      return true;
   }

   public static boolean factionGateSatisfied(Player player, ItemStack stack) {
      return gear(stack).map(definition -> factionGateSatisfied(player, definition)).orElse(true);
   }

   public static boolean factionGateSatisfied(Player player, GearDefinition gear) {
      if (gear == null || gear.factionGate().isBlank()) {
         return true;
      }
      if (player != null && player.getAbilities().instabuild) {
         return true;
      }
      Identifier factionId = safeIdentifier(gear.factionGate());
      if (factionId == null) {
         return false;
      }
      int required = requiredReputation(gear);
      return EchoCoreServices.factionProfile(player, factionId)
         .map(profile -> profile.reputation() >= required)
         .orElse(false);
   }

   public static int requiredReputation(GearDefinition gear) {
      if (gear == null || gear.factionGate().isBlank()) {
         return 0;
      }
      String path = gear.id().getPath();
      String fullId = gear.id().toString();
      return ArmoryContent.factionUnlocks().stream()
         .filter(unlock -> unlock.unlockId().equals(path) || unlock.unlockId().equals(fullId))
         .mapToInt(FactionUnlockDefinition::minReputation)
         .findFirst()
         .orElse(0);
   }

   public static String factionGateLine(GearDefinition gear) {
      if (gear == null || gear.factionGate().isBlank()) {
         return "";
      }
      return gear.title() + " requires " + gear.factionGate() + " reputation " + requiredReputation(gear) + ".";
   }

   public static void recharge(ItemStack stack) {
      initialize(stack);
      EnergyState energy = stack.get(ModDataComponents.ENERGY_STATE.get());
      if (energy != null) {
         stack.set(ModDataComponents.ENERGY_STATE.get(), energy.charged());
      }
   }

   public static boolean rechargeWithFuel(ItemStack stack, ItemStack fuel) {
      initialize(stack);
      EnergyState energy = stack.get(ModDataComponents.ENERGY_STATE.get());
      if (energy == null || energy.capacity() <= 0 || energy.stored() >= energy.capacity()) {
         return false;
      }
      if (!isRechargeFuel(fuel)) {
         return false;
      }
      stack.set(ModDataComponents.ENERGY_STATE.get(), energy.charged());
      fuel.shrink(1);
      return true;
   }

   public static boolean isRechargeFuel(ItemStack stack) {
      return !stack.isEmpty() && (stack.is(ModItems.VEIL_CRYSTAL.get()) || stack.is(ModItems.RESONANCE_SHARD.get()));
   }

   public static boolean repairWithPlate(ItemStack stack, ItemStack material) {
      if (stack.isEmpty() || !stack.isDamaged() || !material.is(ModItems.ARMORY_ALLOY_PLATE.get())) {
         return false;
      }
      int repair = Math.max(1, stack.getMaxDamage() / 3);
      stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
      material.shrink(1);
      initialize(stack);
      return true;
   }

   public static boolean upgradeTier(ItemStack stack, ItemStack material, boolean weaponStation) {
      Optional<GearDefinition> gear = gear(stack);
      if (gear.isEmpty() || material.isEmpty()) {
         return false;
      }
      initialize(stack);
      EquipmentTier tier = stack.getOrDefault(ModDataComponents.EQUIPMENT_TIER.get(), new EquipmentTier(gear.get().tier(), gear.get().craftingStage()));
      if (tier.tier() >= 4 || !upgradeMaterialMatches(material, tier.tier(), weaponStation)) {
         return false;
      }
      int nextTier = Math.min(4, tier.tier() + 1);
      stack.set(ModDataComponents.EQUIPMENT_TIER.get(), new EquipmentTier(nextTier, upgradeStage(nextTier)));
      material.shrink(1);
      return true;
   }

   public static List<SynergyDefinition> activeSynergies(Player player) {
      if (player == null) {
         return List.of();
      }
      Set<String> tags = activeTags(player);
      return ArmoryContent.synergies().stream()
         .filter(synergy -> tags.containsAll(synergy.requiredTags()))
         .toList();
   }

   public static boolean hasActiveSynergyEffect(Player player, String effect) {
      return effect != null && activeSynergies(player).stream().anyMatch(synergy -> effect.equals(synergy.effect()));
   }

   public static boolean spendEnergy(ItemStack stack, int amount) {
      EnergyState energy = stack.get(ModDataComponents.ENERGY_STATE.get());
      if (energy == null || amount <= 0) {
         return true;
      }
      if (energy.stored() < amount) {
         return false;
      }
      stack.set(ModDataComponents.ENERGY_STATE.get(), energy.spend(amount));
      return true;
   }

   public static String displayId(ItemStack stack) {
      Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id == null ? "unknown" : id.toString();
   }

   private static Identifier safeIdentifier(String value) {
      if (value == null || value.isBlank()) {
         return null;
      }
      try {
         return Identifier.parse(value);
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static boolean upgradeMaterialMatches(ItemStack material, int currentTier, boolean weaponStation) {
      if (currentTier <= 1) {
         return weaponStation ? material.is(ModItems.RESONANCE_SHARD.get()) : material.is(ModItems.ARMORY_ALLOY_PLATE.get());
      }
      if (currentTier == 2) {
         return material.is(ModItems.VEIL_CRYSTAL.get());
      }
      return material.is(ModItems.BLACKBOX_FRAGMENT.get());
   }

   private static String upgradeStage(int tier) {
      return switch (tier) {
         case 2 -> "Mid-tech upgraded";
         case 3 -> "Industrial upgraded";
         case 4 -> "Endgame upgraded";
         default -> "Early survival";
      };
   }

   private static Set<String> activeTags(Player player) {
      HashSet<String> tags = new HashSet<>();
      for (ItemStack stack : equippedStacks(player)) {
         gear(stack).ifPresent(gear -> tags.addAll(gear.tags()));
         for (ModuleDefinition module : moduleDefinitions(stack)) {
            tags.add(module.slotType());
            tags.add(module.effectType());
            tags.addAll(module.synergyTags());
         }
      }
      return tags;
   }

   private static List<ItemStack> equippedStacks(Player player) {
      ArrayList<ItemStack> stacks = new ArrayList<>(armorStacks(player));
      stacks.add(player.getMainHandItem());
      stacks.add(player.getOffhandItem());
      return stacks;
   }

   public enum ProtectionType {
      TOXIC,
      RADIATION,
      COLD,
      HEAT,
      FRACTURE
   }
}

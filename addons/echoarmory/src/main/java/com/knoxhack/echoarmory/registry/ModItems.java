package com.knoxhack.echoarmory.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.item.ArmoryArmorItem;
import com.knoxhack.echoarmory.item.ArmoryModuleItem;
import com.knoxhack.echoarmory.item.ArmoryUtilityItem;
import com.knoxhack.echoarmory.item.ArmoryWeaponItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;

public final class ModItems {
   public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoArmory.MODID);
   private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

   public static final EchoBackendRegistryEntry<Item> ARMORY_ALLOY_PLATE = utility("armory_alloy_plate", "Refined plate used in modular armory frames.", p -> p);
   public static final EchoBackendRegistryEntry<Item> VEIL_CRYSTAL = utility("veil_crystal", "Stores Veil pressure for modules and overload cores.", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> RESONANCE_SHARD = utility("resonance_shard", "A tuned shard used by harmonic weapons and sigils.", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> BLACKBOX_FRAGMENT = utility("blackbox_fragment", "Late-game armory memory substrate.", p -> p.rarity(Rarity.RARE).fireResistant());
   public static final EchoBackendRegistryEntry<Item> AMMO_CRYSTALS = utility("ammo_crystals", "Consumable reserve for Armory ranged weapons.", p -> p.stacksTo(64));

   public static final EchoBackendRegistryEntry<Item> ALLOY_SWORD = weapon("alloy_sword", p -> p.stacksTo(1).durability(320));
   public static final EchoBackendRegistryEntry<Item> FROST_BLADE = weapon("frost_blade", p -> p.stacksTo(1).durability(420).rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> VEIL_SABRE = weapon("veil_sabre", p -> p.stacksTo(1).durability(560).rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> HARMONIC_STAFF = weapon("harmonic_staff", p -> p.stacksTo(1).durability(420).rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> ARCANE_DAGGER = weapon("arcane_dagger", p -> p.stacksTo(1).durability(260).rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> ENERGY_RIFLE = weapon("energy_rifle", p -> p.stacksTo(1).durability(520).rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> VEIL_BOW = weapon("veil_bow", p -> p.stacksTo(1).durability(440).rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> CONVERGENCE_GUN = weapon("convergence_gun", p -> p.stacksTo(1).durability(720).rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> RESONANCE_HAMMER = weapon("resonance_hammer", p -> p.stacksTo(1).durability(620).rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> SIGIL_CHAKRAM = weapon("sigil_chakram", p -> p.stacksTo(1).durability(480).rarity(Rarity.EPIC));
   public static final EchoBackendRegistryEntry<Item> CONSTRUCT_GAUNTLET = weapon("construct_gauntlet", p -> p.stacksTo(1).durability(520).rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> ARCANE_SHIELD = weapon("arcane_shield", p -> p.stacksTo(1).durability(480).rarity(Rarity.UNCOMMON));

   public static final EchoBackendRegistryEntry<Item> VEIL_RESISTANT_HELM = armor("veil_resistant_helm", "veil_resistant_helm", ArmorType.HELMET, Rarity.UNCOMMON);
   public static final EchoBackendRegistryEntry<Item> THERMAL_CHESTPLATE = armor("thermal_chestplate", "thermal_chestplate", ArmorType.CHESTPLATE, Rarity.UNCOMMON);
   public static final EchoBackendRegistryEntry<Item> DRONE_LEGGINGS = armor("drone_leggings", "drone_leggings", ArmorType.LEGGINGS, Rarity.RARE);
   public static final EchoBackendRegistryEntry<Item> ORBITAL_BOOTS = armor("orbital_boots", "orbital_boots", ArmorType.BOOTS, Rarity.EPIC);
   public static final EchoBackendRegistryEntry<Item> CONSTRUCT_HARNESS = armor("construct_harness", "construct_harness", ArmorType.CHESTPLATE, Rarity.EPIC);
   public static final EchoBackendRegistryEntry<Item> SIGIL_AUGMENTED_SUIT = armor("sigil_augmented_suit", "sigil_augmented_suit", ArmorType.CHESTPLATE, Rarity.EPIC);

   public static final EchoBackendRegistryEntry<Item> FIRE_CORE = module("fire_core", Rarity.UNCOMMON);
   public static final EchoBackendRegistryEntry<Item> FROST_CORE = module("frost_core", Rarity.UNCOMMON);
   public static final EchoBackendRegistryEntry<Item> LIGHTNING_CORE = module("lightning_core", Rarity.UNCOMMON);
   public static final EchoBackendRegistryEntry<Item> VOID_CORE = module("void_core", Rarity.RARE);
   public static final EchoBackendRegistryEntry<Item> STABILITY_RUNE = module("stability_rune", Rarity.COMMON);
   public static final EchoBackendRegistryEntry<Item> LIFE_LEECH_SIGIL = module("life_leech_sigil", Rarity.RARE);
   public static final EchoBackendRegistryEntry<Item> VEIL_SHIELD = module("veil_shield", Rarity.RARE);
   public static final EchoBackendRegistryEntry<Item> THERMAL_REGULATOR = module("thermal_regulator", Rarity.UNCOMMON);
   public static final EchoBackendRegistryEntry<Item> GAS_MASK_FILTER = module("gas_mask_filter", Rarity.COMMON);
   public static final EchoBackendRegistryEntry<Item> RADIATION_SHIELD = module("radiation_shield", Rarity.UNCOMMON);
   public static final EchoBackendRegistryEntry<Item> MOBILITY_SERVO = module("mobility_servo", Rarity.UNCOMMON);
   public static final EchoBackendRegistryEntry<Item> DRONE_DOCK = module("drone_dock", Rarity.RARE);

   private ModItems() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
   }

   public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
      return List.copyOf(CREATIVE_ITEMS);
   }

   private static EchoBackendRegistryEntry<Item> weapon(String name, UnaryOperator<Item.Properties> properties) {
      return item(name, itemProperties -> new ArmoryWeaponItem(EchoArmory.MODID + ":" + name, itemProperties), properties);
   }

   private static EchoBackendRegistryEntry<Item> armor(String registryName, String gearId, ArmorType type, Rarity rarity) {
      return item(registryName,
         itemProperties -> new ArmoryArmorItem(EchoArmory.MODID + ":" + gearId, itemProperties),
         p -> p.stacksTo(1).humanoidArmor(ModArmorMaterials.forArmor(registryName), type).rarity(rarity));
   }

   private static EchoBackendRegistryEntry<Item> module(String name, Rarity rarity) {
      return item(name, itemProperties -> new ArmoryModuleItem(EchoArmory.MODID + ":" + name, itemProperties), p -> p.rarity(rarity));
   }

   private static EchoBackendRegistryEntry<Item> utility(String name, String tooltip, UnaryOperator<Item.Properties> properties) {
      return item(name, itemProperties -> new ArmoryUtilityItem(EchoArmory.MODID + ":" + name, tooltip, itemProperties), properties);
   }

   private static EchoBackendRegistryEntry<BlockItem> blockItem(String name, EchoBackendRegistryEntry<? extends Block> block) {
      return EchoBackendRegistryBridge.registerWithId(ITEMS, name, id -> new BlockItem(
         block.get(),
         new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
   }

   private static <T extends Item> EchoBackendRegistryEntry<T> item(String name, Function<Item.Properties, T> factory,
         UnaryOperator<Item.Properties> properties) {
      return tracked(EchoBackendRegistryBridge.registerWithId(ITEMS, name, id ->
         factory.apply(properties.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))))));
   }

   private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
      CREATIVE_ITEMS.add(item);
      return item;
   }

   static {
      ModBlocks.ALL_BLOCKS.forEach(block -> tracked(blockItem(block.id().getPath(), block)));
   }
}

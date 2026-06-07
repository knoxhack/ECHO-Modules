package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.item.CoreAccessKeyItem;
import com.knoxhack.echonexusprotocol.item.NexusArmorItem;
import com.knoxhack.echonexusprotocol.item.NexusChargeItem;
import com.knoxhack.echonexusprotocol.item.NexusFieldChargeItem;
import com.knoxhack.echonexusprotocol.item.NexusScannerVisorItem;
import com.knoxhack.echonexusprotocol.item.NexusUtilityItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;

public final class ModItems {
   public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoNexusProtocol.MODID);
   private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();
   public static final EchoBackendRegistryEntry<Item> NEXUS_SHARD = tracked(
      item("nexus_shard", p -> new NexusChargeItem(600, 2, p), p -> p.rarity(Rarity.UNCOMMON))
   );
   public static final EchoBackendRegistryEntry<Item> STABLE_NEXUS_CORE = simple("stable_nexus_core", p -> p.rarity(Rarity.RARE).fireResistant());
   public static final EchoBackendRegistryEntry<Item> BLACKBOX_FRAGMENT = simple("blackbox_fragment", p -> p.rarity(Rarity.RARE).fireResistant());
   public static final EchoBackendRegistryEntry<Item> CORRUPTED_FERRITE = simple("corrupted_ferrite", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> STATIC_FLUID = tracked(
      item("static_fluid", p -> new NexusChargeItem(150, 12, p), p -> p.stacksTo(16).rarity(Rarity.UNCOMMON))
   );
   public static final EchoBackendRegistryEntry<Item> WHITE_SIGNAL_BARK = simple("white_signal_bark");
   public static final EchoBackendRegistryEntry<Item> NEXUS_GEL = simple("nexus_gel", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> REALITY_DUST = simple("reality_dust", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> FIELD_MEMBRANE = simple("field_membrane");
   public static final EchoBackendRegistryEntry<Item> CORE_GLASS = simple("core_glass");
   public static final EchoBackendRegistryEntry<Item> SIGNAL_WIRE = simple("signal_wire");
   public static final EchoBackendRegistryEntry<Item> FILTER_MEMBRANE = simple("filter_membrane");
   public static final EchoBackendRegistryEntry<Item> STABILIZED_ALLOY = simple("stabilized_alloy");
   public static final EchoBackendRegistryEntry<Item> ECHO_CRYSTAL_DUST = simple("echo_crystal_dust");
   public static final EchoBackendRegistryEntry<Item> CLEAN_RESONANCE_BATTERY = simple("clean_resonance_battery", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> MEMORY_SHARD = simple("memory_shard", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> DATA_FRAGMENT = simple("data_fragment");
   public static final EchoBackendRegistryEntry<Item> REACTOR_CORE = simple("reactor_core", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> CORE_ACCESS_KEY = tracked(item("core_access_key", CoreAccessKeyItem::new, p -> p.rarity(Rarity.RARE).fireResistant().stacksTo(1)));
   public static final EchoBackendRegistryEntry<Item> CORE_KEY_ASSEMBLY = tracked(item("core_key_assembly", CoreAccessKeyItem::new, p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(1)));
   public static final EchoBackendRegistryEntry<Item> NEXUS_SCANNER_VISOR = tracked(
      item("nexus_scanner_visor", NexusScannerVisorItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON))
   );
   public static final EchoBackendRegistryEntry<Item> NEXUS_PICKAXE = tracked(
      item("nexus_pickaxe", p -> new NexusUtilityItem(NexusUtilityItem.Mode.PICKAXE, p), p -> p.stacksTo(1).durability(640).rarity(Rarity.RARE))
   );
   public static final EchoBackendRegistryEntry<Item> SIGNAL_BLADE = tracked(
      item(
         "signal_blade", p -> new NexusUtilityItem(NexusUtilityItem.Mode.SIGNAL_BLADE, p), p -> p.stacksTo(1).durability(520).rarity(Rarity.RARE)
      )
   );
   public static final EchoBackendRegistryEntry<Item> REALITY_ANCHOR = tracked(
      item("reality_anchor", p -> new NexusUtilityItem(NexusUtilityItem.Mode.REALITY_ANCHOR, p), p -> p.stacksTo(1).rarity(Rarity.RARE))
   );
   public static final EchoBackendRegistryEntry<Item> FIELD_ANCHOR = tracked(
      item("field_anchor", p -> new NexusUtilityItem(NexusUtilityItem.Mode.FIELD_ANCHOR, p), p -> p.stacksTo(1).rarity(Rarity.RARE))
   );
   public static final EchoBackendRegistryEntry<Item> PURITY_CHARGE = tracked(
      item("purity_charge", p -> new NexusFieldChargeItem(NexusFieldChargeItem.Mode.PURITY, p), p -> p.stacksTo(16).rarity(Rarity.UNCOMMON))
   );
   public static final EchoBackendRegistryEntry<Item> STABILIZED_PURITY_CHARGE = tracked(
      item("stabilized_purity_charge", p -> new NexusFieldChargeItem(NexusFieldChargeItem.Mode.STABILIZED_PURITY, p), p -> p.stacksTo(8).rarity(Rarity.RARE))
   );
   public static final EchoBackendRegistryEntry<Item> COLLAPSE_CHARGE = tracked(
      item("collapse_charge", p -> new NexusFieldChargeItem(NexusFieldChargeItem.Mode.COLLAPSE, p), p -> p.stacksTo(16).rarity(Rarity.RARE))
   );
   public static final EchoBackendRegistryEntry<Item> NEXUS_HELMET = armor("nexus_helmet", ArmorType.HELMET);
   public static final EchoBackendRegistryEntry<Item> NEXUS_CHESTPLATE = armor("nexus_chestplate", ArmorType.CHESTPLATE);
   public static final EchoBackendRegistryEntry<Item> NEXUS_LEGGINGS = armor("nexus_leggings", ArmorType.LEGGINGS);
   public static final EchoBackendRegistryEntry<Item> NEXUS_BOOTS = armor("nexus_boots", ArmorType.BOOTS);

   private ModItems() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
   }

   public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
      return List.copyOf(CREATIVE_ITEMS);
   }

   private static EchoBackendRegistryEntry<Item> armor(String name, ArmorType type) {
      return tracked(item(name, NexusArmorItem::new, p -> p.humanoidArmor(ArmorMaterials.DIAMOND, type).rarity(Rarity.RARE).stacksTo(1)));
   }

   private static EchoBackendRegistryEntry<Item> simple(String name) {
      return simple(name, p -> p);
   }

   private static EchoBackendRegistryEntry<Item> simple(String name, UnaryOperator<Properties> properties) {
      return tracked(item(name, Item::new, properties));
   }

   private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
      CREATIVE_ITEMS.add(item);
      return item;
   }

   static {
      ModBlocks.ALL_BLOCKS.forEach(block -> tracked(blockItem(block.id().getPath(), block)));
   }

   private static <T extends Item> EchoBackendRegistryEntry<T> item(String name, Function<Properties, T> factory,
         UnaryOperator<Properties> propertyMutator) {
      return EchoBackendRegistryBridge.registerWithId(ITEMS, name,
         id -> factory.apply(propertyMutator.apply(properties(id))));
   }

   private static EchoBackendRegistryEntry<BlockItem> blockItem(String name, EchoBackendRegistryEntry<? extends Block> block) {
      return EchoBackendRegistryBridge.registerWithId(ITEMS, name,
         id -> new BlockItem(block.get(), properties(id)));
   }

   private static Properties properties(Identifier id) {
      return new Properties().setId(ResourceKey.create(Registries.ITEM, id));
   }
}

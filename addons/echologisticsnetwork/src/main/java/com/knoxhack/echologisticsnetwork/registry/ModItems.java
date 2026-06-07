package com.knoxhack.echologisticsnetwork.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.item.LogisticsToolItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

public final class ModItems {
   public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoLogisticsNetwork.MODID);
   private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

   public static final EchoBackendRegistryEntry<Item> SUPPLY_TAG = tool("supply_tag", LogisticsToolItem.Mode.SUPPLY_TAG, p -> p);
   public static final EchoBackendRegistryEntry<Item> LOGISTICS_CHIP = simple("logistics_chip", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> COURIER_DRONE_MODULE = simple("courier_drone_module", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> ROUTE_MANIFEST = tool("route_manifest", LogisticsToolItem.Mode.ROUTE_MANIFEST, p -> p.stacksTo(16));
   public static final EchoBackendRegistryEntry<Item> LOADOUT_CARD = tool("loadout_card", LogisticsToolItem.Mode.LOADOUT_CARD, p -> p.stacksTo(16));
   public static final EchoBackendRegistryEntry<Item> REMOTE_REQUEST_TABLET = tool("remote_request_tablet", LogisticsToolItem.Mode.REMOTE_REQUEST_TABLET, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

   private ModItems() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
   }

   public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
      return List.copyOf(CREATIVE_ITEMS);
   }

   private static EchoBackendRegistryEntry<Item> simple(String name) {
      return simple(name, p -> p);
   }

   private static EchoBackendRegistryEntry<Item> simple(String name, UnaryOperator<Properties> properties) {
      return item(name, Item::new, properties);
   }

   private static EchoBackendRegistryEntry<Item> tool(String name, LogisticsToolItem.Mode mode, UnaryOperator<Properties> properties) {
      return item(name, itemProperties -> new LogisticsToolItem(mode, itemProperties), properties);
   }

   private static EchoBackendRegistryEntry<BlockItem> blockItem(String name, EchoBackendRegistryEntry<? extends Block> block) {
      return EchoBackendRegistryBridge.registerWithId(ITEMS, name, id -> new BlockItem(
         block.get(),
         new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
   }

   private static <T extends Item> EchoBackendRegistryEntry<T> item(String name, Function<Properties, T> factory,
         UnaryOperator<Properties> properties) {
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

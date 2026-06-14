package com.knoxhack.echoblockworks.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.item.BlockworksPatternCutterItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
   public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoBlockworks.MODID);
   private static final List<NativeRegistryHolder<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

   public static final NativeRegistryHolder<Item> ECHO_PATTERN_CUTTER;

   static {
      ModBlocks.creativeBlocks().forEach(block -> tracked(blockItem(block)));
      ECHO_PATTERN_CUTTER = tracked(register("echo_pattern_cutter", id -> new BlockworksPatternCutterItem(new Item.Properties()
         .setId(ResourceKey.create(Registries.ITEM, id))
         .stacksTo(1)
         .durability(384)
         .rarity(Rarity.UNCOMMON))));
   }

   private ModItems() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
   }

   public static List<NativeRegistryHolder<? extends Item>> creativeItems() {
      return List.copyOf(CREATIVE_ITEMS);
   }

   private static <T extends Item> NativeRegistryHolder<T> tracked(NativeRegistryHolder<T> item) {
      CREATIVE_ITEMS.add(item);
      return item;
   }

   private static NativeRegistryHolder<BlockItem> blockItem(NativeRegistryHolder<? extends net.minecraft.world.level.block.Block> block) {
      return register(block.id(), id -> new BlockItem(block.get(), new Item.Properties()
         .setId(ResourceKey.create(Registries.ITEM, id))
         .useBlockDescriptionPrefix()));
   }

   private static <T extends Item> NativeRegistryHolder<T> register(String id, java.util.function.Function<net.minecraft.resources.Identifier, T> factory) {
      EchoBackendRegistryEntry<T> entry = EchoBackendRegistryBridge.registerWithId(ITEMS, id, factory);
      return NativeRegistryHolder.deferred(id, entry);
   }
}

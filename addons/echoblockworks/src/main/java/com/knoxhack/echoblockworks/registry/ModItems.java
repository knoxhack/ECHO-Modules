package com.knoxhack.echoblockworks.registry;

import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.item.BlockworksPatternCutterItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
   private static final List<NativeRegistryHolder<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

   public static final NativeRegistryHolder<Item> ECHO_PATTERN_CUTTER;

   static {
      ModBlocks.creativeBlocks().forEach(block -> tracked(NativeRegistryHolder.of(block.id(), new BlockItem(block.get(), new Item.Properties()))));
      ECHO_PATTERN_CUTTER = tracked(NativeRegistryHolder.of("echo_pattern_cutter", new BlockworksPatternCutterItem(new Item.Properties()
         .stacksTo(1)
         .durability(384)
         .rarity(Rarity.UNCOMMON))));
   }

   private ModItems() {
   }

   public static void register() {
   }

   public static List<NativeRegistryHolder<? extends Item>> creativeItems() {
      return List.copyOf(CREATIVE_ITEMS);
   }

   private static <T extends Item> NativeRegistryHolder<T> tracked(NativeRegistryHolder<T> item) {
      CREATIVE_ITEMS.add(item);
      return item;
   }
}

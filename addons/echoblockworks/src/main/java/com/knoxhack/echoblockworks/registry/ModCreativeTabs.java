package com.knoxhack.echoblockworks.registry;

import com.knoxhack.echoblockworks.EchoBlockworks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public final class ModCreativeTabs {
   public static final NativeRegistryHolder<CreativeModeTab> BLOCKWORKS = NativeRegistryHolder.of("blockworks",
      CreativeModeTab.builder()
         .title(Component.translatable("itemGroup.echoblockworks.blockworks"))
         .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
         .icon(() -> ModBlocks.blockForId("reinforced_metal_panel")
            .orElse(ModBlocks.BLOCKWORKS_TABLE).get().asItem().getDefaultInstance())
         .displayItems((parameters, output) -> ModItems.creativeItems().forEach(item -> output.accept(item.get())))
         .build());

   private ModCreativeTabs() {
   }

   public static void register() {
   }
}

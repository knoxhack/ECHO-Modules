package com.knoxhack.echoblockworks.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblockworks.EchoBlockworks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public final class ModCreativeTabs {
   private static final Object TABS = EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoBlockworks.MODID);

   public static final NativeRegistryHolder<CreativeModeTab> BLOCKWORKS = register("blockworks", () -> CreativeModeTab.builder()
         .title(Component.translatable("itemGroup.echoblockworks.blockworks"))
         .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
         .icon(() -> ModBlocks.blockForId("reinforced_metal_panel")
            .orElse(ModBlocks.BLOCKWORKS_TABLE).get().asItem().getDefaultInstance())
         .displayItems((parameters, output) -> ModItems.creativeItems().forEach(item -> output.accept(item.get())))
         .build());

   private ModCreativeTabs() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
   }

   private static <T extends CreativeModeTab> NativeRegistryHolder<T> register(String id, java.util.function.Supplier<T> factory) {
      EchoBackendRegistryEntry<T> entry = EchoBackendRegistryBridge.register(TABS, id, factory);
      return NativeRegistryHolder.deferred(id, entry);
   }
}

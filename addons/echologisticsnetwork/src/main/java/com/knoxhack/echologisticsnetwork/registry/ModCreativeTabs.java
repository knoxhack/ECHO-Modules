package com.knoxhack.echologisticsnetwork.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModCreativeTabs {
   private static final Object TABS = EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoLogisticsNetwork.MODID);

   public static final EchoBackendRegistryEntry<CreativeModeTab> LOGISTICS_NETWORK = EchoBackendRegistryBridge.register(TABS,
      "logistics_network",
      () -> CreativeModeTab.builder()
         .title(Component.translatable("itemGroup.echologisticsnetwork.logistics_network"))
         .withTabsBefore(new ResourceKey[]{CreativeModeTabs.REDSTONE_BLOCKS})
         .icon(() -> ((Item)ModItems.REMOTE_REQUEST_TABLET.get()).getDefaultInstance())
         .displayItems((parameters, output) -> ModItems.creativeItems().forEach(item -> output.accept(item.get())))
         .build()
   );

   private ModCreativeTabs() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
   }
}

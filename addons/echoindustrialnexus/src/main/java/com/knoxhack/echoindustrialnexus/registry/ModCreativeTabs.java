package com.knoxhack.echoindustrialnexus.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModCreativeTabs {
   private static final Object TABS = EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoIndustrialNexus.MODID);
   public static final EchoBackendRegistryEntry<CreativeModeTab> INDUSTRIAL_NEXUS = EchoBackendRegistryBridge.register(
      TABS,
      "industrial_nexus",
      () -> CreativeModeTab.builder()
         .title(Component.translatable("itemGroup.echoindustrialnexus.industrial_nexus"))
         .withTabsBefore(new ResourceKey[]{CreativeModeTabs.REDSTONE_BLOCKS})
         .icon(() -> ((Item)ModItems.THERMAL_WRENCH.get()).getDefaultInstance())
         .displayItems((parameters, output) -> ModItems.creativeItems().forEach(output::accept))
         .build()
   );

   private ModCreativeTabs() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
   }
}

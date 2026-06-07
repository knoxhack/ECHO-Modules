package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModCreativeTabs {
   private static final Object TABS = EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoNexusProtocol.MODID);
   public static final EchoBackendRegistryEntry<CreativeModeTab> NEXUS_PROTOCOL = EchoBackendRegistryBridge.register(TABS,
      "nexus_protocol",
      () -> CreativeModeTab.builder()
         .title(Component.translatable("itemGroup.echonexusprotocol.nexus_protocol"))
         .withTabsBefore(new ResourceKey[]{CreativeModeTabs.COMBAT})
         .icon(() -> ((Item)ModItems.NEXUS_SHARD.get()).getDefaultInstance())
         .displayItems((parameters, output) -> ModItems.creativeItems().forEach(item -> output.accept(item.get())))
         .build()
   );

   private ModCreativeTabs() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
   }
}

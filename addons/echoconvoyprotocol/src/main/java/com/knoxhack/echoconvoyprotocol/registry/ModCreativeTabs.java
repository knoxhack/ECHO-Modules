package com.knoxhack.echoconvoyprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

public final class ModCreativeTabs {
   private static final Object CREATIVE_TABS = EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoConvoyProtocol.MODID);

   public static final EchoBackendRegistryEntry<CreativeModeTab> CONVOY_PROTOCOL =
      EchoBackendRegistryBridge.register(CREATIVE_TABS, "convoy_protocol", () -> CreativeModeTab.builder()
         .title(Component.translatable("itemGroup.echoconvoyprotocol.convoy_protocol"))
         .icon(() -> ModItems.ROUTE_BEACON.get().getDefaultInstance())
         .displayItems((parameters, output) -> ModItems.creativeItems().forEach(item -> output.accept(item.get())))
         .build());

   private ModCreativeTabs() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(CREATIVE_TABS, eventBus);
   }
}

package com.knoxhack.echostationfall.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echostationfall.EchoStationfall;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public final class ModCreativeTabs {
    private static final Object TABS = EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoStationfall.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> STATIONFALL =
            EchoBackendRegistryBridge.register(TABS, "stationfall", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.echostationfall.stationfall"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.STATIONFALL_BLACKBOX.get().getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.creativeItems().forEach(output::accept))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}

package com.knoxhack.signalos.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.signalos.SignalOS;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public final class ModCreativeTabs {
    private static final Object CREATIVE_MODE_TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, SignalOS.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> SIGNALOS_TAB = EchoBackendRegistryBridge.register(CREATIVE_MODE_TABS,
            "signalos",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.signalos"))
                    .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                    .icon(() -> ModBlocks.TERMINAL_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.TERMINAL_ITEM.get());
                        output.accept(ModBlocks.WORKSTATION_ITEM.get());
                        output.accept(ModBlocks.SERVER_RACK_ITEM.get());
                        output.accept(ModBlocks.NETWORK_RELAY_ITEM.get());
                        output.accept(ModBlocks.DATA_DRIVE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(CREATIVE_MODE_TABS, eventBus);
    }
}

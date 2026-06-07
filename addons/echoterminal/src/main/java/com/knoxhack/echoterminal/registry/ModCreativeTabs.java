package com.knoxhack.echoterminal.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoterminal.EchoTerminal;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public final class ModCreativeTabs {
    private static final Object CREATIVE_MODE_TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoTerminal.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> TERMINAL_TAB = EchoBackendRegistryBridge.register(CREATIVE_MODE_TABS,
            "terminal_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.echoterminal"))
                    .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                    .icon(() -> ModBlocks.ECHO_TERMINAL_BLOCK_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.ECHO_TERMINAL_BLOCK_ITEM.get());
                        output.accept(ModItems.ECHO_TERMINAL_REMOTE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(CREATIVE_MODE_TABS, eventBus);
    }
}

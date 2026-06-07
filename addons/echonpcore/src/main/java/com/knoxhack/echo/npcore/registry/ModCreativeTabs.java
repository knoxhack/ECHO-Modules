package com.knoxhack.echo.npcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.npcore.EchoNpcCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public final class ModCreativeTabs {
    private static final Object TABS = EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoNpcCore.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> NPCORE = EchoBackendRegistryBridge.register(
            TABS,
            "npcore",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.echonpcore.npcore"))
                    .withTabsBefore(new ResourceKey[]{CreativeModeTabs.SPAWN_EGGS})
                    .icon(() -> ModItems.ECHO_NPC_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.creativeItems().forEach(output::accept))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}

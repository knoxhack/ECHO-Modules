package com.knoxhack.echofamiliarcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echofamiliarcore.EchoFamiliarCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModCreativeTabs {
    private static final Object TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoFamiliarCore.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> FAMILIARCORE_TAB = EchoBackendRegistryBridge.register(TABS,
            "familiarcore",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.echofamiliarcore.familiarcore"))
                    .withTabsBefore(new ResourceKey[]{CreativeModeTabs.TOOLS_AND_UTILITIES})
                    .icon(() -> ((Item) ModItems.AETHER_WISP_CHARM.get()).getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.creativeItems().forEach(entry -> output.accept((Item) entry.get())))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}

package com.knoxhack.echoritualcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoritualcore.EchoRitualCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModCreativeTabs {
    private static final Object TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoRitualCore.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> RITUALCORE_TAB = EchoBackendRegistryBridge.register(TABS,
            "ritualcore",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.echoritualcore.ritualcore"))
                    .withTabsBefore(new ResourceKey[]{CreativeModeTabs.FUNCTIONAL_BLOCKS})
                    .icon(() -> ((Item) ModItems.RITUAL_FOCUS.get()).getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.creativeItems().forEach(output::accept))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}

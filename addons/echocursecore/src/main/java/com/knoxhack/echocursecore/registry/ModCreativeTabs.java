package com.knoxhack.echocursecore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echocursecore.EchoCurseCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModCreativeTabs {
    private static final Object TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoCurseCore.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> CURSECORE_TAB = EchoBackendRegistryBridge.register(TABS, 
            "cursecore",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.echocursecore.cursecore"))
                    .withTabsBefore(new ResourceKey[]{CreativeModeTabs.INGREDIENTS})
                    .icon(() -> ((Item) ModItems.ECHO_ROT_SAMPLE.get()).getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.creativeItems().forEach(output::accept))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}

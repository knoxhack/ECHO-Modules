package com.knoxhack.echomultiblockcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public final class ModCreativeTabs {
    private static final Object TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoMultiblockCore.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> MULTIBLOCK_CORE = EchoBackendRegistryBridge.register(
            TABS,
            "multiblock_core",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.echomultiblockcore.multiblock_core"))
                    .withTabsBefore(new ResourceKey[]{CreativeModeTabs.REDSTONE_BLOCKS})
                    .icon(() -> ModItems.SIGNAL_TOWER_BLUEPRINT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.creativeItems().forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}

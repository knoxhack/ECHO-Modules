package com.knoxhack.echopowergrid.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModCreativeTabs {
    private static final Object CREATIVE_TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoPowerGrid.MODID);

    public static final NativeRegistryHolder<CreativeModeTab> POWERGRID_TAB = tracked(
        "powergrid",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.echopowergrid.powergrid"))
            .withTabsBefore(new ResourceKey[]{CreativeModeTabs.FUNCTIONAL_BLOCKS})
            .icon(() -> ((Item) ModItems.POWER_CELL.get()).getDefaultInstance())
            .displayItems((parameters, output) -> ModItems.creativeItems().forEach(item -> output.accept(item.get())))
            .build()
    );

    private ModCreativeTabs() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(CREATIVE_TABS, eventBus);
    }

    private static NativeRegistryHolder<CreativeModeTab> tracked(
            String name, java.util.function.Supplier<? extends CreativeModeTab> tab) {
        EchoBackendRegistryEntry<CreativeModeTab> entry = EchoBackendRegistryBridge.register(CREATIVE_TABS, name, tab);
        return NativeRegistryHolder.deferred(name, entry);
    }
}

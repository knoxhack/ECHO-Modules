package com.knoxhack.echopowergrid.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModCreativeTabs {
    public static final NativeRegistryHolder<CreativeModeTab> POWERGRID_TAB = NativeRegistryHolder.of(
        "powergrid",
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.echopowergrid.powergrid"))
            .withTabsBefore(new ResourceKey[]{CreativeModeTabs.FUNCTIONAL_BLOCKS})
            .icon(() -> ((Item) ModItems.POWER_CELL.get()).getDefaultInstance())
            .displayItems((parameters, output) -> ModItems.creativeItems().forEach(item -> output.accept(item.get())))
            .build()
    );

    private ModCreativeTabs() {}

    public static void register() {
    }
}

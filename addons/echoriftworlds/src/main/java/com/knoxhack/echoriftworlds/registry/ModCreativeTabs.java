package com.knoxhack.echoriftworlds.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoriftworlds.EchoRiftWorlds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModCreativeTabs {
    private static final Object TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoRiftWorlds.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> RIFTWORLDS_TAB = EchoBackendRegistryBridge.register(TABS,
            "riftworlds",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.echoriftworlds.riftworlds"))
                    .withTabsBefore(new ResourceKey[]{CreativeModeTabs.FUNCTIONAL_BLOCKS})
                    .icon(() -> ((Item) ModItems.RIFT_ANCHOR_SHARD.get()).getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.creativeItems().forEach(output::accept))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}

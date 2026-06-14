package com.knoxhack.echo.settlementcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.settlementcore.EchoSettlementCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

public final class ModCreativeTabs {
    private static final Object CREATIVE_TABS =
        EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoSettlementCore.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> SETTLEMENT =
        EchoBackendRegistryBridge.register(CREATIVE_TABS, "settlement", () -> CreativeModeTab.builder()
            .title(Component.literal("ECHO: Settlement"))
            .icon(() -> ModBlocks.AIRLOCK.get().asItem().getDefaultInstance())
            .displayItems((parameters, output) -> ModItems.creativeItems().forEach(item -> output.accept(item.get())))
            .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(CREATIVE_TABS, eventBus);
    }
}

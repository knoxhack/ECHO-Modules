package com.knoxhack.echo.equipmentcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.equipmentcore.EchoEquipmentCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

public final class ModCreativeTabs {
    private static final Object CREATIVE_TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoEquipmentCore.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> EQUIPMENT =
            EchoBackendRegistryBridge.register(CREATIVE_TABS, "equipment", () -> CreativeModeTab.builder()
                    .title(Component.literal("ECHO: EquipmentCore"))
                    .icon(() -> ModItems.HADAL_HARDSUIT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.creativeItems().forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(CREATIVE_TABS, eventBus);
    }
}

package com.knoxhack.echomultiblockcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.BlueprintData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class ModDataComponents {
    private static final Object DATA_COMPONENTS =
            EchoBackendRegistryBridge.create(Registries.DATA_COMPONENT_TYPE, EchoMultiblockCore.MODID);

    public static final EchoBackendRegistryEntry<DataComponentType<BlueprintData>> BLUEPRINT_DATA =
            EchoBackendRegistryBridge.register(DATA_COMPONENTS, "blueprint_data", () -> DataComponentType.<BlueprintData>builder()
                    .persistent(BlueprintData.CODEC)
                    .networkSynchronized(BlueprintData.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(DATA_COMPONENTS, eventBus);
    }
}

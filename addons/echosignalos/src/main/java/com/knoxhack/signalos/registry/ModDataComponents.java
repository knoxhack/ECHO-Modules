package com.knoxhack.signalos.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.SignalOsDriveData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class ModDataComponents {
    public static final Object DATA_COMPONENT_TYPES =
            EchoBackendRegistryBridge.create(Registries.DATA_COMPONENT_TYPE, SignalOS.MODID);

    public static final EchoBackendRegistryEntry<DataComponentType<SignalOsDriveData>> DRIVE_DATA =
            EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "drive_data",
                    () -> DataComponentType.<SignalOsDriveData>builder()
                    .persistent(SignalOsDriveData.CODEC)
                    .networkSynchronized(SignalOsDriveData.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(DATA_COMPONENT_TYPES, eventBus);
    }
}

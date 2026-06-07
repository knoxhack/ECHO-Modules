package com.knoxhack.echorecovery.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echorecovery.EchoRecovery;
import net.minecraft.core.registries.Registries;

public final class ModDataComponents {
    public static final Object DATA_COMPONENTS = EchoBackendRegistryBridge.create(Registries.DATA_COMPONENT_TYPE, EchoRecovery.MODID);

    private ModDataComponents() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(DATA_COMPONENTS, eventBus);
    }
}

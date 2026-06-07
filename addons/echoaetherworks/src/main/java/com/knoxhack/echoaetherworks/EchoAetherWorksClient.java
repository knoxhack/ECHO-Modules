package com.knoxhack.echoaetherworks;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoaetherworks.client.screen.AetherMachineScreen;
import com.knoxhack.echoaetherworks.registry.ModMenus;

public final class EchoAetherWorksClient {
    public EchoAetherWorksClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoAetherWorksClient::registerScreens);
    }

    private static void registerScreens(Object event) {
        EchoBackendClientBridge.registerMenuScreen(event, ModMenus.AETHER_MACHINE.get(), AetherMachineScreen.class);
    }
}

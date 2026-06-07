package com.knoxhack.echorecovery;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echorecovery.client.screen.GraveScreen;
import com.knoxhack.echorecovery.registry.ModMenus;

public class EchoRecoveryClient {

    public EchoRecoveryClient(Object modEventBus) {
    }

    static void registerMenuScreens(Object event) {
        EchoBackendClientBridge.registerMenuScreen(event, ModMenus.GRAVE.get(), GraveScreen.class);
    }
}

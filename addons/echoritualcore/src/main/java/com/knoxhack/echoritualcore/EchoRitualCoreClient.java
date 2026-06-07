package com.knoxhack.echoritualcore;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoritualcore.client.screen.RitualAltarScreen;
import com.knoxhack.echoritualcore.registry.ModMenus;

public class EchoRitualCoreClient {
    public EchoRitualCoreClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::registerScreens);
    }

    private void registerScreens(Object event) {
        EchoBackendClientBridge.registerMenuScreen(event, ModMenus.RITUAL_ALTAR.get(), RitualAltarScreen.class);
    }
}

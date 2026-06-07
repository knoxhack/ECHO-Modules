package com.knoxhack.echocursecore;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echocursecore.client.CurseContractScreen;
import com.knoxhack.echocursecore.client.CurseHudOverlay;
import com.knoxhack.echocursecore.registry.ModMenus;

public final class EchoCurseCoreClient {
    public EchoCurseCoreClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoCurseCoreClient::registerScreens);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoCurseCoreClient::onRenderGui);
    }

    private static void registerScreens(Object event) {
        EchoBackendClientBridge.registerMenuScreen(event, ModMenus.CURSE_CONTRACT.get(), CurseContractScreen.class);
    }

    private static void onRenderGui(Object event) {
        var graphics = EchoBackendClientBridge.guiGraphics(event);
        if (graphics != null) {
            CurseHudOverlay.render(graphics);
        }
    }
}

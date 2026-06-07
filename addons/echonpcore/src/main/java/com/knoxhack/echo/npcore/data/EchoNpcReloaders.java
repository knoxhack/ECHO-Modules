package com.knoxhack.echo.npcore.data;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echo.npcore.EchoNpcCore;
import net.minecraft.resources.Identifier;

public final class EchoNpcReloaders {
    private EchoNpcReloaders() {
    }

    public static void addServerReloadListeners(Object event) {
        EchoBackendWorldEventBridge.addServerReloadListener(
                event,
                Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "content"),
                new EchoNpcJsonReloadListener());
    }
}

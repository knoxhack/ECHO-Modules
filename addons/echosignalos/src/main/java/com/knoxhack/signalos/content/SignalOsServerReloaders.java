package com.knoxhack.signalos.content;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.signalos.SignalOS;
import net.minecraft.resources.Identifier;

public final class SignalOsServerReloaders {
    private SignalOsServerReloaders() {
    }

    public static void addServerReloadListeners(Object event) {
        EchoBackendWorldEventBridge.addServerReloadListener(event,
                Identifier.fromNamespaceAndPath(SignalOS.MODID, "content"),
                new SignalOsJsonContentLoader());
    }
}

package com.knoxhack.echorecovery.content;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echorecovery.EchoRecovery;
import net.minecraft.resources.Identifier;

public final class RecoveryReloaders {
    private RecoveryReloaders() {
    }

    public static void addServerReloadListeners(Object event) {
        EchoBackendWorldEventBridge.addServerReloadListener(
                event,
                Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "content"),
                new RecoveryJsonReloadListener());
    }
}

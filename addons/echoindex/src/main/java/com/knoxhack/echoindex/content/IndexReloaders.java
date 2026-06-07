package com.knoxhack.echoindex.content;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoindex.EchoIndex;
import net.minecraft.resources.Identifier;

public final class IndexReloaders {
    private IndexReloaders() {
    }

    public static void addServerReloadListeners(Object event) {
        EchoBackendWorldEventBridge.addServerReloadListener(
                event,
                Identifier.fromNamespaceAndPath(EchoIndex.MODID, "content"),
                new IndexJsonReloadListener());
        EchoBackendWorldEventBridge.addServerReloadListener(
                event,
                Identifier.fromNamespaceAndPath(EchoIndex.MODID, "sources"),
                new IndexSourceReloadListener());
    }
}

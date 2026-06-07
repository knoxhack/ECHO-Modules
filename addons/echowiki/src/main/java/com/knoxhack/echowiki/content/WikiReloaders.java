package com.knoxhack.echowiki.content;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echowiki.EchoWiki;
import net.minecraft.resources.Identifier;

public final class WikiReloaders {
    private WikiReloaders() {
    }

    public static void addServerReloadListeners(Object event) {
        EchoBackendWorldEventBridge.addServerReloadListener(event,
                Identifier.fromNamespaceAndPath(EchoWiki.MODID, "content"),
                new WikiJsonReloadListener());
    }
}

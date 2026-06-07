package com.knoxhack.echoindex.client;

import com.knoxhack.echoindex.EchoIndexClient;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.Map;
import net.minecraft.client.Minecraft;

public final class IndexFallbackScreen {
    private IndexFallbackScreen() {
    }

    public static void open() {
        EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(
                "open",
                "index.fallback_screen.open_catalog",
                IndexCatalogScreen.class.getName(),
                Map.of(
                        "targetScreenClass", IndexCatalogScreen.class.getName(),
                        "transitionSource", "index_fallback_screen"
                ));
        if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
            return;
        }
        Minecraft.getInstance().setScreen(new IndexCatalogScreen());
    }
}

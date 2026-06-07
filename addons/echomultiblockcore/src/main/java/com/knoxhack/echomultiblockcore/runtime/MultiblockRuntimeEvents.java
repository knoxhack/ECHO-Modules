package com.knoxhack.echomultiblockcore.runtime;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echomultiblockcore.network.MultiblockDefinitionSync;
import net.minecraft.server.level.ServerPlayer;

public final class MultiblockRuntimeEvents {
    private MultiblockRuntimeEvents() {
    }

    public static void onServerTick(Object event) {
        // Controllers tick through block entity tickers; this hook stays available for optional global sweeps.
    }

    public static void onPlayerLoggedIn(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.loggedInServerPlayer(event);
        if (player != null) {
            MultiblockDefinitionSync.sendTo(player);
        }
    }
}

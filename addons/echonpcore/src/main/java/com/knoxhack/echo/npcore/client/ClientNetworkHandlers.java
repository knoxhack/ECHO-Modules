package com.knoxhack.echo.npcore.client;

import com.knoxhack.echo.npcore.client.screen.EchoNpcScreen;
import com.knoxhack.echo.npcore.client.screencore.ScreenCoreNpcScreenAdapter;
import com.knoxhack.echo.npcore.config.EchoNpcCoreConfig;
import com.knoxhack.echo.npcore.network.OpenNpcScreenPacket;
import com.knoxhack.echo.npcore.network.SyncNpcScreenStatePacket;

public final class ClientNetworkHandlers {
    private ClientNetworkHandlers() {
    }

    public static void handleOpenNpcScreen(OpenNpcScreenPacket packet) {
        if (!ScreenCoreNpcScreenAdapter.tryOpen(packet.state())) {
            openClassic(packet);
        }
    }

    public static void handleSyncNpcScreenState(SyncNpcScreenStatePacket packet) {
        if (!ScreenCoreNpcScreenAdapter.trySync(packet.state())) {
            updateClassic(packet);
        }
    }

    private static void openClassic(OpenNpcScreenPacket packet) {
        if (EchoNpcCoreConfig.bool(EchoNpcCoreConfig.FALLBACK_TO_CLASSIC_NPC_SCREENS, true)) {
            EchoNpcScreen.open(packet.state());
        }
    }

    private static void updateClassic(SyncNpcScreenStatePacket packet) {
        if (EchoNpcCoreConfig.bool(EchoNpcCoreConfig.FALLBACK_TO_CLASSIC_NPC_SCREENS, true)) {
            EchoNpcScreen.updateOrOpen(packet.state());
        }
    }
}

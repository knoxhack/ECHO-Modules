package com.knoxhack.echonetcore.service;

import com.knoxhack.echonetcore.client.EchoNetClientActions;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class EchoNeoForgeClientNetworkRuntime {
    private EchoNeoForgeClientNetworkRuntime() {
    }

    public static void install() {
        EchoNetClientActions.installRuntimeClientActionTransport(payload -> {
            ClientPacketDistributor.sendToServer(payload);
            return true;
        });
    }
}

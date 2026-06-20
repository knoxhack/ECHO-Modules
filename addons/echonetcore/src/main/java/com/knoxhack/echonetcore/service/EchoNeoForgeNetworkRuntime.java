package com.knoxhack.echonetcore.service;

import com.knoxhack.echonetcore.EchoNetCore;
import com.knoxhack.echonetcore.api.EchoNetSend;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EchoNeoForgeNetworkRuntime {
    private static boolean installed;

    private EchoNeoForgeNetworkRuntime() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        EchoNetSend.installRuntimeSendTransport((player, payload, kind) -> {
            PacketDistributor.sendToPlayer(player, payload);
            return true;
        });
        installClientTransportIfPresent();
        EchoNetCore.LOGGER.info("ECHO: NetCore NeoForge packet transport online.");
    }

    private static void installClientTransportIfPresent() {
        try {
            Class.forName("net.minecraft.client.Minecraft");
            Class.forName("com.knoxhack.echonetcore.service.EchoNeoForgeClientNetworkRuntime")
                    .getMethod("install")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Dedicated servers do not expose the client packet distributor.
        }
    }
}

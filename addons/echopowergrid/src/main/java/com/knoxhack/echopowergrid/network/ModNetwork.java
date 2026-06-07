package com.knoxhack.echopowergrid.network;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static EchoPayloadRegistrar registerPayloads() {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.clientboundSync(registrar, PowerGridNetworkSummaryPacket.TYPE,
                PowerGridNetworkSummaryPacket.CODEC,
                (packet, player, context) -> handleClient("handle", packet));
        return registrar;
    }

    private static void handleClient(String method, Object packet) {
        try {
            Class.forName("com.knoxhack.echopowergrid.client.PowerGridClientState")
                    .getMethod(method, packet.getClass())
                    .invoke(null, packet);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}

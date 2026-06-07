package com.knoxhack.echolens.network;

import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import com.knoxhack.echonetcore.api.EchoRateLimitPolicy;
import com.knoxhack.echolens.config.LensConfig;
import com.knoxhack.echolens.registry.LensServerScanService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ModNetwork {
    private static boolean registered;

    private ModNetwork() {
    }

    public static void registerPayloads(Object event) {
        registered = true;
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.serverboundAction(registrar, LensScanRequestPacket.TYPE, LensScanRequestPacket.CODEC,
                EchoRateLimitPolicy.of(LensConfig.integer(LensConfig.SERVER_SCAN_RATE_LIMIT, 4),
                        "echolens_deep_scan"),
                ModNetwork::handleRequest);
        EchoNetPayloads.clientboundSync(registrar, LensScanResponsePacket.TYPE, LensScanResponsePacket.CODEC,
                ModNetwork::handleResponse);
    }

    public static boolean registered() {
        return registered;
    }

    private static void handleRequest(LensScanRequestPacket packet, ServerPlayer player, EchoPayloadContext context) {
        EchoNetSend.toPlayer(player, LensServerScanService.scan(player, packet), EchoPacketKind.CLIENTBOUND_SYNC);
    }

    private static void handleResponse(LensScanResponsePacket packet, Player player, EchoPayloadContext context) {
        try {
            Class.forName("com.knoxhack.echolens.client.LensServerScanClientState")
                    .getMethod("apply", LensScanResponsePacket.class)
                    .invoke(null, packet);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            com.knoxhack.echolens.EchoLens.LOGGER.warn("Could not apply Lens server scan response.", exception);
        }
    }
}

package com.knoxhack.echocursecore.network;

import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echocursecore.EchoCurseCore;
import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import net.minecraft.server.level.ServerPlayer;

public final class CurseCoreNetwork {
    private CurseCoreNetwork() {
    }

    public static void registerPayloads(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.clientboundSync(registrar, CurseHudSyncPacket.TYPE, CurseHudSyncPacket.CODEC,
                (packet, player, context) -> handleClient(packet));
    }

    public static void sendTo(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            EchoNetSend.toPlayer(player, CurseHudSyncPacket.from(player), EchoPacketKind.CLIENTBOUND_SYNC);
        } catch (RuntimeException exception) {
            EchoCurseCore.LOGGER.debug("Curse HUD sync skipped for {}: {}",
                    player.getScoreboardName(), exception.getMessage());
        }
    }

    private static void handleClient(CurseHudSyncPacket packet) {
        try {
            Class.forName("com.knoxhack.echocursecore.client.CurseHudClientState")
                    .getMethod("apply", CurseHudSyncPacket.class)
                    .invoke(null, packet);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoCurseCore.LOGGER.debug("Curse HUD sync skipped on non-client context.", exception);
        }
    }
}

package com.knoxhack.signalos.network;

import com.knoxhack.echonetcore.api.EchoNetSend;
import net.minecraft.server.level.ServerPlayer;

public final class SignalOsTerminalSync {
    private SignalOsTerminalSync() {
    }

    public static void send(ServerPlayer player) {
        if (player != null) {
            EchoNetSend.toPlayer(player, SignalOsTerminalStatePacket.from(player));
        }
    }
}

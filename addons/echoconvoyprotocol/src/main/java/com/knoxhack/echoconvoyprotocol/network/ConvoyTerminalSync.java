package com.knoxhack.echoconvoyprotocol.network;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import net.minecraft.server.level.ServerPlayer;

public final class ConvoyTerminalSync {
   private ConvoyTerminalSync() {
   }

   public static void send(ServerPlayer player) {
      if (player != null) {
         try {
            EchoNetSend.toPlayer(player, ConvoyTerminalStatePacket.from(player), EchoPacketKind.CLIENTBOUND_SYNC);
         } catch (RuntimeException exception) {
            EchoConvoyProtocol.LOGGER.debug("Skipped optional convoy terminal sync for {}: {}", player.getName().getString(), exception.getMessage());
         }
      }
   }
}

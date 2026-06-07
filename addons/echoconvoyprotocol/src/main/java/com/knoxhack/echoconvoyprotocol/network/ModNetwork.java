package com.knoxhack.echoconvoyprotocol.network;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;

public final class ModNetwork {
   private ModNetwork() {
   }

   public static void registerPayloads(Object event) {
      EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
      EchoNetPayloads.clientboundSync(registrar, ConvoyTerminalStatePacket.TYPE, ConvoyTerminalStatePacket.CODEC,
         ModNetwork::handleTerminalState);
   }

   private static void handleTerminalState(ConvoyTerminalStatePacket packet,
         net.minecraft.world.entity.player.Player player, EchoPayloadContext context) {
      ConvoyTerminalClientState.apply(packet);
   }
}

package com.knoxhack.echoindustrialnexus.network;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;

public final class ModNetwork {
   private ModNetwork() {
   }

   public static void registerPayloads(Object event) {
      EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
      EchoNetPayloads.clientboundSync(registrar, IndustrialFactorySnapshotPacket.TYPE,
         IndustrialFactorySnapshotPacket.CODEC,
         (packet, player, context) -> handleClient("handle", packet));
   }

   private static void handleClient(String method, Object packet) {
      try {
         Class.forName("com.knoxhack.echoindustrialnexus.client.IndustrialFactoryClientState")
            .getMethod(method, packet.getClass())
            .invoke(null, packet);
      } catch (ReflectiveOperationException ignored) {
      }
   }
}

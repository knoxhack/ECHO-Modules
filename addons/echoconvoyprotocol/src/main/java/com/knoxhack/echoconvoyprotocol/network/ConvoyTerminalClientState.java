package com.knoxhack.echoconvoyprotocol.network;

public final class ConvoyTerminalClientState {
   private static volatile ConvoyTerminalStatePacket snapshot = ConvoyTerminalStatePacket.empty();

   private ConvoyTerminalClientState() {
   }

   public static void apply(ConvoyTerminalStatePacket packet) {
      snapshot = packet == null ? ConvoyTerminalStatePacket.empty() : packet;
   }

   public static ConvoyTerminalStatePacket snapshot() {
      return snapshot;
   }
}

package com.knoxhack.echoindustrialnexus.client;

import com.knoxhack.echoindustrialnexus.network.IndustrialFactorySnapshotPacket;
import java.util.List;

public final class IndustrialFactoryClientState {
   private static volatile IndustrialFactorySnapshotPacket snapshot =
      new IndustrialFactorySnapshotPacket(List.of(), "Factory Command awaiting sync.", 0L);
   private static volatile long lastRequestMillis;

   private IndustrialFactoryClientState() {
   }

   public static void handle(IndustrialFactorySnapshotPacket packet) {
      if (packet != null) {
         snapshot = packet;
      }
   }

   public static IndustrialFactorySnapshotPacket snapshot() {
      return snapshot;
   }

   public static boolean shouldRequest(long intervalMillis) {
      long now = System.currentTimeMillis();
      if (now - lastRequestMillis < Math.max(250L, intervalMillis)) {
         return false;
      }
      lastRequestMillis = now;
      return true;
   }
}

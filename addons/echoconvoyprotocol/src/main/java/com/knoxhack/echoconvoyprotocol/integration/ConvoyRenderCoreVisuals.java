package com.knoxhack.echoconvoyprotocol.integration;

public final class ConvoyRenderCoreVisuals {
   private ConvoyRenderCoreVisuals() {
   }

   public static String roverVisualStateName(boolean damaged, boolean hasTravelPower, boolean driven, boolean moving) {
      if (damaged) {
         return "DAMAGED";
      }
      if (!hasTravelPower) {
         return "OFFLINE";
      }
      if (driven || moving) {
         return "ACTIVE";
      }
      return "ONLINE";
   }
}

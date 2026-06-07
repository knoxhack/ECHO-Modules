package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import net.minecraft.resources.Identifier;

public final class ConvoyTerminalIds {
   public static final Identifier CONVOY_TAB = id("convoy_routes");
   public static final Identifier SCAN_ACTION = id("scan");
   public static final Identifier START_ACTION = id("start_route");
   public static final Identifier COMPLETE_ACTION = id("complete_route");
   public static final Identifier CLAIM_ACTION = id("claim_route");

   private ConvoyTerminalIds() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, path);
   }
}

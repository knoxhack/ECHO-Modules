package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import net.minecraft.resources.Identifier;

public final class LogisticsTerminalIds {
   public static final Identifier LOGISTICS_TAB = id("logistics");
   public static final Identifier SCAN_ACTION = id("scan");
   public static final Identifier REQUEST_ACTION = id("request_loadout");
   public static final Identifier DISPATCH_ACTION = id("dispatch_drone");
   public static final Identifier CANCEL_ACTION = id("cancel_delivery");
   public static final Identifier CLAIM_RELAY_ACTION = id("claim_relay_rewards");
   public static final Identifier REFRESH_OFFERS_ACTION = id("refresh_faction_offers");

   private LogisticsTerminalIds() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, path);
   }
}

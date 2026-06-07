package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import net.minecraft.resources.Identifier;

public final class IndustrialTerminalIds {
   public static final Identifier ECHO_TAB = Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, "industrial_nexus");
   public static final Identifier CHAPTER = Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, "industrial_nexus");
   public static final Identifier FACTORY_SYNC = id("factory/sync");
   public static final Identifier FACTORY_REVALIDATE = id("factory/revalidate");
   public static final Identifier FACTORY_QUEUE_TASK = id("factory/queue_task");
   public static final Identifier FACTORY_CLEAR_QUEUE = id("factory/clear_queue");
   public static final Identifier FACTORY_RETRY_BLOCKED = id("factory/retry_blocked");
   public static final Identifier FACTORY_REQUEST_LOGISTICS = id("factory/request_logistics");
   public static final Identifier FACTORY_TOGGLE_LOGISTICS_RESTOCK = id("factory/toggle_logistics_restock");
   public static final Identifier FACTORY_SET_LOGISTICS_RESTOCK_TARGET = id("factory/set_logistics_restock_target");
   public static final Identifier FACTORY_REQUEST_LOGISTICS_RESTOCK_NOW = id("factory/request_logistics_restock_now");

   private IndustrialTerminalIds() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, path);
   }
}

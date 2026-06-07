package com.knoxhack.echonexusprotocol.integration;

import net.minecraft.resources.Identifier;

public final class NexusTerminalIds {
   public static final Identifier CHAPTER_ID = id("nexus_protocol");
   public static final Identifier RESEARCH_TAB = id("research");
   public static final Identifier FIELD_TAB = id("field");
   public static final Identifier FIELD_MAP_TAB = id("field_map");
   public static final Identifier SCAN_ACTION = id("scan");

   private NexusTerminalIds() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("echonexusprotocol", path);
   }
}

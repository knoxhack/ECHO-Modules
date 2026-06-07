package com.knoxhack.echoblackboxprotocol.integration;

import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import net.minecraft.resources.Identifier;

public final class BlackboxTerminalIds {
   public static final Identifier CHAPTER_ID = id("blackbox_protocol");
   public static final Identifier ACCESS_TAB = id("blackbox_access");
   public static final Identifier ARCHIVE_TAB = id("memory_archive");
   public static final Identifier TRUTH_TAB = id("truth_engine");

   private BlackboxTerminalIds() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoBlackboxProtocol.MODID, path);
   }
}

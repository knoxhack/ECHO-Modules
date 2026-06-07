package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import net.minecraft.resources.Identifier;

public final class ReclamationTerminalIds {
   public static final Identifier FIELD_TAB = id("agriculture_reclamation");
   public static final Identifier CHAPTER = id("agriculture_reclamation");
   public static final Identifier SCAN_ACTION = id("scan_reclamation");
   public static final Identifier REPORT_ACTION = id("report_reclamation");

   private ReclamationTerminalIds() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, path);
   }
}

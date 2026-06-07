package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.EchoRenderCore;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

public final class RenderCoreWarnings {
   private static final Set<Identifier> MISSING_PROFILES = ConcurrentHashMap.newKeySet();
   private static final Set<String> DEBUG_WARNINGS = ConcurrentHashMap.newKeySet();

   private RenderCoreWarnings() {
   }

   public static void missingProfile(Identifier id) {
      if (id != null && MISSING_PROFILES.add(id)) {
         EchoRenderCore.LOGGER.warn("RenderCore visual profile {} is missing; using fallback rendering.", id);
      }
   }

   public static void clear() {
      MISSING_PROFILES.clear();
      DEBUG_WARNINGS.clear();
   }

   public static void warn(String message) {
      if (message != null && !message.isBlank() && DEBUG_WARNINGS.add(message)) {
         EchoRenderCore.LOGGER.warn(message);
      }
   }
}

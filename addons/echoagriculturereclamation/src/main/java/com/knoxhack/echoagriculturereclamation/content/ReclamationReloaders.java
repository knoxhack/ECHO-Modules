package com.knoxhack.echoagriculturereclamation.content;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import net.minecraft.resources.Identifier;

public final class ReclamationReloaders {
   private ReclamationReloaders() {
   }

   public static void addServerReloadListeners(Object event) {
      EchoBackendWorldEventBridge.addServerReloadListener(
         event,
         Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, "content"),
         new ReclamationJsonReloadListener());
   }
}

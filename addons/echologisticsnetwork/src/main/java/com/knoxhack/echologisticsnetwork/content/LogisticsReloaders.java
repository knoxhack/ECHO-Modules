package com.knoxhack.echologisticsnetwork.content;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import net.minecraft.resources.Identifier;

public final class LogisticsReloaders {
   private LogisticsReloaders() {
   }

   public static void addServerReloadListeners(Object event) {
      EchoBackendWorldEventBridge.addServerReloadListener(
         event,
         Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, "content"),
         new LogisticsJsonReloadListener());
   }
}

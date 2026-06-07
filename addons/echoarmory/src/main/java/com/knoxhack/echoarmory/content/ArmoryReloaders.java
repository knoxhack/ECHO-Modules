package com.knoxhack.echoarmory.content;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoarmory.EchoArmory;
import net.minecraft.resources.Identifier;

public final class ArmoryReloaders {
   private ArmoryReloaders() {
   }

   public static void addServerReloadListeners(Object event) {
      EchoBackendWorldEventBridge.addServerReloadListener(
         event,
         Identifier.fromNamespaceAndPath(EchoArmory.MODID, "content"),
         new ArmoryJsonReloadListener());
   }
}

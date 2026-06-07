package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echoagriculturereclamation.registry.ModEntities;

public final class ReclamationRenderCoreClientIntegration {
   private ReclamationRenderCoreClientIntegration() {
   }

   public static void registerEntityRenderers(Object event) {
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.POLLINATOR_DRONE.get(), RenderCorePollinatorDroneRenderer::new);
   }
}

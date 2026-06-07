package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echologisticsnetwork.entity.CourierDroneEntity;
import com.knoxhack.echologisticsnetwork.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class LogisticsRenderCoreClientIntegration {
   private LogisticsRenderCoreClientIntegration() {
   }

   public static void registerEntityRenderers(Object event) {
      EntityRendererProvider<CourierDroneEntity> renderer = RenderCoreCourierDroneRenderer::new;
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.COURIER_DRONE.get(), renderer);
   }
}

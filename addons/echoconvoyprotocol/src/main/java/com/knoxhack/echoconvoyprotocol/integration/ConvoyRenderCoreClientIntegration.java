package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echoconvoyprotocol.client.ConvoyRenderCoreVehicleRenderer;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class ConvoyRenderCoreClientIntegration {
   private ConvoyRenderCoreClientIntegration() {
   }

   public static void registerVehicleRenderers(Object event) {
      EntityRendererProvider<ConvoyVehicleEntity> provider = ConvoyRenderCoreVehicleRenderer::new;
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.SCRAP_BIKE.get(), provider);
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.WASTELAND_ROVER.get(), provider);
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.CARGO_CRAWLER.get(), provider);
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ARMORED_RELAY_TRUCK.get(), provider);
   }
}

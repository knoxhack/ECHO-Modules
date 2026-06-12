package com.knoxhack.echologisticsnetwork;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echologisticsnetwork.client.CourierDroneRenderer;
import com.knoxhack.echologisticsnetwork.client.LogisticsScreen;
import com.knoxhack.echologisticsnetwork.entity.CourierDroneEntity;
import com.knoxhack.echologisticsnetwork.registry.ModEntities;
import com.knoxhack.echologisticsnetwork.registry.ModMenus;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class EchoLogisticsNetworkClient {
   public EchoLogisticsNetworkClient() {
      this(null);
   }

   public EchoLogisticsNetworkClient(Object modEventBus) {
      if (EchoRuntimeModules.isLoaded("echoterminal")) {
         registerTerminalClientIntegration();
      }
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoLogisticsNetworkClient::registerMenuScreens);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoLogisticsNetworkClient::registerEntityRenderers);
   }

   static void registerMenuScreens(Object event) {
      EchoBackendClientBridge.registerMenuScreen(event, ModMenus.LOGISTICS.get(), LogisticsScreen.class);
   }

   static void registerEntityRenderers(Object event) {
      if (EchoRuntimeModules.isLoaded("echorendercore") && registerRenderCoreEntityRenderers(event)) {
         return;
      }
      EntityRendererProvider<CourierDroneEntity> renderer = CourierDroneRenderer::new;
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.COURIER_DRONE.get(), renderer);
   }

   private static void registerTerminalClientIntegration() {
      try {
         Class.forName("com.knoxhack.echologisticsnetwork.integration.LogisticsTerminalClientIntegration")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException exception) {
         EchoLogisticsNetwork.LOGGER.warn("ECHO Logistics Network terminal client integration could not be registered.", exception);
      }
   }

   private static boolean registerRenderCoreEntityRenderers(Object event) {
      try {
         Class.forName("com.knoxhack.echologisticsnetwork.integration.LogisticsRenderCoreClientIntegration")
            .getMethod("registerEntityRenderers", Object.class)
            .invoke(null, event);
         return true;
      } catch (ReflectiveOperationException | LinkageError exception) {
         EchoLogisticsNetwork.LOGGER.warn("ECHO Logistics Network RenderCore entity renderer integration unavailable; using generated family fallback renderer.", exception);
         return false;
      }
   }

}

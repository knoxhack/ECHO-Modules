package com.knoxhack.echoagriculturereclamation;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoagriculturereclamation.client.HydroponicTrayRenderer;
import com.knoxhack.echoagriculturereclamation.client.HydroponicTrayScreen;
import com.knoxhack.echoagriculturereclamation.client.PollinatorDroneRenderer;
import com.knoxhack.echoagriculturereclamation.client.ReclamationMachineScreen;
import com.knoxhack.echoagriculturereclamation.registry.ModBlockEntities;
import com.knoxhack.echoagriculturereclamation.registry.ModEntities;
import com.knoxhack.echoagriculturereclamation.registry.ModMenus;

public class EchoAgricultureReclamationClient {
   public EchoAgricultureReclamationClient(Object modEventBus) {
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoAgricultureReclamationClient::registerRenderers);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoAgricultureReclamationClient::registerMenuScreens);
      if (EchoRuntimeModules.isLoaded("echoterminal")) {
         registerTerminalClientIntegration();
      }
   }

   static void registerRenderers(Object event) {
      EchoBackendClientBridge.registerBlockEntityRenderer(event, ModBlockEntities.HYDROPONIC_TRAY.get(), HydroponicTrayRenderer::new);
      if (EchoRuntimeModules.isLoaded("echorendercore") && registerRenderCoreEntityRenderers(event)) {
         return;
      }
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.POLLINATOR_DRONE.get(), PollinatorDroneRenderer::new);
   }

   static void registerMenuScreens(Object event) {
      EchoBackendClientBridge.registerMenuScreen(event, ModMenus.RECLAMATION_MACHINE.get(), ReclamationMachineScreen.class);
      EchoBackendClientBridge.registerMenuScreen(event, ModMenus.HYDROPONIC_TRAY.get(), HydroponicTrayScreen.class);
   }

   private static void registerTerminalClientIntegration() {
      try {
         Class.forName("com.knoxhack.echoagriculturereclamation.integration.ReclamationTerminalClientIntegration")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException | LinkageError exception) {
         EchoAgricultureReclamation.LOGGER.warn("ECHO Agriculture Reclamation terminal client integration could not be registered.", exception);
      }
   }

   private static boolean registerRenderCoreEntityRenderers(Object event) {
      try {
         Class.forName("com.knoxhack.echoagriculturereclamation.integration.ReclamationRenderCoreClientIntegration")
            .getMethod("registerEntityRenderers", Object.class)
            .invoke(null, event);
         return true;
      } catch (ReflectiveOperationException | LinkageError exception) {
         EchoAgricultureReclamation.LOGGER.warn("ECHO Agriculture Reclamation RenderCore entity renderer integration unavailable; using generated fallback renderers.", exception);
         return false;
      }
   }

}

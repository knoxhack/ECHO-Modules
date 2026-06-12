package com.knoxhack.echoconvoyprotocol;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoconvoyprotocol.client.ConvoyVehicleModel;
import com.knoxhack.echoconvoyprotocol.client.ConvoyVehicleRenderer;
import com.knoxhack.echoconvoyprotocol.client.ConvoyStationScreen;
import com.knoxhack.echoconvoyprotocol.client.ConvoyUpgradeScreen;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleKind;
import com.knoxhack.echoconvoyprotocol.registry.ModEntities;
import com.knoxhack.echoconvoyprotocol.registry.ModMenus;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class EchoConvoyProtocolClient {
   public EchoConvoyProtocolClient() {
      this(null);
   }

   public EchoConvoyProtocolClient(Object modEventBus) {
      if (EchoRuntimeModules.isLoaded("echoterminal")) {
         registerTerminalClientIntegration();
      }
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoConvoyProtocolClient::registerMenuScreens);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoConvoyProtocolClient::registerLayerDefinitions);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoConvoyProtocolClient::registerEntityRenderers);
   }

   static void registerMenuScreens(Object event) {
      EchoBackendClientBridge.registerMenuScreen(event, ModMenus.CONVOY_STATION.get(), ConvoyStationScreen.class);
      EchoBackendClientBridge.registerMenuScreen(event, ModMenus.VEHICLE_UPGRADES.get(), ConvoyUpgradeScreen.class);
   }

   static void registerLayerDefinitions(Object event) {
      for (ConvoyVehicleKind kind : ConvoyVehicleKind.values()) {
         EchoBackendClientBridge.registerLayerDefinition(
            event,
            ConvoyVehicleModel.layerLocation(kind),
            () -> ConvoyVehicleModel.createBodyLayer(kind)
         );
      }
   }

   static void registerEntityRenderers(Object event) {
      if (!registerRenderCoreVehicleRenderers(event)) {
         registerFallbackVehicleRenderers(event);
      }
   }

   private static void registerFallbackVehicleRenderers(Object event) {
      EntityRendererProvider<ConvoyVehicleEntity> provider = ConvoyVehicleRenderer::new;
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.SCRAP_BIKE.get(), provider);
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.WASTELAND_ROVER.get(), provider);
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.CARGO_CRAWLER.get(), provider);
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ARMORED_RELAY_TRUCK.get(), provider);
   }

   private static void registerTerminalClientIntegration() {
      try {
         Class.forName("com.knoxhack.echoconvoyprotocol.integration.ConvoyTerminalClientIntegration")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException exception) {
         EchoConvoyProtocol.LOGGER.warn("ECHO Convoy Protocol terminal client integration could not be registered.", exception);
      }
   }

   private static boolean registerRenderCoreVehicleRenderers(Object event) {
      if (!EchoRuntimeModules.isLoaded("echorendercore")) {
         return false;
      }
      try {
         Class.forName("com.knoxhack.echoconvoyprotocol.integration.ConvoyRenderCoreClientIntegration")
            .getMethod("registerVehicleRenderers", Object.class)
            .invoke(null, event);
         return true;
      } catch (ReflectiveOperationException exception) {
         EchoConvoyProtocol.LOGGER.warn("ECHO Convoy Protocol RenderCore vehicle integration could not be registered; using fallback renderers.", exception);
         return false;
      }
   }
}

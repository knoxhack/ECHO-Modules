package com.knoxhack.echoarmory;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoarmory.client.ArmoryStationScreen;
import com.knoxhack.echoarmory.client.ArmoryProjectileRenderer;
import com.knoxhack.echoarmory.entity.ArmoryProjectileEntity;
import com.knoxhack.echoarmory.registry.ModEntities;
import com.knoxhack.echoarmory.registry.ModMenus;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class EchoArmoryClient {
   public EchoArmoryClient() {
      this(null);
   }

   public EchoArmoryClient(Object modEventBus) {
      if (EchoRuntimeModules.isLoaded("echoterminal")) {
         registerTerminalClientIntegration();
      }
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoArmoryClient::registerMenuScreens);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoArmoryClient::registerEntityRenderers);
   }

   static void registerMenuScreens(Object event) {
      EchoBackendClientBridge.registerMenuScreen(event, ModMenus.ARMORY_STATION.get(), ArmoryStationScreen.class);
   }

   static void registerEntityRenderers(Object event) {
      EntityRendererProvider<ArmoryProjectileEntity> renderer = ArmoryProjectileRenderer::new;
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ENERGY_BOLT.get(), renderer);
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.VEIL_ARROW.get(), renderer);
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.SIGIL_CHAKRAM.get(), renderer);
   }

   private static void registerTerminalClientIntegration() {
      try {
         Class.forName("com.knoxhack.echoarmory.integration.ArmoryTerminalClientIntegration")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException exception) {
         EchoArmory.LOGGER.warn("ECHO Armory terminal client integration could not be registered.", exception);
      }
   }
}

package com.knoxhack.echoindustrialnexus;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobFamilyRenderer;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoindustrialnexus.client.IndustrialMachineScreen;
import com.knoxhack.echoindustrialnexus.client.IndustrialMultiblockControllerScreen;
import com.knoxhack.echoindustrialnexus.registry.ModEntities;
import com.knoxhack.echoindustrialnexus.registry.ModMenus;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;

public class EchoIndustrialNexusClient {
   public EchoIndustrialNexusClient() {
      if (EchoRuntimeModules.isLoaded("echoterminal")) {
         registerTerminalClientIntegration();
      }
   }

   static void registerEntityRenderers(Object event) {
      boolean renderCoreLoaded = EchoRuntimeModules.isLoaded("echorendercore");
      boolean renderCoreEntities = renderCoreLoaded && registerRenderCoreEntityRenderers(event);
      if (!renderCoreEntities) {
         registerFallbackEntityRenderers(event);
      }
      if (EchoRuntimeModules.isLoaded("echorendercore")) {
         registerRenderCoreMachineRenderer(event);
      }
   }

   private static void registerFallbackEntityRenderers(Object event) {
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.FURNACE_WARDEN.get(),
         renderer("furnace_warden", EchoMobFamily.INDUSTRIAL_CONSTRUCT, 1.08F, 0.9F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.FURNACE_DRONE.get(),
         renderer("furnace_drone", EchoMobFamily.INDUSTRIAL_CONSTRUCT, 0.82F, 0.5F));
   }

   static void registerMenuScreens(Object event) {
      EchoBackendClientBridge.registerMenuScreen(event, ModMenus.INDUSTRIAL_MACHINE.get(), IndustrialMachineScreen.class);
      EchoBackendClientBridge.registerMenuScreen(event, ModMenus.INDUSTRIAL_MULTIBLOCK_CONTROLLER.get(),
         IndustrialMultiblockControllerScreen.class);
   }

   private static void registerTerminalClientIntegration() {
      try {
         Class.forName("com.knoxhack.echoindustrialnexus.integration.IndustrialTerminalClientIntegration")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException exception) {
         EchoIndustrialNexus.LOGGER.warn("ECHO Industrial Nexus terminal client integration could not be registered.", exception);
      }
   }

   private static void registerRenderCoreMachineRenderer(Object event) {
      try {
         Class.forName("com.knoxhack.echoindustrialnexus.integration.IndustrialRenderCoreClientIntegration")
            .getMethod("registerMachineRenderer", Object.class)
            .invoke(null, event);
      } catch (ReflectiveOperationException exception) {
         EchoIndustrialNexus.LOGGER.warn("ECHO Industrial Nexus RenderCore client integration could not be registered.", exception);
      }
   }

   private static boolean registerRenderCoreEntityRenderers(Object event) {
      try {
         Class.forName("com.knoxhack.echoindustrialnexus.integration.IndustrialRenderCoreClientIntegration")
            .getMethod("registerEntityRenderers", Object.class)
            .invoke(null, event);
         return true;
      } catch (ReflectiveOperationException | LinkageError exception) {
         EchoIndustrialNexus.LOGGER.warn("ECHO Industrial Nexus RenderCore entity renderer integration unavailable; using generated family fallback renderers.", exception);
         return false;
      }
   }

   private static <T extends Mob> EntityRendererProvider<T> renderer(String entityName, EchoMobFamily family,
         float scale, float shadow) {
      return context -> new EchoMobFamilyRenderer<>(context, EchoIndustrialNexus.MODID, entityName, family, scale, shadow);
   }
}

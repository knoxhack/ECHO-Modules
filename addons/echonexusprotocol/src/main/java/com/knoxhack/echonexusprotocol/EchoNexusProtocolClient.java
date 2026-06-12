package com.knoxhack.echonexusprotocol;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobFamilyRenderer;
import com.knoxhack.echonexusprotocol.client.NexusMachineScreen;
import com.knoxhack.echonexusprotocol.integration.NexusTerminalIntegration;
import com.knoxhack.echonexusprotocol.registry.ModEntities;
import com.knoxhack.echonexusprotocol.registry.ModMenus;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;

public class EchoNexusProtocolClient {
   public EchoNexusProtocolClient() {
      this(null);
   }

   public EchoNexusProtocolClient(Object modEventBus) {
      registerTerminalClientIntegration();
      if (EchoRuntimeModules.isLoaded("echorendercore")) {
         registerRenderCoreStaticSurfaces();
      }
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoNexusProtocolClient::registerMenuScreens);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoNexusProtocolClient::registerEntityRenderers);
   }

   private static void registerRenderCoreStaticSurfaces() {
      try {
         Class.forName("com.knoxhack.echonexusprotocol.integration.NexusRenderCoreClientIntegration")
            .getMethod("registerStaticSurfaces")
            .invoke(null);
      } catch (ReflectiveOperationException | LinkageError exception) {
         EchoNexusProtocol.LOGGER.warn("ECHO Nexus Protocol RenderCore static surface integration unavailable.", exception);
      }
   }

   private static void registerTerminalClientIntegration() {
      if (!EchoRuntimeModules.isLoaded("echoterminal")) {
         return;
      }

      try {
         NexusTerminalIntegration.register();
      } catch (LinkageError error) {
         EchoNexusProtocol.LOGGER.warn("ECHO-7 Nexus Terminal client integration skipped because echoterminal APIs were unavailable.", error);
      }
   }

   static void registerMenuScreens(Object event) { EchoBackendClientBridge.registerMenuScreen(event, ModMenus.NEXUS_MACHINE.get(), NexusMachineScreen.class); }

   static void registerEntityRenderers(Object event) {
      if (EchoRuntimeModules.isLoaded("echorendercore") && registerRenderCoreEntityRenderers(event)) {
         return;
      }
      registerFallbackEntityRenderers(event);
   }

   public static void registerFallbackEntityRenderers(Object event) {
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.NEXUS_HUSK.get(), renderer("nexus_husk", EchoMobFamily.HUMANOID, 1.0F, 0.55F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.DATA_WRAITH.get(), renderer("data_wraith", EchoMobFamily.WRAITH, 0.9F, 0.25F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.STATIC_CRAWLER.get(), renderer("static_crawler", EchoMobFamily.CRAWLER, 0.72F, 0.25F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.CORE_SOLDIER.get(), renderer("core_soldier", EchoMobFamily.HUMANOID, 1.08F, 0.62F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ARCHIVE_SEEKER.get(), renderer("archive_seeker", EchoMobFamily.HUMANOID, 1.18F, 0.45F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.CORRUPTION_WARDEN.get(), renderer("corruption_warden", EchoMobFamily.HEAVY_BOSS, 1.35F, 0.95F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.NEXUS_GUARDIAN.get(), renderer("nexus_guardian", EchoMobFamily.HEAVY_BOSS, 1.65F, 1.1F));
   }

   private static boolean registerRenderCoreEntityRenderers(Object event) {
      try {
         Class.forName("com.knoxhack.echonexusprotocol.integration.NexusRenderCoreClientIntegration")
            .getMethod("registerEntityRenderers", Object.class)
            .invoke(null, event);
         return true;
      } catch (ReflectiveOperationException | LinkageError exception) {
         EchoNexusProtocol.LOGGER.warn("ECHO Nexus Protocol RenderCore entity renderer integration unavailable; using generated family fallback renderers.", exception);
         return false;
      }
   }

   private static <T extends Mob> EntityRendererProvider<T> renderer(String entityName, EchoMobFamily family,
         float scale, float shadow) {
      return context -> new EchoMobFamilyRenderer<>(context, EchoNexusProtocol.MODID, entityName, family, scale, shadow);
   }
}

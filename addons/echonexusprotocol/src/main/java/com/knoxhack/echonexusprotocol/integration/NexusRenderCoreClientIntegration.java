package com.knoxhack.echonexusprotocol.integration;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echorendercore.client.EchoRenderCoreMobFamilyRenderer;
import com.knoxhack.echorendercore.client.RenderCoreStaticSurfaceRegistry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public final class NexusRenderCoreClientIntegration {
   private static boolean staticSurfacesRegistered;

   private NexusRenderCoreClientIntegration() {
   }

   public static void registerStaticSurfaces() {
      if (staticSurfacesRegistered) {
         return;
      }
      staticSurfacesRegistered = true;
      RenderCoreStaticSurfaceRegistry.register(
         Identifier.fromNamespaceAndPath(EchoNexusProtocol.MODID, "nexus_crystal_cluster"),
         Identifier.fromNamespaceAndPath(EchoNexusProtocol.MODID, "static/nexus_crystal_cluster"),
         "crystal_block"
      );
   }

   public static void registerEntityRenderers(Object event) {
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.NEXUS_HUSK.get(), renderer("nexus_husk", EchoMobFamily.HUMANOID, 1.0F, 0.55F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.DATA_WRAITH.get(), renderer("data_wraith", EchoMobFamily.WRAITH, 0.9F, 0.25F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.STATIC_CRAWLER.get(), renderer("static_crawler", EchoMobFamily.CRAWLER, 0.72F, 0.25F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.CORE_SOLDIER.get(), renderer("core_soldier", EchoMobFamily.HUMANOID, 1.08F, 0.62F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ARCHIVE_SEEKER.get(), renderer("archive_seeker", EchoMobFamily.HUMANOID, 1.18F, 0.45F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.CORRUPTION_WARDEN.get(), renderer("corruption_warden", EchoMobFamily.HEAVY_BOSS, 1.35F, 0.95F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.NEXUS_GUARDIAN.get(), renderer("nexus_guardian", EchoMobFamily.HEAVY_BOSS, 1.65F, 1.1F));
   }

   private static <T extends Mob> EntityRendererProvider<T> renderer(String entityName, EchoMobFamily family,
         float scale, float shadow) {
      return context -> new EchoRenderCoreMobFamilyRenderer<>(context, EchoNexusProtocol.MODID, entityName, family, scale, shadow);
   }
}

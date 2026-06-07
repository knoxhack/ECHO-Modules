package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echorendercore.client.EchoRenderCoreMobFamilyRenderer;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import com.knoxhack.echoindustrialnexus.client.IndustrialRenderCoreMachineRenderer;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import com.knoxhack.echoindustrialnexus.registry.ModEntities;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;

public final class IndustrialRenderCoreClientIntegration {
   private IndustrialRenderCoreClientIntegration() {
   }

   public static void registerMachineRenderer(Object event) {
      BlockEntityRendererProvider<IndustrialMachineBlockEntity, IndustrialRenderCoreMachineRenderer.MachineVisualRenderState> provider =
         IndustrialRenderCoreMachineRenderer::new;
      EchoBackendClientBridge.registerBlockEntityRenderer(event, ModBlockEntities.INDUSTRIAL_MACHINE.get(),
         provider);
   }

   public static void registerEntityRenderers(Object event) {
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.FURNACE_WARDEN.get(),
         renderer("furnace_warden", EchoMobFamily.INDUSTRIAL_CONSTRUCT, 1.08F, 0.9F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.FURNACE_DRONE.get(),
         renderer("furnace_drone", EchoMobFamily.INDUSTRIAL_CONSTRUCT, 0.82F, 0.5F));
   }

   private static <T extends Mob> EntityRendererProvider<T> renderer(String entityName, EchoMobFamily family,
         float scale, float shadow) {
      return context -> new EchoRenderCoreMobFamilyRenderer<>(context, EchoIndustrialNexus.MODID, entityName, family, scale, shadow);
   }
}

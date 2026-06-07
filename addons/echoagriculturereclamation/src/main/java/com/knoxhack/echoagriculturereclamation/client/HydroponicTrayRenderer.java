package com.knoxhack.echoagriculturereclamation.client;

import com.knoxhack.echoagriculturereclamation.block.ReclamationCropBlock;
import com.knoxhack.echoagriculturereclamation.block.entity.HydroponicTrayBlockEntity;
import com.knoxhack.echoagriculturereclamation.content.ReclamationCropLogic;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class HydroponicTrayRenderer implements BlockEntityRenderer<HydroponicTrayBlockEntity, HydroponicTrayRenderer.TrayRenderState> {
   private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
   private final BlockModelResolver blockModelResolver;

   public HydroponicTrayRenderer(BlockEntityRendererProvider.Context context) {
      this.blockModelResolver = context.blockModelResolver();
   }

   @Override
   public TrayRenderState createRenderState() {
      return new TrayRenderState();
   }

   @Override
   public void extractRenderState(HydroponicTrayBlockEntity tray, TrayRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
      BlockEntityRenderer.super.extractRenderState(tray, state, partialTick, cameraPos, breakProgress);
      state.cropModel.clear();
      state.hasCrop = false;
      SeedProfile profile = tray.profile();
      if (profile == null) {
         return;
      }
      int age = Math.max(0, Math.min(7, tray.age()));
      ReclamationCropBlock crop = ModBlocks.cropBlock(profile.spec());
      BlockState cropState = crop.defaultBlockState()
         .setValue(ReclamationCropBlock.AGE, age)
         .setValue(ReclamationCropBlock.STABILIZED, ReclamationCropLogic.stable(profile));
      blockModelResolver.update(state.cropModel, cropState, DISPLAY_CONTEXT);
      state.hasCrop = !state.cropModel.isEmpty();
   }

   @Override
   public void submit(TrayRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      if (!state.hasCrop) {
         return;
      }
      poseStack.pushPose();
      poseStack.translate(0.125D, 1.0D, 0.125D);
      poseStack.scale(0.75F, 0.75F, 0.75F);
      state.cropModel.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
      poseStack.popPose();
   }

   public static final class TrayRenderState extends BlockEntityRenderState {
      private final BlockModelRenderState cropModel = new BlockModelRenderState();
      private boolean hasCrop;
   }
}

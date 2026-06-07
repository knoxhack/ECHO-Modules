package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.RenderCoreBlockVisualHost;
import com.knoxhack.echorendercore.profile.VisualProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class RenderCoreBlockEntityRenderer<T extends BlockEntity>
      extends AdvancedBlockEntityVisualRenderer<T, RenderCoreBlockEntityRenderer.BlockVisualRenderState> {
   private final VisualHostFactory<T> hostFactory;

   public RenderCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context, VisualHostFactory<T> hostFactory) {
      this.hostFactory = hostFactory;
   }

   @Override
   public BlockVisualRenderState createRenderState() {
      return new BlockVisualRenderState();
   }

   @Override
   public void extractRenderState(T blockEntity, BlockVisualRenderState state, float partialTick, Vec3 cameraPos,
         ModelFeatureRenderer.CrumblingOverlay breakProgress) {
      RenderCoreBlockVisualHost host = hostFactory == null ? null : hostFactory.create(blockEntity, partialTick);
      state.data = RenderCoreBlockVisuals.resolve(
         blockEntity.getLevel(),
         blockEntity.getBlockPos(),
         blockEntity.getBlockState(),
         host,
         partialTick,
         state.lightCoords
      );
   }

   @Override
   public void submit(BlockVisualRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      RenderCoreBlockVisuals.RenderData data = state.data;
      if (data == null || data.visualProfile() == null) {
         return;
      }
      VisualProfile profile = data.visualProfile();
      submitBlockModelLayers(data.blockState(), poseStack, collector, data.context(), profile);
      RenderCoreBlockVisuals.spawnParticlesOncePerTick(data);
   }

   @FunctionalInterface
   public interface VisualHostFactory<T extends BlockEntity> {
      RenderCoreBlockVisualHost create(T blockEntity, float partialTick);
   }

   public static final class BlockVisualRenderState extends BlockEntityRenderState {
      private RenderCoreBlockVisuals.RenderData data;
   }
}

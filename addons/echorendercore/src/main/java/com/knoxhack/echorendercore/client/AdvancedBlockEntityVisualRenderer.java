package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualContext;
import com.knoxhack.echorendercore.profile.VisualLayerProfile;
import com.knoxhack.echorendercore.profile.VisualProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class AdvancedBlockEntityVisualRenderer<T extends BlockEntity, S extends BlockEntityRenderState>
      implements BlockEntityRenderer<T, S> {
   protected VisualProfile profile(VisualContext context) {
      return com.knoxhack.echorendercore.profile.RenderCoreProfiles.visual(context.profileId());
   }

   protected List<VisualLayerProfile> layers(VisualContext context, VisualProfile profile) {
      return profile == null ? List.of() : profile.layersFor(context.state(), context.variant());
   }

   protected Vec3 anchor(Level level, BlockPos pos, VisualProfile profile, String anchorName) {
      return ParticleAnchorResolver.resolve(pos == null ? Vec3.ZERO : Vec3.atCenterOf(pos), profile, anchorName);
   }

   protected void rememberDebugTarget(Level level, BlockPos pos, VisualProfile profile, VisualContext context) {
      RenderCoreDebugTargets.rememberBlock(level, pos, profile, context);
   }

   protected Map<String, List<BlockStateModelPart>> resolveBlockParts(BlockState blockState, VisualProfile profile) {
      return BakedBlockPartResolver.resolve(blockState, profile);
   }

   protected boolean submitBlockModelLayers(BlockState blockState, PoseStack poseStack, SubmitNodeCollector collector,
         VisualContext context, VisualProfile profile) {
      if (profile == null || profile.blockParts().isEmpty()) {
         return false;
      }
      boolean submitted = false;
      int order = 6;
      for (VisualLayerProfile layer : layers(context, profile)) {
         submitted |= BakedBlockPartMask.submitLayer(blockState, profile, layer, poseStack, collector, context, order++);
      }
      return submitted;
   }
}

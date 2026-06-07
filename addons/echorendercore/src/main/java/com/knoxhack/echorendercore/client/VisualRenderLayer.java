package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class VisualRenderLayer {
   private VisualRenderLayer() {
   }

   public static <S extends EntityRenderState> void submitBase(EntityModel<S> model, S state,
         PoseStack poseStack, SubmitNodeCollector collector, VisualContext context, Identifier texture) {
      if (texture != null) {
         collector.submitModel(model, state, poseStack, texture, context.packedLight(), OverlayTexture.NO_OVERLAY, state.outlineColor, null);
      }
   }
}

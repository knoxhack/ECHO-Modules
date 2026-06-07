package com.knoxhack.echorendercore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class OverlayRenderLayer {
   private static final int OVERLAY_COLOR = 0xAAFFFFFF;

   private OverlayRenderLayer() {
   }

   public static <S extends EntityRenderState> void submit(EntityModel<S> model, S state, PoseStack poseStack,
         SubmitNodeCollector collector, Identifier texture, int order) {
      submit(model, state, poseStack, collector, texture, order, OVERLAY_COLOR);
   }

   public static <S extends EntityRenderState> void submit(EntityModel<S> model, S state, PoseStack poseStack,
         SubmitNodeCollector collector, Identifier texture, int order, int color) {
      submit(model, state, poseStack, collector, texture, order, color, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
   }

   public static <S extends EntityRenderState> void submit(EntityModel<S> model, S state, PoseStack poseStack,
         SubmitNodeCollector collector, Identifier texture, int order, int color, int light, int overlay, int outlineColor) {
      if (texture == null || state.isInvisible) {
         return;
      }
      OrderedSubmitNodeCollector ordered = collector.order(order);
      ordered.submitModel(model, state, poseStack, renderType(texture), light, overlay,
         color == 0 ? OVERLAY_COLOR : color, null, outlineColor, null);
   }

   public static RenderType renderType(Identifier texture) {
      return RenderTypes.entityTranslucent(texture);
   }
}

package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class GlowRenderLayer {
   private static final int ECHO_CYAN = 0xFF66E8FF;

   private GlowRenderLayer() {
   }

   public static boolean shouldGlow(VisualState state) {
      return state == VisualState.ONLINE
         || state == VisualState.ACTIVE
         || state == VisualState.WORKING
         || state == VisualState.SCANNING
         || state == VisualState.CHARGING
         || state == VisualState.COMPLETE;
   }

   public static <S extends EntityRenderState> void submit(EntityModel<S> model, S state, PoseStack poseStack,
         SubmitNodeCollector collector, Identifier texture) {
      submit(model, state, poseStack, collector, texture, ECHO_CYAN);
   }

   public static <S extends EntityRenderState> void submit(EntityModel<S> model, S state, PoseStack poseStack,
         SubmitNodeCollector collector, Identifier texture, int color) {
      submit(model, state, poseStack, collector, texture, color, 1, 0xF000F0, OverlayTexture.NO_OVERLAY, state.outlineColor);
   }

   public static <S extends EntityRenderState> void submit(EntityModel<S> model, S state, PoseStack poseStack,
         SubmitNodeCollector collector, Identifier texture, int color, int order, int light, int overlay, int outlineColor) {
      if (texture == null || state.isInvisible) {
         return;
      }
      OrderedSubmitNodeCollector ordered = collector.order(Math.max(1, order));
      ordered.submitModel(model, state, poseStack, RenderTypes.eyes(texture), light, overlay,
         color == 0 ? ECHO_CYAN : color, null, outlineColor, null);
   }

   public static RenderType renderType(Identifier texture) {
      return RenderTypes.eyes(texture);
   }
}

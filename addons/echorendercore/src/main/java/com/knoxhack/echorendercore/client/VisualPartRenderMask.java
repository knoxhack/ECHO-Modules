package com.knoxhack.echorendercore.client;

import com.knoxhack.echocore.client.model.EchoNamedModelPartProvider;
import com.knoxhack.echorendercore.EchoRenderCore;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class VisualPartRenderMask {
   private VisualPartRenderMask() {
   }

   public static <S extends EntityRenderState> boolean submit(EntityModel<S> model, S state, PoseStack poseStack,
         SubmitNodeCollector collector, Collection<String> partNames, RenderType renderType, int order, int light,
         int color) {
      return submit(model, state, poseStack, collector, partNames, renderType, order, light, OverlayTexture.NO_OVERLAY, color);
   }

   public static <S extends EntityRenderState> boolean submit(EntityModel<S> model, S state, PoseStack poseStack,
         SubmitNodeCollector collector, Collection<String> partNames, RenderType renderType, int order, int light,
         int overlay, int color) {
      if (partNames == null || partNames.isEmpty()) {
         return false;
      }
      Map<String, ModelPart> parts;
      if (model instanceof EchoNamedModelPartProvider provider) {
         parts = provider.echoNamedModelParts();
      } else if (model instanceof RenderCorePartProvider provider) {
         parts = provider.renderCoreParts();
      } else {
         warn("RenderCore layer mask requested for model {} but it does not expose named parts.", model.getClass().getName());
         return true;
      }
      if (parts.isEmpty()) {
         warn("RenderCore layer mask requested for model {} but no named parts were provided.", model.getClass().getName());
         return true;
      }
      model.setupAnim(state);
      OrderedSubmitNodeCollector ordered = collector.order(order);
      boolean submitted = false;
      for (String partName : partNames) {
         ModelPart part = parts.get(partName);
         if (part == null) {
            warn("RenderCore layer mask skipped missing model part '{}'.", partName);
            continue;
         }
         ordered.submitModelPart(part, poseStack, renderType, light, overlay, null, color, null);
         submitted = true;
      }
      return submitted || !partNames.isEmpty();
   }

   private static void warn(String message, Object... args) {
      if (DebugVisualOverrides.missingPartWarnings()) {
         EchoRenderCore.LOGGER.warn(message, args);
      }
   }
}

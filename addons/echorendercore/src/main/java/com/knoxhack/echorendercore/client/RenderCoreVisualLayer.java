package com.knoxhack.echorendercore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class RenderCoreVisualLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
   public RenderCoreVisualLayer(RenderLayerParent<S, M> parent) {
      super(parent);
   }

   @Override
   public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, S state, float yRot, float xRot) {
      RenderCoreEntityVisuals.RenderData data = RenderCoreEntityVisuals.get(state);
      if (data == null || data.visualProfile() == null) {
         return;
      }
      @SuppressWarnings("unchecked")
      EntityModel<S> model = (EntityModel<S>) getParentModel();
      model.setupAnim(state);
      RenderCoreEntityVisuals.spawnParticlesOncePerTick(data, model);
      VisualProfileRenderer.submitEntityLayers(model, state, poseStack, collector, data.context(), data.visualProfile());
   }
}

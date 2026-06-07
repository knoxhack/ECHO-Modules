package com.knoxhack.echorendercore.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public abstract class RenderCoreMobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
      extends MobRenderer<T, S, M> {
   private final Identifier profileId;
   private final Identifier fallbackTexture;

   protected RenderCoreMobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius,
         Identifier profileId, Identifier fallbackTexture) {
      super(context, model, shadowRadius);
      this.profileId = profileId;
      this.fallbackTexture = fallbackTexture;
      addLayer(new RenderCoreVisualLayer<>(this));
   }

   @Override
   public void extractRenderState(T entity, S state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      RenderCoreEntityVisuals.attach(entity, state, profileId(entity), fallbackTexture(entity), visualVariant(entity),
         partialTick, fallbackStatus(entity));
   }

   protected Identifier profileId(T entity) {
      return profileId;
   }

   protected Identifier fallbackTexture(T entity) {
      return fallbackTexture;
   }

   protected com.knoxhack.echorendercore.api.VisualVariant visualVariant(T entity) {
      return com.knoxhack.echorendercore.api.VisualVariant.DEFAULT;
   }

   protected String fallbackStatus(T entity) {
      return "rendercore_native";
   }
}

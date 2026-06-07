package com.knoxhack.echorendercore.client;

import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobModelFactory;
import com.knoxhack.echocore.client.model.EchoMobRenderIds;
import com.knoxhack.echocore.client.model.EchoMobRenderState;
import com.knoxhack.echocore.client.ui.EchoOverheadDialogCards;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public class EchoRenderCoreMobFamilyRenderer<T extends Mob> extends RenderCoreMobRenderer<T, EchoMobRenderState,
      net.minecraft.client.model.EntityModel<EchoMobRenderState>> {
   private final Identifier texture;
   private final float scale;
   private final float shadow;

   public EchoRenderCoreMobFamilyRenderer(EntityRendererProvider.Context context, String modid, String entityName,
         EchoMobFamily family, float scale, float shadow) {
      super(context, EchoMobModelFactory.create(context, family, entityName), shadow, profile(modid, entityName),
         texture(modid, entityName));
      this.texture = texture(modid, entityName);
      this.scale = scale;
      this.shadow = shadow;
   }

   @Override
   public EchoMobRenderState createRenderState() {
      return new EchoMobRenderState();
   }

   @Override
   public void extractRenderState(T entity, EchoMobRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      this.shadowRadius = shadow;
      state.tint = tint(entity, state, partialTick);
   }

   @Override
   public Identifier getTextureLocation(EchoMobRenderState state) {
      return texture;
   }

   @Override
   public void submit(EchoMobRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
         CameraRenderState cameraRenderState) {
      super.submit(state, poseStack, collector, cameraRenderState);
      EchoOverheadDialogCards.submit(state, getFont(), poseStack, collector, cameraRenderState);
   }

   @Override
   protected int getModelTint(EchoMobRenderState state) {
      return state.tint;
   }

   @Override
   protected void scale(EchoMobRenderState state, PoseStack poseStack) {
      if (scale != 1.0F) {
         poseStack.scale(scale, scale, scale);
      }
   }

   protected int tint(T entity, EchoMobRenderState state, float partialTick) {
      return 0xFFFFFFFF;
   }

   private static Identifier texture(String modid, String entityName) {
      return EchoMobRenderIds.baseTexture(modid, entityName);
   }

   private static Identifier profile(String modid, String entityName) {
      return EchoMobRenderIds.profile(modid, entityName);
   }
}

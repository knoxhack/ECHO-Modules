package com.knoxhack.echoarmory.client;

import com.knoxhack.echoarmory.entity.ArmoryProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ArmoryProjectileRenderer extends EntityRenderer<ArmoryProjectileEntity, ArmoryProjectileRenderState> {
   public ArmoryProjectileRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   @Override
   public ArmoryProjectileRenderState createRenderState() {
      return new ArmoryProjectileRenderState();
   }

   @Override
   public void extractRenderState(ArmoryProjectileEntity entity, ArmoryProjectileRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      state.kind = entity.projectileKind();
   }
}
